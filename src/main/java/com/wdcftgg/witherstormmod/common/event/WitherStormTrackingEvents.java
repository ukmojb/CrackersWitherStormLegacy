package com.wdcftgg.witherstormmod.common.event;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

/** 维护跨维度时凋零风暴的远距实体跟踪状态。 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WitherStormTrackingEvents {
    private WitherStormTrackingEvents() {
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        StormDiagnosticLogger.info(
                "[风暴诊断][维度事件] 玩家={} 实例={} {} -> {} 当前世界={} 死亡={}",
                player.getName(), System.identityHashCode(player), event.fromDim, event.toDim,
                System.identityHashCode(player.world), player.isDead);
        MinecraftServer server = player.getServer();
        WorldServer previousWorld = server == null ? null : server.getWorld(event.fromDim);
        clearWorldTracking(previousWorld, player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getOriginal() instanceof EntityPlayerMP)) return;
        EntityPlayerMP original = (EntityPlayerMP) event.getOriginal();
        StormDiagnosticLogger.info(
                "[风暴诊断][克隆事件] 旧玩家={} 旧实例={} 旧维度={} 新实例={} 新维度={} 死亡克隆={}",
                original.getName(), System.identityHashCode(original), original.dimension,
                System.identityHashCode(event.getEntityPlayer()), event.getEntityPlayer().dimension,
                event.isWasDeath());
        MinecraftServer server = original.getServer();
        if (server == null) return;
        // 死亡重生会替换玩家实体，必须用旧实例移除 BossInfo 观众和远距风暴跟踪。
        for (WorldServer world : server.worlds) clearWorldTracking(world, original);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        StormDiagnosticLogger.info(
                "[风暴诊断][重生事件] 玩家={} 新实例={} 维度={} 世界实例={} 坐标=({},{},{})",
                player.getName(), System.identityHashCode(player), player.dimension,
                System.identityHashCode(player.world), player.posX, player.posY, player.posZ);
    }

    private static void clearWorldTracking(WorldServer world, EntityPlayerMP player) {
        if (world == null || player == null) return;
        StormDiagnosticLogger.info(
                "[风暴诊断][清理世界追踪] 玩家={} 实例={} 玩家维度={} 清理维度={} 世界实例={}",
                player.getName(), System.identityHashCode(player), player.dimension,
                world.provider.getDimension(), System.identityHashCode(world));
        world.getEntityTracker().removePlayerFromTrackers(player);
    }
}
