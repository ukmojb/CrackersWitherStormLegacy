package com.wdcftgg.witherstormmod.common.config;

import java.util.Locale;


public final class ConfiguredListMatcher {
    private ConfiguredListMatcher() {
    }

    public static boolean matches(String value, String[] entries) {
        if (value == null || entries == null) return false;
        String normalizedValue = normalize(value);
        if (normalizedValue.isEmpty()) return false;
        int separator = normalizedValue.indexOf(':');
        String namespace = separator < 0 ? "" : normalizedValue.substring(0, separator);
        for (String entry : entries) {
            String normalizedEntry = normalize(entry);
            if (normalizedEntry.isEmpty()) continue;
            if ("*".equals(normalizedEntry) || normalizedEntry.equals(normalizedValue)) return true;
            if (normalizedEntry.endsWith(":*")
                    && !namespace.isEmpty()
                    && normalizedEntry.equals(namespace + ":*")) return true;
        }
        return false;
    }

    public static boolean allows(String value, String[] entries, boolean whitelistMode) {
        return whitelistMode == matches(value, entries);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
