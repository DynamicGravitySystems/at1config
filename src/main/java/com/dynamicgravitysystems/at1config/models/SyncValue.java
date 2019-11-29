package com.dynamicgravitysystems.at1config.models;

import com.dynamicgravitysystems.common.gravity.DataField;
import com.dynamicgravitysystems.common.gravity.GravityReading;

public class SyncValue {
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

    public static SyncValue fromGravityReading(GravityReading reading) {

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

    public int getTimeDifference() {
        return timeDifference;
    }

    public int getFreqDerivative() {
        return freqDerivative;
    }

    public int getAdjustmentSteps() {
        return adjustmentSteps;
    }

    public double getElecTemperature() {
        return elecTemperature;
    }

    public SyncState getState() {
        return state;
    }
}
