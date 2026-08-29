package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.client.model.SantaHatModel;
import com.wdcftgg.witherstormmod.client.model.WitherStormPhaseModel;
import com.wdcftgg.witherstormmod.client.util.SpecialDay;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;


public class SantaHatLayer implements LayerRenderer<WitherStormEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Tags.MOD_ID, "textures/misc/santa_hat.png");

    private final SantaHatModel model = new SantaHatModel();
    private final WitherStormRenderer renderer;

    public SantaHatLayer(WitherStormRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(WitherStormEntity entity, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (SpecialDay.getForCurrentDate() != SpecialDay.CHRISTMAS) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        ((WitherStormPhaseModel) renderer.getMainModel()).renderSantaHats(entity, model, scale);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
