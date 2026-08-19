package com.wdcftgg.witherstormmod.common.entity;

import com.google.common.base.Optional;
import com.wdcftgg.witherstormmod.api.common.entity.WitherStormBase;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.init.ModDamageSources;
import com.wdcftgg.witherstormmod.common.init.ModAttributes;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityBodyHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.EnumFacing;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.potion.PotionEffect;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.world.EnumDifficulty;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.world.BowelsBossfightController;
import com.wdcftgg.witherstormmod.common.world.ChunkLoadingManager;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.resource.UpstreamItemTags;
import com.wdcftgg.witherstormmod.common.util.TractorBeamHelper;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import com.wdcftgg.witherstormmod.Tags;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import java.util.Objects;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

public final class SupplementalEntities {

    private SupplementalEntities() {
    }

    public static class FlamingWitherSkullEntity extends EntityWitherSkull
            implements IEntityAdditionalSpawnData {
        public FlamingWitherSkullEntity(World world) { super(world); setSize(0.8F, 0.8F); }
        public FlamingWitherSkullEntity(World world, EntityLivingBase shooter, double accelerationX, double accelerationY, double accelerationZ) {
            super(world, shooter, accelerationX, accelerationY, accelerationZ);
            setSize(0.8F, 0.8F);
        }

        /**
         * EntityWitherSkull's vanilla spawn packet does not carry the
         * acceleration vector. The upstream projectile is an
         * AbstractHurtingProjectile and explicitly synchronizes it; without
         * the same data the 1.12 client waits for periodic position corrections
         * and the skull visibly advances in steps.
         */
        @Override
        public void writeSpawnData(ByteBuf buffer) {
            buffer.writeDouble(accelerationX);
            buffer.writeDouble(accelerationY);
            buffer.writeDouble(accelerationZ);
        }

        @Override
        public void readSpawnData(ByteBuf buffer) {
            // Builds predating projectile interpolation did not append this
            // vector to Forge's entity-spawn packet. Keep their packets
            // readable instead of disconnecting a multiplayer client.
            if (buffer.readableBytes() < Double.BYTES * 3) return;
            accelerationX = buffer.readDouble();
            accelerationY = buffer.readDouble();
            accelerationZ = buffer.readDouble();
        }

        @Override
        protected EnumParticleTypes getParticleType() {
            return WitherStormMod.isAprilFools() && WitherStormClientConfig.aprilFools
                    ? EnumParticleTypes.HEART : super.getParticleType();
        }

        @Override
        protected void onImpact(RayTraceResult result) {
            if (world.isRemote) return;
            if (result.entityHit == null) {
                explodeAndDiscard();
                return;
            }
            if (result.entityHit instanceof EntityLivingBase
                    && ((EntityLivingBase) result.entityHit).isActiveItemStackBlocking()
                    && ((EntityLivingBase) result.entityHit).getActiveItemStack().getItem() == Items.SHIELD) {
                explodeAndDiscard();
                return;
            }

            boolean damaged;
            if (shootingEntity != null) {
                damaged = result.entityHit.attackEntityFrom(
                        ModDamageSources.flamingWitherSkull(this, shootingEntity), 10.0F);
                if (damaged) {
                    if (result.entityHit.isEntityAlive()) applyEnchantments(shootingEntity, result.entityHit);
                    else shootingEntity.heal(10.0F);
                }
            } else {
                damaged = result.entityHit.attackEntityFrom(DamageSource.MAGIC, 8.0F);
            }
            if (damaged && result.entityHit instanceof EntityLivingBase
                    && (world.getDifficulty() == EnumDifficulty.NORMAL
                    || world.getDifficulty() == EnumDifficulty.HARD)) {
                ((EntityLivingBase) result.entityHit).addPotionEffect(new PotionEffect(MobEffects.WITHER, 180, 1));
            }
        }

        protected void explodeAndDiscard() {
            boolean mobGriefing = ForgeEventFactory.getMobGriefingEvent(
                    world, shootingEntity == null ? this : shootingEntity);
            playSound(ModSounds.get("flaming_skull_impact"), 6.0F,
                    (rand.nextFloat() - rand.nextFloat()) * -0.2F + getImpactPitch());
            ModNetwork.shakeNear(world, posX, posY, posZ, getShakeRange(), 20.0F, getShakePower());

            Entity explosionSource = shootingEntity == null ? null : this;
            world.newExplosion(explosionSource, posX, posY, posZ,
                    getExplosionSize(), mobGriefing, mobGriefing);

            setDead();
        }

        protected float getExplosionSize() {
            return (float) WitherStormConfig.flamingSkullExplosionSize + rand.nextInt(2);
        }

        protected float getImpactPitch() { return 1.0F; }
        protected double getShakeRange() { return 45.0D; }
        protected float getShakePower() { return 4.0F; }

        @Override
        public boolean canBeCollidedWith() { return true; }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (isEntityInvulnerable(source) || !(source.getTrueSource() instanceof EntityLivingBase)) return false;
            EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
            ItemStack weapon = attacker.getHeldItemMainhand();
            if (weapon.isEmpty() || !(weapon.getItem() instanceof ItemSword)) return false;
            Vec3d look = attacker.getLookVec();
            if (look == null) return false;
            markVelocityChanged();
            motionX = look.x;
            motionY = look.y;
            motionZ = look.z;
            accelerationX = motionX * 0.1D;
            accelerationY = motionY * 0.1D;
            accelerationZ = motionZ * 0.1D;
            shootingEntity = attacker;
            ModNetwork.syncDamagingProjectile(this);
            weapon.damageItem(120 + rand.nextInt(140), attacker);
            return true;
        }
    }

    public static class BlueFlamingWitherSkullEntity extends FlamingWitherSkullEntity {
        public BlueFlamingWitherSkullEntity(World world) { super(world); setInvulnerable(true); }
        public BlueFlamingWitherSkullEntity(World world, EntityLivingBase shooter, double accelerationX, double accelerationY, double accelerationZ) {
            super(world, shooter, accelerationX, accelerationY, accelerationZ);
            setInvulnerable(true);
        }

        @Override protected float getExplosionSize() {
            return (float) WitherStormConfig.flamingSkullExplosionSize + 4.0F;
        }
        @Override protected float getImpactPitch() { return 0.8F; }
        @Override protected double getShakeRange() { return 60.0D; }
        @Override protected float getShakePower() { return 6.0F; }
    }

    public static class TentacleSpikeEntity extends Entity {
        private UUID ownerUuid;
        private EntityLivingBase owner;
        private boolean sentSpikeEvent;
        private int warmupDelayTicks;
        private int lifeTicks = 22;
        private boolean clientAttackStarted;
        private float damageModifier;

        public TentacleSpikeEntity(World world) {
            super(world);
            setSize(0.5F, 1.4F);
            noClip = true;
        }

        public TentacleSpikeEntity(World world, double x, double y, double z, float yawRadians, int warmup,
                             EntityLivingBase owner, float damageModifier) {
            this(world);
            warmupDelayTicks = warmup;
            setOwner(owner);
            rotationYaw = yawRadians * 57.295776F;
            setPosition(x, y, z);
            this.damageModifier = damageModifier;
        }

        @Override
        protected void entityInit() {
        }

        public void setOwner(EntityLivingBase owner) {
            this.owner = owner;
            ownerUuid = owner == null ? null : owner.getUniqueID();
        }

        public EntityLivingBase getOwner() {
            if (owner == null && ownerUuid != null && world instanceof WorldServer) {
                Entity entity = ((WorldServer) world).getEntityFromUuid(ownerUuid);
                if (entity instanceof EntityLivingBase) owner = (EntityLivingBase) entity;
            }
            return owner;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            motionX = motionY = motionZ = 0.0D;
            if (world.isRemote) {
                if (clientAttackStarted) lifeTicks--;
                return;
            }
            if (--warmupDelayTicks < 0) {
                if (warmupDelayTicks == -2) {
                    for (EntityLivingBase target : world.getEntitiesWithinAABB(EntityLivingBase.class,
                            getEntityBoundingBox().grow(0.6D, 0.0D, 0.6D))) {
                        dealDamageTo(target);
                    }
                }
                if (!sentSpikeEvent) {
                    world.setEntityState(this, (byte) 4);
                    sentSpikeEvent = true;
                }
                if (--lifeTicks < 0) setDead();
            }
        }

        private void dealDamageTo(EntityLivingBase target) {
            EntityLivingBase spikeOwner = getOwner();
            if (!target.isEntityAlive() || target == spikeOwner || target.isEntityInvulnerable(DamageSource.GENERIC)) return;
            if (spikeOwner != null && spikeOwner.isOnSameTeam(target)) return;
            DamageSource source = spikeOwner == null ? DamageSource.GENERIC : DamageSource.causeIndirectDamage(this, spikeOwner);
            target.attackEntityFrom(source, 6.0F + damageModifier);
        }

        @Override
        public void handleStatusUpdate(byte id) {
            super.handleStatusUpdate(id);
            if (id == 4) {
                clientAttackStarted = true;
                if (!isSilent()) {
                    world.playSound(posX, posY, posZ, ModSounds.get("tentacle_spike_stab"), SoundCategory.HOSTILE,
                            1.0F, rand.nextFloat() * 0.2F + 0.85F, false);
                }
            }
        }

        public float getAnimationProgress(float partialTicks) {
            if (!clientAttackStarted) return 0.0F;
            int remaining = lifeTicks - 2;
            return remaining <= 0 ? 1.0F : 1.0F - (remaining - partialTicks) / 20.0F;
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound compound) {
            warmupDelayTicks = compound.getInteger("WarmupDelay");
            if (compound.hasUniqueId("Owner")) ownerUuid = compound.getUniqueId("Owner");
            damageModifier = compound.getFloat("DamageModifier");
            lifeTicks = compound.hasKey("LifeTicks") ? compound.getInteger("LifeTicks") : 22;
            sentSpikeEvent = compound.getBoolean("SentSpikeEvent");
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound compound) {
            compound.setInteger("WarmupDelay", warmupDelayTicks);
            if (ownerUuid != null) compound.setUniqueId("Owner", ownerUuid);
            compound.setFloat("DamageModifier", damageModifier);
            compound.setInteger("LifeTicks", lifeTicks);
            compound.setBoolean("SentSpikeEvent", sentSpikeEvent);
        }

        @Override
        public boolean canBeCollidedWith() {
            return false;
        }

        @Override
        public boolean canBePushed() {
            return false;
        }
    }

    public static class BlockClusterEntity extends Entity {
        private static final int CLIENT_RENDER_INTERPOLATION_STEPS = 3;
        private static final double CLIENT_RENDER_SNAP_DISTANCE_SQUARED = 64.0D * 64.0D;
        private static final DataParameter<NBTTagCompound> CLUSTER_DATA =
                EntityDataManager.createKey(BlockClusterEntity.class, DataSerializers.COMPOUND_TAG);
        private static final DataParameter<Float> PITCH_VELOCITY =
                EntityDataManager.createKey(BlockClusterEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> YAW_VELOCITY =
                EntityDataManager.createKey(BlockClusterEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Boolean> PHYSICS =
                EntityDataManager.createKey(BlockClusterEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> FORCE_RENDER =
                EntityDataManager.createKey(BlockClusterEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Integer> SHAKE_TIME =
                EntityDataManager.createKey(BlockClusterEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Optional<BlockPos>> FADE_POSITION =
                EntityDataManager.createKey(BlockClusterEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
        private static final DataParameter<Float> FADE_STRENGTH =
                EntityDataManager.createKey(BlockClusterEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Integer> FADE_DISTANCE_OFFSET =
                EntityDataManager.createKey(BlockClusterEntity.class, DataSerializers.VARINT);
        private final Map<BlockPos, IBlockState> blocks = new LinkedHashMap<BlockPos, IBlockState>();
        private int nonAirBlockCount;
        private int blockDataRevision;
        private float clusterPitch;
        private float previousClusterPitch;
        private float clusterYaw;
        private float previousClusterYaw;
        private float clusterSizeX = 1.0F;
        private float clusterSizeY = 1.0F;
        private float clusterSizeZ = 1.0F;
        private float shakeX;
        private float previousShakeX;
        private float shakeZ;
        private float previousShakeZ;
        private int shakeTime;
        private int sink;
        private boolean antiStacking;
        private boolean shouldCrumble;
        private boolean dropItems;
        private boolean createdFromBeam;
        private boolean createdFromFallingBlock;
        private boolean shouldNotCountToConsumedMass;
        private int headCreatedFrom = -1;
        private int time;
        private boolean resetGravityOnLoad = true;
        private float fadeAmount = 1.0F;
        private float previousFadeAmount = 1.0F;
        private double tractorBeamDistanceThreshold;
        private final List<NBTTagCompound> tileData = new ArrayList<NBTTagCompound>();
        private BlockPos startPos = BlockPos.ORIGIN;
        private boolean clientRenderPositionInitialized;
        private double clientRenderX;
        private double clientRenderY;
        private double clientRenderZ;
        private double previousClientRenderX;
        private double previousClientRenderY;
        private double previousClientRenderZ;
        private double clientRenderTargetX;
        private double clientRenderTargetY;
        private double clientRenderTargetZ;
        private int clientRenderPositionSteps;

        public BlockClusterEntity(World world) {
            super(world);
            dropItems = WitherStormConfig.blockClustersDropItems;
            setClusterSize(1.0F, 1.0F, 1.0F);
        }

        public BlockClusterEntity(World world, double positionX, double positionY, double positionZ, IBlockState state) {
            this(world);
            setPosition(positionX, positionY, positionZ);
            addBlock(BlockPos.ORIGIN, state);
        }

        public BlockClusterEntity(World world, double positionX, double positionY, double positionZ,
                            Map<BlockPos, IBlockState> states) {
            this(world);
            setPosition(positionX, positionY, positionZ);
            setBlocks(states);
        }

        @Override
        protected void entityInit() {
            dataManager.register(CLUSTER_DATA, new NBTTagCompound());
            dataManager.register(PITCH_VELOCITY, 0.0F);
            dataManager.register(YAW_VELOCITY, 0.0F);
            dataManager.register(PHYSICS, true);
            dataManager.register(FORCE_RENDER, false);
            dataManager.register(SHAKE_TIME, 0);
            dataManager.register(FADE_POSITION, Optional.<BlockPos>absent());
            dataManager.register(FADE_STRENGTH, 10.0F);
            dataManager.register(FADE_DISTANCE_OFFSET, 0);
        }

        public Map<BlockPos, IBlockState> getBlocks() {
            return blocks;
        }

        /** 上游的 map 填充入口：空 map 保持已有簇不变。 */
        public void populate(Map<BlockPos, IBlockState> states) {
            if (states != null && !states.isEmpty()) setBlocks(states);
        }

        /** 上游公共 API：返回簇内当前记录的方块数量。 */
        public int getSize() {
            return blocks.size();
        }

        /** 上游公共 API：检查簇内是否包含指定方块类型。 */
        public boolean containsBlock(Block block) {
            if (block == null) return false;
            for (IBlockState state : blocks.values()) {
                if (state != null && state.getBlock() == block) return true;
            }
            return false;
        }

        /** 1.12 等价的同步写入入口，供第三方方块簇构建器使用。 */
        public void setTileData(List<NBTTagCompound> values) {
            tileData.clear();
            if (values != null) {
                for (NBTTagCompound value : values) {
                    if (value != null) tileData.add(value.copy());
                }
            }
            syncClusterData();
        }

        public int getBlockDataRevision() {
            return blockDataRevision;
        }

        public void setBlocks(Map<BlockPos, IBlockState> states) {
            replaceBlockContents(states);
            startPos = calculateStartPos(blocks.keySet());
            updateClusterSize();
            syncClusterData();
        }

        public void setStartPos(BlockPos position) {
            startPos = position == null ? BlockPos.ORIGIN : position.toImmutable();
            syncClusterData();
        }

        public void addBlock(BlockPos offset, IBlockState state) {
            putBlock(offset, state);
            updateClusterSize();
            syncClusterData();
        }

        /** 与上游一致的 state-first 重载。 */
        public void addBlock(IBlockState state, BlockPos offset) {
            addBlock(offset, state);
        }

        public void addBlocks(Map<BlockPos, IBlockState> states) {
            for (Map.Entry<BlockPos, IBlockState> entry : states.entrySet()) {
                putBlock(entry.getKey(), entry.getValue());
            }
            updateClusterSize();
            syncClusterData();
        }

        private void putBlock(BlockPos offset, IBlockState state) {
            IBlockState previous = blocks.put(offset, state);
            if (isNonAir(previous)) nonAirBlockCount--;
            if (isNonAir(state)) nonAirBlockCount++;
            if (!Objects.equals(previous, state)) blockDataRevision++;
        }

        private void replaceBlockContents(Map<BlockPos, IBlockState> states) {
            Map<BlockPos, IBlockState> replacement = states == blocks
                    ? new LinkedHashMap<BlockPos, IBlockState>(states) : states;
            clearBlockContents();
            blocks.putAll(replacement);
            for (IBlockState state : blocks.values()) {
                if (isNonAir(state)) nonAirBlockCount++;
            }
            if (!blocks.isEmpty()) blockDataRevision++;
        }

        private void clearBlockContents() {
            if (!blocks.isEmpty()) blockDataRevision++;
            blocks.clear();
            nonAirBlockCount = 0;
        }

        private static boolean isNonAir(@Nullable IBlockState state) {
            return state != null && state.getBlock() != Blocks.AIR;
        }

        /** 将一段方块区域转换为可移动的实体簇，并从世界中取走原方块。 */
        public void populate(BlockPos minimum, BlockPos maximum) {
            clearBlockContents();
            tileData.clear();
            int deltaX = maximum.getX() - minimum.getX();
            int deltaY = maximum.getY() - minimum.getY();
            int deltaZ = maximum.getZ() - minimum.getZ();
            // 上游实体位于区域的水平中心、较低的一侧；方块绝对坐标由中心 startPos 加相对偏移还原。
            setPosition(minimum.getX() + deltaX / 2.0D + 0.5D,
                    minimum.getY() + Math.min(deltaY, 0),
                    minimum.getZ() + deltaZ / 2.0D + 0.5D);
            setClusterSize(Math.abs(deltaX) + 1.0F, Math.abs(deltaY) + 1.0F,
                    Math.abs(deltaZ) + 1.0F);
            startPos = minimum.add((int) (deltaX / 2.0D), (int) (deltaY / 2.0D),
                    (int) (deltaZ / 2.0D));
            for (int x = minimum.getX(); x <= maximum.getX(); x++) {
                for (int y = minimum.getY(); y <= maximum.getY(); y++) {
                    for (int z = minimum.getZ(); z <= maximum.getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        IBlockState state = world.getBlockState(pos);
                        if (state.getBlock() == Blocks.AIR || state.getBlock().isReplaceable(world, pos)) continue;
                        TileEntity tile = world.getTileEntity(pos);
                        if (tile != null) addTileData(tile.writeToNBT(new NBTTagCompound()));
                        putBlock(pos.subtract(startPos), state);
                        world.setBlockToAir(pos);
                    }
                }
            }
            syncClusterData();
        }

        /** Converts the same strict-radius sphere used by the upstream entity. */
        public void populateWithRadius(BlockPos center, int radius, BlockStateSelector selector) {
            populateWithRadius(center, (float) radius, selector);
        }

        /** 保留上游高斯生成出的亚方块半径，而不是在生成前将其截断为整数。 */
        public void populateWithRadius(BlockPos center, float radius, BlockStateSelector selector) {
            clearBlockContents();
            tileData.clear();
            startPos = center.toImmutable();
            float clampedRadius = Math.max(1.0F, radius);
            float radiusSquared = clampedRadius * clampedRadius;
            int scanRadius = MathHelper.ceil(clampedRadius);
            for (int x = -scanRadius; x <= scanRadius; x++) {
                for (int y = -scanRadius; y <= scanRadius; y++) {
                    for (int z = -scanRadius; z <= scanRadius; z++) {
                        if (x * x + y * y + z * z >= radiusSquared) continue;
                        BlockPos worldPos = center.add(x, y, z);
                        IBlockState state = world.getBlockState(worldPos);
                        if (world.isAirBlock(worldPos) || !selector.test(world, worldPos, state)) continue;
                        TileEntity tile = world.getTileEntity(worldPos);
                        if (tile != null) addTileData(tile.writeToNBT(new NBTTagCompound()));
                        putBlock(new BlockPos(x, y, z), state);
                        world.setBlockToAir(worldPos);
                    }
                }
            }
            float intendedSize = Math.max(1.0F, scanRadius * 2.0F - 1.0F);
            setClusterSize(intendedSize, intendedSize, intendedSize);
            setPosition(center.getX() + 0.5D,
                    center.getY() - clusterSizeY / 2.0D + 0.5D,
                    center.getZ() + 0.5D);
            syncClusterData();
        }

        public void setPhysics(boolean physics) {
            dataManager.set(PHYSICS, physics);
            noClip = !physics;
        }

        public boolean physicsEnabled() {
            return dataManager.get(PHYSICS);
        }

        public float getClusterPitch(float partialTicks) {
            return previousClusterPitch + (clusterPitch - previousClusterPitch) * partialTicks;
        }

        public float getClusterYaw(float partialTicks) {
            return previousClusterYaw + (clusterYaw - previousClusterYaw) * partialTicks;
        }

        public double getClientRenderX(float partialTicks) {
            return clientRenderPositionInitialized
                    ? previousClientRenderX + (clientRenderX - previousClientRenderX) * partialTicks
                    : lastTickPosX + (posX - lastTickPosX) * partialTicks;
        }

        public double getClientRenderY(float partialTicks) {
            return clientRenderPositionInitialized
                    ? previousClientRenderY + (clientRenderY - previousClientRenderY) * partialTicks
                    : lastTickPosY + (posY - lastTickPosY) * partialTicks;
        }

        public double getClientRenderZ(float partialTicks) {
            return clientRenderPositionInitialized
                    ? previousClientRenderZ + (clientRenderZ - previousClientRenderZ) * partialTicks
                    : lastTickPosZ + (posZ - lastTickPosZ) * partialTicks;
        }

        @Override
        public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch,
                                                 int positionIncrements, boolean teleport) {
            if (world.isRemote) {
                initializeClientRenderPosition();
                clientRenderTargetX = x;
                clientRenderTargetY = y;
                clientRenderTargetZ = z;
                double deltaX = x - clientRenderX;
                double deltaY = y - clientRenderY;
                double deltaZ = z - clientRenderZ;
                if (teleport || deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
                        > CLIENT_RENDER_SNAP_DISTANCE_SQUARED) {
                    setClientRenderPosition(x, y, z);
                    clientRenderPositionSteps = 0;
                } else {
                    clientRenderPositionSteps = CLIENT_RENDER_INTERPOLATION_STEPS;
                }
            }
            // Keep the logical entity at the server-authoritative position;
            // only its renderer follows the smoothed coordinates above.
            super.setPositionAndRotationDirect(x, y, z, yaw, pitch, positionIncrements, teleport);
        }

        @Override
        public void onUpdate() {
            if (world.isRemote) beginClientRenderTick();
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            previousShakeX = shakeX;
            previousShakeZ = shakeZ;
            if (shakeTime > 0) {
                float time = shakeTime;
                shakeX = MathHelper.sin(time * 4.5F) * 0.05F + (rand.nextFloat() - 0.5F) * 0.05F;
                shakeZ = MathHelper.cos(time * 3.5F) * 0.15F + (rand.nextFloat() - 0.5F) * 0.2F;
                if (--shakeTime == 0) setShakeTime(0);
            } else {
                shakeX = 0.0F;
                shakeZ = 0.0F;
            }

            previousClusterPitch = clusterPitch;
            previousClusterYaw = clusterYaw;
            if (shakeTime <= 0) {
                clusterPitch += dataManager.get(PITCH_VELOCITY);
                clusterYaw += dataManager.get(YAW_VELOCITY);
            }
            if (!hasNoGravity()) motionY -= 0.04D;
            // Pulled clusters receive an authoritative position every tracker
            // update. Predicting that same motion on the client makes each
            // correction look like the cluster rebounds. Their dedicated
            // render position interpolates those samples instead.
            if (!world.isRemote || physicsEnabled()) {
                move(MoverType.SELF, motionX, motionY, motionZ);
            }
            noClip = !physicsEnabled();
            time++;
            super.onUpdate();
            if (world.isRemote) {
                updateClientRenderPosition();
                updateFadeAmount();
                return;
            }
            if (WitherStormConfig.clustersRemoveItems && blocks.size() != 1 && !createdFromBeam) {
                for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, getEntityBoundingBox())) {
                    if (!item.isDead && item.getOwner() == null) item.setDead();
                }
            }
            if (blocks.isEmpty() || containsOnlyAir()) {
                setDead();
            } else if (onGround) {
                place();
            } else if (posY + clusterSizeY <= 0.0D || time > 600) {
                discardOrDrop();
            }
        }

        private void initializeClientRenderPosition() {
            if (clientRenderPositionInitialized) return;
            clientRenderPositionInitialized = true;
            clientRenderX = previousClientRenderX = clientRenderTargetX = posX;
            clientRenderY = previousClientRenderY = clientRenderTargetY = posY;
            clientRenderZ = previousClientRenderZ = clientRenderTargetZ = posZ;
        }

        private void beginClientRenderTick() {
            initializeClientRenderPosition();
            previousClientRenderX = clientRenderX;
            previousClientRenderY = clientRenderY;
            previousClientRenderZ = clientRenderZ;
        }

        private void updateClientRenderPosition() {
            if (clientRenderPositionSteps > 0) {
                clientRenderX += (clientRenderTargetX - clientRenderX) / clientRenderPositionSteps;
                clientRenderY += (clientRenderTargetY - clientRenderY) / clientRenderPositionSteps;
                clientRenderZ += (clientRenderTargetZ - clientRenderZ) / clientRenderPositionSteps;
                --clientRenderPositionSteps;
            } else {
                clientRenderX = posX;
                clientRenderY = posY;
                clientRenderZ = posZ;
            }
        }

        private void setClientRenderPosition(double x, double y, double z) {
            clientRenderX = previousClientRenderX = x;
            clientRenderY = previousClientRenderY = y;
            clientRenderZ = previousClientRenderZ = z;
        }

        /** 上游方块簇跳过火焰、流体和脚步等普通实体基础逻辑，但仍允许传送门推进。 */
        @Override
        public void onEntityUpdate() {
            world.profiler.startSection("entityBaseTick");
            prevRotationPitch = rotationPitch;
            prevRotationYaw = rotationYaw;
            if (!world.isRemote && world instanceof WorldServer) {
                world.profiler.startSection("portal");
                if (inPortal) {
                    MinecraftServer server = world.getMinecraftServer();
                    if (server != null && server.getAllowNether()) {
                        if (!isRiding()) {
                            int maximumPortalTime = getMaxInPortalTime();
                            if (portalCounter++ >= maximumPortalTime) {
                                portalCounter = maximumPortalTime;
                                timeUntilPortal = getPortalCooldown();
                                int targetDimension = world.provider.getDimensionType().getId() == -1 ? 0 : -1;
                                changeDimension(targetDimension);
                            }
                        }
                        inPortal = false;
                    }
                } else {
                    portalCounter = Math.max(0, portalCounter - 4);
                }
                decrementTimeUntilPortal();
                world.profiler.endSection();
            }
            firstUpdate = false;
            world.profiler.endSection();
        }

        private boolean containsOnlyAir() {
            return nonAirBlockCount == 0;
        }

        private void discardOrDrop() {
            if (dropItems && world.getGameRules().getBoolean("doEntityDrops")) {
                for (Map.Entry<BlockPos, IBlockState> entry : blocks.entrySet()) {
                    spawnBlockDrop(entry.getValue(), new BlockPos(this).add(entry.getKey()));
                }
            }
            setDead();
        }

        private void spawnBlockDrop(IBlockState state, BlockPos position) {
            Block block = state.getBlock();
            Item item = Item.getItemFromBlock(block);
            if (item == Items.AIR) return;
            EntityItem dropped = new EntityItem(world, position.getX(), position.getY(), position.getZ(),
                    new ItemStack(item, 1, block.damageDropped(state)));
            dropped.setDefaultPickupDelay();
            world.spawnEntity(dropped);
        }

        private void updateFadeAmount() {
            if (shakeTime > 0) return;
            previousFadeAmount = fadeAmount;
            BlockPos fadePosition = getFadePos();
            if (fadePosition == null) return;
            double maximumDistance = Math.sqrt(startPos.distanceSq(fadePosition)) - getFadeDistanceOffset();
            Vec3d fadeCenter = new Vec3d(fadePosition).add(0.5D, 0.5D, 0.5D);
            double distance = Math.max(0.0D,
                    fadeCenter.distanceTo(getPositionVector()) - getFadeDistanceOffset());
            fadeAmount = Math.min(1.0F,
                    (float) distance / Math.min((float) maximumDistance, getFadeStrength()));
        }

        /** 在阶段结束时将簇中的方块完整放回世界。 */
        public void place() {
            if (world.isRemote) return;
            BlockPos base = new BlockPos(this);
            if (antiStacking) {
                BlockPos scan = base;
                IBlockState scanState = world.getBlockState(scan);
                for (int i = 0; i < 50 && scanState.getBlock() == Blocks.AIR; i++) {
                    scan = scan.down();
                    scanState = world.getBlockState(scan);
                }
                base = new BlockPos(base.getX(), scan.getY(), base.getZ());
            }
            int verticalCenter = MathHelper.floor(
                    (getEntityBoundingBox().maxY - getEntityBoundingBox().minY) / 2.0D - 0.5D);
            for (Map.Entry<BlockPos, IBlockState> entry : blocks.entrySet()) {
                BlockPos offset = entry.getKey();
                BlockPos target = base.add(offset.getX(), offset.getY() - sink, offset.getZ()).up(verticalCenter);
                IBlockState existing = world.getBlockState(target);
                boolean protectedBlock = UpstreamBlockTags.contains("minecraft:wither_immune", existing);
                if (world.getTileEntity(target) == null && !protectedBlock
                        && !UpstreamBlockTags.contains(UpstreamBlockTags.BLOCK_CLUSTERS_CANNOT_PLACE,
                        entry.getValue())
                        && world.setBlockState(target, entry.getValue(), 3)) {
                    NBTTagCompound tile = getTileDataFromOffset(offset);
                    TileEntity placedTile = world.getTileEntity(target);
                    if (tile != null && placedTile != null) {
                        tile.setInteger("x", target.getX());
                        tile.setInteger("y", target.getY());
                        tile.setInteger("z", target.getZ());
                        placedTile.readFromNBT(tile);
                        placedTile.markDirty();
                    }
                    world.notifyNeighborsOfStateChange(target, entry.getValue().getBlock(), false);
                } else if (dropItems && world.getGameRules().getBoolean("doEntityDrops")) {
                    spawnBlockDrop(entry.getValue(), target);
                }
            }
            clearBlockContents();
            setDead();
        }

        public void setShakeTime(int value) {
            shakeTime = value;
            dataManager.set(SHAKE_TIME, value);
        }
        public int getShakeTime() { return shakeTime; }
        public void setSink(int value) { sink = value; }
        public int getSink() { return sink; }
        public void setAntiStacking(boolean value) { antiStacking = value; }
        public boolean isAntiStacking() { return antiStacking; }
        public boolean antiStacking() { return antiStacking; }
        public void setShouldCrumble(boolean value) { shouldCrumble = value; }
        public boolean shouldCrumble() { return shouldCrumble; }
        public void setDropItems(boolean value) { dropItems = value; }
        public void setForceRender(boolean value) { dataManager.set(FORCE_RENDER, value); }
        public boolean forceRender() { return dataManager.get(FORCE_RENDER); }
        public void setFadePos(BlockPos value) {
            dataManager.set(FADE_POSITION, Optional.fromNullable(value));
        }
        @Nullable
        public BlockPos getFadePos() {
            Optional<BlockPos> value = dataManager.get(FADE_POSITION);
            return value.isPresent() ? value.get() : null;
        }
        public void setFadeStrength(float value) { dataManager.set(FADE_STRENGTH, value); }
        public float getFadeStrength() { return dataManager.get(FADE_STRENGTH); }
        public void setFadeDistanceOffset(int value) { dataManager.set(FADE_DISTANCE_OFFSET, value); }
        public int getFadeDistanceOffset() { return dataManager.get(FADE_DISTANCE_OFFSET); }
        public void setRotateClockwise(boolean value) {
            if (value) addTag("RotateClockwise");
            else removeTag("RotateClockwise");
        }
        public boolean isRotateClockwise() { return getTags().contains("RotateClockwise"); }
        public void setCreatedFromTractorBeam(boolean value) { createdFromBeam = value; }
        public boolean createdFromTractorBeam() { return createdFromBeam; }
        public void setCreatedFromFallingBlock(boolean value) { createdFromFallingBlock = value; }
        public boolean createdFromFallingBlock() { return createdFromFallingBlock; }
        public void setShouldNotCountToConsumedMass(boolean value) { shouldNotCountToConsumedMass = value; }
        public boolean shouldNotCountToConsumedMass() { return shouldNotCountToConsumedMass; }
        public void setShouldntCountToConsumedEntities(boolean value) {
            shouldNotCountToConsumedMass = value;
        }
        public boolean shouldntCountToConsumedEntities() {
            return shouldNotCountToConsumedMass;
        }
        public void setHeadCreatedFrom(int value) { headCreatedFrom = value; }
        public int getHeadCreatedFrom() { return headCreatedFrom; }
        public void setTime(int value) { time = value; }
        public int getTime() { return time; }
        public void setResetGravityOnLoad(boolean value) { resetGravityOnLoad = value; }
        public void setTractorBeamDistanceThreshold(double value) { tractorBeamDistanceThreshold = value; }
        public double getTractorBeamDistanceThreshold() { return tractorBeamDistanceThreshold; }
        public List<NBTTagCompound> getTileData() { return tileData; }
        public void addTileData(NBTTagCompound value) { if (value != null) tileData.add(value.copy()); }
        public BlockPos getStartPos() { return startPos; }
        public void setRotationDelta(float pitch, float yaw) {
            dataManager.set(PITCH_VELOCITY, pitch);
            dataManager.set(YAW_VELOCITY, yaw);
        }
        public void setRotationDelta(Vec2f rotation) {
            setRotationDelta(rotation == null ? 0.0F : rotation.x,
                    rotation == null ? 0.0F : rotation.y);
        }
        public Vec2f getRotationDelta() {
            return new Vec2f(dataManager.get(PITCH_VELOCITY), dataManager.get(YAW_VELOCITY));
        }
        public void setSize(float sizeX, float sizeY, float sizeZ) {
            setClusterSize(sizeX, sizeY, sizeZ);
            syncClusterData();
        }
        public float getClusterSizeX() { return clusterSizeX; }
        public float getClusterSizeY() { return clusterSizeY; }
        public float getClusterSizeZ() { return clusterSizeZ; }
        public float getClusterXRot(float partialTicks) { return getClusterPitch(partialTicks); }
        public float getClusterYRot(float partialTicks) { return getClusterYaw(partialTicks); }
        public float getShakeX(float partialTicks) { return previousShakeX + (shakeX - previousShakeX) * partialTicks; }
        public float getShakeZ(float partialTicks) { return previousShakeZ + (shakeZ - previousShakeZ) * partialTicks; }
        public float getFadeAmount(float partialTicks) {
            return previousFadeAmount + (fadeAmount - previousFadeAmount) * partialTicks;
        }

        static boolean isInsidePopulateRadius(int x, int y, int z, int radiusSquared) {
            return x * x + y * y + z * z < radiusSquared;
        }

        public NBTTagCompound getTileDataFromOffset(BlockPos offset) {
            BlockPos expected = startPos.add(offset);
            for (NBTTagCompound data : tileData) {
                if (data.getInteger("x") == expected.getX() && data.getInteger("y") == expected.getY()
                        && data.getInteger("z") == expected.getZ()) return data.copy();
            }
            return null;
        }

        public interface BlockStateSelector {
            boolean test(World world, BlockPos pos, IBlockState state);
        }

        public BlockClusterEntity splitAt(EnumFacing.Axis axis) {
            if (blocks.size() < 2 || world.isRemote) return null;
            // 上游以分割前的总方块数决定是否继续保留坍塌行为。
            boolean crumbleAfterSplit = blocks.size() >= 10 && shouldCrumble;
            Map<BlockPos, IBlockState> separated = new LinkedHashMap<BlockPos, IBlockState>();
            Map<BlockPos, IBlockState> remaining = new LinkedHashMap<BlockPos, IBlockState>(blocks);
            java.util.Iterator<Map.Entry<BlockPos, IBlockState>> iterator = remaining.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, IBlockState> entry = iterator.next();
                BlockPos offset = entry.getKey();
                int coordinate = axis == EnumFacing.Axis.X ? offset.getX() : axis == EnumFacing.Axis.Y ? offset.getY() : offset.getZ();
                // 上游以原点为分界，负坐标一侧成为新的质量簇。
                if (coordinate < 0) {
                    separated.put(offset, entry.getValue());
                    iterator.remove();
                }
            }
            if (separated.isEmpty() || remaining.isEmpty()) return null;
            BlockPos originalStartPos = startPos;
            float originalSizeX = clusterSizeX;
            float originalSizeY = clusterSizeY;
            float originalSizeZ = clusterSizeZ;
            if (!crumbleAfterSplit) shouldCrumble = false;
            // 上游保留分裂前的包围盒和坐标基准；两半仍以同一中心旋转和着陆。
            replaceBlockContents(remaining);
            setClusterSize(originalSizeX, originalSizeY, originalSizeZ);
            startPos = originalStartPos;
            syncClusterData();
            BlockClusterEntity split = new BlockClusterEntity(world);
            split.setPosition(posX, posY, posZ);
            split.replaceBlockContents(separated);
            split.startPos = originalStartPos;
            split.setClusterSize(originalSizeX, originalSizeY, originalSizeZ);
            split.motionX = motionX * 0.8D;
            split.motionY = motionY * 0.8D;
            split.motionZ = motionZ * 0.8D;
            split.setRotationDelta(dataManager.get(PITCH_VELOCITY), dataManager.get(YAW_VELOCITY));
            split.setNoGravity(hasNoGravity());
            split.setPhysics(physicsEnabled());
            split.shouldCrumble = shouldCrumble;
            split.setFadePos(getFadePos());
            split.setFadeStrength(getFadeStrength());
            split.setFadeDistanceOffset(getFadeDistanceOffset());
            split.syncClusterData();
            return split;
        }

        private void updateClusterSize() {
            if (blocks.isEmpty()) {
                setClusterSize(1.0F, 1.0F, 1.0F);
                return;
            }
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos pos : blocks.keySet()) {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }
            setClusterSize(Math.max(1.0F, maxX - minX + 1.0F),
                    Math.max(1.0F, maxY - minY + 1.0F),
                    Math.max(1.0F, maxZ - minZ + 1.0F));
        }

        private void setClusterSize(float sizeX, float sizeY, float sizeZ) {
            clusterSizeX = sizeX;
            clusterSizeY = sizeY;
            clusterSizeZ = sizeZ;
            super.setSize(Math.max(sizeX, sizeZ), sizeY);
            refreshClusterBoundingBox();
        }

        private void refreshClusterBoundingBox() {
            setEntityBoundingBox(new AxisAlignedBB(posX - clusterSizeX / 2.0D, posY,
                    posZ - clusterSizeZ / 2.0D, posX + clusterSizeX / 2.0D,
                    posY + clusterSizeY, posZ + clusterSizeZ / 2.0D));
        }

        @Override
        public void setPosition(double x, double y, double z) {
            super.setPosition(x, y, z);
            if (clusterSizeX > 0.0F && clusterSizeY > 0.0F && clusterSizeZ > 0.0F) {
                refreshClusterBoundingBox();
            }
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound compound) {
            compound.setTag("ClusterBlocks", writeBlocks());
            compound.setFloat("ClusterSizeX", clusterSizeX);
            compound.setFloat("ClusterSizeY", clusterSizeY);
            compound.setFloat("ClusterSizeZ", clusterSizeZ);
            compound.setFloat("ClusterPitch", clusterPitch);
            compound.setFloat("ClusterYaw", clusterYaw);
            compound.setFloat("PitchVelocity", dataManager.get(PITCH_VELOCITY));
            compound.setFloat("YawVelocity", dataManager.get(YAW_VELOCITY));
            compound.setBoolean("Physics", physicsEnabled());
            compound.setInteger("ClusterTime", time);
            compound.setInteger("ShakeTime", shakeTime);
            compound.setInteger("Sink", sink);
            compound.setBoolean("AntiStacking", antiStacking);
            compound.setBoolean("ShouldCrumble", shouldCrumble);
            compound.setBoolean("DropItems", dropItems);
            compound.setBoolean("ForceRender", forceRender());
            compound.setBoolean("CreatedFromBeam", createdFromBeam);
            compound.setBoolean("CreatedFromFallingBlock", createdFromFallingBlock);
            compound.setBoolean("ShouldNotCountToConsumedMass", shouldNotCountToConsumedMass);
            compound.setInteger("HeadCreatedFrom", headCreatedFrom);
            compound.setBoolean("ResetGravity", resetGravityOnLoad);
            BlockPos currentFadePos = getFadePos();
            if (currentFadePos != null) compound.setLong("FadePos", currentFadePos.toLong());
            compound.setFloat("FadeStrength", getFadeStrength());
            compound.setInteger("FadeDistanceOffset", getFadeDistanceOffset());
            compound.setDouble("TractorBeamDistanceThreshold", tractorBeamDistanceThreshold);
            compound.setLong("ClusterStartPos", startPos.toLong());
            NBTTagList tiles = new NBTTagList();
            for (NBTTagCompound data : tileData) tiles.appendTag(data.copy());
            compound.setTag("TileData", tiles);
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound compound) {
            readBlocks(compound.getTagList("ClusterBlocks", 10));
            if (compound.hasKey("ClusterSizeX", 5) && compound.hasKey("ClusterSizeY", 5)
                    && compound.hasKey("ClusterSizeZ", 5)) {
                setClusterSize(compound.getFloat("ClusterSizeX"), compound.getFloat("ClusterSizeY"),
                        compound.getFloat("ClusterSizeZ"));
            }
            clusterPitch = previousClusterPitch = compound.getFloat("ClusterPitch");
            clusterYaw = previousClusterYaw = compound.getFloat("ClusterYaw");
            setRotationDelta(compound.getFloat("PitchVelocity"), compound.getFloat("YawVelocity"));
            setPhysics(!compound.hasKey("Physics") || compound.getBoolean("Physics"));
            time = compound.getInteger("ClusterTime");
            setShakeTime(compound.getInteger("ShakeTime"));
            sink = compound.getInteger("Sink");
            antiStacking = compound.getBoolean("AntiStacking");
            shouldCrumble = compound.getBoolean("ShouldCrumble");
            dropItems = compound.getBoolean("DropItems");
            setForceRender(compound.getBoolean("ForceRender"));
            createdFromBeam = compound.getBoolean("CreatedFromBeam");
            createdFromFallingBlock = compound.getBoolean("CreatedFromFallingBlock");
            shouldNotCountToConsumedMass = compound.getBoolean("ShouldNotCountToConsumedMass");
            headCreatedFrom = compound.getInteger("HeadCreatedFrom");
            resetGravityOnLoad = compound.getBoolean("ResetGravity");
            if (resetGravityOnLoad) setNoGravity(false);
            setFadePos(compound.hasKey("FadePos") ? BlockPos.fromLong(compound.getLong("FadePos")) : null);
            setFadeStrength(compound.hasKey("FadeStrength") ? compound.getFloat("FadeStrength") : 10.0F);
            setFadeDistanceOffset(compound.getInteger("FadeDistanceOffset"));
            if (compound.hasKey("TractorBeamDistanceThreshold")) tractorBeamDistanceThreshold = compound.getDouble("TractorBeamDistanceThreshold");
            if (compound.hasKey("ClusterStartPos")) startPos = BlockPos.fromLong(compound.getLong("ClusterStartPos"));
            tileData.clear();
            NBTTagList tiles = compound.getTagList("TileData", 10);
            for (int i = 0; i < tiles.tagCount(); i++) tileData.add(tiles.getCompoundTagAt(i));
            syncClusterData();
        }

        private void syncClusterData() {
            if (world == null || world.isRemote) return;
            NBTTagCompound compound = new NBTTagCompound();
            compound.setTag("Blocks", writeBlocks());
            compound.setFloat("SizeX", clusterSizeX);
            compound.setFloat("SizeY", clusterSizeY);
            compound.setFloat("SizeZ", clusterSizeZ);
            compound.setLong("StartPos", startPos.toLong());
            NBTTagList tiles = new NBTTagList();
            for (NBTTagCompound data : tileData) tiles.appendTag(data.copy());
            compound.setTag("Tiles", tiles);
            dataManager.set(CLUSTER_DATA, compound);
        }

        private void applySynchronizedClusterData(NBTTagCompound compound) {
            if (compound == null || !compound.hasKey("Blocks", 9)) return;
            readBlocks(compound.getTagList("Blocks", 10));
            if (compound.hasKey("SizeX", 5) && compound.hasKey("SizeY", 5)
                    && compound.hasKey("SizeZ", 5)) {
                setClusterSize(compound.getFloat("SizeX"), compound.getFloat("SizeY"),
                        compound.getFloat("SizeZ"));
            }
            if (compound.hasKey("StartPos", 4)) startPos = BlockPos.fromLong(compound.getLong("StartPos"));
            tileData.clear();
            NBTTagList tiles = compound.getTagList("Tiles", 10);
            for (int index = 0; index < tiles.tagCount(); index++) {
                tileData.add(tiles.getCompoundTagAt(index));
            }
        }

        @Override
        public void notifyDataManagerChange(DataParameter<?> key) {
            super.notifyDataManagerChange(key);
            if (CLUSTER_DATA.equals(key) && world.isRemote) {
                applySynchronizedClusterData(dataManager.get(CLUSTER_DATA));
            } else if (PHYSICS.equals(key)) {
                noClip = !physicsEnabled();
            } else if (SHAKE_TIME.equals(key)) {
                shakeTime = dataManager.get(SHAKE_TIME);
            }
        }

        private NBTTagList writeBlocks() {
            NBTTagList list = new NBTTagList();
            for (Map.Entry<BlockPos, IBlockState> entry : blocks.entrySet()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("X", entry.getKey().getX());
                tag.setInteger("Y", entry.getKey().getY());
                tag.setInteger("Z", entry.getKey().getZ());
                tag.setInteger("State", Block.getStateId(entry.getValue()));
                list.appendTag(tag);
            }
            return list;
        }

        private void readBlocks(NBTTagList list) {
            clearBlockContents();
            for (int index = 0; index < list.tagCount(); index++) {
                NBTTagCompound tag = list.getCompoundTagAt(index);
                IBlockState state = Block.getStateById(tag.getInteger("State"));
                if (state != null) {
                    putBlock(new BlockPos(tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z")), state);
                }
            }
            updateClusterSize();
        }

        private static BlockPos calculateStartPos(Iterable<BlockPos> positions) {
            int minX = 0, minY = 0, minZ = 0;
            int maxX = 0, maxY = 0, maxZ = 0;
            for (BlockPos pos : positions) {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }
            return new BlockPos(minX + (maxX - minX) / 2.0D,
                    minY + (maxY - minY) / 2.0D,
                    minZ + (maxZ - minZ) / 2.0D);
        }

    }

    public abstract static class StormPartBase extends SickenedMobEntity {
        private static final DataParameter<Integer> OWNER_ID = EntityDataManager.createKey(StormPartBase.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> PART_INDEX = EntityDataManager.createKey(StormPartBase.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> STORM_PHASE = EntityDataManager.createKey(StormPartBase.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> STORM_PLAY_DEAD_STATE =
                EntityDataManager.createKey(StormPartBase.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> STORM_PLAY_DEAD_TICKS =
                EntityDataManager.createKey(StormPartBase.class, DataSerializers.VARINT);
        private static final DataParameter<Boolean> OTHER_HEADS_DISABLED =
                EntityDataManager.createKey(StormPartBase.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> INDEPENDENT_BOWELS_PART =
                EntityDataManager.createKey(StormPartBase.class, DataSerializers.BOOLEAN);
        private UUID ownerUuid;
        private WitherStormEntity owner;
        private int orphanTicks;

        protected StormPartBase(World world) {
            super(world);
            noClip = true;
            setNoGravity(true);
            setNoAI(true);
            enablePersistence();
        }

        @Override
        public boolean canBePushed() {
            return false;
        }

        @Override
        public boolean canBeCollidedWith() {
            return false;
        }

        @Override
        public boolean isPushedByWater() {
            return false;
        }

        @Override
        public void applyEntityCollision(Entity entityIn) {
        }

        @Override
        public void addVelocity(double x, double y, double z) {
        }

        @Override
        public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) {
        }

        @Override
        protected boolean canBeRidden(Entity entityIn) {
            return false;
        }

        @Override
        public boolean isEntityInsideOpaqueBlock() {
            return false;
        }

        @Override
        public boolean isPotionApplicable(PotionEffect effect) {
            return false;
        }

        @Override
        public boolean canBeLeashedTo(EntityPlayer player) {
            return false;
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(OWNER_ID, -1);
            dataManager.register(PART_INDEX, 0);
            dataManager.register(STORM_PHASE, 0);
            dataManager.register(STORM_PLAY_DEAD_STATE, WitherStormEntity.PlayDeadState.NORMAL_BEHAVIOR.ordinal());
            dataManager.register(STORM_PLAY_DEAD_TICKS, 0);
            dataManager.register(OTHER_HEADS_DISABLED, false);
            dataManager.register(INDEPENDENT_BOWELS_PART, false);
        }

        public void bindTo(WitherStormEntity owner, int index) {
            bindTo(owner, index, true);
        }

        protected void bindTo(WitherStormEntity owner, int index, boolean notifyStateChange) {
            this.owner = owner;
            ownerUuid = owner.getUniqueID();
            dataManager.set(OWNER_ID, owner.getEntityId());
            dataManager.set(PART_INDEX, index);
            synchronizeWithOwner(owner, notifyStateChange);
        }

        public void setIndependentBowelsPart() {
            dataManager.set(INDEPENDENT_BOWELS_PART, true);
            owner = null;
            ownerUuid = null;
            dataManager.set(OWNER_ID, -1);
        }

        public boolean isIndependentBowelsPart() {
            return dataManager.get(INDEPENDENT_BOWELS_PART);
        }

        public int getPartIndex() {
            return dataManager.get(PART_INDEX);
        }

        protected UUID getOwnerUuid() {
            return ownerUuid;
        }

        protected void setOwnerUuid(UUID uuid) {
            owner = null;
            ownerUuid = uuid;
            // 实体 ID 只在当前世界会话内有效，重载后必须重新解析 UUID。
            dataManager.set(OWNER_ID, -1);
        }

        protected WitherStormEntity getOwnerStorm() {
            if (owner != null && owner.world == world
                    && (ownerUuid == null || ownerUuid.equals(owner.getUniqueID()))) {
                return owner;
            }
            owner = null;
            Entity entity = world.getEntityByID(dataManager.get(OWNER_ID));
            if (entity instanceof WitherStormEntity
                    && (ownerUuid == null || ownerUuid.equals(entity.getUniqueID()))) {
                owner = (WitherStormEntity) entity;
                ownerUuid = owner.getUniqueID();
                return owner;
            }
            if (ownerUuid == null) return null;
            if (world instanceof WorldServer) {
                Entity resolved = ((WorldServer) world).getEntityFromUuid(ownerUuid);
                if (resolved instanceof WitherStormEntity) {
                    owner = (WitherStormEntity) resolved;
                }
            }
            if (owner == null) {
                List<WitherStormEntity> storms = world.getEntities(WitherStormEntity.class,
                        storm -> ownerUuid.equals(storm.getUniqueID()));
                if (!storms.isEmpty()) owner = storms.get(0);
            }
            if (owner == null) return null;
            dataManager.set(OWNER_ID, owner.getEntityId());
            return owner;
        }

        /** 将父风暴的持久化状态复制到分裂体，允许父实体短暂不在加载列表时继续渲染正确状态。 */
        protected void synchronizeWithOwner(WitherStormEntity owner) {
            synchronizeWithOwner(owner, true);
        }

        private void synchronizeWithOwner(WitherStormEntity owner, boolean notifyStateChange) {
            if (owner == null) return;
            ownerUuid = owner.getUniqueID();
            dataManager.set(OWNER_ID, owner.getEntityId());
            dataManager.set(STORM_PHASE, owner.getPhase());
            WitherStormEntity.PlayDeadState state = owner.getPlayDeadState();
            WitherStormEntity.PlayDeadState previous = getStormPlayDeadState();
            dataManager.set(STORM_PLAY_DEAD_STATE, state.ordinal());
            dataManager.set(STORM_PLAY_DEAD_TICKS, owner.getPlayDeadStateTicks());
            dataManager.set(OTHER_HEADS_DISABLED, owner.areOtherHeadsDisabled());
            if (notifyStateChange && previous != state) onOwnerPlayDeadStateChanged(previous, state);
        }

        protected void onOwnerPlayDeadStateChanged(WitherStormEntity.PlayDeadState previous,
                                                   WitherStormEntity.PlayDeadState current) { }

        void synchronizeStateFromOwner() {
            WitherStormEntity owner = getOwnerStorm();
            if (owner != null) synchronizeWithOwner(owner);
        }

        public int getStormPhase() {
            return MathHelper.clamp(dataManager.get(STORM_PHASE), 0, 7);
        }

        protected void setStormPhase(int phase) {
            dataManager.set(STORM_PHASE, MathHelper.clamp(phase, 0, 7));
        }

        public WitherStormEntity.PlayDeadState getStormPlayDeadState() {
            int state = MathHelper.clamp(dataManager.get(STORM_PLAY_DEAD_STATE), 0,
                    WitherStormEntity.PlayDeadState.values().length - 1);
            return WitherStormEntity.PlayDeadState.values()[state];
        }

        public boolean isStormPlayDeadAiDisabled() {
            return getStormPlayDeadState() == WitherStormEntity.PlayDeadState.FALLING
                    || getStormPlayDeadState() == WitherStormEntity.PlayDeadState.PLAYING_DEAD;
        }

        public int getStormPlayDeadTicks() {
            return Math.max(0, dataManager.get(STORM_PLAY_DEAD_TICKS));
        }

        public boolean areStormOtherHeadsDisabled() {
            return dataManager.get(OTHER_HEADS_DISABLED);
        }

        @Override
        public void onLivingUpdate() {
            if (isIndependentBowelsPart()) {
                super.onLivingUpdate();
                motionX = motionY = motionZ = 0.0D;
                return;
            }
            WitherStormEntity owner = getOwnerStorm();
            if (owner == null || owner.isDead) {
                if (!world.isRemote && shouldDiscardWhenOwnerMissing()) {
                    if (++orphanTicks > 200) setDead();
                } else {
                    orphanTicks = 0;
                }
                return;
            }
            orphanTicks = 0;
            synchronizeWithOwner(owner);
            double[] offset = getOffset(owner, dataManager.get(PART_INDEX));
            updateAttachedPosition(owner, owner.posX + offset[0], owner.posY + offset[1], owner.posZ + offset[2]);
            rotationYaw = owner.rotationYaw;
            rotationYawHead = owner.rotationYawHead;
            if (shouldResetAttachedMotion()) motionX = motionY = motionZ = 0.0D;
            dataManager.set(STORM_PHASE, owner.getPhase());
        }

        protected void updateAttachedPosition(WitherStormEntity owner, double x, double y, double z) {
            setPosition(x, y, z);
        }

        protected boolean shouldResetAttachedMotion() {
            return true;
        }

        protected boolean shouldDiscardWhenOwnerMissing() {
            return true;
        }

        protected abstract double[] getOffset(WitherStormEntity owner, int index);

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (isIndependentBowelsPart()) return attackPartDirectly(source, amount);
            WitherStormEntity owner = getOwnerStorm();
            return owner != null && owner.attackEntityFrom(source, amount * getDamageTransfer());
        }

        protected boolean attackPartDirectly(DamageSource source, float amount) {
            return super.attackEntityFrom(source, amount);
        }

        protected float getDamageTransfer() { return 1.0F; }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            if (ownerUuid != null) compound.setUniqueId("WitherStormOwner", ownerUuid);
            compound.setInteger("WitherStormPartIndex", dataManager.get(PART_INDEX));
            compound.setInteger("WitherStormPartPhase", dataManager.get(STORM_PHASE));
            compound.setInteger("WitherStormPartPlayDeadState", dataManager.get(STORM_PLAY_DEAD_STATE));
            compound.setInteger("WitherStormPartPlayDeadTicks", dataManager.get(STORM_PLAY_DEAD_TICKS));
            compound.setBoolean("WitherStormPartOtherHeadsDisabled", dataManager.get(OTHER_HEADS_DISABLED));
            compound.setBoolean("IndependentBowelsPart", isIndependentBowelsPart());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            ownerUuid = compound.hasUniqueId("WitherStormOwner") ? compound.getUniqueId("WitherStormOwner") : null;
            dataManager.set(PART_INDEX, compound.getInteger("WitherStormPartIndex"));
            dataManager.set(STORM_PHASE, compound.getInteger("WitherStormPartPhase"));
            dataManager.set(STORM_PLAY_DEAD_STATE, MathHelper.clamp(
                    compound.getInteger("WitherStormPartPlayDeadState"), 0,
                    WitherStormEntity.PlayDeadState.values().length - 1));
            dataManager.set(STORM_PLAY_DEAD_TICKS, Math.max(0,
                    compound.getInteger("WitherStormPartPlayDeadTicks")));
            dataManager.set(OTHER_HEADS_DISABLED, compound.getBoolean("WitherStormPartOtherHeadsDisabled"));
            dataManager.set(INDEPENDENT_BOWELS_PART, compound.getBoolean("IndependentBowelsPart"));
        }

        @Override protected void despawnEntity() { }

        @Override
        public void setDead() {
            if (!world.isRemote && this instanceof WitherStormSegmentEntity && !isIndependentBowelsPart()) {
                ChunkLoadingManager.INSTANCE.releaseEntity(world, "segment", getUniqueID());
            }
            super.setDead();
        }
    }

    public static class CommandBlockEntity extends StormPartBase
            implements BossThemeProvider, IEntityAdditionalSpawnData {
        private static final byte HIT_GLARE_STATUS = 15;
        private static final DataParameter<Integer> CORE_STATE =
                EntityDataManager.createKey(CommandBlockEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> CORE_MODE =
                EntityDataManager.createKey(CommandBlockEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> MODE_ANIMATION =
                EntityDataManager.createKey(CommandBlockEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Float> PROTECTION_Y_OFFSET =
                EntityDataManager.createKey(CommandBlockEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Integer> LURING_PLAYER_ID =
                EntityDataManager.createKey(CommandBlockEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Boolean> PODIUM_ANCHOR_VALID =
                EntityDataManager.createKey(CommandBlockEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Integer> PODIUM_ANCHOR_X =
                EntityDataManager.createKey(CommandBlockEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> PODIUM_ANCHOR_Y =
                EntityDataManager.createKey(CommandBlockEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> PODIUM_ANCHOR_Z =
                EntityDataManager.createKey(CommandBlockEntity.class, DataSerializers.VARINT);
        private static final float[] RIB_BASE_Y_ROTATIONS = {0.0F, 180.0F, 145.0F, 35.0F, -35.0F, 215.0F};

        private final CommandBlockTentacleManager tentacleManager = new CommandBlockTentacleManager(this);
        private final RibAnimation[] ribAnimations = createRibAnimations();
        private UUID podiumClusterUuid;
        private BlockClusterEntity podiumCluster;
        private double podiumClusterYOffset = Double.NaN;
        private int coreStateTicks;
        private int modeAnimationTicks;
        private int previousModeAnimationTicks;
        private int specialDeathTime;
        private int hitGlareTime;
        private float previousProtectionYOffset;
        private EntityPlayer luringPlayer;
        private final BossInfoServer coreBossInfo = new BossInfoServer(
                getDisplayName(), BossInfo.Color.WHITE, BossInfo.Overlay.PROGRESS);
        private final Set<EntityPlayerMP> directBossBarViewers = new HashSet<EntityPlayerMP>();
        private final Set<EntityPlayerMP> outsideBossBarViewers = new HashSet<EntityPlayerMP>();

        public enum CoreMode {
            NONE,
            RIBS,
            TENTACLES
        }

        public enum CoreState {
            IDLE,
            PLAYING_DEAD,
            LURING,
            REACTIVATING,
            BOSSFIGHT
        }

        /** 保存单根肋骨根节点和四节骨段的平滑旋转状态。 */
        public static final class RibAnimation {
            private final float baseXRotationOffset;
            private final float baseYRotationOffset;
            private float baseXRotation;
            private float previousBaseXRotation;
            private float targetBaseXRotation;
            private int baseXRotationSteps;
            private float baseYRotation;
            private float previousBaseYRotation;
            private float targetBaseYRotation;
            private int baseYRotationSteps;
            private float xRotation;
            private float previousXRotation;
            private float targetXRotation;
            private int xRotationSteps;
            private float yRotation;
            private float previousYRotation;
            private float targetYRotation;
            private int yRotationSteps;

            private RibAnimation(float baseXRotationOffset, float baseYRotationOffset) {
                this.baseXRotationOffset = baseXRotationOffset;
                this.baseYRotationOffset = baseYRotationOffset;
            }

            private void tick() {
                previousBaseXRotation = baseXRotation;
                previousBaseYRotation = baseYRotation;
                previousXRotation = xRotation;
                previousYRotation = yRotation;
                if (baseXRotationSteps > 0) {
                    baseXRotation += MathHelper.wrapDegrees(targetBaseXRotation - baseXRotation)
                            / baseXRotationSteps--;
                }
                if (baseYRotationSteps > 0) {
                    baseYRotation += MathHelper.wrapDegrees(targetBaseYRotation - baseYRotation)
                            / baseYRotationSteps--;
                }
                if (xRotationSteps > 0) {
                    xRotation += MathHelper.wrapDegrees(targetXRotation - xRotation) / xRotationSteps--;
                }
                if (yRotationSteps > 0) {
                    yRotation += MathHelper.wrapDegrees(targetYRotation - yRotation) / yRotationSteps--;
                }
            }

            private boolean moveBaseTo(float x, float y, int steps) {
                boolean changed = targetBaseXRotation != x || targetBaseYRotation != y;
                if (targetBaseXRotation != x) {
                    targetBaseXRotation = x;
                    baseXRotationSteps = steps;
                }
                if (targetBaseYRotation != y) {
                    targetBaseYRotation = y;
                    baseYRotationSteps = steps;
                }
                return changed;
            }

            private boolean moveTo(float x, float y, int steps) {
                boolean changed = targetXRotation != x || targetYRotation != y;
                if (targetXRotation != x) {
                    targetXRotation = x;
                    xRotationSteps = steps;
                }
                if (targetYRotation != y) {
                    targetYRotation = y;
                    yRotationSteps = steps;
                }
                return changed;
            }

            public float getBaseXRotation(float partialTicks) {
                return previousBaseXRotation
                        + (baseXRotation - previousBaseXRotation) * partialTicks
                        + baseXRotationOffset;
            }

            public float getBaseYRotation(float partialTicks) {
                return previousBaseYRotation
                        + MathHelper.wrapDegrees(baseYRotation - previousBaseYRotation) * partialTicks
                        + baseYRotationOffset;
            }

            public float getXRotation(float partialTicks) {
                return previousXRotation
                        + MathHelper.wrapDegrees(xRotation - previousXRotation) * partialTicks;
            }

            public float getYRotation(float partialTicks) {
                return previousYRotation
                        + MathHelper.wrapDegrees(yRotation - previousYRotation) * partialTicks;
            }

            private NBTTagCompound writeToNBT() {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setFloat("BaseXRot", baseXRotation);
                compound.setFloat("BaseYRot", baseYRotation);
                compound.setFloat("XRot", xRotation);
                compound.setFloat("YRot", yRotation);
                return compound;
            }

            private void readFromNBT(NBTTagCompound compound) {
                baseXRotation = compound.hasKey("BaseXRot", 5)
                        ? compound.getFloat("BaseXRot") : compound.getFloat("BaseXRotation");
                previousBaseXRotation = baseXRotation;
                baseYRotation = compound.hasKey("BaseYRot", 5)
                        ? compound.getFloat("BaseYRot") : compound.getFloat("BaseYRotation");
                previousBaseYRotation = baseYRotation;
                xRotation = compound.hasKey("XRot", 5)
                        ? compound.getFloat("XRot") : compound.getFloat("XRotation");
                previousXRotation = xRotation;
                yRotation = compound.hasKey("YRot", 5)
                        ? compound.getFloat("YRot") : compound.getFloat("YRotation");
                previousYRotation = yRotation;
            }

            private void writeToBuffer(ByteBuf buffer) {
                buffer.writeFloat(baseXRotation);
                buffer.writeFloat(targetBaseXRotation);
                buffer.writeInt(baseXRotationSteps);
                buffer.writeFloat(baseYRotation);
                buffer.writeFloat(targetBaseYRotation);
                buffer.writeInt(baseYRotationSteps);
                buffer.writeFloat(xRotation);
                buffer.writeFloat(targetXRotation);
                buffer.writeInt(xRotationSteps);
                buffer.writeFloat(yRotation);
                buffer.writeFloat(targetYRotation);
                buffer.writeInt(yRotationSteps);
            }

            private void readFromBuffer(ByteBuf buffer) {
                baseXRotation = buffer.readFloat();
                previousBaseXRotation = baseXRotation;
                targetBaseXRotation = buffer.readFloat();
                baseXRotationSteps = Math.max(0, buffer.readInt());
                baseYRotation = buffer.readFloat();
                previousBaseYRotation = baseYRotation;
                targetBaseYRotation = buffer.readFloat();
                baseYRotationSteps = Math.max(0, buffer.readInt());
                xRotation = buffer.readFloat();
                previousXRotation = xRotation;
                targetXRotation = buffer.readFloat();
                xRotationSteps = Math.max(0, buffer.readInt());
                yRotation = buffer.readFloat();
                previousYRotation = yRotation;
                targetYRotation = buffer.readFloat();
                yRotationSteps = Math.max(0, buffer.readInt());
            }
        }

        private static RibAnimation[] createRibAnimations() {
            RibAnimation[] animations = new RibAnimation[RIB_BASE_Y_ROTATIONS.length];
            for (int index = 0; index < animations.length; index++) {
                animations[index] = new RibAnimation(0.0F, RIB_BASE_Y_ROTATIONS[index]);
            }
            return animations;
        }

        public CommandBlockEntity(World world) {
            super(world);
            setSize(1.0F, 1.0F);
            experienceValue = 10;
            coreBossInfo.setVisible(false);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(CORE_STATE, CoreState.IDLE.ordinal());
            dataManager.register(CORE_MODE, CoreMode.NONE.ordinal());
            dataManager.register(MODE_ANIMATION, 0);
            dataManager.register(PROTECTION_Y_OFFSET, 0.0F);
            dataManager.register(LURING_PLAYER_ID, -1);
            dataManager.register(PODIUM_ANCHOR_VALID, false);
            dataManager.register(PODIUM_ANCHOR_X, 0);
            dataManager.register(PODIUM_ANCHOR_Y, 0);
            dataManager.register(PODIUM_ANCHOR_Z, 0);
        }

        @Override protected double getSickenedHealth() { return 64.0D; }
        @Override protected double getSickenedDamage() { return 18.0D; }
        @Override protected double getSickenedSpeed() { return 0.0D; }
        @Override protected double getSickenedArmor() { return 32.0D; }
        @Override protected double getSickenedKnockbackResistance() { return 1.0D; }
        @Override public String getSickenedType() { return "command_block"; }

        @Override public float getEyeHeight() { return 0.5F; }
        @Override public AxisAlignedBB getRenderBoundingBox() { return getEntityBoundingBox().grow(20.0D); }
        @Override public boolean canBeCollidedWith() { return true; }
        /** The upstream LivingEntity is pickable even though it has no collision box. */
        @Override public boolean canBeAttackedWithItem() { return true; }

        /**
         * The command block is rendered together with a large ribcage, while its
         * physical entity remains the upstream 1x1x1 core.  This envelope is for
         * interaction selection only; it is intentionally not used for physics.
         */
        public AxisAlignedBB getInteractionBoundingBox() {
            return getEntityBoundingBox().grow(2.0D);
        }
        /**
         * 1.12's entity attack packet ray-tests the physical entity box before
         * Forge emits AttackEntityEvent.  The upstream renderer exposes a
         * ribcage much larger than the 1x1 command block, so retain the 1x1
         * physics box but use the same two-block interaction envelope for
         * vanilla entity selection as the client ray fallback.
         */
        @Override public float getCollisionBorderSize() { return 2.0F; }
        @Override public boolean hasNoGravity() { return true; }
        @Override public boolean isImmuneToExplosions() { return true; }
        @Override public EnumPushReaction getPushReaction() { return EnumPushReaction.IGNORE; }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return ModSounds.get("command_block_damage");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return ModSounds.get("command_block_death");
        }

        @Override
        protected float getSoundVolume() {
            return 4.0F;
        }

        @Override
        protected float getSoundPitch() {
            return 1.0F;
        }

        /** 上游保留 10 点经验奖励值，但明确禁止命令方块实体实际生成经验球。 */
        @Override
        protected int getExperiencePoints(EntityPlayer player) {
            return 0;
        }

        @Override
        public void setIndependentBowelsPart() {
            super.setIndependentBowelsPart();
            setCoreState(CoreState.BOSSFIGHT);
            setCoreMode(CoreMode.TENTACLES);
        }

        public void setBowelsOwnerUuid(UUID uuid) {
            if (uuid == null ? getOwnerUuid() != null : !uuid.equals(getOwnerUuid())) {
                setOwnerUuid(uuid);
            }
        }

        @Override
        public void setPositionAndRotationDirect(double x, double y, double z,
                                                 float yaw, float pitch,
                                                 int positionRotationIncrements,
                                                 boolean teleport) {
            if (world.isRemote && isIndependentBowelsPart()) {
                setPosition(x, y, z);
                setRotation(yaw, pitch);
                return;
            }
            super.setPositionAndRotationDirect(x, y, z, yaw, pitch,
                    positionRotationIncrements, teleport);
        }

        @Override protected double[] getOffset(WitherStormEntity owner, int index) {
            BlockPos podium = owner.getPlayingDeadPodiumPosition();
            if (podium != null) {
                return new double[]{podium.getX() + 0.5D - owner.posX,
                        podium.getY() + 11.0D - owner.posY,
                        podium.getZ() + 0.5D - owner.posZ};
            }
            return new double[]{0.0D, 1.0D, 0.0D};
        }

        @Override
        protected void updateAttachedPosition(WitherStormEntity owner, double x, double y, double z) {
            prevRenderYawOffset = owner.prevRenderYawOffset;
            renderYawOffset = owner.renderYawOffset;
            prevRotationYaw = owner.prevRotationYaw;
            rotationYaw = owner.rotationYaw;
            // The podium anchor is authoritative on the server. Never derive the
            // client position from the storm body, because its interpolated owner
            // position can put the core back inside the fallen model.
            if (world.isRemote) return;
            BlockPos podium = owner.getPlayingDeadPodiumPosition();
            if (podium != null) setPlayingDeadPodiumAnchor(podium);
            super.updateAttachedPosition(owner, x, y, z);
        }

        public void setPlayingDeadPodiumAnchor(BlockPos podium) {
            if (podium == null) {
                dataManager.set(PODIUM_ANCHOR_VALID, false);
                return;
            }
            dataManager.set(PODIUM_ANCHOR_X, podium.getX());
            dataManager.set(PODIUM_ANCHOR_Y, podium.getY());
            dataManager.set(PODIUM_ANCHOR_Z, podium.getZ());
            dataManager.set(PODIUM_ANCHOR_VALID, true);
        }

        private void applyPlayingDeadPodiumAnchor() {
            if (!dataManager.get(PODIUM_ANCHOR_VALID) || isIndependentBowelsPart()) return;
            setPosition(dataManager.get(PODIUM_ANCHOR_X) + 0.5D,
                    dataManager.get(PODIUM_ANCHOR_Y) + 11.0D,
                    dataManager.get(PODIUM_ANCHOR_Z) + 0.5D);
        }

        /**
         * Handles the upstream command-block-tool interaction without depending
         * on 1.12's inconsistent player DamageSource path.
         */
        public boolean attackPlayingDeadCore(EntityPlayer player) {
            if (world.isRemote || player == null || isIndependentBowelsPart()
                    || getCoreState() != CoreState.PLAYING_DEAD
                    || !isCommandBlockTool(player.getHeldItemMainhand())) return false;
            handleCommandBlockToolHit(player);
            setCoreState(CoreState.REACTIVATING);
            return true;
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (source == DamageSource.OUT_OF_WORLD) return attackPartDirectly(source, amount);
            if (isIndependentBowelsPart()) return BowelsBossfightController.attack(this, source);

            // Forge 1.12 damage sources produced by some item/attack paths do
            // not consistently populate trueSource. The upstream 1.20 code
            // uses the immediate attacker; retain a true-source fallback for
            // vanilla/player damage sources.
            Entity trueSource = source.getImmediateSource();
            if (!(trueSource instanceof EntityLivingBase)) trueSource = source.getTrueSource();
            boolean commandBlockTool = false;
            if (trueSource instanceof EntityLivingBase) {
                EntityLivingBase attacker = (EntityLivingBase) trueSource;
                commandBlockTool = isCommandBlockTool(attacker.getHeldItemMainhand());
                if (commandBlockTool) handleCommandBlockToolHit(attacker);
            }
            if (!world.isRemote && getCoreState() == CoreState.PLAYING_DEAD
                    && (source.isExplosion() || commandBlockTool)) {
                setCoreState(CoreState.REACTIVATING);
            }
            return false;
        }

        private void handleCommandBlockToolHit(EntityLivingBase attacker) {
            playSound(ModSounds.get("command_block_hit"), 4.0F, 1.0F);
            if (!world.isRemote && getCoreState() != CoreState.BOSSFIGHT) {
                attacker.knockBack(this, 1.0F, posX - attacker.posX, posZ - attacker.posZ);
            }
        }

        private static boolean isCommandBlockTool(ItemStack stack) {
            return UpstreamItemTags.contains(UpstreamItemTags.COMMAND_BLOCK_TOOLS, stack)
                    || isLegacyCommandBlockTool(stack);
        }

        private static boolean isLegacyCommandBlockTool(ItemStack stack) {
            if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return false;
            ResourceLocation id = stack.getItem().getRegistryName();
            if (!Tags.MOD_ID.equals(id.getNamespace())) return false;
            String path = id.getPath();
            return path.equals("eye_of_the_storm") || path.equals("formidi_blade")
                    || path.matches("(?:wooden_|stone_|iron_|gold_)?command_block_(?:sword|pickaxe|axe|shovel|hoe)");
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            if (world.isRemote) applyPlayingDeadPodiumAnchor();
            previousModeAnimationTicks = modeAnimationTicks;
            previousProtectionYOffset = getProtectionYOffset();
            tickCoreMode();
            if (world.isRemote) {
                beginCoreStateTick();
                tickRibInterpolation();
                if (hitGlareTime > 0) --hitGlareTime;
                return;
            }
            if (isIndependentBowelsPart()) {
                beginCoreStateTick();
                tickRibInterpolation();
                BowelsBossfightController.tick(this);
                updateCoreBossInfo();
                return;
            }
            WitherStormEntity owner = getOwnerStorm();
            if (owner != null && !owner.isDead) {
                owner.registerPlayingDeadCommandBlock(this);
                if (getCoreState() == CoreState.IDLE
                        && owner.getPlayDeadState() == WitherStormEntity.PlayDeadState.PLAYING_DEAD) {
                    setCoreState(CoreState.PLAYING_DEAD);
                }
            }
            tickCoreState(owner);
            tickRibInterpolation();
            updateCoreBossInfo();
        }

        private void tickCoreMode() {
            ++modeAnimationTicks;
            if (world.isRemote) return;
            tentacleManager.tick(getCoreMode());
            if (ticksExisted % 120 == 0) dataManager.set(MODE_ANIMATION, modeAnimationTicks);
        }

        private void updateCoreBossInfo() {
            coreBossInfo.setPercent(MathHelper.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
            coreBossInfo.setVisible(isEntityAlive() && getCoreState() == CoreState.BOSSFIGHT
                    && (getHealth() < getMaxHealth()
                    || BowelsBossfightController.shouldShowBossBar(this)));
        }

        private void beginCoreStateTick() {
            ++coreStateTicks;
            CoreState state = getCoreState();
            if (!world.isRemote && state != CoreState.PLAYING_DEAD) {
                float particleSpeed = (getMaxHealth() - getHealth()) / getMaxHealth() + 1.0F;
                ModNetwork.sendCommandBlockTickParticles(this, particleSpeed,
                        state == CoreState.LURING ? dataManager.get(LURING_PLAYER_ID) : -1);
            }
            if (getCoreMode() != CoreMode.RIBS) return;
            int delay = state == CoreState.LURING ? 40 : state == CoreState.REACTIVATING ? 20 : 0;
            if (state != CoreState.BOSSFIGHT && coreStateTicks <= delay) return;

            if (state == CoreState.PLAYING_DEAD) {
                Random random = new Random(getUniqueID().getLeastSignificantBits());
                for (RibAnimation animation : ribAnimations) {
                    moveRibBase(animation, -50.0F, 0.0F, Math.max(4, random.nextInt(11)));
                    moveRib(animation, Math.max(30.0F, random.nextInt(131)),
                            random.nextInt(21) - 10.0F, Math.max(4, random.nextInt(11)));
                }
            } else if (state == CoreState.REACTIVATING) {
                for (RibAnimation animation : ribAnimations) {
                    moveRibBase(animation, 0.0F, 0.0F, 40);
                    moveRib(animation, 70.0F, 0.0F, 20);
                }
                if (!world.isRemote && coreStateTicks > delay + 20
                        && getProtectionYOffset() > -0.8F) {
                    setProtectionYOffset(Math.max(-0.8F, getProtectionYOffset() - 0.05F));
                }
            } else {
                for (RibAnimation animation : ribAnimations) {
                    moveRibBase(animation, -50.0F, 0.0F, 10);
                    moveRib(animation, 60.0F, 0.0F, 20);
                }
            }
        }

        private void tickRibInterpolation() {
            for (RibAnimation animation : ribAnimations) animation.tick();
        }

        private void moveRibBase(RibAnimation animation, float x, float y, int steps) {
            if (animation.moveBaseTo(x, y, steps) && !world.isRemote) playRibMovementSound();
        }

        private void moveRib(RibAnimation animation, float x, float y, int steps) {
            if (animation.moveTo(x, y, steps) && !world.isRemote) playRibMovementSound();
        }

        private void playRibMovementSound() {
            SoundEvent sound = ModSounds.get("rib_bone_crack");
            if (sound == null) return;
            for (int index = 0; index < ribAnimations.length; index++) {
                world.playSound(null, posX + rand.nextGaussian() * 3.0D, posY + getEyeHeight(),
                        posZ + rand.nextGaussian() * 3.0D, sound, SoundCategory.AMBIENT, 0.2F, 0.8F);
            }
        }

        private void tickCoreState(@Nullable WitherStormEntity owner) {
            beginCoreStateTick();
            CoreState state = getCoreState();
            if (state == CoreState.PLAYING_DEAD) {
                EntityPlayerMP player = findLuringPlayer(6.0D);
                if (player != null) {
                    setLuringPlayer(player);
                    setCoreState(CoreState.LURING);
                }
                int revivalTime = Math.max(0, WitherStormConfig.revivalTimeMinutes);
                if (WitherStormConfig.revivalTimer && revivalTime > 0
                        && coreStateTicks > revivalTime * 1200) {
                    setCoreState(CoreState.REACTIVATING);
                }
            } else if (state == CoreState.LURING) {
                EntityPlayerMP player = getServerLuringPlayer();
                if (player == null || !isLuringPlayerEligible(player)
                        || getDistance(player) > 12.0F || coreStateTicks >= 240) {
                    setLuringPlayer(null);
                    setCoreState(CoreState.REACTIVATING);
                    return;
                }

                Vec3d pull = getPositionVector().subtract(player.getPositionVector()).normalize().scale(0.025D);
                player.motionX = pull.x;
                player.motionZ = pull.z;
                player.velocityChanged = true;
                ModNetwork.setPlayerMotion(player, player,
                        new Vec3d(player.motionX, player.motionY, player.motionZ));
                if (getDistance(player) < 3.0F) {
                    setLuringPlayer(null);
                    setCoreState(CoreState.REACTIVATING);
                }
            } else if (state == CoreState.REACTIVATING && owner != null && !owner.isDead
                    && coreStateTicks > 60
                    && owner.getPlayDeadState() != WitherStormEntity.PlayDeadState.REVIVING) {
                owner.reviveFromPlayingDead();
            }
        }

        @Nullable
        private EntityPlayerMP findLuringPlayer(double range) {
            EntityPlayerMP nearest = null;
            double nearestDistance = range * range;
            for (EntityPlayerMP player : world.getEntitiesWithinAABB(EntityPlayerMP.class,
                    getEntityBoundingBox().grow(range))) {
                double distance = getDistanceSq(player);
                if (distance <= nearestDistance && isLuringPlayerEligible(player)) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }
            return nearest;
        }

        private boolean isLuringPlayerEligible(EntityPlayer player) {
            return player != null && player.isEntityAlive() && !player.capabilities.isCreativeMode
                    && !player.isSpectator() && !player.isInvisible();
        }

        @Nullable
        private EntityPlayerMP getServerLuringPlayer() {
            if (luringPlayer instanceof EntityPlayerMP && !luringPlayer.isDead) {
                return (EntityPlayerMP) luringPlayer;
            }
            Entity entity = world.getEntityByID(dataManager.get(LURING_PLAYER_ID));
            luringPlayer = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
            return luringPlayer instanceof EntityPlayerMP ? (EntityPlayerMP) luringPlayer : null;
        }

        private void setLuringPlayer(@Nullable EntityPlayer player) {
            luringPlayer = player;
            dataManager.set(LURING_PLAYER_ID, player == null ? -1 : player.getEntityId());
        }

        public int getLuringPlayerId() {
            return dataManager.get(LURING_PLAYER_ID);
        }

        public CoreMode getCoreMode() {
            int mode = MathHelper.clamp(dataManager.get(CORE_MODE), 0, CoreMode.values().length - 1);
            return CoreMode.values()[mode];
        }

        public void setCoreMode(CoreMode mode) {
            if (mode == null || getCoreMode() == mode) return;
            CoreMode previous = getCoreMode();
            dataManager.set(CORE_MODE, mode.ordinal());
            modeAnimationTicks = 0;
            dataManager.set(MODE_ANIMATION, 0);
            if (!world.isRemote && previous == CoreMode.TENTACLES && mode != CoreMode.TENTACLES) {
                tentacleManager.removeTentacles();
            }
        }

        public int getModeAnimationTicks() {
            return world.isRemote ? Math.max(modeAnimationTicks, dataManager.get(MODE_ANIMATION))
                    : modeAnimationTicks;
        }

        public float getModeAnimation(float partialTicks) {
            int current = getModeAnimationTicks();
            return previousModeAnimationTicks + (current - previousModeAnimationTicks) * partialTicks;
        }

        public RibAnimation getRibAnimation(int index) {
            return index >= 0 && index < ribAnimations.length ? ribAnimations[index] : null;
        }

        public float getProtectionYOffset() {
            return dataManager.get(PROTECTION_Y_OFFSET);
        }

        public float getProtectionYOffset(float partialTicks) {
            float current = getProtectionYOffset();
            return previousProtectionYOffset + (current - previousProtectionYOffset) * partialTicks;
        }

        private void setProtectionYOffset(float offset) {
            dataManager.set(PROTECTION_Y_OFFSET, MathHelper.clamp(offset, -0.8F, 0.0F));
        }

        public CoreState getCoreState() {
            int state = MathHelper.clamp(dataManager.get(CORE_STATE), 0, CoreState.values().length - 1);
            return CoreState.values()[state];
        }

        public void setCoreState(CoreState state) {
            if (state == null || getCoreState() == state) return;
            dataManager.set(CORE_STATE, state.ordinal());
            coreStateTicks = 0;
            modeAnimationTicks = 0;
            dataManager.set(MODE_ANIMATION, 0);
            setProtectionYOffset(0.0F);
            if (state == CoreState.BOSSFIGHT) {
                setCoreMode(CoreMode.TENTACLES);
            } else if (state != CoreState.IDLE && !isIndependentBowelsPart()
                    && getCoreMode() == CoreMode.NONE) {
                setCoreMode(CoreMode.RIBS);
            }
            initializeCoreState(state);
        }

        private void initializeCoreState(CoreState state) {
            if (world.isRemote) return;
            if (state == CoreState.LURING) {
                world.playSound(null, getPosition(), ModSounds.get("command_block_activates"),
                        SoundCategory.HOSTILE, 5.0F, 1.0F);
                world.playSound(null, getPosition(), ModSounds.get("tremble"),
                        SoundCategory.AMBIENT, 10.0F, 1.0F);
                ModNetwork.shakeTracking(this, 40.0F, 5.0F);
                ModNetwork.sendCommandBlockParticles(world,
                        new Vec3d(posX, posY + getEyeHeight(), posZ), 10,
                        0.0D, 0.0D, 0.0D, 0.5D,
                        ModNetwork.COMMAND_BLOCK_PARTICLES_GAUSSIAN);
            } else if (state == CoreState.REACTIVATING) {
                world.playSound(null, getPosition(), ModSounds.get("tremble"),
                        SoundCategory.AMBIENT, 10.0F, 1.0F);
                ModNetwork.shakeTracking(this, 120.0F, 5.0F);
            }
        }

        public int getCoreStateTicks() {
            return Math.max(0, coreStateTicks);
        }

        public int getHitGlareTime() {
            return Math.max(0, hitGlareTime);
        }

        public int getSpecialDeathTime() {
            return Math.max(0, specialDeathTime);
        }

        public void triggerHitGlare() {
            if (!world.isRemote) world.setEntityState(this, HIT_GLARE_STATUS);
        }

        public boolean hasTrackingPlayers() {
            return !directBossBarViewers.isEmpty();
        }

        public boolean takeBowelsDamage(DamageSource source, float amount) {
            return attackPartDirectly(source, amount);
        }

        public void awakenStructureTentacles(boolean indefinite) {
            tentacleManager.awakenTentacles(indefinite);
        }

        public void curlStructureTentacles(boolean skipSwinging) {
            tentacleManager.curlTentacles(skipSwinging);
        }

        public void stopCurlingStructureTentacles() {
            tentacleManager.stopCurlingTentacles();
        }

        public void removeStructureTentacles() {
            tentacleManager.removeTentacles();
        }

        @Override
        public int getBrightnessForRender() {
            return getCoreMode() == CoreMode.TENTACLES
                    ? 15728880 : super.getBrightnessForRender();
        }

        public boolean shouldShowOwnerBossBar() {
            CoreState state = getCoreState();
            return state == CoreState.LURING || state == CoreState.REACTIVATING;
        }

        @Override
        public SoundEvent getBossTheme() {
            return ModSounds.get("wither_storm_final_boss_theme");
        }

        @Override
        public boolean shouldPlayBossTheme() {
            return isEntityAlive() && getHealth() < getMaxHealth()
                    && getCoreState() == CoreState.BOSSFIGHT;
        }

        @Override
        public int getBossThemePriority() {
            return 3;
        }

        @Override
        public int getBossThemeFadeTime() {
            return 40;
        }

        @Override
        public Vec3d getBossThemePosition() {
            return getPositionVector();
        }

        @Override
        public void addTrackingPlayer(EntityPlayerMP player) {
            if (player == null) return;
            directBossBarViewers.add(player);
            coreBossInfo.addPlayer(player);
        }

        @Override
        public void removeTrackingPlayer(EntityPlayerMP player) {
            directBossBarViewers.remove(player);
            if (!outsideBossBarViewers.contains(player)) coreBossInfo.removePlayer(player);
        }

        public void addOutsideBossBarViewer(EntityPlayerMP player) {
            if (player == null) return;
            outsideBossBarViewers.add(player);
            coreBossInfo.addPlayer(player);
        }

        public void removeOutsideBossBarViewer(EntityPlayerMP player) {
            outsideBossBarViewers.remove(player);
            if (!directBossBarViewers.contains(player)) coreBossInfo.removePlayer(player);
        }

        void synchronizeOutsideBossBarViewers(Iterable<EntityPlayerMP> players) {
            Set<EntityPlayerMP> desired = new HashSet<EntityPlayerMP>();
            for (EntityPlayerMP player : players) {
                if (player != null) desired.add(player);
            }
            for (EntityPlayerMP player : new HashSet<EntityPlayerMP>(outsideBossBarViewers)) {
                if (!desired.contains(player)) removeOutsideBossBarViewer(player);
            }
            for (EntityPlayerMP player : desired) addOutsideBossBarViewer(player);
        }

        @Override
        public void notifyDataManagerChange(DataParameter<?> key) {
            super.notifyDataManagerChange(key);
            if (!world.isRemote) return;
            if (CORE_STATE.equals(key)) {
                coreStateTicks = 0;
                modeAnimationTicks = 0;
            } else if (CORE_MODE.equals(key)) {
                modeAnimationTicks = 0;
            } else if (MODE_ANIMATION.equals(key)) {
                modeAnimationTicks = dataManager.get(MODE_ANIMATION);
            }
        }

        @Override
        public void handleStatusUpdate(byte id) {
            if (id == HIT_GLARE_STATUS) {
                hitGlareTime = 60;
            } else {
                super.handleStatusUpdate(id);
            }
        }

        public void createPodiumCluster() {
            findPodiumCluster();
            if (world.isRemote || podiumCluster != null || podiumClusterUuid != null) return;
            BlockPos center = getPosition();
            BlockClusterEntity cluster = new BlockClusterEntity(world);
            cluster.populate(center.add(-5, -13, -5), center.add(5, 6, 5));
            if (cluster.getBlocks().isEmpty()) return;
            cluster.setResetGravityOnLoad(false);
            cluster.setNoGravity(true);
            cluster.setPhysics(false);
            cluster.setForceRender(true);
            if (world.spawnEntity(cluster)) {
                podiumCluster = cluster;
                podiumClusterUuid = cluster.getUniqueID();
                podiumClusterYOffset = cluster.posY - posY;
            }
        }

        /**
         * Applies one authoritative lift pose to the command block and podium.
         * No caller may advance either entity independently.
         */
        public void applyBowelsPodiumLiftPose(double expectedY) {
            if (world.isRemote) return;
            findPodiumCluster();
            if (podiumCluster != null && !podiumCluster.isDead) {
                double offsetY = getPodiumClusterYOffset();
                podiumCluster.setPosition(posX, expectedY + offsetY, posZ);
                podiumCluster.motionX = 0.0D;
                podiumCluster.motionY = 0.0D;
                podiumCluster.motionZ = 0.0D;
            }
            setPosition(posX, expectedY, posZ);
            motionX = motionY = motionZ = 0.0D;
        }

        private double getPodiumClusterYOffset() {
            if (!Double.isNaN(podiumClusterYOffset)) return podiumClusterYOffset;
            int verticalCenter = MathHelper.floor(
                    (podiumCluster.getEntityBoundingBox().maxY
                            - podiumCluster.getEntityBoundingBox().minY) / 2.0D - 0.5D);
            int topOffset = Integer.MIN_VALUE;
            Block topBlock = ModBlocks.get("tainted_dust_block");
            for (Map.Entry<BlockPos, IBlockState> entry : podiumCluster.getBlocks().entrySet()) {
                BlockPos offset = entry.getKey();
                if (offset.getX() == 0 && offset.getZ() == 0
                        && entry.getValue().getBlock() == topBlock) {
                    topOffset = Math.max(topOffset, offset.getY());
                }
            }
            podiumClusterYOffset = topOffset == Integer.MIN_VALUE
                    ? -13.0D : -1.0D - verticalCenter - topOffset;
            return podiumClusterYOffset;
        }

        public void finishPodiumMove(double expectedY) {
            applyBowelsPodiumLiftPose(expectedY);
            findPodiumCluster();
            BlockPos blockPosition = new BlockPos(posX, expectedY, posZ);
            // This port's populate()/place() pair already round-trips the
            // captured blocks at the entity position. The modern cluster has
            // a different origin convention and its extra +1Y must not be
            // copied here or the podium top intersects the command block.
            Vec3d alignedPosition = new Vec3d(blockPosition.getX() + 0.5D,
                    blockPosition.getY(), blockPosition.getZ() + 0.5D);
            if (podiumCluster != null && !podiumCluster.isDead) {
                podiumCluster.place();
            }
            setPosition(alignedPosition.x, alignedPosition.y, alignedPosition.z);
            podiumCluster = null;
            podiumClusterUuid = null;
            podiumClusterYOffset = Double.NaN;
        }

        public void findPodiumCluster() {
            if (podiumCluster != null && !podiumCluster.isDead) return;
            podiumCluster = null;
            if (podiumClusterUuid == null || !(world instanceof WorldServer)) return;
            Entity entity = ((WorldServer) world).getEntityFromUuid(podiumClusterUuid);
            if (entity instanceof BlockClusterEntity && !entity.isDead) podiumCluster = (BlockClusterEntity) entity;
        }

        public BlockClusterEntity getPodiumCluster() {
            findPodiumCluster();
            return podiumCluster;
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setInteger("Mode", getCoreMode().ordinal());
            compound.setInteger("State", getCoreState().ordinal());
            compound.setInteger("StateTicks", coreStateTicks);
            compound.setInteger("ModeAnim", modeAnimationTicks);
            compound.setFloat("YOffset", getProtectionYOffset());
            compound.setFloat("YBodyRot", renderYawOffset);
            NBTTagList structures = new NBTTagList();
            for (RibAnimation animation : ribAnimations) structures.appendTag(animation.writeToNBT());
            compound.setTag("Structures", structures);
            UUID owner = getOwnerUuid();
            if (owner != null) compound.setUniqueId("OwnerUUID", owner);
            tentacleManager.writeToNBT(compound);
            if (podiumClusterUuid != null) compound.setUniqueId("PodiumCluster", podiumClusterUuid);
            if (!Double.isNaN(podiumClusterYOffset)) {
                compound.setDouble("PodiumClusterYOffset", podiumClusterYOffset);
            }
            // 1.12 的独立死亡控制器需要在重载后继续其 240 tick 序列。
            compound.setInteger("CommandBlockDeathTicks", specialDeathTime);
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            podiumClusterUuid = compound.hasUniqueId("PodiumCluster") ? compound.getUniqueId("PodiumCluster") : null;
            podiumClusterYOffset = compound.hasKey("PodiumClusterYOffset", 6)
                    ? compound.getDouble("PodiumClusterYOffset") : Double.NaN;
            String stateKey = compound.hasKey("State", 3) ? "State" : "CommandBlockState";
            if (compound.hasKey(stateKey, 3)) {
                dataManager.set(CORE_STATE, MathHelper.clamp(compound.getInteger(stateKey),
                        0, CoreState.values().length - 1));
            } else if (isIndependentBowelsPart()) {
                dataManager.set(CORE_STATE, CoreState.BOSSFIGHT.ordinal());
            }
            coreStateTicks = Math.max(0, compound.hasKey("StateTicks", 3)
                    ? compound.getInteger("StateTicks") : compound.getInteger("CommandBlockStateTicks"));
            CoreMode fallbackMode = isIndependentBowelsPart() ? CoreMode.TENTACLES
                    : getCoreState() == CoreState.IDLE ? CoreMode.NONE : CoreMode.RIBS;
            int mode = compound.hasKey("Mode", 3) ? compound.getInteger("Mode")
                    : compound.hasKey("CommandBlockMode", 3)
                    ? compound.getInteger("CommandBlockMode") : fallbackMode.ordinal();
            dataManager.set(CORE_MODE, MathHelper.clamp(mode, 0, CoreMode.values().length - 1));
            modeAnimationTicks = Math.max(0, compound.hasKey("ModeAnim", 3)
                    ? compound.getInteger("ModeAnim") : compound.getInteger("CommandBlockModeAnimation"));
            previousModeAnimationTicks = modeAnimationTicks;
            dataManager.set(MODE_ANIMATION, modeAnimationTicks);
            setProtectionYOffset(compound.hasKey("YOffset", 5)
                    ? compound.getFloat("YOffset") : compound.getFloat("CommandBlockProtectionYOffset"));
            previousProtectionYOffset = getProtectionYOffset();
            if (compound.hasKey("YBodyRot", 5)) {
                renderYawOffset = prevRenderYawOffset = compound.getFloat("YBodyRot");
            }
            if (compound.hasUniqueId("OwnerUUID")) setOwnerUuid(compound.getUniqueId("OwnerUUID"));
            specialDeathTime = Math.max(0, compound.getInteger("CommandBlockDeathTicks"));
            NBTTagList ribAnimationData = compound.hasKey("Structures", 9)
                    ? compound.getTagList("Structures", 10)
                    : compound.getTagList("CommandBlockRibAnimations", 10);
            for (int index = 0; index < ribAnimationData.tagCount()
                    && index < ribAnimations.length; index++) {
                ribAnimations[index].readFromNBT(ribAnimationData.getCompoundTagAt(index));
            }
            tentacleManager.readFromNBT(compound);
            luringPlayer = null;
            dataManager.set(LURING_PLAYER_ID, -1);
            BowelsBossfightController.restoreLoadedPhase(this);
        }

        @Override
        public void writeSpawnData(ByteBuf buffer) {
            buffer.writeInt(Math.max(0, coreStateTicks));
            buffer.writeInt(Math.max(0, modeAnimationTicks));
            buffer.writeFloat(getProtectionYOffset());
            for (RibAnimation animation : ribAnimations) animation.writeToBuffer(buffer);
        }

        @Override
        public void readSpawnData(ByteBuf buffer) {
            coreStateTicks = Math.max(0, buffer.readInt());
            modeAnimationTicks = Math.max(0, buffer.readInt());
            previousModeAnimationTicks = modeAnimationTicks;
            dataManager.set(MODE_ANIMATION, modeAnimationTicks);
            setProtectionYOffset(buffer.readFloat());
            previousProtectionYOffset = getProtectionYOffset();
            for (RibAnimation animation : ribAnimations) animation.readFromBuffer(buffer);
        }

        @Override
        public void setCustomNameTag(String name) {
            super.setCustomNameTag(name);
            coreBossInfo.setName(getDisplayName());
        }

        @Override
        public void onDeath(DamageSource source) {
            if (!world.isRemote && isIndependentBowelsPart()) {
                BowelsBossfightController.beginDeath(this, source);
            }
            super.onDeath(source);
        }

        @Override
        protected void onDeathUpdate() {
            if (!isIndependentBowelsPart()) {
                super.onDeathUpdate();
                return;
            }
            if (world.isRemote) {
                // Keep the client-side entity through the same 240-tick death
                // sequence. Calling vanilla onDeathUpdate at tick 161 would
                // remove the model around tick 180 while the server still has
                // the command block and arena animation alive.
                if (specialDeathTime <= 240) {
                    ++specialDeathTime;
                    return;
                }
                // The upstream death phase removes the core after its full
                // 240-tick animation rather than vanilla's 20-tick timer.
                setDead();
                return;
            } else if (BowelsBossfightController.isDeathSequence(this)) {
                BowelsBossfightController.tickDeath(this, ++specialDeathTime);
                return;
            }
            super.onDeathUpdate();
            if (!world.isRemote && deathTime >= 20) {
                BowelsBossfightController.finishDeathRemoval(this);
            }
        }

        @Override
        public void setDead() {
            if (!world.isRemote) tentacleManager.removeTentacles();
            clearBossBarState();
            super.setDead();
        }

        public void discardDuplicateBowelsCore() {
            if (!world.isRemote) {
                tentacleManager.discardOwnedTentaclesWithoutResolvingSavedReferences();
            }
            clearBossBarState();
            super.setDead();
        }

        private void clearBossBarState() {
            coreBossInfo.setVisible(false);
            directBossBarViewers.clear();
            outsideBossBarViewers.clear();
        }
    }

    public static class WitherStormHeadEntity extends StormPartBase
            implements TractorBeamProvider, WitherStormBase, DistantStormPart {
        private static final DataParameter<Boolean> ACTIVE = EntityDataManager.createKey(WitherStormHeadEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> ROARING = EntityDataManager.createKey(WitherStormHeadEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> BITING = EntityDataManager.createKey(WitherStormHeadEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> HURT = EntityDataManager.createKey(WitherStormHeadEntity.class, DataSerializers.BOOLEAN);

        private Vec3d distractedPos;
        private int distractedTime;
        private int nextRoar;
        private int roarTime;
        private int shootTime = 100;
        private int biteTime;
        private float mouthAnimation;
        private float previousMouthAnimation;
        private float fadeAnimation;
        private float previousFadeAnimation;
        private boolean shaking;
        private float shakeAnimation;
        private float previousShakeAnimation;
        private int specialDeathTime;
        private final List<BlockPos> playingJukeboxes = new ArrayList<BlockPos>();
        private LookAtTargetGoal lookGoal;

        public WitherStormHeadEntity(World world) {
            super(world);
            forceSpawn = true;
            ignoreFrustumCheck = true;
            isImmuneToFire = true;
            setSize(5.0F, 5.0F);
            if (getNavigator() instanceof PathNavigateGround) {
                ((PathNavigateGround) getNavigator()).setCanSwim(true);
            }
            nextRoar = WitherStormPartLogic.initialRoarDelay(rand);
            this.lookHelper = new WitherStormHeadLookHelper(this);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(ACTIVE, true);
            dataManager.register(ROARING, false);
            dataManager.register(BITING, false);
            dataManager.register(HURT, false);
        }
        @Override protected double getSickenedHealth() { return 60.0D; }
        @Override protected double getSickenedSpeed() { return 0.0D; }
        // Monster.createMonsterAttributes leaves attack damage at the vanilla 2.0;
        // the head's 3.5 bite is dealt explicitly in customServerAiStep.
        @Override protected double getSickenedDamage() { return 2.0D; }
        @Override protected double getSickenedFollowRange() { return 40.0D; }
        @Override protected double getSickenedArmor() { return 8.0D; }
        @Override protected double getSickenedKnockbackResistance() { return 0.0D; }
        @Override public String getSickenedType() { return "wither_storm_head"; }
        @Override protected double[] getOffset(WitherStormEntity owner, int index) {
            double side = index == 0 ? 0.0D : (index == 1 ? -1.0D : 1.0D) * owner.width * 0.42D;
            return new double[]{side, owner.height * 0.72D, -owner.width * 0.18D};
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new DoNothingGoal(this));
            tasks.addTask(1, new LookAtDistractionGoal(this));
            lookGoal = new LookAtTargetGoal(this);
            tasks.addTask(2, lookGoal);
            tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 12.0F));
            tasks.addTask(4, new YAffectedLookRandomlyGoal(this));
            targetTasks.addTask(0, new EntityAIHurtByTarget(this, true));
            targetTasks.addTask(1, new NearestDistractionGoal(this));
            targetTasks.addTask(2, new AttackTargetGoal(this));
        }

        /** 上游头部使用空的身体旋转控制器，避免空闲注视时被 1.12 的默认逻辑改写身体角度。 */
        @Override
        protected EntityBodyHelper createBodyHelper() {
            return new EntityBodyHelper(this) {
                @Override
                public void updateRenderAngles() {
                }
            };
        }

        @Override
        public void setIndependentBowelsPart() {
            super.setIndependentBowelsPart();
            setNoAI(false);
            setNoGravity(true);
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            WitherStormEntity owner = getOwnerStorm();
            if (owner != null && !isIndependentBowelsPart()) {
                owner.attackHead(getPartIndex(), source.getTrueSource());
                return true;
            }
            if (!isIndependentBowelsPart() || !isActive() || isHurt()) {
                return source == DamageSource.OUT_OF_WORLD && super.attackEntityFrom(source, amount);
            }
            if (source != DamageSource.OUT_OF_WORLD && !isRoaring()) {
                setRoar(true);
                setRoarTime(20);
            }
            boolean damaged = attackPartDirectly(source, amount);
            if (damaged && getHealth() < getMaxHealth() / 1.5F) setHurt(true);
            return damaged;
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            setNoGravity(true);
            previousMouthAnimation = mouthAnimation;
            mouthAnimation = WitherStormPartLogic.advanceMouth(mouthAnimation, isRoaring(), isBiting());
            previousFadeAnimation = fadeAnimation;
            fadeAnimation = WitherStormPartLogic.advanceFade(fadeAnimation, isPlayingDead(), rand);
            previousShakeAnimation = shakeAnimation;
            if (shaking) {
                shakeAnimation = WitherStormPartLogic.advanceShake(shakeAnimation, true, rand);
                if (previousShakeAnimation >= 2.0F) {
                    shakeAnimation = previousShakeAnimation = 0.0F;
                    shaking = false;
                }
            }

            if (!isIndependentBowelsPart() || world.isRemote) return;
            copyPlayingJukeboxesFromOwner();
            if (distractedTime > 0 && --distractedTime == 0) distractedPos = null;
            if (!isDeadOrPlayingDead()) {
                if (--nextRoar == 0) {
                    setRoar(false);
                    setRoarTime(40);
                    nextRoar = WitherStormPartLogic.nextRoarDelay(rand);
                }
                if (roarTime > 0 && --roarTime == 0) disableRoar();
                if (biteTime > 0 && --biteTime == 0) {
                    setBiting(false);
                    playSound(ModSounds.get("wither_storm_bite"), getSoundVolume(), 1.0F);
                }
                if (isHurt() && ticksExisted % 20 == 0 && shootTime > 60) shaking = true;
                if (isHurt() && shootTime > 0) {
                    --shootTime;
                    if (shootTime < 60 && getAttackTarget() != null) {
                        EntityLivingBase target = getAttackTarget();
                        // Upstream hurt-fire aiming intentionally targets Entity.position(),
                        // while the normal look goal targets the victim's eyes.
                        setLookAt(0, target.getPositionVector(), 3);
                        shaking = false;
                    }
                    if (shootTime == 0) {
                        shootSkullAtTarget();
                        shootTime = WitherStormPartLogic.nextShotDelay(rand);
                        shaking = false;
                    }
                }
            }
        }

        private void copyPlayingJukeboxesFromOwner() {
            WitherStormEntity owner = getOwnerStorm();
            if (owner == null) return;
            playingJukeboxes.clear();
            playingJukeboxes.addAll(owner.getPlayingJukeboxes());
        }

        List<BlockPos> getPlayingJukeboxes() { return playingJukeboxes; }

        public boolean isActive() { return dataManager.get(ACTIVE); }
        public void setActive(boolean active) {
            dataManager.set(ACTIVE, active);
        }
        public boolean isRoaring() { return dataManager.get(ROARING); }
        /** Starts a roar; screaming=true selects the hurt roar variant. */
        public void setRoar(boolean screaming) {
            dataManager.set(ROARING, true);
            playSound(ModSounds.get(screaming ? "wither_storm_hurt" : "wither_storm_roar"), getSoundVolume(), 1.0F);
        }
        public int getRoarTime() { return roarTime; }
        public void setRoarTime(int ticks) {
            roarTime = Math.max(0, ticks);
        }
        public void disableRoar() { dataManager.set(ROARING, false); }
        public boolean isBiting() { return dataManager.get(BITING); }
        public void setBiting(boolean biting) {
            if (biting) biteTime = 10;
            dataManager.set(BITING, biting);
        }
        public boolean isPlayingDead() { return !isActive(); }
        public boolean isDeadOrPlayingDead() { return isDead || getHealth() <= 0.0F || isPlayingDead(); }
        public boolean isHurt() { return dataManager.get(HURT); }
        public void setHurt(boolean hurt) {
            dataManager.set(HURT, hurt);
            if (lookGoal == null) return;
            tasks.removeTask(lookGoal);
            if (!hurt) tasks.addTask(2, lookGoal);
        }

        @Override
        public boolean hasNoGravity() {
            return true;
        }

        @Override
        public boolean canBeAttackedWithItem() {
            return !isPlayingDead();
        }

        /** LivingEntity is pickable upstream; StormPartBase disables it for attached parts. */
        @Override
        public boolean canBeCollidedWith() {
            return true;
        }

        @Override
        protected void updateAITasks() {
            super.updateAITasks();
            if (!isIndependentBowelsPart() || !isActive() || isHurt()) return;
            if (ticksExisted % 80 == 0) heal(10.0F);
            EntityLivingBase target = getAttackTarget();
            if (target == null || !canAttackTarget(target)) return;
            Vec3d delta = getPositionVector().subtract(target.getPositionVector()).normalize().scale(0.2D);
            if (delta.lengthSquared() > 0.0D) {
                Entity pulled = target;
                Entity vehicle = target.getRidingEntity();
                if (vehicle != null && WitherStormConfig.shouldPickUpVehicles
                        && canPullVehicle(vehicle)) {
                    pulled = vehicle;
                }
                pulled.motionX = delta.x;
                pulled.motionY = delta.y;
                pulled.motionZ = delta.z;
                pulled.velocityChanged = true;
                target.velocityChanged = true;
                if (target instanceof EntityPlayerMP) {
                    ModNetwork.setPlayerMotion((EntityPlayerMP) target, pulled, delta);
                }
            }
            if (getEntityBoundingBox().intersects(target.getEntityBoundingBox())) {
                if (target instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) target;
                    if (!player.capabilities.disableDamage && player.isEntityAlive()) {
                        player.attackEntityFrom(ModDamageSources.witherStormAttack(this), 3.5F);
                    }
                } else if (target.isEntityAlive()) {
                    target.attackEntityFrom(ModDamageSources.witherStormAttackMob(this), Float.MAX_VALUE);
                }
                setBiting(true);
            }
        }

        private void shootSkullAtTarget() {
            EntityLivingBase target = getAttackTarget();
            if (target == null || !canAttackTarget(target)) return;
            Vec3d direction = target.getPositionVector().subtract(getPositionVector()).normalize();
            EntityWitherSkull skull = new EntityWitherSkull(world, this, direction.x, direction.y, direction.z);
            if (rand.nextInt(16) == 1) skull.setInvulnerable(true);
            world.spawnEntity(skull);
            skull.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 2.0F, 1.0F);
        }

        private boolean canPullVehicle(Entity vehicle) {
            return !(vehicle instanceof EntityLivingBase)
                    || canAttackTarget((EntityLivingBase) vehicle);
        }

        private boolean canAttackTarget(EntityLivingBase target) {
            return target != this && target.isEntityAlive()
                    && WitherStormEntity.isStormTargetType(target)
                    && (!(target instanceof EntityPlayer)
                    || !((EntityPlayer) target).capabilities.disableDamage
                    && !((EntityPlayer) target).isSpectator());
        }

        private boolean isATarget(EntityLivingBase target) {
            double range = getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
            for (WitherStormHeadEntity head : world.getEntitiesWithinAABB(WitherStormHeadEntity.class,
                    getEntityBoundingBox().grow(range))) {
                if (!head.isHurt() && head.getAttackTarget() == target) return true;
            }
            return false;
        }

        public void setDistractedPos(int head, Vec3d pos) { distractedPos = pos; }
        public Vec3d getDistractedPos(int head) { return distractedPos; }
        public void makeDistracted(Vec3d pos, int time, int head) {
            distractedPos = pos;
            distractedTime = Math.max(0, time);
        }

        public float getMouthAnimation(float partialTicks) {
            return previousMouthAnimation + (mouthAnimation - previousMouthAnimation) * partialTicks;
        }

        public float getMouthAnimation(int head, float partialTicks) { return getMouthAnimation(partialTicks); }
        public float getBrokenJawAnimation(int head, float partialTicks) { return 0.0F; }

        public float getFadeAnimation(float partialTicks) {
            return previousFadeAnimation + (fadeAnimation - previousFadeAnimation) * partialTicks;
        }

        public float getFadeAnimation() { return fadeAnimation; }

        @Override
        public int getBrightnessForRender() {
            return WitherStormPartLogic.applyFadeLight(super.getBrightnessForRender(), getFadeAnimation());
        }

        public float getHeadShakeAnimation(float partialTicks) {
            return WitherStormPartLogic.shakeRoll(previousShakeAnimation, shakeAnimation, partialTicks);
        }

        public float getHeadShakeAnim(int head, float partialTicks) { return getHeadShakeAnimation(partialTicks); }
        public float getTentacleAnimation(float partialTicks) { return 0.0F; }
        public float getHeadYRot(int head) { return rotationYawHead; }
        public float getHeadYRotO(int head) { return prevRotationYawHead; }
        public float getHeadXRot(int head) { return rotationPitch; }
        public float getHeadXRotO(int head) { return prevRotationPitch; }
        public float getXBodyRot() { return 0.0F; }
        public float getXBodyRotO() { return 0.0F; }
        public boolean isPosBehindBack(Vec3d pos) { return false; }
        public boolean areOtherHeadsDisabled() { return false; }
        public boolean isHeadInjured(int head) { return isHurt(); }

        public boolean tractorBeamActive(int head) { return isActive() && !isPlayingDead() && !isHurt(); }
        @Override
        public boolean canBeDistracted(int head) { return tractorBeamActive(head); }
        @Override
        public double getTractorBeamCutoffDistance(int head) { return -1.0D; }
        public boolean canSee(int head, Entity entity) { return canEntityBeSeen(entity); }
        public int getTotalHeads() { return 1; }
        public EntityLivingBase getTarget(int head) { return getAttackTarget(); }
        public void setTarget(int head, EntityLivingBase target) { setAttackTarget(target); }
        public Vec3d getHeadPos(int head) { return WorldUtil.centerOf(getEntityBoundingBox()); }
        @Override
        public Vec3d getHeadPositionForBeam(int head) { return getHeadPos(head); }
        @Override
        public Vec3d getHeadPositionForBeam(int head, float partialTicks) {
            return new Vec3d(prevPosX + (posX - prevPosX) * partialTicks,
                    prevPosY + (posY - prevPosY) * partialTicks + height * 0.5D,
                    prevPosZ + (posZ - prevPosZ) * partialTicks);
        }
        @Override
        public Vec3d getHeadDirectionForBeam(int head) {
            return getHeadDirectionForBeam(head, 1.0F);
        }
        @Override
        public Vec3d getHeadDirectionForBeam(int head, float partialTicks) {
            float interpolatedYaw = prevRotationYawHead
                    + MathHelper.wrapDegrees(rotationYawHead - prevRotationYawHead) * partialTicks;
            float interpolatedPitch = prevRotationPitch
                    + (rotationPitch - prevRotationPitch) * partialTicks;
            float pitch = interpolatedPitch * 0.017453292F;
            float yaw = interpolatedYaw * 0.017453292F;
            float horizontal = MathHelper.cos(pitch);
            return new Vec3d(-MathHelper.sin(yaw) * horizontal,
                    -MathHelper.sin(pitch), MathHelper.cos(yaw) * horizontal).normalize();
        }
        public void setLookAt(int head, Vec3d pos, int steps) {
            if (pos != null) {
                // WitherStormHeadEntity delegates to LookControl#setLookAt(Vec3):
                // 10 degrees/tick horizontally and 40 degrees/tick vertically.
                // The API's steps value is deliberately ignored upstream.
                getLookHelper().setLookPosition(pos.x, pos.y, pos.z, 10.0F, 40.0F);
            }
        }

        @Override
        public float getEyeHeight() { return height / 1.5F; }

        @Override
        public Entity changeDimension(int dimensionIn) {
            return null;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return isPlayingDead() ? null : ModSounds.get("wither_storm_ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.get("wither_storm_hurt"); }

        @Override
        protected SoundEvent getDeathSound() { return null; }

        @Override
        protected float getSoundVolume() { return 8.0F; }

        @Override
        public int getTalkInterval() { return 80 + rand.nextInt(40); }

        @Override
        public void onDeath(DamageSource cause) {
            super.onDeath(cause);
            setRoar(true);
        }

        @Override
        protected void onDeathUpdate() {
            ++specialDeathTime;
            if (!world.isRemote) rotationPitch = Math.max(-50.0F, rotationPitch - 1.0F);
            if (specialDeathTime > 120) setDead();
        }

        /** 1.12 equivalent of the upstream ConditionalLookController(resetXRot=false). */
        private static final class WitherStormHeadLookHelper extends EntityLookHelper {
            private static final double ROTATION_EPSILON = 1.0E-5D;
            private static final float IDLE_YAW_SPEED = 10.0F;
            private static final float NAVIGATING_YAW_LIMIT = 75.0F;

            private final WitherStormHeadEntity head;
            private float maximumYawChange;
            private float maximumPitchChange;
            private int lookAtTicks;
            private double targetX;
            private double targetY;
            private double targetZ;

            WitherStormHeadLookHelper(WitherStormHeadEntity head) {
                super(head);
                this.head = head;
            }

            @Override
            public void setLookPositionWithEntity(Entity target, float deltaYaw, float deltaPitch) {
                double y = target instanceof EntityLivingBase
                        ? target.posY + target.getEyeHeight()
                        : (target.getEntityBoundingBox().minY + target.getEntityBoundingBox().maxY) * 0.5D;
                setLookPosition(target.posX, y, target.posZ, deltaYaw, deltaPitch);
            }

            @Override
            public void setLookPosition(double x, double y, double z, float deltaYaw, float deltaPitch) {
                targetX = x;
                targetY = y;
                targetZ = z;
                maximumYawChange = deltaYaw;
                maximumPitchChange = deltaPitch;
                lookAtTicks = 2;
            }

            @Override
            public void onUpdateLook() {
                // Do not reset rotationPitch here. Upstream's predicate is always
                // false so an idle wall head keeps its spawn/death pitch.
                if (lookAtTicks > 0) {
                    --lookAtTicks;
                    double x = targetX - head.posX;
                    double y = targetY - (head.posY + head.getEyeHeight());
                    double z = targetZ - head.posZ;
                    double horizontal = Math.sqrt(x * x + z * z);
                    if (Math.abs(z) > ROTATION_EPSILON || Math.abs(x) > ROTATION_EPSILON) {
                        float yaw = (float) (MathHelper.atan2(z, x) * 57.2957763671875D) - 90.0F;
                        head.rotationYawHead = rotateTowards(
                                head.rotationYawHead, yaw, maximumYawChange);
                    }
                    if (Math.abs(y) > ROTATION_EPSILON || Math.abs(horizontal) > ROTATION_EPSILON) {
                        float pitch = (float) (-(MathHelper.atan2(y, horizontal)
                                * 57.2957763671875D));
                        head.rotationPitch = rotateTowards(
                                head.rotationPitch, pitch, maximumPitchChange);
                    }
                } else {
                    head.rotationYawHead = rotateTowards(
                            head.rotationYawHead, head.renderYawOffset, IDLE_YAW_SPEED);
                }

                // Vanilla 1.20 only applies its 75-degree body-relative clamp
                // while navigation has an active path. This immobile head normally
                // has no path and therefore has a full horizontal turn range.
                if (!head.getNavigator().noPath()) {
                    float relativeYaw = MathHelper.wrapDegrees(
                            head.rotationYawHead - head.renderYawOffset);
                    if (relativeYaw < -NAVIGATING_YAW_LIMIT) {
                        head.rotationYawHead = head.renderYawOffset - NAVIGATING_YAW_LIMIT;
                    } else if (relativeYaw > NAVIGATING_YAW_LIMIT) {
                        head.rotationYawHead = head.renderYawOffset + NAVIGATING_YAW_LIMIT;
                    }
                }
            }

            @Override public boolean getIsLooking() { return lookAtTicks > 0; }
            @Override public double getLookPosX() { return targetX; }
            @Override public double getLookPosY() { return targetY; }
            @Override public double getLookPosZ() { return targetZ; }

            private static float rotateTowards(float current, float target, float maximumChange) {
                float difference = MathHelper.wrapDegrees(target - current);
                difference = MathHelper.clamp(difference, -maximumChange, maximumChange);
                return current + difference;
            }
        }

        private static class DoNothingGoal extends EntityAIBase {
            private final WitherStormHeadEntity head;

            DoNothingGoal(WitherStormHeadEntity head) {
                this.head = head;
                setMutexBits(7);
            }

            @Override
            public boolean shouldExecute() { return head.isPlayingDead(); }
        }

        private static class LookAtDistractionGoal extends EntityAIBase {
            private final WitherStormHeadEntity head;
            private Vec3d targetPosition;

            LookAtDistractionGoal(WitherStormHeadEntity head) {
                this.head = head;
                setMutexBits(2);
            }

            @Override
            public boolean shouldExecute() {
                targetPosition = head.distractedPos;
                return targetPosition != null;
            }

            @Override
            public boolean shouldContinueExecuting() {
                return shouldExecute();
            }

            @Override
            public void updateTask() {
                if (targetPosition != null) head.setLookAt(0, targetPosition, 10);
            }

            @Override
            public void resetTask() {
                targetPosition = null;
                head.setDistractedPos(0, null);
            }
        }

        private static class LookAtTargetGoal extends EntityAIBase {
            private final WitherStormHeadEntity head;
            private EntityLivingBase target;

            LookAtTargetGoal(WitherStormHeadEntity head) {
                this.head = head;
                setMutexBits(2);
            }

            @Override
            public boolean shouldExecute() {
                EntityLivingBase currentTarget = head.getAttackTarget();
                if (currentTarget == null || !currentTarget.isEntityAlive()) return false;
                target = currentTarget;
                return true;
            }

            @Override
            public boolean shouldContinueExecuting() {
                return shouldExecute();
            }

            @Override
            public void updateTask() {
                if (target != null) head.setLookAt(0, target.getPositionEyes(1.0F), 3);
            }

            @Override
            public void resetTask() {
                target = null;
            }
        }

        private static class NearestDistractionGoal extends EntityAIBase {
            private static final int SEARCH_INTERVAL = 8;
            private static final int MAX_UNSEEN_TICKS = 180;
            private final WitherStormHeadEntity head;
            private EntityFireworkRocket target;
            private int unseenTicks;

            NearestDistractionGoal(WitherStormHeadEntity head) {
                this.head = head;
                setMutexBits(1);
            }

            @Override
            public boolean shouldExecute() {
                if (!head.isActive() || head.isHurt() || head.distractedPos != null
                        || head.getRNG().nextInt(SEARCH_INTERVAL) != 0) return false;
                target = findNearestFirework();
                return target != null;
            }

            @Override
            public boolean shouldContinueExecuting() {
                if (head.distractedPos == null) {
                    if (!isTargetUsable()) return false;
                    double followDistance = getFollowDistance();
                    if (head.getDistanceSq(target) > followDistance * followDistance) return false;
                    if (head.canEntityBeSeen(target)) {
                        unseenTicks = 0;
                    } else if (unseenTicks++ > MAX_UNSEEN_TICKS) {
                        return false;
                    }
                    head.makeDistracted(target.getPositionVector(),
                            80 + head.getRNG().nextInt(80), 0);
                    return true;
                }

                if (!isTargetUsable()) {
                    if (head.getRNG().nextInt(SEARCH_INTERVAL) == 0) {
                        Vec3d position = head.distractedPos;
                        if (position != null) {
                            head.setDistractedPos(0, position.add(
                                    head.getRNG().nextGaussian(), head.getRNG().nextGaussian(),
                                    head.getRNG().nextGaussian()));
                        }
                    }
                    EntityFireworkRocket previousTarget = target;
                    target = findNearestFirework();
                    if (target != null && target != previousTarget) {
                        head.makeDistracted(target.getPositionVector(),
                                80 + head.getRNG().nextInt(80), 0);
                    }
                }
                return true;
            }

            @Override
            public void startExecuting() {
                unseenTicks = 0;
                head.setAttackTarget(null);
            }

            @Override
            public void resetTask() {
                head.setDistractedPos(0, null);
                target = null;
                unseenTicks = 0;
            }

            @Nullable
            private EntityFireworkRocket findNearestFirework() {
                double range = getFollowDistance();
                EntityFireworkRocket nearest = null;
                double nearestDistance = Double.MAX_VALUE;
                for (EntityFireworkRocket firework : head.world.getEntitiesWithinAABB(
                        EntityFireworkRocket.class, head.getEntityBoundingBox().grow(range),
                        entity -> !entity.isDead)) {
                    double distance = firework.getPositionVector().squareDistanceTo(head.getHeadPos(0));
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = firework;
                    }
                }
                return nearest;
            }

            private boolean isTargetUsable() {
                return target != null && !target.isDead;
            }

            private double getFollowDistance() {
                return head.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
            }
        }

        /** 1.20 YAffectedLookRandomlyGoal 在 1.12 的等价实现。 */
        private static class YAffectedLookRandomlyGoal extends EntityAIBase {
            private final WitherStormHeadEntity head;
            private int lookTime;
            private double lookX;
            private double lookY;
            private double lookZ;

            YAffectedLookRandomlyGoal(WitherStormHeadEntity head) {
                this.head = head;
                setMutexBits(3);
            }

            @Override
            public boolean shouldExecute() {
                return head.getRNG().nextFloat() < 0.02F;
            }

            @Override
            public boolean shouldContinueExecuting() {
                return lookTime >= 0;
            }

            @Override
            public void startExecuting() {
                float pitch = MathHelper.clamp(-head.getRNG().nextInt(180), -140, -30);
                float yaw = MathHelper.wrapDegrees(head.renderYawOffset) + 90.0F
                        + MathHelper.clamp(head.getRNG().nextInt(360) - 180, -80, 80);
                double pitchRadians = pitch * 0.017453292F;
                double yawRadians = yaw * 0.017453292F;
                lookX = head.posX + Math.cos(yawRadians) * 30.0D;
                lookY = head.posY + head.getEyeHeight() + Math.sin(pitchRadians) * 30.0D;
                lookZ = head.posZ + Math.sin(yawRadians) * 30.0D;
                lookTime = 20 + head.getRNG().nextInt(20);
            }

            @Override
            public void updateTask() {
                --lookTime;
                head.setLookAt(0, new Vec3d(lookX, lookY, lookZ), 3);
            }
        }

        private static class AttackTargetGoal
                extends EntityAINearestAttackableTarget<EntityLivingBase> {
            private final WitherStormHeadEntity head;

            AttackTargetGoal(WitherStormHeadEntity head) {
                super(head, EntityLivingBase.class, 100, true, false,
                        target -> head.canAttackTarget(target) && !head.isATarget(target));
                this.head = head;
            }

            @Override
            protected AxisAlignedBB getTargetableArea(double targetDistance) {
                return head.getEntityBoundingBox().grow(targetDistance);
            }
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setBoolean("IsRoaring", isRoaring());
            compound.setInteger("RoarTime", roarTime);
            compound.setBoolean("IsActive", isActive());
            if (distractedPos != null) compound.setTag("DistractedPos", writeVector(distractedPos));
            compound.setInteger("DistractedTime", distractedTime);
            compound.setFloat("YBodyRot", renderYawOffset);
            compound.setBoolean("IsHurt", isHurt());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            setActive(compound.hasKey("IsActive", 1)
                    ? compound.getBoolean("IsActive")
                    : !compound.hasKey("Active") || compound.getBoolean("Active"));
            dataManager.set(ROARING, compound.hasKey("IsRoaring", 1)
                    ? compound.getBoolean("IsRoaring") : compound.getBoolean("Roaring"));
            roarTime = Math.max(0, compound.getInteger("RoarTime"));
            dataManager.set(BITING, false);
            biteTime = 0;
            nextRoar = WitherStormPartLogic.initialRoarDelay(rand);
            shootTime = 100;
            setHurt(compound.hasKey("IsHurt", 1) ? compound.getBoolean("IsHurt")
                    : compound.hasKey("Hurt") ? compound.getBoolean("Hurt")
                    : compound.getInteger("HurtTime") > 0);
            distractedTime = Math.max(0, compound.getInteger("DistractedTime"));
            playingJukeboxes.clear();
            if (compound.hasKey("DistractedPos", 10)) {
                distractedPos = readVector(compound.getCompoundTag("DistractedPos"));
            } else if (compound.hasKey("DistractedX") && compound.hasKey("DistractedY")
                    && compound.hasKey("DistractedZ")) {
                distractedPos = new Vec3d(compound.getDouble("DistractedX"), compound.getDouble("DistractedY"),
                        compound.getDouble("DistractedZ"));
            } else {
                distractedPos = null;
            }
            mouthAnimation = previousMouthAnimation = 0.0F;
            fadeAnimation = previousFadeAnimation = 0.0F;
            shakeAnimation = previousShakeAnimation = 0.0F;
            float bodyYaw = compound.hasKey("YBodyRot", 5)
                    ? compound.getFloat("YBodyRot") : compound.getFloat("BodyYaw");
            renderYawOffset = prevRenderYawOffset = bodyYaw;
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
    }

    public static class WitherStormSegmentEntity extends StormPartBase
            implements TractorBeamProvider, WitherStormBase, DistantStormPart {
        private static final DataParameter<Boolean> DYING = EntityDataManager.createKey(WitherStormSegmentEntity.class,
                DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> DROPPING = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Integer> HEAD_ANIMATION_FLAGS = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> HEAD_INJURY_FLAGS = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> FIRST_HEAD_TARGET = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> SECOND_HEAD_TARGET = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> THIRD_HEAD_TARGET = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Float> FIRST_HEAD_YAW = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> SECOND_HEAD_YAW = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> THIRD_HEAD_YAW = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> FIRST_HEAD_PITCH = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> SECOND_HEAD_PITCH = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> THIRD_HEAD_PITCH = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> BODY_YAW = EntityDataManager.createKey(
                WitherStormSegmentEntity.class, DataSerializers.FLOAT);
        private final int tillFreeFall;
        private int dropTime;
        private int nextDropTime;
        private int timeWithParent;
        private Vec3d wantedSegmentPos;
        private Vec3d randomStrollPos;
        private int tillNextRandomStroll;
        private float randomBodyRotAngleOffset;
        private int flickerTime;
        private int nextFlicker = 40;
        private int tentacleTickCount;
        private int previousTentacleTickCount;
        private double followVelocityX;
        private double followVelocityY;
        private double followVelocityZ;
        private int deathTicks;
        private double deathFallDistance;
        private boolean deathLandingHandled;
        private boolean shouldFlicker;
        private float fadeAnimation;
        private float previousFadeAnimation;
        private final WitherStormSegmentManager segmentManager =
                new WitherStormSegmentManager(this);
        private final Map<UUID, NBTTagCompound> consumedPets =
                new LinkedHashMap<UUID, NBTTagCompound>();
        private final List<BlockPos> playingJukeboxes = new ArrayList<BlockPos>();

        public WitherStormSegmentEntity(World world) {
            super(world);
            forceSpawn = true;
            setSize(15.0F, WitherStormConfig.squashHitbox ? 1.0F : 17.5F);
            setNoGravity(true);
            experienceValue = 0;
            setStormPhase(6);
            tillFreeFall = WitherStormPartLogic.segmentFreeFallDelay(rand);
            nextDropTime = 120 + rand.nextInt(160);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(DYING, false);
            dataManager.register(DROPPING, false);
            dataManager.register(HEAD_ANIMATION_FLAGS, 0);
            dataManager.register(HEAD_INJURY_FLAGS, 0);
            dataManager.register(FIRST_HEAD_TARGET, 0);
            dataManager.register(SECOND_HEAD_TARGET, 0);
            dataManager.register(THIRD_HEAD_TARGET, 0);
            dataManager.register(FIRST_HEAD_YAW, 0.0F);
            dataManager.register(SECOND_HEAD_YAW, 0.0F);
            dataManager.register(THIRD_HEAD_YAW, 0.0F);
            dataManager.register(FIRST_HEAD_PITCH, 0.0F);
            dataManager.register(SECOND_HEAD_PITCH, 0.0F);
            dataManager.register(THIRD_HEAD_PITCH, 0.0F);
            dataManager.register(BODY_YAW, 0.0F);
        }

        /** 性能优化：与主风暴一致，位移全 0 时跳过巨型 AABB 的方块碰撞枚举。 */
        @Override
        public void move(MoverType type, double x, double y, double z) {
            if (type != MoverType.PISTON && x == 0.0D && y == 0.0D && z == 0.0D) {
                collidedHorizontally = false;
                collidedVertically = false;
                collided = false;
                onGround = false;
                return;
            }
            super.move(type, x, y, z);
        }

        @Override protected double getSickenedHealth() { return 4000.0D; }
        @Override protected double getSickenedSpeed() { return 0.0D; }
        @Override protected double getSickenedDamage() { return 2.0D; }
        @Override protected double getSickenedFollowRange() { return 160.0D; }
        @Override protected double getSickenedArmor() { return 6.0D; }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getAttributeMap().registerAttribute(ModAttributes.TARGET_STATIONARY_FLYING_SPEED)
                    .setBaseValue(0.4D);
            getAttributeMap().registerAttribute(ModAttributes.SLOW_FLYING_SPEED)
                    .setBaseValue(0.05D);
            getAttributeMap().registerAttribute(ModAttributes.EVOLUTION_SPEED)
                    .setBaseValue(1.0D);
            getAttributeMap().registerAttribute(ModAttributes.HUNCHBACK_FOLLOW_RANGE)
                    .setBaseValue(40.0D);
        }
        @Override protected double getSickenedKnockbackResistance() { return 0.6D; }
        @Override public String getSickenedType() { return "wither_storm_segment"; }
        @Override public float getEyeHeight() { return 10.0F; }
        @Override protected float getSoundVolume() { return 25.0F; }
        @Override protected double[] getOffset(WitherStormEntity owner, int index) {
            int segmentIndex = isMirrored() ? 1 : 2;
            return new double[]{owner.getDesiredSegmentX(segmentIndex) - owner.posX,
                    owner.getDesiredSegmentY(segmentIndex) - owner.posY,
                    owner.getDesiredSegmentZ(segmentIndex) - owner.posZ};
        }
        @Override protected float getDamageTransfer() { return 0.5F; }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (isInDeathSequence()) return false;
            if (source.canHarmInCreative()) return attackPartDirectly(source, amount);
            return !WitherStormConfig.witherStormInvulnerability
                    && attackPartDirectly(source, 0.0F);
        }

        @Override
        public AxisAlignedBB getRenderBoundingBox() {
            // 覆盖分裂体头部延伸与仰视剔除（RenderManager 视锥判断用此包围盒）
            return getEntityBoundingBox().grow(150.0D);
        }

        @Override
        public boolean isInRangeToRenderDist(double distance) {
            return WitherStormEntity.isInWitherStormRenderRange(this, distance);
        }

        @Override
        public boolean isImmuneToExplosions() {
            return true;
        }

        @Override
        public Entity changeDimension(int dimensionIn) {
            return null;
        }

        @Override
        public void bindTo(WitherStormEntity owner, int index) {
            bindSegmentTo(owner, index, true);
        }

        void bindToWithoutStateTransition(WitherStormEntity owner, int index) {
            bindSegmentTo(owner, index, false);
        }

        private void bindSegmentTo(WitherStormEntity owner, int index, boolean notifyStateChange) {
            super.bindTo(owner, index, notifyStateChange);
            timeWithParent = 0;
            wantedSegmentPos = null;
            randomStrollPos = null;
            tillNextRandomStroll = 0;
            randomBodyRotAngleOffset = 0.0F;
            followVelocityX = followVelocityY = followVelocityZ = 0.0D;
            noClip = false;
            setNoGravity(true);
            prevRotationPitch = rotationPitch = 0.0F;
            dataManager.set(BODY_YAW, owner.renderYawOffset);
        }

        @Override
        void synchronizeStateFromOwner() {
            WitherStormEntity owner = getOwnerStorm();
            if (owner == null) return;
            synchronizeWithOwner(owner);
            prevRotationPitch = rotationPitch = 0.0F;
        }

        @Override
        protected void onOwnerPlayDeadStateChanged(WitherStormEntity.PlayDeadState previous,
                                                   WitherStormEntity.PlayDeadState current) {
            if (world.isRemote) return;
            boolean previouslyDisabled = previous == WitherStormEntity.PlayDeadState.FALLING
                    || previous == WitherStormEntity.PlayDeadState.PLAYING_DEAD;
            boolean currentlyDisabled = current == WitherStormEntity.PlayDeadState.FALLING
                    || current == WitherStormEntity.PlayDeadState.PLAYING_DEAD;
            if (currentlyDisabled && !previouslyDisabled) segmentManager.releaseTrackedEntities();
            if (current == WitherStormEntity.PlayDeadState.FALLING) {
                segmentManager.onStartFalling();
            } else if (current == WitherStormEntity.PlayDeadState.PLAYING_DEAD) {
                segmentManager.onStartPlayingDead();
            } else {
                segmentManager.onAiRestored();
            }
        }

        @Override
        protected void updateAttachedPosition(WitherStormEntity owner, double x, double y, double z) {
            wantedSegmentPos = new Vec3d(x, y, z);
            updateDropState(owner);
            Vec3d desiredPosition = new Vec3d(x, y, z);
            boolean detachedFromParentMovement = isDetachedFromParentMovement();
            boolean dropping = isDropping();
            if (!world.isRemote) {
                if (!detachedFromParentMovement) updateRandomStroll(owner, desiredPosition);
                if (!detachedFromParentMovement) updateBodyYaw(owner);
            }
            if (detachedFromParentMovement || dropping) {
                noClip = false;
                setNoGravity(false);
                boolean dampVerticalSpeed = detachedFromParentMovement
                        && (getStormPlayDeadState() == WitherStormEntity.PlayDeadState.PLAYING_DEAD
                        || getStormPlayDeadState() == WitherStormEntity.PlayDeadState.FALLING
                        && getStormPlayDeadTicks() <= 200);
                updateGravityMovement(dampVerticalSpeed);
                return;
            }
            noClip = false;
            setNoGravity(true);
            double parentDeltaX = owner.posX - posX;
            double parentDeltaZ = owner.posZ - posZ;
            if (parentDeltaX * parentDeltaX + parentDeltaZ * parentDeltaZ > 40000.0D) {
                setPosition(x, y, z);
                followVelocityX = followVelocityY = followVelocityZ = 0.0D;
                collidedHorizontally = false;
                collidedVertically = false;
                collided = false;
                onGround = false;
                return;
            }
            Vec3d movementTarget = randomStrollPos == null ? desiredPosition : randomStrollPos;
            double deltaX = movementTarget.x - posX;
            double deltaY = movementTarget.y - posY;
            double deltaZ = movementTarget.z - posZ;
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            double verticalSpeed = randomStrollPos == null ? 0.02D : 0.01D;
            followVelocityY *= 0.6D;
            if (posY < movementTarget.y || !owner.isArmored() && posY < movementTarget.y + 5.0D) {
                followVelocityY = deltaY * verticalSpeed;
            }
            if (horizontalDistance > 1.0D) {
                double speed = randomStrollPos == null
                        ? owner.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue()
                        + (owner.shouldSpeedUp()
                        ? owner.getDefaultChasingSpeed() + 0.05D
                        : owner.getDefaultNormalSpeed() + 0.08D) : 0.01D;
                speed = Math.min(speed, distance * 0.01D);
                followVelocityX += deltaX / horizontalDistance * speed - followVelocityX * 0.6D;
                followVelocityZ += deltaZ / horizontalDistance * speed - followVelocityZ * 0.6D;
            } else {
                followVelocityX *= 0.4D;
                followVelocityZ *= 0.4D;
            }
            move(MoverType.SELF, followVelocityX, followVelocityY, followVelocityZ);
        }

        private boolean isDetachedFromParentMovement() {
            return isStormPlayDeadAiDisabled();
        }

        @Override
        protected boolean shouldDiscardWhenOwnerMissing() {
            return false;
        }

        private void updateGravityMovement(boolean dampVerticalSpeed) {
            if (onGround) {
                motionX = motionY = motionZ = 0.0D;
                return;
            }
            if (dampVerticalSpeed) motionY *= 0.6D;
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= 0.91D;
            motionY = (motionY - 0.08D) * 0.98D;
            motionZ *= 0.91D;
        }

        @Override
        protected boolean shouldResetAttachedMotion() {
            return !isDetachedFromParentMovement() && !isDropping();
        }

        private void updateRandomStroll(WitherStormEntity owner, Vec3d desiredPosition) {
            double horizontalSpeed = Math.sqrt(owner.motionX * owner.motionX + owner.motionZ * owner.motionZ);
            boolean canStroll = !isDropping() && horizontalSpeed < 0.3D
                    && getPositionVector().distanceTo(desiredPosition) < 50.0D;
            if (!canStroll) {
                tillNextRandomStroll = 0;
                randomStrollPos = null;
                randomBodyRotAngleOffset = 0.0F;
                return;
            }
            if (tillNextRandomStroll == 0) {
                randomStrollPos = desiredPosition.add(rand.nextDouble() * 20.0D - 10.0D,
                        rand.nextDouble() * 40.0D - 20.0D, rand.nextDouble() * 20.0D - 10.0D);
                tillNextRandomStroll = 200 + rand.nextInt(100);
                randomBodyRotAngleOffset = (rand.nextFloat() * 20.0F + 20.0F)
                        * (isMirrored() ? -1.0F : 1.0F);
            }
            if (tillNextRandomStroll > 0) --tillNextRandomStroll;
            if (randomStrollPos != null && getPositionVector().distanceTo(randomStrollPos) < 5.0D) {
                tillNextRandomStroll = 0;
            }
        }

        private void updateBodyYaw(WitherStormEntity owner) {
            Vec3d ultimateTargetPos = getUltimateTargetPos();
            EntityLivingBase ultimateTarget = getUltimateTarget();
            if (ultimateTargetPos == null || !shouldRotateTowardsUltimateTarget()
                    || segmentManager.isUltimateTargetInUseForBodyRotation(ultimateTarget)) return;
            double deltaX = ultimateTargetPos.x - posX;
            double deltaZ = ultimateTargetPos.z - posZ;
            if (deltaX * deltaX + deltaZ * deltaZ <= 0.0001D) return;
            float wantedYaw = (float) (MathHelper.atan2(deltaZ, deltaX)
                    * 180.0D / Math.PI) - 90.0F;
            wantedYaw = MathHelper.wrapDegrees(wantedYaw + randomBodyRotAngleOffset);
            float currentYaw = dataManager.get(BODY_YAW);
            if (timeWithParent < 2) {
                currentYaw = wantedYaw;
            } else {
                float rotationSpeed = (float) WitherStormConfig.rotationSpeed;
                float delta = MathHelper.clamp(MathHelper.wrapDegrees(wantedYaw - currentYaw),
                        -rotationSpeed, rotationSpeed);
                currentYaw = MathHelper.wrapDegrees(currentYaw + delta);
            }
            dataManager.set(BODY_YAW, currentYaw);
        }

        private void updateDropState(WitherStormEntity owner) {
            if (world.isRemote) return;
            SupplementalEntities.CommandBlockEntity commandBlock = owner.getBowelsCommandBlock();
            if (commandBlock != null && commandBlock.getHealth() < commandBlock.getMaxHealth()) {
                if (nextDropTime > 0) --nextDropTime;
                if (nextDropTime == 0) {
                    dropTime = WitherStormPartLogic.segmentDropDuration(rand);
                    float ratio = commandBlock.getHealth() / Math.max(1.0F, commandBlock.getMaxHealth());
                    nextDropTime = WitherStormPartLogic.segmentDropCooldown(rand, ratio);
                }
            }
            if (dropTime > 0) --dropTime;
            boolean dropping = dropTime > 0;
            if (dataManager.get(DROPPING) != dropping) dataManager.set(DROPPING, dropping);
        }

        public int getTimeWithParent() { return timeWithParent; }
        public int getDropTime() {
            if (!world.isRemote) return dropTime;
            return isDropping() ? 1 : 0;
        }
        public int getTimeTillFreeFall() { return tillFreeFall; }
        public Vec3d getWantedSegmentPos() { return wantedSegmentPos; }

        private boolean isDropping() {
            return world.isRemote ? dataManager.get(DROPPING) : dropTime > 0;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            previousTentacleTickCount = tentacleTickCount;
            if (!isDeadOrPlayingDead()) ++tentacleTickCount;
            previousFadeAnimation = fadeAnimation;
            fadeAnimation = WitherStormPartLogic.advanceFade(fadeAnimation,
                    onGround && isDeadOrPlayingDead(), rand);
        }

        @Override
        public void onLivingUpdate() {
            tickFlicker();
            if (isInDeathSequence()) {
                applySegmentBodyYaw();
                segmentManager.tick();
                return;
            }
            WitherStormEntity owner = getOwnerStorm();
            super.onLivingUpdate();
            if (owner != null) {
                ++timeWithParent;
                copyPlayingJukeboxesFromOwner();
            }
            if (!world.isRemote) updateFlickerTrigger();
            applySegmentBodyYaw();
            segmentManager.tick();
        }

        private void updateFlickerTrigger() {
            CommandBlockEntity commandBlock = getBowelsCommandBlock();
            if (commandBlock == null || commandBlock.getHealth() >= commandBlock.getMaxHealth()
                    || nextFlicker <= 0) return;
            --nextFlicker;
            if (nextFlicker > 0) return;
            flickerTime = 60;
            world.setEntityState(this, (byte) 11);
            float healthRatio = commandBlock.getHealth() / Math.max(1.0F, commandBlock.getMaxHealth());
            nextFlicker = (int) (rand.nextInt(40) + 60.0F * Math.max(0.2F, healthRatio));
        }

        private void tickFlicker() {
            if (flickerTime <= 0) return;
            --flickerTime;
            shouldFlicker = MathHelper.cos(flickerTime + rand.nextInt(20))
                    * MathHelper.sin(flickerTime + 30 + rand.nextInt(20)) < -0.5F;
            if (flickerTime == 0) shouldFlicker = false;
        }

        @Override
        public void handleStatusUpdate(byte id) {
            if (id == 11) {
                flickerTime = 60;
            } else {
                super.handleStatusUpdate(id);
            }
        }

        public boolean shouldFlicker() {
            return shouldFlicker;
        }

        private void applySegmentBodyYaw() {
            float bodyYaw = dataManager.get(BODY_YAW);
            renderYawOffset = bodyYaw;
            rotationYaw = bodyYaw;
            rotationYawHead = bodyYaw;
        }

        private void copyPlayingJukeboxesFromOwner() {
            WitherStormEntity owner = getOwnerStorm();
            if (owner == null) return;
            playingJukeboxes.clear();
            playingJukeboxes.addAll(owner.getPlayingJukeboxes());
        }

        public void beginDeathSequence() {
            if (world.isRemote || isInDeathSequence() || isDead) return;
            if (getHealth() > 0.0F) {
                attackPartDirectly(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
            } else {
                beginDeathSequenceInternal();
            }
        }

        void beginDeathSequenceFromParent() {
            beginDeathSequence();
        }

        private void beginDeathSequenceInternal() {
            if (isInDeathSequence() || isDead) return;
            dataManager.set(DYING, true);
            deathTicks = 0;
            deathFallDistance = Math.max(0.0D, fallDistance);
            deathLandingHandled = false;
            prevRotationPitch = rotationPitch = 0.0F;
            segmentManager.onDeath();
            segmentManager.releaseTrackedEntities();
            setNoAI(true);
            setNoGravity(false);
            noClip = false;
            motionX = motionY = motionZ = 0.0D;
        }

        @Override
        public void onDeath(DamageSource cause) {
            super.onDeath(cause);
            if (!world.isRemote && !isInDeathSequence()) {
                releaseConsumedPets();
                WitherStormEntity.cureSickenedInArea(world, segmentManager.getSearchBox());
                beginDeathSequence();
            }
        }

        @Override
        protected void onDeathUpdate() {
            if (!isInDeathSequence()) {
                super.onDeathUpdate();
                return;
            }
            tickDeathSequence();
        }

        @Override
        public void setDead() {
            if (!world.isRemote) segmentManager.discardTrackedEntities();
            super.setDead();
        }

        public boolean isInDeathSequence() { return dataManager.get(DYING); }
        public int getDeathTime() { return deathTicks; }

        public int getInvulnerableTicks() {
            WitherStormEntity owner = getOwnerStorm();
            return owner == null ? 0 : owner.getInvulnerableTicks();
        }

        public int getStartingInvulnerableTicks() {
            WitherStormEntity owner = getOwnerStorm();
            return owner == null ? 0 : owner.getStartingInvulnerableTicks();
        }

        private void tickDeathSequence() {
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            ++deathTicks;
            boolean wasOnGround = onGround;
            double previousY = posY;
            if (!onGround && deathTicks < tillFreeFall) motionY *= 0.6D;
            if (!onGround) {
                move(MoverType.SELF, motionX, motionY, motionZ);
                double fallen = Math.max(0.0D, previousY - posY);
                deathFallDistance += fallen;
                motionX *= 0.91D;
                motionY = (motionY - 0.08D) * 0.98D;
                motionZ *= 0.91D;
            } else {
                motionX = motionY = motionZ = 0.0D;
            }
            if (!world.isRemote) {
                if (getPhase() > 5 && deathTicks < 240
                        && ForgeEventFactory.getMobGriefingEvent(world, this)) {
                    dropDeathClusters();
                }
                if (!deathLandingHandled && !wasOnGround && onGround && deathFallDistance > 18.0D) {
                    deathLandingHandled = true;
                    onBigFall();
                }
            } else if (!onGround) {
                world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                        posX + (rand.nextFloat() - 0.5F) * (width + 5.0F),
                        posY + (rand.nextFloat() - 0.5F) * (height + 5.0F),
                        posZ + (rand.nextFloat() - 0.5F) * (width + 5.0F),
                        -5.0D, 0.0D, 0.0D);
            }
            if (!world.isRemote && deathTicks >= 360) {
                setDead();
            }
        }

        private void dropDeathClusters() {
            if (deathTicks > 10 && deathTicks % 10 == 0) dropSmallMassCluster(1);
        }

        protected void onBigFall() {
            world.playSound(null, getPosition(), ModSounds.get("wither_storm_thump"),
                    SoundCategory.HOSTILE, getSoundVolume() + 3.0F, 1.0F);
            ModNetwork.shakeTracking(this, 30.0F, 12.0F);
            for (int i = 0; i < 6; i++) {
                world.newExplosion(this, posX, posY - i, posZ, 16.0F, false,
                        ForgeEventFactory.getMobGriefingEvent(world, this));
            }
        }

        protected void dropSmallMassCluster(int radius) {
            BlockClusterEntity cluster = MassClusterBuilder.buildSmallDeathCluster(world, rand, radius);
            if (cluster.getBlocks().isEmpty()) return;
            cluster.setPosition(posX + rand.nextGaussian() * 5.0D,
                    posY + getUnmodifiedHeight() * 0.5D + rand.nextGaussian() * 5.0D,
                    posZ + rand.nextGaussian() * 5.0D);
            cluster.setSink(-1);
            cluster.motionX = rand.nextGaussian() * 0.4D;
            cluster.motionY = rand.nextGaussian() * 0.3D;
            cluster.motionZ = rand.nextGaussian() * 0.4D;
            cluster.setRotationDelta(rand.nextInt(90) * 0.15F, rand.nextInt(90) * 0.15F);
            world.spawnEntity(cluster);
        }

        public float getUnmodifiedHeight() {
            return 17.5F;
        }

        void captureConsumedPet(EntityLivingBase living) {
            WitherStormEntity owner = getOwnerStorm();
            if (owner != null) owner.captureConsumedPet(living);
            else ConsumedPetStorage.capture(consumedPets, living);
        }

        private void releaseConsumedPets() {
            BlockPos currentPosition = getPosition();
            BlockPos surface = new BlockPos(currentPosition.getX(),
                    WorldUtil.getMotionBlockingHeightIgnoringLeaves(world,
                            currentPosition.getX(), currentPosition.getZ()),
                    currentPosition.getZ());
            ConsumedPetStorage.release(world, consumedPets,
                    new Vec3d(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D));
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            UUID parent = getOwnerUuid();
            if (parent != null) compound.setUniqueId("Parent", parent);
            compound.setInteger("TimeWithParent", timeWithParent);
            compound.setInteger("DropTime", dropTime);
            compound.setInteger("NextDropTime", nextDropTime);
            compound.setInteger("RandomStrollTime", tillNextRandomStroll);
            compound.setFloat("RandomBodyRotationOffset", randomBodyRotAngleOffset);
            compound.setDouble("FollowVelocityX", followVelocityX);
            compound.setDouble("FollowVelocityY", followVelocityY);
            compound.setDouble("FollowVelocityZ", followVelocityZ);
            compound.setFloat("SegmentBodyYaw", dataManager.get(BODY_YAW));
            if (randomStrollPos != null) {
                compound.setDouble("RandomStrollX", randomStrollPos.x);
                compound.setDouble("RandomStrollY", randomStrollPos.y);
                compound.setDouble("RandomStrollZ", randomStrollPos.z);
            }
            compound.setBoolean("DeathSequence", isInDeathSequence());
            compound.setInteger("DeathTicks", deathTicks);
            compound.setDouble("DeathFallDistance", deathFallDistance);
            compound.setBoolean("DeathLandingHandled", deathLandingHandled);
            ConsumedPetStorage.write(compound, "ConsumedPets", consumedPets);
            NBTTagList jukeboxes = new NBTTagList();
            for (BlockPos position : playingJukeboxes) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setTag("Pos", NBTUtil.createPosTag(position));
                jukeboxes.appendTag(entry);
            }
            compound.setTag("PlayingJukeboxes", jukeboxes);
            segmentManager.writeToNBT(compound);
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            prevRotationPitch = rotationPitch = 0.0F;
            if (compound.hasUniqueId("Parent") && getOwnerUuid() == null) setOwnerUuid(compound.getUniqueId("Parent"));
            timeWithParent = Math.max(0, compound.getInteger("TimeWithParent"));
            dropTime = Math.max(0, compound.getInteger("DropTime"));
            dataManager.set(DROPPING, dropTime > 0);
            if (compound.hasKey("NextDropTime")) {
                nextDropTime = Math.max(0, compound.getInteger("NextDropTime"));
            }
            tillNextRandomStroll = Math.max(0, compound.getInteger("RandomStrollTime"));
            randomBodyRotAngleOffset = compound.getFloat("RandomBodyRotationOffset");
            followVelocityX = compound.getDouble("FollowVelocityX");
            followVelocityY = compound.getDouble("FollowVelocityY");
            followVelocityZ = compound.getDouble("FollowVelocityZ");
            if (compound.hasKey("SegmentBodyYaw")) dataManager.set(BODY_YAW, compound.getFloat("SegmentBodyYaw"));
            if (compound.hasKey("RandomStrollX") && compound.hasKey("RandomStrollY")
                    && compound.hasKey("RandomStrollZ")) {
                randomStrollPos = new Vec3d(compound.getDouble("RandomStrollX"),
                        compound.getDouble("RandomStrollY"), compound.getDouble("RandomStrollZ"));
            }
            dataManager.set(DYING, compound.getBoolean("DeathSequence"));
            deathTicks = Math.max(0, compound.getInteger("DeathTicks"));
            deathFallDistance = Math.max(0.0D, compound.getDouble("DeathFallDistance"));
            deathLandingHandled = compound.getBoolean("DeathLandingHandled");
            setStormPhase(Math.max(6, getStormPhase()));
            ConsumedPetStorage.read(compound, "ConsumedPets", consumedPets);
            playingJukeboxes.clear();
            NBTTagList jukeboxes = compound.getTagList("PlayingJukeboxes", 10);
            for (int index = 0; index < jukeboxes.tagCount(); index++) {
                NBTTagCompound entry = jukeboxes.getCompoundTagAt(index);
                if (entry.hasKey("Pos", 10)) {
                    playingJukeboxes.add(NBTUtil.getPosFromTag(entry.getCompoundTag("Pos")));
                } else if (entry.hasKey("Pos", 4)) {
                    playingJukeboxes.add(BlockPos.fromLong(entry.getLong("Pos")));
                }
            }
            segmentManager.readFromNBT(compound);
            segmentManager.restorePlayDeadPose(getStormPlayDeadState());
            if (isInDeathSequence()) {
                setHealth(0.0F);
                setNoAI(true);
                setNoGravity(false);
                noClip = false;
            } else if (isDetachedFromParentMovement()) {
                setNoGravity(false);
                noClip = false;
            } else {
                setNoGravity(true);
                noClip = false;
            }
        }

        @Nullable
        EntityLivingBase getSegmentTarget(int head) { return segmentManager.getTarget(head); }
        boolean isTargeting(Entity entity) {
            if (entity == null) return false;
            for (int head = 0; head < 3; head++) {
                if (segmentManager.getTarget(head) == entity) return true;
            }
            return false;
        }
        public Vec3d getSegmentHeadPosition(int head) { return segmentManager.getHeadPosition(head, 1.0F); }
        @Override
        public Vec3d getHeadPos(int head) { return getSegmentHeadPosition(head); }
        @Override
        public Vec3d getHeadPositionForBeam(int head) { return getSegmentHeadPosition(head); }
        @Override
        public Vec3d getHeadPositionForBeam(int head, float partialTicks) {
            return segmentManager.getHeadPosition(head, partialTicks);
        }
        @Override
        public Vec3d getHeadDirectionForBeam(int head) { return segmentManager.getLookVector(head); }
        @Override
        public Vec3d getHeadDirectionForBeam(int head, float partialTicks) {
            return segmentManager.getLookVector(head, partialTicks);
        }
        @Override
        public double getTractorBeamCutoffDistance(int head) {
            return segmentManager.getTractorBeamCutoffDistance(head);
        }
        @Override
        public double getTractorBeamCutoffDistance(int head, float partialTicks) {
            if (!world.isRemote) return getTractorBeamCutoffDistance(head);
            return TractorBeamHelper.findCutoffDistance(world,
                    getHeadPositionForBeam(head, partialTicks),
                    getHeadDirectionForBeam(head, partialTicks), 250.0D);
        }
        List<BlockPos> getPlayingJukeboxes() { return playingJukeboxes; }
        boolean isInsideSegmentTractorBeam(Entity entity, int excludedHead) {
            return segmentManager.isInsideOwnTractorBeam(entity, excludedHead);
        }
        public boolean isInsideTractorBeam(Entity entity, double radius) {
            return segmentManager.findContainingTractorBeamHead(entity, radius) >= 0;
        }
        public boolean isEntityNearby(Entity entity) {
            return entity != null && segmentManager.getSearchBox().contains(entity.getPositionVector());
        }
        public void ignoreTractorBeamTarget(Entity entity) {
            segmentManager.ignoreTarget(entity);
        }
        WitherStormSegmentManager getSegmentManager() { return segmentManager; }
        boolean isTrackingForConsumption(Entity entity) { return segmentManager.isTracking(entity); }

        public boolean isMirrored() { return getPartIndex() == 1; }
        public int getTotalHeads() { return 3; }
        boolean isPositionBehindBack(Vec3d position) {
            if (position == null) return false;
            float angle = (float) (MathHelper.atan2(position.x - posX, position.z - posZ)
                    * 180.0D / Math.PI);
            float difference = MathHelper.wrapDegrees(-getSegmentBodyYaw() - angle + 180.0F);
            return difference > 80.0F || difference < -80.0F;
        }
        public boolean isEntityBehindBack(Entity entity) {
            return entity != null && isPositionBehindBack(entity.getPositionVector());
        }
        public boolean areOtherHeadsDisabled() {
            WitherStormEntity owner = getOwnerStorm();
            return owner != null ? owner.areOtherHeadsDisabled() : areStormOtherHeadsDisabled();
        }

        public int getPhase() { return getStormPhase(); }
        @Nullable
        public WitherStormEntity getParentStorm() { return getOwnerStorm(); }
        public boolean isPlayingDead() { return isStormPlayDeadAiDisabled() || isInDeathSequence(); }
        public boolean isDeadOrPlayingDead() {
            return isDead || getHealth() <= 0.0F || isPlayingDead();
        }
        @Nullable
        public EntityLivingBase getUltimateTarget() {
            WitherStormEntity owner = getOwnerStorm();
            return owner == null ? null : owner.getUltimateTarget();
        }
        @Nullable
        public Vec3d getUltimateTargetPos() {
            WitherStormEntity owner = getOwnerStorm();
            return owner == null ? null : owner.getUltimateTargetPos();
        }
        @Nullable
        public CommandBlockEntity getBowelsCommandBlock() {
            WitherStormEntity owner = getOwnerStorm();
            return owner == null ? null : owner.getBowelsCommandBlock();
        }
        public boolean isBeingTornApart() {
            WitherStormEntity owner = getOwnerStorm();
            return owner != null && owner.isBeingTornApart();
        }
        public boolean shouldRotateTowardsUltimateTarget() {
            WitherStormEntity owner = getOwnerStorm();
            return owner != null && owner.shouldRotateTowardsUltimateTarget();
        }
        public boolean tractorBeamActive(int head) { return segmentManager.isTractorBeamActive(head); }
        public boolean isHeadInjured(int head) { return segmentManager.isHeadInjured(head); }
        public boolean canBeDistracted(int head) { return getPhase() > 3 && tractorBeamActive(head); }
        public boolean canSee(int head, Entity entity) { return segmentManager.canSee(head, entity); }
        public void makeDistracted(Vec3d position, int ticks, int head) {
            segmentManager.makeDistracted(head, position, ticks);
        }
        public boolean attackHead(int head, @Nullable Entity attacker) {
            return segmentManager.attemptAttack(head, attacker, 20);
        }
        public boolean attackHeadFromExplosion(int head, @Nullable Entity attacker) {
            return segmentManager.attackFromExplosion(head, attacker);
        }
        public void hurtHeadDirectly(int head, @Nullable Entity attacker) {
            segmentManager.hurtHeadDirectly(head, attacker);
        }
        public void releaseTrackedEntities() {
            segmentManager.releaseTrackedEntities();
        }
        public AxisAlignedBB getHeadBounds(int head) { return segmentManager.getHeadBounds(head); }
        public boolean canPlayerReachHead(EntityPlayer player, int head, double reach) {
            if (player == null || player.world != world || player.isDead || player.isSpectator()
                    || head < 0 || head >= getTotalHeads() || reach <= 0.0D) return false;
            Vec3d eye = player.getPositionEyes(1.0F);
            Vec3d end = eye.add(player.getLook(1.0F).scale(reach));
            AxisAlignedBB bounds = getHeadBounds(head);
            return bounds.contains(eye) || bounds.calculateIntercept(eye, end) != null;
        }
        public float getHeadYaw(int head, float partialTicks) { return segmentManager.getYaw(head, partialTicks); }
        public float getHeadPitch(int head, float partialTicks) { return segmentManager.getPitch(head, partialTicks); }
        @Override
        public float getHeadYRot(int head) { return getHeadYaw(head, 1.0F); }
        @Override
        public float getHeadYRotO(int head) { return getHeadYaw(head, 0.0F); }
        @Override
        public float getHeadXRot(int head) { return getHeadPitch(head, 1.0F); }
        @Override
        public float getHeadXRotO(int head) { return getHeadPitch(head, 0.0F); }
        @Override
        public float getHeadShakeAnim(int head, float partialTicks) {
            return getHeadShakeAnimation(head, partialTicks);
        }
        public float getMouthAnimation(int head, float partialTicks) {
            return segmentManager.getMouthAnimation(head, partialTicks);
        }
        public float getBrokenJawAnimation(int head, float partialTicks) {
            return segmentManager.getBrokenJawAnimation(head, partialTicks);
        }
        public float getHeadShakeAnimation(int head, float partialTicks) {
            return segmentManager.getHeadShakeAnimation(head, partialTicks);
        }
        public int getHeadHurtDuration(int head) { return segmentManager.getHeadHurtDuration(head); }
        public void handleHeadAttackedOnClient(int head) {
            segmentManager.handleHeadAttackedOnClient(head);
        }
        public float getSegmentBodyYaw() { return dataManager.get(BODY_YAW); }
        @Override
        public float getXBodyRot() { return getBodyXRotation(1.0F); }
        @Override
        public float getXBodyRotO() { return getBodyXRotation(0.0F); }
        public float getBodyXRotation(float partialTicks) {
            return prevRotationPitch + (rotationPitch - prevRotationPitch) * partialTicks;
        }
        public float getTentacleAnimation(float partialTicks) {
            return previousTentacleTickCount + (tentacleTickCount - previousTentacleTickCount) * partialTicks;
        }
        public float getFadeAnimation(float partialTicks) {
            return previousFadeAnimation + (fadeAnimation - previousFadeAnimation) * partialTicks;
        }
        public float getFadeAnimation() { return fadeAnimation; }

        @Override
        public boolean isDistracted(int head) {
            return segmentManager.isDistracted(head);
        }

        @Override
        @Nullable
        public Vec3d getDistractedPos(int head) {
            return segmentManager.getDistractedPos(head);
        }

        @Override
        public void setDistractedPos(int head, @Nullable Vec3d position) {
            segmentManager.setDistractedPos(head, position);
        }

        @Override
        @Nullable
        public EntityLivingBase getTarget(int head) {
            return segmentManager.getTarget(head);
        }

        @Override
        public void setTarget(int head, @Nullable EntityLivingBase target) {
            segmentManager.setTarget(head, target);
        }

        @Override
        public void setLookAt(int head, @Nullable Vec3d position, int steps) {
            if (position != null) {
                segmentManager.makeDistracted(head, position, Math.max(1, steps));
            }
        }

        @Override
        public boolean isPosBehindBack(Vec3d position) {
            return isPositionBehindBack(position);
        }

        @Override
        public int getBrightnessForRender() {
            return WitherStormPartLogic.applyFadeLight(super.getBrightnessForRender(), getFadeAnimation());
        }

        int getHeadAnimationFlags() { return dataManager.get(HEAD_ANIMATION_FLAGS); }
        boolean isHeadFlagSet(int bit) { return (getHeadAnimationFlags() & bit) != 0; }
        void setHeadFlag(int bit, boolean enabled) {
            int flags = getHeadAnimationFlags();
            dataManager.set(HEAD_ANIMATION_FLAGS, enabled ? flags | bit : flags & ~bit);
        }
        boolean isHeadInjuryFlagSet(int head) {
            return (dataManager.get(HEAD_INJURY_FLAGS) & 1 << MathHelper.clamp(head, 0, 2)) != 0;
        }
        void setHeadInjuryFlag(int head, boolean injured) {
            int bit = 1 << MathHelper.clamp(head, 0, 2);
            int flags = dataManager.get(HEAD_INJURY_FLAGS);
            dataManager.set(HEAD_INJURY_FLAGS, injured ? flags | bit : flags & ~bit);
        }
        int getWatchedTargetId(int head) { return dataManager.get(getHeadTargetParameter(head)); }
        void updateWatchedTargetId(int head, int targetId) {
            dataManager.set(getHeadTargetParameter(head), Math.max(0, targetId));
        }
        void updateHeadRotation(int head, float yaw, float pitch) {
            dataManager.set(getHeadYawParameter(head), yaw);
            dataManager.set(getHeadPitchParameter(head), pitch);
        }
        float getSyncedHeadYaw(int head) { return dataManager.get(getHeadYawParameter(head)); }
        float getSyncedHeadPitch(int head) { return dataManager.get(getHeadPitchParameter(head)); }

        private static DataParameter<Integer> getHeadTargetParameter(int head) {
            switch (MathHelper.clamp(head, 0, 2)) {
                case 1: return SECOND_HEAD_TARGET;
                case 2: return THIRD_HEAD_TARGET;
                default: return FIRST_HEAD_TARGET;
            }
        }

        private static DataParameter<Float> getHeadYawParameter(int head) {
            switch (MathHelper.clamp(head, 0, 2)) {
                case 1: return SECOND_HEAD_YAW;
                case 2: return THIRD_HEAD_YAW;
                default: return FIRST_HEAD_YAW;
            }
        }

        private static DataParameter<Float> getHeadPitchParameter(int head) {
            switch (MathHelper.clamp(head, 0, 2)) {
                case 1: return SECOND_HEAD_PITCH;
                case 2: return THIRD_HEAD_PITCH;
                default: return FIRST_HEAD_PITCH;
            }
        }
    }
}
