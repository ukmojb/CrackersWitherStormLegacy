package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.Tags;
import net.minecraft.client.renderer.entity.RenderPig;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(RenderPig.class)
public abstract class PigRendererMixin {

    private static final ResourceLocation REUBEN_TEXTURE = new ResourceLocation(Tags.MOD_ID,
            "textures/entity/misc/reuben.png");

    @Inject(method = "getEntityTexture(Lnet/minecraft/entity/passive/EntityPig;)"
            + "Lnet/minecraft/util/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void witherstormmod$reubenTexture(EntityPig pig,
                                              CallbackInfoReturnable<ResourceLocation> callback) {
        if (pig.hasCustomName() && "reuben".equals(pig.getCustomNameTag())) {
            callback.setReturnValue(REUBEN_TEXTURE);
        }
    }
}
