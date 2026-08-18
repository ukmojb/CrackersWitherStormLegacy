package com.wdcftgg.witherstormmod.mixin;

import com.wdcftgg.witherstormmod.common.entity.SymbiontDragonFireballEntity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 让共生体龙息弹只使用客户端重建的命令方块轨迹。 */
@Mixin(EntityFireball.class)
public abstract class EntityFireballMixin {

    @Redirect(
            method = "onUpdate()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V",
                    ordinal = 1),
            require = 1)
    private void witherstormmod$suppressVanillaTrailParticle(
            World world, EnumParticleTypes particle, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, int[] parameters) {
        if ((Object) this instanceof SymbiontDragonFireballEntity) return;
        world.spawnParticle(particle, x, y, z, velocityX, velocityY, velocityZ, parameters);
    }
}
