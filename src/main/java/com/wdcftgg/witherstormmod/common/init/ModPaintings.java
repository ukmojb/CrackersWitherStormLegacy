package com.wdcftgg.witherstormmod.common.init;

import net.minecraft.entity.item.EntityPainting;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

/**
 * 1.12.2 的画作枚举是 final 枚举，无法用 Constructor.newInstance 反射创建；
 * 这里用 Unsafe.allocateInstance 构造 AMULET 画作（16x32，对应上游
 * PaintingVariant(16, 32)），再写入 $VALUES 并清空 Class 的枚举缓存。
 */
@SuppressWarnings("removal")
public final class ModPaintings {

    public static EntityPainting.EnumArt AMULET;

    private ModPaintings() {
    }

    public static void register() {
        if (AMULET != null) return;
        try {
            Unsafe unsafe = unsafe();
            Field valuesField = EntityPainting.EnumArt.class.getDeclaredField("$VALUES");
            Object valuesBase = unsafe.staticFieldBase(valuesField);
            long valuesOffset = unsafe.staticFieldOffset(valuesField);
            valuesField.setAccessible(true);
            EntityPainting.EnumArt[] current =
                    (EntityPainting.EnumArt[]) valuesField.get(null);

            EntityPainting.EnumArt amulet = (EntityPainting.EnumArt)
                    unsafe.allocateInstance(EntityPainting.EnumArt.class);
            putObject(unsafe, amulet, Enum.class, "AMULET", "name");
            putInt(unsafe, amulet, Enum.class, current.length, "ordinal");
            putObject(unsafe, amulet, EntityPainting.EnumArt.class, "Amulet",
                    "title", "field_75702_A");
            putInt(unsafe, amulet, EntityPainting.EnumArt.class, 16,
                    "sizeX", "field_75703_B");
            putInt(unsafe, amulet, EntityPainting.EnumArt.class, 32,
                    "sizeY", "field_75704_C");
            putInt(unsafe, amulet, EntityPainting.EnumArt.class, 0,
                    "offsetX", "field_75699_D");
            putInt(unsafe, amulet, EntityPainting.EnumArt.class, 0,
                    "offsetY", "field_75700_E");
            AMULET = amulet;

            EntityPainting.EnumArt[] extended = Arrays.copyOf(current, current.length + 1);
            extended[current.length] = amulet;
            unsafe.putObject(valuesBase, valuesOffset, extended);

            clearEnumCache(unsafe, EntityPainting.EnumArt.class);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to register the Wither Storm Amulet painting", exception);
        }
    }

    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static void putObject(Unsafe unsafe, Object target, Class<?> owner,
                                  Object value, String... fieldNames) throws Exception {
        Field field = findField(owner, fieldNames);
        unsafe.putObject(target, unsafe.objectFieldOffset(field), value);
    }

    private static void putInt(Unsafe unsafe, Object target, Class<?> owner,
                               int value, String... fieldNames) throws Exception {
        Field field = findField(owner, fieldNames);
        unsafe.putInt(target, unsafe.objectFieldOffset(field), value);
    }

    static Field findField(Class<?> owner, String... fieldNames) throws NoSuchFieldException {
        for (String fieldName : fieldNames) {
            try {
                return owner.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                // Try the next MCP/SRG name.
            }
        }
        throw new NoSuchFieldException(
                "Unable to find " + Arrays.toString(fieldNames) + " in " + owner.getName());
    }

    private static void clearEnumCache(Unsafe unsafe, Class<?> enumClass) throws Exception {
        clearStatic(unsafe, Class.class, enumClass, "enumConstants");
        clearStatic(unsafe, Class.class, enumClass, "enumConstantDirectory");
    }

    @SuppressWarnings("unchecked")
    private static void clearStatic(Unsafe unsafe, Class<?> fieldOwner, Object target,
                                    String fieldName) throws Exception {
        Field field = fieldOwner.getDeclaredField(fieldName);
        Object value = unsafe.getObject(target, unsafe.objectFieldOffset(field));
        if (value instanceof Map) {
            ((Map<?, ?>) value).clear();
        }
        unsafe.putObject(target, unsafe.objectFieldOffset(field), null);
    }
}