package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        switchScene("welcome.fxml");
        stage.setTitle("Club Event Management");
        stage.setWidth(1000);
        stage.setHeight(650);
        stage.show();
    }

    public static void switchScene(String fxml) throws Exception {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxml));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                Main.class.getResource("style.css").toExternalForm()
        );
        stage.setScene(scene);
    }

    public static void main(String[] args) {
        launch();
    }
}
