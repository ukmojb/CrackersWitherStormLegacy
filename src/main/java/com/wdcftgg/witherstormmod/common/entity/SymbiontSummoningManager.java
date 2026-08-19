package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.item.FormidibombItem;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.world.BowelsDimensions;
import com.wdcftgg.witherstormmod.common.world.BowelsInstanceData;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 对齐上游 SymbiontSummoningManager 的服务端状态和生成规则。
 * 召唤冷却、玩家保护和最近一次召唤记录都写入实体 NBT，避免区块重载后重复召唤。
 */
public final class SymbiontSummoningManager {
    private static final String PLAYER_DATA_KEY = "WitherStormLegacySymbiont";
    private static final String SUMMONING_DATA = "SummoningData";
    private static final String SUMMONER = "Summoner";
    private static final String SUMMONER_PHASE = "SummonerPhase";
    private static final String SUMMONING_COOLDOWN_UNTIL = "SummoningCooldownUntil";
    private static final String INVULNERABLE_UNTIL = "InvulnerableUntil";

    private final WitherStormEntity storm;
    private int timeTillCanSummonSymbiont;

    public SymbiontSummoningManager(WitherStormEntity storm) {
        this.storm = storm;
    }

    public void tick() {
        if (storm.world.isRemote || storm.isDead) return;
        if (timeTillCanSummonSymbiont > 0) --timeTillCanSummonSymbiont;
        if (!WitherStormConfig.canSummonSymbiont) return;
        int delay = MathHelper.clamp(WitherStormConfig.minimumSpawnCheckInterval, 1, 240) * 20;
        // 上游字节码把随机乘数放在取模之外：`ticks % delay * (nextInt(3)+1) == 0`
        // 数学上等价于每 delay tick 检查一次，且每 tick 都会消耗一次 RNG。
        // 这里逐字保留该表达式与优先级，不再引入更稀疏的随机化间隔。
        if (storm.ticksExisted % delay * (storm.getRNG().nextInt(3) + 1) != 0) return;

        List<EntityPlayer> players = storm.world.getEntitiesWithinAABB(EntityPlayer.class,
                storm.getSearchBox(), player -> player != null && player.isEntityAlive());
        // 保留上游比较器的 floor(真实距离差) 及稳定排序语义。
        Collections.sort(players, (first, second) -> MathHelper.floor(
                first.getDistance(storm) - second.getDistance(storm)));
        for (EntityPlayer player : players) {
            if (!playerApplicable(player)) continue;
            if (canSummonSymbiont()) {
                summonSymbiont(player);
            }
            break;
        }
    }

    protected boolean canSummonSymbiont() {
        if (storm.isDeadOrPlayingDead() || !storm.isEntityAlive()) return false;
        if (storm.getPhase() < 5 || storm.getConsumedMass() < storm.getConsumptionAmountForPhase(5)) return false;
        if (timeTillCanSummonSymbiont > 0 || storm.hasRecentlyBeenRevived()) return false;
        if (storm.isAttractingFormidibomb()) return false;
        if (storm.getBowelsCommandBlock() != null && storm.getBowelsCommandBlock().getHealth()
                < storm.getBowelsCommandBlock().getMaxHealth()) return false;

        AxisAlignedBB search = storm.getSearchBox().grow(50.0D);
        for (Entity entity : storm.world.getEntitiesWithinAABB(Entity.class, search)) {
            if (entity == storm || entity.isDead) continue;
            if (entity instanceof PowerfulExplosiveEntity.FormidibombEntity) {
                if (((PowerfulExplosiveEntity.FormidibombEntity) entity).getStartFuse() > 0) return false;
            } else if (entity instanceof SickenedEntities.WitheredSymbiontEntity) return false;
        }

        if (isPlayerInsideBowelsInstance()) return false;
        return true;
    }

