package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminLoginController {

    @FXML
    private TextField username;

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

        String inputId = username.getText();
        String inputPassword = password.getText();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data/clubnexus.db");
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT password_hash FROM admins WHERE admin_id = ?")) {

            stmt.setString(1, inputId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next() || !inputPassword.equals(rs.getString("password_hash"))) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Failed");
                alert.setHeaderText(null);
                alert.setContentText("Login failed");
                alert.showAndWait();
                return;
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Login Successful");
            alert.setHeaderText(null);
            alert.setContentText("Admin login successful!");
            alert.showAndWait();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Login Failed");
            alert.setHeaderText(null);
            alert.setContentText("Login failed");
            alert.showAndWait();
            return;
        }

        try {
            Main.switchScene("admin_dashboard.fxml");

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Dashboard could not be loaded");
            alert.showAndWait();
        }
    }

}
