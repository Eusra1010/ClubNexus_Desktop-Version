package org.example;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CreateAnnouncementController {

    @FXML private TextField titleField;
    @FXML private ComboBox<EventOption> eventCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextArea bodyArea;

    @FXML
    public void initialize() {
        // Load events for optional association
        List<EventOption> options = new ArrayList<>();
        options.add(new EventOption(null, "None"));
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT event_id, event_name FROM events ORDER BY event_date DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    options.add(new EventOption(rs.getString("event_id"), rs.getString("event_name")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        eventCombo.setItems(FXCollections.observableArrayList(options));
        eventCombo.getSelectionModel().selectFirst();

        // Types
        typeCombo.setItems(FXCollections.observableArrayList(
                "EVENT_UPDATE",
                "REGISTRATION_CONFIRMED",
                "EMAIL_SENT",
                "PAYMENT_INFORMATION_SENT",
                "EVENT_REMINDER",
                "EVENT_CANCELLED"
        ));
    }

    @FXML
    private void onBack() throws Exception {
        // Return to admin dashboard main content
        Main.switchScene("admin_dashboard.fxml");
    }

    @FXML
    private void onPublish() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String type = typeCombo.getValue();
        EventOption ev = eventCombo.getValue();
        String body = bodyArea.getText();

        if (title.isEmpty() || type == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Title and Type are required.", ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
            return;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO announcements (title, type, body, event_id, is_system_generated, published) VALUES (?,?,?,?,0,1)"
             )) {
            ps.setString(1, title);
            ps.setString(2, type);
            ps.setString(3, body);
            ps.setString(4, ev == null ? null : ev.id);
            ps.executeUpdate();

            Alert a = new Alert(Alert.AlertType.INFORMATION, "Announcement published.", ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();

            // Clear form
            titleField.clear();
            bodyArea.clear();
            eventCombo.getSelectionModel().selectFirst();
            typeCombo.getSelectionModel().clearSelection();

        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, "Failed to publish announcement.", ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    public static class EventOption {
        final String id;
        final String name;
        public EventOption(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}
