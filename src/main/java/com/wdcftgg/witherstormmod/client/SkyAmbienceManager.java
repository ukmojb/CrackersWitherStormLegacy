package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.client.resources.WitherStormResourceConfigManager;
import com.wdcftgg.witherstormmod.client.resources.color.SkyColorSet;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;





public final class SkyAmbienceManager {

    public static final SkyAmbienceManager INSTANCE = new SkyAmbienceManager();
    private static final int COLOR_TRANSITION = 40;

    private float alpha = 1.0F;
    private float alphaO = 1.0F;
    private int colorTransitionTime;
    private SkyColorSet current = SkyColorSet.DEFAULT;
    private SkyColorSet previous = SkyColorSet.DEFAULT;
    @Nullable
    private World activeWorld;
    private long lastUpdateTick = Long.MIN_VALUE;

    private SkyAmbienceManager() {
    }

    public void tick(Minecraft minecraft) {
        if (minecraft.world == null || minecraft.player == null) {
            activeWorld = null;
            reset();
            return;
        }
        if (activeWorld != minecraft.world) {
            activeWorld = minecraft.world;
            reset();
        }

        if (minecraft.isGamePaused() || !WitherStormClientConfig.renderSkyAmbienceEffects) return;
        long worldTick = minecraft.world.getTotalWorldTime();
        if (lastUpdateTick == worldTick) return;
        lastUpdateTick = worldTick;
        List<WitherStormEntity> storms = minecraft.world.loadedEntityList.stream()
                .filter(entity -> entity instanceof WitherStormEntity)
                .map(entity -> (WitherStormEntity) entity)
                .filter(new java.util.function.Predicate<WitherStormEntity>() {
                    private final java.util.Set<java.util.UUID> seen =
                            new java.util.HashSet<java.util.UUID>();

                    @Override
                    public boolean test(WitherStormEntity storm) {
                        return seen.add(storm.getUniqueID());
                    }
                })
                .sorted(Comparator.comparingInt(WitherStormEntity::getConsumedMass).reversed())
                .collect(Collectors.toList());
        WitherStormEntity storm = storms.isEmpty() ? null : storms.get(0);
        alphaO = alpha;
        if (storm != null && !storm.isDeadOrPlayingDead()) {
            Entity viewEntity = minecraft.getRenderViewEntity();
            if (viewEntity == null) viewEntity = minecraft.player;
            double distance = viewEntity.getPositionVector().distanceTo(storm.getPositionVector());
            float distanceAlpha = MathHelper.clamp(
                    (float) ((distance - 200.0D) * 0.005D), 0.0F, 1.0F);
            float magnitude = modifyEffectMagnitude(storm, distanceAlpha);
            alpha += (magnitude - alpha) / 25.0F;
            SkyColorSet set = WitherStormResourceConfigManager.INSTANCE
                    .getColorSetByPhase(storm.getPhase()).getSkyColors();
            if (set != null && !set.equals(current)) {
                previous = current;
                current = set;
                colorTransitionTime = COLOR_TRANSITION;
            }
        } else {
            alpha += (1.0F - alpha) / 100.0F;
        }
        if (colorTransitionTime > 0) {
            --colorTransitionTime;
        }
    }

    private void reset() {
        alpha = 1.0F;
        alphaO = 1.0F;
        colorTransitionTime = 0;
        current = SkyColorSet.DEFAULT;
        previous = SkyColorSet.DEFAULT;
        lastUpdateTick = Long.MIN_VALUE;
    }

    private float lerpAlpha(float partialTicks) {
        return alphaO + (alpha - alphaO) * partialTicks;
    }

    public float modifySkyDarken(float original, float partialTicks) {
        if (!WitherStormClientConfig.renderSkyAmbienceEffects) return original;
        return original * Math.min(lerpAlpha(partialTicks) + 0.4F, 1.0F);
    }

