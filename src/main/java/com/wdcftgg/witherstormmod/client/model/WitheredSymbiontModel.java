package com.wdcftgg.witherstormmod.client.model;

import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;


public class WitheredSymbiontModel extends ModelBiped {
    private final ModelRenderer[] tentacleBases = new ModelRenderer[3];
    private final ModelRenderer[][] tentacles = new ModelRenderer[3][4];
    private float crouchAnimation;

    public WitheredSymbiontModel() {
        super(0.0F, 0.0F, 96, 96);

        bipedLeftLeg = new ModelRenderer(this, 16, 48);
        bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
        bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
        bipedHeadwear.showModel = false;

        addMass(bipedBody, 36, 32, -4.5F, -4.5F, -1.5F, 6, 9, 3,
                1.2478F, 5.7655F, 2.1684F, -0.0783F, -0.1909F, -0.2221F);
        addMass(bipedBody, 32, 4, -3.5F, -2.5F, -2.5F, 7, 9, 3,
                -0.6213F, 2.8595F, -1.0036F, 0.2661F, -0.1641F, 0.244F);
        addMass(bipedBody, 0, 32, -2.0F, -2.0F, -2.0F, 4, 4, 4,
                -2.0355F, 9.8385F, 1.8323F, -2.5517F, -0.5708F, 1.7651F);
        addMass(bipedBody, 16, 32, -2.5F, -0.5F, -4.5F, 5, 5, 5,
                1.5F, 6.5F, -1.5F, 0.6346F, -0.678F, -0.4326F);

        addMass(bipedRightLeg, 16, 32, -2.5F, -2.5F, 1.5F, 5, 5, 5,
                -1.5F, 7.5F, -1.5F, 0.0406F, 0.4854F, -1.2322F);
        addMass(bipedRightLeg, 16, 32, -2.5F, -2.5F, -2.5F, 5, 5, 5,
                -1.5F, 7.5F, -1.5F, 0.6929F, 0.4557F, 0.3503F);
        addMass(bipedRightLeg, 16, 32, -1.5F, -3.5F, -2.5F, 5, 5, 5,
                -1.5F, 3.5F, -1.5F, 0.432F, -0.8648F, -0.6805F);

        addMass(bipedRightArm, 0, 32, -2.5F, -1.5F, -1.5F, 4, 4, 4,
                -1.2211F, 4.1554F, 1.9027F, 1.6619F, -0.8156F, -1.5077F);
        addMass(bipedRightArm, 16, 32, -2.0F, 2.0F, 21.0F, 5, 5, 5,
                5.0F, 22.0F, 0.0F, 1.7759F, 0.1628F, -0.3449F);
        addMass(bipedRightArm, 16, 32, -2.0F, -15.0F, 12.0F, 5, 5, 5,
                5.0F, 22.0F, 0.0F, 0.9318F, -0.52F, -0.4963F);

        addMass(bipedHead, 0, 32, -2.0F, -2.0F, -2.0F, 4, 4, 4,
                -5.2146F, -1.96F, 2.8784F, 3.1231F, -0.7686F, -1.9387F);
        addMass(bipedHead, 16, 32, -1.5F, -2.5F, -2.5F, 5, 5, 5,
                -4.7943F, -4.4645F, -3.6255F, -2.4784F, -0.4058F, -2.4209F);
        addMass(bipedHead, 16, 32, -1.5F, -2.5F, -2.5F, 5, 5, 5,
                -2.5F, -7.5F, 2.5F, -2.4343F, -0.4891F, 2.7597F);

        createTentacle(0, -3.5F, 9.5F, 0.0F);
        createTentacle(1, 2.5F, 10.5F, 0.0F);
        createTentacle(2, -0.5F, 0.5F, 0.0F);
    }

