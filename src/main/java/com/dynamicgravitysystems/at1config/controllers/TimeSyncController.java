package com.dynamicgravitysystems.at1config.controllers;

import com.dynamicgravitysystems.at1config.command.MarineSensorCommand;
import com.dynamicgravitysystems.at1config.parsing.DataParser;
import com.dynamicgravitysystems.at1config.services.SerialMessage;
import com.dynamicgravitysystems.at1config.services.SerialServiceManager;
import com.dynamicgravitysystems.at1config.util.BaseController;
import com.dynamicgravitysystems.at1config.util.DataSource;
import com.dynamicgravitysystems.common.filters.Filter;
import com.dynamicgravitysystems.common.filters.MovingAverageFilter;
import com.dynamicgravitysystems.common.gravity.DataField;
import com.dynamicgravitysystems.common.gravity.GravityReading;
import com.dynamicgravitysystems.common.gravity.MarineGravityReading;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    @FXML LineChart<Long, Double> chartSync;
    @FXML NumberAxis chartXaxis;
    @FXML NumberAxis chartYaxis;

    private enum SyncState {
        DATA("Data Mode (Default)"),
        VIEW("Viewing Synchronization"),
        SYNC("Auto-Sync Active");

        private final String displayValue;

        SyncState(String displayValue) {
            this.displayValue = displayValue;
        }

        @Override
        public String toString() {
            return displayValue;
        }
    }

    private static class SyncValue {
        private final int timeDifference;
        private final int freqDerivative;
        private final int adjustmentSteps;
        private final double elecTemperature;
        private final SyncState state;


        private SyncValue(int timeDifference, int freqDerivative, int adjustmentSteps, double elecTemperature, SyncState state) {
            this.timeDifference = timeDifference;
            this.freqDerivative = freqDerivative;
            this.adjustmentSteps = adjustmentSteps;
            this.elecTemperature = elecTemperature;
            this.state = state;
        }

        static SyncValue fromGravityReading(GravityReading reading) {

            int adjStateValue = (int) reading.getValue(DataField.ADJUSTMENT_ENABLED);
            SyncState syncState;
            switch (adjStateValue) {
                case 0:
                    syncState = SyncState.VIEW;
                    break;
                case 1:
                    syncState = SyncState.SYNC;
                    break;
                default:
                    syncState = SyncState.DATA;
            }

            return new SyncValue((int) reading.getValue(DataField.TIME_DIFF, -1),
                    (int) reading.getValue(DataField.FREQ_DERIVATIVE, -1),
                    (int) reading.getValue(DataField.ADJUSTMENT_STEPS, -1),
                    reading.getValue(DataField.ELEC_TEMP),
                    syncState);
        }
    }


    private final ObjectProperty<SyncState> syncStateProperty = new SimpleObjectProperty<>(SyncState.DATA);

    private final IntegerProperty timeDifference = new SimpleIntegerProperty(0);
    private final IntegerProperty frequencyDerivative = new SimpleIntegerProperty(0);
    private final IntegerProperty adjustmentSteps = new SimpleIntegerProperty(0);
    private final DoubleProperty elecTemp = new SimpleDoubleProperty(0);
    private final LongProperty chartWidth = new SimpleLongProperty(1800);

    private final Filter temperatureFilter = new MovingAverageFilter(200);

    private final SerialServiceManager serialManager = SerialServiceManager.getInstance();
    private final DataParser processor = DataParser.INSTANCE;

    private final XYChart.Series<Long, Double> timeDiffSeries = new XYChart.Series<>();
    private final XYChart.Series<Long, Double> freqDerivativeSeries = new XYChart.Series<>();
    private final XYChart.Series<Long, Double> elecTempSeries = new XYChart.Series<>();

    private final CompositeDisposable subscriptions = new CompositeDisposable();

    private ConnectableObservable<SyncValue> baseSource;


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

        chartSync.getData().add(timeDiffSeries);
        cbTimeDiff.setSelected(true);
        chartSync.getData().add(freqDerivativeSeries);
        cbFreqDeriv.setSelected(true);
        chartSync.getData().add(elecTempSeries);
        cbElecTemp.setSelected(true);

        cbFreqDeriv.setOnAction(event -> toggleSeries(freqDerivativeSeries.getName()));
        cbTimeDiff.setOnAction(event -> toggleSeries(timeDiffSeries.getName()));
        cbElecTemp.setOnAction(event -> toggleSeries(elecTempSeries.getName()));

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
                                            .apply(sv.elecTemperature));
                            Platform.runLater(() -> {
                                elecTemp.setValue(elecTempValue);
                                elecTempSeries.getData().add(new XYChart.Data<>(getEpochNow(), elecTempValue));
                                updateChartBounds();
                            });
                        })
                        .doOnError(error -> LOG.error("Error parsing marine gravity reading, reason: {}", error.getMessage()))
                        .onErrorResumeNext(Observable.empty())).publish();

        interpretStatus();

        subscriptions.add(baseSource.skipWhile(reading -> reading.state == SyncState.DATA).subscribe(
                this::updateProperties
        ));

        baseSource.connect();

        // TODO: Need routine to calculate upper/lower bounds dynamically
        chartXaxis.setAutoRanging(false);
        updateChartBounds();
        chartXaxis.setTickUnit(300);
        chartXaxis.setTickLabelFormatter(new StringConverter<>() {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            @Override
            public String toString(Number object) {
                return Instant.ofEpochSecond(object.longValue()).atZone(ZoneOffset.UTC).format(formatter);
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        });

        chartSync.setCreateSymbols(false);
        timeDiffSeries.setName("Time Difference");
        freqDerivativeSeries.setName("Frequency Derivative");
        elecTempSeries.setName("Electronics Temperature");

        // TODO: Implement drag-zoom
//        chartSync.setOnMouseMoved(mouseEvent -> {
//            System.out.println("Y pos: " + mouseEvent.getY());
//            System.out.println("Y Value: " + chartYaxis.getValueForDisplay(mouseEvent.getY() - chartSync.getBaselineOffset()));
//
//        });
    }

    /**
     * Start the clock synchronization routine
     */
    @FXML
    public void startSync() {
        LOG.info("Beginning sync routine");

        serialManager.sendCommand(MarineSensorCommand.FIND_FREQ_OFFSET);
        subscriptions.add(baseSource.skipWhile(syncValue -> syncValue.state != SyncState.SYNC)
                .timeout(timeoutSeconds, TimeUnit.SECONDS)
                .takeUntil(syncValue -> syncValue.state == SyncState.VIEW)
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
        subscriptions.add(baseSource.skipWhile(syncValue -> syncValue.state != SyncState.VIEW)
                .timeout(timeoutSeconds, TimeUnit.SECONDS)
                .take(1)
                .subscribe(syncValue -> LOG.debug("View mode entered"),
                        error -> {
                            if (error instanceof TimeoutException) {
                                showAlert(Alert.AlertType.ERROR, "Timeout occured waiting for sync mode switch");
                            } else {
                                LOG.error(error);
                            }
                        },
                        () -> LOG.debug("View sync observable completed")
                ));
    }

    @FXML
    public void clearChart() {
        freqDerivativeSeries.getData().clear();
        timeDiffSeries.getData().clear();
        elecTempSeries.getData().clear();
    }

    @FXML
    public void setChartWidth() {
        try {
            long width = Long.parseLong(txtPlotWidth.getText().strip());
            if (width < 0)
                throw new NumberFormatException();
            chartWidth.set(width * 60);
        } catch (NumberFormatException ex) {
            LOG.warn("Invalid value supplied for plot width");
            txtPlotWidth.setText("" + (chartWidth.getValue() / 60));
        }

    }

    private void toggleSeries(String seriesName) {
        for (XYChart.Series<Long, Double> series : chartSync.getData()) {
            if (series.getName().equals(seriesName)) {
                series.getNode().setVisible(!series.getNode().isVisible());
                break;
            }
        }
    }

    private void sendAdjustment(int value) {
        serialManager.sendRawCommand(DataSource.GRAVITY, "e3_" + value + "\r\n");
    }

    private void updateChartBounds() {
        long now = getEpochNow();
        chartXaxis.setLowerBound(now - chartWidth.get());
        chartXaxis.setUpperBound(now + 1);
    }

    private long getEpochNow() {
        return ZonedDateTime.now().toEpochSecond();
    }

    private void updateProperties(final SyncValue value) {
        final long time = ZonedDateTime.now().toEpochSecond();

        final int tDiff = value.timeDifference;
        final int fDeriv = value.freqDerivative;

        Platform.runLater(() -> {
            timeDifference.setValue(tDiff);
            frequencyDerivative.setValue(fDeriv);
            adjustmentSteps.set(value.adjustmentSteps);
            timeDiffSeries.getData().add(new XYChart.Data<>(time, (double) tDiff));
            freqDerivativeSeries.getData().add(new XYChart.Data<>(time, (double) fDeriv));
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
        subscriptions.add(baseSource.subscribe(syncValue -> {
            if (syncValue.state != syncStateProperty.get()) {
                LOG.debug("Setting new Sync State value of: " + syncValue.state);
                Platform.runLater(() -> syncStateProperty.set(syncValue.state));
            }
        }));
    }

    @Override
    public void dispose() {
        LOG.info("Disposing of {}", getClass().getName());
        subscriptions.clear();
    }
}
