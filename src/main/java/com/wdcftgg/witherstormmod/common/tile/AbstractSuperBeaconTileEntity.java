package com.wdcftgg.witherstormmod.common.tile;

import com.wdcftgg.witherstormmod.common.beacon.SuperBeaconLogic;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.world.ChunkLoadingManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.LockCode;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

public abstract class AbstractSuperBeaconTileEntity extends TileEntity {
    protected int ticks;
    protected int activationTime;
    protected int beamHeight;
    protected Potion effect;
    protected boolean active;
    protected float activationAnimation;
    protected float previousActivationAnimation;
    protected int beaconLevel;
    int poweringUpAnimation;
    protected boolean showWorkingArea;
    protected int effectSetCooldown;
    protected String customName;
    private LockCode lockCode = LockCode.EMPTY_CODE;
    private final int ambientSoundOffset = new Random().nextInt(100);

    protected final void tickBeaconBase() {
        ticks++;
        if (world != null && !world.isRemote) {
            ModNetwork.updateDistantSuperBeacon(this);
        }
        if (hasReachedPowerUpClimax()) {
            activationTime++;
            if (beamHeight < 1024) beamHeight += activationTime / 2;
        }
        previousActivationAnimation = activationAnimation;
        boolean animate = shouldDoActivatedAnimation();
        activationAnimation += ((animate ? 1.0F : 0.0F) - activationAnimation) / 8.0F;

        if (world != null && !world.isRemote && isActive() && !isPoweringUp()
                && (ticks + ambientSoundOffset) % 80 == 0
                && ModSounds.get("withered_beacon_ambient") != null) {
            world.playSound(null, pos, ModSounds.get("withered_beacon_ambient"),
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        if (poweringUpAnimation > 0) {
            poweringUpAnimation--;
            doPoweringUpAnimation();
        }
        if (world != null && !world.isRemote) {
            if (isActive() && effect != null) applyEffect();
            if (effectSetCooldown > 0) effectSetCooldown--;
        }
    }

    protected abstract void applyEffect();

    protected void doPoweringUpAnimation() {
    }

    protected boolean shouldDoActivatedAnimation() {
        return isActive() && hasReachedPowerUpClimax();
    }

    protected void setActive(boolean value) {
        if (active == value) return;
        active = value;
        playSound(value ? "withered_beacon_activate" : "withered_beacon_deactivate", 1.0F, 1.0F);
        if (value) doActivationSequence();
        markAndNotify();
    }

    public void doActivationSequence() {
        beamHeight = 0;
        activationTime = 0;
        markAndNotify();
    }

    public void doPowerUp(EntityPlayerMP player) {
        playSound("withered_beacon_activate", 1.0F, 1.0F);
    }

    protected void playSound(String name, float volume, float pitch) {
        if (world == null || world.isRemote || ModSounds.get(name) == null) return;
        world.playSound(null, pos, ModSounds.get(name), SoundCategory.BLOCKS, volume,
                pitch + (world.rand.nextFloat() - 0.5F) * 0.35F);
    }

    public Set<Potion> getValidEffects() {
        return Collections.emptySet();
    }

    public boolean setEffect(Potion value) {
        if (value != null && !getValidEffects().contains(value)) return false;
        if (effect == value) return true;
        effect = value;
        markAndNotify();
        return true;
    }

    public Potion getEffect() {
        return effect;
    }

    public boolean isActive() {
        return active;
    }

    public int getBeaconLevel() {
        return beaconLevel;
    }

    public int getBeamHeight() {
        return beamHeight;
    }

    public int getTicks() {
        return ticks;
    }

    public float getActivationAnimation(float partialTicks) {
        return previousActivationAnimation
                + (activationAnimation - previousActivationAnimation) * partialTicks;
    }

    public boolean isPoweringUp() {
        return poweringUpAnimation > 0;
    }

    public boolean hasReachedPowerUpClimax() {
        return poweringUpAnimation <= SuperBeaconLogic.POWER_UP_CLIMAX;
    }

    public int getCooldown() {
        return effectSetCooldown;
    }

    public void setCooldown(int cooldown) {
        effectSetCooldown = Math.max(0, cooldown);
        markAndNotify();
    }

    public boolean showWorkingArea() {
        return showWorkingArea;
    }

    public void setShowWorkingArea(boolean show) {
        showWorkingArea = show;
        markAndNotify();
    }

    public int[] getBeamColor() {
        return new int[] {255, 255, 255};
    }

    public String getNameForGui() {
        return customName == null || customName.isEmpty()
                ? "container.witherstormmod.withered_beacon" : customName;
    }

    public void setCustomName(String name) {
        customName = name;
        markAndNotify();
    }

    public boolean hasCustomName() {
        return customName != null && !customName.isEmpty();
    }

    public ITextComponent getDisplayName() {
        return hasCustomName() ? new TextComponentString(customName)
                : new TextComponentTranslation("container.witherstormmod.withered_beacon");
    }

    public boolean isLocked() {
        return !lockCode.isEmpty();
    }

    public LockCode getLockCode() {
        return lockCode;
    }

    public void setLockCode(LockCode code) {
        lockCode = code == null ? LockCode.EMPTY_CODE : code;
        markAndNotify();
    }

    public boolean canPlayerUseItems(EntityPlayer player) {
        if (!isLocked()) return true;
        ItemStack held = player.getHeldItemMainhand();
        if (!held.isEmpty() && held.hasDisplayName()
                && lockCode.getLock().equals(held.getDisplayName())) {
            return true;
        }
        if (!world.isRemote) {
            player.sendStatusMessage(new TextComponentTranslation("container.isLocked", getDisplayName()), true);
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        return false;
    }

    public int getField(int id) {
        return id == 0 ? beaconLevel
                : id == 1 ? (effect == null ? -1 : Potion.getIdFromPotion(effect))
                : id == 2 ? (showWorkingArea ? 1 : 0)
                : id == 3 ? effectSetCooldown : 0;
    }

    public void setField(int id, int value) {
        if (id == 0) beaconLevel = value;
        else if (id == 1) effect = value < 0 ? null : Potion.getPotionById(value);
        else if (id == 2) showWorkingArea = value == 1;
        else if (id == 3) effectSetCooldown = Math.max(0, value);
    }

    public float getBeamThickness() {
        return 0.2F;
    }

    public float getOuterBeamThickness() {
        return 0.25F;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public void invalidate() {
        if (world != null && !world.isRemote && isActive()) {
            playSound("withered_beacon_deactivate", 1.0F, 1.0F);
        }
        if (world != null && !world.isRemote) {
            ModNetwork.removeDistantSuperBeacon(this);
            ChunkLoadingManager.INSTANCE
                    .releaseSuperBeacon(world, pos);
        }
        super.invalidate();
    }

    protected void markAndNotify() {
        markDirty();
        if (world != null) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("ActivationTime", activationTime);
        compound.setInteger("BeamHeight", beamHeight);
        compound.setBoolean("IsActive", active);
        compound.setInteger("PowerUpTime", poweringUpAnimation);
        compound.setFloat("ActivationAnim", activationAnimation);
        compound.setInteger("Primary", effect == null ? -1 : Potion.getIdFromPotion(effect));
        compound.setBoolean("ShowWorkingArea", showWorkingArea);
        compound.setInteger("Cooldown", effectSetCooldown);
        if (customName != null && !customName.isEmpty()) {
            compound.setString("CustomName", customName);
        }
        lockCode.toNBT(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        activationTime = compound.getInteger("ActivationTime");
        beamHeight = compound.getInteger("BeamHeight");
        active = compound.getBoolean("IsActive");
        poweringUpAnimation = compound.getInteger("PowerUpTime");
        activationAnimation = MathHelper.clamp(compound.getFloat("ActivationAnim"), 0.0F, 1.0F);
        previousActivationAnimation = activationAnimation;
        int effectId = compound.hasKey("Primary") ? compound.getInteger("Primary") : -1;
        effect = effectId < 0 ? null : Potion.getPotionById(effectId);
        showWorkingArea = compound.getBoolean("ShowWorkingArea");
        effectSetCooldown = Math.max(0, compound.getInteger("Cooldown"));
        customName = compound.hasKey("CustomName", 8)
                ? compound.getString("CustomName") : null;
        lockCode = LockCode.fromNBT(compound);
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager network, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }
}
