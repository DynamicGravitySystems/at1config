package com.dynamicgravitysystems.at1config.util;

import java.net.URL;

public enum WindowManager {
    MAIN_WINDOW("/MainApplicationWindow.fxml"),
    TIME_SYNC("/TimeSynchronizerWindow.fxml");

    private final String fxmlPath;

    WindowManager(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    public URL getUrl() {
        return WindowManager.class.getResource(fxmlPath);
    }

}
