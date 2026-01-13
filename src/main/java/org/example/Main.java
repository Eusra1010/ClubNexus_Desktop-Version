package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Database.getConnection();
        stage = primaryStage;
        switchScene("welcome.fxml");
        stage.setTitle("Club Event Management");
        stage.setWidth(1000);
        stage.setHeight(650);
        stage.show();
    }

    public static void switchScene(String fxml) throws Exception {
        try {
            var url = Main.class.getResource("/org/example/" + fxml);
            if (url == null) {
                throw new IllegalStateException("Resource not found: " + fxml);
            }
            FXMLLoader loader = new FXMLLoader(url);
            Scene scene = new Scene(loader.load());
            var css = Main.class.getResource("/org/example/style.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                // Write detailed error to data/scene-error.txt for troubleshooting
                java.nio.file.Path cwd = java.nio.file.Paths.get("").toAbsolutePath();
                java.nio.file.Path probe = cwd;
                java.nio.file.Path dataDir = null;
                for (int i = 0; i < 6 && probe != null; i++) {
                    if (java.nio.file.Files.exists(probe.resolve("pom.xml"))) {
                        dataDir = probe.resolve("data");
                        break;
                    }
                    probe = probe.getParent();
                }
                if (dataDir == null) dataDir = cwd.resolve("data");
                if (!java.nio.file.Files.exists(dataDir)) {
                    java.nio.file.Files.createDirectories(dataDir);
                }
                java.nio.file.Path out = dataDir.resolve("scene-error.txt");
                StringBuilder sb = new StringBuilder();
                sb.append("Scene: ").append(fxml).append(System.lineSeparator());
                sb.append("Error: ").append(e.toString()).append(System.lineSeparator());
                Throwable c = e.getCause();
                int depth = 0;
                while (c != null && depth < 5) {
                    sb.append("Cause ").append(depth + 1).append(": ").append(c.toString()).append(System.lineSeparator());
                    c = c.getCause();
                    depth++;
                }
                java.nio.file.Files.writeString(out, sb.toString());
            } catch (Exception ignore) {}
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Failed to open '" + fxml + "'\n" + String.valueOf(e.getMessage()) +
                            "\nSee data/scene-error.txt for details.",
                    javafx.scene.control.ButtonType.OK
            );
            a.setHeaderText(null);
            a.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
