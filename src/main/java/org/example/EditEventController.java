package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class EditEventController {

    @FXML private TextField nameField;
    @FXML private DatePicker datePicker;
    @FXML private TextField venueField;
    @FXML private TextField feeField;
    @FXML private DatePicker deadlinePicker;
    @FXML private ToggleButton registrationSwitch;
    @FXML private Label statusInfo;
    @FXML private Label nameError;
    @FXML private Label dateError;
    @FXML private Label venueError;
    @FXML private Label feeError;
    @FXML private Label deadlineError;

    private String eventId;
    private String originalStatus;

    public void setEventId(String eventId) {
        this.eventId = eventId;
        loadEvent();
    }

    private void loadEvent() {
        String sql = "SELECT event_name, event_date, venue, fees, registration_deadline, registration_open, status FROM events WHERE event_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                nameField.setText(rs.getString("event_name"));
                String ds = rs.getString("event_date");
                if (ds != null && !ds.isEmpty()) datePicker.setValue(LocalDate.parse(ds));
                venueField.setText(rs.getString("venue"));
                feeField.setText(rs.getString("fees"));
                String dl = rs.getString("registration_deadline");
                if (dl != null && !dl.isEmpty()) deadlinePicker.setValue(LocalDate.parse(dl));

                boolean open = rs.getInt("registration_open") == 1;
                registrationSwitch.setSelected(open);
                registrationSwitch.setText(open ? "Open" : "Closed");

                originalStatus = rs.getString("status");
                boolean cancelled = "CANCELLED".equalsIgnoreCase(originalStatus);
                statusInfo.setVisible(cancelled);
                statusInfo.setManaged(cancelled);
                if (cancelled) {
                    statusInfo.setText("Cancelled events cannot be edited");
                    disableAll();
                }

                registrationSwitch.selectedProperty().addListener((obs, oldV, newV) -> {
                    registrationSwitch.setText(newV ? "Open" : "Closed");
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to load event: " + e.getMessage());
        }
    }

    private void disableAll() {
        nameField.setDisable(true);
        datePicker.setDisable(true);
        venueField.setDisable(true);
        feeField.setDisable(true);
        deadlinePicker.setDisable(true);
        registrationSwitch.setDisable(true);
    }

    @FXML
    private void onSave() {
        if (eventId == null) return;

        String name = safe(nameField.getText());
        String venue = safe(venueField.getText());
        LocalDate date = datePicker.getValue();
        LocalDate deadline = deadlinePicker.getValue();
        String fees = safe(feeField.getText());
        int regOpen = registrationSwitch.isSelected() ? 1 : 0;

        clearErrors();
        boolean hasError = false;
        if (name.isEmpty()) { setError(nameError, "Name is required"); hasError = true; }
        if (venue.isEmpty()) { setError(venueError, "Venue is required"); hasError = true; }
        if (date == null) { setError(dateError, "Date is required"); hasError = true; }
        if (deadline == null) { setError(deadlineError, "Deadline is required"); hasError = true; }
        // Fees should be numeric and >= 0
        if (!fees.isEmpty()) {
            try {
                double f = Double.parseDouble(fees);
                if (f < 0) { setError(feeError, "Fees cannot be negative"); hasError = true; }
            } catch (NumberFormatException nfe) {
                setError(feeError, "Fees must be a number"); hasError = true; }
        }
        // Deadline not before date
        // Deadline must be strictly before the event date
        if (date != null && deadline != null && !deadline.isBefore(date)) {
            setError(deadlineError, "Deadline must be before the event date");
            hasError = true;
        }
        if (hasError) return;

        // Derive status: keep CANCELLED, else ACTIVE if reg open, else CLOSED
        String status = originalStatus;
        if (!"CANCELLED".equalsIgnoreCase(originalStatus)) {
            status = regOpen == 1 ? "ACTIVE" : "CLOSED";
        }

        String sql = "UPDATE events SET event_name=?, event_date=?, venue=?, fees=?, registration_deadline=?, registration_open=?, status=? WHERE event_id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, date.toString());
            ps.setString(3, venue);
            ps.setString(4, fees.isEmpty() ? "0" : fees);
            ps.setString(5, deadline.toString());
            ps.setInt(6, regOpen);
            ps.setString(7, status);
            ps.setString(8, eventId);
            ps.executeUpdate();

            showAlert("Changes saved successfully");
            // Close the window
            registrationSwitch.getScene().getWindow().hide();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to save: " + e.getMessage());
        }
    }

    private void clearErrors() {
        hideError(nameError);
        hideError(dateError);
        hideError(venueError);
        hideError(feeError);
        hideError(deadlineError);
    }
    private void setError(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }
    private void hideError(Label lbl) {
        if (lbl == null) return;
        lbl.setText("");
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setTitle("Edit Event");
        a.setContentText(msg);
        a.showAndWait();
    }
}
