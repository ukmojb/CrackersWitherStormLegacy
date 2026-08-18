package com.wdcftgg.witherstormmod.client.particle;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.entity.PowerfulExplosiveEntity;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** 上游 Formidibomb 引信期间的 command_block 动画粒子。 */
@SideOnly(Side.CLIENT)
public final class CommandBlockParticle extends Particle {
    public static final int SUPER_BEACON_RESUMMON_BURST = 0;
    public static final int SUPER_BEACON_ITEM_BURST = 1;
    private static final ResourceLocation[] SPRITE_LOCATIONS = {
            new ResourceLocation(Tags.MOD_ID, "particle/command_block"),
            new ResourceLocation(Tags.MOD_ID, "particle/command_block_1"),
            new ResourceLocation(Tags.MOD_ID, "particle/command_block_2"),
            new ResourceLocation(Tags.MOD_ID, "particle/command_block_3")
    };
    private final TextureAtlasSprite[] sprites;

    private CommandBlockParticle(World world, double x, double y, double z,
                                       double motionX, double motionY, double motionZ,
                                       TextureAtlasSprite[] sprites) {
        super(world, x, y, z);
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.particleMaxAge = 10 + this.rand.nextInt(12);
        this.particleGravity = 0.0F;
        // 1.12 multiplies particleScale by 0.1 while building the quad;
        // upstream 1.20 uses 0.03 directly as the quad half-size.
        this.particleScale = 0.3F;
        this.canCollide = false;
        this.sprites = sprites;
        this.setParticleTexture(sprites[this.rand.nextInt(sprites.length)]);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!isExpired && sprites.length > 0) {
            int frame = Math.min(sprites.length - 1,
                    particleAge * (sprites.length - 1) / Math.max(1, particleMaxAge));
            setParticleTexture(sprites[frame]);
            if (particleAge > particleMaxAge / 2) {
                setAlphaF(Math.max(0.0F,
                        1.0F - (float) (particleAge - particleMaxAge / 2) / particleMaxAge));
            }
        }
        motionX *= 0.9285714286D;
        motionY *= 0.9285714286D;
        motionZ *= 0.9285714286D;
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return 15728880;
    }

    /** 注册上游的四个 sprite；每个 sprite 自身仍由外部 .mcmeta 驱动 1x4 插值动画。 */
    public static void registerSprites(TextureMap textureMap) {
        for (ResourceLocation location : SPRITE_LOCATIONS) {
            textureMap.registerSprite(location);
        }
    }

    /** 按上游 FormidibombEntity.tick 的公式生成六个粒子。 */
    public static void spawnForBomb(PowerfulExplosiveEntity.FormidibombEntity bomb) {
        if (bomb == null || bomb.world == null || !bomb.world.isRemote || bomb.isDead) return;
        int startFuse = bomb.getStartFuse();
        int currentFuse = bomb.getFuseLife();
        if (startFuse <= 0 || currentFuse <= 0) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite[] sprites = resolveSprites(minecraft);
        if (sprites == null) return;

        float fuseProgress = calculateFuseProgress(currentFuse, startFuse);
        float radius = calculateRadius(fuseProgress);
        float speed = calculateSpeed(fuseProgress);
        Random random = bomb.world.rand;
        for (int i = 0; i < 6; i++) {
            Vec3d offset = sampleOffset(random, radius);
            double offsetX = offset.x;
            double offsetY = offset.y;
            double offsetZ = offset.z;
            Vec3d delta = new Vec3d(offsetX, offsetY, offsetZ).scale(speed);
            if (fuseProgress > 0.5F) delta = delta.normalize();
            minecraft.effectRenderer.addEffect(new CommandBlockParticle(bomb.world,
                    bomb.posX + offsetX, bomb.posY + offsetY + 0.5D, bomb.posZ + offsetZ,
                    -delta.x, -delta.y, -delta.z, sprites));
        }
    }

    /** 为共生体龙息弹重建上游的命令方块轨迹粒子。 */
    public static void spawnForSymbiontDragonFireball(Entity fireball) {
        if (fireball == null || fireball.world == null || !fireball.world.isRemote || fireball.isDead) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite[] sprites = resolveSprites(minecraft);
        if (sprites == null) return;
        minecraft.effectRenderer.addEffect(new CommandBlockParticle(fireball.world,
                fireball.posX, fireball.posY + 0.5D, fireball.posZ,
                0.0D, 0.0D, 0.0D, sprites));
    }

    /** Upstream WitheredSymbiontEntity emits five command-block particles while casting. */
    public static void spawnForSymbiont(SickenedEntities.WitheredSymbiontEntity symbiont) {
        if (symbiont == null || symbiont.world == null || !symbiont.world.isRemote
                || symbiont.isDead || (!symbiont.isCastingSpell() && !symbiont.isSummoningMobs())) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite[] sprites = resolveSprites(minecraft);
        if (sprites == null) return;
        Random random = symbiont.world.rand;
        Vec3d eyePosition = symbiont.getPositionEyes(1.0F);
        for (int i = 0; i < 5; i++) {
            double x = symbiont.posX + random.nextGaussian() * 2.0D;
            double y = symbiont.posY + symbiont.getEyeHeight() + random.nextGaussian() * 2.0D;
            double z = symbiont.posZ + random.nextGaussian() * 2.0D;
            Vec3d delta = eyePosition.subtract(x, y, z);
            if (delta.lengthSquared() > 1.0E-6D) delta = delta.normalize().scale(0.2D);
            minecraft.effectRenderer.addEffect(new CommandBlockParticle(symbiont.world,
                    x, y, z, delta.x, delta.y, delta.z, sprites));
        }
    }

    /** Upstream WitherStormEntity emits five command-block particles while below phase 3. */
    public static void spawnForWitherStorm(WitherStormEntity storm) {
        if (storm == null || storm.world == null || !storm.world.isRemote || storm.isDead
                || storm.getPhase() >= 3) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite[] sprites = resolveSprites(minecraft);
        if (sprites == null) return;

        Random random = storm.world.rand;
        float angle = (storm.getBodyYRotation(1.0F) + 90.0F) * (float) (Math.PI / 180.0D);
        double x = Math.sin(angle) * 0.3D + storm.posX;
        double z = Math.cos(angle) * 0.3D + storm.posZ;
        double y = storm.posY + 1.4D;
        for (int i = 0; i < 5; i++) {
            double startX = x + random.nextGaussian();
            double startY = y + random.nextGaussian();
            double startZ = z + random.nextGaussian();
            Vec3d delta = new Vec3d(x, y, z).subtract(startX, startY, startZ);
            if (delta.lengthSquared() > 1.0E-6D) delta = delta.normalize().scale(0.1D);
            minecraft.effectRenderer.addEffect(new CommandBlockParticle(storm.world,
                    startX, startY, startZ, delta.x, delta.y, delta.z, sprites));
        }
    }

    /** 对应上游 ParticleEvents：掉落的命令方块书/工具持续冒出命令方块粒子。 */
    public static void spawnForItemEntity(EntityItem item) {
        if (item == null || item.world == null || !item.world.isRemote || item.isDead) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite[] sprites = resolveSprites(minecraft);
        if (sprites == null) return;
        Random random = item.world.rand;
        for (int i = 0; i < 2; i++) {
            double x = item.posX + random.nextFloat() * 0.4D;
            double y = item.posY + random.nextFloat() * 0.4D;
            double z = item.posZ + random.nextFloat() * 0.4D;
            Vec3d velocity = item.getLook(1.0F).subtract(x, y, z).normalize().scale(0.05D);
            minecraft.effectRenderer.addEffect(new CommandBlockParticle(item.world,
                    x, y, z, velocity.x, velocity.y, velocity.z, sprites));
        }
    }

    /** 重建上游 Formidibomb 方块随机显示 tick 的六颗恒速粒子。 */
    public static void spawnForBlock(World world, BlockPos position, Random random) {
        if (world == null || !world.isRemote || position == null || random == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite[] sprites = resolveSprites(minecraft);
        if (sprites == null) return;
        for (int i = 0; i < 6; i++) {
            Vec3d offset = sampleOffset(random, 3.0F);
            Vec3d velocity = offset.scale(-0.1D);
            minecraft.effectRenderer.addEffect(new CommandBlockParticle(world,
                    position.getX() + offset.x, position.getY() + offset.y,
                    position.getZ() + offset.z, velocity.x, velocity.y, velocity.z, sprites));
        }
    }

    /** 每收到一个服务端核心 tick 事件，仅生成该 tick 对应的一批粒子。 */
    public static void spawnForCommandBlock(SupplementalEntities.CommandBlockEntity commandBlock,
                                            double particleSpeed, int luringPlayerId) {
        if (commandBlock == null || commandBlock.world == null || !commandBlock.world.isRemote
                || commandBlock.isDead) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite[] sprites = resolveSprites(minecraft);
        if (sprites == null) return;

        Random random = commandBlock.world.rand;
        double speed = Math.max(0.0D, particleSpeed) * 0.1D;
        for (int i = 0; i < 5; i++) {
            Vec3d offset = new Vec3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            Vec3d velocity = offset.scale(-1.0D)
                    .normalize().scale(speed);
            minecraft.effectRenderer.addEffect(new CommandBlockParticle(commandBlock.world,
                    commandBlock.posX + offset.x,
                    commandBlock.posY + commandBlock.getEyeHeight() + offset.y,
                    commandBlock.posZ + offset.z, velocity.x, velocity.y, velocity.z, sprites));
        }

        Entity entity = commandBlock.world.getEntityByID(luringPlayerId);
        if (!(entity instanceof EntityPlayer) || entity.isDead) return;
        EntityPlayer player = (EntityPlayer) entity;
        for (int i = 0; i < 4; i++) {
            double x = player.posX + random.nextGaussian() * player.width * 0.4D;
            double y = player.getEntityBoundingBox().minY + random.nextGaussian() * player.height * 0.4D;
            double z = player.posZ + random.nextGaussian() * player.width * 0.4D;
            Vec3d velocity = new Vec3d(commandBlock.posX - x,
                    commandBlock.posY + commandBlock.getEyeHeight() - y, commandBlock.posZ - z)
                    .normalize().scale(0.1D);
            minecraft.effectRenderer.addEffect(new CommandBlockParticle(commandBlock.world,
                    x, y, z, velocity.x, velocity.y, velocity.z, sprites));
        }
    }

    /** 上游复活仪式每 tick 在命令方块周围生成一颗向中心收拢的粒子。 */
    public static void spawnForSuperBeacon(World world, BlockPos beaconPos, Random random) {
        if (world == null || !world.isRemote || beaconPos == null || random == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureAtlasSprite[] sprites = resolveSprites(minecraft);
        if (sprites == null) return;

        double targetX = beaconPos.getX();
        double targetY = beaconPos.getY() + 3.0D;
        double targetZ = beaconPos.getZ();
        double x = targetX + random.nextGaussian();
        double y = targetY + random.nextGaussian();
        double z = targetZ + random.nextGaussian();
        Vec3d velocity = new Vec3d(targetX - x, targetY - y, targetZ - z).normalize().scale(0.1D);
        minecraft.effectRenderer.addEffect(new CommandBlockParticle(
                world, x, y, z, velocity.x, velocity.y, velocity.z, sprites));
    }

    /** 客户端重建上游服务端粒子包的一次性爆发。 */
    public static void spawnSuperBeaconBurst(BlockPos beaconPos, int type) {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft.world;
        if (world == null || beaconPos == null) return;
        TextureAtlasSprite[] sprites = resolveSprites(minecraft);
        if (sprites == null) return;

        Vec3d smokeCenter = getSuperBeaconBurstCenter(beaconPos, type, false);
        Vec3d commandCenter = getSuperBeaconBurstCenter(beaconPos, type, true);
        Random random = world.rand;
        for (int i = 0; i < 20; i++) {
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    smokeCenter.x + random.nextGaussian(), smokeCenter.y + random.nextGaussian(),
                    smokeCenter.z + random.nextGaussian(), random.nextGaussian() * 0.01D,
                    random.nextGaussian() * 0.01D, random.nextGaussian() * 0.01D);
        }
        for (int i = 0; i < 50; i++) {
            minecraft.effectRenderer.addEffect(new CommandBlockParticle(world,
                    commandCenter.x + random.nextGaussian(), commandCenter.y + random.nextGaussian(),
                    commandCenter.z + random.nextGaussian(), random.nextGaussian() * 0.015D,
                    random.nextGaussian() * 0.015D, random.nextGaussian() * 0.015D, sprites));
        }
    }

    /** 重建服务端命令方块粒子包，支持原版粒子包的高斯分布和受击均匀速度。 */
    public static void spawnBurst(Vec3d center, int count,
                                  double spreadX, double spreadY, double spreadZ,
                                  double speed, int distribution) {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft.world;
        if (world == null || center == null || count <= 0) return;
        TextureAtlasSprite[] sprites = resolveSprites(minecraft);
        if (sprites == null) return;
        Random random = world.rand;
        for (int index = 0; index < count; index++) {
            double x = center.x;
            double y = center.y;
            double z = center.z;
            double velocityX;
            double velocityY;
            double velocityZ;
            if (distribution == ModNetwork.COMMAND_BLOCK_PARTICLES_EXACT_VELOCITY) {
                velocityX = spreadX * speed;
                velocityY = spreadY * speed;
                velocityZ = spreadZ * speed;
            } else if (distribution == ModNetwork.COMMAND_BLOCK_PARTICLES_UNIFORM_VELOCITY) {
                velocityX = (random.nextFloat() - 0.5F) * speed;
                velocityY = (random.nextFloat() - 0.5F) * speed;
                velocityZ = (random.nextFloat() - 0.5F) * speed;
            } else {
                x += random.nextGaussian() * spreadX;
                y += random.nextGaussian() * spreadY;
                z += random.nextGaussian() * spreadZ;
                velocityX = random.nextGaussian() * speed;
                velocityY = random.nextGaussian() * speed;
                velocityZ = random.nextGaussian() * speed;
            }
            minecraft.effectRenderer.addEffect(new CommandBlockParticle(world,
                    x, y, z, velocityX, velocityY, velocityZ, sprites));
        }
    }

    static Vec3d getSuperBeaconBurstCenter(BlockPos beaconPos, int type, boolean commandBlockParticle) {
        boolean itemCraft = type == SUPER_BEACON_ITEM_BURST;
        double yOffset = itemCraft ? 2.0D : commandBlockParticle ? 3.0D : 0.0D;
        return new Vec3d(beaconPos.getX(), beaconPos.getY() + yOffset, beaconPos.getZ());
    }

    private static TextureAtlasSprite[] resolveSprites(Minecraft minecraft) {
        if (minecraft == null || minecraft.effectRenderer == null) return null;
        TextureMap textureMap = minecraft.getTextureMapBlocks();
        TextureAtlasSprite[] sprites = new TextureAtlasSprite[SPRITE_LOCATIONS.length];
        for (int i = 0; i < SPRITE_LOCATIONS.length; i++) {
            sprites[i] = textureMap.getAtlasSprite(SPRITE_LOCATIONS[i].toString());
            if (sprites[i] == null || "missingno".equals(sprites[i].getIconName())) return null;
        }
        return sprites;
    }

    static Vec3d sampleOffset(Random random, float radius) {
        return new Vec3d(
                (random.nextFloat() * 2.0F - 1.0F) * radius,
                (random.nextFloat() * 2.0F - 1.0F) * radius,
                (random.nextFloat() * 2.0F - 1.0F) * radius);
    }

    static float calculateFuseProgress(int currentFuse, int startFuse) {
        return startFuse > 0 ? 1.0F - (float) currentFuse / (float) startFuse : 0.0F;
    }

    static float calculateRadius(float fuseProgress) {
        return fuseProgress < 0.5F ? 3.0F * (1.0F - fuseProgress) : 1.5F;
    }

    static float calculateSpeed(float fuseProgress) {
        return fuseProgress < 0.5F
                ? 0.1F * (1.0F - 2.0F * fuseProgress)
                : 0.1F * (8.0F * (fuseProgress - 0.5F));
    }

    static int getParticlesPerTick() {
        return 6;
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public boolean shouldDisableDepth() {
        return false;
    }
}
