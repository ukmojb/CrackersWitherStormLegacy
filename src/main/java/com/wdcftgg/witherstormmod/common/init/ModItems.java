package com.wdcftgg.witherstormmod.common.init;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.item.CommandBlockAxeItem;
import com.wdcftgg.witherstormmod.common.item.AmuletItem;
import com.wdcftgg.witherstormmod.common.item.FormidiBladeItem;
import com.wdcftgg.witherstormmod.common.item.EyeOfTheStormItem;
import com.wdcftgg.witherstormmod.common.item.GoldenAppleStewItem;
import com.wdcftgg.witherstormmod.common.item.EffectFoodItem;
import com.wdcftgg.witherstormmod.common.item.FoiledItem;
import com.wdcftgg.witherstormmod.common.item.CommandBlockHoeItem;
import com.wdcftgg.witherstormmod.common.item.SimpleItem;
import com.wdcftgg.witherstormmod.common.item.CommandBlockPickaxeItem;
import com.wdcftgg.witherstormmod.common.item.CommandBlockShovelItem;
import com.wdcftgg.witherstormmod.common.item.CommandBlockSwordItem;
import com.wdcftgg.witherstormmod.common.item.ModToolMaterials;
import com.wdcftgg.witherstormmod.common.item.WitheredNetherStarItem;
import com.wdcftgg.witherstormmod.common.item.PhasometerItem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModItems {

    private static final Map<String, Item> ITEMS = new LinkedHashMap<String, Item>();
    private static final String[] ITEM_NAMES = ModRegistryNames.itemNames();

    static {
        for (String name : ITEM_NAMES) {
            if (!ModBlocksContains.blockExists(name)) {
                ITEMS.put(name, createItem(name));
            }
        }
    }

    private ModItems() {
    }

    public static void bootstrap() {
    }

    public static Item get(String name) {
        Item item = ITEMS.get(name);
        if (item != null) {
            return item;
        }
        return ModBlocks.getItem(name);
    }

    public static String[] getRegisteredNames() {
        return ITEM_NAMES.clone();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(ITEMS.values().toArray(new Item[0]));
    }

    @SideOnly(Side.CLIENT)
    public static void registerModels() {
        for (Item item : ITEMS.values()) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
        FormidiBladeItem.registerPropertyOverrides();
    }

    private static final class ModBlocksContains {
        private static boolean blockExists(String name) {
            return ModBlocks.get(name) != null;
        }
    }

    private static Item createItem(String name) {
        if ("amulet".equals(name)) {
            return new AmuletItem(name);
        }
        if ("phasometer".equals(name)) {
            return new PhasometerItem(name);
        }
        if ("withered_bone".equals(name)) {
            return new SimpleItem(name, EnumRarity.UNCOMMON);
        }
        if ("command_block_book".equals(name)) {
            return new FoiledItem(name, EnumRarity.RARE);
        }
        if ("formidi_blade".equals(name)) {
            return new FormidiBladeItem(name);
        }
        if ("eye_of_the_storm".equals(name)) {
            return new EyeOfTheStormItem(name);
        }
        if ("golden_apple_stew".equals(name)) {
            return new GoldenAppleStewItem(name);
        }
        if ("withered_flesh".equals(name)) {
            return new EffectFoodItem(name, 4, 0.1F, true, EnumRarity.UNCOMMON,
                    EffectFoodItem.effect(MobEffects.HUNGER, 800, 0, 0.8F),
                    EffectFoodItem.effect(MobEffects.WITHER, 400, 0, 1.0F));
        }
        if ("withered_spider_eye".equals(name)) {
            return new EffectFoodItem(name, 2, 0.8F, false, EnumRarity.UNCOMMON,
                    EffectFoodItem.effect(MobEffects.POISON, 200, 0, 1.0F),
                    EffectFoodItem.effect(MobEffects.WITHER, 400, 0, 1.0F));
        }
        if ("withered_nether_star".equals(name)) {
            return new WitheredNetherStarItem(name);
        }
        if (name.endsWith("_sword") || "formidi_blade".equals(name)) {
            return new CommandBlockSwordItem(name, materialFor(name), swordDamage(name), swordSpeed(name));
        }
        if (name.endsWith("_pickaxe")) {
            return new CommandBlockPickaxeItem(name, materialFor(name), pickaxeDamage(name), pickaxeSpeed(name));
        }
        if (name.endsWith("_axe")) {
            return new CommandBlockAxeItem(name, materialFor(name), axeDamage(name), axeSpeed(name));
        }
        if (name.endsWith("_shovel")) {
            return new CommandBlockShovelItem(name, materialFor(name), shovelDamage(name), shovelSpeed(name));
        }
        if (name.endsWith("_hoe")) {
            return new CommandBlockHoeItem(name, materialFor(name), hoeDamage(name), hoeSpeed(name));
        }
        return new SimpleItem(name);
    }

    private static Item.ToolMaterial materialFor(String name) {
        if (name.startsWith("wooden_")) {
            return ModToolMaterials.WOOD_COMMAND_BLOCK;
        }
        if (name.startsWith("stone_")) {
            return ModToolMaterials.STONE_COMMAND_BLOCK;
        }
        if (name.startsWith("iron_")) {
            return ModToolMaterials.IRON_COMMAND_BLOCK;
        }
        if (name.startsWith("gold_")) {
            return ModToolMaterials.GOLD_COMMAND_BLOCK;
        }
        return ModToolMaterials.COMMAND_BLOCK;
    }

    private static float swordDamage(String name) {
        if (name.startsWith("iron_")) return 4.0F;
        if (name.startsWith("gold_")) return -1.0F;
        return 3.0F;
    }

    private static float swordSpeed(String name) {
        if (name.startsWith("iron_")) return -2.8F;
        if (name.startsWith("gold_")) return -1.2F;
        return -2.4F;
    }

    private static float pickaxeDamage(String name) { return name.startsWith("iron_") ? 3.0F : 1.0F; }
    private static float pickaxeSpeed(String name) { return name.startsWith("iron_") ? -3.2F : -2.8F; }
    private static float axeDamage(String name) { return name.startsWith("command_block_") ? 5.0F : 6.0F; }
    private static float axeSpeed(String name) {
        if (name.startsWith("wooden_") || name.startsWith("stone_")) return -3.2F;
        if (name.startsWith("iron_")) return -3.1F;
        return -3.0F;
    }
    private static float shovelDamage(String name) { return name.startsWith("iron_") ? 2.5F : 1.5F; }
    private static float shovelSpeed(String name) {
        if (name.startsWith("command_block_")) return -3.4F;
        if (name.startsWith("stone_")) return -2.0F;
        return -3.0F;
    }
    private static float hoeDamage(String name) {
        if (name.startsWith("wooden_") || name.startsWith("command_block_")) return -4.0F;
        if (name.startsWith("stone_")) return -3.0F;
        if (name.startsWith("iron_")) return 9.0F;
        return 0.0F;
    }
    private static float hoeSpeed(String name) {
        if (name.startsWith("wooden_")) return 3.0F;
        if (name.startsWith("command_block_")) return 0.0F;
        if (name.startsWith("iron_")) return -3.5F;
        return -3.0F;
    }
}
