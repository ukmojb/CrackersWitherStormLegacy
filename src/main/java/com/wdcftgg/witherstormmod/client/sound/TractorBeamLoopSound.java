package com.wdcftgg.witherstormmod.client.sound;

import com.wdcftgg.witherstormmod.common.entity.TractorBeamProvider;
import com.wdcftgg.witherstormmod.common.util.TractorBeamHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;


public final class TractorBeamLoopSound extends MovingSound {
    public static final double MAXIMUM_DISTANCE = 30.0D;

    private final Entity entity;
    private final TractorBeamProvider provider;
    private final int head;

    public TractorBeamLoopSound(Entity entity, TractorBeamProvider provider, int head,
                                SoundEvent sound, Vec3d initialPosition) {
        super(sound, SoundCategory.AMBIENT);
        this.entity = entity;
        this.provider = provider;
        this.head = head;
        repeat = true;
        repeatDelay = 0;
        volume = 0.0F;
        pitch = 1.0F;
        setPosition(initialPosition);
    }

    @Override
    public void update() {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null || entity.isDead || entity.world != minecraft.world
                || provider.isDeadOrPlayingDead() || !provider.tractorBeamActive(head)) {
            donePlaying = true;
            return;
        }

        Vec3d closest = calculateClosestPoint(entity, provider, head, player.getPositionVector());
        xPosF += ((float) closest.x - xPosF) * 0.1F;
        yPosF += ((float) closest.y - yPosF) * 0.1F;
        zPosF += ((float) closest.z - zPosF) * 0.1F;
        double distance = Math.sqrt(player.getDistanceSq(closest.x, closest.y, closest.z));
        if (distance > MAXIMUM_DISTANCE) {
            donePlaying = true;
            return;
        }
        volume = Math.max(0.0F, 0.3F - (float) distance / 60.0F);
    }

    public void stop() {
        donePlaying = true;
    }

    public static Vec3d calculateClosestPoint(Entity entity, TractorBeamProvider provider,
                                               int head, Vec3d targetPosition) {
        Vec3d headPosition = provider.getHeadPositionForBeam(head);
        Vec3d direction = provider.getHeadDirectionForBeam(head);
        double cutoff = provider.getTractorBeamCutoffDistance(head);
        if (cutoff < 0.0D) {
            cutoff = TractorBeamHelper.findCutoffDistance(
                    entity.world, headPosition, direction, 250.0D);
        }
        return TractorBeamHelper.calculateClosestPoint(
                targetPosition, headPosition, direction, cutoff);
    }

    private void setPosition(Vec3d position) {
        xPosF = (float) position.x;
        yPosF = (float) position.y;
        zPosF = (float) position.z;
    }
}
