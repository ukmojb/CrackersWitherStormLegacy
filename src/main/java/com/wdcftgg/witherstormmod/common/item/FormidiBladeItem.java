package com.wdcftgg.witherstormmod.common.item;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;





public class FormidiBladeItem extends CommandBlockSwordItem {

    public static final int DEFAULT_RELEASE_TIME = 40;
    public static final String POWER = "Power";
    public static final String IS_CHARGED = "IsCharged";
    public static final ResourceLocation ANIM_PROPERTY =
            new ResourceLocation(Tags.MOD_ID, "anim");
    private static final float POWER_DECREASE_PER_TICK = 0.2F;

    public FormidiBladeItem(String name) {
        super(name, ModToolMaterials.FORMIDI_BLADE, 3.0F, -3.7F);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity,
                         int slot, boolean selected) {
        NBTTagCompound tag = getBladeTag(stack, true);
        if (tag == null || !tag.hasKey(IS_CHARGED) || tag.getBoolean(IS_CHARGED)) return;
        if (!tag.hasKey(POWER)) return;
        float power = tag.getFloat(POWER);
        if (power > POWER_DECREASE_PER_TICK) {
            tag.setFloat(POWER, power - POWER_DECREASE_PER_TICK);
        } else {
            tag.removeTag(POWER);
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.getCooldownTracker().hasCooldown(this)) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
        }
        NBTTagCompound tag = getBladeTag(stack, false);
        float power = tag == null ? 0.0F : tag.getFloat(POWER);
        if (power >= 1.0F) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
        }
        player.setActiveHand(hand);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity,
                                     int timeLeft) {
        int elapsed = Math.max(0, getMaxItemUseDuration(stack) - timeLeft);
        float power = Math.min(1.0F, elapsed / (float) DEFAULT_RELEASE_TIME);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setFloat(POWER, power);
        tag.setBoolean(IS_CHARGED, true);
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(World world, Entity location, ItemStack stack) {
        return FireResistantItemEntity.create(world, location, stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        tooltip.add(TextFormatting.DARK_GRAY
                + I18n.format("item.witherstormmod.formidi_blade.author"));
        tooltip.add(TextFormatting.DARK_GRAY
                + I18n.format("item.witherstormmod.formidi_blade.use"));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack,
                                                boolean slotChanged) {
        return slotChanged && super.shouldCauseReequipAnimation(oldStack, newStack, true);
    }


    public static float getPower(EntityLivingBase living, ItemStack stack, boolean using) {
        float current = 0.0F;
        if (using && living != null && living.isHandActive()) {
            int remaining = living.getItemInUseCount();
            if (remaining > 0) {
                current = Math.min(1.0F,
                        (float) (72000 - remaining) / DEFAULT_RELEASE_TIME);
            }
        }
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = getBladeTag(stack, false);
            if (tag.hasKey(POWER)) {
                return Math.max(current, tag.getFloat(POWER));
            }
        }
        return current;
    }

    public static NBTTagCompound getBladeTag(ItemStack stack, boolean create) {
        NBTTagCompound root = stack.getTagCompound();
        if (root == null) {
            if (!create) return new NBTTagCompound();
            root = new NBTTagCompound();
            stack.setTagCompound(root);
        }
        NBTTagCompound legacy = root.getCompoundTag("WitherStormMod");
        if (!root.hasKey(POWER) && legacy.hasKey(POWER)) {
            root.setFloat(POWER, legacy.getFloat(POWER));
        }
        if (!root.hasKey(IS_CHARGED) && legacy.hasKey(IS_CHARGED)) {
            root.setBoolean(IS_CHARGED, legacy.getBoolean(IS_CHARGED));
        }
        if (legacy.hasKey(POWER)) legacy.removeTag(POWER);
        if (legacy.hasKey(IS_CHARGED)) legacy.removeTag(IS_CHARGED);
        if (legacy.getKeySet().isEmpty()) root.removeTag("WitherStormMod");
        return root;
    }

    public static void registerPropertyOverrides() {
        FormidiBladeItem item = (FormidiBladeItem) ModItems
                .get("formidi_blade");
        item.addPropertyOverride(ANIM_PROPERTY, (stack, world, entity) -> {
            EntityLivingBase living = entity instanceof EntityLivingBase
                    ? (EntityLivingBase) entity : null;
            boolean holding = living != null && (living.getHeldItemMainhand() == stack
                    || living.getHeldItemOffhand() == stack);
            return getPower(living, stack, holding && living.isHandActive());
        });
    }
}
