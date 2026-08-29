package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.WitherStormMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class UpstreamEntityTags {

    public static final String WITHER_STORM_TARGETING_BLACKLIST =
            "witherstormmod:wither_storm_targeting_blacklist";
    public static final String FAVOURABLE_MOBS = "witherstormmod:favourable_mobs";
    public static final String HIGH_IMMUNITY = "witherstormmod:high_immunity";
    public static final String SICKENED_MOBS = "witherstormmod:sickened_mobs";
    public static final String WITHER_SICKNESS_IMMUNE = "witherstormmod:wither_sickness_immune";

    private static volatile Map<String, TagDefinition> definitions = Collections.emptyMap();
    private static volatile boolean initialized;

    private UpstreamEntityTags() {
    }

    public static synchronized void initialize() {
        Map<String, TagDefinition> loaded = new LinkedHashMap<String, TagDefinition>();
        try {
            for (String entryName : UpstreamResourceArchive.listEntries("data/", ".json")) {
                String tagName = tagName(entryName);
                if (tagName == null) continue;
                JsonObject root;
                try (InputStream stream = UpstreamResourceArchive.open(entryName);
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }
                JsonArray sourceValues = root.getAsJsonArray("values");
                if (sourceValues == null) {
                    throw new IOException("Missing values in upstream entity tag " + entryName);
                }
                List<String> values = new ArrayList<String>();
                for (JsonElement element : sourceValues) {
                    values.add(readTagValue(element, entryName));
                }
                loaded.put(tagName, new TagDefinition(
                        root.has("replace") && root.get("replace").getAsBoolean(), values));
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load entity tags from the external Wither Storm archive",
                    exception);
        }
        requireTag(loaded, WITHER_STORM_TARGETING_BLACKLIST);
        requireTag(loaded, FAVOURABLE_MOBS);
        requireTag(loaded, HIGH_IMMUNITY);
        requireTag(loaded, SICKENED_MOBS);
        requireTag(loaded, WITHER_SICKNESS_IMMUNE);
        definitions = Collections.unmodifiableMap(loaded);
        initialized = true;
        WitherStormMod.LOGGER.info("Indexed {} external upstream entity tags", loaded.size());
    }

    public static boolean contains(String tagName, Entity entity) {
        if (entity == null) return false;
        ResourceLocation id = entity instanceof EntityPlayer
                ? new ResourceLocation("minecraft", "player") : EntityList.getKey(entity);
        if (id == null) return false;
        ensureInitialized();
        return contains(tagName, id.toString(), new HashSet<String>());
    }

    private static boolean contains(String tagName, String entityId, Set<String> visiting) {
        if (!visiting.add(tagName)) return false;
        try {
            TagDefinition definition = definitions.get(tagName);
            if (definition == null) return false;
            for (String value : definition.values) {
                if (value.startsWith("#")) {
                    if (contains(value.substring(1), entityId, visiting)) return true;
                } else if (value.equals(entityId)) {
                    return true;
                }
            }
            return false;
        } finally {
            visiting.remove(tagName);
        }
    }

    private static String tagName(String entryName) {
        if (!entryName.startsWith("data/") || !entryName.endsWith(".json")) return null;
        int separator = entryName.indexOf("/tags/entity_types/", 5);
        if (separator < 0) return null;
        String namespace = entryName.substring(5, separator);
        String path = entryName.substring(separator + "/tags/entity_types/".length(),
                entryName.length() - ".json".length());
        return namespace.isEmpty() || path.isEmpty() ? null : namespace + ':' + path;
    }

    private static String readTagValue(JsonElement element, String entryName) throws IOException {
        if (element.isJsonPrimitive()) return element.getAsString();
        if (!element.isJsonObject()) throw new IOException("Invalid value in upstream entity tag " + entryName);
        JsonObject object = element.getAsJsonObject();
        JsonElement id = object.get("id");
        if (id == null || !id.isJsonPrimitive()) {
            throw new IOException("Missing id in upstream entity tag value " + entryName);
        }
        return id.getAsString();
    }

    private static void ensureInitialized() {
        if (!initialized) initialize();
    }

    private static void requireTag(Map<String, TagDefinition> loaded, String tagName) {
        if (!loaded.containsKey(tagName)) {
            throw new IllegalStateException("Required upstream entity tag is missing: " + tagName);
        }
    }

    private static final class TagDefinition {
        private final boolean replace;
        private final List<String> values;

        private TagDefinition(boolean replace, List<String> values) {
            this.replace = replace;
            this.values = Collections.unmodifiableList(new ArrayList<String>(values));
        }
    }
}
