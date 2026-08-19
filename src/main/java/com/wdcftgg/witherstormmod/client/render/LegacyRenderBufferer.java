package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.client.OptifineCompat;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class LegacyRenderBufferer implements IResourceManagerReloadListener {
    public static final LegacyRenderBufferer INSTANCE = new LegacyRenderBufferer();
    private static final ExecutorService BUFFER_BUILDERS = new ThreadPoolExecutor(
            0, 3, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
            new BufferBuilderThreadFactory());

    private final Set<ManagedVertexBuffer> liveBuffers = Collections.newSetFromMap(
            new IdentityHashMap<ManagedVertexBuffer, Boolean>());
    private final List<Runnable> invalidators = new ArrayList<Runnable>();
    private World currentWorld;
    private boolean initialized;
    private boolean configuredEnabled;
    private boolean configuredAsync;
    private long renderFrame;

    private LegacyRenderBufferer() {
    }

    public void initialize() {
        if (initialized) return;
        initialized = true;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) minecraft.getResourceManager()).registerReloadListener(this);
        }
        currentWorld = minecraft.world;
        configuredEnabled = shouldUse();
        configuredAsync = WitherStormClientConfig.asyncBufferBuilders;
    }

    public void tick(World world) {
        boolean enabled = shouldUse();
        boolean async = WitherStormClientConfig.asyncBufferBuilders;
        if (world != currentWorld || enabled != configuredEnabled || async != configuredAsync) {
            invalidateAll();
            currentWorld = world;
            configuredEnabled = enabled;
            configuredAsync = async;
        }
    }

    public boolean shouldUse() {
        return WitherStormClientConfig.vertexBufferRendering
                && OpenGlHelper.vboSupported && !OptifineCompat.areShadersActive();
    }

    public boolean shouldBuildAsynchronously() {
        return shouldUse() && WitherStormClientConfig.asyncBufferBuilders;
    }

    /** 对应上游 RenderBufferer.getTotalInstances：返回当前仍受管的 GPU 缓冲实例数。 */
    public synchronized int getTotalInstances() {
        return liveBuffers.size();
    }

    public void beginRenderFrame() {
        renderFrame++;
        List<ManagedVertexBuffer> stale = new ArrayList<ManagedVertexBuffer>();
        synchronized (this) {
            for (ManagedVertexBuffer buffer : liveBuffers) {
                if (buffer.lastUsedFrame < renderFrame - 1L) stale.add(buffer);
            }
        }
        for (ManagedVertexBuffer buffer : stale) buffer.close();
    }

    public <T> Future<T> submit(Callable<T> task) {
        return BUFFER_BUILDERS.submit(task);
    }

    public synchronized void registerInvalidator(Runnable invalidator) {
        if (!invalidators.contains(invalidator)) invalidators.add(invalidator);
    }

    public ManagedVertexBuffer upload(CpuBuffer cpuBuffer) {
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            throw new IllegalStateException("VBO upload must run on the Minecraft render thread");
        }
        VertexBuffer vertexBuffer = new VertexBuffer(cpuBuffer.getFormat());
        vertexBuffer.bufferData(cpuBuffer.getData());
        ManagedVertexBuffer managed = new ManagedVertexBuffer(
                vertexBuffer, cpuBuffer.getVertexCount(), cpuBuffer.getDrawMode(),
                cpuBuffer.getFormat());
        synchronized (this) {
            liveBuffers.add(managed);
        }
        return managed;
    }

    public static CpuBuffer finish(BufferBuilder builder) {
        builder.finishDrawing();
        ByteBuffer data = builder.getByteBuffer().duplicate();
        data.position(0);
        return new CpuBuffer(data, builder.getVertexCount(), builder.getDrawMode(),
                builder.getVertexFormat());
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        invalidateAll();
    }

    public void invalidateAll() {
        List<ManagedVertexBuffer> buffers;
        List<Runnable> callbacks;
        synchronized (this) {
            buffers = new ArrayList<ManagedVertexBuffer>(liveBuffers);
            callbacks = new ArrayList<Runnable>(invalidators);
        }
        for (ManagedVertexBuffer buffer : buffers) buffer.close();
        for (Runnable callback : callbacks) callback.run();
    }

    /** 对应上游 GameRenderer.close：退出客户端时释放 GPU 缓冲并关闭异步构建池。 */
    public void shutdown() {
        invalidateAll();
        BUFFER_BUILDERS.shutdown();
    }

    private synchronized void unregister(ManagedVertexBuffer buffer) {
        liveBuffers.remove(buffer);
    }

    public static final class CpuBuffer {
        private final ByteBuffer data;
        private final int vertexCount;
        private final int drawMode;
        private final VertexFormat format;

        private CpuBuffer(ByteBuffer data, int vertexCount, int drawMode, VertexFormat format) {
            this.data = data;
            this.vertexCount = vertexCount;
            this.drawMode = drawMode;
            this.format = format;
        }

        private ByteBuffer getData() {
            data.position(0);
            return data;
        }

        private int getVertexCount() {
            return vertexCount;
        }

        private int getDrawMode() {
            return drawMode;
        }

        private VertexFormat getFormat() {
            return format;
        }
    }

    public final class ManagedVertexBuffer implements AutoCloseable {
        private final VertexBuffer buffer;
        private final int vertexCount;
        private final int drawMode;
        private final int vertexStride;
        private final int textureOffset;
        private final int colorOffset;
        private final int normalOffset;
        private boolean closed;
        private long lastUsedFrame;

        private ManagedVertexBuffer(VertexBuffer buffer, int vertexCount, int drawMode,
                                    VertexFormat format) {
            this.buffer = buffer;
            this.vertexCount = vertexCount;
            this.drawMode = drawMode;
            this.vertexStride = format.getSize();
            this.textureOffset = format.getUvOffsetById(0);
            this.colorOffset = format.getColorOffset();
            this.normalOffset = format.getNormalOffset();
            this.lastUsedFrame = renderFrame;
        }

        public void draw(float red, float green, float blue, float alpha,
                         boolean useVertexColors) {
            if (closed || vertexCount <= 0) return;
            lastUsedFrame = renderFrame;
            buffer.bindBuffer();
            try {
                GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GlStateManager.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                GL11.glVertexPointer(3, GL11.GL_FLOAT, vertexStride, 0L);
                GL11.glTexCoordPointer(2, GL11.GL_FLOAT, vertexStride, textureOffset);
                GL11.glNormalPointer(GL11.GL_BYTE, vertexStride, normalOffset);
                if (useVertexColors) {
                    GlStateManager.glEnableClientState(GL11.GL_COLOR_ARRAY);
                    GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, vertexStride, colorOffset);
                } else {
                    GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
                    GlStateManager.color(red, green, blue, alpha);
                }
                buffer.drawArrays(drawMode);
            } finally {
                GlStateManager.glDisableClientState(GL11.GL_NORMAL_ARRAY);
                GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
                GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                buffer.unbindBuffer();
                GlStateManager.resetColor();
            }
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            buffer.deleteGlBuffers();
            unregister(this);
        }
    }

    private static final class BufferBuilderThreadFactory implements ThreadFactory {
        private static final AtomicInteger NEXT_ID = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable,
                    "WitherStorm-BufferBuilder-" + NEXT_ID.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
