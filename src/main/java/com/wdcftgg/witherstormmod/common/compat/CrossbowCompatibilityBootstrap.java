package com.wdcftgg.witherstormmod.common.compat;

import com.wdcftgg.witherstormmod.WitherStormMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;


public final class CrossbowCompatibilityBootstrap {
    private static final String CROSSBOW_MOD_ID = "crossbow";
    private static final String EVENT_HANDLER =
            "com.wdcftgg.witherstormmod.common.compat.CrossbowModCompatibility";

    private CrossbowCompatibilityBootstrap() {
    }

    public static void register() {
        if (!Loader.isModLoaded(CROSSBOW_MOD_ID)) return;
        try {
            MinecraftForge.EVENT_BUS.register(Class.forName(EVENT_HANDLER));
            WitherStormMod.LOGGER.info("Enabled Crossbow ender pearl compatibility");
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Unable to initialize Crossbow compatibility", exception);
        }
    }
}
