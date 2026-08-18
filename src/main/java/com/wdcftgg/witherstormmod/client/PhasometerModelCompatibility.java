package com.wdcftgg.witherstormmod.client;

import com.google.common.collect.ImmutableMap;
import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.vector.Vector3f;

import javax.vecmath.Matrix4f;
import java.util.EnumMap;

/** Recreates Forge 1.20's separate-transforms loader with the 1.12 baked-model API. */
final class PhasometerModelCompatibility {
    private static final ResourceLocation GUI_MODEL =
            new ResourceLocation(Tags.MOD_ID, "phasometer_gui");
    private static final ModelResourceLocation ITEM_MODEL =
            new ModelResourceLocation(Tags.MOD_ID + ":phasometer", "inventory");
    private static final ModelResourceLocation BAKED_GUI_MODEL =
            new ModelResourceLocation(GUI_MODEL, "inventory");
    private static final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation>
            UPSTREAM_HAND_TRANSFORMS = createUpstreamHandTransforms();

    private PhasometerModelCompatibility() {
    }

    static void registerModels() {
        Item phasometer = ModItems.get("phasometer");
        if (phasometer != null) ModelBakery.registerItemVariants(phasometer, GUI_MODEL);
    }

    static void bakeModels(ModelBakeEvent event) {
        IBakedModel base = event.getModelRegistry().getObject(ITEM_MODEL);
        IBakedModel gui = event.getModelRegistry().getObject(BAKED_GUI_MODEL);
        IBakedModel missing = event.getModelManager().getMissingModel();
        if (base == null || base == missing || gui == null || gui == missing) {
            WitherStormMod.LOGGER.error(
                    "Unable to attach phasometer perspective models (base={}, gui={})", base, gui);
            return;
        }
        event.getModelRegistry().putObject(ITEM_MODEL, new PerspectiveModel(base, gui));
    }

    private static final class PerspectiveModel extends BakedModelWrapper<IBakedModel> {
        private final IBakedModel guiModel;

        private PerspectiveModel(IBakedModel baseModel, IBakedModel guiModel) {
            super(baseModel);
            this.guiModel = guiModel;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(
                ItemCameraTransforms.TransformType cameraTransformType) {
            if (cameraTransformType == ItemCameraTransforms.TransformType.GUI
                    || cameraTransformType == ItemCameraTransforms.TransformType.GROUND
                    || cameraTransformType == ItemCameraTransforms.TransformType.FIXED) {
                return guiModel.handlePerspective(cameraTransformType);
            }
            TRSRTransformation transform = UPSTREAM_HAND_TRANSFORMS.get(cameraTransformType);
            if (transform != null) {
                return PerspectiveMapWrapper.handlePerspective(originalModel,
                        UPSTREAM_HAND_TRANSFORMS, cameraTransformType);
            }
            return originalModel.handlePerspective(cameraTransformType);
        }
    }

    private static ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation>
    createUpstreamHandTransforms() {
        EnumMap<ItemCameraTransforms.TransformType, TRSRTransformation> transforms =
                new EnumMap<ItemCameraTransforms.TransformType, TRSRTransformation>(
                        ItemCameraTransforms.TransformType.class);
        TRSRTransformation thirdPerson = transform(90.0F, 180.0F, 180.0F,
                0.0F, -1.5F, -7.0F);
        TRSRTransformation firstPerson = transform(-90.0F, 0.0F, 0.0F,
                -1.75F, 1.0F, -5.75F);
        transforms.put(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, thirdPerson);
        transforms.put(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, thirdPerson);
        transforms.put(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, firstPerson);
        transforms.put(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, firstPerson);
        return ImmutableMap.copyOf(transforms);
    }

    private static TRSRTransformation transform(float rotationX, float rotationY, float rotationZ,
                                                  float translationX, float translationY,
                                                  float translationZ) {
        ItemTransformVec3f vanilla =
                new ItemTransformVec3f(
                        new Vector3f(rotationX, rotationY, rotationZ),
                        new Vector3f(translationX / 16.0F, translationY / 16.0F,
                                translationZ / 16.0F),
                        new Vector3f(1.0F, 1.0F, 1.0F));
        return TRSRTransformation.blockCenterToCorner(TRSRTransformation.from(vanilla));
    }
}
