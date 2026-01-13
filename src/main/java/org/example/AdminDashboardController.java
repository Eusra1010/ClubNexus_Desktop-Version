package org.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class AdminDashboardController {

    @FXML
    private StackPane contentHolder;

    // Store dashboard content (the VBox already in FXML)
    private Node dashboardView;

    @FXML
    public void initialize() {
        dashboardView = contentHolder.getChildren().get(0);
    }

    // Sidebar: Dashboard
    @FXML
    public void showDashboard() {
        contentHolder.getChildren().setAll(dashboardView);
    }

    // Sidebar: Create Event
    @FXML
    public void onCreateEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AdminDashboardController.class
                            .getResource("create_event.fxml")
            );

            Parent page = loader.load();

            contentHolder.getChildren().clear();
            contentHolder.getChildren().add(page);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Sidebar: Manage Events
    @FXML
    public void onManageEvents() {
        loadIntoContent("manage_events.fxml");
    }

    // Sidebar: View Registrations
    @FXML
    public void onViewRegistrations() {
        loadIntoContent("view_registrations.fxml");
    }

    // Sidebar: Announcements (Create Announcement)
    @FXML
    public void onAnnouncements() {
        loadIntoContent("create_announcement.fxml");
    }


    private void loadIntoContent(String fxml) {
        try {
            Parent page = FXMLLoader.load(
                    getClass().getResource("/org/example/" + fxml)
            );
            contentHolder.getChildren().clear();
            contentHolder.getChildren().add(page);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLogout() throws Exception {
        // From main admin dashboard, logout should go to welcome page
        Main.switchScene("welcome.fxml");
    }
}
