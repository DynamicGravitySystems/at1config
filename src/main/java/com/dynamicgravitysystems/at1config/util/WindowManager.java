package com.dynamicgravitysystems.at1config.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public enum WindowManager {
    MAIN_WINDOW("/main.fxml"),
    TIME_SYNC("/timesync.fxml");

    private final String fxmlPath;

    WindowManager(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    public URL getUrl() {
        return WindowManager.class.getResource(fxmlPath);
    }

    public ParentAndController load() throws IOException {
        return load(WindowManager.class);
    }

    public ParentAndController load(Class base) throws IOException {

        final FXMLLoader loader = new FXMLLoader(base.getResource(fxmlPath));
        Parent parent = loader.load();
        BaseController controller = loader.getController();

        return new ParentAndController(parent, controller);
    }


}
