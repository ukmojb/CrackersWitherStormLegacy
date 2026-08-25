package com.wdcftgg.witherstormmod.common.entity;

/** 集中定义主风暴三颗头相对身体的水平转动约束。 */
final class WitherStormHeadYawConstraint {
    private static final float MAXIMUM_HEAD_YAW = 80.0F;

    private WitherStormHeadYawConstraint() {
    }

    /**
     * 三颗头统一约束到身体前向 ±80 度，对齐上游 LookControl.clampHeadRotationToBody；
     * 死亡或装死时保留脚本控制的头部姿态。
     */
    static float constrain(int phase, int head, boolean deadOrPlayingDead,
                           float yaw, float bodyYaw) {
        if (deadOrPlayingDead) return yaw;
        float relativeYaw = wrapDegrees(yaw - bodyYaw);
        return bodyYaw + Math.max(-MAXIMUM_HEAD_YAW,
                Math.min(MAXIMUM_HEAD_YAW, relativeYaw));
    }

    /**
     * 判断目标是否位于身体前向弧之外。这里不能在归一化前额外加 180 度，
     * 否则会把正前方与正后方完全颠倒。
     */
    static boolean isOutsideForwardArc(double deltaX, double deltaZ, float bodyYaw) {
        float targetAngle = (float) (Math.atan2(deltaX, deltaZ) * 180.0D / Math.PI);
        float difference = wrapDegrees(-bodyYaw - targetAngle);
        return difference > MAXIMUM_HEAD_YAW || difference < -MAXIMUM_HEAD_YAW;
    }

    private static float wrapDegrees(float angle) {
        float wrapped = angle % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }
}
