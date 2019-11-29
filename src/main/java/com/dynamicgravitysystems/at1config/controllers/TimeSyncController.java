package com.dynamicgravitysystems.at1config.controllers;

import com.dynamicgravitysystems.at1config.command.MarineSensorCommand;
import com.dynamicgravitysystems.at1config.controls.LineChartControl;
import com.dynamicgravitysystems.at1config.models.SyncState;
import com.dynamicgravitysystems.at1config.models.SyncValue;
import com.dynamicgravitysystems.at1config.parsing.DataParser;
import com.dynamicgravitysystems.at1config.services.SerialMessage;
import com.dynamicgravitysystems.at1config.services.SerialServiceManager;
import com.dynamicgravitysystems.at1config.util.BaseController;
import com.dynamicgravitysystems.at1config.util.DataSource;
import com.dynamicgravitysystems.common.filters.Filter;
import com.dynamicgravitysystems.common.filters.MovingAverageFilter;
import com.dynamicgravitysystems.common.gravity.DataField;
import com.dynamicgravitysystems.common.gravity.MarineGravityReading;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeSyncController extends BaseController {
    private static final Logger LOG = LogManager.getLogger(TimeSyncController.class.getName());
    private static final int timeoutSeconds = 4;

    @FXML TextField txtTimeDiff;
    @FXML TextField txtFreqDeriv;
    @FXML TextField txtAdjSteps;
    @FXML TextField txtElecTemp;
    @FXML TextField txtPlotWidth;

    @FXML Label lblSyncStatus;

    @FXML Button btnStartSync;
    @FXML Button btnViewSync;
    @FXML Button btnSendAdjustment;
    @FXML Button btnSetPlotWidth;

    @FXML ChoiceBox<Integer> cbAdjustment;

    @FXML CheckBox cbTimeDiff;
    @FXML CheckBox cbFreqDeriv;
    @FXML CheckBox cbElecTemp;

    @FXML LineChartControl chart;


    private final ObjectProperty<SyncState> syncStateProperty = new SimpleObjectProperty<>(SyncState.DATA);

    private final IntegerProperty timeDifference = new SimpleIntegerProperty(0);
    private final IntegerProperty frequencyDerivative = new SimpleIntegerProperty(0);
    private final IntegerProperty adjustmentSteps = new SimpleIntegerProperty(0);
    private final DoubleProperty elecTemp = new SimpleDoubleProperty(0);

    private final Filter temperatureFilter = new MovingAverageFilter(200);

    private final SerialServiceManager serialManager = SerialServiceManager.getInstance();
    private final DataParser processor = DataParser.INSTANCE;

    private final CompositeDisposable subscriptions = new CompositeDisposable();

    private ConnectableObservable<SyncValue> baseSource;

    private final String etempSeries = "Electronics Temperature";
    private final String freqSeries = "Frequency Derivative";
    private final String timeSeries = "Time Difference";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (serialManager.isNotConnected(DataSource.GRAVITY))
            throw new IllegalStateException("TimeSyncController cannot be initialized if there is no Gravity source");

        cbAdjustment.getItems().addAll(-3, -2, -1, 0, 1, 2, 3);
        cbAdjustment.getSelectionModel().select(3);
        cbAdjustment.disableProperty().bind(syncStateProperty.isEqualTo(SyncState.DATA));
        btnSendAdjustment.disableProperty().bind(syncStateProperty.isEqualTo(SyncState.DATA)
                .or(cbAdjustment.valueProperty().isEqualTo(0)));
        btnSendAdjustment.setOnAction(event -> {
            if (cbAdjustment.getValue() != 0) {
                LOG.info("Sending adjustment step of {}", cbAdjustment.getValue());
                sendAdjustment(cbAdjustment.getValue());
                cbAdjustment.getSelectionModel().select(3);  // Reselect 0 value after sending
            }
        });

        btnViewSync.disableProperty().bind(syncStateProperty.isEqualTo(SyncState.VIEW)
                .or(syncStateProperty.isEqualTo(SyncState.SYNC)));
        btnStartSync.disableProperty().bind(syncStateProperty.isEqualTo(SyncState.SYNC));

        txtTimeDiff.textProperty().bind(timeDifference.asString());
        txtFreqDeriv.textProperty().bind(frequencyDerivative.asString());
        txtAdjSteps.textProperty().bind(adjustmentSteps.asString());
        txtElecTemp.textProperty().bind(elecTemp.asString());
        txtPlotWidth.textProperty().set("30");
        lblSyncStatus.textProperty().bind(syncStateProperty.asString());

        chart.addSeries(etempSeries, freqSeries, timeSeries);

        cbFreqDeriv.setOnAction(event -> chart.toggleSeries(freqSeries));
        cbTimeDiff.setOnAction(event -> chart.toggleSeries(timeSeries));
        cbElecTemp.setOnAction(event -> chart.toggleSeries(etempSeries));

        baseSource = serialManager.getSubject(DataSource.GRAVITY)
                .observeOn(Schedulers.io())
                .filter(serialMessage -> serialMessage.getEvent() == SerialMessage.SerialEvent.RECEIVED)
                .flatMap(value -> Observable.just(value.getValue())
                        .map(MarineGravityReading::withSyncFields)
                        .map(SyncValue::fromGravityReading)
                        .doOnNext(sv -> {
                            final double elecTempValue = temperatureFilter.filter(
                                    processor.getCalibration()
                                            .getFieldCalibration(DataField.ELEC_TEMP)
                                            .apply(sv.getElecTemperature()));
                            Platform.runLater(() -> {
                                chart.push(etempSeries, getEpochNow(), elecTempValue);
                                elecTemp.setValue(elecTempValue);
                            });
                        })
                        .doOnError(error -> LOG.error("Error parsing marine gravity reading, reason: {}", error.getMessage()))
                        .onErrorResumeNext(Observable.empty()))
                .publish();

        interpretStatus();

        subscriptions.add(baseSource.skipWhile(reading -> reading.getState() == SyncState.DATA).subscribe(
                this::updateProperties
        ));

        baseSource.connect();
    }

    /**
     * Start the clock synchronization routine
     */
    @FXML
    public void startSync() {
        LOG.info("Beginning sync routine");

        serialManager.sendCommand(MarineSensorCommand.FIND_FREQ_OFFSET);
        subscriptions.add(baseSource.skipWhile(syncValue -> syncValue.getState() != SyncState.SYNC)
                .timeout(timeoutSeconds, TimeUnit.SECONDS)
                .takeUntil(syncValue -> syncValue.getState() == SyncState.VIEW)
                .subscribe(syncValue -> {
                        },
                        error -> {
                            if (error instanceof TimeoutException) {
                                showAlert(Alert.AlertType.ERROR, "Timeout occurred waiting for sync mode switch");
                            } else {
                                LOG.error(error);
                            }
                        },
                        () -> {  // onComplete
                            LOG.info("Time synchronization has completed");
                            showAlert(Alert.AlertType.INFORMATION, "Time Synchronization has completed");
                        }
                ));
    }

    @FXML
    public void viewSync() {
        serialManager.sendCommand(MarineSensorCommand.DISPLAY_FREQ_OFFSET);
        subscriptions.add(baseSource.skipWhile(syncValue -> syncValue.getState() != SyncState.VIEW)
                .timeout(timeoutSeconds, TimeUnit.SECONDS)
                .take(1)
                .subscribe(syncValue -> LOG.debug("View mode entered"),
                        error -> {
                            if (error instanceof TimeoutException) {
                                showAlert(Alert.AlertType.ERROR, "Timeout occurred waiting for sync mode switch");
                            } else {
                                LOG.error(error);
                            }
                        },
                        () -> LOG.debug("View sync observable completed")
                ));
    }

    @FXML
    public void clearChart() {
        chart.clear();
    }

    @FXML
    public void setChartWidth() {
        try {
            long width = Long.parseLong(txtPlotWidth.getText().strip());
            if (width < 0)
                throw new NumberFormatException();
            chart.setChartWidth(width * 60);
        } catch (NumberFormatException ex) {
            LOG.warn("Invalid value supplied for plot width");
            txtPlotWidth.setText("" + chart.getChartWidth() / 60);
        }
    }

    private void sendAdjustment(int value) {
        serialManager.sendRawCommand(DataSource.GRAVITY, "e3_" + value + "\r\n");
    }

    private long getEpochNow() {
        return ZonedDateTime.now().toEpochSecond();
    }

    private void updateProperties(final SyncValue value) {
        final long time = ZonedDateTime.now().toEpochSecond();

        final int tDiff = value.getTimeDifference();
        final int fDeriv = value.getFreqDerivative();

        Platform.runLater(() -> {
            timeDifference.setValue(tDiff);
            frequencyDerivative.setValue(fDeriv);
            adjustmentSteps.set(value.getAdjustmentSteps());
            chart.push(timeSeries, time, tDiff);
            chart.push(freqSeries, time, fDeriv);
        });
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Platform.runLater(() -> {
            final Alert alert = new Alert(alertType, message);
            alert.initOwner(getStage().getScene().getWindow());
            alert.show();
        });
    }

    private void interpretStatus() {
        subscriptions.add(baseSource
                .distinctUntilChanged(SyncValue::getState)
                .subscribe(syncValue -> {
                    final SyncState state = syncValue.getState();
                    LOG.debug("Setting new Sync State value of: " + state);
                    Platform.runLater(() -> syncStateProperty.set(state));
                }));
    }

    @Override
    public void dispose() {
        LOG.info("Disposing of {}", getClass().getName());
        subscriptions.clear();
    }
}
