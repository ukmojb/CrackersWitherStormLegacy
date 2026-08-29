package com.wdcftgg.witherstormmod.client.sound;

import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.common.entity.BossThemeProvider;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;


public final class BossThemeManager {
    public static final BossThemeManager INSTANCE = new BossThemeManager();

    private final Set<Integer> deniedStorms = new HashSet<Integer>();
    private BossThemeSound theme;
    private World currentWorld;
    private boolean minecraftMusicSuppressed;

    private BossThemeManager() {
    }

    public void tick(Minecraft minecraft) {
        if (minecraft.world != currentWorld) resetWorld(minecraft.world);
        if (!WitherStormClientConfig.playMinecraftMusic) {
            suppressMinecraftMusic(minecraft);
        } else if (minecraftMusicSuppressed) {
            minecraft.getMusicTicker().timeUntilNextMusic = 0;
            minecraftMusicSuppressed = false;
        }
        if (minecraft.world == null || minecraft.player == null) return;

        if (!isCategoryAudible(minecraft, SoundCategory.MUSIC)) {
            if (theme != null) theme.forceStop();
            theme = null;
            return;
        }

        if (WitherStormClientConfig.playWitherStormTheme) {
            for (Entity entity : minecraft.world.loadedEntityList) {
                if (!(entity instanceof BossThemeProvider)) continue;
                BossThemeProvider candidate = (BossThemeProvider) entity;
                if (!canPlay(candidate) || !isCategoryAudible(minecraft, candidate.getBossThemeCategory())) continue;
                BossThemeProvider active = theme == null ? null : theme.getProvider();
                if (active == candidate) {
                    theme.continueSound();
                } else if (active == null || candidate.getBossThemePriority() > active.getBossThemePriority()) {
                    if (theme != null) theme.stopSound();
                    play(minecraft, candidate);
                }
            }
        }

        if (theme != null && theme.getTheme() != theme.getProvider().getBossTheme()) {
            BossThemeProvider provider = theme.getProvider();
            theme.stopSound();
            play(minecraft, provider);
        }
        if (theme != null && (theme.isDonePlaying()
                || theme.getTicks() > 2 && !minecraft.getSoundHandler().isSoundPlaying(theme))) {
            theme = null;
        }
    }

    public void setStormAccess(int entityId, boolean allowed) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world != currentWorld) resetWorld(minecraft.world);
        if (allowed) deniedStorms.remove(entityId);
        else deniedStorms.add(entityId);
    }

    boolean canPlay(BossThemeProvider provider) {
        if (!WitherStormClientConfig.playWitherStormTheme || provider == null
                || provider.getBossTheme() == null || !provider.shouldPlayBossTheme()) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        double distance = provider.getBossThemeDistance();
        if (minecraft.player == null || distance < Double.POSITIVE_INFINITY
                && minecraft.player.getPositionVector().squareDistanceTo(provider.getBossThemePosition())
                > distance * distance) return false;
        if (provider instanceof SickenedEntities.WitheredSymbiontEntity
                && !WitherStormClientConfig.playSymbiontTheme) return false;
        return !(provider instanceof WitherStormEntity)
                || !deniedStorms.contains(((WitherStormEntity) provider).getEntityId());
    }

    @Nullable
    public BossThemeProvider getActiveProvider() {
        return theme == null ? null : theme.getProvider();
    }


    public void forceStop() {
        if (theme != null) theme.forceStop();
    }

    @Nullable
    BossThemeProvider findMatchingProvider(BossThemeProvider original) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) return null;
        for (Entity entity : minecraft.world.loadedEntityList) {
            if (!(entity instanceof BossThemeProvider)) continue;
            BossThemeProvider candidate = (BossThemeProvider) entity;
            if (original.matchesBossTheme(candidate) && canPlay(candidate)) return candidate;
        }
        return null;
    }

    private void play(Minecraft minecraft, BossThemeProvider provider) {
        theme = new BossThemeSound(provider);
        minecraft.getSoundHandler().playSound(theme);
    }

    private static boolean isCategoryAudible(Minecraft minecraft, SoundCategory category) {
        return minecraft.gameSettings.getSoundLevel(SoundCategory.MASTER) > 0.0F
                && minecraft.gameSettings.getSoundLevel(category) > 0.0F;
    }

    private void suppressMinecraftMusic(Minecraft minecraft) {
        MusicTicker ticker = minecraft.getMusicTicker();
        ISound currentMusic = ticker.currentMusic;
        if (currentMusic != null) minecraft.getSoundHandler().stopSound(currentMusic);
        ticker.currentMusic = null;
        ticker.timeUntilNextMusic = Integer.MAX_VALUE;
        minecraftMusicSuppressed = true;
    }

    private void resetWorld(@Nullable World world) {
        if (theme != null) theme.forceStop();
        theme = null;
        deniedStorms.clear();
        currentWorld = world;
    }
}
