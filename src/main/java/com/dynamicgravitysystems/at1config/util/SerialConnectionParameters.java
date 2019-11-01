package com.dynamicgravitysystems.at1config.util;

public class SerialConnectionParameters {

    private final String device;
    private final int baudRate;
    private final int dataBits;
    private final int stopBits;
    private final int parity;
    private final int readTimeout;

    public SerialConnectionParameters(String device, int baudRate, int dataBits, int stopBits, int parity, int readTimeout) {
        this.device = device;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.readTimeout = readTimeout;
    }

    public static SerialConnectionParameters forPortAndBaud(String device, int baudRate) {
        return new SerialConnectionParameters(device, baudRate, 8, 1, 0, 1050);
    }

    public String getDevice() {
        return device;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public int getDataBits() {
        return dataBits;
    }

    public int getStopBits() {
        return stopBits;
    }

    public int getParity() {
        return parity;
    }

    public int getReadTimeout() {
        return readTimeout;
    }
}
