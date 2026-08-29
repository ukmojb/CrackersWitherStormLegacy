package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.common.init.ModRegistryNames;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class LanguageResourceConverter {

    private static final String PREFIX = "assets/witherstormmod/lang/";

    private LanguageResourceConverter() {
    }

    static boolean handles(String name) {
        return name.startsWith(PREFIX) && name.endsWith(".lang");
    }

    static String sourceName(String legacyName) {
        return legacyName.substring(0, legacyName.length() - ".lang".length()) + ".json";
    }

    static String englishSourceName() {
        return PREFIX + "en_us.json";
    }

    static byte[] convert(byte[] localizedSource, byte[] englishSource) {
        LinkedHashMap<String, String> english = convertSource(englishSource);
        addRegisteredFallbacks(english, english);

        LinkedHashMap<String, String> localized = convertSource(localizedSource);
        addRegisteredFallbacks(localized, english);

        StringBuilder output = new StringBuilder();
        for (Map.Entry<String, String> entry : localized.entrySet()) {
            output.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static LinkedHashMap<String, String> convertSource(byte[] source) {
        JsonObject translations = JsonParser.parseString(
                new String(source, StandardCharsets.UTF_8)).getAsJsonObject();
        LinkedHashMap<String, String> converted = new LinkedHashMap<String, String>();
        for (Map.Entry<String, JsonElement> entry : translations.entrySet()) {
            converted.put(entry.getKey(), entry.getValue().getAsString());
        }
        for (Map.Entry<String, JsonElement> entry : translations.entrySet()) {
            String key = entry.getKey();
            String legacyKey = null;
            if (key.startsWith("item.witherstormmod.")) {
                legacyKey = "item." + key.substring("item.witherstormmod.".length()) + ".name";
            } else if (key.startsWith("block.witherstormmod.")) {
                legacyKey = "tile." + key.substring("block.witherstormmod.".length()) + ".name";
            } else if (key.startsWith("entity.witherstormmod.")) {
                String entityName = key.substring("entity.witherstormmod.".length());
                converted.put("entity." + entityName + ".name", entry.getValue().getAsString());

                legacyKey = key + ".name";
            } else if (key.startsWith("effect.witherstormmod.")) {
                legacyKey = "potion." + key.substring("effect.witherstormmod.".length());
            } else if (key.startsWith("item.minecraft.potion.effect.")) {
                legacyKey = "potion.effect." + key.substring("item.minecraft.potion.effect.".length());
            } else if (key.startsWith("item.minecraft.splash_potion.effect.")) {
                legacyKey = "splash_potion.effect."
                        + key.substring("item.minecraft.splash_potion.effect.".length());
            } else if (key.startsWith("item.minecraft.lingering_potion.effect.")) {
                legacyKey = "lingering_potion.effect."
                        + key.substring("item.minecraft.lingering_potion.effect.".length());
            }
            if (legacyKey != null) {
                converted.put(legacyKey, entry.getValue().getAsString());
            }
        }
        if (!converted.containsKey("itemGroup.witherstormmod")) {
            String upstream = converted.get("itemGroup.wither_storm_mod");
            converted.put("itemGroup.witherstormmod",
                    upstream == null ? "Cracker's Wither Storm Legacy" : upstream);
        }
        return converted;
    }

    private static void addRegisteredFallbacks(LinkedHashMap<String, String> target,
                                               Map<String, String> english) {
        for (String name : ModRegistryNames.itemNames()) {
            addFallback(target, english, "item." + name + ".name", name);
        }
        for (String name : ModRegistryNames.blockNames()) {
            if (!ModRegistryNames.isItemlessBlock(name)) {
                addFallback(target, english, "tile." + name + ".name", name);
            }
        }
        if (!target.containsKey("itemGroup.witherstormmod")) {
            target.put("itemGroup.witherstormmod", english.get("itemGroup.witherstormmod"));
        }
    }

    private static void addFallback(LinkedHashMap<String, String> target, Map<String, String> english,
                                    String key, String registeredName) {
        if (target.containsKey(key)) {
            return;
        }
        String fallback = english.get(key);
        target.put(key, fallback == null ? humanize(registeredName) : fallback);
    }

    private static String humanize(String name) {
        StringBuilder result = new StringBuilder();
        for (String word : name.split("_")) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            result.append(word.substring(1));
        }
        return result.toString();
    }
}
