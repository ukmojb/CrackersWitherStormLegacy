package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.api.common.event.WitherStormFindUltimateTargetEvent;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import com.wdcftgg.witherstormmod.common.resource.UpstreamItemTags;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.GameType;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;








public final class UltimateTargetManager {

    public enum DistractionReason { FINISHED_CHASING, FINISHED_CHASING_DELAYED, TIRED_OF_CHASING, FORCED }

    private final WitherStormEntity storm;
    private EntityLivingBase ultimateTarget;
    private Vec3d previousTargetPosition;
    private BlockPos alternativeUltimateTarget;
    private BlockPos blockTargetOverride;
    private BlockPos distractedPos;
    private BlockPos randomStrollPos;
    private ChunkPos center;
    private UUID targetOverride;
    private UUID ignoredTarget;
    private int ignoringTargetFor;
    private boolean targetStationary;
    private boolean canCountRunawayAttempt;
    private boolean canBeDistracted;
    private boolean distracted;
    private int stationaryTicks;
    private int runawayDiminishTicks;
    private int runawayAttempts;
    private int ticksSinceDistracted;
    private int distractionDuration;
    private int distractionWait;
    private int tillShowHole;
    private int tillRandomStroll;
    private int cannotSeeTargetTicks;
    private int cannotReachTargetTicks;
    private int timeTillIgnoreTarget;
    private int tiredOfChasingTicks;
    private UUID farthestTarget;
    private long farthestTargetUntil;
    private UUID randomTarget;
    private long randomTargetUntil;
    private WitherStormConfig.UltimateTargetingType randomizedType;
    private long randomizedTypeUntil;
    private DistractionReason distractionReason;

    public UltimateTargetManager(WitherStormEntity storm) {
        this.storm = storm;
    }

    public void tick() {
        if (storm.world.isRemote || storm.isDead) return;

        List<EntityPlayer> players = new ArrayList<EntityPlayer>();
        for (EntityPlayer player : storm.world.playerEntities) {
            if (!isHiddenUltimateTarget(player)) players.add(player);
        }
        EntityLivingBase selected = findUltimateTarget(players);
        if (selected != ultimateTarget) {
            cannotReachTargetTicks = 0;
            timeTillIgnoreTarget = 1200 + storm.getRNG().nextInt(600);
        }
        ultimateTarget = selected;
        if (selected != null) {
            Vec3d currentPosition = selected.getPositionVector();
            alternativeUltimateTarget = selected.getPosition();
            if (WitherStormConfig.ignoreUltimateTargetIfHidden && !distracted
                    && selected instanceof EntityPlayer && players.size() > 1
                    && ignoredTarget == null
                    && !storm.canSeeOrIsInOpenArea(selected)
                    && storm.getDistanceSq(selected.posX, storm.posY, selected.posZ) < 22500.0D) {
                cannotReachTargetTicks++;
                if (cannotReachTargetTicks > Math.max(1, timeTillIgnoreTarget)) {
                    ignoreTarget((EntityPlayer) selected, 12000 + storm.getRNG().nextInt(6000));
                    cannotReachTargetTicks = 0;
                }
            } else if (cannotReachTargetTicks > 0) {
                cannotReachTargetTicks--;
            }
        }

        tickIgnoredTarget();

        tickTargetingMode(players);
        tickTargetToolTimer(players);

        Vec3d ultimateTargetPos = getUltimateTargetPos();
        if (ultimateTargetPos != null) {
            BlockPos targetBlock = new BlockPos(ultimateTargetPos);
            if (center == null) setCenter(new ChunkPos(targetBlock));
            int distance = (int) (storm.getPositionVector().distanceTo(ultimateTargetPos)
                    * Math.max(0.1D, WitherStormConfig.distanceMultiplier));
            if (WitherStormConfig.usePhaseAsDistanceMultiplier) {
                distance = (int) (distance * (storm.getPhase() * 0.2D + 1.0D));
            }
            if (distracted) tickDistracted(ultimateTargetPos);
            else tickFocusedTarget(ultimateTargetPos, targetBlock, distance);

            if (runawayAttempts >= Math.max(1, WitherStormConfig.targetRunawayAttemptsRequired)
                    && WitherStormConfig.targetRunawayAttempts && !distracted) {
                accelerate();
                canCountRunawayAttempt = false;
                runawayAttempts = 0;
            }
        }

        if (tillRandomStroll > 0 && --tillRandomStroll == 0) findAndSetRandomNearbyStrollPos();
        if (tillShowHole > 0 && --tillShowHole == 0 && storm.getPhase() > 6) storm.setShouldShowHole(true);

        previousTargetPosition = ultimateTarget == null ? null : ultimateTarget.getPositionVector();
    }

