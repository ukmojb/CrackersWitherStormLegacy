package com.wdcftgg.witherstormmod.client.sound;

import com.wdcftgg.witherstormmod.common.entity.BossThemeProvider;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;


public final class BossThemeSound extends MovingSound {
    private BossThemeProvider provider;
    private final SoundEvent theme;
    private float fade;
    private boolean stopping;
    private boolean starting = true;
    private int ticks;

    BossThemeSound(BossThemeProvider provider) {
        super(provider.getBossTheme(), provider.getBossThemeCategory());
        this.provider = provider;
        theme = provider.getBossTheme();
        repeat = true;
        repeatDelay = 0;
        volume = 0.0F;
        pitch = 1.0F;
        attenuationType = AttenuationType.NONE;
    }

    @Override
    public void update() {
        ++ticks;
        if (!BossThemeManager.INSTANCE.canPlay(provider)) {
            BossThemeProvider replacement = BossThemeManager.INSTANCE.findMatchingProvider(provider);
            if (replacement != null) {
                provider = replacement;
                continueSound();
            } else {
                stopSound();
            }
        }

        Vec3d position = provider.getBossThemePosition();
        xPosF = (float) position.x;
        yPosF = (float) position.y;
        zPosF = (float) position.z;

        int fadeTime = Math.max(1, provider.getBossThemeFadeTime());
        if (stopping) {
            if (fade > 0.0F) {
                --fade;
            } else {
                donePlaying = true;
            }
        } else if (starting) {
            if (fade < fadeTime) {
                ++fade;
            } else {
                starting = false;
            }
        }
        volume = MathHelper.clamp(fade / fadeTime, 0.0F, 1.0F);
    }

    public void stopSound() {
        if (stopping) return;
        stopping = true;
        fade = Math.max(1, provider.getBossThemeFadeTime());
    }

    public void continueSound() {
        if (!stopping) return;
        stopping = false;
        starting = true;
    }

    public void forceStop() {
        donePlaying = true;
    }

    public boolean isStopping() {
        return stopping;
    }

    public BossThemeProvider getProvider() {
        return provider;
    }

    public SoundEvent getTheme() {
        return theme;
    }

    int getTicks() {
        return ticks;
    }
}
