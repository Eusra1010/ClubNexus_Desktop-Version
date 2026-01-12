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
    @FXML private TableColumn<Participant, String> nameCol;
    @FXML private TableColumn<Participant, String> emailCol;
    @FXML private TableColumn<Participant, String> contactCol;
    // Only show basic participant info as before

    private final ObservableList<Participant> rows = FXCollections.observableArrayList();

    private static final Pattern BD_PHONE = Pattern.compile("^01\\d{9}$");

    @FXML
    public void initialize() {
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

        StringBuilder sql = new StringBuilder(
            "SELECT id, full_name, email, contact, paid FROM registrations WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (eventId != null) {
            sql.append(" AND event_id = ?");
            params.add(eventId);
        }
        if (!search.isEmpty()) {
            sql.append(" AND (LOWER(full_name) LIKE ? OR LOWER(email) LIKE ?)");
            String like = "%" + search.toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY registered_at DESC");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("full_name");
                    String email = rs.getString("email");
                    String contact = rs.getString("contact");
                    boolean paid = rs.getInt("paid") == 1;
                    rows.add(new Participant(id, name, email, contact, paid));
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
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty email;
        private final javafx.beans.property.SimpleStringProperty contact;
        private boolean paid;

        public Participant(int id, String name, String email, String contact, boolean paid) {
            this.id = id;
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.email = new javafx.beans.property.SimpleStringProperty(email);
            this.contact = new javafx.beans.property.SimpleStringProperty(contact);
            this.paid = paid;
        }

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
