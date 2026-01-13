package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
// Removed Swing/ZXing imports as ticket download is no longer needed

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentMyRegistrationsController {

    @FXML private TableView<Row> table;
    @FXML private TableColumn<Row, String> eventCol;
    @FXML private TableColumn<Row, String> dateCol;
    @FXML private TableColumn<Row, String> statusCol;
    @FXML private TableColumn<Row, String> paidCol;
    @FXML private TableColumn<Row, Void> actionsCol;

    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set constrained resize policy programmatically to avoid FXML coercion issues
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        eventCol.setCellValueFactory(c -> c.getValue().eventNameProperty());
        dateCol.setCellValueFactory(c -> c.getValue().eventDateProperty());
        statusCol.setCellValueFactory(c -> c.getValue().statusProperty());
        paidCol.setCellValueFactory(
                c -> new javafx.beans.property.SimpleStringProperty(
                        c.getValue().paid ? "PAID" : "UNPAID"
                )
        );

        actionsCol.setCellFactory(tc -> new TableCell<>() {

            private final Button cancelBtn = new Button("Cancel");
            private final Button viewBtn   = new Button("Open");
            private final Button payBtn    = new Button("Pay");

            private final HBox box = new HBox(8, viewBtn, payBtn, cancelBtn);

            {
                box.getStyleClass().add("row-actions");
                cancelBtn.getStyleClass().addAll("action-btn", "action-cancel");
                viewBtn.getStyleClass().addAll("action-btn", "action-open");
                payBtn.getStyleClass().addAll("action-btn", "action-pay");

                viewBtn.setTooltip(new Tooltip("Open event details"));
                payBtn.setTooltip(new Tooltip("Show billing info and mark as paid"));
                cancelBtn.setTooltip(new Tooltip("Cancel this registration"));

                cancelBtn.setOnAction(e -> cancel(getTableView().getItems().get(getIndex())));
                viewBtn.setOnAction(e -> openEvent(getTableView().getItems().get(getIndex())));
                payBtn.setOnAction(e -> pay(getTableView().getItems().get(getIndex())));
                // no ticket action
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }
                int idx = getIndex();
                var items = getTableView().getItems();
                if (idx < 0 || idx >= items.size()) {
                    setGraphic(null);
                    return;
                }
                Row r = items.get(idx);
                String st = r.getStatus(); // ✅ FIX

                cancelBtn.setDisable(
                        "CANCELLED".equalsIgnoreCase(st) ||
                                "CLOSED".equalsIgnoreCase(st)
                );

                payBtn.setDisable(
                        r.paid ||
                                "CANCELLED".equalsIgnoreCase(st) ||
                                "CLOSED".equalsIgnoreCase(st)
                );

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(box);
            }
        });

        table.setItems(rows);
        refresh();
    }

    @FXML
    private void onBack() throws Exception {
        Main.switchScene("student_dashboard.fxml");
    }

    private void refresh() {
        rows.clear();
        String roll = StudentSession.getCurrentRoll();
        if (roll == null || roll.isBlank()) return;

        String sql =
                "SELECT r.id, r.event_id, e.event_name, e.event_date, e.status, r.paid, r.ticket_code " +
                        "FROM registrations r " +
                        "JOIN events e ON e.event_id = r.event_id " +
                        "WHERE r.roll = ? " +
                        "ORDER BY r.registered_at DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roll);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Row r = new Row(
                            rs.getInt("id"),
                            rs.getString("event_id"),
                            rs.getString("event_name"),
                            rs.getString("event_date"),
                            rs.getString("status"),
                            rs.getInt("paid") == 1
                    );
                    r.ticketCode = rs.getString("ticket_code");
                    rows.add(r);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancel(Row r) {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Cancel your registration for this event?",
                ButtonType.NO, ButtonType.YES
        );
        confirm.setHeaderText(null);

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        try (Connection conn = Database.getConnection()) {

            try (PreparedStatement ps =
                         conn.prepareStatement("DELETE FROM registrations WHERE id = ?")) {
                ps.setInt(1, r.id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps =
                         conn.prepareStatement(
                                 "UPDATE events SET registration_count = " +
                                         "CASE WHEN registration_count > 0 THEN registration_count - 1 ELSE 0 END " +
                                         "WHERE event_id = ?")) {
                ps.setString(1, r.eventId);
                ps.executeUpdate();
            }

            refresh();
            new Alert(Alert.AlertType.INFORMATION, "Registration cancelled.", ButtonType.OK).showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to cancel.", ButtonType.OK).showAndWait();
        }
    }

    private void openEvent(Row r) {
        try {
            Main.switchScene("view_events.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pay(Row r) {
        try (Connection conn = Database.getConnection()) {

            String code = (r.ticketCode == null || r.ticketCode.isBlank())
                    ? java.util.UUID.randomUUID().toString()
                    : r.ticketCode;

            // Mark as paid in DB without a real payment flow
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE registrations SET paid = 1, paid_at = datetime('now'), ticket_code = ? WHERE id = ?")) {
                ps.setString(1, code);
                ps.setInt(2, r.id);
                ps.executeUpdate();
            }

            r.paid = true;
            r.ticketCode = code;
            table.refresh();

            // Optional: Add a notification entry students can view later
            try (PreparedStatement psAnn = conn.prepareStatement(
                    "INSERT INTO announcements (title, type, body, event_id, is_system_generated, published) " +
                            "VALUES (?,?,?,?,1,1)")) {
                psAnn.setString(1, "Billing Information Sent");
                psAnn.setString(2, "BILLING_INFO");
                psAnn.setString(3, "Billing information is sent to you via mail for '" + r.eventName.get() + "'.");
                psAnn.setString(4, r.eventId);
                psAnn.executeUpdate();
            }

            // Show the message and offer to open Notifications
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Payment");
            info.setHeaderText(null);
            info.setContentText("Billing information is sent to you via mail.");
            ButtonType open = new ButtonType("Open", ButtonBar.ButtonData.OK_DONE);
            ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            info.getButtonTypes().setAll(open, close);
            ButtonType res = info.showAndWait().orElse(close);
            if (res == open) {
                try { Main.switchScene("student_notifications.fxml"); } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            e.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Payment Error");
            err.setHeaderText(null);
            err.setContentText("Failed to mark as paid: " + (e.getMessage() == null ? "Unknown error" : e.getMessage()));
            err.getButtonTypes().setAll(ButtonType.OK);
            err.showAndWait();
        }
    }

    // View ticket feature removed per request

    /* ======================= ROW MODEL ======================= */

    public static class Row {
        final int id;
        final String eventId;
        boolean paid;
        String ticketCode;

        private final javafx.beans.property.SimpleStringProperty eventName;
        private final javafx.beans.property.SimpleStringProperty eventDate;
        private final javafx.beans.property.SimpleStringProperty status;

        public Row(int id, String eventId, String eventName, String eventDate, String status, boolean paid) {
            this.id = id;
            this.eventId = eventId;
            this.paid = paid;
            this.eventName = new javafx.beans.property.SimpleStringProperty(eventName);
            this.eventDate = new javafx.beans.property.SimpleStringProperty(eventDate);
            this.status = new javafx.beans.property.SimpleStringProperty(status);
        }

        public javafx.beans.property.StringProperty eventNameProperty() { return eventName; }
        public javafx.beans.property.StringProperty eventDateProperty() { return eventDate; }
        public javafx.beans.property.StringProperty statusProperty() { return status; }

        public String getStatus() { return status.get(); } // ✅ FIX
    }
}
