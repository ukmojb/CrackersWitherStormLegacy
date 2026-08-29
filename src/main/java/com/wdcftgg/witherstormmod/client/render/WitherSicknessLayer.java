package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessCapability;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessTracker;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;


public final class WitherSicknessLayer<T extends EntityLivingBase, M extends ModelBase>
        implements LayerRenderer<T> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID,
            "textures/entity/wither_sickness_layer/wither_sickness_layer.png");
    private final RenderLivingBase<T> renderer;
    private final M model;

    public WitherSicknessLayer(RenderLivingBase<T> renderer, M model) {
        this.renderer = renderer;
        this.model = model;
    }

    @Override
    public void doRenderLayer(T entity, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (!WitherStormClientConfig.witherSicknessLayer || entity.isInvisible()) return;
        WitherSicknessTracker tracker = WitherSicknessCapability.get(entity);
        if (tracker == null || tracker.isActuallyImmune()
                || !tracker.isInfected() && !tracker.isBeingCured()) return;

        float healthRatio = 0.8F - entity.getHealth() / entity.getMaxHealth();
        float alpha = tracker.getDelayTicks()
                / (float) Math.max(1, tracker.getApplicationDelay())
                * 0.5F * ((MathHelper.cos(
                (entity.ticksExisted + partialTicks) * healthRatio) + 2.0F) * 0.25F) + 0.2F;
        if (tracker.isBeingCured()) {
            alpha = ((float) tracker.getCureDelay() - tracker.getCureDelayTicks())
                    / (float) Math.max(1, tracker.getCureDelay())
                    * 0.5F * (alpha * 2.0F);
        }
        alpha = MathHelper.clamp(alpha, 0.0F, 1.0F);
        if (alpha <= 0.01F) return;

        renderer.bindTexture(TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        model.setModelAttributes(renderer.getMainModel());
        model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
