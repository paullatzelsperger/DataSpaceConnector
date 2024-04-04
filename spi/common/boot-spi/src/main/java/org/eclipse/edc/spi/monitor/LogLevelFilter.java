/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.monitor;

import java.util.Map;
import java.util.function.Supplier;

public class LogLevelFilter implements Monitor {
    private final Monitor wrappedMonitor;
    private final LogLevel minimumLogLevel;

    public LogLevelFilter(Monitor monitor) {
        this(monitor, LogLevel.INFO);
    }

    public LogLevelFilter(Monitor monitor, LogLevel level) {
        this.wrappedMonitor = monitor;
        this.minimumLogLevel = level;
    }

    @Override
    public void severe(Supplier<String> supplier, Throwable... errors) {
        if (isEnabled(LogLevel.SEVERE)) {
            wrappedMonitor.severe(supplier, errors);
        }
    }

    @Override
    public void severe(String message, Throwable... errors) {
        if (isEnabled(LogLevel.SEVERE)) {
            wrappedMonitor.severe(message, errors);
        }
    }

    @Override
    public void severe(Map<String, Object> data) {
        if (isEnabled(LogLevel.SEVERE)) {
            wrappedMonitor.severe(data);
        }
    }

    @Override
    public void warning(Supplier<String> supplier, Throwable... errors) {
        if (isEnabled(LogLevel.WARNING)) {
            wrappedMonitor.warning(supplier, errors);
        }
    }

    @Override
    public void warning(String message, Throwable... errors) {
        if (isEnabled(LogLevel.WARNING)) {
            wrappedMonitor.warning(message, errors);
        }
    }

    @Override
    public void info(Supplier<String> supplier, Throwable... errors) {
        if (isEnabled(LogLevel.INFO)) {
            wrappedMonitor.info(supplier, errors);
        }
    }

    @Override
    public void info(String message, Throwable... errors) {
        if (isEnabled(LogLevel.INFO)) {
            wrappedMonitor.info(message, errors);
        }
    }

    @Override
    public void debug(Supplier<String> supplier, Throwable... errors) {
        if (isEnabled(LogLevel.DEBUG)) {
            wrappedMonitor.debug(supplier, errors);
        }
    }

    @Override
    public void debug(String message, Throwable... errors) {
        if (isEnabled(LogLevel.DEBUG)) {
            wrappedMonitor.debug(message, errors);
        }
    }

    private boolean isEnabled(LogLevel logLevel) {
        try {
            if (minimumLogLevel == LogLevel.NONE) {
                return false;
            }
            return minimumLogLevel.getValue() <= logLevel.getValue();
        } catch (NullPointerException | IllegalArgumentException e) {
            return true;
        }
    }
}