    private void createTentacle(int index, float x, float y, float z) {
        ModelRenderer base = new ModelRenderer(this, 54, 30);
        base.setRotationPoint(x, y, z);
        tentacleBases[index] = base;
        bipedBody.addChild(base);

        int[][] textureOffsets = {{54, 30}, {72, 33}, {68, 19}, {58, 0}};
        float[] boxOffsets = {-1.5F, -1.3F, -0.9F, -0.5F};
        float[] widths = {3.0F, 2.6F, 1.8F, 1.0F};
        int[] lengths = {8, 9, 11, 16};
        float[] pivots = {0.0F, 9.0F, 9.0F, 11.0F};
        ModelRenderer parent = base;
        for (int segment = 0; segment < 4; segment++) {
            ModelRenderer part = new ModelRenderer(this,
                    textureOffsets[segment][0], textureOffsets[segment][1]);
            ModelBuilders.addBox(part, textureOffsets[segment][0], textureOffsets[segment][1],
                    boxOffsets[segment], boxOffsets[segment], segment == 0 ? 1.0F : 0.0F,
                    widths[segment], widths[segment], lengths[segment],
                    0.0F, 1.0F, 1.0F, false);
            if (segment > 0) part.setRotationPoint(0.0F, 0.0F, pivots[segment]);
            parent.addChild(part);
            tentacles[index][segment] = part;
            parent = part;
        }
    }

    private void addMass(ModelRenderer parent, int textureX, int textureY,
                         float boxX, float boxY, float boxZ,
                         int width, int height, int depth,
                         float pivotX, float pivotY, float pivotZ,
                         float rotateX, float rotateY, float rotateZ) {
        ModelRenderer mass = new ModelRenderer(this, textureX, textureY);
        mass.addBox(boxX, boxY, boxZ, width, height, depth, 0.0F);
        mass.setRotationPoint(pivotX, pivotY, pivotZ);
        mass.rotateAngleX = rotateX;
        mass.rotateAngleY = rotateY;
        mass.rotateAngleZ = rotateZ;
        parent.addChild(mass);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                    float partialTickTime) {
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTickTime);
        crouchAnimation = 0.0F;
        if (!(entity instanceof SickenedEntities.WitheredSymbiontEntity)) return;

