package com.wdcftgg.witherstormmod.mixin;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.entity.DistantStormPart;
import com.wdcftgg.witherstormmod.common.entity.DistantStormTrackingResync;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Set;

/** 在维度切换完成后，仅重建远距风暴实体对指定玩家的追踪。 */
@Mixin(EntityTracker.class)
public abstract class EntityTrackerMixin implements DistantStormTrackingResync {
    @Shadow @Final private Set<EntityTrackerEntry> entries;

    @Override
    public void witherstormmod$resyncDistantStorms(EntityPlayerMP player) {
        if (player == null) return;
        int count = 0;
        for (EntityTrackerEntry entry : new ArrayList<EntityTrackerEntry>(entries)) {
            Entity tracked = entry.getTrackedEntity();
            if (!(tracked instanceof DistantStormPart) || tracked.world != player.world) continue;
            // 旧生成包可能已进入被卸载的客户端世界，必须先移除服务端记录再重新加入。
            entry.removeFromTrackedPlayers(player);
            entry.updatePlayerEntity(player);
            ++count;
        }
        StormDiagnosticLogger.info(
                "[风暴诊断][客户端维度就绪后重同步] 玩家={} 玩家实例={} 维度={} 世界实例={} 条目={}",
                player.getName(), System.identityHashCode(player), player.dimension,
                System.identityHashCode(player.world), count);
    }
}
