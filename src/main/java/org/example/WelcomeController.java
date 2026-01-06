package org.example;

import javafx.fxml.FXML;

public class WelcomeController {

    @FXML
    private void openStudentLogin() {
        try {
            Main.switchScene("student_login.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openStudentSignup() {
        try {
            Main.switchScene("student_signup.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAdminLogin() {
        try {
            Main.switchScene("admin_login.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
