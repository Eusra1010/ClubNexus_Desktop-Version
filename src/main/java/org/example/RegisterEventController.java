package org.example;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class RegisterEventController {

    // Static context set by ViewEventsController before scene switch
    public static String selectedEventId;
    public static String selectedEventName;
    public static String selectedClub;

    @FXML private Label eventNameLabel;
    @FXML private Label clubLabel;

    @FXML private TextField fullNameField;
    @FXML private TextField contactField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> batchCombo;
    @FXML private TextField departmentField;

    @FXML
    public void initialize() {
        // Populate batch options
        batchCombo.setItems(FXCollections.observableArrayList("2k20","2k21","2k22","2k23","2k24"));

        // Ensure event context is available; if not, prompt to select one
        if (selectedEventId == null) {
            try { promptForEventSelection(); } catch (Exception ignored) {}
        }

        // Set event context labels
        eventNameLabel.setText(selectedEventName != null ? selectedEventName : "");
        clubLabel.setText(selectedClub != null ? selectedClub : "");
    }

    @FXML
    private void goBack() throws Exception {
        Main.switchScene("view_events.fxml");
    }

    @FXML
    private void submit() {
        String name = safe(fullNameField.getText());
        String contact = safe(contactField.getText());
        String email = safe(emailField.getText());
        String batch = batchCombo.getValue();
        String department = safe(departmentField.getText());
        String university = fetchUniversityForCurrentStudent();

        StringBuilder errs = new StringBuilder();
        if (name.isEmpty()) errs.append("Full Name is required\n");
        String digits = contact.replaceAll("[^0-9]", "");
        if (!digits.matches("01\\d{9}")) errs.append("Contact must be 11-digit BD number starting 01\n");
        if (email.isEmpty() || !email.contains("@")) errs.append("Valid Email is required\n");
        if (batch == null || batch.isEmpty()) errs.append("Batch is required\n");
        if (department.isEmpty()) errs.append("Department is required\n");
        if (selectedEventId == null) errs.append("Event context missing\n");

        if (errs.length() > 0) {
            Alert a = new Alert(Alert.AlertType.ERROR, errs.toString(), ButtonType.OK);
            a.setHeaderText("Invalid registration");
            a.showAndWait();
            return;
        }

        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO registrations (event_id, full_name, roll, email, contact, paid, batch, department, university) VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)")) {
                ps.setString(1, selectedEventId);
                ps.setString(2, name);
                ps.setString(3, org.example.StudentSession.getCurrentRoll());
                ps.setString(4, email);
                ps.setString(5, contact);
                ps.setString(6, batch);
                ps.setString(7, department);
                ps.setString(8, university);
                ps.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE events SET registration_count = COALESCE(registration_count, 0) + 1 WHERE event_id = ?")) {
                ps2.setString(1, selectedEventId);
                ps2.executeUpdate();
            }
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Registered successfully!", ButtonType.OK);
            ok.setHeaderText(null);
            ok.showAndWait();
            try { Main.switchScene("view_events.fxml"); } catch (Exception ignore) {}
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Registration Error");
            err.setHeaderText(null);
            err.setContentText("Failed to register: " + (ex.getMessage() == null ? "Unknown error" : ex.getMessage()));
            err.getButtonTypes().setAll(ButtonType.OK);
            err.showAndWait();
        }
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private void promptForEventSelection() throws Exception {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT event_id, event_name, club_name FROM events WHERE COALESCE(registration_open,1)=1 AND UPPER(COALESCE(status,'ACTIVE')) <> 'CANCELLED' ORDER BY event_date ASC");
             java.sql.ResultSet rs = ps.executeQuery()) {

            java.util.List<EventChoice> options = new java.util.ArrayList<>();
            while (rs.next()) {
                options.add(new EventChoice(
                        rs.getString("event_id"),
                        rs.getString("event_name"),
                        rs.getString("club_name")
                ));
            }

            if (options.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "No open events available.", ButtonType.OK);
                a.setHeaderText(null);
                a.showAndWait();
                return;
            }

            ChoiceDialog<EventChoice> dialog = new ChoiceDialog<>(options.get(0), options);
            dialog.setTitle("Select Event");
            dialog.setHeaderText("Choose an event to register");
            dialog.setContentText("Event:");

            java.util.Optional<EventChoice> sel = dialog.showAndWait();
            if (sel.isPresent()) {
                EventChoice ch = sel.get();
                selectedEventId = ch.id;
                selectedEventName = ch.name;
                selectedClub = ch.club;
            }
        }
    }

    static class EventChoice {
        final String id; final String name; final String club;
        EventChoice(String id, String name, String club) { this.id = id; this.name = name; this.club = club; }
        public String toString() { return name + " (" + club + ")"; }
    }

    private String fetchUniversityForCurrentStudent() {
        String roll = org.example.StudentSession.getCurrentRoll();
        if (roll == null || roll.isBlank()) return null;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT university FROM students WHERE roll = ?")) {
            ps.setString(1, roll);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("university");
            }
        } catch (Exception ignore) {}
        return null;
    }
}
