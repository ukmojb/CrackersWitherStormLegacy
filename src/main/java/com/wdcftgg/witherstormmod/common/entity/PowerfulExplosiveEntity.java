package com.wdcftgg.witherstormmod.common.entity;

import com.google.common.base.Optional;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.item.FormidibombItem;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public abstract class PowerfulExplosiveEntity extends EntityTNTPrimed {

    protected PowerfulExplosiveEntity(World world) {
        super(world);
    }

    protected PowerfulExplosiveEntity(World world, double positionX, double positionY, double positionZ, EntityLivingBase igniter) {
        super(world, positionX, positionY, positionZ, igniter);
    }

    protected abstract float getExplosionStrength();

    protected boolean causesFire() {
        return true;
    }

    protected void beforeExplosion() {
    }

    protected void explode() {
        world.newExplosion(this, posX, posY + height / 16.0F, posZ, getExplosionStrength(), causesFire(), true);
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (!hasNoGravity()) {
            motionY -= 0.03999999910593033D;
        }
        move(MoverType.SELF, motionX, motionY, motionZ);
        motionX *= 0.9800000190734863D;
        motionY *= 0.9800000190734863D;
        motionZ *= 0.9800000190734863D;
        if (onGround) {
            motionX *= 0.699999988079071D;
            motionZ *= 0.699999988079071D;
            motionY *= -0.5D;
        }
        setFuse(getFuse() - 1);
        if (getFuse() <= 0) {
            setDead();
            if (!world.isRemote) {
                beforeExplosion();
                explode();
            }
        } else {
            handleWaterMovement();
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX, posY + 0.5D, posZ, 0.0D, 0.0D, 0.0D);
        }
    }

    public static class SuperTntEntity extends PowerfulExplosiveEntity {
        public SuperTntEntity(World world) {
            super(world);
            setFuse(320);
        }

        public SuperTntEntity(World world, double positionX, double positionY, double positionZ,
                              EntityLivingBase igniter) {
            super(world, positionX, positionY, positionZ, igniter);
            setFuse(320);
        }

        @Override protected float getExplosionStrength() { return 32.0F; }

        @Override protected boolean causesFire() { return false; }

        /** 对应上游 MixinPrimedTnt：牵引光束内的超级 TNT 靠近头部立即爆炸，远离时拉长引信。 */
        @Override
        public void onUpdate() {
            updateFuseInTractorBeam();
            super.onUpdate();
        }

        private void updateFuseInTractorBeam() {
            if (world.isRemote || isDead) return;
            for (WitherStormEntity storm : world.getEntitiesWithinAABB(WitherStormEntity.class,
                    getEntityBoundingBox().grow(100.0D, 200.0D, 100.0D))) {
                if (storm.isDeadOrPlayingDead()) continue;
                int head = storm.findContainingTractorBeamHead(this, 4.0D);
                if (head < 0) continue;
                Vec3d headPosition = storm.getHeadPosition(head, 1.0F);
                if (headPosition == null) return;
                if (getDistanceSq(headPosition.x, headPosition.y, headPosition.z) > 144.0D) {
                    if (getFuse() == 20) setFuse(80);
                } else {
                    setFuse(0);
                }
                return;
            }
        }
    }

    public static class FormidibombEntity extends PowerfulExplosiveEntity implements FormidibombSource {
        private static final DataParameter<Optional<IBlockState>> BLOCK_STATE = EntityDataManager.createKey(
                FormidibombEntity.class, DataSerializers.OPTIONAL_BLOCK_STATE);
        private static final DataParameter<Integer> START_FUSE = EntityDataManager.createKey(
                FormidibombEntity.class, DataSerializers.VARINT);
        private int airTime;

        public FormidibombEntity(World world) {
            super(world);
            initiateFuse(1200);
        }

        public FormidibombEntity(World world, double positionX, double positionY, double positionZ,
                                 @Nullable EntityLivingBase igniter) {
            this(world, positionX, positionY, positionZ, igniter, null, null);
        }

        public FormidibombEntity(World world, double positionX, double positionY, double positionZ,
                                 @Nullable EntityLivingBase igniter, @Nullable FormidibombSource previous,
                                 @Nullable IBlockState blockState) {
            super(world, positionX, positionY, positionZ, igniter);
            initiateFuse(1200);
            if (previous != null && previous.getStartFuse() > 0) {
                setFuse(previous.getFuseLife());
                setStartFuse(previous.getStartFuse());
            }
            setBlockState(blockState);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(BLOCK_STATE, Optional.<IBlockState>absent());
            dataManager.register(START_FUSE, 0);
        }

        @Override protected float getExplosionStrength() { return 32.0F; }

        public void initiateFuse(int fuse) {
            setFuse(fuse);
            setStartFuse(fuse);
        }

        public void setStartFuse(int startFuse) { dataManager.set(START_FUSE, startFuse); }
        @Override public int getStartFuse() { return dataManager.get(START_FUSE); }
        @Override public int getFuseLife() { return getFuse(); }
        @Override public Vec3d getFormidibombPosition() { return getPositionVector(); }
        @Override public boolean isFormidibombAlive() { return !isDead; }
        public int getAirTime() { return airTime; }

        public IBlockState getBlockState() {
            return dataManager.get(BLOCK_STATE).or(ModBlocks.get("formidibomb").getDefaultState());
        }

        public void setBlockState(@Nullable IBlockState blockState) {
            dataManager.set(BLOCK_STATE, Optional.fromNullable(blockState));
        }

        @Override
        public ItemStack getPickedResult(RayTraceResult target) {
            ItemStack stack = new ItemStack(ModBlocks.get("formidibomb"));
            FormidibombItem.setFuseState(stack, getFuse(), getStartFuse());
            return stack;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (onGround) airTime = 0;
            else ++airTime;
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setInteger("StartFuse", getStartFuse());
            compound.setTag("State", NBTUtil.writeBlockState(new NBTTagCompound(), getBlockState()));
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            if (compound.hasKey("StartFuse", 3)) setStartFuse(compound.getInteger("StartFuse"));
            if (compound.hasKey("State", 10)) setBlockState(NBTUtil.readBlockState(compound.getCompoundTag("State")));
        }

        @Override
        protected void explode() {
            FormidibombExplosion.explode(world, getTntPlacedBy(), 48 + world.rand.nextInt(9), 3, posX, posY, posZ);
            ModNetwork.shakeNear(world, posX, posY, posZ, 100.0D, 480.0F, 24.0F);
        }
    }
}
