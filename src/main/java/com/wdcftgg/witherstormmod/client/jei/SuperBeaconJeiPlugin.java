package com.wdcftgg.witherstormmod.client.jei;

import com.wdcftgg.witherstormmod.common.beacon.SuperBeaconRecipes;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 为超级信标的两类配方提供 JEI 分类与催化剂入口。 */
@JEIPlugin
public final class SuperBeaconJeiPlugin implements IModPlugin {

    public static final String ITEM_CRAFTING_UID = "witherstormmod.super_beacon_item_crafting";
    public static final String SUMMONING_UID = "witherstormmod.super_beacon_summoning";

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        mezz.jei.api.IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new SuperBeaconItemCraftingCategory(guiHelper),
                new SuperBeaconSummoningCategory(guiHelper));
    }

    @Override
    public void register(IModRegistry registry) {
        List<SuperBeaconRecipes.Recipe> itemCrafting = new ArrayList<SuperBeaconRecipes.Recipe>();
        List<SuperBeaconRecipes.Recipe> summoning = new ArrayList<SuperBeaconRecipes.Recipe>();
        for (SuperBeaconRecipes.Recipe recipe : SuperBeaconRecipes.getRecipes()) {
            if (recipe.isEntityRecipe()) {
                // 上游明确隐藏彩蛋用的 Reuben 猪配方。
                if (SuperBeaconLayout.shouldShowSummoningEntity(recipe.entity)) summoning.add(recipe);
            } else {
                itemCrafting.add(recipe);
            }
        }

        registry.handleRecipes(SuperBeaconItemCraftingRecipeWrapper.class,
                wrapper -> wrapper, ITEM_CRAFTING_UID);
        registry.addRecipes(wrap(itemCrafting, SuperBeaconItemCraftingRecipeWrapper::new),
                ITEM_CRAFTING_UID);

        registry.handleRecipes(SuperBeaconSummoningRecipeWrapper.class,
                wrapper -> wrapper, SUMMONING_UID);
        registry.addRecipes(wrap(summoning, SuperBeaconSummoningRecipeWrapper::new),
                SUMMONING_UID);

        ItemStack beacon = new ItemStack(ModBlocks.get("super_beacon"));
        ItemStack supportBeacon = new ItemStack(ModBlocks.get("super_support_beacon"));
        registry.addRecipeCatalyst(beacon, ITEM_CRAFTING_UID, SUMMONING_UID);
        registry.addRecipeCatalyst(supportBeacon, ITEM_CRAFTING_UID, SUMMONING_UID);
        registry.addIngredientInfo(beacon, ItemStack.class, "withered_beacon.info");
        registry.addIngredientInfo(supportBeacon, ItemStack.class, "withered_beacon.info");
    }

    private static List<SuperBeaconRecipeWrapper> wrap(
            List<SuperBeaconRecipes.Recipe> recipes,
            java.util.function.Function<SuperBeaconRecipes.Recipe, ? extends SuperBeaconRecipeWrapper> factory) {
        List<SuperBeaconRecipeWrapper> wrapped = new ArrayList<SuperBeaconRecipeWrapper>();
        for (SuperBeaconRecipes.Recipe recipe : recipes) {
            wrapped.add(factory.apply(recipe));
        }
        return wrapped;
    }
}
