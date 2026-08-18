package com.wdcftgg.witherstormmod.common.item;

import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import com.google.common.collect.Multimap;

public class EyeOfTheStormItem extends CommandBlockSwordItem {
    private static final String ENTITY_HEALTH_RATIO = "EntityHealthRatio";
    private static final UUID DAMAGE_MODIFIER_ID =
            UUID.fromString("823350e7-4c91-4a1f-8c01-8735113f066e");

    public EyeOfTheStormItem(String name) {
        super(name, ModToolMaterials.EYE_OF_THE_STORM);
    }

    /** 对应上游 ItemAttributeModifierEvent：生命值越低伤害越低。 */
    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(
            EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> modifiers = super.getAttributeModifiers(slot, stack);
        if (slot != EntityEquipmentSlot.MAINHAND) return modifiers;
        NBTTagCompound tag = stack.getSubCompound("WitherStormMod");
        if (tag == null || !tag.hasKey(ENTITY_HEALTH_RATIO)) return modifiers;
        float ratio = tag.getFloat(ENTITY_HEALTH_RATIO);
        if (ratio <= 0.0F) return modifiers;
        modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                new AttributeModifier(DAMAGE_MODIFIER_ID, "Health damage modifier",
                        -ratio * 5.0D, 0));
        return modifiers;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof EntityLivingBase && (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).capabilities.isCreativeMode)) {
            EntityLivingBase living = (EntityLivingBase) entity;
            stack.getOrCreateSubCompound("WitherStormMod").setFloat(ENTITY_HEALTH_RATIO, living.getHealth() / living.getMaxHealth());
        } else if (stack.getSubCompound("WitherStormMod") != null) {
            stack.getSubCompound("WitherStormMod").removeTag(ENTITY_HEALTH_RATIO);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.DARK_GRAY + new TextComponentTranslation("item.witherstormmod.eye_of_the_storm.author").getFormattedText());
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (!super.hitEntity(stack, target, attacker)) return false;
        float healthRatio = attacker.getHealth() / attacker.getMaxHealth();
        if (attacker.getRNG().nextFloat() <= healthRatio && (!(attacker instanceof EntityPlayer) || !((EntityPlayer) attacker).capabilities.isCreativeMode)) {
            return true;
        }
        double minHeight = Math.min(target.posY, attacker.posY);
        double maxHeight = Math.max(target.posY, attacker.posY) + 1.0D;
        float hitAngle = (float) MathHelper.atan2(target.posZ - attacker.posZ, target.posX - attacker.posX);
        float damageModifier = EnchantmentHelper.getModifierForCreature(stack, target.getCreatureAttribute());
        createSpike(attacker, target.posX, target.posZ, minHeight, maxHeight, hitAngle, 0, damageModifier);
        for (int i = 0; i < 5; i++) {
            float angle = i / 5.0F * (float) Math.PI * 2.0F + hitAngle;
            for (int distance = 1; distance <= 2; distance++) {
                createSpike(attacker, target.posX + MathHelper.sin(angle) * distance,
                        target.posZ + MathHelper.cos(angle) * distance, minHeight, maxHeight, angle,
                        distance * 5 + attacker.getRNG().nextInt(4) - 2, damageModifier);
            }
        }
        return true;
    }

    private static void createSpike(EntityLivingBase owner, double x, double z, double minHeight, double maxHeight,
                                    float yaw, int delay, float damageModifier) {
        BlockPos cursor = new BlockPos(x, maxHeight, z);
        int minimumY = MathHelper.floor(minHeight) - 1;
        while (cursor.getY() >= minimumY) {
            BlockPos below = cursor.down();
            IBlockState belowState = owner.world.getBlockState(below);
            if (belowState.isSideSolid(owner.world, below, EnumFacing.UP)) {
                double offset = 0.0D;
                IBlockState state = owner.world.getBlockState(cursor);
                AxisAlignedBB collision = state.getCollisionBoundingBox(owner.world, cursor);
                if (collision != null) offset = collision.maxY;
                owner.world.spawnEntity(new SupplementalEntities.TentacleSpikeEntity(owner.world, x, cursor.getY() + offset,
                        z, yaw, delay, owner, damageModifier));
                return;
            }
            cursor = cursor.down();
        }
    }
}
