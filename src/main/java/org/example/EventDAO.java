package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

public class EventDAO {

    public static void insert(Event event) throws Exception {

        String sql = """
            INSERT INTO events
            (event_id, event_name, club_name, event_date, venue,
             fees, registration_deadline, registration_open)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = Database.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setString(1, event.getEventId());
        ps.setString(2, event.getEventName());
        ps.setString(3, event.getClubName());
        ps.setString(4, event.getEventDate());
        ps.setString(5, event.getVenue());
        ps.setString(6, String.valueOf(event.getFees()));
        ps.setString(7, event.getRegistrationDeadline());
        ps.setInt(8, event.isRegistrationOpen() ? 1 : 0);

        ps.executeUpdate();
        ps.close();
        conn.close();
    }

    public static String generateEventId() {
        return UUID.randomUUID().toString();
    }
}