    private void tickTargetingMode(List<EntityPlayer> players) {
        WitherStormConfig.UltimateTargetingType type = WitherStormConfig.ultimateTargetingType;
        if (type == null) type = WitherStormConfig.UltimateTargetingType.NEAREST;
        if (type == WitherStormConfig.UltimateTargetingType.RANDOMIZED) type = randomizedType;
        if (type == WitherStormConfig.UltimateTargetingType.RANDOM_STROLL
                || type == WitherStormConfig.UltimateTargetingType.RANDOM_STROLL_NEAR_PLAYER) {
            updateRandomStrollTarget(players, type == WitherStormConfig.UltimateTargetingType.RANDOM_STROLL_NEAR_PLAYER);
        }
    }

    private void tickTargetToolTimer(List<EntityPlayer> players) {
        if (storm.getPhase() > 6 && tillShowHole <= 0 && !storm.isBeingTornApart()
                && carriesCommandBlockTool(players)) {
            tillShowHole = Math.max(1, WitherStormConfig.tillShouldShowHole) * 1200
                    + storm.getRNG().nextInt(4800);
        }
    }

    private void tickDistracted(Vec3d targetPosition) {
        ticksSinceDistracted++;
        if (ticksSinceDistracted > distractionDuration || distractedPos == null) {
            makeFocused();
            return;
        }
        double followRange = storm.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        if (distractionReason != DistractionReason.TIRED_OF_CHASING
                && storm.getPositionVector().distanceTo(targetPosition) < followRange * 2.5D) {
            makeFocused();
            return;
        }
        if (distractionReason == DistractionReason.TIRED_OF_CHASING
                && ultimateTarget != null && previousTargetPosition != null) {
            double speed = horizontalDistance(previousTargetPosition, ultimateTarget.getPositionVector());
            int limit = Math.max(1, WitherStormConfig.boatingForTooLongSeconds) * 20;
            if (speed >= 0.39D) {
                tiredOfChasingTicks = Math.min(limit, tiredOfChasingTicks + 1);
            } else if (tiredOfChasingTicks > 0 && --tiredOfChasingTicks == 0) {
                makeFocused();
            }
        }
    }

    private void tickFocusedTarget(Vec3d targetPosition, BlockPos targetBlock, int distance) {
        int stationaryLimit = Math.max(1, WitherStormConfig.targetStationaryMinutes) * 1200;
        int runawayLimit = Math.max(0, stationaryLimit
                - Math.max(1, WitherStormConfig.targetRunawayMinutes) * 1200);
        int attemptDiminishLimit = Math.max(1, WitherStormConfig.minutesTillRunawayAttemptDiminish) * 1200;
        if (ultimateTarget != null && WorldUtil.hasLineOfSight(storm, ultimateTarget)) cannotSeeTargetTicks = 0;
        else cannotSeeTargetTicks++;
        updateHiddenRandomStroll(targetPosition);

        if (isPosInChunkRadius(targetBlock)) {
            if (stationaryTicks > Math.max(2400, stationaryLimit - distance)) targetStationary = true;
            if (stationaryTicks <= Math.max(2400, stationaryLimit)) {
                stationaryTicks++;
                if (stationaryTicks > Math.max(1, WitherStormConfig.targetRunawayAttemptMinutes) * 1200
                        && WitherStormConfig.targetRunawayAttempts) {
                    canCountRunawayAttempt = true;
                }
            }
            if (runawayDiminishTicks > attemptDiminishLimit) {
                reduceRunawayAttempts();
                runawayDiminishTicks = 0;
            } else {
                runawayDiminishTicks++;
            }
        } else if (stationaryTicks <= Math.max(2400, runawayLimit - distance)) {
            if (targetStationary) maybeStartFinishedChasingDistraction(targetPosition);
            targetStationary = false;
            setCenter(new ChunkPos(targetBlock));
            stationaryTicks = 0;
        } else {
            stationaryTicks = Math.max(0, stationaryTicks - 1);
            targetStationary = true;
        }

        double followRange = storm.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        if (targetStationary && !canBeDistracted
                && storm.getPositionVector().distanceTo(targetPosition)
                < followRange + Math.max(0, WitherStormConfig.maximumDistractionDistance)) {
            canBeDistracted = true;
        }
        if (distractionWait > 0) {
            distractionWait--;
            if (distractionWait <= 0 && canBeDistracted
                    && storm.getPositionVector().distanceTo(targetPosition)
                    >= followRange + 50.0D
                    && !targetStationary) {
                makeDistracted(DistractionReason.FINISHED_CHASING_DELAYED);
            }
        }
        if (WitherStormConfig.boatingForTooLongDistractions && targetStationary
                && canBeDistracted && ultimateTarget != null && previousTargetPosition != null) {
            double speed = horizontalDistance(previousTargetPosition, ultimateTarget.getPositionVector());
            if (speed >= 0.39D) tiredOfChasingTicks++;
            else if (tiredOfChasingTicks > 0) tiredOfChasingTicks--;
            if (tiredOfChasingTicks > Math.max(1, WitherStormConfig.boatingForTooLongSeconds) * 20) {
                makeDistracted(DistractionReason.TIRED_OF_CHASING);
            }
        }
    }

