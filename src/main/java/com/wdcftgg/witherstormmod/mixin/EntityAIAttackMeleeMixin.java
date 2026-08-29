package com.wdcftgg.witherstormmod.mixin;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;





@Mixin(EntityAIAttackMelee.class)
public abstract class EntityAIAttackMeleeMixin {

    @Shadow
    protected EntityCreature attacker;

    @Inject(method = "updateTask()V", at = @At("HEAD"), cancellable = true)
    private void witherstormmod$skipUpdateWithoutTarget(CallbackInfo callback) {
        if (attacker.getAttackTarget() == null) callback.cancel();
    }
}
