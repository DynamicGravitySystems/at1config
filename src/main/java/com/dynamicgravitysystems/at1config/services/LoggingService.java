package com.dynamicgravitysystems.at1config.services;

import com.dynamicgravitysystems.at1config.util.DataSource;
import com.dynamicgravitysystems.at1config.util.LoggingServiceConfiguration;

import java.io.IOException;

public interface LoggingService {

    void configure(DataSource source, LoggingServiceConfiguration configuration);

    void log(DataSource source, String data) throws IOException;

    void flush(DataSource source) throws IOException;

    void shutdown() throws IOException;

}
