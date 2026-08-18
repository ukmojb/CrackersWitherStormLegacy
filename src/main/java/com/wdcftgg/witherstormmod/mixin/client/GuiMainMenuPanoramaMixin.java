package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.client.PanoramaCustomizer;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.12.2 equivalent of the upstream title-screen panorama override. */
@Mixin(GuiMainMenu.class)
public abstract class GuiMainMenuPanoramaMixin {
    private static final ResourceLocation CUSTOM_PANORAMA = new ResourceLocation(Tags.MOD_ID,
            "textures/gui/title/background/panorama_0.png");

    /** Update the static six-face array before GuiMainMenu renders its first frame. */
    @Inject(method = "initGui", at = @At("HEAD"))
    private void witherstormmod$syncPanoramaBeforeRender(CallbackInfo callback) {
        PanoramaCustomizer.sync();
    }

    @Redirect(method = "drawPanorama", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void witherstormmod$bindPanorama(TextureManager textureManager, ResourceLocation original) {
        if (WitherStormClientConfig.customPanorama
                && "minecraft:textures/gui/title/background/panorama_0.png".equals(original.toString())) {
            textureManager.bindTexture(CUSTOM_PANORAMA);
        } else {
            textureManager.bindTexture(original);
        }
    }
}
