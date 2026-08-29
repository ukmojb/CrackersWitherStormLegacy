package com.wdcftgg.witherstormmod.common.event;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.taint.TaintingManager;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;


@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WitherStormUtilEvents {

    private WitherStormUtilEvents() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        if (event.getEntity() instanceof EntityFishHook) {
            if (hit.typeOfHit == RayTraceResult.Type.ENTITY
                    && (hit.entityHit instanceof WitherStormEntity
                    || hit.entityHit instanceof SupplementalEntities.WitherStormSegmentEntity
                    || hit.entityHit instanceof SupplementalEntities.CommandBlockEntity)) {
                event.setCanceled(true);
            }
            return;
        }
        if (!(event.getEntity() instanceof EntityPotion)) return;
        EntityPotion potion = (EntityPotion) event.getEntity();
        if (potion.world.isRemote || hit.typeOfHit != RayTraceResult.Type.BLOCK) return;
        PotionType type = PotionUtils.getPotionFromItem(potion.getPotion());
        if (type == null) return;
        BlockPos center = hit.getBlockPos();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    TaintingManager.taintBlock(potion.world, center.add(x, y, z), type);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            ModNetwork.syncDiagnosticLogging((EntityPlayerMP) event.player);
        }
        if (!WitherStormConfig.flyingEnabledWarning
                || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.getServer() == null || player.getServer().isFlightAllowed()
                || !player.getServer().getPlayerList().canSendCommands(player.getGameProfile())) return;
        player.sendMessage(new TextComponentTranslation(
                "chat.witherstormmod.flyingDisabled.notice"));
        WitherStormConfig.flyingEnabledWarning = false;
        ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
    }

}
