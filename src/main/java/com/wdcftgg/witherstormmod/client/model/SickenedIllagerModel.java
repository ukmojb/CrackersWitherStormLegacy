package com.wdcftgg.witherstormmod.client.model;

import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

public class SickenedIllagerModel extends ModelBase {

    private final boolean crossbowPose;
    private final ModelRenderer head;
    private final ModelRenderer body;
    private final ModelRenderer arms;
    private final ModelRenderer rightArm;
    private final ModelRenderer leftArm;
    private final ModelRenderer rightLeg;
    private final ModelRenderer leftLeg;

    public SickenedIllagerModel(boolean crossbowPose) {
        this.crossbowPose = crossbowPose;
        textureWidth = 64;
        textureHeight = 64;
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8);
        head.setTextureOffset(24, 0).addBox(-1.0F, -3.0F, -6.0F, 2, 4, 2);
        body = new ModelRenderer(this, 16, 20);
        body.addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6);
        body.setTextureOffset(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6, 0.5F);
        arms = new ModelRenderer(this);
        arms.setRotationPoint(0.0F, 2.0F, 0.0F);
        arms.setTextureOffset(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4, 8, 4);
        ModelRenderer crossedLeftArm = new ModelRenderer(this, 44, 22);
        crossedLeftArm.mirror = true;
        crossedLeftArm.addBox(4.0F, -2.0F, -2.0F, 4, 8, 4);
        arms.addChild(crossedLeftArm);
        arms.setTextureOffset(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8, 4, 4);
        rightArm = new ModelRenderer(this, 40, 46);
        rightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4);
        rightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        leftArm = new ModelRenderer(this, 40, 46);
        leftArm.mirror = true;
        leftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4);
        leftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        rightLeg = new ModelRenderer(this, 0, 22);
        rightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4);
        rightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);
        leftLeg = new ModelRenderer(this, 0, 22);
        leftLeg.mirror = true;
        leftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4);
        leftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                       float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        head.render(scale);
        body.render(scale);
        if (!crossbowPose && !isAggressive(entity)) {
            arms.render(scale);
        } else {
            rightArm.render(scale);
            leftArm.render(scale);
        }
        rightLeg.render(scale);
        leftLeg.render(scale);
    }

    public void postRenderArm(float scale, EnumHandSide side) {
        (side == EnumHandSide.RIGHT ? rightArm : leftArm).postRender(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {
        head.rotateAngleY = netHeadYaw * 0.017453292F;
        head.rotateAngleX = headPitch * 0.017453292F;
        rightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * 0.5F;
        leftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount * 0.5F;
        rightArm.rotateAngleZ = leftArm.rotateAngleZ = 0.0F;
        arms.rotationPointY = 3.0F;
        arms.rotationPointZ = -1.0F;
        arms.rotateAngleX = -0.75F;
        if (crossbowPose) {
            rightArm.rotateAngleY = -0.3F + head.rotateAngleY;
            leftArm.rotateAngleY = 0.6F + head.rotateAngleY;
            rightArm.rotateAngleX = -1.5708F + head.rotateAngleX;
            leftArm.rotateAngleX = -1.5F + head.rotateAngleX;
        } else if (isAggressive(entity)) {
            float swing = MathHelper.sin(swingProgress * (float) Math.PI);
            float eased = MathHelper.sin((1.0F - (1.0F - swingProgress) * (1.0F - swingProgress)) * (float) Math.PI);
            rightArm.rotateAngleY = 0.15707964F;
            leftArm.rotateAngleY = -0.15707964F;
            rightArm.rotateAngleX = -1.8849558F + swing * 2.2F - eased * 0.4F;
            leftArm.rotateAngleX = swing * 1.2F - eased * 0.4F;
            rightArm.rotateAngleZ = MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
            leftArm.rotateAngleZ = -MathHelper.cos(ageInTicks * 0.09F) * 0.05F - 0.05F;
        }
    }

    private boolean isAggressive(Entity entity) {
        return entity instanceof SickenedEntities.SickenedVindicatorEntity
                && ((SickenedEntities.SickenedVindicatorEntity) entity).isAggressive();
    }
}
