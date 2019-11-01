package com.dynamicgravitysystems.at1config.util;

import com.dynamicgravitysystems.common.gravity.MarineSensorCalibration;

import java.util.Optional;

public enum SharedState {
    STATE;

    private MarineSensorCalibration calibration;

    SharedState() {}

    public void setCalibration(MarineSensorCalibration calibration) {
        this.calibration = calibration;
    }

    public Optional<MarineSensorCalibration> getCalibration() {
        return Optional.ofNullable(calibration);
    }
}
