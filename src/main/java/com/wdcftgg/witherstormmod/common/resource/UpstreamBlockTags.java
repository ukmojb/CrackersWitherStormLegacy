package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.WitherStormMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockPressurePlateWeighted;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.BlockSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockTrapDoor;

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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the upstream data-pack block tags without copying ARR data into this mod.
 * Vanilla and Forge tag families are matched with their Minecraft 1.12.2 semantics.
 */
public final class UpstreamBlockTags {

    private static final int BLOCK_STATE_CACHE_SIZE = 1 << 16;
    private static final byte CACHE_UNKNOWN = 0;
    private static final byte CACHE_MISS = 1;
    private static final byte CACHE_MATCH = 2;

    public static final String WITHER_STORM_BLOCK_BLACKLIST =
            "witherstormmod:wither_storm_block_blacklist";
    public static final String LESS_FAVORABLE_BLOCKS_HUNCH =
            "witherstormmod:less_favorable_blocks_hunch";
    public static final String NATURE_CLUSTER_WHITELIST =
            "witherstormmod:wither_storm_nature_cluster_whitelist";
    public static final String SMALL_CLUSTER_BLACKLIST =
            "witherstormmod:wither_storm_small_cluster_blacklist";
    public static final String LESS_FAVORABLE_BLOCKS =
            "witherstormmod:less_favorable_blocks";
    public static final String TRACTOR_BEAM_DISTRACTION_BLOCKS =
            "witherstormmod:tractor_beam_distraction_blocks";
    public static final String BEACONS = "witherstormmod:beacons";
    public static final String WITHERED_BEACON_BASE = "witherstormmod:withered_beacon_base";
    public static final String AQUA_SUPPORT_BASE = "witherstormmod:aqua_support_base";
    public static final String GREEN_SUPPORT_BASE = "witherstormmod:green_support_base";
    public static final String GRAY_SUPPORT_BASE = "witherstormmod:gray_support_base";
    public static final String RED_SUPPORT_BASE = "witherstormmod:red_support_base";
    public static final String BLOCK_CLUSTERS_CANNOT_PLACE =
            "witherstormmod:block_clusters_cannot_place";
    public static final String CAVE_IN_BLACKLIST =
            "witherstormmod:cave_in_blacklist";
    public static final String WITHER_STORM_SUMMON_COMMAND_BLOCKS =
            "witherstormmod:wither_storm_summon_command_blocks";
    public static final String WITHER_STORM_SUMMON_BASE_BLOCKS =
            "witherstormmod:wither_storm_summon_base_blocks";
    public static final String SICKENED_BEE_CAN_CONVERT =
            "witherstormmod:sickened_bee_can_convert";
    public static final String TAINTED_BLOCKS = "witherstormmod:tainted_blocks";

    private static volatile Map<String, TagDefinition> definitions = Collections.emptyMap();
    private static volatile ConcurrentHashMap<String, byte[]> membershipCaches =
            new ConcurrentHashMap<String, byte[]>();
    private static volatile boolean initialized;

