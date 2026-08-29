package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.api.common.event.CanWitherStormTargetMobEvent;
import com.wdcftgg.witherstormmod.common.advancement.ModCriteriaTriggers;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessCapability;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessTracker;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.resource.UpstreamEntityTags;
import com.wdcftgg.witherstormmod.common.util.TractorBeamHelper;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;








public final class WitherStormHeadManager {
    private static final int ENTITY_DISTRACTION_UNSEEN_LIMIT = 180;
    private static final double[][][] OFFSETS = {
            {{0, 3, 0}, {-1.3, 2.2, 0}, {1.3, 2.2, 0}},
            {{0, 3, 0}, {-1.3, 2.2, 0}, {1.3, 2.2, 0}},
            {{0, 2.75, .5}, {-1.3, 2.2, 0}, {1.3, 2.2, 0}},
            {{0, 2.75, .5}, {-1.3, 2.2, 0}, {1.3, 2.2, 0}},
            {{0, 12, 10}, {-12, 22.5, 10}, {8.5, 24.5, 16}},
            {{0, 12, 10}, {-12, 22.5, 10}, {8.5, 24.5, 16}},
            {{0, 12, 10}, {-12, 22.5, 10}, {8.5, 24.5, 16}},
            {{0, 12, 10}, {-12, 22.5, 10}, {8.5, 24.5, 16}}
    };

    private final WitherStormEntity storm;
    private final HeadState[] heads = {new HeadState(), new HeadState(), new HeadState()};

    private List<EntityLivingBase> targetCandidates = java.util.Collections.emptyList();
    private int idleTargetTicks;

    WitherStormHeadManager(WitherStormEntity storm) {
        this.storm = storm;
        for (int i = 0; i < heads.length; i++) {
            heads[i].requiredHits = 1 + storm.getRNG().nextInt(2);
        }
    }

    public void tick() {
        tick(true);
    }






    public void tickWithoutLookAi() {
        tick(false);
    }

    private void tick(boolean runLookAi) {
        int flags = storm.getHeadAnimationFlags();
        for (int index = 0; index < heads.length; index++) {
            HeadState head = heads[index];
            if (storm.world.isRemote && head.hurtOverlayTicks > 0) head.hurtOverlayTicks--;
            head.positionO = head.position;
            head.position = calculatePosition(index);
            head.yawO = head.yaw;
            head.pitchO = head.pitch;
            if (storm.world.isRemote) {



                head.yaw = smoothSyncedRotation(head.yaw, storm.getSyncedHeadYaw(index),
                        storm.getPhase() > 3 ? 5.0F : 8.0F);
                head.pitch = smoothSyncedRotation(head.pitch, storm.getSyncedHeadPitch(index),
                        storm.getPhase() > 3 ? 5.0F : 8.0F);
            } else {
                if (runLookAi) updateLook(index, head);
                else tickHeadLerp(head);
                constrainHeadYaw(index, head);
                storm.updateHeadRotation(index, head.yaw, head.pitch);
            }
            updateBeamCutoff(index, head);
            head.mouthO = head.mouth;
            head.brokenO = head.broken;
            head.shakeO = head.shake;

            boolean roaring = (flags & roarBit(index)) != 0;
            boolean biting = (flags & biteBit(index)) != 0;
            if (!biting && roaring) {
                head.mouth += (1.0F - head.mouth) * 0.15F + 0.04F;
                head.mouth = Math.min(head.mouth, 2.0F);
            } else if (biting) {
                head.mouth += (1.0F - head.mouth) * 0.16F + 0.1F;
                head.mouth = Math.min(head.mouth, 1.4F);
            } else {
                head.mouth += -head.mouth * 0.16F - 0.02F;
                head.mouth = Math.max(head.mouth, 0.0F);
            }
            if (storm.onGround && storm.isDeadOrPlayingDead()) {
                head.broken += (1.0F - head.broken) * 0.2F + 0.05F;
                head.broken = Math.min(head.broken, 1.5F);
            } else {
                head.broken += -head.broken * 0.2F - 0.05F;
                head.broken = Math.max(head.broken, 0.0F);
            }
            if ((flags & shakeBit(index)) != 0) {
                head.shake += 0.02F + storm.getRNG().nextFloat() * 0.05F;
                if (head.shakeO >= 2.0F) {
                    head.shakeO = head.shake = 0.0F;
                    if (!storm.world.isRemote) {
                        storm.setHeadFlag(shakeBit(index), false);
                        head.nextShake = 20 + storm.getRNG().nextInt(20);
                    }
                }
            } else if (head.shake != 0.0F) {
                head.shakeO = head.shake = 0.0F;
            }
        }
        if (!storm.world.isRemote && runLookAi) serverTick();
    }

