package com.wdcftgg.witherstormmod.common.resources.taint;

import net.minecraftforge.fml.common.registry.EntityEntry;

import java.util.Objects;


public final class MobConversion {
    private final EntityEntry from;
    private final EntityEntry to;
    private final boolean canBeConvertedFromWitherSickness;

    public MobConversion(EntityEntry from, EntityEntry to,
                         boolean canBeConvertedFromWitherSickness) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.canBeConvertedFromWitherSickness = canBeConvertedFromWitherSickness;
    }

    public EntityEntry from() {
        return from;
    }

    public EntityEntry to() {
        return to;
    }

    public boolean canBeConvertedFromWitherSickness() {
        return canBeConvertedFromWitherSickness;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof MobConversion)) return false;
        MobConversion other = (MobConversion) object;
        return canBeConvertedFromWitherSickness == other.canBeConvertedFromWitherSickness
                && from.equals(other.from) && to.equals(other.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, canBeConvertedFromWitherSickness);
    }

    @Override
    public String toString() {
        return "MobConversion[from=" + from + ", to=" + to
                + ", canBeConvertedFromWitherSickness="
                + canBeConvertedFromWitherSickness + "]";
    }
}
