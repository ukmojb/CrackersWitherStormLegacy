package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.common.init.ModEffects;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Pseudo
@Mixin(targets = "tfar.classicbar.overlays.vanillaoverlays.HealthRenderer", remap = false)
public abstract class ClassicBarHealthRendererMixin {

    @Redirect(
            method = "renderBar",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;isPotionActive(Lnet/minecraft/potion/Potion;)Z",
                    ordinal = 1, remap = true),
            remap = false)
    private boolean witherstormmod$useWitheredHealthColor(EntityPlayer player, Potion potion) {
        return potion == MobEffects.WITHER && player.isPotionActive(ModEffects.WITHER_SICKNESS)
                || player.isPotionActive(potion);
    }

    @Redirect(
            method = "renderText",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;isPotionActive(Lnet/minecraft/potion/Potion;)Z",
                    ordinal = 1, remap = true),
            remap = false)
    private boolean witherstormmod$useWitheredHealthTextColor(EntityPlayer player, Potion potion) {
        return potion == MobEffects.WITHER && player.isPotionActive(ModEffects.WITHER_SICKNESS)
                || player.isPotionActive(potion);
    }
}
