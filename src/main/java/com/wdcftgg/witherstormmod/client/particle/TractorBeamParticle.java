package com.wdcftgg.witherstormmod.client.particle;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.TractorBeamProvider;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.util.TractorBeamHelper;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;


@SideOnly(Side.CLIENT)
public final class TractorBeamParticle extends Particle {
    private static final ResourceLocation SPRITE_LOCATION =
            new ResourceLocation(Tags.MOD_ID, "particle/tractor_beam");

    private final int sourceEntityId;
    private final int head;

    private TractorBeamParticle(World world, double x, double y, double z,
                                double motionX, double motionY, double motionZ,
                                int sourceEntityId, int head, TextureAtlasSprite sprite) {
        super(world, x, y, z);
        this.sourceEntityId = sourceEntityId;
        this.head = head;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.particleScale = 0.1F;
        this.particleMaxAge = 20 + this.rand.nextInt(30);
        this.particleGravity = 0.0F;
        this.canCollide = false;
        this.setParticleTexture(sprite);
    }


    public static void registerSprite(TextureMap textureMap) {
        textureMap.registerSprite(SPRITE_LOCATION);
    }


    public static void spawnForProvider(Entity source, TractorBeamProvider provider) {
        if (!WitherStormClientConfig.tractorBeamParticles || source == null || provider == null
                || source.world == null || !source.world.isRemote || source.isDead
                || provider.isDeadOrPlayingDead() || !shouldRenderForBodyPose(source)) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        boolean aprilFools = WitherStormMod.isAprilFools() && WitherStormClientConfig.aprilFools;
        TextureAtlasSprite sprite = aprilFools ? null : resolveSprite(minecraft);
        if (!aprilFools && sprite == null) return;

        Random random = source.world.rand;
        double verticalOffset = source instanceof SupplementalEntities.WitherStormHeadEntity
                ? 1.5D : 5.5D;
        for (int head = 0; head < provider.getTotalHeads(); head++) {
            if (!provider.tractorBeamActive(head)) continue;
            Vec3d headPosition = provider.getHeadPositionForBeam(head);
            Vec3d direction = provider.getHeadDirectionForBeam(head);
            if (headPosition == null || direction == null || direction.lengthSquared() <= 0.0001D) continue;

            for (int amount = 0; amount < 5; amount++) {
                double distance = random.nextFloat() * 200.0D;
                Vec3d position = headPosition.add(direction.scale(distance)).add(0.0D, verticalOffset, 0.0D);
                double distanceAllowed = position.distanceTo(headPosition) * 2.0D * 0.02D;
                position = position.add(random.nextGaussian() * distanceAllowed,
                        random.nextGaussian() * distanceAllowed,
                        random.nextGaussian() * distanceAllowed);
                int containingHead = findContainingHead(provider, position);
                if (containingHead < 0) continue;

                Vec3d delta = position.subtract(provider.getHeadPositionForBeam(containingHead));
                if (delta.lengthSquared() <= 0.0001D) continue;
                delta = delta.normalize().scale(-0.8D);
                if (aprilFools) {
                    source.world.spawnParticle(EnumParticleTypes.HEART,
                            position.x, position.y, position.z, delta.x, delta.y, delta.z);
                } else {
                    minecraft.effectRenderer.addEffect(new TractorBeamParticle(source.world,
                            position.x, position.y, position.z, delta.x, delta.y, delta.z,
                            source.getEntityId(), containingHead, sprite));
                }
            }

            if (getPhase(source) < 4) continue;
            spawnBeamImpactParticles(source, provider, head, headPosition, direction, random);
        }
    }

    private static void spawnBeamImpactParticles(Entity source, TractorBeamProvider provider, int head,
                                                 Vec3d headPosition, Vec3d direction, Random random) {
        double cutoff = provider.getTractorBeamCutoffDistance(head);
        double distance = cutoff < 0.0D ? 200.0D : cutoff + 30.0D;
        for (int index = 0; index < 10; index++) {
            float spread = 8.0F;
            float yawOffset = (random.nextFloat() * spread - spread * 0.5F) * 0.017453292F;
            float pitchOffset = (random.nextFloat() * spread - spread * 0.5F) * 0.017453292F;
            Vec3d variedDirection = direction.rotatePitch(pitchOffset).rotateYaw(yawOffset).normalize();
            Vec3d end = headPosition.add(variedDirection.scale(distance)).add(0.0D, 5.5D, 0.0D);
            RayTraceResult hit = source.world.rayTraceBlocks(
                    headPosition, end, false, true, false);
            if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) continue;
            BlockPos hitPosition = hit.getBlockPos();
            if (!WorldUtil.isBlockExposed(source.world, hitPosition)) continue;
            IBlockState state = source.world.getBlockState(hitPosition);
            Vec3d delta = new Vec3d(hitPosition).add(0.5D, 0.5D, 0.5D)
                    .subtract(headPosition).normalize();
            source.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                    hitPosition.getX() + 0.5D, hitPosition.getY() + 1.0D,
                    hitPosition.getZ() + 0.5D,
                    -delta.x, -delta.y, -delta.z, Block.getStateId(state));
        }
    }

    private static int findContainingHead(TractorBeamProvider provider, Vec3d position) {
        for (int head = 0; head < provider.getTotalHeads(); head++) {
            if (!provider.tractorBeamActive(head)) continue;
            Vec3d origin = provider.getHeadPositionForBeam(head);
            Vec3d direction = provider.getHeadDirectionForBeam(head);
            if (origin != null && direction != null
                    && TractorBeamHelper.isInsideTractorBeam(position, origin, direction,
                    provider.getTractorBeamCutoffDistance(head), 4.0D)) {
                return head;
            }
        }
        return -1;
    }

    private static boolean shouldRenderForBodyPose(Entity source) {
        if (source instanceof WitherStormEntity) {
            return ((WitherStormEntity) source).getBodyXRotation(1.0F) == 0.0F;
        }
        if (source instanceof SupplementalEntities.WitherStormSegmentEntity) {
            return ((SupplementalEntities.WitherStormSegmentEntity) source).getBodyXRotation(1.0F) == 0.0F;
        }
        return true;
    }

    private static int getPhase(Entity source) {
        if (source instanceof WitherStormEntity) return ((WitherStormEntity) source).getPhase();
        if (source instanceof SupplementalEntities.WitherStormSegmentEntity) {
            return ((SupplementalEntities.WitherStormSegmentEntity) source).getPhase();
        }
        return -1;
    }

    private static TextureAtlasSprite resolveSprite(Minecraft minecraft) {
        if (minecraft == null || minecraft.effectRenderer == null) return null;
        TextureAtlasSprite sprite = minecraft.getTextureMapBlocks().getAtlasSprite(SPRITE_LOCATION.toString());
        return sprite == null || "missingno".equals(sprite.getIconName()) ? null : sprite;
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public boolean shouldDisableDepth() {
        return false;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        Entity source = world.getEntityByID(sourceEntityId);
        if (!(source instanceof TractorBeamProvider)
                || !((TractorBeamProvider) source).tractorBeamActive(head)
                || !TractorBeamHelper.isInsideTractorBeam(new Vec3d(posX, posY, posZ),
                ((TractorBeamProvider) source).getHeadPositionForBeam(head),
                ((TractorBeamProvider) source).getHeadDirectionForBeam(head),
                ((TractorBeamProvider) source).getTractorBeamCutoffDistance(head), 4.0D)) {
            setExpired();
        }
    }
}
