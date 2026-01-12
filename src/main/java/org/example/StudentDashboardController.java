package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class StudentDashboardController {

    @FXML
    private void goHome() throws Exception {
        Main.switchScene("welcome.fxml");
    }

    @FXML
    private void openNotifications() throws Exception {
        Main.switchScene("student_notifications.fxml");
    }

    @FXML
    private void goToViewEvents() throws Exception {
        try {
            java.net.URL resource = Main.class.getResource("/org/example/view_events.fxml");
            if (resource == null) {
                Alert a = new Alert(Alert.AlertType.ERROR, "View Events screen missing (resource not found)", ButtonType.OK);
                a.setHeaderText(null);
                a.showAndWait();
                return;
            }
            Main.switchScene("view_events.fxml");
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Unable to open View Events: " + e.getMessage(), ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    @FXML
    private void goToMyRegistrations() throws Exception {
        Main.switchScene("my_registrations.fxml");
    }
}
