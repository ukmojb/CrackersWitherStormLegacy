package com.wdcftgg.witherstormmod.common.init;

import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraftforge.common.util.EnumHelper;


public final class ModCreatureAttributes {
    public static final EnumCreatureAttribute SICKENED = createSickenedAttribute();

    private ModCreatureAttributes() {
    }

    public static void bootstrap() {

    }

    private static EnumCreatureAttribute createSickenedAttribute() {
        EnumCreatureAttribute attribute = EnumHelper.addCreatureAttribute("SICKENED");
        if (attribute == null) {
            throw new IllegalStateException("Unable to register SICKENED creature attribute");
        }
        return attribute;
    }
}
