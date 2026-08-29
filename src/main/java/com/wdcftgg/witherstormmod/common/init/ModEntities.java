package com.wdcftgg.witherstormmod.common.init;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.entity.PowerfulExplosiveEntity;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.SymbiontDragonFireballEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public final class ModEntities {
    private static final int DISTANT_STORM_TRACKING_RANGE = Integer.MAX_VALUE;
    static final int WITHERED_SYMBIONT_UPDATE_FREQUENCY = 3;

    private ModEntities() {
    }

    public static void register() {
        int entityId = 1;
        registerLiving("wither_storm", WitherStormEntity.class, entityId++, 0x21162C, 0x7F3FBA,
                DISTANT_STORM_TRACKING_RANGE, 1);
        registerLiving("sickened_bee", SickenedEntities.SickenedBeeEntity.class, entityId++, 0xD6B34A, 0x6D2A83, 80);
        registerLiving("sickened_cat", SickenedEntities.SickenedCatEntity.class, entityId++, 0x795548, 0xB22AFF, 80);
        registerLiving("sickened_chicken", SickenedEntities.SickenedChickenEntity.class, entityId++, 0xD8D8D8, 0x6D2A83, 80);
        registerLiving("sickened_cow", SickenedEntities.SickenedCowEntity.class, entityId++, 0x44352E, 0xA12AFF, 80);
        registerLiving("sickened_creeper", SickenedEntities.SickenedCreeperEntity.class, entityId++, 0x3E8E31, 0xA12AFF, 80);
        registerLiving("sickened_iron_golem", SickenedEntities.SickenedIronGolemEntity.class, entityId++, 0xC0A98B, 0xA12AFF, 96);
        registerLiving("sickened_mushroom_cow", SickenedEntities.SickenedMushroomCowEntity.class, entityId++, 0xA93D37, 0x6D2A83, 80);
        registerLiving("sickened_parrot", SickenedEntities.SickenedParrotEntity.class, entityId++, 0xCC2E2E, 0xA12AFF, 80);
        registerLiving("sickened_phantom", SickenedEntities.SickenedPhantomEntity.class, entityId++, 0x52636A, 0xA12AFF, 96);
        registerLiving("sickened_pig", SickenedEntities.SickenedPigEntity.class, entityId++, 0xE49A9A, 0x6D2A83, 80);
        registerLiving("sickened_pillager", SickenedEntities.SickenedPillagerEntity.class, entityId++, 0x53605C, 0xA12AFF, 96);
        registerLiving("sickened_skeleton", SickenedEntities.SickenedSkeletonEntity.class, entityId++, 0xC7C7C7, 0x6D2A83, 80);
        registerLiving("sickened_snow_golem", SickenedEntities.SickenedSnowGolemEntity.class, entityId++, 0xE8F1F1, 0xA12AFF, 80);
        registerLiving("sickened_spider", SickenedEntities.SickenedSpiderEntity.class, entityId++, 0x342D27, 0xA12AFF, 80);
        registerLiving("sickened_villager", SickenedEntities.SickenedVillagerEntity.class, entityId++, 0x7A5A42, 0x6D2A83, 80);
        registerLiving("sickened_vindicator", SickenedEntities.SickenedVindicatorEntity.class, entityId++, 0x596560, 0xA12AFF, 96);
        registerLiving("sickened_wolf", SickenedEntities.SickenedWolfEntity.class, entityId++, 0xA9A9A9, 0x6D2A83, 80);
        registerLiving("sickened_zombie", SickenedEntities.SickenedZombieEntity.class, entityId++, 0x507A4A, 0xA12AFF, 80);
        registerLiving("tentacle", SickenedEntities.TentacleEntity.class, entityId++, 0x201323, 0x913CC4, 160);


        registerLiving("withered_symbiont", SickenedEntities.WitheredSymbiontEntity.class, entityId++,
                0x251A2A, 0xD053FF, 160, WITHERED_SYMBIONT_UPDATE_FREQUENCY);
        registerLiving("tainted_slime", SickenedEntities.TaintedSlimeEntity.class, entityId++, 0x34203B, 0xA64DCF, 80);
        registerProjectile("super_tnt", PowerfulExplosiveEntity.SuperTntEntity.class, entityId++);
        registerProjectile("formidibomb", PowerfulExplosiveEntity.FormidibombEntity.class, entityId++);
        registerProjectile("flaming_wither_skull", SupplementalEntities.FlamingWitherSkullEntity.class, entityId++);
        registerProjectile("blue_flaming_wither_skull", SupplementalEntities.BlueFlamingWitherSkullEntity.class, entityId++);
        registerProjectile("tentacle_spike", SupplementalEntities.TentacleSpikeEntity.class, entityId++);
        registerProjectile("block_cluster", SupplementalEntities.BlockClusterEntity.class, entityId++, 1);
        registerLiving("command_block", SupplementalEntities.CommandBlockEntity.class, entityId++,
                0xBA6B33, 0x232323, 160, 1);
        registerLiving("wither_storm_head", SupplementalEntities.WitherStormHeadEntity.class, entityId++,
                0x21162C, 0x7F3FBA, DISTANT_STORM_TRACKING_RANGE, 1);
        registerLiving("wither_storm_segment", SupplementalEntities.WitherStormSegmentEntity.class, entityId,
                0x21162C, 0x4C285B, DISTANT_STORM_TRACKING_RANGE, 1);
        registerProjectile("symbiont_dragon_fireball", SymbiontDragonFireballEntity.class, entityId + 1);
    }

    private static void registerLiving(String name, Class<? extends Entity> entityClass, int entityId, int primaryColor, int secondaryColor, int trackingRange) {
        registerLiving(name, entityClass, entityId, primaryColor, secondaryColor, trackingRange, 2);
    }

    private static void registerLiving(String name, Class<? extends Entity> entityClass, int entityId,
                                       int primaryColor, int secondaryColor, int trackingRange,
                                       int updateFrequency) {
        EntityRegistry.registerModEntity(new ResourceLocation(Tags.MOD_ID, name), entityClass, name, entityId,
                Tags.MOD_ID, trackingRange, updateFrequency, true, primaryColor, secondaryColor);
    }

    private static void registerProjectile(String name, Class<? extends Entity> entityClass, int entityId) {
        registerProjectile(name, entityClass, entityId, 10);
    }

    private static void registerProjectile(String name, Class<? extends Entity> entityClass, int entityId,
                                           int updateFrequency) {
        EntityRegistry.registerModEntity(new ResourceLocation(Tags.MOD_ID, name), entityClass, name, entityId,
                Tags.MOD_ID, 160, updateFrequency, true);
    }
}
