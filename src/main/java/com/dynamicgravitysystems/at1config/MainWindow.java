package com.dynamicgravitysystems.at1config;

import com.dynamicgravitysystems.at1config.services.SerialProvider;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class MainWindow extends Application {
    public void start(Stage stage) throws Exception {
        Parent main = FXMLLoader.load(getClass().getResource("/main_window.fxml"));

        Scene scene = new Scene(main);
        stage.setScene(scene);
        stage.show();

        SerialProvider sp = new SerialProvider("COM3", 57600);
    }

    public static void main(String[] args) {
        launch();
    }
}
