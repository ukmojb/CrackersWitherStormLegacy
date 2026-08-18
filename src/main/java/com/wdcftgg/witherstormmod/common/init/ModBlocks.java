package com.wdcftgg.witherstormmod.common.init;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.block.TaintedButtonBlock;
import com.wdcftgg.witherstormmod.common.block.AxisBlock;
import com.wdcftgg.witherstormmod.common.block.SuperBeaconBlock;
import com.wdcftgg.witherstormmod.common.block.SuperSupportBeaconBlock;
import com.wdcftgg.witherstormmod.common.block.FireworkBundleBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedTorchBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedStandingSignBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedWallSignBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedCeilingHangingSignBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedWallHangingSignBlock;
import com.wdcftgg.witherstormmod.common.item.TaintedSignItem;
import com.wdcftgg.witherstormmod.common.item.TaintedTorchItem;
import com.wdcftgg.witherstormmod.common.item.SlabItem;
import com.wdcftgg.witherstormmod.common.item.RarityBlockItem;
import com.wdcftgg.witherstormmod.common.block.TaintedDoorBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedSandBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedFenceBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedFenceGateBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedVeinBlock;
import com.wdcftgg.witherstormmod.common.block.SimpleBlock;
import com.wdcftgg.witherstormmod.common.block.PowerfulExplosiveBlock;
import com.wdcftgg.witherstormmod.common.block.FormidibombBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedPressurePlateBlock;
import com.wdcftgg.witherstormmod.common.block.DirectionalBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedGlassPaneBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedSlabBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedStairsBlock;
import com.wdcftgg.witherstormmod.common.block.StrippableLogBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedTrapdoorBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedDustBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedDustLampBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedCarvedPumpkinBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedPumpkinBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedStatueBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedWallBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedMushroomBlock;
import com.wdcftgg.witherstormmod.common.block.PottedTaintedMushroomBlock;
import com.wdcftgg.witherstormmod.common.block.TaintedLeavesBlock;
import com.wdcftgg.witherstormmod.common.block.WitheredPhlegmBlock;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import com.wdcftgg.witherstormmod.common.item.FormidibombItem;
import com.wdcftgg.witherstormmod.common.item.TaintedCarvedPumpkinItem;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.EnumRarity;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModBlocks {

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<String, Block>();
    private static final Map<String, Item> BLOCK_ITEMS = new LinkedHashMap<String, Item>();
    private static final String[] BLOCK_NAMES = ModRegistryNames.blockNames();

    static {
        for (String name : BLOCK_NAMES) {
            BLOCKS.put(name, createBlock(name));
        }
    }

    private ModBlocks() {
    }

    public static void bootstrap() {
    }

    public static Block get(String name) {
        return BLOCKS.get(name);
    }

    public static String[] getRegisteredNames() {
        return BLOCK_NAMES.clone();
    }

    public static boolean isItemless(String name) {
        return ModRegistryNames.isItemlessBlock(name);
    }

    public static Item getItem(String name) {
        return BLOCK_ITEMS.get(name);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOCKS.values().toArray(new Block[0]));
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        for (Block block : BLOCKS.values()) {
            if (isItemless(block)) {
                continue;
            }
            Item item;
            String name = block.getRegistryName().getPath();
            if (block instanceof TaintedDoorBlock) item = new ItemDoor(block);
            else if (block instanceof TaintedSlabBlock) item = new SlabItem((TaintedSlabBlock) block);
            else if ("tainted_sign".equals(name)) item = new TaintedSignItem(name);
            else if ("tainted_torch".equals(name)) item = new TaintedTorchItem(name);
            else if ("formidibomb".equals(name)) item = new FormidibombItem(block);
            else if (block instanceof TaintedCarvedPumpkinBlock) item = new TaintedCarvedPumpkinItem(block);
            else item = createRarityBlockItem(block, name);
            item.setRegistryName(block.getRegistryName());
            item.setTranslationKey(block.getTranslationKey().replace("tile.", ""));
            item.setCreativeTab(ModCreativeTabs.MAIN);
            BLOCK_ITEMS.put(name, item);
            event.getRegistry().register(item);
            registerOreDictionaryEntry(name, item);
            if (block instanceof TaintedCarvedPumpkinBlock) {
                ((TaintedCarvedPumpkinBlock) block).registerDispenserBehavior(item);
            }
        }
    }

    private static void registerOreDictionaryEntry(String name, Item item) {
        if ("tainted_log".equals(name) || "tainted_wood".equals(name)) {
            OreDictionary.registerOre("logWood", item);
        } else if ("tainted_planks".equals(name)) {
            OreDictionary.registerOre("plankWood", item);
        } else if ("tainted_cobblestone".equals(name)) {
            OreDictionary.registerOre("cobblestone", item);
        } else if ("tainted_stone".equals(name)) {
            OreDictionary.registerOre("stone", item);
        }
    }

    @SideOnly(Side.CLIENT)
    public static void registerModels() {
        for (Block block : BLOCKS.values()) {
            String name = block.getRegistryName().getPath();
            if ("tainted_hanging_sign".equals(name)
                    || "tainted_wall_hanging_sign".equals(name)) {
                mapHangingSignToParticleModel(block);
            }
            if (isItemless(block)) {
                continue;
            }
            Item item = BLOCK_ITEMS.get(name);
            if (item == null) {
                throw new IllegalStateException("Missing registered block item for " + block.getRegistryName());
            }
            ModelResourceLocation inventoryModel = new ModelResourceLocation(block.getRegistryName(), "inventory");
            ModelLoader.setCustomModelResourceLocation(item, 0, inventoryModel);
            if (block instanceof TaintedLeavesBlock) {
                ModelLoader.setCustomStateMapper(block, new StateMap.Builder()
                        .ignore(BlockLeaves.DECAYABLE,
                                BlockLeaves.CHECK_DECAY)
                        .build());
            }
            if (block instanceof TaintedWallBlock) {
                ModelLoader.setCustomModelResourceLocation(item, 1, inventoryModel);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static void mapHangingSignToParticleModel(Block block) {
        ModelLoader.setCustomStateMapper(block, mappedBlock -> {
            Map<IBlockState, ModelResourceLocation> locations =
                    new LinkedHashMap<IBlockState, ModelResourceLocation>();
            // The external upstream sign model has no geometry; it only supplies tainted-plank particles.
            ModelResourceLocation particleModel = new ModelResourceLocation(
                    Tags.MOD_ID + ":tainted_sign", "normal");
            for (IBlockState state
                    : mappedBlock.getBlockState().getValidStates()) {
                locations.put(state, particleModel);
            }
            return locations;
        });
    }

    private static Block createBlock(String name) {
        if ("super_tnt".equals(name)) {
            return new PowerfulExplosiveBlock(name, false);
        }
        if ("formidibomb".equals(name)) {
            return new FormidibombBlock(name);
        }
        if ("super_beacon".equals(name)) {
            return new SuperBeaconBlock(name);
        }
        if ("super_support_beacon".equals(name)) {
            return new SuperSupportBeaconBlock(name);
        }
        if ("firework_bundle".equals(name)) {
            return new FireworkBundleBlock(name);
        }
        if ("withered_phlegm_block".equals(name)) {
            return new WitheredPhlegmBlock(name);
        }
        if ("tainted_torch".equals(name)) {
            return new TaintedTorchBlock(name, false);
        }
        if ("tainted_wall_torch".equals(name)) {
            return new TaintedTorchBlock(name, true);
        }
        if ("tainted_sign".equals(name)) {
            return new TaintedStandingSignBlock(name);
        }
        if ("tainted_wall_sign".equals(name)) {
            return new TaintedWallSignBlock(name);
        }
        if ("tainted_hanging_sign".equals(name)) {
            return new TaintedCeilingHangingSignBlock(name);
        }
        if ("tainted_wall_hanging_sign".equals(name)) {
            return new TaintedWallHangingSignBlock(name);
        }
        if ("tainted_dust".equals(name)) {
            return new TaintedDustBlock(name);
        }
        if ("tainted_dust_block".equals(name)) {
            return new TaintedDustLampBlock(name);
        }
        if ("tainted_flesh_veins".equals(name)) {
            return new TaintedVeinBlock(name);
        }
        if ("tainted_flesh_block".equals(name)) {
            return createSimpleBlock(name, Material.CLOTH, 0.6F, 0.6F, SoundType.SLIME, false);
        }
        if ("infected_flesh_block".equals(name)) {
            return createSimpleBlock(name, Material.CLOTH, 0.8F, 0.8F, SoundType.SLIME, false);
        }
        if ("hardened_flesh_block".equals(name)) {
            return createSimpleBlock(name, Material.CLOTH, -1.0F, 3600000.0F, SoundType.SLIME, false);
        }
        if ("tainted_mushroom".equals(name)) {
            return new TaintedMushroomBlock(name);
        }
        if ("potted_tainted_mushroom".equals(name)) {
            return new PottedTaintedMushroomBlock(name);
        }
        if ("tainted_pumpkin".equals(name)) {
            return new TaintedPumpkinBlock(name);
        }
        if ("tainted_carved_pumpkin".equals(name) || "tainted_jack_o_lantern".equals(name)) {
            return new TaintedCarvedPumpkinBlock(name, "tainted_jack_o_lantern".equals(name));
        }
        if ("tainted_stone".equals(name)) {
            return createSimpleBlock(name, Material.ROCK, 1.5F, 6.0F, SoundType.STONE, true);
        }
        if ("tainted_cobblestone".equals(name)) {
            return createSimpleBlock(name, Material.ROCK, 2.0F, 6.0F, SoundType.STONE, true);
        }
        if ("tainted_dirt".equals(name)) {
            return createSimpleBlock(name, Material.GROUND, 0.5F, 0.5F, SoundType.GROUND, false);
        }
        if ("tainted_sandstone".equals(name) || "tainted_cut_sandstone".equals(name)
                || "tainted_chiseled_sandstone".equals(name) || "tainted_smooth_sandstone".equals(name)) {
            return createSimpleBlock(name, Material.ROCK, 0.8F, 0.8F, SoundType.STONE, true);
        }
        if ("tainted_glass".equals(name)) {
            return createSimpleBlock(name, Material.GLASS, 0.6F, 1200.0F, SoundType.GLASS, false);
        }
        if ("tainted_planks".equals(name)) {
            return createSimpleBlock(name, Material.WOOD, 2.0F, 3.0F, SoundType.WOOD, false);
        }
        if ("tainted_leaves".equals(name)) {
            return new TaintedLeavesBlock(name);
        }
        if ("tainted_zombie_sitting".equals(name) || "tainted_zombie_wall".equals(name)
                || "tainted_zombie_lying".equals(name)) {
            return new TaintedStatueBlock(name,
                    TaintedStatueBlock.StatueMaterial.TAINTED_ZOMBIE);
        }
        if ("tainted_bone_pile".equals(name) || "tainted_skeleton_wall".equals(name)
                || "tainted_skull_ceiling".equals(name)) {
            return new TaintedStatueBlock(name,
                    TaintedStatueBlock.StatueMaterial.TAINTED_BONE);
        }
        if ("tainted_log".equals(name)) {
            return new StrippableLogBlock(name, "stripped_tainted_log");
        }
        if ("tainted_wood".equals(name)) {
            return new StrippableLogBlock(name, "stripped_tainted_wood");
        }
        if (name.endsWith("_log") || name.endsWith("_wood")) {
            return new AxisBlock(name);
        }
        if (name.endsWith("_slab")) {
            return new TaintedSlabBlock(name, name.equals("tainted_slab") ? Material.WOOD : Material.ROCK);
        }
        if (name.endsWith("_wall")) {
            return new TaintedWallBlock(name, modelSource(name.replace("_wall", "")));
        }
        if ("tainted_glass_pane".equals(name)) {
            return new TaintedGlassPaneBlock(name);
        }
        if (name.endsWith("_stairs")) {
            boolean heavyStone = "tainted_stone_stairs".equals(name) || "tainted_cobblestone_stairs".equals(name);
            return new TaintedStairsBlock(name, modelSource(name).getDefaultState(), heavyStone ? 3.0F : 2.0F,
                    heavyStone ? 6.0F : 3.0F);
        }
        if (name.endsWith("_fence")) {
            return new TaintedFenceBlock(name, Material.WOOD, MapColor.PURPLE);
        }
        if (name.endsWith("_fence_gate")) {
            return new TaintedFenceGateBlock(name);
        }
        if (name.endsWith("_door")) {
            return new TaintedDoorBlock(name);
        }
        if (name.endsWith("_trapdoor")) {
            return new TaintedTrapdoorBlock(name);
        }
        if (name.endsWith("_button")) {
            return new TaintedButtonBlock(name, !name.contains("stone"));
        }
        if (name.endsWith("_pressure_plate")) {
            boolean stone = name.contains("stone");
            return new TaintedPressurePlateBlock(name, stone ? Material.ROCK : Material.WOOD,
                    stone ? BlockPressurePlate.Sensitivity.MOBS : BlockPressurePlate.Sensitivity.EVERYTHING);
        }
        if (name.endsWith("_sand")) {
            return new TaintedSandBlock(name, Material.SAND);
        }

        Material material = materialFor(name);
        return createSimpleBlock(name, material, 1.5F, 6.0F, SoundType.STONE, false);
    }

    private static Block createSimpleBlock(String name, Material material, float hardness, float resistance,
                                           SoundType soundType, boolean requiresPickaxe) {
        SimpleBlock block = new SimpleBlock(name, material, hardness, resistance, soundType);
        if (material == Material.GLASS) {
            block.setLightOpacity(0);
        }
        if (requiresPickaxe) {
            block.setHarvestLevel("pickaxe", 0);
        }
        return block;
    }

    private static Item createRarityBlockItem(Block block, String name) {
        if ("super_tnt".equals(name)) return new RarityBlockItem(block, EnumRarity.RARE);
        if ("super_beacon".equals(name)) return new RarityBlockItem(block, EnumRarity.EPIC);
        if ("super_support_beacon".equals(name)) return new RarityBlockItem(block, EnumRarity.RARE);
        if ("tainted_dust".equals(name)) return new RarityBlockItem(block, EnumRarity.UNCOMMON);
        if ("withered_phlegm_block".equals(name)) return new RarityBlockItem(block, EnumRarity.UNCOMMON);
        if (name.startsWith("tainted_zombie_") || name.startsWith("tainted_skeleton_")
                || "tainted_bone_pile".equals(name) || "tainted_skull_ceiling".equals(name)) {
            return new RarityBlockItem(block, EnumRarity.UNCOMMON);
        }
        return new ItemBlock(block);
    }

    private static Material materialFor(String name) {
        if (name.contains("glass")) {
            return Material.GLASS;
        }
        if (name.contains("leaves")) {
            return Material.LEAVES;
        }
        if (name.contains("log") || name.contains("wood") || name.contains("planks") || name.contains("sign") || name.contains("torch") || name.contains("mushroom")) {
            return Material.WOOD;
        }
        if (name.contains("flesh") || name.contains("bone") || name.contains("zombie") || name.contains("skeleton") || name.contains("skull") || name.contains("phlegm")) {
            return Material.CLOTH;
        }
        return Material.ROCK;
    }

    private static Block modelSource(String name) {
        String sourceName = switch (name) {
            case "tainted_stairs" -> "tainted_planks";
            default -> name.endsWith("_stairs")
                    ? name.substring(0, name.length() - "_stairs".length())
                    : name;
        };
        Block source = BLOCKS.get(sourceName);
        if (source == null) {
            throw new IllegalStateException("Missing model source " + sourceName + " for " + name);
        }
        return source;
    }

    private static boolean isItemless(Block block) {
        if (block.getRegistryName() == null) {
            return false;
        }
        return ModRegistryNames.isItemlessBlock(block.getRegistryName().getPath());
    }
}
