package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.client.render.LegacyRenderBufferer;
import com.wdcftgg.witherstormmod.client.shader.PostProcessingShaders;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Minecraft.class)
public abstract class MinecraftLifecycleMixin {
    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void witherstormmod$shutdownRenderBufferer(CallbackInfo callback) {
        LegacyRenderBufferer.INSTANCE.shutdown();
        PostProcessingShaders.INSTANCE.shutdown();
    }
}
