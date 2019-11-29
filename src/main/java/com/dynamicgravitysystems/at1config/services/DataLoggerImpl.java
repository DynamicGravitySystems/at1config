package com.dynamicgravitysystems.at1config.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A Thread-Safe Data Writing Utility
 * <p>
 * DataLoggerImpl class is immutable, to reconfigure parameters a new instance should be created
 */
public class DataLoggerImpl implements DataLogger, AutoCloseable {
    private final static Logger LOG = LogManager.getLogger(DataLoggerImpl.class.getName());
    private final static List<OpenOption> BASE_OPTIONS = new ArrayList<>();

    static {
        BASE_OPTIONS.add(StandardOpenOption.CREATE);
        BASE_OPTIONS.add(StandardOpenOption.WRITE);
    }

    private final Path logPath;
    private final String lineEnding;
    private final BufferedWriter writer;

    DataLoggerImpl(final Path logPath, final boolean append, final String lineEnding) throws IOException {
        this.logPath = logPath;
        this.lineEnding = lineEnding;

        final List<OpenOption> options = new ArrayList<>(BASE_OPTIONS);
        if (append)
            options.add(StandardOpenOption.APPEND);
        else
            options.add(StandardOpenOption.TRUNCATE_EXISTING);

        writer = Files.newBufferedWriter(this.logPath, options.toArray(OpenOption[]::new));
    }

    @Override
    public synchronized void log(String data) {
        try {
            writer.write(data + lineEnding);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    @Override
    public synchronized void flush() {
        try {
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    @Override
    public synchronized void close() {
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