    @Nullable
    public Vec3d blendSkyColor(Entity viewEntity, float partialTicks, Vec3d original) {
        return blendColor(viewEntity, partialTicks, original,
                SkyColorSet::getSkyColor, SkyColorSet::getNightSkyColor);
    }

    @Nullable
    public Vec3d blendCloudColor(Entity viewEntity, float partialTicks, Vec3d original) {
        return blendColor(viewEntity, partialTicks, original,
                SkyColorSet::getCloudColor, SkyColorSet::getNightCloudColor);
    }

    @Nullable
    public Vec3d blendFogColor(Entity viewEntity, float partialTicks, Vec3d original) {
        return blendColor(viewEntity, partialTicks, original,
                SkyColorSet::getFogColor, SkyColorSet::getNightFogColor);
    }

    @Nullable
    private Vec3d blendColor(Entity viewEntity, float partialTicks, Vec3d original,
                             Function<SkyColorSet, Color> dayGetter,
                             Function<SkyColorSet, Color> nightGetter) {
        if (!WitherStormClientConfig.renderSkyAmbienceEffects) return null;
        float alpha = lerpAlpha(partialTicks);
        if (alpha >= 0.999F) return null;
        Color color = lerpColorsByTransition(dayGetter, nightGetter, partialTicks);
        float red = color.getRed() / 255.0F;
        float green = color.getGreen() / 255.0F;
        float blue = color.getBlue() / 255.0F;
        return new Vec3d(
                red + ((float) original.x - red) * alpha,
                green + ((float) original.y - green) * alpha,
                blue + ((float) original.z - blue) * alpha);
    }

    private Color lerpColorsByTransition(Function<SkyColorSet, Color> dayGetter,
                                         Function<SkyColorSet, Color> nightGetter,
                                         float partialTicks) {
        float transition = MathHelper.clamp(
                (colorTransitionTime - partialTicks) / COLOR_TRANSITION, 0.0F, 1.0F);
        Color previousColor = lerpNightColors(dayGetter.apply(previous),
                nightGetter.apply(previous), partialTicks);
        Color currentColor = lerpNightColors(dayGetter.apply(current),
                nightGetter.apply(current), partialTicks);
        int red = MathHelper.clamp(Math.round(currentColor.getRed()
                + (previousColor.getRed() - currentColor.getRed()) * transition), 0, 255);
        int green = MathHelper.clamp(Math.round(currentColor.getGreen()
                + (previousColor.getGreen() - currentColor.getGreen()) * transition), 0, 255);
        int blue = MathHelper.clamp(Math.round(currentColor.getBlue()
                + (previousColor.getBlue() - currentColor.getBlue()) * transition), 0, 255);
        int alpha = MathHelper.clamp(Math.round(currentColor.getAlpha()
                + (previousColor.getAlpha() - currentColor.getAlpha()) * transition), 0, 255);
        return new Color(red, green, blue, alpha);
    }

    private static Color lerpNightColors(Color day, @Nullable Color night, float partialTicks) {
        if (night == null) return day;
        World world = Minecraft.getMinecraft().world;
        float daylightWave = MathHelper.cos(world.getCelestialAngle(partialTicks)
                * (float) (Math.PI * 2.0D)) * 2.0F + 0.5F;
        float nightLerp = 1.0F - MathHelper.clamp(daylightWave + 0.5F, 0.0F, 1.0F);
        return new Color(
                Math.round(day.getRed() + (night.getRed() - day.getRed()) * nightLerp),
                Math.round(day.getGreen() + (night.getGreen() - day.getGreen()) * nightLerp),
                Math.round(day.getBlue() + (night.getBlue() - day.getBlue()) * nightLerp),
                Math.round(day.getAlpha() + (night.getAlpha() - day.getAlpha()) * nightLerp));
    }

    private static float modifyEffectMagnitude(WitherStormEntity storm, float alpha) {
        if (storm.getPhase() < 5) return 1.0F;
        return Math.min(alpha + 0.15F, 1.0F);
    }
}
