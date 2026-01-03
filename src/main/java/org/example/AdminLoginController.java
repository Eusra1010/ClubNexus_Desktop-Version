package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;


public class AdminLoginController {

    @FXML
    private PasswordField password;

    private boolean visible = false;

    @FXML
    private void togglePassword() {
        if (!visible) {
            password.setPromptText(password.getText());
            password.clear();
            password.setStyle("-fx-echo-char: 0;");
        } else {
            password.setText(password.getPromptText());
            password.setPromptText("Password");
            password.setStyle("");
        }
        visible = !visible;
    }

    @FXML
    private void onLogin() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Login Successful");
        alert.setHeaderText(null);
        alert.setContentText("Login successful!");

        alert.showAndWait();

        System.out.println("Admin login successful");
    }

}
