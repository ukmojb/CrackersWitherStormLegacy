package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.client.PhasometerOverlay;
import com.wdcftgg.witherstormmod.common.item.PhasometerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(LayerHeldItem.class)
public abstract class LayerHeldItemMixin {

    @Shadow @Final protected RenderLivingBase<?> livingEntityRenderer;

    @Inject(method = "renderHeldItem", at = @At("HEAD"), cancellable = true)
    private void witherstormmod$renderPhasometerAtEye(EntityLivingBase entity, ItemStack stack,
                                                       ItemCameraTransforms.TransformType transform,
                                                       EnumHandSide handSide,
                                                       CallbackInfo callback) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (!PhasometerOverlay.isScoping(player)
                || !(stack.getItem() instanceof PhasometerItem)
                || !(livingEntityRenderer.getMainModel() instanceof ModelBiped)) return;
        EnumHandSide activeSide = player.getActiveHand() == EnumHand.MAIN_HAND
                ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        if (handSide != activeSide) return;

        callback.cancel();
        ModelBiped model = (ModelBiped) livingEntityRenderer.getMainModel();
        GlStateManager.pushMatrix();
        try {
            if (entity.isSneaking()) GlStateManager.translate(0.0F, 0.2F, 0.0F);
            model.bipedHead.postRender(0.0625F);
            GlStateManager.translate(
                    (handSide == EnumHandSide.LEFT ? -2.5F : 2.5F) / 16.0F,
                    -0.0625F, 0.0F);
            Minecraft.getMinecraft().getItemRenderer().renderItemSide(entity, stack,
                    ItemCameraTransforms.TransformType.HEAD,
                    handSide == EnumHandSide.LEFT);
        } finally {
            GlStateManager.popMatrix();
        }
    }
}
