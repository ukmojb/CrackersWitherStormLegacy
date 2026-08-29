package com.wdcftgg.witherstormmod.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;


public final class WitherSicknessLayerInstaller {

    private WitherSicknessLayerInstaller() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void install() {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        for (Render render : manager.entityRenderMap.values()) {
            if (render instanceof RenderLivingBase) {
                RenderLivingBase living = (RenderLivingBase) render;
                living.addLayer(new WitherSicknessLayer(living, living.getMainModel()));
            }
        }
    }
}
