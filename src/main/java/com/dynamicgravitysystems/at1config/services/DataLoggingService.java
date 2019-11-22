package com.dynamicgravitysystems.at1config.services;

import com.dynamicgravitysystems.at1config.util.DataSource;
import com.dynamicgravitysystems.at1config.util.LoggingServiceConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public enum DataLoggingService implements LoggingService {
    INSTANCE;

    private static final Logger LOG = LogManager.getLogger(DataLoggingService.class.getName());
    private static final String ENDL = "\r\n";
    private final EnumMap<DataSource, BufferedWriter> logWriters = new EnumMap<>(DataSource.class);

    DataLoggingService() {

    }

    public void setLogFile(DataSource source, Path file) throws IOException {

        BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (logWriters.containsKey(source)) {
            logWriters.get(source).close();
        }

        logWriters.put(source, writer);

        LOG.info("Set log file for {} to {}", source, file);
    }

    @Override
    public void configure(DataSource source, LoggingServiceConfiguration configuration) {
        // compute a new bufferedWriter for the logWriters map
        logWriters.compute(source, (src, writer) -> {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            if (configuration.getFilePath() == null) {
                return null;
            }
            if (!configuration.isEnabled()) {
                return null;
            }

            List<OpenOption> options = new ArrayList<>();
            options.add(StandardOpenOption.CREATE);
            options.add(StandardOpenOption.WRITE);
            if (configuration.isAppend())
                options.add(StandardOpenOption.APPEND);
            else
                options.add(StandardOpenOption.TRUNCATE_EXISTING);

            try {
                return Files.newBufferedWriter(configuration.getFilePath(), options.toArray(OpenOption[]::new));
            } catch (IOException e) {
                return null;
            }
        });
    }

    @Override
    public void log(DataSource source, String data) throws IOException {
        BufferedWriter writer = logWriters.get(source);
        if (writer == null)
            return;

        synchronized (logWriters.get(source)) {
            writer.write(data + ENDL);
        }
    }

    @Override
    public void flush(DataSource source) {
        try {
            Writer writer = logWriters.get(source);
            if (writer != null)
                writer.flush();
        } catch (IOException e) {
            LOG.debug("Unable to flush writer for data source {}", source, e);

        }
    }

    public void closeAll() {
        logWriters.forEach((source, writer) -> {
            try {
                writer.flush();
                writer.close();
                LOG.debug("Closed writer for source: {}", source);
            } catch (IOException e) {
                LOG.error("Unable to close writer for source: {}", source, e);
            }
        });
    }

    public static void shutdown() {
        LOG.info("Closing all open data log files");
        getInstance().closeAll();
    }

    public static DataLoggingService getInstance() {
        return INSTANCE;
    }

}
