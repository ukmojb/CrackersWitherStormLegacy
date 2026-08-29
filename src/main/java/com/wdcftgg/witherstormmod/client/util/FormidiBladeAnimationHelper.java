package com.wdcftgg.witherstormmod.client.util;

import com.wdcftgg.witherstormmod.client.AmuletAnimationHelper;
import com.wdcftgg.witherstormmod.common.item.FormidiBladeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderSpecificHandEvent;




public final class FormidiBladeAnimationHelper {

    private FormidiBladeAnimationHelper() {
    }

    public static void onRenderItemInHand(RenderSpecificHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof FormidiBladeItem)) return;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;
        NBTTagCompound tag = FormidiBladeItem.getBladeTag(stack, false);
        float power = Math.min(1.0F, tag.getFloat(FormidiBladeItem.POWER));
        if (power <= 0.0F) return;

        EnumHandSide mainHand = Minecraft.getMinecraft().gameSettings.mainHand;
        boolean rightSide = event.getHand() == EnumHand.MAIN_HAND
                ? mainHand == EnumHandSide.RIGHT
                : mainHand == EnumHandSide.LEFT;
        AmuletAnimationHelper.drawGlare(rightSide, 1.0F, -0.33D, 0.089D, -0.43D,
                0.5058824F, 0.0F, 0.7764706F, 0.0F,
                player.ticksExisted, event.getPartialTicks(), power);
    }
}
