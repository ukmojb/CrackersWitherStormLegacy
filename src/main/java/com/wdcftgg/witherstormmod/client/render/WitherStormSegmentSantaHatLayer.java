package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.client.model.SantaHatModel;
import com.wdcftgg.witherstormmod.client.model.WitherStormSegmentModel;
import com.wdcftgg.witherstormmod.client.util.SpecialDay;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;


public final class WitherStormSegmentSantaHatLayer
        implements LayerRenderer<SupplementalEntities.WitherStormSegmentEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Tags.MOD_ID, "textures/misc/santa_hat.png");
    private final SantaHatModel model = new SantaHatModel();
    private final WitherStormSegmentRenderer renderer;

    public WitherStormSegmentSantaHatLayer(WitherStormSegmentRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(SupplementalEntities.WitherStormSegmentEntity entity,
                              float limbSwing, float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (SpecialDay.getForCurrentDate() != SpecialDay.CHRISTMAS) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        ((WitherStormSegmentModel) renderer.getMainModel()).renderSantaHats(entity, model, scale);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
