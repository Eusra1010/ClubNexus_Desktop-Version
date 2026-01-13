package org.example;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Database {

    private static final String DB_URL;

    static {
        String url;
        try {
            // Resolve data directory at the project root (where pom.xml lives) if possible,
            // otherwise fall back to current working directory. This avoids writing under target/classes.
            Path cwd = Paths.get("").toAbsolutePath();
            Path probe = cwd;
            Path dataDir = null;
            for (int i = 0; i < 6 && probe != null; i++) {
                if (Files.exists(probe.resolve("pom.xml"))) {
                    dataDir = probe.resolve("data");
                    break;
                }
                probe = probe.getParent();
            }
            if (dataDir == null) {
                dataDir = cwd.resolve("data");
            }
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            Path dbPath = dataDir.resolve("clubnexus.db").toAbsolutePath();
            url = "jdbc:sqlite:" + dbPath.toString();
        } catch (Exception e) {
            // Fallback to relative path if any issue computing absolute path
            url = "jdbc:sqlite:data/clubnexus.db";
        }
        DB_URL = url;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    roll TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    university TEXT
                )
            """);

            // Add university column for existing installations
            try { stmt.execute("ALTER TABLE students ADD COLUMN university TEXT"); } catch (Exception ignore) {}

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS admins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    admin_id TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    club_name TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS events (
                    event_id TEXT PRIMARY KEY,
                    event_name TEXT NOT NULL,
                    event_date TEXT NOT NULL,
                    venue TEXT NOT NULL,
                    club_name TEXT NOT NULL,
                    fees TEXT,
                    registration_open INTEGER,
                    registration_count INTEGER,
                    status TEXT,
                    registration_deadline TEXT,
                    is_group INTEGER DEFAULT 0,
                    min_group_size INTEGER,
                    max_group_size INTEGER
                )
            """);

            // Backfill missing event columns if table existed earlier without them
            try { stmt.execute("ALTER TABLE events ADD COLUMN registration_open INTEGER"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE events ADD COLUMN registration_count INTEGER"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE events ADD COLUMN status TEXT"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE events ADD COLUMN registration_deadline TEXT"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE events ADD COLUMN fees TEXT"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE events ADD COLUMN is_group INTEGER DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE events ADD COLUMN min_group_size INTEGER"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE events ADD COLUMN max_group_size INTEGER"); } catch (Exception ignore) {}

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS registrations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    roll TEXT,
                    email TEXT NOT NULL,
                    contact TEXT NOT NULL,
                    paid INTEGER DEFAULT 0,
                    paid_at TEXT,
                    ticket_code TEXT,
                    registered_at TEXT DEFAULT (datetime('now')),
                    batch TEXT,
                    department TEXT,
                    university TEXT,
                    group_name TEXT
                )
            """);

            // Add optional columns for batch, department, university, group_name if not present
            try { stmt.execute("ALTER TABLE registrations ADD COLUMN batch TEXT"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE registrations ADD COLUMN department TEXT"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE registrations ADD COLUMN roll TEXT"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE registrations ADD COLUMN paid_at TEXT"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE registrations ADD COLUMN ticket_code TEXT"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE registrations ADD COLUMN university TEXT"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE registrations ADD COLUMN group_name TEXT"); } catch (Exception ignore) {}

            // New table to store group members for group events
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS registration_members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    registration_id INTEGER NOT NULL,
                    full_name TEXT NOT NULL,
                    email TEXT NOT NULL,
                    contact TEXT NOT NULL,
                    department TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now'))
                )
            """);

            // Announcements: admin-created and system-generated messages
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS announcements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    type TEXT NOT NULL,
                    body TEXT,
                    event_id TEXT,
                    is_system_generated INTEGER NOT NULL DEFAULT 0,
                    published INTEGER NOT NULL DEFAULT 1,
                    start_at TEXT,
                    end_at TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now'))
                )
            """);

            // Track read status per student for notifications badge
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS student_notification_status (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    announcement_id INTEGER NOT NULL,
                    student_id TEXT NOT NULL,
                    read_at TEXT,
                    UNIQUE (announcement_id, student_id)
                )
            """);

            // Ensure the DB file is not hidden on Windows
            try {
                String os = System.getProperty("os.name");
                if (os != null && os.toLowerCase().contains("windows")) {
                    Path dbPath = Paths.get(DB_URL.replace("jdbc:sqlite:", ""));
                    if (Files.exists(dbPath)) {
                        Files.setAttribute(dbPath, "dos:hidden", false);
                    }
                }
            } catch (Exception ignore) {
                // If attributes are unsupported or fail, continue
            }

            // Initialize/repair denormalized counts for events
            try {
                stmt.executeUpdate("UPDATE events SET registration_count = (SELECT COUNT(*) FROM registrations r WHERE r.event_id = events.event_id)");
            } catch (Exception ignore) {}

            // Write debug info with resolved DB path and current tables to help diagnose viewer issues
            try {
                Path dbPath = Paths.get(DB_URL.replace("jdbc:sqlite:", ""));
                StringBuilder info = new StringBuilder();
                info.append("DB Path: ").append(dbPath.toString()).append(System.lineSeparator());
                try {
                    long size = Files.exists(dbPath) ? Files.size(dbPath) : -1;
                    info.append("DB Size: ").append(size).append(" bytes").append(System.lineSeparator());
                } catch (Exception ignore2) {}
                info.append("Tables:\n");
                try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")) {
                    while (rs.next()) {
                        info.append(" - ").append(rs.getString("name")).append(System.lineSeparator());
                    }
                }
                Path infoFile = dbPath.getParent().resolve("db-info.txt");
                Files.writeString(infoFile, info.toString());
            } catch (Exception ignore) {}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }
}
