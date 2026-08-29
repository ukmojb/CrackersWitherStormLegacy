package com.wdcftgg.witherstormmod.common.entity;

import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;


public interface BossThemeProvider {
    SoundEvent getBossTheme();

    default SoundCategory getBossThemeCategory() {
        return SoundCategory.MUSIC;
    }

    default int getBossThemeFadeTime() {
        return 240;
    }

    boolean shouldPlayBossTheme();

    int getBossThemePriority();

    Vec3d getBossThemePosition();

    default double getBossThemeDistance() {
        return Double.POSITIVE_INFINITY;
    }

    default boolean matchesBossTheme(BossThemeProvider other) {
        return other != null && getBossTheme() == other.getBossTheme()
                && getBossThemeCategory() == other.getBossThemeCategory()
                && getBossThemePriority() == other.getBossThemePriority();
    }
}
