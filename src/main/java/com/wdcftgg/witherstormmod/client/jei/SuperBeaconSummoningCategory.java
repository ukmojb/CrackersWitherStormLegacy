package com.wdcftgg.witherstormmod.client.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IGuiItemStackGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

/** 超级信标召唤实体分类。 */
public final class SuperBeaconSummoningCategory
        extends AbstractSuperBeaconCategory<SuperBeaconSummoningRecipeWrapper> {

    public SuperBeaconSummoningCategory(IGuiHelper guiHelper) {
        super(guiHelper, "textures/gui/jei/summoning_icon.png");
    }

    @Override
    public String getUid() {
        return SuperBeaconJeiPlugin.SUMMONING_UID;
    }

    @Override
    public String getTitle() {
        return new TextComponentTranslation(
                "witherstormmod.jei.resummoning_super_beacon.title").getFormattedText();
    }

    @Override
    protected void setResult(IGuiItemStackGroup stacks, int slotIndex,
                             List<List<ItemStack>> outputs, int centerX, int centerY) {
        // 召唤结果由配方包装器在中心绘制实体；输出物品仅用于 JEI 查询。
    }
}
