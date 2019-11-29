package com.dynamicgravitysystems.at1config.services;

import com.dynamicgravitysystems.at1config.util.DataSource;
import com.dynamicgravitysystems.at1config.util.LoggingServiceConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public enum DataLoggingService implements LoggingService {
    INSTANCE;

    private static final Logger LOG = LogManager.getLogger(DataLoggingService.class.getName());
    private final DataLogger NOOP_LOGGER = new NoopDataLogger();
    private final EnumMap<DataSource, DataLogger> dataLoggers = new EnumMap<>(DataSource.class);

    DataLoggingService() {
        for (DataSource source : DataSource.values())
            dataLoggers.put(source, NOOP_LOGGER);
    }

    @Override
    public synchronized void configure(DataSource source, LoggingServiceConfiguration configuration) {
        dataLoggers.compute(source, (dataSource, dataLogger) -> {
            assert dataLogger != null;
            dataLogger.close();

            if (!configuration.isEnabled()) {
                LOG.debug("No-Op Logger added (configuration state is disabled)");
                return NOOP_LOGGER;
            }

            try {
                LOG.debug("Instantiated new Data Logger for source {}", source);
                return new DataLoggerImpl(configuration.getFilePath(), configuration.isAppend(), configuration.getLineEnding());
            } catch (IOException e) {
                LOG.error("Error instantiating new DataLogger", e);
                return NOOP_LOGGER;
            }
        });
    }

    @Override
    public void log(final DataSource source, final String data) {
        dataLoggers.entrySet().stream()
                .filter((entry) -> entry.getKey() == source)
                .map(Map.Entry::getValue)
                .findFirst()
                .ifPresent(value -> value.log(data));
    }

    @Override
    public void flush(DataSource source) {
        dataLoggers.forEach(((dataSource, dataLogger) -> dataLogger.flush()));
    }

    @Override
    public void shutdown() {
        LOG.info("Closing all open data log files");
        dataLoggers.forEach(((dataSource, dataLogger) -> dataLogger.close()));
    }

    public static DataLoggingService getInstance() {
        return INSTANCE;
    }

}
