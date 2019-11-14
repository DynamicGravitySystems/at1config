package com.dynamicgravitysystems.at1config.services;

import com.dynamicgravitysystems.at1config.util.SerialConnectionParameters;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class SerialPortRunnable implements Runnable {
    private final Logger log;
    private final SerialPort port;
    private final PublishSubject<SerialMessage> messageSubject = PublishSubject.create();
    private final AtomicBoolean stopped = new AtomicBoolean(false);


    SerialPortRunnable(final String device, final SerialConnectionParameters connectionParameters) {
        log = LogManager.getLogger(SerialPortRunnable.class.getName() + "." + device);

        port = SerialPort.getCommPort(device);
        port.setComPortParameters(connectionParameters.getBaudRate(),
                connectionParameters.getDataBits(),
                connectionParameters.getStopBits(),
                connectionParameters.getParity());
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, connectionParameters.getReadTimeout(), 0);

    }

    Observable<SerialMessage> getMessageSubject() {
        return messageSubject;
    }

    @Override
    public void run() {
        if (port.openPort())
            messageSubject.onNext(SerialMessage.connected());
        else {
            log.error("Failed to open serial port");
            stopped.set(true);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(port.getInputStream()))) {
            String currentLine;
            while (!stopped.get()) {
                try {
                    currentLine = reader.readLine();
                    if (currentLine != null) {
                        currentLine = currentLine.strip().replaceAll("[\\r\\n]", "");
                        messageSubject.onNext(SerialMessage.ofValue(currentLine));
                        log.trace("Received Data: {}", currentLine);
                    }
                } catch (SerialPortTimeoutException ex) {
                    messageSubject.onNext(SerialMessage.timeout());
                    log.warn("Serial Read timed out");
                }
            }
        } catch (IOException ex) {
            log.error("IO Exception reading from serial port", ex);
        } finally {
            cleanup();
        }
    }

    void cancel() {
        stopped.set(true);
    }

    boolean isRunning() {
        return !stopped.get();
    }

    synchronized boolean writeToSerial(String value) {
        if (!port.isOpen())
            throw new IllegalStateException("Serial port is not opened for writing");

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(port.getOutputStream(), StandardCharsets.US_ASCII))) {
            writer.write(value);
            log.debug("Sent value '{}' to serial port", value);

        } catch (IOException ex) {
            log.error("Unable to write to serial port", ex);
            return false;
        }
        return true;
    }

    private void cleanup() {
        log.info("Cleaning up serial connection");
        if (port.isOpen())
            port.closePort();
        messageSubject.onNext(SerialMessage.disconnected());
        messageSubject.onComplete();
    }

}
