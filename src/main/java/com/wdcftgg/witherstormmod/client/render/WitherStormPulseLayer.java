package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.client.model.WitherStormPhaseModel;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;


public final class WitherStormPulseLayer implements LayerRenderer<WitherStormEntity> {
    private static final ResourceLocation PULSE_TEXTURE = new ResourceLocation(Tags.MOD_ID,
            "textures/entity/wither_storm/wither_storm_pulse.png");
    private final WitherStormRenderer renderer;

    public WitherStormPulseLayer(WitherStormRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(WitherStormEntity entity, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (!WitherStormClientConfig.renderPulse || !entity.isBeingTornApart()
                || entity.isDead) return;
        renderer.bindTexture(PULSE_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        WitherStormPhaseModel model = (WitherStormPhaseModel) renderer.getMainModel();
        model.renderPulse(entity, partialTicks, scale);
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
