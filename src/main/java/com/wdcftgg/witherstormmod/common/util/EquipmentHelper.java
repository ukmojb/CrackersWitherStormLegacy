package com.wdcftgg.witherstormmod.common.util;

import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.world.DifficultyInstance;

import java.util.Random;


public final class EquipmentHelper {
    private static final WeightedItem[] HELMETS = {
            item(Items.AIR, 15), item(Items.LEATHER_HELMET, 25), item(Items.CHAINMAIL_HELMET, 20),
            item(Items.GOLDEN_HELMET, 15), item(Items.IRON_HELMET, 10), item(Items.DIAMOND_HELMET, 5),

            item(Items.LEATHER_HELMET, 1)
    };
    private static final WeightedItem[] ADVANCED_HELMETS = append(HELMETS,
            item(Items.LEATHER_HELMET, 5), item(Items.CHAINMAIL_HELMET, 10),
            item(Items.GOLDEN_HELMET, 15), item(Items.IRON_HELMET, 20),
            item(Items.DIAMOND_HELMET, 10));
    private static final WeightedItem[] CHESTPLATES = {
            item(Items.AIR, 15), item(Items.LEATHER_CHESTPLATE, 25), item(Items.CHAINMAIL_CHESTPLATE, 20),
            item(Items.GOLDEN_CHESTPLATE, 15), item(Items.IRON_CHESTPLATE, 10),
            item(Items.DIAMOND_CHESTPLATE, 5)
    };
    private static final WeightedItem[] ADVANCED_CHESTPLATES = append(CHESTPLATES,
            item(Items.LEATHER_CHESTPLATE, 5), item(Items.CHAINMAIL_CHESTPLATE, 10),
            item(Items.GOLDEN_CHESTPLATE, 15), item(Items.IRON_CHESTPLATE, 20),
            item(Items.DIAMOND_CHESTPLATE, 10));
    private static final WeightedItem[] LEGGINGS = {
            item(Items.AIR, 15), item(Items.LEATHER_LEGGINGS, 25), item(Items.CHAINMAIL_LEGGINGS, 20),
            item(Items.GOLDEN_LEGGINGS, 15), item(Items.IRON_LEGGINGS, 10), item(Items.DIAMOND_LEGGINGS, 5)
    };
    private static final WeightedItem[] ADVANCED_LEGGINGS = append(LEGGINGS,
            item(Items.LEATHER_LEGGINGS, 5), item(Items.CHAINMAIL_LEGGINGS, 10),
            item(Items.GOLDEN_LEGGINGS, 15), item(Items.IRON_LEGGINGS, 20),
            item(Items.DIAMOND_LEGGINGS, 10));
    private static final WeightedItem[] BOOTS = {
            item(Items.AIR, 15), item(Items.LEATHER_BOOTS, 25), item(Items.CHAINMAIL_BOOTS, 20),
            item(Items.GOLDEN_BOOTS, 15), item(Items.IRON_BOOTS, 10), item(Items.DIAMOND_BOOTS, 5)
    };
    private static final WeightedItem[] ADVANCED_BOOTS = append(BOOTS,
            item(Items.LEATHER_BOOTS, 5), item(Items.CHAINMAIL_BOOTS, 15),
            item(Items.GOLDEN_BOOTS, 10), item(Items.IRON_BOOTS, 20),
            item(Items.DIAMOND_BOOTS, 10));

    private EquipmentHelper() {
    }

    public static boolean canWearArmor(SickenedMobEntity mob) {
        return mob instanceof SickenedEntities.SickenedZombieEntity
                || mob instanceof SickenedEntities.SickenedSkeletonEntity
                || mob instanceof SickenedEntities.SickenedVillagerEntity
                || mob instanceof SickenedEntities.SickenedPillagerEntity;
    }

    public static void applyEquipment(SickenedMobEntity mob, DifficultyInstance difficulty,
                                      boolean useAdvanced) {
        if (mob == null || difficulty == null || !canWearArmor(mob)) return;
        equip(mob, EntityEquipmentSlot.HEAD, useAdvanced ? ADVANCED_HELMETS : HELMETS, difficulty);

        equip(mob, EntityEquipmentSlot.CHEST, useAdvanced ? ADVANCED_CHESTPLATES : HELMETS, difficulty);
        equip(mob, EntityEquipmentSlot.LEGS, useAdvanced ? ADVANCED_LEGGINGS : HELMETS, difficulty);
        equip(mob, EntityEquipmentSlot.FEET, useAdvanced ? ADVANCED_BOOTS : BOOTS, difficulty);
    }

    private static void equip(SickenedMobEntity mob, EntityEquipmentSlot slot,
                              WeightedItem[] choices, DifficultyInstance difficulty) {
        if (!mob.getItemStackFromSlot(slot).isEmpty()) return;
        Item selected = choose(mob.getRNG(), choices);
        ItemStack stack = selected == Items.AIR ? ItemStack.EMPTY : new ItemStack(selected);
        if (!stack.isEmpty()) {
            int enchantmentLevel = (int) (5.0F + difficulty.getClampedAdditionalDifficulty()
                    * mob.getRNG().nextInt(40));
            stack = EnchantmentHelper.addRandomEnchantment(
                    mob.getRNG(), stack, enchantmentLevel, false);
        }
        mob.setItemStackToSlot(slot, stack);
    }

    private static Item choose(Random random, WeightedItem[] choices) {
        int totalWeight = 0;
        for (WeightedItem choice : choices) totalWeight += choice.weight;
        int selectedWeight = random.nextInt(totalWeight);
        for (WeightedItem choice : choices) {
            selectedWeight -= choice.weight;
            if (selectedWeight < 0) return choice.item;
        }
        return choices[choices.length - 1].item;
    }

    private static WeightedItem item(Item item, int weight) {
        return new WeightedItem(item, weight);
    }

    private static WeightedItem[] append(WeightedItem[] base, WeightedItem... additions) {
        WeightedItem[] result = new WeightedItem[base.length + additions.length];
        System.arraycopy(base, 0, result, 0, base.length);
        System.arraycopy(additions, 0, result, base.length, additions.length);
        return result;
    }

    private static final class WeightedItem {
        private final Item item;
        private final int weight;

        private WeightedItem(Item item, int weight) {
            this.item = item;
            this.weight = weight;
        }
    }
}
