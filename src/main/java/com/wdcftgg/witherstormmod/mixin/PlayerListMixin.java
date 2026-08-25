package com.wdcftgg.witherstormmod.mixin;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 在玩家加入目标维度前解除旧世界的远距风暴跟踪。 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(
            method = "transferPlayerToDimension(Lnet/minecraft/entity/player/EntityPlayerMP;ILnet/minecraftforge/common/util/ITeleporter;)V",
            at = @At("HEAD"))
    private void witherstormmod$logDimensionTransferStart(EntityPlayerMP player,
                                                           int destinationDimension,
                                                           ITeleporter teleporter,
                                                           CallbackInfo callback) {
        StormDiagnosticLogger.info(
                "[风暴诊断][服务端传送开始] 玩家={} 实例={} 源维度={} 目标维度={} 世界实例={} 死亡={}",
                player.getName(), System.identityHashCode(player), player.dimension,
                destinationDimension, System.identityHashCode(player.world), player.isDead);
    }

    @Inject(
            method = "transferPlayerToDimension(Lnet/minecraft/entity/player/EntityPlayerMP;ILnet/minecraftforge/common/util/ITeleporter;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldServer;removeEntityDangerously(Lnet/minecraft/entity/Entity;)V"))
    private void witherstormmod$clearSourceTrackingBeforeTransfer(EntityPlayerMP player,
                                                                   int destinationDimension,
                                                                   ITeleporter teleporter,
                                                                   CallbackInfo callback) {
        WorldServer sourceWorld = player.getServerWorld();
        StormDiagnosticLogger.info(
                "[风暴诊断][服务端传送清理] 玩家={} 实例={} 源维度={} 世界实例={}",
                player.getName(), System.identityHashCode(player),
                sourceWorld == null ? "null" : sourceWorld.provider.getDimension(),
                sourceWorld == null ? "null" : System.identityHashCode(sourceWorld));
        if (sourceWorld != null) sourceWorld.getEntityTracker().removePlayerFromTrackers(player);
    }

    @Inject(
            method = "transferPlayerToDimension(Lnet/minecraft/entity/player/EntityPlayerMP;ILnet/minecraftforge/common/util/ITeleporter;)V",
            at = @At("RETURN"))
    private void witherstormmod$logDimensionTransferEnd(EntityPlayerMP player,
                                                         int destinationDimension,
                                                         ITeleporter teleporter,
                                                         CallbackInfo callback) {
        StormDiagnosticLogger.info(
                "[风暴诊断][服务端传送完成] 玩家={} 实例={} 当前维度={} 世界实例={} 坐标=({},{},{}) 死亡={}",
                player.getName(), System.identityHashCode(player), player.dimension,
                System.identityHashCode(player.world), player.posX, player.posY, player.posZ,
                player.isDead);
    }

    @Inject(
            method = "recreatePlayerEntity(Lnet/minecraft/entity/player/EntityPlayerMP;IZ)Lnet/minecraft/entity/player/EntityPlayerMP;",
            at = @At("HEAD"))
    private void witherstormmod$logRespawnStart(EntityPlayerMP player, int destinationDimension,
                                                boolean conqueredEnd,
                                                CallbackInfoReturnable<EntityPlayerMP> callback) {
        StormDiagnosticLogger.info(
                "[风暴诊断][服务端重生开始] 旧玩家={} 实例={} 当前维度={} 请求维度={} 世界实例={} 死亡={} 末地返回={}",
                player.getName(), System.identityHashCode(player), player.dimension,
                destinationDimension, System.identityHashCode(player.world), player.isDead,
                conqueredEnd);
    }

    @Inject(
            method = "recreatePlayerEntity(Lnet/minecraft/entity/player/EntityPlayerMP;IZ)Lnet/minecraft/entity/player/EntityPlayerMP;",
            at = @At("RETURN"))
    private void witherstormmod$logRespawnEnd(EntityPlayerMP player, int destinationDimension,
                                              boolean conqueredEnd,
                                              CallbackInfoReturnable<EntityPlayerMP> callback) {
        EntityPlayerMP replacement = callback.getReturnValue();
        StormDiagnosticLogger.info(
                "[风暴诊断][服务端重生完成] 旧实例={} 新实例={} 新维度={} 世界实例={} 坐标=({},{},{})",
                System.identityHashCode(player),
                replacement == null ? "null" : System.identityHashCode(replacement),
                replacement == null ? "null" : replacement.dimension,
                replacement == null ? "null" : System.identityHashCode(replacement.world),
                replacement == null ? "null" : replacement.posX,
                replacement == null ? "null" : replacement.posY,
                replacement == null ? "null" : replacement.posZ);
    }
}
