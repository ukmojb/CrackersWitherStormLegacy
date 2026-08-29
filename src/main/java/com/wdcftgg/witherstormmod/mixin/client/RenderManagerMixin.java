package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(RenderManager.class)
public abstract class RenderManagerMixin {

    @Inject(method = "renderDebugBoundingBox", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableTexture2D()V"))
    private void witherstormmod$renderAdditionalHitboxes(Entity entity, double renderX,
                                                          double renderY, double renderZ,
                                                          float entityYaw, float partialTicks,
                                                          CallbackInfo callback) {
        if (entity instanceof SupplementalEntities.WitherStormSegmentEntity) {
            SupplementalEntities.WitherStormSegmentEntity segment =
                    (SupplementalEntities.WitherStormSegmentEntity) entity;
            double offsetX = renderX - segment.posX;
            double offsetY = renderY - segment.posY;
            double offsetZ = renderZ - segment.posZ;
            for (int head = 0; head < segment.getTotalHeads(); head++) {
                Vec3d headPosition = segment.getHeadPositionForBeam(head, partialTicks);
                AxisAlignedBB bounds = new AxisAlignedBB(
                        headPosition.x - 3.0D, headPosition.y - 3.0D, headPosition.z - 3.0D,
                        headPosition.x + 3.0D, headPosition.y + 3.0D, headPosition.z + 3.0D)
                        .offset(offsetX, offsetY, offsetZ);
                drawBounds(bounds, 1.0F, head == 0 ? 0.6F : 1.0F, 0.0F);
                Vec3d origin = headPosition.add(offsetX, offsetY, offsetZ);
                drawDirection(origin, segment.getHeadDirectionForBeam(head, partialTicks).scale(6.0D));
            }
            return;
        }
        if (!(entity instanceof WitherStormEntity)) return;
        WitherStormEntity storm = (WitherStormEntity) entity;
        double offsetX = renderX - storm.posX;
        double offsetY = renderY - storm.posY;
        double offsetZ = renderZ - storm.posZ;

        for (int head = 0; head < storm.getTotalHeads(); head++) {
            AxisAlignedBB bounds = storm.getHeadBounds(head, partialTicks)
                    .offset(offsetX, offsetY, offsetZ);
            if (head == 0) {
                drawBounds(bounds, 1.0F, 0.6F, 0.0F);
            } else {
                drawBounds(bounds, 1.0F, 1.0F, 0.0F);
            }
            Vec3d headPosition = storm.getHeadPosition(head, partialTicks);
            Vec3d origin = new Vec3d(headPosition.x + offsetX,
                    headPosition.y + offsetY, headPosition.z + offsetZ);
            Vec3d direction = storm.getHeadDirectionForBeam(head, partialTicks)
                    .scale(storm.getPhase() > 3 ? 6.0D : 1.0D);
            drawDirection(origin, direction);
        }

        if (storm.getPhase() <= 4) return;
        for (AxisAlignedBB bounds : storm.getBodySectionBounds()) {
            drawBounds(bounds.offset(offsetX, offsetY, offsetZ), 0.0F, 1.0F, 0.0F);
        }
        AxisAlignedBB entrance = storm.getBowelsEntranceBounds();
        if (entrance != null) {
            drawBounds(entrance.offset(offsetX, offsetY, offsetZ), 0.0F, 1.0F, 1.0F);
        }
    }

    private static void drawBounds(AxisAlignedBB bounds, float red, float green, float blue) {
        RenderGlobal.drawBoundingBox(bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ, red, green, blue, 1.0F);
    }

    private static void drawDirection(Vec3d origin, Vec3d direction) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(origin.x, origin.y, origin.z).color(0, 0, 255, 255).endVertex();
        buffer.pos(origin.x + direction.x, origin.y + direction.y, origin.z + direction.z)
                .color(0, 0, 255, 255).endVertex();
        tessellator.draw();
    }
}
