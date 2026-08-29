package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.client.model.WitherStormPhaseModel;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;


public final class WitherStormArmorLayer implements LayerRenderer<WitherStormEntity> {
    private static final ResourceLocation WITHER_ARMOR =
            new ResourceLocation("textures/entity/wither/wither_armor.png");

    private final WitherStormRenderer renderer;
    private final WitherStormPhaseModel armorModel =
            new WitherStormPhaseModel(WitherStormPhaseModel.Form.COMMAND_BLOCK);

    public WitherStormArmorLayer(WitherStormRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(WitherStormEntity entity, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (!entity.isArmored()) return;

        renderer.bindTexture(WITHER_ARMOR);
        GlStateManager.depthMask(!entity.isInvisible());
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.loadIdentity();
        GlStateManager.translate(MathHelper.cos(ageInTicks * 0.02F) * 3.0F,
                ageInTicks * 0.01F, 0.0F);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.enableBlend();
        GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE);

        armorModel.setModelAttributes(renderer.getMainModel());
        armorModel.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
        try {
            armorModel.render(entity, limbSwing, limbSwingAmount, ageInTicks,
                    netHeadYaw, headPitch, scale);
        } finally {
            Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.loadIdentity();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(true);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