    protected boolean playerApplicable(EntityPlayer player) {
        if (!player.isEntityAlive() || player.isSpectator() || player.capabilities.disableDamage) return false;
        double deltaX = player.posX - storm.posX;
        double deltaZ = player.posZ - storm.posZ;
        double followRange = storm.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        if (deltaX * deltaX + deltaZ * deltaZ > followRange * followRange) return false;
        if (shouldIgnorePlayer(player)) return false;
        if (hasRecentSummon(player, storm)) return false;

        for (ItemStack stack : getAllInventoryStacks(player)) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() == ModItems.get("command_block_book")) return false;
            if (stack.getItem() instanceof FormidibombItem && FormidibombItem.getStartFuse(stack) > 0) return false;
            if (storm.getPhase() > 5 && isCommandBlockTool(stack)) return false;
        }
        return true;
    }

    public void summonSymbiont(EntityPlayer player) {
        // 上游用 cos 算 X、sin 算 Z；此前移植写反会导致召唤点绕风暴旋转 90°。
        float angle = -(float) Math.atan2(player.posX - storm.posX, player.posZ - storm.posZ);
        float spawnX = MathHelper.cos(angle) * 30.0F + (float) storm.posX;
        float spawnZ = MathHelper.sin(angle) * 30.0F + (float) storm.posZ;

        for (int attempt = 0; attempt < 10; attempt++) {
            int randomX = MathHelper.floor(spawnX) + (int) (storm.getRNG().nextGaussian() * 10.0D) + 5;
            int randomZ = MathHelper.floor(spawnZ) + (int) (storm.getRNG().nextGaussian() * 10.0D) + 5;
            BlockPos spawnPos = findHighestSpawnPos(randomX, randomZ);
            if (spawnPos == null) continue;

            SickenedEntities.WitheredSymbiontEntity symbiont = new SickenedEntities.WitheredSymbiontEntity(storm.world);
            IBlockState floorState = storm.world.getBlockState(spawnPos);
            AxisAlignedBB floorCollision = floorState.getCollisionBoundingBox(storm.world, spawnPos);
            double y = spawnPos.getY() + (floorCollision == Block.NULL_AABB
                    ? 1.0D : floorCollision.maxY);
            BlockPos spawnEntityPos = new BlockPos(spawnPos.getX(), MathHelper.floor(y), spawnPos.getZ());
            if (!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(
                    EntityLiving.SpawnPlacementType.ON_GROUND, storm.world, spawnEntityPos)) continue;
            symbiont.setPosition(spawnPos.getX() + 0.5D, y, spawnPos.getZ() + 0.5D);
            AxisAlignedBB body = symbiont.getEntityBoundingBox();
            if (!storm.world.checkNoEntityCollision(body, symbiont)
                    || !storm.world.getCollisionBoxes(symbiont, body).isEmpty()
                    || storm.world.containsAnyLiquid(body)) continue;
            lookAt(symbiont, player);
            symbiont.setOwner(storm);
            symbiont.setAttackTarget(player);
            if (!storm.world.spawnEntity(symbiont)) continue;
            if (!ForgeEventFactory.doSpecialSpawn(symbiont, storm.world,
                    (float) symbiont.posX, (float) symbiont.posY, (float) symbiont.posZ)) {
                symbiont.onInitialSpawn(storm.world.getDifficultyForLocation(symbiont.getPosition()), null);
            }
            symbiont.spawnExplosionParticle();

            if (storm.world instanceof WorldServer) {
                WorldServer world = (WorldServer) storm.world;
                for (EntityPlayer nearby : world.getEntitiesWithinAABB(EntityPlayer.class,
                        storm.getSearchBox(), candidate -> candidate != null && candidate.isEntityAlive())) {
                    if (nearby instanceof EntityPlayerMP) {
                        CriteriaTriggers.SUMMONED_ENTITY.trigger((EntityPlayerMP) nearby, symbiont);
                    }
                }
                double commandSpreadX = storm.getRNG().nextGaussian();
                double commandSpreadY = storm.getRNG().nextGaussian();
                double commandSpreadZ = storm.getRNG().nextGaussian();
                ModNetwork.sendCommandBlockParticles(world, symbiont.getPositionVector(), 20,
                        commandSpreadX, commandSpreadY, commandSpreadZ, 0.2D,
                        ModNetwork.COMMAND_BLOCK_PARTICLES_GAUSSIAN);
                double poofSpreadX = storm.getRNG().nextGaussian();
                double poofSpreadY = storm.getRNG().nextGaussian();
                double poofSpreadZ = storm.getRNG().nextGaussian();
                world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                        symbiont.posX, symbiont.posY, symbiont.posZ, 20,
                        poofSpreadX, poofSpreadY, poofSpreadZ, 0.01D);
            }
            storm.world.playSound(null, storm.getPosition(), ModSounds.get("command_block_summon"),
                    SoundCategory.HOSTILE, 15.0F, 1.0F);
            symbiont.playSound(ModSounds.get("withered_symbiont_spawn"), 12.0F, 1.0F);
            timeTillCanSummonSymbiont = MathHelper.clamp(
                    WitherStormConfig.witherStormSummoningDelay, 1, 20) * 1200
                    + storm.getRNG().nextInt(12000);
            markSummoned(player, storm);
            return;
        }
    }

    @Nullable
    private BlockPos findHighestSpawnPos(int x, int z) {
        Integer highest = null;
        BlockPos result = null;
        for (int offsetX = -5; offsetX <= 5; offsetX++) {
            for (int offsetZ = -5; offsetZ <= 5; offsetZ++) {
                int candidateX = x + offsetX;
                int candidateZ = z + offsetZ;
                // This local scan already returns the supporting block Y;
                // convert its collision shape to the entity feet position below.
                int y = getMotionBlockingNoLeavesHeight(candidateX, candidateZ);
                if (y < 0) continue;
                if (highest != null && y <= highest) continue;
                highest = y;
                result = new BlockPos(candidateX, y, candidateZ);
            }
        }
        if (result == null) return null;
        IBlockState state = storm.world.getBlockState(result);
        if (!storm.world.isSideSolid(result, EnumFacing.UP)
                || state.getCollisionBoundingBox(storm.world, result) == Block.NULL_AABB) return null;
        return result;
    }

    private int getMotionBlockingNoLeavesHeight(int x, int z) {
        BlockPos column = new BlockPos(x, 0, z);
        int top = Math.min(storm.world.getActualHeight() - 1,
                storm.world.getHeight(column).getY() - 1);
        for (int y = top; y >= 0; y--) {
            BlockPos position = new BlockPos(x, y, z);
            IBlockState state = storm.world.getBlockState(position);
            if (state.getBlock().isLeaves(state, storm.world, position)) continue;
            if (state.getMaterial().blocksMovement() || state.getMaterial().isLiquid()) return y;
        }
        return -1;
    }

    private static void lookAt(SickenedEntities.WitheredSymbiontEntity symbiont, EntityPlayer player) {
        double dx = player.posX - symbiont.posX;
        double dy = player.posY + player.getEyeHeight() - (symbiont.posY + symbiont.getEyeHeight());
        double dz = player.posZ - symbiont.posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        symbiont.rotationYaw = (float) (MathHelper.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        symbiont.rotationYawHead = symbiont.rotationYaw;
        symbiont.rotationPitch = (float) (-(MathHelper.atan2(dy, horizontal) * 180.0D / Math.PI));
    }

    private boolean isPlayerInsideBowelsInstance() {
        // 只读取已加载的肠道世界，绝不能在这里创建维度，否则每次召唤检查都会
        // 触发“Loading dimension 223 / Unloading dimension 223”的反复加载。
        WorldServer bowels = DimensionManager.getWorld(BowelsDimensions.DIMENSION_ID);
        if (bowels == null) return false;
        BowelsInstanceData.Instance instance = BowelsInstanceData.get(bowels).get(storm.getUniqueID());
        if (instance == null || instance.completed) return false;
        AxisAlignedBB area = new AxisAlignedBB(instance.center).grow(50.0D);
        for (EntityPlayer player : bowels.getEntitiesWithinAABB(EntityPlayer.class, area)) {
            if (player.isEntityAlive()) return true;
        }
        return false;
    }

    private static List<ItemStack> getAllInventoryStacks(EntityPlayer player) {
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        stacks.addAll(player.inventory.mainInventory);
        stacks.addAll(player.inventory.armorInventory);
        stacks.addAll(player.inventory.offHandInventory);
        return stacks;
    }

    private static boolean isCommandBlockTool(ItemStack stack) {
        ResourceLocation name = stack.getItem().getRegistryName();
        if (name == null || !Tags.MOD_ID.equals(name.getNamespace())) return false;
        String path = name.getPath();
        return path.contains("command_block_") && (path.endsWith("_sword") || path.endsWith("_pickaxe")
                || path.endsWith("_axe") || path.endsWith("_shovel") || path.endsWith("_hoe"));
    }

    public int getSummoningDelay() {
        return timeTillCanSummonSymbiont;
    }

    public void setSummoningDelay(int delay) {
        timeTillCanSummonSymbiont = Math.max(0, delay);
    }

    public void writeToNBT(NBTTagCompound compound) {
        compound.setInteger("SymbiontSummoningCooldown", timeTillCanSummonSymbiont);
    }

    public void readFromNBT(NBTTagCompound compound) {
        setSummoningDelay(compound.getInteger("SymbiontSummoningCooldown"));
    }

    public static void markSummoned(EntityPlayer player, WitherStormEntity storm) {
        NBTTagCompound data = getPlayerData(player);
        NBTTagCompound snapshot = getSnapshot(data, storm.getUniqueID(), true);
        snapshot.setInteger(SUMMONER_PHASE, storm.getPhase());
        snapshot.setLong(SUMMONING_COOLDOWN_UNTIL, player.world.getTotalWorldTime()
                + MathHelper.clamp(WitherStormConfig.playerSummoningDelay, 1, 60) * 1200L
                + player.getRNG().nextInt(2400));
        savePlayerData(player, data);
    }

    public static boolean hasRecentSummon(EntityPlayer player, WitherStormEntity storm) {
        NBTTagCompound snapshot = getSnapshot(getPlayerData(player), storm.getUniqueID(), false);
        return snapshot != null && snapshot.getInteger(SUMMONER_PHASE) == storm.getPhase()
                && snapshot.getLong(SUMMONING_COOLDOWN_UNTIL) > player.world.getTotalWorldTime();
    }

    public static void markKilledSymbiont(EntityPlayer player, @Nullable WitherStormEntity storm) {
        if (storm == null) return;
        NBTTagCompound data = getPlayerData(player);
        NBTTagCompound snapshot = getSnapshot(data, storm.getUniqueID(), true);
        if (!snapshot.hasKey(SUMMONER_PHASE, 3)) snapshot.setInteger(SUMMONER_PHASE, storm.getPhase());
        snapshot.setLong(SUMMONING_COOLDOWN_UNTIL, player.world.getTotalWorldTime()
                + MathHelper.clamp(WitherStormConfig.playerSummoningDelayOnKill, 1, 60) * 1200L
                + player.getRNG().nextInt(24000));
        savePlayerData(player, data);
    }

    public static void makeInvulnerable(EntityPlayer player) {
        makeInvulnerable(player,
                MathHelper.clamp(WitherStormConfig.playerInvulnerableTime, 1, 10) * 1200
                        + player.getRNG().nextInt(1200));
    }

    public static void makeInvulnerable(EntityPlayer player, int ticks) {
        NBTTagCompound data = getPlayerData(player);
        data.setLong(INVULNERABLE_UNTIL,
                player.world.getTotalWorldTime() + Math.max(0, ticks));
        savePlayerData(player, data);
    }

    public static boolean shouldIgnorePlayer(EntityPlayer player) {
        return getPlayerData(player).getLong(INVULNERABLE_UNTIL) > player.world.getTotalWorldTime();
    }

    private static NBTTagCompound getPlayerData(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        NBTTagCompound persistentData = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persistentData.hasKey(PLAYER_DATA_KEY, 10) && entityData.hasKey(PLAYER_DATA_KEY, 10)) {
            persistentData.setTag(PLAYER_DATA_KEY, entityData.getCompoundTag(PLAYER_DATA_KEY).copy());
        }
        if (!persistentData.hasKey(PLAYER_DATA_KEY, 10)) {
            persistentData.setTag(PLAYER_DATA_KEY, new NBTTagCompound());
        }
        return persistentData.getCompoundTag(PLAYER_DATA_KEY);
    }

    private static void savePlayerData(EntityPlayer player, NBTTagCompound data) {
        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound persistentData = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        persistentData.setTag(PLAYER_DATA_KEY, data);
        entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persistentData);
    }

    @Nullable
    private static NBTTagCompound getSnapshot(NBTTagCompound data, UUID stormId, boolean create) {
        NBTTagList snapshots = data.getTagList(SUMMONING_DATA, 10);
        for (int index = 0; index < snapshots.tagCount(); index++) {
            NBTTagCompound snapshot = snapshots.getCompoundTagAt(index);
            if (snapshot.hasUniqueId(SUMMONER) && stormId.equals(snapshot.getUniqueId(SUMMONER))) {
                return snapshot;
            }
        }
        if (!create) return null;
        NBTTagCompound snapshot = new NBTTagCompound();
        snapshot.setUniqueId(SUMMONER, stormId);
        snapshots.appendTag(snapshot);
        data.setTag(SUMMONING_DATA, snapshots);
        return snapshot;
    }
}
