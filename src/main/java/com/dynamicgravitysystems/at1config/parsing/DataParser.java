package com.dynamicgravitysystems.at1config.parsing;

import com.dynamicgravitysystems.common.gravity.DataFieldSet;
import com.dynamicgravitysystems.common.gravity.GravityReading;
import com.dynamicgravitysystems.common.gravity.SensorCalibration;

import java.util.EnumMap;
import java.util.Map;


/**
 * The DataParser will maintain the global state of the application, whether it is configured for Airborne or Marine data
 * <p>
 * The user will manually specify the state through an option menu or button.
 * The Default state will be Marine, as that is the most common application
 * <p>
 * Depending on the state the raw data is parsed differently. All states however return an object which is a sub-class
 * of GravityReading i.e. MarineGravityReading or AirborneGravityReading
 * <p>
 * The SensorCalibration is common to all gravity readings, so does not necessarily need to be managed by the states
 */
public enum DataParser implements ParserState {
    INSTANCE;

    private final Map<State, ParserState> stateMap = new EnumMap<>(State.class);
    // TODO improve this logic
    private SensorCalibration calibration = SensorCalibration.identity(DataFieldSet.MARINE);
    private State currentState;

    {
        stateMap.put(State.MARINE, new MarineParserState());
        stateMap.put(State.AIRBORNE, new AirborneParserState());
    }

    public enum State {
        MARINE,
        AIRBORNE;
    }

    DataParser() {
        currentState = State.MARINE;
    }

    public void setCalibration(SensorCalibration calibration) {
        this.calibration = calibration;
    }

    public SensorCalibration getCalibration() {
        return calibration;
    }

    public GravityReading calibrate(GravityReading reading) {
        if (calibration != null)
            reading.calibrate(calibration);
        return reading;
    }

    @Override
    public GravityReading parse(String value) {
        return stateMap.get(currentState).parse(value);
    }

    public void setState(State state) {
        currentState = state;
    }

    public State getState() {
        return currentState;
    }
}
