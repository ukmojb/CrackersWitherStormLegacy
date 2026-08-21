package com.wdcftgg.witherstormmod.common.init;

import net.minecraft.entity.item.EntityPainting;
import net.minecraftforge.common.util.EnumHelper;

/**
 * 用 Forge 的 EnumHelper 扩展 1.12.2 的画作枚举，补上游 16x32 的 AMULET 画作。
 * 原先用 sun.misc.Unsafe 写 final 枚举字段：JDK 24+ 已把这些字段访问 API 标记为待移除，
 * 而反射 {@link java.lang.reflect.Field#set} 与变量句柄都拒绝写 final 字段，故改用生态标准的
 * EnumHelper（内部走 ReflectionFactory），与本模组既有的 addCreatureAttribute/addToolMaterial 用法一致。
 */
public final class ModPaintings {

    public static EntityPainting.EnumArt AMULET;

    private ModPaintings() {
    }

    public static void register() {
        if (AMULET != null) return;
        // EnumArt 构造器：EnumArt(String title, int sizeX, int sizeY, int offsetX, int offsetY)。
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
