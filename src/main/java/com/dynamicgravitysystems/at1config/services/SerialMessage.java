package com.dynamicgravitysystems.at1config.services;

public class SerialMessage {

    public enum SerialEvent {
        CONNECTED,
        FAILED,
        TIMEOUT,
        RECEIVED,
        DISCONNECTED
    }

    private final SerialEvent event;
    private final String value;

    private SerialMessage(SerialEvent event) {
        this(event, null);
    }

    private SerialMessage(SerialEvent event, String value) {
        this.event = event;
        this.value = value;
    }

    public SerialEvent getEvent() {
        return event;
    }

    public String getValue() {
        return value;
    }

    public boolean hasValue() {
        return event == SerialEvent.RECEIVED;
    }

    public static SerialMessage ofValue(String value) {
        return new SerialMessage(SerialEvent.RECEIVED, value);
    }

    public static SerialMessage timeout() {
        return new SerialMessage(SerialEvent.TIMEOUT);
    }

    static SerialMessage connected() {
        return new SerialMessage(SerialEvent.CONNECTED);
    }

    static SerialMessage failed() {
        return new SerialMessage(SerialEvent.FAILED);
    }

    static SerialMessage disconnected() {
        return new SerialMessage(SerialEvent.DISCONNECTED);
    }

}
