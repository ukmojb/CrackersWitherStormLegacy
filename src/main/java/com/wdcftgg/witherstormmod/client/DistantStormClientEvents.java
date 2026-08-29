package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.entity.DistantStormPart;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;


@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class DistantStormClientEvents {
    private static World lastLoggedWorld;
    private static int lastSummaryTick = Integer.MIN_VALUE;

    private DistantStormClientEvents() {
    }

    @SubscribeEvent
    public static void onCanUpdate(EntityEvent.CanUpdate event) {
        if (WitherStormClientConfig.distantRenderer
                && event.getEntity() instanceof DistantStormPart) {
            event.setCanUpdate(true);
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (!world.isRemote) return;
        StormDiagnosticLogger.info(
                "[风暴诊断][客户端世界加载] 维度={} 世界实例={} 已加载实体={}",
                world.provider.getDimension(), System.identityHashCode(world),
                world.loadedEntityList.size());
        logWorldEntities("客户端世界加载明细", world);
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (!world.isRemote) return;
        StormDiagnosticLogger.info(
                "[风暴诊断][客户端世界卸载] 维度={} 世界实例={} 已加载实体={}",
                world.provider.getDimension(), System.identityHashCode(world),
                world.loadedEntityList.size());
        logWorldEntities("客户端世界卸载明细", world);
        if (lastLoggedWorld == world) {
            lastLoggedWorld = null;
            lastSummaryTick = Integer.MIN_VALUE;
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote || !(event.getEntity() instanceof DistantStormPart)) return;
        if (!StormDiagnosticLogger.isEnabled()) return;
        StormDiagnosticLogger.info("[风暴诊断][客户端风暴实体加入] 世界实例={} {}",
                System.identityHashCode(event.getWorld()), describe(event.getEntity()));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft.world;
        if (world == null || minecraft.player == null) return;
        if (!StormDiagnosticLogger.isEnabled()) {
            lastLoggedWorld = null;
            lastSummaryTick = Integer.MIN_VALUE;
            return;
        }
        int tick = minecraft.player.ticksExisted;
        boolean changedWorld = lastLoggedWorld != world;
        if (!changedWorld && (tick % 20 != 0 || tick == lastSummaryTick)) return;
        lastLoggedWorld = world;
        lastSummaryTick = tick;
        StormDiagnosticLogger.info(
                "[风暴诊断][客户端世界摘要] 客户端tick={} 玩家实例={} 玩家维度={} 世界维度={} 世界实例={} 玩家坐标=({},{},{}) 已加载实体={}",
                tick, System.identityHashCode(minecraft.player), minecraft.player.dimension,
                world.provider.getDimension(), System.identityHashCode(world),
                minecraft.player.posX, minecraft.player.posY, minecraft.player.posZ,
                world.loadedEntityList.size());
        logWorldEntities("客户端风暴实体摘要", world);
    }

    private static void logWorldEntities(String marker, World world) {
        if (!StormDiagnosticLogger.isEnabled()) return;
        int count = 0;
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof DistantStormPart)) continue;
            ++count;
            StormDiagnosticLogger.info("[风暴诊断][{}] 世界实例={} {}",
                    marker, System.identityHashCode(world), describe(entity));
        }
        StormDiagnosticLogger.info("[风暴诊断][{}计数] 世界实例={} 数量={}",
                marker, System.identityHashCode(world), count);
    }

    private static String describe(Entity entity) {
        StringBuilder state = new StringBuilder();
        if (entity instanceof WitherStormEntity) {
            WitherStormEntity storm = (WitherStormEntity) entity;
            state.append(" phase=").append(storm.getPhase())
                    .append(" playDead=").append(storm.getPlayDeadState())
                    .append(" consumedMass=").append(storm.getConsumedMass())
                    .append(" deathTime=").append(storm.getDeathTime())
                    .append(" invisible=").append(storm.isInvisible())
                    .append(" fade=").append(storm.getFadeAnimation())
                    .append(" brightness=").append(storm.getBrightnessForRender())
                    .append(" debrisRings=").append(storm.getDebrisRings().size());
        } else if (entity instanceof SupplementalEntities.WitherStormSegmentEntity) {
            SupplementalEntities.WitherStormSegmentEntity segment =
                    (SupplementalEntities.WitherStormSegmentEntity) entity;
            WitherStormEntity owner = segment.getParentStorm();
            state.append(" phase=").append(segment.getPhase())
                    .append(" playingDead=").append(segment.isPlayingDead())
                    .append(" owner=").append(owner == null ? "null" : owner.getUniqueID());
        }
        return "类型=" + entity.getClass().getSimpleName()
                + " 实例=" + System.identityHashCode(entity)
                + " id=" + entity.getEntityId()
                + " uuid=" + entity.getUniqueID()
                + " 维度=" + entity.dimension
                + " 坐标=(" + entity.posX + "," + entity.posY + "," + entity.posZ + ")"
                + " addedToChunk=" + entity.addedToChunk
                + " forceSpawn=" + entity.forceSpawn
                + " isDead=" + entity.isDead
                + " ticks=" + entity.ticksExisted
                + state;
    }
}
