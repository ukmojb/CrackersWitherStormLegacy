package com.wdcftgg.witherstormmod.common.world;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.tile.AbstractSuperBeaconTileEntity;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ChunkLoadingManager implements ForgeChunkManager.LoadingCallback {
    private static final String MANAGED = "WitherStormLegacyManaged";
    private static final String LOADER_KEY = "LoaderKey";
    private static final String LOADER_KIND = "LoaderKind";
    private static final String CENTER_X = "CenterX";
    private static final String CENTER_Z = "CenterZ";
    private static final String RADIUS = "Radius";
    private static final String BATCH_INDEX = "BatchIndex";
    private static final String CHUNKS = "Chunks";
    private static final String CHUNK_X = "X";
    private static final String CHUNK_Z = "Z";
    private static final int SEGMENT_RADIUS = 6;
    private static final int BOWELS_RADIUS = 3;
    private static final int BEACON_RADIUS = 0;
    private static final long RESTORE_GRACE_TICKS = 600L;
    private static long lastProfileTick = Long.MIN_VALUE;
    private static long profileNanos;

    public static final ChunkLoadingManager INSTANCE = new ChunkLoadingManager();

    private final Map<WorldServer, Map<String, TicketGroup>> groupsByWorld =
            new IdentityHashMap<WorldServer, Map<String, TicketGroup>>();
    private boolean registered;

    private ChunkLoadingManager() {
    }

    public synchronized void register(WitherStormMod mod) {
        if (registered) return;
        ForgeChunkManager.setForcedChunkLoadingCallback(mod, this);
        registered = true;
    }

    @Override
    public synchronized void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        if (!(world instanceof WorldServer)) {
            for (ForgeChunkManager.Ticket ticket : tickets) ForgeChunkManager.releaseTicket(ticket);
            return;
        }
        WorldServer serverWorld = (WorldServer) world;
        Map<String, List<ForgeChunkManager.Ticket>> restored = new LinkedHashMap<String, List<ForgeChunkManager.Ticket>>();
        for (ForgeChunkManager.Ticket ticket : tickets) {
            NBTTagCompound data = ticket.getModData();
            if (!data.getBoolean(MANAGED) || !data.hasKey(LOADER_KEY, 8)) continue;
            String key = data.getString(LOADER_KEY);
            List<ForgeChunkManager.Ticket> loaderTickets = restored.get(key);
            if (loaderTickets == null) {
                loaderTickets = new ArrayList<ForgeChunkManager.Ticket>();
                restored.put(key, loaderTickets);
            }
            loaderTickets.add(ticket);
        }

        Map<String, TicketGroup> worldGroups = groups(serverWorld);
        for (Map.Entry<String, List<ForgeChunkManager.Ticket>> entry : restored.entrySet()) {
            List<ForgeChunkManager.Ticket> loaderTickets = entry.getValue();
            Collections.sort(loaderTickets, Comparator.comparingInt(ticket -> ticket.getModData().getInteger(BATCH_INDEX)));
            NBTTagCompound first = loaderTickets.get(0).getModData();
            TicketGroup existing = worldGroups.remove(entry.getKey());
            if (existing != null) existing.release();
            TicketGroup group = new TicketGroup(serverWorld, entry.getKey(), first.getString(LOADER_KIND),
                    first.getInteger(CENTER_X), first.getInteger(CENTER_Z),
                    Math.max(0, first.getInteger(RADIUS)), loaderTickets, serverWorld.getTotalWorldTime());
            worldGroups.put(entry.getKey(), group);
            group.restoreChunks();
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.world.isRemote || !(event.world instanceof WorldServer)) return;
        INSTANCE.tickWorld((WorldServer) event.world);
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote && event.getWorld() instanceof WorldServer) {
            INSTANCE.forgetWorld((WorldServer) event.getWorld());
        }
    }

    private synchronized void tickWorld(WorldServer world) {
        long profileStart = System.nanoTime();
        long now = world.getTotalWorldTime();
        Map<String, TicketGroup> existingGroups = groupsByWorld.get(world);
        if (!WitherStormConfig.shouldChunkLoadWhenNoPlayers && world.playerEntities.isEmpty()) {
            if (existingGroups != null) {
                for (TicketGroup group : existingGroups.values()) group.suspend();
            }
            return;
        }
        if (existingGroups != null) {
            for (TicketGroup group : existingGroups.values()) group.resume(now);
        }

        List<Entity> entities = new ArrayList<Entity>(world.loadedEntityList);
        for (Entity entity : entities) {
            if (entity.isDead) continue;
            if (entity instanceof WitherStormEntity) {
                String key = entityKey("storm", entity.getUniqueID());
                touch(world, key, "storm", entity.chunkCoordX, entity.chunkCoordZ,
                        WitherStormConfig.chunkLoadingRadius, now);
            } else if (entity instanceof SupplementalEntities.WitherStormSegmentEntity
                    && !((SupplementalEntities.WitherStormSegmentEntity) entity).isIndependentBowelsPart()) {
                String key = entityKey("segment", entity.getUniqueID());
                touch(world, key, "segment", entity.chunkCoordX,
                        entity.chunkCoordZ, SEGMENT_RADIUS, now);
            }
        }

        for (TileEntity tile : new ArrayList<TileEntity>(
                world.loadedTileEntityList)) {
            if (tile instanceof AbstractSuperBeaconTileEntity
                    && !tile.isInvalid()) {
                ChunkPos center = new ChunkPos(tile.getPos());
                String key = beaconKey(tile.getPos());
                touch(world, key, "super_beacon", center.x, center.z,
                        BEACON_RADIUS, now);
            }
        }

        if (world.provider.getDimension() == BowelsDimensions.DIMENSION_ID) {
            for (BowelsInstanceData.Instance instance : BowelsInstanceData.get(world).getInstances()) {
                if (instance.completed) continue;
                ChunkPos center = new ChunkPos(instance.getArenaPosition());
                String key = bowelsKey(instance.stormUuid);
                touch(world, key, "bowels", center.x, center.z, BOWELS_RADIUS, now);
            }
        }

        Map<String, TicketGroup> worldGroups = groupsByWorld.get(world);
        if (worldGroups == null || worldGroups.isEmpty()) return;
        List<String> stale = new ArrayList<String>();
        for (TicketGroup group : worldGroups.values()) {
            if (now - group.lastSeenTick > RESTORE_GRACE_TICKS) stale.add(group.key);
        }
        for (String key : stale) release(world, key);
        long elapsed = System.nanoTime() - profileStart;
        profileNanos += elapsed;
        if (now - lastProfileTick >= 200) {
            WitherStormMod.LOGGER.info("ChunkLoading worldTick profile: "
                    + String.format(java.util.Locale.ROOT, "%.3f",
                    profileNanos / 1000000.0D / 200) + "ms/tick");
            lastProfileTick = now;
            profileNanos = 0;
        }
    }

    private void touch(WorldServer world, String key, String kind, int centerX, int centerZ, int radius, long now) {
        Map<String, TicketGroup> worldGroups = groups(world);
        TicketGroup group = worldGroups.get(key);
        if (group == null) {
            group = new TicketGroup(world, key, kind, centerX, centerZ, radius,
                    new ArrayList<ForgeChunkManager.Ticket>(), now);
            worldGroups.put(key, group);
        }
        group.lastSeenTick = now;
        if (group.centerX != centerX || group.centerZ != centerZ || group.radius != radius || !group.isComplete()) {
            group.reconfigure(centerX, centerZ, radius);
        }
    }

    public synchronized void releaseEntity(World world, String kind, UUID uuid) {
        if (world instanceof WorldServer) release((WorldServer) world, entityKey(kind, uuid));
    }

    public synchronized void releaseBowelsInstance(World world, UUID stormUuid) {
        if (world instanceof WorldServer) release((WorldServer) world, bowelsKey(stormUuid));
    }

    public synchronized void releaseSuperBeacon(World world, BlockPos position) {
        if (world instanceof WorldServer && position != null) {
            release((WorldServer) world, beaconKey(position));
        }
    }


    public static List<String> describeGroups(WorldServer world) {
        List<String> lines = new ArrayList<String>();
        Map<String, TicketGroup> groups = INSTANCE.groupsByWorld.get(world);
        if (groups == null) return lines;
        for (TicketGroup group : groups.values()) {
            int chunkCount = 0;
            for (ForgeChunkManager.Ticket ticket : group.tickets) {
                chunkCount += ticket.getChunkList().size();
            }
            lines.add(group.key + " kind=" + group.kind
                    + " center=" + group.centerX + "," + group.centerZ
                    + " radius=" + group.radius
                    + " tickets=" + group.tickets.size()
                    + " chunks=" + chunkCount
                    + " suspended=" + group.suspended);
        }
        Collections.sort(lines);
        return lines;
    }

    public static List<LoaderDescription> describeStorms(WorldServer world) {
        List<LoaderDescription> descriptions = new ArrayList<LoaderDescription>();
        Map<String, TicketGroup> groups = INSTANCE.groupsByWorld.get(world);
        if (groups == null) return descriptions;
        for (TicketGroup group : groups.values()) {
            if (!group.key.startsWith("storm:")) continue;
            try {
                descriptions.add(describe(group,
                        UUID.fromString(group.key.substring("storm:".length()))));
            } catch (IllegalArgumentException ignored) {

            }
        }
        Collections.sort(descriptions,
                Comparator.comparing(description -> description.uuid.toString()));
        return descriptions;
    }

    @Nullable
    public static LoaderDescription describeStorm(WorldServer world, UUID uuid) {
        Map<String, TicketGroup> groups = INSTANCE.groupsByWorld.get(world);
        if (groups == null) return null;
        TicketGroup group = groups.get(entityKey("storm", uuid));
        return group == null ? null : describe(group, uuid);
    }

    private static LoaderDescription describe(TicketGroup group, UUID uuid) {
        int chunks = 0;
        for (ForgeChunkManager.Ticket ticket : group.tickets) {
            chunks += ticket.getChunkList().size();
        }
        return new LoaderDescription(uuid, group.centerX, group.centerZ, group.radius,
                group.tickets.size(), chunks, group.suspended);
    }

    public static final class LoaderDescription {
        public final UUID uuid;
        public final int centerX;
        public final int centerZ;
        public final int radius;
        public final int ticketCount;
        public final int chunkCount;
        public final boolean suspended;

        private LoaderDescription(UUID uuid, int centerX, int centerZ, int radius,
                                  int ticketCount, int chunkCount, boolean suspended) {
            this.uuid = uuid;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            this.ticketCount = ticketCount;
            this.chunkCount = chunkCount;
            this.suspended = suspended;
        }
    }


    public static void refresh(WorldServer world) {
        Map<String, TicketGroup> groups = INSTANCE.groupsByWorld.get(world);
        if (groups == null) return;
        long now = world.getTotalWorldTime();
        for (TicketGroup group : groups.values()) {
            if (group.suspended) {
                group.resume(now);
            } else {
                group.reconfigure(group.centerX, group.centerZ, group.radius);
            }
        }
    }

    private void release(WorldServer world, String key) {
        Map<String, TicketGroup> worldGroups = groupsByWorld.get(world);
        if (worldGroups == null) return;
        TicketGroup group = worldGroups.remove(key);
        if (group != null) group.release();
        if (worldGroups.isEmpty()) groupsByWorld.remove(world);
    }

    private void forgetWorld(WorldServer world) {
        groupsByWorld.remove(world);
    }

    private Map<String, TicketGroup> groups(WorldServer world) {
        Map<String, TicketGroup> groups = groupsByWorld.get(world);
        if (groups == null) {
            groups = new LinkedHashMap<String, TicketGroup>();
            groupsByWorld.put(world, groups);
        }
        return groups;
    }

    static String entityKey(String kind, UUID uuid) {
        return kind + ":" + uuid;
    }

    static String bowelsKey(UUID stormUuid) {
        return "bowels:" + stormUuid;
    }

    static String beaconKey(BlockPos position) {
        return "super_beacon:" + position.toLong();
    }

    static List<ChunkPos> createChunkPlan(int centerX, int centerZ, int radius) {
        List<ChunkPos> chunks = new ArrayList<ChunkPos>();
        for (int offsetX = -radius; offsetX <= radius; offsetX++) {
            for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                chunks.add(new ChunkPos(centerX + offsetX, centerZ + offsetZ));
            }
        }
        Collections.sort(chunks, (first, second) -> {
            int firstX = first.x - centerX;
            int firstZ = first.z - centerZ;
            int secondX = second.x - centerX;
            int secondZ = second.z - centerZ;
            int comparison = Integer.compare(Math.max(Math.abs(firstX), Math.abs(firstZ)),
                    Math.max(Math.abs(secondX), Math.abs(secondZ)));
            if (comparison != 0) return comparison;
            comparison = Integer.compare(firstX * firstX + firstZ * firstZ, secondX * secondX + secondZ * secondZ);
            if (comparison != 0) return comparison;
            comparison = Integer.compare(first.x, second.x);
            return comparison != 0 ? comparison : Integer.compare(first.z, second.z);
        });
        return chunks;
    }

    static int requiredTicketCount(int chunkCount, int ticketCapacity) {
        if (chunkCount <= 0) return 0;
        if (ticketCapacity <= 0) return 1;
        return (chunkCount + ticketCapacity - 1) / ticketCapacity;
    }

    private static final class TicketGroup {
        private final WorldServer world;
        private final String key;
        private final String kind;
        private final List<ForgeChunkManager.Ticket> tickets;
        private int centerX;
        private int centerZ;
        private int radius;
        private int configuredChunks;
        private long lastSeenTick;
        private boolean suspended;

        private TicketGroup(WorldServer world, String key, String kind, int centerX, int centerZ, int radius,
                            List<ForgeChunkManager.Ticket> tickets, long lastSeenTick) {
            this.world = world;
            this.key = key;
            this.kind = kind;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            this.tickets = tickets;
            this.lastSeenTick = lastSeenTick;
        }

        private void restoreChunks() {
            configuredChunks = 0;
            for (ForgeChunkManager.Ticket ticket : tickets) {
                NBTTagList chunks = ticket.getModData().getTagList(CHUNKS, 10);
                for (int index = 0; index < chunks.tagCount(); index++) {
                    NBTTagCompound chunk = chunks.getCompoundTagAt(index);
                    ForgeChunkManager.forceChunk(ticket, new ChunkPos(chunk.getInteger(CHUNK_X), chunk.getInteger(CHUNK_Z)));
                    configuredChunks++;
                }
            }
        }

        private boolean isComplete() {
            int diameter = radius * 2 + 1;
            return configuredChunks == diameter * diameter;
        }

        private void suspend() {
            if (suspended) return;
            for (ForgeChunkManager.Ticket ticket : tickets) {
                for (ChunkPos position : new ArrayList<ChunkPos>(ticket.getChunkList())) {
                    ForgeChunkManager.unforceChunk(ticket, position);
                }
            }
            configuredChunks = 0;
            suspended = true;
        }

        private void resume(long currentTick) {
            if (!suspended) return;
            suspended = false;
            lastSeenTick = currentTick;
            reconfigure(centerX, centerZ, radius);
        }

        private void reconfigure(int newCenterX, int newCenterZ, int newRadius) {
            suspended = false;
            centerX = newCenterX;
            centerZ = newCenterZ;
            radius = Math.max(0, newRadius);
            List<ChunkPos> desired = createChunkPlan(centerX, centerZ, radius);
            Set<ChunkPos> desiredSet = new HashSet<ChunkPos>(desired);
            int capacity = ForgeChunkManager.getMaxChunkDepthFor(Tags.MOD_ID);
            int ticketCount = requiredTicketCount(desired.size(), capacity);
            Set<ForgeChunkManager.Ticket> changedTickets = Collections.newSetFromMap(
                    new IdentityHashMap<ForgeChunkManager.Ticket, Boolean>());

            while (tickets.size() < ticketCount) {
                ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(
                        WitherStormMod.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
                if (ticket == null) break;
                tickets.add(ticket);
                changedTickets.add(ticket);
            }
            while (tickets.size() > ticketCount) {
                ForgeChunkManager.Ticket ticket = tickets.remove(tickets.size() - 1);
                ForgeChunkManager.releaseTicket(ticket);
            }

            Map<ForgeChunkManager.Ticket, List<ChunkPos>> assignments =
                    new IdentityHashMap<ForgeChunkManager.Ticket, List<ChunkPos>>();
            Set<ChunkPos> assigned = new HashSet<ChunkPos>();
            for (ForgeChunkManager.Ticket ticket : tickets) {
                List<ChunkPos> retained = new ArrayList<ChunkPos>();
                for (ChunkPos previous : new ArrayList<ChunkPos>(ticket.getChunkList())) {
                    if (desiredSet.contains(previous) && assigned.add(previous)) {
                        retained.add(previous);
                    } else {
                        ForgeChunkManager.unforceChunk(ticket, previous);
                        changedTickets.add(ticket);
                    }
                }
                assignments.put(ticket, retained);
            }

            int batchCapacity = capacity <= 0 ? Integer.MAX_VALUE : capacity;
            for (ChunkPos position : desired) {
                if (assigned.contains(position)) continue;
                ForgeChunkManager.Ticket destination = findTicketWithCapacity(tickets, assignments, batchCapacity);
                if (destination == null) break;
                ForgeChunkManager.forceChunk(destination, position);
                assignments.get(destination).add(position);
                assigned.add(position);
                changedTickets.add(destination);
            }

            configuredChunks = assigned.size();
            for (int ticketIndex = 0; ticketIndex < tickets.size(); ticketIndex++) {
                ForgeChunkManager.Ticket ticket = tickets.get(ticketIndex);
                NBTTagCompound data = ticket.getModData();
                data.setBoolean(MANAGED, true);
                data.setString(LOADER_KEY, key);
                data.setString(LOADER_KIND, kind);
                data.setInteger(CENTER_X, centerX);
                data.setInteger(CENTER_Z, centerZ);
                data.setInteger(RADIUS, radius);
                data.setInteger(BATCH_INDEX, ticketIndex);
                if (changedTickets.contains(ticket) || !data.hasKey(CHUNKS, 9)) {
                    NBTTagList chunks = new NBTTagList();
                    for (ChunkPos position : assignments.get(ticket)) {
                        NBTTagCompound chunk = new NBTTagCompound();
                        chunk.setInteger(CHUNK_X, position.x);
                        chunk.setInteger(CHUNK_Z, position.z);
                        chunks.appendTag(chunk);
                    }
                    data.setTag(CHUNKS, chunks);
                }
            }
            if (configuredChunks < desired.size()) {
                WitherStormMod.LOGGER.error("Unable to allocate enough Forge chunk tickets for {}: loaded {}/{} chunks",
                        key, configuredChunks, desired.size());
            }
        }

        @Nullable
        private static ForgeChunkManager.Ticket findTicketWithCapacity(
                List<ForgeChunkManager.Ticket> tickets,
                Map<ForgeChunkManager.Ticket, List<ChunkPos>> assignments,
                int capacity) {
            for (ForgeChunkManager.Ticket ticket : tickets) {
                List<ChunkPos> chunks = assignments.get(ticket);
                if (chunks != null && chunks.size() < capacity) return ticket;
            }
            return null;
        }

        private void release() {
            for (ForgeChunkManager.Ticket ticket : tickets) ForgeChunkManager.releaseTicket(ticket);
            tickets.clear();
            configuredChunks = 0;
        }
    }
}
