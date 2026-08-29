package com.wdcftgg.witherstormmod.api.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;




public interface WitherStormBase {

    enum DistractionType {
        ENTITY_BASED,
        STRUCTURES
    }

    float getMouthAnimation(int head, float partialTicks);

    float getBrokenJawAnimation(int head, float partialTicks);

    float getFadeAnimation(float partialTicks);

    float getFadeAnimation();

    float getTentacleAnimation(float partialTicks);

    Vec3d getHeadPos(int head);

    float getHeadYRot(int head);

    float getHeadYRotO(int head);

    float getHeadXRot(int head);

    float getHeadXRotO(int head);

    float getXBodyRot();

    float getXBodyRotO();

    boolean areOtherHeadsDisabled();

    boolean isHeadInjured(int head);

    default boolean canBeDistracted(int head, DistractionType type) {
        return tractorBeamActive(head);
    }

    default boolean canBeDistracted(int head) {
        return tractorBeamActive(head);
    }

    default boolean isDistracted(int head) {
        return getDistractedPos(head) != null;
    }

    @Nullable
    Vec3d getDistractedPos(int head);

    void setDistractedPos(int head, @Nullable Vec3d position);

    void makeDistracted(Vec3d position, int ticks, int head);

    void setLookAt(int head, @Nullable Vec3d position, int steps);

    default void setLookAt(int head, @Nullable Vec3d position) {
        setLookAt(head, position, 3);
    }

    float getHeadShakeAnim(int head, float partialTicks);

    @Nullable
    EntityLivingBase getTarget(int head);

    void setTarget(int head, @Nullable EntityLivingBase target);

    boolean canSee(int head, Entity entity);

    boolean isPosBehindBack(Vec3d position);

    default boolean isEntityBehindBack(Entity entity) {
        return isPosBehindBack(entity.getPositionVector());
    }

    boolean isDeadOrPlayingDead();

    boolean isPlayingDead();

    default int getTotalHeads() {
        return 3;
    }

    default double getTractorBeamCutoffDistance(int head) {
        return -1.0D;
    }

    default boolean tractorBeamActive(int head) {
        return (!areOtherHeadsDisabled() || head == 0) && !isHeadInjured(head);
    }

    default Vec3d getViewVector(float x, float y, float range) {
        float xRadians = x * 0.017453292F;
        float negativeYRadians = -y * 0.017453292F;
        float cosY = MathHelper.cos(negativeYRadians);
        float sinY = MathHelper.sin(negativeYRadians);
        float cosX = MathHelper.cos(xRadians);
        float sinX = MathHelper.sin(xRadians);
        return new Vec3d(sinY * cosX * range,
                -sinX * range,
                cosY * cosX * range);
    }
}