    private UpstreamBlockTags() {
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
                    throw new IOException("Missing values in upstream block tag " + entryName);
                }
                List<String> values = new ArrayList<String>();
                for (JsonElement element : sourceValues) {
                    String value = readTagValue(element, entryName);
                    if (value != null) values.add(value);
                }
                loaded.put(tagName, new TagDefinition(
                        root.has("replace") && root.get("replace").getAsBoolean(), values));
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load block tags from the external Wither Storm archive",
                    exception);
        }
        requireTag(loaded, WITHER_STORM_BLOCK_BLACKLIST);
        requireTag(loaded, LESS_FAVORABLE_BLOCKS_HUNCH);
        requireTag(loaded, NATURE_CLUSTER_WHITELIST);
        requireTag(loaded, SMALL_CLUSTER_BLACKLIST);
        requireTag(loaded, LESS_FAVORABLE_BLOCKS);
        requireTag(loaded, TRACTOR_BEAM_DISTRACTION_BLOCKS);
        requireTag(loaded, BEACONS);
        requireTag(loaded, WITHERED_BEACON_BASE);
        requireTag(loaded, AQUA_SUPPORT_BASE);
        requireTag(loaded, GREEN_SUPPORT_BASE);
        requireTag(loaded, GRAY_SUPPORT_BASE);
        requireTag(loaded, RED_SUPPORT_BASE);
        requireTag(loaded, BLOCK_CLUSTERS_CANNOT_PLACE);
        requireTag(loaded, CAVE_IN_BLACKLIST);
        requireTag(loaded, WITHER_STORM_SUMMON_COMMAND_BLOCKS);
        requireTag(loaded, WITHER_STORM_SUMMON_BASE_BLOCKS);
        requireTag(loaded, SICKENED_BEE_CAN_CONVERT);
        requireTag(loaded, TAINTED_BLOCKS);
        definitions = Collections.unmodifiableMap(loaded);
        membershipCaches = new ConcurrentHashMap<String, byte[]>();
        initialized = true;
        WitherStormMod.LOGGER.info("Indexed {} external upstream block tags", loaded.size());
    }

    public static boolean contains(String tagName, IBlockState state) {
        if (state == null) return false;
        ensureInitialized();
        int stateId = Block.getStateId(state);
        if (stateId < 0 || stateId >= BLOCK_STATE_CACHE_SIZE) {
            return containsUncached(tagName, state, new HashSet<String>());
        }
        ConcurrentHashMap<String, byte[]> caches = membershipCaches;
        byte[] cache = caches.get(tagName);
        if (cache == null) {
            byte[] created = new byte[BLOCK_STATE_CACHE_SIZE];
            byte[] existing = caches.putIfAbsent(tagName, created);
            cache = existing == null ? created : existing;
        }
        byte cached = cache[stateId];
        if (cached != CACHE_UNKNOWN) return cached == CACHE_MATCH;
        boolean result = containsUncached(tagName, state, new HashSet<String>());
        cache[stateId] = result ? CACHE_MATCH : CACHE_MISS;
        return result;
    }

    private static boolean containsUncached(String tagName, IBlockState state, Set<String> visiting) {
        if (!visiting.add(tagName)) return false;
        try {
            TagDefinition definition = definitions.get(tagName);
            if (definition != null) {
                for (String value : definition.values) {
                    if (value.startsWith("#")) {
                        if (containsUncached(value.substring(1), state, visiting)) return true;
                    } else if (matchesBlockId(value, state.getBlock())) {
                        return true;
                    }
                }
                if (definition.replace) return false;
            }
            return matchesLegacyFamily(tagName, state);
        } finally {
            visiting.remove(tagName);
        }
    }

    private static boolean matchesBlockId(String sourceId, Block block) {
        ResourceLocation registryName = Block.REGISTRY.getNameForObject(block);
        if (registryName == null) registryName = block.getRegistryName();
        if (registryName == null) return false;
        String legacyId = registryName.toString();
        if (sourceId.equals(legacyId)) return true;
        if (sourceId.startsWith("minecraft:")
                && legacyId.equals("futuremc:" + sourceId.substring("minecraft:".length()))) return true;
        if ("minecraft:cobweb".equals(sourceId)) return "minecraft:web".equals(legacyId);
        if ("minecraft:dirt_path".equals(sourceId)) return "minecraft:grass_path".equals(legacyId);
        if ("minecraft:sugar_cane".equals(sourceId)) return "minecraft:reeds".equals(legacyId);
        if ("minecraft:lily_pad".equals(sourceId)) return "minecraft:waterlily".equals(legacyId);
        if ("minecraft:dead_bush".equals(sourceId)) return "minecraft:deadbush".equals(legacyId);
        if ("minecraft:snow".equals(sourceId)) return "minecraft:snow_layer".equals(legacyId);
        if ("minecraft:water".equals(sourceId)) return "minecraft:flowing_water".equals(legacyId);
        if ("minecraft:lava".equals(sourceId)) return "minecraft:flowing_lava".equals(legacyId);
        if ("minecraft:repeater".equals(sourceId)) {
            return "minecraft:powered_repeater".equals(legacyId)
                    || "minecraft:unpowered_repeater".equals(legacyId);
        }
        if ("minecraft:comparator".equals(sourceId)) {
            return "minecraft:powered_comparator".equals(legacyId)
                    || "minecraft:unpowered_comparator".equals(legacyId);
        }
        if ("minecraft:redstone_torch".equals(sourceId)
                || "minecraft:redstone_wall_torch".equals(sourceId)) {
            return "minecraft:redstone_torch".equals(legacyId)
                    || "minecraft:unlit_redstone_torch".equals(legacyId);
        }
        if ("minecraft:torch".equals(sourceId) || "minecraft:wall_torch".equals(sourceId)) {
            return "minecraft:torch".equals(legacyId);
        }
        return false;
    }

    private static boolean matchesLegacyFamily(String tagName, IBlockState state) {
        Block block = state.getBlock();
        if ("minecraft:wither_immune".equals(tagName)) {
            return block == Blocks.BEDROCK || block == Blocks.BARRIER
                    || block == Blocks.END_PORTAL || block == Blocks.END_PORTAL_FRAME
                    || block == Blocks.END_GATEWAY || block == Blocks.COMMAND_BLOCK
                    || block == Blocks.CHAIN_COMMAND_BLOCK || block == Blocks.REPEATING_COMMAND_BLOCK
                    || block == Blocks.STRUCTURE_BLOCK || block == Blocks.STRUCTURE_VOID
                    || block == Blocks.PISTON_EXTENSION || block == Blocks.PISTON_HEAD;
        }
        if ("minecraft:dragon_immune".equals(tagName)) {
            return block == Blocks.BEDROCK || block == Blocks.BARRIER
                    || block == Blocks.END_PORTAL || block == Blocks.END_PORTAL_FRAME
                    || block == Blocks.END_GATEWAY || block == Blocks.COMMAND_BLOCK
                    || block == Blocks.CHAIN_COMMAND_BLOCK || block == Blocks.REPEATING_COMMAND_BLOCK
                    || block == Blocks.STRUCTURE_BLOCK || block == Blocks.STRUCTURE_VOID
                    || block == Blocks.OBSIDIAN || block == Blocks.END_STONE
                    || block == Blocks.IRON_BARS;
        }
        if ("minecraft:portals".equals(tagName)) {
            return block == Blocks.PORTAL || block == Blocks.END_PORTAL
                    || block == Blocks.END_GATEWAY;
        }
        if ("minecraft:dirt".equals(tagName)) {
            return block == Blocks.DIRT || block == Blocks.GRASS
                    || block == Blocks.MYCELIUM || block == Blocks.GRASS_PATH;
        }
        if ("minecraft:planks".equals(tagName)) {
            return block instanceof BlockPlanks;
        }
        if ("minecraft:overworld_natural_logs".equals(tagName)
                || "minecraft:logs".equals(tagName)) {
            return block == Blocks.LOG || block == Blocks.LOG2;
        }
        if ("minecraft:base_stone_overworld".equals(tagName)) {
            if (block != Blocks.STONE) return false;
            int variant = state.getBlock().getMetaFromState(state) & 7;
            return variant == 0 || variant == 1 || variant == 3 || variant == 5;
        }
        if ("forge:cobblestone".equals(tagName)) {
            return block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE;
        }
        if ("forge:sandstone".equals(tagName)) {
            return block == Blocks.SANDSTONE || block == Blocks.RED_SANDSTONE;
        }
        if ("forge:stone".equals(tagName)) return block == Blocks.STONE;
        if ("forge:gravel".equals(tagName)) return block == Blocks.GRAVEL;
        if ("forge:sand".equals(tagName) || "minecraft:sand".equals(tagName)) {
            return block == Blocks.SAND;
        }
        if ("minecraft:terracotta".equals(tagName)) {
            return block == Blocks.HARDENED_CLAY || block == Blocks.STAINED_HARDENED_CLAY;
        }
        if ("minecraft:banners".equals(tagName)) return block instanceof BlockBanner;
        if ("minecraft:flowers".equals(tagName)) {
            return block instanceof BlockFlower || block == Blocks.DOUBLE_PLANT;
        }
        if ("minecraft:small_flowers".equals(tagName)) return block instanceof BlockFlower;
        if ("minecraft:leaves".equals(tagName)) return block instanceof BlockLeaves;
        if ("forge:fences".equals(tagName)) return block instanceof BlockFence;
        if ("minecraft:saplings".equals(tagName)) return block instanceof BlockSapling;
        if ("minecraft:flower_pots".equals(tagName)) return block instanceof BlockFlowerPot;
        if ("minecraft:crops".equals(tagName)) return block instanceof BlockCrops;
        if ("minecraft:beds".equals(tagName)) return block instanceof BlockBed;
        if ("minecraft:wool_carpets".equals(tagName)) return block == Blocks.CARPET;
        if ("minecraft:wool".equals(tagName)) return block == Blocks.WOOL;
        if ("minecraft:signs".equals(tagName)) return block instanceof BlockSign;
        if ("minecraft:pressure_plates".equals(tagName)) {
            return block instanceof BlockPressurePlate || block instanceof BlockPressurePlateWeighted;
        }
        if ("minecraft:buttons".equals(tagName)) return block instanceof BlockButton;
        if ("minecraft:rails".equals(tagName)) return block instanceof BlockRailBase;
        if ("forge:glass".equals(tagName)) {
            return block == Blocks.GLASS || block == Blocks.STAINED_GLASS;
        }
        if ("forge:glass_panes".equals(tagName)) {
            return block instanceof BlockPane && block != Blocks.IRON_BARS;
        }
        if ("minecraft:walls".equals(tagName)) {
            return block == Blocks.COBBLESTONE_WALL;
        }
        if ("minecraft:beacon_base_blocks".equals(tagName)) {
            return block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK
                    || block == Blocks.DIAMOND_BLOCK || block == Blocks.EMERALD_BLOCK;
        }
        if ("minecraft:stone_bricks".equals(tagName)) return block == Blocks.STONEBRICK;
        if ("minecraft:doors".equals(tagName)) return block instanceof BlockDoor;
        if ("minecraft:trapdoors".equals(tagName)) return block instanceof BlockTrapDoor;
        if ("minecraft:campfires".equals(tagName)) {
            ResourceLocation id = block.getRegistryName();
            return id != null && "futuremc".equals(id.getNamespace()) && id.getPath().contains("campfire");
        }
        if ("forge:barrels".equals(tagName)) return hasRegistryName(block, "futuremc:barrel");
        if ("forge:bookshelves".equals(tagName)) return block == Blocks.BOOKSHELF;
        if ("forge:chests".equals(tagName)) {
            return block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST;
        }
        if ("minecraft:wooden_buttons".equals(tagName)) {
            // 1.12 只有橡木木按钮一种木质按钮。
            return block == Blocks.WOODEN_BUTTON;
        }
        if ("minecraft:wooden_doors".equals(tagName)) {
            return block == Blocks.OAK_DOOR || block == Blocks.SPRUCE_DOOR
                    || block == Blocks.BIRCH_DOOR || block == Blocks.JUNGLE_DOOR
                    || block == Blocks.ACACIA_DOOR || block == Blocks.DARK_OAK_DOOR;
        }
        if ("minecraft:wooden_trapdoors".equals(tagName)) {
            // 1.12 只有橡木活板门一种木质活板门。
            return block == Blocks.TRAPDOOR;
        }
        if ("minecraft:fence_gates".equals(tagName)) {
            return block == Blocks.OAK_FENCE_GATE || block == Blocks.SPRUCE_FENCE_GATE
                    || block == Blocks.BIRCH_FENCE_GATE || block == Blocks.JUNGLE_FENCE_GATE
                    || block == Blocks.ACACIA_FENCE_GATE || block == Blocks.DARK_OAK_FENCE_GATE;
        }
        if ("minecraft:wooden_fences".equals(tagName)) {
            return block == Blocks.OAK_FENCE || block == Blocks.SPRUCE_FENCE
                    || block == Blocks.BIRCH_FENCE || block == Blocks.JUNGLE_FENCE
                    || block == Blocks.ACACIA_FENCE || block == Blocks.DARK_OAK_FENCE;
        }
        if ("minecraft:wooden_pressure_plates".equals(tagName)) {
            // 1.12 只有橡木压力板一种木质压力板。
            return block == Blocks.WOODEN_PRESSURE_PLATE;
        }
        if ("minecraft:wooden_stairs".equals(tagName)) {
            return block == Blocks.OAK_STAIRS || block == Blocks.SPRUCE_STAIRS
                    || block == Blocks.BIRCH_STAIRS || block == Blocks.JUNGLE_STAIRS
                    || block == Blocks.ACACIA_STAIRS || block == Blocks.DARK_OAK_STAIRS;
        }
        if ("minecraft:wooden_slabs".equals(tagName)) {
            // 1.12 木台阶是独立方块，而不是 stone_slab 的变体。
            return block == Blocks.WOODEN_SLAB || block == Blocks.DOUBLE_WOODEN_SLAB;
        }
        if ("minecraft:fire".equals(tagName)) return block == Blocks.FIRE;
        if ("minecraft:needs_diamond_tool".equals(tagName)) return block.getHarvestLevel(state) >= 3;
        if ("forge:ores".equals(tagName)) {
            return block instanceof BlockOre || block == Blocks.QUARTZ_ORE
                    || block == Blocks.REDSTONE_ORE || block == Blocks.LIT_REDSTONE_ORE;
        }
        if ("minecraft:replaceable".equals(tagName)) return state.getMaterial().isReplaceable();
        if ("minecraft:shulker_boxes".equals(tagName)) return block instanceof BlockShulkerBox;
        if ("minecraft:anvil".equals(tagName)) return block instanceof BlockAnvil;
        if ("minecraft:ice".equals(tagName)) {
            return block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.FROSTED_ICE;
        }
        return false;
    }

    private static boolean hasRegistryName(Block block, String expected) {
        ResourceLocation id = block.getRegistryName();
        return id != null && expected.equals(id.toString());
    }

    private static String tagName(String entryName) {
        if (!entryName.startsWith("data/") || !entryName.endsWith(".json")) return null;
        int separator = entryName.indexOf("/tags/blocks/", 5);
        if (separator < 0) return null;
        String namespace = entryName.substring(5, separator);
        String path = entryName.substring(separator + "/tags/blocks/".length(),
                entryName.length() - ".json".length());
        return namespace.isEmpty() || path.isEmpty() ? null : namespace + ':' + path;
    }

    private static String readTagValue(JsonElement element, String entryName) throws IOException {
        if (element.isJsonPrimitive()) return element.getAsString();
        if (!element.isJsonObject()) throw new IOException("Invalid value in upstream block tag " + entryName);
        JsonObject object = element.getAsJsonObject();
        JsonElement id = object.get("id");
        if (id == null || !id.isJsonPrimitive()) {
            throw new IOException("Missing id in upstream block tag value " + entryName);
        }
        boolean required = !object.has("required") || object.get("required").getAsBoolean();
        return required ? id.getAsString() : id.getAsString();
    }

    private static void requireTag(Map<String, TagDefinition> loaded, String tagName) {
        if (!loaded.containsKey(tagName)) {
            throw new IllegalStateException("Required upstream block tag is missing: " + tagName);
        }
    }

    private static void ensureInitialized() {
        if (!initialized) initialize();
    }

    static List<String> valuesForTesting(String tagName) {
        ensureInitialized();
        TagDefinition definition = definitions.get(tagName);
        return definition == null ? Collections.<String>emptyList() : definition.values;
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
