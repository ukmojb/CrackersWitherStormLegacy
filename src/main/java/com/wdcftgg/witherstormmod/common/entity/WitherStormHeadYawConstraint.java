package com.wdcftgg.witherstormmod.common.entity;


final class WitherStormHeadYawConstraint {
    private static final float MAXIMUM_HEAD_YAW = 80.0F;

    private WitherStormHeadYawConstraint() {
    }





    static float constrain(int phase, int head, boolean deadOrPlayingDead,
                           float yaw, float bodyYaw) {
        if (deadOrPlayingDead) return yaw;
        float relativeYaw = wrapDegrees(yaw - bodyYaw);
        return bodyYaw + Math.max(-MAXIMUM_HEAD_YAW,
                Math.min(MAXIMUM_HEAD_YAW, relativeYaw));
    }





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
