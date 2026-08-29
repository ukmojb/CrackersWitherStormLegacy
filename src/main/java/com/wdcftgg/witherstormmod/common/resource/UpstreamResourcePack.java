package com.wdcftgg.witherstormmod.common.resource;

import net.minecraft.client.resources.FileResourcePack;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class UpstreamResourcePack extends FileResourcePack {

    private static final byte[] PACK_METADATA = ("{\"pack\":{\"pack_format\":3,\"description\":\"Cracker's Wither Storm Legacy upstream resources\"}}")
            .getBytes(StandardCharsets.UTF_8);
    private static final String[] PADDED_TEXTURES = {"flesh_skele", "flesh_skull_e", "flesh_zomb", "flesh_zomb_e"};
    private static final String CROSSBOW_MOD_ENDER_PEARL_TEXTURE =
            "assets/witherstormmod/textures/item/crossbow_mod_ender_pearl.png";
    private static final String CROSSBOW_ENDER_PEARL_TEXTURE =
            "assets/witherstormmod/textures/item/crossbow_ender_pearl.png";
    private static final String TITLE_PANORAMA_FACE_0 =
            "assets/witherstormmod/textures/gui/title/background/panorama_0.png";

    public UpstreamResourcePack(File resourcePackFile) {
        super(resourcePackFile);
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        if ("pack.mcmeta".equals(name)) {
            return new ByteArrayInputStream(PACK_METADATA);
        }
        if (ModelResourceConverter.handles(name)) {
            String sourceName = ModelResourceConverter.sourceName(name);
            try (InputStream source = super.getInputStreamByName(sourceName)) {
                return new ByteArrayInputStream(
                        ModelResourceConverter.convert(name, source.readAllBytes()));
            }
        }
        if (LanguageResourceConverter.handles(name)) {
            byte[] localized = readResource(LanguageResourceConverter.sourceName(name));
            byte[] english = readResource(LanguageResourceConverter.englishSourceName());
            return new ByteArrayInputStream(LanguageResourceConverter.convert(localized, english));
        }
        if (SoundResourceConverter.handles(name)) {
            return new ByteArrayInputStream(SoundResourceConverter.convert(readResource(name)));
        }
        if (TITLE_PANORAMA_FACE_0.equals(name)) {
            return normalizedPanoramaTexture();
        }
        if (isModernDefinition(name)) {
            throw new FileNotFoundException(name);
        }
        String mappedName = mapLegacyTexturePath(name);
        if (CROSSBOW_MOD_ENDER_PEARL_TEXTURE.equals(mappedName)) {
            return crossbowEnderPearlOverlayTexture();
        }
        if (isLegacyParticleTexture(mappedName)) {
            InputStream normalized = normalizedParticleTexture(mappedName);
            if (normalized != null) {
                return normalized;
            }
        }
        if (isBlockTexture(mappedName)) {
            InputStream normalized = normalizedBlockTexture(mappedName);
            if (normalized != null) {
                return normalized;
            }
        }
        return super.getInputStreamByName(mappedName);
    }

    @Override
    public boolean hasResourceName(String name) {
        if ("pack.mcmeta".equals(name)) {
            return true;
        }
        if (ModelResourceConverter.handles(name)) {
            return super.hasResourceName(ModelResourceConverter.sourceName(name));
        }
        if (LanguageResourceConverter.handles(name)) {
            return super.hasResourceName(LanguageResourceConverter.sourceName(name))
                    && super.hasResourceName(LanguageResourceConverter.englishSourceName());
        }
        if (SoundResourceConverter.handles(name)) {
            return super.hasResourceName(name);
        }
        if (isModernDefinition(name)) {
            return false;
        }
        String mappedName = mapLegacyTexturePath(name);
        if (CROSSBOW_MOD_ENDER_PEARL_TEXTURE.equals(mappedName)) {
            return super.hasResourceName(CROSSBOW_ENDER_PEARL_TEXTURE);
        }
        if (isLegacyParticleTexture(mappedName)) {
            return hasResourceNameDirect(mappedName);
        }
        if (isBlockTexture(mappedName) && hasNormalizedBlockTexture(mappedName)) {
            return true;
        }
        return super.hasResourceName(mappedName);
    }

    private InputStream crossbowEnderPearlOverlayTexture() throws IOException {
        BufferedImage image;
        try (InputStream source = super.getInputStreamByName(CROSSBOW_ENDER_PEARL_TEXTURE)) {
            image = ImageIO.read(source);
        }
        if (image == null) {
            throw new IOException("Unable to decode upstream crossbow ender pearl texture");
        }
        BufferedImage overlay = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        try {
            int retainedPixels = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int color = image.getRGB(x, y);
                    int red = color >>> 16 & 255;
                    int green = color >>> 8 & 255;
                    int blue = color & 255;
                    if ((color >>> 24) != 0 && green > red && blue > red) {
                        overlay.setRGB(x, y, color);
                        retainedPixels++;
                    }
                }
            }
            if (retainedPixels == 0) {
                throw new IOException("Upstream crossbow ender pearl texture has no pearl pixels");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(overlay, "png", output);
            return new ByteArrayInputStream(output.toByteArray());
        } finally {
            image.flush();
            overlay.flush();
        }
    }






    private InputStream normalizedPanoramaTexture() throws IOException {
        BufferedImage image;
        try (InputStream source = super.getInputStreamByName(TITLE_PANORAMA_FACE_0)) {
            image = ImageIO.read(source);
        }
        if (image == null) {
            throw new IOException("Unable to decode upstream title panorama");
        }
        int targetSize = nextPowerOfTwo(Math.max(image.getWidth(), image.getHeight()));
        if (image.getWidth() == targetSize && image.getHeight() == targetSize) {
            image.flush();
            return super.getInputStreamByName(TITLE_PANORAMA_FACE_0);
        }

        BufferedImage normalized = new BufferedImage(
                targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(image, 0, 0, targetSize, targetSize, null);
        } finally {
            graphics.dispose();
            image.flush();
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(normalized, "png", output);
            return new ByteArrayInputStream(output.toByteArray());
        } finally {
            normalized.flush();
        }
    }

    private InputStream normalizedBlockTexture(String name) throws IOException {
        if (name.endsWith(".mcmeta") || !hasResourceNameDirect(name)) {
            return null;
        }
        String metadataName = name + ".mcmeta";
        if (hasResourceNameDirect(metadataName)) {
            return null;
        }
        BufferedImage image;
        try (InputStream source = super.getInputStreamByName(name)) {
            image = ImageIO.read(source);
        }
        if (image == null) {
            return null;
        }
        int largestDimension = Math.max(image.getWidth(), image.getHeight());
        int targetSize = 1;
        while (targetSize < largestDimension) {
            targetSize <<= 1;
        }
        if (image.getWidth() == targetSize && image.getHeight() == targetSize) {
            image.flush();
            return super.getInputStreamByName(name);
        }
        BufferedImage normalized = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            if (isPaddedStaticTexture(name)) {
                graphics.drawImage(image, 0, 0, null);
            } else {
                graphics.drawImage(image, 0, 0, targetSize, targetSize, null);
            }
        } finally {
            graphics.dispose();
            image.flush();
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(normalized, "png", output);
            return new ByteArrayInputStream(output.toByteArray());
        } finally {
            normalized.flush();
        }
    }

    private InputStream normalizedParticleTexture(String name) throws IOException {
        if (!hasResourceNameDirect(name)) {
            return null;
        }
        BufferedImage image;
        try (InputStream source = super.getInputStreamByName(name)) {
            image = ImageIO.read(source);
        }
        if (image == null || image.getWidth() <= 0 || image.getHeight() % image.getWidth() != 0) {
            if (image != null) image.flush();
            return super.getInputStreamByName(name);
        }

        int frameCount = image.getHeight() / image.getWidth();
        int frameSize = Math.max(16, nextPowerOfTwo(image.getWidth()));
        if (image.getWidth() == frameSize) {
            image.flush();
            return super.getInputStreamByName(name);
        }

        BufferedImage normalized = new BufferedImage(
                frameSize, frameSize * frameCount, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(image, 0, 0, normalized.getWidth(), normalized.getHeight(), null);
        } finally {
            graphics.dispose();
            image.flush();
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(normalized, "png", output);
            return new ByteArrayInputStream(output.toByteArray());
        } finally {
            normalized.flush();
        }
    }

    private boolean hasNormalizedBlockTexture(String name) {
        if (!hasResourceNameDirect(name)) {
            return false;
        }
        return !hasResourceNameDirect(name + ".mcmeta");
    }

    private boolean hasResourceNameDirect(String name) {
        return super.hasResourceName(name);
    }

    private static boolean isBlockTexture(String name) {
        return name.startsWith("assets/")
                && (name.contains("/textures/blocks/") || name.contains("/textures/block/"))
                && name.endsWith(".png");
    }

    private static boolean isLegacyParticleTexture(String name) {
        if (!name.startsWith("assets/witherstormmod/textures/particle/") || !name.endsWith(".png")) {
            return false;
        }
        String fileName = name.substring(name.lastIndexOf('/') + 1);
        return "command_block.png".equals(fileName)
                || "command_block_1.png".equals(fileName)
                || "command_block_2.png".equals(fileName)
                || "command_block_3.png".equals(fileName)
                || "phlegm.png".equals(fileName)
                || "tractor_beam.png".equals(fileName);
    }

    private static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) result <<= 1;
        return result;
    }

    private static String mapLegacyTexturePath(String name) {
        return name.replace("/textures/blocks/", "/textures/block/")
                .replace("/textures/items/", "/textures/item/");
    }

    private static boolean isPaddedStaticTexture(String name) {
        int separator = name.lastIndexOf('/');
        String base = separator < 0 ? name : name.substring(separator + 1);
        if (base.endsWith(".png")) {
            base = base.substring(0, base.length() - 4);
        }
        for (String padded : PADDED_TEXTURES) {
            if (padded.equals(base)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isModernDefinition(String name) {
        return name.startsWith("assets/witherstormmod/lang/")
                || name.startsWith("assets/minecraft/models/")
                || name.startsWith("assets/minecraft/blockstates/");
    }

    private byte[] readResource(String name) throws IOException {
        try (InputStream input = super.getInputStreamByName(name)) {
            return input.readAllBytes();
        }
    }
}
