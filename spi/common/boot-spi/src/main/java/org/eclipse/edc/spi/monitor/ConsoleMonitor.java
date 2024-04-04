/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.spi.monitor;

import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Default monitor implementation. Outputs messages to the console.
 */
public class ConsoleMonitor implements Monitor {

    private static final String SEVERE = "SEVERE";
    private static final String WARNING = "WARNING";
    private static final String INFO = "INFO";
    private static final String DEBUG = "DEBUG";

    private final boolean useColor;

    private final LogLevel level;
    private final String prefix;

    public ConsoleMonitor() {
        this(true);
    }

    public ConsoleMonitor(boolean useColor) {
        this(null, LogLevel.DEBUG, useColor);
    }

    public ConsoleMonitor(boolean useColor, LogLevel level) {
        this(null, level, useColor);
    }

    public ConsoleMonitor(@Nullable String runtimeName, LogLevel level) {
        this(runtimeName, level, true);
    }

    public ConsoleMonitor(@Nullable String runtimeName, LogLevel level, boolean useColor) {
        this.prefix = runtimeName == null ? "" : "[%s] ".formatted(runtimeName);
        this.level = level;
        this.useColor = useColor;
    }

    @Override
    public void severe(Supplier<String> supplier, Throwable... errors) {
        output(SEVERE, supplier, errors);
    }

    @Override
    public void warning(Supplier<String> supplier, Throwable... errors) {
        if (LogLevel.WARNING.getValue() < level.getValue()) {
            return;
        }
        output(WARNING, supplier, errors);
    }

    @Override
    public void info(Supplier<String> supplier, Throwable... errors) {
        if (LogLevel.INFO.getValue() < level.getValue()) {
            return;
        }
        output(INFO, supplier, errors);
    }

    @Override
    public void debug(Supplier<String> supplier, Throwable... errors) {
        if (LogLevel.DEBUG.getValue() < level.getValue()) {
            return;
        }
        output(DEBUG, supplier, errors);
    }

    private void output(String level, Supplier<String> supplier, Throwable... errors) {
        var time = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        var colorCode = useColor ? getColorCode(level) : "";
        var resetCode = useColor ? ConsoleColor.RESET : "";

        System.out.println(colorCode + prefix + level + " " + time + " " + sanitizeMessage(supplier) + resetCode);
        if (errors != null) {
            for (var error : errors) {
                if (error != null) {
                    System.out.print(colorCode);
                    error.printStackTrace(System.out);
                    System.out.print(resetCode);
                }
            }
        }
    }

    private String getColorCode(String level) {
        return switch (level) {
            case SEVERE -> ConsoleColor.RED;
            case WARNING -> ConsoleColor.YELLOW;
            case INFO -> ConsoleColor.GREEN;
            case DEBUG -> ConsoleColor.BLUE;
            default -> "";
        };
    }

}
