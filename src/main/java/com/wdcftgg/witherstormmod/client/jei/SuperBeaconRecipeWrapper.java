package com.wdcftgg.witherstormmod.client.jei;

import com.wdcftgg.witherstormmod.common.beacon.SuperBeaconRecipes;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.gui.Gui;
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


public abstract class SuperBeaconRecipeWrapper implements IRecipeWrapper {
    private static final ResourceLocation SLOT_TEXTURE =
            new ResourceLocation("witherstormmod", "textures/gui/jei/slot.png");
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
        drawSlots(minecraft);
        String conditionKey = conditionTranslationKey(recipe.condition);
        if (conditionKey == null) return;
        String text = new TextComponentTranslation(conditionKey).getFormattedText();
        int x = (recipeWidth - minecraft.fontRenderer.getStringWidth(text)) / 2;
        minecraft.fontRenderer.drawString(text, x, recipeHeight - 10, 0xFFFFFFFF);
    }

    private void drawSlots(Minecraft minecraft) {
        minecraft.getTextureManager().bindTexture(SLOT_TEXTURE);
        int totalSize = recipe.getIngredients().size();
        int centerY = SuperBeaconLayout.centerY(recipe.condition);
        for (int index = 0; index < totalSize; index++) {
            int x = SuperBeaconLayout.inputX(index, totalSize) - 1;
            int y = SuperBeaconLayout.inputY(index, totalSize, centerY) - 1;
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F,
                    SuperBeaconLayout.SLOT_SIZE,
                    SuperBeaconLayout.SLOT_SIZE, 256.0F, 256.0F);
        }
        if (!recipe.isEntityRecipe()) {
            Gui.drawModalRectWithCustomSizedTexture(SuperBeaconLayout.WIDTH / 2 - 9,
                    centerY - 9, 0.0F, 0.0F,
                    SuperBeaconLayout.SLOT_SIZE,
                    SuperBeaconLayout.SLOT_SIZE, 256.0F, 256.0F);
        }
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
