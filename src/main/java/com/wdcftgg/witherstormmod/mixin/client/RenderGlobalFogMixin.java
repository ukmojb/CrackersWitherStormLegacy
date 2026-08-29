package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.client.OptifineCompat;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;






@Mixin(RenderGlobal.class)
public abstract class RenderGlobalFogMixin {
    @Redirect(method = "renderSky(FI)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GlStateManager;enableFog()V"))
    private void witherstormmod$skipSkyFog() {
        if (!shouldDisableFog()) {
            GlStateManager.enableFog();
        }
    }

    @Redirect(method = "renderSky(FI)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldProvider;isSkyColored()Z"))
    private boolean witherstormmod$removeFoglessLowerSkySeam(WorldProvider provider) {
        return !shouldDisableFog() && provider.isSkyColored();
    }

    private static boolean shouldDisableFog() {
        return WitherStormClientConfig.disableVanillaFog && !OptifineCompat.isLoaded();
    }
}
