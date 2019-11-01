package com.dynamicgravitysystems.at1config.bindings;

import com.dynamicgravitysystems.common.gravity.DataField;
import com.dynamicgravitysystems.common.gravity.MarineGravityReading;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.EnumMap;
import java.util.Map;

public class MarineGravityReadingBinding {

    private final Map<DataField, DoubleProperty> propertyMap = new EnumMap<>(DataField.class);

    public MarineGravityReadingBinding() {
        for (DataField field : DataField.values()) {
            propertyMap.put(field, new SimpleDoubleProperty(0d));
        }
    }

    public void update(MarineGravityReading reading) {
        for (DataField field : reading.getFields()) {
            propertyMap.get(field).set(reading.getValue(field));
        }
    }

    public DoubleProperty getProperty(DataField field) {
        return propertyMap.get(field);
    }

}
