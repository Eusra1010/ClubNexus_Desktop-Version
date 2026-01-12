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
                    "INSERT INTO registrations (event_id, full_name, email, contact, paid, batch, department) VALUES (?, ?, ?, ?, 0, ?, ?)")) {
                ps.setString(1, selectedEventId);
                ps.setString(2, name);
                ps.setString(3, email);
                ps.setString(4, contact);
                ps.setString(5, batch);
                ps.setString(6, department);
                ps.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE events SET registration_count = COALESCE(registration_count, 0) + 1 WHERE event_id = ?")) {
                ps2.setString(1, selectedEventId);
                ps2.executeUpdate();
            }
            Alert choice = new Alert(Alert.AlertType.INFORMATION);
            choice.setHeaderText(null);
            choice.setContentText("Registered successfully! What would you like to do next?");
            ButtonType stayEvents = new ButtonType("View More Events", ButtonBar.ButtonData.OK_DONE);
            ButtonType goDashboard = new ButtonType("Go to Dashboard", ButtonBar.ButtonData.FINISH);
            choice.getButtonTypes().setAll(stayEvents, goDashboard);
            ButtonType result = choice.showAndWait().orElse(stayEvents);
            if (result == goDashboard) {
                try { Main.switchScene("student_dashboard.fxml"); } catch (Exception ignore) {}
            } else {
                try { Main.switchScene("view_events.fxml"); } catch (Exception ignore) {}
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to register", ButtonType.OK);
            err.showAndWait();
        }
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
