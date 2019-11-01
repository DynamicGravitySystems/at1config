package com.dynamicgravitysystems.at1config.controllers;

import com.dynamicgravitysystems.at1config.bindings.MarineGravityReadingBinding;
import com.dynamicgravitysystems.at1config.command.MarineSensorCommand;
import com.dynamicgravitysystems.at1config.services.SerialMessage;
import com.dynamicgravitysystems.at1config.services.SerialServiceManager;
import com.dynamicgravitysystems.at1config.util.BaseController;
import com.dynamicgravitysystems.at1config.util.SerialConnectionParameters;
import com.dynamicgravitysystems.at1config.util.SharedState;
import com.dynamicgravitysystems.at1config.windows.TimeSynchronizerWindow;
import com.dynamicgravitysystems.common.gravity.MarineGravityReading;
import com.dynamicgravitysystems.common.gravity.MarineSensorCalibration;
import com.dynamicgravitysystems.common.ini.IniFile;
import com.fazecast.jSerialComm.SerialPort;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainWindowController extends BaseController {
    private static final Logger LOG = LogManager.getLogger(MainWindowController.class.getName());
    private static final String DISCONNECTED = "DISCONNECTED";
    private static final String CONNECTED = "CONNECTED";

    private static final PseudoClass DANGER = PseudoClass.getPseudoClass("danger");
    private static final PseudoClass SUCCESS = PseudoClass.getPseudoClass("success");

    @FXML MenuItem menuItemExit;
    @FXML Button btnClamp;
    @FXML Button btnUnclamp;
    @FXML Button btnBeginSync;
    @FXML Button btnRefreshPorts;
    @FXML Button btnClearGravity;
    @FXML Button btnClearGPS;
    @FXML Button btnDisconnectAll;

    /*Gravity Data Connection Controls*/
    @FXML Label lblGravityStatus;
    @FXML ToggleButton btnToggleGravity;
    @FXML ChoiceBox<String> selectPortGravity;
    @FXML ChoiceBox<Integer> selectBaudGravity;

    /*GPS Data Connection Controls*/
    @FXML Label lblGpsStatus;
    @FXML ToggleButton btnToggleGPS;
    @FXML ChoiceBox<String> selectPortGPS;
    @FXML ChoiceBox<Integer> selectBaudGPS;


    @FXML TextArea dataGravity;
    @FXML TextArea dataGPS;

    private final ObservableList<String> comPorts = FXCollections.observableArrayList();
    private final ObservableList<Integer> baudRates;

    private final MarineGravityReadingBinding gravityReadingBinding = new MarineGravityReadingBinding();
    private final SerialServiceManager serialManager = SerialServiceManager.getInstance();
    private final SharedState state = SharedState.STATE;

    public MainWindowController() {
        baudRates = FXCollections.observableArrayList(2400, 4800, 7200, 9600, 14400, 19200, 38400,
                56000, 57600, 76800, 115200, 128000);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshSerialPorts();

        final BooleanProperty connectedGravity = serialManager.getConnectedProperty(SerialServiceManager.SerialSource.GRAVITY);
        final BooleanProperty connectedGPS = serialManager.getConnectedProperty(SerialServiceManager.SerialSource.GPS);

        /*Action Bindings*/
        btnClearGravity.setOnAction(event -> dataGravity.clear());
        btnClearGPS.setOnAction(event -> dataGPS.clear());
        btnClamp.setOnAction(event -> serialManager.sendCommand(MarineSensorCommand.CLAMP));
        btnUnclamp.setOnAction(event -> serialManager.sendCommand(MarineSensorCommand.UNCLAMP));
        btnDisconnectAll.setOnAction(event -> {
            serialManager.disconnect(SerialServiceManager.SerialSource.GRAVITY);
            serialManager.disconnect(SerialServiceManager.SerialSource.GPS);
            btnToggleGravity.setSelected(false);
            btnToggleGPS.setSelected(false);
        });

        /*Property Bindings*/
        btnClamp.disableProperty().bind(connectedGravity.not());
        btnUnclamp.disableProperty().bind(connectedGravity.not());

        btnRefreshPorts.disableProperty().bind(connectedGravity.and(connectedGPS));
        btnBeginSync.disableProperty().bind(connectedGravity.not());

        selectPortGravity.disableProperty().bind(connectedGravity);
        selectBaudGravity.disableProperty().bind(connectedGravity);
        selectPortGPS.disableProperty().bind(connectedGPS);
        selectBaudGPS.disableProperty().bind(connectedGPS);

        /*Gravity Connection Pane*/
        lblGravityStatus.textProperty().bind(conditionalStringBinding(connectedGravity, CONNECTED, DISCONNECTED));

        lblGravityStatus.pseudoClassStateChanged(DANGER, true);
        connectedGravity.addListener((property, previous, current) -> {
            lblGravityStatus.pseudoClassStateChanged(SUCCESS, current);
            lblGravityStatus.pseudoClassStateChanged(DANGER, !current);
            if(current)
                btnToggleGravity.setText("Disconnect");
            else
                btnToggleGravity.setText("Connect");
        });

        selectPortGravity.setItems(comPorts);
        selectBaudGravity.setItems(baudRates);
        selectBaudGravity.getSelectionModel().select(baudRates.indexOf(57600));

        /*GPS Connection Pane*/
        lblGpsStatus.textProperty().bind(conditionalStringBinding(connectedGPS, CONNECTED, DISCONNECTED));

        lblGpsStatus.pseudoClassStateChanged(DANGER, true);
        connectedGPS.addListener((property, previous, current) -> {
            lblGpsStatus.pseudoClassStateChanged(SUCCESS, current);
            lblGpsStatus.pseudoClassStateChanged(DANGER, !current);
            if(current)
                btnToggleGPS.setText("Disconnect");
            else
                btnToggleGPS.setText("Connect");
        });

        selectPortGPS.setItems(comPorts);
        selectBaudGPS.setItems(baudRates);
        selectBaudGPS.getSelectionModel().select(baudRates.indexOf(38400));

        selectPortGravity.getSelectionModel().selectFirst();
    }

    @FXML
    public void refreshSerialPorts() {
        final String gravityPort = selectPortGravity.getValue();
        final String gpsPort = selectPortGPS.getValue();

        SerialPort[] ports = SerialPort.getCommPorts();
        if (!comPorts.isEmpty())
            comPorts.clear();
        for (SerialPort port : ports) {
            comPorts.add(port.getSystemPortName());
        }
        comPorts.sort(String::compareTo);

        // Select previously selected port if it is still available
        if (comPorts.contains(gravityPort))
            selectPortGravity.getSelectionModel().select(gravityPort);

        if (comPorts.contains(gpsPort))
            selectPortGPS.getSelectionModel().select(gpsPort);
    }

    @FXML
    public void connectSerialGravity() {
        if (btnToggleGravity.isSelected()) {
            final String selectedPort = selectPortGravity.getValue();
            if (selectedPort == null) {
                LOG.warn("A valid serial port must be selected");
                btnToggleGravity.setSelected(false);
                return;
            }

            final int baudRate = selectBaudGravity.getValue();
            LOG.info("Connecting to Gravity source on port {} with baud rate {}", selectedPort, baudRate);
            SerialConnectionParameters parameters = SerialConnectionParameters.forPortAndBaud(selectedPort, baudRate);

            serialManager.connect(SerialServiceManager.SerialSource.GRAVITY, parameters)
                    .observeOn(Schedulers.io())
                    .map(message -> {
                        // TODO: Set a data state on UI
                        SerialMessage.SerialEvent event = message.getEvent();
                        System.out.println("Event is: " + event);
                        return message;
                    })
                    .filter(serialMessage -> serialMessage.getEvent() == SerialMessage.SerialEvent.RECEIVED)
                    .doOnNext(value -> Platform.runLater(() -> dataGravity.appendText(value.getValue() + "\n")))
                    .flatMap(message -> Observable.just(message.getValue())
                            .map(MarineGravityReading::fromString)
                            .onErrorResumeNext(Observable.empty())
                            .onExceptionResumeNext(Observable.empty()))
                    .subscribe(gravityReadingBinding::update);

        } else {
            LOG.info("Disconnecting from Gravity source");
            serialManager.disconnect(SerialServiceManager.SerialSource.GRAVITY);
        }
    }

    @FXML
    public void connectSerialGPS() {
        if (btnToggleGPS.isSelected()) {
            final String selectedPort = selectPortGPS.getValue();
            if (selectedPort == null) {
                LOG.warn("A valid serial port must be selected");
                btnToggleGPS.setSelected(false);
                return;
            }

            final int baudRate = selectBaudGPS.getValue();
            LOG.info("Connecting to GPS source on port {} with baud rate {}", selectedPort, baudRate);
            SerialConnectionParameters parameters = SerialConnectionParameters.forPortAndBaud(selectedPort, baudRate);

            serialManager.connect(SerialServiceManager.SerialSource.GPS, parameters)
                    .doOnNext(value -> Platform.runLater(() -> dataGPS.appendText(value + "\n")))
                    .observeOn(Schedulers.io())
                    .subscribe(value -> {
                    });

        } else {
            LOG.info("Disconnecting from GPS source");
            serialManager.disconnect(SerialServiceManager.SerialSource.GPS);
        }
    }

    @FXML
    public void enterSyncMode() throws IOException {
        TimeSynchronizerWindow tsw = new TimeSynchronizerWindow(getStage());
        tsw.show();
    }

    @FXML
    public void loadIni() {
        LOG.debug("Loading Meter.ini");
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Meter.ini");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Meter Config", "*.ini"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File selection = chooser.showOpenDialog(new Stage());
        if (selection == null)
            return;

        try {
            IniFile meterIni = IniFile.fromFile(selection);
            state.setCalibration(MarineSensorCalibration.fromIni(meterIni));
            LOG.info("Successfully loaded sensor calibration");
        } catch (IOException ex) {
            LOG.error("Error parsing Meter.ini file", ex);
        }
    }

    @FXML
    public void exit() {
        SerialServiceManager.shutdown();
        Platform.exit();
        System.exit(0);
    }

    @Override
    public void dispose() {
        LOG.debug("MainController disposing");
    }

    private static StringBinding conditionalStringBinding(final BooleanProperty property,
                                                          final String valueIfTrue,
                                                          final String valueIfFalse) {
        return Bindings.createStringBinding(() -> {
            if (property.get())
                return valueIfTrue;
            else
                return valueIfFalse;
        }, property);
    }

}
