package com.wdcftgg.witherstormmod.client.model.witherstorm;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.BufferBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModelBuilders {
    private ModelBuilders() { }


    public static void addBox(ModelRenderer renderer, float textureU, float textureV,
                              float x, float y, float z,
                              float width, float height, float depth,
                              float inflate, float textureScaleU, float textureScaleV,
                              boolean mirror) {
        float originalTextureWidth = renderer.textureWidth;
        float originalTextureHeight = renderer.textureHeight;
        renderer.textureWidth = originalTextureWidth * textureScaleU;
        renderer.textureHeight = originalTextureHeight * textureScaleV;
        renderer.cubeList.add(new FloatModelBox(renderer, textureU, textureV,
                x, y, z, width, height, depth, inflate, mirror));
        renderer.textureWidth = originalTextureWidth;
        renderer.textureHeight = originalTextureHeight;
    }


    public interface PreciseModelBoxBounds {
        float minimumX();
        float minimumY();
        float minimumZ();
        float maximumX();
        float maximumY();
        float maximumZ();
    }

    public static final class CubeDeformation {
        public static final CubeDeformation f_171458_ = new CubeDeformation(0.0F);
        final float inflate;
        public CubeDeformation(float inflate) { this.inflate = inflate; }
        public CubeDeformation m_171469_(float amount) { return new CubeDeformation(inflate + amount); }
    }

    public static final class PartPose {
        public static final PartPose f_171404_ = new PartPose(0, 0, 0, 0, 0, 0);
        final float x, y, z, xRot, yRot, zRot;
        private PartPose(float x, float y, float z, float xRot, float yRot, float zRot) {
            this.x = x; this.y = y; this.z = z; this.xRot = xRot; this.yRot = yRot; this.zRot = zRot;
        }
        public static PartPose m_171419_(float x, float y, float z) { return new PartPose(x, y, z, 0, 0, 0); }
        public static PartPose m_171423_(float x, float y, float z, float xRot, float yRot, float zRot) {
            return new PartPose(x, y, z, xRot, yRot, zRot);
        }
    }

    private static final class Cube {
        final int u, v;
        final float x, y, z, width, height, depth, inflate, textureScaleU, textureScaleV;
        final boolean mirror;
        Cube(int u, int v, float x, float y, float z, float width, float height, float depth, float inflate,
             float textureScaleU, float textureScaleV, boolean mirror) {
            this.u = u; this.v = v; this.x = x; this.y = y; this.z = z;
            this.width = width; this.height = height; this.depth = depth; this.inflate = inflate;
            this.textureScaleU = textureScaleU;
            this.textureScaleV = textureScaleV;
            this.mirror = mirror;
        }
    }

    public static final class CubeListBuilder {
        private final List<Cube> cubes = new ArrayList<Cube>();
        private int textureU;
        private int textureV;
        private boolean mirror;
        public static CubeListBuilder m_171558_() { return new CubeListBuilder(); }
        public CubeListBuilder m_171514_(int u, int v) { textureU = u; textureV = v; return this; }
        public CubeListBuilder m_171480_() { return m_171555_(true); }
        public CubeListBuilder m_171555_(boolean mirror) { this.mirror = mirror; return this; }
        public CubeListBuilder m_171488_(float x, float y, float z, float width, float height, float depth, CubeDeformation deformation) {
            cubes.add(new Cube(textureU, textureV, x, y, z, width, height, depth, deformation.inflate, 1.0F, 1.0F, mirror));
            return this;
        }
        public CubeListBuilder m_171506_(float x, float y, float z, float width, float height, float depth, boolean mirror) {
            cubes.add(new Cube(textureU, textureV, x, y, z, width, height, depth, 0.0F, 1.0F, 1.0F, mirror));
            return this;
        }
        public CubeListBuilder m_171496_(float x, float y, float z, float width, float height, float depth,
                                         CubeDeformation deformation, float textureScaleU, float textureScaleV) {
            cubes.add(new Cube(textureU, textureV, x, y, z, width, height, depth, deformation.inflate,
                    textureScaleU, textureScaleV, mirror));
            return this;
        }
    }

    public static final class PartDefinition {
        private final ModelBase model;
        private final ModelRenderer renderer;
        private final Map<String, PartDefinition> children = new LinkedHashMap<String, PartDefinition>();

        public PartDefinition(ModelBase model, ModelRenderer renderer) {
            this.model = model;
            this.renderer = renderer;
        }

        public PartDefinition m_171599_(String name, CubeListBuilder builder, PartPose pose) {
            ModelRenderer child = new ModelRenderer(model);
            child.setRotationPoint(pose.x, pose.y, pose.z);
            child.rotateAngleX = pose.xRot;
            child.rotateAngleY = pose.yRot;
            child.rotateAngleZ = pose.zRot;
            for (Cube cube : builder.cubes) {
                child.setTextureOffset(cube.u, cube.v);
                child.textureWidth = model.textureWidth * cube.textureScaleU;
                child.textureHeight = model.textureHeight * cube.textureScaleV;
                if (isWhole(cube.width) && isWhole(cube.height) && isWhole(cube.depth)) {
                    child.cubeList.add(new ModelBox(child, cube.u, cube.v, cube.x, cube.y, cube.z,
                            Math.round(cube.width), Math.round(cube.height), Math.round(cube.depth),
                            cube.inflate, cube.mirror));
                } else {
                    child.cubeList.add(new FloatModelBox(child, cube.u, cube.v,
                            cube.x, cube.y, cube.z, cube.width, cube.height, cube.depth,
                            cube.inflate, cube.mirror));
                }
            }
            child.textureWidth = model.textureWidth;
            child.textureHeight = model.textureHeight;
            renderer.addChild(child);
            PartDefinition definition = new PartDefinition(model, child);
            children.put(name, definition);
            return definition;
        }

        public PartDefinition child(String name) { return children.get(name); }
        public PartDefinition m_171597_(String name) { return children.get(name); }
        public Map<String, PartDefinition> children() { return children; }
        public ModelRenderer renderer() { return renderer; }
    }

    private static boolean isWhole(float value) {
        return value == Math.round(value);
    }


    private static final class FloatModelBox extends ModelBox implements PreciseModelBoxBounds {
        private final TexturedQuad[] floatQuads;
        private final float preciseMinimumX;
        private final float preciseMinimumY;
        private final float preciseMinimumZ;
        private final float preciseMaximumX;
        private final float preciseMaximumY;
        private final float preciseMaximumZ;

        FloatModelBox(ModelRenderer renderer, float textureU, float textureV,
                      float x, float y, float z, float width, float height, float depth,
                      float inflate, boolean mirror) {
            super(renderer, Math.round(textureU), Math.round(textureV), x, y, z,
                    Math.round(width), Math.round(height), Math.round(depth), inflate, mirror);

            preciseMinimumX = x;
            preciseMinimumY = y;
            preciseMinimumZ = z;
            preciseMaximumX = x + width;
            preciseMaximumY = y + height;
            preciseMaximumZ = z + depth;
            float maximumX = x + width;
            float maximumY = y + height;
            float maximumZ = z + depth;
            x -= inflate;
            y -= inflate;
            z -= inflate;
            maximumX += inflate;
            maximumY += inflate;
            maximumZ += inflate;
            if (mirror) {
                float swap = maximumX;
                maximumX = x;
                x = swap;
            }

            PositionTextureVertex p0 = vertex(x, y, z);
            PositionTextureVertex p1 = vertex(maximumX, y, z);
            PositionTextureVertex p2 = vertex(maximumX, maximumY, z);
            PositionTextureVertex p3 = vertex(x, maximumY, z);
            PositionTextureVertex p4 = vertex(x, y, maximumZ);
            PositionTextureVertex p5 = vertex(maximumX, y, maximumZ);
            PositionTextureVertex p6 = vertex(maximumX, maximumY, maximumZ);
            PositionTextureVertex p7 = vertex(x, maximumY, maximumZ);
            float textureWidth = renderer.textureWidth;
            float textureHeight = renderer.textureHeight;

            floatQuads = new TexturedQuad[]{
                    quad(new PositionTextureVertex[]{p5, p1, p2, p6},
                            textureU + depth + width, textureV + depth,
                            textureU + depth + width + depth, textureV + depth + height,
                            textureWidth, textureHeight),
                    quad(new PositionTextureVertex[]{p0, p4, p7, p3},
                            textureU, textureV + depth,
                            textureU + depth, textureV + depth + height,
                            textureWidth, textureHeight),
                    quad(new PositionTextureVertex[]{p5, p4, p0, p1},
                            textureU + depth, textureV,
                            textureU + depth + width, textureV + depth,
                            textureWidth, textureHeight),
                    quad(new PositionTextureVertex[]{p2, p3, p7, p6},
                            textureU + depth + width, textureV + depth,
                            textureU + depth + width + width, textureV,
                            textureWidth, textureHeight),
                    quad(new PositionTextureVertex[]{p1, p0, p3, p2},
                            textureU + depth, textureV + depth,
                            textureU + depth + width, textureV + depth + height,
                            textureWidth, textureHeight),
                    quad(new PositionTextureVertex[]{p4, p5, p6, p7},
                            textureU + depth + width + depth, textureV + depth,
                            textureU + depth + width + depth + width, textureV + depth + height,
                            textureWidth, textureHeight)
            };
            if (mirror) {
                for (TexturedQuad quad : floatQuads) quad.flipFace();
            }
        }

        private static PositionTextureVertex vertex(float x, float y, float z) {
            return new PositionTextureVertex(x, y, z, 0.0F, 0.0F);
        }

        private static TexturedQuad quad(PositionTextureVertex[] vertices,
                                         float minimumU, float minimumV,
                                         float maximumU, float maximumV,
                                         float textureWidth, float textureHeight) {
            vertices[0] = vertices[0].setTexturePosition(maximumU / textureWidth,
                    minimumV / textureHeight);
            vertices[1] = vertices[1].setTexturePosition(minimumU / textureWidth,
                    minimumV / textureHeight);
            vertices[2] = vertices[2].setTexturePosition(minimumU / textureWidth,
                    maximumV / textureHeight);
            vertices[3] = vertices[3].setTexturePosition(maximumU / textureWidth,
                    maximumV / textureHeight);
            return new TexturedQuad(vertices);
        }

        @Override
        public void render(BufferBuilder renderer, float scale) {
            for (TexturedQuad quad : floatQuads) quad.draw(renderer, scale);
        }

        @Override public float minimumX() { return preciseMinimumX; }
        @Override public float minimumY() { return preciseMinimumY; }
        @Override public float minimumZ() { return preciseMinimumZ; }
        @Override public float maximumX() { return preciseMaximumX; }
        @Override public float maximumY() { return preciseMaximumY; }
        @Override public float maximumZ() { return preciseMaximumZ; }
    }
}
