package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;


public class StudentSignupController {

    @FXML
    private TextField fullName;

    @FXML
    private TextField roll;

    @FXML
    private TextField university;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField visiblePasswordField;

    @FXML
    private Label passwordHint;

    @FXML
    private Button signupBtn;

    private boolean passwordVisible = false;

    @FXML
    private void initialize() {


        passwordHint.setText(
                "Password must contain both uppercase and lowercase letters, " +
                        "at least one number, at least one special character (@, #, &, etc.), " +
                        "and be at least 8 characters long"
        );

        signupBtn.setDisable(true);

        passwordField.textProperty().addListener(
                (obs, oldVal, newVal) -> validatePassword(newVal)
        );

        visiblePasswordField.textProperty().addListener(
                (obs, oldVal, newVal) -> validatePassword(newVal)
        );
    }

    private void validatePassword(String pwd) {

        if (pwd == null || pwd.isEmpty()) {
            passwordHint.setText(
                    "Password must contain both uppercase and lowercase letters, " +
                            "at least one number, at least one special character (@, #, &, etc.), " +
                            "and be at least 8 characters long"
            );
            signupBtn.setDisable(true);
            return;
        }

        if (!pwd.matches(".*[a-z].*") || !pwd.matches(".*[A-Z].*")) {
            passwordHint.setText(
                    "Password must contain both uppercase and lowercase letters"
            );
            signupBtn.setDisable(true);
            return;
        }

        if (!pwd.matches(".*\\d.*")) {
            passwordHint.setText(
                    "Password must contain at least one number"
            );
            signupBtn.setDisable(true);
            return;
        }

        if (!pwd.matches(".*[@#&!$%^*()].*")) {
            passwordHint.setText(
                    "Password must contain at least one special character (@, #, &, etc.)"
            );
            signupBtn.setDisable(true);
            return;
        }

        if (pwd.length() < 8) {
            passwordHint.setText(
                    "Password must be at least 8 characters long"
            );
            signupBtn.setDisable(true);
            return;
        }


        passwordHint.setText("Password meets all requirements");
        signupBtn.setDisable(false);
    }

    @FXML
    private void togglePassword() {

        if (!passwordVisible) {

            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            // Hide password
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
        }

        passwordVisible = !passwordVisible;
    }

    @FXML
    private void onSignup() {

        try {
            String name = fullName.getText();
            String rollNo = roll.getText();
            String universityName = university.getText();

            String rawPassword = passwordVisible
                    ? visiblePasswordField.getText()
                    : passwordField.getText();

            String hashedPassword = PasswordUtil.hashPassword(rawPassword);

            var conn = Database.getConnection();
                var ps = conn.prepareStatement(
                    "INSERT INTO students (full_name, roll, password_hash, university) VALUES (?, ?, ?, ?)"
                );

            ps.setString(1, name);
            ps.setString(2, rollNo);
            ps.setString(3, hashedPassword);
                ps.setString(4, universityName);
                ps.executeUpdate();

            ps.close();
            conn.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Sign up successful!");

            alert.showAndWait();

            Main.switchScene("welcome.fxml");

        } catch (Exception e) {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Roll already exists or database error");

            alert.showAndWait();
        }
    }



}
