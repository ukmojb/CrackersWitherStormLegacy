package com.wdcftgg.witherstormmod.client.util;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.resource.UpstreamResourceArchive;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public final class UpstreamSplashes {
    private static final String SPLASH_MIXIN_CLASS =
            "nonamecrackers2/witherstormmod/mixin/MixinSplashManager.class";
    private static final ResourceLocation VANILLA_SPLASHES =
            new ResourceLocation("minecraft", "texts/splashes.txt");
    private static final int CLASS_FILE_MAGIC = 0xCAFEBABE;
    private static final int MINCERAFT_HASH = 125780783;
    private static final Random RANDOM = new Random();

    private static volatile List<String> upstreamSplashes;

    private UpstreamSplashes() {
    }

    @Nullable
    public static String choose(IResourceManager resourceManager) {
        List<String> choices = new ArrayList<String>();
        try (IResource resource = resourceManager.getResource(VANILLA_SPLASHES);
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && line.hashCode() != MINCERAFT_HASH) choices.add(line);
            }
        } catch (IOException exception) {
            WitherStormMod.LOGGER.warn("Unable to read the vanilla main-menu splashes", exception);
        }
        choices.addAll(getUpstreamSplashes());
        return choices.isEmpty() ? null : choices.get(RANDOM.nextInt(choices.size()));
    }

    private static List<String> getUpstreamSplashes() {
        List<String> current = upstreamSplashes;
        if (current != null) return current;
        synchronized (UpstreamSplashes.class) {
            current = upstreamSplashes;
            if (current == null) {
                current = loadUpstreamSplashes();
                upstreamSplashes = current;
            }
        }
        return current;
    }

    private static List<String> loadUpstreamSplashes() {
        try (InputStream stream = UpstreamResourceArchive.open(SPLASH_MIXIN_CLASS);
             DataInputStream input = new DataInputStream(new BufferedInputStream(stream))) {
            List<String> splashes = readStringConstants(input);
            WitherStormMod.LOGGER.info("Loaded {} upstream main-menu splashes", splashes.size());
            return Collections.unmodifiableList(splashes);
        } catch (IOException | RuntimeException exception) {
            WitherStormMod.LOGGER.warn(
                    "Failed to read main-menu splashes from the external upstream JAR", exception);
            return Collections.emptyList();
        }
    }

    private static List<String> readStringConstants(DataInputStream input) throws IOException {
        if (input.readInt() != CLASS_FILE_MAGIC) {
            throw new IOException("Upstream MixinSplashManager.class has an invalid class header");
        }
        input.readUnsignedShort();
        input.readUnsignedShort();
        int constantPoolCount = input.readUnsignedShort();
        String[] utf8 = new String[constantPoolCount];
        int[] stringIndexes = new int[constantPoolCount];
        for (int index = 1; index < constantPoolCount; index++) {
            int tag = input.readUnsignedByte();
            switch (tag) {
                case 1:
                    utf8[index] = input.readUTF();
                    break;
                case 8:
                    stringIndexes[index] = input.readUnsignedShort();
                    break;
                case 3:
                case 4:
                case 9:
                case 10:
                case 11:
                case 12:
                case 17:
                case 18:
                    skipFully(input, 4);
                    break;
                case 5:
                case 6:
                    skipFully(input, 8);
                    index++;
                    break;
                case 7:
                case 16:
                case 19:
                case 20:
                    skipFully(input, 2);
                    break;
                case 15:
                    skipFully(input, 3);
                    break;
                default:
                    throw new IOException("Unsupported class constant tag " + tag);
            }
        }
        List<String> splashes = new ArrayList<String>();
        for (int stringIndex : stringIndexes) {
            if (stringIndex <= 0 || stringIndex >= utf8.length || utf8[stringIndex] == null) continue;
            splashes.add(utf8[stringIndex]);
        }
        return splashes;
    }

    private static void skipFully(DataInputStream input, int length) throws IOException {
        int remaining = length;
        while (remaining > 0) {
            int skipped = input.skipBytes(remaining);
            if (skipped <= 0) {
                input.readByte();
                skipped = 1;
            }
            remaining -= skipped;
        }
    }
}
