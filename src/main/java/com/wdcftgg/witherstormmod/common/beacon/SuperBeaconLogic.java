package com.wdcftgg.witherstormmod.common.beacon;

import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.block.Block;
import net.minecraft.potion.Potion;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public final class SuperBeaconLogic {
    public static final int POWER_UP_ANIMATION_TIME = 80;
    public static final int POWER_UP_CLIMAX = 40;
    public static final int EFFECT_CHANGE_COOLDOWN = 200;
    public static final int MAIN_EFFECT_DURATION = 505;
    public static final int SUPPORT_EFFECT_DURATION = 205;
    public static final int MAIN_EFFECT_RADIUS = 128;
    public static final int SUPPORT_EFFECT_ARC = 90;
    public static final int SUPPORT_SCAN_DISTANCE = 5;
    public static final int RESUMMON_START = 60;
    public static final int RESUMMON_TIME = 372;
    public static final int RESUMMON_CLUSTER_END = 352;
    public static final int RESUMMON_SUPPORT_INTERVAL = 40;
    public static final int RESUMMON_STORM_PULSE_INTERVAL = 40;

    private static final Set<Potion> MAIN_EFFECTS = effects(
            MobEffects.STRENGTH,
            MobEffects.RESISTANCE,
            MobEffects.HASTE,
            MobEffects.JUMP_BOOST,
            MobEffects.SPEED,
            MobEffects.NIGHT_VISION,
            MobEffects.REGENERATION,
            MobEffects.SATURATION);

    private SuperBeaconLogic() {
    }

    public static Set<Potion> getMainEffects() {
        return MAIN_EFFECTS;
    }

    public static boolean canChangeEffect(int cooldown, boolean clearing) {
        return clearing || cooldown <= 0;
    }

    public static float angleDegrees(double x, double z) {
        return (float) Math.toDegrees(Math.atan2(x, z));
    }

    public static float wrappedAngleDifference(float first, float second) {
        float difference = (first - second + 540.0F) % 360.0F - 180.0F;
        return difference < -180.0F ? difference + 360.0F : difference;
    }

    public static boolean isInsideSupportArc(double mainX, double mainZ,
                                             double supportX, double supportZ,
                                             double playerX, double playerZ) {
        float beamAngle = angleDegrees(supportX - mainX, supportZ - mainZ);
        float playerAngle = angleDegrees(playerX - mainX, playerZ - mainZ);
        return Math.abs(wrappedAngleDifference(beamAngle, playerAngle))
                <= SUPPORT_EFFECT_ARC / 2.0F;
    }

    public static int getSupportResummonThreshold(int colorOrdinal) {
        return RESUMMON_START + colorOrdinal * RESUMMON_SUPPORT_INTERVAL;
    }

    public static int getMainResummonThreshold() {
        return getSupportResummonThreshold(SupportColor.values().length);
    }

    public static boolean isWitherStormResummon(String entityId) {
        return "witherstormmod:wither_storm".equals(entityId);
    }

    public static boolean shouldFinishResummon(String entityId, int ticks) {
        return isWitherStormResummon(entityId)
                ? ticks > RESUMMON_TIME : ticks >= RESUMMON_START;
    }

    public static boolean shouldPulseWitherStormResummon(int ticks) {
        return ticks > RESUMMON_START && ticks % RESUMMON_STORM_PULSE_INTERVAL == 0;
    }

    public static int getClusterSpawnInterval(int ticks) {
        return Math.max(1, RESUMMON_TIME / Math.max(1, ticks));
    }

    public static boolean shouldSpawnResummonCluster(int ticks) {
        return ticks > RESUMMON_START && ticks < RESUMMON_CLUSTER_END
                && ticks % getClusterSpawnInterval(ticks) == 0;
    }

    public static float getResummonItemScale(float ticks) {
        return Math.max(0.0F, 1.0F - ticks / RESUMMON_START);
    }

    private static Set<Potion> effects(Potion... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<Potion>(Arrays.asList(values)));
    }

    public enum SupportColor {
        AQUA(Blocks.DIAMOND_BLOCK, 5, 255, 255,
                MobEffects.NIGHT_VISION, MobEffects.WATER_BREATHING, MobEffects.HASTE),
        GREEN(Blocks.EMERALD_BLOCK, 26, 255, 0,
                MobEffects.SPEED, MobEffects.WATER_BREATHING, MobEffects.JUMP_BOOST),
        GRAY(Blocks.IRON_BLOCK, 255, 255, 255,
                MobEffects.STRENGTH, MobEffects.INVISIBILITY, MobEffects.FIRE_RESISTANCE),
        RED(Blocks.REDSTONE_BLOCK, 240, 39, 7,
                MobEffects.RESISTANCE, MobEffects.REGENERATION, MobEffects.SATURATION);

        private final Block baseBlock;
        private final int red;
        private final int green;
        private final int blue;
        private final Set<Potion> validEffects;

        SupportColor(Block baseBlock, int red, int green, int blue, Potion... validEffects) {
            this.baseBlock = baseBlock;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.validEffects = effects(validEffects);
        }

        public Block getBaseBlock() {
            return baseBlock;
        }

        public float[] getBeamColor() {
            return new float[] {red / 255.0F, green / 255.0F, blue / 255.0F};
        }

        public Set<Potion> getValidEffects() {
            return validEffects;
        }

        public static SupportColor forBase(Block block) {
            for (SupportColor color : values()) {
                if (color.baseBlock == block) return color;
            }
            return null;
        }
    }
}
