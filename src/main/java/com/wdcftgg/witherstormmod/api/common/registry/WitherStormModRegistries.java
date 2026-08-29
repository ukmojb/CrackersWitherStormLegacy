package com.wdcftgg.witherstormmod.api.common.registry;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.api.common.ai.symbiont.SpellType;
import com.wdcftgg.witherstormmod.common.entity.SymbiontSpells;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;


@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class WitherStormModRegistries {
    public static final ResourceLocation SPELL_TYPES_NAME =
            new ResourceLocation(Tags.MOD_ID, "symbiont_spells");
    public static Supplier<IForgeRegistry<SpellType>> SPELL_TYPES;

    private static IForgeRegistry<SpellType> spellTypes;

    public WitherStormModRegistries() {
    }

    @SubscribeEvent
    public static synchronized void registerRegistries(@Nonnull RegistryEvent.NewRegistry event) {
        if (spellTypes != null) return;
        final IForgeRegistry<SpellType> registry = new RegistryBuilder<SpellType>()
                .setName(SPELL_TYPES_NAME)
                .setType(SpellType.class)
                .create();
        spellTypes = registry;
        SPELL_TYPES = () -> registry;
    }

    @SubscribeEvent
    public static void registerBuiltInSpellTypes(RegistryEvent.Register<SpellType> event) {
        if (event.getRegistry() != spellTypes) return;
        SymbiontSpells.registerApiTypes();
    }

    public static synchronized SpellType registerSpellType(ResourceLocation id, SpellType type) {
        if (id == null) throw new NullPointerException("id");
        if (type == null) throw new NullPointerException("type");
        IForgeRegistry<SpellType> registry = requireRegistry();
        SpellType existing = registry.getValue(id);
        if (existing != null && existing != type) {
            throw new IllegalArgumentException("Spell type ID is already registered: " + id);
        }
        ResourceLocation existingId = registry.getKey(type);
        if (existingId != null && !existingId.equals(id)) {
            throw new IllegalArgumentException("Spell type is already registered as " + existingId);
        }
        if (existing == type) return type;
        ResourceLocation assignedId = type.getRegistryName();
        if (assignedId != null && !assignedId.equals(id)) {
            throw new IllegalArgumentException("Spell type is already named " + assignedId);
        }
        if (assignedId == null) type.setRegistryName(id);
        registry.register(type);
        return type;
    }

    public static SpellType registerSpellType(String namespace, String path, SpellType type) {
        return registerSpellType(new ResourceLocation(namespace, path), type);
    }

    public static SpellType registerSpellType(SpellType type) {
        if (type == null || type.getRegistryName() == null) {
            throw new IllegalArgumentException(
                    "Spell type must have a registry name before registration");
        }
        return registerSpellType(type.getRegistryName(), type);
    }

    public static synchronized List<SpellType> getSpellTypes() {
        return Collections.unmodifiableList(
                new ArrayList<SpellType>(requireRegistry().getValuesCollection()));
    }

    @Nullable
    public static synchronized SpellType getSpellType(ResourceLocation id) {
        return id == null ? null : requireRegistry().getValue(id);
    }

    @Nullable
    public static SpellType getSpellType(String id) {
        if (id == null || id.isEmpty()) return null;
        try {
            return getSpellType(new ResourceLocation(id));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    public static synchronized ResourceLocation getSpellTypeId(SpellType type) {
        return type == null ? null : requireRegistry().getKey(type);
    }

    private static IForgeRegistry<SpellType> requireRegistry() {
        if (spellTypes == null) {
            throw new IllegalStateException("Symbiont spell registry has not been initialized yet");
        }
        return spellTypes;
    }
}
