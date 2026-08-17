package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.client.model.SickenedIllagerModel;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHandSide;

public final class SickenedIllagerHeldItemLayer extends LayerHeldItem {
    private final boolean aggressiveOnly;

    public SickenedIllagerHeldItemLayer(RenderLivingBase<?> renderer, boolean aggressiveOnly) {
        super(renderer);
        this.aggressiveOnly = aggressiveOnly;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (!aggressiveOnly || entity instanceof SickenedEntities.SickenedVindicatorEntity
                && ((SickenedEntities.SickenedVindicatorEntity) entity).isAggressive()) {
            super.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks,
                    ageInTicks, netHeadYaw, headPitch, scale);
        }
    }

    @Override
    protected void translateToHand(EnumHandSide side) {
        ((SickenedIllagerModel) livingEntityRenderer.getMainModel())
                .postRenderArm(0.0625F, side);
    }
}
