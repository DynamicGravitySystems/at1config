package com.dynamicgravitysystems.at1config.services;

import com.dynamicgravitysystems.at1config.command.SerialCommand;
import com.dynamicgravitysystems.at1config.util.DataSource;
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
    private final Map<DataSource, Observable<SerialMessage>> publishers = new EnumMap<>(DataSource.class);
    private final Map<DataSource, SerialPortRunnable> connections = new EnumMap<>(DataSource.class);
    private final Map<DataSource, BooleanProperty> connectedProperties = new EnumMap<>(DataSource.class);

    {
        connectedProperties.put(DataSource.GRAVITY, new SimpleBooleanProperty(false));
        connectedProperties.put(DataSource.GPS, new SimpleBooleanProperty(false));
    }

    SerialServiceManager() {
    }

    public static SerialServiceManager getInstance() {
        return INSTANCE;
    }

    public static void shutdown() {
        LOG.info("Shutting down Serial Service Manager");
        INSTANCE.disconnect(DataSource.GRAVITY);
        INSTANCE.disconnect(DataSource.GPS);
        INSTANCE.executor.shutdown();
    }

    public synchronized Observable<SerialMessage> connect(DataSource source, SerialConnectionParameters parameters) {
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
    public void disconnect(final DataSource source) {
        if (!connections.containsKey(source)) {
            LOG.warn("Unable to disconnect source {}, no connection exists", source);
            return;
        }

        connections.get(source).cancel();
        connections.remove(source);
        publishers.remove(source);
        connectedProperties.get(source).set(false);
    }

    public boolean isConnected(final DataSource source) {
        return connections.containsKey(source) && connections.get(source).isRunning();
    }

    public Observable<SerialMessage> getSubject(final DataSource source) {
        if (!publishers.containsKey(source))
            throw new IllegalStateException("No subject found for source: " + source);
        return publishers.get(source);
    }

    public boolean sendCommand(SerialCommand command) {
        return sendCommand(DataSource.GRAVITY, command);
    }

    public boolean sendCommand(DataSource source, SerialCommand command) {
        if (!isConnected(source)) {
            LOG.warn("Source {} is not connected, cannot send command", source);
            return false;
        }

        LOG.info("Sending command '{}' to serial source {}", command.getCommand(), source);
        return connections.get(source).writeToSerial(command.getCommand());
    }

    public boolean sendRawCommand(DataSource source, String command) {
        if (!isConnected(source)) {
            LOG.warn("Source {} is not connected, cannot send command", source);
            return false;
        }
        return connections.get(source).writeToSerial(command);
    }

    public BooleanProperty getConnectedProperty(DataSource source) {
        return connectedProperties.get(source);
    }
}
