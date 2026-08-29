package com.wdcftgg.witherstormmod.common.capability;

import com.wdcftgg.witherstormmod.Tags;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;


public final class WitherSicknessCapability {
    public static final ResourceLocation ID = new ResourceLocation(Tags.MOD_ID, "wither_sickness_tracker");

    @CapabilityInject(WitherSicknessTracker.class)
    public static final Capability<WitherSicknessTracker> TRACKER = null;

    private static boolean registered;

    private WitherSicknessCapability() {
    }

    public static synchronized void register() {
        if (registered) return;
        CapabilityManager.INSTANCE.register(WitherSicknessTracker.class,
                new Capability.IStorage<WitherSicknessTracker>() {
                    @Override
                    public NBTBase writeNBT(Capability<WitherSicknessTracker> capability,
                                            WitherSicknessTracker instance, EnumFacing side) {
                        return instance.write();
                    }

                    @Override
                    public void readNBT(Capability<WitherSicknessTracker> capability,
                                        WitherSicknessTracker instance, EnumFacing side, NBTBase nbt) {
                        if (nbt instanceof NBTTagCompound) instance.read((NBTTagCompound) nbt);
                    }
                }, WitherSicknessTracker::new);
        registered = true;
    }

    @Nullable
    public static WitherSicknessTracker get(EntityLivingBase entity) {
        if (entity == null || TRACKER == null) return null;
        return entity.getCapability(TRACKER, null);
    }

    public static final class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final WitherSicknessTracker tracker;

        public Provider(EntityLivingBase entity) {
            tracker = new WitherSicknessTracker(entity);
        }

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == TRACKER;
        }

        @Override
        @Nullable
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == TRACKER ? TRACKER.cast(tracker) : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return tracker.write();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            tracker.read(nbt);
        }
    }
}
