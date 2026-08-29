package com.wdcftgg.witherstormmod.common.resources.taint;

import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;


public class SingleBlockTaintRecipe extends TaintRecipe {
    private final Block block;

    public SingleBlockTaintRecipe(Block block, @Nullable Potion effect, IBlockState replacement,
                                  List<IProperty<?>> propertiesToCopy) {
        super(effect, replacement, propertiesToCopy);
        this.block = Objects.requireNonNull(block, "block");
    }

    @Override
    public boolean canConvertBlock(IBlockState state) {
        return state != null && state.getBlock() == block;
    }

    @Override
    public String getName() {
        return registryName().getPath();
    }

    @Override
    public void serializeFrom(JsonObject object) {
        object.addProperty("block", registryName().toString());
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public int compareTo(TaintRecipe other) {
        return other instanceof TagBasedTaintRecipe ? 1 : 0;
    }

    private ResourceLocation registryName() {
        ResourceLocation name = Block.REGISTRY.getNameForObject(block);
        return Objects.requireNonNull(name, "Block is not registered: " + block);
    }
}
