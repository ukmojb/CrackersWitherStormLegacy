package com.wdcftgg.witherstormmod.common.world;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.SymbiontSummoningManager;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.potion.PotionEffect;
import net.minecraft.init.MobEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraft.util.Rotation;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class BowelsManager {
    /** 上游 WitherStormBowelsManager 的 NORTH_HEAD_POS/SOUTH_HEAD_POS 常量，
     * Y 分量由 instance.getArenaHeadY() 按网络锚点换算。 */
    private static final BlockPos[] HEAD_OFFSETS = {
            new BlockPos(-2, 0, 27), new BlockPos(-3, 0, -23)
    };

    private BowelsManager() {
    }

    public enum BowelsEnterStatus {
        SUCCESS,
        CANT_SETUP_BOWELS,
        ENTITY_CANNOT_CHANGE
    }

    public static BowelsEnterStatus enterWithStatus(WitherStormEntity storm, Entity entity) {
        if (!canEnter(storm, entity)) return BowelsEnterStatus.ENTITY_CANNOT_CHANGE;
        return enter(storm, entity) == null
                ? BowelsEnterStatus.CANT_SETUP_BOWELS
                : BowelsEnterStatus.SUCCESS;
    }

    private static boolean canEnter(WitherStormEntity storm, Entity entity) {
        if (storm == null || entity == null || !storm.isEntityAlive() || entity.isDead
                || !entity.isAddedToWorld() || entity.isRiding()
                || !entity.getPassengers().isEmpty()) {
            return false;
        }
        if (entity instanceof EntityPlayerMP) {
            return ((EntityPlayerMP) entity).getServer() != null;
        }
        return !entity.world.isRemote && entity.world.getMinecraftServer() != null;
    }

    public static BowelsInstanceData.Instance enter(WitherStormEntity storm, EntityPlayerMP player) {
        MinecraftServer server = player.getServer();
        if (server == null || !storm.isEntityAlive() || player.isDead || !player.isAddedToWorld()
                || player.isRiding() || !player.getPassengers().isEmpty()) return null;
        WorldServer bowels = server.getWorld(BowelsDimensions.DIMENSION_ID);
        if (bowels == null) return null;
        BowelsInstanceData data = BowelsInstanceData.get(bowels);
        if (!BowelsInstanceData.checkStructuresEnabled(bowels)) return null;
        BowelsInstanceData.Instance instance = data.getOrCreate(storm.getUniqueID(), storm.dimension, storm.getPosition());
        if (instance == null) return null;
        prepareArena(bowels, data, instance);
        BlockPos entrance = findEntrance(bowels, instance);
        playTransportSound(player);
        server.getPlayerList().transferPlayerToDimension(player, BowelsDimensions.DIMENSION_ID,
                new BowelsTeleporter(entrance));
        return instance;
    }

    public static BowelsInstanceData.Instance enter(WitherStormEntity storm, Entity entity) {
        if (entity instanceof EntityPlayerMP) return enter(storm, (EntityPlayerMP) entity);
        MinecraftServer server = entity.world.getMinecraftServer();
        if (server == null || entity.world.isRemote || entity.isDead || !storm.isEntityAlive()
                || !entity.isAddedToWorld() || entity.isRiding() || !entity.getPassengers().isEmpty()) return null;
        WorldServer bowels = server.getWorld(BowelsDimensions.DIMENSION_ID);
        if (bowels == null) return null;
        BowelsInstanceData data = BowelsInstanceData.get(bowels);
        if (!BowelsInstanceData.checkStructuresEnabled(bowels)) return null;
        BowelsInstanceData.Instance instance = data.getOrCreate(storm.getUniqueID(), storm.dimension, storm.getPosition());
        if (instance == null) return null;
        prepareArena(bowels, data, instance);
        entity.changeDimension(BowelsDimensions.DIMENSION_ID, new BowelsTeleporter(findEntrance(bowels, instance)));
        return instance;
    }

    public static void leave(EntityPlayerMP player) {
        MinecraftServer server = player.getServer();
        if (server == null || player.dimension != BowelsDimensions.DIMENSION_ID
                || !player.isAddedToWorld() || player.isRiding() || !player.getPassengers().isEmpty()) return;
        WorldServer bowels = server.getWorld(BowelsDimensions.DIMENSION_ID);
        BowelsInstanceData.Instance instance = BowelsInstanceData.get(bowels).findContaining(player.getPosition());
        int destinationDimension = instance == null ? 0 : instance.originDimension;
        BlockPos destination = instance == null ? server.getWorld(0).getSpawnPoint() : instance.origin.up(5);
        playTransportSound(player);
        server.getPlayerList().transferPlayerToDimension(player, destinationDimension, new BowelsTeleporter(destination));
        SymbiontSummoningManager.makeInvulnerable(player, 2400);
        if (WitherStormConfig.bowelsFallResistance) {
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 120, 255, false, false));
        }
    }

    private static void playTransportSound(EntityPlayerMP player) {
        if (ModSounds.get("bowels_transport") == null) return;
        player.connection.sendPacket(new SPacketSoundEffect(
                ModSounds.get("bowels_transport"), SoundCategory.AMBIENT,
                player.posX, player.posY, player.posZ, 1.0F, 1.0F));
    }

    public static void leave(Entity entity) {
        if (entity instanceof EntityPlayerMP) {
            leave((EntityPlayerMP) entity);
            return;
        }
        MinecraftServer server = entity.world.getMinecraftServer();
        if (server == null || entity.world.isRemote || entity.isDead
                || entity.dimension != BowelsDimensions.DIMENSION_ID || !entity.isAddedToWorld()
                || entity.isRiding() || !entity.getPassengers().isEmpty()) return;
        WorldServer bowels = server.getWorld(BowelsDimensions.DIMENSION_ID);
        BowelsInstanceData.Instance instance = BowelsInstanceData.get(bowels).findContaining(entity.getPosition());
        int destinationDimension = instance == null ? 0 : instance.originDimension;
        WorldServer destinationWorld = server.getWorld(destinationDimension);
        if (destinationWorld == null) return;
        BlockPos destination = instance == null ? destinationWorld.getSpawnPoint() : instance.origin.up(5);
        Entity transferred = entity.changeDimension(destinationDimension, new BowelsTeleporter(destination));
        if (WitherStormConfig.bowelsFallResistance && transferred instanceof EntityLivingBase) {
            ((EntityLivingBase) transferred).addPotionEffect(
                    new PotionEffect(MobEffects.RESISTANCE, 120, 255, false, false));
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.world.isRemote
                || event.world.provider.getDimension() != BowelsDimensions.DIMENSION_ID) return;
        WorldServer world = (WorldServer) event.world;
        BowelsInstanceData data = BowelsInstanceData.get(world);
        for (BowelsInstanceData.Instance instance : data.getInstances()) {
            if (instance.needsCoordinateMigration()) migrateLegacyArena(world, data, instance);
            if (!instance.completed && instance.prepared && world.getTotalWorldTime() % 20L == 0L) {
                ensureCommandBlock(world, data, instance);
                ensureArenaEntities(world, data, instance);
            }
        }
        for (Entity entity : new ArrayList<Entity>(event.world.loadedEntityList)) {
            if (!entity.isDead && entity.posY < 50.0D) leave(entity);
        }
    }

    public static void prepareArena(WorldServer world, BowelsInstanceData data, BowelsInstanceData.Instance instance) {
        if (instance.needsCoordinateMigration()) migrateLegacyArena(world, data, instance);
        if (!instance.prepared) {
            if (StructureTemplates.placeBowelsNetwork(world, instance.center, world.rand)) {
                // 新版网络按上游起始块原点 Y=100 放置；旧存档无此字段时保持旧锚点 88，
                // 墙头按各自锚点 +28 修复，避免旧网络中的头部被埋入墙内。
                instance.networkBaseY = 100;
            }
            BlockPos arena = instance.getArenaPosition();
            placeCenteredPodium(world, arena);
            instance.arenaTentacleTargetCount = 6 + world.rand.nextInt(6);
            spawnArenaTentacles(world, instance, arena, instance.arenaTentacleTargetCount);
            spawnArenaHeads(world, instance);
            instance.prepared = true;
            data.markDirty();
        }
        ensureCommandBlock(world, data, instance);
        ensureArenaEntities(world, data, instance);
    }

    private static void ensureCommandBlock(WorldServer world, BowelsInstanceData data,
                                           BowelsInstanceData.Instance instance) {
        if (instance.completed || instance.bossPhase >= 17) return;
        BlockPos arena = instance.getArenaPosition();
        world.getChunkProvider().provideChunk(arena.getX() >> 4, arena.getZ() >> 4);
        Entity existing = instance.commandBlockUuid == null
                ? null : world.getEntityFromUuid(instance.commandBlockUuid);
        SupplementalEntities.CommandBlockEntity core = resolveCanonicalCommandBlock(
                world, instance, arena, existing);
        if (core != null) {
            boolean changed = !core.getUniqueID().equals(instance.commandBlockUuid);
            instance.commandBlockUuid = core.getUniqueID();
            core.setBowelsOwnerUuid(instance.stormUuid);
            alignBowelsCoreRotation(core);
            double centeredX = arena.getX() + 0.5D;
            double centeredZ = arena.getZ() + 0.5D;
            if (Math.abs(core.posX - centeredX) > 1.0E-6D
                    || Math.abs(core.posZ - centeredZ) > 1.0E-6D) {
                core.setPosition(centeredX, core.posY, centeredZ);
            }
            core.applyBowelsPodiumLiftPose(BowelsBossfightController.getExpectedCoreY(instance));
            if (changed) data.markDirty();
            return;
        }

        core = new SupplementalEntities.CommandBlockEntity(world);
        core.setIndependentBowelsPart();
        core.setBowelsOwnerUuid(instance.stormUuid);
        core.setPosition(arena.getX() + 0.5D,
                BowelsBossfightController.getExpectedCoreY(instance), arena.getZ() + 0.5D);
        alignBowelsCoreRotation(core);
        int completedHits = instance.bossPhase >= 12 ? 3 : instance.bossPhase >= 6 ? 2
                : instance.bossPhase >= 1 ? 1 : 0;
        core.setHealth(core.getMaxHealth() * (4 - completedHits) / 4.0F);
        if (world.spawnEntity(core)) {
            instance.commandBlockUuid = core.getUniqueID();
            data.markDirty();
        }
    }

    @Nullable
    private static SupplementalEntities.CommandBlockEntity resolveCanonicalCommandBlock(
            WorldServer world, BowelsInstanceData.Instance instance, BlockPos arena,
            Entity savedEntity) {
        AxisAlignedBB arenaBounds = new AxisAlignedBB(arena).grow(32.0D, 24.0D, 32.0D);
        List<SupplementalEntities.CommandBlockEntity> candidates =
                world.getEntitiesWithinAABB(SupplementalEntities.CommandBlockEntity.class,
                        arenaBounds, entity -> !entity.isDead && entity.isIndependentBowelsPart());
        SupplementalEntities.CommandBlockEntity saved = savedEntity instanceof SupplementalEntities.CommandBlockEntity
                && !savedEntity.isDead
                && ((SupplementalEntities.CommandBlockEntity) savedEntity).isIndependentBowelsPart()
                ? (SupplementalEntities.CommandBlockEntity) savedEntity : null;
        SupplementalEntities.CommandBlockEntity canonical = saved;
        if (instance.bossPhase == 2 || instance.bossPhase == 7 || instance.bossPhase == 13) {
            for (SupplementalEntities.CommandBlockEntity candidate : candidates) {
                if (candidate.getPodiumCluster() != null) {
                    canonical = candidate;
                    break;
                }
            }
        }
        if (canonical == null) {
            double closestDistance = Double.MAX_VALUE;
            for (SupplementalEntities.CommandBlockEntity candidate : candidates) {
                double distance = candidate.getDistanceSqToCenter(arena);
                if (distance < closestDistance) {
                    canonical = candidate;
                    closestDistance = distance;
                }
            }
        }
        if (canonical == null) return null;
        for (SupplementalEntities.CommandBlockEntity candidate : candidates) {
            if (candidate == canonical) continue;
            SupplementalEntities.BlockClusterEntity duplicateCluster = candidate.getPodiumCluster();
            if (duplicateCluster != null && !duplicateCluster.isDead) duplicateCluster.setDead();
            candidate.discardDuplicateBowelsCore();
        }
        return canonical;
    }

    private static void ensureArenaEntities(WorldServer world, BowelsInstanceData data,
                                            BowelsInstanceData.Instance instance) {
        if (instance.completed || instance.bossPhase >= 17) return;
        BlockPos arena = instance.getArenaPosition();
        for (int chunkX = (arena.getX() - 32) >> 4; chunkX <= (arena.getX() + 32) >> 4; chunkX++) {
            for (int chunkZ = (arena.getZ() - 32) >> 4; chunkZ <= (arena.getZ() + 32) >> 4; chunkZ++) {
                world.getChunkProvider().provideChunk(chunkX, chunkZ);
            }
        }

        boolean changed = ensureArenaHeads(world, instance);
        if (instance.bossPhase < 9) changed |= ensureArenaTentacles(world, instance, arena);
        if (changed) data.markDirty();
    }

    private static boolean ensureArenaHeads(WorldServer world, BowelsInstanceData.Instance instance) {
        boolean changed = false;
        for (int index = 0; index < HEAD_OFFSETS.length; index++) {
            BlockPos position = getArenaHeadPosition(instance, index);
            SupplementalEntities.WitherStormHeadEntity head = resolveArenaHead(world,
                    instance.arenaHeadUuids[index], position);
            if (head == null && instance.bossPhase < 17) {
                head = spawnArenaHead(world, position, index, Rotation.NONE);
                if (head != null) changed = true;
            }
            if (head != null) repairArenaHeadOrientation(head, index, Rotation.NONE);
            UUID resolved = head == null ? null : head.getUniqueID();
            if (resolved != null && !resolved.equals(instance.arenaHeadUuids[index])) {
                instance.arenaHeadUuids[index] = resolved;
                changed = true;
            }
            if (head != null && instance.bossPhase >= 14 && instance.bossPhase <= 16) {
                head.setActive(true);
            }
        }
        return changed;
    }

    private static BlockPos getArenaHeadPosition(BowelsInstanceData.Instance instance, int index) {
        BlockPos structureCenter = instance.getStructureCenter();
        return new BlockPos(structureCenter.getX() + HEAD_OFFSETS[index].getX(),
                instance.getArenaHeadY(), structureCenter.getZ() + HEAD_OFFSETS[index].getZ());
    }

    private static SupplementalEntities.WitherStormHeadEntity resolveArenaHead(
            WorldServer world, UUID uuid, BlockPos expectedPosition) {
        Entity saved = uuid == null ? null : world.getEntityFromUuid(uuid);
        if (saved instanceof SupplementalEntities.WitherStormHeadEntity && !saved.isDead
                && ((SupplementalEntities.WitherStormHeadEntity) saved).isIndependentBowelsPart()) {
            SupplementalEntities.WitherStormHeadEntity head =
                    (SupplementalEntities.WitherStormHeadEntity) saved;
            snapArenaHead(head, expectedPosition);
            return head;
        }
        SupplementalEntities.WitherStormHeadEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        // 旧存档头部曾按锚点 88 + 128 生成，与新凹槽高度最多相差 12 格；
        // 扩大搜索范围并只修复位置，避免再次生成重复头实体。
        for (SupplementalEntities.WitherStormHeadEntity candidate : world.getEntitiesWithinAABB(
                SupplementalEntities.WitherStormHeadEntity.class,
                new AxisAlignedBB(expectedPosition).grow(16.0D))) {
            if (candidate.isDead || !candidate.isIndependentBowelsPart()) continue;
            double distance = candidate.getDistanceSqToCenter(expectedPosition);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        if (nearest != null) snapArenaHead(nearest, expectedPosition);
        return nearest;
    }

    /** 旧存档头部位置修复：与上游 EntityType.spawn(alignPosition=false) 一样，
     * 实体水平中心精确落在方块角点，不再额外 +0.5。 */
    private static void snapArenaHead(SupplementalEntities.WitherStormHeadEntity head,
                                      BlockPos expectedPosition) {
        double targetX = expectedPosition.getX();
        double targetY = expectedPosition.getY();
        double targetZ = expectedPosition.getZ();
        if (Math.abs(head.posX - targetX) <= 1.0E-4D
                && Math.abs(head.posY - targetY) <= 1.0E-4D
                && Math.abs(head.posZ - targetZ) <= 1.0E-4D) return;
        head.setPosition(targetX, targetY, targetZ);
        head.prevPosX = targetX;
        head.prevPosY = targetY;
        head.prevPosZ = targetZ;
    }

    /** Restores the fixed body anchor established by upstream spawnHeads(). */
    private static void repairArenaHeadOrientation(
            SupplementalEntities.WitherStormHeadEntity head, int index, Rotation rotation) {
        float bodyYaw = rotationYaw(rotation) + (index == 0 ? 180.0F : 0.0F);
        head.rotationYaw = head.prevRotationYaw = bodyYaw;
        head.renderYawOffset = head.prevRenderYawOffset = bodyYaw;
        if (!head.isActive()) {
            head.rotationYawHead = head.prevRotationYawHead = bodyYaw;
            head.rotationPitch = head.prevRotationPitch = 60.0F;
        }
    }

    private static boolean ensureArenaTentacles(WorldServer world,
                                                 BowelsInstanceData.Instance instance,
                                                 BlockPos arena) {
        ArrayList<SickenedEntities.TentacleEntity> loaded = new ArrayList<>();
        for (SickenedEntities.TentacleEntity tentacle : world.getEntitiesWithinAABB(
                SickenedEntities.TentacleEntity.class, new AxisAlignedBB(arena).grow(32.0D, 16.0D, 32.0D))) {
            if (!tentacle.isDead && !tentacle.isCommandBlockStructureTentacle()) loaded.add(tentacle);
        }
        boolean changed = false;
        for (SickenedEntities.TentacleEntity tentacle : loaded) {
            if (!instance.arenaTentacleUuids.contains(tentacle.getUniqueID())) {
                instance.arenaTentacleUuids.add(tentacle.getUniqueID());
                changed = true;
            }
        }
        instance.arenaTentacleUuids.removeIf(uuid -> {
            Entity entity = world.getEntityFromUuid(uuid);
            return entity == null || entity.isDead || !(entity instanceof SickenedEntities.TentacleEntity)
                    || ((SickenedEntities.TentacleEntity) entity).isCommandBlockStructureTentacle();
        });
        if (instance.arenaTentacleTargetCount < 6) {
            instance.arenaTentacleTargetCount = Math.max(loaded.size(), 6 + world.rand.nextInt(6));
            changed = true;
        }
        int missing = instance.arenaTentacleTargetCount - loaded.size();
        if (missing > 0) {
            int before = instance.arenaTentacleUuids.size();
            spawnArenaTentacles(world, instance, arena, missing);
            changed |= instance.arenaTentacleUuids.size() != before;
        }
        return changed;
    }

    private static void placeCenteredPodium(WorldServer world, BlockPos center) {
        Template template = StructureTemplates.get("bowels_podium");
        if (template == null) return;
        Rotation rotation = StructureTemplates.getFeatureRotation(center);
        BlockPos origin = StructureTemplates.getTopAnchoredFeatureOrigin(template, center, rotation);
        StructureTemplates.place(world, "bowels_podium", origin, rotation, true);
    }

    private static void removeCenteredPodium(WorldServer world, BlockPos center) {
        Template template = StructureTemplates.get("bowels_podium");
        if (template == null) return;
        Rotation rotation = StructureTemplates.getFeatureRotation(center);
        BlockPos origin = StructureTemplates.getTopAnchoredFeatureOrigin(template, center, rotation);
        StructureTemplates.remove(world, "bowels_podium", origin, rotation);
    }

    private static void spawnArenaTentacles(WorldServer world, BowelsInstanceData.Instance instance,
                                            BlockPos center, int amount) {
        for (int index = 0; index < amount; index++) {
            BlockPos spawn = null;
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = center.getX() + world.rand.nextInt(50) - 25;
                int z = center.getZ() + world.rand.nextInt(50) - 25;
                BlockPos candidate = new BlockPos(x, center.getY(), z);
                for (int down = 0; down < 10 && world.isAirBlock(candidate.down()); down++) {
                    candidate = candidate.down();
                }
                BlockPos floor = candidate.down();
                if (!world.isAirBlock(candidate) || !world.isSideSolid(floor, EnumFacing.UP)
                        || Math.sqrt(candidate.distanceSq(center)) <= 10.0D) continue;
                if (!world.getEntitiesWithinAABB(SickenedEntities.TentacleEntity.class,
                        new AxisAlignedBB(candidate).grow(10.0D)).isEmpty()) continue;
                if (!hasTentacleSpace(world, candidate)) continue;
                spawn = candidate;
                break;
            }
            if (spawn == null) continue;
            SickenedEntities.TentacleEntity tentacle = new SickenedEntities.TentacleEntity(world);
            tentacle.setPosition(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
            tentacle.rotationYaw = world.rand.nextFloat() * 360.0F;
            tentacle.onInitialSpawn(world.getDifficultyForLocation(spawn), null);
            tentacle.setDormant(true);
            tentacle.lerpCurlTo(0.1F, 0.05F * (float) tentacle.getRNG().nextGaussian(), 8);
            if (world.spawnEntity(tentacle)) instance.arenaTentacleUuids.add(tentacle.getUniqueID());
        }
    }

    private static void spawnArenaHeads(WorldServer world, BowelsInstanceData.Instance instance) {
        for (int index = 0; index < HEAD_OFFSETS.length; index++) {
            SupplementalEntities.WitherStormHeadEntity head = spawnArenaHead(
                    world, getArenaHeadPosition(instance, index), index, Rotation.NONE);
            if (head != null) instance.arenaHeadUuids[index] = head.getUniqueID();
        }
    }

    private static SupplementalEntities.WitherStormHeadEntity spawnArenaHead(
            WorldServer world, BlockPos position, int index, Rotation rotation) {
        SupplementalEntities.WitherStormHeadEntity head = new SupplementalEntities.WitherStormHeadEntity(world);
        head.setIndependentBowelsPart();
        // 上游 EntityType.spawn(alignPosition=false) 把实体水平中心放在方块角点，
        // 不额外 +0.5；凹槽按墙面几何贴合。
        head.setPosition(position.getX(), position.getY(), position.getZ());
        head.onInitialSpawn(world.getDifficultyForLocation(position), null);
        head.setActive(false);
        repairArenaHeadOrientation(head, index, rotation);
        return world.spawnEntity(head) ? head : null;
    }

    private static BlockPos findEntrance(WorldServer world, BowelsInstanceData.Instance instance) {
        BlockPos center = instance.center;
        if (WitherStormConfig.randomBowelsEntrance) {
            for (int radius = 96; radius > 35; radius -= 10) {
                int startAngle = world.rand.nextInt(360);
                for (int offset = 0; offset < 360; offset += 20) {
                    double angle = Math.toRadians(startAngle + offset);
                    int x = (int) (Math.sin(angle) * radius) + center.getX();
                    int z = (int) (Math.cos(angle) * radius) + center.getZ();
                    BlockPos current = new BlockPos(x, 112, z);
                    for (int down = 0; down < 30 && world.isAirBlock(current.down()); down++) {
                        current = current.down();
                    }
                    BlockPos floor = current.down();
                    if (!world.isAirBlock(current) || !hasVerticalSpace(world, floor, 2)
                            || countNearbyAir(world, current, 10, 1600) > 1600) continue;
                    return current;
                }
            }
        }
        return findFixedEntrance(world, center, Rotation.NONE);
    }

    private static BlockPos findFixedEntrance(WorldServer world, BlockPos center, Rotation rotation) {
        BlockPos start = new BlockPos(center.getX(), 120, center.getZ())
                .add(new BlockPos(-112, 0, 0).rotate(rotation));
        BlockPos cursor = start;
        while (world.isAirBlock(cursor) && cursor.getY() > 1) cursor = cursor.down();
        return cursor.up();
    }

    private static int countNearbyAir(WorldServer world, BlockPos center, int radius, int stopAfter) {
        int count = 0;
        for (BlockPos pos : BlockPos.getAllInBox(center.add(-radius, -radius, -radius),
                center.add(radius, radius, radius))) {
            if (world.isAirBlock(pos) && ++count > stopAfter) return count;
        }
        return count;
    }

    private static void migrateLegacyArena(WorldServer world, BowelsInstanceData data,
                                           BowelsInstanceData.Instance instance) {
        if (!instance.prepared || instance.completed) {
            instance.finishCoordinateMigration();
            data.markDirty();
            return;
        }
        BlockPos legacyArena = instance.getLegacyArenaPosition();
        BlockPos arena = instance.getArenaPosition();
        int deltaY = arena.getY() - legacyArena.getY();
        if (deltaY == 0) {
            instance.finishCoordinateMigration();
            data.markDirty();
            return;
        }

        int centerChunkX = legacyArena.getX() >> 4;
        int centerChunkZ = legacyArena.getZ() >> 4;
        for (int x = centerChunkX - 2; x <= centerChunkX + 2; x++) {
            for (int z = centerChunkZ - 2; z <= centerChunkZ + 2; z++) {
                world.getChunkProvider().provideChunk(x, z);
            }
        }

        AxisAlignedBB legacyArea = new AxisAlignedBB(legacyArena).grow(42.0D, 32.0D, 42.0D);
        UUID commandBlockUuid = instance.commandBlockUuid;
        for (Entity entity : new ArrayList<Entity>(world.loadedEntityList)) {
            if (entity.isDead || !legacyArea.contains(entity.getPositionVector())) continue;
            boolean commandBlock = entity instanceof SupplementalEntities.CommandBlockEntity
                    && (commandBlockUuid == null || commandBlockUuid.equals(entity.getUniqueID()))
                    && ((SupplementalEntities.CommandBlockEntity) entity).isIndependentBowelsPart();
            boolean head = entity instanceof SupplementalEntities.WitherStormHeadEntity
                    && ((SupplementalEntities.WitherStormHeadEntity) entity).isIndependentBowelsPart();
            boolean tentacle = entity instanceof SickenedEntities.TentacleEntity;
            boolean podiumCluster = entity instanceof SupplementalEntities.BlockClusterEntity
                    && entity.getDistanceSq(legacyArena) <= 24.0D * 24.0D;
            if (!commandBlock && !head && !tentacle && !podiumCluster) continue;
            moveEntityVertically(entity, deltaY);
            if (commandBlock) {
                ((SupplementalEntities.CommandBlockEntity) entity).setBowelsOwnerUuid(instance.stormUuid);
                instance.commandBlockUuid = entity.getUniqueID();
            }
        }

        removeCenteredPodium(world, legacyArena);
        placeCenteredPodium(world, arena);
        instance.finishCoordinateMigration();
        data.markDirty();
    }

    private static void moveEntityVertically(Entity entity, int deltaY) {
        entity.setPosition(entity.posX, entity.posY + deltaY, entity.posZ);
        entity.prevPosY += deltaY;
        entity.lastTickPosY += deltaY;
    }

    private static float rotationYaw(Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return 90.0F;
            case CLOCKWISE_180:
                return 180.0F;
            case COUNTERCLOCKWISE_90:
                return 270.0F;
            default:
                return 0.0F;
        }
    }

    /** 本移植的肠道主结构固定为 NONE；对应上游 createCommandBlock 的 rotation + 90。 */
    private static void alignBowelsCoreRotation(SupplementalEntities.CommandBlockEntity core) {
        float yaw = 90.0F;
        core.prevRotationYaw = core.rotationYaw = yaw;
        core.prevRotationYawHead = core.rotationYawHead = yaw;
        core.prevRenderYawOffset = core.renderYawOffset = yaw;
    }

    private static BlockPos findFloor(WorldServer world, BlockPos start, int verticalSearch) {
        BlockPos cursor = start;
        for (int step = 0; step < verticalSearch && cursor.getY() > 1; step++, cursor = cursor.down()) {
            if (!world.isAirBlock(cursor) || world.isAirBlock(cursor.down())) continue;
            return cursor.down();
        }
        return null;
    }

    private static boolean hasVerticalSpace(WorldServer world, BlockPos floor, int height) {
        if (!world.isSideSolid(floor, EnumFacing.UP)) return false;
        for (int y = 1; y <= height; y++) {
            BlockPos pos = floor.up(y);
            if (!world.isAirBlock(pos) && !world.getBlockState(pos).getBlock().isReplaceable(world, pos)) return false;
        }
        return true;
    }

    private static boolean hasTentacleSpace(WorldServer world, BlockPos spawn) {
        for (BlockPos pos : BlockPos.getAllInBox(spawn, spawn.add(1, 8, 1))) {
            if (world.getBlockState(pos).getCollisionBoundingBox(world, pos) != Block.NULL_AABB) return false;
        }
        return true;
    }
}
