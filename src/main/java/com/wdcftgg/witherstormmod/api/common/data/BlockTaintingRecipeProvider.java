package com.wdcftgg.witherstormmod.api.common.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wdcftgg.witherstormmod.common.resources.taint.SingleBlockTaintRecipe;
import com.wdcftgg.witherstormmod.common.resources.taint.TagBasedTaintRecipe;
import com.wdcftgg.witherstormmod.common.resources.taint.TaintRecipe;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;





public abstract class BlockTaintingRecipeProvider {

    private final String modid;
    private final List<JsonObject> recipes = new ArrayList<JsonObject>();

    public BlockTaintingRecipeProvider(String modid) {
        this.modid = modid;
    }

    protected abstract void addRecipes();

    protected void add(TaintRecipe recipe) {
        JsonObject object = new JsonObject();
        recipe.serializeFrom(object);
        if (recipe.effect() != null) {
            ResourceLocation effectName = Potion.REGISTRY.getNameForObject(recipe.effect());
            if (effectName == null) {
                throw new IllegalArgumentException("Potion is not registered: " + recipe.effect());
            }
            object.addProperty("potion_effect", effectName.toString());
        }
        object.add("replacement", serializeState(recipe.replacement()));
        if (!recipe.propertiesToCopy().isEmpty()) {
            JsonArray copy = new JsonArray();
            for (IProperty<?> property : recipe.propertiesToCopy()) copy.add(property.getName());
            object.add("properties_to_copy", copy);
        }
        recipes.add(object);
    }

    protected void add(Block from, @Nullable Potion effect, IBlockState to,
                       IProperty<?>... propertiesToCopy) {
        add(new SingleBlockTaintRecipe(from, effect, to,
                Arrays.<IProperty<?>>asList(propertiesToCopy)));
    }

    protected void add(Block from, @Nullable Potion effect, IBlockState to) {
        add(from, effect, to, new IProperty<?>[0]);
    }

    protected void addAndCopyAllProperties(Block from, @Nullable Potion effect, Block to) {
        IBlockState state = to.getDefaultState();
        add(from, effect, state,
                state.getPropertyKeys().toArray(new IProperty<?>[state.getPropertyKeys().size()]));
    }

    protected void addAndCopyAllProperties(Block from, Block to) {
        addAndCopyAllProperties(from, null, to);
    }

    protected void add(Block from, IBlockState to, IProperty<?>... propertiesToCopy) {
        add(from, null, to, propertiesToCopy);
    }

    protected void add(Block from, IBlockState to) {
        add(from, null, to);
    }

    protected void add(ResourceLocation fromTag, @Nullable Potion effect, IBlockState to,
                       IProperty<?>... propertiesToCopy) {
        add(new TagBasedTaintRecipe(fromTag, effect, to,
                Arrays.<IProperty<?>>asList(propertiesToCopy)));
    }

    protected void add(ResourceLocation fromTag, @Nullable Potion effect, IBlockState to) {
        add(fromTag, effect, to, new IProperty<?>[0]);
    }

    protected void addAndCopyAllProperties(ResourceLocation fromTag, @Nullable Potion effect,
                                           Block to) {
        IBlockState state = to.getDefaultState();
        add(fromTag, effect, state,
                state.getPropertyKeys().toArray(new IProperty<?>[state.getPropertyKeys().size()]));
    }

    protected void add(ResourceLocation fromTag, IBlockState to,
                       IProperty<?>... propertiesToCopy) {
        add(fromTag, null, to, propertiesToCopy);
    }

    protected void add(ResourceLocation fromTag, IBlockState to) {
        add(fromTag, null, to);
    }

    protected void addAndCopyAllProperties(ResourceLocation fromTag, Block to) {
        addAndCopyAllProperties(fromTag, null, to);
    }

    protected void add(String blockOrTag, String replacementName,
                       Map<String, String> replacementProperties, String... propertiesToCopy) {
        add(blockOrTag, null, replacementName, replacementProperties, propertiesToCopy);
    }

