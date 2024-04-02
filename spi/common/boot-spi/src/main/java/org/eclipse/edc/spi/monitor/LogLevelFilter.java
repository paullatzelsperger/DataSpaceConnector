package org.eclipse.edc.spi.monitor;

import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Map;
import java.util.function.Supplier;

public class LogLevelFilter implements Monitor {
    private final ServiceExtensionContext context;
    private final Monitor wrappedMonitor;

    public LogLevelFilter(ServiceExtensionContext context, Monitor monitor) {
        this.context = context;
        this.wrappedMonitor = monitor;
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
            var value = context.getConfig().getString("edc.log.level", LogLevel.INFO.toString());

            var configuredLevel = LogLevel.valueOf(value);
            if (configuredLevel == LogLevel.NONE) {
                return false;
            }
            return configuredLevel.getValue() <= logLevel.getValue();
        } catch (NullPointerException | IllegalArgumentException e) {
            return true;
        }
    }
}
