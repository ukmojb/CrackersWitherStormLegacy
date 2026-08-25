package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.common.init.ModDamageSources;
import com.wdcftgg.witherstormmod.api.common.event.CanWitherStormTargetMobEvent;
import com.wdcftgg.witherstormmod.common.advancement.ModCriteriaTriggers;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessCapability;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessTracker;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.resource.UpstreamEntityTags;
import com.wdcftgg.witherstormmod.common.resource.UpstreamItemTags;
import com.wdcftgg.witherstormmod.common.util.TractorBeamHelper;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** 还原分段从主风暴继承到的独立三头选敌、牵引与吸收状态。 */
final class WitherStormSegmentManager {
    private static final float MAXIMUM_HEAD_YAW = 80.0F;
    private static final int ENTITY_DISTRACTION_UNSEEN_LIMIT = 180;
    private static final double ENTITY_CONSUMPTION_RADIUS = 64.0D;
    private static final double[][] HEAD_OFFSETS = {
            {0.0D, 9.0D, 14.0D},
            {6.0D, 8.0D, 12.0D},
            {-6.0D, 8.0D, 12.0D}
    };

    private final SupplementalEntities.WitherStormSegmentEntity segment;
    private final HeadState[] heads = {new HeadState(), new HeadState(), new HeadState()};
    private final Map<UUID, Entity> trackedEntities = new LinkedHashMap<UUID, Entity>();
    private final List<UUID> savedTrackedEntities = new ArrayList<UUID>();
    private final IgnoredTargetsManager ignoredTargets;
    /** 性能优化：头部视线结果只在同 tick 复用，保持上游每 tick 检测。 */
    private long pullSightCacheCycle = Long.MIN_VALUE;
    private final Map<String, Boolean> pullSightCache = new LinkedHashMap<String, Boolean>();
    /** 性能优化：牵引候选实体每 tick 重建（检测机制与上游一致）。 */
    private final List<Entity> pullableCandidates = new ArrayList<Entity>();
    private final WitherStormPulling.Source trackedEntityPullSource = new WitherStormPulling.Source() {
        @Override public WitherStormEntity getStorm() { return segment.getOwnerStorm(); }
        @Override public boolean usesRegisteredPullBehaviors() { return false; }
        @Override public int getPhase() {
            return segment.getPhase();
        }
        @Override public float getWidth() { return segment.width; }
        @Override public Vec3d getEyePosition() { return segment.getPositionEyes(1.0F); }
        @Override public BlockPos getBlockPosition() { return segment.getPosition(); }
        @Override public boolean isTractorBeamActive(int head) {
            return WitherStormSegmentManager.this.isTractorBeamActive(head);
        }
        @Override public Vec3d getHeadPosition(int head) {
            return WitherStormSegmentManager.this.getHeadPosition(head, 1.0F);
        }
        @Override public Vec3d getHeadDirection(int head) {
            return WitherStormSegmentManager.getHeadDirection(
                    heads[MathHelper.clamp(head, 0, 2)]);
        }
        @Override public double getTractorBeamCutoff(int head) {
            return heads[MathHelper.clamp(head, 0, 2)].beamCutoff;
        }
    };
    private List<SupplementalEntities.WitherStormSegmentEntity> familySegments = Collections.emptyList();
    private int trackedEntityTicks;
    private int idleTargetTicks;
    private int nextUndergroundRumble;

    WitherStormSegmentManager(SupplementalEntities.WitherStormSegmentEntity segment) {
        this.segment = segment;
        ignoredTargets = new IgnoredTargetsManager(segment, this::getSearchBox);
        nextUndergroundRumble = 1200 + segment.getRNG().nextInt(1200);
        for (int head = 0; head < heads.length; head++) {
            heads[head].requiredHits = 1 + segment.getRNG().nextInt(2);
        }
    }

    void tick() {
        beginHeadTick();
        if (segment.world.isRemote) {
            readSyncedHeadRotations();
            return;
        }
        resolveSavedTrackedEntities();
        ignoredTargets.tick();
        if (segment.isDead) {
            clearTargets();
            return;
        }
        if (segment.isInDeathSequence()) {
            updateDeathHeadRotations();
            return;
        }
        WitherStormEntity owner = segment.getOwnerStorm();
        if (owner == null || owner.isDead) {
            clearTargets();
            return;
        }
        if (owner.isDeadOrPlayingDead()) {
            releaseTrackedEntities();
            clearTargets();
            updatePlayDeadHeadRotations();
            return;
        }
        familySegments = findFamilySegments();
        tickDistractions(owner);
        updateTargets(owner);
        updateHeadRotations(owner);
        tickHeadState(owner);
        tickHeadClusterPickups(owner);
        tickDefaultClusterSource(owner);
        WitherStormClusterManager.createCollisionClusters(segment, this::trackEntity);
        convertFallingBlocks();
        applyMassAbsorption(owner);
        tickHeads(owner);
        tickProjectilesHittingHeads(owner);
        tickTrackedEntities(owner);
        tickMainTargetTimeout();
        healWhileCompletelyInvulnerable();
        updateCaveRumbles();
    }

    private void healWhileCompletelyInvulnerable() {
        if (WitherStormConfig.witherStormInvulnerability && segment.ticksExisted % 20 == 0) {
            segment.heal(10.0F);
        }
    }

    private void updateCaveRumbles() {
        if (!WitherStormConfig.caveRumbles || nextUndergroundRumble <= 0) return;
        --nextUndergroundRumble;
        if (nextUndergroundRumble > 0) return;
        if (segment.world instanceof WorldServer) {
            for (EntityPlayerMP player : segment.world.getEntitiesWithinAABB(
                    EntityPlayerMP.class, getSearchBox().grow(50.0D))) {
                if (!canSeeOrIsInOpenArea(player)) {
                    CaveRumbleManager.trigger((WorldServer) segment.world, player,
                            WitherStormConfig.caveRumbleIntensity, segment.getRNG());
                }
            }
        }
        if (WitherStormConfig.chanceForExtendedRumbles && segment.getRNG().nextInt(3) != 0) {
            nextUndergroundRumble = 100 + segment.getRNG().nextInt(60);
        } else {
            int minimum = Math.max(1, WitherStormConfig.caveRumbleIntervalMin * 20);
            int maximum = Math.max(minimum, WitherStormConfig.caveRumbleIntervalMax * 20);
            nextUndergroundRumble = minimum + (maximum > minimum
                    ? segment.getRNG().nextInt(maximum - minimum) : 0);
        }
    }

    private void beginHeadTick() {
        for (int head = 0; head < heads.length; head++) {
            HeadState state = heads[head];
            if (segment.world.isRemote && state.hurtOverlayTicks > 0) state.hurtOverlayTicks--;
            state.positionO = state.position;
            state.position = calculateHeadPosition(head);
            state.yawO = state.yaw;
            state.pitchO = state.pitch;
            state.mouthO = state.mouth;
            state.brokenJawO = state.brokenJaw;
            state.shakeO = state.shake;
            state.mouth = WitherStormPartLogic.advanceMouth(state.mouth,
                    segment.isHeadFlagSet(roarBit(head)), segment.isHeadFlagSet(biteBit(head)));
            if (segment.onGround && segment.isInDeathSequence()) {
                state.brokenJaw = Math.min(1.5F,
                        state.brokenJaw + (1.0F - state.brokenJaw) * 0.2F + 0.05F);
            } else {
                state.brokenJaw = Math.max(0.0F,
                        state.brokenJaw - state.brokenJaw * 0.2F - 0.05F);
            }
            if (segment.isHeadFlagSet(shakeBit(head))) {
                state.shake = WitherStormPartLogic.advanceShake(state.shake, true, segment.getRNG());
                if (state.shakeO >= 2.0F) {
                    state.shakeO = state.shake = 0.0F;
                    if (!segment.world.isRemote) {
                        segment.setHeadFlag(shakeBit(head), false);
                        state.nextShake = 20 + segment.getRNG().nextInt(20);
                    }
                }
            } else if (state.shake != 0.0F) {
                state.shakeO = state.shake = 0.0F;
            }
        }
    }

    private void readSyncedHeadRotations() {
        for (int head = 0; head < heads.length; head++) {
            float maximum = segment.getPhase() > 3 ? 5.0F : 8.0F;
            heads[head].yaw = smoothSyncedRotation(heads[head].yaw,
                    segment.getSyncedHeadYaw(head), maximum);
            heads[head].pitch = smoothSyncedRotation(heads[head].pitch,
                    segment.getSyncedHeadPitch(head), maximum);
        }
    }

    private void clearTargets() {
        for (int head = 0; head < heads.length; head++) {
            setTarget(head, null);
        }
        idleTargetTicks = 0;
    }

    private void tickMainTargetTimeout() {
        if (heads[0].target != null) ++idleTargetTicks;
        if (idleTargetTicks > 1800 || heads[0].target == null) {
            setTarget(0, null);
            idleTargetTicks = 0;
        }
    }

    private void tickDefaultClusterSource(WitherStormEntity owner) {
        int interval = WitherStormConfig.devourerClusterPickupInterval;
        if (!ForgeEventFactory.getMobGriefingEvent(segment.world, segment)
                || segment.ticksExisted % interval != 0) return;
        SupplementalEntities.BlockClusterEntity cluster = owner.createDefaultClusterForSegment(segment);
        if (cluster != null) trackEntity(cluster);
    }

    private void updateTargets(WitherStormEntity owner) {
        for (int head = 0; head < heads.length; head++) {
            HeadState state = heads[head];
            if (!isHeadEnabled(owner, head) || isHeadInjured(head) || state.isDistracted()) {
                setTarget(head, null);
                continue;
            }

            EntityLivingBase revengeTarget = head == 0 ? segment.getRevengeTarget() : null;
            if (revengeTarget != null && revengeTarget != state.target
                    && isRevengeTargetApplicable(owner, revengeTarget)) {
                countWitherSicknessContact(revengeTarget);
                setTarget(head, revengeTarget);
                continue;
            }
            if (canContinueTarget(owner, head, state)) {
                setTarget(head, state.target);
                continue;
            }
            setTarget(head, findTarget(owner, head));
        }
        logPlayerTargetingDiagnostics(owner);
    }

