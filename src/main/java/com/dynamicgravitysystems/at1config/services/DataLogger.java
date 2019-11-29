package com.dynamicgravitysystems.at1config.services;

public interface DataLogger {
    void log(String data);

    void flush();

    void close();
}