    private void maybeStartFinishedChasingDistraction(Vec3d targetPosition) {
        if (storm.getPhase() <= 3) return;
        SupplementalEntities.CommandBlockEntity commandBlock = storm.getBowelsCommandBlock();
        if (commandBlock != null && commandBlock.getHealth() < commandBlock.getMaxHealth()) return;
        boolean random = WitherStormConfig.randomDistractionChances;
        boolean shouldNotBeDistracted = random && storm.getRNG().nextInt(30) == 1;
        boolean shouldActuallyBeDistracted = random && storm.getRNG().nextInt(10) == 1;
        if ((!canBeDistracted() && !shouldActuallyBeDistracted) || shouldNotBeDistracted) return;
        double followRange = storm.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        int minimumDistance = Math.max(0, WitherStormConfig.minimumDistractionDistance);
        if (minimumDistance != 0 && storm.getPositionVector().distanceTo(targetPosition)
                < followRange + minimumDistance) {
            distractionWait = Math.max(1, WitherStormConfig.distractionWaitTime) * 1200
                    + storm.getRNG().nextInt(1200);
        }
        makeDistracted(DistractionReason.FINISHED_CHASING);
    }

    private void updateHiddenRandomStroll(Vec3d targetPosition) {
        if (!WitherStormConfig.randomStrollingWhenTargetHidden || ultimateTarget == null
                || storm.getPositionVector().distanceTo(targetPosition) >= 300.0D) {
            randomStrollPos = null;
            tillRandomStroll = 0;
        } else if (cannotSeeTarget()) {
            if (tillRandomStroll == 0) {
                tillRandomStroll = randomStrollPos == null ? 600 : 1200 + storm.getRNG().nextInt(600);
            }
        } else {
            randomStrollPos = null;
            tillRandomStroll = 0;
        }
    }

    @Nullable
    public EntityLivingBase findUltimateTarget(List<EntityPlayer> players) {
        EntityLivingBase override = null;
        EntityPlayer amuletTarget = null;
        double amuletDistance = Double.MAX_VALUE;
        Item amulet = ModItems.get("amulet");
        if (WitherStormConfig.amuletOverride && amulet != null) {
            for (EntityPlayer player : players) {
                if (!isUltimateTargetCandidate(player) || !hasItemInMainInventory(player, amulet)) continue;
                double distance = storm.getDistanceSq(player);
                if (distance < amuletDistance) {
                    amuletDistance = distance;
                    amuletTarget = player;
                }
            }
        }
        if (amuletTarget != null) override = amuletTarget;

        EntityLivingBase explicitOverride = resolveOverride();
        if (isExplicitOverrideCandidate(explicitOverride)) override = explicitOverride;

        WitherStormEntity largerStorm = null;
        double largerDistance = Double.MAX_VALUE;
        for (Entity entity : storm.world.loadedEntityList) {
            if (!(entity instanceof WitherStormEntity) || entity == storm) continue;
            WitherStormEntity candidate = (WitherStormEntity) entity;
            if (!WitherStormConfig.witherStormsFollowBiggerStorms
                    || candidate.isDeadOrPlayingDead()
                    || candidate.getConsumedMass() <= storm.getConsumedMass()
                    || candidate.dimension != storm.dimension || candidate.getDistanceSq(storm) > 1000000.0D) continue;
            if (candidate.getDistanceSq(storm) < largerDistance) {
                largerDistance = candidate.getDistanceSq(storm);
                largerStorm = candidate;
            }
        }
        if (largerStorm != null) override = largerStorm;

        List<EntityPlayer> valid = new ArrayList<EntityPlayer>();
        for (EntityPlayer player : players) {
            if (isUltimateTargetCandidate(player)) valid.add(player);
        }
        EntityLivingBase finalTarget = override == null ? chooseByType(valid) : override;
        WitherStormFindUltimateTargetEvent event =
                new WitherStormFindUltimateTargetEvent(storm, finalTarget);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getOriginalUltimateTarget();
    }

