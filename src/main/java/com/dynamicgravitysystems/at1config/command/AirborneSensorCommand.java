package com.dynamicgravitysystems.at1config.command;

public enum AirborneSensorCommand implements SerialCommand {
    FIND_FREQ_OFFSET("e\n"),
    DISPLAY_FREQ_OFFSET("g\n");

    private final String command;

    AirborneSensorCommand(String command) {
        this.command = command;
    }

    @Override
    public String getCommand() {
        return command;
    }

}
