package com.dynamicgravitysystems.at1config.util;

import com.dynamicgravitysystems.common.gravity.SensorCalibration;

import java.util.Optional;

public enum SharedState {
    STATE;

    private SensorCalibration calibration;

    SharedState() {
    }

    public void setCalibration(SensorCalibration calibration) {
        this.calibration = calibration;
    }

    public Optional<SensorCalibration> getCalibration() {
        return Optional.ofNullable(calibration);
    }
}
