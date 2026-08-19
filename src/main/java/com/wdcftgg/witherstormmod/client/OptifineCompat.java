package com.wdcftgg.witherstormmod.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * OptiFine 检测的统一入口，替代散落在雾、天空、VBO 与后处理路径中的重复反射。
 * 语义分两级：
 * <ul>
 *   <li>{@link #isLoaded()}：OptiFine 本体已安装（{@code optifine.Config} 可加载）。用于
 *       本模组不能触碰的 OptiFine 自有渲染路径（如 {@code RenderGlobal.renderSky}）。</li>
 *   <li>{@link #areShadersActive()}：OptiFine 着色器包已激活。用于本模组直接操作
 *       GL 雾状态、投影矩阵或后处理 framebuffer 的路径。</li>
 * </ul>
 * 检测入口与旧实现保持一致：优先 {@code optifine.Config.isShaders()}，回退到
 * {@code net.optifine.shaders.Shaders.shaderPackLoaded} 字段。
 */
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
                // 不同 1.12 OptiFine 构建的着色器状态入口不同，继续尝试备用字段。
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

    /** OptiFine 是否已安装。 */
    public static boolean isLoaded() {
        discover();
        return loaded;
    }

    /** OptiFine 着色器包是否激活；OptiFine 未安装时恒为 false。 */
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
