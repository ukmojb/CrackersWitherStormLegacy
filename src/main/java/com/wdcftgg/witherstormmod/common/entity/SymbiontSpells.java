package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.api.common.ai.symbiont.SpellType;
import com.wdcftgg.witherstormmod.api.common.ai.symbiont.SymbiontSpell;
import com.wdcftgg.witherstormmod.api.common.registry.WitherStormModRegistries;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEvokerFangs;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.entity.projectile.EntitySpectralArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import thedarkcolour.futuremc.entity.trident.Trident;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;






public final class SymbiontSpells {
    private static final Map<Type, SpellType> API_TYPES =
            new EnumMap<Type, SpellType>(Type.class);
    private static final ResourceLocation FUTURE_MC_TRIDENT =
            new ResourceLocation("futuremc", "trident");
    private static final Map<SpellType, Type> LEGACY_TYPES =
            new IdentityHashMap<SpellType, Type>();

    private SymbiontSpells() {
    }

    public enum Type {
        EMPTY(0, false, 3.0D, 1.0D, 0, 0),
        EVOKER_FANGS(20, true, 3.0D, 1.0D, 60, 100),
        SHULKER_BULLETS(80, true, 3.0D, 1.0D, 500, 620),
        ARROWS(120, true, 3.0D, 1.0D, 160, 200),
        SMASH(40, true, 3.0D, 1.0D, 160, 200),
        FIRE_BALLS(160, true, 3.0D, 1.0D, 400, 520),
        WITHER_SKULLS(220, true, 3.0D, 1.0D, 440, 580),
        PULL(240, false, 3.0D, 1.0D, 340, 420),
        THROWING(240, true, 3.0D, 1.0D, 360, 480),
        BOMBING(60, true, 3.0D, 1.0D, 360, 600),
        PULSE(160, true, 6.0D, 2.0D, 480, 600);

        final int spellTime;
        final boolean protectsCaster;
        final double protectionRadius;
        final double protectionStrength;
        private final int minimumDelay;
        private final int randomDelayBound;

        Type(int spellTime, boolean protectsCaster, double protectionRadius, double protectionStrength,
             int minimumDelay, int randomDelayBound) {
            this.spellTime = spellTime;
            this.protectsCaster = protectsCaster;
            this.protectionRadius = protectionRadius;
            this.protectionStrength = protectionStrength;
            this.minimumDelay = minimumDelay;
            this.randomDelayBound = randomDelayBound;
        }

        int getDelay(Random random, float modifier) {
            if (this == EMPTY) return 0;
            return Math.max(minimumDelay, random.nextInt(randomDelayBound)) - MathHelper.floor(modifier) * 10;
        }

        public static Type byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : EMPTY;
        }

