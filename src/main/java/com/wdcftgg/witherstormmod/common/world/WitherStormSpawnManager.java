package com.wdcftgg.witherstormmod.common.world;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WitherStormSpawnManager implements IWorldGenerator {
    public static final WitherStormSpawnManager INSTANCE = new WitherStormSpawnManager();

    private static final int OVERWORLD = 0;
    private static final int STRUCTURE_SALT = 406417795;
    private static final int MINIMUM_AUTO_SPAWN_TICKS = 100;
    private static final int PLATFORM_RECOVERY_INTERVAL = 20;

    private WitherStormSpawnManager() {
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator,
                         IChunkProvider chunkProvider) {
        if (world.isRemote || world.provider.getDimension() != OVERWORLD || chunkX != 0 || chunkZ != 0
                || !world.getWorldInfo().isMapFeaturesEnabled()) return;
        WitherStormSpawnData data = WitherStormSpawnData.get(world);
        generateStartingStructure(world, data, "chunk generation");
    }

    private void generateStartingStructure(World world, WitherStormSpawnData data, String source) {
        if (data.isPlatformGenerated() && data.getSpawnPosition() != null) return;

        Random platformRandom = new Random(world.getSeed() ^ STRUCTURE_SALT);
        BlockPos origin = world.getHeight(BlockPos.ORIGIN);
        String template = selectPlatform(world.getBiome(origin), platformRandom);
        Rotation[] rotations = Rotation.values();
        Rotation rotation = rotations[platformRandom.nextInt(rotations.length)];
        BlockPos spawnPosition = StructureTemplates.placeStormSpawnPlatform(world, template, origin, rotation);
        if (spawnPosition == null) {
            WitherStormMod.LOGGER.error("Unable to generate upstream Wither Storm spawn platform {}", template);
            return;
        }
        data.recordPlatform(spawnPosition);
        WitherStormMod.LOGGER.info("Generated {} at {} with storm spawn marker {} via {}", template,
                origin, spawnPosition, source);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote
                || !(event.world instanceof WorldServer)
                || event.world.provider.getDimension() != OVERWORLD) {
            return;
        }
        INSTANCE.tick((WorldServer) event.world);
    }

    private void tick(WorldServer world) {
        WitherStormSpawnData data = WitherStormSpawnData.get(world);
        recoverMissingStartingStructure(world, data);
        if (!WitherStormConfig.isSummoningDimensionAllowed(
                world.provider.getDimension())) return;
        if (!WitherStormConfig.autoSpawnWitherStorm) {
            data.setHasSpawnedWitherStorm(true);
            return;
        }

        int elapsedTicks = data.advanceTickCount();
        int requiredTicks = Math.max(WitherStormConfig.autoSpawnTime * 60 * 20,
                MINIMUM_AUTO_SPAWN_TICKS);
        if (data.hasSpawnedWitherStorm() || elapsedTicks <= requiredTicks) return;

        // 上游在查找平台前就锁定生成状态，因此平台缺失时也不会每刻重试。
        data.setHasSpawnedWitherStorm(true);
        BlockPos spawnPosition = data.getSpawnPosition();
        if (spawnPosition == null) {
            WitherStormMod.LOGGER.warn("Automatic Wither Storm spawn skipped because no platform marker was generated");
            return;
        }

        world.getChunkProvider().provideChunk(spawnPosition.getX() >> 4, spawnPosition.getZ() >> 4);
        WitherStormEntity storm = new WitherStormEntity(world);
        storm.setPosition(spawnPosition.getX() + 0.5D, spawnPosition.getY(), spawnPosition.getZ() + 0.5D);
        storm.makeInvulnerable();
        if (ModSounds.get("command_block_activates") != null) {
            world.playSound(null, spawnPosition, ModSounds.get("command_block_activates"),
                    SoundCategory.HOSTILE, 4.0F, 1.0F);
        }
        if (!world.spawnEntity(storm)) {
            WitherStormMod.LOGGER.error("Automatic Wither Storm spawn failed at {}", spawnPosition);
            return;
        }
        for (EntityPlayer player : world.playerEntities) {
            if (player instanceof EntityPlayerMP && !player.isSpectator()) {
                CriteriaTriggers.SUMMONED_ENTITY.trigger((EntityPlayerMP) player, storm);
            }
        }
        WitherStormMod.LOGGER.info("Automatically spawned the Wither Storm at {} after {} ticks",
                spawnPosition, elapsedTicks);
    }

    private void recoverMissingStartingStructure(WorldServer world, WitherStormSpawnData data) {
        if (data.isPlatformGenerated() && data.getSpawnPosition() != null) return;
        if (!world.getWorldInfo().isMapFeaturesEnabled()
                || world.getTotalWorldTime() % PLATFORM_RECOVERY_INTERVAL != 0L) return;

        // Existing worlds may already contain chunk 0,0 from before this generator was present.
        // Providing the chunk lets the normal callback run for new chunks; the direct call then
        // repairs only the old-chunk case if no platform record was produced.
        world.getChunkProvider().provideChunk(0, 0);
        if (!data.isPlatformGenerated() || data.getSpawnPosition() == null) {
            generateStartingStructure(world, data, "world tick recovery");
        }
    }

    private static String selectPlatform(Biome biome, Random random) {
        if (WitherStormConfig.autoSpawnWitherStorm) return "auto_spawn_platform";

        boolean taiga = BiomeDictionary.hasType(biome, BiomeDictionary.Type.CONIFEROUS);
        boolean forest = BiomeDictionary.hasType(biome, BiomeDictionary.Type.FOREST) && !taiga;
        ResourceLocation biomeName = biome.getRegistryName();
        boolean darkForest = biomeName != null && biomeName.getPath().contains("roofed_forest");
        if (forest && random.nextFloat() <= 0.2F) return "ruins_storm_spawn_platform";
        if ((taiga || darkForest) && random.nextFloat() <= 0.25F) {
            return "order_temple_storm_spawn_platform";
        }
        if ((forest || BiomeDictionary.hasType(biome, BiomeDictionary.Type.PLAINS))
                && random.nextFloat() <= 0.2F) {
            return "forest_storm_spawn_platform";
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY)
                && BiomeDictionary.hasType(biome, BiomeDictionary.Type.HOT)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)) {
            return "desert_storm_spawn_platform";
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.JUNGLE)) {
            return "jungle_storm_spawn_platform";
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SAVANNA)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.MESA)) {
            return "savanna_storm_spawn_platform";
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)) {
            return "snowy_storm_spawn_platform";
        }
        if (taiga) return "taiga_storm_spawn_platform";
        return "storm_spawn_platform";
    }
}
