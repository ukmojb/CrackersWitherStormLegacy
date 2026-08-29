package com.wdcftgg.witherstormmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;


public final class ClientEffects {
    private static float shakeX;
    private static float shakeY;
    private static float previousShakeX;
    private static float previousShakeY;
    private static float shakeDuration;
    private static float initialShakeDuration;
    private static float shakePower;
    private static int blindDuration;
    private static int blindFadeInDuration;
    private static int blindFadeOutDuration;
    private static float blindFade;
    private static float previousBlindFade;

    private ClientEffects() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.world == null || minecraft.player == null) {
            reset();
            return;
        }
        if (minecraft.isGamePaused()) return;

        previousShakeX = shakeX;
        previousShakeY = shakeY;
        if (!WitherStormClientConfig.cameraShakeEffects) {
            shakeX = 0.0F;
            shakeY = 0.0F;
            shakeDuration = 0.0F;
            initialShakeDuration = 0.0F;
            shakePower = 0.0F;
        } else if (shakeDuration > 0.0F) {
            float currentPower = minecraft.player.onGround ? shakePower : 0.0F;
            float percentage = shakeDuration / initialShakeDuration;
            shakeX = currentPower * (minecraft.player.getRNG().nextFloat() * 2.0F - 1.0F) * percentage;
            shakeY = currentPower * (minecraft.player.getRNG().nextFloat() * 2.0F - 1.0F) * percentage;
            shakeDuration -= 1.0F;
        }

        previousBlindFade = blindFade;
        if (blindFadeInDuration > 0) {
            blindFade += (1.0F - blindFade) / blindFadeInDuration;
            --blindFadeInDuration;
        } else if (blindDuration > 0) {
            --blindDuration;
        } else if (blindFadeOutDuration > 0) {
            blindFade += (0.0F - blindFade) / blindFadeOutDuration;
            --blindFadeOutDuration;
        }
    }

    public static void shake(float duration, float power) {
        if (!WitherStormClientConfig.cameraShakeEffects) return;
        initialShakeDuration = duration;
        shakeDuration = duration;
        shakePower = power;
    }

    public static void blind(int duration, int fadeInDuration, int fadeOutDuration) {
        blindDuration = duration;
        blindFadeInDuration = fadeInDuration;
        blindFadeOutDuration = fadeOutDuration;
    }

    public static float getShakeTranslationX(float partialTicks) {
        float degrees = previousShakeX + (shakeX - previousShakeX) * partialTicks;
        return MathHelper.sin((float) Math.toRadians(degrees));
    }

    public static float getShakeTranslationY(float partialTicks) {
        float degrees = previousShakeY + (shakeY - previousShakeY) * partialTicks;
        return MathHelper.sin((float) Math.toRadians(degrees));
    }

    public static float getBlindFade(float partialTicks) {
        return MathHelper.clamp(previousBlindFade + (blindFade - previousBlindFade) * partialTicks, 0.0F, 1.0F);
    }

    public static void playGlobalSound(ResourceLocation soundId, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getMinecraft();
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
        if (minecraft.player == null || sound == null) return;
        minecraft.getSoundHandler().playSound(new PositionedSoundRecord(
                sound.getSoundName(), SoundCategory.HOSTILE, volume, pitch, false, 0,
                ISound.AttenuationType.NONE, (float) minecraft.player.posX,
                (float) minecraft.player.posY, (float) minecraft.player.posZ));
    }


    public static void spawnFormidibombExplosion(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) return;

        int poofCount = Math.max(4500, minecraft.world.rand.nextInt(5001));
        for (int index = 0; index < poofCount; index++) {
            double speedX = minecraft.world.rand.nextGaussian() * 0.5D;
            double speedY = minecraft.world.rand.nextGaussian() * 0.5D;
            double speedZ = minecraft.world.rand.nextGaussian() * 0.5D;
            double offsetX = minecraft.world.rand.nextGaussian() * 4.0D;
            double offsetY = minecraft.world.rand.nextGaussian() * 4.0D;
            double offsetZ = minecraft.world.rand.nextGaussian() * 4.0D;
            minecraft.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    x + offsetX, y + offsetY, z + offsetZ, speedX, speedY, speedZ);
        }

        int explosionCount = Math.max(50, minecraft.world.rand.nextInt(76));
        for (int index = 0; index < explosionCount; index++) {
            double offsetX = minecraft.world.rand.nextGaussian() * 12.0D;
            double offsetY = minecraft.world.rand.nextGaussian() * 12.0D;
            double offsetZ = minecraft.world.rand.nextGaussian() * 12.0D;
            minecraft.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                    x + offsetX, y + offsetY, z + offsetZ, 0.0D, 0.0D, 0.0D);
        }
    }

    public static void reset() {
        shakeX = 0.0F;
        shakeY = 0.0F;
        previousShakeX = 0.0F;
        previousShakeY = 0.0F;
        shakeDuration = 0.0F;
        initialShakeDuration = 0.0F;
        shakePower = 0.0F;
        blindDuration = 0;
        blindFadeInDuration = 0;
        blindFadeOutDuration = 0;
        blindFade = 0.0F;
        previousBlindFade = 0.0F;
    }
}
