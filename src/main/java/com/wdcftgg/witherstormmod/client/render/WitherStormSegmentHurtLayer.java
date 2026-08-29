package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.client.model.WitherStormSegmentModel;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;


public final class WitherStormSegmentHurtLayer
        implements LayerRenderer<SupplementalEntities.WitherStormSegmentEntity> {
    private static final ResourceLocation HURT_OVERLAY = new ResourceLocation(Tags.MOD_ID,
            "textures/entity/wither_storm/wither_storm_hurt_overlay.png");
    private final WitherStormSegmentRenderer renderer;

    public WitherStormSegmentHurtLayer(WitherStormSegmentRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(SupplementalEntities.WitherStormSegmentEntity entity,
                              float limbSwing, float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        renderer.bindTexture(HURT_OVERLAY);
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(true);
        if (entity.getDeathTime() > 0) GlStateManager.depthFunc(GL11.GL_EQUAL);
        ((WitherStormSegmentModel) renderer.getMainModel()).renderHeads(entity, scale,
                head -> entity.isHeadInjured(head));
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
