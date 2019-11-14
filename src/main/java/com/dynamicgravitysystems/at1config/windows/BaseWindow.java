package com.dynamicgravitysystems.at1config.windows;

import com.dynamicgravitysystems.at1config.util.BaseController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public abstract class BaseWindow {

    private final Stage stage;
    private final Scene scene;
    private final BaseController controller;

    BaseWindow(Stage stage, URL fxmlUrl, double minHeight, double minWidth) throws IOException {
        this.stage = stage;
        stage.setMinHeight(minHeight);
        stage.setMinWidth(minWidth);

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        scene = new Scene(loader.load());
        stage.setScene(scene);

        controller = loader.getController();
        controller.setStage(stage);

        stage.setOnCloseRequest(event -> controller.dispose());
    }

    BaseWindow(Stage stage, URL fxmlUrl) throws IOException {
        this(stage, fxmlUrl, 480, 600);
    }

    protected Scene getScene() {
        return scene;
    }

    protected Stage getStage() {
        return stage;
    }

    void setStylesheets(String... stylesheets) {
        scene.getStylesheets().addAll(stylesheets);
    }

    void setIcon(URL icon) {
        try {
            stage.getIcons().add(new Image(icon.openStream()));
        } catch (IOException ex) {

        }
    }

    void setTitle(String title) {
        stage.setTitle(title);
    }

    public void show() {
        stage.show();
    }
}
