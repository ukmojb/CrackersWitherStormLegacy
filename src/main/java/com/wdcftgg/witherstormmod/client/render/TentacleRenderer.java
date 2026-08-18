package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.client.model.TentacleModel;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/** 1.12 equivalent of the upstream non-living tentacle renderer. */
public final class TentacleRenderer extends RenderLivingBase<SickenedEntities.TentacleEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            Tags.MOD_ID, "textures/entity/tentacle/tentacle.png");

    private final TentacleModel model;

    public TentacleRenderer(RenderManager manager) {
        super(manager, new TentacleModel(), 0.5F);
        model = (TentacleModel) mainModel;
    }

    @Override
    protected ResourceLocation getEntityTexture(SickenedEntities.TentacleEntity entity) {
        return TEXTURE;
    }

    @Override
    public void doRender(SickenedEntities.TentacleEntity entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        // RenderLivingBase applies this corpse roll before model rendering.
        // This renderer owns the draw path, so reproduce the same 1.12
        // death-time interpolation instead of leaving field tentacles frozen
        // upright for their entire vanilla 20-tick death window.
        if (entity.deathTime > 0) {
            float progress = (entity.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            progress = MathHelper.sqrt(Math.min(progress, 1.0F));
            GlStateManager.rotate(progress * 90.0F, 0.0F, 0.0F, 1.0F);
        }
        GlStateManager.scale(-2.0F, -2.0F, 2.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableCull();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        bindEntityTexture(entity);
        boolean brightness = setDoRenderBrightness(entity, partialTicks);
        try {
            model.render(entity, partialTicks, 0.0F, 0.0F,
                    entity.rotationYaw, entity.rotationPitch, 0.0625F);
        } finally {
            if (brightness) unsetBrightness();
        }
        GlStateManager.enableCull();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }
}
