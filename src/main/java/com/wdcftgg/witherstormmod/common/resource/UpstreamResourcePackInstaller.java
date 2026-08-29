package com.wdcftgg.witherstormmod.common.resource;

import com.wdcftgg.witherstormmod.WitherStormMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;

import java.io.File;
import java.util.List;

public final class UpstreamResourcePackInstaller {

    private UpstreamResourcePackInstaller() {
    }

    @SuppressWarnings("deprecation")
    public static void install() {
        Minecraft minecraft = Minecraft.getMinecraft();
        File source = UpstreamResourceArchive.getArchiveFile();
        addDefaultResourcePack(minecraft, source);

        minecraft.refreshResources();
        WitherStormMod.LOGGER.info("Mounted external Wither Storm resource pack: {}", source.getAbsolutePath());
    }

    private static void addDefaultResourcePack(Minecraft minecraft, File file) {
        List<IResourcePack> defaultPacks = minecraft.defaultResourcePacks;
        String expectedName = file.getName();
        defaultPacks.removeIf(pack -> expectedName.equals(pack.getPackName()));

        defaultPacks.add(0, new UpstreamResourcePack(file));
    }
}
