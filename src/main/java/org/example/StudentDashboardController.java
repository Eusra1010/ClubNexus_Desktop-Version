package org.example;

import javafx.fxml.FXML;

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
        Main.switchScene("view_events.fxml");
    }

    @FXML
    private void goToMyRegistrations() throws Exception {
        Main.switchScene("my_registrations.fxml");
    }
}
