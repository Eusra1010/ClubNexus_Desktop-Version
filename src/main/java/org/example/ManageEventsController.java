package org.example;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ManageEventsController {

    @FXML private TextField searchField;
    @FXML private VBox eventsList;
    @FXML private Hyperlink viewAllLink;

    private enum Filter { ALL, ACTIVE, PAST, CANCELLED }
    private Filter currentFilter = Filter.ALL;
    private boolean showingAll = false;

    @FXML
    public void initialize() {
        eventsList.setFillWidth(true);
        updateViewAllLabel();
        loadEvents();
    }

    @FXML
    private void onSearch() {
        loadEvents();
    }

    @FXML private void filterAll() { currentFilter = Filter.ALL; loadEvents(); }
    @FXML private void filterActive() { currentFilter = Filter.ACTIVE; loadEvents(); }
    @FXML private void filterPast() { currentFilter = Filter.PAST; loadEvents(); }
    @FXML private void filterCancelled() { currentFilter = Filter.CANCELLED; loadEvents(); }
    @FXML private void onToggleViewAll() { showingAll = !showingAll; updateViewAllLabel(); loadEvents(); }

    private void loadEvents() {
        String query = buildQuery();
        List<EventRow> rows = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            int paramIndex = 1;
            String term = searchField.getText() == null ? "" : searchField.getText().trim();

            if (!term.isEmpty()) {
                String like = "%" + term + "%";
                ps.setString(paramIndex++, like);
                ps.setString(paramIndex++, like);
            }

            if (currentFilter == Filter.PAST) {
                ps.setString(paramIndex++, LocalDate.now().toString());
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new EventRow(
                        rs.getString("event_id"),
                        rs.getString("event_name"),
                        rs.getString("event_date"),
                        rs.getString("venue"),
                        rs.getString("club_name"),
                        rs.getString("status"),
                        rs.getInt("registration_open"),
                        rs.getInt("registration_count")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        render(rows);
    }

    private String buildQuery() {
        StringBuilder sb = new StringBuilder(
                "SELECT event_id, event_name, event_date, venue, club_name, status, registration_open, registration_count FROM events"
        );

        List<String> where = new ArrayList<>();
        String term = searchField == null ? "" : searchField.getText();

        if (term != null && !term.trim().isEmpty()) {
            where.add("(event_name LIKE ? OR venue LIKE ?)");
        }

        switch (currentFilter) {
            case ACTIVE -> where.add("status = 'ACTIVE'");
            case CANCELLED -> where.add("status = 'CANCELLED'");
            case PAST -> where.add("event_date < ?");
            default -> {}
        }

        if (!where.isEmpty()) {
            sb.append(" WHERE ").append(String.join(" AND ", where));
        }

        sb.append(" ORDER BY event_date DESC");

        if (!showingAll) {
            sb.append(" LIMIT 5");
        }

        return sb.toString();
    }

    private void updateViewAllLabel() {
        if (viewAllLink != null) {
            viewAllLink.setText(showingAll ? "Show Less" : "View All");
        }
    }

    private void render(List<EventRow> rows) {
        eventsList.getChildren().clear();

        if (rows.isEmpty()) {
            Label empty = new Label("No events to show");
            empty.getStyleClass().add("empty-text");
            eventsList.getChildren().add(empty);
            return;
        }

        for (EventRow r : rows) {
            eventsList.getChildren().add(buildRow(r));
        }
    }

    private VBox buildRow(EventRow r) {
        VBox row = new VBox(8);
        row.getStyleClass().add("event-row");
        row.setFillWidth(true);

        // Header line: Title on left (wrap), Status on right
        HBox header = new HBox(8);
        Label title = new Label("Event: " + r.eventName);
        title.getStyleClass().add("event-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label status = new Label("Status: " + r.status);
        status.getStyleClass().add("event-status");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        header.getChildren().addAll(title, headerSpacer, status);

        // Meta info: wraps as needed
        FlowPane meta = new FlowPane(8, 6);
        meta.getStyleClass().add("event-meta-pane");
        Label date = new Label("Date: " + r.eventDate);
        date.getStyleClass().add("event-meta");
        Label venue = new Label("Venue: " + r.venue);
        venue.getStyleClass().add("event-meta");
        Label regs = new Label("Registrations: " + r.registrationCount);
        regs.getStyleClass().add("event-meta");
        meta.getChildren().addAll(date, venue, regs);
        // wrap to row width
        meta.prefWrapLengthProperty().bind(row.widthProperty().subtract(40));

        // Actions: wrap and align right
        FlowPane actions = new FlowPane(8, 8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button edit = new Button("Edit");
        edit.getStyleClass().addAll("action-btn", "edit-btn");
        edit.setDisable("CANCELLED".equalsIgnoreCase(r.status));
        edit.setOnAction(e -> onEdit(r.eventId, r.status));

        Button close = new Button("Close Registration");
        close.getStyleClass().addAll("action-btn", "close-btn");
        close.setDisable(r.registrationOpen == 0);
        close.setOnAction(e -> onCloseRegistration(r.eventId));

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().addAll("action-btn", "cancel-btn");
        cancel.setDisable("CANCELLED".equalsIgnoreCase(r.status));
        cancel.setOnAction(e -> onCancel(r.eventId));
        actions.getChildren().addAll(edit, close, cancel);
        actions.prefWrapLengthProperty().bind(row.widthProperty().subtract(40));

        row.getChildren().addAll(header, meta, actions);
        return row;
    }

    private void onEdit(String eventId, String status) {
        if ("CANCELLED".equalsIgnoreCase(status)) {
            return; // no action for cancelled events
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    org.example.ManageEventsController.class.getResource("/org/example/edit_event.fxml")
            );
            javafx.scene.Parent root = loader.load();
            EditEventController controller = loader.getController();
            controller.setEventId(eventId);

                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("Edit Event");
                javafx.scene.Scene scene = new javafx.scene.Scene(root, 520, 640);
                // Attach global stylesheet so content-area and form styles apply
                scene.getStylesheets().add(
                    org.example.Main.class.getResource("/org/example/style.css").toExternalForm()
                );
                stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setOnHidden(ev -> loadEvents());
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onCloseRegistration(String eventId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Close Registration");
        confirm.setHeaderText(null);
        confirm.setContentText("Stop new registrations for this event?");
        confirm.getButtonTypes().setAll(ButtonType.NO, ButtonType.YES);
        ButtonType result = confirm.showAndWait().orElse(ButtonType.NO);
        if (result != ButtonType.YES) return;

        update("UPDATE events SET registration_open = 0, status = 'CLOSED' WHERE event_id = ?", eventId);
    }

    private void onCancel(String eventId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Event");
        confirm.setHeaderText(null);
        confirm.setContentText("This will cancel the event and close permanently.");
        ButtonType back = new ButtonType("Back", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType cancelEvent = new ButtonType("Cancel Event", ButtonBar.ButtonData.OK_DONE);
        confirm.getButtonTypes().setAll(back, cancelEvent);
        ButtonType result = confirm.showAndWait().orElse(back);
        if (result != cancelEvent) return;

        update("UPDATE events SET status = 'CANCELLED', registration_open = 0 WHERE event_id = ?", eventId);
    }

    private void update(String sql, String eventId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.executeUpdate();
            loadEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class EventRow {
        final String eventId, eventName, eventDate, venue, clubName, status;
        final int registrationOpen;
        final int registrationCount;

        EventRow(String eventId, String eventName, String eventDate,
                 String venue, String clubName, String status, int registrationOpen, int registrationCount) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.eventDate = eventDate;
            this.venue = venue;
            this.clubName = clubName;
            this.status = status;
            this.registrationOpen = registrationOpen;
            this.registrationCount = registrationCount;
        }
    }
}
