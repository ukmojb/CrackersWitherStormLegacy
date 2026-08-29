package com.wdcftgg.witherstormmod.api.common.data;

import com.google.gson.JsonObject;
import com.wdcftgg.witherstormmod.common.resources.taint.MobConversion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;





public abstract class MobConversionProvider {

    private final String modid;
    private final Map<String, JsonObject> conversions = new LinkedHashMap<String, JsonObject>();

    public MobConversionProvider(String modid) {
        this.modid = modid;
    }

    protected abstract void addConversions();

    protected void add(MobConversion conversion) {
        ResourceLocation from = conversion.from().getRegistryName();
        ResourceLocation to = conversion.to().getRegistryName();
        if (from == null || to == null) {
            throw new IllegalArgumentException("Mob conversion contains an unregistered entity type");
        }
        add(from.toString(), to.toString(), conversion.canBeConvertedFromWitherSickness());
    }

    protected void add(EntityEntry from, EntityEntry to, boolean convertFromWitherSickness) {
        add(new MobConversion(from, to, convertFromWitherSickness));
    }

    protected void add(EntityEntry from, EntityEntry to) {
        add(from, to, true);
    }

    protected void add(String fromId, String toId, boolean convertFromSickness) {
        if (conversions.containsKey(fromId)) {
            throw new IllegalArgumentException("Type '" + fromId + "' is already mapped");
        }
        JsonObject conversion = new JsonObject();
        conversion.addProperty("convert_from_sickness", convertFromSickness);
        conversion.addProperty("from", fromId);
        conversion.addProperty("to", toId);
        conversions.put(fromId, conversion);
    }

    protected void add(String fromId, String toId) {
        add(fromId, toId, true);
    }

    public void run(File outputDirectory) throws IOException {
        conversions.clear();
        addConversions();
        File target = new File(outputDirectory, "data/" + modid + "/tainting/entity");
        Files.createDirectories(target.toPath());
        for (JsonObject conversion : conversions.values()) {
            String from = conversion.get("from").getAsString();
            String to = conversion.get("to").getAsString();
            String fromPath = from.contains(":") ? from.substring(from.indexOf(':') + 1) : from;
            String toPath = to.contains(":") ? to.substring(to.indexOf(':') + 1) : to;
            File out = new File(target, fromPath + "_to_" + toPath + ".json");
            Files.createDirectories(out.toPath().getParent());
            Files.write(out.toPath(),
                    conversion.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    public String getName() {
        return "Mob conversions";
    }
}
