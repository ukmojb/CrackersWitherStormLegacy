package com.wdcftgg.witherstormmod.mixin;

import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(Potion.class)
public abstract class PotionMixin {

    @Redirect(
            method = "affectEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/EntityLivingBase;ID)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;heal(F)V"))
    private void witherstormmod$protectCommandBlockFromInstantHealing(
            EntityLivingBase target, float amount) {
        if (!(target instanceof SupplementalEntities.CommandBlockEntity)) {
            target.heal(amount);
        }
    }

    @Redirect(
            method = "affectEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/EntityLivingBase;ID)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private boolean witherstormmod$protectStormEntitiesFromInstantDamage(
            EntityLivingBase target, DamageSource source, float amount) {
        if (target instanceof WitherStormEntity) {
            return ((WitherStormEntity) target).getPhase() < 3
                    && target.attackEntityFrom(source, amount);
        }
        if (target instanceof SupplementalEntities.WitherStormSegmentEntity) {
            return ((SupplementalEntities.WitherStormSegmentEntity) target).getPhase() < 3
                    && target.attackEntityFrom(source, amount);
        }
        return !(target instanceof SickenedMobEntity)
                && target.attackEntityFrom(source, amount);
    }
}
