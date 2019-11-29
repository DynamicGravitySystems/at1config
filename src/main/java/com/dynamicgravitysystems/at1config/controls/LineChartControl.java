package com.dynamicgravitysystems.at1config.controls;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

public class LineChartControl extends LineChart<Number, Number> {

    public static class TimestampConverter extends StringConverter<Number> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        @Override
        public String toString(Number object) {
            return Instant.ofEpochSecond(object.longValue()).atZone(ZoneOffset.UTC).format(formatter);
        }

        @Override
        public Number fromString(String string) {
            return null;
        }
    }


    private final DoubleProperty chartWidth = new SimpleDoubleProperty(1800);
    private Map<String, XYChart.Series<Number, Number>> seriesMap = new WeakHashMap<>();


    public LineChartControl() {
        super(new NumberAxis(), new NumberAxis());

        setCreateSymbols(false);
        setAnimated(false);
        getXAxis().setAutoRanging(false);
        getXAxis().setTickUnit(300);
        getXAxis().setTickLabelFormatter(new TimestampConverter());

        // TODO: Implement drag-zoom
//        setOnMouseMoved(mouseEvent -> {
//            System.out.println("Y pos: " + mouseEvent.getY());
//            System.out.println("Y Value: " + chartYaxis.getValueForDisplay(mouseEvent.getY() - chartSync.getBaselineOffset()));
//
//        });
    }

    public void toggleSeries(final String name) {
        getData().stream()
                .filter(series -> series.getName().equals(name))
                .findFirst()
                .ifPresent(series -> series.getNode().setVisible(!series.getNode().isVisible()));
    }

    public void addSeries(String... name) {
        if (!Platform.isFxApplicationThread())
            throw new IllegalStateException("addSeries must be executed on FX Application Thread");
        Arrays.stream(name).forEach(seriesName -> {
            final XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(seriesName);
            getData().add(series);
            seriesMap.put(seriesName, series);
        });

    }

    public void removeSeries(final String name) {
        getData().removeIf(series -> series.getName().equals(name));
    }

    public void push(final String name, final Number x, final Number y) {
        final XYChart.Series<Number, Number> series;
        if ((series = seriesMap.get(name)) != null) {
            series.getData().add(new XYChart.Data<>(x, y));
            updateXAxisBounds();
            series.getData().removeIf(data -> data.getXValue().doubleValue() < getXAxis().getLowerBound());
        }
    }

    public void clear() {
        seriesMap.values().forEach(series -> series.getData().clear());
    }

    public void setChartWidth(final double width) {
        chartWidth.set(width);
    }

    /**
     * Returns the width of the plot (x-axis) in seconds
     */
    public double getChartWidth() {
        return chartWidth.get();
    }

    private void updateXAxisBounds() {
        final long now = ZonedDateTime.now().toEpochSecond();

        getXAxis().setLowerBound(now - chartWidth.get());
        getXAxis().setUpperBound(now + 1);
    }

    @Override
    public NumberAxis getXAxis() {
        return (NumberAxis) super.getXAxis();
    }
}
