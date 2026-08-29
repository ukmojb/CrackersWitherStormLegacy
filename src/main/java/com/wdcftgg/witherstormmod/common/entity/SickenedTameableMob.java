package com.wdcftgg.witherstormmod.common.entity;

import com.google.common.base.Optional;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;




abstract class SickenedTameableMob extends SickenedMobEntity implements IEntityOwnable {
    private static final DataParameter<Byte> TAME_FLAGS = EntityDataManager.createKey(
            SickenedTameableMob.class, DataSerializers.BYTE);
    private static final DataParameter<Optional<UUID>> OWNER = EntityDataManager.createKey(
            SickenedTameableMob.class, DataSerializers.OPTIONAL_UNIQUE_ID);

    SickenedTameableMob(World world) {
        super(world);
    }

    @Override
    protected int getSickenedExperience() {
        return 1 + rand.nextInt(3);
    }

    @Override
    protected boolean growsFromChild() {
        return true;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TAME_FLAGS, (byte) 0);
        dataManager.register(OWNER, Optional.absent());
    }

    public boolean isSickenedTamed() {
        return (dataManager.get(TAME_FLAGS) & 4) != 0;
    }

    public void setSickenedTamed(boolean tamed) {
        byte flags = dataManager.get(TAME_FLAGS);
        dataManager.set(TAME_FLAGS, tamed ? (byte) (flags | 4) : (byte) (flags & ~4));
    }

    public boolean isSickenedSitting() {
        return false;
    }

    public void setSickenedSitting(boolean sitting) {

    }

    @Nullable
    @Override
    public UUID getOwnerId() {
        return dataManager.get(OWNER).orNull();
    }

    public void setOwnerId(@Nullable UUID ownerId) {
        dataManager.set(OWNER, Optional.fromNullable(ownerId));
    }

    @Nullable
    @Override
    public EntityLivingBase getOwner() {
        try {
            UUID ownerId = getOwnerId();
            return ownerId == null ? null : world.getPlayerEntityByUUID(ownerId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public void copySpeciesDataFrom(EntityLivingBase original) {
        super.copySpeciesDataFrom(original);
        if (!(original instanceof EntityTameable)) return;
        EntityTameable tameable = (EntityTameable) original;
        setOwnerId(tameable.getOwnerId());
        setSickenedTamed(tameable.isTamed());
    }

    @Override
    public void copySpeciesDataTo(EntityLivingBase cured) {
        super.copySpeciesDataTo(cured);
        if (!(cured instanceof EntityTameable)) return;
        EntityTameable tameable = (EntityTameable) cured;
        tameable.setOwnerId(getOwnerId());
        tameable.setTamed(isSickenedTamed());
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        UUID ownerId = getOwnerId();
        compound.setString("OwnerUUID", ownerId == null ? "" : ownerId.toString());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        String owner = compound.getString("OwnerUUID");
        if (!owner.isEmpty()) {
            try {
                setOwnerId(UUID.fromString(owner));
                setSickenedTamed(true);
            } catch (IllegalArgumentException ignored) {
                setOwnerId(null);
                setSickenedTamed(false);
            }
        } else {
            setOwnerId(null);
            setSickenedTamed(false);
        }
    }
}
