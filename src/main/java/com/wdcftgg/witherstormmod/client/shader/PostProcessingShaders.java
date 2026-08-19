package com.wdcftgg.witherstormmod.client.shader;

import com.google.gson.JsonSyntaxException;
import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.client.OptifineCompat;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.common.entity.FormidibombSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.renderer.GlStateManager;

import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.io.IOException;

/** Formidibomb 引信期间使用上游外部资源包提供的色差后处理。 */
public final class PostProcessingShaders implements IResourceManagerReloadListener {

    public static final PostProcessingShaders INSTANCE = new PostProcessingShaders();
    private static final ResourceLocation ABERRATION =
            new ResourceLocation(Tags.MOD_ID, "shaders/post/aberration.json");

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private ShaderGroup aberrationEffect;
    private FormidibombSource source;
    private int framebufferWidth = -1;
    private int framebufferHeight = -1;
    private boolean initialized;
    private IResourceManager pendingResourceManager;
    private boolean reloadPending;

    private PostProcessingShaders() {
    }

    public void initialize() {
        if (initialized) return;
        initialized = true;
        IResourceManager resourceManager = minecraft.getResourceManager();
        if (resourceManager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) resourceManager).registerReloadListener(this);
        } else {
            onResourceManagerReload(resourceManager);
        }
    }

    public void setSource(@Nullable FormidibombSource source) {
        this.source = source;
    }

    public boolean shouldRenderChromaticAberration() {
        return WitherStormClientConfig.chromaticAberration
                && source != null && source.isFormidibombAlive() && source.getStartFuse() > 0;
    }

    public void render(float partialTicks) {
        // OptiFine 着色器激活时其自己的后处理链独占主 framebuffer，1.12 ShaderGroup
        // 会与之争抢并破坏画面，因此着色器激活期间跳过本模组色差后处理。
        if (OptifineCompat.areShadersActive() || !shouldRenderChromaticAberration()) return;
        loadPendingEffect();
        if (aberrationEffect == null) return;
        resizeIfNeeded();
        int startFuse = source.getStartFuse();
        float progress = MathHelper.clamp(
                (startFuse - source.getFuseLife()) / (float) startFuse, 0.0F, 1.0F);
        float multiplier = progress * 0.1F;
        for (Shader shader : aberrationEffect.listShaders) {
            shader.getShaderManager().getShaderUniformOrDefault("Multiplier").set(multiplier);
        }
        aberrationEffect.render(partialTicks);
        minecraft.getFramebuffer().bindFramebuffer(false);
        // 1.12 ShaderGroup.render 结束后把光照、alpha test 与混合留在关闭状态，
        // 也不重置深度写入。显式恢复成 RenderWorldLastEvent 的进入状态，避免
        // 引信结束后下一帧继续携带后处理状态。
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        deleteEffect();
        pendingResourceManager = resourceManager;
        reloadPending = true;
    }

    public void shutdown() {
        source = null;
        deleteEffect();
        pendingResourceManager = null;
        reloadPending = false;
    }

    private void loadPendingEffect() {
        if (!reloadPending || pendingResourceManager == null
                || ShaderLinkHelper.getStaticShaderLinkHelper() == null
                || minecraft.getFramebuffer() == null) return;
        IResourceManager resourceManager = pendingResourceManager;
        reloadPending = false;
        reload(resourceManager);
    }

    private void reload(IResourceManager resourceManager) {
        deleteEffect();
        try {
            aberrationEffect = new ShaderGroup(minecraft.getTextureManager(), resourceManager,
                    minecraft.getFramebuffer(), ABERRATION);
            resizeIfNeeded();
        } catch (JsonSyntaxException | IOException exception) {
            WitherStormMod.LOGGER.warn("Failed to load external Formidibomb shader {}", ABERRATION,
                    exception);
            deleteEffect();
        }
    }

    private void resizeIfNeeded() {
        if (aberrationEffect == null) return;
        int width = minecraft.displayWidth;
        int height = minecraft.displayHeight;
        if (framebufferWidth == width && framebufferHeight == height) return;
        aberrationEffect.createBindFramebuffers(width, height);
        framebufferWidth = width;
        framebufferHeight = height;
    }

    private void deleteEffect() {
        if (aberrationEffect != null) aberrationEffect.deleteShaderGroup();
        aberrationEffect = null;
        framebufferWidth = -1;
        framebufferHeight = -1;
    }
}
