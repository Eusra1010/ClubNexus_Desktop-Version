package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ViewRegistrationsController {

    @FXML private ComboBox<EventOption> eventSelector;
    @FXML private TextField searchField;

    @FXML private Label totalLabel;
    @FXML private Label paidLabel;
    @FXML private Label unpaidLabel;

    @FXML private TableView<Participant> table;
    @FXML private TableColumn<Participant, String> groupCol;
    @FXML private TableColumn<Participant, String> universityCol;
    @FXML private TableColumn<Participant, String> nameCol;
    @FXML private TableColumn<Participant, String> emailCol;
    @FXML private TableColumn<Participant, String> contactCol;
    // Only show basic participant info as before

    private final ObservableList<Participant> rows = FXCollections.observableArrayList();

    private static final Pattern BD_PHONE = Pattern.compile("^01\\d{9}$");

    @FXML
    public void initialize() {
        groupCol.setCellValueFactory(c -> c.getValue().groupNameProperty());
        universityCol.setCellValueFactory(c -> c.getValue().universityProperty());
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        emailCol.setCellValueFactory(c -> c.getValue().emailProperty());
        contactCol.setCellValueFactory(c -> c.getValue().contactProperty());
        // Removed Status and Actions columns to restore previous view

        table.setItems(rows);

        loadEvents();

        searchField.textProperty().addListener((obs, o, n) -> refresh());
        eventSelector.valueProperty().addListener((obs, o, n) -> refresh());

        // Default load
        refresh();
    }

    private void loadEvents() {
        List<EventOption> options = new ArrayList<>();
        options.add(new EventOption(null, "All Events"));
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT event_id, event_name FROM events ORDER BY event_date DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    options.add(new EventOption(rs.getString("event_id"), rs.getString("event_name")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        eventSelector.setItems(FXCollections.observableArrayList(options));
        eventSelector.getSelectionModel().selectFirst();
    }

    private void refresh() {
        rows.clear();
        String search = searchField.getText() == null ? "" : searchField.getText().trim();
        EventOption selected = eventSelector.getValue();
        String eventId = selected == null ? null : selected.id;

        // Build a UNION query so older individual registrations (without members) also show
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT mid, mname, memail, mcontact, paid, group_name, university, rtime FROM (\n");
        // Part 1: members joined with registrations
        sql.append("  SELECT rm.id AS mid, rm.full_name AS mname, rm.email AS memail, rm.contact AS mcontact, r.paid AS paid, r.group_name AS group_name, r.university AS university, r.registered_at AS rtime\n");
        sql.append("  FROM registration_members rm\n");
        sql.append("  JOIN registrations r ON rm.registration_id = r.id\n");
        sql.append("  WHERE 1=1");
        if (eventId != null) { sql.append(" AND r.event_id = ?"); params.add(eventId); }
        if (!search.isEmpty()) {
            sql.append(" AND (LOWER(r.group_name) LIKE ? OR LOWER(r.university) LIKE ? OR LOWER(rm.full_name) LIKE ? OR LOWER(rm.email) LIKE ?)");
            String like = "%" + search.toLowerCase() + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }
        sql.append("\n  UNION ALL\n");
        // Part 2: registrations without members
        sql.append("  SELECT r.id AS mid, r.full_name AS mname, r.email AS memail, r.contact AS mcontact, r.paid AS paid, r.group_name AS group_name, r.university AS university, r.registered_at AS rtime\n");
        sql.append("  FROM registrations r\n");
        sql.append("  WHERE NOT EXISTS (SELECT 1 FROM registration_members rm2 WHERE rm2.registration_id = r.id)");
        if (eventId != null) { sql.append(" AND r.event_id = ?"); params.add(eventId); }
        if (!search.isEmpty()) {
            sql.append(" AND (LOWER(r.group_name) LIKE ? OR LOWER(r.university) LIKE ? OR LOWER(r.full_name) LIKE ? OR LOWER(r.email) LIKE ?)");
            String like = "%" + search.toLowerCase() + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }
        sql.append("\n) t ORDER BY rtime DESC, mid ASC");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("mid");
                    String name = rs.getString("mname");
                    String email = rs.getString("memail");
                    String contact = rs.getString("mcontact");
                    boolean paid = rs.getInt("paid") == 1;
                    String groupName = rs.getString("group_name");
                    String university = rs.getString("university");
                    rows.add(new Participant(id, name, email, contact, paid, groupName, university));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateSummary();
    }

    private void updateSummary() {
        int total = rows.size();
        long paidCount = rows.stream().filter(Participant::isPaid).count();
        long unpaidCount = total - paidCount;
        totalLabel.setText("Total: " + total);
        paidLabel.setText("Paid: " + paidCount);
        unpaidLabel.setText("Unpaid: " + unpaidCount);
    }

    // Admin sees full contact for operational needs

    public static class Participant {
        final int id;
        private final javafx.beans.property.SimpleStringProperty groupName;
        private final javafx.beans.property.SimpleStringProperty university;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty email;
        private final javafx.beans.property.SimpleStringProperty contact;
        private boolean paid;

        public Participant(int id, String name, String email, String contact, boolean paid, String groupName, String university) {
            this.id = id;
            this.groupName = new javafx.beans.property.SimpleStringProperty(groupName == null ? "" : groupName);
            this.university = new javafx.beans.property.SimpleStringProperty(university == null ? "" : university);
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.email = new javafx.beans.property.SimpleStringProperty(email);
            this.contact = new javafx.beans.property.SimpleStringProperty(contact);
            this.paid = paid;
        }

        public javafx.beans.property.StringProperty groupNameProperty() { return groupName; }
        public javafx.beans.property.StringProperty universityProperty() { return university; }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty emailProperty() { return email; }
        public javafx.beans.property.StringProperty contactProperty() { return contact; }
        public boolean isPaid() { return paid; }
        public void setPaid(boolean paid) { this.paid = paid; }
    }

    public static class EventOption {
        final String id;
        final String name;
        public EventOption(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}
