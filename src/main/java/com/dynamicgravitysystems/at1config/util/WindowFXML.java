package com.dynamicgravitysystems.at1config.util;

import java.net.URL;

public enum WindowFXML {
    MAIN_WINDOW("/MainApplicationWindow.fxml"),
    TIME_SYNC("/TimeSynchronizerWindow.fxml");

    private final String fxmlPath;

    WindowFXML(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    public URL getUrl() {
        return WindowFXML.class.getResource(fxmlPath);
    }

}
