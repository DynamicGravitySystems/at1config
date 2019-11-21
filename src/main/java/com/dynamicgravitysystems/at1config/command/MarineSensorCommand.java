package com.dynamicgravitysystems.at1config.command;


public enum MarineSensorCommand implements SerialCommand {
    FIND_FREQ_OFFSET("e1\r\n"),
    DISPLAY_FREQ_OFFSET("e2\r\n"),
    CLAMP("c"),
    UNCLAMP("u"),
    SET_CLAMP_LIMITS("l"),
    STOP_CLAMP_MOTOR("s"),
    FEEDBACK_ON("f"),
    FEEDBACK_OFF("o");

    private final String command;

    MarineSensorCommand(String command) {
        this.command = command;
    }

    @Override
    public String getCommand() {
        return command;
    }

}