        SickenedEntities.WitheredSymbiontEntity symbiont = (SickenedEntities.WitheredSymbiontEntity) entity;
        float animation = symbiont.isVulnerable() ? 0.0F : symbiont.ticksExisted + partialTickTime;
        animateTentacle(animation, limbSwing, 0, 0.0F, -30.0F, 10.0F);
        animateTentacle(animation, limbSwing, 1, 20.0F, 40.0F, 15.0F);
        animateTentacle(animation, limbSwing, 2, 40.0F, -10.0F, 50.0F);
        crouchAnimation = symbiont.getVulnerableAnim(partialTickTime);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scaleFactor, entity);
        if (!(entity instanceof SickenedEntities.WitheredSymbiontEntity)) return;

        SickenedEntities.WitheredSymbiontEntity symbiont = (SickenedEntities.WitheredSymbiontEntity) entity;
        float tick = symbiont.isVulnerable() ? 0.0F : ageInTicks;
        float attackSwing = MathHelper.sin(swingProgress * (float) Math.PI);
        float easedAttackSwing = MathHelper.sin(
                (1.0F - (1.0F - swingProgress) * (1.0F - swingProgress)) * (float) Math.PI);
        bipedRightArm.rotateAngleZ = 0.0F;
        bipedLeftArm.rotateAngleZ = 0.0F;
        bipedRightArm.rotateAngleY = -(0.1F - attackSwing * 0.6F);
        bipedLeftArm.rotateAngleY = 0.1F - attackSwing * 0.6F;
        float restingArmAngle = -(float) Math.PI /
                (symbiont.getAttackTarget() != null ? 1.5F : 2.25F);
        bipedRightArm.rotateAngleX = restingArmAngle + attackSwing * 1.2F - easedAttackSwing * 0.4F;
        bipedLeftArm.rotateAngleX = restingArmAngle + attackSwing * 1.2F - easedAttackSwing * 0.4F;
        bobArms(tick);

        bipedBody.rotateAngleX += crouchAnimation;
        bipedRightArm.rotateAngleX += crouchAnimation;
        bipedLeftArm.rotateAngleX += crouchAnimation;



        bipedRightLeg.rotationPointZ = crouchAnimation * 10.5F;
        bipedLeftLeg.rotationPointZ = crouchAnimation * 10.5F;
        bipedRightLeg.rotationPointY = 12.0F + crouchAnimation * 0.4F;
        bipedLeftLeg.rotationPointY = 12.0F + crouchAnimation * 0.4F;
        bipedHead.rotationPointY = crouchAnimation * 2.0F;
        bipedBody.rotationPointY = crouchAnimation * 3.0F;
        bipedLeftArm.rotationPointY = 2.0F + crouchAnimation * 3.0F;
        bipedRightArm.rotationPointY = 2.0F + crouchAnimation * 3.0F;
        bipedLeftArm.rotationPointZ = crouchAnimation * 2.0F;
        bipedRightArm.rotationPointZ = crouchAnimation * 2.0F;

        if (symbiont.isCastingSpell() || symbiont.isSummoningMobs()
                || !symbiont.isEntityAlive() || symbiont.hasAttackDelay()) {
            float spellWave = MathHelper.sin(ageInTicks * 0.5F);
            bipedLeftArm.rotateAngleY = -0.4F - 0.8F * spellWave;
            bipedRightArm.rotateAngleY = -0.4F + 0.8F * spellWave;
            bipedLeftArm.rotateAngleX = -2.5F;
            bipedRightArm.rotateAngleX = -2.5F;
        }
    }

    private void bobArms(float ageInTicks) {
        float roll = MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        float pitch = MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
        bipedRightArm.rotateAngleZ += roll;
        bipedLeftArm.rotateAngleZ -= roll;
        bipedRightArm.rotateAngleX += pitch;
        bipedLeftArm.rotateAngleX -= pitch;
    }

    private void animateTentacle(float animation, float additionalAnimation, int index,
                                 float offset, float xAngleOffset, float yAngleOffset) {
        float cosineWave = MathHelper.cos((animation + offset * 10.0F) * 0.04F) * 1.5F
                + MathHelper.cos(additionalAnimation);
        float sineWave = MathHelper.sin((animation + offset * 10.0F) * 0.02F) * 1.5F
                + MathHelper.sin(additionalAnimation);
        float segmentXOffset = yAngleOffset * 0.017453292F;
        float segmentYOffset = xAngleOffset * 0.017453292F;

        ModelRenderer base = tentacleBases[index];
        base.rotateAngleY = sineWave * cosineWave * 0.05F;
        base.rotateAngleX = cosineWave * sineWave * 0.05F;
        ModelRenderer[] chain = tentacles[index];
        chain[0].rotateAngleX = cosineWave * -0.1F;
        chain[1].rotateAngleX = cosineWave * 0.1F + segmentXOffset;
        chain[2].rotateAngleX = cosineWave * 0.075F + segmentXOffset;
        chain[3].rotateAngleX = cosineWave * 0.05F + segmentXOffset;
        chain[0].rotateAngleY = sineWave * 0.1F + segmentYOffset;
        chain[1].rotateAngleY = sineWave * 0.075F + segmentYOffset;
        chain[2].rotateAngleY = sineWave * 0.05F + segmentYOffset;
        chain[3].rotateAngleY = sineWave * 0.01F + segmentYOffset;
    }

    public void copyBipedPoseTo(ModelBiped target) {
        copyPartPose(bipedHead, target.bipedHead);
        copyPartPose(bipedHead, target.bipedHeadwear);
        copyPartPose(bipedBody, target.bipedBody);
        copyPartPose(bipedRightArm, target.bipedRightArm);
        copyPartPose(bipedLeftArm, target.bipedLeftArm);
        copyPartPose(bipedRightLeg, target.bipedRightLeg);
        copyPartPose(bipedLeftLeg, target.bipedLeftLeg);
    }

    private static void copyPartPose(ModelRenderer source, ModelRenderer target) {
        target.rotationPointX = source.rotationPointX;
        target.rotationPointY = source.rotationPointY;
        target.rotationPointZ = source.rotationPointZ;
        target.rotateAngleX = source.rotateAngleX;
        target.rotateAngleY = source.rotateAngleY;
        target.rotateAngleZ = source.rotateAngleZ;
    }
}
