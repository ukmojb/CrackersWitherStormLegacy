package com.wdcftgg.witherstormmod.common.util;

import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class EvolutionProfiler {
    private final Map<Integer, Integer> ticksToEvolve = new LinkedHashMap<Integer, Integer>();
    private final List<Integer> consumedEntitiesPerSeconds = new ArrayList<Integer>();
    private double consumedEntitiesPerSecond;
    private int ticksSinceLastPhase;
    private int lastConsumedEntities;
    private boolean profiling;

    public void tick(WitherStormEntity storm) {
        ++ticksSinceLastPhase;
        MinecraftServer server = storm.world.getMinecraftServer();
        if (server == null) return;

        if (ticksToEvolve.containsKey(7)) {
            for (Map.Entry<Integer, Integer> entry : ticksToEvolve.entrySet()) {
                sendToAll(server, TextFormatting.GOLD + entry.getValue().toString()
                        + " ticks to evolve from " + (entry.getKey() - 1)
                        + " to " + entry.getKey());
            }
            profiling = false;
        }

        if (ticksSinceLastPhase % 20 != 0) return;
        consumedEntitiesPerSeconds.add(storm.getConsumedMass() - lastConsumedEntities);
        lastConsumedEntities = storm.getConsumedMass();
        int sum = 0;
        for (Integer amount : consumedEntitiesPerSeconds) sum += amount;
        consumedEntitiesPerSecond = sum / (double) consumedEntitiesPerSeconds.size();
        if (consumedEntitiesPerSeconds.size() > 60) {
            consumedEntitiesPerSeconds.clear();
            sendToAll(server, TextFormatting.YELLOW + "Consumed entities per second for phase "
                    + storm.getPhase() + ": " + consumedEntitiesPerSecond);
        }
    }

    public void onEvolve(WitherStormEntity storm) {
        int phase = storm.getPhase();
        ticksToEvolve.put(phase, ticksSinceLastPhase);
        MinecraftServer server = storm.world.getMinecraftServer();
        if (server != null) {
            sendToAll(server, TextFormatting.GOLD + "Phase " + (phase - 1) + " to "
                    + phase + " took " + ticksSinceLastPhase + " ticks");
        }
        ticksSinceLastPhase = 0;
    }

    public void begin() {
        profiling = true;
        ticksToEvolve.clear();
        ticksSinceLastPhase = 0;
        consumedEntitiesPerSeconds.clear();
        consumedEntitiesPerSecond = 0.0D;
    }

    public boolean isProfiling() {
        return profiling;
    }

    public double getConsumedEntitiesPerSecond() {
        return consumedEntitiesPerSecond;
    }

    public void writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("IsProfiling", profiling);
        compound.setInteger("TicksSinceLastPhase", ticksSinceLastPhase);
        NBTTagList entries = new NBTTagList();
        for (Map.Entry<Integer, Integer> entry : ticksToEvolve.entrySet()) {
            NBTTagCompound entryTag = new NBTTagCompound();
            entryTag.setInteger("Phase", entry.getKey());
            entryTag.setInteger("Ticks", entry.getValue());
            entries.appendTag(entryTag);
        }
        compound.setTag("TicksToEvolve", entries);
    }

    public void readFromNBT(NBTTagCompound compound) {
        profiling = compound.getBoolean("IsProfiling");
        ticksSinceLastPhase = Math.max(0, compound.getInteger("TicksSinceLastPhase"));
        ticksToEvolve.clear();
        NBTTagList entries = compound.getTagList("TicksToEvolve", 10);
        for (int index = 0; index < entries.tagCount(); ++index) {
            NBTTagCompound entry = entries.getCompoundTagAt(index);
            int phase = entry.getInteger("Phase");
            if (phase >= 0 && phase <= 7) {
                ticksToEvolve.put(phase, Math.max(0, entry.getInteger("Ticks")));
            }
        }
    }

    private static void sendToAll(MinecraftServer server, String message) {
        TextComponentString component = new TextComponentString(message);
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            player.sendMessage(component);
        }
    }
}
