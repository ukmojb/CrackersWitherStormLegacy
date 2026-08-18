package com.wdcftgg.witherstormmod.common.item;

import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.FormidibombExplosion;
import com.wdcftgg.witherstormmod.common.entity.PowerfulExplosiveEntity;
import com.wdcftgg.witherstormmod.common.tile.FormidibombTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.util.List;

public class FormidibombItem extends RarityBlockItem {

    public FormidibombItem(Block block) {
        super(block, EnumRarity.EPIC);
        setMaxStackSize(1);
    }

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player) {
        setFuse(stack, WitherStormConfig.craftFuseTicks);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        tickFuse(stack, world, entity, entity.getPosition());
    }

    @Override
    public boolean onEntityItemUpdate(EntityItem entityItem) {
        tickFuse(entityItem.getItem(), entityItem.world, null, entityItem.getPosition());
        return false;
    }

    private void tickFuse(ItemStack stack, World world, Entity holder, BlockPos position) {
        int startFuse = getStartFuse(stack);
        if (world.isRemote || startFuse <= 0) return;
        int fuse = getFuse(stack);
        if (WitherStormConfig.formidibombFuseEnabled) {
            --fuse;
            stack.getOrCreateSubCompound("WitherStormMod").setInteger("Fuse", fuse);
        }
        if (WitherStormConfig.shouldDropFromInventory
                && fuse <= startFuse / Math.max(1, WitherStormConfig.dropInterval)) {
            spawnBomb(stack, world, holder, position, fuse);
        }
        if (fuse <= 0) {
            stack.shrink(1);
            FormidibombExplosion.explode(world, holder, 48 + world.rand.nextInt(9), 3,
                    position.getX(), position.getY(), position.getZ());
        }
    }

    private void spawnBomb(ItemStack stack, World world, Entity holder, BlockPos position, int fuse) {
        EntityLivingBase owner = holder instanceof EntityLivingBase ? (EntityLivingBase) holder : null;
        PowerfulExplosiveEntity.FormidibombEntity bomb = new PowerfulExplosiveEntity.FormidibombEntity(world,
                position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, owner);
        bomb.setFuse(fuse);
        bomb.setStartFuse(getStartFuse(stack));
        if (world.spawnEntity(bomb)) {
            stack.shrink(1);
            world.playSound(null, bomb.posX, bomb.posY, bomb.posZ, SoundEvents.ENTITY_TNT_PRIMED,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
                                EnumFacing side, float hitX, float hitY, float hitZ,
                                IBlockState newState) {
        if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) return false;
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof FormidibombTileEntity) {
            ((FormidibombTileEntity) tile).setFuse(getFuse(stack), getStartFuse(stack), player);
        }
        return true;
    }

    public static int getFuse(ItemStack stack) {
        return stack.getSubCompound("WitherStormMod") == null ? 0 : stack.getSubCompound("WitherStormMod").getInteger("Fuse");
    }

    public static int getStartFuse(ItemStack stack) {
        return stack.getSubCompound("WitherStormMod") == null ? 0 : stack.getSubCompound("WitherStormMod").getInteger("StartFuse");
    }

    public static void setFuse(ItemStack stack, int fuse) {
        setFuseState(stack, fuse, fuse);
    }

    public static void setFuseState(ItemStack stack, int fuse, int startFuse) {
        stack.getOrCreateSubCompound("WitherStormMod").setInteger("Fuse", fuse);
        stack.getOrCreateSubCompound("WitherStormMod").setInteger("StartFuse", startFuse);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getFuse(stack) > 0 && getFuse(stack) < getStartFuse(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0D - getFuse(stack) / (double) Math.max(1, getStartFuse(stack));
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        int fuse = getFuse(stack);
        int startFuse = getStartFuse(stack);
        int pulse = fuse > 0 ? startFuse / fuse : 0;
        return pulse % 2 == 0 ? 12718080 : 10027161;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        int fuse = getFuse(stack);
        int startFuse = getStartFuse(stack);
        if (startFuse > 0 && fuse < startFuse) {
            TextFormatting color = fuse / 10 % 2 == 0 ? TextFormatting.RED : TextFormatting.DARK_PURPLE;
            tooltip.add(color + new TextComponentTranslation(
                    "description.formidibomb.fuse", Math.max(0, fuse) / 20).getFormattedText());
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
}
