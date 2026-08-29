package com.wdcftgg.witherstormmod.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


final class ConsumedPetStorage {
    private ConsumedPetStorage() {
    }

    static void capture(Map<UUID, NBTTagCompound> consumedPets, EntityLivingBase living) {
        if (!(living instanceof EntityTameable)) return;
        EntityTameable tameable = (EntityTameable) living;
        UUID petUuid = living.getUniqueID();
        ResourceLocation entityId = EntityList.getKey(living);
        if (!tameable.isTamed() || tameable.getOwnerId() == null || entityId == null
                || consumedPets.containsKey(petUuid)) return;
        NBTTagCompound saved = new NBTTagCompound();
        saved.setString("id", entityId.toString());
        living.writeToNBT(saved);
        saved.removeTag("Dimension");
        saved.removeTag("Motion");
        saved.removeTag("Pos");
        saved.removeTag("Rotation");
        consumedPets.put(petUuid, saved);
    }

    static void release(World world, Map<UUID, NBTTagCompound> consumedPets, Vec3d position) {
        if (world.isRemote || consumedPets.isEmpty()) return;
        List<NBTTagCompound> savedPets = new ArrayList<NBTTagCompound>(consumedPets.values());
        consumedPets.clear();
        for (NBTTagCompound saved : savedPets) {
            Entity pet = EntityList.createEntityFromNBT(saved.copy(), world);
            if (pet == null) continue;
            pet.setPosition(position.x, position.y, position.z);
            if (pet instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) pet;
                living.setHealth(living.getMaxHealth());
                living.clearActivePotions();
                living.addPotionEffect(new PotionEffect(MobEffects.REGENERATION,
                        200, 0, false, false));
            }
            world.spawnEntity(pet);
        }
    }

    static void write(NBTTagCompound compound, String key,
                      Map<UUID, NBTTagCompound> consumedPets) {
        NBTTagList pets = new NBTTagList();
        for (Map.Entry<UUID, NBTTagCompound> pet : consumedPets.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setUniqueId("id", pet.getKey());
            entry.setTag("Entity", pet.getValue().copy());
            pets.appendTag(entry);
        }
        compound.setTag(key, pets);
    }

    static void read(NBTTagCompound compound, String key,
                     Map<UUID, NBTTagCompound> consumedPets) {
        consumedPets.clear();
        NBTTagList pets = compound.getTagList(key, 10);
        for (int index = 0; index < pets.tagCount(); index++) {
            NBTTagCompound entry = pets.getCompoundTagAt(index);
            if (entry.hasKey("Entity", 10)) {
                UUID petUuid = entry.hasUniqueId("id") ? entry.getUniqueId("id") : UUID.randomUUID();
                consumedPets.put(petUuid, entry.getCompoundTag("Entity").copy());
            } else {
                UUID petUuid = entry.hasUniqueId("UUID")
                        ? entry.getUniqueId("UUID") : UUID.randomUUID();
                consumedPets.put(petUuid, entry.copy());
            }
        }
    }
}
