package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentLoginController {

    @FXML
    private TextField roll;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField visiblePasswordField;

    private boolean passwordVisible = false;

    @FXML
    private void togglePassword() {

        if (!passwordVisible) {
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
        }

        passwordVisible = !passwordVisible;
    }

    @FXML
    private void onLogin() {

        try {
            String rollNo = roll.getText();

            String rawPassword = passwordVisible
                    ? visiblePasswordField.getText()
                    : passwordField.getText();

            String hashedInput = PasswordUtil.hashPassword(rawPassword);

            Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT password_hash FROM students WHERE roll = ?"
            );
            ps.setString(1, rollNo);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                if (hashedInput.equals(storedHash)) {

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Login Successful");
                    alert.setHeaderText(null);
                    alert.setContentText("Student login successful!");
                    alert.showAndWait();

                    // 👉 GO TO STUDENT DASHBOARD (placeholder)
                    Main.switchScene("student_dashboard.fxml");

                } else {
                    showError("Incorrect password");
                }

            } else {
                showError("Roll not found");
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {
            showError("Database error");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Failed");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
