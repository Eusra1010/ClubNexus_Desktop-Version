package org.example;

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
            Path dataDir = Paths.get("data");
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
                    password_hash TEXT NOT NULL
                )
            """);

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
                    registration_deadline TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS registrations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    email TEXT NOT NULL,
                    contact TEXT NOT NULL,
                    paid INTEGER DEFAULT 0,
                    registered_at TEXT DEFAULT (datetime('now'))
                )
            """);

            // Add optional columns for batch and department if not present
            try { stmt.execute("ALTER TABLE registrations ADD COLUMN batch TEXT"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE registrations ADD COLUMN department TEXT"); } catch (Exception ignore) {}

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }
}
