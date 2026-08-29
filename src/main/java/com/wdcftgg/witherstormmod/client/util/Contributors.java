package com.wdcftgg.witherstormmod.client.util;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.resource.UpstreamResourceArchive;
import net.minecraft.client.Minecraft;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class Contributors {
    private static final String CONTRIBUTORS_CLASS =
            "nonamecrackers2/witherstormmod/client/util/Contributors.class";
    private static final Pattern CONTRIBUTOR_ENTRY =
            Pattern.compile("^(dev|patron|kofi)\\.([A-Za-z0-9_]{1,16})$");
    private static final int CLASS_FILE_MAGIC = 0xCAFEBABE;

    private static volatile ContributorSets contributorSets;

    private Contributors() {
    }

    public static boolean isDeveloper(String name) {
        return getContributorSets().developers.contains(name);
    }

    public static boolean isPatron(String name) {
        return getContributorSets().patrons.contains(name);
    }

    public static boolean isKofi(String name) {
        return getContributorSets().kofi.contains(name);
    }

    public static boolean currentPlayerHasCosmetic() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null) return false;
        String name = minecraft.player.getGameProfile().getName();
        return isDeveloper(name) || isPatron(name) || isKofi(name);
    }

    private static ContributorSets getContributorSets() {
        ContributorSets current = contributorSets;
        if (current != null) return current;
        synchronized (Contributors.class) {
            current = contributorSets;
            if (current == null) {
                current = loadContributorSets();
                contributorSets = current;
            }
        }
        return current;
    }

    private static ContributorSets loadContributorSets() {
        Set<String> developers = new HashSet<String>();
        Set<String> patrons = new HashSet<String>();
        Set<String> kofi = new HashSet<String>();
        try (InputStream stream = UpstreamResourceArchive.open(CONTRIBUTORS_CLASS);
             DataInputStream input = new DataInputStream(new BufferedInputStream(stream))) {
            readConstantPool(input, developers, patrons, kofi);
            WitherStormMod.LOGGER.info(
                    "Loaded upstream contributor cosmetics: developers={}, patrons={}, kofi={}",
                    developers.size(), patrons.size(), kofi.size());
        } catch (IOException | RuntimeException exception) {
            WitherStormMod.LOGGER.warn(
                    "Failed to read contributor cosmetics from the external upstream JAR", exception);
        }
        return new ContributorSets(developers, patrons, kofi);
    }

    private static void readConstantPool(DataInputStream input, Set<String> developers,
                                         Set<String> patrons, Set<String> kofi) throws IOException {
        if (input.readInt() != CLASS_FILE_MAGIC) {
            throw new IOException("Upstream Contributors.class has an invalid class header");
        }
        input.readUnsignedShort();
        input.readUnsignedShort();
        int constantPoolCount = input.readUnsignedShort();
        for (int index = 1; index < constantPoolCount; index++) {
            int tag = input.readUnsignedByte();
            switch (tag) {
                case 1:
                    addContributor(input.readUTF(), developers, patrons, kofi);
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
                case 8:
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
    }

    private static void addContributor(String value, Set<String> developers,
                                       Set<String> patrons, Set<String> kofi) {
        Matcher matcher = CONTRIBUTOR_ENTRY.matcher(value);
        if (!matcher.matches()) return;
        String name = matcher.group(2);
        switch (matcher.group(1)) {
            case "dev":
                developers.add(name);
                break;
            case "patron":
                patrons.add(name);
                break;
            case "kofi":
                kofi.add(name);
                break;
            default:
                break;
        }
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

    private static final class ContributorSets {
        private final Set<String> developers;
        private final Set<String> patrons;
        private final Set<String> kofi;

        private ContributorSets(Set<String> developers, Set<String> patrons, Set<String> kofi) {
            this.developers = Collections.unmodifiableSet(new HashSet<String>(developers));
            this.patrons = Collections.unmodifiableSet(new HashSet<String>(patrons));
            this.kofi = Collections.unmodifiableSet(new HashSet<String>(kofi));
        }
    }
}
