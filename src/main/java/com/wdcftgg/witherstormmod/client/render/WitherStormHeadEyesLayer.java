package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.client.model.WitherStormHeadModel;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;


public final class WitherStormHeadEyesLayer implements LayerRenderer<SupplementalEntities.WitherStormHeadEntity> {
    private static final ResourceLocation EMISSIVE = new ResourceLocation(Tags.MOD_ID,
            "textures/entity/wither_storm_head/wither_storm_head_emissive.png");
    private static final ResourceLocation EMISSIVE_HURT = new ResourceLocation(Tags.MOD_ID,
            "textures/entity/wither_storm_head/wither_storm_head_emissive_hurt.png");
    private final StormPartRenderer<SupplementalEntities.WitherStormHeadEntity> renderer;

    public WitherStormHeadEyesLayer(StormPartRenderer<SupplementalEntities.WitherStormHeadEntity> renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(SupplementalEntities.WitherStormHeadEntity entity, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        boolean additive = !entity.isPlayingDead();
        ResourceLocation texture = additive && entity.isHurt() ? EMISSIVE_HURT : EMISSIVE;
        renderer.bindTexture(texture);
        float previousBrightnessX = OpenGlHelper.lastBrightnessX;
        float previousBrightnessY = OpenGlHelper.lastBrightnessY;
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean previousLighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        if (additive) {



            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.disableCull();
            GlStateManager.disableLighting();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
        } else {
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableCull();
            GlStateManager.depthMask(true);
        }



        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                additive ? 240.0F : 0.0F, 240.0F);
        try {
            WitherStormHeadModel model = (WitherStormHeadModel) renderer.getMainModel();
            model.setModelAttributes(renderer.getMainModel());
            model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        } finally {
            GlStateManager.depthFunc(previousDepthFunc);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                    previousBrightnessX, previousBrightnessY);
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            if (previousLighting) GlStateManager.enableLighting();
            else GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
