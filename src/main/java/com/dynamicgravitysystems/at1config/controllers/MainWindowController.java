package com.dynamicgravitysystems.at1config.controllers;

import com.dynamicgravitysystems.at1config.command.MarineSensorCommand;
import com.dynamicgravitysystems.at1config.controls.SerialConnectionControl;
import com.dynamicgravitysystems.at1config.parsing.DataParser;
import com.dynamicgravitysystems.at1config.services.SerialServiceManager;
import com.dynamicgravitysystems.at1config.util.BaseController;
import com.dynamicgravitysystems.at1config.util.DataSource;
import com.dynamicgravitysystems.at1config.windows.TimeSynchronizerWindow;
import com.dynamicgravitysystems.common.gravity.SensorCalibration;
import com.dynamicgravitysystems.common.ini.IniFile;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class MainWindowController extends BaseController {
    private static final Logger LOG = LogManager.getLogger(MainWindowController.class.getName());

    @FXML private MenuItem menuToolsTimeSync;
    @FXML private MenuItem menuItemSettings;

    @FXML SerialConnectionControl gravityConnectionControl;
    @FXML SerialConnectionControl gpsConnectionControl;

    /*Sensor Command Action Buttons*/
    @FXML private Button btnClamp;
    @FXML private Button btnUnclamp;
    @FXML private Button btnClampLimits;
    @FXML private Button btnStopClamp;
    @FXML private Button btnFeedbackOn;
    @FXML private Button btnFeedbackOff;

    @FXML private Button btnBeginSync;
    @FXML private Button btnRefreshPorts;
    @FXML private Button btnClearGravity;
    @FXML private Button btnClearGPS;
    @FXML private Button btnDisconnectAll;

    @FXML private TextArea dataGravity;
    @FXML private TextArea dataGPS;

    private final SerialServiceManager serialManager = SerialServiceManager.getInstance();
    private final DataParser processor = DataParser.INSTANCE;


    public MainWindowController() {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshSerialPorts();

        final BooleanProperty connectedGravity = serialManager.getConnectedProperty(DataSource.GRAVITY);
        final BooleanProperty connectedGPS = serialManager.getConnectedProperty(DataSource.GPS);

        /*Menu Item Bindings*/
        menuToolsTimeSync.disableProperty().bind(connectedGravity.not());
        //  menuItemSettings.setOnAction(event -> new ApplicationSettingsWindow(getStage()).show());

        /*Action Bindings*/
        btnClearGravity.setOnAction(event -> dataGravity.clear());
        btnClearGPS.setOnAction(event -> dataGPS.clear());

        btnClamp.setOnAction(event -> serialManager.sendCommand(MarineSensorCommand.CLAMP));
        btnUnclamp.setOnAction(event -> serialManager.sendCommand(MarineSensorCommand.UNCLAMP));
        btnClampLimits.setOnAction(event -> serialManager.sendCommand(MarineSensorCommand.SET_CLAMP_LIMITS));
        btnStopClamp.setOnAction(event -> serialManager.sendCommand(MarineSensorCommand.STOP_CLAMP_MOTOR));
        btnFeedbackOn.setOnAction(event -> serialManager.sendCommand(MarineSensorCommand.FEEDBACK_ON));
        btnFeedbackOff.setOnAction(event -> serialManager.sendCommand(MarineSensorCommand.FEEDBACK_OFF));

        btnDisconnectAll.setOnAction(event -> Arrays.stream(DataSource.values()).forEach(serialManager::disconnect));

        /*Property Bindings*/
        btnClamp.disableProperty().bind(connectedGravity.not());
        btnUnclamp.disableProperty().bind(connectedGravity.not());
        btnClampLimits.disableProperty().bind(connectedGravity.not());
        btnStopClamp.disableProperty().bind(connectedGravity.not());
        btnFeedbackOn.disableProperty().bind(connectedGravity.not());
        btnFeedbackOff.disableProperty().bind(connectedGravity.not());

        btnRefreshPorts.disableProperty().bind(connectedGravity.and(connectedGPS));
        btnBeginSync.disableProperty().bind(connectedGravity.not());

        gravityConnectionControl.setDataConsumer(value -> dataGravity.appendText(value + "\n"));
        gravityConnectionControl.setPortSelectionIndex(0);
        gpsConnectionControl.setDataConsumer(value -> dataGPS.appendText(value + "\n"));
        gpsConnectionControl.setPortSelectionIndex(1);

    }

    @FXML
    public void refreshSerialPorts() {
        serialManager.refreshCommPorts();
        gravityConnectionControl.reselectPort();
        gpsConnectionControl.reselectPort();
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
            processor.setCalibration(SensorCalibration.fromIni(meterIni));
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

    private void showAlert(Alert.AlertType alertType, String message) {
        Platform.runLater(() -> new Alert(alertType, message).show());
    }

}
