package com.dynamicgravitysystems.at1config.parsing;

import com.dynamicgravitysystems.common.gravity.GravityReading;

public interface ParserState {

    GravityReading parse(String value);

}
