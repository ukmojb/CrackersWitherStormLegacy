package com.wdcftgg.witherstormmod.mixin;

import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;





@Mixin(EntityWither.class)
public abstract class WitherMixin {

    @Inject(method = "attackEntityWithRangedAttack(Lnet/minecraft/entity/EntityLivingBase;F)V",
            at = @At("HEAD"), cancellable = true)
    private void witherstormmod$skipSickenedTargets(
            EntityLivingBase target, float distanceFactor, CallbackInfo callbackInfo) {
        if (target instanceof SickenedMobEntity) {
            callbackInfo.cancel();
        }
    }
}
