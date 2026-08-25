package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import net.minecraft.client.gui.BossInfoClient;
import net.minecraft.client.gui.GuiBossOverlay;
import net.minecraft.network.play.server.SPacketUpdateBossInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

/** 记录客户端 BossBar 表收到的每次增删及其最终状态。 */
@Mixin(GuiBossOverlay.class)
public abstract class GuiBossOverlayMixin {
    @Shadow @Final private Map<UUID, BossInfoClient> mapBossInfos;

    @Inject(method = "read", at = @At("HEAD"))
    private void witherstormmod$logBossPacketStart(SPacketUpdateBossInfo packet,
                                                   CallbackInfo callback) {
        if (!StormDiagnosticLogger.isEnabled()) return;
        StormDiagnosticLogger.info(
                "[风暴诊断][客户端BossBar包开始] 操作={} uuid={} 包名称={} 处理前数量={} 处理前存在={} 列表={}",
                packet.getOperation(), packet.getUniqueId(), packetName(packet),
                mapBossInfos.size(), mapBossInfos.containsKey(packet.getUniqueId()),
                describeBossBars());
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void witherstormmod$logBossPacketEnd(SPacketUpdateBossInfo packet,
                                                 CallbackInfo callback) {
        if (!StormDiagnosticLogger.isEnabled()) return;
        StormDiagnosticLogger.info(
                "[风暴诊断][客户端BossBar包完成] 操作={} uuid={} 处理后数量={} 处理后存在={} 列表={}",
                packet.getOperation(), packet.getUniqueId(), mapBossInfos.size(),
                mapBossInfos.containsKey(packet.getUniqueId()), describeBossBars());
    }

    @Inject(method = "clearBossInfos", at = @At("HEAD"))
    private void witherstormmod$logBossClear(CallbackInfo callback) {
        if (!StormDiagnosticLogger.isEnabled()) return;
        StormDiagnosticLogger.info("[风暴诊断][客户端BossBar全清] 清理前数量={} 列表={}",
                mapBossInfos.size(), describeBossBars());
    }

    private static String packetName(SPacketUpdateBossInfo packet) {
        return packet.getName() == null ? "null" : packet.getName().getUnformattedText();
    }

    private String describeBossBars() {
        StringBuilder description = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<UUID, BossInfoClient> entry : mapBossInfos.entrySet()) {
            if (!first) description.append(", ");
            first = false;
            description.append(entry.getKey()).append('=')
                    .append(entry.getValue().getName().getUnformattedText());
        }
        return description.append(']').toString();
    }
}
