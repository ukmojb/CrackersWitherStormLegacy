package com.wdcftgg.witherstormmod.common.init;

import net.minecraft.entity.item.EntityPainting;
import net.minecraftforge.common.util.EnumHelper;







public final class ModPaintings {

    public static EntityPainting.EnumArt AMULET;

    private ModPaintings() {
    }

    public static void register() {
        if (AMULET != null) return;

        AMULET = EnumHelper.addEnum(
                EntityPainting.EnumArt.class,
                "AMULET",
                new Class[]{String.class, int.class, int.class, int.class, int.class},
                "Amulet", 16, 32, 0, 0);
        if (AMULET == null) {
            throw new IllegalStateException("Unable to register the Wither Storm Amulet painting");
        }
    }
}
