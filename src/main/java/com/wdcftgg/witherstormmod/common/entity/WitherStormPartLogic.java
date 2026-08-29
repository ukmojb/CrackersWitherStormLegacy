package com.wdcftgg.witherstormmod.common.entity;

import java.util.Random;


public final class WitherStormPartLogic {
    private WitherStormPartLogic() {
    }

    public static int initialRoarDelay(Random random) {
        return 400 + random.nextInt(600);
    }

    public static int nextRoarDelay(Random random) {
        return 400 + random.nextInt(600);
    }

    public static int nextShotDelay(Random random) {
        return 60 + random.nextInt(40);
    }

    public static double randomBetween(Random random, double minimum, double maximum) {
        return minimum + random.nextDouble() * (maximum - minimum);
    }

    public static float advanceMouth(float current, boolean roaring, boolean biting) {
        if (!biting && roaring) {
            return clamp(current + (1.0F - current) * 0.15F + 0.04F, 0.0F, 2.0F);
        }
        if (biting) {
            return clamp(current + (1.0F - current) * 0.16F + 0.1F, 0.0F, 1.4F);
        }
        return clamp(current - current * 0.16F - 0.02F, 0.0F, 2.0F);
    }

    public static float advanceFade(float current, boolean shouldFade, Random random) {
        float next = shouldFade ? current + 1.0F + random.nextFloat() * 2.0F
                : current - 1.0F - random.nextFloat() * 2.0F;
        return clamp(next, 0.0F, 300.0F);
    }

    public static int applyFadeLight(int packedLight, float fadeAnimation) {
        int blockLight = Math.max(0, (int) ((100.0F - fadeAnimation) / 4.0F - 10.0F));
        return packedLight & 0xFFFF0000 | blockLight << 4;
    }

    public static float advanceShake(float current, boolean shaking, Random random) {
        return shaking ? current + 0.02F + random.nextFloat() * 0.05F : current;
    }

    public static float shakeRoll(float previous, float current, float partialTicks) {
        float lerp = clamp(previous + (current - previous) * partialTicks, 0.0F, 1.0F);
        return (float) Math.sin(lerp * Math.PI) * (float) Math.sin(lerp * Math.PI * 12.0F)
                * 0.05F * (float) Math.PI;
    }

    public static int segmentDropDuration(Random random) {
        return 10 + random.nextInt(5);
    }

    public static int segmentDropCooldown(Random random, float healthRatio) {
        return (int) ((360 + random.nextInt(160)) * Math.max(0.2F, healthRatio));
    }

    public static int segmentFreeFallDelay(Random random) {
        return Math.max(220, random.nextInt(260));
    }


    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
