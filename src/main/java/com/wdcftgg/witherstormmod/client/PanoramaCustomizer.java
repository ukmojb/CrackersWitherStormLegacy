package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/** 上游 MixinTitleScreen 的 1.12 实现：按当前配置选择主菜单全景第 0 面。 */
public final class PanoramaCustomizer {
    private static final ResourceLocation CUSTOM_FACE = new ResourceLocation(Tags.MOD_ID,
            "textures/gui/title/background/panorama_0.png");

    private static ResourceLocation[] panoramaPaths;
    private static ResourceLocation originalFace;
    private static boolean resolutionAttempted;

    private PanoramaCustomizer() {
    }

    public static void sync() {
        resolvePanoramaPaths();
        if (panoramaPaths == null || panoramaPaths.length == 0 || originalFace == null) return;
        ResourceLocation desired = WitherStormClientConfig.customPanorama
                ? CUSTOM_FACE : originalFace;
        if (!desired.equals(panoramaPaths[0])) {
            panoramaPaths[0] = desired;
            WitherStormMod.LOGGER.info("Main menu panorama face 0 set to {}", desired);
        }
    }

    private static void resolvePanoramaPaths() {
        if (resolutionAttempted) return;
        resolutionAttempted = true;
        try {
            Field field = ObfuscationReflectionHelper.findField(
                    GuiMainMenu.class, "field_73978_o");
            field.setAccessible(true);
            panoramaPaths = (ResourceLocation[]) field.get(null);
            if (panoramaPaths != null && panoramaPaths.length > 0) {
                originalFace = panoramaPaths[0];
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            WitherStormMod.LOGGER.warn("Unable to access the main menu panorama",
                    exception);
        }
    }
}
