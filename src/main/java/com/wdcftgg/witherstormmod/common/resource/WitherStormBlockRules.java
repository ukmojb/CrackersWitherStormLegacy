package com.wdcftgg.witherstormmod.common.resource;

import com.wdcftgg.witherstormmod.common.config.ConfiguredListMatcher;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

/** Resolves the configured block overrides before the external upstream blacklist. */
public final class WitherStormBlockRules {
    private static final byte UNKNOWN = 0;
    private static final byte DENIED = 1;
    private static final byte ALLOWED = 2;
    private static final byte[] CACHE = new byte[65536];
    private static String[] cachedWhitelist;
    private static String[] cachedBlacklist;

    private WitherStormBlockRules() {
    }

    public static boolean canConsume(IBlockState state) {
        if (state == null || state.getBlock() == Blocks.AIR) return false;
        refreshCacheIfNeeded();
        int stateId = Block.getStateId(state);
        if (stateId >= 0 && stateId < CACHE.length && CACHE[stateId] != UNKNOWN) {
            return CACHE[stateId] == ALLOWED;
        }

        ResourceLocation registryName = state.getBlock().getRegistryName();
        String value = registryName == null ? null : registryName.toString();
        boolean allowed;
        if (ConfiguredListMatcher.matches(value, WitherStormConfig.consumableBlockBlacklist)) {
            allowed = false;
        } else if (ConfiguredListMatcher.matches(value, WitherStormConfig.consumableBlockWhitelist)) {
            allowed = true;
        } else {
            allowed = !UpstreamBlockTags.contains(UpstreamBlockTags.WITHER_STORM_BLOCK_BLACKLIST, state);
        }
        if (stateId >= 0 && stateId < CACHE.length) CACHE[stateId] = allowed ? ALLOWED : DENIED;
        return allowed;
    }

    private static void refreshCacheIfNeeded() {
        if (cachedWhitelist == WitherStormConfig.consumableBlockWhitelist
                && cachedBlacklist == WitherStormConfig.consumableBlockBlacklist) return;
        java.util.Arrays.fill(CACHE, UNKNOWN);
        cachedWhitelist = WitherStormConfig.consumableBlockWhitelist;
        cachedBlacklist = WitherStormConfig.consumableBlockBlacklist;
    }
}
