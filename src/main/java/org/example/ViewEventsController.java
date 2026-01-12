package org.example;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ViewEventsController {

    @FXML private Button toggleViewBtn;
    @FXML private TextField searchField;
    @FXML private ScrollPane listScroll;
    @FXML private VBox eventsList;

    @FXML private VBox registrationPane;
    @FXML private ScrollPane regScroll;
    @FXML private Label eventTitle;
    @FXML private Label eventClub;
    @FXML private Label eventDate;
    @FXML private Label eventVenue;
    @FXML private Label eventFees;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField contactField;
    @FXML private ComboBox<String> batchCombo;
    @FXML private TextField departmentField;
    @FXML private Button registerBtn;

    private boolean viewAll = false;
    private EventRow selected;

    private static final String[] PALETTE = new String[]{
            "#E74C3C","#3B82F6","#8B5CF6","#F59E0B","#10B981","#EC4899"
    };

    @FXML
    public void initialize() {
        loadEvents();
        registerBtn.setDisable(true);
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> loadEvents());
        }
        if (batchCombo != null) {
            batchCombo.setItems(javafx.collections.FXCollections.observableArrayList("2k20","2k21","2k22","2k23","2k24"));
        }
        // Start with panel visible but disabled, or hide until selection if preferred
        registrationPane.setVisible(true);
        registrationPane.setManaged(true);
    }

    @FXML
    private void toggleView() {
        viewAll = !viewAll;
        toggleViewBtn.setText(viewAll ? "Back" : "View All Events");
        registrationPane.setVisible(!viewAll);
        registrationPane.setManaged(!viewAll);
        if (regScroll != null) {
            regScroll.setVisible(!viewAll);
            regScroll.setManaged(!viewAll);
        }
    }

    private void loadEvents() {
        List<EventRow> events = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = buildQuery(conn)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(new EventRow(
                            rs.getString("event_id"),
                            rs.getString("event_name"),
                            rs.getString("event_date"),
                            rs.getString("venue"),
                            rs.getString("club_name"),
                            rs.getString("fees"),
                            rs.getInt("registration_open"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        eventsList.getChildren().clear();
        for (int i = 0; i < events.size(); i++) {
            Pane card = buildCompactCard(events.get(i), i);
            eventsList.getChildren().add(card);
        }
    }

    private PreparedStatement buildQuery(Connection conn) throws Exception {
        String search = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String base = "SELECT event_id, event_name, event_date, venue, club_name, fees, registration_open, status FROM events";
        String order = " ORDER BY event_date ASC";
        if (search.isEmpty()) {
            return conn.prepareStatement(base + order);
        } else {
            String sql = base + " WHERE LOWER(event_name) LIKE ? OR LOWER(club_name) LIKE ? OR LOWER(venue) LIKE ?" + order;
            PreparedStatement ps = conn.prepareStatement(sql);
            String like = "%" + search + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            return ps;
        }
    }

    private Pane buildCompactCard(EventRow e, int idx) {
        String color = PALETTE[idx % PALETTE.length];

        HBox card = new HBox(10);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: " + color + 
            "; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 10, 0.3, 0, 4);");
        card.setOnMouseClicked((MouseEvent m) -> selectEvent(e));

        // Accent bar
        Region accent = new Region();
        accent.setPrefWidth(6);
        accent.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");

        VBox details = new VBox(4);
        Label title = new Label(e.name);
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label line1 = new Label("Organized by: " + e.club);
        line1.setStyle("-fx-text-fill: rgba(255,255,255,0.95); -fx-font-size: 11px;");
        Label line2 = new Label("Date: " + e.date + "  •  Venue: " + e.venue);
        line2.setStyle("-fx-text-fill: rgba(255,255,255,0.95); -fx-font-size: 11px;");
        Label line3 = new Label("Fees: " + (e.fees == null || e.fees.isBlank() ? "Free" : e.fees));
        line3.setStyle("-fx-text-fill: rgba(255,255,255,0.95); -fx-font-size: 11px;");
        details.getChildren().addAll(title, line1, line2, line3);

        Button regBtn = new Button("Register");
        regBtn.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-text-fill: #083c3b; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 10;");
        regBtn.setDisable(e.registrationOpen == 0 || "CANCELLED".equalsIgnoreCase(e.status));
        regBtn.setOnAction(ev -> onCardRegister(e));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(accent, details, spacer, regBtn);
        card.setPrefHeight(80);
        return card;
    }

    @FXML
    private void goBack() throws Exception {
        Main.switchScene("student_dashboard.fxml");
    }

    @FXML
    private void onSearch() {
        loadEvents();
    }

    private void selectEvent(EventRow e) {
        selected = e;
        eventTitle.setText(e.name);
        eventClub.setText("Organized by: " + e.club);
        eventDate.setText("Date: " + e.date);
        eventVenue.setText("Venue: " + e.venue);
        eventFees.setText("Fees: " + (e.fees == null || e.fees.isBlank() ? "Free" : e.fees));
        registerBtn.setDisable(e.registrationOpen == 0 || "CANCELLED".equalsIgnoreCase(e.status));
    }

    private void onCardRegister(EventRow e) {
        // Populate selection and reveal side panel
        selectEvent(e);
        registrationPane.setVisible(true);
        registrationPane.setManaged(true);
        if (regScroll != null) {
            regScroll.setVisible(true);
            regScroll.setManaged(true);
        }
        // Ensure the register button reflects event status
        registerBtn.setDisable(e.registrationOpen == 0 || "CANCELLED".equalsIgnoreCase(e.status));
    }

    @FXML
    private void onRegisterFromPanel() {
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Select an event first", ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
            return;
        }
        // Validate and insert registration inline
        String nm = safe(nameField.getText());
        String em = safe(emailField.getText());
        String ct = safe(contactField.getText());
        String bt = batchCombo == null ? null : batchCombo.getValue();
        String dept = safe(departmentField == null ? null : departmentField.getText());

        java.util.List<String> errs = new java.util.ArrayList<>();
        if (nm.isEmpty()) errs.add("Full Name is required");
        String digits = ct.replaceAll("[^0-9]", "");
        if (!digits.matches("01\\d{9}")) errs.add("Contact must be 11-digit BD number starting 01");
        if (em.isEmpty() || !em.contains("@")) errs.add("Valid Email is required");
        if (bt == null || bt.isEmpty()) errs.add("Batch is required");
        if (dept.isEmpty()) errs.add("Department is required");

        if (!errs.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.ERROR, String.join("\n", errs), ButtonType.OK);
            a.setHeaderText("Invalid registration");
            a.showAndWait();
            return;
        }

        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO registrations (event_id, full_name, email, contact, paid, batch, department) VALUES (?, ?, ?, ?, 0, ?, ?)")) {
                ps.setString(1, selected.id);
                ps.setString(2, nm);
                ps.setString(3, em);
                ps.setString(4, ct);
                ps.setString(5, bt);
                ps.setString(6, dept);
                ps.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE events SET registration_count = COALESCE(registration_count, 0) + 1 WHERE event_id = ?")) {
                ps2.setString(1, selected.id);
                ps2.executeUpdate();
            }
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Registered successfully!", ButtonType.OK);
            ok.setHeaderText(null);
            ok.showAndWait();
            // Clear form for next registration
            nameField.clear();
            emailField.clear();
            contactField.clear();
            if (batchCombo != null) batchCombo.getSelectionModel().clearSelection();
            if (departmentField != null) departmentField.clear();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to register", ButtonType.OK);
            err.showAndWait();
        }
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    static class EventRow {
        final String id;
        final String name;
        final String date;
        final String venue;
        final String club;
        final String fees;
        final int registrationOpen;
        final String status;
        EventRow(String id, String name, String date, String venue, String club, String fees, int registrationOpen, String status) {
            this.id = id; this.name = name; this.date = date; this.venue = venue; this.club = club; this.fees = fees; this.registrationOpen = registrationOpen; this.status = status;
        }
    }
}