        public int getSpellTime() { return spellTime; }
        public boolean protectsCaster() { return protectsCaster; }
        public double getProtectionRadius() { return protectionRadius; }
        public double getProtectionStrength() { return protectionStrength; }
    }

    abstract static class Spell {
        final SickenedEntities.WitheredSymbiontEntity entity;
        final Type type;
        final List<Entity> projectiles = new ArrayList<Entity>();

        Spell(SickenedEntities.WitheredSymbiontEntity entity, Type type) {
            this.entity = entity;
            this.type = type;
        }

        void start(EntityLivingBase target) {
        }

        abstract void cast(EntityLivingBase target);

        void tick(EntityLivingBase target) {
        }

        void finish() {
            for (Entity projectile : projectiles) {
                if (projectile != null && !projectile.isDead) projectile.setNoGravity(false);
            }
            projectiles.clear();
        }

        int getDelay(float modifier) {
            return type.getDelay(entity.getRNG(), modifier);
        }

        void projectileCommandParticle(Entity target, double radius) {
            if (!(entity.world instanceof WorldServer) || target == null) return;
            double x = target.posX + entity.getRNG().nextGaussian() * radius;
            double y = target.posY + target.getEyeHeight() + entity.getRNG().nextGaussian() * radius;
            double z = target.posZ + entity.getRNG().nextGaussian() * radius;
            Vec3d delta = target.getPositionEyes(1.0F).subtract(x, y, z).normalize().scale(0.1D);
            ModNetwork.sendCommandBlockParticles(entity.world,
                    new Vec3d(x, y, z), 1, delta.x, delta.y, delta.z, 1.0D,
                    ModNetwork.COMMAND_BLOCK_PARTICLES_EXACT_VELOCITY);
        }

        void bombingParticles(Entity target) {
            if (!(entity.world instanceof WorldServer) || target == null) return;
            double x = target.posX + entity.getRNG().nextGaussian();
            double y = target.posY + target.getEyeHeight() + entity.getRNG().nextGaussian();
            double z = target.posZ + entity.getRNG().nextGaussian();
            Vec3d delta = target.getPositionEyes(1.0F).subtract(x, y, z).normalize().scale(0.1D);
            ModNetwork.sendCommandBlockParticles(entity.world,
                    new Vec3d(x, y, z), 1, delta.x, delta.y, delta.z, 1.0D,
                    ModNetwork.COMMAND_BLOCK_PARTICLES_EXACT_VELOCITY);
            ((WorldServer) entity.world).spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    target.posX, target.posY, target.posZ, 0,
                    delta.x, delta.y, delta.z, 0.125D);
        }

        void pullCommandParticle(Entity target) {
            if (!(entity.world instanceof WorldServer) || target == null) return;
            AxisAlignedBB bounds = target.getEntityBoundingBox();
            double x = target.posX + entity.getRNG().nextGaussian() * (bounds.maxX - bounds.minX) * 0.4D;
            double y = bounds.minY + entity.getRNG().nextGaussian() * (bounds.maxY - bounds.minY) * 0.4D;
            double z = target.posZ + entity.getRNG().nextGaussian() * (bounds.maxZ - bounds.minZ) * 0.4D;
            Vec3d delta = entity.getPositionEyes(1.0F).subtract(x, y, z).normalize().scale(0.1D);
            ModNetwork.sendCommandBlockParticles(entity.world,
                    new Vec3d(x, y, z), 1, delta.x, delta.y, delta.z, 1.0D,
                    ModNetwork.COMMAND_BLOCK_PARTICLES_EXACT_VELOCITY);
        }

        void pulseCommandParticles(Entity target, boolean center) {
            if (!(entity.world instanceof WorldServer) || target == null) return;
            double x = target.posX;
            double y = target.posY + target.getEyeHeight();
            double z = target.posZ;
            if (!center) {
                x += entity.getRNG().nextGaussian();
                y += entity.getRNG().nextGaussian();
                z += entity.getRNG().nextGaussian();
            }
            Vec3d delta = target.getPositionEyes(1.0F).subtract(x, y, z);
            if (delta.lengthSquared() > 1.0E-6D) delta = delta.normalize().scale(0.1D);
            ModNetwork.sendCommandBlockParticles(entity.world,
                    new Vec3d(x, y, z), center ? 3 : 1,
                    delta.x, delta.y, delta.z, 1.0D,
                    center ? ModNetwork.COMMAND_BLOCK_PARTICLES_GAUSSIAN
                            : ModNetwork.COMMAND_BLOCK_PARTICLES_EXACT_VELOCITY);
        }

        void projectilePoofParticle(Entity target, double radius) {
            if (!(entity.world instanceof WorldServer) || target == null) return;
            double x = target.posX + entity.getRNG().nextGaussian() * radius;
            double y = target.posY + target.getEyeHeight() + entity.getRNG().nextGaussian() * radius;
            double z = target.posZ + entity.getRNG().nextGaussian() * radius;
            Vec3d delta = target.getPositionEyes(1.0F).subtract(x, y, z).normalize().scale(0.1D);
            ((WorldServer) entity.world).spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    x, y, z, 0, delta.x, delta.y, delta.z, 1.0D);
        }

        EntityLivingBase randomPlayerOrFallback(EntityLivingBase fallback) {
            return entity.getRandomNearbyTargetOrFallback(fallback, true);
        }
    }

    static Spell create(SickenedEntities.WitheredSymbiontEntity entity, Type type) {
        switch (type) {
            case EVOKER_FANGS: return new EvokerFangsSpell(entity, type);
            case SHULKER_BULLETS: return new ShulkerBulletsSpell(entity, type);
            case ARROWS: return new ArrowsSpell(entity, type);
            case SMASH: return new SmashSpell(entity, type);
            case FIRE_BALLS: return new FireballsSpell(entity, type);
            case WITHER_SKULLS: return new WitherSkullsSpell(entity, type);
            case PULL: return new PullSpell(entity, type);
            case THROWING: return new ThrowingSpell(entity, type);
            case BOMBING: return new BombingSpell(entity, type);
            case PULSE: return new PulseSpell(entity, type);
            default: return new EmptySpell(entity, Type.EMPTY);
        }
    }


    public static void registerApiTypes() {
        for (Type type : Type.values()) {
            SpellType apiType = apiType(type);
            apiType.setRegistryName(Tags.MOD_ID, type.name().toLowerCase(Locale.ROOT));
            WitherStormModRegistries.registerSpellType(apiType);
        }
    }


    public static SpellType apiType(Type type) {
        SpellType existing = API_TYPES.get(type);
        if (existing != null) return existing;
        SpellType created = createApiType(type);
        API_TYPES.put(type, created);
        LEGACY_TYPES.put(created, type);
        return created;
    }

    public static Type legacyType(SpellType type) {
        Type legacy = LEGACY_TYPES.get(type);
        return legacy == null ? Type.EMPTY : legacy;
    }

    private static SpellType createApiType(Type type) {
        int time = type.getSpellTime();
        boolean protects = type.protectsCaster();
        double radius = type.getProtectionRadius();
        double strength = type.getProtectionStrength();
        Optional<Supplier<SoundEvent>> loop = type == Type.PULL
                ? Optional.<Supplier<SoundEvent>>of(
                () -> ModSounds.get("withered_symbiont_pull"))
                : Optional.<Supplier<SoundEvent>>empty();
        return new SpellType(
                (entity, spellType) -> new ApiSpellAdapter(entity, type, spellType),
                time, loop,
                protects, radius, strength);
    }

    private static final class ApiSpellAdapter extends SymbiontSpell {
        private final Spell delegate;

        ApiSpellAdapter(SickenedEntities.WitheredSymbiontEntity entity, Type type,
                        SpellType apiType) {
            super(entity, apiType);
            this.delegate = create(entity, type);
        }

        @Override
        public void start(EntityLivingBase target) {
            delegate.start(target);
        }

        @Override
        public void cast(EntityLivingBase target) {
            delegate.cast(target);
        }

        @Override
        public void finish() {
            delegate.finish();
        }

        @Override
        public void doCasting(EntityLivingBase target) {
            delegate.tick(target);
        }

        @Override
        public int getDelay(Random random, float modifier) {
            return delegate.getDelay(modifier);
        }
    }

    private static final class EmptySpell extends Spell {
        EmptySpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) { super(entity, type); }
        @Override void cast(EntityLivingBase target) { }
    }

    private static final class ArrowsSpell extends Spell {
        ArrowsSpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) { super(entity, type); }

        @Override
        void start(EntityLivingBase target) {
            discardProjectiles(projectiles);
        }

        @Override
        void tick(EntityLivingBase target) {
            int count = entity.shouldIncreaseDifficulty() ? 6 : 3;
            int releaseAge = entity.shouldIncreaseDifficulty() ? 10 : 20;
            if (entity.ticksExisted % 4 == 0) {
                for (int index = 0; index < count; index++) {
                    EntityTippedArrow arrow = new EntityTippedArrow(entity.world, entity);
                    arrow.setNoGravity(true);
                    arrow.noClip = true;
                    arrow.setPosition(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
                    arrow.motionX = entity.getRNG().nextGaussian() * 0.55D;
                    arrow.motionY = entity.getRNG().nextDouble() * 0.75D;
                    arrow.motionZ = entity.getRNG().nextGaussian() * 0.55D;
                    entity.world.spawnEntity(arrow);
                    projectiles.add(arrow);
                }
                entity.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 4.0F,
                        0.5F + (entity.getRNG().nextFloat() - 0.5F) * 0.1F);
            }

            for (Entity projectile : projectiles) {
                if (!(projectile instanceof EntityArrow) || projectile.isDead) continue;
                EntityArrow arrow = (EntityArrow) projectile;
                if (arrow.ticksExisted == releaseAge) {
                    EntityLivingBase chosen = randomPlayerOrFallback(target);
                    if (chosen == null) continue;
                    double x = chosen.posX + entity.getRNG().nextGaussian() * 2.0D - arrow.posX;
                    double y = chosen.posY + chosen.height / 3.0D
                            + entity.getRNG().nextGaussian() * 0.5D - arrow.posY;
                    double z = chosen.posZ + entity.getRNG().nextGaussian() * 2.0D - arrow.posZ;
                    double horizontal = Math.sqrt(x * x + z * z);
                    arrow.setNoGravity(false);
                    arrow.noClip = false;
                    arrow.shoot(x, y + horizontal * 0.2D, z, 2.5F,
                            14.0F - entity.world.getDifficulty().getId() * 4.0F);
                    arrow.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 2.0F, 1.0F);
                } else if (arrow.ticksExisted < releaseAge) {
                    projectileCommandParticle(arrow, 0.5D);
                }
            }
        }

        @Override void cast(EntityLivingBase target) { }

        @Override
        void finish() {
            for (Entity projectile : new ArrayList<Entity>(projectiles)) {
                projectilePoofParticle(projectile, 0.5D);
                if (projectile != null && !projectile.isDead) projectile.setDead();
            }
            projectiles.clear();
        }
    }

    private static final class BombingSpell extends Spell {
        BombingSpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) { super(entity, type); }

        @Override
        void start(EntityLivingBase target) {
            double x = entity.posX + entity.getRNG().nextGaussian();
            double z = entity.posZ + entity.getRNG().nextGaussian();
            SickenedEntities.SickenedCreeperEntity creeper = new SickenedEntities.SickenedCreeperEntity(entity.world);
            creeper.setNoGravity(true);
            creeper.setPosition(x, entity.posY + 1.0D, z);
            creeper.motionY = entity.getRNG().nextDouble() * 0.07D;
            creeper.setAttackTarget(target);
            entity.world.spawnEntity(creeper);
            projectiles.add(creeper);
            entity.playSound(SoundEvents.ENTITY_FIREWORK_LAUNCH, 4.0F, 0.75F);
        }

        @Override
        void tick(EntityLivingBase target) {
            for (Entity projectile : projectiles) {
                if (!projectile.isDead) bombingParticles(projectile);
            }
        }

        @Override
        void cast(EntityLivingBase target) {
            for (Entity raw : projectiles) {
                if (!(raw instanceof SickenedEntities.SickenedCreeperEntity) || raw.isDead) continue;
                SickenedEntities.SickenedCreeperEntity creeper = (SickenedEntities.SickenedCreeperEntity) raw;
                EntityLivingBase chosen = randomPlayerOrFallback(target);
                if (chosen == null) continue;
                double x = chosen.posX - creeper.posX;
                double y = entity.getRNG().nextGaussian() * 4.0D + chosen.posY + chosen.height * 0.34D - creeper.posY;
                double z = chosen.posZ - creeper.posZ;
                double distance = Math.max(0.001D, Math.sqrt(x * x + y * y + z * z));
                creeper.motionX = x / (distance * 0.3D);
                creeper.motionY = y / (distance * 0.25D) + 1.0D;
                creeper.motionZ = z / (distance * 0.3D);
                creeper.velocityChanged = true;
                creeper.setNoGravity(false);
                creeper.setAttackTarget(chosen);
                creeper.ignite();
            }
            entity.playSound(SoundEvents.ENTITY_FIREWORK_BLAST, 4.0F, 1.0F);
        }

        @Override
        void finish() {
            if (entity.getAttackTarget() == null) {
                for (Entity projectile : new ArrayList<Entity>(projectiles)) {
                    if (projectile != null && !projectile.isDead) projectile.setDead();
                }
                projectiles.clear();
            } else projectiles.clear();
        }
    }

    private static final class EvokerFangsSpell extends Spell {
        EvokerFangsSpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) { super(entity, type); }

        @Override
        void cast(EntityLivingBase target) {
            int chance = entity.shouldIncreaseDifficulty() ? 6 : 3;
            if (entity.getRNG().nextInt(chance) == 1) {
                for (int index = 0; index < 3; index++) entity.summonSupportMob(true, 6);
            }

            EntityLivingBase chosen = randomPlayerOrFallback(target);
            if (chosen == null) return;
            double minimumY = Math.min(chosen.posY, entity.posY) - 2.0D;
            double maximumY = Math.max(chosen.posY, entity.posY) + 2.0D;
            int fangsPerRing = 8;
            for (int ring = 0; ring < 16; ring++) {
                double radius = 1.5D + ring * (3.0D + entity.getRNG().nextDouble());
                fangsPerRing += 4;
                double increment = Math.PI * 2.0D / fangsPerRing;
                for (int fang = 0; fang < fangsPerRing; fang++) {
                    double angle = fang * increment;
                    createFang(entity.posX + Math.cos(angle) * radius,
                            entity.posZ + Math.sin(angle) * radius,
                            minimumY, maximumY, (float) angle, fang + 2);
                }
            }
        }

        private void createFang(double x, double z, double minimumY, double maximumY, float yaw, int delay) {
            BlockPos cursor = new BlockPos(x, maximumY, z);
            while (cursor.getY() >= MathHelper.floor(minimumY) - 1) {
                BlockPos below = cursor.down();
                IBlockState floor = entity.world.getBlockState(below);
                if (floor.isSideSolid(entity.world, below, EnumFacing.UP)) {
                    AxisAlignedBB collision = entity.world.getBlockState(cursor)
                            .getCollisionBoundingBox(entity.world, cursor);
                    double offset = collision == null ? 0.0D : collision.maxY;
                    entity.world.spawnEntity(new EntityEvokerFangs(entity.world, x,
                            cursor.getY() + offset, z, yaw, delay, entity));
                    return;
                }
                cursor = cursor.down();
            }
        }
    }

    private static final class FireballsSpell extends Spell {
        private final int variant;
        private final int count;
        private final int throwInterval;

        FireballsSpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) {
            super(entity, type);
            variant = entity.getRNG().nextInt(7);
            count = variant == 0 ? 16 : variant == 1 ? 6 : variant == 2 ? 4 : 8;
            throwInterval = type.spellTime / count;
        }

        @Override
        void start(EntityLivingBase target) {
            discardProjectiles(projectiles);
            double y = entity.posY + 4.0D;
            float theta = (float) (Math.PI * 2.0D / count);
            for (int index = 0; index < count; index++) {
                float angle = theta * index;
                EntityFireball projectile = makeProjectile();
                projectile.setPosition(7.0D * MathHelper.sin(angle) + entity.posX, y,
                        7.0D * MathHelper.cos(angle) + entity.posZ);
                hold(projectile);
                entity.world.spawnEntity(projectile);
                projectiles.add(projectile);
            }
        }

        private EntityFireball makeProjectile() {
            if (variant == 0) return new EntitySmallFireball(entity.world, entity, 0.0D, 0.0D, 0.0D);
            if (variant == 1) {
                return new SymbiontDragonFireballEntity(entity.world, entity, 0.0D, 0.0D, 0.0D);
            }
            if (variant == 2) return new SupplementalEntities.FlamingWitherSkullEntity(entity.world, entity, 0.0D, 0.0D, 0.0D);
            EntityLargeFireball fireball = new EntityLargeFireball(entity.world, entity, 0.0D, 0.0D, 0.0D);
            fireball.explosionPower = 1;
            return fireball;
        }

        @Override
        void tick(EntityLivingBase target) {
            int elapsed = type.spellTime - entity.getSpellCastingTime();
            for (int index = projectiles.size() - 1; index >= 0; index--) {
                Entity raw = projectiles.get(index);
                if (!(raw instanceof EntityFireball) || raw.isDead) {
                    projectiles.remove(index);
                    continue;
                }
                EntityFireball fireball = (EntityFireball) raw;
                int size = Math.max(1, projectiles.size());
                float angle = (float) (Math.PI * 2.0D / size) * index + elapsed * 0.08F;
                Vec3d wanted = new Vec3d(7.0D * MathHelper.sin(angle) + entity.posX,
                        entity.posY + 4.0D, 7.0D * MathHelper.cos(angle) + entity.posZ);
                double distance = fireball.getPositionVector().distanceTo(wanted);
                Vec3d movement = wanted.subtract(fireball.getPositionVector()).normalize()
                        .scale(Math.min(1.0D, distance));
                if (elapsed % throwInterval == 0 && index == 0) {
                    EntityLivingBase chosen = randomPlayerOrFallback(target);
                    if (chosen == null) continue;
                    Vec3d acceleration = chosen.getPositionEyes(1.0F)
                            .subtract(fireball.getPositionVector()).normalize().scale(0.1D);
                    fireball.noClip = false;
                    fireball.setNoGravity(false);
                    fireball.motionX = fireball.motionY = fireball.motionZ = 0.0D;
                    fireball.accelerationX = acceleration.x;
                    fireball.accelerationY = acceleration.y;
                    fireball.accelerationZ = acceleration.z;
                    ModNetwork.syncDamagingProjectile(fireball);
                    fireball.velocityChanged = true;
                    fireball.playSound(variant == 2 ? ModSounds.get("wither_storm_shoot")
                            : SoundEvents.ENTITY_GHAST_SHOOT, 4.0F, 1.0F);
                    projectiles.remove(index);
                } else {
                    fireball.motionX = movement.x;
                    fireball.motionY = movement.y;
                    fireball.motionZ = movement.z;
                    hold(fireball);
                    fireball.velocityChanged = true;
                }
            }
        }

        private static void hold(EntityFireball fireball) {
            fireball.noClip = true;
            fireball.setNoGravity(true);
            fireball.accelerationX = fireball.accelerationY = fireball.accelerationZ = 0.0D;
        }

        @Override void cast(EntityLivingBase target) { }

        @Override
        void finish() {
            for (Entity projectile : new ArrayList<Entity>(projectiles)) {
                if (projectile != null && !projectile.isDead) projectile.setDead();
            }
            projectiles.clear();
        }
    }

    static final class SmashSpell extends Spell {
        private int stompCount;
        private int stompCooldown;

        SmashSpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) { super(entity, type); }

        @Override
        void start(EntityLivingBase target) {
            stompCount = entity.getRNG().nextInt(3) + 1;
        }

        @Override
        void cast(EntityLivingBase target) {
            if (target != null && entity.world.getBlockState(new BlockPos(entity).down()).getBlock() != Blocks.AIR) {
                stomp(target);
            }
        }

        @Override
        void tick(EntityLivingBase target) {
            if (entity.shouldIncreaseDifficulty() && entity.world instanceof WorldServer) {
                ((WorldServer) entity.world).spawnParticle(EnumParticleTypes.CLOUD,
                        entity.posX + entity.getRNG().nextFloat(), entity.posY,
                        entity.posZ + entity.getRNG().nextFloat(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            if (stompCount > 0 && stompCooldown <= 0) {
                stomp(target);
                stompCount--;
                stompCooldown = 5;
                if (stompCount > 0) cast(randomPlayerOrFallback(target));
            } else {
                stompCooldown--;
            }
        }

        private void stomp(EntityLivingBase target) {
            if (target == null || entity.world.getBlockState(new BlockPos(entity).down()).getBlock() == Blocks.AIR) return;
            float upward = 0.42F * 5.0F;
            PotionEffect jump = entity.getActivePotionEffect(MobEffects.JUMP_BOOST);
            if (jump != null) upward += 0.1F * (jump.getAmplifier() + 1);
            double x = entity.posX - target.posX;
            double z = entity.posZ - target.posZ;
            double multiplier = Math.min(0.2D, entity.getDistance(target) * 0.05D);
            entity.motionX += -x * multiplier;
            entity.motionY = upward;
            entity.motionZ += -z * multiplier;
            entity.velocityChanged = true;
            entity.setSmashing(true);
            entity.playSound(SoundEvents.BLOCK_STONE_STEP, 4.0F, 1.0F);
        }
    }

    private static final class PullSpell extends Spell {
        PullSpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) { super(entity, type); }
        @Override void cast(EntityLivingBase target) { }

        @Override
        void tick(EntityLivingBase target) {
            for (EntityLivingBase candidate : entity.getNearbyMobTargets()) {
                if (!candidate.isEntityAlive() || candidate == entity) continue;
                if (candidate.getDistance(entity) < 3.0F) {
                    entity.breakSpell();
                    return;
                }
                Vec3d pull = entity.getPositionVector().subtract(candidate.getPositionVector()).normalize().scale(0.1D);
                candidate.motionX = pull.x;
                candidate.motionZ = pull.z;
                candidate.velocityChanged = true;
                if (candidate instanceof EntityPlayerMP) {
                    EntityPlayerMP player = (EntityPlayerMP) candidate;
                    ModNetwork.setPlayerMotion(player, candidate,
                            new Vec3d(candidate.motionX, candidate.motionY, candidate.motionZ));
                }
                pullCommandParticle(candidate);
            }
        }
    }

    private static final class PulseSpell extends Spell {
        PulseSpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) { super(entity, type); }

        @Override
        void tick(EntityLivingBase target) {
            pulseCommandParticles(entity, true);
            for (EntityLivingBase candidate : entity.getNearbyPulseTargets()) {
                pulseCommandParticles(candidate, false);
            }
            if (entity.ticksExisted % 16 == 0) {
                entity.playSound(ModSounds.get("command_block_activates"), 4.0F, 0.75F);
            }
        }

        @Override
        void cast(EntityLivingBase target) {
            entity.playSound(ModSounds.get("command_block_activates"), 4.0F, 2.0F);
            for (BlockPos pos : getNearbyBlocks()) {
                SupplementalEntities.BlockClusterEntity cluster = new SupplementalEntities.BlockClusterEntity(entity.world);
                cluster.populateWithRadius(pos, 1, SymbiontSpells::isPulseThrowable);
                if (cluster.getBlocks().isEmpty()) continue;
                cluster.setTime(100);
                cluster.setShouldCrumble(false);
                int rotation = entity.getRNG().nextInt(129) - 64;
                cluster.setRotationDelta(rotation * 0.0625F, rotation * 0.0625F);
                cluster.setPhysics(true);
                Vec3d movement = new Vec3d(pos.getX() + entity.getRNG().nextInt(4) - entity.posX,
                        pos.getY() + entity.getRNG().nextInt(4) - entity.posY,
                        pos.getZ() + entity.getRNG().nextInt(4) - entity.posZ).normalize().scale(2.0D);
                cluster.motionX = movement.x;
                cluster.motionY = movement.y;
                cluster.motionZ = movement.z;
                entity.world.spawnEntity(cluster);
            }
            for (EntityLivingBase candidate : entity.getNearbyPulseTargets()) {
                Vec3d movement = new Vec3d(candidate.posX, candidate.posY + 1.0D, candidate.posZ)
                        .subtract(entity.getPositionVector()).normalize().scale(3.0D);
                candidate.motionX = movement.x;
                candidate.motionY = movement.y;
                candidate.motionZ = movement.z;
                candidate.velocityChanged = true;
            }
        }

        private List<BlockPos> getNearbyBlocks() {
            List<BlockPos> result = new ArrayList<BlockPos>();
            BlockPos origin = new BlockPos(entity);
            for (int x = -16; x <= 16; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -16; z <= 16 && result.size() < 1024; z++) {
                        BlockPos pos = origin.add(x, y, z);
                        if (isPulseThrowable(entity.world, pos, entity.world.getBlockState(pos))) result.add(pos);
                    }
                }
            }
            return result;
        }
    }

    private static final class ShulkerBulletsSpell extends Spell {
        ShulkerBulletsSpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) { super(entity, type); }

        @Override
        void start(EntityLivingBase target) {
            discardProjectiles(projectiles);
            int amount = entity.shouldIncreaseDifficulty() ? 6 : 5;
            for (int index = 0; index < amount; index++) {
                float angle = (float) (Math.PI * 2.0D / amount) * index;
                EntityShulkerBullet bullet = new EntityShulkerBullet(entity.world);
                bullet.owner = entity;
                bullet.setPosition(5.0D * MathHelper.sin(angle) + entity.posX, entity.posY,
                        5.0D * MathHelper.cos(angle) + entity.posZ);
                bullet.setNoGravity(true);
                bullet.noClip = true;
                entity.world.spawnEntity(bullet);
                projectiles.add(bullet);
            }
        }

        @Override
        void tick(EntityLivingBase target) {
            int size = projectiles.size();
            int elapsed = type.spellTime - entity.getSpellCastingTime();
            for (int index = 0; index < size; index++) {
                Entity bullet = projectiles.get(index);
                if (bullet.isDead) continue;
                float angle = (float) (Math.PI * 2.0D / size) * index + elapsed * 0.1F;
                bullet.setPosition(5.0D * MathHelper.sin(angle) + entity.posX, entity.posY,
                        5.0D * MathHelper.cos(angle) + entity.posZ);
                bullet.motionX = bullet.motionY = bullet.motionZ = 0.0D;
                bullet.velocityChanged = true;
                if (entity.world instanceof WorldServer) {
                    BlockPos min = new BlockPos(
                            bullet.getEntityBoundingBox().minX - 1.0D,
                            bullet.getEntityBoundingBox().minY - 1.0D,
                            bullet.getEntityBoundingBox().minZ - 1.0D);
                    BlockPos max = new BlockPos(
                            bullet.getEntityBoundingBox().maxX + 1.0D,
                            bullet.getEntityBoundingBox().maxY + 1.0D,
                            bullet.getEntityBoundingBox().maxZ + 1.0D);
                    for (BlockPos pos : BlockPos.getAllInBoxMutable(min, max)) {
                        if (!entity.world.isAirBlock(pos)) entity.world.destroyBlock(pos, true);
                    }
                }
            }
        }

        @Override
        void cast(EntityLivingBase target) {
            for (Entity projectile : projectiles) {
                if (!(projectile instanceof EntityShulkerBullet) || projectile.isDead) continue;
                EntityLivingBase chosen = randomPlayerOrFallback(target);
                EntityShulkerBullet bullet = (EntityShulkerBullet) projectile;
                bullet.target = chosen;
                bullet.direction = EnumFacing.UP;
                bullet.selectNextMoveDirection(EnumFacing.Axis.Y);
                projectile.setNoGravity(false);
            }
            projectiles.clear();
            entity.playSound(SoundEvents.ENTITY_SHULKER_SHOOT, 4.0F, 1.0F);
        }

        @Override
        void finish() {
            super.finish();
        }
    }

    private static final class ThrowingSpell extends Spell {
        ThrowingSpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) { super(entity, type); }

        @Override
        void start(EntityLivingBase target) {
            discardProjectiles(projectiles);
        }

        @Override
        void tick(EntityLivingBase target) {
            int difficulty = Math.max(1, entity.world.getDifficulty().getId());
            int divisor = Math.max(2, entity.getRNG().nextInt(Math.max(1, 24 / difficulty)));
            if (entity.ticksExisted % divisor == 0) {
                Entity projectile = makeProjectile(entity.getRNG().nextInt(5), entity.getRNG().nextInt(8),
                        entity.getRNG().nextInt(12));
                projectile.setNoGravity(true);
                projectile.noClip = true;
                projectile.setPosition(entity.posX + entity.getRNG().nextGaussian() * 5.0D,
                        entity.posY + entity.getRNG().nextDouble() * 10.0D + 2.0D,
                        entity.posZ + entity.getRNG().nextGaussian() * 5.0D);
                entity.world.spawnEntity(projectile);
                projectiles.add(projectile);
            }

            for (Entity projectile : projectiles) {
                if (projectile.isDead) continue;
                projectileCommandParticle(projectile, 0.5D);
                EntityLivingBase chosen = randomPlayerOrFallback(target);
                if (chosen == null) continue;
                if (projectile instanceof EntityArrow && projectile.hasNoGravity()) {
                    Vec3d drift = chosen.getPositionVector().subtract(projectile.getPositionVector())
                            .normalize().scale(0.005D);
                    projectile.motionX = drift.x;
                    projectile.motionY = drift.y;
                    projectile.motionZ = drift.z;
                }
                if (projectile.ticksExisted == 40) {
                    double x = chosen.posX - projectile.posX;
                    double y = chosen.posY + chosen.height / 3.0D - projectile.posY;
                    double z = chosen.posZ - projectile.posZ;
                    double horizontal = Math.sqrt(x * x + z * z);
                    projectile.setNoGravity(false);
                    projectile.noClip = false;
                    projectile.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 4.0F, 1.0F);
                    float velocity = projectile instanceof EntityPotion
                            ? (float) (0.75D + horizontal * 0.02D)
                            : (float) (1.6D + horizontal * 0.02D);
                    shoot(projectile, x, y + horizontal * 0.2D, z, velocity,
                            projectile instanceof EntityPotion ? 8.0F
                                    : 14.0F - entity.world.getDifficulty().getId() * 4.0F);
                }
            }
        }

        private Entity makeProjectile(int randomProjectile, int randomPotion, int randomArrow) {
            if (randomProjectile == 0) {
                ItemStack potion = new ItemStack(Items.SPLASH_POTION);
                PotionUtils.addPotionToItemStack(potion, PotionTypes.EMPTY);
                PotionEffect effect = getPotion(randomPotion);
                if (effect != null) PotionUtils.appendEffects(potion, Collections.singletonList(effect));
                return new EntityPotion(entity.world, entity, potion);
            }
            if (randomProjectile == 1) return new EntitySnowball(entity.world, entity);
            if (randomProjectile == 3) return new EntitySpectralArrow(entity.world, entity);
            if (randomProjectile == 4) {
                Item tridentItem = ForgeRegistries.ITEMS.getValue(FUTURE_MC_TRIDENT);
                if (tridentItem != null) return new Trident(entity.world, entity, new ItemStack(tridentItem));
            }

            EntityTippedArrow arrow = new EntityTippedArrow(entity.world, entity);
            if (randomProjectile == 2 && randomArrow < 6) {
                ItemStack tipped = new ItemStack(Items.TIPPED_ARROW);
                PotionUtils.addPotionToItemStack(tipped, PotionTypes.EMPTY);
                PotionUtils.appendEffects(tipped, Collections.singletonList(getArrowTip(randomPotion)));
                arrow.setPotionEffect(tipped);
            }
            return arrow;
        }

        @Nullable
        private static PotionEffect getPotion(int value) {
            switch (value) {
                case 0: return new PotionEffect(MobEffects.WITHER, 60, 2);
                case 1: return new PotionEffect(MobEffects.MINING_FATIGUE, 400, 2);
                case 2: return new PotionEffect(MobEffects.HUNGER, 100, 1);
                case 3: return new PotionEffect(MobEffects.WEAKNESS, 800, 2);
                case 4: return new PotionEffect(MobEffects.BLINDNESS, 100, 1);
                case 5: return new PotionEffect(MobEffects.SLOWNESS, 40, 1);
                case 6: return new PotionEffect(MobEffects.BLINDNESS, 60);
                default: return null;
            }
        }

        private static PotionEffect getArrowTip(int value) {
            switch (value) {
                case 0: return new PotionEffect(MobEffects.BLINDNESS, 10, 5);
                case 1: return new PotionEffect(MobEffects.SLOWNESS, 5, 8);
                case 2: return new PotionEffect(MobEffects.WEAKNESS, 10, 1);
                case 3: return new PotionEffect(MobEffects.HUNGER, 10, 2);
                case 4: return new PotionEffect(MobEffects.MINING_FATIGUE, 10, 3);
                default: return new PotionEffect(MobEffects.NAUSEA, 10);
            }
        }

        @Override void cast(EntityLivingBase target) { }

        @Override
        void finish() {
            for (Entity projectile : new ArrayList<Entity>(projectiles)) {
                projectilePoofParticle(projectile, 0.5D);
                if (projectile != null && !projectile.isDead) projectile.setDead();
            }
            projectiles.clear();
        }
    }

    private static final class WitherSkullsSpell extends Spell {
        WitherSkullsSpell(SickenedEntities.WitheredSymbiontEntity entity, Type type) { super(entity, type); }
        @Override void cast(EntityLivingBase target) { }

        @Override
        void tick(EntityLivingBase target) {
            int interval = entity.shouldIncreaseDifficulty() ? 10 : 20;
            if (target == null || entity.ticksExisted % interval != 0) return;
            int elapsed = type.spellTime - entity.getSpellCastingTime();
            double radius = 2.0D;
            float angle = elapsed * 0.08F;
            float targetAngle = (float) Math.atan2(target.posX - entity.posX, target.posZ - entity.posZ);
            float offset = (float) Math.PI / 2.0F;
            double x = MathHelper.sin(offset - targetAngle) * radius
                    + MathHelper.sin(angle) * MathHelper.sin(offset * 2.0F - targetAngle) * radius + entity.posX;
            double y = entity.posY + MathHelper.cos(angle) * radius;
            double z = MathHelper.cos(offset - targetAngle) * radius
                    + MathHelper.sin(angle) * MathHelper.cos(offset * 2.0F - targetAngle) * radius + entity.posZ;
            Vec3d acceleration = new Vec3d(x, y, z).subtract(target.getPositionEyes(1.0F))
                    .normalize().scale(-2.5D);
            EntityWitherSkull skull = new EntityWitherSkull(entity.world, entity,
                    acceleration.x, acceleration.y, acceleration.z);
            skull.setPosition(x, y, z);
            if (entity.getRNG().nextInt(11) == 1) skull.setInvulnerable(true);
            entity.world.spawnEntity(skull);
            skull.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 4.0F, 1.0F);
            if (entity.world instanceof WorldServer) {
                ((WorldServer) entity.world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        x, y, z, 20, 0.25D, 0.25D, 0.25D, 0.01D);
                ModNetwork.sendCommandBlockParticles(entity.world, new Vec3d(x, y, z), 20,
                        entity.getRNG().nextGaussian(), entity.getRNG().nextGaussian(),
                        entity.getRNG().nextGaussian(), 0.2D,
                        ModNetwork.COMMAND_BLOCK_PARTICLES_GAUSSIAN);
            }
        }
    }

    static boolean isPulseThrowable(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() == Blocks.AIR || state.getMaterial().isLiquid()
                || UpstreamBlockTags.contains("minecraft:replaceable", state)) return false;
        return !UpstreamBlockTags.contains(UpstreamBlockTags.TAINTED_BLOCKS, state)
                && !UpstreamBlockTags.contains(UpstreamBlockTags.WITHER_STORM_BLOCK_BLACKLIST, state)
                && !UpstreamBlockTags.contains(UpstreamBlockTags.SMALL_CLUSTER_BLACKLIST, state)
                && !UpstreamBlockTags.contains("minecraft:beacon_base_blocks", state)
                && !UpstreamBlockTags.contains(UpstreamBlockTags.RED_SUPPORT_BASE, state)
                && !UpstreamBlockTags.contains(UpstreamBlockTags.GREEN_SUPPORT_BASE, state)
                && !UpstreamBlockTags.contains(UpstreamBlockTags.AQUA_SUPPORT_BASE, state)
                && !UpstreamBlockTags.contains(UpstreamBlockTags.GRAY_SUPPORT_BASE, state)
                && !UpstreamBlockTags.contains(UpstreamBlockTags.WITHERED_BEACON_BASE, state);
    }

    private static void shoot(Entity projectile, double x, double y, double z, float velocity, float inaccuracy) {
        if (projectile instanceof EntityArrow) {
            ((EntityArrow) projectile).shoot(x, y, z, velocity, inaccuracy);
        } else if (projectile instanceof EntityThrowable) {
            ((EntityThrowable) projectile).shoot(x, y, z, velocity, inaccuracy);
        }
        projectile.velocityChanged = true;
    }

    private static void discardProjectiles(List<Entity> projectiles) {
        for (Entity projectile : projectiles) {
            if (projectile != null && !projectile.isDead) projectile.setDead();
        }
        projectiles.clear();
    }
}