    @Nullable
    private EntityPlayer chooseByType(List<EntityPlayer> players) {
        WitherStormConfig.UltimateTargetingType type = WitherStormConfig.ultimateTargetingType;
        if (type == null) type = WitherStormConfig.UltimateTargetingType.NEAREST;
        if (type == WitherStormConfig.UltimateTargetingType.RANDOMIZED) {
            long now = System.currentTimeMillis();
            if (randomizedType == null || now >= randomizedTypeUntil) {
                WitherStormConfig.UltimateTargetingType[] choices = players.size() <= 1
                        ? new WitherStormConfig.UltimateTargetingType[]{
                        WitherStormConfig.UltimateTargetingType.NEAREST,
                        WitherStormConfig.UltimateTargetingType.RANDOM_STROLL,
                        WitherStormConfig.UltimateTargetingType.RANDOM_STROLL_NEAR_PLAYER}
                        : new WitherStormConfig.UltimateTargetingType[]{
                        WitherStormConfig.UltimateTargetingType.NEAREST,
                        WitherStormConfig.UltimateTargetingType.FARTHEST,
                        WitherStormConfig.UltimateTargetingType.GROUP,
                        WitherStormConfig.UltimateTargetingType.RANDOM_PLAYER,
                        WitherStormConfig.UltimateTargetingType.RANDOM_STROLL};
                WitherStormConfig.UltimateTargetingType previous = randomizedType;
                do {
                    randomizedType = choices[storm.getRNG().nextInt(choices.length)];
                } while (choices.length > 1 && randomizedType == previous);
                randomizedTypeUntil = now + Math.max(1, WitherStormConfig.randomizedTargetingTime) * 60000L;
                if (WitherStormConfig.randomlySpeedUpWithTargetChange && storm.getPhase() >= 4
                        && storm.getRNG().nextInt(11) == 0) {
                    accelerate();
                }
            }
            type = randomizedType;
        }
        if (type == WitherStormConfig.UltimateTargetingType.NONE) {
            return null;
        }
        if (type == WitherStormConfig.UltimateTargetingType.RANDOM_STROLL) {
            return null;
        }
        if (type == WitherStormConfig.UltimateTargetingType.RANDOM_STROLL_NEAR_PLAYER) {
            return null;
        }
        if (players.isEmpty()) return null;
        if (type == WitherStormConfig.UltimateTargetingType.RANDOM_PLAYER) {
            long now = System.currentTimeMillis();
            EntityPlayer selected = resolvePlayer(randomTarget);
            if (selected == null || now >= randomTargetUntil) {
                List<EntityPlayer> survival = new ArrayList<EntityPlayer>();
                for (EntityPlayer player : players) {
                    if (isSurvivalPlayer(player)) survival.add(player);
                }
                selected = survival.isEmpty() ? findNearestPlayer(players)
                        : survival.get(storm.getRNG().nextInt(survival.size()));
                if (selected == null) return null;
                randomTarget = selected.getUniqueID();
                randomTargetUntil = now + 300000L;
            }
            return selected;
        }
        if (type == WitherStormConfig.UltimateTargetingType.FARTHEST) {
            long now = System.currentTimeMillis();
            EntityPlayer selected = resolvePlayer(farthestTarget);
            if (selected == null || now >= farthestTargetUntil) {
                selected = Collections.max(players, (left, right) -> Double.compare(
                        storm.getDistanceSq(left), storm.getDistanceSq(right)));
                farthestTarget = selected.getUniqueID();
                farthestTargetUntil = now + Math.max(1, WitherStormConfig.farthestTargetingTime) * 60000L;
            }
            return selected;
        }
        if (type == WitherStormConfig.UltimateTargetingType.GROUP) {
            EntityPlayer selected = null;
            int maximum = -1;
            for (EntityPlayer player : players) {
                int nearby = storm.world.getEntitiesWithinAABB(EntityPlayer.class,
                        player.getEntityBoundingBox().grow(20.0D),
                        this::isGroupMember).size();
                if (nearby > maximum) {
                    maximum = nearby;
                    selected = player;
                }
            }
            return selected;
        }
        EntityPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (EntityPlayer player : players) {
            double distance = storm.getDistanceSq(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private void updateRandomStrollTarget(List<EntityPlayer> players, boolean nearPlayer) {
        if (alternativeUltimateTarget != null
                && horizontalDistance(storm.getPositionVector(),
                new Vec3d(alternativeUltimateTarget)) >= 100.0D) return;
        int radius = Math.max(1, WitherStormConfig.maxRandomStrollTargetingTypeRadius);
        BlockPos center = storm.getPosition();
        if (nearPlayer) {
            EntityPlayer nearest = findNearestPlayer(players);
            if (nearest == null) return;
            center = nearest.getPosition();
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos candidate = center.add(storm.getRNG().nextInt(radius * 2) - radius, 0,
                    storm.getRNG().nextInt(radius * 2) - radius);
            if ((alternativeUltimateTarget == null
                    || (Math.sqrt(alternativeUltimateTarget.distanceSq(candidate)) > radius / 2.0D
                    && horizontalDistance(storm.getPositionVector(),
                    new Vec3d(candidate)) > radius / 2.0D))
                    && storm.world.isValid(candidate)
                    && storm.world.getWorldBorder().contains(candidate)) {
                alternativeUltimateTarget = candidate;
                return;
            }
        }
    }

    @Nullable
    private EntityPlayer findNearestPlayer(List<EntityPlayer> players) {
        EntityPlayer nearest = null;
        double distance = Double.MAX_VALUE;
        for (EntityPlayer player : players) {
            if (!isUltimateTargetCandidate(player)) continue;
            double current = storm.getDistanceSq(player);
            if (current < distance) {
                distance = current;
                nearest = player;
            }
        }
        return nearest;
    }

    @Nullable
    private EntityPlayer resolvePlayer(@Nullable UUID uuid) {
        if (uuid == null) return null;
        for (EntityPlayer player : storm.world.playerEntities) {
            if (uuid.equals(player.getUniqueID()) && isUltimateTargetCandidate(player)) return player;
        }
        return null;
    }

    private EntityLivingBase resolveOverride() {
        if (targetOverride == null) return null;
        for (Entity entity : storm.world.loadedEntityList) {
            if (targetOverride.equals(entity.getUniqueID()) && entity instanceof EntityLivingBase) return (EntityLivingBase) entity;
        }
        return null;
    }

    private boolean isExplicitOverrideCandidate(@Nullable EntityLivingBase entity) {
        return entity != null && entity != storm && !entity.isDead
                && entity.world == storm.world && entity.dimension == storm.dimension
                && !(entity instanceof SupplementalEntities.WitherStormSegmentEntity);
    }

    private boolean isUltimateTargetCandidate(@Nullable EntityPlayer player) {
        return player != null && !player.isDead && player.world == storm.world
                && player.dimension == storm.dimension && !player.isSpectator()
                && !player.capabilities.disableDamage
                && !isHiddenUltimateTarget(player);
    }

    private boolean isGroupMember(@Nullable EntityPlayer player) {
        return player != null && player.isEntityAlive() && !player.isSpectator()
                && !player.capabilities.disableDamage;
    }

    private static boolean isSurvivalPlayer(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return false;
        GameType gameType = ((EntityPlayerMP) player).interactionManager.getGameType();
        return gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE;
    }

    private void tickIgnoredTarget() {
        if (ignoringTargetFor > 0 && --ignoringTargetFor == 0) ignoredTarget = null;
        if (ignoringTargetFor <= 0) ignoredTarget = null;
    }

    public void ignoreTarget(EntityPlayer player, int ticks) {
        if (player == null || ticks <= 0) return;
        ignoredTarget = player.getUniqueID();
        ignoringTargetFor = ticks;
    }

    private boolean isHiddenUltimateTarget(@Nullable Entity entity) {
        return entity != null && ignoredTarget != null
                && ignoredTarget.equals(entity.getUniqueID());
    }

    static int getHeadEscapeTicks(int configuredSeconds, int randomBonus) {
        return Math.max(0, configuredSeconds) * 20 + MathHelper.clamp(randomBonus, 0, 79);
    }

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        double deltaX = first.x - second.x;
        double deltaZ = first.z - second.z;
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    private boolean hasItemInMainInventory(EntityPlayer player, Item item) {
        for (ItemStack stack : player.inventory.mainInventory) if (!stack.isEmpty() && stack.getItem() == item) return true;
        return false;
    }

    private boolean carriesCommandBlockTool(List<EntityPlayer> players) {
        for (EntityPlayer player : players) {
            for (ItemStack stack : player.inventory.mainInventory) {
                if (!stack.isEmpty()
                        && UpstreamItemTags.contains(UpstreamItemTags.COMMAND_BLOCK_TOOLS, stack)) return true;
            }
        }
        return false;
    }

    public void accelerate() {
        stationaryTicks = Math.max(1, WitherStormConfig.targetStationaryMinutes) * 1200;
        targetStationary = true;
        if (distracted) makeFocused();
    }

    public void deaccelerate() {
        stationaryTicks = 0;
        targetStationary = false;
    }

    public void makeDistracted(DistractionReason reason) {
        if (!WitherStormConfig.targettingDistractionsEnabled || distractionWait > 0) return;
        BlockPos position = findDistractPos();
        if (position == null) return;
        distractedPos = position;
        distracted = true;
        double distance = Math.sqrt(storm.getDistanceSq(
                position.getX(), position.getY(), position.getZ()));
        distractionDuration = Math.max(4800,
                Math.max(1, WitherStormConfig.distractionTimeMinutes) * 1200
                        + storm.getRNG().nextInt(12000) - (int) distance * 2);
        canBeDistracted = false;
        distractionReason = reason;
    }

    public void makeFocused() {
        distracted = false;
        ticksSinceDistracted = 0;
        canBeDistracted = false;
        distractedPos = null;
        distractionDuration = 0;
        distractionWait = 0;
        distractionReason = null;
    }

    @Nullable
    public BlockPos findDistractPos() {
        BlockPos structure = findNearestDistractableStructure();
        if (structure != null) return structure;

        Vec3d targetPosition = getUltimateTargetPos();
        if (targetPosition == null) return null;
        int multiplier = Math.max(1, WitherStormConfig.searchableRangeMultiplier);
        double followRange = storm.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        double range = followRange * 2.0D * multiplier;
        double horizontalExtent = storm.width * range;
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos candidate = new BlockPos(
                    storm.posX + (storm.getRNG().nextDouble() - 0.5D) * horizontalExtent,
                    storm.posY,
                    storm.posZ + (storm.getRNG().nextDouble() - 0.5D) * horizontalExtent);
            if (storm.world.getBlockState(candidate).getBlock() == Blocks.AIR
                    && Math.sqrt(storm.getDistanceSq(candidate.getX(), candidate.getY(), candidate.getZ()))
                    > 2000.0D * multiplier
                    && new Vec3d(candidate).distanceTo(targetPosition) > 500.0D) {
                return candidate;
            }
        }

        return null;
    }

    @Nullable
    private BlockPos findNearestDistractableStructure() {
        if (!(storm.world instanceof WorldServer)) return null;
        WorldServer world = (WorldServer) storm.world;
        BlockPos origin = storm.getPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        String[] structures = {"Village", "Mansion", "Monument"};
        for (String name : structures) {
            BlockPos candidate = world.findNearestStructure(name, origin, false);
            if (candidate == null) continue;
            ChunkPos chunk = new ChunkPos(candidate);
            ChunkPos current = new ChunkPos(origin);
            if (Math.abs(chunk.x - current.x) > 100 || Math.abs(chunk.z - current.z) > 100) continue;
            double distance = candidate.distanceSq(origin);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    @Nullable
    public EntityLivingBase getUltimateTarget() { return ultimateTarget; }

    @Nullable
    public Vec3d getUltimateTargetPos() {
        if (blockTargetOverride != null) return new Vec3d(blockTargetOverride);
        if (ultimateTarget != null) return ultimateTarget.getPositionVector();
        if (alternativeUltimateTarget != null) return new Vec3d(alternativeUltimateTarget);
        return null;
    }

    @Nullable
    public Vec3d getMovementTargetPos() {
        if (distracted && distractedPos != null) return new Vec3d(distractedPos);
        if (randomStrollPos != null) return new Vec3d(randomStrollPos);

        if (ultimateTarget == null && blockTargetOverride == null) return null;
        return getUltimateTargetPos();
    }

    @Nullable public BlockPos getAlternativeUltimateTarget() { return alternativeUltimateTarget; }
    public void setAlternativeUltimateTarget(@Nullable BlockPos pos) { alternativeUltimateTarget = pos; }
    @Nullable public BlockPos getDistractedPos() { return distractedPos; }
    public void setDistractedPos(@Nullable BlockPos pos) { distractedPos = pos; }
    public boolean isTargetStationary() { return targetStationary; }
    public int targetStationaryTicks() { return stationaryTicks; }
    public int getRunawayAttempts() { return runawayAttempts; }
    public void countRunawayAttempt() { runawayAttempts++; }
    public void setRunawayAttempts(int amount) { runawayAttempts = Math.max(0, amount); }
    public void reduceRunawayAttempts() { if (runawayAttempts > 0) runawayAttempts--; }
    public int getRunawayDiminishTicks() { return runawayDiminishTicks; }
    public boolean isDistracted() { return distracted; }
    public boolean canBeDistracted() {
        return canBeDistracted && WitherStormConfig.targettingDistractionsEnabled;
    }
    public void setCanBeDistracted(boolean value) { canBeDistracted = value; }
    public int getTicksSinceDistracted() { return ticksSinceDistracted; }
    public int getDistractedTickTime() { return distractionDuration; }
    public int getDistractionWait() { return distractionWait; }
    public void setDistractionWait(int ticks) { distractionWait = Math.max(0, ticks); }
    public int tillShowHole() { return tillShowHole; }
    public void setTillShowHole(int ticks) { tillShowHole = Math.max(0, ticks); }
    public void setTargetOverride(@Nullable UUID uuid) { targetOverride = uuid; }
    @Nullable public UUID getTargetOverride() { return targetOverride; }
    public void setBlockTargetOverride(@Nullable BlockPos pos) { blockTargetOverride = pos; }
    @Nullable public BlockPos getBlockTargetOverride() { return blockTargetOverride; }
    @Nullable public ChunkPos getCenter() { return center; }
    public void setCenter(@Nullable ChunkPos value) {
        center = value;
        if (value != null && canCountRunawayAttempt) {
            countRunawayAttempt();
            canCountRunawayAttempt = false;
        }
    }
    @Nullable public BlockPos getRandomStrollPos() { return randomStrollPos; }
    public boolean isRandomStrolling() { return randomStrollPos != null; }
    public boolean cannotSeeTarget() { return cannotSeeTargetTicks > 600; }

    public boolean isPosInChunkRadius(BlockPos pos) {
        if (pos == null || center == null) return false;
        ChunkPos current = new ChunkPos(pos);
        int radius = Math.max(0, WitherStormConfig.targetStationaryChunkRadius);
        return Math.abs(current.x - center.x) <= radius
                && Math.abs(current.z - center.z) <= radius;
    }

    public void findAndSetRandomNearbyStrollPos() {
        Vec3d targetPosition = getUltimateTargetPos();
        if (targetPosition == null) {
            randomStrollPos = null;
            return;
        }
        BlockPos selected = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            float angle = storm.getRNG().nextFloat() * (float) (Math.PI * 2.0D);
            int radius = 150 + storm.getRNG().nextInt(50);
            int offsetX = (int) (MathHelper.sin(angle) * radius);
            int offsetZ = (int) (MathHelper.cos(angle) * radius);
            BlockPos candidate = new BlockPos(
                    MathHelper.floor(targetPosition.x) + offsetX,
                    MathHelper.floor(targetPosition.y),
                    MathHelper.floor(targetPosition.z) + offsetZ);
            if (randomStrollPos == null || (Math.sqrt(randomStrollPos.distanceSq(candidate)) > 100.0D
                    && horizontalDistance(storm.getPositionVector(), new Vec3d(candidate)) > 100.0D)) {
                selected = candidate;
                break;
            }
        }
        randomStrollPos = selected;
    }

    public void save(NBTTagCompound compound) {
        if (alternativeUltimateTarget != null) {
            compound.setTag("AlternativeUltimateTarget", NBTUtil.createPosTag(alternativeUltimateTarget));
        }
        if (center != null) {
            NBTTagCompound chunk = new NBTTagCompound();
            chunk.setInteger("x", center.x);
            chunk.setInteger("z", center.z);
            compound.setTag("UltimateTargetChunkPos", chunk);
        }
        compound.setInteger("TargetStationaryTicks", stationaryTicks);
        compound.setInteger("TargetRunawayAttempts", runawayAttempts);
        if (targetOverride != null) compound.setUniqueId("TargetOverride", targetOverride);
        if (blockTargetOverride != null) {
            compound.setTag("BlockTargetOverride", NBTUtil.createPosTag(blockTargetOverride));
        }

        NBTTagCompound distraction = new NBTTagCompound();
        if (distractedPos != null) {
            distraction.setTag("DistractedPos", NBTUtil.createPosTag(distractedPos));
        }
        distraction.setBoolean("CanBeDistracted", canBeDistracted());
        distraction.setBoolean("IsDistracted", distracted);
        distraction.setInteger("TicksSinceDistracted", ticksSinceDistracted);
        distraction.setInteger("CanBeDistractedFor", distractionDuration);
        distraction.setInteger("DistractionWait", distractionWait);
        if (distractionReason != null) {
            distraction.setInteger("DistractionReason", distractionReason.ordinal());
        }
        distraction.setInteger("TiredOfChasingTicks", tiredOfChasingTicks);
        compound.setTag("UltimateTargetDistraction", distraction);

        if (randomStrollPos != null) {
            compound.setTag("RandomStrollPos", NBTUtil.createPosTag(randomStrollPos));
        }
        compound.setInteger("RandomStrollTimer", tillRandomStroll);
        if (ignoredTarget != null) compound.setUniqueId("IgnoredTarget", ignoredTarget);
        compound.setInteger("IgnoringTargetFor", ignoringTargetFor);
        compound.setInteger("CannotReachTargetFor", cannotReachTargetTicks);
        compound.setInteger("TimeTillIgnoreTarget", timeTillIgnoreTarget);
        compound.setInteger("CannotSeeTargetFor", cannotSeeTargetTicks);
    }

    public void read(NBTTagCompound compound) {
        NBTTagCompound source = compound.hasKey("WitherStormUltimateTarget", 10)
                && !compound.hasKey("UltimateTargetDistraction", 10)
                ? compound.getCompoundTag("WitherStormUltimateTarget") : compound;

        ultimateTarget = null;
        previousTargetPosition = null;
        alternativeUltimateTarget = readBlockPos(source, "AlternativeUltimateTarget");
        if (source.hasKey("UltimateTargetChunkPos", 10)) {
            NBTTagCompound chunk = source.getCompoundTag("UltimateTargetChunkPos");
            center = new ChunkPos(chunk.getInteger("x"), chunk.getInteger("z"));
        } else if (source.hasKey("UltimateTargetChunkX", 3)
                && source.hasKey("UltimateTargetChunkZ", 3)) {
            center = new ChunkPos(source.getInteger("UltimateTargetChunkX"),
                    source.getInteger("UltimateTargetChunkZ"));
        } else {
            center = null;
        }
        blockTargetOverride = readBlockPos(source, "BlockTargetOverride");
        targetOverride = source.hasUniqueId("TargetOverride")
                ? source.getUniqueId("TargetOverride") : null;
        stationaryTicks = Math.max(0, source.getInteger("TargetStationaryTicks"));
        runawayAttempts = Math.max(0, source.getInteger("TargetRunawayAttempts"));
        runawayDiminishTicks = Math.max(0, source.getInteger("RunawayDiminishTicks"));
        canCountRunawayAttempt = source.getBoolean("CanCountRunawayAttempt");
        targetStationary = source.getBoolean("TargetStationary");

        NBTTagCompound distraction = source.hasKey("UltimateTargetDistraction", 10)
                ? source.getCompoundTag("UltimateTargetDistraction") : source;
        distractedPos = readBlockPos(distraction, "DistractedPos");
        canBeDistracted = distraction.getBoolean("CanBeDistracted");
        distracted = distraction.getBoolean("IsDistracted") && distractedPos != null;
        ticksSinceDistracted = Math.max(0, distraction.getInteger("TicksSinceDistracted"));
        distractionDuration = Math.max(0, distraction.hasKey("CanBeDistractedFor", 3)
                ? distraction.getInteger("CanBeDistractedFor")
                : distraction.getInteger("DistractionDuration"));
        distractionWait = Math.max(0, distraction.getInteger("DistractionWait"));
        int distractionOrdinal = distraction.getInteger("DistractionReason");
        distractionReason = distraction.hasKey("DistractionReason", 3)
                && distractionOrdinal >= 0 && distractionOrdinal < DistractionReason.values().length
                ? DistractionReason.values()[distractionOrdinal] : null;
        tiredOfChasingTicks = Math.max(0, distraction.getInteger("TiredOfChasingTicks"));

        randomStrollPos = readBlockPos(source, "RandomStrollPos");
        tillRandomStroll = Math.max(0, source.hasKey("RandomStrollTimer", 3)
                ? source.getInteger("RandomStrollTimer") : source.getInteger("TillRandomStroll"));
        ignoredTarget = source.hasUniqueId("IgnoredTarget")
                ? source.getUniqueId("IgnoredTarget") : null;
        ignoringTargetFor = Math.max(0, source.getInteger("IgnoringTargetFor"));
        if (ignoringTargetFor <= 0 && source.getInteger("IgnoredTargetTicks") > 0) {
            ignoringTargetFor = source.getInteger("IgnoredTargetTicks");
        }
        if (ignoredTarget == null) {
            NBTTagList ignored = source.getTagList("IgnoredTargets", 10);
            for (int index = 0; index < ignored.tagCount(); index++) {
                NBTTagCompound target = ignored.getCompoundTagAt(index);
                if (target.hasUniqueId("UUID") && target.getInteger("Ticks") > 0) {
                    ignoredTarget = target.getUniqueId("UUID");
                    ignoringTargetFor = target.getInteger("Ticks");
                    break;
                }
            }
        }
        if (ignoringTargetFor <= 0) ignoredTarget = null;
        cannotReachTargetTicks = Math.max(0, source.hasKey("CannotReachTargetFor", 3)
                ? source.getInteger("CannotReachTargetFor")
                : source.getInteger("CannotReachTargetTicks"));
        timeTillIgnoreTarget = Math.max(0, source.getInteger("TimeTillIgnoreTarget"));
        cannotSeeTargetTicks = Math.max(0, source.hasKey("CannotSeeTargetFor", 3)
                ? source.getInteger("CannotSeeTargetFor")
                : source.getInteger("CannotSeeTargetTicks"));
        tillShowHole = Math.max(0, source.getInteger("TillShowHole"));

        farthestTarget = source.hasUniqueId("FarthestTarget")
                ? source.getUniqueId("FarthestTarget") : null;
        randomTarget = source.hasUniqueId("RandomTarget")
                ? source.getUniqueId("RandomTarget") : null;
        int randomizedOrdinal = source.getInteger("RandomizedTargetingType");
        randomizedType = source.hasKey("RandomizedTargetingType", 3) && randomizedOrdinal >= 0
                && randomizedOrdinal < WitherStormConfig.UltimateTargetingType.values().length
                ? WitherStormConfig.UltimateTargetingType.values()[randomizedOrdinal] : null;
        farthestTargetUntil = source.getLong("FarthestTargetUntil");
        randomTargetUntil = source.getLong("RandomTargetUntil");
        randomizedTypeUntil = source.getLong("RandomizedTypeUntil");
    }

    @Nullable
    private static BlockPos readBlockPos(NBTTagCompound compound, String key) {
        if (compound.hasKey(key, 10)) return NBTUtil.getPosFromTag(compound.getCompoundTag(key));
        if (compound.hasKey(key, 4)) return BlockPos.fromLong(compound.getLong(key));
        return null;
    }
}
