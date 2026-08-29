package com.wdcftgg.witherstormmod.mixin;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.entity.DistantStormPart;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {
    @Shadow @Final private Entity trackedEntity;
    @Shadow @Final private int range;
    @Shadow private long encodedPosX;
    @Shadow private long encodedPosZ;

    @Inject(method = "isVisibleTo(Lnet/minecraft/entity/player/EntityPlayerMP;)Z",
            at = @At("HEAD"), cancellable = true)
    private void witherstormmod$useDistantStormRange(EntityPlayerMP player,
                                                      CallbackInfoReturnable<Boolean> callback) {
        if (!(trackedEntity instanceof DistantStormPart)) return;
        if (player.world != trackedEntity.world) {
            callback.setReturnValue(false);
            return;
        }
        double distanceX = player.posX - encodedPosX / 4096.0D;
        double distanceZ = player.posZ - encodedPosZ / 4096.0D;
        callback.setReturnValue(distanceX >= -range && distanceX <= range
                && distanceZ >= -range && distanceZ <= range
                && trackedEntity.isSpectatedByPlayer(player));
    }

    @Inject(method = "updatePlayerEntity(Lnet/minecraft/entity/player/EntityPlayerMP;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;addTrackingPlayer(Lnet/minecraft/entity/player/EntityPlayerMP;)V",
                    shift = At.Shift.AFTER))
    private void witherstormmod$logDistantTrackingAdd(EntityPlayerMP player, CallbackInfo callback) {
        if (!(trackedEntity instanceof DistantStormPart)) return;
        StormDiagnosticLogger.info(
                "[风暴诊断][服务器追踪加入] 实体={} id={} uuid={} 实体维度={} 世界实例={} 玩家={} 玩家实例={} 玩家维度={} 玩家世界={} 范围={}",
                trackedEntity.getClass().getSimpleName(), trackedEntity.getEntityId(),
                trackedEntity.getUniqueID(), trackedEntity.dimension,
                System.identityHashCode(trackedEntity.world), player.getName(),
                System.identityHashCode(player), player.dimension,
                System.identityHashCode(player.world), range);
    }

    @Inject(method = "updatePlayerEntity(Lnet/minecraft/entity/player/EntityPlayerMP;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;removeTrackingPlayer(Lnet/minecraft/entity/player/EntityPlayerMP;)V",
                    shift = At.Shift.AFTER))
    private void witherstormmod$logDistantTrackingRemoveByVisibility(EntityPlayerMP player,
                                                                     CallbackInfo callback) {
        witherstormmod$logDistantTrackingRemove("可见性", player);
    }

    @Inject(method = {
            "removeFromTrackedPlayers(Lnet/minecraft/entity/player/EntityPlayerMP;)V",
            "removeTrackedPlayerSymmetric(Lnet/minecraft/entity/player/EntityPlayerMP;)V"
    }, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;removeTrackingPlayer(Lnet/minecraft/entity/player/EntityPlayerMP;)V",
            shift = At.Shift.AFTER))
    private void witherstormmod$logDistantTrackingRemoveExplicit(EntityPlayerMP player,
                                                                  CallbackInfo callback) {
        witherstormmod$logDistantTrackingRemove("显式清理", player);
    }

    private void witherstormmod$logDistantTrackingRemove(String reason, EntityPlayerMP player) {
        if (!(trackedEntity instanceof DistantStormPart)) return;
        StormDiagnosticLogger.info(
                "[风暴诊断][服务器追踪移除:{}] 实体={} id={} uuid={} 实体维度={} 世界实例={} 玩家={} 玩家实例={} 玩家维度={} 玩家世界={}",
                reason, trackedEntity.getClass().getSimpleName(), trackedEntity.getEntityId(),
                trackedEntity.getUniqueID(), trackedEntity.dimension,
                System.identityHashCode(trackedEntity.world), player.getName(),
                System.identityHashCode(player), player.dimension,
                System.identityHashCode(player.world));
    }
}
