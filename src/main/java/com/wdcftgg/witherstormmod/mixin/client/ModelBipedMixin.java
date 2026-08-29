package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.client.PhasometerOverlay;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ModelBiped.class)
public abstract class ModelBipedMixin {

    @Shadow public ModelRenderer bipedHead;
    @Shadow public ModelRenderer bipedRightArm;
    @Shadow public ModelRenderer bipedLeftArm;

    @Inject(method = "setRotationAngles", at = @At("TAIL"))
    private void witherstormmod$posePhasometerArm(float limbSwing, float limbSwingAmount,
                                                  float ageInTicks, float netHeadYaw,
                                                  float headPitch, float scaleFactor,
                                                  Entity entity, CallbackInfo callback) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (!PhasometerOverlay.isScoping(player)) return;

        EnumHandSide activeSide = player.getActiveHand() == EnumHand.MAIN_HAND
                ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        ModelRenderer arm = activeSide == EnumHandSide.RIGHT ? bipedRightArm : bipedLeftArm;
        float side = activeSide == EnumHandSide.RIGHT ? -1.0F : 1.0F;
        float crouchingOffset = player.isSneaking() ? 0.2617994F : 0.0F;
        arm.rotateAngleX = MathHelper.clamp(
                bipedHead.rotateAngleX - 1.9198622F - crouchingOffset, -2.4F, 3.3F);
        arm.rotateAngleY = bipedHead.rotateAngleY + side * 0.2617994F;
        arm.rotateAngleZ = MathHelper.clamp(
                bipedHead.rotateAngleZ + side * 0.2617994F, -1.2F, 1.2F);
    }
}
