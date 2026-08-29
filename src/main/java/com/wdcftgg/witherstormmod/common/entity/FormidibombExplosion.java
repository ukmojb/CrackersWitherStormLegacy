package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.init.ModDamageSources;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FormidibombExplosion {

    private FormidibombExplosion() {
    }

    public static void explode(World world, @Nullable Entity source, int radius, int squish,
                               double x, double y, double z) {
        Explosion explosion = new Explosion(world, source, x, y, z, radius, true, true);


        world.playSound(null, x, y, z, ModSounds.get("tremble"), SoundCategory.BLOCKS, 32.0F, 1.0F);

        float diameter = radius * 2.0F;
        AxisAlignedBB area = new AxisAlignedBB(
                MathHelper.floor(x - diameter - 1.0D), MathHelper.floor(y - diameter - 1.0D), MathHelper.floor(z - diameter - 1.0D),
                MathHelper.floor(x + diameter + 1.0D), MathHelper.floor(y + diameter + 1.0D), MathHelper.floor(z + diameter + 1.0D));


        List<WitherStormEntity> storms = world.getEntitiesWithinAABB(
                WitherStormEntity.class, area.grow(200.0D));
        for (WitherStormEntity storm : storms) storm.onFormidibombExplosion();

        ModNetwork.sendFormidibombExplosion(world, source, x, y, z, radius, squish);
        ModNetwork.shakeDimension(world, 100.0F, 7.5F);
        ModNetwork.blindNear(world, x, y, z, 250.0D, 260, 40, 240);

        List<Drop> drops = new ArrayList<Drop>();
        Set<BlockPos> affectedBlocks = collectAffectedBlocks(world, explosion, x, y, z, radius, squish);
        for (BlockPos pos : affectedBlocks) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == Blocks.AIR) continue;
            Block block = state.getBlock();

            if (block.canDropFromExplosion(explosion)) {
                NonNullList<ItemStack> blockDrops = NonNullList.create();
                block.getDrops(blockDrops, world, pos, state, 0);
                for (ItemStack stack : blockDrops) mergeDrop(drops, stack, pos);
            }

            float remainingPower = radius * (0.7F + world.rand.nextFloat() * 0.6F);
            float resistance = source != null
                    ? source.getExplosionResistance(explosion, world, pos, state)
                    : block.getExplosionResistance(world, pos, null, explosion);
            remainingPower -= (resistance + 0.3F)
                    * (WitherStormConfig.lowerBlockResistance ? 0.01F : 0.3F);
            if (remainingPower <= 0.0F
                    || source != null && !source.canExplosionDestroyBlock(
                    explosion, world, pos, state, remainingPower)) continue;

            block.onBlockExploded(world, pos, explosion);
            if (world.rand.nextInt(3) == 0 && world.isAirBlock(pos) && world.getBlockState(pos.down()).isFullBlock()) {
                world.setBlockState(pos, Blocks.FIRE.getDefaultState());
            }
        }

        List<Entity> affected = world.getEntitiesWithinAABBExcludingEntity(null, area);
        ForgeEventFactory.onExplosionDetonate(world, explosion, affected, diameter);
        Vec3d center = new Vec3d(x, y, z);
        for (Entity entity : affected) {
            if (entity.isImmuneToExplosions()) continue;
            double distance = Math.sqrt(entity.getDistanceSq(center.x, center.y, center.z)) / diameter;
            if (distance > 1.0D) continue;

            double relativeX = entity.posX - x;
            double relativeY = (entity instanceof EntityTNTPrimed ? entity.posY : entity.posY + entity.getEyeHeight()) - y;
            double relativeZ = entity.posZ - z;
            double length = Math.sqrt(relativeX * relativeX + relativeY * relativeY + relativeZ * relativeZ);
            if (length == 0.0D) continue;
            relativeX /= length;
            relativeY /= length;
            relativeZ /= length;

            double visibility = world.getBlockDensity(center, entity.getEntityBoundingBox());
            double power = (1.0D - distance) * visibility;
            float damage = (float) ((int) ((power * power + power) * 0.5D * 7.0D * diameter + 1.0D));
            entity.attackEntityFrom(ModDamageSources.formidibomb(source), damage);
            double knockback = entity instanceof EntityLivingBase
                    ? EnchantmentProtection.getBlastDamageReduction((EntityLivingBase) entity, power)
                    : power;
            entity.motionX += relativeX * (knockback + radius);
            entity.motionY += relativeY * (knockback + radius);
            entity.motionZ += relativeZ * (knockback + radius);
            entity.velocityChanged = true;
        }

        for (Drop drop : drops) Block.spawnAsEntity(world, drop.pos, drop.stack);
    }


    private static Set<BlockPos> collectAffectedBlocks(World world, Explosion explosion,
                                                       double x, double y, double z,
                                                       int radius, int squish) {
        Set<BlockPos> positions = new HashSet<BlockPos>();
        int grid = 8;
        double verticalScale = Math.sqrt(Math.max(1, squish));
        for (int ix = -grid; ix <= grid; ix++) {
            for (int iy = -grid; iy <= grid; iy++) {
                for (int iz = -grid; iz <= grid; iz++) {
                    if (Math.max(Math.max(Math.abs(ix), Math.abs(iy)), Math.abs(iz)) != grid) continue;
                    double dx = ix;
                    double dy = iy / verticalScale;
                    double dz = iz;
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (length < 1.0E-6D) continue;
                    dx /= length;
                    dy /= length * verticalScale;
                    dz /= length;
                    double px = x;
                    double py = y;
                    double pz = z;
                    double power = radius * (0.7D + world.rand.nextDouble() * 0.6D);
                    while (power > 0.0D) {
                        BlockPos pos = new BlockPos(MathHelper.floor(px), MathHelper.floor(py), MathHelper.floor(pz));
                        if (!world.isBlockLoaded(pos)) break;
                        IBlockState state = world.getBlockState(pos);
                        if (state.getBlock() != Blocks.AIR) positions.add(pos);
                        Block block = state.getBlock();
                        if (block != Blocks.AIR) {
                            float resistance = block.getExplosionResistance(world, pos, null, explosion);
                            power -= (resistance + 0.3D)
                                    * (WitherStormConfig.lowerBlockResistance ? 0.01D : 0.3D);
                        }

                        power -= 0.225D;
                        px += dx * 0.3D;
                        py += dy * 0.3D;
                        pz += dz * 0.3D;
                    }
                }
            }
        }
        return positions;
    }

    private static void mergeDrop(List<Drop> drops, ItemStack stack, BlockPos pos) {
        if (stack.isEmpty()) return;
        for (Drop drop : drops) {
            if (!ItemStack.areItemsEqual(drop.stack, stack) || !ItemStack.areItemStackTagsEqual(drop.stack, stack)) continue;
            int amount = Math.min(16 - drop.stack.getCount(), stack.getCount());
            if (amount <= 0) continue;
            drop.stack.grow(amount);
            stack.shrink(amount);
            if (stack.isEmpty()) return;
        }
        drops.add(new Drop(stack.copy(), pos.toImmutable()));
    }

    private static final class Drop {
        private final ItemStack stack;
        private final BlockPos pos;

        private Drop(ItemStack stack, BlockPos pos) {
            this.stack = stack;
            this.pos = pos;
        }
    }
}
