package com.wdcftgg.witherstormmod.client.sound;

import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class WitherStormLoopSound extends MovingSound {
    private static final float FADE_TICKS = 40.0F;

    private final int stormEntityId;
    private final World clientWorld;
    private final float attenuationDistance;
    @Nullable
    private WitherStormEntity storm;
    private Vec3d position;
    private float fade;
    private float distanceVolume;
    private float dampen;
    private boolean stopping;

    public WitherStormLoopSound(WitherStormEntity storm, SoundEvent sound, float attenuationDistance) {
        this(storm.getEntityId(), storm, storm.getPositionVector(), sound, attenuationDistance);
    }

    public WitherStormLoopSound(int stormEntityId, Vec3d position, SoundEvent sound,
                                float attenuationDistance) {
        this(stormEntityId, null, position, sound, attenuationDistance);
    }

    private WitherStormLoopSound(int stormEntityId, @Nullable WitherStormEntity storm,
                                 Vec3d position, SoundEvent sound, float attenuationDistance) {
        super(sound, SoundCategory.AMBIENT);
        this.stormEntityId = stormEntityId;
        this.storm = storm;
        this.position = position;
        this.clientWorld = Minecraft.getMinecraft().world;
        this.attenuationDistance = attenuationDistance;
        repeat = true;
        repeatDelay = 0;
        attenuationType = AttenuationType.NONE;
        volume = 0.0F;
        pitch = 1.0F;
    }

    public void bindTo(@Nullable WitherStormEntity storm) {
        if (storm == null || storm.getEntityId() == stormEntityId) this.storm = storm;
    }

    public void updatePosition(Vec3d position) {
        if (position != null) this.position = position;
    }

    public void requestStop() {
        if (stopping) return;
        stopping = true;
        fade = FADE_TICKS;
    }

    public void stopImmediately() {
        donePlaying = true;
    }

    @Override
    public void update() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (clientWorld != minecraft.world) {
            donePlaying = true;
            return;
        }
        if (storm != null) {
            if (storm.world != minecraft.world || storm.isDead || !storm.isEntityAlive()) {
                storm = null;
            } else {
                position = storm.getPositionVector();
            }
        }
        xPosF = (float) position.x;
        yPosF = (float) position.y;
        zPosF = (float) position.z;
        if (stopping) {
            if (fade > 0.0F) fade -= 1.0F;
            else {
                donePlaying = true;
                return;
            }
        } else if (fade < FADE_TICKS) {
            fade += 1.0F;
        }

        if (minecraft.player == null) {
            volume = 0.0F;
            return;
        }
        float distance = (float) Math.sqrt(minecraft.player.getDistanceSq(xPosF, yPosF, zPosF));
        updateDampening(minecraft, distance);
        float targetDistanceVolume = MathHelper.clamp(1.0F - distance / attenuationDistance, 0.0F, 1.0F);
        distanceVolume += MathHelper.clamp(targetDistanceVolume - distanceVolume, -0.035F, 0.035F);
        float unobstructedVolume = MathHelper.clamp(fade / FADE_TICKS, 0.0F, 1.0F);
        volume = Math.max(0.0F, unobstructedVolume - dampen * 0.02F) * distanceVolume;
    }

    private void updateDampening(Minecraft minecraft, float distance) {
        if (distance >= 1000.0F) {
            dampen = 0.0F;
            return;
        }
        float maximumDampening = 15.0F;
        if (WitherStormConfig.occludeSoundsUnderground
                && !WorldUtil.isInAnOpenArea(minecraft.player)) {
            maximumDampening = 30.0F + MathHelper.clamp(
                    (float) -minecraft.player.posY + 40.0F, 0.0F, 20.0F);
        }
        RayTraceResult result = minecraft.world.rayTraceBlocks(
                new Vec3d(xPosF, yPosF, zPosF), minecraft.player.getPositionVector(),
                true, true, false);
        boolean obstructed = result != null && result.typeOfHit == RayTraceResult.Type.BLOCK;
        if (obstructed && dampen < maximumDampening) dampen += 1.0F;
        else if (obstructed && dampen > maximumDampening) dampen -= 1.0F;
        else if (!obstructed && dampen > 0.0F) dampen -= 1.0F;
    }
}
