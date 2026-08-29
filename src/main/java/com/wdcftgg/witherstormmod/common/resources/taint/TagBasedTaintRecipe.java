package com.wdcftgg.witherstormmod.common.resources.taint;

import com.google.gson.JsonObject;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;


public class TagBasedTaintRecipe extends TaintRecipe {
    private final ResourceLocation tag;

    public TagBasedTaintRecipe(ResourceLocation tag, @Nullable Potion effect,
                               IBlockState replacement,
                               List<IProperty<?>> propertiesToCopy) {
        super(effect, replacement, propertiesToCopy);
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    @Override
    public boolean canConvertBlock(IBlockState state) {
        return UpstreamBlockTags.contains(tag.toString(), state);
    }

    @Override
    public String getName() {
        return tag.getPath();
    }

    @Override
    public void serializeFrom(JsonObject object) {
        object.addProperty("block", "#" + tag);
    }

    public ResourceLocation getTag() {
        return tag;
    }

    @Override
    public int compareTo(TaintRecipe other) {
        return other instanceof SingleBlockTaintRecipe ? -1 : 0;
    }
}
