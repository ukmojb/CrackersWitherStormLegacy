package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class LootTableResourceConverter {

    public static final String LOOT_TABLE_PREFIX = "data/witherstormmod/loot_tables/";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> MUSIC_DISCS = Arrays.asList(
            "record_13", "record_cat", "record_blocks", "record_chirp", "record_far", "record_mall",
            "record_mellohi", "record_stal", "record_strad", "record_ward", "record_11", "record_wait");

    private LootTableResourceConverter() {
    }

    public static JsonObject convert(String sourceName, InputStream source) throws IOException {
        try {
            JsonObject sourceTable = JsonParser.parseReader(
                    new InputStreamReader(source, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray pools = new JsonArray();
            JsonArray sourcePools = required(sourceTable, "pools").getAsJsonArray();
            for (int poolIndex = 0; poolIndex < sourcePools.size(); poolIndex++) {
                JsonObject sourcePool = sourcePools.get(poolIndex).getAsJsonObject();
                JsonArray entries = new JsonArray();
                for (JsonElement sourceEntry : required(sourcePool, "entries").getAsJsonArray()) {
                    for (JsonElement convertedEntry : convertEntry(sourceEntry.getAsJsonObject())) {
                        entries.add(convertedEntry);
                    }
                }
                if (entries.size() == 0) continue;

                JsonObject pool = new JsonObject();
                pool.addProperty("name", "legacy_pool_" + poolIndex);
                pool.add("rolls", convertRange(required(sourcePool, "rolls")));
                pool.add("entries", entries);
                if (sourcePool.has("bonus_rolls")) {
                    pool.add("bonus_rolls", convertRange(sourcePool.get("bonus_rolls")));
                }
                if (sourcePool.has("conditions")) {
                    pool.add("conditions", convertConditions(sourcePool.getAsJsonArray("conditions")));
                }
                pools.add(pool);
            }
            JsonObject converted = new JsonObject();
            converted.add("pools", pools);
            return converted;
        } catch (RuntimeException exception) {
            throw new IOException("Unable to convert upstream loot table " + sourceName, exception);
        }
    }

    public static String serialize(JsonObject table) {
        return GSON.toJson(table);
    }

    private static List<JsonElement> convertEntry(JsonObject source) throws IOException {
        String type = unqualify(string(source, "type"));
        if ("tag".equals(type) && "minecraft:creeper_drop_music_discs".equals(string(source, "name"))) {
            JsonElement[] records = new JsonElement[MUSIC_DISCS.size()];
            for (int index = 0; index < MUSIC_DISCS.size(); index++) {
                JsonObject record = new JsonObject();
                record.addProperty("type", "item");
                record.addProperty("name", "minecraft:" + MUSIC_DISCS.get(index));
                record.addProperty("weight", 1);
                JsonArray conditions = new JsonArray();
                conditions.add(skeletonKillerCondition());
                record.add("conditions", conditions);
                records[index] = record;
            }
            return Arrays.asList(records);
        }
        if ("alternatives".equals(type)) {
            java.util.ArrayList<JsonElement> alternatives = new java.util.ArrayList<JsonElement>();
            for (JsonElement child : required(source, "children").getAsJsonArray()) {
                alternatives.addAll(convertEntry(child.getAsJsonObject()));
            }
            return alternatives;
        }
        if (!"item".equals(type)) {
            throw new IOException("Unsupported Minecraft 1.20 loot entry type " + type);
        }

        JsonObject converted = copyObject(source);
        converted.addProperty("type", "item");
        converted.remove("children");
        converted.remove("expand");
        String itemName = string(converted, "name");
        if ("minecraft:wither_rose".equals(itemName)) converted.addProperty("name", "futuremc:wither_rose");
        if ("minecraft:phantom_membrane".equals(itemName)) converted.addProperty("name", "minecraft:leather");
        if ("minecraft:crossbow".equals(itemName)) converted.addProperty("name", "crossbow:crossbow");
        if ("minecraft:suspicious_stew".equals(itemName)) converted.addProperty("name", "futuremc:suspicious_stew");
        if ("minecraft:glow_berries".equals(itemName)) converted.addProperty("name", "minecraft:melon");
        if ("minecraft:enchanted_golden_apple".equals(itemName)) {
            converted.addProperty("name", "minecraft:golden_apple");
            JsonArray functions = converted.has("functions")
                    ? converted.getAsJsonArray("functions") : new JsonArray();
            JsonObject setData = new JsonObject();
            setData.addProperty("function", "set_data");
            setData.addProperty("data", 1);
            functions.add(setData);
            converted.add("functions", functions);
        }
        if (converted.has("conditions")) {
            converted.add("conditions", convertConditions(converted.getAsJsonArray("conditions")));
        }
        if (converted.has("functions")) {
            converted.add("functions", convertFunctions(converted.getAsJsonArray("functions")));
        }
        return java.util.Collections.<JsonElement>singletonList(converted);
    }

    private static JsonArray convertConditions(JsonArray source) throws IOException {
        JsonArray conditions = new JsonArray();
        for (JsonElement value : source) {
            JsonObject converted = convertCondition(value.getAsJsonObject());
            if (converted != null) conditions.add(converted);
        }
        return conditions;
    }

    private static JsonObject convertCondition(JsonObject source) throws IOException {
        String type = unqualify(string(source, "condition"));
        if ("survives_explosion".equals(type) || "explosion_decay".equals(type)) return null;
        if ("entity_properties".equals(type)) {
            JsonObject predicate = source.has("predicate") && source.get("predicate").isJsonObject()
                    ? source.getAsJsonObject("predicate") : null;
            if (predicate != null && predicate.has("type")
                    && "#minecraft:skeletons".equals(predicate.get("type").getAsString())) {
                return skeletonKillerCondition();
            }
            JsonObject flags = predicate != null && predicate.has("flags") && predicate.get("flags").isJsonObject()
                    ? predicate.getAsJsonObject("flags") : null;
            if (flags != null && flags.has("is_on_fire")) {
                JsonObject converted = new JsonObject();
                converted.addProperty("condition", "entity_properties");
                converted.add("entity", copy(required(source, "entity")));
                JsonObject properties = new JsonObject();
                properties.add("on_fire", copy(flags.get("is_on_fire")));
                converted.add("properties", properties);
                return converted;
            }
        }
        if ("any_of".equals(type)) return null;
        JsonObject converted = copyObject(source);
        converted.addProperty("condition", type);
        converted.remove("predicate");
        return converted;
    }

    private static JsonArray convertFunctions(JsonArray source) throws IOException {
        JsonArray functions = new JsonArray();
        for (JsonElement value : source) {
            JsonObject converted = convertFunction(value.getAsJsonObject());
            if (converted != null) functions.add(converted);
        }
        return functions;
    }

    private static JsonObject convertFunction(JsonObject source) throws IOException {
        String type = unqualify(string(source, "function"));
        if ("explosion_decay".equals(type)) return null;
        if ("set_stew_effect".equals(type)) {
            JsonObject converted = copyObject(source);
            converted.addProperty("function", "witherstormmod:set_stew_effect");
            if (converted.has("conditions")) {
                converted.add("conditions", convertConditions(converted.getAsJsonArray("conditions")));
            }
            return converted;
        }
        JsonObject converted = copyObject(source);
        converted.addProperty("function", type);
        converted.remove("add");
        if (converted.has("count")) converted.add("count", convertRange(converted.get("count")));
        if (converted.has("levels")) converted.add("levels", convertRange(converted.get("levels")));
        if (converted.has("conditions")) {
            converted.add("conditions", convertConditions(converted.getAsJsonArray("conditions")));
        }
        return converted;
    }

    private static JsonElement convertRange(JsonElement source) {
        if (!source.isJsonObject()) return copy(source);
        JsonObject range = source.getAsJsonObject();
        if (!range.has("min") && !range.has("max")) return copy(source);
        JsonElement min = range.get("min");
        JsonElement max = range.get("max");
        if (min != null && max != null
                && new BigDecimal(min.getAsString()).compareTo(new BigDecimal(max.getAsString())) == 0) {
            return copy(min);
        }
        JsonObject converted = new JsonObject();
        if (min != null) converted.add("min", copy(min));
        if (max != null) converted.add("max", copy(max));
        return converted;
    }

    private static JsonObject skeletonKillerCondition() {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "witherstormmod:skeleton_killer");
        return condition;
    }

    private static String unqualify(String value) {
        return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
    }

    private static JsonElement required(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) throw new IOException("Missing loot table member " + key);
        return value;
    }

    private static String string(JsonObject object, String key) throws IOException {
        JsonElement value = required(object, key);
        if (!value.isJsonPrimitive()) throw new IOException("Loot table member " + key + " is not a string");
        return value.getAsString();
    }

    private static JsonObject copyObject(JsonObject value) {
        return value.deepCopy();
    }

    private static JsonElement copy(JsonElement value) {
        return value.deepCopy();
    }
}
