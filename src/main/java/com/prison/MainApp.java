package com.prison;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {


    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/fxml/login.fxml"));

        Parent root = FXMLLoader.load(
                getClass().getResource("/fxml/login.fxml")
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/css/modern.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.show();

        stage.setTitle("Prison Face Recognition System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
