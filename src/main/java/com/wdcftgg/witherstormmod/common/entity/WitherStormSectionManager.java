package com.wdcftgg.witherstormmod.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;


final class WitherStormSectionManager {
    private final WitherStormEntity storm;
    private final BowelsEntranceSection bowelsEntranceSection;

    WitherStormSectionManager(WitherStormEntity storm) {
        this.storm = storm;
        this.bowelsEntranceSection = new BowelsEntranceSection();
    }

    void tick() {
        int phase = storm.getPhase();
        if (phase <= 4) {
            bowelsEntranceSection.deactivate();
            return;
        }

        float bodyXRotation = storm.getBodyXRotation(1.0F);
        for (AxisAlignedBB bounds : getBodySectionBounds(phase, bodyXRotation)) pushEntities(bounds);
        bowelsEntranceSection.tick(phase, bodyXRotation);
    }

    AxisAlignedBB[] getBodySectionBounds() {
        int phase = storm.getPhase();
        if (phase <= 4) return new AxisAlignedBB[0];
        return getBodySectionBounds(phase, storm.getBodyXRotation(1.0F));
    }

    AxisAlignedBB getBowelsEntranceBounds() {
        int phase = storm.getPhase();
        return bowelsEntranceSection.getBounds(phase, storm.getBodyXRotation(1.0F));
    }

    private AxisAlignedBB[] getBodySectionBounds(int phase, float bodyXRotation) {
        if (bodyXRotation != 0.0F) {
            return new AxisAlignedBB[] {createFallingSection(phase, bodyXRotation)};
        }
        return createSideSections(phase, bodyXRotation);
    }

    private AxisAlignedBB[] createSideSections(int phase, float bodyXRotation) {
        float height;
        float width;
        double horizontalOffset;
        if (phase >= 7) {
            height = 95.0F;
            width = 45.0F;
            horizontalOffset = 28.0D;
        } else if (phase == 6 || storm.getConsumedMass() >= storm.getConsumptionAmountForPhase(5)) {
            height = 60.0F;
            width = 35.0F;
            horizontalOffset = 24.0D;
        } else {
            height = 30.0F;
            width = 15.0F;
            horizontalOffset = 13.0D;
        }
        return new AxisAlignedBB[] {
                createSection(height, width, horizontalOffset, 28.0D, 0.0D, bodyXRotation, false),
                createSection(height, width, -horizontalOffset, 28.0D, 0.0D, bodyXRotation, false)
        };
    }

    private AxisAlignedBB createFallingSection(int phase, float bodyXRotation) {
        if (phase >= 7) {
            return createSection(60.0F, 85.0F, 0.0D, 80.0D, 30.0D,
                    bodyXRotation, true);
        }
        if (phase == 6 || storm.getConsumedMass() >= storm.getConsumptionAmountForPhase(5)) {
            return createSection(50.0F, 70.0F, 0.0D, 60.0D, 30.0D,
                    bodyXRotation, true);
        }
        return createSection(30.0F, 35.0F, 0.0D, 35.0D, 15.0D,
                bodyXRotation, true);
    }

    private AxisAlignedBB createSection(float height, float width, double offsetX, double offsetY,
                                        double offsetZ, float bodyXRotation, boolean fallingSection) {
        if (fallingSection) {
            double rotationFactor = Math.sin(Math.toRadians(bodyXRotation));
            offsetX *= rotationFactor;
            offsetZ *= rotationFactor;
        }

        float bodyYaw = storm.renderYawOffset * 0.017453292F;
        float bodyYawRight = (storm.renderYawOffset + 90.0F) * 0.017453292F;
        float bodyPitch = (bodyXRotation + 90.0F) * 0.017453292F;
        double lateralX = MathHelper.cos(bodyYaw) * offsetX;
        double lateralZ = MathHelper.sin(bodyYaw) * offsetX;
        float offsetAngle = (float) Math.atan2(offsetZ, offsetY);
        double radius = Math.sqrt(offsetY * offsetY + offsetZ * offsetZ);
        double rawX = MathHelper.cos(bodyPitch + offsetAngle) * MathHelper.cos(bodyYawRight);
        double rawY = MathHelper.sin(bodyPitch + offsetAngle);
        double rawZ = MathHelper.cos(bodyPitch + offsetAngle) * MathHelper.sin(bodyYawRight);
        Vec3d center = new Vec3d(storm.posX + lateralX + rawX * radius,
                storm.posY + rawY * radius, storm.posZ + lateralZ + rawZ * radius);
        double halfWidth = width * 0.5D;
        return new AxisAlignedBB(center.x - halfWidth, center.y, center.z - halfWidth,
                center.x + halfWidth, center.y + height, center.z + halfWidth);
    }

    private void pushEntities(AxisAlignedBB bounds) {
        List<Entity> entities = storm.world.getEntitiesWithinAABBExcludingEntity(storm, bounds);
        for (Entity entity : entities) {
            if (entity.isDead || entity.noClip || storm.noClip || !entity.canBePushed()
                    || !EntitySelectors.NOT_SPECTATING.apply(entity)
                    || !EntitySelectors.getTeamCollisionPredicate(storm).apply(entity)
                    || storm.isRidingOrBeingRiddenBy(entity)) continue;
            double deltaX = entity.posX - (bounds.minX + bounds.maxX) * 0.5D;
            double deltaZ = entity.posZ - (bounds.minZ + bounds.maxZ) * 0.5D;
            double horizontalDistance = MathHelper.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (horizontalDistance < 0.01D) continue;
            deltaX /= horizontalDistance;
            deltaZ /= horizontalDistance;
            double strength = Math.min(1.0D, 1.0D / horizontalDistance) * 0.05D;
            if (!entity.isRiding()) {
                entity.addVelocity(deltaX * strength, 0.0D, deltaZ * strength);
            }
        }
    }


    private final class BowelsEntranceSection {
        private AxisAlignedBB bounds;

        void tick(int phase, float bodyXRotation) {
            bounds = createBounds(phase, bodyXRotation);
            if (bounds == null) return;


            pushEntities(bounds);
            for (Entity entity : storm.world.getEntitiesWithinAABB(Entity.class, bounds)) {
                storm.handleBowelsEntranceCollision(entity);
            }
        }

        AxisAlignedBB getBounds(int phase, float bodyXRotation) {
            bounds = createBounds(phase, bodyXRotation);
            return bounds;
        }

        void deactivate() {
            bounds = null;
        }

        private AxisAlignedBB createBounds(int phase, float bodyXRotation) {
            if (phase < 7 || !storm.isBeingTornApart()) return null;
            return createSection(16.0F, 16.0F,
                    -3.0D, 34.0D, -24.0D, bodyXRotation, false);
        }
    }
}
