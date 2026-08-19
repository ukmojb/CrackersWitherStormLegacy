package com.wdcftgg.witherstormmod.common.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BowelsInstanceData extends WorldSavedData {
    public static final String DATA_NAME = "witherstormmod_bowels_instances";
    public static final int CURRENT_COORDINATE_VERSION = 1;
    private final List<Instance> instances = new ArrayList<>();

    public BowelsInstanceData() {
        super(DATA_NAME);
    }

    public BowelsInstanceData(String name) {
        super(name);
    }

    public static BowelsInstanceData get(World world) {
        BowelsInstanceData data = (BowelsInstanceData) world.getPerWorldStorage().getOrLoadData(BowelsInstanceData.class, DATA_NAME);
        if (data == null) {
            data = new BowelsInstanceData();
            world.getPerWorldStorage().setData(DATA_NAME, data);
        }
        return data;
    }

    public Instance getOrCreate(UUID stormUuid, int originDimension, BlockPos origin) {
        Instance existing = get(stormUuid);
        if (existing != null) return existing;
        int index = instances.size();
        int gridX = index % 16;
        int gridZ = index / 16;
        BlockPos center = new BlockPos(gridX * 1024, 96, gridZ * 1024);
        Instance instance = new Instance(stormUuid, center, originDimension, origin);
        instances.add(instance);
        markDirty();
        return instance;
    }

    /**
     * 对应上游 WitherStormBowelsManager.getAvailableStructure 的结构生成检查：
     * 世界禁用结构生成时向全体玩家发红色警告并拒绝访问肠道。
     */
    public static boolean checkStructuresEnabled(World world) {
        if (world.getWorldInfo().isMapFeaturesEnabled()) return true;
        MinecraftServer server = world.getMinecraftServer();
        if (server != null) {
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                player.sendMessage(new TextComponentTranslation(
                        "chat.witherstormmod.bowels.structuresDisabled")
                        .setStyle(new Style().setColor(TextFormatting.RED)));
            }
        }
        return false;
    }

    /** 上游 WitherStormBowelsManager.get：已完成实例不返回，重进时会创建新实例。 */
    @Nullable
    public Instance get(UUID stormUuid) {
        for (Instance instance : instances) {
            if (instance.stormUuid.equals(stormUuid) && !instance.completed) return instance;
        }
        return null;
    }

    /** 供死亡收尾等必须访问已完成实例的路径使用。 */
    @Nullable
    public Instance getIncludingCompleted(UUID stormUuid) {
        for (Instance instance : instances) if (instance.stormUuid.equals(stormUuid)) return instance;
        return null;
    }

    @Nullable
    public Instance findContaining(BlockPos pos) {
        for (Instance instance : instances) {
            if (Math.abs(pos.getX() - instance.center.getX()) <= 192 && Math.abs(pos.getZ() - instance.center.getZ()) <= 192) return instance;
        }
        return null;
    }

    public List<Instance> getInstances() {
        return java.util.Collections.unmodifiableList(instances);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        instances.clear();
        NBTTagList list = compound.getTagList("Instances", 10);
        for (int i = 0; i < list.tagCount(); i++) instances.add(Instance.read(list.getCompoundTagAt(i)));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (Instance instance : instances) list.appendTag(instance.write());
        compound.setTag("Instances", list);
        return compound;
    }

    public static class Instance {
        public final UUID stormUuid;
        public final BlockPos center;
        public final int originDimension;
        public final BlockPos origin;
        public UUID commandBlockUuid;
        public final UUID[] arenaHeadUuids = new UUID[2];
        public final List<UUID> arenaTentacleUuids = new ArrayList<>();
        public int arenaTentacleTargetCount;
        public UUID killerUuid;
        public boolean prepared;
        public boolean completed;
        public int bossPhase;
        public int bossPhaseTicks;
        public BlockPos arenaPosition;
        /** 肠道网络主结构模板的放置原点 Y。新版按上游 BowelsStructure 对齐 100，
         * 旧存档在升级前按旧锚点 88 放置，只能以 88 + 28 的墙头凹槽高度修复头部。 */
        public int networkBaseY = 88;
        private int coordinateVersion = CURRENT_COORDINATE_VERSION;

        private Instance(UUID stormUuid, BlockPos center, int originDimension, BlockPos origin) {
            this.stormUuid = stormUuid;
            this.center = center;
            this.originDimension = originDimension;
            this.origin = origin;
        }

        private NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setUniqueId("Storm", stormUuid);
            tag.setLong("Center", center.toLong());
            tag.setInteger("OriginDimension", originDimension);
            tag.setLong("Origin", origin.toLong());
            if (commandBlockUuid != null) tag.setUniqueId("CommandBlock", commandBlockUuid);
            for (int index = 0; index < arenaHeadUuids.length; index++) {
                if (arenaHeadUuids[index] != null) tag.setUniqueId("ArenaHead" + index, arenaHeadUuids[index]);
            }
            NBTTagList tentacles = new NBTTagList();
            for (UUID uuid : arenaTentacleUuids) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setUniqueId("UUID", uuid);
                tentacles.appendTag(entry);
            }
            tag.setTag("ArenaTentacles", tentacles);
            tag.setInteger("ArenaTentacleTargetCount", arenaTentacleTargetCount);
            if (killerUuid != null) tag.setUniqueId("Killer", killerUuid);
            tag.setBoolean("Prepared", prepared);
            tag.setBoolean("Completed", completed);
            tag.setInteger("BossPhase", bossPhase);
            tag.setInteger("BossPhaseTicks", bossPhaseTicks);
            tag.setInteger("CoordinateVersion", coordinateVersion);
            if (arenaPosition != null) tag.setLong("ArenaPosition", arenaPosition.toLong());
            tag.setInteger("NetworkBaseY", networkBaseY);
            return tag;
        }

        private static Instance read(NBTTagCompound tag) {
            Instance instance = new Instance(tag.getUniqueId("Storm"), BlockPos.fromLong(tag.getLong("Center")),
                    tag.getInteger("OriginDimension"), BlockPos.fromLong(tag.getLong("Origin")));
            if (tag.hasUniqueId("CommandBlock")) instance.commandBlockUuid = tag.getUniqueId("CommandBlock");
            for (int index = 0; index < instance.arenaHeadUuids.length; index++) {
                if (tag.hasUniqueId("ArenaHead" + index)) {
                    instance.arenaHeadUuids[index] = tag.getUniqueId("ArenaHead" + index);
                }
            }
            NBTTagList tentacles = tag.getTagList("ArenaTentacles", 10);
            for (int index = 0; index < tentacles.tagCount(); index++) {
                NBTTagCompound entry = tentacles.getCompoundTagAt(index);
                if (entry.hasUniqueId("UUID")) instance.arenaTentacleUuids.add(entry.getUniqueId("UUID"));
            }
            instance.arenaTentacleTargetCount = tag.hasKey("ArenaTentacleTargetCount", 3)
                    ? tag.getInteger("ArenaTentacleTargetCount") : instance.arenaTentacleUuids.size();
            if (tag.hasUniqueId("Killer")) instance.killerUuid = tag.getUniqueId("Killer");
            instance.prepared = tag.getBoolean("Prepared");
            instance.completed = tag.getBoolean("Completed");
            instance.bossPhase = tag.getInteger("BossPhase");
            instance.bossPhaseTicks = tag.getInteger("BossPhaseTicks");
            instance.coordinateVersion = tag.hasKey("CoordinateVersion", 3)
                    ? tag.getInteger("CoordinateVersion") : 0;
            instance.arenaPosition = tag.hasKey("ArenaPosition", 4)
                    ? BlockPos.fromLong(tag.getLong("ArenaPosition")) : null;
            instance.networkBaseY = tag.hasKey("NetworkBaseY", 3)
                    ? tag.getInteger("NetworkBaseY") : 88;
            return instance;
        }

        /** The generated network is anchored at Y=96, while upstream arena offsets use a Y=0 center. */
        public BlockPos getStructureCenter() {
            return new BlockPos(center.getX(), 0, center.getZ());
        }

        public BlockPos getArenaPosition() {
            int x = arenaPosition == null ? center.getX() - 3 : arenaPosition.getX();
            int z = arenaPosition == null ? center.getZ() : arenaPosition.getZ();
            return new BlockPos(x, 110, z);
        }

        /** 上游墙头偏移 Y=128 相对起始块原点 Y=100，因此凹槽位于网络原点 +28。 */
        public int getArenaHeadY() {
            return networkBaseY + 28;
        }

        public boolean needsCoordinateMigration() {
            return coordinateVersion < CURRENT_COORDINATE_VERSION
                    || arenaPosition != null && center.getY() != 0
                    && arenaPosition.getY() == center.getY() + 110;
        }

        public BlockPos getLegacyArenaPosition() {
            if (arenaPosition != null && needsCoordinateMigration()) return arenaPosition;
            return center.add(-3, 110, 0);
        }

        public void finishCoordinateMigration() {
            arenaPosition = getArenaPosition();
            coordinateVersion = CURRENT_COORDINATE_VERSION;
        }
    }
}
