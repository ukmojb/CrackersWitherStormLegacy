package com.wdcftgg.witherstormmod.client.sound;

import com.wdcftgg.witherstormmod.common.entity.FormidibombSource;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;


public class FormidibombFuseSound extends MovingSound {
    private final FormidibombSource source;

    public FormidibombFuseSound(FormidibombSource source, SoundEvent sound) {
        super(sound, SoundCategory.BLOCKS);
        this.source = source;
        repeat = true;
        repeatDelay = 0;
    }

    public void stop() {
        donePlaying = true;
    }

    @Override
    public void update() {
        if (!source.isFormidibombAlive()) {
            donePlaying = true;
            return;
        }
        int fuse = source.getFuseLife();
        int startFuse = source.getStartFuse();
        volume = calculateVolume(fuse, startFuse);
        pitch = calculatePitch(fuse, startFuse);
        Vec3d position = source.getFormidibombPosition();
        xPosF = (float) position.x;
        yPosF = (float) position.y;
        zPosF = (float) position.z;
    }

    static float calculatePercentage(int fuse, int startFuse) {
        return startFuse > 0 ? (startFuse - fuse) / (float) startFuse : 0.0F;
    }

    static float calculateVolume(int fuse, int startFuse) {
        return 1.0F + calculatePercentage(fuse, startFuse) * 2.0F;
    }

    static float calculatePitch(int fuse, int startFuse) {
        float additionalPitch = startFuse > 0
                ? (float) Math.max(0.0D, (120.0D - fuse) / 120.0D) : 0.0F;
        return 1.0F + calculatePercentage(fuse, startFuse) * 0.1F + additionalPitch;
    }
}
