package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.common.init.ModPaintings;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPainting;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.util.ResourceLocation;


public class WitherStormPaintingRenderer extends RenderPainting {

    public WitherStormPaintingRenderer(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPainting entity) {
        if (entity.art == ModPaintings.AMULET) {
            AmuletPaintingAtlas.ensureLoaded();
            return AmuletPaintingAtlas.ATLAS;
        }
        return super.getEntityTexture(entity);
    }
}
