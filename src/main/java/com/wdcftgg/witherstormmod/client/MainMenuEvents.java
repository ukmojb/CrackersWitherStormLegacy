package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.client.util.UpstreamSplashes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Client event replacements for the main-menu injections. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class MainMenuEvents {
    private MainMenuEvents() {
    }

    @SubscribeEvent
    public static void chooseSplash(GuiOpenEvent event) {
        if (!(event.getGui() instanceof GuiMainMenu)) return;
        PanoramaCustomizer.sync();
        String splash = UpstreamSplashes.choose(Minecraft.getMinecraft().getResourceManager());
        if (splash != null) ((GuiMainMenu) event.getGui()).splashText = splash;
    }

    @SubscribeEvent
    public static void syncPanorama(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.getGui() instanceof GuiMainMenu) PanoramaCustomizer.sync();
    }
}