    private void serverTick() {
        if (storm.getInvulnerableTicks() > 0 || storm.isDeadOrPlayingDead()) {
            for (int index = 0; index < heads.length; index++) {
                heads[index].target = null;
                storm.updateWatchedTargetId(index, 0);
            }
            targetCandidates = java.util.Collections.emptyList();
            storm.setAttackTarget(null);
            idleTargetTicks = 0;
            logPlayerTargetingDiagnostics("风暴无敌、死亡或装死，头部选敌未运行");
            return;
        }
        tickDistractions();

        targetCandidates = scanTargetCandidates();
        for (int index = 0; index < heads.length; index++) {
            HeadState head = heads[index];
            boolean enabled = isEnabled(index);
            if (head.injuryTicks > 0) {
                if (--head.injuryTicks == 0) {
                    storm.setHeadInjuryFlag(index, false);
                    if (!storm.isDeadOrPlayingDead()) storm.playHeadTractorBeamActivationSound(index);
                } else if (head.nextShake > 0 && --head.nextShake == 0) {
                    storm.setHeadFlag(shakeBit(index), true);
                }
            }
            if (head.injuryCooldown > 0) head.injuryCooldown--;

            EntityLivingBase target = head.injuryTicks > 0 || head.isDistracted() ? null
                    : selectTarget(index, targetCandidates);
            if (head.target != target) {
                StormDiagnosticLogger.info(
                        "[风暴诊断][主体头目标切换] 风暴={} 阶段={} tick={} 头={} 原目标={} 新目标={} 启用={} 受伤剩余={} 分心剩余={}",
                        storm.getUniqueID(), storm.getPhase(), storm.ticksExisted, index,
                        describeEntity(head.target), describeEntity(target), enabled,
                        head.injuryTicks, head.distractionTicks);
                head.target = target;
                head.targetUnseenTicks = 0;
                head.lastTargetPosition = null;
            }
            if (index == 0 && storm.getAttackTarget() != target) storm.setAttackTarget(target);
            storm.updateWatchedTargetId(index, target == null || !enabled ? 0 : target.getEntityId());

            if (storm.isHeadFlagSet(roarBit(index)) && ++head.roarTicks > 40) {
                head.roarTicks = 0;
                storm.setHeadFlag(roarBit(index), false);
            }
            if (storm.isHeadFlagSet(biteBit(index)) && ++head.biteTicks > 10) {
                head.biteTicks = 0;
                storm.setHeadFlag(biteBit(index), false);
                storm.playHeadBiteSound(index);
            }
            if (!head.roarScheduleInitialized) {
                head.nextRoar = 201 + storm.getRNG().nextInt(200);
                head.roarScheduleInitialized = true;
            } else if (--head.nextRoar <= 0) {
                head.nextRoar = nextRoarDelay() + 1;
                boolean canShootFlamingSkull = storm.tractorBeamActive(index)
                        || index == 0 && storm.getPhase() < 4 && target != null;
                if (canShootFlamingSkull && !storm.isAttractingFormidibomb()) {
                    Vec3d look = getLookVector(head);
                    storm.spawnFlamingWitherSkull(index, head.position.x + look.x,
                            head.position.y + look.y, head.position.z + look.z);
                }
                startRoar(index, head.injuryTicks > 0);
            }
            if (storm.tractorBeamActive(index) && storm.getPhase() >= 2
                    && storm.ticksExisted >= head.nextClusterPickup) {
                head.nextClusterPickup = storm.ticksExisted + nextClusterPickupDelay();
                storm.createClusterFromLook(head.pitch, head.yaw, storm.getClusterRadius(), index);
                storm.removeFluidFromLook(head.pitch, head.yaw, index);
            }
            if (!enabled || head.injuryTicks > 0 || storm.isDeadOrPlayingDead()) continue;

            if (storm.ticksExisted < head.nextHeadUpdate) continue;
            if (storm.getPhase() < 4) {
                head.nextHeadUpdate = storm.ticksExisted + 10 + storm.getRNG().nextInt(10);
            } else {
                head.nextHeadUpdate = storm.ticksExisted + 1200 + storm.getRNG().nextInt(120);
            }
            int idleHeadUpdates = head.idleAttacks++;
            if (idleHeadUpdates > 15) {
                if (!storm.tractorBeamActive(index)) {
                    if (storm.getPhase() < 4) {
                        performEarlyPhaseRangedAttack(index, head, true);
                    } else {
                        Vec3d origin = head.position;
                        storm.performRangedAttack(index,
                                WitherStormPartLogic.randomBetween(
                                        storm.getRNG(), origin.x - 10.0D, origin.x + 10.0D),
                                WitherStormPartLogic.randomBetween(
                                        storm.getRNG(), origin.y - 5.0D, origin.y + 5.0D),
                                WitherStormPartLogic.randomBetween(
                                        storm.getRNG(), origin.z - 10.0D, origin.z + 10.0D), true);
                    }
                }
                head.idleAttacks = 0;
            }
            if (target != null && !head.isDistracted()) {
                if (!storm.tractorBeamActive(index)) {
                    if (storm.getPhase() < 4) {
                        performEarlyPhaseRangedAttack(index, head,
                                index == 0 && storm.getRNG().nextFloat() < 0.001F);
                    } else {
                        storm.performRangedAttack(index, target);
                    }
                }
                head.nextHeadUpdate = storm.ticksExisted + (storm.getPhase() < 4
                        ? 40 + storm.getRNG().nextInt(20)
                        : 1800 + storm.getRNG().nextInt(160));
                head.idleAttacks = 0;
            } else {
                head.nextHeadUpdate = storm.ticksExisted + 40 + storm.getRNG().nextInt(20);
            }
        }
        logPlayerTargetingDiagnostics(null);
        tickMainTargetTimeout();
    }

    private void performEarlyPhaseRangedAttack(int index, HeadState head, boolean dangerous) {
        Vec3d look = getLookVector(head);
        storm.performRangedAttack(index, head.position.x + look.x,
                head.position.y + look.y, head.position.z + look.z, dangerous);
    }

    private void tickMainTargetTimeout() {
        if (heads[0].target != null) ++idleTargetTicks;
        if (idleTargetTicks > 1800 || heads[0].target == null) {
            setTarget(0, null);
            idleTargetTicks = 0;
        }
    }


    private void tickDistractions() {
        for (int index = 0; index < heads.length; index++) {
            HeadState head = heads[index];
            if (head.distractionTicks > 0) {
                tickActiveDistraction(index, head);
                continue;
            }
            clearDistraction(index, head);
            if (head.nextDistractionCheck > 0) head.nextDistractionCheck--;
            if (!canStartEntityDistraction(index)) continue;

            if (storm.getRNG().nextInt(2) == 0) {
                EntityFireworkRocket firework = findFirework(index);
                if (firework != null) {
                    startEntityDistraction(index, head, firework);
                    continue;
                }
            }

            if (head.nextDistractionCheck > 0 || !canStartBlockDistraction(index)) continue;
            BlockPos block = findDistractionBlock(index);
            if (block != null) {
                Vec3d position = new Vec3d(block).add(0.5D, 0.5D, 0.5D);
                if (storm.isPositionBehindBack(position)) continue;
                boolean overlapsOtherHead = false;
                for (int otherIndex = 0; otherIndex < heads.length; otherIndex++) {
                    Vec3d otherPosition = heads[otherIndex].distractionPosition;
                    if (otherIndex != index && otherPosition != null
                            && otherPosition.squareDistanceTo(position) < 100.0D
                            && storm.getRNG().nextInt(5) != 0) {
                        overlapsOtherHead = true;
                        break;
                    }
                }
                if (overlapsOtherHead) {
                    head.nextDistractionCheck = 60;
                    continue;
                }
                head.distractionPosition = position;
                head.distractionTicks = 120 + storm.getRNG().nextInt(60);
                storm.setHeadDistractionFlag(index, true);
                StormDiagnosticLogger.info(
                        "[风暴诊断][主体头分心开始] 风暴={} 阶段={} tick={} 头={} 类型=方块 位置={} 时长={} 原目标={}",
                        storm.getUniqueID(), storm.getPhase(), storm.ticksExisted, index,
                        position, head.distractionTicks, describeEntity(head.target));
            }
        }
    }

    private void tickActiveDistraction(int index, HeadState head) {
        --head.distractionTicks;
        if (head.distractionEntity == null) {
            if (head.distractionTicks == 0) clearDistraction(index, head);
            return;
        }

        Entity distractionEntity = resolveEntity(head.distractionEntity);
        if (distractionEntity == null || distractionEntity.isDead) {
            if (head.distractionPosition != null && storm.getRNG().nextInt(8) == 0) {
                head.distractionPosition = head.distractionPosition.add(storm.getRNG().nextGaussian(),
                        storm.getRNG().nextGaussian(), storm.getRNG().nextGaussian());
            }
            EntityFireworkRocket replacement = canStartEntityDistraction(index) ? findFirework(index) : null;
            if (replacement != null && !replacement.getUniqueID().equals(head.distractionEntity)) {
                startEntityDistraction(index, head, replacement);
            } else if (head.distractionTicks == 0) {
                clearDistraction(index, head);
            }
            return;
        }

        if (head.distractionTicks > 0) return;
        double followDistance = storm.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        if (storm.getDistanceSq(distractionEntity) > followDistance * followDistance) {
            clearDistraction(index, head);
            return;
        }
        if (storm.canSeeWithCache(index, distractionEntity)) {
            head.distractionUnseenTicks = 0;
        } else if (head.distractionUnseenTicks++ > ENTITY_DISTRACTION_UNSEEN_LIMIT) {
            clearDistraction(index, head);
            return;
        }
        head.distractionPosition = distractionEntity.getPositionVector().add(0.0D, 10.0D, 0.0D);
        head.distractionTicks = 80 + storm.getRNG().nextInt(80);
    }

