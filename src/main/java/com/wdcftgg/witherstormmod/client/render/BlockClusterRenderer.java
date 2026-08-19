package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BlockClusterRenderer extends Render<SupplementalEntities.BlockClusterEntity> {
    private final Map<SupplementalEntities.BlockClusterEntity, ClusterBufferCache> bufferCaches =
            new WeakHashMap<SupplementalEntities.BlockClusterEntity, ClusterBufferCache>();

    public BlockClusterRenderer(RenderManager renderManager) {
        super(renderManager);
        shadowSize = 0.5F;
        LegacyRenderBufferer.INSTANCE.registerInvalidator(this::clearBufferCaches);
    }

    @Override
    public void doRender(SupplementalEntities.BlockClusterEntity entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        if (entity.getBlocks().isEmpty()) return;
        // Render from the cluster's client-owned interpolation instead of the
        // raw EntityTracker sample supplied by RenderManager. This is the
        // positional counterpart of the storm body's getBodyYRotation path.
        double trackedX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double trackedY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double trackedZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        x += entity.getClientRenderX(partialTicks) - trackedX;
        y += entity.getClientRenderY(partialTicks) - trackedY;
        z += entity.getClientRenderZ(partialTicks) - trackedZ;
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.translate(0.0F, 0.5F, 0.0F);
        float shakeX = entity.getShakeX(partialTicks);
        float shakeZ = entity.getShakeZ(partialTicks);
        GlStateManager.rotate(-entity.getClusterYaw(partialTicks) - shakeX * 50.0F,
                0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(entity.getClusterPitch(partialTicks) - shakeZ * 30.0F,
                1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, -0.5F, 0.0F);
        GlStateManager.translate(getParityOffset(entity.getClusterSizeX()),
                getParityOffset(entity.getClusterSizeY()), getParityOffset(entity.getClusterSizeZ()));

        float fade = entity.getFadeAmount(partialTicks);
        float scale = getFadeScale(fade);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate(shakeX, 0.0F, shakeZ);
        GlStateManager.color(Math.min(1.0F, fade + 0.1F), fade,
                Math.min(1.0F, fade + 0.2F), 1.0F);
        boolean renderedBuffered = LegacyRenderBufferer.INSTANCE.shouldUse()
                && renderBufferedBlocks(entity, dispatcher);
        if (!renderedBuffered) renderImmediateBlocks(entity, dispatcher);
        renderAnimatedBlocks(entity, partialTicks);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        pruneDeadCaches(entity);
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private void renderImmediateBlocks(SupplementalEntities.BlockClusterEntity entity,
                                       BlockRendererDispatcher dispatcher) {
        IBlockAccess blockAccess = new ClusterBlockAccess(entity);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        BlockPos startPosition = entity.getStartPos();
        double translatedX = -startPosition.getX();
        double translatedY = entity.getClusterSizeY() / 2.0D - startPosition.getY();
        double translatedZ = -startPosition.getZ();
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            prepareLayer(layer);
            ForgeHooksClient.setRenderLayer(layer);
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            builder.setTranslation(translatedX, translatedY, translatedZ);
            try {
                for (Map.Entry<BlockPos, IBlockState> entry : entity.getBlocks().entrySet()) {
                    IBlockState state = entry.getValue();
                    if (state.getRenderType() != EnumBlockRenderType.MODEL
                            || !state.getBlock().canRenderInLayer(state, layer)) continue;
                    BlockPos worldPosition = entry.getKey().add(startPosition);
                    dispatcher.renderBlock(state, worldPosition, blockAccess, builder);
                }
            } finally {
                builder.setTranslation(0.0D, 0.0D, 0.0D);
                ForgeHooksClient.setRenderLayer(null);
            }
            tessellator.draw();
        }
        restoreLayerState();
    }

    private void renderAnimatedBlocks(SupplementalEntities.BlockClusterEntity entity,
                                      float partialTicks) {
        for (Map.Entry<BlockPos, IBlockState> entry : entity.getBlocks().entrySet()) {
            IBlockState state = entry.getValue();
            if (state.getRenderType() != EnumBlockRenderType.ENTITYBLOCK_ANIMATED) continue;
            BlockPos offset = entry.getKey();
            GlStateManager.pushMatrix();
            GlStateManager.translate(offset.getX(), offset.getY() + entity.getClusterSizeY() / 2.0F,
                    offset.getZ());
            renderTileEntity(entity, offset, state, partialTicks);
            GlStateManager.popMatrix();
        }
    }

    private boolean renderBufferedBlocks(SupplementalEntities.BlockClusterEntity entity,
                                         BlockRendererDispatcher dispatcher) {
        int revision = entity.getBlockDataRevision();
        ClusterBufferCache cache = bufferCaches.get(entity);
        if (cache == null || cache.revision != revision || cache.hasClosedBuffers()) {
            if (cache != null) cache.close();
            cache = createCache(entity, dispatcher, revision);
            bufferCaches.put(entity, cache);
        }
        if (!cache.prepareBuffers()) return false;

        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            LegacyRenderBufferer.ManagedVertexBuffer buffer = cache.buffers.get(layer);
            if (buffer == null) continue;
            prepareLayer(layer);
            buffer.draw(1.0F, 1.0F, 1.0F, 1.0F, true);
        }
        restoreLayerState();
        return true;
    }

    private ClusterBufferCache createCache(SupplementalEntities.BlockClusterEntity entity,
                                           BlockRendererDispatcher dispatcher,
                                           int revision) {
        BlockSnapshot snapshot = collectSnapshot(entity, dispatcher);
        if (LegacyRenderBufferer.INSTANCE.shouldBuildAsynchronously()) {
            Future<BlockBuildResult> future = LegacyRenderBufferer.INSTANCE.submit(
                    () -> buildBuffers(snapshot));
            return new ClusterBufferCache(revision, future, null);
        }
        try {
            return new ClusterBufferCache(revision, null, buildBuffers(snapshot));
        } catch (RuntimeException exception) {
            WitherStormMod.LOGGER.error("Failed to build cached block cluster geometry", exception);
            return new ClusterBufferCache(revision, null, null, true);
        }
    }

    private static BlockSnapshot collectSnapshot(SupplementalEntities.BlockClusterEntity entity,
                                                 BlockRendererDispatcher dispatcher) {
        EnumMap<BlockRenderLayer, List<QuadSnapshot>> layers =
                new EnumMap<BlockRenderLayer, List<QuadSnapshot>>(BlockRenderLayer.class);
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            layers.put(layer, new ArrayList<QuadSnapshot>());
        }
        IBlockAccess blockAccess = new ClusterBlockAccess(entity);
        for (Map.Entry<BlockPos, IBlockState> entry : entity.getBlocks().entrySet()) {
            IBlockState state = entry.getValue();
            if (state.getRenderType() != EnumBlockRenderType.MODEL) continue;
            IBakedModel model = dispatcher.getModelForState(state);
            BlockPos offset = entry.getKey();
            float translatedX = offset.getX();
            float translatedY = offset.getY() + entity.getClusterSizeY() / 2.0F;
            float translatedZ = offset.getZ();
            BlockPos worldPosition = offset.add(entity.getStartPos());
            long randomSeed = offset.toLong();
            for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                if (!state.getBlock().canRenderInLayer(state, layer)) continue;
                ForgeHooksClient.setRenderLayer(layer);
                try {
                    for (EnumFacing facing : EnumFacing.values()) {
                        if (!state.shouldSideBeRendered(blockAccess, worldPosition, facing)) continue;
                        appendQuads(layers.get(layer), model.getQuads(state, facing, randomSeed),
                                state, blockAccess, worldPosition,
                                translatedX, translatedY, translatedZ);
                    }
                    appendQuads(layers.get(layer), model.getQuads(state, null, randomSeed),
                            state, blockAccess, worldPosition,
                            translatedX, translatedY, translatedZ);
                } finally {
                    ForgeHooksClient.setRenderLayer(null);
                }
            }
        }
        return new BlockSnapshot(layers);
    }

    private static void appendQuads(List<QuadSnapshot> target, List<BakedQuad> quads,
                                    IBlockState state, IBlockAccess blockAccess,
                                    BlockPos worldPosition, float translatedX,
                                    float translatedY, float translatedZ) {
        for (BakedQuad quad : quads) {
            Vec3i normal = quad.getFace().getDirectionVec();
            float red = 1.0F;
            float green = 1.0F;
            float blue = 1.0F;
            if (quad.hasTintIndex()) {
                int tint = Minecraft.getMinecraft().getBlockColors().colorMultiplier(
                        state, blockAccess, worldPosition, quad.getTintIndex());
                if (EntityRenderer.anaglyphEnable) tint = TextureUtil.anaglyphColor(tint);
                red = (tint >> 16 & 255) / 255.0F;
                green = (tint >> 8 & 255) / 255.0F;
                blue = (tint & 255) / 255.0F;
            }
            target.add(new QuadSnapshot(normalizeQuad(quad), translatedX, translatedY,
                    translatedZ, red, green, blue,
                    normal.getX(), normal.getY(), normal.getZ()));
        }
    }

    private static int[] normalizeQuad(BakedQuad quad) {
        int[] data = quad.getVertexData();
        if (DefaultVertexFormats.ITEM.equals(quad.getFormat()) && data.length == 28) {
            return data.clone();
        }
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(DefaultVertexFormats.ITEM);
        quad.pipe(builder);
        return builder.build().getVertexData().clone();
    }

    private static BlockBuildResult buildBuffers(BlockSnapshot snapshot) {
        EnumMap<BlockRenderLayer, LegacyRenderBufferer.CpuBuffer> buffers =
                new EnumMap<BlockRenderLayer, LegacyRenderBufferer.CpuBuffer>(BlockRenderLayer.class);
        for (Map.Entry<BlockRenderLayer, List<QuadSnapshot>> entry : snapshot.layers.entrySet()) {
            List<QuadSnapshot> quads = entry.getValue();
            if (quads.isEmpty()) continue;
            BufferBuilder builder = new BufferBuilder(Math.max(256, quads.size() * 28));
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
            for (QuadSnapshot quad : quads) {
                builder.addVertexData(quad.vertexData);
                builder.putColorRGB_F4(quad.red, quad.green, quad.blue);
                builder.putPosition(quad.translatedX, quad.translatedY, quad.translatedZ);
                builder.putNormal(quad.normalX, quad.normalY, quad.normalZ);
            }
            buffers.put(entry.getKey(), LegacyRenderBufferer.finish(builder));
        }
        return new BlockBuildResult(buffers);
    }

    private static void prepareLayer(BlockRenderLayer layer) {
        GlStateManager.enableAlpha();
        if (layer == BlockRenderLayer.TRANSLUCENT) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(false);
        } else {
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
        }
    }

    private static void restoreLayerState() {
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.resetColor();
    }

    private void pruneDeadCaches(SupplementalEntities.BlockClusterEntity renderedEntity) {
        if (renderedEntity.ticksExisted % 20 != 0) return;
        Iterator<Map.Entry<SupplementalEntities.BlockClusterEntity, ClusterBufferCache>> iterator =
                bufferCaches.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SupplementalEntities.BlockClusterEntity, ClusterBufferCache> entry = iterator.next();
            SupplementalEntities.BlockClusterEntity entity = entry.getKey();
            if (!entity.isDead && entity.world == renderedEntity.world) continue;
            entry.getValue().close();
            iterator.remove();
        }
    }

    private void clearBufferCaches() {
        for (ClusterBufferCache cache : bufferCaches.values()) cache.close();
        bufferCaches.clear();
    }

    private void renderTileEntity(SupplementalEntities.BlockClusterEntity entity, BlockPos offset,
                                  IBlockState state, float partialTicks) {
        NBTTagCompound storedData = entity.getTileDataFromOffset(offset);
        if (storedData == null) return;
        TileEntity tile = state.getBlock().createTileEntity(entity.world, state);
        if (tile == null) return;
        BlockPos renderPosition = new BlockPos(entity.posX + offset.getX(),
                entity.posY + offset.getY() + entity.getClusterSizeY() / 2.0F - 0.5F,
                entity.posZ + offset.getZ());
        NBTTagCompound renderData = storedData.copy();
        renderData.setInteger("x", renderPosition.getX());
        renderData.setInteger("y", renderPosition.getY());
        renderData.setInteger("z", renderPosition.getZ());
        tile.setWorld(entity.world);
        tile.readFromNBT(renderData);
        tile.setPos(renderPosition);
        TileEntityRendererDispatcher.instance.render(tile, 0.0D, 0.0D, 0.0D,
                partialTicks, -1, 1.0F);
    }

    @Override
    public boolean shouldRender(SupplementalEntities.BlockClusterEntity entity, ICamera camera,
                                double cameraX, double cameraY, double cameraZ) {
        return (WitherStormClientConfig.blockClusterRendering || entity.forceRender())
                && super.shouldRender(entity, camera, cameraX, cameraY, cameraZ);
    }

    static float getParityOffset(float size) {
        return -0.5F - (Math.round(size) % 2 == 0 ? 0.5F : 0.0F);
    }

    static float getFadeScale(float fade) {
        return Math.max(0.8F, fade * 0.5F + 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(SupplementalEntities.BlockClusterEntity entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    private static final class BlockSnapshot {
        private final EnumMap<BlockRenderLayer, List<QuadSnapshot>> layers;

        private BlockSnapshot(EnumMap<BlockRenderLayer, List<QuadSnapshot>> layers) {
            this.layers = layers;
        }
    }

    private static final class ClusterBlockAccess implements IBlockAccess {
        private final SupplementalEntities.BlockClusterEntity entity;

        private ClusterBlockAccess(SupplementalEntities.BlockClusterEntity entity) {
            this.entity = entity;
        }

        @Override
        public TileEntity getTileEntity(BlockPos position) {
            return null;
        }

        @Override
        public int getCombinedLight(BlockPos position, int lightValue) {
            // 上游在无着色器时将方块簇作为全亮实体渲染，避免簇内面受世界光照/区块状态影响。
            return 15728880;
        }

        @Override
        public IBlockState getBlockState(BlockPos position) {
            IBlockState state = entity.getBlocks().get(position.subtract(entity.getStartPos()));
            return state == null ? Blocks.AIR.getDefaultState() : state;
        }

        @Override
        public boolean isAirBlock(BlockPos position) {
            return getBlockState(position).getBlock().isAir(getBlockState(position), this, position);
        }

        @Override
        public Biome getBiome(BlockPos position) {
            return entity.world.getBiome(position);
        }

        @Override
        public int getStrongPower(BlockPos position, EnumFacing direction) {
            return getBlockState(position).getStrongPower(this, position, direction);
        }

        @Override
        public WorldType getWorldType() {
            return entity.world.getWorldType();
        }

        @Override
        public boolean isSideSolid(BlockPos position, EnumFacing side, boolean defaultValue) {
            IBlockState state = getBlockState(position);
            return state.isSideSolid(this, position, side);
        }
    }

    private static final class QuadSnapshot {
        private final int[] vertexData;
        private final float translatedX;
        private final float translatedY;
        private final float translatedZ;
        private final float red;
        private final float green;
        private final float blue;
        private final int normalX;
        private final int normalY;
        private final int normalZ;

        private QuadSnapshot(int[] vertexData, float translatedX, float translatedY,
                             float translatedZ, float red, float green, float blue,
                             int normalX, int normalY, int normalZ) {
            this.vertexData = vertexData;
            this.translatedX = translatedX;
            this.translatedY = translatedY;
            this.translatedZ = translatedZ;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
        }
    }

    private static final class BlockBuildResult {
        private final EnumMap<BlockRenderLayer, LegacyRenderBufferer.CpuBuffer> buffers;

        private BlockBuildResult(EnumMap<BlockRenderLayer, LegacyRenderBufferer.CpuBuffer> buffers) {
            this.buffers = buffers;
        }
    }

    private static final class ClusterBufferCache {
        private final int revision;
        private Future<BlockBuildResult> pending;
        private BlockBuildResult built;
        private final EnumMap<BlockRenderLayer, LegacyRenderBufferer.ManagedVertexBuffer> buffers =
                new EnumMap<BlockRenderLayer, LegacyRenderBufferer.ManagedVertexBuffer>(BlockRenderLayer.class);
        private boolean failed;

        private ClusterBufferCache(int revision, Future<BlockBuildResult> pending,
                                   BlockBuildResult built) {
            this(revision, pending, built, false);
        }

        private ClusterBufferCache(int revision, Future<BlockBuildResult> pending,
                                   BlockBuildResult built, boolean failed) {
            this.revision = revision;
            this.pending = pending;
            this.built = built;
            this.failed = failed;
        }

        private boolean prepareBuffers() {
            if (failed) return false;
            if (pending != null) {
                if (!pending.isDone()) return false;
                try {
                    built = pending.get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    failed = true;
                } catch (ExecutionException | CancellationException exception) {
                    WitherStormMod.LOGGER.error("Failed to build cached block cluster geometry",
                            exception instanceof ExecutionException
                                    ? ((ExecutionException) exception).getCause() : exception);
                    failed = true;
                }
                pending = null;
                if (failed) return false;
            }
            if (built != null) {
                for (Map.Entry<BlockRenderLayer, LegacyRenderBufferer.CpuBuffer> entry
                        : built.buffers.entrySet()) {
                    buffers.put(entry.getKey(),
                            LegacyRenderBufferer.INSTANCE.upload(entry.getValue()));
                }
                built = null;
            }
            return !failed;
        }

        private boolean hasClosedBuffers() {
            for (LegacyRenderBufferer.ManagedVertexBuffer buffer : buffers.values()) {
                if (buffer.isClosed()) return true;
            }
            return false;
        }

        private void close() {
            if (pending != null) pending.cancel(false);
            pending = null;
            built = null;
            for (LegacyRenderBufferer.ManagedVertexBuffer buffer : buffers.values()) buffer.close();
            buffers.clear();
        }
    }
}
