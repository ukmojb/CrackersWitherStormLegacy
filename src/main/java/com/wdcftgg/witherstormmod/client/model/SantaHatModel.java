package com.wdcftgg.witherstormmod.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;


public class SantaHatModel extends ModelBase {

    private final ModelRenderer base;

    public SantaHatModel() {
        textureWidth = 64;
        textureHeight = 64;
        base = new ModelRenderer(this, 0, 0);
        base.addBox(-6.0F, -3.0F, -6.0F, 12, 3, 12);
        base.setRotationPoint(0.0F, -4.0F, 6.1F);

        ModelRenderer segment1 = new ModelRenderer(this, 0, 15);
        segment1.addBox(-5.0F, -3.0F, -5.0F, 10, 4, 10);
        segment1.setRotationPoint(0.0F, -3.0F, 0.0F);
        segment1.rotateAngleZ = -0.1222F;
        base.addChild(segment1);

        ModelRenderer segment2 = new ModelRenderer(this, 0, 29);
        segment2.addBox(-4.0F, -3.0F, -4.0F, 8, 5, 8);
        segment2.setRotationPoint(0.0F, -2.0F, 0.0F);
        segment2.rotateAngleZ = -0.6196F;
        segment1.addChild(segment2);

        ModelRenderer segment3 = new ModelRenderer(this, 30, 15);
        segment3.addBox(-3.0F, -3.0F, -3.0F, 6, 4, 6);
        segment3.setRotationPoint(0.0F, -3.0F, 0.0F);
        segment3.rotateAngleZ = -0.3229F;
        segment2.addChild(segment3);

        ModelRenderer segment4 = new ModelRenderer(this, 24, 29);
        segment4.addBox(-2.0F, -2.0F, -2.0F, 4, 3, 4);
        segment4.setRotationPoint(0.25F, -3.0F, 0.0F);
        segment4.rotateAngleZ = -0.4887F;
        segment3.addChild(segment4);

        ModelRenderer tip = new ModelRenderer(this, 0, 0);
        tip.addBox(-1.0F, -2.0F, -1.0F, 2, 2, 2);
        tip.setRotationPoint(0.0F, -1.7F, 0.0F);
        tip.rotateAngleZ = -0.5672F;
        segment4.addChild(tip);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                       float netHeadYaw, float headPitch, float scale) {
        base.render(scale);
    }
}