    @Nullable
    private EntityLivingBase findTarget(WitherStormEntity owner, int head) {
        double range = segment.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        AxisAlignedBB search = segment.getEntityBoundingBox().grow(range, range + 50.0D, range);
        EntityLivingBase nearest = null;
        EntityLivingBase nearestSpecialTarget = null;
        double nearestDistance = Double.MAX_VALUE;
        double nearestSpecialTargetDistance = Double.MAX_VALUE;
        boolean preferSpecialTarget = WitherStormConfig.specialTargetingBias
                && segment.getPlayingJukeboxes().isEmpty()
                && segment.getRNG().nextInt(100) <= MathHelper.clamp(
                WitherStormConfig.specialTargetingBiasChance, 0, 100);
        for (EntityLivingBase candidate : segment.world.getEntitiesWithinAABB(EntityLivingBase.class, search)) {
            if (!isTargetApplicable(owner, candidate, head)) continue;
            double distance = candidate.getDistanceSq(
                    segment.posX, segment.posY + segment.getEyeHeight(), segment.posZ);
            if (preferSpecialTarget
                    && (UpstreamEntityTags.contains(UpstreamEntityTags.FAVOURABLE_MOBS, candidate)
                    || candidate instanceof EntityPlayer)
                    && distance < nearestSpecialTargetDistance) {
                nearestSpecialTarget = candidate;
                nearestSpecialTargetDistance = distance;
            }
            if (candidate.getEntityBoundingBox().getAverageEdgeLength() <= 0.5D
                    && candidate.getRNG().nextInt(4) != 0) continue;
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        EntityLivingBase selected = nearestSpecialTarget == null ? nearest : nearestSpecialTarget;
        if (StormDiagnosticLogger.isEnabled() && (selected != null || segment.ticksExisted % 20 == 0)) {
            StormDiagnosticLogger.info(
                    "[风暴诊断][分体头选敌结果] 分体={} 所属风暴={} 阶段={} tick={} 头={} 启用特殊偏置={} 最近普通={} 最近特殊={} 最终选择={}",
                    segment.getUniqueID(), segment.getOwnerUuid(), owner.getPhase(),
                    segment.ticksExisted, head, preferSpecialTarget, describeEntity(nearest),
                    describeEntity(nearestSpecialTarget), describeEntity(selected));
        }
        if (selected != null) countWitherSicknessContact(selected);
        return selected;
    }

    private boolean isTargetApplicable(WitherStormEntity owner, @Nullable EntityLivingBase entity,
                                       int head) {
        if (entity == null || entity == segment || entity == owner || !entity.isEntityAlive()
                || entity.world != segment.world || entity.dimension != segment.dimension
                || segment.isOnSameTeam(entity)
                || !owner.isValidStormTarget(entity)
                || ignoredTargets.shouldIgnoreTarget(entity)
                || isTracking(entity)
                || isPassengerTarget(entity)
                || isInsideOtherTractorBeam(entity, head)
                || !canSeeWithCache(head, entity)
                || segment.isEntityBehindBack(entity)
                || isTargetInUseByStormFamily(entity, head, head == 0)) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player.capabilities.disableDamage || player.isSpectator() || owner.hasRecentlyBeenRevived()
                    || player.isHandActive() && player.getActiveItemStack().getItem() == Items.SHIELD) return false;
        }
        boolean cancelled = MinecraftForge.EVENT_BUS.post(
                new CanWitherStormTargetMobEvent(owner, segment, entity));
        if (cancelled && entity instanceof EntityPlayer && segment.ticksExisted % 20 == 0) {
            StormDiagnosticLogger.info(
                    "[风暴诊断][分体玩家目标事件拒绝] 分体={} 所属风暴={} 阶段={} tick={} 玩家={} 玩家UUID={} 头={}",
                    segment.getUniqueID(), segment.getOwnerUuid(), owner.getPhase(),
                    segment.ticksExisted, entity.getName(), entity.getUniqueID(), head);
        }
        return !cancelled;
    }

    /** 每秒记录一次玩家在该分体三个头中的新目标判定结果。 */
    private void logPlayerTargetingDiagnostics(WitherStormEntity owner) {
        if (!StormDiagnosticLogger.isEnabled() || segment.ticksExisted % 20 != 0) return;
        double range = segment.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        AxisAlignedBB search = segment.getEntityBoundingBox().grow(range, range + 50.0D, range);
        for (EntityPlayer player : segment.world.playerEntities) {
            boolean inSearch = search.intersects(player.getEntityBoundingBox());
            long protection = SymbiontSummoningManager.getIgnoreTicksRemaining(player);
            for (int head = 0; head < heads.length; head++) {
                HeadState state = heads[head];
                String result = describePlayerTargetRejection(owner, head, player,
                        inSearch, protection);
                StormDiagnosticLogger.info(
                        "[风暴诊断][分体头玩家判定] 分体={} 所属风暴={} 阶段={} tick={} 玩家={} 玩家UUID={} 维度={} 距离平方={} 搜索范围内={} 头={} 结果={} 当前目标={} 光束启用={} 受伤剩余={} 分心剩余={} 保护剩余={} yaw={} pitch={} cutoff={}",
                        segment.getUniqueID(), segment.getOwnerUuid(), owner.getPhase(),
                        segment.ticksExisted, player.getName(), player.getUniqueID(),
                        player.dimension, segment.getDistanceSq(player), inSearch, head,
                        result, describeEntity(state.target), isHeadBeamActive(owner, head),
                        state.injuryTicks, state.distractionTicks, protection,
                        state.yaw, state.pitch, state.beamCutoff);
            }
        }
    }

    private String describePlayerTargetRejection(WitherStormEntity owner, int head,
                                                  EntityPlayer player, boolean inSearch,
                                                  long protection) {
        HeadState state = heads[head];
        if (!isHeadEnabled(owner, head)) return "头部未启用";
        if (isHeadInjured(head)) return "头部受伤";
        if (state.isDistracted()) return "头部正在分心";
        if (!inSearch) return "目标搜索范围外";
        if (!player.isEntityAlive()) return "玩家已死亡";
        if (player.world != segment.world || player.dimension != segment.dimension) return "世界或维度不同";
        if (player.capabilities.disableDamage) return "玩家处于无敌模式";
        if (player.isSpectator()) return "玩家处于旁观模式";
        if (owner.hasRecentlyBeenRevived()) return "风暴刚复活，暂不选玩家";
        if (protection > 0L) return "玩家目标保护剩余" + protection + "tick";
        if (segment.isOnSameTeam(player)) return "玩家与分体同队";
        if (ignoredTargets.shouldIgnoreTarget(player)) return "分体忽略目标管理器拒绝";
        if (isTracking(player)) return "玩家已进入分体吞噬追踪";
        if (isPassengerTarget(player)) return "玩家是风暴家族附近乘客目标";
        if (isInsideOtherTractorBeam(player, head)) return "玩家已在其他光束内";
        if (!canSeeWithCache(head, player)) return "该头没有视线";
        if (segment.isEntityBehindBack(player)) return "玩家位于分体背后";
        if (isTargetInUseByStormFamily(player, head, head == 0)) return "玩家已被风暴家族其他头占用";
        if (player.isHandActive() && player.getActiveItemStack().getItem() == Items.SHIELD) return "玩家正在举盾";
        return "基础条件通过（事件总线仍可取消）";
    }

    private static String describeEntity(@Nullable Entity entity) {
        return entity == null ? "无" : entity.getName() + "#" + entity.getEntityId()
                + "/" + entity.getUniqueID();
    }

    private boolean canContinueTarget(WitherStormEntity owner, int head, HeadState state) {
        EntityLivingBase target = state.target;
        if (target == null) return false;
        if (!target.isEntityAlive()) return rejectContinuedTarget(owner, head, target, "目标死亡");
        if (target.world != segment.world || target.dimension != segment.dimension) {
            return rejectContinuedTarget(owner, head, target, "世界或维度不同");
        }
        if (segment.isOnSameTeam(target)) {
            return rejectContinuedTarget(owner, head, target, "目标变为同队");
        }
        double followDistance = segment.getEntityAttribute(
                SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue() + 100.0D;
        if (segment.getDistanceSq(target) > followDistance * followDistance) {
            return rejectContinuedTarget(owner, head, target, "超出持续目标距离");
        }
        if (canSeeWithCache(head, target)) {
            state.targetUnseenTicks = 0;
        } else if (++state.targetUnseenTicks > (owner.getPhase() < 4 ? 80 : 20)) {
            return rejectContinuedTarget(owner, head, target,
                    "失去视线超过容忍时间，未见tick=" + state.targetUnseenTicks);
        }
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            if (player.capabilities.disableDamage || player.isSpectator()) {
                return rejectContinuedTarget(owner, head, target, "玩家切换为无敌或旁观模式");
            }
        }
        if (owner.getPhase() > 3 && segment.isEntityBehindBack(target)) {
            return rejectContinuedTarget(owner, head, target, "阶段4+目标进入分体背后");
        }
        Vec3d position = target.getPositionVector();
        if (state.lastTargetPosition != null
                && position.distanceTo(state.lastTargetPosition) > 20.0D) {
            return rejectContinuedTarget(owner, head, target, "单tick位移超过20格");
        }
        if (isTracking(target)) {
            return rejectContinuedTarget(owner, head, target, "目标进入分体吞噬追踪");
        }
        state.lastTargetPosition = position;
        return true;
    }

    private boolean rejectContinuedTarget(WitherStormEntity owner, int head,
                                          EntityLivingBase target, String reason) {
        StormDiagnosticLogger.info(
                "[风暴诊断][分体头持续目标拒绝] 分体={} 所属风暴={} 阶段={} tick={} 头={} 目标={} 原因={}",
                segment.getUniqueID(), segment.getOwnerUuid(), owner.getPhase(),
                segment.ticksExisted, head, describeEntity(target), reason);
        return false;
    }

    private boolean isRevengeTargetApplicable(WitherStormEntity owner, EntityLivingBase entity) {
        double followDistance = segment.getEntityAttribute(
                SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue() + 100.0D;
        return owner.isValidStormTarget(entity)
                && !segment.isOnSameTeam(entity)
                && segment.getDistanceSq(entity) <= followDistance * followDistance
                && canSeeWithCache(0, entity);
    }

    public void setTarget(int head, @Nullable EntityLivingBase target) {
        int index = MathHelper.clamp(head, 0, heads.length - 1);
        HeadState state = heads[index];
        if (state.target != target) {
            StormDiagnosticLogger.info(
                    "[风暴诊断][分体头目标切换] 分体={} 所属风暴={} 阶段={} tick={} 头={} 原目标={} 新目标={} 受伤剩余={} 分心剩余={}",
                    segment.getUniqueID(), segment.getOwnerUuid(), segment.getPhase(),
                    segment.ticksExisted, index, describeEntity(state.target),
                    describeEntity(target), state.injuryTicks, state.distractionTicks);
            state.target = target;
            state.targetUnseenTicks = 0;
            state.lastTargetPosition = null;
        }
        if (index == 0 && segment.getAttackTarget() != target) segment.setAttackTarget(target);
        segment.updateWatchedTargetId(index, target == null ? 0 : target.getEntityId());
    }

    private static void countWitherSicknessContact(EntityLivingBase target) {
        WitherSicknessTracker tracker = WitherSicknessCapability.get(target);
        if (tracker != null) tracker.countContact();
    }

    private boolean isPassengerTarget(Entity entity) {
        AxisAlignedBB nearby = segment.getEntityBoundingBox().grow(10.0D, 255.0D, 10.0D);
        for (EntityLivingBase other : segment.world.getEntitiesWithinAABB(EntityLivingBase.class, nearby)) {
            if (other != entity && other.isRidingSameEntity(entity)) return true;
        }
        return false;
    }

    private boolean isTargetInUseByStormFamily(Entity entity, int requesterHead, boolean allowCurrentTarget) {
        WitherStormEntity owner = segment.getOwnerStorm();
        if (owner != null && owner.isTargetedByMainHeadFamily(entity)) return true;
        for (SupplementalEntities.WitherStormSegmentEntity other : getFamilySegments()) {
            for (int head = 0; head < 3; head++) {
                if (other == segment && head == requesterHead && allowCurrentTarget) continue;
                if (other.getSegmentTarget(head) == entity) return true;
            }
        }
        return false;
    }

    boolean isUltimateTargetInUseForBodyRotation(@Nullable EntityLivingBase ultimateTarget) {
        if (ultimateTarget == null) return false;
        WitherStormEntity owner = segment.getOwnerStorm();
        if (owner != null && owner.isTargetedByMainHeadFamily(ultimateTarget)) return true;
        for (SupplementalEntities.WitherStormSegmentEntity other : getFamilySegments()) {
            int firstHead = other == segment ? 1 : 0;
            for (int head = firstHead; head < 3; head++) {
                if (other.getSegmentTarget(head) == ultimateTarget) return true;
            }
        }
        return false;
    }

    private void tickDistractions(WitherStormEntity owner) {
        for (int head = 0; head < heads.length; head++) {
            HeadState state = heads[head];
            if (state.distractionTicks > 0) {
                tickActiveDistraction(owner, head, state);
                continue;
            }
            clearDistraction(head, state, "开始新一轮分心检查前清理旧状态");
            if (state.nextDistractionCheck > 0) --state.nextDistractionCheck;
            if (!canStartEntityDistraction(owner, head)) continue;

            if (segment.getRNG().nextInt(2) == 0) {
                EntityFireworkRocket firework = findFirework(head);
                if (firework != null) {
                    startEntityDistraction(head, state, firework);
                    continue;
                }
            }

            if (state.nextDistractionCheck > 0 || !canStartBlockDistraction(owner, head)) continue;
            BlockPos block = findDistractionBlock(head);
            if (block != null) {
                Vec3d position = new Vec3d(block).add(0.5D, 0.5D, 0.5D);
                if (segment.isPositionBehindBack(position)) continue;
                boolean overlapsOtherHead = false;
                for (int otherHead = 0; otherHead < heads.length; otherHead++) {
                    Vec3d otherPosition = heads[otherHead].distractionPosition;
                    if (otherHead != head && otherPosition != null
                            && otherPosition.squareDistanceTo(position) < 100.0D
                            && segment.getRNG().nextInt(5) != 0) {
                        overlapsOtherHead = true;
                        break;
                    }
                }
                if (overlapsOtherHead) {
                    state.nextDistractionCheck = 60;
                    continue;
                }
                state.distractionPosition = position;
                state.distractionTicks = 120 + segment.getRNG().nextInt(60);
                StormDiagnosticLogger.info(
                        "[风暴诊断][分体头分心开始] 分体={} 所属风暴={} 阶段={} tick={} 头={} 类型=方块 位置={} 时长={} 原目标={}",
                        segment.getUniqueID(), segment.getOwnerUuid(), segment.getPhase(),
                        segment.ticksExisted, head, position, state.distractionTicks,
                        describeEntity(state.target));
            }
        }
    }

    private void tickActiveDistraction(WitherStormEntity owner, int head, HeadState state) {
        --state.distractionTicks;
        if (state.distractionEntity == null) {
            if (state.distractionTicks == 0) clearDistraction(head, state, "方块分心计时结束");
            return;
        }

        Entity distraction = resolve(state.distractionEntity);
        if (distraction == null || distraction.isDead) {
            if (state.distractionPosition != null && segment.getRNG().nextInt(8) == 0) {
                state.distractionPosition = state.distractionPosition.add(segment.getRNG().nextGaussian(),
                        segment.getRNG().nextGaussian(), segment.getRNG().nextGaussian());
            }
            EntityFireworkRocket replacement = canStartEntityDistraction(owner, head) ? findFirework(head) : null;
            if (replacement != null && !replacement.getUniqueID().equals(state.distractionEntity)) {
                startEntityDistraction(head, state, replacement);
            } else if (state.distractionTicks == 0) {
                clearDistraction(head, state, "实体分心计时结束且没有替代实体");
            }
            return;
        }

        if (state.distractionTicks > 0) return;
        double followDistance = segment.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        if (segment.getDistanceSq(distraction) > followDistance * followDistance) {
            clearDistraction(head, state, "分心实体超出跟随范围");
            return;
        }
        if (canSeeWithCache(head, distraction)) {
            state.distractionUnseenTicks = 0;
        } else if (state.distractionUnseenTicks++ > ENTITY_DISTRACTION_UNSEEN_LIMIT) {
            clearDistraction(head, state, "分心实体失去视线过久");
            return;
        }
        state.distractionPosition = distraction.getPositionVector().add(0.0D, 10.0D, 0.0D);
        state.distractionTicks = 80 + segment.getRNG().nextInt(80);
    }

    private void startEntityDistraction(int head, HeadState state, EntityFireworkRocket firework) {
        state.distractionEntity = firework.getUniqueID();
        state.distractionPosition = firework.getPositionVector().add(0.0D, 10.0D, 0.0D);
        state.distractionTicks = 80 + segment.getRNG().nextInt(80);
        state.distractionUnseenTicks = 0;
        StormDiagnosticLogger.info(
                "[风暴诊断][分体头分心开始] 分体={} 所属风暴={} 阶段={} tick={} 头={} 类型=实体 实体={} 位置={} 时长={} 原目标={}",
                segment.getUniqueID(), segment.getOwnerUuid(), segment.getPhase(),
                segment.ticksExisted, head, describeEntity(firework),
                state.distractionPosition, state.distractionTicks, describeEntity(state.target));
    }

    private void clearDistraction(int head, HeadState state, String reason) {
        boolean wasDistracted = state.isDistracted() || state.distractionEntity != null
                || state.distractionPosition != null;
        UUID oldEntity = state.distractionEntity;
        Vec3d oldPosition = state.distractionPosition;
        state.clearDistraction();
        if (wasDistracted) {
            StormDiagnosticLogger.info(
                    "[风暴诊断][分体头分心结束] 分体={} 所属风暴={} 阶段={} tick={} 头={} 原因={} 实体UUID={} 位置={}",
                    segment.getUniqueID(), segment.getOwnerUuid(), segment.getPhase(),
                    segment.ticksExisted, head, reason, oldEntity, oldPosition);
        }
    }

    private boolean canStartEntityDistraction(WitherStormEntity owner, int head) {
        return isHeadBeamActive(owner, head) && !heads[head].isDistracted();
    }

    private boolean canStartBlockDistraction(WitherStormEntity owner, int head) {
        if (!canStartEntityDistraction(owner, head)) return false;
        HeadState state = heads[head];
        EntityLivingBase target = state.target;
        return target == null || !TractorBeamHelper.isInsideTractorBeam(target.getPositionVector(),
                state.position, getHeadDirection(state), state.beamCutoff, 4.0D);
    }

    @Nullable
    private EntityFireworkRocket findFirework(int head) {
        HeadState state = heads[head];
        double range = segment.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        AxisAlignedBB search = segment.getEntityBoundingBox().grow(range, range + 255.0D, range);
        EntityFireworkRocket nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (EntityFireworkRocket firework : segment.world.getEntitiesWithinAABB(
                EntityFireworkRocket.class, search)) {
            if (firework.isDead || !canSeeWithCache(head, firework) || segment.isEntityBehindBack(firework)
                    || segment.getDistanceSq(firework) > range * range) continue;
            double distance = firework.getDistanceSq(
                    state.position.x, state.position.y, state.position.z);
            if (distance < nearestDistance) {
                nearest = firework;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    @Nullable
    private BlockPos findDistractionBlock(int head) {
        if (!ForgeEventFactory.getMobGriefingEvent(segment.world, segment)) return null;
        HeadState state = heads[head];
        Vec3d end = state.position.add(getHeadDirection(state).scale(200.0D));
        RayTraceResult hit = segment.world.rayTraceBlocks(state.position, end, false, true, false);
        BlockPos beamEnd = hit == null || hit.typeOfHit == RayTraceResult.Type.MISS
                ? new BlockPos(end) : hit.getBlockPos();
        BlockPos origin = beamEnd.add(segment.getRNG().nextInt(9) - 4,
                segment.getRNG().nextInt(9) - 4, segment.getRNG().nextInt(9) - 4);
        if (!segment.world.isBlockLoaded(origin)) return null;
        int searchRadius = Math.max(4, WitherStormConfig.tractorBeamBlockSearchRadius);
        return WorldUtil.findLoadedBlockSpiralOutwards(segment.world, origin, searchRadius,
                blockState -> UpstreamBlockTags.contains(
                        UpstreamBlockTags.TRACTOR_BEAM_DISTRACTION_BLOCKS, blockState));
    }

    private void updateHeadRotations(WitherStormEntity owner) {
        float bodyYaw = segment.getSegmentBodyYaw();
        for (int head = 0; head < heads.length; head++) {
            HeadState state = heads[head];
            state.lerpPitchSteps = 0;
            state.lerpYawSteps = 0;
            EntityLivingBase target = state.target;
            Vec3d targetPosition = state.isDistracted() ? state.distractionPosition
                    : target != null && target.isEntityAlive()
                    ? new Vec3d(target.posX, target.posY + target.getEyeHeight(), target.posZ) : null;
            if (targetPosition != null) {
                // 上游分裂体继承主风暴的 LookAtTargetGoal：phase>3 用 50 步、phase<=3 用 3 步。
                lookAtPosition(owner, head, state, targetPosition,
                        state.isDistracted() ? 10 : owner.getPhase() > 3 ? 50 : 3);
            } else {
                // 上游 WitherStormLookRandomlyGoal：受伤或 phase<4 用 3 步，否则 50 步。
                lookAtPosition(owner, head, state, getRandomLookPosition(owner, state, bodyYaw),
                        owner.getPhase() >= 4 && state.injuryTicks <= 0 ? 50 : 3);
            }
            constrainHeadYaw(state, bodyYaw);
            segment.updateHeadRotation(head, state.yaw, state.pitch);
            updateBeamCutoff(state);
        }
    }

    private void lookAtPosition(WitherStormEntity owner, int head, HeadState state,
                                Vec3d position, int additionalHeadSteps) {
        double deltaX = position.x - state.position.x;
        double deltaY = position.y - state.position.y;
        double deltaZ = position.z - state.position.z;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float wantedYaw = (float) (MathHelper.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - 90.0F;
        float wantedPitch = (float) (-(MathHelper.atan2(deltaY, horizontal) * 180.0D / Math.PI));
        if (head == 0) {
            // 上游主头 LookControl.setLookAt 用 getHeadRotSpeed()=10°/tick 转 yaw，
            // WitherStormLookController.tick 对 pitch 复用同一 f_24938_(=10)，与阶段无关。
            float maximum = 10.0F;
            state.yaw = rotateTowards(state.yaw, wantedYaw, maximum);
            state.pitch = rotateTowards(state.pitch, wantedPitch, maximum);
            return;
        }
        int steps = Math.max(1, additionalHeadSteps);
        state.yaw += MathHelper.wrapDegrees(wantedYaw - state.yaw) / steps;
        state.pitch += MathHelper.wrapDegrees(wantedPitch - state.pitch) / steps;
    }

    private static void constrainHeadYaw(HeadState state, float bodyYaw) {
        float relativeYaw = MathHelper.wrapDegrees(state.yaw - bodyYaw);
        state.yaw = bodyYaw + MathHelper.clamp(relativeYaw,
                -MAXIMUM_HEAD_YAW, MAXIMUM_HEAD_YAW);
    }

    private Vec3d getRandomLookPosition(WitherStormEntity owner, HeadState state, float bodyYaw) {
        if (!state.randomLookInitialized || state.randomLookTicks < 0) {
            float pitch = MathHelper.clamp(-segment.getRNG().nextInt(180), -140, -30)
                    * 0.017453292F;
            float yaw = (MathHelper.wrapDegrees(bodyYaw) + 90.0F
                    + MathHelper.clamp(segment.getRNG().nextInt(360) - 180, -80, 80))
                    * 0.017453292F;
            state.randomLookX = Math.cos(yaw) * 30.0D;
            state.randomLookY = Math.sin(pitch) * 30.0D;
            state.randomLookZ = Math.sin(yaw) * 30.0D;
            state.randomLookTicks = (owner.getPhase() < 4 || state.injuryTicks > 0 ? 20 : 120)
                    + segment.getRNG().nextInt(20);
            state.randomLookInitialized = true;
        }
        --state.randomLookTicks;
        return state.position.add(state.randomLookX, state.randomLookY, state.randomLookZ);
    }

    private void updateDeathHeadRotations() {
        clearTargets();
        for (int head = 0; head < heads.length; head++) {
            HeadState state = heads[head];
            if (segment.onGround) {
                state.pitch += MathHelper.wrapDegrees(90.0F - state.pitch) / 10.0F;
            } else {
                state.pitch += MathHelper.wrapDegrees(-50.0F - state.pitch) / 64.0F;
            }
            segment.updateHeadRotation(head, state.yaw, state.pitch);
        }
    }

    private void updatePlayDeadHeadRotations() {
        for (int head = 0; head < heads.length; head++) {
            HeadState state = heads[head];
            tickHeadLerp(state);
            segment.updateHeadRotation(head, state.yaw, state.pitch);
            updateBeamCutoff(state);
        }
    }

    private void tickHeadLerp(HeadState state) {
        if (state.lerpPitchSteps > 0) {
            state.pitch += MathHelper.wrapDegrees(state.lerpPitchTarget - state.pitch)
                    / state.lerpPitchSteps;
            --state.lerpPitchSteps;
        }
        if (state.lerpYawSteps > 0) {
            state.yaw += MathHelper.wrapDegrees(state.lerpYawTarget - state.yaw)
                    / state.lerpYawSteps;
            --state.lerpYawSteps;
        }
    }

    private void lerpHeadTo(HeadState state, float pitch, float yaw, int steps) {
        state.lerpPitchTarget = pitch;
        state.lerpYawTarget = yaw;
        state.lerpPitchSteps = Math.max(0, steps);
        state.lerpYawSteps = Math.max(0, steps);
    }

    private void updateBeamCutoff(HeadState state) {
        Vec3d direction = getHeadDirection(state);
        state.beamCutoff = getBeamCutoff(state.position, direction);
    }

    private static float rotateTowards(float current, float wanted, float maximum) {
        float delta = MathHelper.clamp(MathHelper.wrapDegrees(wanted - current), -maximum, maximum);
        return MathHelper.wrapDegrees(current + delta);
    }

    private static float smoothSyncedRotation(float current, float target, float maximumChange) {
        return current + MathHelper.clamp(MathHelper.wrapDegrees(target - current),
                -maximumChange, maximumChange);
    }

    private void tickHeads(WitherStormEntity owner) {
        // 性能优化：每 tick 单次扫描填充牵引候选（检测机制与上游一致）
        pullableCandidates.clear();
        if (!segment.isDead) {
            AxisAlignedBB search = segment.getEntityBoundingBox().grow(320.0D);
            for (Entity entity : segment.world.getEntitiesWithinAABB(Entity.class, search,
                    entity -> isBasicPullable(entity) && entity != segment && entity != owner && !entity.isDead
                            && entity.dimension == segment.dimension
                            && !ignoredTargets.shouldIgnoreTarget(entity)
                            && !(entity instanceof SupplementalEntities.StormPartBase)
                            && !(entity instanceof WitherStormEntity)
                            && !(entity instanceof PowerfulExplosiveEntity.FormidibombEntity)
                            && !isTracking(entity)
                            && (!(entity instanceof EntityPlayer)
                            || !((EntityPlayer) entity).capabilities.disableDamage))) {
                pullableCandidates.add(entity);
            }
        }
        logPlayerBeamDiagnostics(owner);
        for (int head = 0; head < 3; head++) {
            if (!isHeadBeamActive(owner, head)) continue;
            Vec3d headPosition = getHeadPosition(head, 1.0F);
            Vec3d direction = getHeadDirection(heads[head]);
            double cutoff = heads[head].beamCutoff;
            for (Entity entity : pullableCandidates) {
                if (!isInsideBeam(entity, headPosition, direction, cutoff)) continue;
                boolean selectedTarget = heads[head].target == entity;
                if (!selectedTarget && !canPullUntargeted(owner, entity, head)) continue;
                double speed = selectedTarget
                        ? WitherStormConfig.tractorPullSpeedModifier : getPullSpeed(entity);
                pullInTarget(owner, entity, speed, head, headPosition, direction, cutoff);
                if (!selectedTarget && !(entity instanceof EntityPlayer)
                        && entity.getDistanceSq(headPosition.x, headPosition.y, headPosition.z) < 400.0D) {
                    trackEntity(entity);
                }
            }
        }
    }

    /** 每秒记录玩家是否进入分体各头光束以及拉力过滤结果。 */
    private void logPlayerBeamDiagnostics(WitherStormEntity owner) {
        if (!StormDiagnosticLogger.isEnabled() || segment.ticksExisted % 20 != 0) return;
        for (EntityPlayer player : segment.world.playerEntities) {
            boolean pullCandidate = pullableCandidates.contains(player);
            for (int head = 0; head < heads.length; head++) {
                boolean active = isHeadBeamActive(owner, head);
                Vec3d origin = getHeadPosition(head, 1.0F);
                Vec3d direction = getHeadDirection(heads[head]);
                double cutoff = heads[head].beamCutoff;
                boolean inside = active && isInsideBeam(player, origin, direction, cutoff);
                boolean selected = heads[head].target == player;
                boolean untargetedEligible = active && inside && pullCandidate && !selected
                        && canPullUntargetedForDiagnostics(owner, player, head);
                String decision = !active ? "光束关闭"
                        : !pullCandidate ? "玩家不在牵引候选列表"
                        : !inside ? "玩家在光束几何外"
                        : selected ? "目标拉力条件通过"
                        : untargetedEligible ? "非目标拉力条件通过"
                        : "非目标拉力过滤拒绝";
                StormDiagnosticLogger.info(
                        "[风暴诊断][分体玩家光束判定] 分体={} 所属风暴={} 阶段={} tick={} 玩家={} 玩家UUID={} 头={} 判定={} 光束启用={} 牵引候选={} 当前目标={} 位于光束内={} 非目标拉力允许={} 玩家位置={} 头位置={} 方向={} cutoff={}",
                        segment.getUniqueID(), segment.getOwnerUuid(), owner.getPhase(),
                        segment.ticksExisted, player.getName(), player.getUniqueID(), head,
                        decision, active, pullCandidate, describeEntity(heads[head].target),
                        inside, untargetedEligible, player.getPositionVector(), origin,
                        direction, cutoff);
            }
        }
    }

    /** 与非目标拉力过滤等价，但不发送目标事件，避免诊断改变模组行为。 */
    private boolean canPullUntargetedForDiagnostics(WitherStormEntity owner,
                                                     EntityPlayer player, int head) {
        return WitherStormConfig.canPickupMobClusters
                && !heads[head].isDistracted()
                && !ignoredTargets.shouldIgnoreTarget(player)
                && !isTargetInUseByStormFamily(player, head, false)
                && !isPassengerTarget(player)
                && !isInsideOtherTractorBeam(player, head)
                && canSeeWithCache(head, player)
                && player.isEntityAlive()
                && player.world == segment.world
                && player.dimension == segment.dimension
                && !segment.isOnSameTeam(player)
                && owner.isValidStormTarget(player)
                && !isTracking(player)
                && !segment.isEntityBehindBack(player)
                && !owner.isBlockingWithShield(player);
    }

    private void tickHeadClusterPickups(WitherStormEntity owner) {
        for (int head = 0; head < heads.length; head++) {
            if (!isHeadBeamActive(owner, head)) continue;
            HeadState state = heads[head];
            tickHeadClusterPickup(owner, head, state.position, getHeadDirection(state));
        }
    }

    private void tickHeadClusterPickup(WitherStormEntity owner, int head,
                                       Vec3d position, Vec3d direction) {
        HeadState state = heads[head];
        if (segment.ticksExisted < state.nextClusterTicks) return;
        state.nextClusterTicks = segment.ticksExisted + 12;
        SupplementalEntities.BlockClusterEntity cluster = owner.createTractorBeamCluster(
                position, direction, 1, head, segment.getRNG(), segment);
        if (cluster != null) trackEntity(cluster);
        owner.removeFluidFromRay(position, direction, segment);
    }

    private void tickHeadState(WitherStormEntity owner) {
        for (int head = 0; head < heads.length; head++) {
            HeadState state = heads[head];
            if (state.injuryTicks > 0 && --state.injuryTicks == 0) {
                segment.setHeadInjuryFlag(head, false);
                if (!segment.isInDeathSequence()) playHeadSound(head,
                        "wither_storm_tractor_beam_activate", segment.getSoundVolume() + 2.5F);
            }
            if (state.injuryCooldown > 0) --state.injuryCooldown;
            if (state.injuryTicks > 0 && state.nextShake > 0 && --state.nextShake == 0) {
                segment.setHeadFlag(shakeBit(head), true);
            }
            if (segment.isHeadFlagSet(roarBit(head)) && ++state.roarTicks > 40) {
                state.roarTicks = 0;
                segment.setHeadFlag(roarBit(head), false);
            }
            if (segment.isHeadFlagSet(biteBit(head)) && ++state.biteTicks > 10) {
                state.biteTicks = 0;
                segment.setHeadFlag(biteBit(head), false);
                playHeadSound(head, "wither_storm_bite", Math.max(2.0F, segment.getSoundVolume()));
            }
            if (!state.roarScheduleInitialized) {
                state.nextRoarTicks = 201 + segment.getRNG().nextInt(200);
                state.roarScheduleInitialized = true;
            } else if (state.nextRoarTicks > 0) {
                --state.nextRoarTicks;
            }
            if (state.roarScheduleInitialized && state.nextRoarTicks <= 0) {
                state.nextRoarTicks = nextRoarDelay() + 1;
                if (isHeadBeamActive(owner, head) && !owner.isAttractingFormidibomb()) {
                    spawnFlamingSkull(head, false);
                }
                startRoaring(head, isHeadInjured(head));
            }
            tickNormalSkullAttack(owner, head, state);
        }
    }

    private void tickNormalSkullAttack(WitherStormEntity owner, int head, HeadState state) {
        if (!isHeadEnabled(owner, head) || isHeadInjured(head)) return;
        if (segment.ticksExisted < state.nextHeadUpdate) return;

        if (owner.getPhase() < 4) {
            state.nextHeadUpdate = segment.ticksExisted + 10 + segment.getRNG().nextInt(10);
        } else {
            state.nextHeadUpdate = segment.ticksExisted + 1200 + segment.getRNG().nextInt(120);
        }

        boolean canShoot = !isHeadBeamActive(owner, head);
        if (state.idleAttacks++ > 15) {
            if (canShoot) {
                Vec3d position = state.position;
                spawnNormalSkull(head,
                        WitherStormPartLogic.randomBetween(
                                segment.getRNG(), position.x - 10.0D, position.x + 10.0D),
                        WitherStormPartLogic.randomBetween(
                                segment.getRNG(), position.y - 5.0D, position.y + 5.0D),
                        WitherStormPartLogic.randomBetween(
                                segment.getRNG(), position.z - 10.0D, position.z + 10.0D),
                        true);
            }
            state.idleAttacks = 0;
        }

        EntityLivingBase target = state.target;
        if (target != null && target.isEntityAlive()) {
            if (canShoot) {
                spawnNormalSkull(head, target.posX,
                        target.posY + target.getEyeHeight() * 0.5D, target.posZ,
                        head == 0 && segment.getRNG().nextFloat() < 0.001F);
            }
            state.nextHeadUpdate = segment.ticksExisted + (owner.getPhase() < 4
                    ? 40 + segment.getRNG().nextInt(20)
                    : 1800 + segment.getRNG().nextInt(160));
            state.idleAttacks = 0;
        } else {
            state.nextHeadUpdate = segment.ticksExisted + 40 + segment.getRNG().nextInt(20);
        }
    }

    private int nextRoarDelay() {
        int minimum = Math.max(1, WitherStormConfig.minimumRoarInterval) * 20;
        int maximum = Math.max(minimum, WitherStormConfig.maximumRoarInterval * 20);
        return minimum + (maximum > minimum ? segment.getRNG().nextInt(maximum - minimum) : 0);
    }

    private void startRoaring(int head, boolean screaming) {
        segment.setHeadFlag(roarBit(head), true);
        if (segment.areOtherHeadsDisabled() && head != 0) return;
        playHeadSound(head, screaming ? "wither_storm_hurt" : "wither_storm_roar",
                Math.max(6.0F, segment.getSoundVolume() + 2.5F));
    }

    private void startBiting(int head) {
        HeadState state = heads[MathHelper.clamp(head, 0, 2)];
        state.biteTicks = 0;
        segment.setHeadFlag(biteBit(head), true);
    }

    private void playHeadSound(int head, String soundName, float volume) {
        Vec3d position = getHeadPosition(head, 1.0F);
        segment.world.playSound(null, position.x, position.y, position.z,
                ModSounds.get(soundName), SoundCategory.HOSTILE, volume, 1.0F);
    }

    private void spawnFlamingSkull(int head, boolean blue) {
        HeadState state = heads[MathHelper.clamp(head, 0, 2)];
        Vec3d position = state.position;
        Vec3d direction = getHeadDirection(state);
        double speed = WitherStormConfig.flamingSkullSpeedModifier;
        SupplementalEntities.FlamingWitherSkullEntity skull = blue
                ? new SupplementalEntities.BlueFlamingWitherSkullEntity(segment.world, segment,
                direction.x * speed, direction.y * speed, direction.z * speed)
                : new SupplementalEntities.FlamingWitherSkullEntity(segment.world, segment,
                direction.x * speed, direction.y * speed, direction.z * speed);
        skull.setPosition(position.x, position.y, position.z);
        segment.world.spawnEntity(skull);
        playHeadSound(head, "wither_storm_shoot", Math.max(5.0F, segment.getSoundVolume() - 5.0F));
    }

    private void spawnNormalSkull(int head, double x, double y, double z, boolean dangerous) {
        Vec3d position = heads[MathHelper.clamp(head, 0, 2)].position;
        segment.world.playEvent(null, 1024,
                new BlockPos(segment.posX, segment.posY, segment.posZ), 0);
        EntityWitherSkull skull = new EntityWitherSkull(segment.world, segment,
                x - position.x, y - position.y, z - position.z);
        skull.setInvulnerable(dangerous);
        skull.setPosition(position.x, position.y, position.z);
        segment.world.spawnEntity(skull);
    }

    private void tickProjectilesHittingHeads(WitherStormEntity owner) {
        for (int head = 0; head < heads.length; head++) {
            if (!isHeadBeamActive(owner, head)) continue;
            List<Entity> projectiles = segment.world.getEntitiesWithinAABB(Entity.class, getHeadBounds(head),
                    entity -> !entity.isDead && WitherStormEntity.isHeadHittingProjectile(entity));
            for (Entity projectile : projectiles) {
                Entity projectileOwner = WitherStormEntity.getProjectileOwner(projectile);
                if (projectileOwner == segment || projectileOwner == owner) continue;
                boolean wasInjured = isHeadInjured(head);
                boolean accepted = attemptAttack(head, projectileOwner, 40);
                if (accepted && !wasInjured && isHeadInjured(head) && projectileOwner instanceof EntityPlayer) {
                    segment.world.playSound(null, projectileOwner.posX, projectileOwner.posY, projectileOwner.posZ,
                            SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
                projectile.setDead();
            }
        }
    }

    boolean attemptAttack(int head, @Nullable Entity attacker, int attemptCooldown) {
        WitherStormEntity owner = segment.getOwnerStorm();
        if (!WitherStormConfig.canAttackHeads || segment.isDead || segment.isInDeathSequence()
                || owner == null || !isHeadEnabled(owner, head)) return false;
        int index = MathHelper.clamp(head, 0, 2);
        HeadState state = heads[index];
        if (state.injuryCooldown > 0 || state.injuryTicks > 0) return false;
        state.injuryCooldown = Math.max(1, attemptCooldown);
        countAttack(owner, index, attacker);
        return true;
    }

    boolean attackFromExplosion(int head, @Nullable Entity attacker) {
        WitherStormEntity owner = segment.getOwnerStorm();
        if (!WitherStormConfig.canAttackHeads || segment.isDead || segment.isInDeathSequence()
                || owner == null || !isHeadEnabled(owner, head)) return false;
        int index = MathHelper.clamp(head, 0, 2);
        if (heads[index].injuryTicks > 0) return false;
        return countAttack(owner, index, attacker);
    }

    private boolean countAttack(WitherStormEntity owner, int index, @Nullable Entity attacker) {
        HeadState state = heads[index];
        ModNetwork.notifyHeadAttacked(segment, index);
        state.hits++;
        segment.setHeadFlag(shakeBit(index), true);
        if (state.hits < requiredHits(index)) {
            startRoaring(index, true);
            state.roarTicks = 20;
            return false;
        }
        hurtHead(owner, index, attacker);
        return true;
    }

    private int requiredHits(int head) {
        HeadState state = heads[MathHelper.clamp(head, 0, 2)];
        if (state.requiredHits <= 0) state.requiredHits = 3 + segment.getRNG().nextInt(3);
        return state.requiredHits;
    }

    private void hurtHead(WitherStormEntity owner, int head, @Nullable Entity attacker) {
        HeadState state = heads[head];
        boolean attackerWasTarget = attacker instanceof EntityPlayerMP && segment.isTargeting(attacker);
        state.injuryTicks = owner.getPhase() > 3 ? 720 : 180;
        state.injuryCooldown = 40;
        state.hits = 0;
        state.requiredHits = 3 + segment.getRNG().nextInt(3);
        state.roarTicks = 20;
        setTarget(head, null);
        segment.setHeadInjuryFlag(head, true);
        segment.setHeadFlag(shakeBit(head), true);
        startRoaring(head, true);
        spawnFlamingSkull(head, true);
        if (attackerWasTarget) {
            EntityPlayerMP player = (EntityPlayerMP) attacker;
            SymbiontSummoningManager.makeInvulnerable(player,
                    UltimateTargetManager.getHeadEscapeTicks(
                            WitherStormConfig.headEscapeTime, segment.getRNG().nextInt(80)),
                    "击伤分体头部后逃脱");
            ModCriteriaTriggers.ESCAPE_WITHER_STORM.trigger(player, owner);
        }
    }

    void hurtHeadDirectly(int head, @Nullable Entity attacker) {
        WitherStormEntity owner = segment.getOwnerStorm();
        if (segment.world.isRemote || segment.isDead || owner == null || owner.isDead) return;
        hurtHead(owner, MathHelper.clamp(head, 0, 2), attacker);
    }

    private boolean canPullUntargeted(WitherStormEntity owner, Entity entity, int head) {
        if (!WitherStormConfig.canPickupMobClusters || heads[head].isDistracted()
                || ignoredTargets.shouldIgnoreTarget(entity)
                || isTargetInUseByStormFamily(entity, head, false)
                || isPassengerTarget(entity)
                || isInsideOtherTractorBeam(entity, head)
                || !canSeeWithCache(head, entity)) return false;
        return !(entity instanceof EntityLivingBase)
                || isTargetApplicable(owner, (EntityLivingBase) entity, head)
                && !owner.isBlockingWithShield((EntityLivingBase) entity);
    }

    private double getPullSpeed(Entity entity) {
        if (entity instanceof EntityItem || entity instanceof EntityBoat || entity instanceof EntityMinecart) {
            return 0.4D;
        }
        if (entity instanceof EntityPlayer) return WitherStormConfig.tractorPullSpeedModifier;
        return WitherStormConfig.tractorPullSpeedModifier - 0.05D
                + new Random(entity.getEntityId()).nextDouble() * 0.1D;
    }

    private void pullInTarget(WitherStormEntity owner, Entity target, double speed, int head,
                              Vec3d headPosition, Vec3d direction, double cutoff) {
        Vec3d targetPosition = headPosition;
        if (!(target instanceof EntityPlayer) && target.getPositionVector().distanceTo(headPosition) >= 25.0D) {
            targetPosition = TractorBeamHelper.calculateClosestPoint(
                    target.getPositionVector(), headPosition, direction, cutoff, -5.0D);
        }
        Vec3d velocity = TractorBeamHelper.calculatePullVelocity(
                target.getPositionVector(), targetPosition, speed);
        if (target instanceof EntityPlayer && StormDiagnosticLogger.isEnabled()
                && segment.ticksExisted % 20 == 0) {
            StormDiagnosticLogger.info(
                    "[风暴诊断][分体玩家拉力执行] 分体={} 所属风暴={} 阶段={} tick={} 玩家={} 玩家UUID={} 头={} 是否选中目标={} 速度倍率={} 拉力速度={} 玩家位置={} 拉取点={} cutoff={}",
                    segment.getUniqueID(), segment.getOwnerUuid(), owner.getPhase(),
                    segment.ticksExisted, target.getName(), target.getUniqueID(), head,
                    heads[head].target == target, speed, velocity,
                    target.getPositionVector(), targetPosition, cutoff);
        }
        if (velocity.lengthSquared() > 0.0D) {
            Entity pulled = target;
            Entity vehicle = target.getRidingEntity();
            if (vehicle != null && WitherStormConfig.shouldPickUpVehicles
                    && owner.canPullVehicle(vehicle)) {
                pulled = vehicle;
            }
            pulled.motionX = velocity.x;
            pulled.motionY = velocity.y;
            pulled.motionZ = velocity.z;
            pulled.velocityChanged = true;
            if (target instanceof EntityPlayerMP) {
                ModNetwork.setPlayerMotion(
                        (EntityPlayerMP) target, pulled, velocity);
            }
        }
        AxisAlignedBB headBox = new AxisAlignedBB(
                headPosition.x - 2.0D, headPosition.y - 4.0D, headPosition.z - 2.0D,
                headPosition.x + 2.0D, headPosition.y + 2.0D, headPosition.z + 2.0D);
        if (!headBox.intersects(target.getEntityBoundingBox())) return;
        if (target instanceof EntityLivingBase && !((EntityLivingBase) target).isEntityAlive()) return;
        if (target instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) target;
            float damage = WitherStormConfig.instantChomp ? Float.MAX_VALUE
                    : (float) segment.getEntityAttribute(
                            SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
            player.attackEntityFrom(ModDamageSources.witherStormAttack(segment), damage);
            if (!player.isEntityAlive()) owner.notifySegmentConsumption(segment, player, 1);
            startBiting(head);
        } else if (target instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) target;
            owner.notifySegmentConsumption(segment, living, 1);
            if (WitherStormConfig.healFromChomp) segment.heal(living.getMaxHealth() * 0.5F);
            segment.captureConsumedPet(living);
            owner.consumeEntityFromSegment(target, segment);
            startBiting(head);
            if (head > 0) heads[head].nextHeadUpdate = segment.ticksExisted
                    + segment.getRNG().nextInt(20) + segment.getRNG().nextInt(60);
        }
    }

    private void convertFallingBlocks() {
        if (!WitherStormConfig.convertFallingBlocks) return;
        List<EntityFallingBlock> fallingBlocks = segment.world.getEntitiesWithinAABB(
                EntityFallingBlock.class, getSearchBox(), falling ->
                        !falling.isDead
                                && segment.world.isBlockLoaded(falling.getPosition())
                                && canSeeOrIsInOpenArea(falling));
        for (EntityFallingBlock falling : fallingBlocks) {
            SupplementalEntities.BlockClusterEntity cluster =
                    new SupplementalEntities.BlockClusterEntity(segment.world,
                            falling.posX, falling.posY, falling.posZ, falling.getBlock());
            cluster.setRotationDelta(segment.getRNG().nextInt(20) * 0.05F,
                    segment.getRNG().nextInt(20) * 0.05F);
            cluster.setNoGravity(true);
            cluster.setPhysics(false);
            cluster.setCreatedFromFallingBlock(true);
            falling.setDead();
            if (segment.world.spawnEntity(cluster)) trackEntity(cluster);
        }
    }

    private void applyMassAbsorption(WitherStormEntity owner) {
        if (!ForgeEventFactory.getMobGriefingEvent(segment.world, segment)) return;
        double searchRadius = ENTITY_CONSUMPTION_RADIUS + 150.0D;
        AxisAlignedBB search = segment.getEntityBoundingBox().grow(searchRadius);
        List<AxisAlignedBB> protectedDropAreas = new ArrayList<AxisAlignedBB>();
        for (EntityPlayerMP player : segment.world.getEntitiesWithinAABB(EntityPlayerMP.class, search)) {
            protectedDropAreas.add(player.getEntityBoundingBox().grow(8.0D));
        }
        List<Entity> entities = segment.world.getEntitiesWithinAABB(Entity.class, search, entity ->
                !entity.isDead && entity.dimension == segment.dimension
                        && (entity instanceof EntityItem
                        || entity instanceof EntitySlime && ((EntitySlime) entity).isSmallSlime())
                        && !isTracking(entity) && segment.getRNG().nextFloat() >= 0.9F);
        for (Entity entity : entities) {
            double distance = entity.getDistance(segment);
            if (entity instanceof EntityItem) {
                ItemStack stack = ((EntityItem) entity).getItem();
                if (WitherStormConfig.removeNearbyJunk
                        && UpstreamItemTags.contains(UpstreamItemTags.JUNK, stack)) {
                    if (!WitherStormEntity.isInsideAny(
                            entity.getPositionVector(), protectedDropAreas)) entity.setDead();
                    continue;
                }
                boolean unappetizing = UpstreamItemTags.contains(UpstreamItemTags.UNAPPETIZING, stack);
                if (distance > ENTITY_CONSUMPTION_RADIUS
                        || unappetizing && distance >= 35.0D
                        || WitherStormEntity.isProtectedFromConsumption(stack)
                        || !canTrackMassEntity(owner, entity)) continue;
            } else if (distance > ENTITY_CONSUMPTION_RADIUS
                    || !canTrackMassEntity(owner, entity)) {
                continue;
            }
            trackEntity(entity);
            entity.setNoGravity(true);
        }
    }

    AxisAlignedBB getSearchBox() {
        double range = segment.getEntityAttribute(
                SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        return segment.getEntityBoundingBox().grow(range, range + 255.0D, range);
    }

    private boolean canTrackMassEntity(WitherStormEntity owner, Entity entity) {
        return !owner.isTrackedForConsumption(entity) && canSeeOrIsInOpenArea(entity);
    }

    private boolean canSeeOrIsInOpenArea(Entity entity) {
        return WorldUtil.isInAnOpenArea(entity) || WorldUtil.hasLineOfSight(segment, entity);
    }

    private void trackEntity(Entity entity) {
        if (entity == null || entity.isDead || entity instanceof EntityPlayer
                || entity instanceof WitherStormEntity
                || entity instanceof SupplementalEntities.StormPartBase
                || entity instanceof PowerfulExplosiveEntity.FormidibombEntity) return;
        if (isTracking(entity)) return;
        trackedEntities.put(entity.getUniqueID(), entity);
        savedTrackedEntities.remove(entity.getUniqueID());
        if (entity instanceof SupplementalEntities.BlockClusterEntity) {
            entity.setNoGravity(true);
            ((SupplementalEntities.BlockClusterEntity) entity).setPhysics(false);
        }
    }

    void trackEntityToConsume(Entity entity) {
        trackEntity(entity);
    }

    private void tickTrackedEntities(WitherStormEntity owner) {
        if (trackedEntities.isEmpty()) return;
        Vec3d absorptionPoint = WorldUtil.centerOf(segment.getEntityBoundingBox());
        AxisAlignedBB absorptionBox = segment.getEntityBoundingBox().grow(
                Math.max(1.0D, segment.width / 1.5D));
        List<Entity> splitClusters = new ArrayList<Entity>();
        Iterator<Map.Entry<UUID, Entity>> iterator = trackedEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next().getValue();
            if (entity == null || entity.isDead || entity.world != segment.world) {
                iterator.remove();
                continue;
            }
            if (WitherStormPulling.canPullIn(entity, trackedEntityPullSource)) {
                Vec3d delta = absorptionPoint.subtract(entity.getPositionVector());
                double distance = delta.length();
                if (distance >= 320.0D || !segment.world.isBlockLoaded(entity.getPosition())) {
                    entity.setPosition(absorptionPoint.x, absorptionPoint.y, absorptionPoint.z);
                }
                Vec3d pullVelocity = WitherStormPulling.getPullVelocity(
                        entity, trackedEntityPullSource, absorptionPoint);
                WitherStormPulling.applyVelocity(entity, pullVelocity, trackedEntityPullSource);
                if (WitherStormPulling.reachesAbsorptionBox(entity, absorptionBox, pullVelocity)) {
                    int amount = 0;
                    boolean countConsumption = true;
                    if (entity instanceof SupplementalEntities.BlockClusterEntity) {
                        SupplementalEntities.BlockClusterEntity cluster =
                                (SupplementalEntities.BlockClusterEntity) entity;
                        if (!cluster.shouldNotCountToConsumedMass()) {
                            amount = cluster.getBlocks().size();
                        } else {
                            countConsumption = false;
                        }
                    } else if (entity instanceof EntityItem) {
                        amount = ((EntityItem) entity).getItem().getCount();
                    } else {
                        amount = 1;
                    }
                    if (countConsumption) owner.notifySegmentConsumption(segment, entity, amount);
                    if (entity instanceof EntityLivingBase) {
                        segment.captureConsumedPet((EntityLivingBase) entity);
                    }
                    owner.consumeEntityFromSegment(entity, segment);
                    iterator.remove();
                }
            }
            if (entity instanceof SupplementalEntities.BlockClusterEntity && !entity.isDead) {
                SupplementalEntities.BlockClusterEntity cluster = (SupplementalEntities.BlockClusterEntity) entity;
                if (cluster.shouldCrumble() && cluster.getShakeTime() <= 0
                        && segment.ticksExisted % 20 == 0 && segment.getRNG().nextInt(3) == 0) {
                    SupplementalEntities.BlockClusterEntity split = cluster.splitAt(
                            EnumFacing.Axis.values()[segment.getRNG().nextInt(3)]);
                    if (split != null && segment.world.spawnEntity(split)) {
                        if (segment.getRNG().nextBoolean()) {
                            owner.trackEntityFromSegment(split);
                        } else {
                            splitClusters.add(split);
                        }
                    }
                }
            }
        }
        for (Entity split : splitClusters) trackEntity(split);
    }

    private void resolveSavedTrackedEntities() {
        ++trackedEntityTicks;
        if (savedTrackedEntities.isEmpty()) return;
        Iterator<UUID> iterator = savedTrackedEntities.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Entity entity = resolve(uuid);
            if (entity != null && !entity.isDead) {
                trackedEntities.put(uuid, entity);
                iterator.remove();
            } else if (trackedEntityTicks > 80) {
                iterator.remove();
            }
        }
    }

    @Nullable
    private Entity resolve(@Nullable UUID uuid) {
        if (uuid == null) return null;
        for (Entity entity : segment.world.loadedEntityList) {
            if (uuid.equals(entity.getUniqueID())) return entity;
        }
        return null;
    }

    boolean isTracking(Entity entity) {
        return entity != null && trackedEntities.containsKey(entity.getUniqueID());
    }

    @Nullable
    public EntityLivingBase getTarget(int head) {
        int index = MathHelper.clamp(head, 0, 2);
        HeadState state = heads[index];
        if (!segment.world.isRemote) return state.target;
        int entityId = segment.getWatchedTargetId(index);
        Entity entity = entityId > 0 ? segment.world.getEntityByID(entityId) : null;
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    public boolean isDistracted(int head) {
        return heads[MathHelper.clamp(head, 0, heads.length - 1)].isDistracted();
    }

    @Nullable
    public Vec3d getDistractedPos(int head) {
        return heads[MathHelper.clamp(head, 0, heads.length - 1)].distractionPosition;
    }

    public void setDistractedPos(int head, @Nullable Vec3d position) {
        HeadState state = heads[MathHelper.clamp(head, 0, heads.length - 1)];
        state.distractionPosition = position;
        if (position != null && state.distractionTicks <= 0) {
            state.distractionTicks = 1;
        }
    }

    boolean isHeadInjured(int head) {
        int index = MathHelper.clamp(head, 0, 2);
        return segment.isHeadInjuryFlagSet(index) || heads[index].injuryTicks > 0;
    }

    AxisAlignedBB getHeadBounds(int head) {
        Vec3d position = heads[MathHelper.clamp(head, 0, 2)].position;
        double size = 3.0D;
        return new AxisAlignedBB(position.x - size, position.y - size, position.z - size,
                position.x + size, position.y + size, position.z + size);
    }

    float getYaw(int head, float partialTicks) {
        HeadState state = heads[MathHelper.clamp(head, 0, 2)];
        return interpolateRotation(state.yawO, state.yaw, partialTicks);
    }

    float getPitch(int head, float partialTicks) {
        HeadState state = heads[MathHelper.clamp(head, 0, 2)];
        return lerp(state.pitchO, state.pitch, partialTicks);
    }

    float getMouthAnimation(int head, float partialTicks) {
        HeadState state = heads[MathHelper.clamp(head, 0, 2)];
        return lerp(state.mouthO, state.mouth, partialTicks);
    }

    float getBrokenJawAnimation(int head, float partialTicks) {
        HeadState state = heads[MathHelper.clamp(head, 0, 2)];
        return lerp(state.brokenJawO, state.brokenJaw, partialTicks);
    }

    float getHeadShakeAnimation(int head, float partialTicks) {
        HeadState state = heads[MathHelper.clamp(head, 0, 2)];
        return WitherStormPartLogic.shakeRoll(state.shakeO, state.shake, partialTicks);
    }

    int getHeadHurtDuration(int head) {
        return heads[MathHelper.clamp(head, 0, 2)].hurtOverlayTicks;
    }

    void handleHeadAttackedOnClient(int head) {
        if (!segment.world.isRemote) return;
        heads[MathHelper.clamp(head, 0, 2)].hurtOverlayTicks = 10;
    }

    Vec3d getHeadPosition(int head, float partialTicks) {
        HeadState state = heads[MathHelper.clamp(head, 0, 2)];
        return new Vec3d(lerp(state.positionO.x, state.position.x, partialTicks),
                lerp(state.positionO.y, state.position.y, partialTicks),
                lerp(state.positionO.z, state.position.z, partialTicks));
    }

    private Vec3d calculateHeadPosition(int head) {
        double[] offset = HEAD_OFFSETS[MathHelper.clamp(head, 0, 2)];
        float bodyYaw = (segment.getSegmentBodyYaw() + 180.0F) * 0.017453292F;
        float bodyYaw90 = (segment.getSegmentBodyYaw() + 270.0F) * 0.017453292F;
        float bodyPitch = -(segment.rotationPitch + 270.0F) * 0.017453292F;
        double lateralX = MathHelper.cos(bodyYaw) * offset[0];
        double lateralZ = MathHelper.sin(bodyYaw) * offset[0];
        float polarOffset = (float) MathHelper.atan2(offset[2], offset[1]);
        double radius = Math.sqrt(offset[2] * offset[2] + offset[1] * offset[1]);
        double rawX = MathHelper.cos(bodyPitch + polarOffset) * MathHelper.cos(bodyYaw90);
        double rawY = MathHelper.sin(bodyPitch + polarOffset);
        double rawZ = MathHelper.cos(bodyPitch + polarOffset) * MathHelper.sin(bodyYaw90);
        return new Vec3d(segment.posX + lateralX + rawX * radius,
                segment.posY + rawY * radius, segment.posZ + lateralZ + rawZ * radius);
    }

    private static Vec3d getHeadDirection(HeadState state) {
        float pitch = state.pitch * 0.017453292F;
        float yaw = state.yaw * 0.017453292F;
        float horizontal = MathHelper.cos(pitch);
        return new Vec3d(-MathHelper.sin(yaw) * horizontal,
                -MathHelper.sin(pitch), MathHelper.cos(yaw) * horizontal).normalize();
    }

    Vec3d getLookVector(int head) {
        return getHeadDirection(heads[MathHelper.clamp(head, 0, heads.length - 1)]);
    }

    Vec3d getLookVector(int head, float partialTicks) {
        float pitch = getPitch(head, partialTicks) * 0.017453292F;
        float yaw = getYaw(head, partialTicks) * 0.017453292F;
        float horizontal = MathHelper.cos(pitch);
        return new Vec3d(-MathHelper.sin(yaw) * horizontal,
                -MathHelper.sin(pitch), MathHelper.cos(yaw) * horizontal).normalize();
    }

    double getTractorBeamCutoffDistance(int head) {
        return heads[MathHelper.clamp(head, 0, heads.length - 1)].beamCutoff;
    }

    private double getBeamCutoff(Vec3d position, Vec3d direction) {
        return TractorBeamHelper.findCutoffDistance(segment.world, position, direction, 250.0D);
    }

    private boolean isInsideBeam(Entity entity, Vec3d origin, Vec3d direction, double cutoff) {
        // 上游对所有实体（含玩家）统一用 4.0 判定光束半径；8.0 会让玩家更难脱离。
        return TractorBeamHelper.isInsideTractorBeam(
                entity.getPositionVector(), origin, direction, cutoff, 4.0D);
    }

    boolean canSee(int head, Entity entity) {
        Vec3d start = getHeadPosition(head, 1.0F);
        Vec3d end = entity instanceof EntityLivingBase
                ? ((EntityLivingBase) entity).getPositionEyes(1.0F) : entity.getPositionVector();
        RayTraceResult hit = segment.world.rayTraceBlocks(start, end, false, true, false);
        return hit == null || hit.typeOfHit == RayTraceResult.Type.MISS;
    }

    /** 性能优化：同 tick 内缓存头部视线结果，同一 tick 内同一头对同一实体只做一次射线。 */
    boolean canSeeWithCache(int head, Entity entity) {
        long cycle = segment.world.getTotalWorldTime();
        if (cycle != pullSightCacheCycle) {
            pullSightCacheCycle = cycle;
            pullSightCache.clear();
        }
        String key = head + ":" + entity.getUniqueID();
        Boolean cached = pullSightCache.get(key);
        if (cached != null) return cached;
        boolean result = canSee(head, entity);
        pullSightCache.put(key, result);
        return result;
    }

    boolean isInsideOtherTractorBeam(Entity entity, int excludedHead) {
        if (entity == null) return false;
        WitherStormEntity owner = segment.getOwnerStorm();
        if (owner == null) return false;
        int containingHead = owner.findContainingTractorBeamHead(entity, 5.0D);
        if (containingHead >= 0 && containingHead != excludedHead) return true;
        for (SupplementalEntities.WitherStormSegmentEntity other : getFamilySegments()) {
            if (other == null || other.isDead) continue;
            WitherStormSegmentManager manager = other.getSegmentManager();
            containingHead = manager.findContainingTractorBeamHead(entity, 5.0D);
            if (containingHead >= 0 && containingHead != excludedHead) return true;
        }
        return false;
    }

    boolean isInsideOwnTractorBeam(Entity entity, int excludedHead) {
        int containingHead = findContainingTractorBeamHead(entity, 5.0D);
        return containingHead >= 0 && containingHead != excludedHead;
    }

    int findContainingTractorBeamHead(Entity entity, double radius) {
        if (entity == null) return -1;
        WitherStormEntity owner = segment.getOwnerStorm();
        if (owner == null) return -1;
        for (int head = 0; head < 3; head++) {
            if (!isHeadBeamActive(owner, head)) continue;
            Vec3d origin = getHeadPosition(head, 1.0F);
            Vec3d direction = getHeadDirection(heads[head]);
            if (TractorBeamHelper.isInsideTractorBeam(entity.getPositionVector(), origin, direction,
                    heads[head].beamCutoff, radius)) return head;
        }
        return -1;
    }

    void ignoreTarget(Entity entity) {
        ignoredTargets.addEntityToIgnore(entity);
    }

    void makeDistracted(int head, Vec3d position, int ticks) {
        HeadState state = heads[MathHelper.clamp(head, 0, heads.length - 1)];
        state.distractionEntity = null;
        state.distractionPosition = position;
        state.distractionTicks = Math.max(1, ticks);
        state.distractionUnseenTicks = 0;
    }

    void onOtherHeadsEnabled() {
        for (int head = 1; head < heads.length; head++) {
            heads[head].nextRoarTicks = segment.getRNG().nextInt(30) + 1;
            heads[head].roarScheduleInitialized = true;
        }
    }

    void onAddedToOwner(WitherStormEntity owner) {
        if (owner == null || segment.world.isRemote) return;
        for (int head = 0; head < heads.length; head++) {
            if (segment.isStormPlayDeadAiDisabled()) {
                heads[head].roarTicks = 0;
                segment.setHeadFlag(roarBit(head), true);
            } else {
                startRoaring(head, false);
            }
        }
    }

    void onStartFalling() {
        WitherStormEntity owner = segment.getOwnerStorm();
        if (owner == null || segment.world.isRemote) return;
        for (int head = 0; head < heads.length; head++) {
            setTarget(head, null);
            lerpHeadTo(heads[head], -50.0F, segment.getSegmentBodyYaw(), 64);
            startRoaring(head, segment.getRNG().nextBoolean());
        }
    }

    void onDeath() {
        if (segment.world.isRemote) return;
        clearTargets();
        for (int head = 0; head < heads.length; head++) {
            segment.setHeadFlag(roarBit(head), true);
            if (!segment.areOtherHeadsDisabled() || head == 0) {
                playHeadSound(head, "wither_storm_roar",
                        Math.max(6.0F, segment.getSoundVolume() + 2.5F));
            }
        }
    }

    void onStartPlayingDead() {
        if (segment.world.isRemote) return;
        for (HeadState state : heads) {
            lerpHeadTo(state, 40.0F, segment.getSegmentBodyYaw(), 16);
        }
    }

    void onAiRestored() {
        WitherStormEntity owner = segment.getOwnerStorm();
        if (owner == null || segment.world.isRemote) return;
        for (int head = 0; head < heads.length; head++) {
            startRoaring(head, segment.getRNG().nextBoolean());
        }
    }

    void restorePlayDeadPose(WitherStormEntity.PlayDeadState state) {
        if (state == WitherStormEntity.PlayDeadState.FALLING) {
            for (HeadState head : heads) {
                lerpHeadTo(head, -50.0F, segment.getSegmentBodyYaw(), 64);
            }
        } else if (state == WitherStormEntity.PlayDeadState.PLAYING_DEAD) {
            for (HeadState head : heads) {
                lerpHeadTo(head, 40.0F, segment.getSegmentBodyYaw(), 16);
            }
        }
    }

    private boolean isHeadBeamActive(WitherStormEntity owner, int head) {
        return owner != null && !owner.isDeadOrPlayingDead() && owner.getPhase() > 3
                && !isHeadInjured(head) && (head == 0 || !owner.areOtherHeadsDisabled());
    }

    boolean isTractorBeamActive(int head) {
        WitherStormEntity owner = segment.getOwnerStorm();
        return head >= 0 && head < heads.length && isHeadBeamActive(owner, head);
    }

    private List<SupplementalEntities.WitherStormSegmentEntity> findFamilySegments() {
        UUID ownerUuid = segment.getOwnerUuid();
        if (ownerUuid == null) return Collections.singletonList(segment);
        return segment.world.getEntities(SupplementalEntities.WitherStormSegmentEntity.class,
                candidate -> !candidate.isDead && ownerUuid.equals(candidate.getOwnerUuid()));
    }

    private List<SupplementalEntities.WitherStormSegmentEntity> getFamilySegments() {
        return familySegments.isEmpty() ? findFamilySegments() : familySegments;
    }

    private static boolean isBasicPullable(Entity entity) {
        return entity instanceof EntityLivingBase || entity instanceof EntityItem
                || entity instanceof EntityBoat || entity instanceof EntityMinecart;
    }

    private static boolean isHeadEnabled(WitherStormEntity owner, int head) {
        return head == 0 || !owner.areOtherHeadsDisabled();
    }

    void releaseTrackedEntities() {
        for (Entity entity : trackedEntities.values()) {
            if (entity == null || entity.isDead) continue;
            entity.setNoGravity(false);
            if (entity instanceof SupplementalEntities.BlockClusterEntity) {
                ((SupplementalEntities.BlockClusterEntity) entity).setPhysics(true);
            }
        }
        trackedEntities.clear();
    }

    void discardTrackedEntities() {
        Iterator<Map.Entry<UUID, Entity>> iterator = trackedEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next().getValue();
            if (entity instanceof SupplementalEntities.BlockClusterEntity) {
                iterator.remove();
                if (!entity.isDead) entity.setDead();
            }
        }
        for (Entity entity : trackedEntities.values()) {
            if (entity == null || entity.isDead) continue;
            entity.setNoGravity(false);
        }
        trackedEntities.clear();
    }

    void writeToNBT(NBTTagCompound compound) {
        compound.setTag("IgnoredTargets", ignoredTargets.writeToNBT());

        NBTTagCompound trackedEntitiesTag = new NBTTagCompound();
        NBTTagList trackedEntityList = new NBTTagList();
        Set<UUID> trackedIds = new LinkedHashSet<UUID>(trackedEntities.keySet());
        trackedIds.addAll(savedTrackedEntities);
        for (UUID uuid : trackedIds) trackedEntityList.appendTag(NBTUtil.createUUIDTag(uuid));
        trackedEntitiesTag.setTag("Entities", trackedEntityList);
        compound.setTag("TrackedEntities", trackedEntitiesTag);

        NBTTagCompound headsTag = new NBTTagCompound();
        for (int head = 0; head < heads.length; head++) {
            HeadState state = heads[head];
            NBTTagCompound headTag = new NBTTagCompound();
            headTag.setBoolean("IsRoaring", segment.isHeadFlagSet(roarBit(head)));
            headTag.setInteger("RoaringTime", state.roarTicks);
            if (state.distractionPosition != null) {
                headTag.setTag("DistractedPos", writeVector(state.distractionPosition));
            }
            headTag.setInteger("DistractedTime", state.distractionTicks);
            headTag.setInteger("AttackCooldown", state.injuryCooldown);
            headTag.setInteger("InjuryTime", state.injuryTicks);
            headTag.setInteger("Hits", state.hits);
            headsTag.setTag(String.valueOf(head), headTag);
        }
        compound.setTag("Heads", headsTag);
    }

    void readFromNBT(NBTTagCompound compound) {
        NBTTagCompound source = compound.hasKey("WitherStormSegmentManager", 10)
                && !compound.hasKey("Heads", 10)
                ? compound.getCompoundTag("WitherStormSegmentManager") : compound;
        int timerFormat = source.getInteger("TimerFormat");
        if (source.hasKey("IgnoredTargets", 10)) {
            ignoredTargets.readFromNBT(source.getCompoundTag("IgnoredTargets"));
        }
        trackedEntities.clear();
        savedTrackedEntities.clear();
        trackedEntityTicks = 0;
        NBTTagList tracked = source.hasKey("TrackedEntities", 10)
                ? source.getCompoundTag("TrackedEntities").getTagList("Entities", 10)
                : source.getTagList("TrackedEntities", 10);
        for (int index = 0; index < tracked.tagCount(); index++) {
            NBTTagCompound entry = tracked.getCompoundTagAt(index);
            UUID uuid = entry.hasUniqueId("UUID") ? entry.getUniqueId("UUID")
                    : entry.hasKey("M", 4) && entry.hasKey("L", 4) ? NBTUtil.getUUIDFromTag(entry) : null;
            if (uuid != null && !savedTrackedEntities.contains(uuid)) savedTrackedEntities.add(uuid);
        }
        NBTTagCompound headsTag = source.hasKey("Heads", 10)
                ? source.getCompoundTag("Heads") : null;
        for (int head = 0; head < heads.length; head++) {
            HeadState state = heads[head];
            if (headsTag != null && headsTag.hasKey(String.valueOf(head), 10)) {
                NBTTagCompound headTag = headsTag.getCompoundTag(String.valueOf(head));
                state.roarTicks = Math.max(0, headTag.getInteger("RoaringTime"));
                state.distractionPosition = headTag.hasKey("DistractedPos", 10)
                        ? readVector(headTag.getCompoundTag("DistractedPos")) : null;
                state.distractionTicks = Math.max(0, headTag.getInteger("DistractedTime"));
                state.injuryCooldown = Math.max(0, headTag.getInteger("AttackCooldown"));
                state.injuryTicks = Math.max(0, headTag.getInteger("InjuryTime"));
                state.hits = Math.max(0, headTag.getInteger("Hits"));
                state.roarScheduleInitialized = false;
                state.distractionEntity = null;
                segment.setHeadFlag(roarBit(head), headTag.getBoolean("IsRoaring"));
                segment.setHeadFlag(biteBit(head), false);
            } else if (source.hasKey("Head" + head, 10)) {
                NBTTagCompound headTag = source.getCompoundTag("Head" + head);
                state.yaw = state.yawO = headTag.getFloat("Yaw");
                state.pitch = state.pitchO = headTag.getFloat("Pitch");
                state.roarTicks = Math.max(0, headTag.getInteger("RoarTicks"));
                state.biteTicks = Math.max(0, headTag.getInteger("BiteTicks"));
                state.nextRoarTicks = Math.max(0, headTag.getInteger("NextRoar"));
                state.roarScheduleInitialized = headTag.getBoolean("RoarScheduleInitialized");
                state.nextHeadUpdate = headTag.hasKey("NextHeadUpdate", 3)
                        ? Math.max(0, headTag.getInteger("NextHeadUpdate"))
                        : Math.max(0, headTag.getInteger("NextAttack"));
                state.idleAttacks = Math.max(0, headTag.getInteger("IdleAttacks"));
                state.nextClusterTicks = segment.ticksExisted
                        + Math.max(0, headTag.getInteger("NextCluster"));
                state.injuryTicks = Math.max(0, headTag.getInteger("InjuryTicks"));
                state.injuryCooldown = Math.max(0, headTag.getInteger("InjuryCooldown"));
                state.hits = Math.max(0, headTag.getInteger("Hits"));
                state.requiredHits = Math.max(0, headTag.getInteger("RequiredHits"));
                state.nextShake = Math.max(0, headTag.getInteger("NextShake"));
                state.lerpPitchTarget = headTag.getFloat("LerpPitchTarget");
                state.lerpYawTarget = headTag.getFloat("LerpYawTarget");
                state.lerpPitchSteps = Math.max(0, headTag.getInteger("LerpPitchSteps"));
                state.lerpYawSteps = Math.max(0, headTag.getInteger("LerpYawSteps"));
                state.mouth = state.mouthO = headTag.getFloat("MouthAnimation");
                state.brokenJaw = state.brokenJawO = headTag.getFloat("BrokenJawAnimation");
                state.shake = state.shakeO = headTag.getFloat("ShakeAnimation");
                state.distractionTicks = Math.max(0, headTag.getInteger("DistractionTicks"));
                state.distractionUnseenTicks = Math.max(0,
                        headTag.getInteger("DistractionUnseenTicks"));
                state.nextDistractionCheck = Math.max(0, headTag.getInteger("NextDistractionCheck"));
                state.distractionPosition = headTag.hasKey("DistractionX", 6)
                        && headTag.hasKey("DistractionY", 6) && headTag.hasKey("DistractionZ", 6)
                        ? new Vec3d(headTag.getDouble("DistractionX"),
                        headTag.getDouble("DistractionY"), headTag.getDouble("DistractionZ")) : null;
                state.distractionEntity = headTag.hasUniqueId("DistractionEntity")
                        ? headTag.getUniqueId("DistractionEntity") : null;
                segment.setHeadFlag(roarBit(head), headTag.getBoolean("Roaring"));
                segment.setHeadFlag(biteBit(head), headTag.getBoolean("Biting"));
            } else if (timerFormat == 1) {
                state.nextRoarTicks = Math.max(0, source.getInteger("NextRoar" + head));
                state.nextClusterTicks = segment.ticksExisted
                        + Math.max(0, source.getInteger("NextCluster" + head));
                state.roarScheduleInitialized = state.nextRoarTicks > 0;
            }
            // 格式 3 将咆哮条件改为严格超过目标 tick；旧存档需补回一个 tick。
            if (timerFormat < 3 && state.roarScheduleInitialized) ++state.nextRoarTicks;
            segment.setHeadInjuryFlag(head, state.injuryTicks > 0);
            segment.updateHeadRotation(head, state.yaw, state.pitch);
        }
    }

    private static NBTTagCompound writeVector(Vec3d vector) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("X", vector.x);
        tag.setDouble("Y", vector.y);
        tag.setDouble("Z", vector.z);
        return tag;
    }

    private static Vec3d readVector(NBTTagCompound tag) {
        return new Vec3d(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
    }

    private static int roarBit(int head) { return 1 << MathHelper.clamp(head, 0, 2); }
    private static int biteBit(int head) { return 1 << (3 + MathHelper.clamp(head, 0, 2)); }
    private static int shakeBit(int head) { return 1 << (6 + MathHelper.clamp(head, 0, 2)); }
    private static float lerp(float start, float end, float partialTicks) {
        return start + (end - start) * partialTicks;
    }
    private static double lerp(double start, double end, float partialTicks) {
        return start + (end - start) * partialTicks;
    }
    private static float interpolateRotation(float start, float end, float partialTicks) {
        return start + MathHelper.wrapDegrees(end - start) * partialTicks;
    }

    private static final class HeadState {
        Vec3d position = Vec3d.ZERO;
        Vec3d positionO = Vec3d.ZERO;
        EntityLivingBase target;
        int targetUnseenTicks;
        Vec3d lastTargetPosition;
        Vec3d distractionPosition;
        UUID distractionEntity;
        int distractionTicks;
        int distractionUnseenTicks;
        int nextDistractionCheck;
        float yaw;
        float yawO;
        float pitch;
        float pitchO;
        float mouth;
        float mouthO;
        float brokenJaw;
        float brokenJawO;
        float shake;
        float shakeO;
        int roarTicks;
        int biteTicks;
        int nextRoarTicks;
        boolean roarScheduleInitialized;
        int nextHeadUpdate;
        int idleAttacks;
        int nextClusterTicks;
        int injuryTicks;
        int injuryCooldown;
        int hurtOverlayTicks;
        int hits;
        int requiredHits;
        int nextShake;
        float lerpPitchTarget;
        float lerpYawTarget;
        int lerpPitchSteps;
        int lerpYawSteps;
        boolean randomLookInitialized;
        int randomLookTicks;
        double randomLookX;
        double randomLookY;
        double randomLookZ;
        double beamCutoff = -1.0D;

        boolean isDistracted() {
            return distractionTicks > 0 && distractionPosition != null;
        }

        void clearDistraction() {
            distractionPosition = null;
            distractionEntity = null;
            distractionTicks = 0;
            distractionUnseenTicks = 0;
        }
    }
}
