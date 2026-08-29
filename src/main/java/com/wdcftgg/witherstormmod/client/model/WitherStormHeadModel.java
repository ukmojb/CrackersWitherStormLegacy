package com.wdcftgg.witherstormmod.client.model;

import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

public class WitherStormHeadModel extends ModelBase {
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0D);

    private final ModelRenderer head;
    private final ModelRenderer upperJaw;
    private final ModelRenderer lowerJaw;

    public WitherStormHeadModel() {
        textureWidth = 160;
        textureHeight = 160;
        head = new ModelRenderer(this);
        head.setRotationPoint(0.0F, 0.0F, 0.0F);

        upperJaw = new ModelRenderer(this, 0, 65);
        upperJaw.setRotationPoint(0.0F, 2.5F, 0.0F);
        upperJaw.addBox(-4.0F, -6.5F, 12.0F, 8, 6, 2, 0.0F);
        upperJaw.setTextureOffset(0, 47).addBox(-2.0F, -8.5F, 10.0F, 4, 2, 2);
        upperJaw.setTextureOffset(0, 35).addBox(-4.0F, -8.5F, 0.0F, 8, 2, 10);
        upperJaw.setTextureOffset(0, 47).addBox(-6.0F, -6.5F, 0.0F, 12, 6, 12);
        ModelBuilders.addBox(upperJaw, 4, 13, -1.0F, -4.5F, 13.1F,
                2.0F, 2.0F, 1.0F, 0.0F, 0.2F, 0.2F, false);
        ModelRenderer upperTeeth = new ModelRenderer(this);
        upperTeeth.setRotationPoint(0.0F, 0.5F, 0.0F);
        addUpperTeeth(upperTeeth);
        upperJaw.addChild(upperTeeth);

        lowerJaw = new ModelRenderer(this, 0, 73);
        lowerJaw.setRotationPoint(0.0F, 2.5F, 0.0F);
        lowerJaw.addBox(-4.0F, 0.5F, 12.0F, 8, 2, 2);
        lowerJaw.setTextureOffset(48, 0).addBox(-6.0F, 0.5F, 0.0F, 12, 2, 12);
        ModelRenderer lowerTeeth = new ModelRenderer(this);
        lowerTeeth.setRotationPoint(0.0F, 2.5F, 8.0F);
        addLowerTeeth(lowerTeeth);
        lowerJaw.addChild(lowerTeeth);
        head.addChild(upperJaw);
        head.addChild(lowerJaw);
    }

    private void addUpperTeeth(ModelRenderer teethRenderer) {
        int[][] teeth = {{-1, 13}, {-3, 12}, {-5, 11}, {-6, 9}, {-6, 7}, {-6, 5}, {-6, 3}, {-6, 1},
                {1, 13}, {3, 12}, {4, 10}, {5, 8}, {5, 6}, {5, 4}, {5, 2}, {5, 0}};
        for (int[] tooth : teeth) teethRenderer.setTextureOffset(0, 54)
                .addBox(tooth[0], -1.0F, tooth[1], 1, 1, 1);
    }

    private void addLowerTeeth(ModelRenderer teethRenderer) {
        int[][] teeth = {{0, 5}, {2, 4}, {4, 3}, {5, 1}, {5, -1}, {5, -3}, {5, -5}, {5, -7},
                {-2, 5}, {-4, 4}, {-5, 2}, {-6, 0}, {-6, -2}, {-6, -4}, {-6, -6}, {-6, -8}};
        for (int[] tooth : teeth) teethRenderer.setTextureOffset(0, 54)
                .addBox(tooth[0], -3.0F, tooth[1], 1, 1, 1);
    }

    @Override
    public void render(Entity entity, float limbSwing, float amount, float age, float yaw, float pitch, float scale) {





        GlStateManager.pushMatrix();
        GlStateManager.scale(3.0F, 3.0F, 3.0F);

        head.rotateAngleY = 3.1416F + yaw * DEGREES_TO_RADIANS;
        head.rotateAngleX = -pitch * DEGREES_TO_RADIANS;
        head.rotateAngleZ = 0.0F;
        float partialTicks = MathHelper.clamp(age - entity.ticksExisted, 0.0F, 1.0F);
        SupplementalEntities.WitherStormHeadEntity stormHead =
                (SupplementalEntities.WitherStormHeadEntity) entity;
        float ticks = stormHead.isDeadOrPlayingDead() ? 0.0F : age;
        lowerJaw.rotateAngleX = WitherStormHeadAnimation.jawPitch(
                stormHead.getMouthAnimation(partialTicks), ticks, 0.0F);
        lowerJaw.rotateAngleZ = WitherStormHeadAnimation.brokenJawRoll(stormHead, 0,
                stormHead.getBrokenJawAnimation(0, partialTicks));
        head.rotateAngleZ = stormHead.getHeadShakeAnimation(partialTicks);
        head.render(scale);
        GlStateManager.popMatrix();
    }
}
