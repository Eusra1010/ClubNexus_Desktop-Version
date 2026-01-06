package org.example;

import javafx.fxml.FXML;

public class AdminDashboardController {

    @FXML
    private void onLogout() throws Exception {
        Main.switchScene("welcome.fxml");
    }
}
