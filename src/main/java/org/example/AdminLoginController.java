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

        String inputId = username.getText().trim();
        String inputPassword = password.getText();

        if (inputId.isEmpty() || inputPassword.isEmpty()) {
            showError("Please enter username and password");
            return;
        }

        try (Connection conn =
                 Database.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(
                             "SELECT password_hash, club_name " +
                                     "FROM admins WHERE admin_id = ?")) {

            stmt.setString(1, inputId);
            ResultSet rs = stmt.executeQuery();

            // ❌ Invalid login
            if (!rs.next()
                    || !inputPassword.equals(rs.getString("password_hash"))) {

                showError("Login failed");
                return;
            }

            // ✅ FETCH CLUB NAME
            String clubName = rs.getString("club_name");

            // ✅ STORE SESSION
            AdminSession.setAdmin(inputId, clubName);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Login Successful");
            alert.setHeaderText(null);
            alert.setContentText(
                    "Admin login successful!\nClub: " + clubName
            );
            alert.showAndWait();

            // ✅ GO TO DASHBOARD
            Main.switchScene("admin_dashboard.fxml");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Login failed");
        }
    }

    // ================= HELPER =================
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
