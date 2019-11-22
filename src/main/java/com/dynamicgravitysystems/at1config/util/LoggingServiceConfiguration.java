package com.dynamicgravitysystems.at1config.util;

import java.nio.file.Path;

public class LoggingServiceConfiguration {
    private final Path filePath;
    private final boolean append;
    private final boolean enabled;

    public LoggingServiceConfiguration(Path filePath, boolean append, boolean enabled) {
        this.filePath = filePath;
        this.append = append;
        this.enabled = enabled;
    }

    public Path getFilePath() {
        return filePath;
    }

    public boolean isAppend() {
        return append;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
