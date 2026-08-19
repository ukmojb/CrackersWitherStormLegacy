package com.wdcftgg.witherstormmod;

import com.wdcftgg.witherstormmod.common.advancement.ModCriteriaTriggers;
import com.wdcftgg.witherstormmod.common.advancement.ExternalAdvancements;
import com.wdcftgg.witherstormmod.api.common.ai.witherstorm.WitherStormWorldInteractions;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessCapability;
import com.wdcftgg.witherstormmod.common.command.WitherStormAdminCommand;
import com.wdcftgg.witherstormmod.common.command.WitherStormSelectorHandler;
import com.wdcftgg.witherstormmod.common.compat.CrossbowCompatibilityBootstrap;
import com.wdcftgg.witherstormmod.common.entity.SymbiontSpells;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.init.ModAttributes;
import com.wdcftgg.witherstormmod.common.init.ModCreatureAttributes;
import com.wdcftgg.witherstormmod.common.init.ModEntities;
import com.wdcftgg.witherstormmod.common.init.ModEffects;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import com.wdcftgg.witherstormmod.common.init.ModPaintings;
import com.wdcftgg.witherstormmod.common.init.ModPotionTypes;
import com.wdcftgg.witherstormmod.common.init.ModRecipes;
import com.wdcftgg.witherstormmod.common.init.ModTileEntities;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.init.ModStats;
import com.wdcftgg.witherstormmod.common.loot.LootConditions;
import com.wdcftgg.witherstormmod.common.loot.LootFunctions;
import com.wdcftgg.witherstormmod.common.loot.ExternalLootTables;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.proxy.CommonProxy;
import com.wdcftgg.witherstormmod.common.gui.ModGuiHandler;
import com.wdcftgg.witherstormmod.common.resource.UpstreamResourceArchive;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.resource.UpstreamEntityTags;
import com.wdcftgg.witherstormmod.common.resource.UpstreamItemTags;
import com.wdcftgg.witherstormmod.common.taint.TaintingData;
import com.wdcftgg.witherstormmod.common.world.BowelsDimensions;
import com.wdcftgg.witherstormmod.common.world.ChunkLoadingManager;
import com.wdcftgg.witherstormmod.common.world.WitherStormSpawnManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import com.cleanroommc.assetmover.AssetMoverAPI;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION,
        acceptableRemoteVersions = "[1.0.0-beta3]",
        dependencies = "required-after:futuremc;required-after:crossbow;required-after:assetmover",
        guiFactory = "com.wdcftgg.witherstormmod.client.config.WitherStormGuiFactory")
public class WitherStormMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    public static final String UPSTREAM_RESOURCEPACK_NAME = "witherstormmod-1.20.1-4.2.1-all.jar";

    @Mod.Instance(Tags.MOD_ID)
    public static WitherStormMod INSTANCE;

    public WitherStormMod() {
        if (FMLCommonHandler.instance().getSide() != Side.CLIENT) return;
        Map<String, String> panoramaAssets = new HashMap<String, String>();
        for (int face = 1; face <= 5; face++) {
            String path = "assets/minecraft/textures/gui/title/background/panorama_" + face + ".png";
            panoramaAssets.put(path, path);
        }
        // Keep Mojang's vanilla faces outside this JAR; AssetMover downloads them
        // into its own resource pack during construction, before preInit reloads assets.
        AssetMoverAPI.fromMinecraft("1.20.1", panoramaAssets);
    }

    @SidedProxy(
            clientSide = "com.wdcftgg.witherstormmod.common.proxy.ClientProxy",
            serverSide = "com.wdcftgg.witherstormmod.common.proxy.CommonProxy")
    public static CommonProxy proxy;

    /** Matches the upstream date gate shared by April Fools client effects. */
    public static boolean isAprilFools() {
        LocalDate date = LocalDate.now();
        return date.getMonthValue() == 4 && date.getDayOfMonth() == 1;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        WitherStormSelectorHandler.register();
        CrossbowCompatibilityBootstrap.register();
        try {
            UpstreamResourceArchive.initialize(event.getModConfigurationDirectory().getParentFile());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to initialize the external Wither Storm resource pack", exception);
        }
        UpstreamBlockTags.initialize();
        UpstreamEntityTags.initialize();
        UpstreamItemTags.initialize();
        TaintingData.initialize();
        WitherStormWorldInteractions.initialize();
        WitherSicknessCapability.register();
        LootConditions.register();
        LootFunctions.register();
        ExternalLootTables.initialize();
        ModNetwork.register();
        BowelsDimensions.register();
        ChunkLoadingManager.INSTANCE.register(this);
        GameRegistry.registerWorldGenerator(WitherStormSpawnManager.INSTANCE, 0);
        ModBlocks.bootstrap();
        ModItems.bootstrap();
        ModEffects.bootstrap();
        ModPotionTypes.bootstrap();
        ModSounds.bootstrap();
        ModAttributes.bootstrap();
        ModCreatureAttributes.bootstrap();
        ModEntities.register();
        ModCriteriaTriggers.register();
        ModTileEntities.register();
        ModStats.register();
        ModPaintings.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
        proxy.preInit(event);
        LOGGER.info("Loaded {} as a 1.12.2 port using upstream resources from {}", Tags.MOD_NAME, UPSTREAM_RESOURCEPACK_NAME);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModRecipes.registerSmelting();
        ModRecipes.registerAnvil();
        ModRecipes.registerStonecutting();
        ModRecipes.registerBrewing();
        proxy.init(event);
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        try {
            ExternalAdvancements.install(event.getServer());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(
                    "Unable to prepare advancements from the external Wither Storm resource pack", exception);
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new WitherStormAdminCommand());
    }
}
