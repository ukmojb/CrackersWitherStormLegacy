package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.client.model.WitheredSymbiontModel;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.entity.Entity;

public final class WitheredSymbiontArmorLayer extends LayerBipedArmor {
    public WitheredSymbiontArmorLayer(WitheredSymbiontRenderer renderer) {
        super(renderer);
    }

    @Override
    protected void initArmor() {
        modelLeggings = new SymbiontArmorModel(0.5F);
        modelArmor = new SymbiontArmorModel(1.0F);
    }

    private static final class SymbiontArmorModel extends ModelBiped {
        private SymbiontArmorModel(float scale) {
            super(scale);
        }

        @Override
        public void setModelAttributes(ModelBase model) {
            super.setModelAttributes(model);
            if (model instanceof WitheredSymbiontModel) {
                ((WitheredSymbiontModel) model).copyBipedPoseTo(this);
            }
        }

        @Override
        public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                      float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {

        }
    }
}
