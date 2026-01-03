package org.example;

import javafx.fxml.FXML;

public class WelcomeController {

    @FXML
    private void openStudentSignup() {
        System.out.println("SIGN UP CLICKED");
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
