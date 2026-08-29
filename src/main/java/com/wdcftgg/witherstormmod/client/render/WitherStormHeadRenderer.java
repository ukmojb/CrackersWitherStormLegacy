package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.client.model.WitherStormHeadModel;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.client.renderer.entity.RenderManager;

public final class WitherStormHeadRenderer extends StormPartRenderer<SupplementalEntities.WitherStormHeadEntity> {
    public WitherStormHeadRenderer(RenderManager manager) {




        super(manager, new WitherStormHeadModel(), 3.5F,
                "textures/entity/wither_storm_head/wither_storm_head.png", 2.0F);
        addLayer(new WitherStormHeadEyesLayer(this));
    }
}
