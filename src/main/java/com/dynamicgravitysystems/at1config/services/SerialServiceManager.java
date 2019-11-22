package com.dynamicgravitysystems.at1config.services;

import com.dynamicgravitysystems.at1config.command.SerialCommand;
import com.dynamicgravitysystems.at1config.util.DataSource;
import com.dynamicgravitysystems.at1config.util.SerialConnectionParameters;
import com.fazecast.jSerialComm.SerialPort;
import io.reactivex.Observable;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public enum SerialServiceManager {
    INSTANCE;

    private final static Logger LOG = LogManager.getLogger(SerialServiceManager.class.getName());
    private final static Marker COMMAND_MARK = MarkerManager.getMarker("COMMAND");
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<DataSource, Observable<SerialMessage>> publishers = new EnumMap<>(DataSource.class);
    private final Map<DataSource, SerialPortRunnable> connections = new EnumMap<>(DataSource.class);
    private final Map<DataSource, BooleanProperty> connectedProperties = new EnumMap<>(DataSource.class);

    {
        connectedProperties.put(DataSource.GRAVITY, new SimpleBooleanProperty(false));
        connectedProperties.put(DataSource.GPS, new SimpleBooleanProperty(false));
    }

    private final ObservableList<String> commPorts = FXCollections.observableArrayList();

    SerialServiceManager() {
        refreshCommPorts();
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

        runner.getMessageSubject().subscribe(event -> {
            if (event.getEvent() == SerialMessage.SerialEvent.CONNECTED) {
                Platform.runLater(() -> connectedProperties.get(source).set(true));
            }
        }, err -> {
            LOG.error("Serial connection has failed");
            Platform.runLater(() -> connectedProperties.get(source).set(false));
            disconnect(source);
        }, () -> Platform.runLater(() -> connectedProperties.get(source).set(false)));

        publishers.put(source, runner.getMessageSubject());
        connections.put(source, runner);
        executor.submit(runner);

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

        DataLoggingService.getInstance().flush(source);

        Platform.runLater(() -> connectedProperties.get(source).set(false));
    }

    public boolean isNotConnected(final DataSource source) {
        return !connections.containsKey(source) || !connections.get(source).isRunning();
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
        if (isNotConnected(source)) {
            LOG.warn("Source {} is not connected, cannot send command", source);
            return false;
        }

        LOG.info(COMMAND_MARK, "Sending command {} <{}> to source: {}", command, command.getCommand(), source);
        return connections.get(source).writeToSerial(command.getCommand());
    }

    public boolean sendRawCommand(DataSource source, String command) {
        if (isNotConnected(source)) {
            LOG.warn("Source {} is not connected, cannot send command", source);
            return false;
        }
        LOG.info(COMMAND_MARK, "Sending raw command <{}> to source: {}", command, source);
        return connections.get(source).writeToSerial(command);
    }

    public BooleanProperty getConnectedProperty(DataSource source) {
        return connectedProperties.get(source);
    }

    public ObservableList<String> getCommPorts() {
        return commPorts;
    }

    public void refreshCommPorts() {
        if (!commPorts.isEmpty())
            commPorts.clear();
        commPorts.addAll(Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortName).collect(Collectors.toList()));
        commPorts.sort(String::compareTo);
    }
}
