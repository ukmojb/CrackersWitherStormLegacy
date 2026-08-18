package com.wdcftgg.witherstormmod.common.taint;

import com.wdcftgg.witherstormmod.common.advancement.ModCriteriaTriggers;
import com.wdcftgg.witherstormmod.api.common.event.SickenedMobConversionEvent;
import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.village.Village;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

/**
 * 面向世界/事件暴露的腐化入口，具体配方来自外部上游 JAR 的
 * data/witherstormmod/tainting 数据（见 TaintingData）。
 */
public final class TaintingManager {

    private TaintingManager() {
    }

    public static boolean taintBlock(World world, BlockPos position) {
        IBlockState original = world.getBlockState(position);
        TaintingData.BlockRecipe recipe = TaintingData.findBlockRecipe(original);
        return applyBlockRecipe(world, position, original, recipe);
    }

    public static boolean taintBlock(World world, BlockPos position, PotionType potionType) {
        IBlockState original = world.getBlockState(position);
        TaintingData.BlockRecipe recipe = TaintingData.findBlockRecipe(original, potionType);
        return applyBlockRecipe(world, position, original, recipe);
    }

    private static boolean applyBlockRecipe(World world, BlockPos position,
                                            IBlockState original,
                                            TaintingData.BlockRecipe recipe) {
        if (recipe == null) return false;
        IBlockState replacement = TaintingData.applyRecipe(recipe, original);
        if (replacement.getBlock() == original.getBlock()) return false;
        return world.setBlockState(position, replacement, 3);
    }

    public static boolean convertEntity(EntityLivingBase original) {
        return convertEntity(original, false);
    }

    public static boolean convertEntity(EntityLivingBase original, boolean fromWitherSickness) {
        if (original.world.isRemote || original.isDead || original instanceof SickenedMobEntity) {
            return false;
        }
        TaintingData.MobRecipe recipe = TaintingData.findMobRecipe(original);
        if (recipe == null || !recipe.canConvertFromWitherSickness() && fromWitherSickness) {
            return false;
        }
        SickenedMobEntity replacement =
                TaintingData.createSickenedEntity(recipe.getSickenedType(), original.world);
        if (replacement == null) return false;
        replacement.rememberOriginal(original);
        copySharedState(original, replacement, true);
        replacement.setSickenedChild(original.isChild());
        replacement.setHealth(replacement.getMaxHealth()
                * Math.max(0.1F, original.getHealth() / original.getMaxHealth()));
        copyEquipment(original, replacement);
        replacement.copySpeciesDataFrom(original);
        SickenedMobConversionEvent.Pre pre = new SickenedMobConversionEvent.Pre(
                original, replacement, SickenedMobConversionEvent.Direction.INFECTION);
        if (MinecraftForge.EVENT_BUS.post(pre)) return false;
        if (!original.world.spawnEntity(replacement)) return false;
        // WorldTainting.convertMob(..., true) moves the equipment into the
        // sickened entity. Clear the source only after the replacement is
        // accepted by the world so a cancelled conversion leaves it intact.
        clearEquipment(original);
        transferRidingRelationships(original, replacement);
        replacement.playSound(ModSounds.get("mob_infected"),
                1.0F, 1.0F);
        original.setDead();
        MinecraftForge.EVENT_BUS.post(new SickenedMobConversionEvent.Post(
                original, replacement, SickenedMobConversionEvent.Direction.INFECTION));
        return true;
    }

    public static boolean canConvertEntity(EntityLivingBase original) {
        return TaintingData.canConvertMob(original, true);
    }

    public static boolean canConvertEntity(EntityLivingBase original, boolean fromWitherSickness) {
        return TaintingData.canConvertMob(original, fromWitherSickness);
    }

