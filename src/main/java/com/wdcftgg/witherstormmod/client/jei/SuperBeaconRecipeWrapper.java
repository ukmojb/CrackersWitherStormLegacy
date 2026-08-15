package com.wdcftgg.witherstormmod.client.jei;

import com.wdcftgg.witherstormmod.common.beacon.SuperBeaconRecipes;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 超级信标配方在 JEI 中的通用展示包装。 */
public abstract class SuperBeaconRecipeWrapper implements IRecipeWrapper {
    private final SuperBeaconRecipes.Recipe recipe;

    protected SuperBeaconRecipeWrapper(SuperBeaconRecipes.Recipe recipe) {
        this.recipe = recipe;
    }

    public SuperBeaconRecipes.Recipe getRecipe() {
        return recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<List<ItemStack>> inputs = new ArrayList<List<ItemStack>>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            inputs.add(Arrays.asList(ingredient.getMatchingStacks()));
        }
        ingredients.setInputLists(ItemStack.class, inputs);

        List<ItemStack> outputs = new ArrayList<ItemStack>();
        if (recipe.isEntityRecipe()) {
            ItemStack egg = spawnEggFor(recipe.entity);
            if (!egg.isEmpty()) outputs.add(egg);
        } else {
            outputs.add(recipe.getResult());
        }
        ingredients.setOutputs(ItemStack.class, outputs);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight,
                         int mouseX, int mouseY) {
        String conditionKey = conditionTranslationKey(recipe.condition);
        if (conditionKey == null) return;
        String text = new TextComponentTranslation(conditionKey).getFormattedText();
        minecraft.fontRenderer.drawString(text, 5, 42, 0xFF404040);
    }

    private static String conditionTranslationKey(String condition) {
        if ("main_activated".equals(condition)) {
            return "witherstormmod.jei.super_beacon.requiresMainActivated";
        }
        if ("all_supports".equals(condition)) {
            return "witherstormmod.jei.super_beacon.requiresAllSupports";
        }
        if ("fully_completed".equals(condition)) {
            return "witherstormmod.jei.super_beacon.requiresFullBeacon";
        }
        return null;
    }

    private static ItemStack spawnEggFor(String entityId) {
        if (entityId == null) return ItemStack.EMPTY;
        ResourceLocation entityName = new ResourceLocation(entityId);
        ItemStack egg = new ItemStack(Items.SPAWN_EGG);
        ItemMonsterPlacer.applyEntityIdToItemStack(egg, entityName);
        return egg;
    }
}
