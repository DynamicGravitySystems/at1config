package com.dynamicgravitysystems.at1config.services;

public class NoopDataLogger implements DataLogger {
    public NoopDataLogger() {
    }

    @Override
    public void log(String data) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }
}
