package com.dynamicgravitysystems.at1config.bindings;

import com.dynamicgravitysystems.common.gravity.DataField;
import com.dynamicgravitysystems.common.gravity.MarineDataField;
import com.dynamicgravitysystems.common.gravity.MarineGravityReading;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.EnumMap;
import java.util.Map;

public class MarineGravityReadingBinding {

    private final Map<MarineDataField, DoubleProperty> propertyMap = new EnumMap<>(MarineDataField.class);

    public MarineGravityReadingBinding() {
        for(MarineDataField field : MarineDataField.values()) {
            propertyMap.put(field, new SimpleDoubleProperty(0d));
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public void update(MarineGravityReading reading) {
        for(DataField field : reading.getFields()){
            propertyMap.get(field).set(reading.getValue(field));
        }
    }

    public DoubleProperty getProperty(MarineDataField field) {
        return propertyMap.get(field);
    }

}
