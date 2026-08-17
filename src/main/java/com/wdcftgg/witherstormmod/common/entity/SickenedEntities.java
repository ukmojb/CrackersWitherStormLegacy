package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.api.common.ai.symbiont.SpellType;
import com.wdcftgg.witherstormmod.api.common.ai.symbiont.SymbiontSpell;
import com.wdcftgg.witherstormmod.api.common.registry.WitherStormModRegistries;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.taint.TaintingManager;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIBreakDoor;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIFleeSun;
import net.minecraft.entity.ai.EntityAIFollow;
import net.minecraft.entity.ai.EntityAIRestrictSun;
import net.minecraft.entity.ai.EntityFlyHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAIOcelotAttack;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIAttackRangedBow;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.Enchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.init.SoundEvents;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EntitySelectors;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.DifficultyInstance;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import thedarkcolour.futuremc.entity.trident.Trident;
import thedarkcolour.futuremc.registry.FSounds;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class SickenedEntities {

    private static final ResourceLocation FUTURE_MC_TRIDENT = new ResourceLocation("futuremc", "trident");

    private SickenedEntities() {
    }

    private static void consumeContainerAndGive(EntityPlayer player, EnumHand hand,
                                                ItemStack held, ItemStack filled) {
        held.shrink(1);
        if (held.isEmpty()) {
            player.setHeldItem(hand, filled);
        } else if (!player.inventory.addItemStackToInventory(filled)) {
            player.dropItem(filled, false);
        }
    }

    private abstract static class FlyingSickenedMob extends SickenedMobEntity {
        FlyingSickenedMob(World world) {
            super(world);
            moveHelper = new EntityFlyHelper(this);
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getAttributeMap().registerAttribute(SharedMonsterAttributes.FLYING_SPEED);
            getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).setBaseValue(getFlyingSpeed());
        }

        protected double getFlyingSpeed() { return 0.4D; }

        @Override
        protected PathNavigate createNavigator(World world) {
            PathNavigateFlying navigator = new PathNavigateFlying(this, world);
            navigator.setCanOpenDoors(false);
            navigator.setCanFloat(true);
            navigator.setCanEnterDoors(true);
            return navigator;
        }

        @Override
        public void onLivingUpdate() {
            setNoGravity(true);
            super.onLivingUpdate();
            setNoGravity(true);
        }

        @Override public void fall(float distance, float damageMultiplier) { }
        @Override protected void updateFallState(double y, boolean onGround, IBlockState state, BlockPos pos) { }
    }

    private abstract static class FlyingSickenedTameableMob extends SickenedTameableMob {
        FlyingSickenedTameableMob(World world) {
            super(world);
            moveHelper = new EntityFlyHelper(this);
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getAttributeMap().registerAttribute(SharedMonsterAttributes.FLYING_SPEED);
            getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).setBaseValue(getFlyingSpeed());
        }

        protected double getFlyingSpeed() { return 0.4D; }

        @Override
        protected PathNavigate createNavigator(World world) {
            PathNavigateFlying navigator = new PathNavigateFlying(this, world);
            navigator.setCanOpenDoors(false);
            navigator.setCanFloat(true);
            navigator.setCanEnterDoors(true);
            return navigator;
        }

        @Override
        public void onLivingUpdate() {
            setNoGravity(true);
            super.onLivingUpdate();
            setNoGravity(true);
        }

        @Override public void fall(float distance, float damageMultiplier) { }
        @Override protected void updateFallState(double y, boolean onGround, IBlockState state, BlockPos pos) { }
    }

    public static class SickenedBeeEntity extends FlyingSickenedMob {
        private static final DataParameter<Byte> BEE_FLAGS = EntityDataManager.createKey(
                SickenedBeeEntity.class, DataSerializers.BYTE);
        private static final DataParameter<Integer> ANGER = EntityDataManager.createKey(
                SickenedBeeEntity.class, DataSerializers.VARINT);
        @Nullable private BlockPos savedFlowerPosition;
        @Nullable private BlockPos hivePosition;
        @Nullable private UUID targetPlayer;
        private int underWaterTicks;
        private int ticksSinceSting;
        private int ticksSincePollination;
        private int cannotEnterHiveTicks;
        private int cropsGrownSincePollination;
        private float bodyPitch;
        private float previousBodyPitch;

        public SickenedBeeEntity(World world) { super(world); setSize(0.7F, 0.6F); }
        @Override public String getSickenedType() { return "sickened_bee"; }
        @Override protected double getSickenedHealth() { return 15.0D; }
        @Override protected double getSickenedSpeed() { return 0.3D; }
        @Override protected double getSickenedDamage() { return 2.0D; }
        @Override protected double getSickenedFollowRange() { return 48.0D; }
        @Override protected double getFlyingSpeed() { return 1.2D; }
        @Override protected boolean usesAlternateVoicePitch() { return true; }
        @Override protected int getSickenedExperience() { return 1 + rand.nextInt(3); }
        @Override protected boolean growsFromChild() { return true; }

        @Override
        protected void initEntityAI() {
            tasks.addTask(1, new FlyingAttackAI(this, 1.2D, 10));
            tasks.addTask(4, new SickenedBeePollinateAI(this));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            tasks.addTask(7, new SickenedBeeTaintAI(this));
            tasks.addTask(8, new RandomFlyingAI(this, 1.0D, 10, 6));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(3);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(BEE_FLAGS, (byte) 0);
            dataManager.register(ANGER, 0);
        }

        @Override
        protected void updateAITasks() {
            super.updateAITasks();
            underWaterTicks = isInWater() ? underWaterTicks + 1 : 0;
            if (underWaterTicks > 200) attackEntityFrom(DamageSource.DROWN, 1.0F);
            tickBeeState();
        }

        private void tickBeeState() {
            if (hasStung()) {
                ++ticksSinceSting;
                if (ticksSinceSting % 5 == 0
                        && rand.nextInt(MathHelper.clamp(1200 - ticksSinceSting, 1, 1200)) == 0) {
                    attackEntityFrom(DamageSource.GENERIC, getHealth());
                }
            }
            if (isAngry()) {
                int anger = getAnger() - 1;
                setAnger(anger);
            }
            if (!hasNectar()) ++ticksSincePollination;
            if (cannotEnterHiveTicks > 0) --cannotEnterHiveTicks;
            if (hivePosition != null && ticksExisted % 20 == 0
                    && !(world.getTileEntity(hivePosition)
                    instanceof thedarkcolour.futuremc.tile.BeeHiveTile)) {
                hivePosition = null;
            }
            EntityLivingBase target = getAttackTarget();
            setNearTarget(isAngry() && !hasStung() && target != null
                    && getDistanceSq(target) < 4.0D);
        }

        @Override
        public void onLivingUpdate() {
            previousBodyPitch = bodyPitch;
            super.onLivingUpdate();
            bodyPitch = isNearTarget()
                    ? Math.min(1.0F, bodyPitch + 0.2F)
                    : Math.max(0.0F, bodyPitch - 0.24F);
            if (ticksExisted % 4 == 0) {
                WitherStormMod.proxy.spawnPhlegmParticle(world,
                        posX + rand.nextFloat() - 0.5D,
                        posY + rand.nextFloat(),
                        posZ + rand.nextFloat() - 0.5D,
                        0.0D, 0.0D, 0.0D);
            }
        }

        @Override
        public boolean attackEntityAsMob(Entity target) {
            if (!infectTarget(target)) return false;
            float damage = (float) ((int) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .getAttributeValue());
            boolean attacked = target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
            if (attacked) {
                setHasStung(true);
                setAttackTarget(null);
                if (getHeldItemMainhand().isEmpty() && target instanceof EntityLivingBase) {
                    int difficulty = (int) world.getDifficultyForLocation(new BlockPos(
                            Math.floor(posX), Math.floor(posY), Math.floor(posZ)))
                            .getAdditionalDifficulty();
                    ((EntityLivingBase) target).addPotionEffect(
                            new PotionEffect(MobEffects.WITHER, 75 * difficulty, 1));
                }
                playSound(FSounds.BEE_STING, 1.0F, 0.8F);
            }
            return attacked;
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (!super.attackEntityFrom(source, amount)) return false;
            Entity attacker = source.getTrueSource();
            boolean creativePlayer = attacker instanceof EntityPlayer
                    && ((EntityPlayer) attacker).capabilities.isCreativeMode;
            if (attacker instanceof EntityLivingBase && !creativePlayer && canEntityBeSeen(attacker)) {
                setPollinating(false);
                setBeeAttacker(attacker);
            }
            return true;
        }

        public boolean isAngry() { return getAnger() > 0; }
        public int getAnger() { return dataManager.get(ANGER); }
        public void setAnger(int anger) { dataManager.set(ANGER, Math.max(0, anger)); }
        public boolean hasStung() { return getBeeFlag(4); }
        public void setHasStung(boolean value) { setBeeFlag(4, value); }
        public boolean isPollinating() { return getBeeFlag(1); }
        public void setPollinating(boolean value) { setBeeFlag(1, value); }
        public boolean isNearTarget() { return getBeeFlag(2); }
        public void setNearTarget(boolean value) { setBeeFlag(2, value); }
        public boolean hasNectar() { return getBeeFlag(8); }
        public void setHasNectar(boolean value) { setBeeFlag(8, value); }
        public float getBodyPitch(float partialTicks) {
            return previousBodyPitch + partialTicks * (bodyPitch - previousBodyPitch);
        }
        private boolean getBeeFlag(int flag) { return (dataManager.get(BEE_FLAGS) & flag) != 0; }
        private void setBeeFlag(int flag, boolean value) {
            byte flags = dataManager.get(BEE_FLAGS);
            dataManager.set(BEE_FLAGS, value ? (byte) (flags | flag) : (byte) (flags & ~flag));
        }
        public boolean hasHive() { return hivePosition != null; }
        @Nullable public BlockPos getHivePos() { return hivePosition; }
        public void setHivePos(@Nullable BlockPos position) { hivePosition = position; }
        public boolean setBeeAttacker(@Nullable Entity entity) { return setBeeAttacker(entity, 400); }
        public boolean setBeeAttacker(@Nullable Entity entity, int angerTicks) {
            setAnger(angerTicks + rand.nextInt(400));
            if (entity instanceof EntityLivingBase) {
                targetPlayer = entity.getUniqueID();
                setAttackTarget((EntityLivingBase) entity);
            }
            return true;
        }

        @Override
        public void copySpeciesDataFrom(EntityLivingBase original) {
            super.copySpeciesDataFrom(original);
            NBTTagCompound data = new NBTTagCompound();
            original.writeToNBT(data);
            readBeeState(data);
        }

        @Override
        public void copySpeciesDataTo(EntityLivingBase cured) {
            super.copySpeciesDataTo(cured);
            NBTTagCompound data = new NBTTagCompound();
            cured.writeToNBT(data);
            writeBeeState(data);
            cured.readFromNBT(data);
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            writeBeeState(compound);
        }

        private void writeBeeState(NBTTagCompound compound) {
            if (savedFlowerPosition != null) {
                compound.setTag("FlowerPos", NBTUtil.createPosTag(savedFlowerPosition));
            }
            compound.setBoolean("HasNectar", hasNectar());
            compound.setBoolean("HasStung", hasStung());
            compound.setInteger("Anger", getAnger());
            compound.setInteger("TicksSinceSting", ticksSinceSting);
            compound.setInteger("TicksSincePollination", ticksSincePollination);
            compound.setInteger("CannotEnterHiveTicks", cannotEnterHiveTicks);
            compound.setInteger("CropsGrownSincePollination", cropsGrownSincePollination);
            if (hivePosition != null) compound.setTag("HivePos", NBTUtil.createPosTag(hivePosition));
            compound.setString("HurtBy", targetPlayer == null ? "" : targetPlayer.toString());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            readBeeState(compound);
        }

        private void readBeeState(NBTTagCompound compound) {
            savedFlowerPosition = compound.hasKey("FlowerPos", 10)
                    ? NBTUtil.getPosFromTag(compound.getCompoundTag("FlowerPos")) : null;
            setHasNectar(compound.getBoolean("HasNectar"));
            setHasStung(compound.getBoolean("HasStung"));
            setAnger(compound.getInteger("Anger"));
            ticksSinceSting = compound.getInteger("TicksSinceSting");
            ticksSincePollination = compound.getInteger("TicksSincePollination");
            cannotEnterHiveTicks = compound.getInteger("CannotEnterHiveTicks");
            cropsGrownSincePollination = compound.getInteger("CropsGrownSincePollination");
            hivePosition = compound.hasKey("HivePos", 10)
                    ? NBTUtil.getPosFromTag(compound.getCompoundTag("HivePos")) : null;
            String hurtBy = compound.getString("HurtBy");
            if (!hurtBy.isEmpty()) {
                try {
                    targetPlayer = UUID.fromString(hurtBy);
                    EntityPlayer player = world.getPlayerEntityByUUID(targetPlayer);
                    if (player != null) setAttackTarget(player);
                }
                catch (IllegalArgumentException ignored) { targetPlayer = null; }
            }
        }

        @Override protected boolean canDespawn() { return false; }
    }

    /** 1.12 等价实现：保留上游 Bee 的授粉目标和花位状态。 */
    private static final class SickenedBeePollinateAI extends EntityAIBase {
        private final SickenedBeeEntity bee;
        private int pollinationTicks;
        private int lastPollinationSoundTick;

        SickenedBeePollinateAI(SickenedBeeEntity bee) {
            this.bee = bee;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (bee.isDead || bee.hasNectar() || bee.getAttackTarget() != null
                    || bee.getRNG().nextFloat() < 0.7F) return false;
            bee.savedFlowerPosition = findFlower();
            if (bee.savedFlowerPosition == null) return false;
            moveToFlower();
            return true;
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (bee.isDead || bee.getAttackTarget() != null || bee.savedFlowerPosition == null) return false;
            if (pollinationTicks > 400) return bee.getRNG().nextFloat() < 0.2F;
            return bee.ticksExisted % 20 != 0 || isPollinationTarget(bee.savedFlowerPosition);
        }

        @Override
        public void startExecuting() {
            bee.setPollinating(true);
            pollinationTicks = 0;
            lastPollinationSoundTick = 0;
        }

        @Override
        public void resetTask() {
            bee.setPollinating(false);
            if (pollinationTicks > 400) bee.setHasNectar(true);
            pollinationTicks = 0;
        }

        @Override
        public void updateTask() {
            ++pollinationTicks;
            if (bee.savedFlowerPosition != null) {
                moveToFlower();
                if (bee.getRNG().nextFloat() < 0.05F
                        && pollinationTicks > lastPollinationSoundTick + 60) {
                    lastPollinationSoundTick = pollinationTicks;
                    bee.playSound(FSounds.BEE_POLLINATE, 1.0F, 1.0F);
                }
            }
        }

        @Nullable
        private BlockPos findFlower() {
            BlockPos origin = new BlockPos(
                    Math.floor(bee.posX), Math.floor(bee.posY + 0.5D), Math.floor(bee.posZ));
            List<BlockPos> flowers = new ArrayList<BlockPos>();
            for (int offsetX = -5; offsetX <= 5; offsetX++) {
                for (int offsetY = -5; offsetY <= 5; offsetY++) {
                    for (int offsetZ = -5; offsetZ <= 5; offsetZ++) {
                        BlockPos candidate = origin.add(offsetX, offsetY, offsetZ);
                        if (isPollinationTarget(candidate)) flowers.add(candidate);
                    }
                }
            }
            return flowers.isEmpty() ? null : flowers.get(bee.getRNG().nextInt(flowers.size()));
        }

        private boolean isPollinationTarget(BlockPos position) {
            IBlockState state = bee.world.getBlockState(position);
            return state.getBlock() != Blocks.AIR
                    && UpstreamBlockTags.contains(UpstreamBlockTags.SICKENED_BEE_CAN_CONVERT, state)
                    && !UpstreamBlockTags.contains(UpstreamBlockTags.TAINTED_BLOCKS, state);
        }

        private void moveToFlower() {
            BlockPos flower = bee.savedFlowerPosition;
            bee.getMoveHelper().setMoveTo(flower.getX() + 0.5D, flower.getY() + 0.5D,
                    flower.getZ() + 0.5D, 1.2D);
        }
    }

    /** 还原病化蜜蜂的随机近身污染行为。 */
    private static final class SickenedBeeTaintAI extends EntityAIBase {
        private final SickenedBeeEntity bee;
        private int useTicks;

        SickenedBeeTaintAI(SickenedBeeEntity bee) {
            this.bee = bee;
        }

        @Override
        public boolean shouldExecute() {
            return !bee.isDead && bee.getAttackTarget() == null && bee.getRNG().nextFloat() >= 0.3F;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !bee.isDead && bee.getAttackTarget() == null && useTicks > 0;
        }

        @Override
        public void startExecuting() {
            useTicks = 120;
        }

        @Override
        public void resetTask() {
            useTicks = 0;
        }

        @Override
        public void updateTask() {
            --useTicks;
            if (bee.getRNG().nextInt(15) != 0) return;
            BlockPos origin = new BlockPos(
                    Math.floor(bee.posX), Math.floor(bee.posY + 0.5D), Math.floor(bee.posZ));
            if (taint(origin)) return;
            BlockPos saved = bee.savedFlowerPosition;
            if (saved != null && Math.abs(origin.getX() - saved.getX())
                    + Math.abs(origin.getY() - saved.getY())
                    + Math.abs(origin.getZ() - saved.getZ()) <= 1) {
                TaintingManager.taintBlock(bee.world, saved);
            }
        }

        private boolean taint(BlockPos position) {
            return UpstreamBlockTags.contains(UpstreamBlockTags.SICKENED_BEE_CAN_CONVERT,
                    bee.world.getBlockState(position))
                    && TaintingManager.taintBlock(bee.world, position);
        }
    }

    public static class SickenedCatEntity extends SickenedTameableMob {
        private static final DataParameter<Integer> CAT_VARIANT = EntityDataManager.createKey(
                SickenedCatEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> COLLAR_COLOR = EntityDataManager.createKey(
                SickenedCatEntity.class, DataSerializers.VARINT);
        private static final UUID TAMED_HEALTH_MODIFIER_ID = UUID.fromString(
                "a117a45e-8f79-4da6-9c26-1513bd1966ee");

        public SickenedCatEntity(World world) { super(world); setSize(0.6F, 0.7F); }
        @Override protected double getSickenedHealth() { return 20.0D; }
        @Override protected double getSickenedSpeed() { return 0.32D; }
        @Override protected double getSickenedDamage() { return 4.0D; }
        @Override protected double getSickenedFollowRange() { return 24.0D; }
        @Override protected boolean usesAlternateVoicePitch() { return true; }
        @Override public String getSickenedType() { return "sickened_cat"; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(CAT_VARIANT, 0);
            dataManager.register(COLLAR_COLOR, EnumDyeColor.RED.getDyeDamage());
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(1, new EntityAILeapAtTarget(this, 0.3F));
            tasks.addTask(1, new EntityAIOcelotAttack(this));
            tasks.addTask(3, new EntityAIWanderAvoidWater(this, 1.0D));
            tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(3);
        }

        public int getCatVariant() {
            return MathHelper.clamp(dataManager.get(CAT_VARIANT), 0, 3);
        }

        public EnumDyeColor getCatCollarColor() {
            return EnumDyeColor.byDyeDamage(dataManager.get(COLLAR_COLOR));
        }

        @Override
        public void copySpeciesDataFrom(EntityLivingBase original) {
            super.copySpeciesDataFrom(original);
            if (original instanceof net.minecraft.entity.passive.EntityOcelot) {
                net.minecraft.entity.passive.EntityOcelot cat =
                        (net.minecraft.entity.passive.EntityOcelot) original;
                dataManager.set(CAT_VARIANT, cat.getTameSkin());
            }
            updateTamedHealthBenefit();
            if (isSickenedTamed()) setHealth(getMaxHealth());
        }

        @Override
        public void copySpeciesDataTo(EntityLivingBase cured) {
            super.copySpeciesDataTo(cured);
            if (cured instanceof net.minecraft.entity.passive.EntityOcelot) {
                ((net.minecraft.entity.passive.EntityOcelot) cured).setTameSkin(getCatVariant());
            }
        }

        private void updateTamedHealthBenefit() {
            IAttributeInstance health = getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
            AttributeModifier existing = health.getModifier(TAMED_HEALTH_MODIFIER_ID);
            if (existing != null) health.removeModifier(existing);
            if (isSickenedTamed()) {
                health.applyModifier(new AttributeModifier(TAMED_HEALTH_MODIFIER_ID,
                        "Sickened tamed mob health benefit", 1.4D, 1));
            }
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setInteger("CatVariant", getCatVariant());
            compound.setByte("CollarColor", (byte) getCatCollarColor().getDyeDamage());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            dataManager.set(CAT_VARIANT, MathHelper.clamp(compound.getInteger("CatVariant"), 0, 3));
            if (compound.hasKey("CollarColor", 99)) {
                dataManager.set(COLLAR_COLOR,
                        EnumDyeColor.byDyeDamage(compound.getByte("CollarColor")).getDyeDamage());
            }
            float savedHealth = getHealth();
            updateTamedHealthBenefit();
            setHealth(Math.min(savedHealth, getMaxHealth()));
        }

        @Override protected boolean canDespawn() { return false; }
    }

    public static class SickenedChickenEntity extends SickenedMobEntity {
        public float wingRotation;
        public float destPos;
        public float previousFlapSpeed;
        public float previousFlap;
        public float wingRotationDelta = 1.0F;
        private boolean chickenJockey;

        public SickenedChickenEntity(World world) { super(world); setSize(0.4F, 0.7F); }
        @Override protected double getSickenedHealth() { return 16.0D; }
        @Override protected double getSickenedSpeed() { return 0.25D; }
        @Override protected double getSickenedDamage() { return 2.0D; }
        @Override protected double getSickenedFollowRange() { return 24.0D; }
        @Override protected boolean usesAlternateVoicePitch() { return true; }
        @Override protected int getSickenedExperience() { return 1 + rand.nextInt(3); }
        @Override protected boolean growsFromChild() { return true; }
        @Override public String getSickenedType() { return "sickened_chicken"; }

        @Override protected void initEntityAI() { initStandardAnimalAI(1.125D); }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            previousFlap = wingRotation;
            previousFlapSpeed = destPos;
            destPos += (onGround ? -1.0F : 4.0F) * 0.3F;
            destPos = MathHelper.clamp(destPos, 0.0F, 1.0F);
            if (!onGround && wingRotationDelta < 1.0F) wingRotationDelta = 1.0F;
            wingRotationDelta *= 0.9F;
            if (!onGround && motionY < 0.0D) motionY *= 0.6D;
            wingRotation += wingRotationDelta * 2.0F;
        }

        public boolean isChickenJockey() {
            return chickenJockey;
        }

        public void setChickenJockey(boolean jockey) {
            chickenJockey = jockey;
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setBoolean("IsChickenJockey", chickenJockey);
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            chickenJockey = compound.getBoolean("IsChickenJockey");
        }

        @Override protected boolean canDespawn() { return false; }
    }

    public static class SickenedCowEntity extends SickenedMobEntity {
        public SickenedCowEntity(World world) { super(world); setSize(0.9F, 1.4F); }
        @Override protected double getSickenedHealth() { return 25.0D; }
        @Override protected double getSickenedSpeed() { return 0.2D; }
        @Override protected double getSickenedDamage() { return 2.0D; }
        @Override protected double getSickenedFollowRange() { return 24.0D; }
        @Override protected boolean usesAlternateVoicePitch() { return true; }
        @Override protected int getSickenedExperience() { return 1 + rand.nextInt(3); }
        @Override protected boolean growsFromChild() { return true; }
        @Override public String getSickenedType() { return "sickened_cow"; }

        @Override protected void initEntityAI() { initStandardAnimalAI(1.125D); }

        @Override
        public boolean processInteract(EntityPlayer player, EnumHand hand) {
            ItemStack held = player.getHeldItem(hand);
            if (held.getItem() == Items.BUCKET && !player.capabilities.isCreativeMode && !isChild()) {
                player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
                consumeContainerAndGive(player, hand, held, new ItemStack(Items.MILK_BUCKET));
                return true;
            }
            return super.processInteract(player, hand);
        }

        @Override protected boolean canDespawn() { return false; }
    }

    public static class SickenedCreeperEntity extends SickenedMobEntity {
        private static final DataParameter<Integer> SWELL_STATE = EntityDataManager.createKey(SickenedCreeperEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Boolean> POWERED = EntityDataManager.createKey(SickenedCreeperEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> IGNITED = EntityDataManager.createKey(SickenedCreeperEntity.class, DataSerializers.BOOLEAN);
        private int oldSwell;
        private int swell;
        private int maxSwell = 40;
        private int explosionRadius = 5;

        public void ignite() {
            dataManager.set(IGNITED, true);
        }

        public SickenedCreeperEntity(World world) { super(world); setSize(0.6F, 1.7F); }
        @Override protected double getSickenedHealth() { return 26.0D; }
        @Override protected double getSickenedSpeed() { return 0.255D; }
        @Override protected double getSickenedFollowRange() { return 18.0D; }
        @Override public String getSickenedType() { return "sickened_creeper"; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(SWELL_STATE, Integer.valueOf(-1));
            dataManager.register(POWERED, Boolean.FALSE);
            dataManager.register(IGNITED, Boolean.FALSE);
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(1, new EntityAISwimming(this));
            tasks.addTask(2, new SickenedCreeperSwellAI(this));
            tasks.addTask(3, new EntityAIAvoidEntity<EntityOcelot>(this,
                    EntityOcelot.class, 6.0F, 1.0D, 1.2D));
            tasks.addTask(4, new EntityAIAttackMelee(this, 1.0D, false));
            tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.8D));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            tasks.addTask(6, new EntityAILookIdle(this));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
        }

        @Override
        public void onUpdate() {
            if (isEntityAlive()) {
                oldSwell = swell;
                if (isIgnited()) setSwellState(1);
                int state = getSwellState();
                if (state > 0 && swell == 0) {
                    playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0F, 0.5F);
                }
                swell += state;
                if (swell < 0) {
                    swell = 0;
                }
                if (swell >= maxSwell) {
                    swell = maxSwell;
                    explode();
                }
            }
            super.onUpdate();
        }

        public int getSwellState() {
            return dataManager.get(SWELL_STATE).intValue();
        }

        public void setSwellState(int state) {
            dataManager.set(SWELL_STATE, Integer.valueOf(state));
        }

        public float getCreeperFlashIntensity(float partialTicks) {
            return MathHelper.clamp((oldSwell + (swell - oldSwell) * partialTicks) / (maxSwell - 2.0F), 0.0F, 1.0F);
        }

        public boolean isPowered() {
            return dataManager.get(POWERED).booleanValue();
        }

        public boolean isIgnited() {
            return dataManager.get(IGNITED);
        }

        @Override
        public boolean processInteract(EntityPlayer player, EnumHand hand) {
            ItemStack held = player.getHeldItem(hand);
            if (held.getItem() == Items.FLINT_AND_STEEL) {
                world.playSound(player, posX, posY, posZ,
                        SoundEvents.ITEM_FLINTANDSTEEL_USE, getSoundCategory(),
                        1.0F, rand.nextFloat() * 0.4F + 0.8F);
                player.swingArm(hand);
                if (!world.isRemote) {
                    ignite();
                    held.damageItem(1, player);
                }
                return true;
            }
            return super.processInteract(player, hand);
        }

        @Override
        public void onStruckByLightning(EntityLightningBolt lightningBolt) {
            super.onStruckByLightning(lightningBolt);
            dataManager.set(POWERED, Boolean.TRUE);
        }

        private void explode() {
            if (!world.isRemote) {
                float multiplier = isPowered() ? 2.0F : 1.0F;
                boolean damagesTerrain = ForgeEventFactory.getMobGriefingEvent(world, this);
                setDead();
                world.newExplosion(this, posX, posY, posZ, explosionRadius * multiplier, false, damagesTerrain);
                spawnLingeringCloud();
            }
        }

        /** Mirrors the 1.12 creeper's potion-effect cloud after its explosion. */
        private void spawnLingeringCloud() {
            java.util.Collection<PotionEffect> effects = getActivePotionEffects();
            if (effects.isEmpty()) return;
            EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(world, posX, posY, posZ);
            cloud.setRadius(2.5F);
            cloud.setRadiusOnUse(-0.5F);
            cloud.setWaitTime(10);
            cloud.setDuration(cloud.getDuration() / 2);
            cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());
            for (PotionEffect effect : effects) {
                cloud.addEffect(new PotionEffect(effect));
            }
            world.spawnEntity(cloud);
        }

        @Override protected int getInfectedHealAmount() { return 0; }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setShort("Fuse", (short) maxSwell);
            compound.setByte("ExplosionRadius", (byte) explosionRadius);
            compound.setBoolean("powered", isPowered());
            compound.setBoolean("ignited", isIgnited());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            if (compound.hasKey("Fuse", 99)) maxSwell = compound.getShort("Fuse");
            if (compound.hasKey("ExplosionRadius", 99)) explosionRadius = compound.getByte("ExplosionRadius");
            dataManager.set(POWERED, Boolean.valueOf(compound.getBoolean("powered")));
            dataManager.set(IGNITED, compound.getBoolean("ignited"));
        }

        private static class SickenedCreeperSwellAI extends EntityAIBase {
            private final SickenedCreeperEntity creeper;
            private EntityLivingBase target;

            SickenedCreeperSwellAI(SickenedCreeperEntity creeper) {
                this.creeper = creeper;
                setMutexBits(1);
            }

            @Override
            public boolean shouldExecute() {
                EntityLivingBase current = creeper.getAttackTarget();
                return creeper.getSwellState() > 0 || current != null && creeper.getDistanceSq(current) < 9.0D;
            }

            @Override
            public void startExecuting() {
                creeper.getNavigator().clearPath();
                target = creeper.getAttackTarget();
            }

            @Override
            public void resetTask() {
                target = null;
            }

            @Override
            public void updateTask() {
                if (target == null || creeper.getDistanceSq(target) > 49.0D || !creeper.getEntitySenses().canSee(target)) {
                    creeper.setSwellState(-1);
                } else {
                    creeper.setSwellState(1);
                }
            }
        }
    }

    public static class SickenedIronGolemEntity extends SickenedMobEntity {
        private int attackAnimationTick;

        public SickenedIronGolemEntity(World world) { super(world); setSize(1.4F, 2.7F); }
        @Override protected double getSickenedHealth() { return 60.0D; }
        @Override protected double getSickenedSpeed() { return 0.25D; }
        @Override protected double getSickenedDamage() { return 10.0D; }
        @Override protected double getSickenedFollowRange() { return 48.0D; }
        @Override protected double getSickenedKnockbackResistance() { return 1.0D; }
        @Override protected int getSickenedExperience() { return 0; }
        @Override public String getSickenedType() { return "sickened_iron_golem"; }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new EntityAIAttackMelee(this, 1.1D, true));
            tasks.addTask(1, new EntityAIMoveTowardsTarget(this, 1.0D, 32.0F));
            tasks.addTask(2, new EntityAIWanderAvoidWater(this, 1.0D));
            tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
            tasks.addTask(4, new EntityAILookIdle(this));
            targetTasks.addTask(0, new EntityAIHurtByTarget(this, false));
            targetTasks.addTask(1, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(2);
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            if (attackAnimationTick > 0) {
                --attackAnimationTick;
            }
            double horizontalSpeedSquared = motionX * motionX + motionZ * motionZ;
            if (world.isRemote && horizontalSpeedSquared > 2.500000277905201E-7D
                    && rand.nextInt(5) == 0) {
                BlockPos groundPos = new BlockPos(MathHelper.floor(posX),
                        MathHelper.floor(posY - 0.2D), MathHelper.floor(posZ));
                IBlockState groundState = world.getBlockState(groundPos);
                if (!groundState.getBlock().isAir(groundState, world, groundPos)) {
                    world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                            posX + (rand.nextDouble() - 0.5D) * width,
                            posY + 0.1D,
                            posZ + (rand.nextDouble() - 0.5D) * width,
                            4.0D * (rand.nextDouble() - 0.5D),
                            0.5D,
                            4.0D * (rand.nextDouble() - 0.5D),
                            Block.getStateId(groundState));
                }
            }
        }

        @Override
        public boolean processInteract(EntityPlayer player, EnumHand hand) {
            if (super.processInteract(player, hand)) return true;
            ItemStack held = player.getHeldItem(hand);
            if (held.getItem() != Items.IRON_INGOT) return false;
            float previousHealth = getHealth();
            heal(getMaxHealth() / 4.0F);
            if (getHealth() == previousHealth) return false;
            playSound(SoundEvents.BLOCK_ANVIL_USE, 1.0F,
                    1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F);
            if (!player.capabilities.isCreativeMode) held.shrink(1);
            return true;
        }

        @Override
        public boolean attackEntityAsMob(Entity entityIn) {
            if (!infectTarget(entityIn)) return false;
            attackAnimationTick = 10;
            if (!world.isRemote) {
                world.setEntityState(this, (byte) 4);
            }
            float damage = (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .getAttributeValue();
            float modifiedDamage = (int) damage > 0
                    ? damage / 2.0F + rand.nextInt((int) damage)
                    : damage;
            boolean attacked = entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), modifiedDamage);
            if (attacked) {
                double resistance = entityIn instanceof EntityLivingBase
                        ? ((EntityLivingBase) entityIn)
                        .getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE)
                        .getAttributeValue()
                        : 0.0D;
                entityIn.motionY += 0.4D * Math.max(0.0D, 1.0D - resistance);
                entityIn.velocityChanged = true;
                applyEnchantments(this, entityIn);
                if (getHeldItemMainhand().isEmpty() && entityIn instanceof EntityLivingBase) {
                    int difficulty = (int) world.getDifficultyForLocation(new BlockPos(this))
                            .getAdditionalDifficulty();
                    ((EntityLivingBase) entityIn).addPotionEffect(
                            new PotionEffect(MobEffects.WITHER, 120 * difficulty));
                }
            }
            playSound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 1.0F, 1.0F);
            return attacked;
        }

        @Override
        public void handleStatusUpdate(byte id) {
            if (id == 4) {
                attackAnimationTick = 10;
            } else {
                super.handleStatusUpdate(id);
            }
        }

        public int getAttackAnimationTick() {
            return attackAnimationTick;
        }

        @Override protected int getInfectedHealAmount() { return 6; }
    }

    public static class SickenedMushroomCowEntity extends SickenedCowEntity implements IShearable {
        public static final String STEW_DURATION_TAG = "SickenedStewDuration";

        public SickenedMushroomCowEntity(World world) { super(world); setSize(0.9F, 1.4F); }
        @Override protected double getSickenedHealth() { return 26.0D; }
        @Override protected double getSickenedSpeed() { return 0.3D; }
        @Override protected double getSickenedDamage() { return 2.0D; }
        @Override public String getSickenedType() { return "sickened_mushroom_cow"; }

        @Override
        public boolean processInteract(EntityPlayer player, EnumHand hand) {
            ItemStack held = player.getHeldItem(hand);
            if (held.getItem() == Items.BOWL && !player.capabilities.isCreativeMode && !isChild()) {
                ItemStack stew = new ItemStack(Items.MUSHROOM_STEW);
                stew.getOrCreateSubCompound("WitherStormMod")
                        .setInteger(STEW_DURATION_TAG, 160 + rand.nextInt(60));
                consumeContainerAndGive(player, hand, held, stew);
                playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
                return true;
            }
            return super.processInteract(player, hand);
        }

        @Override
        public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
            return !isChild();
        }

        @Override
        public List<ItemStack> onSheared(ItemStack item, IBlockAccess blockAccess, BlockPos pos, int fortune) {
            if (!world.isRemote) {
                playSound(SoundEvents.ENTITY_MOOSHROOM_SHEAR, 1.0F, 1.0F);
                if (world instanceof WorldServer) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                            posX, posY + height * 0.5D, posZ, 1,
                            0.0D, 0.0D, 0.0D, 0.0D);
                }
                SickenedCowEntity cow = new SickenedCowEntity(world);
                cow.setLocationAndAngles(posX, posY, posZ, rotationYaw, rotationPitch);
                cow.setHealth(getHealth());
                cow.renderYawOffset = renderYawOffset;
                if (hasCustomName()) {
                    cow.setCustomNameTag(getCustomNameTag());
                    cow.setAlwaysRenderNameTag(getAlwaysRenderNameTag());
                }
                if (isNoDespawnRequired()) cow.enablePersistence();
                cow.setEntityInvulnerable(getIsInvulnerable());
                setDead();
                world.spawnEntity(cow);
            }
            ItemStack mushroom = new ItemStack(ModBlocks.getItem("tainted_mushroom"));
            List<ItemStack> drops = new ArrayList<ItemStack>(3);
            drops.add(mushroom.copy());
            drops.add(mushroom.copy());
            drops.add(mushroom.copy());
            return drops;
        }

        @Override protected boolean canDespawn() { return false; }
    }

    public static class SickenedParrotEntity extends FlyingSickenedTameableMob {
        private static final DataParameter<Integer> VARIANT = EntityDataManager.createKey(
                SickenedParrotEntity.class, DataSerializers.VARINT);
        private float flap;
        private float flapSpeed;
        private float previousFlapSpeed;
        private float previousFlap;
        private float flapping = 1.0F;
        private boolean partying;
        private BlockPos jukeboxPosition;

        public SickenedParrotEntity(World world) { super(world); setSize(0.5F, 0.9F); }
        @Override protected double getSickenedHealth() { return 16.0D; }
        @Override protected double getSickenedSpeed() { return 0.4D; }
        @Override protected double getSickenedDamage() { return 2.0D; }
        @Override protected double getSickenedFollowRange() { return 24.0D; }
        @Override protected double getFlyingSpeed() { return 0.9D; }
        @Override protected boolean usesAlternateVoicePitch() { return true; }
        @Override public String getSickenedType() { return "sickened_parrot"; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(VARIANT, 0);
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(1, new FlyingAttackAI(this, 1.1D, 10));
            tasks.addTask(2, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            tasks.addTask(3, new EntityAIFollow(this, 1.0D, 3.0F, 7.0F));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(3);
        }

        @Override
        public void onLivingUpdate() {
            if (jukeboxPosition == null
                    || jukeboxPosition.distanceSq(posX, posY, posZ) > 12.0D
                    || world.getBlockState(jukeboxPosition).getBlock() != Blocks.JUKEBOX) {
                partying = false;
                jukeboxPosition = null;
            }
            super.onLivingUpdate();
            calculateFlapping();
        }

        @Override
        public void setPartying(BlockPos position, boolean isPartying) {
            jukeboxPosition = position;
            partying = isPartying;
        }

        public boolean isSickenedPartying() {
            return partying;
        }

        public boolean isSickenedFlying() {
            return !onGround;
        }

        public float getFlapBob(float partialTicks) {
            float interpolatedFlap = previousFlap + (flap - previousFlap) * partialTicks;
            float interpolatedSpeed = previousFlapSpeed
                    + (flapSpeed - previousFlapSpeed) * partialTicks;
            return (MathHelper.sin(interpolatedFlap) + 1.0F) * interpolatedSpeed;
        }

        private void calculateFlapping() {
            previousFlap = flap;
            previousFlapSpeed = flapSpeed;
            flapSpeed += (onGround ? -1.0F : 4.0F) * 0.3F;
            flapSpeed = MathHelper.clamp(flapSpeed, 0.0F, 1.0F);
            if (!onGround && flapping < 1.0F) flapping = 1.0F;
            flapping *= 0.9F;
            if (!onGround && motionY < 0.0D) motionY *= 0.6D;
            flap += flapping * 2.0F;
        }

        public int getParrotVariant() {
            return MathHelper.clamp(dataManager.get(VARIANT), 0, 4);
        }

        @Override
        public void copySpeciesDataFrom(EntityLivingBase original) {
            super.copySpeciesDataFrom(original);
            if (original instanceof net.minecraft.entity.passive.EntityParrot) {
                dataManager.set(VARIANT,
                        ((net.minecraft.entity.passive.EntityParrot) original).getVariant());
            }
        }

        @Override
        public void copySpeciesDataTo(EntityLivingBase cured) {
            super.copySpeciesDataTo(cured);
            if (cured instanceof net.minecraft.entity.passive.EntityParrot) {
                ((net.minecraft.entity.passive.EntityParrot) cured).setVariant(getParrotVariant());
            }
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setInteger("Variant", getParrotVariant());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            dataManager.set(VARIANT, MathHelper.clamp(compound.getInteger("Variant"), 0, 4));
        }

        @Override protected boolean canDespawn() { return false; }
    }

    public static class SickenedPhantomEntity extends FlyingSickenedMob {
        private static final DataParameter<Integer> PHANTOM_SIZE = EntityDataManager.createKey(
                SickenedPhantomEntity.class, DataSerializers.VARINT);
        private static final float BASE_WIDTH = 0.9F;
        private static final float BASE_HEIGHT = 0.5F;
        private Vec3d moveTargetPoint = Vec3d.ZERO;
        private BlockPos anchorPoint = BlockPos.ORIGIN;
        private PhantomAttackPhase attackPhase = PhantomAttackPhase.CIRCLE;

        public SickenedPhantomEntity(World world) {
            super(world);
            setSize(0.9F, 0.5F);
            moveHelper = new PhantomMoveHelper(this);
        }
        @Override public String getSickenedType() { return "sickened_phantom"; }
        @Override protected double getSickenedHealth() { return 20.0D; }
        @Override protected double getSickenedSpeed() { return 0.25D; }
        @Override protected double getSickenedDamage() { return 3.0D; }
        @Override protected double getFlyingSpeed() { return 0.5D; }
        @Override protected boolean usesAlternateVoicePitch() { return true; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(PHANTOM_SIZE, 0);
        }

        public int getPhantomSize() {
            return dataManager.get(PHANTOM_SIZE);
        }

        public void setPhantomSize(int size) {
            dataManager.set(PHANTOM_SIZE, MathHelper.clamp(size, 0, 64));
        }

        private void updatePhantomSizeInfo() {
            int size = getPhantomSize();
            float width = BASE_WIDTH + 0.2F * size;
            float scale = width / BASE_WIDTH;
            setSize(width, BASE_HEIGHT * scale);
            getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .setBaseValue(6.0D + size);
        }

        @Override
        public void notifyDataManagerChange(DataParameter<?> key) {
            super.notifyDataManagerChange(key);
            if (PHANTOM_SIZE.equals(key)) updatePhantomSizeInfo();
        }

        @Override
        public void copySpeciesDataFrom(EntityLivingBase original) {
            super.copySpeciesDataFrom(original);
            anchorPoint = new BlockPos(this).up(5);
            NBTTagCompound data = new NBTTagCompound();
            original.writeToNBT(data);
            if (data.hasKey("Size", 99)) setPhantomSize(data.getInteger("Size"));
        }

        @Override
        public void copySpeciesDataTo(EntityLivingBase cured) {
            super.copySpeciesDataTo(cured);
            NBTTagCompound data = new NBTTagCompound();
            cured.writeToNBT(data);
            data.setInteger("Size", getPhantomSize());
            cured.readFromNBT(data);
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setInteger("Size", getPhantomSize());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            setPhantomSize(compound.getInteger("Size"));
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(1, new PhantomAttackStrategyAI(this));
            if (WitherStormConfig.phantomsOrbitWitherStorm) {
                tasks.addTask(1, new OrbitWitherStormAI(this));
            }
            tasks.addTask(2, new PhantomSweepAttackAI(this));
            tasks.addTask(3, new PhantomCircleAroundAnchorAI(this));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(3);
        }

        @Override
        public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty,
                                                @Nullable IEntityLivingData livingData) {
            anchorPoint = new BlockPos(this).up(5);
            return super.onInitialSpawn(difficulty, livingData);
        }

        @Override
        protected boolean canDespawn() {
            return !WitherStormConfig.phantomsOrbitWitherStorm && super.canDespawn();
        }
    }

    private enum PhantomAttackPhase {
        CIRCLE,
        SWOOP
    }

    private abstract static class PhantomMoveTargetAI extends EntityAIBase {
        protected final SickenedPhantomEntity entity;

        PhantomMoveTargetAI(SickenedPhantomEntity entity) {
            this.entity = entity;
            setMutexBits(1);
        }

        protected boolean touchingTarget() {
            return entity.moveTargetPoint.squareDistanceTo(entity.getPositionVector()) < 4.0D;
        }
    }

    private static final class PhantomAttackStrategyAI extends EntityAIBase {
        private final SickenedPhantomEntity entity;
        private int nextSweepTick;

        PhantomAttackStrategyAI(SickenedPhantomEntity entity) {
            this.entity = entity;
        }

        @Override
        public boolean shouldExecute() {
            return entity.getAttackTarget() != null;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return entity.getAttackTarget() != null;
        }

        @Override
        public void startExecuting() {
            nextSweepTick = 10;
            entity.attackPhase = PhantomAttackPhase.CIRCLE;
        }

        @Override
        public void resetTask() {
            entity.anchorPoint = new BlockPos(entity).up(10 + entity.getRNG().nextInt(20));
        }

        @Override
        public void updateTask() {
            if (entity.attackPhase != PhantomAttackPhase.CIRCLE || --nextSweepTick > 0) return;
            entity.attackPhase = PhantomAttackPhase.SWOOP;
            EntityLivingBase target = entity.getAttackTarget();
            if (target != null) {
                entity.anchorPoint = new BlockPos(target).up(20 + entity.getRNG().nextInt(20));
            }
            nextSweepTick = 8 + entity.getRNG().nextInt(4) * 20;
            entity.playSound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 10.0F,
                    0.95F + entity.getRNG().nextFloat() * 0.1F);
        }
    }

    private static final class PhantomSweepAttackAI extends PhantomMoveTargetAI {
        PhantomSweepAttackAI(SickenedPhantomEntity entity) {
            super(entity);
        }

        @Override
        public boolean shouldExecute() {
            return entity.getAttackTarget() != null
                    && entity.attackPhase == PhantomAttackPhase.SWOOP;
        }

        @Override
        public boolean shouldContinueExecuting() {
            EntityLivingBase target = entity.getAttackTarget();
            if (target == null || !target.isEntityAlive()) return false;
            if (target instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) target;
                if (player.isSpectator() || player.capabilities.isCreativeMode) return false;
            }
            return shouldExecute();
        }

        @Override
        public void resetTask() {
            entity.setAttackTarget(null);
            entity.attackPhase = PhantomAttackPhase.CIRCLE;
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = entity.getAttackTarget();
            if (target == null) return;
            entity.moveTargetPoint = new Vec3d(target.posX,
                    target.posY + target.height * 0.5D, target.posZ);
            if (entity.getEntityBoundingBox().grow(0.2D)
                    .intersects(target.getEntityBoundingBox())) {
                entity.attackEntityAsMob(target);
                entity.attackPhase = PhantomAttackPhase.CIRCLE;
                return;
            }
            if (entity.collidedHorizontally) {
                entity.attackPhase = PhantomAttackPhase.CIRCLE;
                return;
            }
            if (entity.ticksExisted % 20 != 0) return;
            List<EntityOcelot> cats = entity.world.getEntitiesWithinAABB(EntityOcelot.class,
                    entity.getEntityBoundingBox().grow(16.0D), EntityLivingBase::isEntityAlive);
            if (!cats.isEmpty()) {
                for (EntityOcelot cat : cats) {
                    cat.playSound(SoundEvents.ENTITY_CAT_HISS, 1.0F, 1.0F);
                }
                entity.attackPhase = PhantomAttackPhase.CIRCLE;
            }
        }
    }

    private static final class PhantomCircleAroundAnchorAI extends PhantomMoveTargetAI {
        private float angle;
        private float distance;
        private float height;
        private float clockwise;

        PhantomCircleAroundAnchorAI(SickenedPhantomEntity entity) {
            super(entity);
        }

        @Override
        public boolean shouldExecute() {
            return entity.getAttackTarget() == null
                    || entity.attackPhase == PhantomAttackPhase.CIRCLE;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return shouldExecute();
        }

        @Override
        public void startExecuting() {
            distance = 5.0F + entity.getRNG().nextFloat() * 10.0F;
            height = -4.0F + entity.getRNG().nextFloat() * 9.0F;
            clockwise = entity.getRNG().nextBoolean() ? 1.0F : -1.0F;
            selectNext();
        }

        @Override
        public void updateTask() {
            if (entity.getRNG().nextInt(350) == 0) {
                height = -4.0F + entity.getRNG().nextFloat() * 9.0F;
            }
            if (entity.getRNG().nextInt(250) == 0) {
                distance += 1.0F;
                if (distance > 15.0F) {
                    distance = 5.0F;
                    clockwise = -clockwise;
                }
            }
            if (entity.getRNG().nextInt(450) == 0) {
                angle = entity.getRNG().nextFloat() * (float) (Math.PI * 2.0D);
                selectNext();
            }
            if (touchingTarget()) selectNext();
            avoidBlockedVerticalTarget();
        }

        private void avoidBlockedVerticalTarget() {
            BlockPos position = new BlockPos(entity);
            if (entity.moveTargetPoint.y < entity.posY && !entity.world.isAirBlock(position.up())) {
                height = Math.max(1.0F, height);
                selectNext();
            }
            if (entity.moveTargetPoint.y > entity.posY && !entity.world.isAirBlock(position.down())) {
                height = Math.min(-1.0F, height);
                selectNext();
            }
        }

        private void selectNext() {
            if (entity.anchorPoint.equals(BlockPos.ORIGIN)) {
                entity.anchorPoint = new BlockPos(entity);
            }
            angle += clockwise * 15.0F * ((float) Math.PI / 180.0F);
            entity.moveTargetPoint = new Vec3d(entity.anchorPoint).add(
                    distance * MathHelper.cos(angle), -4.0F + height,
                    distance * MathHelper.sin(angle));
        }
    }

    /** 上游病化幻翼始终优先环绕 phase 3+ 且在视线内的风暴。 */
    private static final class OrbitWitherStormAI extends PhantomMoveTargetAI {
        private static final double SEARCH_RANGE = 100.0D;

        private WitherStormEntity storm;
        private float angle;
        private float distance;
        private float height;
        private float clockwise;

        private OrbitWitherStormAI(SickenedPhantomEntity entity) {
            super(entity);
        }

        @Override
        public boolean shouldExecute() {
            if (!WitherStormConfig.phantomsOrbitWitherStorm || entity.isDead) return false;
            storm = findStorm();
            return storm != null && storm.getPhase() > 2;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return shouldExecute();
        }

        @Override
        public void startExecuting() {
            distance = 25.0F + entity.getRNG().nextFloat() * 10.0F;
            height = -4.0F + entity.getRNG().nextFloat() * 9.0F;
            clockwise = entity.getRNG().nextBoolean() ? 1.0F : -1.0F;
            selectNext();
        }

        @Override
        public void updateTask() {
            if (storm == null) return;
            if (entity.getRNG().nextInt(350) == 0) {
                height = -4.0F + entity.getRNG().nextFloat() * 9.0F;
            }
            if (entity.getRNG().nextInt(250) == 0) {
                distance += 1.0F;
                if (distance > 75.0F) {
                    distance = 25.0F;
                    clockwise = -clockwise;
                }
            }
            if (entity.getRNG().nextInt(450) == 0) {
                angle = entity.getRNG().nextFloat() * (float) (Math.PI * 2.0D);
                selectNext();
            }
            if (touchingTarget()) selectNext();
            BlockPos position = new BlockPos(entity);
            if (entity.moveTargetPoint.y < entity.posY && !entity.world.isAirBlock(position.up())) {
                height = Math.max(1.0F, height);
                selectNext();
            }
            if (entity.moveTargetPoint.y > entity.posY && !entity.world.isAirBlock(position.down())) {
                height = Math.min(-1.0F, height);
                selectNext();
            }
        }

        private void selectNext() {
            angle += clockwise * 15.0F * ((float) Math.PI / 180.0F);
            if (storm != null) {
                entity.anchorPoint = new BlockPos(storm).up((int) storm.height + 20);
            }
            entity.moveTargetPoint = new Vec3d(entity.anchorPoint).add(
                    distance * MathHelper.cos(angle), -4.0F + height,
                    distance * MathHelper.sin(angle));
        }

        private WitherStormEntity findStorm() {
            return entity.world.getEntitiesWithinAABB(WitherStormEntity.class,
                    entity.getEntityBoundingBox().grow(SEARCH_RANGE),
                    candidate -> candidate != null && candidate.isEntityAlive()
                            && !candidate.isDeadOrPlayingDead()
                            && entity.getEntitySenses().canSee(candidate))
                    .stream().min(Comparator.comparingDouble(entity::getDistanceSq)).orElse(null);
        }
    }

    private static final class PhantomMoveHelper extends EntityMoveHelper {
        private final SickenedPhantomEntity phantom;
        private float flyingSpeed = 0.1F;

        PhantomMoveHelper(SickenedPhantomEntity phantom) {
            super(phantom);
            this.phantom = phantom;
        }

        @Override
        public void onUpdateMoveHelper() {
            if (phantom.collidedHorizontally) {
                phantom.rotationYaw += 180.0F;
                flyingSpeed = 0.1F;
            }
            double deltaX = phantom.moveTargetPoint.x - phantom.posX;
            double deltaY = phantom.moveTargetPoint.y - phantom.posY;
            double deltaZ = phantom.moveTargetPoint.z - phantom.posZ;
            double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (Math.abs(horizontal) <= 1.0E-5D) return;

            double horizontalScale = 1.0D - Math.abs(deltaY * 0.7D) / horizontal;
            deltaX *= horizontalScale;
            deltaZ *= horizontalScale;
            horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (distance <= 1.0E-5D) return;

            float oldYaw = phantom.rotationYaw;
            float desiredYaw = (float) (MathHelper.atan2(deltaZ, deltaX) * 57.2957763671875D);
            float wrappedYaw = MathHelper.wrapDegrees(phantom.rotationYaw + 90.0F);
            phantom.rotationYaw = approachDegrees(wrappedYaw,
                    MathHelper.wrapDegrees(desiredYaw), 4.0F) - 90.0F;
            phantom.renderYawOffset = phantom.rotationYaw;
            if (Math.abs(MathHelper.wrapDegrees(oldYaw - phantom.rotationYaw)) < 3.0F) {
                flyingSpeed = approach(flyingSpeed, 1.8F,
                        0.005F * (1.8F / Math.max(0.1F, flyingSpeed)));
            } else {
                flyingSpeed = approach(flyingSpeed, 0.2F, 0.025F);
            }

            float desiredPitch = (float) (-(MathHelper.atan2(-deltaY, horizontal)
                    * 57.2957763671875D));
            phantom.rotationPitch = desiredPitch;
            float movementYaw = (phantom.rotationYaw + 90.0F) * ((float) Math.PI / 180.0F);
            double targetMotionX = flyingSpeed * MathHelper.cos(movementYaw)
                    * Math.abs(deltaX / distance);
            double targetMotionZ = flyingSpeed * MathHelper.sin(movementYaw)
                    * Math.abs(deltaZ / distance);
            double targetMotionY = flyingSpeed * MathHelper.sin(desiredPitch
                    * ((float) Math.PI / 180.0F)) * Math.abs(deltaY / distance);
            phantom.motionX += (targetMotionX - phantom.motionX) * 0.2D;
            phantom.motionY += (targetMotionY - phantom.motionY) * 0.2D;
            phantom.motionZ += (targetMotionZ - phantom.motionZ) * 0.2D;
        }

        private static float approach(float current, float target, float amount) {
            if (current < target) return Math.min(current + amount, target);
            return Math.max(current - amount, target);
        }

        private static float approachDegrees(float current, float target, float amount) {
            float difference = MathHelper.wrapDegrees(target - current);
            return current + MathHelper.clamp(difference, -amount, amount);
        }
    }

    public static class SickenedPigEntity extends SickenedMobEntity {
        private static final DataParameter<Boolean> SADDLED = EntityDataManager.createKey(
                SickenedPigEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Integer> BOOST_TIME = EntityDataManager.createKey(
                SickenedPigEntity.class, DataSerializers.VARINT);
        private boolean boosting;
        private int boostTime;
        private int totalBoostTime;

        public SickenedPigEntity(World world) { super(world); setSize(0.9F, 0.9F); }
        @Override protected double getSickenedHealth() { return 20.0D; }
        @Override protected double getSickenedSpeed() { return 0.25D; }
        @Override protected double getSickenedDamage() { return 2.0D; }
        @Override protected double getSickenedFollowRange() { return 24.0D; }
        @Override protected int getSickenedExperience() { return 1 + rand.nextInt(3); }
        @Override protected boolean growsFromChild() { return true; }
        @Override protected boolean usesAlternateVoicePitch() { return true; }
        @Override public String getSickenedType() { return "sickened_pig"; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(SADDLED, false);
            dataManager.register(BOOST_TIME, 0);
        }

        @Override protected void initEntityAI() { initStandardAnimalAI(1.125D); }

        @Override
        public Entity getControllingPassenger() {
            return getPassengers().isEmpty() ? null : getPassengers().get(0);
        }

        public boolean canBeSteered() {
            Entity passenger = getControllingPassenger();
            if (!(passenger instanceof EntityPlayer)) return false;
            EntityPlayer player = (EntityPlayer) passenger;
            return player.getHeldItemMainhand().getItem() == Items.CARROT_ON_A_STICK
                    || player.getHeldItemOffhand().getItem() == Items.CARROT_ON_A_STICK;
        }

        @Override
        public void notifyDataManagerChange(DataParameter<?> key) {
            if (BOOST_TIME.equals(key) && world.isRemote) {
                boosting = true;
                boostTime = 0;
                totalBoostTime = dataManager.get(BOOST_TIME);
            }
            super.notifyDataManagerChange(key);
        }

        public boolean getSaddled() {
            return dataManager.get(SADDLED);
        }

        public void setSaddled(boolean saddled) {
            dataManager.set(SADDLED, saddled);
        }

        @Override
        public boolean processInteract(EntityPlayer player, EnumHand hand) {
            if (super.processInteract(player, hand)) return true;
            ItemStack held = player.getHeldItem(hand);
            if (getSaddled() && !isBeingRidden()) {
                if (!world.isRemote) player.startRiding(this);
                return true;
            }
            if (held.getItem() == Items.SADDLE && !getSaddled() && !isChild()) {
                setSaddled(true);
                world.playSound(player, posX, posY, posZ, SoundEvents.ENTITY_PIG_SADDLE,
                        SoundCategory.NEUTRAL, 0.5F, 1.0F);
                held.shrink(1);
                return true;
            }
            return false;
        }

        @Override
        public AxisAlignedBB getRenderBoundingBox() {
            return getEntityBoundingBox().grow(3.0D);
        }

        @Override
        public void travel(float strafe, float vertical, float forward) {
            Entity passenger = getControllingPassenger();
            if (!isBeingRidden() || !canBeSteered() || passenger == null) {
                stepHeight = 0.5F;
                jumpMovementFactor = 0.02F;
                super.travel(strafe, vertical, forward);
                return;
            }

            rotationYaw = passenger.rotationYaw;
            prevRotationYaw = rotationYaw;
            rotationPitch = passenger.rotationPitch * 0.5F;
            setRotation(rotationYaw, rotationPitch);
            renderYawOffset = rotationYaw;
            rotationYawHead = rotationYaw;
            stepHeight = 1.0F;
            jumpMovementFactor = getAIMoveSpeed() * 0.1F;
            if (boosting && boostTime++ > totalBoostTime) boosting = false;

            if (canPassengerSteer()) {
                float speed = (float) getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED)
                        .getAttributeValue() * 0.225F;
                if (boosting && totalBoostTime > 0) {
                    speed += speed * 1.15F * MathHelper.sin(
                            (float) boostTime / (float) totalBoostTime * (float) Math.PI);
                }
                setAIMoveSpeed(speed);
                super.travel(0.0F, 0.0F, 1.0F);
            } else {
                motionX = 0.0D;
                motionY = 0.0D;
                motionZ = 0.0D;
            }

            prevLimbSwingAmount = limbSwingAmount;
            double movedX = posX - prevPosX;
            double movedZ = posZ - prevPosZ;
            float movement = MathHelper.sqrt(movedX * movedX + movedZ * movedZ) * 4.0F;
            if (movement > 1.0F) movement = 1.0F;
            limbSwingAmount += (movement - limbSwingAmount) * 0.4F;
            limbSwing += limbSwingAmount;
        }

        public boolean boost() {
            if (boosting) return false;
            boosting = true;
            boostTime = 0;
            totalBoostTime = getRNG().nextInt(841) + 140;
            dataManager.set(BOOST_TIME, totalBoostTime);
            return true;
        }

        @Override
        public void onDeath(DamageSource cause) {
            super.onDeath(cause);
            if (!world.isRemote && getSaddled()) dropItem(Items.SADDLE, 1);
        }

        @Override
        public void onStruckByLightning(EntityLightningBolt lightningBolt) {
            if (world.isRemote || isDead) return;
            EntityPigZombie pigZombie = new EntityPigZombie(world);
            pigZombie.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            pigZombie.setLocationAndAngles(posX, posY, posZ, rotationYaw, rotationPitch);
            pigZombie.setNoAI(isAIDisabled());
            if (hasCustomName()) {
                pigZombie.setCustomNameTag(getCustomNameTag());
                pigZombie.setAlwaysRenderNameTag(getAlwaysRenderNameTag());
            }
            world.spawnEntity(pigZombie);
            setDead();
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setBoolean("Saddle", getSaddled());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            setSaddled(compound.getBoolean("Saddle"));
        }

        @Override protected boolean canDespawn() { return false; }
    }

    public static class SickenedPillagerEntity extends SickenedMobEntity implements IRangedAttackMob {
        public SickenedPillagerEntity(World world) {
            super(world);
            setSize(0.6F, 1.95F);
            setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
        @Override protected double getSickenedHealth() { return 30.0D; }
        @Override protected double getSickenedSpeed() { return 0.37D; }
        @Override protected double getSickenedDamage() { return 6.0D; }
        @Override protected double getSickenedFollowRange() { return 48.0D; }
        @Override protected boolean usesAlternateVoicePitch() { return true; }
        @Override public String getSickenedType() { return "sickened_pillager"; }

        @Override
        protected void initEntityAI() {
            tasks.addTask(1, new EntityAISwimming(this));
            tasks.addTask(2, new EntityAIAttackRangedBow<SickenedPillagerEntity>(this, 1.0D, 20, 15.0F));
            tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.8D));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            tasks.addTask(6, new EntityAILookIdle(this));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(3);
        }

        @Override
        public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
            fireSickenedArrow(this, target, distanceFactor);
        }

        @Override public void setSwingingArms(boolean swingingArms) { }
    }

    public static class SickenedSkeletonEntity extends SickenedMobEntity implements IRangedAttackMob {
        private static final DataParameter<Boolean> SWINGING_ARMS = EntityDataManager.createKey(
                SickenedSkeletonEntity.class, DataSerializers.BOOLEAN);
        private EntityAIAttackRangedBow<SickenedSkeletonEntity> rangedAttackGoal;
        private EntityAIAttackMelee meleeAttackGoal;

        public SickenedSkeletonEntity(World world) {
            super(world);
            setSize(0.6F, 1.99F);
            setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            setCombatTask();
        }
        @Override protected double getSickenedHealth() { return 24.0D; }
        @Override protected double getSickenedSpeed() { return 0.28D; }
        @Override public String getSickenedType() { return "sickened_skeleton"; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(SWINGING_ARMS, false);
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(1, new EntityAISwimming(this));
            tasks.addTask(2, new EntityAIRestrictSun(this));
            tasks.addTask(3, new EntityAIFleeSun(this, 1.0D));
            tasks.addTask(3, new EntityAIAvoidEntity<EntityWolf>(this,
                    EntityWolf.class, 6.0F, 1.0D, 1.2D));
            tasks.addTask(5, new EntityAIWanderAvoidWater(this, 1.0D));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            tasks.addTask(6, new EntityAILookIdle(this));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(3);
            rangedAttackGoal = new EntityAIAttackRangedBow<SickenedSkeletonEntity>(
                    this, 1.0D, 20, 15.0F);
            meleeAttackGoal = new EntityAIAttackMelee(this, 1.2D, false);
        }

        private void setCombatTask() {
            if (world == null || world.isRemote) return;
            // EntityLiving invokes initEntityAI from its constructor, before this
            // subclass's fields are initialized. Recreate the retained goals here.
            if (rangedAttackGoal == null) {
                rangedAttackGoal = new EntityAIAttackRangedBow<SickenedSkeletonEntity>(
                        this, 1.0D, 20, 15.0F);
            }
            if (meleeAttackGoal == null) {
                meleeAttackGoal = new EntityAIAttackMelee(this, 1.2D, false);
            }
            tasks.removeTask(rangedAttackGoal);
            tasks.removeTask(meleeAttackGoal);
            if (getHeldItemMainhand().getItem() instanceof ItemBow) {
                rangedAttackGoal.setAttackCooldown(world.getDifficulty() == EnumDifficulty.HARD ? 20 : 40);
                tasks.addTask(4, rangedAttackGoal);
            } else {
                tasks.addTask(4, meleeAttackGoal);
            }
        }

        @Override
        public void setItemStackToSlot(EntityEquipmentSlot slot, ItemStack stack) {
            super.setItemStackToSlot(slot, stack);
            if (slot == EntityEquipmentSlot.MAINHAND) setCombatTask();
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            setCombatTask();
        }

        @Override
        public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
            fireSickenedArrow(this, target, distanceFactor, 0.25F);
        }

        @Override
        public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty,
                                                @Nullable IEntityLivingData livingData) {
            IEntityLivingData result = super.onInitialSpawn(difficulty, livingData);
            initializeTaintedPumpkinHead(this);
            return result;
        }

        public boolean isSwingingArms() {
            return dataManager.get(SWINGING_ARMS);
        }

        @Override
        public void setSwingingArms(boolean swingingArms) {
            dataManager.set(SWINGING_ARMS, swingingArms);
        }
    }

    public static class SickenedSnowGolemEntity extends SickenedMobEntity implements IRangedAttackMob, IShearable {
        private static final DataParameter<Boolean> PUMPKIN = EntityDataManager.createKey(
                SickenedSnowGolemEntity.class, DataSerializers.BOOLEAN);

        public SickenedSnowGolemEntity(World world) { super(world); setSize(0.7F, 1.9F); }
        @Override protected double getSickenedHealth() { return 8.0D; }
        @Override protected double getSickenedSpeed() { return 0.24D; }
        @Override protected int getSickenedExperience() { return 0; }
        @Override public String getSickenedType() { return "sickened_snow_golem"; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(PUMPKIN, true);
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(1, new EntityAIAttackRanged(this, 1.25D, 12, 10.0F));
            tasks.addTask(2, new EntityAIWanderAvoidWater(this, 1.0D));
            tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
            tasks.addTask(4, new EntityAILookIdle(this));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(3);
        }

        @Override
        public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
            SickenedSnowball snowball = new SickenedSnowball(world, this, rand.nextFloat() < 0.1F);
            double targetY = target.posY + target.getEyeHeight() - 1.1D;
            double dx = target.posX - posX;
            double dy = targetY - snowball.posY;
            double dz = target.posZ - posZ;
            double arc = MathHelper.sqrt(dx * dx + dz * dz) * 0.2D;
            snowball.shoot(dx, dy + arc, dz, 1.6F, 12.0F);
            playSound(SoundEvents.ENTITY_SNOWMAN_SHOOT, 1.0F, 0.4F / (rand.nextFloat() * 0.4F + 0.8F));
            world.spawnEntity(snowball);
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            if (world.isRemote) return;
            BlockPos currentPos = new BlockPos(this);
            if (world.getBiome(currentPos).getTemperature(currentPos) > 1.0F) {
                attackEntityFrom(DamageSource.ON_FIRE, 1.0F);
            }
            if (!ForgeEventFactory.getMobGriefingEvent(world, this)) return;

            for (int corner = 0; corner < 4; corner++) {
                int x = MathHelper.floor(posX + (corner % 2 * 2 - 1) * 0.25F);
                int y = MathHelper.floor(posY);
                int z = MathHelper.floor(posZ + (corner / 2 % 2 * 2 - 1) * 0.25F);
                BlockPos snowPos = new BlockPos(x, y, z);
                IBlockState state = world.getBlockState(snowPos);
                if (state.getBlock().isAir(state, world, snowPos)
                        && world.getBiome(snowPos).getTemperature(snowPos) < 0.8F
                        && Blocks.SNOW_LAYER.canPlaceBlockAt(world, snowPos)) {
                    world.setBlockState(snowPos, Blocks.SNOW_LAYER.getDefaultState());
                }
            }
        }

        public boolean isPumpkinEquipped() {
            return dataManager.get(PUMPKIN);
        }

        public void setPumpkinEquipped(boolean equipped) {
            dataManager.set(PUMPKIN, equipped);
        }

        @Override
        public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
            return isPumpkinEquipped();
        }

        @Override
        public List<ItemStack> onSheared(ItemStack item, IBlockAccess blockAccess, BlockPos pos, int fortune) {
            if (!world.isRemote) {
                setPumpkinEquipped(false);
                playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1.0F, 1.0F);
            }
            return Collections.singletonList(new ItemStack(ModBlocks.get("tainted_carved_pumpkin")));
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setBoolean("Pumpkin", isPumpkinEquipped());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            if (compound.hasKey("Pumpkin", 1)) {
                setPumpkinEquipped(compound.getBoolean("Pumpkin"));
            }
        }

        @Override public void setSwingingArms(boolean swingingArms) { }
        @Override protected boolean canDespawn() { return false; }
    }

    public static class SickenedSpiderEntity extends SickenedMobEntity {
        private static final DataParameter<Byte> CLIMBING = EntityDataManager.createKey(
                SickenedSpiderEntity.class, DataSerializers.BYTE);

        public SickenedSpiderEntity(World world) { super(world); setSize(1.6F, 1.1F); }
        @Override protected double getSickenedHealth() { return 20.0D; }
        @Override protected double getSickenedSpeed() { return 0.34D; }
        @Override protected double getSickenedDamage() { return 3.0D; }
        @Override protected double getSickenedFollowRange() { return 32.0D; }
        @Override public String getSickenedType() { return "sickened_spider"; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(CLIMBING, (byte) 0);
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(1, new EntityAISwimming(this));
            tasks.addTask(3, new EntityAILeapAtTarget(this, 0.45F));
            tasks.addTask(4, new SickenedSpiderAttackGoal(this));
            tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.8D));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            tasks.addTask(6, new EntityAILookIdle(this));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(3);
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (!world.isRemote) setBesideClimbableBlock(collidedHorizontally);
        }

        @Override
        public boolean isOnLadder() {
            return isBesideClimbableBlock();
        }

        public boolean isBesideClimbableBlock() {
            return (dataManager.get(CLIMBING) & 1) != 0;
        }

        public void setBesideClimbableBlock(boolean climbing) {
            byte state = dataManager.get(CLIMBING);
            state = climbing ? (byte) (state | 1) : (byte) (state & -2);
            dataManager.set(CLIMBING, state);
        }

        @Override
        public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty,
                                                @Nullable IEntityLivingData livingData) {
            if (rand.nextInt(100) == 0) {
                SickenedSkeletonEntity skeleton = new SickenedSkeletonEntity(world);
                skeleton.setLocationAndAngles(posX, posY, posZ, rotationYaw, 0.0F);
                skeleton.onInitialSpawn(difficulty, null);
                if (world.spawnEntity(skeleton)) skeleton.startRiding(this);
            }

            SickenedSpiderGroupData groupData = livingData instanceof SickenedSpiderGroupData
                    ? (SickenedSpiderGroupData) livingData
                    : new SickenedSpiderGroupData();
            if (livingData == null && world.getDifficulty() == EnumDifficulty.HARD
                    && rand.nextFloat() < 0.1F * difficulty.getClampedAdditionalDifficulty()) {
                groupData.setRandomEffect(rand);
            }
            if (groupData.effect != null) {
                addPotionEffect(new PotionEffect(groupData.effect, Integer.MAX_VALUE));
            }
            return groupData;
        }

        private static final class SickenedSpiderAttackGoal extends EntityAIAttackMelee {
            private final SickenedSpiderEntity spider;

            private SickenedSpiderAttackGoal(SickenedSpiderEntity spider) {
                super(spider, 1.0D, true);
                this.spider = spider;
            }

            @Override
            public boolean shouldExecute() {
                return super.shouldExecute() && !spider.isBeingRidden();
            }

            @Override
            protected double getAttackReachSqr(EntityLivingBase target) {
                return 4.0F + target.width;
            }
        }

        private static final class SickenedSpiderGroupData implements IEntityLivingData {
            private Potion effect;

            private void setRandomEffect(Random random) {
                int selection = random.nextInt(5);
                effect = selection <= 1 ? MobEffects.SPEED
                        : selection == 2 ? MobEffects.STRENGTH
                        : selection == 3 ? MobEffects.REGENERATION
                        : MobEffects.INVISIBILITY;
            }
        }
    }

    public static class SickenedVillagerEntity extends SickenedZombieEntity {
        private static final DataParameter<Integer> PROFESSION = EntityDataManager.createKey(
                SickenedVillagerEntity.class, DataSerializers.VARINT);
        private static final DataParameter<String> PROFESSION_SKIN = EntityDataManager.createKey(
                SickenedVillagerEntity.class, DataSerializers.STRING);

        public SickenedVillagerEntity(World world) { super(world); setSize(0.6F, 1.95F); }
        @Override public String getSickenedType() { return "sickened_villager"; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(PROFESSION, 0);
            dataManager.register(PROFESSION_SKIN,
                    "minecraft:textures/entity/zombie_villager/zombie_farmer.png");
        }

        public int getProfession() {
            return dataManager.get(PROFESSION);
        }

        public ResourceLocation getProfessionSkin() {
            return new ResourceLocation(dataManager.get(PROFESSION_SKIN));
        }

        @Override
        public void copySpeciesDataFrom(EntityLivingBase original) {
            if (original instanceof EntityVillager) {
                EntityVillager villager = (EntityVillager) original;
                setProfession(villager.getProfession(),
                        villager.getProfessionForge().getZombieSkin());
            } else if (original instanceof EntityZombieVillager) {
                EntityZombieVillager villager = (EntityZombieVillager) original;
                setProfession(villager.getProfession(),
                        villager.getForgeProfession().getZombieSkin());
            }
        }

        @Override
        public void copySpeciesDataTo(EntityLivingBase cured) {
            if (cured instanceof EntityVillager) {
                EntityVillager villager = (EntityVillager) cured;
                villager.setProfession(getProfession());
                addCuredTrades(villager);
            } else if (cured instanceof EntityZombieVillager) {
                ((EntityZombieVillager) cured).setProfession(getProfession());
            }
        }

        private void addCuredTrades(EntityVillager villager) {
            ResourceLocation profession = villager.getProfessionForge().getRegistryName();
            if (profession != null && "minecraft".equals(profession.getNamespace())
                    && "nitwit".equals(profession.getPath())) return;

            MerchantRecipeList offers = villager.getRecipes(null);
            addTrade(offers, 1, ModItems.get("withered_flesh"), 16, 5);
            addTrade(offers, 1, ModItems.get("withered_bone"), 16, 5);
            addTrade(offers, 1, ModItems.get("withered_spider_eye"), 16, 5);

            String professionPath = profession == null ? "" : profession.getPath();
            if ("priest".equals(professionPath)) {
                addTrade(offers, 1, ModItems.get("tainted_dust"), 12, 2);
            } else if ("farmer".equals(professionPath) && isFarmerCareer(villager)) {
                addTrade(offers, 16, ModItems.get("golden_apple_stew"), 1, 30);
            } else if ("mason".equals(professionPath)) {
                addTrade(offers, 2, ModItems.get("tainted_zombie_sitting"), 12, 20);
                addTrade(offers, 2, ModItems.get("tainted_zombie_wall"), 12, 20);
                addTrade(offers, 2, ModItems.get("tainted_zombie_lying"), 12, 20);
                addTrade(offers, 2, ModItems.get("tainted_bone_pile"), 12, 20);
                addTrade(offers, 2, ModItems.get("tainted_skeleton_wall"), 12, 20);
                addTrade(offers, 2, ModItems.get("tainted_skull_ceiling"), 12, 20);
            }
            villager.setRecipes(offers);
        }

        private static boolean isFarmerCareer(EntityVillager villager) {
            NBTTagCompound data = new NBTTagCompound();
            villager.writeEntityToNBT(data);
            return !data.hasKey("Career", 3) || data.getInteger("Career") <= 1;
        }

        private static void addTrade(MerchantRecipeList offers, int emeraldCost,
                                     Item result, int resultCount, int maxUses) {
            if (result == null) return;
            offers.add(new MerchantRecipe(new ItemStack(Items.EMERALD, emeraldCost),
                    ItemStack.EMPTY, new ItemStack(result, resultCount), 0, maxUses));
        }

        private void setProfession(int profession, ResourceLocation skin) {
            dataManager.set(PROFESSION, profession);
            dataManager.set(PROFESSION_SKIN, skin.toString());
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setInteger("Profession", getProfession());
            compound.setString("ProfessionSkin", getProfessionSkin().toString());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            int profession = compound.getInteger("Profession");
            ResourceLocation skin = compound.hasKey("ProfessionSkin", 8)
                    ? new ResourceLocation(compound.getString("ProfessionSkin"))
                    : defaultZombieProfessionSkin(profession);
            setProfession(profession, skin);
        }

        private static ResourceLocation defaultZombieProfessionSkin(int profession) {
            String[] names = {"farmer", "librarian", "priest", "smith", "butcher"};
            String name = names[MathHelper.clamp(profession, 0, names.length - 1)];
            return new ResourceLocation("minecraft",
                    "textures/entity/zombie_villager/zombie_" + name + ".png");
        }
    }

    public static class SickenedVindicatorEntity extends SickenedMobEntity {
        private static final DataParameter<Boolean> AGGRESSIVE = EntityDataManager.createKey(
                SickenedVindicatorEntity.class, DataSerializers.BOOLEAN);
        private boolean johnny;

        public SickenedVindicatorEntity(World world) { super(world); setSize(0.6F, 1.95F); }
        @Override protected double getSickenedHealth() { return 30.0D; }
        @Override protected double getSickenedSpeed() { return 0.35D; }
        @Override protected double getSickenedDamage() { return 6.0D; }
        @Override protected double getSickenedFollowRange() { return 48.0D; }
        @Override protected boolean usesAlternateVoicePitch() { return true; }
        @Override public String getSickenedType() { return "sickened_vindicator"; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(AGGRESSIVE, false);
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(4, new EntityAIAttackMelee(this, 1.0D, false) {
                @Override
                public void startExecuting() {
                    super.startExecuting();
                    SickenedVindicatorEntity.this.setAggressive(true);
                }

                @Override
                public void resetTask() {
                    super.resetTask();
                    SickenedVindicatorEntity.this.setAggressive(false);
                }
            });
            tasks.addTask(8, new EntityAIWander(this, 0.6D));
            tasks.addTask(9, new EntityAIWatchClosest(this, EntityPlayer.class, 3.0F, 1.0F));
            tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, true,
                    SickenedVindicatorEntity.class));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(
                    this, EntityPlayer.class, true));
            targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityVillager>(
                    this, EntityVillager.class, true));
            targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityIronGolem>(
                    this, EntityIronGolem.class, true));
            addSickenedMobTargetGoal(3);
            targetTasks.addTask(4, new EntityAINearestAttackableTarget<EntityLivingBase>(
                    this, EntityLivingBase.class, 0, true, true,
                    target -> johnny && target != null && target.attackable()
                            && !(target instanceof SickenedMobEntity)));
        }

        @Override
        protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
            setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }

        @Override
        public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty,
                                                @Nullable IEntityLivingData livingData) {
            IEntityLivingData result = super.onInitialSpawn(difficulty, livingData);
            setEquipmentBasedOnDifficulty(difficulty);
            setEnchantmentBasedOnDifficulty(difficulty);
            return result;
        }

        public boolean isAggressive() {
            return dataManager.get(AGGRESSIVE);
        }

        private void setAggressive(boolean aggressive) {
            dataManager.set(AGGRESSIVE, aggressive);
        }

        @Override
        public void setCustomNameTag(String name) {
            super.setCustomNameTag(name);
            if (!johnny && "Johnny".equals(name)) johnny = true;
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            if (johnny) compound.setBoolean("Johnny", true);
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            johnny = compound.getBoolean("Johnny") || "Johnny".equals(getCustomNameTag());
        }
    }

    public static class SickenedWolfEntity extends SickenedTameableMob {
        private static final DataParameter<Boolean> BEGGING = EntityDataManager.createKey(
                SickenedWolfEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Integer> COLLAR_COLOR = EntityDataManager.createKey(
                SickenedWolfEntity.class, DataSerializers.VARINT);
        private float headRotationCourse;
        private float previousHeadRotationCourse;
        private boolean wet;
        private boolean shaking;
        private float shakeTime;
        private float previousShakeTime;

        public SickenedWolfEntity(World world) { super(world); setSize(0.6F, 0.85F); }
        @Override protected double getSickenedHealth() { return 18.0D; }
        @Override protected double getSickenedSpeed() { return 0.3D; }
        @Override protected double getSickenedDamage() { return 3.0D; }
        @Override protected double getSickenedFollowRange() { return 24.0D; }
        @Override protected boolean usesAlternateVoicePitch() { return true; }
        @Override public String getSickenedType() { return "sickened_wolf"; }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(BEGGING, false);
            dataManager.register(COLLAR_COLOR, EnumDyeColor.RED.getDyeDamage());
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(1, new EntityAILeapAtTarget(this, 0.4F));
            tasks.addTask(2, new EntityAIAttackMelee(this, 1.125D, false));
            tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            tasks.addTask(4, new EntityAIWanderAvoidWater(this, 1.0D));
            tasks.addTask(5, new EntityAILookIdle(this));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(3);
        }

        public boolean isSickenedAngry() {
            return true;
        }

        public EnumDyeColor getCollarColor() {
            return EnumDyeColor.byDyeDamage(dataManager.get(COLLAR_COLOR));
        }

        public float getInterestedAngle(float partialTicks) {
            return (previousHeadRotationCourse
                    + (headRotationCourse - previousHeadRotationCourse) * partialTicks)
                    * 0.15F * (float) Math.PI;
        }

        public float getShakeAngle(float partialTicks, float offset) {
            float progress = (previousShakeTime
                    + (shakeTime - previousShakeTime) * partialTicks + offset) / 1.8F;
            progress = MathHelper.clamp(progress, 0.0F, 1.0F);
            return MathHelper.sin(progress * (float) Math.PI)
                    * MathHelper.sin(progress * (float) Math.PI * 11.0F)
                    * 0.15F * (float) Math.PI;
        }

        public float getTailRotation() {
            if (isSickenedAngry()) return 1.5393804F;
            return isSickenedTamed()
                    ? (0.55F - (getMaxHealth() - getHealth()) * 0.02F) * (float) Math.PI
                    : 0.62831855F;
        }

        public boolean isWolfWet() {
            return wet;
        }

        public float getShadingWhileWet(float partialTicks) {
            return 0.75F + (previousShakeTime
                    + (shakeTime - previousShakeTime) * partialTicks) / 2.0F * 0.25F;
        }

        public void setBegging(boolean begging) {
            dataManager.set(BEGGING, begging);
        }

        public boolean isBegging() {
            return dataManager.get(BEGGING);
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            if (!world.isRemote && wet && !shaking && getNavigator().noPath() && onGround) {
                shaking = true;
                shakeTime = 0.0F;
                previousShakeTime = 0.0F;
                world.setEntityState(this, (byte) 8);
            }
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            previousHeadRotationCourse = headRotationCourse;
            headRotationCourse += ((isBegging() ? 1.0F : 0.0F) - headRotationCourse) * 0.4F;
            if (isWet()) {
                wet = true;
                shaking = false;
                shakeTime = 0.0F;
                previousShakeTime = 0.0F;
            } else if (shaking) {
                if (shakeTime == 0.0F) {
                    playSound(SoundEvents.ENTITY_WOLF_SHAKE, getSoundVolume(),
                            (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
                }
                previousShakeTime = shakeTime;
                shakeTime += 0.05F;
                if (previousShakeTime >= 2.0F) {
                    wet = false;
                    shaking = false;
                    previousShakeTime = 0.0F;
                    shakeTime = 0.0F;
                }
                if (shakeTime > 0.4F) {
                    float y = (float) getEntityBoundingBox().minY;
                    int particles = (int) (MathHelper.sin((shakeTime - 0.4F)
                            * (float) Math.PI) * 7.0F);
                    for (int index = 0; index < particles; index++) {
                        float offsetX = (rand.nextFloat() * 2.0F - 1.0F) * width * 0.5F;
                        float offsetZ = (rand.nextFloat() * 2.0F - 1.0F) * width * 0.5F;
                        world.spawnParticle(EnumParticleTypes.WATER_SPLASH,
                                posX + offsetX, y + 0.8F, posZ + offsetZ,
                                motionX, motionY, motionZ);
                    }
                }
            }
        }

        @Override
        public void handleStatusUpdate(byte id) {
            if (id == 8) {
                shaking = true;
                shakeTime = 0.0F;
                previousShakeTime = 0.0F;
            } else {
                super.handleStatusUpdate(id);
            }
        }

        @Override
        protected float getSoundVolume() {
            return 0.4F;
        }

        @Override
        public void copySpeciesDataFrom(EntityLivingBase original) {
            super.copySpeciesDataFrom(original);
            if (original instanceof net.minecraft.entity.passive.EntityWolf) {
                dataManager.set(COLLAR_COLOR,
                        ((net.minecraft.entity.passive.EntityWolf) original)
                                .getCollarColor().getDyeDamage());
            }
        }

        @Override
        public void copySpeciesDataTo(EntityLivingBase cured) {
            super.copySpeciesDataTo(cured);
            if (cured instanceof net.minecraft.entity.passive.EntityWolf) {
                ((net.minecraft.entity.passive.EntityWolf) cured).setCollarColor(getCollarColor());
            }
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setByte("CollarColor", (byte) getCollarColor().getDyeDamage());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            if (compound.hasKey("CollarColor", 99)) {
                dataManager.set(COLLAR_COLOR,
                        EnumDyeColor.byDyeDamage(compound.getByte("CollarColor")).getDyeDamage());
            }
        }

        @Override protected boolean canDespawn() { return false; }
    }

    public static class SickenedZombieEntity extends SickenedMobEntity implements IRangedAttackMob {
        private static final IAttribute SPAWN_REINFORCEMENTS_CHANCE = new RangedAttribute(
                null, "zombie.spawnReinforcements", 0.0D, 0.0D, 1.0D)
                .setDescription("Spawn Reinforcements Chance");
        private static final UUID BABY_SPEED_BOOST_ID = UUID.fromString(
                "B9766B59-9566-4402-BC1F-2EE2A276D836");
        private static final AttributeModifier BABY_SPEED_BOOST = new AttributeModifier(
                BABY_SPEED_BOOST_ID, "Baby speed boost", 0.5D, 1);

        private EntityAIBreakDoor breakDoorGoal;
        private boolean breakDoorsTaskSet;

        public SickenedZombieEntity(World world) { super(world); setSize(0.6F, 1.95F); }
        @Override protected double getSickenedHealth() { return 24.0D; }
        @Override protected double getSickenedSpeed() { return 0.28D; }
        @Override protected double getSickenedDamage() { return 3.5D; }
        @Override protected double getSickenedFollowRange() { return 48.0D; }
        @Override protected double getSickenedArmor() { return 2.2D; }
        @Override public String getSickenedType() { return "sickened_zombie"; }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getAttributeMap().registerAttribute(SPAWN_REINFORCEMENTS_CHANCE)
                    .setBaseValue(rand.nextDouble() * ForgeModContainer.zombieSummonBaseChance);
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(2, new EntityAIAttackMelee(this, 1.0D, false));
            tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0D));
            tasks.addTask(6, new EntityAIMoveThroughVillage(this, 1.0D, false));
            tasks.addTask(7, new EntityAIWanderAvoidWater(this, 1.0D));
            tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            tasks.addTask(8, new EntityAILookIdle(this));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(
                    this, EntityPlayer.class, true));
            addSickenedMobTargetGoal(3);
            breakDoorGoal = new EntityAIBreakDoor(this);
            tasks.addTask(2, new SickenedZombieTridentAttackGoal(this));
        }

        public void setBreakDoorsAItask(boolean enabled) {
            if (breakDoorGoal == null || breakDoorsTaskSet == enabled
                    || !(getNavigator() instanceof PathNavigateGround)) return;
            breakDoorsTaskSet = enabled;
            ((PathNavigateGround) getNavigator()).setBreakDoors(enabled);
            if (enabled) tasks.addTask(1, breakDoorGoal);
            else tasks.removeTask(breakDoorGoal);
        }

        @Override
        public void setSickenedChild(boolean child) {
            super.setSickenedChild(child);
            if (world == null || world.isRemote) return;
            IAttributeInstance movement = getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
            AttributeModifier existing = movement.getModifier(BABY_SPEED_BOOST_ID);
            if (existing != null) movement.removeModifier(existing);
            if (child) movement.applyModifier(BABY_SPEED_BOOST);
        }

        @Override
        protected int getExperiencePoints(EntityPlayer player) {
            if (isChild()) experienceValue = (int) (experienceValue * 2.5F);
            return super.getExperiencePoints(player);
        }

        @Override
        public float getEyeHeight() {
            return isChild() ? 0.93F : 1.74F;
        }

        @Override
        public boolean attackEntityAsMob(Entity target) {
            boolean attacked = super.attackEntityAsMob(target);
            if (attacked && getHeldItemMainhand().isEmpty() && isBurning()) {
                float difficulty = world.getDifficultyForLocation(new BlockPos(this))
                        .getAdditionalDifficulty();
                if (rand.nextFloat() < difficulty * 0.3F) target.setFire(2 * (int) difficulty);
            }
            return attacked;
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (!super.attackEntityFrom(source, amount)) return false;
            trySpawnReinforcement(source);
            return true;
        }

        private void trySpawnReinforcement(DamageSource source) {
            if (world.isRemote || world.getDifficulty() != EnumDifficulty.HARD
                    || !world.getGameRules().getBoolean("doMobSpawning")) return;
            EntityLivingBase target = getAttackTarget();
            if (target == null && source.getTrueSource() instanceof EntityLivingBase) {
                target = (EntityLivingBase) source.getTrueSource();
            }
            IAttributeInstance reinforcement = getEntityAttribute(SPAWN_REINFORCEMENTS_CHANCE);
            if (target == null || rand.nextFloat() >= reinforcement.getAttributeValue()) return;

            BlockPos origin = new BlockPos(this);
            for (int attempt = 0; attempt < 50; ++attempt) {
                int x = origin.getX() + MathHelper.getInt(rand, 7, 40)
                        * MathHelper.getInt(rand, -1, 1);
                int y = origin.getY() + MathHelper.getInt(rand, 7, 40)
                        * MathHelper.getInt(rand, -1, 1);
                int z = origin.getZ() + MathHelper.getInt(rand, 7, 40)
                        * MathHelper.getInt(rand, -1, 1);
                BlockPos spawnPosition = new BlockPos(x, y, z);
                if (!world.getBlockState(spawnPosition.down()).isSideSolid(
                        world, spawnPosition.down(), EnumFacing.UP)
                        || world.getLightFromNeighbors(spawnPosition) >= 10) continue;

                SickenedZombieEntity summoned = new SickenedZombieEntity(world);
                summoned.setPosition(x, y, z);
                if (world.isAnyPlayerWithinRangeAt(x, y, z, 7.0D)
                        || !world.checkNoEntityCollision(summoned.getEntityBoundingBox(), summoned)
                        || !world.getCollisionBoxes(summoned, summoned.getEntityBoundingBox()).isEmpty()
                        || world.containsAnyLiquid(summoned.getEntityBoundingBox())) continue;
                summoned.setAttackTarget(target);
                summoned.onInitialSpawn(world.getDifficultyForLocation(spawnPosition), null);
                if (!world.spawnEntity(summoned)) continue;
                reinforcement.applyModifier(new AttributeModifier(
                        "Zombie reinforcement caller charge", -0.05D, 0));
                summoned.getEntityAttribute(SPAWN_REINFORCEMENTS_CHANCE).applyModifier(
                        new AttributeModifier("Zombie reinforcement callee charge", -0.05D, 0));
                break;
            }
        }

        @Override
        protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
            super.setEquipmentBasedOnDifficulty(difficulty);
            float equipmentChance = world.getDifficulty() == EnumDifficulty.HARD ? 0.05F : 0.01F;
            if (rand.nextFloat() < equipmentChance) {
                int selection = rand.nextInt(4);
                if (selection == 0) {
                    setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                } else if (selection < 4) {
                    setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
                } else if (selection == 4) {
                    setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
                }
            }
        }

        @Override
        public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty,
                                                @Nullable IEntityLivingData livingData) {
            IEntityLivingData result = super.onInitialSpawn(difficulty, livingData);
            SickenedZombieGroupData groupData = result instanceof SickenedZombieGroupData
                    ? (SickenedZombieGroupData) result
                    : new SickenedZombieGroupData(world.rand.nextFloat()
                    < ForgeModContainer.zombieBabyChance);
            setSickenedChild(groupData.child);
            float localDifficulty = difficulty.getClampedAdditionalDifficulty();
            setBreakDoorsAItask(rand.nextFloat() < localDifficulty * 0.1F);
            initializeTaintedPumpkinHead(this);
            if (!isChild()) {
                applyZombieSpawnBonuses(localDifficulty);
                return groupData;
            }

            if (rand.nextFloat() < 0.05F) {
                List<SickenedChickenEntity> nearby = world.getEntitiesWithinAABB(
                        SickenedChickenEntity.class,
                        getEntityBoundingBox().grow(5.0D, 3.0D, 5.0D),
                        chicken -> chicken != null && chicken.isEntityAlive() && !chicken.isBeingRidden());
                if (!nearby.isEmpty()) {
                    SickenedChickenEntity chicken = nearby.get(0);
                    chicken.setChickenJockey(true);
                    startRiding(chicken);
                }
            } else if (rand.nextFloat() < 0.05F) {
                SickenedChickenEntity chicken = new SickenedChickenEntity(world);
                chicken.setLocationAndAngles(posX, posY, posZ, rotationYaw, 0.0F);
                chicken.onInitialSpawn(difficulty, null);
                chicken.setChickenJockey(true);
                if (world.spawnEntity(chicken)) startRiding(chicken);
            }
            applyZombieSpawnBonuses(localDifficulty);
            return groupData;
        }

        private void applyZombieSpawnBonuses(float localDifficulty) {
            getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).applyModifier(
                    new AttributeModifier("Random spawn bonus", rand.nextDouble() * 0.05D, 0));
            double followRangeBonus = rand.nextDouble() * 1.5D * localDifficulty;
            if (followRangeBonus > 1.0D) {
                getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).applyModifier(
                        new AttributeModifier("Random zombie-spawn bonus", followRangeBonus, 2));
            }
            if (rand.nextFloat() < localDifficulty * 0.05F) {
                getEntityAttribute(SPAWN_REINFORCEMENTS_CHANCE).applyModifier(
                        new AttributeModifier("Leader zombie bonus",
                                rand.nextDouble() * 0.25D + 0.5D, 0));
                getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).applyModifier(
                        new AttributeModifier("Leader zombie bonus",
                                rand.nextDouble() * 3.0D + 1.0D, 2));
                setBreakDoorsAItask(true);
            }
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setBoolean("CanBreakDoors", breakDoorsTaskSet);
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            setBreakDoorsAItask(compound.getBoolean("CanBreakDoors"));
        }

        @Override
        public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
            Item tridentItem = getFutureTrident();
            if (tridentItem == null) return;
            Trident trident = new Trident(world, this, new ItemStack(tridentItem));
            double dx = target.posX - posX;
            double dy = target.posY + target.height / 3.0D - trident.posY;
            double dz = target.posZ - posZ;
            double horizontalDistance = MathHelper.sqrt(dx * dx + dz * dz);
            trident.shoot(dx, dy + horizontalDistance * 0.2D, dz, 1.6F,
                    14 - world.getDifficulty().getId() * 4);
            swingArm(EnumHand.MAIN_HAND);
            playSound(FSounds.INSTANCE.getTRIDENT_THROW(), 1.0F,
                    1.0F / (rand.nextFloat() * 0.4F + 0.8F));
            world.spawnEntity(trident);
        }

        @Override
        public void setSwingingArms(boolean swingingArms) { }

        private static final class SickenedZombieTridentAttackGoal extends EntityAIAttackRanged {
            private final SickenedZombieEntity zombie;

            private SickenedZombieTridentAttackGoal(SickenedZombieEntity zombie) {
                super(zombie, 1.0D, 40, 10.0F);
                this.zombie = zombie;
            }

            @Override
            public boolean shouldExecute() {
                return hasTrident() && super.shouldExecute();
            }

            @Override
            public boolean shouldContinueExecuting() {
                return hasTrident() && super.shouldContinueExecuting();
            }

            private boolean hasTrident() {
                Item trident = getFutureTrident();
                return trident != null && zombie.getHeldItemMainhand().getItem() == trident;
            }
        }

        private static final class SickenedZombieGroupData implements IEntityLivingData {
            private final boolean child;

            private SickenedZombieGroupData(boolean child) {
                this.child = child;
            }
        }
    }

    public static class TentacleEntity extends SickenedMobEntity implements IEntityMultiPart {
        private static final DataParameter<Boolean> DORMANT = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> CURLING = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> CAN_SWING = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> CAN_STRANGLE = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Integer> ANIMATION_OFFSET = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Float> X_OFFSET = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> Y_OFFSET = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Integer> OFFSET_STEPS = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Boolean> SHOULD_WRAP_Y = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Float> X_OFFSET_ANIMATION = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> Y_OFFSET_ANIMATION = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> X_CURL = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> Y_CURL = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Integer> CURL_STEPS = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Float> X_CURL_ANIMATION = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> Y_CURL_ANIMATION = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.FLOAT);
        private static final DataParameter<Float> LAST_X_CURL_ANIMATION = EntityDataManager.createKey(TentacleEntity.class, DataSerializers.FLOAT);
        private final TentaclePart[] multipartParts;
        private int tentacleAnimation;
        private float xOffsetAnimation;
        private float yOffsetAnimation;
        private float xCurlAnimation;
        private float yCurlAnimation;
        private int awakeTicks;
        private boolean indefinitelyAwake;
        private int swingTicks;
        private int nextSwing;
        private int strangleTicks;
        private int knockbackWait;
        private double attackKnockback = 1.5D;
        private double curlX;
        private double curlY;
        private double curlZ;
        private UUID commandBlockStructureOwner;
        private int commandBlockStructureIndex = -1;
        private int missingCommandBlockStructureOwnerTicks;

        public TentacleEntity(World world) {
            super(world);
            setSize(7.5F, 9.5F);
            multipartParts = new TentaclePart[] {
                    new TentaclePart(this, "segment_0", 1.5F, 1.5F, 0),
                    new TentaclePart(this, "segment_1", 1.5F, 2.0F, 1),
                    new TentaclePart(this, "segment_2", 1.5F, 2.5F, 2),
                    new TentaclePart(this, "segment_3", 1.5F, 3.0F, 3),
                    new TentaclePart(this, "segment_4", 1.5F, 3.0F, 4)
            };
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new DormantGoal(this));
            tasks.addTask(1, new SwingGoal(this));
            tasks.addTask(2, new StrangleGoal(this));
            targetTasks.addTask(0, new TentacleTargetGoal<EntityPlayer>(this, EntityPlayer.class));
            targetTasks.addTask(1, new TentacleTargetGoal<EntityAnimal>(this, EntityAnimal.class));
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(DORMANT, false);
            dataManager.register(CURLING, false);
            dataManager.register(CAN_SWING, true);
            dataManager.register(CAN_STRANGLE, true);
            dataManager.register(ANIMATION_OFFSET, 0);
            dataManager.register(X_OFFSET, 20.0F);
            dataManager.register(Y_OFFSET, 0.0F);
            dataManager.register(OFFSET_STEPS, 0);
            dataManager.register(SHOULD_WRAP_Y, true);
            dataManager.register(X_OFFSET_ANIMATION, 0.0F);
            dataManager.register(Y_OFFSET_ANIMATION, 0.0F);
            dataManager.register(X_CURL, 1.3F);
            dataManager.register(Y_CURL, 1.0F);
            dataManager.register(CURL_STEPS, 0);
            dataManager.register(X_CURL_ANIMATION, 0.0F);
            dataManager.register(Y_CURL_ANIMATION, 0.0F);
            dataManager.register(LAST_X_CURL_ANIMATION, 0.0F);
        }

        @Override
        public void onLivingUpdate() {
            if (!world.isRemote && !validateCommandBlockStructureOwner()) return;
            setNoGravity(true);
            super.onLivingUpdate();
            setNoGravity(true);
            motionX = motionY = motionZ = 0.0D;
            fallDistance = 0.0F;
            tickTentacleAnimation();
            updateMultipartChain();
            if (isDormant()) {
                setAttackTarget(null);
                getNavigator().clearPath();
                removePassengers();
                if (isDoingSwingAttack()) stopSwingAnimation();
                if (strangleTicks > 0) stopStrangleLocal();
                return;
            }
            tickAwakeAnimation();
            tickSwingAttack();
            if (nextSwing > 0) --nextSwing;
            if (strangleTicks > 0 && --strangleTicks == 0) stopStrangleLocal();
        }

        private void tickTentacleAnimation() {
            int offsetSteps = dataManager.get(OFFSET_STEPS);
            if (offsetSteps > 0) {
                xOffsetAnimation += MathHelper.wrapDegrees(
                        dataManager.get(X_OFFSET_ANIMATION) - xOffsetAnimation) / offsetSteps;
                yOffsetAnimation += (dataManager.get(Y_OFFSET_ANIMATION) - yOffsetAnimation) / offsetSteps;
            }
            int curlSteps = dataManager.get(CURL_STEPS);
            if (curlSteps > 0) {
                xCurlAnimation += (dataManager.get(X_CURL_ANIMATION) - xCurlAnimation) / curlSteps;
                float curlDelta = dataManager.get(Y_CURL_ANIMATION) - yCurlAnimation;
                yCurlAnimation += (dataManager.get(SHOULD_WRAP_Y)
                        ? MathHelper.wrapDegrees(curlDelta) : curlDelta) / curlSteps;
            }
            if (!isDormant()) ++tentacleAnimation;

            float speed = strangleTicks > 0 ? 15.0F
                    : awakeTicks > 0 || indefinitelyAwake ? 6.0F : 1.0F;
            float reach = awakeTicks > 0 || indefinitelyAwake ? 4.0F : 1.0F;
            float animation = tentacleAnimation + dataManager.get(ANIMATION_OFFSET);
            rotationPitch = (float) Math.toDegrees(MathHelper.cos(animation * 0.05F * speed))
                    * 0.05F * reach - 90.0F + dataManager.get(X_OFFSET) + xOffsetAnimation;
            rotationYaw = (float) Math.toDegrees(MathHelper.sin(animation * 0.06F * speed))
                    * 0.14F * reach - 270.0F + dataManager.get(Y_OFFSET) + yOffsetAnimation;
        }

        private void updateMultipartChain() {
            for (TentaclePart part : multipartParts) {
                part.beginManualTick();
                part.pushIntersectingEntities();
            }
            double[] nextX = new double[multipartParts.length];
            double[] nextY = new double[multipartParts.length];
            double[] nextZ = new double[multipartParts.length];
            float[] nextYaw = new float[multipartParts.length];
            float[] nextPitch = new float[multipartParts.length];
            for (int index = 1; index < multipartParts.length; index++) {
                TentaclePart parent = multipartParts[index - 1];
                TentaclePart child = multipartParts[index];
                Vec3d look = parent.getLookVec();
                Vec3d delta = new Vec3d(look.x * parent.width,
                        look.y * parent.height, look.z * parent.width);
                float childPitch = parent.rotationPitch * getPartXCurl(index - 1);
                float baseYaw = dataManager.get(Y_OFFSET) + yOffsetAnimation;
                float childYaw = (parent.rotationYaw - baseYaw) * getEffectiveYCurl() + baseYaw;
                child.updateDynamicSize(child.rotationPitch * getPartXCurl(index));
                nextX[index] = parent.posX + delta.x;
                nextY[index] = parent.posY + delta.y;
                nextZ[index] = parent.posZ + delta.z;
                nextYaw[index] = childYaw;
                nextPitch[index] = childPitch;
            }
            for (int index = multipartParts.length - 1; index >= 1; index--) {
                multipartParts[index].setPartLocation(nextX[index], nextY[index], nextZ[index],
                        nextYaw[index], nextPitch[index]);
            }
            TentaclePart root = multipartParts[0];
            root.setPartLocation(posX, posY, posZ, rotationYaw, rotationPitch);
        }

        private float getPartXCurl(int segment) {
            float curl = dataManager.get(X_CURL) + xCurlAnimation;
            if (segment == 3) curl += dataManager.get(LAST_X_CURL_ANIMATION);
            return curl;
        }

        private float getEffectiveYCurl() {
            return dataManager.get(Y_CURL) + yCurlAnimation;
        }

        public boolean isDormant() { return dataManager.get(DORMANT); }
        public void setDormant(boolean dormant) {
            dataManager.set(DORMANT, dormant);
            if (dormant) {
                setAttackTarget(null);
                removePassengers();
            }
        }
        public void doAwakeAnimation() {
            startAwakeAnimation(false);
            if (!world.isRemote) world.setEntityState(this, (byte) 13);
            playSound(ModSounds.get("whoosh"), 3.0F, 1.0F);
        }
        private void startAwakeAnimation(boolean indefinite) {
            indefinitelyAwake = indefinite;
            awakeTicks = 40;
            lerpCurlYTo(0.05F * (float) rand.nextGaussian(), 8);
        }
        public void doIndefiniteAwakeAnimation() {
            if (!world.isRemote) doAwakeAnimation();
            indefinitelyAwake = true;
            awakeTicks = 0;
            if (!world.isRemote) world.setEntityState(this, (byte) 14);
        }
        private void stopAwakeAnimation() {
            indefinitelyAwake = false;
            lerpCurlYTo(0.0F, 4);
        }
        public boolean isDoingSwingAttack() { return swingTicks > 0; }
        public boolean canDoSwingAttack() { return nextSwing <= 0 && !isDoingSwingAttack(); }
        public void setCanSwing(boolean canSwing) {
            dataManager.set(CAN_SWING, canSwing);
            if (!canSwing) stopSwingAnimation();
        }
        public boolean canSwing() { return dataManager.get(CAN_SWING); }
        public void setCanStrangle(boolean canStrangle) {
            dataManager.set(CAN_STRANGLE, canStrangle);
            if (!canStrangle) {
                removePassengers();
                stopStrangleLocal();
            }
        }
        public boolean canStrangle() { return dataManager.get(CAN_STRANGLE); }

        public void bindToCommandBlock(SupplementalEntities.CommandBlockEntity commandBlock, int index) {
            commandBlockStructureOwner = commandBlock.getUniqueID();
            commandBlockStructureIndex = index;
            missingCommandBlockStructureOwnerTicks = 0;
            attackKnockback = 6.5D;
        }

        public boolean isCommandBlockStructureOf(SupplementalEntities.CommandBlockEntity commandBlock) {
            return commandBlock != null && commandBlockStructureOwner != null
                    && commandBlockStructureOwner.equals(commandBlock.getUniqueID());
        }

        public boolean isCommandBlockStructureTentacle() {
            return commandBlockStructureOwner != null;
        }

        public int getCommandBlockStructureIndex() {
            return commandBlockStructureIndex;
        }

        public int getStructureAnimationOffset() {
            return dataManager.get(ANIMATION_OFFSET);
        }

        public void configureStructurePose(float xOffset, float yOffset, float xCurl,
                                            float yCurl, int animationOffset) {
            setSavedXOffset(xOffset);
            setSavedYOffset(yOffset);
            setSavedXCurl(xCurl);
            setSavedYCurl(yCurl);
            dataManager.set(ANIMATION_OFFSET, animationOffset);
        }

        private boolean validateCommandBlockStructureOwner() {
            if (commandBlockStructureOwner == null) return true;
            Entity owner = world instanceof WorldServer
                    ? ((WorldServer) world).getEntityFromUuid(commandBlockStructureOwner) : null;
            if (owner instanceof SupplementalEntities.CommandBlockEntity && !owner.isDead) {
                missingCommandBlockStructureOwnerTicks = 0;
                SupplementalEntities.CommandBlockEntity commandBlock =
                        (SupplementalEntities.CommandBlockEntity) owner;
                if (commandBlock.getCoreMode()
                        == SupplementalEntities.CommandBlockEntity.CoreMode.TENTACLES) return true;
                setDead();
                return false;
            }
            if (++missingCommandBlockStructureOwnerTicks > 200) setDead();
            return false;
        }
        public void curlAround(Vec3d position) {
            dataManager.set(CURLING, true);
            curlX = position.x;
            curlY = position.y;
            curlZ = position.z;
            Vec3d delta = position.subtract(getPositionVector());
            float angle = (float) Math.toDegrees(MathHelper.atan2(delta.x, delta.z));
            lerpBaseYTo(-(angle + 180.0F + getSavedYOffset()), 8, false);
            lerpCurlTo(0.1F, 0.1F, 4);
        }
        public void stopCurlingAround() {
            dataManager.set(CURLING, false);
            lerpBaseYTo(0.0F, 8, true);
            lerpCurlTo(0.0F, 0.0F, 4);
        }
        public boolean isCurling() { return dataManager.get(CURLING); }

        @Override
        public boolean attackEntityAsMob(Entity entityIn) {
            return !isDormant() && super.attackEntityAsMob(entityIn);
        }
        @Override protected double getSickenedHealth() { return 80.0D; }
        @Override protected double getSickenedSpeed() { return 0.0D; }
        @Override protected double getSickenedDamage() { return 12.0D; }
        @Override protected double getSickenedFollowRange() { return 8.0D; }
        @Override protected double getSickenedKnockbackResistance() { return 0.0D; }
        @Override public String getSickenedType() { return "tentacle"; }

        @Override
        public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingData) {
            setSavedYOffset((float) rand.nextInt(360));
            setSavedXOffset(15.0F + rand.nextFloat() * 5.0F);
            setSavedXCurl(1.25F + rand.nextFloat() * 0.1F);
            dataManager.set(ANIMATION_OFFSET, rand.nextInt(35) * 10000);
            return super.onInitialSpawn(difficulty, livingData);
        }

        @Override public void move(MoverType type, double x, double y, double z) { }
        @Override public void applyEntityCollision(Entity entityIn) { }
        /** 现代版 isPushable=false 会跳过主体盒推挤；五段 multipart 仍各自执行上游碰撞。 */
        @Override protected void collideWithNearbyEntities() { }
        @Override public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) { }
        @Override public boolean canBePushed() { return false; }
        @Override public boolean canBeCollidedWith() { return false; }
        @Override public boolean isOnLadder() { return false; }
        @Override public void fall(float distance, float damageMultiplier) { }
        @Override public boolean isPotionApplicable(PotionEffect effect) { return false; }
        @Override public float getEyeHeight() { return height * 0.5F; }
        @Override protected boolean canDespawn() { return false; }

        @Override
        public AxisAlignedBB getRenderBoundingBox() {
            return getEntityBoundingBox().grow(4.0D);
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            return source == DamageSource.OUT_OF_WORLD && super.attackEntityFrom(source, amount);
        }

        @Override
        public boolean attackEntityFromPart(MultiPartEntityPart part, DamageSource source, float amount) {
            return (!isDormant() || source == DamageSource.OUT_OF_WORLD)
                    && super.attackEntityFrom(source, amount);
        }

        @Override
        public World getWorld() {
            return world;
        }

        @Override
        public Entity[] getParts() {
            return multipartParts;
        }

        @Override
        public void updatePassenger(Entity passenger) {
            if (passenger.getRidingEntity() == this) {
                TentaclePart tip = multipartParts[multipartParts.length - 1];
                if (passenger.getDistance(tip) < 20.0F) {
                    passenger.setPosition(tip.posX, tip.posY, tip.posZ);
                } else {
                    super.updatePassenger(passenger);
                }
                passenger.motionX = passenger.motionY = passenger.motionZ = 0.0D;
            }
        }

        @Override
        public void handleStatusUpdate(byte id) {
            if (id == 11) {
                startStrangleLocal();
            } else if (id == 12) {
                stopStrangleLocal();
            } else if (id == 13) {
                startAwakeAnimation(false);
            } else if (id == 14) {
                indefinitelyAwake = true;
                awakeTicks = 0;
            } else if (id == 15) {
                swingTicks = 40;
                knockbackWait = 0;
            } else if (id == 16) {
                stopSwingAnimation();
            } else {
                super.handleStatusUpdate(id);
            }
        }

        public float getSegmentPitch(int segment, float partialTicks) {
            TentaclePart part = multipartParts[MathHelper.clamp(segment, 0, multipartParts.length - 1)];
            TentaclePart previous = segment <= 0 ? null : multipartParts[segment - 1];
            float currentPitch = interpolateRotation(part.prevRotationPitch, part.rotationPitch, partialTicks);
            float previousPitch = previous == null ? 0.0F
                    : interpolateRotation(previous.prevRotationPitch, previous.rotationPitch, partialTicks) + 90.0F;
            return -(currentPitch - previousPitch + 90.0F) * 0.017453292F;
        }

        public float getSegmentYaw(int segment, float partialTicks) {
            TentaclePart part = multipartParts[MathHelper.clamp(segment, 0, multipartParts.length - 1)];
            TentaclePart previous = segment <= 0 ? null : multipartParts[segment - 1];
            float currentYaw = interpolateRotation(part.prevRotationYaw, part.rotationYaw, partialTicks);
            float previousYaw = previous == null ? 0.0F
                    : interpolateRotation(previous.prevRotationYaw, previous.rotationYaw, partialTicks);
            return (currentYaw - previousYaw) * 0.017453292F;
        }

        private static float interpolateRotation(float previous, float current, float partialTicks) {
            return previous + (current - previous) * partialTicks;
        }

        private void tickAwakeAnimation() {
            if (awakeTicks > 0 && --awakeTicks == 0 && !indefinitelyAwake) stopAwakeAnimation();
        }

        private void startSwingAttack() {
            swingTicks = 40;
            knockbackWait = 0;
            if (!world.isRemote) world.setEntityState(this, (byte) 15);
        }

        private void tickSwingAttack() {
            if (swingTicks <= 0) return;
            --swingTicks;
            if (swingTicks == 25) {
                lerpBaseYTo(80.0F, 4, false);
                lerpCurlYTo(-0.1F, 4);
            }
            if (swingTicks == 15 && getAttackTarget() != null) {
                doSwingAnimation(getAttackTarget().getPositionVector(), 20.0F, 2);
            }
            if (!world.isRemote && ++knockbackWait >= 35) strikeSwingTarget();
            if (swingTicks == 0) stopSwingAnimation();
        }

        private void strikeSwingTarget() {
            EntityLivingBase target = getAttackTarget();
            if (target == null || !target.isEntityAlive()) return;
            if (target instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) target;
                if (!player.getActiveItemStack().isEmpty() && player.getActiveItemStack().getItem() == Items.SHIELD
                        && !player.getCooldownTracker().hasCooldown(Items.SHIELD)) {
                    player.getCooldownTracker().setCooldown(Items.SHIELD, 100);
                    player.resetActiveHand();
                    world.setEntityState(player, (byte) 30);
                }
            }
            if (target.attackEntityFrom(DamageSource.causeMobDamage(this), (float) getEntityAttribute(
                    SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * 3.5F)) {
                target.knockBack(this, (float) attackKnockback,
                        posX - target.posX, posZ - target.posZ);
            }
        }

        private void stopSwingAnimation() {
            boolean wasSwinging = swingTicks > 0;
            swingTicks = 0;
            knockbackWait = 0;
            stopSwingAnimation(false);
            if (wasSwinging && !world.isRemote) world.setEntityState(this, (byte) 16);
        }

        private void doSwingAnimation(Vec3d target, float overReach, int steps) {
            Vec3d delta = target.subtract(getPositionVector());
            float angle = (float) Math.toDegrees(MathHelper.atan2(delta.x, delta.z));
            lerpBaseYTo(-(angle + 360.0F - 90.0F + overReach
                    + MathHelper.wrapDegrees(getSavedYOffset())) % 360.0F, steps, false);
            lerpCurlTo(0.1F, 0.1F, 4);
            playSound(ModSounds.get("whoosh"), 3.0F, 1.0F);
        }

        private void stopSwingAnimation(boolean saveCurrentBaseY) {
            if (saveCurrentBaseY) {
                float current = dataManager.get(Y_OFFSET) + yOffsetAnimation;
                setSavedYOffset(current);
            }
            lerpBaseYTo(0.0F, 12, true);
            lerpCurlTo(0.0F, 0.0F, 4);
        }

        private void startStrangleLocal() {
            strangleTicks = 20;
        }

        private void doStrangle() {
            startStrangleLocal();
            if (!world.isRemote) world.setEntityState(this, (byte) 11);
        }

        private void stopStrangleLocal() {
            strangleTicks = 0;
        }

        private void stopStrangle() {
            stopStrangleLocal();
            if (!world.isRemote) world.setEntityState(this, (byte) 12);
        }

        public void lerpBaseOffsetTo(float x, float y, int steps) {
            dataManager.set(X_OFFSET_ANIMATION, x);
            dataManager.set(Y_OFFSET_ANIMATION, y);
            dataManager.set(OFFSET_STEPS, steps);
        }

        public void lerpCurlTo(float x, float y, int steps) {
            dataManager.set(X_CURL_ANIMATION, x);
            dataManager.set(Y_CURL_ANIMATION, y);
            dataManager.set(CURL_STEPS, steps);
        }

        public void lerpBaseXTo(float x, int steps) {
            dataManager.set(X_OFFSET_ANIMATION, x);
            dataManager.set(OFFSET_STEPS, steps);
        }

        public void lerpBaseYTo(float y, int steps, boolean wrap) {
            dataManager.set(Y_OFFSET_ANIMATION, y);
            dataManager.set(OFFSET_STEPS, steps);
            dataManager.set(SHOULD_WRAP_Y, wrap);
        }

        public void lerpCurlXTo(float x, int steps) {
            dataManager.set(X_CURL_ANIMATION, x);
            dataManager.set(CURL_STEPS, steps);
        }

        public void lerpCurlYTo(float y, int steps) {
            dataManager.set(Y_CURL_ANIMATION, y);
            dataManager.set(CURL_STEPS, steps);
        }

        private void setSavedXOffset(float value) {
            dataManager.set(X_OFFSET, value);
        }

        private void setSavedYOffset(float value) {
            dataManager.set(Y_OFFSET, value);
        }

        private void setSavedXCurl(float value) {
            dataManager.set(X_CURL, value);
        }

        private void setSavedYCurl(float value) {
            dataManager.set(Y_CURL, value);
        }

        private float getSavedYOffset() {
            return dataManager.get(Y_OFFSET);
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setBoolean("Dormant", isDormant());
            compound.setBoolean("Curling", isCurling());
            compound.setBoolean("CanSwing", canSwing());
            compound.setBoolean("CanStrangle", canStrangle());
            compound.setInteger("AnimOffset", dataManager.get(ANIMATION_OFFSET));
            compound.setFloat("XOffset", dataManager.get(X_OFFSET));
            compound.setFloat("YOffset", dataManager.get(Y_OFFSET));
            compound.setFloat("XCurl", dataManager.get(X_CURL));
            compound.setFloat("YCurl", dataManager.get(Y_CURL));
            compound.setFloat("XOffsetAnim", dataManager.get(X_OFFSET_ANIMATION));
            compound.setFloat("YOffsetAnim", dataManager.get(Y_OFFSET_ANIMATION));
            compound.setFloat("XCurlAnim", dataManager.get(X_CURL_ANIMATION));
            compound.setFloat("YCurlAnim", dataManager.get(Y_CURL_ANIMATION));
            compound.setFloat("LastXCurlAnim", dataManager.get(LAST_X_CURL_ANIMATION));
            compound.setInteger("AwakeTicks", awakeTicks);
            compound.setBoolean("IndefinitelyAwake", indefinitelyAwake);
            compound.setInteger("SwingTicks", swingTicks);
            compound.setInteger("NextSwing", nextSwing);
            compound.setInteger("StrangleTicks", strangleTicks);
            compound.setDouble("AttackKnockback", attackKnockback);
            compound.setDouble("CurlTargetX", curlX);
            compound.setDouble("CurlTargetY", curlY);
            compound.setDouble("CurlTargetZ", curlZ);
            if (commandBlockStructureOwner != null) {
                compound.setUniqueId("CommandBlockStructureOwner", commandBlockStructureOwner);
                compound.setInteger("CommandBlockStructureIndex", commandBlockStructureIndex);
            }
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            setDormant(compound.getBoolean("Dormant"));
            dataManager.set(CURLING, compound.getBoolean("Curling"));
            setCanSwing(!compound.hasKey("CanSwing") || compound.getBoolean("CanSwing"));
            setCanStrangle(!compound.hasKey("CanStrangle") || compound.getBoolean("CanStrangle"));
            if (compound.hasKey("AnimOffset")) dataManager.set(ANIMATION_OFFSET, compound.getInteger("AnimOffset"));
            if (compound.hasKey("XOffset")) setSavedXOffset(compound.getFloat("XOffset"));
            if (compound.hasKey("YOffset")) setSavedYOffset(compound.getFloat("YOffset"));
            if (compound.hasKey("XCurl")) setSavedXCurl(compound.getFloat("XCurl"));
            if (compound.hasKey("YCurl")) setSavedYCurl(compound.getFloat("YCurl"));
            if (compound.hasKey("XOffsetAnim")) lerpBaseXTo(compound.getFloat("XOffsetAnim"), 1);
            if (compound.hasKey("YOffsetAnim")) lerpBaseYTo(compound.getFloat("YOffsetAnim"), 1, true);
            if (compound.hasKey("XCurlAnim")) lerpCurlXTo(compound.getFloat("XCurlAnim"), 1);
            if (compound.hasKey("YCurlAnim")) lerpCurlYTo(compound.getFloat("YCurlAnim"), 1);
            if (compound.hasKey("LastXCurlAnim")) {
                dataManager.set(LAST_X_CURL_ANIMATION, compound.getFloat("LastXCurlAnim"));
            }
            awakeTicks = compound.getInteger("AwakeTicks");
            indefinitelyAwake = compound.getBoolean("IndefinitelyAwake");
            swingTicks = compound.getInteger("SwingTicks");
            nextSwing = compound.getInteger("NextSwing");
            strangleTicks = compound.getInteger("StrangleTicks");
            curlX = compound.getDouble("CurlTargetX");
            curlY = compound.getDouble("CurlTargetY");
            curlZ = compound.getDouble("CurlTargetZ");
            commandBlockStructureOwner = compound.hasUniqueId("CommandBlockStructureOwner")
                    ? compound.getUniqueId("CommandBlockStructureOwner") : null;
            commandBlockStructureIndex = commandBlockStructureOwner == null ? -1
                    : compound.getInteger("CommandBlockStructureIndex");
            attackKnockback = compound.hasKey("AttackKnockback", 6)
                    ? compound.getDouble("AttackKnockback")
                    : commandBlockStructureOwner == null ? 1.5D : 6.5D;
            missingCommandBlockStructureOwnerTicks = 0;
        }

        private static final class TentaclePart extends MultiPartEntityPart {
            private final float baseWidth;
            private final float baseHeight;

            private TentaclePart(TentacleEntity parent, String name, float width, float height, int segment) {
                super(parent, name, width, height);
                baseWidth = width;
                baseHeight = height;
            }

            private void beginManualTick() {
                onUpdate();
            }

            private void setPartLocation(double x, double y, double z, float yaw, float pitch) {
                setPosition(x, y, z);
                rotationYaw = yaw;
                rotationPitch = pitch;
            }

            private void updateDynamicSize(float rotation) {
                float sine = Math.abs(MathHelper.sin(rotation * 0.017453292F));
                width = sine * (baseWidth - baseHeight) + baseHeight;
                height = sine * (baseHeight - baseWidth) + baseWidth;
                float halfWidth = width * 0.5F;
                setEntityBoundingBox(new AxisAlignedBB(posX - halfWidth, posY, posZ - halfWidth,
                        posX + halfWidth, posY + height, posZ + halfWidth));
            }

            private void pushIntersectingEntities() {
                TentacleEntity parent = (TentacleEntity) this.parent;
                for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(parent, getEntityBoundingBox())) {
                    if (entity instanceof MultiPartEntityPart || entity.isEntityEqual(parent)
                            || entity.isDead || entity.noClip || parent.noClip || !entity.canBePushed()
                            || !EntitySelectors.NOT_SPECTATING.apply(entity)
                            || !EntitySelectors.getTeamCollisionPredicate(parent).apply(entity)
                            || parent.isRidingOrBeingRiddenBy(entity)) continue;
                    entity.applyEntityCollision(this);
                }
            }

            @Override
            public boolean canBePushed() {
                return false;
            }
        }

        private static final class DormantGoal extends EntityAIBase {
            private final TentacleEntity tentacle;

            private DormantGoal(TentacleEntity tentacle) {
                this.tentacle = tentacle;
                setMutexBits(7);
            }

            @Override public boolean shouldExecute() { return tentacle.isDormant(); }
            @Override public boolean shouldContinueExecuting() { return tentacle.isDormant(); }
            @Override public void startExecuting() { tentacle.setAttackTarget(null); }
        }

        private static final class SwingGoal extends EntityAIBase {
            private final TentacleEntity tentacle;

            private SwingGoal(TentacleEntity tentacle) { this.tentacle = tentacle; }

            @Override
            public boolean shouldExecute() {
                EntityLivingBase target = tentacle.getAttackTarget();
                return tentacle.canSwing() && !tentacle.isDormant() && target != null && target.isEntityAlive()
                        && tentacle.isEntityAlive()
                        && (tentacle.getHealth() < tentacle.getMaxHealth() || !tentacle.canStrangle())
                        && tentacle.canDoSwingAttack();
            }

            @Override
            public boolean shouldContinueExecuting() {
                EntityLivingBase target = tentacle.getAttackTarget();
                return target != null && target.isEntityAlive() && tentacle.isEntityAlive()
                        && tentacle.isDoingSwingAttack();
            }

            @Override
            public void startExecuting() {
                tentacle.startSwingAttack();
                tentacle.nextSwing = 120 + tentacle.getRNG().nextInt(120);
            }
        }

        private static final class StrangleGoal extends EntityAIBase {
            private final TentacleEntity tentacle;
            private int grabWait;
            private int nextStrangle;

            private StrangleGoal(TentacleEntity tentacle) { this.tentacle = tentacle; }

            @Override
            public boolean shouldExecute() {
                EntityLivingBase target = tentacle.getAttackTarget();
                return tentacle.canStrangle() && !tentacle.isDormant() && target != null && target.isEntityAlive()
                        && tentacle.isEntityAlive() && !tentacle.isDoingSwingAttack();
            }

            @Override public boolean shouldContinueExecuting() { return shouldExecute(); }

            @Override
            public void startExecuting() {
                EntityLivingBase target = tentacle.getAttackTarget();
                if (target != null) tentacle.doSwingAnimation(target.getPositionVector(), 0.0F, 4);
                tentacle.dataManager.set(LAST_X_CURL_ANIMATION, 0.3F);
                nextStrangle = 20 + tentacle.getRNG().nextInt(40);
            }

            @Override
            public void updateTask() {
                EntityLivingBase target = tentacle.getAttackTarget();
                if (target == null) return;
                if (!tentacle.getEntitySenses().canSee(target)) {
                    target.dismountRidingEntity();
                    tentacle.setAttackTarget(null);
                    return;
                }
                if (++grabWait > 5 && target.getRidingEntity() != tentacle) target.startRiding(tentacle, true);
                if (nextStrangle > 0 && --nextStrangle <= 0) {
                    tentacle.doStrangle();
                    nextStrangle = 20 + tentacle.getRNG().nextInt(40);
                    tentacle.attackEntityAsMob(target);
                }
            }

            @Override
            public void resetTask() {
                tentacle.removePassengers();
                grabWait = 0;
                tentacle.stopSwingAnimation(true);
                tentacle.stopStrangle();
                tentacle.dataManager.set(LAST_X_CURL_ANIMATION, 0.0F);
            }
        }

        private static final class TentacleTargetGoal<T extends EntityLivingBase>
                extends EntityAINearestAttackableTarget<T> {
            private final TentacleEntity tentacle;

            private TentacleTargetGoal(TentacleEntity tentacle, Class<T> targetClass) {
                super(tentacle, targetClass, 10, true, true,
                        target -> target != null && !(target instanceof SickenedMobEntity));
                this.tentacle = tentacle;
            }

            @Override
            protected AxisAlignedBB getTargetableArea(double targetDistance) {
                return tentacle.getEntityBoundingBox().grow(targetDistance);
            }

            @Override
            public void startExecuting() {
                for (TentacleEntity other : tentacle.world.getEntitiesWithinAABB(TentacleEntity.class,
                        getTargetableArea(getTargetDistance() + 10.0D))) {
                    if (other != tentacle && other.isEntityAlive() && other.getAttackTarget() == targetEntity) return;
                }
                super.startExecuting();
            }
        }
    }

    public static class WitheredSymbiontEntity extends SickenedMobEntity
            implements BossThemeProvider {
        private static final DataParameter<Integer> BOSSFIGHT_STAGE = EntityDataManager.createKey(WitheredSymbiontEntity.class, DataSerializers.VARINT);
        private static final DataParameter<String> SPELL_TYPE = EntityDataManager.createKey(WitheredSymbiontEntity.class, DataSerializers.STRING);
        private static final DataParameter<Boolean> NON_BOSS_MODE = EntityDataManager.createKey(WitheredSymbiontEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> RUSH_MODE = EntityDataManager.createKey(WitheredSymbiontEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> SHOULD_NOT_GO_OVER_HALF = EntityDataManager.createKey(WitheredSymbiontEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Integer> SPELL_CASTING_TIME = EntityDataManager.createKey(WitheredSymbiontEntity.class, DataSerializers.VARINT);
        private static final DataParameter<Boolean> SMASHING = EntityDataManager.createKey(WitheredSymbiontEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Integer> ATTACK_DELAY = EntityDataManager.createKey(WitheredSymbiontEntity.class, DataSerializers.VARINT);
        private final BossInfoServer bossInfo = new BossInfoServer(getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);
        private UUID ownerUuid;
        private List<EntityAIBase> bossFightGoals;
        private SymbiontAttackGoal attackGoal;
        private PrepareSpellGoal prepareSpellGoal;
        private UseSpellGoal useSpellGoal;
        private SummonMobsGoal summonMobsGoal;
        private DoNothingGoal doNothingGoal;
        private SymbiontSpell spellInstance;
        private int stageTicks;
        private int nextSpellPickCount;
        private int spellsUsed;
        private int smashAirTime;
        /** 上游的特殊死亡计时；不能复用 deathTime，否则会触发 1.12 默认侧翻。 */
        private int specialDeathTime;
        private final List<EntityLivingBase> entitiesToThrow = new ArrayList<EntityLivingBase>();
        private final List<ItemStack> dropItems = new ArrayList<ItemStack>();
        private final List<UUID> fightContributors = new ArrayList<UUID>();
        private float crouchAnimation;
        private float previousCrouchAnimation;
        private float tearAlpha;
        private float previousTearAlpha;
        private boolean healthScaled;

        public WitheredSymbiontEntity(World world) {
            super(world);
            setSize(1.2F, 3.8F);
            stepHeight = 1.0F;
            experienceValue = 150;
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(BOSSFIGHT_STAGE, BossfightStage.ATTACKING.ordinal());
            dataManager.register(SPELL_TYPE, "witherstormmod:empty");
            dataManager.register(NON_BOSS_MODE, false);
            dataManager.register(RUSH_MODE, false);
            dataManager.register(SHOULD_NOT_GO_OVER_HALF, true);
            dataManager.register(SPELL_CASTING_TIME, 0);
            dataManager.register(SMASHING, false);
            dataManager.register(ATTACK_DELAY, 0);
        }

        @Override
        protected void initEntityAI() {
            bossFightGoals = new ArrayList<EntityAIBase>();
            attackGoal = new SymbiontAttackGoal(this);
            prepareSpellGoal = new PrepareSpellGoal(this);
            useSpellGoal = new UseSpellGoal(this);
            summonMobsGoal = new SummonMobsGoal(this);
            doNothingGoal = new DoNothingGoal(this);
            Collections.addAll(bossFightGoals, attackGoal, prepareSpellGoal, useSpellGoal, summonMobsGoal, doNothingGoal);

            tasks.addTask(1, prepareSpellGoal);
            tasks.addTask(2, useSpellGoal);
            tasks.addTask(3, attackGoal);
            tasks.addTask(4, new EntityAISwimming(this));
            tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.7D));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
            tasks.addTask(7, new EntityAILookIdle(this));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class,
                    10, true, false, WitheredSymbiontEntity::isValidPlayerTarget));
            if (WitherStormConfig.shouldSymbiontAttackMobs) {
                targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityLiving>(
                        this, EntityLiving.class, 10, true, false,
                        WitheredSymbiontEntity::isValidMobTarget));
            }
        }

        @Override protected double getSickenedHealth() { return 60.0D; }
        @Override protected double getSickenedSpeed() { return 0.15D; }
        @Override protected double getSickenedArmor() { return 1.0D; }
        @Override protected double getSickenedDamage() { return 16.0D; }
        @Override protected double getSickenedFollowRange() { return 45.0D; }
        @Override public String getSickenedType() { return "withered_symbiont"; }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            // 1.20 uses a conditional look controller: the symbiont may turn
            // normally while active, but its head must stop tracking targets
            // during the vulnerable/dead state. 1.12 has no conditional
            // controller, so clear the look target after vanilla AI runs.
            if (isVulnerable() || isDead) {
                getLookHelper().setLookPosition(posX, posY + getEyeHeight(), posZ, 30.0F, 30.0F);
                rotationYawHead = renderYawOffset;
            }
            previousCrouchAnimation = crouchAnimation;
            if (isVulnerable()) {
                crouchAnimation = Math.min(0.6F, crouchAnimation + (1.0F - crouchAnimation) * 0.1F + 0.02F);
            } else {
                crouchAnimation = Math.max(0.0F, crouchAnimation - crouchAnimation * 0.4F - 0.1F);
            }
            previousTearAlpha = tearAlpha;
            if (WitherStormConfig.attackableWhenNotVulnerable || isVulnerable()) {
                tearAlpha = Math.min(1.0F, tearAlpha + 0.05F);
            }
            else tearAlpha = Math.max(0.0F, tearAlpha - 0.05F);

            if (world.isRemote) {
                spawnMovementParticles();
                return;
            }

            if (!isEntityAlive()) return;
            stageTicks++;
            tickSpellCasting();
            if (nextSpellPickCount > 0) nextSpellPickCount--;
            tickSmashing();
            if (getStage().shouldMoveToNextStage(this)) nextStage();
            if (getAttackDelay() > 0) {
                setAttackDelay(getAttackDelay() - 1);
                if (getAttackDelay() <= 0 && isVulnerable()) setStage(BossfightStage.ATTACKING);
            }
            bossInfo.setName(getDisplayName());
            bossInfo.setPercent(MathHelper.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        }

        @Override
        public boolean processInteract(EntityPlayer player, EnumHand hand) {
            // Upstream explicitly rejects ordinary interaction with the boss.
            return false;
        }

        /** Matches the upstream block particle kicked up by the symbiont while moving. */
        private void spawnMovementParticles() {
            if (motionX * motionX + motionY * motionY + motionZ * motionZ
                    <= 2.500000277905201E-7D || rand.nextInt(5) != 0) return;
            BlockPos ground = new BlockPos(MathHelper.floor(posX),
                    MathHelper.floor(posY - 0.2D), MathHelper.floor(posZ));
            IBlockState state = world.getBlockState(ground);
            if (state.getBlock() == Blocks.AIR) return;
            world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                    posX + (rand.nextDouble() - 0.5D) * width, posY + 0.1D,
                    posZ + (rand.nextDouble() - 0.5D) * width,
                    4.0D * (rand.nextDouble() - 0.5D), 0.5D,
                    4.0D * (rand.nextDouble() - 0.5D), Block.getStateId(state));
        }

        private void scaleHealthForNearbyPlayers() {
            healthScaled = true;
            List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class,
                    getEntityBoundingBox().grow(150.0D), player -> player != null && player.isEntityAlive()
                            && !player.isSpectator());
            if (players.size() <= 1) return;
            double maximum = getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue()
                    + players.size() * WitherStormConfig.healthScalePerPlayer;
            getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(maximum);
            setHealth((float) maximum);
        }

        @Override
        public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty,
                                                @Nullable IEntityLivingData livingData) {
            scaleHealthForNearbyPlayers();
            return super.onInitialSpawn(difficulty, livingData);
        }

        private void tickSpellCasting() {
            if (!isCastingSpell()) return;
            setSpellCastingTime(getSpellCastingTime() - 1);
            EntityLivingBase target = getAttackTarget();
            if (spellInstance != null) {
                if (target != null && target.isEntityAlive()) spellInstance.doCasting(target);
                else if ((getSpell().spellTime() - getSpellCastingTime()) % 20 == 0) breakSpell();
            }
            applySpellProtection();
            if (getSpellCastingTime() <= 0) castSpell();
        }

        private void applySpellProtection() {
            SpellType spell = getSpell();
            if (!spell.doProtection()) return;
            double radius = spell.protectionRadius();
            for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class,
                    getEntityBoundingBox().grow(radius), this::isProtectionTarget)) {
                if (!entitiesToThrow.contains(player)) {
                    entitiesToThrow.add(player);
                    playSound(ModSounds.get("withered_symbiont_launch_mob"), 16.0F, 1.0F);
                }
            }
            for (int index = entitiesToThrow.size() - 1; index >= 0; index--) {
                EntityLivingBase target = entitiesToThrow.get(index);
                if (!target.isEntityAlive() || getDistance(target) > radius) {
                    entitiesToThrow.remove(index);
                    continue;
                }
                Vec3d pulled = getPositionVector().subtract(target.getPositionVector()).normalize();
                Vec3d movement = new Vec3d(pulled.x, pulled.y - 0.5D, pulled.z)
                        .scale(-spell.protectionThrowStrength());
                target.motionX = movement.x;
                target.motionY = movement.y;
                target.motionZ = movement.z;
                target.velocityChanged = true;
            }
        }

        /** Equivalent to the upstream combat TargetingConditions protection predicate. */
        private boolean isProtectionTarget(EntityPlayer player) {
            return player != null && player.isEntityAlive() && !player.isSpectator()
                    && !player.capabilities.disableDamage
                    && getDistance(player) <= getSpell().protectionRadius();
        }

        private void tickSmashing() {
            if (!isSmashing()) return;
            if (smashAirTime > 0) {
                smashAirTime--;
                if (smashAirTime <= 0) {
                    motionY = -5.0D;
                    velocityChanged = true;
                }
            } else if (onGround) {
                setSmashing(false);
                float strength = shouldIncreaseDifficulty() ? 2.5F : 1.5F;
                world.newExplosion(this, posX, posY, posZ, strength, false,
                        ForgeEventFactory.getMobGriefingEvent(world, this));
            }
        }

        public BossfightStage getStage() {
            return BossfightStage.byOrdinal(dataManager.get(BOSSFIGHT_STAGE));
        }

        public void setStage(BossfightStage stage) {
            BossfightStage previous = getStage();
            if (previous != stage) previous.finish(this);
            dataManager.set(BOSSFIGHT_STAGE, stage.ordinal());
            stageTicks = 0;
            configureBossFightGoals(stage);
            if (previous == BossfightStage.ATTACKING && stage != BossfightStage.ATTACKING
                    && !world.isRemote) {
                setSpell(SymbiontSpells.Type.EMPTY);
            }
        }

        private void configureBossFightGoals(BossfightStage stage) {
            if (world.isRemote || bossFightGoals == null) return;
            for (EntityAIBase goal : bossFightGoals) tasks.removeTask(goal);
            if (stage == BossfightStage.ATTACKING) {
                spellsUsed = 0;
                tasks.addTask(1, prepareSpellGoal);
                tasks.addTask(2, useSpellGoal);
                tasks.addTask(3, attackGoal);
            } else if (stage == BossfightStage.SUMMONING) {
                tasks.addTask(1, summonMobsGoal);
            } else {
                tasks.addTask(1, doNothingGoal);
                playSound(ModSounds.get("withered_symbiont_power_down"), 4.0F, 1.0F);
            }
        }

        public void nextStage() {
            BossfightStage next = getStage().next();
            if (next == BossfightStage.SUMMONING && isNonBossMode()) next = next.next();
            setStage(next);
        }

        public int getStageTicks() { return stageTicks; }
        public void setStageTicks(int ticks) { stageTicks = Math.max(0, ticks); }

        public SpellType getSpell() {
            SpellType spell = WitherStormModRegistries.getSpellType(dataManager.get(SPELL_TYPE));
            return spell == null ? SymbiontSpells.apiType(SymbiontSpells.Type.EMPTY) : spell;
        }

        public SymbiontSpells.Type getLegacySpell() {
            return SymbiontSpells.legacyType(getSpell());
        }

        public void setSpell(SpellType spell) {
            ResourceLocation id = WitherStormModRegistries.getSpellTypeId(spell);
            if (id == null) throw new IllegalArgumentException("Unregistered spell type");
            if (spellInstance != null && getSpell() != spell) spellInstance.finish();
            dataManager.set(SPELL_TYPE, id.toString());
            if (!world.isRemote) spellInstance = spell.makeSpell(this);
        }

        public void setSpell(SymbiontSpells.Type spell) {
            setSpell(SymbiontSpells.apiType(spell));
        }

        public boolean hasSpell() {
            return getSpell() != SymbiontSpells.apiType(SymbiontSpells.Type.EMPTY);
        }
        public boolean isCastingSpell() { return getSpellCastingTime() > 0; }
        public boolean isSummoningMobs() { return getStage() == BossfightStage.SUMMONING; }
        public boolean isVulnerable() { return getStage() == BossfightStage.VULNERABLE; }
        public int getSpellCastingTime() { return dataManager.get(SPELL_CASTING_TIME); }
        private void setSpellCastingTime(int time) { dataManager.set(SPELL_CASTING_TIME, Math.max(0, time)); }

        public void beginSpellCasting() {
            if (world.isRemote || spellInstance == null || getAttackTarget() == null) return;
            spellInstance.start(getAttackTarget());
            setSpellCastingTime(getSpell().spellTime());
        }

        public void breakSpell() {
            if (!isCastingSpell()) return;
            setSpellCastingTime(0);
            if (spellInstance != null) spellInstance.finish();
            entitiesToThrow.clear();
            if (!world.isRemote) world.setEntityState(this, (byte) 11);
        }

        private void castSpell() {
            if (spellInstance != null) {
                EntityLivingBase target = getAttackTarget();
                if (target != null && target.isEntityAlive()) spellInstance.cast(target);
                spellInstance.finish();
            }
            entitiesToThrow.clear();
        }

        @Override
        public void handleStatusUpdate(byte id) {
            if (id == 11) {
                setSpellCastingTime(0);
                entitiesToThrow.clear();
            } else if (id == 12) {
                setAttackDelay(20);
            } else {
                super.handleStatusUpdate(id);
            }
        }

        public boolean canPickSpell() { return !hasSpell() || nextSpellPickCount <= 0; }
        public void setAndCastSpell(SpellType type) {
            if (type == SymbiontSpells.apiType(SymbiontSpells.Type.EMPTY) || isVulnerable()) return;
            nextSpellPickCount = 0;
            setSpell(type);
            useSpellGoal.nextAttackTick = ticksExisted + 1;
            playSound(ModSounds.get("withered_symbiont_prepare_spell"), 4.0F, 1.0F);
            nextSpellPickCount = 400 + rand.nextInt(400) - (shouldIncreaseDifficulty() ? 320 : 0);
            if (shouldNotGoOverHalfHealth() && getHealth() / getMaxHealth() <= 0.5F) setHalfHealthLimit(false);
        }

        public void setAndCastSpell(SymbiontSpells.Type type) {
            setAndCastSpell(SymbiontSpells.apiType(type));
        }
        public int getNextSpellPickCount() { return nextSpellPickCount; }
        public void setNextSpellPickCount(int count) { nextSpellPickCount = Math.max(0, count); }
        public int getSpellsUsed() { return spellsUsed; }
        public void spellUsed() { spellsUsed++; }
        public boolean shouldIncreaseDifficulty() { return isRushMode() || getHealth() / getMaxHealth() <= 0.5F; }
        public boolean shouldNotGoOverHalfHealth() { return !isNonBossMode() && dataManager.get(SHOULD_NOT_GO_OVER_HALF); }
        public void setHalfHealthLimit(boolean flag) { dataManager.set(SHOULD_NOT_GO_OVER_HALF, flag); }
        public boolean isSmashing() { return dataManager.get(SMASHING); }

        public void setSmashing(boolean flag) {
            dataManager.set(SMASHING, flag);
            if (flag) smashAirTime = 20;
        }

        public int getAttackDelay() { return dataManager.get(ATTACK_DELAY); }
        public boolean hasAttackDelay() { return getAttackDelay() > 0; }
        public boolean isStillAlive() { return isEntityAlive(); }
        private void setAttackDelay(int delay) { dataManager.set(ATTACK_DELAY, Math.max(0, delay)); }

        public void activateAttackDelay() {
            setAttackDelay(20);
            if (!world.isRemote) world.setEntityState(this, (byte) 12);
        }

        public float getVulnerableAnimation(float partialTicks) {
            return previousCrouchAnimation + (crouchAnimation - previousCrouchAnimation) * partialTicks;
        }

        public float getVulnerableAnim(float partialTicks) {
            return getVulnerableAnimation(partialTicks);
        }

        public float getTearAlpha(float partialTicks) {
            return previousTearAlpha + (tearAlpha - previousTearAlpha) * partialTicks;
        }

        @Override
        public boolean attackEntityAsMob(Entity target) {
            float base = (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
            float damage = (int) base > 0 ? base / 2.0F + rand.nextInt((int) base) : base;
            boolean attacked = target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
            if (attacked) {
                target.motionY += 0.8D;
                target.velocityChanged = true;
                applyEnchantments(this, target);
            }
            return attacked;
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (source == DamageSource.OUT_OF_WORLD) return super.attackEntityFrom(source, amount);
            Entity attacker = source.getImmediateSource();
            if (attacker == null) return false;
            Entity projectileOwner = source.getTrueSource();
            if (attacker != projectileOwner
                    && (attacker instanceof EntityArrow || attacker instanceof EntityFireball
                    || attacker instanceof EntityShulkerBullet)
                    && projectileOwner instanceof SickenedMobEntity) return false;
            if (!isVulnerable() && (isCastingSpell() || !WitherStormConfig.attackableWhenNotVulnerable)) return false;

            double angle = Math.atan2(attacker.posX - posX, attacker.posZ - posZ) * 180.0D / Math.PI;
            double difference = (-renderYawOffset - angle + 180.0D + 360.0D) % 360.0D;
            if (difference > 40.0D && difference < 320.0D) return false;
            if (isVulnerable() && getAttackDelay() <= 0) activateAttackDelay();
            if (!isVulnerable() && attacker instanceof EntityLivingBase && !isDead) {
                new SymbiontSpells.SmashSpell(this, SymbiontSpells.Type.SMASH)
                        .cast((EntityLivingBase) attacker);
            }
            if (source.isExplosion()) amount /= 4.0F;
            float halfHealth = getMaxHealth() * 0.5F;
            boolean reachesHalfHealth = false;
            if (shouldNotGoOverHalfHealth()) {
                reachesHalfHealth = SymbiontHalfHealthGate.reachesThreshold(
                        getHealth(), getMaxHealth(), amount);
                amount = SymbiontHalfHealthGate.clampDamage(getHealth(), getMaxHealth(), amount);
            }
            float before = getHealth();
            boolean result = super.attackEntityFrom(source, amount);
            if (result && reachesHalfHealth && getHealth() > halfHealth) {
                // 1.12 在此后才应用护甲；归位到门槛，确保下一轮施法能解除半血锁。
                setHealth(halfHealth);
            }
            if (result && before - getHealth() >= 5.0F && attacker instanceof EntityPlayer
                    && !fightContributors.contains(attacker.getUniqueID())) {
                fightContributors.add(attacker.getUniqueID());
            }
            return result;
        }

        @Override
        public void onKillEntity(EntityLivingBase victim) {
            if (!world.isRemote) TaintingManager.convertEntity(victim, false);
        }

        @Override public boolean isOnLadder() { return false; }
        @Override public void fall(float distance, float damageMultiplier) { }
        @Override protected void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos) { }

        @Override
        public void applyEntityCollision(Entity entityIn) {
            if (!world.isRemote && entityIn instanceof EntityPlayer && rand.nextInt(20) == 0) {
                EntityPlayer player = (EntityPlayer) entityIn;
                if (player.isEntityAlive() && !player.isSpectator() && !player.capabilities.disableDamage) {
                    setAttackTarget(player);
                }
            }
            super.applyEntityCollision(entityIn);
        }

        @Override
        public boolean startRiding(Entity entityIn, boolean force) {
            return !(entityIn instanceof EntityBoat) && !(entityIn instanceof EntityMinecart)
                    && super.startRiding(entityIn, force);
        }

        public List<EntityLivingBase> getNearbyMobTargets() {
            double range = getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
            return world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(range), target ->
                    target != this && isValidMobTarget(target) && isCombatTarget(target));
        }

        private static boolean isValidMobTarget(@Nullable EntityLivingBase target) {
            return target != null && target.isEntityAlive()
                    && !(target instanceof SickenedMobEntity)
                    && !(target instanceof WitherStormEntity)
                    && !(target instanceof SupplementalEntities.StormPartBase)
                    && !(target instanceof TentacleEntity)
                    && !(target instanceof EntityEnderman)
                    && !(target instanceof EntityDragon)
                    && !(target instanceof EntityWither)
                    && !(target instanceof EntityWitherSkeleton)
                    && (target instanceof EntityVillager || target instanceof EntityGolem
                    || target instanceof IMob || target instanceof EntityAnimal || target instanceof EntityPlayer);
        }

        private static boolean isValidPlayerTarget(@Nullable EntityPlayer player) {
            return player != null && player.isEntityAlive() && !player.isSpectator()
                    && !player.capabilities.disableDamage;
        }

        private boolean isCombatTarget(@Nullable EntityLivingBase target) {
            if (target == null || !target.isEntityAlive() || isOnSameTeam(target)) return false;
            if (target instanceof EntityPlayer && !isValidPlayerTarget((EntityPlayer) target)) return false;
            return getEntitySenses().canSee(target);
        }

        public List<EntityLivingBase> getNearbyPulseTargets() {
            double range = getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
            return world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(range), target ->
                    target != null && target != this && target.isEntityAlive()
                            && !(target instanceof WitheredSymbiontEntity)
                            && !(target instanceof WitherStormEntity)
                            && !(target instanceof SupplementalEntities.StormPartBase)
                            && !(target instanceof TentacleEntity)
                            && !(target instanceof SupplementalEntities.CommandBlockEntity)
                            && isCombatTarget(target));
        }

        @Nullable
        public EntityLivingBase getRandomNearbyTargetOrFallback(@Nullable EntityLivingBase fallback, boolean playersOnly) {
            double range = getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
            List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class,
                    getEntityBoundingBox().grow(range), target -> target != null && target != this
                            && target != fallback && target.isEntityAlive()
                            && (!playersOnly || target instanceof EntityPlayer)
                            && isCombatTarget(target));
            if (!targets.isEmpty() && rand.nextInt(targets.size() + 1) != 0) {
                Collections.shuffle(targets, rand);
                return targets.get(0);
            }
            return fallback;
        }

        public void summonSupportMob(boolean illagersOnly) {
            summonSupportMob(illagersOnly, 16);
        }

        /** Matches the upstream shared summon helper's five-attempt search. */
        public void summonSupportMob(boolean illagersOnly, int diameter) {
            if (world.isRemote) return;
            SickenedMobEntity mob = createWeightedSupportMob(illagersOnly, shouldIncreaseDifficulty());
            if (mob == null) return;
            BlockPos origin = new BlockPos(this);
            BlockPos spawn = null;
            int halfDiameter = Math.max(1, diameter / 2);
            for (int attempt = 0; attempt < 5 && spawn == null; attempt++) {
                int x = origin.getX() + rand.nextInt(diameter) - halfDiameter;
                int z = origin.getZ() + rand.nextInt(diameter) - halfDiameter;
                BlockPos cursor = new BlockPos(x,
                        com.wdcftgg.witherstormmod.common.util.WorldUtil
                                .getMotionBlockingHeightIgnoringLeaves(world, x, z), z);
                if (Math.sqrt(cursor.distanceSq(origin)) > 6.0D
                        && WorldEntitySpawner.canCreatureTypeSpawnAtLocation(
                        EntityLiving.SpawnPlacementType.ON_GROUND, world, cursor)) {
                    spawn = cursor;
                }
            }
            if (spawn == null) return;
            mob.setPosition(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
            if (!ForgeEventFactory.doSpecialSpawn(mob, world, (float) mob.posX,
                    (float) mob.posY, (float) mob.posZ)) {
                mob.onInitialSpawn(world.getDifficultyForLocation(spawn), null);
            }
            applySupportModifier(mob, SharedMonsterAttributes.MAX_HEALTH,
                    "194fec31-b36e-41fc-ad72-02a5cb891def",
                    -(mob.getRNG().nextDouble() + 0.5D) * 2.0D);
            applySupportModifier(mob, SharedMonsterAttributes.MOVEMENT_SPEED,
                    "5965c24d-8ac1-4f04-92ee-3d2724f976e8", -0.08D);
            mob.enablePersistence();
            mob.spawnExplosionParticle();
            if (world instanceof WorldServer) {
                WorldServer server = (WorldServer) world;
                ModNetwork.sendCommandBlockParticles(world, mob.getPositionVector(), 20,
                        rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(), 0.2D,
                        ModNetwork.COMMAND_BLOCK_PARTICLES_GAUSSIAN);
                server.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, mob.posX,
                        mob.posY, mob.posZ, 20, rand.nextGaussian(), rand.nextGaussian(),
                        rand.nextGaussian(), 0.01D);
            }
            world.spawnEntity(mob);
        }

        private static void applySupportModifier(EntityLivingBase entity,
                                                  net.minecraft.entity.ai.attributes.IAttribute attribute,
                                                  String uuid, double amount) {
            net.minecraft.entity.ai.attributes.IAttributeInstance instance =
                    entity.getEntityAttribute(attribute);
            if (instance != null) {
                instance.applyModifier(new net.minecraft.entity.ai.attributes.AttributeModifier(
                        UUID.fromString(uuid), "Wither storm support mob", amount, 0));
            }
        }

        @Nullable
        private SickenedMobEntity createWeightedSupportMob(boolean illagersOnly, boolean difficult) {
            String[] ids;
            int[] weights;
            if (illagersOnly) {
                ids = new String[] {"pillager", "vindicator"};
                weights = new int[] {3, 3};
            } else if (difficult) {
                ids = new String[] {"zombie", "villager", "skeleton", "spider", "creeper", "snow_golem",
                        "phantom", "bee", "parrot", "wolf", "cat", "pillager", "vindicator"};
                weights = new int[] {8, 6, 8, 6, 1, 1, 3, 6, 1, 2, 2, 3, 6};
            } else {
                ids = new String[] {"zombie", "villager", "skeleton", "spider", "creeper", "snow_golem",
                        "chicken", "cow", "mushroom_cow", "pig", "bee", "parrot", "wolf", "cat",
                        "pillager", "vindicator"};
                weights = new int[] {8, 4, 8, 4, 1, 2, 3, 3, 1, 3, 4, 4, 4, 4, 3, 3};
            }
            int total = 0;
            for (int weight : weights) total += weight;
            int selected = rand.nextInt(total);
            String id = ids[ids.length - 1];
            for (int index = 0; index < weights.length; index++) {
                selected -= weights[index];
                if (selected < 0) { id = ids[index]; break; }
            }
            if ("zombie".equals(id)) return new SickenedZombieEntity(world);
            if ("villager".equals(id)) return new SickenedVillagerEntity(world);
            if ("skeleton".equals(id)) return new SickenedSkeletonEntity(world);
            if ("spider".equals(id)) return new SickenedSpiderEntity(world);
            if ("creeper".equals(id)) return new SickenedCreeperEntity(world);
            if ("snow_golem".equals(id)) return new SickenedSnowGolemEntity(world);
            if ("chicken".equals(id)) return new SickenedChickenEntity(world);
            if ("cow".equals(id)) return new SickenedCowEntity(world);
            if ("mushroom_cow".equals(id)) return new SickenedMushroomCowEntity(world);
            if ("pig".equals(id)) return new SickenedPigEntity(world);
            if ("bee".equals(id)) return new SickenedBeeEntity(world);
            if ("parrot".equals(id)) return new SickenedParrotEntity(world);
            if ("wolf".equals(id)) return new SickenedWolfEntity(world);
            if ("cat".equals(id)) return new SickenedCatEntity(world);
            if ("pillager".equals(id)) return new SickenedPillagerEntity(world);
            return new SickenedVindicatorEntity(world);
        }

        public void setOwner(@Nullable WitherStormEntity owner) {
            ownerUuid = owner == null ? null : owner.getUniqueID();
        }

        @Nullable
        public WitherStormEntity getOwner() {
            if (ownerUuid == null) return null;
            for (Entity entity : world.loadedEntityList) {
                if (entity instanceof WitherStormEntity && ownerUuid.equals(entity.getUniqueID())) {
                    return (WitherStormEntity) entity;
                }
            }
            return null;
        }

        public boolean isNonBossMode() { return dataManager.get(NON_BOSS_MODE); }

        @Override
        public SoundEvent getBossTheme() {
            return ModSounds.get(shouldIncreaseDifficulty()
                    ? "withered_symbiont_intense_theme" : "withered_symbiont_theme");
        }

        @Override
        public boolean shouldPlayBossTheme() {
            return isEntityAlive();
        }

        @Override
        public int getBossThemePriority() {
            return 2;
        }

        @Override
        public int getBossThemeFadeTime() {
            return 120;
        }

        @Override
        public double getBossThemeDistance() {
            return 45.0D;
        }

        @Override
        public Vec3d getBossThemePosition() {
            return getPositionVector();
        }

        public boolean canBeAttackedWhenNotVulnerable() {
            return WitherStormConfig.attackableWhenNotVulnerable;
        }

        public void setNonBossMode(boolean mode) {
            dataManager.set(NON_BOSS_MODE, mode);
            experienceValue = mode ? 25 : 150;
        }

        public boolean isRushMode() { return dataManager.get(RUSH_MODE); }
        public void setRushMode(boolean mode) { dataManager.set(RUSH_MODE, mode); }

        @Override
        protected boolean canDespawn() {
            return !isConverting();
        }

        @Override
        protected void despawnEntity() {
            if (isNoDespawnRequired() || hasCustomName() || isConverting()) {
                idleTime = 0;
                return;
            }
            List<WitherStormEntity> nearbyStorms = world.getEntitiesWithinAABB(
                    WitherStormEntity.class, getEntityBoundingBox().grow(400.0D),
                    storm -> storm != null && storm.isEntityAlive());
            if (nearbyStorms.isEmpty()) super.despawnEntity();
            else idleTime = 0;
        }

        @Override
        public void addTrackingPlayer(EntityPlayerMP player) {
            super.addTrackingPlayer(player);
            bossInfo.addPlayer(player);
        }

        @Override
        public void removeTrackingPlayer(EntityPlayerMP player) {
            super.removeTrackingPlayer(player);
            bossInfo.removePlayer(player);
        }

        @Override
        public void setCustomNameTag(String name) {
            super.setCustomNameTag(name);
            bossInfo.setName(getDisplayName());
        }

        @Override protected SoundEvent getAmbientSound() { return isVulnerable() ? null : ModSounds.get("withered_symbiont_ambient"); }
        @Override protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.get("withered_symbiont_hurt"); }
        @Override protected SoundEvent getDeathSound() { return ModSounds.get(isNonBossMode() ? "withered_symbiont_normal_death" : "withered_symbiont_death"); }
        @Override protected float getSoundVolume() { return isDead ? 1.0F : super.getSoundVolume(); }
        protected float getDeathMaxRotation() { return 1.0F; }

        protected void playStepSound(BlockPos pos, IBlockState blockIn) {
            playSound(ModSounds.get("withered_symbiont_step"), 0.3F, 1.0F);
        }

        @Override
        protected void dropLoot(boolean wasRecentlyHit, int lootingModifier, DamageSource source) {
            if (isNonBossMode() || !(world instanceof WorldServer)) {
                super.dropLoot(wasRecentlyHit, lootingModifier, source);
                return;
            }
            ResourceLocation tableId = getLootTable();
            if (tableId == null) return;
            LootTable table = world.getLootTableManager().getLootTableFromLocation(tableId);
            LootContext.Builder context = new LootContext.Builder((WorldServer) world)
                    .withLootedEntity(this).withDamageSource(source);
            if (wasRecentlyHit && attackingPlayer != null) {
                context.withPlayer(attackingPlayer).withLuck(attackingPlayer.getLuck());
            }
            dropItems.clear();
            dropItems.addAll(table.generateLootForPools(rand, context.build()));
        }

        @Override
        protected void onDeathUpdate() {
            if (isNonBossMode()) {
                super.onDeathUpdate();
                return;
            }
            specialDeathTime++;
            if (world instanceof WorldServer) {
                int particles = Math.max(0, (320 - specialDeathTime) / 40);
                ((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                        posX, posY + height * 0.5D, posZ, particles,
                        width * 0.5D, height * 0.5D, width * 0.5D, 0.02D);
            }
            float turn = MathHelper.clamp(MathHelper.wrapDegrees(-50.0F - rotationPitch), -3.0F, 3.0F);
            rotationPitch += turn;
            if (specialDeathTime < 320) return;
            if (!world.isRemote) {
                distributeCapturedDrops();
                if (world.getGameRules().getBoolean("doMobLoot")) {
                    int experience = getExperiencePoints(attackingPlayer);
                    while (experience > 0) {
                        int split = EntityXPOrb.getXPSplit(experience);
                        experience -= split;
                        world.spawnEntity(new EntityXPOrb(world, posX, posY, posZ, split));
                    }
                }
                if (world instanceof WorldServer) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,
                            posX, posY + height * 0.5D, posZ, 20,
                            width * 0.5D, height * 0.5D, width * 0.5D, 0.02D);
                }
            }
            setDead();
        }

        private void distributeCapturedDrops() {
            List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class,
                    getEntityBoundingBox().grow(20.0D), player -> player != null && player.isEntityAlive());
            if (WitherStormConfig.bookDropsInInventory
                    && players.size() > 1 && !fightContributors.isEmpty()) {
                for (UUID contributor : fightContributors) {
                    for (EntityPlayer player : players) {
                        if (!player.getUniqueID().equals(contributor)) continue;
                        for (ItemStack stack : dropItems) {
                            ItemStack copy = stack.copy();
                            if (!player.inventory.addItemStackToInventory(copy) && !copy.isEmpty()) {
                                EntityItem dropped = player.entityDropItem(copy, 0.0F);
                                if (dropped != null) dropped.setOwner(player.getName());
                            }
                        }
                    }
                }
            } else {
                for (ItemStack stack : dropItems) {
                    EntityItem dropped = entityDropItem(stack.copy(), 8.0F);
                    if (dropped != null) {
                        dropped.motionX = 0.0D;
                        dropped.motionY = -0.08D;
                        dropped.motionZ = 0.0D;
                        dropped.setNoPickupDelay();
                    }
                }
            }
            dropItems.clear();
        }

        @Override
        public void onDeath(DamageSource cause) {
            super.onDeath(cause);
            if (world.isRemote) return;
            WitherStormEntity owner = getOwner();
            Entity source = cause == null ? null : cause.getTrueSource();
            if (source instanceof EntityPlayer) {
                SymbiontSummoningManager.markKilledSymbiont((EntityPlayer) source, owner);
            }
            for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class,
                    getEntityBoundingBox().grow(20.0D))) {
                SymbiontSummoningManager.makeInvulnerable(player);
            }
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            if (ownerUuid != null) compound.setUniqueId("Summoner", ownerUuid);
            compound.setBoolean("IsNonBossMode", isNonBossMode());
            compound.setBoolean("IsRushMode", isRushMode());
            compound.setInteger("Stage", getStage().ordinal());
            compound.setInteger("StageTicks", stageTicks);
            ResourceLocation spellId = WitherStormModRegistries.getSpellTypeId(getSpell());
            compound.setString("Spell", spellId == null ? "witherstormmod:empty" : spellId.toString());
            compound.setInteger("SpellCastingTicks", getSpellCastingTime());
            compound.setInteger("NextSpellPick", nextSpellPickCount);
            compound.setBoolean("Smashing", isSmashing());
            compound.setInteger("SmashAirTime", smashAirTime);
            compound.setInteger("SpellsUsed", spellsUsed);
            compound.setInteger("AttackDelay", getAttackDelay());
            compound.setBoolean("ShouldNotGoOverHalf", dataManager.get(SHOULD_NOT_GO_OVER_HALF));
            compound.setBoolean("HealthScaled", healthScaled);
            NBTTagList savedDrops = new NBTTagList();
            for (ItemStack stack : dropItems) savedDrops.appendTag(stack.writeToNBT(new NBTTagCompound()));
            compound.setTag("DropItems", savedDrops);
            NBTTagList contributors = new NBTTagList();
            for (UUID id : fightContributors) contributors.appendTag(new NBTTagString(id.toString()));
            compound.setTag("FightContributors", contributors);
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            ownerUuid = compound.hasUniqueId("Summoner") ? compound.getUniqueId("Summoner")
                    : compound.hasUniqueId("WitherStormOwner")
                    ? compound.getUniqueId("WitherStormOwner") : null;
            setNonBossMode(compound.getBoolean("IsNonBossMode"));
            setRushMode(compound.getBoolean("IsRushMode"));
            setStage(BossfightStage.byOrdinal(compound.getInteger("Stage")));
            stageTicks = compound.getInteger("StageTicks");
            if (compound.hasKey("Spell", 8)) {
                SpellType savedSpell = WitherStormModRegistries.getSpellType(compound.getString("Spell"));
                setSpell(savedSpell == null
                        ? SymbiontSpells.apiType(SymbiontSpells.Type.EMPTY) : savedSpell);
            } else {
                // 迁移旧版以枚举 ordinal 保存的法术。
                setSpell(SymbiontSpells.Type.byOrdinal(compound.getInteger("Spell")));
            }
            setSpellCastingTime(compound.getInteger("SpellCastingTicks"));
            nextSpellPickCount = compound.getInteger("NextSpellPick");
            setSmashing(compound.getBoolean("Smashing"));
            smashAirTime = compound.getInteger("SmashAirTime");
            spellsUsed = compound.getInteger("SpellsUsed");
            setAttackDelay(compound.getInteger("AttackDelay"));
            if (compound.hasKey("ShouldNotGoOverHalf")) {
                dataManager.set(SHOULD_NOT_GO_OVER_HALF, compound.getBoolean("ShouldNotGoOverHalf"));
            }
            healthScaled = compound.getBoolean("HealthScaled");
            dropItems.clear();
            NBTTagList savedDrops = compound.getTagList("DropItems", 10);
            for (int index = 0; index < savedDrops.tagCount(); index++) {
                dropItems.add(new ItemStack(savedDrops.getCompoundTagAt(index)));
            }
            fightContributors.clear();
            NBTTagList contributors = compound.getTagList("FightContributors", 8);
            for (int index = 0; index < contributors.tagCount(); index++) {
                try { fightContributors.add(UUID.fromString(contributors.getStringTagAt(index))); }
                catch (IllegalArgumentException ignored) { }
            }
        }

        public enum BossfightStage {
            ATTACKING {
                @Override boolean shouldMoveToNextStage(WitheredSymbiontEntity entity) {
                    return entity.getSpellsUsed() > 5 && !entity.isCastingSpell()
                            && entity.getStageTicks() % 80 == 0 && entity.getAttackTarget() != null;
                }

                @Override void finish(WitheredSymbiontEntity entity) {
                    entity.spellsUsed = 0;
                    entity.setSpell(SymbiontSpells.Type.EMPTY);
                }
            },
            SUMMONING,
            VULNERABLE {
                @Override boolean shouldMoveToNextStage(WitheredSymbiontEntity entity) {
                    return entity.getStageTicks() > 4800;
                }
            };

            boolean shouldMoveToNextStage(WitheredSymbiontEntity entity) { return false; }
            void finish(WitheredSymbiontEntity entity) { }
            BossfightStage next() { return values()[(ordinal() + 1) % values().length]; }
            static BossfightStage byOrdinal(int value) {
                return value >= 0 && value < values().length ? values()[value] : ATTACKING;
            }
        }

        private static final class SymbiontAttackGoal extends EntityAIAttackMelee {
            private final WitheredSymbiontEntity entity;
            private SymbiontAttackGoal(WitheredSymbiontEntity entity) { super(entity, 1.0D, true); this.entity = entity; }
            @Override public boolean shouldExecute() {
                return entity.getStage() == BossfightStage.ATTACKING && !entity.isCastingSpell() && super.shouldExecute();
            }
            @Override public boolean shouldContinueExecuting() {
                return entity.getStage() == BossfightStage.ATTACKING && !entity.isCastingSpell()
                        && super.shouldContinueExecuting();
            }
        }

        private static final class PrepareSpellGoal extends EntityAIBase {
            private final WitheredSymbiontEntity entity;
            private PrepareSpellGoal(WitheredSymbiontEntity entity) { this.entity = entity; }
            @Override public boolean shouldExecute() {
                EntityLivingBase target = entity.getAttackTarget();
                return target != null && target.isEntityAlive() && !entity.isCastingSpell() && entity.canPickSpell();
            }
            @Override public boolean shouldContinueExecuting() {
                EntityLivingBase target = entity.getAttackTarget();
                return target != null && target.isEntityAlive() && entity.canPickSpell();
            }
            @Override public void startExecuting() {
                List<SpellType> spells = new ArrayList<SpellType>();
                SpellType empty = SymbiontSpells.apiType(SymbiontSpells.Type.EMPTY);
                for (SpellType spell : WitherStormModRegistries.getSpellTypes()) {
                    if (spell != empty && spell != entity.getSpell()) spells.add(spell);
                }
                if (spells.isEmpty()) return;
                entity.setSpell(spells.get(entity.getRNG().nextInt(spells.size())));
                entity.useSpellGoal.nextAttackTick = entity.ticksExisted + 40 + entity.getRNG().nextInt(20);
                entity.playSound(ModSounds.get("withered_symbiont_prepare_spell"), 4.0F, 1.0F);
                entity.nextSpellPickCount = 400 + entity.getRNG().nextInt(400)
                        - (entity.shouldIncreaseDifficulty() ? 320 : 0);
                if (entity.shouldNotGoOverHalfHealth() && entity.getHealth() / entity.getMaxHealth() <= 0.5F) {
                    entity.setHalfHealthLimit(false);
                }
            }
        }

        private static final class UseSpellGoal extends EntityAIBase {
            private final WitheredSymbiontEntity entity;
            private int nextAttackTick;
            private UseSpellGoal(WitheredSymbiontEntity entity) { this.entity = entity; setMutexBits(1); }
            @Override public boolean shouldExecute() {
                EntityLivingBase target = entity.getAttackTarget();
                return target != null && target.isEntityAlive() && !entity.isCastingSpell()
                        && entity.hasSpell() && entity.spellInstance != null && entity.ticksExisted > nextAttackTick;
            }
            @Override public boolean shouldContinueExecuting() {
                EntityLivingBase target = entity.getAttackTarget();
                return target != null && target.isEntityAlive() && entity.hasSpell()
                        && entity.ticksExisted >= nextAttackTick || entity.isCastingSpell();
            }
            @Override public void startExecuting() {
                float modifier = worldDifficulty(entity) + (entity.shouldIncreaseDifficulty() ? 60.0F : 0.0F);
                int delay = entity.spellInstance.getDelay(entity.getRNG(), modifier);
                delay = Math.max(delay, entity.getSpell().spellTime() + 10);
                nextAttackTick = entity.ticksExisted + delay;
                entity.playSound(ModSounds.get("withered_symbiont_cast_spell"), 4.0F, 1.0F);
                entity.beginSpellCasting();
                if (entity.getAttackTarget() instanceof EntityPlayer) entity.spellUsed();
            }
            private static float worldDifficulty(WitheredSymbiontEntity entity) {
                return entity.world.getDifficultyForLocation(new BlockPos(entity)).getAdditionalDifficulty();
            }
        }

        private static final class SummonMobsGoal extends EntityAIBase {
            private final WitheredSymbiontEntity entity;
            private int time;
            private SummonMobsGoal(WitheredSymbiontEntity entity) { this.entity = entity; setMutexBits(7); }
            @Override public boolean shouldExecute() {
                EntityLivingBase target = entity.getAttackTarget();
                return target != null && target.isEntityAlive();
            }
            @Override public boolean shouldContinueExecuting() { return shouldExecute() && time > 0; }
            @Override public void startExecuting() {
                time = 60 + entity.getRNG().nextInt(60) + (entity.shouldIncreaseDifficulty() ? 40 : 0);
                entity.playSound(ModSounds.get("withered_symbiont_summon"), 4.0F, 1.0F);
            }
            @Override public void updateTask() {
                if (time > 0 && --time % 10 == 0) entity.summonSupportMob(false);
            }
            @Override public void resetTask() { entity.nextStage(); }
        }

        private static final class DoNothingGoal extends EntityAIBase {
            private final WitheredSymbiontEntity entity;
            private DoNothingGoal(WitheredSymbiontEntity entity) { this.entity = entity; setMutexBits(7); }
            @Override public boolean shouldExecute() { return entity.isVulnerable(); }
            @Override public boolean shouldContinueExecuting() { return entity.isVulnerable(); }
            @Override public void startExecuting() {
                // 1.12 的目标切换不会总是重置刚被移除近战目标留下的导航路径。
                entity.getNavigator().clearPath();
            }
            @Override public void updateTask() {
                entity.getNavigator().clearPath();
                float turn = MathHelper.clamp(MathHelper.wrapDegrees(55.0F - entity.rotationPitch), -3.0F, 3.0F);
                entity.rotationPitch += turn;
            }
        }
    }

    public static class TaintedSlimeEntity extends EntitySlime {
        public TaintedSlimeEntity(World world) {
            super(world);
        }
    }

    private static class RandomFlyingAI extends EntityAIBase {
        private final SickenedMobEntity entity;
        private final double speed;
        private final int horizontalRange;
        private final int verticalRange;

        RandomFlyingAI(SickenedMobEntity entity, double speed, int horizontalRange, int verticalRange) {
            this.entity = entity;
            this.speed = speed;
            this.horizontalRange = horizontalRange;
            this.verticalRange = verticalRange;
        }

        @Override
        public boolean shouldExecute() {
            return entity.getAttackTarget() == null && (!entity.getMoveHelper().isUpdating() || entity.getRNG().nextInt(5) == 0);
        }

        @Override
        public void startExecuting() {
            double x = entity.posX + entity.getRNG().nextInt(horizontalRange * 2 + 1) - horizontalRange;
            double y = entity.posY + entity.getRNG().nextInt(verticalRange * 2 + 1) - verticalRange;
            double z = entity.posZ + entity.getRNG().nextInt(horizontalRange * 2 + 1) - horizontalRange;
            entity.getMoveHelper().setMoveTo(x, Math.max(2.0D, y), z, speed);
        }

        @Override public boolean shouldContinueExecuting() { return false; }
    }

    private static class FlyingAttackAI extends EntityAIBase {
        private final SickenedMobEntity entity;
        private final double speed;
        private final int attackDelay;
        private int cooldown;

        FlyingAttackAI(SickenedMobEntity entity, double speed, int attackDelay) {
            this.entity = entity;
            this.speed = speed;
            this.attackDelay = attackDelay;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (entity instanceof SickenedBeeEntity
                    && (!((SickenedBeeEntity) entity).isAngry()
                    || ((SickenedBeeEntity) entity).hasStung())) return false;
            return entity.getAttackTarget() != null;
        }
        @Override public boolean shouldContinueExecuting() { return shouldExecute(); }

        @Override
        public void updateTask() {
            EntityLivingBase target = entity.getAttackTarget();
            if (target == null) return;
            if (cooldown > 0) --cooldown;
            entity.getMoveHelper().setMoveTo(target.posX, target.posY + target.getEyeHeight() * 0.5D, target.posZ, speed);
            entity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            double reach = entity.width + target.width + 0.8D;
            if (cooldown <= 0 && entity.getDistanceSq(target) <= reach * reach) {
                entity.attackEntityAsMob(target);
                cooldown = attackDelay;
                entity.getMoveHelper().setMoveTo(entity.posX, entity.posY + 4.0D, entity.posZ, 0.8D);
            }
        }
    }

    private static void fireSickenedArrow(SickenedMobEntity shooter, EntityLivingBase target, float distanceFactor) {
        fireSickenedArrow(shooter, target, distanceFactor, 0.0F);
    }

    private static void fireSickenedArrow(SickenedMobEntity shooter, EntityLivingBase target,
                                          float distanceFactor, float potentChance) {
        EntityTippedArrow arrow = new EntityTippedArrow(shooter.world, shooter);
        double dx = target.posX - shooter.posX;
        double dy = target.getEntityBoundingBox().minY + target.height / 3.0F - arrow.posY;
        double dz = target.posZ - shooter.posZ;
        double arc = MathHelper.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + arc * 0.2D, dz, 1.6F, 14 - shooter.world.getDifficulty().getId() * 4);
        arrow.setDamage(2.0D + distanceFactor * 2.0D);
        ItemStack weapon = shooter.getHeldItemMainhand();
        int power = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, weapon);
        if (power > 0) arrow.setDamage(arrow.getDamage() + power * 0.5D + 0.5D);
        int punch = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, weapon);
        if (punch > 0) arrow.setKnockbackStrength(punch);
        if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, weapon) > 0) arrow.setFire(100);
        if (potentChance > 0.0F && shooter.getRNG().nextFloat() < potentChance) {
            arrow.addEffect(new PotionEffect(MobEffects.WITHER, 40, 1));
        }
        shooter.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (shooter.getRNG().nextFloat() * 0.4F + 0.8F));
        shooter.world.spawnEntity(arrow);
    }

    @Nullable
    private static Item getFutureTrident() {
        return ForgeRegistries.ITEMS.getValue(FUTURE_MC_TRIDENT);
    }

    private static void initializeTaintedPumpkinHead(SickenedMobEntity entity) {
        ItemStack head = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (head.isEmpty()) {
            Calendar calendar = entity.world.getCurrentDate();
            if (calendar.get(Calendar.MONTH) == Calendar.OCTOBER
                    && calendar.get(Calendar.DAY_OF_MONTH) == 31
                    && entity.getRNG().nextFloat() < 0.25F) {
                head = new ItemStack(entity.getRNG().nextFloat() < 0.1F
                        ? Blocks.LIT_PUMPKIN : Blocks.PUMPKIN);
                entity.setItemStackToSlot(EntityEquipmentSlot.HEAD, head);
                entity.setDropChance(EntityEquipmentSlot.HEAD, 0.0F);
            }
        }

        Item headItem = head.getItem();
        if (headItem == Item.getItemFromBlock(Blocks.LIT_PUMPKIN)) {
            entity.setItemStackToSlot(EntityEquipmentSlot.HEAD,
                    new ItemStack(ModBlocks.get("tainted_jack_o_lantern")));
        } else if (headItem == Item.getItemFromBlock(Blocks.PUMPKIN)) {
            entity.setItemStackToSlot(EntityEquipmentSlot.HEAD,
                    new ItemStack(ModBlocks.get("tainted_carved_pumpkin")));
        }
    }

    private static class SickenedSnowball extends EntitySnowball {
        private final boolean potent;

        SickenedSnowball(World world, EntityLivingBase thrower, boolean potent) {
            super(world, thrower);
            this.potent = potent;
        }

        @Override
        protected void onImpact(RayTraceResult result) {
            if (!world.isRemote && potent && result.entityHit instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) result.entityHit;
                target.addPotionEffect(new PotionEffect(MobEffects.WITHER, 150, 1));
                if (world instanceof WorldServer) {
                    WorldServer server = (WorldServer) world;
                    for (int index = 0; index < 5; index++) {
                        double particleX = target.posX + rand.nextGaussian() * 0.5D;
                        double particleY = target.posY + rand.nextGaussian() * 0.5D;
                        double particleZ = target.posZ + rand.nextGaussian() * 0.5D;
                        Vec3d velocity = new Vec3d(particleX, particleY, particleZ)
                                .subtract(target.getPositionEyes(1.0F)).normalize().scale(0.1D);
                        server.spawnParticle(EnumParticleTypes.SMOKE_LARGE, particleX, particleY, particleZ,
                                0, velocity.x, velocity.y, velocity.z, 1.0D);
                    }
                }
            }
            super.onImpact(result);
        }
    }
}
