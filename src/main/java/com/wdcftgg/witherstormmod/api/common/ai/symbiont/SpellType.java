package com.wdcftgg.witherstormmod.api.common.ai.symbiont;

import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;




public final class SpellType extends IForgeRegistryEntry.Impl<SpellType> {

    private final BiFunction<SickenedEntities.WitheredSymbiontEntity, SpellType, SymbiontSpell>
            spellFactory;
    private final int spellTime;
    private final Optional<Supplier<SoundEvent>> spellLoop;
    private final boolean doProtection;
    private final double protectionRadius;
    private final double protectionThrowStrength;

    public SpellType(
            BiFunction<SickenedEntities.WitheredSymbiontEntity, SpellType, SymbiontSpell> spellFactory,
            int spellTime, Optional<Supplier<SoundEvent>> spellLoop, boolean doProtection,
            double protectionRadius, double protectionThrowStrength) {
        this.spellFactory = spellFactory;
        this.spellTime = spellTime;
        this.spellLoop = spellLoop;
        this.doProtection = doProtection;
        this.protectionRadius = protectionRadius;
        this.protectionThrowStrength = protectionThrowStrength;
    }

    public SpellType(
            BiFunction<SickenedEntities.WitheredSymbiontEntity, SpellType, SymbiontSpell> spellFactory,
            int spellTime, Optional<Supplier<SoundEvent>> spellLoop, boolean doProtection) {
        this(spellFactory, spellTime, spellLoop, doProtection, 3.0D, 1.0D);
    }

    public SpellType(
            BiFunction<SickenedEntities.WitheredSymbiontEntity, SpellType, SymbiontSpell> spellFactory,
            int spellTime, Optional<Supplier<SoundEvent>> spellLoop) {
        this(spellFactory, spellTime, spellLoop, true, 3.0D, 1.0D);
    }

    public SymbiontSpell makeSpell(SickenedEntities.WitheredSymbiontEntity entity) {
        return spellFactory.apply(entity, this);
    }

    public BiFunction<SickenedEntities.WitheredSymbiontEntity, SpellType, SymbiontSpell>
            spellFactory() {
        return spellFactory;
    }

    public int spellTime() {
        return spellTime;
    }

    public Optional<Supplier<SoundEvent>> spellLoop() {
        return spellLoop;
    }

    public boolean doProtection() {
        return doProtection;
    }

    public double protectionRadius() {
        return protectionRadius;
    }

    public double protectionThrowStrength() {
        return protectionThrowStrength;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        SpellType that = (SpellType) other;
        return spellTime == that.spellTime
                && doProtection == that.doProtection
                && Double.compare(protectionRadius, that.protectionRadius) == 0
                && Double.compare(protectionThrowStrength, that.protectionThrowStrength) == 0
                && Objects.equals(spellFactory, that.spellFactory)
                && Objects.equals(spellLoop, that.spellLoop);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(spellFactory);
        result = 31 * result + Integer.hashCode(spellTime);
        result = 31 * result + Objects.hashCode(spellLoop);
        result = 31 * result + Boolean.hashCode(doProtection);
        result = 31 * result + Double.hashCode(protectionRadius);
        result = 31 * result + Double.hashCode(protectionThrowStrength);
        return result;
    }

    @Override
    public String toString() {
        return "SpellType[spellFactory=" + spellFactory
                + ", spellTime=" + spellTime
                + ", spellLoop=" + spellLoop
                + ", doProtection=" + doProtection
                + ", protectionRadius=" + protectionRadius
                + ", protectionThrowStrength=" + protectionThrowStrength + "]";
    }

}
