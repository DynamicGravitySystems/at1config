package com.dynamicgravitysystems.at1config.util;

import java.nio.file.Path;

public class LoggingServiceConfiguration {
    private static final String DEFAULT_LINE_ENDING = "\r\n";
    private final Path filePath;
    private final boolean append;
    private final boolean enabled;
    private final String lineEnding;

    public LoggingServiceConfiguration(Path filePath, boolean append, boolean enabled) {
        this(filePath, append, enabled, DEFAULT_LINE_ENDING);
    }

    public LoggingServiceConfiguration(Path filePath, boolean append, boolean enabled, String lineEnding) {
        this.filePath = filePath;
        this.append = append;
        this.enabled = enabled;
        this.lineEnding = lineEnding;
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

    public String getLineEnding() {
        return lineEnding;
    }
}
