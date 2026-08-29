package com.wdcftgg.witherstormmod.client.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiItemStackGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;


public final class SuperBeaconItemCraftingCategory
        extends AbstractSuperBeaconCategory<SuperBeaconItemCraftingRecipeWrapper> {

    public SuperBeaconItemCraftingCategory(IGuiHelper guiHelper) {
        super(guiHelper, "textures/gui/jei/crafting_icon.png");
    }

    @Override
    public String getUid() {
        return SuperBeaconJeiPlugin.ITEM_CRAFTING_UID;
    }

    @Override
    public String getTitle() {
        return new TextComponentTranslation(
                "witherstormmod.jei.item_craft_super_beacon.title").getFormattedText();
    }

    @Override
    protected void setResult(IGuiItemStackGroup stacks, int slotIndex,
                             List<List<ItemStack>> outputs, int centerX, int centerY) {
        stacks.init(slotIndex, false, centerX - 8, centerY - 8);
        if (!outputs.isEmpty() && !outputs.get(0).isEmpty()) {
            stacks.set(slotIndex, outputs.get(0));
        }
    }
}
