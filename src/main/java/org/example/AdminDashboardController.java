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
        Main.switchScene("admin_dashboard.fxml");
    }
}
