package com.dynamicgravitysystems.at1config.controllers;

import com.dynamicgravitysystems.at1config.command.MarineSensorCommand;
import com.dynamicgravitysystems.at1config.services.SerialMessage;
import com.dynamicgravitysystems.at1config.services.SerialServiceManager;
import com.dynamicgravitysystems.at1config.util.BaseController;
import com.dynamicgravitysystems.at1config.util.SharedState;
import com.dynamicgravitysystems.common.gravity.DataField;
import com.dynamicgravitysystems.common.gravity.MarineGravityReading;
import com.dynamicgravitysystems.common.gravity.MarineSensorCalibration;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @FXML Label lblSyncStatus;

    @FXML Button btnStartSync;
    @FXML Button btnViewSync;

    @FXML CheckBox cbTimeDiff;
    @FXML CheckBox cbFreqDeriv;
    @FXML CheckBox cbElecTemp;

    @FXML LineChart<Long, Double> chartSync;
    @FXML NumberAxis chartXaxis;
    @FXML NumberAxis chartYaxis;

    private final IntegerProperty timeDifference = new SimpleIntegerProperty(0);
    private final IntegerProperty frequencyDerivative = new SimpleIntegerProperty(0);
    private final IntegerProperty adjustmentSteps = new SimpleIntegerProperty(0);
    private final DoubleProperty elecTemp = new SimpleDoubleProperty(0);
    private final BooleanProperty syncModeEnabled = new SimpleBooleanProperty(false);
    private final StringProperty syncStatus = new SimpleStringProperty("Not Synchronizing");
    private final StringProperty syncStatusStyle = new SimpleStringProperty("");

    private final SerialServiceManager serialManager = SerialServiceManager.getInstance();

    private final XYChart.Series<Long, Double> timeDiffSeries = new XYChart.Series<>();
    private final XYChart.Series<Long, Double> freqDerivativeSeries = new XYChart.Series<>();
    private final XYChart.Series<Long, Double> elecTempSeries = new XYChart.Series<>();

    private final List<io.reactivex.disposables.Disposable> subscriptions = new ArrayList<>();
    private final SharedState state = SharedState.STATE;

    private Observable<MarineGravityReading> baseSource;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!serialManager.isConnected(SerialServiceManager.SerialSource.GRAVITY))
            throw new IllegalStateException("TimeSyncController cannot be initialized if there is no Gravity source");

        txtTimeDiff.textProperty().bind(timeDifference.asString());
        txtFreqDeriv.textProperty().bind(frequencyDerivative.asString());
        txtAdjSteps.textProperty().bind(adjustmentSteps.asString());
        txtElecTemp.textProperty().bind(elecTemp.asString());
        lblSyncStatus.textProperty().bind(syncStatus);
        lblSyncStatus.styleProperty().bind(syncStatusStyle);
        btnViewSync.disableProperty().bind(syncModeEnabled);

        cbFreqDeriv.setOnAction(event -> {
            if (cbFreqDeriv.isSelected())
                chartSync.getData().add(freqDerivativeSeries);
            else
                chartSync.getData().remove(freqDerivativeSeries);
        });

        cbTimeDiff.setOnAction(event -> {
            if (cbTimeDiff.isSelected())
                chartSync.getData().add(timeDiffSeries);
            else
                chartSync.getData().remove(timeDiffSeries);
        });

        cbElecTemp.setOnAction(event -> {
            if (cbElecTemp.isSelected())
                chartSync.getData().add(elecTempSeries);
            else
                chartSync.getData().remove(elecTempSeries);
        });


        baseSource = serialManager.getSubject(SerialServiceManager.SerialSource.GRAVITY)
                .observeOn(Schedulers.io())
                .filter(serialMessage -> serialMessage.getEvent() == SerialMessage.SerialEvent.RECEIVED)
                .flatMap(value -> Observable.just(value.getValue())
                        .map(MarineGravityReading::fromString)
                        .doOnNext(reading -> {
                            final double elecTempValue = state.getCalibration()
                                    .orElse(MarineSensorCalibration.identity())
                                    .getFieldCalibration(DataField.ELEC_TEMP)
                                    .apply(reading.getValue(DataField.ELEC_TEMP));
                            Platform.runLater(() -> {
                                elecTemp.setValue(elecTempValue);
                                elecTempSeries.getData().add(new XYChart.Data<>(ZonedDateTime.now().toEpochSecond(), elecTempValue));
                            });
                        })
                        .onErrorResumeNext(Observable.empty()));

        // TODO: Need routine to calculate upper/lower bounds dynamically
        chartXaxis.setAutoRanging(false);
        long epochNow = ZonedDateTime.now().toEpochSecond();
        chartXaxis.setLowerBound(epochNow);
        chartXaxis.setUpperBound(epochNow + 1800);
        chartXaxis.setTickUnit(300);

        timeDiffSeries.setName("Time Difference");
        freqDerivativeSeries.setName("Frequency Derivative");

        // Turn off line point symbols
        chartSync.setCreateSymbols(false);
    }

    /**
     * Start the clock synchronization routine
     */
    @FXML
    public void startSync() {
        LOG.info("Beginning sync routine");
        syncStatus.setValue("Performing Synchronization");
        syncStatusStyle.setValue("-fx-text-fill: #33cc33; -fx-font-weight: bolder;");

        serialManager.sendCommand(MarineSensorCommand.FIND_FREQ_OFFSET);
        subscriptions.add(baseSource
                .skipWhile(reading -> reading.getIntValue(DataField.ADJUSTMENT_ENABLED) != 1)
                .timeout(timeoutSeconds, TimeUnit.SECONDS)
                .doOnNext(next -> Platform.runLater(() -> syncModeEnabled.set(true)))
                .takeUntil(reading -> reading.getIntValue(DataField.ADJUSTMENT_ENABLED) == 0)
                .subscribe(this::updateProperties,
                        error -> {
                            if (error instanceof TimeoutException) {
                                showAlert(Alert.AlertType.ERROR, "Timeout occurred waiting for sync mode switch");
                                Platform.runLater(() -> {
                                    syncStatus.setValue("Sync Command Timeout");
                                    syncStatusStyle.setValue("-fx-text-fill: #cc0000;");
                                });
                            } else {
                                LOG.error("Error in Sync observer", error);
                            }
                        },
                        () -> {
                            LOG.info("Sync has completed (state transitioned 1 -> 0");
                            Platform.runLater(() -> {
                                syncStatus.setValue("Synchronization Complete");
                            });
                            showAlert(Alert.AlertType.INFORMATION, "Synchronization Complete");
                        }));
    }

    @FXML
    public void viewSync() {
        syncStatus.setValue("Viewing Synchronization");
        syncStatusStyle.setValue("");
        serialManager.sendCommand(MarineSensorCommand.DISPLAY_FREQ_OFFSET);
        subscriptions.add(baseSource
                .skipWhile(reading -> reading.getIntValue(DataField.ADJUSTMENT_ENABLED) != 0)
                .timeout(timeoutSeconds, TimeUnit.SECONDS)
                .doOnNext(next -> Platform.runLater(() -> syncModeEnabled.set(true)))
                .subscribe(this::updateProperties,
                        error -> {
                            if (error instanceof TimeoutException)
                                showAlert(Alert.AlertType.ERROR, "Timeout occurred waiting for sync mode view switch");
                            Platform.runLater(() -> {
                                syncStatus.setValue("Sync Command Timeout");
                                syncStatusStyle.setValue("-fx-text-fill: #cc0000;");
                            });
                        }));
    }

    @FXML
    public void clearChart() {
        freqDerivativeSeries.getData().clear();
        timeDiffSeries.getData().clear();
        elecTempSeries.getData().clear();
    }

    private void updateProperties(final MarineGravityReading reading) {
        final long time = ZonedDateTime.now().toEpochSecond();
        final double tdiff = reading.getValue(DataField.TIME_DIFF);
        final double fderiv = reading.getValue(DataField.FREQ_DERIVATIVE);

        Platform.runLater(() -> {
            timeDifference.setValue((int) tdiff);
            frequencyDerivative.setValue((int) fderiv);
            adjustmentSteps.set(reading.getIntValue(DataField.ADJUSTMENT_STEPS));
            timeDiffSeries.getData().add(new XYChart.Data<>(time, tdiff));
            freqDerivativeSeries.getData().add(new XYChart.Data<>(time, fderiv));
        });
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Platform.runLater(() -> {
            new Alert(alertType, message).show();
        });
    }

    @Override
    public void dispose() {
        LOG.info("Disposing of {}", getClass().getName());
        for (Disposable sub : subscriptions) {
            sub.dispose();
        }
    }
}
