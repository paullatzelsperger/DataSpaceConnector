package org.eclipse.edc.spi.monitor;

public enum LogLevel {
    SEVERE(3), WARNING(2), INFO(1), DEBUG(0), NONE(-1);

    private final int value;

    LogLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
