package com.wdcftgg.witherstormmod.client.jei;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/** 两个风暴信标分类共用的环形材料布局。 */
abstract class AbstractSuperBeaconCategory<T extends SuperBeaconRecipeWrapper>
        implements IRecipeCategory<T> {

    private final IDrawable background;
    private final IDrawable icon;

    AbstractSuperBeaconCategory(IGuiHelper guiHelper, String iconPath) {
        this.background = guiHelper.createBlankDrawable(SuperBeaconLayout.WIDTH, SuperBeaconLayout.HEIGHT);
        this.icon = new BeaconIcon(
                guiHelper.createDrawableIngredient(new ItemStack(
                        ModBlocks.get("super_beacon"))),
                guiHelper.createDrawable(new ResourceLocation(Tags.MOD_ID, iconPath), 0, 0, 8, 8));
    }

    @Override
    public String getModName() {
        return Tags.MOD_NAME;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, T recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup stacks = recipeLayout.getItemStacks();
        List<List<ItemStack>> inputs = ingredients.getInputs(ItemStack.class);
        List<List<ItemStack>> outputs = ingredients.getOutputs(ItemStack.class);
        int centerY = SuperBeaconLayout.centerY(recipeWrapper.getRecipe().condition);
        for (int index = 0; index < inputs.size(); index++) {
            int x = SuperBeaconLayout.inputX(index, inputs.size());
            int y = SuperBeaconLayout.inputY(index, inputs.size(), centerY);
            stacks.init(index, true, x, y);
            stacks.set(index, inputs.get(index));
        }

        setResult(stacks, inputs.size(), outputs, SuperBeaconLayout.WIDTH / 2, centerY);
    }

    protected abstract void setResult(IGuiItemStackGroup stacks, int slotIndex,
                                      List<List<ItemStack>> outputs, int centerX, int centerY);

    private static final class BeaconIcon implements IDrawable {
        private final IDrawable beacon;
        private final IDrawable overlay;

        private BeaconIcon(IDrawable beacon, IDrawable overlay) {
            this.beacon = beacon;
            this.overlay = overlay;
        }

        @Override
        public int getWidth() {
            return beacon.getWidth();
        }

        @Override
        public int getHeight() {
            return beacon.getHeight();
        }

        @Override
        public void draw(Minecraft minecraft, int xOffset, int yOffset) {
            beacon.draw(minecraft, xOffset, yOffset);
            overlay.draw(minecraft, xOffset + 8, yOffset + 8);
        }
    }
}
