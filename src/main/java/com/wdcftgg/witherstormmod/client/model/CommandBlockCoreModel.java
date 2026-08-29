package com.wdcftgg.witherstormmod.client.model;

import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

public class CommandBlockCoreModel extends ModelBase {
    private final RibPart[] ribs = new RibPart[6];

    public CommandBlockCoreModel() {
        textureWidth = 128;
        textureHeight = 128;
        ribs[0] = new RibPart(this, 0.0F, 24.0F, 68.0F, false);
        ribs[1] = new RibPart(this, 0.0F, 24.0F, -68.0F, false);
        ribs[2] = new RibPart(this, 42.0F, 24.0F, -57.0F, false);
        ribs[3] = new RibPart(this, 42.0F, 24.0F, 57.0F, false);
        ribs[4] = new RibPart(this, -42.0F, 24.0F, 57.0F, true);
        ribs[5] = new RibPart(this, -42.0F, 24.0F, -57.0F, true);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                       float netHeadYaw, float headPitch, float scale) {
        if (!(entity instanceof SupplementalEntities.CommandBlockEntity)) return;
        SupplementalEntities.CommandBlockEntity commandBlock =
                (SupplementalEntities.CommandBlockEntity) entity;
        if (commandBlock.getCoreMode() != SupplementalEntities.CommandBlockEntity.CoreMode.RIBS) return;

        float partialTicks = MathHelper.clamp(ageInTicks - entity.ticksExisted, 0.0F, 1.0F);
        for (int index = 0; index < ribs.length; index++) {
            SupplementalEntities.CommandBlockEntity.RibAnimation animation =
                    commandBlock.getRibAnimation(index);
            if (animation == null) continue;
            ribs[index].apply(animation, partialTicks);
            ribs[index].render(scale);
        }
    }

    private static final class RibPart {
        private final ModelRenderer base;
        private final ModelRenderer segmentOne;
        private final ModelRenderer segmentTwo;
        private final ModelRenderer segmentThree;
        private final ModelRenderer segmentFour;

        private RibPart(ModelBase model, float x, float y, float z, boolean mirror) {
            base = new ModelRenderer(model);
            base.setRotationPoint(x, y, z);

            segmentOne = createSegment(model, 0, 0, mirror,
                    -6.5F, -21.0F, -4.0F, 13, 21, 13);
            base.addChild(segmentOne);

            segmentTwo = createSegment(model, 0, 34, mirror,
                    -5.5F, -29.0F, -4.0F, 11, 29, 10);
            segmentTwo.setRotationPoint(0.0F, -21.0F, 0.0F);
            segmentOne.addChild(segmentTwo);

            segmentThree = createSegment(model, 0, 73, mirror,
                    -4.5F, -29.0F, -3.0F, 9, 29, 8);
            segmentThree.setRotationPoint(0.0F, -29.0F, -1.0F);
            segmentTwo.addChild(segmentThree);

            segmentFour = createSegment(model, 52, 0, mirror,
                    -3.5F, -32.0F, -3.0F, 7, 32, 6);
            segmentFour.setRotationPoint(0.0F, -29.0F, 0.0F);
            segmentThree.addChild(segmentFour);
        }

        private static ModelRenderer createSegment(ModelBase model, int textureX, int textureY,
                                                   boolean mirror, float x, float y, float z,
                                                   int width, int height, int depth) {
            ModelRenderer segment = new ModelRenderer(model, textureX, textureY);
            segment.mirror = mirror;
            segment.addBox(x, y, z, width, height, depth);
            return segment;
        }

        private void apply(SupplementalEntities.CommandBlockEntity.RibAnimation animation,
                           float partialTicks) {
            base.rotateAngleX = radians(animation.getBaseXRotation(partialTicks));
            base.rotateAngleY = radians(animation.getBaseYRotation(partialTicks));
            float xRotation = animation.getXRotation(partialTicks);
            float yRotation = animation.getYRotation(partialTicks);
            setRotation(segmentOne, xRotation * 0.4F, yRotation * 0.4F);
            setRotation(segmentTwo, xRotation * 0.4F, yRotation * 0.4F);
            setRotation(segmentThree, xRotation * 0.8F, yRotation * 0.8F);
            setRotation(segmentFour, xRotation, yRotation);
        }

        private static void setRotation(ModelRenderer model, float x, float y) {
            model.rotateAngleX = radians(x);
            model.rotateAngleY = radians(y);
            model.rotateAngleZ = 0.0F;
        }

        private static float radians(float degrees) {
            return degrees * 0.017453292F;
        }

        private void render(float scale) {



            base.render(scale);
        }
    }
}
