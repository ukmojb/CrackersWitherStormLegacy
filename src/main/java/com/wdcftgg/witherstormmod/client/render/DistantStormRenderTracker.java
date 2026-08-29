package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.common.entity.DistantStormPart;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;


public final class DistantStormRenderTracker {
    private static final Set<Entity> RENDERED_THIS_FRAME =
            Collections.newSetFromMap(new IdentityHashMap<Entity, Boolean>());
    private static final Map<Entity, Integer> LAST_STANDARD_LOG_TICK =
            new IdentityHashMap<Entity, Integer>();
    private static final Map<Entity, Integer> LAST_FALLBACK_LOG_TICK =
            new IdentityHashMap<Entity, Integer>();
    private static World loggedWorld;
    private static int lastSummaryTick = Integer.MIN_VALUE;

    private DistantStormRenderTracker() {
    }

    public static void markRendered(Entity entity) {
        if (entity instanceof DistantStormPart) {
            RENDERED_THIS_FRAME.add(entity);
            int tick = clientTick();
            if (StormDiagnosticLogger.isEnabled()
                    && shouldLogEntity(LAST_STANDARD_LOG_TICK, entity, tick)) {
                StormDiagnosticLogger.info(
                        "[风暴诊断][普通实体渲染] 类型={} 实例={} id={} uuid={} 维度={} addedToChunk={} isDead={} tick={} 坐标=({},{},{})",
                        entity.getClass().getSimpleName(), System.identityHashCode(entity),
                        entity.getEntityId(), entity.getUniqueID(), entity.dimension,
                        entity.addedToChunk, entity.isDead, tick,
                        entity.posX, entity.posY, entity.posZ);
            }
        }
    }

    public static void renderMissing(float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        try {
            World world = minecraft.world;
            int tick = clientTick();
            boolean diagnosticsEnabled = StormDiagnosticLogger.isEnabled();
            if (!diagnosticsEnabled || loggedWorld != world) {
                loggedWorld = world;
                lastSummaryTick = Integer.MIN_VALUE;
                LAST_STANDARD_LOG_TICK.clear();
                LAST_FALLBACK_LOG_TICK.clear();
            }
            boolean logSummary = diagnosticsEnabled && tick != Integer.MIN_VALUE
                    && tick != lastSummaryTick && tick % 20 == 0;
            if (logSummary) lastSummaryTick = tick;
            if (!WitherStormClientConfig.distantRenderer
                    || world == null
                    || minecraft.getRenderViewEntity() == null) {
                if (logSummary) {
                    StormDiagnosticLogger.info(
                            "[风暴诊断][远距补绘不可用] tick={} 配置={} 世界={} 观察实体={}",
                            tick, WitherStormClientConfig.distantRenderer,
                            world == null ? "null" : System.identityHashCode(world),
                            minecraft.getRenderViewEntity() == null ? "null"
                                    : minecraft.getRenderViewEntity().getEntityId());
                }
                return;
            }

            RenderManager renderManager = minecraft.getRenderManager();
            int distantCount = 0;
            int deadCount = 0;
            int standardCount = 0;
            int outsideFarPlaneCount = 0;
            int fallbackCount = 0;



            minecraft.entityRenderer.enableLightmap();
            RenderHelper.enableStandardItemLighting();
            try {
                for (Entity entity : world.loadedEntityList) {
                    if (!(entity instanceof DistantStormPart)) continue;
                    ++distantCount;
                    if (entity.isDead) {
                        ++deadCount;
                        continue;
                    }
                    if (RENDERED_THIS_FRAME.contains(entity)) {
                        ++standardCount;
                        continue;
                    }
                    if (!DistantProjection.isWithinFarPlane(entity.posX, entity.posY, entity.posZ,
                            renderManager.viewerPosX, renderManager.viewerPosY,
                            renderManager.viewerPosZ)) {
                        ++outsideFarPlaneCount;
                        continue;
                    }
                    ++fallbackCount;
                    boolean logEntity = diagnosticsEnabled
                            && shouldLogEntity(LAST_FALLBACK_LOG_TICK, entity, tick);
                    if (logEntity) {
                        StormDiagnosticLogger.info(
                                "[风暴诊断][远距补绘开始] 类型={} 实例={} id={} uuid={} 维度={} addedToChunk={} tick={} 坐标=({},{},{}) 观察坐标=({},{},{})",
                                entity.getClass().getSimpleName(), System.identityHashCode(entity),
                                entity.getEntityId(), entity.getUniqueID(), entity.dimension,
                                entity.addedToChunk, tick, entity.posX, entity.posY, entity.posZ,
                                renderManager.viewerPosX, renderManager.viewerPosY,
                                renderManager.viewerPosZ);
                    }
                    renderManager.renderEntityStatic(entity, partialTicks, false);
                    if (logEntity) {
                        StormDiagnosticLogger.info(
                                "[风暴诊断][远距补绘完成] 类型={} 实例={} id={} uuid={} tick={}",
                                entity.getClass().getSimpleName(), System.identityHashCode(entity),
                                entity.getEntityId(), entity.getUniqueID(), tick);
                    }
                }
            } finally {
                RenderHelper.disableStandardItemLighting();
                minecraft.entityRenderer.disableLightmap();
            }
            if (logSummary) {
                StormDiagnosticLogger.info(
                        "[风暴诊断][远距渲染摘要] tick={} 世界实例={} 维度={} 风暴实体={} 普通渲染={} 补绘={} 超出扩展远平面={} 死亡={}",
                        tick, System.identityHashCode(world), world.provider.getDimension(),
                        distantCount, standardCount, fallbackCount, outsideFarPlaneCount, deadCount);
            }
        } finally {
            RENDERED_THIS_FRAME.clear();
        }
    }

    private static int clientTick() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.player == null ? Integer.MIN_VALUE : minecraft.player.ticksExisted;
    }

    private static boolean shouldLogEntity(Map<Entity, Integer> ticks, Entity entity, int tick) {
        Integer previous = ticks.get(entity);
        if (previous != null && (tick == Integer.MIN_VALUE || tick - previous < 20)) return false;
        ticks.put(entity, tick);
        return true;
    }
}
