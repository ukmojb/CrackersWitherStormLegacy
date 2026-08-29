package com.wdcftgg.witherstormmod.common.event;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.item.AmuletItem;
import com.wdcftgg.witherstormmod.common.item.FormidiBladeItem;
import com.wdcftgg.witherstormmod.common.item.GoldenAppleStewItem;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class SpecialItemEvents {
    private SpecialItemEvents() { }






    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void useImportantItemOnMob(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EntityLiving)) return;
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof AmuletItem)
                && !(stack.getItem() instanceof GoldenAppleStewItem)) return;

        EntityPlayer player = event.getEntityPlayer();
        ItemStack original = stack.copy();
        if (!stack.interactWithEntity(player, (EntityLiving) event.getTarget(), event.getHand())) {
            return;
        }
        if (player.capabilities.isCreativeMode && stack == player.getHeldItem(event.getHand())
                && stack.getCount() < original.getCount()) {
            stack.setCount(original.getCount());
        } else if (!player.capabilities.isCreativeMode && stack.isEmpty()) {
            ForgeEventFactory.onPlayerDestroyItem(player, original, event.getHand());
        }
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }






    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attackCommandBlock(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof SupplementalEntities.CommandBlockEntity)
                || event.getEntityPlayer().world.isRemote) return;
        SupplementalEntities.CommandBlockEntity core =
                (SupplementalEntities.CommandBlockEntity) event.getTarget();
        if (core.attackPlayingDeadCore(event.getEntityPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void potTaintedMushroom(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().getBlockState(event.getPos()).getBlock() != Blocks.FLOWER_POT
                || event.getItemStack().isEmpty()
                || event.getItemStack().getItem() != ModBlocks.getItem("tainted_mushroom")) return;
        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        if (!(tile instanceof TileEntityFlowerPot)
                || !((TileEntityFlowerPot) tile).getFlowerItemStack().isEmpty()) return;

        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
        if (event.getWorld().isRemote) return;
        event.getWorld().setBlockState(event.getPos(),
                ModBlocks.get("potted_tainted_mushroom").getDefaultState(), 3);
        event.getEntityPlayer().addStat(StatList.FLOWER_POTTED);
        if (!event.getEntityPlayer().capabilities.isCreativeMode) {
            event.getItemStack().shrink(1);
        }
    }

    @SubscribeEvent
    public static void formidiBladeChargedAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;
        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof FormidiBladeItem)) return;
        if (player.getCooldownTracker().hasCooldown(stack.getItem())) return;
        NBTTagCompound tag = FormidiBladeItem.getBladeTag(stack, false);
        if (tag == null || !tag.hasKey(FormidiBladeItem.POWER)) return;
        float power = Math.min(1.0F, tag.getFloat(FormidiBladeItem.POWER));
        if (power <= 0.0F) return;

        tag.setBoolean(FormidiBladeItem.IS_CHARGED, false);
        tag.setFloat(FormidiBladeItem.POWER, 2.0F);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                ModSounds.get("formidi_blade_decharge"), SoundCategory.PLAYERS, 1.0F, 1.0F);
        player.getCooldownTracker().setCooldown(stack.getItem(), 100);
        Entity target = event.getTarget();
        player.world.newExplosion(player, target.posX, target.posY, target.posZ,
                4.0F * power, false, false);
        ModNetwork.shakeNear(player.world, player.posX, player.posY, player.posZ,
                32.0D, 40.0F, 2.5F);
    }

    @SubscribeEvent
    public static void applySickenedStewEffect(LivingEntityUseItemEvent.Finish event) {
        ItemStack consumed = event.getItem();
        if (consumed.getItem() != Items.MUSHROOM_STEW || event.getEntityLiving().world.isRemote) return;
        NBTTagCompound tag = consumed.getSubCompound("WitherStormMod");
        if (tag == null || !tag.hasKey(SickenedEntities.SickenedMushroomCowEntity.STEW_DURATION_TAG, 99)) return;
        int duration = tag.getInteger(SickenedEntities.SickenedMushroomCowEntity.STEW_DURATION_TAG);
        if (duration > 0) event.getEntityLiving().addPotionEffect(new PotionEffect(MobEffects.WITHER, duration));
    }

    @SubscribeEvent
    public static void boostSickenedPig(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (event.getWorld().isRemote
                || !(player.getRidingEntity() instanceof SickenedEntities.SickenedPigEntity)
                || event.getItemStack().getItem() != Items.CARROT_ON_A_STICK) return;
        ItemStack stack = event.getItemStack();
        if (stack.getMaxDamage() - stack.getItemDamage() < 7) return;
        SickenedEntities.SickenedPigEntity pig =
                (SickenedEntities.SickenedPigEntity) player.getRidingEntity();
        if (!pig.boost()) return;

        NBTTagCompound itemTag = stack.getTagCompound();
        stack.damageItem(7, player);
        if (stack.isEmpty()) {
            ItemStack fishingRod = new ItemStack(Items.FISHING_ROD);
            fishingRod.setTagCompound(itemTag);
            player.setHeldItem(event.getHand(), fishingRod);
        }
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }
}
