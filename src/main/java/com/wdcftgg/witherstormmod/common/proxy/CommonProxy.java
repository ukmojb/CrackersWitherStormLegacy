package com.wdcftgg.witherstormmod.common.proxy;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import com.wdcftgg.witherstormmod.common.tile.WitheredPhlegmTileEntity;
import com.wdcftgg.witherstormmod.common.tile.AbstractSuperBeaconTileEntity;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Set;
import net.minecraft.potion.Potion;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
    }

    public Object createCrossbowEnderPearlAmmo() {
        return null;
    }

    public void registerCrossbowModModels() {
    }

    public void handleShakeScreen(float duration, float power) {
    }

    public void handleBlindScreen(int duration, int fadeInDuration, int fadeOutDuration) {
    }

    public void handleGlobalSound(ResourceLocation sound, float volume, float pitch) {
    }

    public void handleStopSound(ResourceLocation sound, SoundCategory category) {
    }

    public void handlePhasometerObservation(EnumHand hand, int dimension,
                                            int remainingUseTicks,
                                            NBTTagCompound observation) {
    }

    public void handleFormidibombExplosion(int sourceEntityId, double x, double y, double z,
                                           int radius, int squish) {
    }

    public void spawnWitheredPhlegmParticles(World world, BlockPos pos, boolean powered,
                                             java.util.Random random) {
    }

    public void spawnPhlegmParticle(World world, double x, double y, double z,
                                    double motionX, double motionY, double motionZ) {
    }

    public void spawnSuperBeaconResummonParticle(World world, BlockPos pos,
                                                  java.util.Random random) {
    }

    public void handleSuperBeaconParticles(BlockPos pos, int type) {
    }

    public void handleSuperBeaconValidEffects(Set<Potion> effects) {
    }

    public void handleCommandBlockParticles(ModNetwork.CommandBlockParticlesMessage message) {
    }

    public void handleCommandBlockTickParticles(ModNetwork.CommandBlockTickParticlesMessage message) {
    }

    public Object createWitheredPhlegmGui(EntityPlayer player, WitheredPhlegmTileEntity tile) {
        return null;
    }

    public Object createSuperBeaconGui(EntityPlayer player, AbstractSuperBeaconTileEntity tile) {
        return null;
    }

    public void handleDistantSuperBeacon(ModNetwork.DistantSuperBeaconMessage message) {
    }

    public void handleBossThemeAccess(int entityId, boolean allowed) {
    }

    public void handleCreateDebris(int entityId, boolean hidden) {
    }

    public void handleWitherStormLoop(ModNetwork.WitherStormLoopMessage message) {
    }

    public void handleWitherStormRotation(int entityId, float xRotation, float yRotation) {
    }

    public void handleWitherSicknessSync(int entityId, NBTTagCompound data) {
    }

    public void handleDamagingProjectileSync(int entityId, double accelerationX,
                                               double accelerationY, double accelerationZ) {
    }

    public void handleHeadAttacked(int entityId, int head) {
    }
}
