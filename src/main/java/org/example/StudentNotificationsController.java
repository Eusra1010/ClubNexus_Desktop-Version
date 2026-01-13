package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class StudentNotificationsController {

    @FXML private ListView<NotificationItem> list;
    @FXML private ToggleButton allToggle;
    @FXML private ToggleButton unreadToggle;

    private final ObservableList<NotificationItem> items = FXCollections.observableArrayList();

    private static final Map<String, String> EMOJI = new HashMap<>();
    static {
        EMOJI.put("EVENT_UPDATE", "🛠");
        EMOJI.put("REGISTRATION_CONFIRMED", "✅");
        EMOJI.put("EMAIL_SENT", "✉️");
        EMOJI.put("PAYMENT_INFORMATION_SENT", "💳");
        EMOJI.put("EVENT_REMINDER", "⏰");
        EMOJI.put("EVENT_CANCELLED", "❌");
    }

    @FXML
    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        allToggle.setToggleGroup(group);
        unreadToggle.setToggleGroup(group);

        list.setItems(items);
        list.setCellFactory(v -> new NotificationCell());

        // allow cell to find controller for markRead
        list.sceneProperty().addListener((o, old, sc) -> {
            if (sc != null) sc.setUserData(this);
        });

        allToggle.setOnAction(e -> refresh());
        unreadToggle.setOnAction(e -> refresh());

        refresh();
    }

    @FXML
    private void onBack() throws Exception {
        Main.switchScene("student_dashboard.fxml");
    }

    @FXML
    private void onMarkAllRead() {
        String sid = currentStudentId();
        if (sid == null) return;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO student_notification_status (announcement_id, student_id, read_at) " +
                             "SELECT a.id, ?, datetime('now') FROM announcements a " +
                             "WHERE a.published = 1 AND (a.start_at IS NULL OR a.start_at <= datetime('now')) " +
                             "AND (a.end_at IS NULL OR a.end_at >= datetime('now')) " +
                             "ON CONFLICT(announcement_id, student_id) DO UPDATE SET read_at = excluded.read_at"
             )) {
            ps.setString(1, sid);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        refresh();
    }

    private void refresh() {
        items.clear();
        boolean onlyUnread = unreadToggle.isSelected();
        String sid = currentStudentId();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.id, a.title, a.type, a.body, a.is_system_generated, a.created_at, ");
        sql.append("CASE WHEN s.read_at IS NULL THEN 0 ELSE 1 END AS is_read ");
        sql.append("FROM announcements a ");
        sql.append("LEFT JOIN student_notification_status s ON s.announcement_id = a.id AND s.student_id = ? ");
        sql.append("WHERE a.published = 1 ");
        sql.append("AND (a.start_at IS NULL OR a.start_at <= datetime('now')) ");
        sql.append("AND (a.end_at IS NULL OR a.end_at >= datetime('now')) ");
        sql.append("AND (a.is_system_generated = 0 OR a.event_id IS NULL OR EXISTS ( ");
        sql.append("  SELECT 1 FROM registrations r WHERE r.event_id = a.event_id AND r.roll = ? ");
        sql.append(")) ");
        if (onlyUnread) {
            sql.append("AND s.read_at IS NULL ");
        }
        sql.append("ORDER BY a.created_at DESC");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            String roll = sid == null ? "guest" : sid;
            ps.setString(1, roll);
            ps.setString(2, roll);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotificationItem it = new NotificationItem();
                    it.id = rs.getInt("id");
                    it.title = rs.getString("title");
                    it.type = rs.getString("type");
                    it.body = rs.getString("body");
                    it.isSystem = rs.getInt("is_system_generated") == 1;
                    it.isRead = rs.getInt("is_read") == 1;
                    items.add(it);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markRead(NotificationItem it) {
        String sid = currentStudentId();
        if (sid == null) return;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO student_notification_status (announcement_id, student_id, read_at) " +
                             "VALUES (?,?,datetime('now')) " +
                             "ON CONFLICT(announcement_id, student_id) DO UPDATE SET read_at = excluded.read_at"
             )) {
            ps.setInt(1, it.id);
            ps.setString(2, sid);
            ps.executeUpdate();
            it.isRead = true;
            list.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ FIX: this method was missing
    private String currentStudentId() {
        return StudentSession.getCurrentRoll();
    }

    private static class NotificationCell extends ListCell<NotificationItem> {
        private final Label emoji = new Label();
        private final Label title = new Label();
        private final Label body = new Label();
        private final Button markBtn = new Button("Mark Read");
        private final HBox box = new HBox(12, emoji, new VBox(title, body), markBtn);

        public NotificationCell() {
            markBtn.getStyleClass().add("header-btn");
        }

        @Override
        protected void updateItem(NotificationItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            emoji.setText(EMOJI.getOrDefault(item.type, "📢"));
            title.setText(item.title == null ? item.type : item.title);
            body.setText(item.body == null ? "" : item.body);
            title.setStyle(item.isRead ? "-fx-font-weight: normal;" : "-fx-font-weight: bold;");
            markBtn.setDisable(item.isRead);
            markBtn.setOnAction(ev ->
                    ((StudentNotificationsController)
                            getListView().getScene().getUserData()).markRead(item)
            );
            setGraphic(box);
        }
    }

    public static class NotificationItem {
        int id;
        String title;
        String type;
        String body;
        boolean isSystem;
        boolean isRead;
    }
}
