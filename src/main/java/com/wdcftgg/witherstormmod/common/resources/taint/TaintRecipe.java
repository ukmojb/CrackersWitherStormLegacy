package com.wdcftgg.witherstormmod.common.resources.taint;

import com.google.gson.JsonObject;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;

import javax.annotation.Nullable;
import java.util.List;


public abstract class TaintRecipe implements Comparable<TaintRecipe> {
    @Nullable
    protected final Potion effect;
    protected final IBlockState replacement;
    protected final List<IProperty<?>> propertiesToCopy;

    public TaintRecipe(@Nullable Potion effect, IBlockState replacement,
                       List<IProperty<?>> propertiesToCopy) {
        this.effect = effect;
        this.replacement = replacement;
        this.propertiesToCopy = propertiesToCopy;
    }

    @Nullable
    public Potion effect() {
        return effect;
    }

    public IBlockState replacement() {
        return replacement;
    }

    public List<IProperty<?>> propertiesToCopy() {
        return propertiesToCopy;
    }

    public abstract boolean canConvertBlock(IBlockState state);

    public abstract void serializeFrom(JsonObject object);

    public abstract String getName();

    public boolean canConvertWithPotion(PotionType potion) {
        if (effect == null || potion == null) return false;
        for (PotionEffect potionEffect : potion.getEffects()) {
            if (potionEffect.getPotion() == effect) return true;
        }
        return false;
    }
}
