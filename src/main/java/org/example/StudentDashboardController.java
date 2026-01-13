package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentDashboardController {

    @FXML private Button notificationsButton;

    @FXML
    public void initialize() {
        // Basic unread count: active published announcements not marked read by this student
        String currentStudentId = org.example.StudentSession.getCurrentRoll();
        int unread = 0;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) AS c FROM announcements a " +
                     "WHERE a.published = 1 AND (a.start_at IS NULL OR a.start_at <= datetime('now')) " +
                     "AND (a.end_at IS NULL OR a.end_at >= datetime('now')) " +
                     "AND NOT EXISTS (SELECT 1 FROM student_notification_status s WHERE s.announcement_id = a.id AND s.student_id = ?)"
             )) {
            ps.setString(1, currentStudentId == null ? "guest" : currentStudentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) unread = rs.getInt("c");
            }
        } catch (Exception ignore) {}

        notificationsButton.setText("🔔" + (unread > 0 ? (" " + unread) : ""));
    }

    @FXML
    private void goHome() throws Exception {
        Main.switchScene("welcome.fxml");
    }

    @FXML
    private void openNotifications() throws Exception {
        try {
            java.net.URL resource = Main.class.getResource("/org/example/student_notifications.fxml");
            if (resource == null) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Notifications screen missing (resource not found)", ButtonType.OK);
                a.setHeaderText(null);
                a.showAndWait();
                return;
            }
            Main.switchScene("student_notifications.fxml");
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Unable to open Notifications: " + e.getMessage(), ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    @FXML
    private void goToViewEvents() throws Exception {
        try {
            java.net.URL resource = Main.class.getResource("/org/example/view_events.fxml");
            if (resource == null) {
                Alert a = new Alert(Alert.AlertType.ERROR, "View Events screen missing (resource not found)", ButtonType.OK);
                a.setHeaderText(null);
                a.showAndWait();
                return;
            }
            Main.switchScene("view_events.fxml");
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Unable to open View Events: " + e.getMessage(), ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    @FXML
    private void goToMyRegistrations() throws Exception {
        try {
            java.net.URL resource = Main.class.getResource("/org/example/my_registrations.fxml");
            if (resource == null) {
                Alert a = new Alert(Alert.AlertType.ERROR, "My Registrations screen missing (resource not found)", ButtonType.OK);
                a.setHeaderText(null);
                a.showAndWait();
                return;
            }
            Main.switchScene("my_registrations.fxml");
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Unable to open My Registrations: " + e.getMessage(), ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        }
    }
}
