package com.wdcftgg.witherstormmod.common.util;

import com.wdcftgg.witherstormmod.WitherStormMod;

/** 统一控制风暴诊断日志，默认关闭并由管理指令显式开启。 */
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
