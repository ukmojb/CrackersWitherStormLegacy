package com.wdcftgg.witherstormmod.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;













public final class OptifineCompat {
    private static boolean discovered;
    private static boolean loaded;
    private static Method isShadersMethod;
    private static Field shaderPackLoadedField;

    private OptifineCompat() {
    }

    private static synchronized void discover() {
        if (discovered) return;
        discovered = true;
        try {
            Class.forName("optifine.Config");
            loaded = true;
            try {
                isShadersMethod = Class.forName("optifine.Config").getMethod("isShaders");
                return;
            } catch (ReflectiveOperationException ignored) {

            }
            try {
                shaderPackLoadedField = Class.forName("net.optifine.shaders.Shaders")
                        .getField("shaderPackLoaded");
            } catch (ReflectiveOperationException ignored) {
                shaderPackLoadedField = null;
            }
        } catch (ClassNotFoundException | LinkageError ignored) {
            loaded = false;
        }
    }


    public static boolean isLoaded() {
        discover();
        return loaded;
    }


    public static boolean areShadersActive() {
        if (!isLoaded()) return false;
        try {
            if (isShadersMethod != null) {
                return Boolean.TRUE.equals(isShadersMethod.invoke(null));
            }
            return shaderPackLoadedField != null && shaderPackLoadedField.getBoolean(null);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
