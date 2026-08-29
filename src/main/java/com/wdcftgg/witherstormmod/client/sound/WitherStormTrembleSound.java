package com.wdcftgg.witherstormmod.client.sound;

import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;


public class WitherStormTrembleSound extends MovingSound {
    private static final float FADE_TICKS = 40.0F;
    private final WitherStormEntity storm;
    private float fade;
    private boolean stopping;

    public WitherStormTrembleSound(WitherStormEntity storm, SoundEvent sound) {
        super(sound, SoundCategory.AMBIENT);
        this.storm = storm;
        repeat = true;
        repeatDelay = 0;
        volume = 0.0F;
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
        if (storm.isDead || storm.world != Minecraft.getMinecraft().world) {
            donePlaying = true;
            return;
        }
        xPosF = (float) storm.posX;
        yPosF = (float) storm.posY;
        zPosF = (float) storm.posZ;

        if (storm.getPlayDeadState() != WitherStormEntity.PlayDeadState.FALLING) {
            requestStop();
        }
        if (stopping) {
            if (fade > 0.0F) fade -= 1.0F;
            else donePlaying = true;
        } else if (fade < FADE_TICKS) {
            fade += 1.0F;
        }
        volume = MathHelper.clamp(fade / FADE_TICKS, 0.0F, 1.0F);
        pitch = 1.0F;
    }
}
