package com.wdcftgg.witherstormmod.common.init;

public final class ModRegistryNames {

    private static final String[] ITEMS = {
            "withered_bone", "withered_flesh", "withered_spider_eye", "golden_apple_stew",
            "amulet", "command_block_book", "withered_nether_star", "command_block_sword",
            "command_block_pickaxe", "command_block_axe",
            "command_block_shovel", "command_block_hoe", "wooden_command_block_sword", "wooden_command_block_pickaxe",
            "wooden_command_block_axe", "wooden_command_block_shovel", "wooden_command_block_hoe",
            "stone_command_block_sword", "stone_command_block_pickaxe", "stone_command_block_axe",
            "stone_command_block_shovel", "stone_command_block_hoe", "iron_command_block_sword",
            "iron_command_block_pickaxe", "iron_command_block_axe", "iron_command_block_shovel",
            "iron_command_block_hoe", "gold_command_block_sword", "gold_command_block_pickaxe",
            "gold_command_block_axe", "gold_command_block_shovel", "gold_command_block_hoe",
            "eye_of_the_storm", "formidi_blade", "phasometer"
    };

    private static final String[] BLOCKS = {
            "super_tnt", "formidibomb", "super_beacon", "super_support_beacon", "firework_bundle",
            "tainted_zombie_sitting", "tainted_zombie_wall", "tainted_zombie_lying", "tainted_bone_pile",
            "tainted_skeleton_wall", "tainted_skull_ceiling", "tainted_flesh_veins", "tainted_flesh_block",
            "infected_flesh_block", "hardened_flesh_block", "withered_phlegm_block", "tainted_stone",
            "tainted_stone_stairs", "tainted_stone_slab", "tainted_stone_button", "tainted_stone_pressure_plate",
            "tainted_cobblestone", "tainted_cobblestone_stairs", "tainted_cobblestone_slab",
            "tainted_cobblestone_wall", "tainted_sand", "tainted_dirt", "tainted_sandstone",
            "tainted_sandstone_slab", "tainted_sandstone_stairs", "tainted_sandstone_wall",
            "tainted_cut_sandstone", "tainted_cut_sandstone_slab", "tainted_chiseled_sandstone",
            "tainted_smooth_sandstone", "tainted_smooth_sandstone_slab", "tainted_smooth_sandstone_stairs",
            "tainted_smooth_sandstone_wall", "tainted_glass", "tainted_glass_pane", "tainted_planks",
            "tainted_torch", "tainted_wall_torch", "tainted_sign", "tainted_wall_sign", "tainted_hanging_sign",
            "tainted_wall_hanging_sign", "stripped_tainted_log",
            "stripped_tainted_wood", "tainted_log", "tainted_wood", "tainted_leaves", "tainted_door",
            "tainted_trapdoor", "tainted_button", "tainted_pressure_plate", "tainted_stairs", "tainted_slab",
            "tainted_fence", "tainted_fence_gate", "tainted_mushroom", "potted_tainted_mushroom",
            "tainted_pumpkin", "tainted_carved_pumpkin", "tainted_jack_o_lantern", "tainted_dust",
            "tainted_dust_block"
    };

    private static final String[] ITEMLESS_BLOCKS = {
            "potted_tainted_mushroom", "tainted_wall_sign", "tainted_wall_torch", "tainted_hanging_sign",
            "tainted_wall_hanging_sign"
    };

    private ModRegistryNames() {
    }

    public static String[] itemNames() {
        return ITEMS.clone();
    }

    public static String[] blockNames() {
        return BLOCKS.clone();
    }

    public static boolean isItemlessBlock(String name) {
        for (String itemless : ITEMLESS_BLOCKS) {
            if (itemless.equals(name)) return true;
        }
        return false;
    }
}