    public static boolean cureEntity(SickenedMobEntity original) {
        EntityLivingBase replacement = TaintingData.createCuredEntity(original);
        if (replacement == null || original.world.isRemote || original.isDead) {
            return false;
        }
        NBTTagCompound saved = original.getOriginalData();
        if (saved != null) {
            replacement.readFromNBT(saved);
        }
        original.copySpeciesDataTo(replacement);
        copySharedState(original, replacement, false);
        float healthRatio = original.getHealth() / original.getMaxHealth();
        replacement.setHealth(Math.max(1.0F, replacement.getMaxHealth() * healthRatio));
        replacement.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200));
        SickenedMobConversionEvent.Pre pre = new SickenedMobConversionEvent.Pre(
                original, replacement, SickenedMobConversionEvent.Direction.CURE);
        if (MinecraftForge.EVENT_BUS.post(pre)) {
            original.setConversionTime(pre.getDelayTicks());
            return false;
        }
        if (!original.world.spawnEntity(replacement)) return false;
        transferRidingRelationships(original, replacement);
        if (replacement instanceof EntityVillager
                && original.getConversionStarter() != null) {
            Village village = replacement.world.getVillageCollection().getNearestVillage(
                    new BlockPos(Math.floor(replacement.posX), Math.floor(replacement.posY),
                            Math.floor(replacement.posZ)), 32);
            if (village != null) {
                village.modifyPlayerReputation(original.getConversionStarter(), 5);
            }
        }
        replacement.playSound(ModSounds.get("mob_cured"),
                1.0F, 1.0F);
        if (original.getConversionStarter() != null && original.world instanceof WorldServer) {
            Entity starter = original.world.getMinecraftServer()
                    .getPlayerList().getPlayerByUUID(original.getConversionStarter());
            if (starter instanceof EntityPlayerMP) {
                ModCriteriaTriggers.CURED_SICKENED_MOB.trigger(
                        (EntityPlayerMP) starter, original, replacement);
            }
        }
        original.setDead();
        MinecraftForge.EVENT_BUS.post(new SickenedMobConversionEvent.Post(
                original, replacement, SickenedMobConversionEvent.Direction.CURE));
        return true;
    }

    private static void copySharedState(EntityLivingBase original, EntityLivingBase replacement,
                                        boolean copyDropChances) {
        replacement.setLocationAndAngles(original.posX, original.posY, original.posZ,
                original.rotationYaw, original.rotationPitch);
        replacement.renderYawOffset = original.renderYawOffset;
        replacement.rotationYawHead = original.rotationYawHead;
        replacement.setSilent(original.isSilent());
        replacement.setNoGravity(original.hasNoGravity());
        replacement.setGlowing(original.isGlowing());
        replacement.setEntityInvulnerable(original.getIsInvulnerable());
        if (original.hasCustomName()) {
            replacement.setCustomNameTag(original.getCustomNameTag());
        }
        replacement.setAlwaysRenderNameTag(original.getAlwaysRenderNameTag());

        if (original instanceof EntityLiving && replacement instanceof EntityLiving) {
            EntityLiving from = (EntityLiving) original;
            EntityLiving to = (EntityLiving) replacement;
            to.setNoAI(from.isAIDisabled());
            to.setCanPickUpLoot(from.canPickUpLoot());
            to.setLeftHanded(from.isLeftHanded());
            if (from.isNoDespawnRequired()) to.enablePersistence();
            if (copyDropChances) copyDropChances(from, to);
        }
    }

    private static void copyEquipment(EntityLivingBase original, EntityLivingBase replacement) {
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack equipped = original.getItemStackFromSlot(slot);
            replacement.setItemStackToSlot(slot,
                    equipped.isEmpty() ? ItemStack.EMPTY : equipped.copy());
        }
    }

    private static void clearEquipment(EntityLivingBase entity) {
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (!entity.getItemStackFromSlot(slot).isEmpty()) {
                entity.setItemStackToSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private static void copyDropChances(EntityLiving original, EntityLiving replacement) {
        NBTTagCompound data = new NBTTagCompound();
        original.writeEntityToNBT(data);
        copyDropChanceList(data.getTagList("HandDropChances", 5), replacement,
                EntityEquipmentSlot.MAINHAND, EntityEquipmentSlot.OFFHAND);
        copyDropChanceList(data.getTagList("ArmorDropChances", 5), replacement,
                EntityEquipmentSlot.FEET, EntityEquipmentSlot.LEGS,
                EntityEquipmentSlot.CHEST, EntityEquipmentSlot.HEAD);
    }

    private static void copyDropChanceList(NBTTagList chances, EntityLiving replacement,
                                           EntityEquipmentSlot... slots) {
        int count = Math.min(chances.tagCount(), slots.length);
        for (int index = 0; index < count; index++) {
            replacement.setDropChance(slots[index], chances.getFloatAt(index));
        }
    }

    private static void transferRidingRelationships(EntityLivingBase original,
                                                     EntityLivingBase replacement) {
        Entity vehicle = original.getRidingEntity();
        List<Entity> passengers = new ArrayList<Entity>(original.getPassengers());
        if (vehicle != null) {
            original.dismountRidingEntity();
            replacement.startRiding(vehicle, true);
        }
        for (Entity passenger : passengers) {
            passenger.startRiding(replacement, true);
        }
    }

    public static ResourceLocation getOriginalType(String sickenedType) {
        return TaintingData.getOriginalType(sickenedType);
    }
}
