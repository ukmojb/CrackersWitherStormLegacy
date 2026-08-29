package com.wdcftgg.witherstormmod.mixin;

import com.wdcftgg.witherstormmod.common.entity.PowerfulExplosiveEntity;
import com.wdcftgg.witherstormmod.common.init.ModDamageSources;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private Entity exploder;


    @Redirect(
            method = "doExplosionA()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/DamageSource;causeExplosionDamage(Lnet/minecraft/world/Explosion;)Lnet/minecraft/util/DamageSource;"))
    private DamageSource witherstormmod$replaceSuperTntDamageSource(Explosion explosion) {
        return exploder instanceof PowerfulExplosiveEntity.SuperTntEntity
                ? ModDamageSources.superTntExplosion()
                : DamageSource.causeExplosionDamage(explosion);
    }
}
