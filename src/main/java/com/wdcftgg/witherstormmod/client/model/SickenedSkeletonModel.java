package com.wdcftgg.witherstormmod.client.model;

import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

public class SickenedSkeletonModel extends ModelBiped {
    public SickenedSkeletonModel() {
        super(0.0F, 0.0F, 64, 32);
        bipedRightArm = new ModelRenderer(this, 40, 16);
        bipedRightArm.addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2, 0.0F);
        bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        bipedLeftArm = new ModelRenderer(this, 40, 16);
        bipedLeftArm.mirror = true;
        bipedLeftArm.addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2, 0.0F);
        bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        bipedRightLeg = new ModelRenderer(this, 0, 16);
        bipedRightLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2, 0.0F);
        bipedRightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);
        bipedLeftLeg = new ModelRenderer(this, 0, 16);
        bipedLeftLeg.mirror = true;
        bipedLeftLeg.addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2, 0.0F);
        bipedLeftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        rightArmPose = ArmPose.EMPTY;
        leftArmPose = ArmPose.EMPTY;
        ItemStack held = entity.getHeldItem(EnumHand.MAIN_HAND);
        if (held.getItem() instanceof ItemBow && isSwingingArms(entity)) {
            if (entity.getPrimaryHand() == EnumHandSide.RIGHT) {
                rightArmPose = ArmPose.BOW_AND_ARROW;
            } else {
                leftArmPose = ArmPose.BOW_AND_ARROW;
            }
        }
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scaleFactor, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity);
        EntityLivingBase living = (EntityLivingBase) entity;
        ItemStack held = living.getHeldItemMainhand();
        if (isSwingingArms(living) && (held.isEmpty() || !(held.getItem() instanceof ItemBow))) {
            float swing = MathHelper.sin(swingProgress * (float) Math.PI);
            float easedSwing = MathHelper.sin((1.0F - (1.0F - swingProgress) * (1.0F - swingProgress)) * (float) Math.PI);
            bipedRightArm.rotateAngleZ = 0.0F;
            bipedLeftArm.rotateAngleZ = 0.0F;
            bipedRightArm.rotateAngleY = -(0.1F - swing * 0.6F);
            bipedLeftArm.rotateAngleY = 0.1F - swing * 0.6F;
            bipedRightArm.rotateAngleX = -((float) Math.PI / 2.0F) - (swing * 1.2F - easedSwing * 0.4F);
            bipedLeftArm.rotateAngleX = -((float) Math.PI / 2.0F) - (swing * 1.2F - easedSwing * 0.4F);
            bipedRightArm.rotateAngleZ += MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
            bipedLeftArm.rotateAngleZ -= MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
            bipedRightArm.rotateAngleX += MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
            bipedLeftArm.rotateAngleX -= MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
        }
    }

    private boolean isSwingingArms(EntityLivingBase entity) {
        return entity instanceof SickenedEntities.SickenedSkeletonEntity
                && ((SickenedEntities.SickenedSkeletonEntity) entity).isSwingingArms();
    }
}
