package com.wdcftgg.witherstormmod.common.taint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.resource.UpstreamResourceArchive;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.block.BlockLeaves;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import com.google.common.base.Optional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从外部上游 JAR 读取腐化数据（52 个方块配方与 22 个生物转化），
 * 并按 Minecraft 1.12.2 的方块/实体语义映射到本移植模组。
 */
public final class TaintingData {

    private static final String BLOCK_PREFIX = "data/witherstormmod/tainting/block/";
    private static final String ENTITY_PREFIX = "data/witherstormmod/tainting/entity/";
    private static final int EXPECTED_BLOCK_RECIPES = 52;
    private static final int EXPECTED_MOB_CONVERSIONS = 22;

    private static volatile List<BlockRecipe> blockRecipes = Collections.emptyList();
    private static volatile List<MobRecipe> mobRecipes = Collections.emptyList();
    private static volatile Map<String, ResourceLocation> originalTypes = Collections.emptyMap();

    private TaintingData() {
    }

    public static synchronized void initialize() {
        if (!blockRecipes.isEmpty()) return;
        List<BlockRecipe> loadedBlocks = new ArrayList<BlockRecipe>();
        List<MobRecipe> loadedMobs = new ArrayList<MobRecipe>();
        Map<String, ResourceLocation> loadedOriginals = new LinkedHashMap<String, ResourceLocation>();
        try {
            for (String entryName : sortedEntries(BLOCK_PREFIX)) {
                JsonObject json = readJson(entryName);
                BlockMatcher matcher = createBlockMatcher(requiredString(json, "block"));
                JsonObject replacement = requiredObject(json, "replacement");
                String replacementName = requiredString(replacement, "Name");
                Block replacementBlock = resolveReplacementBlock(replacementName);
                Map<String, String> replacementProperties = readReplacementProperties(replacement);
                IBlockState replacementState = applyProperties(replacementBlock.getDefaultState(),
                        replacement.has("Properties") ? replacement.getAsJsonObject("Properties") : null);
                List<String> propertiesToCopy = readPropertiesToCopy(json);
                Potion potionEffect = null;
                if (json.has("potion_effect")) {
                    ResourceLocation effectId = new ResourceLocation(requiredString(json, "potion_effect"));
                    potionEffect = ForgeRegistries.POTIONS.getValue(effectId);
                    if (potionEffect == null) {
                        throw new IOException("Unknown tainting potion effect " + effectId);
                    }
                }
                loadedBlocks.add(new BlockRecipe(requiredString(json, "block"), replacementName,
                        replacementProperties, matcher, replacementBlock, replacementState,
                        propertiesToCopy, potionEffect));
            }
            for (String entryName : sortedEntries(ENTITY_PREFIX)) {
                JsonObject json = readJson(entryName);
                String fromId = mapFromEntityId(requiredString(json, "from"));
                String toId = requiredString(json, "to");
                if (!toId.startsWith("witherstormmod:sickened_")) {
                    throw new IOException("Unsupported sickened entity conversion target: " + toId);
                }
                String sickenedType = toId.substring("witherstormmod:".length());
                boolean fromSickness = json.has("convert_from_sickness")
                        && json.get("convert_from_sickness").getAsBoolean();
                ResourceLocation fromKey = new ResourceLocation(fromId);
                loadedMobs.add(new MobRecipe(fromKey, sickenedType, fromSickness));
                loadedOriginals.put(sickenedType, fromKey);
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to load tainting recipes from the external Wither Storm archive", exception);
        }
        if (loadedBlocks.size() != EXPECTED_BLOCK_RECIPES) {
            throw new IllegalStateException("Expected " + EXPECTED_BLOCK_RECIPES
                    + " external block tainting recipes but loaded " + loadedBlocks.size());
        }
        if (loadedMobs.size() != EXPECTED_MOB_CONVERSIONS) {
            throw new IllegalStateException("Expected " + EXPECTED_MOB_CONVERSIONS
                    + " external mob conversions but loaded " + loadedMobs.size());
        }
        blockRecipes = Collections.unmodifiableList(loadedBlocks);
        mobRecipes = Collections.unmodifiableList(loadedMobs);
        originalTypes = Collections.unmodifiableMap(loadedOriginals);
        WitherStormMod.LOGGER.info("Loaded {} block tainting recipes and {} mob conversions "
                + "from the external Wither Storm archive", loadedBlocks.size(), loadedMobs.size());
    }

    static BlockRecipe findBlockRecipe(IBlockState state) {
        for (BlockRecipe recipe : blockRecipes) {
            if (recipe.matcher.matches(state)) return recipe;
        }
        return null;
    }

    static BlockRecipe findBlockRecipe(IBlockState state, PotionType potionType) {
        for (BlockRecipe recipe : blockRecipes) {
            if (recipe.matcher.matches(state) && recipe.canConvertWithPotion(potionType)) return recipe;
        }
        return null;
    }

    static MobRecipe findMobRecipe(EntityLivingBase entity) {
        for (MobRecipe recipe : mobRecipes) {
            if (recipe.matches(entity)) return recipe;
        }
        return null;
    }

    static boolean canConvertMob(EntityLivingBase entity, boolean fromWitherSickness) {
        if (entity == null || entity instanceof SickenedMobEntity) return false;
        MobRecipe recipe = findMobRecipe(entity);
        return recipe != null && (recipe.fromSicknessAllowed || !fromWitherSickness);
    }

    static IBlockState applyRecipe(BlockRecipe recipe, IBlockState source) {
        IBlockState replacement = recipe.replacementState;
        for (String propertyName : recipe.propertiesToCopy) {
            replacement = copyProperty(source, replacement, propertyName);
        }
        if (source.getBlock() instanceof BlockLeaves
                && replacement.getBlock() instanceof BlockLeaves) {
            if (recipe.propertiesToCopy.contains("persistent")) {
                replacement = replacement.withProperty(BlockLeaves.DECAYABLE,
                        source.getValue(BlockLeaves.DECAYABLE));
            }
            if (recipe.propertiesToCopy.contains("distance")) {
                replacement = replacement.withProperty(BlockLeaves.CHECK_DECAY,
                        source.getValue(BlockLeaves.CHECK_DECAY));
            }
        }
        return replacement;
    }

    static SickenedMobEntity createSickenedEntity(String sickenedType, World world) {
        if ("sickened_bee".equals(sickenedType)) return new SickenedEntities.SickenedBeeEntity(world);
        if ("sickened_cat".equals(sickenedType)) return new SickenedEntities.SickenedCatEntity(world);
        if ("sickened_chicken".equals(sickenedType)) return new SickenedEntities.SickenedChickenEntity(world);
        if ("sickened_cow".equals(sickenedType)) return new SickenedEntities.SickenedCowEntity(world);
        if ("sickened_creeper".equals(sickenedType)) return new SickenedEntities.SickenedCreeperEntity(world);
        if ("sickened_iron_golem".equals(sickenedType)) return new SickenedEntities.SickenedIronGolemEntity(world);
        if ("sickened_mushroom_cow".equals(sickenedType)) return new SickenedEntities.SickenedMushroomCowEntity(world);
        if ("sickened_parrot".equals(sickenedType)) return new SickenedEntities.SickenedParrotEntity(world);
        if ("sickened_phantom".equals(sickenedType)) return new SickenedEntities.SickenedPhantomEntity(world);
        if ("sickened_pig".equals(sickenedType)) return new SickenedEntities.SickenedPigEntity(world);
        if ("sickened_pillager".equals(sickenedType)) return new SickenedEntities.SickenedPillagerEntity(world);
        if ("sickened_skeleton".equals(sickenedType)) return new SickenedEntities.SickenedSkeletonEntity(world);
        if ("sickened_snow_golem".equals(sickenedType)) return new SickenedEntities.SickenedSnowGolemEntity(world);
        if ("sickened_spider".equals(sickenedType)) return new SickenedEntities.SickenedSpiderEntity(world);
        if ("sickened_villager".equals(sickenedType)) return new SickenedEntities.SickenedVillagerEntity(world);
        if ("sickened_vindicator".equals(sickenedType)) return new SickenedEntities.SickenedVindicatorEntity(world);
        if ("sickened_wolf".equals(sickenedType)) return new SickenedEntities.SickenedWolfEntity(world);
        if ("sickened_zombie".equals(sickenedType)) return new SickenedEntities.SickenedZombieEntity(world);
        return null;
    }

    static EntityLivingBase createCuredEntity(SickenedMobEntity original) {
        World world = original.world;
        ResourceLocation originalType = original.getOriginalType();
        if (originalType != null) {
            EntityLivingBase cured = createVanillaEntity(originalType, world);
            if (cured != null) return cured;
        }
        ResourceLocation fallback = originalTypes.get(original.getSickenedType());
        if (fallback != null) {
            EntityLivingBase cured = createVanillaEntity(fallback, world);
            if (cured != null) return cured;
        }
        return null;
    }

    static ResourceLocation getOriginalType(String sickenedType) {
        ResourceLocation type = originalTypes.get(sickenedType);
        if (type == null) return null;
        return new ResourceLocation(mapToLegacyEntityId(type.toString()));
    }

    private static EntityLivingBase createVanillaEntity(ResourceLocation type, World world) {
        String path = type.getPath();
        if ("ocelot".equals(path)) return new EntityOcelot(world);
        if ("chicken".equals(path)) return new EntityChicken(world);
        if ("cow".equals(path)) return new EntityCow(world);
        if ("creeper".equals(path)) return new EntityCreeper(world);
        if ("villager_golem".equals(path)) return new EntityIronGolem(world);
        if ("mushroom_cow".equals(path)) return new EntityMooshroom(world);
        if ("parrot".equals(path)) return new EntityParrot(world);
        if ("pig".equals(path)) return new EntityPig(world);
        if ("skeleton".equals(path)) return new EntitySkeleton(world);
        if ("stray".equals(path)) return new EntityStray(world);
        if ("snowman".equals(path)) return new EntitySnowman(world);
        if ("spider".equals(path)) return new EntitySpider(world);
        if ("villager".equals(path)) return new EntityVillager(world);
        if ("vindication_illager".equals(path)) return new EntityVindicator(world);
        if ("wolf".equals(path)) return new EntityWolf(world);
        if ("zombie".equals(path)) return new EntityZombie(world);
        if ("husk".equals(path)) return new EntityHusk(world);
        if ("zombie_villager".equals(path)) return new EntityZombieVillager(world);
        Class<? extends Entity> entityClass = EntityList.getClass(type);
        if (entityClass == null) return null;
        try {
            Entity entity = entityClass.getConstructor(World.class).newInstance(world);
            return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IBlockState copyProperty(IBlockState source, IBlockState target, String propertyName) {
        for (IProperty sourceProperty : source.getPropertyKeys()) {
            if (!sourceProperty.getName().equals(propertyName)) continue;
            Comparable value = (Comparable) source.getValue(sourceProperty);
            String valueName = value.toString();
            for (IProperty targetProperty : target.getPropertyKeys()) {
                if (!targetProperty.getName().equals(propertyName)) continue;
                Optional<? extends Comparable> parsed = targetProperty.parseValue(valueName);
                if (parsed.isPresent()) {
                    target = target.withProperty(targetProperty, parsed.get());
                }
            }
        }
        return target;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IBlockState applyProperties(IBlockState state, JsonObject properties) {
        if (properties == null) return state;
        for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
            String value = entry.getValue().getAsString();
            for (IProperty property : state.getPropertyKeys()) {
                if (!property.getName().equals(entry.getKey())) continue;
                Optional<? extends Comparable> parsed = property.parseValue(value);
                if (parsed.isPresent()) {
                    state = state.withProperty(property, parsed.get());
                }
            }
        }
        return state;
    }

    private static Block resolveReplacementBlock(String name) throws IOException {
        if ("minecraft:wither_rose".equals(name)) {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("futuremc", "wither_rose"));
            if (block == null) {
                throw new IOException("Future MC wither rose is unavailable for block tainting");
            }
            return block;
        }
        if (name.startsWith("witherstormmod:")) {
            Block block = ModBlocks.get(name.substring("witherstormmod:".length()));
            if (block == null) throw new IOException("Missing local replacement block " + name);
            return block;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
        if (block == null) throw new IOException("Missing replacement block " + name);
        return block;
    }

    private static BlockMatcher createBlockMatcher(String blockId) throws IOException {
        if (blockId.startsWith("#")) {
            String tagName = blockId.substring(1);
            return state -> UpstreamBlockTags.contains(tagName, state);
        }
        if ("minecraft:stone".equals(blockId)) return any(Blocks.STONE);
        if ("minecraft:cobblestone".equals(blockId)) return any(Blocks.COBBLESTONE);
        if ("minecraft:sand".equals(blockId)) return meta(Blocks.SAND, 0);
        if ("minecraft:red_sand".equals(blockId)) return meta(Blocks.SAND, 1);
        if ("minecraft:sandstone".equals(blockId)) return meta(Blocks.SANDSTONE, 0);
        if ("minecraft:cut_sandstone".equals(blockId) || "minecraft:chiseled_sandstone".equals(blockId)) {
            // 1.12 没有单独的切制砂岩方块，其外观对应砂岩的錾制变体（元数据 1）。
            return meta(Blocks.SANDSTONE, 1);
        }
        if ("minecraft:smooth_sandstone".equals(blockId)) return meta(Blocks.SANDSTONE, 2);
        if ("minecraft:red_sandstone".equals(blockId)) return meta(Blocks.RED_SANDSTONE, 0);
        if ("minecraft:cut_red_sandstone".equals(blockId)) return meta(Blocks.RED_SANDSTONE, 1);
        if ("minecraft:smooth_red_sandstone".equals(blockId)) return meta(Blocks.RED_SANDSTONE, 2);
        if ("minecraft:stone_slab".equals(blockId)) return slab(Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 0);
        if ("minecraft:sandstone_slab".equals(blockId)) return slab(Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 1);
        if ("minecraft:cobblestone_slab".equals(blockId)) {
            return slab(Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 3);
        }
        if ("minecraft:red_sandstone_slab".equals(blockId)) {
            return slab(Blocks.STONE_SLAB2, Blocks.DOUBLE_STONE_SLAB2, 0);
        }
        if ("minecraft:cut_sandstone_slab".equals(blockId) || "minecraft:smooth_sandstone_slab".equals(blockId)
                || "minecraft:cut_red_sandstone_slab".equals(blockId)
                || "minecraft:smooth_red_sandstone_slab".equals(blockId)) {
            // 1.12 没有对应的独立切制/平滑台阶方块。
            return state -> false;
        }
        // 1.12 的圆石楼梯沿用旧注册名 minecraft:stone_stairs；普通石楼梯尚不存在。
        if ("minecraft:stone_stairs".equals(blockId)) return state -> false;
        if ("minecraft:cobblestone_stairs".equals(blockId)) return any(Blocks.STONE_STAIRS);
        if ("minecraft:sandstone_stairs".equals(blockId)) return any(Blocks.SANDSTONE_STAIRS);
        if ("minecraft:red_sandstone_stairs".equals(blockId)) return any(Blocks.RED_SANDSTONE_STAIRS);
        if ("minecraft:smooth_sandstone_stairs".equals(blockId)
                || "minecraft:smooth_red_sandstone_stairs".equals(blockId)) {
            return state -> false;
        }
        if ("minecraft:sandstone_wall".equals(blockId) || "minecraft:red_sandstone_wall".equals(blockId)
                || "minecraft:smooth_sandstone_wall".equals(blockId)
                || "minecraft:smooth_red_sandstone_wall".equals(blockId)) {
            // 1.12 只有圆石墙，没有砂岩墙。
            return state -> false;
        }
        if ("minecraft:glass".equals(blockId)) return any(Blocks.GLASS);
        if ("minecraft:glass_pane".equals(blockId)) return any(Blocks.GLASS_PANE);
        if ("minecraft:carved_pumpkin".equals(blockId)) {
            // 1.12 的南瓜方块本身就带脸，语义上等价于上游的雕刻南瓜。
            return any(Blocks.PUMPKIN);
        }
        if ("minecraft:pumpkin".equals(blockId)) return state -> false;
        if ("minecraft:jack_o_lantern".equals(blockId)) return any(Blocks.LIT_PUMPKIN);
        if ("minecraft:brown_mushroom".equals(blockId)) return any(Blocks.BROWN_MUSHROOM);
        if ("minecraft:red_mushroom".equals(blockId)) return any(Blocks.RED_MUSHROOM);
        if ("minecraft:redstone_wire".equals(blockId)) return any(Blocks.REDSTONE_WIRE);
        if ("minecraft:redstone_block".equals(blockId)) return any(Blocks.REDSTONE_BLOCK);
        if ("minecraft:stone_button".equals(blockId)) return any(Blocks.STONE_BUTTON);
        if ("minecraft:stone_pressure_plate".equals(blockId)) return any(Blocks.STONE_PRESSURE_PLATE);
        if ("minecraft:torch".equals(blockId)) return standingTorch();
        if ("minecraft:wall_torch".equals(blockId)) return wallTorch();
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
        if (block == null) {
            throw new IOException("Unknown upstream block tainting source " + blockId);
        }
        return any(block);
    }

    private static BlockMatcher any(final Block block) {
        return state -> state.getBlock() == block;
    }

    private static BlockMatcher meta(final Block block, final int variant) {
        return state -> state.getBlock() == block
                && (state.getBlock().getMetaFromState(state) & 7) == variant;
    }

    private static BlockMatcher standingTorch() {
        return state -> state.getBlock() == Blocks.TORCH
                && state.getValue(BlockTorch.FACING) == EnumFacing.UP;
    }

    private static BlockMatcher wallTorch() {
        return state -> state.getBlock() == Blocks.TORCH
                && state.getValue(BlockTorch.FACING).getAxis().isHorizontal();
    }

    private static BlockMatcher slab(final Block single, final Block full, final int variant) {
        return state -> {
            Block block = state.getBlock();
            return (block == single || block == full)
                    && (block.getMetaFromState(state) & 7) == variant;
        };
    }

    private static String mapFromEntityId(String id) throws IOException {
        if ("minecraft:bee".equals(id)) return "futuremc:bee";
        if ("minecraft:cat".equals(id)) return "minecraft:ocelot";
        if ("minecraft:iron_golem".equals(id)) return "minecraft:villager_golem";
        if ("minecraft:snow_golem".equals(id)) return "minecraft:snowman";
        if ("minecraft:mooshroom".equals(id)) return "minecraft:mushroom_cow";
        if ("minecraft:vindicator".equals(id)) return "minecraft:vindication_illager";
        if (id.startsWith("minecraft:")) return id;
        throw new IOException("Unknown upstream mob conversion source " + id);
    }

    private static String mapToLegacyEntityId(String id) {
        if ("minecraft:bee".equals(id)) return "futuremc:bee";
        if ("minecraft:cat".equals(id)) return "minecraft:ocelot";
        if ("minecraft:iron_golem".equals(id)) return "minecraft:villager_golem";
        if ("minecraft:snow_golem".equals(id)) return "minecraft:snowman";
        if ("minecraft:mooshroom".equals(id)) return "minecraft:mushroom_cow";
        if ("minecraft:vindicator".equals(id)) return "minecraft:vindication_illager";
        return id;
    }

    private static List<String> sortedEntries(String prefix) throws IOException {
        List<String> entries = new ArrayList<String>(
                UpstreamResourceArchive.listEntries(prefix, ".json"));
        Collections.sort(entries);
        return entries;
    }

    private static JsonObject readJson(String entryName) throws IOException {
        try (InputStream stream = UpstreamResourceArchive.open(entryName);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid upstream tainting recipe " + entryName, exception);
        }
    }

    private static String requiredString(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IOException("Missing tainting recipe member " + key);
        }
        return value.getAsString();
    }

    private static JsonObject requiredObject(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IOException("Missing tainting recipe object " + key);
        }
        return value.getAsJsonObject();
    }

    private static List<String> readPropertiesToCopy(JsonObject object) {
        if (!object.has("properties_to_copy")) return Collections.emptyList();
        List<String> names = new ArrayList<String>();
        for (JsonElement element : object.getAsJsonArray("properties_to_copy")) {
            names.add(element.getAsString());
        }
        return names;
    }

    interface BlockMatcher {
        boolean matches(IBlockState state);
    }

    static final class BlockRecipe {
        private final String sourceId;
        private final String replacementName;
        private final Map<String, String> replacementProperties;
        private final BlockMatcher matcher;
        private final Block replacementBlock;
        private final IBlockState replacementState;
        private final List<String> propertiesToCopy;
        private final Potion potionEffect;

        private BlockRecipe(String sourceId, String replacementName,
                            Map<String, String> replacementProperties,
                            BlockMatcher matcher, Block replacementBlock,
                            IBlockState replacementState, List<String> propertiesToCopy,
                            Potion potionEffect) {
            this.sourceId = sourceId;
            this.replacementName = replacementName;
            this.replacementProperties = replacementProperties;
            this.matcher = matcher;
            this.replacementBlock = replacementBlock;
            this.replacementState = replacementState;
            this.propertiesToCopy = propertiesToCopy;
            this.potionEffect = potionEffect;
        }

        Block getReplacementBlock() {
            return replacementBlock;
        }

        String getReplacementName() {
            return replacementName;
        }

        boolean canConvertWithPotion(PotionType potionType) {
            if (potionEffect == null || potionType == null) return false;
            for (PotionEffect effect : potionType.getEffects()) {
                if (effect.getPotion() == potionEffect) return true;
            }
            return false;
        }
    }

    static final class MobRecipe {
        private final ResourceLocation from;
        private final String sickenedType;
        private final boolean fromSicknessAllowed;

        private MobRecipe(ResourceLocation from, String sickenedType, boolean fromSicknessAllowed) {
            this.from = from;
            this.sickenedType = sickenedType;
            this.fromSicknessAllowed = fromSicknessAllowed;
        }

        String getSickenedType() {
            return sickenedType;
        }

        boolean canConvertFromWitherSickness() {
            return fromSicknessAllowed;
        }

        private boolean matches(EntityLivingBase entity) {
            ResourceLocation key = EntityList.getKey(entity);
            if (key != null && key.equals(from)) return true;
            Class<? extends Entity> entityClass = EntityList.getClass(from);
            return entityClass != null && entityClass.isAssignableFrom(entity.getClass());
        }
    }

    private static Map<String, String> readReplacementProperties(JsonObject replacement) {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        if (replacement.has("Properties")) {
            JsonObject source = replacement.getAsJsonObject("Properties");
            for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
                properties.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return properties;
    }

    public static List<BlockRecipeView> getBlockRecipes() {
        List<BlockRecipeView> views = new ArrayList<BlockRecipeView>();
        for (BlockRecipe recipe : blockRecipes) {
            views.add(new BlockRecipeView(recipe.sourceId, recipe.replacementName,
                    new LinkedHashMap<String, String>(recipe.replacementProperties),
                    new ArrayList<String>(recipe.propertiesToCopy),
                    recipe.potionEffect == null || recipe.potionEffect.getRegistryName() == null
                            ? null : recipe.potionEffect.getRegistryName().toString()));
        }
        return Collections.unmodifiableList(views);
    }

    public static List<MobConversionView> getMobConversions() {
        List<MobConversionView> views = new ArrayList<MobConversionView>();
        for (MobRecipe recipe : mobRecipes) {
            views.add(new MobConversionView(recipe.from.toString(),
                    "witherstormmod:" + recipe.sickenedType, recipe.fromSicknessAllowed));
        }
        return Collections.unmodifiableList(views);
    }

    public static final class BlockRecipeView {
        public final String sourceId;
        public final String replacementName;
        public final Map<String, String> replacementProperties;
        public final List<String> propertiesToCopy;
        public final String potionEffect;

        private BlockRecipeView(String sourceId, String replacementName,
                                Map<String, String> replacementProperties,
                                List<String> propertiesToCopy, String potionEffect) {
            this.sourceId = sourceId;
            this.replacementName = replacementName;
            this.replacementProperties = replacementProperties;
            this.propertiesToCopy = propertiesToCopy;
            this.potionEffect = potionEffect;
        }
    }

    public static final class MobConversionView {
        public final String fromId;
        public final String toId;
        public final boolean convertFromSickness;

        private MobConversionView(String fromId, String toId, boolean convertFromSickness) {
            this.fromId = fromId;
            this.toId = toId;
            this.convertFromSickness = convertFromSickness;
        }
    }
}
