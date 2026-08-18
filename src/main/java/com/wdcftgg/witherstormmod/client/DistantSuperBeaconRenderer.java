package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.client.render.DistantProjection;
import com.wdcftgg.witherstormmod.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(
        modid = Tags.MOD_ID,
        value = Side.CLIENT)
public final class DistantSuperBeaconRenderer {
    private static final Map<BlockPos, State> STATES = new HashMap<BlockPos, State>();
    private static World stateWorld;

    private DistantSuperBeaconRenderer() {
    }

    public static void update(ModNetwork.DistantSuperBeaconMessage message) {
        switchWorld(Minecraft.getMinecraft().world);
        if (message.isRemoved()) STATES.remove(message.getPosition());
        else STATES.put(message.getPosition(), new State(message));
    }

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null || minecraft.player == null) return;
        switchWorld(minecraft.world);
        if (STATES.isEmpty()) return;
        long now = minecraft.world.getTotalWorldTime();
        boolean extendedProjection = WitherStormClientConfig.distantRenderer;
        if (extendedProjection) DistantProjection.push();
        try {
            for (Map.Entry<BlockPos, State> entry : STATES.entrySet()) {
                State state = entry.getValue();
                BlockPos position = entry.getKey();
                if (minecraft.world.isBlockLoaded(position, false)) {
                    TileEntity tile = minecraft.world.getTileEntity(position);
                    if (tile != null) continue;
                }
                if (!state.active || state.beamHeight <= 0) continue;
                double x = position.getX() - minecraft.getRenderManager().viewerPosX;
                double y = position.getY() - minecraft.getRenderManager().viewerPosY;
                double z = position.getZ() - minecraft.getRenderManager().viewerPosZ;
                double cameraDx = position.getX() - minecraft.getRenderManager().viewerPosX;
                double cameraDy = position.getY() - minecraft.getRenderManager().viewerPosY;
                double cameraDz = position.getZ() - minecraft.getRenderManager().viewerPosZ;
                float cameraDistance = (float) (Math.sqrt(cameraDx * cameraDx + cameraDy * cameraDy
                        + cameraDz * cameraDz) - minecraft.gameSettings.renderDistanceChunks * 16.0F);
                float widthMultiplier = Math.max(1.0F, cameraDistance * 0.01F);
                float beamPartialTicks = cameraDistance > 0.0F ? 0.0F : event.getPartialTicks();
                long beamTime = cameraDistance > 0.0F ? 0L : now;
                float[] color = {state.red / 255.0F, state.green / 255.0F,
                        state.blue / 255.0F};
                minecraft.getTextureManager().bindTexture(TileEntityBeaconRenderer.TEXTURE_BEACON_BEAM);
                GlStateManager.pushAttrib();
                try {
                    GlStateManager.disableFog();
                    TileEntityBeaconRenderer.renderBeamSegment(x, y + 0.5D, z, beamPartialTicks,
                            1.0D, beamTime, 0, state.beamHeight, color,
                            state.thickness * widthMultiplier,
                            state.outerThickness * widthMultiplier);
                } finally {
                    GlStateManager.popAttrib();
                }
            }
        } finally {
            if (extendedProjection) DistantProjection.pop();
        }
    }

    private static void switchWorld(World world) {
        if (stateWorld == world) return;
        STATES.clear();
        stateWorld = world;
    }

    private static final class State {
        private final int red;
        private final int green;
        private final int blue;
        private final boolean active;
        private final int beamHeight;
        private final float thickness;
        private final float outerThickness;

        private State(ModNetwork.DistantSuperBeaconMessage message) {
            int[] color = message.getColor();
            red = color[0];
            green = color[1];
            blue = color[2];
            active = message.isActive();
            beamHeight = message.getBeamHeight();
            thickness = message.getThickness();
            outerThickness = message.getOuterThickness();
        }
    }
}
