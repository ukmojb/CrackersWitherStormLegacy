package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.common.item.FormidibombItem;
import com.wdcftgg.witherstormmod.common.tile.FormidibombTileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;


public final class IgnoredTargetsManager {
    private static final int DEFAULT_IGNORE_TICKS = 80;
    private static final double FORMIDIBOMB_RESTRICTION_RADIUS = 20.0D;
    private static final double SYMBIONT_RESTRICTION_RADIUS = 50.0D;

    private final Entity storm;
    private final Supplier<AxisAlignedBB> searchBoxSupplier;
    private final Map<UUID, Integer> ignoredEntities = new LinkedHashMap<UUID, Integer>();
    private final List<AxisAlignedBB> restrictedTargetingRegions = new ArrayList<AxisAlignedBB>();

    public IgnoredTargetsManager(WitherStormEntity storm) {
        this(storm, storm::getSearchBox);
    }

    IgnoredTargetsManager(Entity storm, Supplier<AxisAlignedBB> searchBoxSupplier) {
        this.storm = storm;
        this.searchBoxSupplier = Objects.requireNonNull(searchBoxSupplier, "searchBoxSupplier");
    }

    public void tick() {
        Iterator<Map.Entry<UUID, Integer>> iterator = ignoredEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remainingTicks = entry.getValue() - 1;
            if (remainingTicks <= 0) iterator.remove();
            else entry.setValue(remainingTicks);
        }

        restrictedTargetingRegions.clear();
        AxisAlignedBB searchBox = searchBoxSupplier.get();
        for (TileEntity tileEntity : storm.world.loadedTileEntityList) {
            if (!(tileEntity instanceof FormidibombTileEntity)
                    || ((FormidibombTileEntity) tileEntity).getStartFuse() <= 0
                    || !searchBox.contains(new Vec3d(tileEntity.getPos()).add(0.5D, 0.5D, 0.5D))) {
                continue;
            }
            restrictedTargetingRegions.add(new AxisAlignedBB(tileEntity.getPos())
                    .grow(FORMIDIBOMB_RESTRICTION_RADIUS));
        }

        for (Entity entity : storm.world.getEntitiesWithinAABB(Entity.class, searchBox)) {
            double restrictionRadius = getTargetRestrictionRadius(entity);
            if (restrictionRadius >= 0.0D) {
                restrictedTargetingRegions.add(new AxisAlignedBB(entity.getPosition())
                        .grow(restrictionRadius));
            }
        }
    }

    private static double getTargetRestrictionRadius(Entity entity) {
        if (entity instanceof PowerfulExplosiveEntity.FormidibombEntity) {
            PowerfulExplosiveEntity.FormidibombEntity formidibomb =
                    (PowerfulExplosiveEntity.FormidibombEntity) entity;
            return !formidibomb.isDead && formidibomb.getStartFuse() > 0
                    ? FORMIDIBOMB_RESTRICTION_RADIUS : -1.0D;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            for (ItemStack stack : player.inventory.mainInventory) {
                if (!stack.isEmpty() && stack.getItem() instanceof FormidibombItem
                        && FormidibombItem.getStartFuse(stack) > 0) {
                    return FORMIDIBOMB_RESTRICTION_RADIUS;
                }
            }
        } else if (entity instanceof SickenedEntities.WitheredSymbiontEntity) {
            return SYMBIONT_RESTRICTION_RADIUS;
        }
        return -1.0D;
    }

    public void addEntityToIgnore(Entity entity) {
        addEntityToIgnore(entity, DEFAULT_IGNORE_TICKS);
    }

    public void addEntityToIgnore(Entity entity, int ticks) {
        if (entity == null || ticks <= 0 || shouldIgnoreTarget(entity)) return;
        ignoredEntities.put(entity.getUniqueID(), ticks);
    }

    public boolean shouldIgnoreTarget(@Nullable Entity entity) {
        if (entity == null) return false;
        if (ignoredEntities.containsKey(entity.getUniqueID())) return true;
        Vec3d eyePosition = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        for (AxisAlignedBB region : restrictedTargetingRegions) {
            if (region.contains(eyePosition)) return true;
        }
        return false;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList entities = new NBTTagList();
        for (Map.Entry<UUID, Integer> entry : ignoredEntities.entrySet()) {
            NBTTagCompound ignoredEntity = new NBTTagCompound();
            ignoredEntity.setUniqueId("UUID", entry.getKey());
            ignoredEntity.setInteger("Ticks", entry.getValue());
            entities.appendTag(ignoredEntity);
        }
        compound.setTag("Entities", entities);
        return compound;
    }

    public void readFromNBT(NBTTagCompound compound) {
        ignoredEntities.clear();
        NBTTagList entities = compound.getTagList("Entities", 10);
        for (int index = 0; index < entities.tagCount(); index++) {
            NBTTagCompound ignoredEntity = entities.getCompoundTagAt(index);
            int ticks = ignoredEntity.getInteger("Ticks");
            if (ignoredEntity.hasUniqueId("UUID") && ticks > 0) {
                ignoredEntities.put(ignoredEntity.getUniqueId("UUID"), ticks);
            }
        }
    }
}
