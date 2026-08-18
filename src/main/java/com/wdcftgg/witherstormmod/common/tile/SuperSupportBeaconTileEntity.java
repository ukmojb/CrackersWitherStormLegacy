package com.wdcftgg.witherstormmod.common.tile;

import com.wdcftgg.witherstormmod.common.beacon.SuperBeaconLogic;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.Set;

public class SuperSupportBeaconTileEntity extends AbstractSuperBeaconTileEntity implements ITickable {

    private SuperBeaconTileEntity.SupportColor color;
    private BlockPos connectedBeacon;

    @Override
    public void update() {
        if (world == null) return;
        tickBeaconBase();
        if (!world.isRemote) {
            SuperBeaconTileEntity.SupportColor detected = detectColor();
            if (detected != color) {
                color = detected;
                markAndNotify();
            }

            SuperBeaconTileEntity main = findNearbyValidBeacon();
            if (main != null) {
                BlockPos newConnection = main.getPos().toImmutable();
                if (!newConnection.equals(connectedBeacon)) {
                    connectedBeacon = newConnection;
                    markAndNotify();
                }
            }
            boolean shouldBeActive = color != null && main != null;
            if (active != shouldBeActive) setActive(shouldBeActive);
        }

        SuperBeaconTileEntity main = getConnectedBeaconEntity();
        if (main == null) return;
        int previousLevel = beaconLevel;
        boolean previousShowWorkingArea = showWorkingArea;
        beaconLevel = main.getBeaconLevel();
        showWorkingArea = main.showWorkingArea();
        if (!world.isRemote && color != null
                && main.getResummonTicks() == getResummonThreshold()) {
            playSound("withered_beacon_activate", 1.0F, 1.0F);
            playSound("tremble", 10.0F, 1.0F);
            BlockPos mainPos = main.getPos();
            ModNetwork.shakeNear(world,
                    mainPos.getX(), mainPos.getY(), mainPos.getZ(),
                    20.0D, 80.0F, 10.0F);
        }
        if (!world.isRemote
                && (beaconLevel != previousLevel || showWorkingArea != previousShowWorkingArea)) {
            markAndNotify();
        }
    }

    private SuperBeaconTileEntity.SupportColor detectColor() {
        Block expected = world.getBlockState(pos.down()).getBlock();
        SuperBeaconLogic.SupportColor logicColor = SuperBeaconLogic.SupportColor.forBase(expected);
        if (logicColor == null) return null;
        for (BlockPos check : BlockPos.getAllInBox(pos.add(-1, -1, -1), pos.add(1, -1, 1))) {
            if (world.getBlockState(check).getBlock() != expected) return null;
        }
        return SuperBeaconTileEntity.SupportColor.valueOf(logicColor.name());
    }

    private SuperBeaconTileEntity findNearbyValidBeacon() {
        int distance = SuperBeaconLogic.SUPPORT_SCAN_DISTANCE;
        AxisAlignedBB searchBox = new AxisAlignedBB(pos).grow(distance);
        for (TileEntity tile : new ArrayList<TileEntity>(world.loadedTileEntityList)) {
            if (!(tile instanceof SuperBeaconTileEntity)) continue;
            SuperBeaconTileEntity beacon = (SuperBeaconTileEntity) tile;
            if (!searchBox.intersects(new AxisAlignedBB(beacon.getPos()))) continue;
            if (!beacon.isConnected(pos)) continue;
            return beacon;
        }
        return null;
    }

    @Override
    protected void applyEffect() {
        SuperBeaconTileEntity main = getConnectedBeaconEntity();
        if (main == null || color == null || effect == null) return;
        int amplifier = Math.max(0, beaconLevel - 1);
        for (EntityPlayer player : world.playerEntities) {
            if (SuperBeaconLogic.isInsideSupportArc(
                    main.getPos().getX(), main.getPos().getZ(),
                    pos.getX(), pos.getZ(),
                    player.posX, player.posZ)) {
                player.addPotionEffect(new PotionEffect(effect,
                        SuperBeaconLogic.SUPPORT_EFFECT_DURATION, amplifier, true, true));
            }
        }
    }

    @Override
    public Set<Potion> getValidEffects() {
        return color == null ? java.util.Collections.<Potion>emptySet()
                : color.getLogic().getValidEffects();
    }

    @Override
    public void doPowerUp(EntityPlayerMP player) {
        super.doPowerUp(player);
    }

    @Override
    public void setShowWorkingArea(boolean show) {
        SuperBeaconTileEntity main = getConnectedBeaconEntity();
        if (main != null) main.setShowWorkingArea(show);
        super.setShowWorkingArea(show);
    }

    public SuperBeaconTileEntity.SupportColor getColor() {
        return color;
    }

    public BlockPos getConnectedBeacon() {
        return connectedBeacon;
    }

    public BlockPos getBeamTarget() {
        SuperBeaconTileEntity main = getConnectedBeaconEntity();
        if (main != null && color != null
                && main.getResummonTicks() > getResummonThreshold()) {
            return connectedBeacon.up(3);
        }
        return connectedBeacon;
    }

    @Override
    protected boolean shouldDoActivatedAnimation() {
        SuperBeaconTileEntity main = getConnectedBeaconEntity();
        return super.shouldDoActivatedAnimation() || main != null && color != null
                && main.getResummonTicks() > getResummonThreshold();
    }

    public int getResummonThreshold() {
        return color == null ? Integer.MAX_VALUE
                : SuperBeaconLogic.getSupportResummonThreshold(color.ordinal());
    }

    public SuperBeaconTileEntity getConnectedBeaconEntity() {
        if (world == null || connectedBeacon == null) return null;
        TileEntity tile = world.getTileEntity(connectedBeacon);
        return tile instanceof SuperBeaconTileEntity ? (SuperBeaconTileEntity) tile : null;
    }

    @Override
    public String getNameForGui() {
        return customName == null || customName.isEmpty()
                ? "container.witherstormmod.withered_support_beacon" : customName;
    }

    @Override
    public int[] getBeamColor() {
        if (color == null) return super.getBeamColor();
        float[] values = color.getLogic().getBeamColor();
        return new int[] {(int) (values[0] * 255.0F), (int) (values[1] * 255.0F),
                (int) (values[2] * 255.0F)};
    }

    @Override
    public float getBeamThickness() {
        return 0.15F;
    }

    @Override
    public float getOuterBeamThickness() {
        return 0.2F;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Color", color == null ? -1 : color.ordinal());
        if (connectedBeacon != null) compound.setLong("Connected", connectedBeacon.toLong());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        int value = compound.getInteger("Color");
        color = value >= 0 && value < SuperBeaconTileEntity.SupportColor.values().length
                ? SuperBeaconTileEntity.SupportColor.values()[value] : null;
        connectedBeacon = compound.hasKey("Connected") ? BlockPos.fromLong(compound.getLong("Connected")) : null;
    }

}
