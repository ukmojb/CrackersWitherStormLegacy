package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;

public final class SickenedBipedRenderer<T extends SickenedMobEntity>
        extends SickenedMobRenderer<T> {

    public SickenedBipedRenderer(RenderManager manager, ModelBiped model, float shadowSize,
                                 String texturePath, boolean skeletonArmor) {
        super(manager, model, shadowSize, texturePath);
        addLayer(new LayerHeldItem(this));
        if (skeletonArmor) {
            addLayer(new LayerBipedArmor(this) {
                @Override
                protected void initArmor() {





                    modelLeggings = new ModelBiped(0.5F);
                    modelArmor = new ModelBiped(1.0F);
                }
            });
        } else {
            addLayer(new LayerBipedArmor(this));
        }
    }
}
