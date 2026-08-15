package com.wdcftgg.witherstormmod.mixin;

import com.wdcftgg.witherstormmod.common.access.EntityLivingBaseExperienceAccess;
import com.wdcftgg.witherstormmod.common.access.EntityLivingBaseDeathProtectionAccess;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseMixin implements EntityLivingBaseExperienceAccess,
        EntityLivingBaseDeathProtectionAccess {

    @Shadow
    protected EntityPlayer attackingPlayer;

    @Shadow
    protected abstract int getExperiencePoints(EntityPlayer player);

    @Unique
    private boolean witherstormmod$skipNextExperienceDrop;

    @Unique
    private boolean witherstormmod$deathProtectionActive;

    @Override
    public int witherstormmod$captureExperienceDrop() {
        EntityLivingBase entity = (EntityLivingBase) (Object) this;
        int originalExperience = getExperiencePoints(attackingPlayer);
        return ForgeEventFactory.getExperienceDrop(entity, attackingPlayer, originalExperience);
    }

    @Override
    public void witherstormmod$skipNextExperienceDrop() {
        witherstormmod$skipNextExperienceDrop = true;
    }

    @Override
    public void witherstormmod$setDeathProtectionActive(boolean active) {
        witherstormmod$deathProtectionActive = active;
    }

    @Override
    public boolean witherstormmod$isDeathProtectionActive() {
        return witherstormmod$deathProtectionActive;
    }

    @Inject(
            method = "checkTotemDeathProtection(Lnet/minecraft/util/DamageSource;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;setHealth(F)V"))
    private void witherstormmod$markDeathProtection(
            DamageSource source, CallbackInfoReturnable<Boolean> callbackInfo) {
        witherstormmod$deathProtectionActive = true;
    }

    @Inject(
            method = "checkTotemDeathProtection(Lnet/minecraft/util/DamageSource;)Z",
            at = @At("TAIL"),
            cancellable = true)
    private void witherstormmod$evolveDyingWitherStorm(
            DamageSource source, CallbackInfoReturnable<Boolean> callbackInfo) {
        if ((Object) this instanceof WitherStormEntity
                && ((WitherStormEntity) (Object) this).tryEvolveFromDeathProtection(source)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "clearActivePotions()V", at = @At("RETURN"))
    private void witherstormmod$resetDeathProtection(CallbackInfo callbackInfo) {
        EntityLivingBase entity = (EntityLivingBase) (Object) this;
        if (!entity.world.isRemote) witherstormmod$deathProtectionActive = false;
    }

    @WrapOperation(
            method = {"onDeathUpdate()V", "func_70609_aI()V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/event/ForgeEventFactory;getExperienceDrop(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/player/EntityPlayer;I)I",
                    remap = false),
            remap = false,
            require = 1)
    private int witherstormmod$skipCapturedExperience(
            EntityLivingBase entity, EntityPlayer player, int originalExperience,
            Operation<Integer> originalOperation) {
        if (!witherstormmod$skipNextExperienceDrop) {
            return originalOperation.call(entity, player, originalExperience);
        }
        witherstormmod$skipNextExperienceDrop = false;
        return 0;
    }
}
