package com.wdcftgg.witherstormmod.common.event;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.entity.ai.AvoidWitherStormAI;
import com.wdcftgg.witherstormmod.common.entity.ai.FightSickenedMobsAI;
import com.wdcftgg.witherstormmod.common.entity.ai.NearestAttackingWitherStormAI;
import com.wdcftgg.witherstormmod.common.entity.ai.SwellAtWitherStormAI;
import com.wdcftgg.witherstormmod.common.entity.ai.VillagerPanicAI;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityVex;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thedarkcolour.futuremc.entity.bee.EntityBee;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** 将上游的凋零风暴通用生物 AI 注入到 1.12.2 对应生物。 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WitherStormMobEvents {
    private static final Set<EntityCreature> INJECTED_ENTITIES =
            Collections.newSetFromMap(new WeakHashMap<EntityCreature, Boolean>());

    private WitherStormMobEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (!event.getWorld().isRemote && entity instanceof WitherStormEntity) {
            WitherStormEntity storm = (WitherStormEntity) entity;
            if (!storm.wasRestoredFromPersistentData()
                    && !WitherStormConfig.isSummoningDimensionAllowed(
                    event.getWorld().provider.getDimension())) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getWorld().isRemote) return;
        redirectSkeletonArrow(entity);
        if (!WitherStormConfig.injectCustomAiBehavior || !(entity instanceof EntityCreature)) return;
        EntityCreature creature = (EntityCreature) entity;
        if (isBlacklisted(creature) || !INJECTED_ENTITIES.add(creature)) return;

        if (canRunAwayFromWitherStorm(creature)) {
            creature.tasks.addTask(0, new AvoidWitherStormAI(creature, 300.0F, 1.55D, 1.55D));
        }
        if (creature instanceof EntityVillager) {
            // 上游村民通过 WitherStormPanicTrigger 专用脑行为恐慌逃离，1.12 以独立 AI 等效
            creature.tasks.addTask(0, new VillagerPanicAI((EntityVillager) creature));
        }
        if (canAttackWitherStormBack(creature)) {
            creature.targetTasks.addTask(0, new NearestAttackingWitherStormAI(creature, 10));
        }
        if (creature instanceof EntityCreeper) {
            // 原版 EntityAICreeperSwell 使用 2；提高优先级后才能稳定拦截其状态更新。
            creature.tasks.addTask(1, new SwellAtWitherStormAI((EntityCreeper) creature));
        }
        if (canAttackSickenedMobs(creature)) {
            creature.targetTasks.addTask(-1, new FightSickenedMobsAI(creature));
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        AvoidWitherStormAI.clearWorldCache(event.getWorld());
    }

    /** 原版及病化骷髅箭生成后沿用上游的光束头部瞄准轨迹。 */
    private static void redirectSkeletonArrow(Entity entity) {
        if (!(entity instanceof EntityArrow)) return;
        EntityArrow arrow = (EntityArrow) entity;
        if (!(arrow.shootingEntity instanceof AbstractSkeleton)
                && !(arrow.shootingEntity instanceof SickenedEntities.SickenedSkeletonEntity)) return;
        EntityLiving skeleton = (EntityLiving) arrow.shootingEntity;
        EntityLivingBase target = skeleton.getAttackTarget();
        if (!(target instanceof WitherStormEntity)) return;
        WitherStormEntity storm = (WitherStormEntity) target;
        int head = storm.findContainingTractorBeamHead(skeleton, 4.0D);
        if (head < 0) return;
        Vec3d headPosition = storm.getHeadPositionForBeam(head);
        double deltaX = headPosition.x - skeleton.posX;
        double deltaY = headPosition.y - arrow.posY;
        double deltaZ = headPosition.z - skeleton.posZ;
        double horizontalDistance = MathHelper.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        arrow.shoot(deltaX, deltaY + horizontalDistance * 0.2D, deltaZ,
                1.6F, 14 - skeleton.world.getDifficulty().getId() * 4);
    }

    private static boolean isBlacklisted(Entity entity) {
        ResourceLocation registryName = EntityList.getKey(entity);
        if (registryName == null || WitherStormConfig.injectAiMobBlacklist == null) return false;
        String entityName = registryName.toString();
        for (String blacklistedName : WitherStormConfig.injectAiMobBlacklist) {
            if (blacklistedName != null && entityName.equals(blacklistedName.trim())) return true;
        }
        return false;
    }

    private static boolean canRunAwayFromWitherStorm(EntityCreature creature) {
        if (creature instanceof SickenedMobEntity || creature instanceof EntityWitherSkeleton) return false;
        return creature instanceof EntityZombie
                || creature instanceof EntityPigZombie
                || creature instanceof EntitySpider
                || creature instanceof AbstractSkeleton
                || creature instanceof EntityCreeper
                || creature instanceof EntityAnimal;
    }

    private static boolean canAttackWitherStormBack(EntityCreature creature) {
        return !(creature instanceof SickenedMobEntity)
                && !(creature instanceof EntityWitherSkeleton)
                && (creature instanceof EntityCreeper || creature instanceof AbstractSkeleton);
    }

    private static boolean canAttackSickenedMobs(EntityCreature creature) {
        if (creature instanceof SickenedMobEntity || creature instanceof EntityWitherSkeleton) return false;
        return creature instanceof EntityShulker
                || creature instanceof EntityEvoker
                || creature instanceof EntityVex
                || creature instanceof EntityWitch
                || creature instanceof EntityLlama
                || creature instanceof EntityVindicator
                || creature instanceof EntityPigZombie
                || creature instanceof EntitySilverfish
                || creature instanceof EntityBlaze
                || creature instanceof EntityWolf
                || creature instanceof EntityBee;
    }
}
