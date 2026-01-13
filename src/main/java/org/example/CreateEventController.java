package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

public class CreateEventController {

    @FXML private TextField eventNameField;
    @FXML private DatePicker eventDatePicker;
    @FXML private TextField venueField;
    @FXML private TextField clubField;
    @FXML private TextField feeField;
    @FXML private DatePicker deadlinePicker;
    @FXML private CheckBox groupEventCheck;
    @FXML private TextField minGroupField;
    @FXML private TextField maxGroupField;

    @FXML
    public void initialize() {

        String club = AdminSession.getClubName();
        clubField.setText(club);
        clubField.setEditable(false);
        clubField.setFocusTraversable(false);
    }

    @FXML
    private void createEvent() {

        String eventName = eventNameField.getText().trim();
        String venue = venueField.getText().trim();
        String clubName = clubField.getText();
        String fees = feeField.getText().trim();

        if (fees.isEmpty()) fees = "0";

        if (eventName.isEmpty()
                || venue.isEmpty()
                || eventDatePicker.getValue() == null
                || deadlinePicker.getValue() == null) {
            showAlert("Fill all required fields");
            return;
        }

        LocalDate eventDate = eventDatePicker.getValue();
        String deadline = deadlinePicker.getValue().toString();

        checkDateAndSave(eventDate, eventName, venue, clubName, fees, deadline);
    }

    private void checkDateAndSave(LocalDate date,
                                  String eventName,
                                  String venue,
                                  String clubName,
                                  String fees,
                                  String deadline) {

        if (isDateBooked(date)) {

            LocalDate suggested = findNextAvailableWeekend(date);

            if (suggested != null) {
                showAlert(
                        "Date booked.\nNearest available date: "
                                + suggested.getDayOfWeek() + ", " + suggested
                );
            } else {
                showAlert("Date booked. No available weekend found.");
            }
            return;
        }

        saveEvent(date.toString(), eventName, venue, clubName, fees, deadline);
    }

    private boolean isDateBooked(LocalDate date) {

        String sql = "SELECT 1 FROM events WHERE event_date = ? LIMIT 1";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, date.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private LocalDate findNextAvailableWeekend(LocalDate from) {

        LocalDate date = from.plusDays(1);

        for (int i = 0; i < 60; i++) {
            DayOfWeek day = date.getDayOfWeek();
            if ((day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
                    && !isDateBooked(date)) {
                return date;
            }
            date = date.plusDays(1);
        }
        return null;
    }

    private void saveEvent(String date,
                           String eventName,
                           String venue,
                           String clubName,
                           String fees,
                           String deadline) {

        String sql = """
                INSERT INTO events (
                    event_id,
                    event_name,
                    event_date,
                    venue,
                    club_name,
                    fees,
                    registration_open,
                    registration_count,
                    status,
                    registration_deadline,
                    is_group,
                    min_group_size,
                    max_group_size
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?, ?, ?)
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, eventName);
            ps.setString(3, date);
            ps.setString(4, venue);
            ps.setString(5, clubName);
            ps.setString(6, fees);
            ps.setInt(7, 1);
            ps.setInt(8, 0);
            ps.setString(9, "ACTIVE");
            ps.setString(10, deadline);

            // Group event flags
            boolean isGroup = groupEventCheck != null && groupEventCheck.isSelected();
            Integer minGroup = parseIntSafe(minGroupField == null ? null : minGroupField.getText());
            Integer maxGroup = parseIntSafe(maxGroupField == null ? null : maxGroupField.getText());
            if (!isGroup) { minGroup = null; maxGroup = null; }
            ps.setInt(11, isGroup ? 1 : 0);
            if (minGroup == null) ps.setNull(12, java.sql.Types.INTEGER); else ps.setInt(12, minGroup);
            if (maxGroup == null) ps.setNull(13, java.sql.Types.INTEGER); else ps.setInt(13, maxGroup);

            ps.executeUpdate();
            showAlert("Event created successfully");
            clearForm();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Create Event");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void clearForm() {
        eventNameField.clear();
        venueField.clear();
        feeField.clear();
        eventDatePicker.setValue(null);
        deadlinePicker.setValue(null);
        if (groupEventCheck != null) groupEventCheck.setSelected(false);
        if (minGroupField != null) minGroupField.clear();
        if (maxGroupField != null) maxGroupField.clear();
    }

    private Integer parseIntSafe(String s) {
        try {
            if (s == null || s.trim().isEmpty()) return null;
            return Integer.parseInt(s.trim());
        } catch (Exception e) { return null; }
    }
}