    private void startEntityDistraction(int index, HeadState head, EntityFireworkRocket firework) {
        head.distractionEntity = firework.getUniqueID();
        head.distractionPosition = firework.getPositionVector().add(0.0D, 10.0D, 0.0D);
        head.distractionTicks = 80 + storm.getRNG().nextInt(80);
        head.distractionUnseenTicks = 0;
        storm.setHeadDistractionFlag(index, true);
        StormDiagnosticLogger.info(
                "[风暴诊断][主体头分心开始] 风暴={} 阶段={} tick={} 头={} 类型=实体 实体={} 位置={} 时长={} 原目标={}",
                storm.getUniqueID(), storm.getPhase(), storm.ticksExisted, index,
                describeEntity(firework), head.distractionPosition, head.distractionTicks,
                describeEntity(head.target));
    }

    private void clearDistraction(int index, HeadState head) {
        boolean wasDistracted = head.isDistracted() || head.distractionEntity != null
                || head.distractionPosition != null;
        Vec3d oldPosition = head.distractionPosition;
        java.util.UUID oldEntity = head.distractionEntity;
        head.clearDistraction();
        storm.setHeadDistractionFlag(index, false);
        if (wasDistracted) {
            StormDiagnosticLogger.info(
                    "[风暴诊断][主体头分心结束] 风暴={} 阶段={} tick={} 头={} 实体UUID={} 位置={}",
                    storm.getUniqueID(), storm.getPhase(), storm.ticksExisted, index,
                    oldEntity, oldPosition);
        }
    }

    private boolean canStartEntityDistraction(int index) {
        HeadState head = heads[index];
        return storm.getPhase() > 3 && !storm.isDeadOrPlayingDead() && head.injuryTicks <= 0
                && storm.tractorBeamActive(index);
    }

    private boolean canStartBlockDistraction(int index) {
        if (!canStartEntityDistraction(index)) return false;
        HeadState head = heads[index];
        EntityLivingBase target = head.target;
        return target == null || !TractorBeamHelper.isInsideTractorBeam(target.getPositionVector(),
                head.position, getLookVector(head), head.beamCutoff, 4.0D);
    }

    @Nullable
    private EntityFireworkRocket findFirework(int index) {
        HeadState head = heads[index];
        double followDistance = storm.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        List<EntityFireworkRocket> fireworks = storm.world.getEntitiesWithinAABB(EntityFireworkRocket.class,
                storm.getSearchBox(), entity -> !entity.isDead && storm.canSeeWithCache(index, entity)
                        && !storm.isEntityBehindBack(entity)
                        && storm.getDistanceSq(entity) <= followDistance * followDistance);
        EntityFireworkRocket nearest = null;
        double distance = Double.MAX_VALUE;
        for (EntityFireworkRocket firework : fireworks) {
            double current = firework.getDistanceSq(head.position.x, head.position.y, head.position.z);
            if (current < distance) {
                distance = current;
                nearest = firework;
            }
        }
        return nearest;
    }

    @Nullable
    private BlockPos findDistractionBlock(int index) {
        if (!ForgeEventFactory.getMobGriefingEvent(storm.world, storm)) return null;
        HeadState head = heads[index];
        Vec3d end = head.position.add(getLookVector(head).scale(200.0D));
        RayTraceResult hit = storm.world.rayTraceBlocks(head.position, end, false, true, false);
        BlockPos beamEnd = hit == null || hit.typeOfHit == RayTraceResult.Type.MISS
                ? new BlockPos(end) : hit.getBlockPos();
        BlockPos origin = beamEnd.add(storm.getRNG().nextInt(9) - 4,
                storm.getRNG().nextInt(9) - 4, storm.getRNG().nextInt(9) - 4);
        if (!storm.world.isBlockLoaded(origin)) return null;
        int searchRadius = Math.max(4, WitherStormConfig.tractorBeamBlockSearchRadius);
        return WorldUtil.findLoadedBlockSpiralOutwards(storm.world, origin, searchRadius,
                state -> UpstreamBlockTags.contains(
                        UpstreamBlockTags.TRACTOR_BEAM_DISTRACTION_BLOCKS, state));
    }

    @Nullable
    private Entity resolveEntity(@Nullable java.util.UUID uuid) {
        if (uuid == null) return null;

        if (storm.world instanceof WorldServer) {
            Entity resolved = ((WorldServer) storm.world).getEntityFromUuid(uuid);
            if (resolved != null) return resolved;
        }
        for (Entity entity : storm.world.loadedEntityList) {
            if (uuid.equals(entity.getUniqueID())) return entity;
        }
        return null;
    }

    private int nextClusterPickupDelay() {
        int phase = storm.getPhase();
        if (phase <= 2) return 24;
        if (phase == 3) return 15;
        if (phase == 4) return 5 + storm.getRNG().nextInt(20);
        if (phase == 5) return 5 + storm.getRNG().nextInt(15);
        return storm.getRNG().nextInt(15);
    }

    private int nextRoarDelay() {
        int minimum = Math.max(1, WitherStormConfig.minimumRoarInterval) * 20;
        int maximum = Math.max(minimum, WitherStormConfig.maximumRoarInterval * 20);
        return minimum + (maximum > minimum ? storm.getRNG().nextInt(maximum - minimum) : 0);
    }

    private void updateBeamCutoff(int index, HeadState head) {
        if (storm.world.isRemote || head.position == null) return;
        if (!storm.tractorBeamActive(index)) {
            head.beamCutoff = -1.0D;
            return;
        }
        head.beamCutoff = TractorBeamHelper.findCutoffDistance(
                storm.world, head.position, getLookVector(head), 250.0D);
    }