    protected void add(String blockOrTag, @Nullable String potionEffect,
                       String replacementName, Map<String, String> replacementProperties,
                       String... propertiesToCopy) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("block", blockOrTag);
        if (potionEffect != null && !potionEffect.isEmpty()) {
            recipe.addProperty("potion_effect", potionEffect);
        }
        JsonObject replacement = new JsonObject();
        replacement.addProperty("Name", replacementName);
        if (replacementProperties != null && !replacementProperties.isEmpty()) {
            JsonObject properties = new JsonObject();
            for (Map.Entry<String, String> entry : replacementProperties.entrySet()) {
                properties.addProperty(entry.getKey(), entry.getValue());
            }
            replacement.add("Properties", properties);
        }
        recipe.add("replacement", replacement);
        if (propertiesToCopy != null && propertiesToCopy.length > 0) {
            JsonArray copy = new JsonArray();
            for (String property : propertiesToCopy) copy.add(property);
            recipe.add("properties_to_copy", copy);
        }
        recipes.add(recipe);
    }

    protected void add(String blockOrTag, String replacementName) {
        add(blockOrTag, null, replacementName, null);
    }

    protected void add(String blockOrTag, String potionEffect, String replacementName) {
        add(blockOrTag, potionEffect, replacementName, null);
    }

    protected void addAndCopyAllProperties(String blockOrTag, String replacementName) {
        addAndCopyAllProperties(blockOrTag, null, replacementName);
    }

    protected void addAndCopyAllProperties(String blockOrTag, @Nullable String potionEffect,
                                           String replacementName) {
        Block replacement = Block.getBlockFromName(replacementName);
        if (replacement == null) {
            add(blockOrTag, potionEffect, replacementName, null);
            return;
        }
        List<String> propertyNames = new ArrayList<String>();
        for (IProperty<?> property : replacement.getDefaultState().getPropertyKeys()) {
            propertyNames.add(property.getName());
        }
        add(blockOrTag, potionEffect, replacementName, null,
                propertyNames.toArray(new String[propertyNames.size()]));
    }

    public void run(File outputDirectory) throws IOException {
        recipes.clear();
        addRecipes();
        File target = new File(outputDirectory, "data/" + modid + "/tainting/block");
        Files.createDirectories(target.toPath());
        Map<String, Integer> usedNames = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < recipes.size(); index++) {
            JsonObject recipe = recipes.get(index);
            String source = recipe.get("block").getAsString();
            if (source.startsWith("#")) source = source.substring(1);
            String path = source.contains(":") ? source.substring(source.indexOf(':') + 1) : source;
            Integer count = usedNames.get(path);
            if (count == null) {
                usedNames.put(path, 1);
            } else {
                usedNames.put(path, count + 1);
                path = path + "_" + (count + 1);
            }
            File out = new File(target, path + "_tainting.json");
            Files.createDirectories(out.toPath().getParent());
            Files.write(out.toPath(),
                    recipe.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    public String getName() {
        return "Block tainting recipes";
    }

    private static JsonObject serializeState(IBlockState state) {
        ResourceLocation blockName = Block.REGISTRY.getNameForObject(state.getBlock());
        if (blockName == null) {
            throw new IllegalArgumentException("Block is not registered: " + state.getBlock());
        }
        JsonObject replacement = new JsonObject();
        replacement.addProperty("Name", blockName.toString());
        if (!state.getProperties().isEmpty()) {
            JsonObject properties = new JsonObject();
            for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
                properties.addProperty(entry.getKey().getName(), propertyValueName(entry));
            }
            replacement.add("Properties", properties);
        }
        return replacement;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValueName(Map.Entry<IProperty<?>, Comparable<?>> entry) {
        IProperty property = entry.getKey();
        return property.getName(entry.getValue());
    }
}
