package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;





public final class AmuletPaintingAtlas {

    public static final ResourceLocation ATLAS = new ResourceLocation(Tags.MOD_ID,
            "textures/painting/amulet_atlas");
    private static DynamicTexture texture;

    private AmuletPaintingAtlas() {
    }

    public static void ensureLoaded() {
        if (texture != null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        try {
            ResourceLocation source = new ResourceLocation(Tags.MOD_ID,
                    "textures/painting/amulet.png");
            IResource resource = minecraft.getResourceManager().getResource(source);
            BufferedImage image = ImageIO.read(resource.getInputStream());
            BufferedImage atlas = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = atlas.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(image, 0, 0, 16, 32,
                    0, 0, image.getWidth(), image.getHeight(), null);
            graphics.dispose();
            image.flush();
            texture = new DynamicTexture(atlas);
            minecraft.getTextureManager().loadTexture(ATLAS, texture);
        } catch (IOException exception) {
            WitherStormMod.LOGGER.error("Unable to build the Amulet painting atlas", exception);
        }
    }
}
