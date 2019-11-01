package com.dynamicgravitysystems.at1config.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public enum ResourceManager {
    LOGO_PNG("/logo.png"),
    DEFAULT_STYLE("/styles.css");

    private final String path;
    private final URL resourceURL;

    ResourceManager(String path) {
        this.path = path;
        resourceURL = getClass().getResource(path);
    }

    public URL getResource() {
        return resourceURL;
    }

    public InputStream getStream() throws IOException {
        return resourceURL.openStream();
    }

    public String getPath() {
        return path;
    }
}
