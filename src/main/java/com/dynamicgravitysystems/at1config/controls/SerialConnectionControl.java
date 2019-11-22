package com.dynamicgravitysystems.at1config.controls;

import com.dynamicgravitysystems.at1config.services.DataLoggingService;
import com.dynamicgravitysystems.at1config.services.LoggingService;
import com.dynamicgravitysystems.at1config.services.SerialMessage;
import com.dynamicgravitysystems.at1config.services.SerialServiceManager;
import com.dynamicgravitysystems.at1config.util.DataSource;
import com.dynamicgravitysystems.at1config.util.LoggingServiceConfiguration;
import com.dynamicgravitysystems.at1config.util.ResourceManager;
import com.dynamicgravitysystems.at1config.util.SerialConnectionParameters;
import com.dynamicgravitysystems.at1config.windows.LogConfigurationDialog;
import io.reactivex.disposables.Disposable;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * The SerialConnectionControl provides a UI element to control the connection to a serial port, allowing user specified
 * port, baud rate, etc. And managing the connection state.
 */
public class SerialConnectionControl extends VBox {
    private static final Logger LOG = LogManager.getLogger(SerialConnectionControl.class.getName());
    private static final PseudoClass DANGER = PseudoClass.getPseudoClass("danger");
    private static final PseudoClass SUCCESS = PseudoClass.getPseudoClass("success");
    private final SerialServiceManager serialManager = SerialServiceManager.getInstance();
    private final LoggingService loggingService = DataLoggingService.getInstance();

    @FXML private Label lblTitle;
    @FXML private Label lblState;
    @FXML private Label lblStatus;
    @FXML private ChoiceBox<String> cbPort;
    @FXML private ChoiceBox<Integer> cbBaud;
    @FXML private ToggleButton btnToggleConnection;
    @FXML private MenuButton btnSettings;
    @FXML private MenuItem btnConfigureLogging;
    @FXML private MenuItem btnConfigureSerial;

    private DataSource source;
    private Disposable subscription;
    private String selectedPort;
    private LoggingServiceConfiguration logConfiguration = new LoggingServiceConfiguration(null, true, false);

    private Consumer<String> dataConsumer = value -> {
    };

    private final BooleanProperty connected = new SimpleBooleanProperty(false);

    public SerialConnectionControl() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/SerialConnection.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        try {
            Image wrench = new Image(ResourceManager.ICON_WRENCH.getStream(), 15, 15, true, true);
            btnSettings.setGraphic(new ImageView(wrench));
        } catch (IOException ignored) {

        }

        // Initialize Control State
        btnToggleConnection.setOnAction(event -> toggleConnection());
        btnConfigureLogging.setOnAction(event -> configureLogging());
        lblState.pseudoClassStateChanged(DANGER, true);

        cbPort.setItems(serialManager.getCommPorts());
        cbPort.getSelectionModel().selectNext();
        cbBaud.getItems().addAll(2400, 4800, 7200, 9600, 14400, 19200, 38400, 56000, 57600, 76800, 115200, 128000);

        lblState.textProperty().bind(Bindings.createStringBinding(() -> {
            if (connected.get())
                return "CONNECTED";
            else
                return "DISCONNECTED";
        }, connected));

        btnToggleConnection.disableProperty().bind(cbPort.valueProperty().isNull().or(cbBaud.valueProperty().isNull()));
        btnToggleConnection.textProperty().bind(Bindings.createStringBinding(() -> {
            if (connected.get())
                return "Disconnect";
            else
                return "Connect";
        }, connected));

        connected.addListener((property, previous, current) -> {
            lblState.pseudoClassStateChanged(SUCCESS, current);
            lblState.pseudoClassStateChanged(DANGER, !current);
        });

        cbPort.disableProperty().bind(connected);
        cbBaud.disableProperty().bind(connected);

        cbPort.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null)
                selectedPort = newValue;
        });

    }

    /**
     * Attempts to re-select the last selected serial port (e.g. after serial port listing has been refreshed)
     */
    public void reselectPort() {
        if (selectedPort != null) {
            int index = cbPort.getItems().indexOf(selectedPort);
            cbPort.getSelectionModel().select(index);
        }
    }

    public String getTitle() {
        return lblTitle.getText();
    }

    public void setTitle(String title) {
        lblTitle.setText(title);
    }

    public void setDefaultBaudRate(int baudRate) {
        cbBaud.getSelectionModel().select(cbBaud.getItems().indexOf(baudRate));
    }

    public int getDefaultBaudRate() {
        return cbBaud.getValue();
    }

    public DataSource getSource() {
        return source;
    }

    public void setSource(DataSource source) {
        this.source = source;
        connected.bind(serialManager.getConnectedProperty(source));
    }

    public void setDataConsumer(Consumer<String> consumer) {
        this.dataConsumer = consumer;
    }

    public void setPortSelectionIndex(int index) {
        cbPort.getSelectionModel().select(index);
    }

    @FXML
    public void toggleConnection() {
        if (btnToggleConnection.isSelected()) {
            connect();
        } else {
            disconnect();
        }
    }

    private void connect() {
        if (source == null)
            throw new IllegalStateException("Data Source must be set for SerialConnectionControl");

        final String portDesc = cbPort.getValue();
        final Integer baudRate = cbBaud.getValue();

        if (portDesc == null) {
            LOG.warn("No port is selected");
            showAlert("Port must be selected");
            btnToggleConnection.setSelected(false);
            return;
        }

        if (baudRate == null) {
            LOG.warn("No baud rate is selected");
            showAlert("Baud Rate must be selected");
            btnToggleConnection.setSelected(false);
            return;
        }

        if (subscription != null && !subscription.isDisposed())
            subscription.dispose();

        final SerialConnectionParameters parameters = SerialConnectionParameters.forPortAndBaud(portDesc, baudRate);
        LOG.info("Connecting to {} source on port {} with baud rate {}", source, portDesc, baudRate);

        subscription = serialManager.connect(source, parameters)
                .doOnNext(message -> Platform.runLater(() -> lblStatus.setText(message.getEvent().toString())))
                .filter(message -> message.getEvent() == SerialMessage.SerialEvent.RECEIVED)
                .subscribe(message -> {  // onNext
                    Platform.runLater(() -> dataConsumer.accept(message.getValue()));
                    loggingService.log(source, message.getValue());
                }, err -> {  // onError
                    LOG.error("Error occurred on serial connection {}", portDesc, err);
                    showAlert("Error occurred on serial connection, connection will be terminated. Reason: " + err.getMessage());
                    Platform.runLater(() -> btnToggleConnection.setSelected(false));

                }, () -> {  // onComplete
                    LOG.info("Serial connection has completed: {}", portDesc);
                    Platform.runLater(() -> btnToggleConnection.setSelected(false));
                });
    }

    private void disconnect() {
        serialManager.disconnect(source);
    }

    private void configureLogging() {
        LogConfigurationDialog dialog = new LogConfigurationDialog(source, logConfiguration);
        dialog.initOwner(getScene().getWindow());
        dialog.showAndWait().ifPresent(result -> {
            LOG.debug("Logging configuration updated");
            logConfiguration = result;
            loggingService.configure(source, logConfiguration);
        });
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.initOwner(getScene().getWindow());
            alert.setHeaderText("Serial Connection Error");
            alert.show();
        });
    }

}
