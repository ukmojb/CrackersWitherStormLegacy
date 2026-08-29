package com.wdcftgg.witherstormmod.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(targets = "thedarkcolour.futuremc.entity.trident.RenderTrident", remap = false)
public abstract class FutureMcTridentRendererMixin {
    @Redirect(
            method = "doRender(Lthedarkcolour/futuremc/entity/trident/Trident;DDDFF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GlStateManager;disableLighting()V",
                    ordinal = 0, remap = true),
            remap = false)
    private void witherstormmod$keepPrimaryModelLighting() {
    }
}
