package com.wdcftgg.witherstormmod.common.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;


public abstract class LegacySpyglassItem extends Item {

    public static final int USE_DURATION = 1200;

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
                                                     EnumHand hand) {
        playUseSound(world, player, getUseSound(), false);
        player.setActiveHand(hand);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {

        return EnumAction.NONE;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world,
                                     EntityLivingBase user, int timeLeft) {
        playUseSound(world, user, getStopUseSound(), true);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase user) {
        playUseSound(world, user, getStopUseSound(), true);
        return stack;
    }

    protected SoundEvent getUseSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_IRON;
    }

    protected SoundEvent getStopUseSound() {
        return SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE;
    }

    protected float getUseSoundVolume() {
        return 0.6F;
    }

    protected float getUseSoundPitch() {
        return 1.65F;
    }

    protected float getStopUseSoundVolume() {
        return 0.45F;
    }

    protected float getStopUseSoundPitch() {
        return 1.7F;
    }

    private void playUseSound(World world, EntityLivingBase user, SoundEvent sound,
                              boolean stopping) {
        world.playSound(null, user.posX, user.posY, user.posZ, sound,
                SoundCategory.PLAYERS,
                stopping ? getStopUseSoundVolume() : getUseSoundVolume(),
                stopping ? getStopUseSoundPitch() : getUseSoundPitch());
    }
}
