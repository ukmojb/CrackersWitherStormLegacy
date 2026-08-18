package com.wdcftgg.witherstormmod.common.event;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.access.EntityLivingBaseDeathProtectionAccess;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessCapability;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessTracker;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.init.ModEffects;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.potion.WitherSicknessEffect;
import com.wdcftgg.witherstormmod.common.taint.TaintingManager;
import com.wdcftgg.witherstormmod.common.world.BowelsDimensions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 驱动凋零病 Capability、持久化、玩家复制和死亡转化。 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WitherSicknessEvents {
    private static long lastProfileTick = Long.MIN_VALUE;
    private static long profileNanos;
    private WitherSicknessEvents() {
    }

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityLivingBase
                && !(event.getObject() instanceof SickenedMobEntity)) {
            event.addCapability(WitherSicknessCapability.ID,
                    new WitherSicknessCapability.Provider((EntityLivingBase) event.getObject()));
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.world.isRemote) return;
        long profileStart = System.nanoTime();
        List<EntityLivingBase> storms = new ArrayList<EntityLivingBase>();
        for (WitherStormEntity storm : WorldUtil.getCachedStorms(event.world)) {
            if (storm.getPhase() > 1) {
                storms.add(storm);
            }
        }
        for (Entity entity : event.world.loadedEntityList) {
            if (entity instanceof SupplementalEntities.WitherStormSegmentEntity
                    && ((SupplementalEntities.WitherStormSegmentEntity) entity).getPhase() > 1) {
                storms.add((SupplementalEntities.WitherStormSegmentEntity) entity);
            }
        }

        for (Entity entity : event.world.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            EntityLivingBase living = (EntityLivingBase) entity;
            WitherSicknessTracker tracker = WitherSicknessCapability.get(living);
            if (tracker == null) continue;
            boolean nearStorm = living.dimension == BowelsDimensions.DIMENSION_ID;
            if (!nearStorm) {
                for (EntityLivingBase storm : storms) {
                    boolean nearby = storm instanceof WitherStormEntity
                            ? ((WitherStormEntity) storm).isEntityNearby(living)
                            : ((SupplementalEntities.WitherStormSegmentEntity) storm)
                            .isEntityNearby(living);
                    if (nearby) {
                        nearStorm = true;
                        break;
                    }
                }
            }
            tracker.setNearStorm(nearStorm);
            tracker.tick();
        }
        long elapsed = System.nanoTime() - profileStart;
        profileNanos += elapsed;
        long tick = event.world.getTotalWorldTime();
        if (tick - lastProfileTick >= 200) {
            WitherStormMod.LOGGER.info("WitherSickness worldTick profile: "
                    + String.format(java.util.Locale.ROOT, "%.3f",
                    profileNanos / 1000000.0D / 40) + "ms/tick");
            lastProfileTick = tick;
            profileNanos = 0;
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntityLiving().world.isRemote || !WitherStormConfig.sickenedMobConversions
                || event.getSource() != WitherSicknessEffect.DAMAGE_SOURCE) return;
        if (TaintingManager.convertEntity(event.getEntityLiving(), true)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPotionRemove(PotionEvent.PotionRemoveEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (((EntityLivingBaseDeathProtectionAccess) entity)
                .witherstormmod$isDeathProtectionActive()
                && event.getPotion() == ModEffects.WITHER_SICKNESS) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        WitherSicknessTracker original = WitherSicknessCapability.get(event.getOriginal());
        WitherSicknessTracker replacement = WitherSicknessCapability.get(event.getEntityPlayer());
        if (original == null || replacement == null) return;
        replacement.copyFrom(original);
        if (!WitherStormConfig.keepSicknessAfterRespawn) {
            replacement.resetInfection();
            return;
        }
        PotionEffect effect = event.getOriginal().getActivePotionEffect(ModEffects.WITHER_SICKNESS);
        if (effect != null) {
            PotionEffect copied = new PotionEffect(effect);
            copied.setCurativeItems(Collections.emptyList());
            event.getEntityPlayer().addPotionEffect(copied);
        }
    }

    @SubscribeEvent
    public static void onCheckDespawn(LivingSpawnEvent.AllowDespawn event) {
        WitherSicknessTracker tracker = WitherSicknessCapability.get(event.getEntityLiving());
        if (tracker != null && !tracker.isActuallyImmune()
                && (tracker.isInfected() || tracker.isBeingCured())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        synchronizePlayer(event.player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        synchronizePlayer(event.player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        synchronizePlayer(event.player);
    }

    @SubscribeEvent
    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)
                || !(event.getTarget() instanceof EntityLivingBase)) return;
        EntityLivingBase target = (EntityLivingBase) event.getTarget();
        WitherSicknessTracker tracker = WitherSicknessCapability.get(target);
        if (tracker != null) {
            ModNetwork.syncWitherSicknessTo(target, tracker.write(),
                    (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    private static void synchronizePlayer(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;
        WitherSicknessTracker tracker = WitherSicknessCapability.get(player);
        if (tracker != null) {
            ModNetwork.syncWitherSicknessTo(player, tracker.write(), (EntityPlayerMP) player);
        }
    }
}
