package com.dynamicgravitysystems.at1config.services;

import com.dynamicgravitysystems.at1config.command.SerialCommand;
import com.dynamicgravitysystems.at1config.util.SerialConnectionParameters;
import io.reactivex.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public enum SerialServiceManager {
    INSTANCE;

    private final static Logger LOG = LogManager.getLogger(SerialServiceManager.class.getName());
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<SerialSource, Observable<SerialMessage>> publishers = new EnumMap<>(SerialSource.class);
    private final Map<SerialSource, SerialPortRunnable> connections = new EnumMap<>(SerialSource.class);
    private final Map<SerialSource, BooleanProperty> connectedProperties = new EnumMap<>(SerialSource.class);

    public enum SerialSource {
        GRAVITY,
        GPS
    }

    SerialServiceManager() {
        connectedProperties.put(SerialSource.GRAVITY, new SimpleBooleanProperty(false));
        connectedProperties.put(SerialSource.GPS, new SimpleBooleanProperty(false));
    }

    public static SerialServiceManager getInstance() {
        return INSTANCE;
    }

    public static void shutdown() {
        LOG.info("Shutting down Serial Service Manager");
        INSTANCE.disconnect(SerialSource.GRAVITY);
        INSTANCE.disconnect(SerialSource.GPS);
        INSTANCE.executor.shutdown();
    }

    public synchronized Observable<SerialMessage> connect(SerialSource source, SerialConnectionParameters parameters) {
        if (connections.containsKey(source))
            throw new IllegalStateException("Connection already established to source: " + source);
        LOG.debug("Connecting to Serial Source {}", source);

        final String device = parameters.getDevice();
        final SerialPortRunnable runner = new SerialPortRunnable(device, parameters);

        publishers.put(source, runner.getMessageSubject());
        connections.put(source, runner);
        executor.submit(runner);
        connectedProperties.get(source).set(true);

        return runner.getMessageSubject();
    }

    /**
     * Disconnect from the specified SerialSource (i.e. a GPS or GRAVITY source)
     * <p>
     * If no connection is presently established to the designated source, no action is performed
     */
    public void disconnect(final SerialSource source) {
        if (!connections.containsKey(source)) {
            LOG.warn("Unable to disconnect source {}, no connection exists", source);
            return;
        }

        connections.get(source).cancel();
        connections.remove(source);
        publishers.remove(source);
        connectedProperties.get(source).set(false);
    }

    public boolean isConnected(final SerialSource source) {
        return connections.containsKey(source) && connections.get(source).isRunning();
    }

    public Observable<SerialMessage> getSubject(final SerialSource source) {
        if (!publishers.containsKey(source))
            throw new IllegalStateException("No subject found for source: " + source);
        return publishers.get(source);
    }

    public boolean sendCommand(SerialCommand command) {
        return sendCommand(SerialSource.GRAVITY, command);
    }

    public boolean sendCommand(SerialSource source, SerialCommand command) {
        if (!isConnected(source)) {
            LOG.warn("Gravity source is not connected, cannot send command");
            return false;
        }

        LOG.info("Sending command '{}' to serial source {}", command.getCommand(), source);
        return connections.get(source).writeToSerial(command.getCommand());
    }

    public BooleanProperty getConnectedProperty(SerialSource source) {
        return connectedProperties.get(source);
    }
}