    @Nullable
    private EntityLivingBase selectTarget(int index, @Nullable List<EntityLivingBase> candidates) {
        if (!isEnabled(index)) return null;
        if (index == 0 && storm.isAttractingFormidibomb()) return null;
        HeadState head = heads[index];
        if (head.isDistracted()) return null;

        EntityLivingBase revengeTarget = index == 0 ? storm.getRevengeTarget() : null;
        if (revengeTarget != null && revengeTarget != head.target
                && isRevengeTargetApplicable(revengeTarget)) {
            countWitherSicknessContact(revengeTarget);
            return revengeTarget;
        }
        if (canContinueTarget(index, head)) return head.target;
        if (candidates == null) return null;

        boolean preferSpecialTarget = WitherStormConfig.specialTargetingBias
                && storm.getPlayingJukeboxes().isEmpty()
                && storm.getRNG().nextInt(100) <= MathHelper.clamp(
                WitherStormConfig.specialTargetingBiasChance, 0, 100);
        EntityLivingBase nearest = null;
        EntityLivingBase nearestSpecialTarget = null;
        double distance = Double.MAX_VALUE;
        double specialTargetDistance = Double.MAX_VALUE;
        for (EntityLivingBase candidate : candidates) {
            if (!isTargetApplicableForHead(index, candidate)) continue;
            double current = candidate.getDistanceSq(
                    storm.posX, storm.posY + storm.getEyeHeight(), storm.posZ);
            if (preferSpecialTarget
                    && (UpstreamEntityTags.contains(UpstreamEntityTags.FAVOURABLE_MOBS, candidate)
                    || candidate instanceof EntityPlayer)
                    && current < specialTargetDistance) {
                specialTargetDistance = current;
                nearestSpecialTarget = candidate;
            }
            if (candidate.getEntityBoundingBox().getAverageEdgeLength() <= 0.5D
                    && candidate.getRNG().nextInt(4) != 0) continue;
            if (current < distance) {
                distance = current;
                nearest = candidate;
            }
        }
        EntityLivingBase selected = nearestSpecialTarget == null ? nearest : nearestSpecialTarget;
        if (StormDiagnosticLogger.isEnabled() && (selected != null || storm.ticksExisted % 20 == 0)) {
            StormDiagnosticLogger.info(
                    "[风暴诊断][主体头选敌结果] 风暴={} 阶段={} tick={} 头={} 候选数量={} 启用特殊偏置={} 最近普通={} 最近特殊={} 最终选择={}",
                    storm.getUniqueID(), storm.getPhase(), storm.ticksExisted, index,
                    candidates.size(), preferSpecialTarget, describeEntity(nearest),
                    describeEntity(nearestSpecialTarget), describeEntity(selected));
        }
        if (selected != null) countWitherSicknessContact(selected);
        return selected;
    }


    private List<EntityLivingBase> scanTargetCandidates() {
        double range = storm.getPhase() > 3
                ? storm.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue()
                : 40.0D;
        return storm.world.getEntitiesWithinAABB(EntityLivingBase.class,
                storm.getEntityBoundingBox().grow(range,
                        storm.getPhase() > 3 ? range + 50.0D : range * 2.0D,
                        range), this::isTargetApplicableUnfiltered);
    }

    private boolean isRevengeTargetApplicable(EntityLivingBase entity) {
        double followDistance = storm.getEntityAttribute(
                SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue() + 100.0D;
        return storm.isValidStormTarget(entity)
                && !storm.isOnSameTeam(entity)
                && storm.getDistanceSq(entity) <= followDistance * followDistance
                && storm.canSeeWithCache(0, entity);
    }

    private boolean canContinueTarget(int index, HeadState head) {
        EntityLivingBase target = head.target;
        if (target == null) return false;
        if (!target.isEntityAlive()) return rejectContinuedTarget(index, target, "目标死亡");
        if (target.world != storm.world || target.dimension != storm.dimension) {
            return rejectContinuedTarget(index, target, "世界或维度不同");
        }
        if (storm.isOnSameTeam(target)) return rejectContinuedTarget(index, target, "目标变为同队");
        double followDistance = storm.getEntityAttribute(
                SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue() + 100.0D;
        if (storm.getDistanceSq(target) > followDistance * followDistance) {
            return rejectContinuedTarget(index, target, "超出持续目标距离");
        }
        if (storm.canSeeWithCache(index, target)) {
            head.targetUnseenTicks = 0;
        } else if (++head.targetUnseenTicks > (storm.getPhase() < 4 ? 80 : 20)) {
            return rejectContinuedTarget(index, target,
                    "失去视线超过容忍时间，未见tick=" + head.targetUnseenTicks);
        }
        if (target instanceof EntityPlayer
                && (((EntityPlayer) target).capabilities.disableDamage
                || ((EntityPlayer) target).isSpectator())) {
            return rejectContinuedTarget(index, target, "玩家切换为无敌或旁观模式");
        }
        if (storm.getPhase() > 3 && storm.isEntityBehindBack(target)) {
            return rejectContinuedTarget(index, target, "阶段4+目标进入风暴背后");
        }
        Vec3d position = target.getPositionVector();
        if (head.lastTargetPosition != null
                && position.distanceTo(head.lastTargetPosition) > 20.0D) {
            return rejectContinuedTarget(index, target, "单tick位移超过20格");
        }
        if (storm.isTrackedForConsumption(target)) {
            return rejectContinuedTarget(index, target, "目标进入吞噬追踪");
        }
        head.lastTargetPosition = position;
        return true;
    }

    private boolean rejectContinuedTarget(int index, EntityLivingBase target, String reason) {
        StormDiagnosticLogger.info(
                "[风暴诊断][主体头持续目标拒绝] 风暴={} 阶段={} tick={} 头={} 目标={} 原因={}",
                storm.getUniqueID(), storm.getPhase(), storm.ticksExisted, index,
                describeEntity(target), reason);
        return false;
    }

    private static void countWitherSicknessContact(EntityLivingBase target) {
        WitherSicknessTracker tracker = WitherSicknessCapability.get(target);
        if (tracker != null) tracker.countContact();
    }


    private boolean isTargetApplicableUnfiltered(EntityLivingBase entity) {
        if (entity == null || !storm.isValidStormTarget(entity)
                || storm.isOnSameTeam(entity)
                || storm.getIgnoredTargetsManager().shouldIgnoreTarget(entity)
                || storm.isTrackedForConsumption(entity)
                || storm.isPassengerTarget(entity)) {
            return false;
        }
        if (storm.getPhase() > 3 && (entity.isInvisible() || storm.isTargetInUseBySegment(entity))) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player.isHandActive() && player.getActiveItemStack().getItem() == Items.SHIELD) return false;
        }
        boolean cancelled = MinecraftForge.EVENT_BUS.post(new CanWitherStormTargetMobEvent(storm, entity));
        if (cancelled && entity instanceof EntityPlayer && storm.ticksExisted % 20 == 0) {
            StormDiagnosticLogger.info(
                    "[风暴诊断][主体玩家目标事件拒绝] 风暴={} 阶段={} tick={} 玩家={} 玩家UUID={}",
                    storm.getUniqueID(), storm.getPhase(), storm.ticksExisted,
                    entity.getName(), entity.getUniqueID());
        }
        return !cancelled;
    }


    private boolean isTargetApplicableForHead(int index, EntityLivingBase entity) {
        if (!storm.canSeeWithCache(index, entity)) return false;


        if (storm.isInsideOtherTractorBeam(entity, index)) return false;
        if (storm.getPhase() > 3 && (isTargetedByAnotherHead(entity, index)
                || storm.isEntityBehindBack(entity))) {
            return false;
        }
        return true;
    }


