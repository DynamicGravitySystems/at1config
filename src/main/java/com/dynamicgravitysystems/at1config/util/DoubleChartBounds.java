package com.dynamicgravitysystems.at1config.util;

public class DoubleChartBounds {

    private double upperBound = Double.MIN_VALUE;
    private double upperBoundPad = .001;
    private double lowerBound = Double.MAX_VALUE;
    private double lowerBoundPad = .001;

    private double tickUnit = 100;

    public DoubleChartBounds() {
    }

    public void update(final double value) {

        this.upperBound = Math.max(upperBound, value);
        this.lowerBound = Math.min(lowerBound, value);

        tickUnit = (getUpperBound() - getLowerBound()) / 10;

    }

    public double getUpperBound() {
        return upperBound + (upperBound * upperBoundPad);
    }

    public double getLowerBound() {
        return lowerBound - (lowerBound * lowerBoundPad);
    }

    public double getTickUnit() {
        return tickUnit;
    }
}
