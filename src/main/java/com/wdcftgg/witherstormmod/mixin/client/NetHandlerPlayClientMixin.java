package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.entity.DistantStormPart;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;


@Mixin(NetHandlerPlayClient.class)
public abstract class NetHandlerPlayClientMixin {
    @Inject(method = "handleRespawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V",
            shift = At.Shift.AFTER))
    private void witherstormmod$logRespawnStart(SPacketRespawn packet, CallbackInfo callback) {
        if (!StormDiagnosticLogger.isEnabled()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        StormDiagnosticLogger.info(
                "[风暴诊断][客户端重生包开始] 线程={} 目标维度={} 旧玩家实例={} 旧玩家维度={} 旧世界实例={} 旧世界维度={}",
                Thread.currentThread().getName(), packet.getDimensionID(),
                minecraft.player == null ? "null" : System.identityHashCode(minecraft.player),
                minecraft.player == null ? "null" : minecraft.player.dimension,
                minecraft.world == null ? "null" : System.identityHashCode(minecraft.world),
                minecraft.world == null ? "null" : minecraft.world.provider.getDimension());
        logDistantEntities("客户端重生包开始明细", minecraft.world);
    }

    @Inject(method = "handleRespawn", at = @At("RETURN"))
    private void witherstormmod$logRespawnEnd(SPacketRespawn packet, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (StormDiagnosticLogger.isEnabled()) {
            StormDiagnosticLogger.info(
                    "[风暴诊断][客户端重生包完成] 线程={} 目标维度={} 新玩家实例={} 新玩家维度={} 新世界实例={} 新世界维度={}",
                    Thread.currentThread().getName(), packet.getDimensionID(),
                    minecraft.player == null ? "null" : System.identityHashCode(minecraft.player),
                    minecraft.player == null ? "null" : minecraft.player.dimension,
                    minecraft.world == null ? "null" : System.identityHashCode(minecraft.world),
                    minecraft.world == null ? "null" : minecraft.world.provider.getDimension());
            logDistantEntities("客户端重生包完成明细", minecraft.world);
        }
        if (minecraft.world != null && minecraft.player != null
                && minecraft.player.dimension == packet.getDimensionID()
                && minecraft.world.provider.getDimension() == packet.getDimensionID()) {
            ModNetwork.notifyClientWorldReady(packet.getDimensionID());
            StormDiagnosticLogger.info(
                    "[风暴诊断][客户端维度就绪确认] 玩家实例={} 维度={} 世界实例={}",
                    System.identityHashCode(minecraft.player), packet.getDimensionID(),
                    System.identityHashCode(minecraft.world));
        }
    }

    @Inject(method = "handleDestroyEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V",
            shift = At.Shift.AFTER))
    private void witherstormmod$logDestroyEntities(SPacketDestroyEntities packet,
                                                    CallbackInfo callback) {
        if (!StormDiagnosticLogger.isEnabled()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        int[] ids = packet.getEntityIDs();
        if (!containsDistantStorm(minecraft.world, ids)) return;
        StormDiagnosticLogger.info(
                "[风暴诊断][客户端实体移除包] 线程={} 世界实例={} 维度={} ids={}",
                Thread.currentThread().getName(),
                minecraft.world == null ? "null" : System.identityHashCode(minecraft.world),
                minecraft.world == null ? "null" : minecraft.world.provider.getDimension(),
                Arrays.toString(ids));
        logDistantEntities("客户端实体移除包前明细", minecraft.world);
    }

    private static boolean containsDistantStorm(World world, int[] ids) {
        if (world == null) return false;
        for (int id : ids) {
            if (world.getEntityByID(id) instanceof DistantStormPart) return true;
        }
        return false;
    }

    private static void logDistantEntities(String marker, World world) {
        if (!StormDiagnosticLogger.isEnabled() || world == null) return;
        int count = 0;
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof DistantStormPart)) continue;
            ++count;
            StormDiagnosticLogger.info(
                    "[风暴诊断][{}] 类型={} 实例={} id={} uuid={} 维度={} addedToChunk={} isDead={} 坐标=({},{},{})",
                    marker, entity.getClass().getSimpleName(), System.identityHashCode(entity),
                    entity.getEntityId(), entity.getUniqueID(), entity.dimension,
                    entity.addedToChunk, entity.isDead, entity.posX, entity.posY, entity.posZ);
        }
        StormDiagnosticLogger.info("[风暴诊断][{}计数] 世界实例={} 数量={}",
                marker, System.identityHashCode(world), count);
    }
}
