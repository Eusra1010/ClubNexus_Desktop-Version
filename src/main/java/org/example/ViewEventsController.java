package org.example;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ViewEventsController {

    @FXML private Button toggleViewBtn;
    @FXML private TextField searchField;
    @FXML private ScrollPane listScroll; // (warning only if unused in code; OK to keep if used in FXML)
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
    @FXML private Label universityLabel;

    @FXML private VBox groupSection;
    @FXML private TextField groupNameField;
    @FXML private Spinner<Integer> groupSizeSpinner;
    @FXML private VBox groupMembersBox;
    @FXML private Label groupConstraintLabel;

    private boolean viewAll = false;
    private EventRow selected;

    private static final String[] PALETTE = new String[]{
            "#E74C3C", "#3B82F6", "#8B5CF6", "#F59E0B", "#10B981", "#EC4899"
    };

    @FXML
    public void initialize() {
        loadEvents();

        if (registerBtn != null) registerBtn.setDisable(true);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> loadEvents());
        }

        if (batchCombo != null) {
            batchCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                    "2k20", "2k21", "2k22", "2k23", "2k24"
            ));
        }

        // Show panel (or you can hide until selection)
        if (registrationPane != null) {
            registrationPane.setVisible(true);
            registrationPane.setManaged(true);
        }
        if (regScroll != null) {
            regScroll.setVisible(true);
            regScroll.setManaged(true);
        }

        // IMPORTANT: Add spinner listener ONCE
        if (groupSizeSpinner != null) {
            groupSizeSpinner.valueProperty().addListener((obs, o, nv) -> renderMemberInputs());
        }

        preloadUniversity();
    }

    @FXML
    private void toggleView() {
        viewAll = !viewAll;

        if (toggleViewBtn != null) {
            toggleViewBtn.setText(viewAll ? "Back" : "View All Events");
        }

        boolean showReg = !viewAll;

        if (registrationPane != null) {
            registrationPane.setVisible(showReg);
            registrationPane.setManaged(showReg);
        }
        if (regScroll != null) {
            regScroll.setVisible(showReg);
            regScroll.setManaged(showReg);
        }
    }

    private void loadEvents() {
        List<EventRow> events = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = buildQuery(conn);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                events.add(new EventRow(
                        rs.getString("event_id"),
                        rs.getString("event_name"),
                        rs.getString("event_date"),
                        rs.getString("venue"),
                        rs.getString("club_name"),
                        rs.getString("fees"),
                        rs.getInt("registration_open"),
                        rs.getString("status"),
                        rs.getInt("is_group"),
                        rs.getInt("min_group_size"),
                        rs.getInt("max_group_size")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (eventsList != null) {
            eventsList.getChildren().clear();
            for (int i = 0; i < events.size(); i++) {
                Pane card = buildCompactCard(events.get(i), i);
                eventsList.getChildren().add(card);
            }
        }
    }

    private PreparedStatement buildQuery(Connection conn) throws Exception {
        String search = (searchField == null || searchField.getText() == null)
                ? ""
                : searchField.getText().trim().toLowerCase();

        String base = "SELECT event_id, event_name, event_date, venue, club_name, fees, " +
                "registration_open, status, is_group, min_group_size, max_group_size FROM events";
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

        Region accent = new Region();
        accent.setPrefWidth(6);
        accent.setStyle("-fx-background-color: rgba(255,255,255,0.35); -fx-background-radius: 4;");

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
        regBtn.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-text-fill: #083c3b; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 10;");

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

        if (eventTitle != null) eventTitle.setText(e.name);
        if (eventClub != null) eventClub.setText("Organized by: " + e.club);
        if (eventDate != null) eventDate.setText("Date: " + e.date);
        if (eventVenue != null) eventVenue.setText("Venue: " + e.venue);
        if (eventFees != null) eventFees.setText("Fees: " + (e.fees == null || e.fees.isBlank() ? "Free" : e.fees));

        if (registerBtn != null) {
            registerBtn.setDisable(e.registrationOpen == 0 || "CANCELLED".equalsIgnoreCase(e.status));
        }

        boolean isGroup = e.isGroup == 1;

        if (groupSection != null) {
            groupSection.setVisible(isGroup);
            groupSection.setManaged(isGroup);
        }

        if (isGroup) {
            int min = e.minGroup > 0 ? e.minGroup : 1;
            int max = e.maxGroup >= min ? e.maxGroup : Math.max(min, 1);

            if (groupConstraintLabel != null) {
                groupConstraintLabel.setText("Members: min " + min + " / max " + max);
            }

            if (groupSizeSpinner != null) {
                groupSizeSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, min)
                );
            }
            renderMemberInputs();
        } else {
            if (groupMembersBox != null) groupMembersBox.getChildren().clear();
            if (groupNameField != null) groupNameField.clear();
        }
    }

    private void onCardRegister(EventRow e) {
        selectEvent(e);

        if (registrationPane != null) {
            registrationPane.setVisible(true);
            registrationPane.setManaged(true);
        }
        if (regScroll != null) {
            regScroll.setVisible(true);
            regScroll.setManaged(true);
        }

        if (registerBtn != null) {
            registerBtn.setDisable(e.registrationOpen == 0 || "CANCELLED".equalsIgnoreCase(e.status));
        }
    }

    @FXML
    private void onRegisterFromPanel() {
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Select an event first", ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
            return;
        }

        String nm = safe(nameField == null ? null : nameField.getText());
        String em = safe(emailField == null ? null : emailField.getText());
        String ct = safe(contactField == null ? null : contactField.getText());
        String bt = batchCombo == null ? null : batchCombo.getValue();
        String dept = safe(departmentField == null ? null : departmentField.getText());
        String uni = safe(universityLabel == null ? null : universityLabel.getText());

        boolean isGroup = selected.isGroup == 1;
        String grpName = isGroup ? safe(groupNameField == null ? null : groupNameField.getText()) : null;
        int grpSize = (isGroup && groupSizeSpinner != null && groupSizeSpinner.getValue() != null)
                ? groupSizeSpinner.getValue()
                : 1;

        List<String> errs = new ArrayList<>();
        if (nm.isEmpty()) errs.add("Full Name is required");

        String digits = ct.replaceAll("[^0-9]", "");
        if (!digits.matches("01\\d{9}")) errs.add("Contact must be 11-digit BD number starting 01");

        if (em.isEmpty() || !em.contains("@")) errs.add("Valid Email is required");
        if (bt == null || bt.isEmpty()) errs.add("Batch is required");
        if (dept.isEmpty()) errs.add("Department is required");

        if (isGroup) {
            if (grpName == null || grpName.isEmpty()) errs.add("Group Name is required");

            int min = selected.minGroup > 0 ? selected.minGroup : 1;
            int max = selected.maxGroup >= min ? selected.maxGroup : Math.max(min, 1);
            if (grpSize < min || grpSize > max) errs.add("Group size must be between " + min + " and " + max);

            for (int i = 1; i < grpSize; i++) {
                MemberInputs mi = findMemberInputs(i);
                if (mi == null) {
                    errs.add("Missing member #" + (i + 1) + " inputs");
                    continue;
                }

                String mn = safe(mi.name.getText());
                String me = safe(mi.email.getText());
                String mc = safe(mi.contact.getText());
                String md = safe(mi.department.getText());

                if (mn.isEmpty()) errs.add("Member #" + (i + 1) + ": Name required");
                String mDigits = mc.replaceAll("[^0-9]", "");
                if (!mDigits.matches("01\\d{9}")) errs.add("Member #" + (i + 1) + ": Contact must be 11-digit starting 01");
                if (me.isEmpty() || !me.contains("@")) errs.add("Member #" + (i + 1) + ": Valid Email required");
                if (md.isEmpty()) errs.add("Member #" + (i + 1) + ": Department required");
            }
        }

        if (!errs.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.ERROR, String.join("\n", errs), ButtonType.OK);
            a.setHeaderText("Invalid registration");
            a.showAndWait();
            return;
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            int regId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO registrations (event_id, full_name, roll, email, contact, paid, batch, department, university, group_name) " +
                            "VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, selected.id);
                ps.setString(2, nm);
                ps.setString(3, StudentSession.getCurrentRoll());
                ps.setString(4, em);
                ps.setString(5, ct);
                ps.setString(6, bt);
                ps.setString(7, dept);
                ps.setString(8, uni);

                if (isGroup) ps.setString(9, grpName);
                else ps.setNull(9, Types.VARCHAR); // ✅ FIX: was Types.TEXT (does not exist)

                ps.executeUpdate();

                try (ResultSet gk = ps.getGeneratedKeys()) {
                    regId = gk.next() ? gk.getInt(1) : -1;
                }
            }

            try (PreparedStatement pm = conn.prepareStatement(
                    "INSERT INTO registration_members (registration_id, full_name, email, contact, department) VALUES (?, ?, ?, ?, ?)")) {

                pm.setInt(1, regId);
                pm.setString(2, nm);
                pm.setString(3, em);
                pm.setString(4, ct);
                pm.setString(5, dept);
                pm.executeUpdate();

                if (isGroup) {
                    for (int i = 1; i < grpSize; i++) {
                        MemberInputs mi = findMemberInputs(i);
                        if (mi == null) continue;

                        pm.setInt(1, regId);
                        pm.setString(2, safe(mi.name.getText()));
                        pm.setString(3, safe(mi.email.getText()));
                        pm.setString(4, safe(mi.contact.getText()));
                        pm.setString(5, safe(mi.department.getText()));
                        pm.executeUpdate();
                    }
                }
            }

            try (PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE events SET registration_count = COALESCE(registration_count, 0) + 1 WHERE event_id = ?")) {
                ps2.setString(1, selected.id);
                ps2.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);

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

    private void clearForm() {
        if (nameField != null) nameField.clear();
        if (emailField != null) emailField.clear();
        if (contactField != null) contactField.clear();
        if (batchCombo != null) batchCombo.getSelectionModel().clearSelection();
        if (departmentField != null) departmentField.clear();

        if (groupNameField != null) groupNameField.clear();
        if (groupMembersBox != null) groupMembersBox.getChildren().clear();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void preloadUniversity() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT university FROM students WHERE roll = ?")) {

            ps.setString(1, StudentSession.getCurrentRoll());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (universityLabel != null) universityLabel.setText(rs.getString("university"));
                }
            }
        } catch (Exception ignore) {}
    }

    private void renderMemberInputs() {
        if (groupMembersBox == null || groupSizeSpinner == null || groupSizeSpinner.getValue() == null) return;

        groupMembersBox.getChildren().clear();
        int n = groupSizeSpinner.getValue();

        for (int i = 1; i < n; i++) {
            VBox row = new VBox(6);

            Label label = new Label("Member " + (i + 1));
            label.getStyleClass().add("white-label");

            TextField nm = new TextField();
            nm.setPromptText("Full Name");
            nm.getStyleClass().add("underline-field");

            TextField em = new TextField();
            em.setPromptText("Email");
            em.getStyleClass().add("underline-field");

            TextField ct = new TextField();
            ct.setPromptText("Contact (01XXXXXXXXX)");
            ct.getStyleClass().add("underline-field");

            TextField dp = new TextField();
            dp.setPromptText("Department");
            dp.getStyleClass().add("underline-field");

            row.getChildren().addAll(label, nm, em, ct, dp);
            row.setUserData(new MemberInputs(nm, em, ct, dp, i));
            groupMembersBox.getChildren().add(row);
        }
    }

    // Java 11-safe version
    private MemberInputs findMemberInputs(int index) {
        if (groupMembersBox == null) return null;

        for (Node n : groupMembersBox.getChildren()) {
            Object ud = n.getUserData();
            if (ud instanceof MemberInputs) {
                MemberInputs mi = (MemberInputs) ud;
                if (mi.index == index) return mi;
            }
        }
        return null;
    }

    static class EventRow {
        final String id;
        final String name;
        final String date;
        final String venue;
        final String club;
        final String fees;
        final int registrationOpen;
        final String status;
        final int isGroup;
        final int minGroup;
        final int maxGroup;

        EventRow(String id, String name, String date, String venue, String club, String fees,
                 int registrationOpen, String status, int isGroup, int minGroup, int maxGroup) {
            this.id = id;
            this.name = name;
            this.date = date;
            this.venue = venue;
            this.club = club;
            this.fees = fees;
            this.registrationOpen = registrationOpen;
            this.status = status;
            this.isGroup = isGroup;
            this.minGroup = minGroup;
            this.maxGroup = maxGroup;
        }
    }

    static class MemberInputs {
        final TextField name;
        final TextField email;
        final TextField contact;
        final TextField department;
        final int index;

        MemberInputs(TextField name, TextField email, TextField contact, TextField department, int index) {
            this.name = name;
            this.email = email;
            this.contact = contact;
            this.department = department;
            this.index = index;
        }
    }
}
