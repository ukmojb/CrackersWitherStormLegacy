package com.wdcftgg.witherstormmod.common.init;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.item.AmuletItem;
import com.wdcftgg.witherstormmod.common.item.PhasometerItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;

public final class ModCreativeTabs {

    public static final CreativeTabs MAIN = new CreativeTabs(Tags.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModItems.get("formidi_blade"));
        }





        @Override
        public void displayAllRelevantItems(NonNullList<ItemStack> items) {
            super.displayAllRelevantItems(items);
            for (int index = 0; index < items.size(); index++) {
                ItemStack stack = items.get(index);
                Item item = stack.getItem();
                if (item == ModItems.get("amulet")) {
                    items.add(index + 1, taggedStack("amulet", AmuletItem.TRACK_ENTITY_TYPES));
                    index++;
                } else if (item == ModItems.get("phasometer")) {
                    items.add(index + 1, taggedStack("phasometer", PhasometerItem.UPGRADED));
                    index++;
                }
            }
        }
    };

    private ModCreativeTabs() {
    }

    private static ItemStack taggedStack(String name, String tagKey) {
        ItemStack stack = new ItemStack(ModItems.get(name));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(tagKey, true);
        stack.setTagCompound(tag);
        return stack;
    }
}
