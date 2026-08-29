package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.WitherStormMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockSapling;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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


public final class UpstreamItemTags {

    public static final String JUNK = "witherstormmod:junk";
    public static final String UNAPPETIZING = "witherstormmod:unappetizing";
    public static final String COMMAND_BLOCK_TOOLS = "witherstormmod:command_block_tools";
    public static final String CANNOT_FALL_IN_VOID = "witherstormmod:cannot_fall_in_void";
    public static final String CURE_INGREDIENT = "witherstormmod:cure_ingredient";
    public static final String CURE_BASE = "witherstormmod:cure_base";
    public static final String TAINTED_LOGS = "witherstormmod:tainted_logs";

    private static volatile Map<String, TagDefinition> definitions = Collections.emptyMap();
    private static volatile boolean initialized;

    private UpstreamItemTags() {
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
                    throw new IOException("Missing values in upstream item tag " + entryName);
                }
                List<String> values = new ArrayList<String>();
                for (JsonElement element : sourceValues) {
                    values.add(readTagValue(element, entryName));
                }
                loaded.put(tagName, new TagDefinition(
                        root.has("replace") && root.get("replace").getAsBoolean(), values));
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load item tags from the external Wither Storm archive",
                    exception);
        }
        requireTag(loaded, JUNK);
        requireTag(loaded, UNAPPETIZING);
        requireTag(loaded, COMMAND_BLOCK_TOOLS);
        requireTag(loaded, CANNOT_FALL_IN_VOID);
        requireTag(loaded, CURE_INGREDIENT);
        requireTag(loaded, CURE_BASE);
        requireTag(loaded, TAINTED_LOGS);
        definitions = Collections.unmodifiableMap(loaded);
        initialized = true;
        WitherStormMod.LOGGER.info("Indexed {} external upstream item tags", loaded.size());
    }

    public static boolean contains(String tagName, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ensureInitialized();
        return contains(tagName, stack, new HashSet<String>());
    }

    private static boolean contains(String tagName, ItemStack stack, Set<String> visiting) {
        if (!visiting.add(tagName)) return false;
        try {
            TagDefinition definition = definitions.get(tagName);
            if (definition != null) {
                for (String value : definition.values) {
                    if (value.startsWith("#")) {
                        if (contains(value.substring(1), stack, visiting)) return true;
                    } else if (matchesItemId(value, stack)) {
                        return true;
                    }
                }
                if (definition.replace) return false;
            }
            return matchesLegacyFamily(tagName, stack);
        } finally {
            visiting.remove(tagName);
        }
    }

    private static boolean matchesItemId(String sourceId, ItemStack stack) {
        Item item = stack.getItem();
        ResourceLocation registryName = Item.REGISTRY.getNameForObject(item);
        if (registryName == null) registryName = item.getRegistryName();
        if (registryName == null) return false;
        String legacyId = registryName.toString();
        if (sourceId.equals(legacyId)) return true;

        if ("minecraft:cocoa_beans".equals(sourceId)) {
            return item == Items.DYE && stack.getMetadata() == EnumDyeColor.BROWN.getDyeDamage();
        }
        if ("minecraft:ink_sac".equals(sourceId)) {
            return item == Items.DYE && stack.getMetadata() == EnumDyeColor.BLACK.getDyeDamage();
        }
        if ("minecraft:wither_skeleton_skull".equals(sourceId)) {
            return item == Item.getItemFromBlock(Blocks.SKULL) && stack.getMetadata() == 1;
        }
        if ("minecraft:bamboo".equals(sourceId)) return "futuremc:bamboo".equals(legacyId);
        if ("minecraft:wither_rose".equals(sourceId)) return "futuremc:wither_rose".equals(legacyId);
        return false;
    }

    private static boolean matchesLegacyFamily(String tagName, ItemStack stack) {
        Item item = stack.getItem();
        Block block = Block.getBlockFromItem(item);
        ResourceLocation registryName = Item.REGISTRY.getNameForObject(item);
        String itemId = registryName == null ? "" : registryName.toString();
        if ("minecraft:saplings".equals(tagName)) {
            return block instanceof BlockSapling || block == Blocks.SAPLING;
        }
        if ("minecraft:fishes".equals(tagName)) {
            return item == Items.FISH || item == Items.COOKED_FISH;
        }
        if ("minecraft:flowers".equals(tagName)) {
            return block instanceof BlockFlower || block == Blocks.DOUBLE_PLANT
                    || "futuremc:wither_rose".equals(itemId);
        }
        return false;
    }

    private static String tagName(String entryName) {
        if (!entryName.startsWith("data/") || !entryName.endsWith(".json")) return null;
        int separator = entryName.indexOf("/tags/items/", 5);
        if (separator < 0) return null;
        String namespace = entryName.substring(5, separator);
        String path = entryName.substring(separator + "/tags/items/".length(),
                entryName.length() - ".json".length());
        return namespace.isEmpty() || path.isEmpty() ? null : namespace + ':' + path;
    }

    private static String readTagValue(JsonElement element, String entryName) throws IOException {
        if (element.isJsonPrimitive()) return element.getAsString();
        if (!element.isJsonObject()) throw new IOException("Invalid value in upstream item tag " + entryName);
        JsonObject object = element.getAsJsonObject();
        JsonElement id = object.get("id");
        if (id == null || !id.isJsonPrimitive()) {
            throw new IOException("Missing id in upstream item tag value " + entryName);
        }
        return id.getAsString();
    }

    private static void requireTag(Map<String, TagDefinition> loaded, String tagName) {
        if (!loaded.containsKey(tagName)) {
            throw new IllegalStateException("Required upstream item tag is missing: " + tagName);
        }
    }

    private static void ensureInitialized() {
        if (!initialized) initialize();
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
