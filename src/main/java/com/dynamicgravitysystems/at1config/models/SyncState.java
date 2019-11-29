package com.dynamicgravitysystems.at1config.models;

public enum SyncState {
    DATA("Data Mode (Default)"),
    VIEW("Viewing Synchronization"),
    SYNC("Auto-Sync Active");

    private final String displayValue;

    SyncState(String displayValue) {
        this.displayValue = displayValue;
    }

    @Override
    public String toString() {
        return displayValue;
    }
}
