package com.dynamicgravitysystems.at1config.parsing;

import com.dynamicgravitysystems.common.gravity.GravityReading;
import com.dynamicgravitysystems.common.gravity.MarineGravityReading;

public class MarineParserState implements ParserState {

    @Override
    public GravityReading parse(String value) {
        return MarineGravityReading.fromString(value);
    }
}
