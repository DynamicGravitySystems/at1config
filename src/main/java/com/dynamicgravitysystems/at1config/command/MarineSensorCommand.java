package com.dynamicgravitysystems.at1config.command;


public enum MarineSensorCommand implements SerialCommand {
    FIND_FREQ_OFFSET("e1\r\n"),
    DISPLAY_FREQ_OFFSET("e2\r\n"),
    CLAMP("c"),
    UNCLAMP("u");

    private final String command;

    MarineSensorCommand(String command) {
        this.command = command;
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public int length() {
        return command.length();
    }
}
