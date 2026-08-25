package com.wdcftgg.witherstormmod.common.event;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.PowerfulExplosiveEntity;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.SymbiontSummoningManager;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WitherStormCombatEvents {

    private static final ResourceLocation WITHER_ROSE = new ResourceLocation("futuremc", "wither_rose");
    private static final String KILLED_BY_STORM_NBT = Tags.MOD_ID + "KilledByStorm";
    private static final Field EXPLOSION_SOURCE_FIELD = ObfuscationReflectionHelper.findField(
            Explosion.class, "field_77283_e");
    private static final Field FIRE_FIELD = ObfuscationReflectionHelper.findField(
            Entity.class, "field_190534_ay");
    private static final Map<EntityLiving, SunBurnSnapshot> SUN_BURN_SNAPSHOTS =
            new WeakHashMap<EntityLiving, SunBurnSnapshot>();

    private WitherStormCombatEvents() {
    }

    /** 在原版 TNT 每次递减引信之前复现上游的牵引光束引信改写。 */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote) return;
        if (event.phase == TickEvent.Phase.END) {
            suppressSunBurnNearEvolvedStorm(event.world);
            return;
        }
        if (event.phase != TickEvent.Phase.START) return;
        for (WitherStormEntity storm : event.world.getEntities(WitherStormEntity.class,
                entity -> !entity.isDead && !entity.isDeadOrPlayingDead())) {
            AxisAlignedBB search = storm.getEntityBoundingBox().grow(100.0D, 200.0D, 100.0D);
            for (EntityTNTPrimed tnt : event.world.getEntitiesWithinAABB(EntityTNTPrimed.class, search,
                    entity -> !(entity instanceof PowerfulExplosiveEntity.FormidibombEntity))) {
                WitherStormEntity nearest = findNearestStorm(event.world, tnt);
                if (nearest != storm) continue;
                int head = storm.findContainingTractorBeamHead(tnt, 4.0D);
                if (head < 0) continue;
                Vec3d headPosition = storm.getHeadPositionForBeam(head);
                if (tnt.getDistanceSq(headPosition.x, headPosition.y, headPosition.z) <= 144.0D) {
                    tnt.setFuse(1);
                } else if (tnt.getFuse() == 21) {
                    // 原版本次 tick 会把 21 减到 20，上游注入将这个结果改写为 80。
                    tnt.setFuse(81);
                }
            }
        }
    }

    /** 在实体自身更新开始前保存燃烧状态，避免把岩浆和火焰攻击误判成日晒。 */
    @SubscribeEvent
    public static void captureSunBurnState(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving().world.isRemote
                || !(event.getEntityLiving() instanceof EntityLiving)) return;
        EntityLiving living = (EntityLiving) event.getEntityLiving();
        if (!isSunBurnCandidate(living) || !isNearEvolvedStorm(living)) {
            SUN_BURN_SNAPSHOTS.remove(living);
            return;
        }
        SUN_BURN_SNAPSHOTS.put(living, new SunBurnSnapshot(
                getFireTicks(living), isExposedToSun(living)));
    }

    private static void suppressSunBurnNearEvolvedStorm(World world) {
        Iterator<Map.Entry<EntityLiving, SunBurnSnapshot>> iterator =
                SUN_BURN_SNAPSHOTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EntityLiving, SunBurnSnapshot> entry = iterator.next();
            EntityLiving living = entry.getKey();
            if (living == null || living.world != world) continue;
            SunBurnSnapshot snapshot = entry.getValue();
            if (!living.isDead && snapshot.fireTicks <= 0 && snapshot.exposedToSun
                    && isNearEvolvedStorm(living) && getFireTicks(living) > 0
                    && !world.isFlammableWithin(living.getEntityBoundingBox().shrink(0.001D))
                    && !living.isInLava()) {
                setFireTicks(living, snapshot.fireTicks);
            }
            iterator.remove();
        }
    }

    private static boolean isSunBurnCandidate(EntityLiving living) {
        return !living.isImmuneToFire()
                && (living instanceof EntityZombie || living instanceof AbstractSkeleton)
                && (!(living instanceof EntityZombie) || !((EntityZombie) living).isChild());
    }

    private static boolean isExposedToSun(EntityLiving living) {
        return living.world.isDaytime()
                && living.getBrightness() > 0.5F
                && living.world.canSeeSky(new BlockPos(
                living.posX, living.posY + living.getEyeHeight(), living.posZ));
    }

    private static boolean isNearEvolvedStorm(Entity entity) {
        World world = entity.world;
        // 性能优化：复用 WorldUtil 的同 tick 共享风暴索引，避免每个僵尸/骷髅
        // 各自做 200 格全加载区扫描。
        for (WitherStormEntity storm : WorldUtil.getCachedStorms(world)) {
            if (storm.isDeadOrPlayingDead() || storm.getPhase() <= 5) continue;
            if (horizontalDistanceSquared(storm, entity) <= 40000.0D) return true;
        }
        return false;
    }

    private static int getFireTicks(Entity entity) {
        try {
            return FIRE_FIELD.getInt(entity);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法读取实体燃烧计时", exception);
        }
    }

    private static void setFireTicks(Entity entity, int ticks) {
        try {
            FIRE_FIELD.setInt(entity, ticks);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法恢复实体燃烧计时", exception);
        }
    }

    private static final class SunBurnSnapshot {
        private final int fireTicks;
        private final boolean exposedToSun;

        private SunBurnSnapshot(int fireTicks, boolean exposedToSun) {
            this.fireTicks = fireTicks;
            this.exposedToSun = exposedToSun;
        }
    }

    private static double horizontalDistanceSquared(Entity first, Entity second) {
        double deltaX = first.posX - second.posX;
        double deltaZ = first.posZ - second.posZ;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    @Nullable
    private static WitherStormEntity findNearestStorm(World world, Entity entity) {
        WitherStormEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        AxisAlignedBB search = entity.getEntityBoundingBox().grow(100.0D, 200.0D, 100.0D);
        for (WitherStormEntity storm : world.getEntitiesWithinAABB(WitherStormEntity.class, search,
                candidate -> !candidate.isDead && !candidate.isDeadOrPlayingDead())) {
            double distance = storm.getDistanceSq(entity);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = storm;
            }
        }
        return nearest;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        EntityLivingBase attacker = victim.getAttackingEntity();
        if (victim.world.isRemote || !isWitherStormEntity(attacker)) return;
        createWitherRose(victim);
        if (victim instanceof EntityPlayerMP) {
            getPersistentPlayerData((EntityPlayer) victim).setUniqueId(
                    KILLED_BY_STORM_NBT, attacker.getUniqueID());
        }
    }

    private static void createWitherRose(EntityLivingBase victim) {
        World world = victim.world;
        Block roseBlock = ForgeRegistries.BLOCKS.getValue(WITHER_ROSE);
        Item roseItem = ForgeRegistries.ITEMS.getValue(WITHER_ROSE);
        boolean placed = false;
        if (roseBlock != null && roseBlock != Blocks.AIR
                && ForgeEventFactory.getMobGriefingEvent(world, victim)) {
            BlockPos position = new BlockPos(victim.posX, victim.posY, victim.posZ);
            IBlockState roseState = roseBlock.getDefaultState();
            if (world.isAirBlock(position) && roseBlock.canPlaceBlockAt(world, position)) {
                placed = world.setBlockState(position, roseState, 3);
            }
        }
        if (!placed && roseItem != null) {
            world.spawnEntity(new EntityItem(world, victim.posX, victim.posY, victim.posZ,
                    new ItemStack(roseItem)));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        SymbiontSummoningManager.makeInvulnerable(event.player, 600, "玩家重生");
        if (!(event.player instanceof EntityPlayerMP) || event.isEndConquered()) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        NBTTagCompound data = getPersistentPlayerData(player);
        if (!data.hasUniqueId(KILLED_BY_STORM_NBT)) return;
        UUID stormId = data.getUniqueId(KILLED_BY_STORM_NBT);
        data.removeTag(KILLED_BY_STORM_NBT);
        if (!WitherStormConfig.preventWitherStormCamping) return;

        WorldServer world = player.getServerWorld();
        Entity entity = world.getEntityFromUuid(stormId);
        if (!(entity instanceof EntityLivingBase) || !isWitherStormEntity(entity)) return;
        EntityLivingBase storm = (EntityLivingBase) entity;
        if (getWitherStormPhase(entity) > 3 && storm.getDistance(player) < 300.0F) {
            movePlayerAwayFromStorm(player, storm);
        }
    }

    private static NBTTagCompound getPersistentPlayerData(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    private static void movePlayerAwayFromStorm(EntityPlayerMP player, EntityLivingBase storm) {
        WorldServer world = player.getServerWorld();
        for (int attempt = 0; attempt < 10; attempt++) {
            float angle = storm.getRNG().nextFloat() * (float) (Math.PI * 2.0D);
            float distance = 300.0F + storm.getRNG().nextInt(200);
            int positionX = MathHelper.floor(storm.posX + MathHelper.cos(angle) * distance);
            int positionZ = MathHelper.floor(storm.posZ + MathHelper.sin(angle) * distance);
            BlockPos position = world.getTopSolidOrLiquidBlock(new BlockPos(positionX, 0, positionZ));
            if (!canRespawnAt(player, world, position)) continue;
            player.setPositionAndUpdate(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
            return;
        }
    }

    private static boolean canRespawnAt(EntityPlayerMP player, WorldServer world, BlockPos position) {
        if (!world.isValid(position) || !world.getWorldBorder().contains(position)
                || !world.isAirBlock(position) || !world.isAirBlock(position.up())) return false;
        IBlockState ground = world.getBlockState(position.down());
        if (!ground.getMaterial().blocksMovement()
                || ground.getBlock() == Blocks.CACTUS || ground.getBlock() == Blocks.MAGMA) return false;
        double positionX = position.getX() + 0.5D;
        double positionZ = position.getZ() + 0.5D;
        AxisAlignedBB bounds = new AxisAlignedBB(
                positionX - player.width * 0.5D, position.getY(), positionZ - player.width * 0.5D,
                positionX + player.width * 0.5D, position.getY() + player.height, positionZ + player.width * 0.5D);
        return world.getCollisionBoxes(player, bounds).isEmpty() && !world.containsAnyLiquid(bounds);
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        World world = event.getWorld();
        if (world.isRemote) return;
        Entity source = getExplosionSource(event.getExplosion());
        if (source == null || isWitherStormEntity(source)) return;
        Vec3d explosionPosition = event.getExplosion().getPosition();
        AxisAlignedBB search = source.getEntityBoundingBox().grow(100.0D);
        for (WitherStormEntity storm : world.getEntitiesWithinAABB(WitherStormEntity.class, search)) {
            for (int head = 0; head < storm.getTotalHeads(); head++) {
                if (!storm.tractorBeamActive(head)) continue;
                if (source instanceof EntityTNTPrimed
                        && !(source instanceof PowerfulExplosiveEntity.FormidibombEntity)
                        && storm.canBeDistracted(head)) {
                    int chance = Math.max(1, MathHelper.ceil(storm.getDistance(source) / 30.0F));
                    if (storm.canSee(head, source) && storm.getRNG().nextInt(chance) == 0) {
                        storm.makeDistracted(source.getPositionVector(), 120 + storm.getRNG().nextInt(60), head);
                    }
                }
                if (isProjectileFromStorm(source, storm) || storm.isDeadOrPlayingDead()
                        || storm.isHeadInjured(head)) continue;
                double radius = storm.getPhase() < 4 ? 5.0D : 12.0D;
                if (storm.getHeadPosition(head, 1.0F).squareDistanceTo(explosionPosition) < radius * radius) {
                    storm.attackHeadFromExplosion(head, source);
                }
            }
        }
        for (SupplementalEntities.WitherStormSegmentEntity segment : world.getEntitiesWithinAABB(
                SupplementalEntities.WitherStormSegmentEntity.class, search)) {
            for (int head = 0; head < segment.getTotalHeads(); head++) {
                if (!segment.tractorBeamActive(head)) continue;
                if (source instanceof EntityTNTPrimed
                        && !(source instanceof PowerfulExplosiveEntity.FormidibombEntity)
                        && segment.canBeDistracted(head)) {
                    int chance = Math.max(1, MathHelper.ceil(segment.getDistance(source) / 30.0F));
                    if (segment.canSee(head, source) && segment.getRNG().nextInt(chance) == 0) {
                        segment.makeDistracted(source.getPositionVector(),
                                120 + segment.getRNG().nextInt(60), head);
                    }
                }
                if (isProjectileFromStorm(source, segment) || segment.isDeadOrPlayingDead()
                        || segment.isHeadInjured(head)) continue;
                double radius = segment.getPhase() < 4 ? 5.0D : 12.0D;
                if (segment.getSegmentHeadPosition(head).squareDistanceTo(explosionPosition)
                        < radius * radius) {
                    segment.attackHeadFromExplosion(head, source);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (isWitherStormEntity(event.getEntityMounting())) event.setCanceled(true);
        if (event.isDismounting() && WitherStormConfig.playerCannotDismountTentacles
                && event.getEntityMounting() instanceof EntityPlayer
                && event.getEntityBeingMounted() instanceof SickenedEntities.TentacleEntity) {
            event.setCanceled(true);
        }
    }

    /** 紫颂果开始食用时，按上游逐头概率让附近主体的活动牵引头转向玩家。 */
    @SubscribeEvent
    public static void onEntityUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntityLiving().world.isRemote || event.getItem().getItem() != Items.CHORUS_FRUIT) return;
        EntityLivingBase user = event.getEntityLiving();
        AxisAlignedBB search = user.getEntityBoundingBox().grow(128.0D, 256.0D, 128.0D);
        for (WitherStormEntity storm : user.world.getEntitiesWithinAABB(WitherStormEntity.class, search)) {
            if (storm.isDeadOrPlayingDead()) continue;
            for (int head = 0; head < storm.getTotalHeads(); head++) {
                if (!storm.tractorBeamActive(head) || !storm.canBeDistracted(head)
                        || storm.getRNG().nextInt(3) != 0) continue;
                storm.makeDistracted(user.getPositionVector(), 80, head);
            }
        }
        // 上游分裂体继承 WitherStormEntity；1.12 移植为独立实体，需显式覆盖同一继承语义。
        for (SupplementalEntities.WitherStormSegmentEntity segment : user.world.getEntitiesWithinAABB(
                SupplementalEntities.WitherStormSegmentEntity.class, search)) {
            if (segment.isDeadOrPlayingDead()) continue;
            for (int head = 0; head < segment.getTotalHeads(); head++) {
                if (!segment.tractorBeamActive(head) || !segment.canBeDistracted(head)
                        || segment.getRNG().nextInt(3) != 0) continue;
                segment.makeDistracted(user.getPositionVector(), 80, head);
            }
        }
    }

    @Nullable
    private static Entity getExplosionSource(Explosion explosion) {
        try {
            return (Entity) EXPLOSION_SOURCE_FIELD.get(explosion);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法读取爆炸源实体", exception);
        }
    }

    private static boolean isProjectileFromStorm(Entity source, Entity storm) {
        return source instanceof EntityFireball && ((EntityFireball) source).shootingEntity == storm;
    }

    private static boolean isWitherStormEntity(@Nullable Entity entity) {
        return entity instanceof WitherStormEntity
                || entity instanceof SupplementalEntities.WitherStormSegmentEntity;
    }

    private static int getWitherStormPhase(Entity entity) {
        if (entity instanceof WitherStormEntity) return ((WitherStormEntity) entity).getPhase();
        return entity instanceof SupplementalEntities.WitherStormSegmentEntity
                ? ((SupplementalEntities.WitherStormSegmentEntity) entity).getPhase() : 0;
    }
}