    private void logPlayerTargetingDiagnostics(@Nullable String globalReason) {
        if (!StormDiagnosticLogger.isEnabled() || storm.ticksExisted % 20 != 0) return;
        double range = storm.getPhase() > 3
                ? storm.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue()
                : 40.0D;
        AxisAlignedBB search = storm.getEntityBoundingBox().grow(range,
                storm.getPhase() > 3 ? range + 50.0D : range * 2.0D, range);
        for (EntityPlayer player : storm.world.playerEntities) {
            boolean inSearch = search.intersects(player.getEntityBoundingBox());
            long protection = SymbiontSummoningManager.getIgnoreTicksRemaining(player);
            for (int index = 0; index < heads.length; index++) {
                HeadState head = heads[index];
                String result = globalReason == null
                        ? describePlayerTargetRejection(index, player, inSearch, protection)
                        : globalReason;
                StormDiagnosticLogger.info(
                        "[风暴诊断][主体头玩家判定] 风暴={} 阶段={} tick={} 玩家={} 玩家UUID={} 维度={} 距离平方={} 搜索范围内={} 共享候选={} 头={} 结果={} 当前目标={} 光束启用={} 受伤剩余={} 分心剩余={} 保护剩余={} yaw={} pitch={} cutoff={}",
                        storm.getUniqueID(), storm.getPhase(), storm.ticksExisted,
                        player.getName(), player.getUniqueID(), player.dimension,
                        storm.getDistanceSq(player), inSearch, targetCandidates.contains(player),
                        index, result, describeEntity(head.target), storm.tractorBeamActive(index),
                        head.injuryTicks, head.distractionTicks, protection,
                        head.yaw, head.pitch, head.beamCutoff);
            }
        }
    }

    private String describePlayerTargetRejection(int index, EntityPlayer player,
                                                  boolean inSearch, long protection) {
        HeadState head = heads[index];
        if (!isEnabled(index)) return "头部未启用";
        if (head.injuryTicks > 0) return "头部受伤";
        if (head.isDistracted()) return "头部正在分心";
        if (index == 0 && storm.isAttractingFormidibomb()) return "主头正在吸引恐怖炸弹";
        if (!inSearch) return "目标搜索范围外";
        if (!player.isEntityAlive()) return "玩家已死亡";
        if (player.world != storm.world || player.dimension != storm.dimension) return "世界或维度不同";
        if (player.capabilities.disableDamage) return "玩家处于无敌模式";
        if (player.isSpectator()) return "玩家处于旁观模式";
        if (storm.hasRecentlyBeenRevived()) return "风暴刚复活，暂不选玩家";
        if (protection > 0L) return "玩家目标保护剩余" + protection + "tick";
        if (storm.isOnSameTeam(player)) return "玩家与风暴同队";
        if (storm.getIgnoredTargetsManager().shouldIgnoreTarget(player)) return "忽略目标管理器拒绝";
        if (storm.isTrackedForConsumption(player)) return "玩家已进入吞噬追踪";
        if (storm.isPassengerTarget(player)) return "玩家是风暴家族附近乘客目标";
        if (storm.getPhase() > 3 && player.isInvisible()) return "阶段4+玩家隐身";
        if (storm.getPhase() > 3 && storm.isTargetInUseBySegment(player)) return "玩家已被分体头占用";
        if (player.isHandActive() && player.getActiveItemStack().getItem() == Items.SHIELD) return "玩家正在举盾";
        if (!storm.canSeeWithCache(index, player)) return "该头没有视线";
        if (storm.isInsideOtherTractorBeam(player, index)) return "玩家已在其他光束内";
        if (storm.getPhase() > 3 && isTargetedByAnotherHead(player, index)) return "玩家已被主体其他头占用";
        if (storm.getPhase() > 3 && storm.isEntityBehindBack(player)) return "玩家位于风暴背后";
        return "基础条件通过（事件总线仍可取消）";
    }

    private static String describeEntity(@Nullable Entity entity) {
        return entity == null ? "无" : entity.getName() + "#" + entity.getEntityId()
                + "/" + entity.getUniqueID();
    }

    private void updateLook(int index, HeadState head) {
        if (storm.getHealth() <= 0.0F) {

            if (!storm.onGround) head.deathPitchSteps = 64;
            if (head.deathPitchSteps > 0) {
                head.pitch += MathHelper.wrapDegrees(-50.0F - head.pitch) / head.deathPitchSteps;
                head.deathPitchSteps--;
            }
            return;
        }
        head.deathPitchSteps = 0;
        if (storm.isPlayDeadAiDisabled()) {
            tickHeadLerp(head);
            return;
        }
        head.lerpPitchSteps = 0;
        head.lerpYawSteps = 0;
        PowerfulExplosiveEntity.FormidibombEntity formidibomb = index == 0
                && storm.isAttractingFormidibomb() ? storm.getFormidibomb() : null;
        if (formidibomb != null && !formidibomb.isDead) {
            lookAtPosition(index, head, new Vec3d(formidibomb.posX,
                    formidibomb.posY + formidibomb.getEyeHeight(), formidibomb.posZ), 30.0F, 1);
            return;
        }
        if (head.explicitLookPosition != null) {
            lookAtPosition(index, head, head.explicitLookPosition, 10.0F,
                    head.explicitLookSteps);
            return;
        }
        if (head.isDistracted() && head.distractionPosition != null) {
            lookAtPosition(index, head, head.distractionPosition, 10.0F, 10);
            return;
        }
        EntityLivingBase target = head.target;
        if (target == null && storm.world.isRemote) {
            int id = storm.getWatchedTargetId(index);
            Entity entity = id > 0 ? storm.world.getEntityByID(id) : null;
            if (entity instanceof EntityLivingBase) target = (EntityLivingBase) entity;
        }
        if (target != null) {
            lookAtPosition(index, head, new Vec3d(target.posX,
                    target.posY + target.getEyeHeight(), target.posZ),


                    10.0F,

                    storm.getPhase() > 3 ? 50 : 3);
        } else {
            lookAtPosition(index, head, getRandomLookPosition(head),

                    10.0F,

                    storm.getPhase() < 4 || head.injuryTicks > 0 ? 3 : 50);
        }
    }

