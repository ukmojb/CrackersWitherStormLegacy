package com.wdcftgg.witherstormmod.common.util;

import com.wdcftgg.witherstormmod.WitherStormMod;


public final class StormDiagnosticLogger {
    private static volatile boolean enabled;

    private StormDiagnosticLogger() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        StormDiagnosticLogger.enabled = enabled;
    }

    public static void info(String message, Object... arguments) {
        if (enabled) WitherStormMod.LOGGER.info(message, arguments);
    }

    public static void warn(String message, Object... arguments) {
        if (enabled) WitherStormMod.LOGGER.warn(message, arguments);
    }
}
