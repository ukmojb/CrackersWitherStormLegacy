package com.wdcftgg.witherstormmod.common.resource;

import com.wdcftgg.witherstormmod.WitherStormMod;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;

public final class UpstreamResourceArchive {

    private static volatile File archiveFile;
    private static volatile List<String> entryNames = Collections.emptyList();
    private static final Map<String, byte[]> ENTRY_CACHE = new ConcurrentHashMap<>();

    private UpstreamResourceArchive() {
    }

    public static synchronized void initialize(File gameDirectory) throws IOException {
        File resourcePackDirectory = new File(gameDirectory, "resourcepacks");
        initializeArchive(new File(resourcePackDirectory, WitherStormMod.UPSTREAM_RESOURCEPACK_NAME));
    }

    public static synchronized void initializeArchive(File candidate) throws IOException {
        File canonical = candidate.getCanonicalFile();
        if (!canonical.isFile()) {
            throw new IOException("Required external resource pack is missing: " + canonical.getAbsolutePath());
        }
        if (canonical.equals(archiveFile)) return;

        List<String> validatedEntries = validate(canonical);
        ENTRY_CACHE.clear();
        entryNames = validatedEntries;
        archiveFile = canonical;
    }

    public static File getArchiveFile() {
        File file = archiveFile;
        if (file == null) {
            throw new IllegalStateException("The external Wither Storm resource pack has not been initialized");
        }
        return file;
    }

    public static InputStream open(String entryName) throws IOException {
        getArchiveFile();
        byte[] cached = ENTRY_CACHE.get(entryName);
        if (cached != null) return new ByteArrayInputStream(cached);

        byte[] content;
        try (ZipFile archive = new ZipFile(archiveFile)) {
            ZipEntry entry = archive.getEntry(entryName);
            if (entry == null || entry.isDirectory()) {
                throw new IOException("Missing upstream resource: " + entryName);
            }
            try (InputStream input = archive.getInputStream(entry)) {
                content = input.readAllBytes();
            }
        }
        byte[] existing = ENTRY_CACHE.putIfAbsent(entryName, content);
        return new ByteArrayInputStream(existing == null ? content : existing);
    }

    public static List<String> listEntries(String prefix, String suffix) throws IOException {
        if (!isSafeSelector(prefix) || !isSafeSelector(suffix)) {
            throw new IllegalArgumentException("Unsafe upstream resource selector");
        }
        List<String> names = new ArrayList<String>();
        getArchiveFile();
        for (String name : entryNames) {
            if (name.startsWith(prefix) && name.endsWith(suffix)) {
                names.add(name);
            }
        }
        return Collections.unmodifiableList(names);
    }

    private static List<String> validate(File file) throws IOException {
        if (file.length() == 0L) {
            throw new IOException("Required external resource pack is empty: " + file.getAbsolutePath());
        }
        try (ZipFile archive = new ZipFile(file)) {
            ZipEntry manifestEntry = archive.getEntry("META-INF/MANIFEST.MF");
            if (manifestEntry == null
                    || archive.getEntry("META-INF/mods.toml") == null
                    || archive.getEntry("pack.mcmeta") == null
                    || archive.getEntry("assets/witherstormmod/sounds.json") == null
                    || archive.getEntry("assets/witherstormmod/particles/command_block.json") == null
                    || archive.getEntry("assets/witherstormmod/particles/tractor_beam.json") == null
                    || archive.getEntry("assets/witherstormmod/textures/particle/command_block.png") == null
                    || archive.getEntry("assets/witherstormmod/textures/particle/command_block_1.png") == null
                    || archive.getEntry("assets/witherstormmod/textures/particle/command_block_2.png") == null
                    || archive.getEntry("assets/witherstormmod/textures/particle/command_block_3.png") == null
                    || archive.getEntry("assets/witherstormmod/textures/particle/tractor_beam.png") == null
                    || archive.getEntry("assets/witherstormmod/textures/gui/title/background/panorama_0.png") == null
                    || archive.getEntry("assets/witherstormmod/lang/en_us.json") == null
                    || archive.getEntry("data/witherstormmod/structures/bowels_podium.nbt") == null) {
                throw new IOException("The external archive is not witherstormmod 4.2.1: " + file.getAbsolutePath());
            }
            Manifest manifest;
            try (InputStream manifestStream = archive.getInputStream(manifestEntry)) {
                manifest = new Manifest(manifestStream);
            }
            String version = manifest.getMainAttributes().getValue("Implementation-Version");
            if (!"1.20.1-4.2.1".equals(version)) {
                throw new IOException("Expected upstream version 1.20.1-4.2.1 but found " + version);
            }

            List<String> names = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && isSafeEntryName(entry.getName())) {
                    names.add(entry.getName());
                }
            }
            return Collections.unmodifiableList(names);
        } catch (ZipException exception) {
            throw new IOException("Required external resource pack is not a valid ZIP/JAR: "
                    + file.getAbsolutePath(), exception);
        }
    }

    private static boolean isSafeSelector(String value) {
        return value != null && !value.startsWith("/") && value.indexOf('\\') < 0
                && !value.equals("..") && !value.startsWith("../") && !value.contains("/../");
    }

    private static boolean isSafeEntryName(String value) {
        return isSafeSelector(value) && !value.endsWith("/..");
    }
}