    private void lookAtPosition(int index, HeadState head, Vec3d position,
                                float mainHeadMaximumRotation, int additionalHeadSteps) {
        double dx = position.x - head.position.x;
        double dy = position.y - head.position.y;
        double dz = position.z - head.position.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float wantedYaw = (float) (MathHelper.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float wantedPitch = (float) (-(MathHelper.atan2(dy, horizontal) * 180.0D / Math.PI));
        if (index == 0) {
            head.yaw = rotlerp(head.yaw, wantedYaw, mainHeadMaximumRotation);
            head.pitch = rotlerp(head.pitch, wantedPitch, mainHeadMaximumRotation);
            return;
        }
        int steps = Math.max(1, additionalHeadSteps);
        head.yaw += MathHelper.wrapDegrees(wantedYaw - head.yaw) / steps;
        head.pitch += MathHelper.wrapDegrees(wantedPitch - head.pitch) / steps;
    }





    private void constrainHeadYaw(int index, HeadState head) {
        head.yaw = WitherStormHeadYawConstraint.constrain(storm.getPhase(), index,
                storm.isDeadOrPlayingDead(), head.yaw, storm.renderYawOffset);
    }

    private Vec3d getRandomLookPosition(HeadState head) {
        if (!head.randomLookInitialized || head.randomLookTicks < 0) {
            float pitch = MathHelper.clamp(-storm.getRNG().nextInt(180), -140, -30)
                    * 0.017453292F;
            float yaw = (MathHelper.wrapDegrees(storm.renderYawOffset) + 90.0F
                    + MathHelper.clamp(storm.getRNG().nextInt(360) - 180, -80, 80))
                    * 0.017453292F;
            head.randomLookX = Math.cos(yaw) * 30.0D;
            head.randomLookY = Math.sin(pitch) * 30.0D;
            head.randomLookZ = Math.sin(yaw) * 30.0D;
            head.randomLookTicks = (storm.getPhase() < 4 || head.injuryTicks > 0 ? 20 : 120)
                    + storm.getRNG().nextInt(20);
            head.randomLookInitialized = true;
        }
        --head.randomLookTicks;
        return head.position.add(head.randomLookX, head.randomLookY, head.randomLookZ);
    }

    private void tickHeadLerp(HeadState head) {
        if (head.lerpPitchSteps > 0) {
            head.pitch += MathHelper.wrapDegrees(head.lerpPitchTarget - head.pitch) / head.lerpPitchSteps;
            --head.lerpPitchSteps;
        }
        if (head.lerpYawSteps > 0) {
            head.yaw += MathHelper.wrapDegrees(head.lerpYawTarget - head.yaw) / head.lerpYawSteps;
            --head.lerpYawSteps;
        }
    }

    private void lerpHeadTo(HeadState head, float pitch, float yaw, int steps) {
        head.lerpPitchTarget = pitch;
        head.lerpYawTarget = yaw;
        head.lerpPitchSteps = Math.max(0, steps);
        head.lerpYawSteps = Math.max(0, steps);
    }

    private Vec3d getLookVector(HeadState head) {
        float pitch = head.pitch * 0.017453292F;
        float yaw = head.yaw * 0.017453292F;
        float horizontal = MathHelper.cos(pitch);
        return new Vec3d(-MathHelper.sin(yaw) * horizontal, -MathHelper.sin(pitch), MathHelper.cos(yaw) * horizontal).normalize();
    }

    public Vec3d getLookVector(int index) {
        return getLookVector(heads[head(index)]);
    }

    public Vec3d getLookVector(int index, float partialTicks) {
        float pitch = getPitch(index, partialTicks) * 0.017453292F;
        float yaw = getYaw(index, partialTicks) * 0.017453292F;
        float horizontal = MathHelper.cos(pitch);
        return new Vec3d(-MathHelper.sin(yaw) * horizontal,
                -MathHelper.sin(pitch), MathHelper.cos(yaw) * horizontal).normalize();
    }

    public double getTractorBeamCutoff(int index) {
        return heads[head(index)].beamCutoff;
    }

    public void onPhaseChanged(int phase) {
        for (HeadState head : heads) {
            head.requiredHits = phase > 3 ? 3 + storm.getRNG().nextInt(3) : 1 + storm.getRNG().nextInt(2);
        }
    }

    public void onOtherHeadsEnabled() {
        for (int index = 1; index < heads.length; index++) {
            heads[index].nextRoar = storm.getRNG().nextInt(30) + 1;
            heads[index].roarScheduleInitialized = true;
        }
    }


    void initializeAdditionalHeadYaw(float yaw) {
        for (int index = 1; index < heads.length; index++) {
            HeadState head = heads[index];
            head.yaw = yaw;
            head.yawO = yaw;
            storm.updateHeadRotation(index, yaw, head.pitch);
        }
    }

    private boolean isEnabled(int index) { return index == 0 || !storm.areOtherHeadsDisabled(); }

    public void startBiting(int index) {
        index = head(index);
        heads[index].biteTicks = 0;
        storm.setHeadFlag(biteBit(index), true);
    }

    public void startRoar(int index) {
        startRoar(index, false);
    }

    private void startRoar(int index, boolean screaming) {
        index = head(index);
        storm.setHeadFlag(roarBit(index), true);
        if (screaming) storm.playHeadHurtSound(index);
        else storm.playHeadRoarSound(index);
    }

    public boolean isDistracted(int index) { return heads[head(index)].isDistracted(); }
    @Nullable public Vec3d getDistractedPos(int index) { return heads[head(index)].distractionPosition; }
    public void setDistractedPos(int index, @Nullable Vec3d position) {
        HeadState head = heads[head(index)];
        head.distractionPosition = position;
        if (position == null) clearDistraction(head(index), head);
        else storm.setHeadDistractionFlag(index, true);
    }
    public void makeDistracted(int index, Vec3d position, int ticks) {
        HeadState head = heads[head(index)];
        head.distractionEntity = null;
        head.distractionPosition = position;
        head.distractionTicks = Math.max(1, ticks);
        head.distractionUnseenTicks = 0;
        storm.setHeadDistractionFlag(index, true);
    }

    public void setLookAt(int index, @Nullable Vec3d position, int steps) {
        HeadState head = heads[head(index)];
        head.explicitLookPosition = position;
        head.explicitLookSteps = Math.max(1, steps);
    }

    public void onHurt() {
        heads[1].idleAttacks += 3;
        heads[2].idleAttacks += 3;
    }

    public void delayAfterChomp(int index) {
        HeadState state = heads[head(index)];
        state.nextHeadUpdate = storm.ticksExisted + storm.getRNG().nextInt(20)
                + storm.getRNG().nextInt(60);
    }

    public void onStartFalling() {
        for (int index = 0; index < heads.length; index++) {
            lerpHeadTo(heads[index], -50.0F, storm.renderYawOffset, 64);
            startRoar(index, storm.getRNG().nextBoolean());
        }
    }

    public void onStartPlayingDead() {
        for (HeadState head : heads) lerpHeadTo(head, 40.0F, storm.renderYawOffset, 16);
    }

    public void restorePlayDeadPose(WitherStormEntity.PlayDeadState state) {
        if (state == WitherStormEntity.PlayDeadState.FALLING) {
            for (HeadState head : heads) lerpHeadTo(head, -50.0F, storm.renderYawOffset, 64);
        } else if (state == WitherStormEntity.PlayDeadState.PLAYING_DEAD) {
            onStartPlayingDead();
        }
    }

    public void onAiRestored() {
        for (int index = 0; index < heads.length; index++) {
            startRoar(index, storm.getRNG().nextBoolean());
        }
    }

    public void onDeath() {
        for (int index = 0; index < heads.length; index++) {
            storm.setHeadFlag(roarBit(index), true);
            storm.playHeadRoarSound(index);
        }
    }

    public boolean attemptAttack(int index, @Nullable Entity attacker, int attemptCooldown) {
        if (!WitherStormConfig.canAttackHeads || storm.isDeadOrPlayingDead()) return false;
        index = head(index);
        HeadState state = heads[index];
        if (state.injuryCooldown > 0 || state.injuryTicks > 0) return false;
        state.injuryCooldown = Math.max(1, attemptCooldown);
        countAttack(index, attacker);
        return true;
    }

    public boolean attackFromExplosion(int index, @Nullable Entity attacker) {
        if (!WitherStormConfig.canAttackHeads || storm.isDeadOrPlayingDead()) return false;
        index = head(index);
        if (heads[index].injuryTicks > 0) return false;
        countAttack(index, attacker);
        return true;
    }

    private boolean countAttack(int index, @Nullable Entity attacker) {
        HeadState state = heads[index];
        ModNetwork.notifyHeadAttacked(storm, index);
        state.hits++;
        storm.setHeadFlag(shakeBit(index), true);
        if (state.hits < requiredHits(index)) {
            storm.setHeadFlag(roarBit(index), true);
            state.roarTicks = 20;
            storm.playHeadHurtSound(index);
            return false;
        }
        hurt(index, attacker);
        return true;
    }

    private int requiredHits(int index) {
        HeadState state = heads[index];
        if (state.requiredHits <= 0) state.requiredHits = storm.getPhase() > 3 ? 3 + storm.getRNG().nextInt(3) : 1 + storm.getRNG().nextInt(2);
        return state.requiredHits;
    }

    private void hurt(int index, @Nullable Entity attacker) {
        HeadState state = heads[index];
        boolean attackerWasTarget = attacker instanceof EntityPlayerMP
                && isTargetedByAnyHead((EntityPlayerMP) attacker);
        state.injuryTicks = storm.getPhase() > 3 ? 720 : 180;
        state.injuryCooldown = 40;
        state.hits = 0;
        state.requiredHits = storm.getPhase() > 3 ? 3 + storm.getRNG().nextInt(3) : 1 + storm.getRNG().nextInt(2);
        state.roarTicks = 20;
        storm.setHeadInjuryFlag(index, true);
        storm.setHeadFlag(roarBit(index), true);
        storm.setHeadFlag(shakeBit(index), true);
        Vec3d look = getLookVector(state);
        storm.spawnBlueFlamingWitherSkull(index, state.position.x + look.x, state.position.y + look.y, state.position.z + look.z);
        storm.playHeadHurtSound(index);
        if (attackerWasTarget) {
            EntityPlayerMP player = (EntityPlayerMP) attacker;
            SymbiontSummoningManager.makeInvulnerable(player,
                    UltimateTargetManager.getHeadEscapeTicks(
                            WitherStormConfig.headEscapeTime, storm.getRNG().nextInt(80)),
                    "击伤主体头部后逃脱");
            ModCriteriaTriggers.ESCAPE_WITHER_STORM.trigger(
                    player, storm);
        }
    }


    public void hurtDirectly(int index, @Nullable Entity attacker) {
        if (storm.world.isRemote || storm.isDead) return;
        hurt(head(index), attacker);
    }

    private boolean isTargetedByAnyHead(EntityLivingBase target) {
        for (HeadState head : heads) {
            if (head.target == target) return true;
        }
        return false;
    }

    private boolean isTargetedByAnotherHead(EntityLivingBase target, int excludedHead) {
        for (int index = 0; index < heads.length; index++) {
            if (index != excludedHead && heads[index].target == target) return true;
        }
        return false;
    }

    public boolean isHeadInjured(int index) {
        index = head(index);
        return storm.isHeadInjuryFlagSet(index) || heads[index].injuryTicks > 0;
    }
    public int getHeadInjuryTicks(int index) { return heads[head(index)].injuryTicks; }
    public int getHeadHurtDuration(int index) { return heads[head(index)].hurtOverlayTicks; }

    public void handleHeadAttackedOnClient(int index) {
        if (!storm.world.isRemote) return;
        heads[head(index)].hurtOverlayTicks = 10;
    }
    public EntityLivingBase getTarget(int index) { return heads[head(index)].target; }
    public void setTarget(int index, @Nullable EntityLivingBase target) {
        index = head(index);
        HeadState state = heads[index];
        state.target = target;
        state.targetUnseenTicks = 0;
        state.lastTargetPosition = null;
        if (index == 0) storm.setAttackTarget(target);
        storm.updateWatchedTargetId(index, target == null ? 0 : target.getEntityId());
    }

    public float getYaw(int index, float partial) {
        HeadState state = heads[head(index)];
        return lerp(state.yawO, state.yaw, partial);
    }

    public float getPitch(int index, float partial) {
        HeadState state = heads[head(index)];
        return lerp(state.pitchO, state.pitch, partial);
    }

    public Vec3d getPosition(int index, float partial) {
        HeadState state = heads[head(index)];
        return new Vec3d(lerp(state.positionO.x, state.position.x, partial),
                lerp(state.positionO.y, state.position.y, partial),
                lerp(state.positionO.z, state.position.z, partial));
    }

    public AxisAlignedBB getBounds(int index) {
        return getBounds(index, 1.0F);
    }

    public AxisAlignedBB getBounds(int index, float partialTicks) {
        Vec3d position = getPosition(index, partialTicks);
        double size = storm.getPhase() > 3 ? 3.0D : 0.5D;
        return new AxisAlignedBB(position.x - size, position.y - size, position.z - size,
                position.x + size, position.y + size, position.z + size);
    }

    public float getMouth(int index, float partial) {
        HeadState state = heads[head(index)];
        return lerp(state.mouthO, state.mouth, partial);
    }

    public float getBrokenAnimation(int index, float partial) {
        HeadState state = heads[head(index)];
        return lerp(state.brokenO, state.broken, partial);
    }

    public float getShakeRoll(int index, float partial) {
        HeadState state = heads[head(index)];
        float value = MathHelper.clamp(lerp(state.shakeO, state.shake, partial), 0.0F, 1.0F);
        return MathHelper.sin(value * (float) Math.PI) * MathHelper.sin(value * (float) Math.PI * 12.0F) * 0.05F * (float) Math.PI;
    }

    private Vec3d calculatePosition(int index) {
        int phase = MathHelper.clamp(storm.getPhase(), 0, OFFSETS.length - 1);
        double[] offset = OFFSETS[phase][head(index)];
        float bodyYaw = (storm.renderYawOffset + 180.0F) * 0.017453292F;
        float bodyYaw90 = (storm.renderYawOffset + 270.0F) * 0.017453292F;
        float bodyPitch = -(storm.getBodyXRotation(1.0F) + 270.0F) * 0.017453292F;
        double lateralX = MathHelper.cos(bodyYaw) * offset[0];
        double lateralZ = MathHelper.sin(bodyYaw) * offset[0];
        float polarOffset = (float) MathHelper.atan2(offset[2], offset[1]);
        double radius = Math.sqrt(offset[2] * offset[2] + offset[1] * offset[1]);
        double rawX = MathHelper.cos(bodyPitch + polarOffset) * MathHelper.cos(bodyYaw90);
        double rawY = MathHelper.sin(bodyPitch + polarOffset);
        double rawZ = MathHelper.cos(bodyPitch + polarOffset) * MathHelper.sin(bodyYaw90);
        return new Vec3d(storm.posX + lateralX + rawX * radius,
                storm.posY + rawY * radius,
                storm.posZ + lateralZ + rawZ * radius);
    }

    public void writeToNBT(NBTTagCompound tag) {
        NBTTagCompound headsTag = new NBTTagCompound();
        for (int index = 0; index < heads.length; index++) {
            HeadState state = heads[index];
            NBTTagCompound head = new NBTTagCompound();
            head.setBoolean("IsRoaring", storm.isHeadFlagSet(roarBit(index)));
            head.setInteger("RoaringTime", state.roarTicks);
            if (state.distractionPosition != null) {
                head.setTag("DistractedPos", writeVector(state.distractionPosition));
            }
            head.setInteger("DistractedTime", state.distractionTicks);
            head.setInteger("AttackCooldown", state.injuryCooldown);
            head.setInteger("InjuryTime", state.injuryTicks);
            head.setInteger("Hits", state.hits);
            headsTag.setTag(String.valueOf(index), head);
        }
        tag.setTag("Heads", headsTag);
    }

    public void readFromNBT(NBTTagCompound tag) {
        NBTTagCompound headsTag = tag.hasKey("Heads", 10) ? tag.getCompoundTag("Heads") : null;
        for (int index = 0; index < heads.length; index++) {
            HeadState state = heads[index];
            NBTTagCompound head = headsTag != null && headsTag.hasKey(String.valueOf(index), 10)
                    ? headsTag.getCompoundTag(String.valueOf(index)) : null;
            if (head != null) {
                storm.setHeadFlag(roarBit(index), head.getBoolean("IsRoaring"));
                state.roarTicks = Math.max(0, head.getInteger("RoaringTime"));
                state.distractionPosition = head.hasKey("DistractedPos", 10)
                        ? readVector(head.getCompoundTag("DistractedPos")) : null;
                state.distractionTicks = Math.max(0, head.getInteger("DistractedTime"));
                state.injuryCooldown = Math.max(0, head.getInteger("AttackCooldown"));
                state.injuryTicks = Math.max(0, head.getInteger("InjuryTime"));
                state.hits = Math.max(0, head.getInteger("Hits"));
                state.roarScheduleInitialized = false;
                state.distractionEntity = null;
            } else {
                String previousKey = "WitherStormInternalHead" + index;
                if (!tag.hasKey(previousKey, 10)) continue;
                head = tag.getCompoundTag(previousKey);
                state.roarTicks = Math.max(0, head.getInteger("RoarTicks"));
                state.biteTicks = Math.max(0, head.getInteger("BiteTicks"));
                state.nextRoar = Math.max(0, head.getInteger("NextRoarTick"));
                state.roarScheduleInitialized = head.hasKey("NextRoarTick", 3);
                state.nextHeadUpdate = head.hasKey("NextHeadUpdate", 3)
                        ? Math.max(0, head.getInteger("NextHeadUpdate"))
                        : Math.max(0, head.getInteger("NextAttackTick"));
                state.nextClusterPickup = head.hasKey("NextClusterPickupDelay", 3)
                        ? storm.ticksExisted + Math.max(0, head.getInteger("NextClusterPickupDelay")) : 0;
                state.idleAttacks = Math.max(0, head.getInteger("IdleAttacks"));
                state.injuryTicks = Math.max(0, head.getInteger("InjuryTicks"));
                state.injuryCooldown = Math.max(0, head.getInteger("InjuryCooldown"));
                state.hits = Math.max(0, head.getInteger("HeadHits"));
                state.requiredHits = Math.max(1, head.getInteger("RequiredHits"));
                state.distractionTicks = Math.max(0, head.getInteger("DistractionTicks"));
                state.distractionUnseenTicks = Math.max(0, head.getInteger("DistractionUnseenTicks"));
                state.nextDistractionCheck = Math.max(0, head.getInteger("NextDistractionCheck"));
                if (head.hasKey("DistractionX") && head.hasKey("DistractionY") && head.hasKey("DistractionZ")) {
                    state.distractionPosition = new Vec3d(head.getDouble("DistractionX"),
                            head.getDouble("DistractionY"), head.getDouble("DistractionZ"));
                }
                state.distractionEntity = head.hasUniqueId("DistractionEntity")
                        ? head.getUniqueId("DistractionEntity") : null;
            }
            storm.setHeadInjuryFlag(index, state.injuryTicks > 0);
            storm.setHeadDistractionFlag(index, state.isDistracted());
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

    private static int roarBit(int index) { return 1 << head(index); }
    private static int biteBit(int index) { return 1 << (3 + head(index)); }
    private static int shakeBit(int index) { return 1 << (6 + head(index)); }
    private static int head(int index) { return MathHelper.clamp(index, 0, 2); }
    private static float lerp(float a, float b, float partial) { return a + (b - a) * partial; }
    private static double lerp(double a, double b, float partial) { return a + (b - a) * partial; }
    private static float rotlerp(float current, float wanted, float max) {
        float delta = MathHelper.wrapDegrees(wanted - current);
        delta = MathHelper.clamp(delta, -max, max);
        return current + delta;
    }

    private static float smoothSyncedRotation(float current, float target, float maximumChange) {
        return current + MathHelper.clamp(MathHelper.wrapDegrees(target - current),
                -maximumChange, maximumChange);
    }

    private static final class HeadState {
        Vec3d position = Vec3d.ZERO;
        Vec3d positionO = Vec3d.ZERO;
        EntityLivingBase target;
        int targetUnseenTicks;
        Vec3d lastTargetPosition;
        float yaw;
        float yawO;
        float pitch;
        float pitchO;
        float mouth;
        float mouthO;
        float broken;
        float brokenO;
        float shake;
        float shakeO;
        int roarTicks;
        int biteTicks;
        int nextRoar;
        boolean roarScheduleInitialized;
        int nextHeadUpdate;
        int nextClusterPickup;
        int idleAttacks;
        int injuryTicks;
        int injuryCooldown;
        int hurtOverlayTicks;
        int hits;
        int requiredHits;
        int nextShake;
        double beamCutoff = -1.0D;
        Vec3d distractionPosition;
        java.util.UUID distractionEntity;
        int distractionTicks;
        int distractionUnseenTicks;
        int nextDistractionCheck;
        int deathPitchSteps;
        float lerpPitchTarget;
        float lerpYawTarget;
        int lerpPitchSteps;
        int lerpYawSteps;
        boolean randomLookInitialized;
        int randomLookTicks;
        double randomLookX;
        double randomLookY;
        double randomLookZ;
        Vec3d explicitLookPosition;
        int explicitLookSteps = 3;

        boolean isDistracted() { return distractionTicks > 0 && distractionPosition != null; }
        void clearDistraction() {
            distractionTicks = 0;
            distractionPosition = null;
            distractionEntity = null;
            distractionUnseenTicks = 0;
        }
    }
}
