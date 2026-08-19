package com.wdcftgg.witherstormmod.common.world;

import com.wdcftgg.witherstormmod.common.capability.WitherSicknessCapability;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessTracker;
import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.resource.UpstreamItemTags;
import com.wdcftgg.witherstormmod.common.util.EquipmentHelper;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

/** Bowels command block 的完整 19 阶段服务端状态机。 */
public final class BowelsBossfightController {
    private static final int PODIUM_MOVE_TICKS = 100;
    private static final double PODIUM_MOVE_PER_TICK = 0.05D;
    private static final double PODIUM_MOVE_HEIGHT = PODIUM_MOVE_TICKS * PODIUM_MOVE_PER_TICK;
    private static final int[] FIXED_PHASE_TICKS = {0, 60, 100, 20, 100, 0, 60, 100, 20, 100, 0, 0, 60, 100, 120, 0, 0, 0, 0};
    private static final Map<SupplementalEntities.CommandBlockEntity, Integer> INITIALIZED_PHASES =
            new WeakHashMap<SupplementalEntities.CommandBlockEntity, Integer>();

    private static final MobWeight[] WAVE_1 = {
            mob("zombie", 15), mob("skeleton", 10), mob("spider", 8), mob("creeper", 2),
            mob("villager", 1), mob("phantom", 1), mob("chicken", 6), mob("cow", 6),
            mob("mushroom_cow", 1), mob("pig", 6), mob("bee", 3), mob("parrot", 2),
            mob("wolf", 2), mob("cat", 2), mob("pillager", 4), mob("vindicator", 2)
    };
    private static final MobWeight[] WAVE_2 = {
            mob("zombie", 10), mob("skeleton", 10), mob("spider", 8), mob("creeper", 6),
            mob("iron_golem", 4), mob("villager", 4), mob("phantom", 2), mob("chicken", 4),
            mob("cow", 4), mob("mushroom_cow", 1), mob("pig", 4), mob("bee", 6),
            mob("parrot", 4), mob("wolf", 3), mob("cat", 3), mob("pillager", 8), mob("vindicator", 4)
    };
    private static final MobWeight[] WAVE_3 = {
            mob("zombie", 10), mob("skeleton", 10), mob("spider", 4), mob("creeper", 6),
            mob("iron_golem", 6), mob("villager", 6), mob("phantom", 4), mob("chicken", 1),
            mob("cow", 1), mob("mushroom_cow", 1), mob("pig", 1), mob("bee", 8),
            mob("parrot", 6), mob("wolf", 5), mob("cat", 5), mob("pillager", 10), mob("vindicator", 5)
    };
    private static final MobWeight[] IDLE_MOBS = {
            mob("zombie", 10), mob("skeleton", 10), mob("spider", 6), mob("creeper", 1),
            mob("chicken", 4), mob("cow", 4), mob("pig", 4), mob("parrot", 2),
            mob("wolf", 1), mob("cat", 1), mob("bee", 1), mob("pillager", 3),
            mob("vindicator", 1), mob("villager", 3)
    };

    private BowelsBossfightController() {
    }

    public static void tick(SupplementalEntities.CommandBlockEntity core) {
        if (core.world.isRemote || !(core.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) core.world;
        BowelsInstanceData data = BowelsInstanceData.get(world);
        BowelsInstanceData.Instance instance = data.findContaining(core.getPosition());
        if (instance == null || instance.completed) return;
        if (instance.commandBlockUuid == null) {
            instance.commandBlockUuid = core.getUniqueID();
            data.markDirty();
        } else if (!instance.commandBlockUuid.equals(core.getUniqueID())) {
            return;
        }
        core.applyBowelsPodiumLiftPose(getExpectedCoreY(instance));
        Integer initializedPhase = INITIALIZED_PHASES.get(core);
        if (initializedPhase == null || initializedPhase != instance.bossPhase) {
            initializePhase(world, core, instance, instance.bossPhase);
        }

        tickAmbientEffects(world, core);

        int phase = instance.bossPhase;
        int ticks = ++instance.bossPhaseTicks;
        tickPhase(world, core, instance, phase, ticks);

        if (isFixedPhase(phase) && ticks > FIXED_PHASE_TICKS[phase]) {
            advance(world, core, data, instance);
        } else if ((phase == 10 || phase == 15) && guardsDefeated(world, core)) {
            advance(world, core, data, instance);
        }
        if (ticks % 20 == 0) data.markDirty();
    }

    public static boolean attack(SupplementalEntities.CommandBlockEntity core, DamageSource source) {
        if (!isCommandBlockTool(source)) return false;
        core.playSound(ModSounds.get("command_block_hit"), 4.0F, 1.0F);
        if (core.world.isRemote || !(core.world instanceof WorldServer)) return false;
        WorldServer world = (WorldServer) core.world;
        BowelsInstanceData data = BowelsInstanceData.get(world);
        BowelsInstanceData.Instance instance = data.findContaining(core.getPosition());
        if (instance == null || instance.completed || core.isEntityInvulnerable(source)
                || (instance.commandBlockUuid != null
                && !instance.commandBlockUuid.equals(core.getUniqueID()))
                || !isVulnerablePhase(instance.bossPhase)) return false;

        float nextHealth = core.getHealth() - core.getMaxHealth() / 4.0F;
        boolean hurt;
        if (nextHealth <= 0.0F) {
            hurt = core.takeBowelsDamage(source, Float.MAX_VALUE);
        } else {
            core.setHealth(nextHealth);
            hurt = true;
        }
        if (!hurt) return false;

        // Striking the core is an explicit upstream hazard. Push the attacker
        // away from the command block after every accepted hit; relying on the
        // generic entity damage path loses this because the core damage is
        // handled by the phase controller above it.
        Entity attacker = source == null ? null : source.getTrueSource();
        if (attacker instanceof EntityLivingBase) {
            double dx = attacker.posX - core.posX;
            double dz = attacker.posZ - core.posZ;
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 1.0E-4D) length = 1.0D;
            ((EntityLivingBase) attacker).addVelocity(dx / length * 0.75D,
                    0.25D, dz / length * 0.75D);
            ((EntityLivingBase) attacker).velocityChanged = true;
        }

        ModNetwork.sendCommandBlockParticles(world,
                WorldUtil.centerOf(core.getEntityBoundingBox()), 100,
                0.0D, 0.0D, 0.0D, 1.0D,
                ModNetwork.COMMAND_BLOCK_PARTICLES_UNIFORM_VELOCITY);
        if (nextHealth > 0.0F) {
            advance(world, core, data, instance);
            WitherStormEntity storm = findStorm(world, instance.stormUuid);
            if (storm != null && !storm.isDead) {
                storm.reactToCommandBlockDamage(core.getRNG());
                core.playSound(ModSounds.get("command_block_damage"), 16.0F, 1.0F);
                core.playSound(ModSounds.get("command_block_cracks"), 16.0F, 1.0F);
            }
            core.triggerHitGlare();
        }
        return hurt;
    }

    public static boolean shouldShowBossBar(SupplementalEntities.CommandBlockEntity core) {
        if (core == null || core.world.isRemote || !(core.world instanceof WorldServer)) return false;
        BowelsInstanceData.Instance instance = BowelsInstanceData.get((WorldServer) core.world)
                .findContaining(core.getPosition());
        return instance != null && !instance.completed && !isIdlePhase(instance.bossPhase);
    }

    public static void beginDeath(SupplementalEntities.CommandBlockEntity core, DamageSource source) {
        if (core.world.isRemote || !(core.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) core.world;
        BowelsInstanceData data = BowelsInstanceData.get(world);
        BowelsInstanceData.Instance instance = data.findContaining(core.getPosition());
        if (instance == null || instance.completed || instance.bossPhase == 17) return;
        Entity killer = source == null ? null : source.getTrueSource();
        instance.killerUuid = killer == null ? null : killer.getUniqueID();
        finishPhase(world, core, instance, instance.bossPhase);
        instance.bossPhase = 17;
        instance.bossPhaseTicks = 0;
        initializePhase(world, core, instance, 17);
        data.markDirty();
    }

    public static void tickDeath(SupplementalEntities.CommandBlockEntity core, int deathTicks) {
        if (core.world.isRemote || !(core.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) core.world;
        BowelsInstanceData data = BowelsInstanceData.get(world);
        BowelsInstanceData.Instance instance = data.findContaining(core.getPosition());
        if (instance == null) {
            if (deathTicks > 240) core.setDead();
            return;
        }
        instance.bossPhaseTicks = deathTicks;
        if (deathTicks <= 160) {
            dropClusterFromCeiling(world, core);
        } else if (deathTicks == 161 && instance.bossPhase == 17) {
            resolveDeath(world, core, instance);
            INITIALIZED_PHASES.put(core, 18);
            data.markDirty();
        } else if (deathTicks > 240 && instance.bossPhase == 18) {
            completeDeath(world, core, instance);
        }
        if (deathTicks % 20 == 0) data.markDirty();
    }

    public static boolean isDeathPhase(SupplementalEntities.CommandBlockEntity core) {
        if (core == null || core.world.isRemote || !(core.world instanceof WorldServer)) return false;
        BowelsInstanceData.Instance instance = BowelsInstanceData.get((WorldServer) core.world)
                .findContaining(core.getPosition());
        return instance != null && !instance.completed && instance.bossPhase == 17;
    }

    /** Includes the post-resolution tail; upstream keeps the entity alive
     * until the full 240-tick command-block death sequence has elapsed. */
    public static boolean isDeathSequence(SupplementalEntities.CommandBlockEntity core) {
        if (core == null || core.world.isRemote || !(core.world instanceof WorldServer)) return false;
        BowelsInstanceData.Instance instance = BowelsInstanceData.get((WorldServer) core.world)
                .findContaining(core.getPosition());
        return instance != null && !instance.completed
                && (instance.bossPhase == 17 || instance.bossPhase == 18);
    }

    public static void restoreLoadedPhase(SupplementalEntities.CommandBlockEntity core) {
        if (core == null || core.world.isRemote || !(core.world instanceof WorldServer)
                || !core.isIndependentBowelsPart()) return;
        WorldServer world = (WorldServer) core.world;
        BowelsInstanceData.Instance instance = BowelsInstanceData.get(world)
                .findContaining(core.getPosition());
        if (instance == null || instance.completed) return;
        if (instance.commandBlockUuid != null
                && !instance.commandBlockUuid.equals(core.getUniqueID())) return;
        core.applyBowelsPodiumLiftPose(getExpectedCoreY(instance));
        initializePhase(world, core, instance, instance.bossPhase);
    }

    public static void finishDeathRemoval(SupplementalEntities.CommandBlockEntity core) {
        if (core == null || core.world.isRemote || !(core.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) core.world;
        BowelsInstanceData.Instance instance = BowelsInstanceData.get(world)
                .findContaining(core.getPosition());
        if (instance == null || instance.completed || instance.bossPhase != 18) return;
        completeDeath(world, core, instance);
        INITIALIZED_PHASES.remove(core);
    }

    /** Repairs worlds completed by builds where the terminal 1.12 damage was rejected. */
    public static boolean reconcileCompletedDeath(WitherStormEntity storm) {
        if (storm == null || storm.world.isRemote || storm.isDead || storm.getHealth() <= 0.0F
                || storm.world.getMinecraftServer() == null) return false;
        WorldServer bowels = storm.world.getMinecraftServer().getWorld(BowelsDimensions.DIMENSION_ID);
        if (bowels == null) return false;
        BowelsInstanceData.Instance instance = BowelsInstanceData.get(bowels).get(storm.getUniqueID());
        if (instance == null || !instance.completed) return false;
        storm.finishBowelsDeath(findEntity(bowels, instance.killerUuid));
        return storm.isDead || storm.getHealth() <= 0.0F;
    }

    private static void advance(WorldServer world, SupplementalEntities.CommandBlockEntity core,
                                BowelsInstanceData data, BowelsInstanceData.Instance instance) {
        finishPhase(world, core, instance, instance.bossPhase);
        instance.bossPhase = Math.min(18, instance.bossPhase + 1);
        instance.bossPhaseTicks = 0;
        initializePhase(world, core, instance, instance.bossPhase);
        data.markDirty();
    }

    private static void initializePhase(WorldServer world,
                                        SupplementalEntities.CommandBlockEntity core,
                                        BowelsInstanceData.Instance instance, int phase) {
        initPhase(world, core, instance, phase);
        INITIALIZED_PHASES.put(core, phase);
    }

    private static void initPhase(WorldServer world, SupplementalEntities.CommandBlockEntity core,
                                  BowelsInstanceData.Instance instance, int phase) {
        switch (phase) {
            case 1:
            case 6:
            case 12:
                ModNetwork.shakeTracking(core, 240.0F, 12.0F);
                core.awakenStructureTentacles(false);
                play(world, core, "loud_tremble", SoundCategory.AMBIENT, 1.0F);
                play(world, core, "bowels_loud_hurt", SoundCategory.HOSTILE, 1.0F);
                if (core.getHealth() / core.getMaxHealth() >= 0.75F) {
                    play(world, core, "wither_storm_reactivates", SoundCategory.HOSTILE, 64.0F);
                }
                break;
            case 2:
            case 7:
            case 13:
                ModNetwork.shakeTracking(core, 120.0F, 12.0F);
                core.createPodiumCluster();
                play(world, core, "loud_tremble", SoundCategory.AMBIENT, 1.0F);
                break;
            case 4:
                activateWave(world, core, 1, 60);
                break;
            case 9:
                ModNetwork.shakeTracking(core, 120.0F, 8.0F);
                activateWave(world, core, 2, 60);
                break;
            case 10:
                core.curlStructureTentacles(false);
                break;
            case 14:
                ModNetwork.shakeTracking(core, 120.0F, 16.0F);
                activateWave(world, core, 3, 80);
                activateHeads(world, core);
                break;
            case 15:
                core.curlStructureTentacles(false);
                break;
            case 17:
                ModNetwork.shakeTracking(core, 240.0F, 14.0F);
                // Keep the command-block break flash readable without hiding
                // the HUD for the entire death transition.
                ModNetwork.blindTracking(core, 80, 20, 40);
                play(world, core, "loud_tremble", SoundCategory.AMBIENT, 5.0F);
                play(world, core, "bowels_loud_hurt", SoundCategory.HOSTILE, 5.0F);
                play(world, core, "command_block_destruct", SoundCategory.HOSTILE, 64.0F);
                for (SickenedEntities.TentacleEntity tentacle : world.getEntitiesWithinAABB(SickenedEntities.TentacleEntity.class,
                        core.getEntityBoundingBox().grow(50.0D))) {
                    tentacle.doIndefiniteAwakeAnimation();
                    tentacle.setCanSwing(false);
                    tentacle.setCanStrangle(false);
                }
                for (SupplementalEntities.WitherStormHeadEntity head : world.getEntitiesWithinAABB(SupplementalEntities.WitherStormHeadEntity.class,
                        core.getEntityBoundingBox().grow(50.0D))) head.setDead();
                for (SickenedMobEntity mob : world.getEntitiesWithinAABB(SickenedMobEntity.class,
                        core.getEntityBoundingBox().grow(50.0D))) {
                    if (mob != core && !(mob instanceof SickenedEntities.TentacleEntity)) mob.setDead();
                }
                break;
            default:
                break;
        }
    }

    private static void tickPhase(WorldServer world, SupplementalEntities.CommandBlockEntity core,
                                  BowelsInstanceData.Instance instance, int phase, int ticks) {
        if (phase == 2 || phase == 7 || phase == 13) {
            // Keep the core and the captured podium on one authoritative
            // absolute height.  Using Entity.move() for the core and motionY
            // for the cluster lets 1.12 collision resolution stop the core on
            // later lifts (most visibly the third hit).  The upstream pair is
            // visually rigid, so both positions must be derived from the same
            // phase clock every tick.
            core.applyBowelsPodiumLiftPose(getExpectedCoreY(instance, phase, ticks));
        } else if (phase == 4 && ticks % 8 == 0) {
            spawnWaveMob(world, core, WAVE_1, 2.0D);
        } else if (phase == 9 && ticks % 10 == 0) {
            spawnWaveMob(world, core, WAVE_2, 4.0D);
        } else if (phase == 14 && ticks % 5 == 0) {
            spawnWaveMob(world, core, WAVE_3, 8.0D);
        } else if ((phase == 10 || phase == 15) && ticks % 40 == 0) {
            core.curlStructureTentacles(true);
        }
    }

    private static void activateWave(WorldServer world, SupplementalEntities.CommandBlockEntity core, int wave, int particleCount) {
        play(world, core, "command_block_activates", SoundCategory.HOSTILE, wave == 3 ? 6.0F : 5.0F);
        ModNetwork.sendCommandBlockParticles(world,
                new Vec3d(core.posX, core.posY + core.getEyeHeight(), core.posZ), particleCount,
                core.getRNG().nextGaussian(), core.getRNG().nextGaussian(),
                core.getRNG().nextGaussian(), 0.2D,
                ModNetwork.COMMAND_BLOCK_PARTICLES_GAUSSIAN);
        if (wave > 1) awakenNearbyTentacles(world, core);
    }

    private static void finishPhase(WorldServer world, SupplementalEntities.CommandBlockEntity core,
                                    BowelsInstanceData.Instance instance, int phase) {
        if (phase == 2 || phase == 7 || phase == 13) {
            core.finishPodiumMove(getExpectedCoreY(instance, phase, PODIUM_MOVE_TICKS));
        } else if (phase == 4 || phase == 9) {
            play(world, core, "command_block_power_down", SoundCategory.HOSTILE, 5.0F);
            if (phase == 9) spawnRushSymbiont(world, core);
        } else if (phase == 14) {
            play(world, core, "command_block_power_down", SoundCategory.HOSTILE, 6.0F);
        } else if (phase == 10 || phase == 15) {
            core.stopCurlingStructureTentacles();
        }
    }

    private static void awakenNearbyTentacles(WorldServer world, SupplementalEntities.CommandBlockEntity core) {
        for (SickenedEntities.TentacleEntity tentacle : world.getEntitiesWithinAABB(SickenedEntities.TentacleEntity.class,
                core.getEntityBoundingBox().grow(50.0D))) {
            tentacle.setDormant(false);
            tentacle.doAwakeAnimation();
        }
    }

    private static void activateHeads(WorldServer world, SupplementalEntities.CommandBlockEntity core) {
        for (SupplementalEntities.WitherStormHeadEntity head : world.getEntitiesWithinAABB(SupplementalEntities.WitherStormHeadEntity.class,
                core.getEntityBoundingBox().grow(50.0D))) {
            head.setActive(true);
            head.setRoar(false);
            head.setRoarTime(40);
        }
    }

    private static void spawnWaveMob(WorldServer world, SupplementalEntities.CommandBlockEntity core,
                                     MobWeight[] weights, double healthBonus) {
        SickenedMobEntity mob = createMob(world, choose(weights, core.getRNG()));
        if (mob == null) return;
        BlockPos pos = randomNearbyPosition(world, core, mob, 50, 5);
        if (pos == null) {
            mob.setDead();
            return;
        }
        mob.setPosition(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        DifficultyInstance difficulty = world.getDifficultyForLocation(pos);
        if (!ForgeEventFactory.doSpecialSpawn(mob, world, (float) mob.posX,
                (float) mob.posY, (float) mob.posZ)) {
            mob.onInitialSpawn(difficulty, null);
        }
        reduceWaveSpeed(mob);
        applyAttributeModifier(mob, SharedMonsterAttributes.MAX_HEALTH,
                "194fec31-b36e-41fc-ad72-02a5cb891def",
                -(mob.getRNG().nextDouble() + 0.5D) * 2.0D, 0);
        mob.playLivingSound();
        mob.spawnExplosionParticle();
        mob.enablePersistence();
        spawnMobParticles(world, core, mob, 20);
        world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, mob.posX,
                mob.posY + mob.getEyeHeight(), mob.posZ,
                20, core.getRNG().nextGaussian(), core.getRNG().nextGaussian(),
                core.getRNG().nextGaussian(), 0.01D);
        if (!world.spawnEntity(mob)) return;
        applyAttributeModifier(mob, SharedMonsterAttributes.MAX_HEALTH,
                "Extra health final bossfight", healthBonus, 0);
        if (EquipmentHelper.canWearArmor(mob)
                && (healthBonus > 2.0D || core.getRNG().nextDouble() >= 0.5D)) {
            EquipmentHelper.applyEquipment(mob, difficulty, healthBonus >= 8.0D);
        }
    }

    private static void reduceWaveSpeed(SickenedMobEntity mob) {
        double reduction = 0.0D;
        if (mob instanceof SickenedEntities.SickenedVindicatorEntity
                || mob instanceof SickenedEntities.SickenedIronGolemEntity) {
            reduction = 0.08D;
        } else if (mob instanceof SickenedEntities.SickenedZombieEntity
                || mob instanceof SickenedEntities.SickenedSkeletonEntity
                || mob instanceof SickenedEntities.SickenedSpiderEntity
                || mob instanceof SickenedEntities.SickenedCreeperEntity
                || mob instanceof SickenedEntities.SickenedPillagerEntity) {
            reduction = 0.06D;
        }
        if (reduction > 0.0D) {
            applyAttributeModifier(mob, SharedMonsterAttributes.MOVEMENT_SPEED,
                    "5965c24d-8ac1-4f04-92ee-3d2724f976e8", -reduction, 0);
        }
    }

    private static void spawnRushSymbiont(WorldServer world, SupplementalEntities.CommandBlockEntity core) {
        SickenedEntities.WitheredSymbiontEntity symbiont =
                new SickenedEntities.WitheredSymbiontEntity(world);
        BlockPos pos = randomNearbyPosition(world, core, symbiont, 50, 20);
        if (pos == null) {
            symbiont.setDead();
            return;
        }
        symbiont.setPosition(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        symbiont.onInitialSpawn(world.getDifficultyForLocation(pos), null);
        if (!world.spawnEntity(symbiont)) return;
        symbiont.setNonBossMode(true);
        symbiont.setRushMode(true);
        applyAttributeModifier(symbiont, SharedMonsterAttributes.MAX_HEALTH,
                "Withered symbiont final boss battle low health", -0.5D, 1);
        symbiont.setHealth(symbiont.getMaxHealth());
        symbiont.enablePersistence();
        spawnMobParticles(world, core, symbiont, 40);
        world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, symbiont.posX,
                symbiont.posY + symbiont.getEyeHeight(), symbiont.posZ,
                40, core.getRNG().nextGaussian(), core.getRNG().nextGaussian(),
                core.getRNG().nextGaussian(), 0.01D);
        symbiont.playSound(ModSounds.get("withered_symbiont_spawn"), 4.0F, 1.0F);
    }

    private static void spawnMobParticles(WorldServer world,
                                          SupplementalEntities.CommandBlockEntity core,
                                          SickenedMobEntity mob, int count) {
        ModNetwork.sendCommandBlockParticles(world,
                new Vec3d(mob.posX, mob.posY + mob.getEyeHeight(), mob.posZ), count,
                core.getRNG().nextGaussian(), core.getRNG().nextGaussian(),
                core.getRNG().nextGaussian(), 0.2D,
                ModNetwork.COMMAND_BLOCK_PARTICLES_GAUSSIAN);
    }

    private static boolean guardsDefeated(WorldServer world, SupplementalEntities.CommandBlockEntity core) {
        if (!world.isAreaLoaded(core.getPosition(), 2)) return false;
        AxisAlignedBB area = core.getEntityBoundingBox().grow(50.0D);
        for (SickenedEntities.WitheredSymbiontEntity symbiont : world.getEntitiesWithinAABB(SickenedEntities.WitheredSymbiontEntity.class, area)) {
            if (!symbiont.isDead && symbiont.isEntityAlive()) return false;
        }
        for (SupplementalEntities.WitherStormHeadEntity head : world.getEntitiesWithinAABB(SupplementalEntities.WitherStormHeadEntity.class, area)) {
            if (head.isEntityAlive() && !head.isPlayingDead() && !head.isHurt()) return false;
        }
        return true;
    }

    private static void tickAmbientEffects(WorldServer world,
                                           SupplementalEntities.CommandBlockEntity core) {
        float healthRatio = core.getHealth() / Math.max(1.0F, core.getMaxHealth());
        if (core.hasTrackingPlayers() && core.getHealth() < core.getMaxHealth()) {
            int nearbyInterval = (int) (healthRatio * 80.0F);
            if (nearbyInterval > 0 && core.ticksExisted % nearbyInterval == 0) {
                dropClusterFromCeiling(world, core);
            }
            int distantInterval = (int) (healthRatio * 16.0F);
            if (distantInterval > 0 && core.ticksExisted % distantInterval == 0) {
                dropDistantClusterFromCeiling(world, core);
            }
        }
        if (world.provider.getDimension() == BowelsDimensions.DIMENSION_ID
                && core.getCoreState() == SupplementalEntities.CommandBlockEntity.CoreState.BOSSFIGHT
                && world.isAreaLoaded(core.getPosition(), 8)
                && core.ticksExisted % 100 == 0 && core.getRNG().nextDouble() <= 0.25D) {
            spawnIdleMobNearPlayer(world, core);
        }
    }

    private static void dropClusterFromCeiling(WorldServer world,
                                               SupplementalEntities.CommandBlockEntity core) {
        if (!world.isAreaLoaded(core.getPosition(), 2)) return;
        float angle = (float) (Math.PI * 2.0D) * core.getRNG().nextFloat();
        float distance = 8.0F + core.getRNG().nextFloat() * 24.0F;
        int x = MathHelper.floor(MathHelper.sin(angle) * distance) + core.getPosition().getX();
        int z = MathHelper.floor(MathHelper.cos(angle) * distance) + core.getPosition().getZ();
        int y = WorldUtil.getCeilingStartingAt(world, core.getPosition().getY() + 10, x, z);
        spawnCeilingCluster(world, new BlockPos(x, y, z), core.getRNG());
    }

    private static void dropDistantClusterFromCeiling(WorldServer world,
                                                      SupplementalEntities.CommandBlockEntity core) {
        if (!world.isAreaLoaded(core.getPosition(), 8)) return;
        for (int attempt = 0; attempt < 128; attempt++) {
            float angle = (float) (Math.PI * 2.0D) * core.getRNG().nextFloat();
            float distance = 32.0F + core.getRNG().nextFloat() * 80.0F;
            int x = MathHelper.floor(MathHelper.sin(angle) * distance) + core.getPosition().getX();
            int z = MathHelper.floor(MathHelper.cos(angle) * distance) + core.getPosition().getZ();
            int startingY = core.getPosition().getY() + core.getRNG().nextInt(49) - 24;
            int y = WorldUtil.getCeilingStartingAt(world, startingY, x, z);
            if (spawnCeilingCluster(world, new BlockPos(x, y, z), core.getRNG())) break;
        }
    }

    private static boolean spawnCeilingCluster(WorldServer world, BlockPos position,
                                               java.util.Random random) {
        if (position.getY() >= world.getActualHeight() || position.getY() <= 0
                || !world.isBlockLoaded(position) || !world.isAirBlock(position.down())) return false;
        SupplementalEntities.BlockClusterEntity cluster =
                new SupplementalEntities.BlockClusterEntity(world);
        cluster.populateWithRadius(position, 1.0F,
                (clusterWorld, clusterPosition, state) -> !UpstreamBlockTags.contains(
                        UpstreamBlockTags.WITHER_STORM_BLOCK_BLACKLIST, state));
        if (cluster.getBlocks().isEmpty()) return false;
        cluster.setRotationDelta(10.0F * (random.nextFloat() - 0.5F),
                10.0F * (random.nextFloat() - 0.5F));
        cluster.setAntiStacking(true);
        return world.spawnEntity(cluster);
    }

    private static void spawnIdleMobNearPlayer(WorldServer world,
                                               SupplementalEntities.CommandBlockEntity core) {
        for (EntityPlayerMP player : playersNear(world, core, 128.0D)) {
            SickenedMobEntity mob = createMob(world, choose(IDLE_MOBS, core.getRNG()));
            if (mob == null) continue;
            BlockPos position = randomPositionAroundPlayer(world, player, mob,
                    core.getRNG(), 8, 16, 10);
            if (position == null) {
                mob.setDead();
                continue;
            }
            mob.setPosition(position.getX() + 0.5D, position.getY() + 1.0D,
                    position.getZ() + 0.5D);
            mob.onInitialSpawn(world.getDifficultyForLocation(position), null);
            reduceWaveSpeed(mob);
            applyAttributeModifier(mob, SharedMonsterAttributes.MAX_HEALTH,
                    "194fec31-b36e-41fc-ad72-02a5cb891def",
                    -(mob.getRNG().nextDouble() + 0.5D) * 2.0D, 0);
            mob.playLivingSound();
            mob.spawnExplosionParticle();
            spawnMobParticles(world, core, mob, 20);
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    mob.posX, mob.posY + mob.getEyeHeight(), mob.posZ,
                    20, core.getRNG().nextGaussian(), core.getRNG().nextGaussian(),
                    core.getRNG().nextGaussian(), 0.01D);
            if (world.spawnEntity(mob)) {
                return;
            }
        }
    }

    private static void resolveDeath(WorldServer bowels, SupplementalEntities.CommandBlockEntity core,
                                     BowelsInstanceData.Instance instance) {
        WitherStormEntity storm = findStorm(bowels, instance.stormUuid);
        Entity killer = findEntity(bowels, instance.killerUuid);
        if (storm != null && !storm.isDead) storm.finishBowelsDeath(killer);
        for (EntityPlayerMP player : playersNear(bowels, core, 150.0D)) {
            if (storm != null && player != killer) player.onKillEntity(storm);
            if (player.getSpawnDimension() == BowelsDimensions.DIMENSION_ID) {
                player.setSpawnChunk(null, false, BowelsDimensions.DIMENSION_ID);
                player.setSpawnDimension(null);
            }
            WitherSicknessTracker tracker = WitherSicknessCapability.get(player);
            if (tracker != null) tracker.cure();
            UUID ownerUuid = player.getUniqueID();
            List<EntityTameable> pets = new ArrayList<EntityTameable>(
                    bowels.getEntitiesWithinAABB(EntityTameable.class,
                    core.getEntityBoundingBox().grow(150.0D),
                    tameable -> ownerUuid.equals(tameable.getOwnerId())));
            for (EntityTameable pet : pets) {
                BowelsManager.leave(pet);
            }
            BowelsManager.leave(player);
            if (ModSounds.get("wither_storm_death") != null) {
                player.connection.sendPacket(new SPacketSoundEffect(
                        ModSounds.get("wither_storm_death"), SoundCategory.HOSTILE,
                        player.posX, player.posY, player.posZ, 1.0F, 1.0F));
            }
            placePlayerNearStorm(player, storm);
        }
        instance.bossPhase = 18;
        instance.bossPhaseTicks = 0;
    }

    private static void placePlayerNearStorm(EntityPlayerMP player, @Nullable WitherStormEntity storm) {
        if (player == null || storm == null || storm.isDead
                || player.dimension != storm.dimension || player.world != storm.world
                || !(storm.world instanceof WorldServer)) return;
        WorldServer stormWorld = (WorldServer) storm.world;
        for (int attempt = 0; attempt < 10; attempt++) {
            float angle = (storm.renderYawOffset + 90.0F) * 0.017453292F;
            double x = MathHelper.sin(angle) * 100.0D + storm.posX
                    + storm.getRNG().nextGaussian() * 5.0D;
            double z = MathHelper.cos(angle) * 100.0D + storm.posZ
                    + storm.getRNG().nextGaussian() * 5.0D;
            BlockPos floor = stormWorld.getHeight(new BlockPos(x, 0.0D, z)).down();
            if (!stormWorld.isSideSolid(floor, EnumFacing.UP)) continue;
            double finalX = floor.getX() + 0.5D;
            double finalY = floor.getY() + 1.0D;
            double finalZ = floor.getZ() + 0.5D;
            double deltaX = storm.posX - finalX;
            double deltaY = storm.posY + storm.getEyeHeight()
                    - (finalY + player.getEyeHeight());
            double deltaZ = storm.posZ - finalZ;
            double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            float yaw = (float) (MathHelper.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - 90.0F;
            float pitch = (float) (-(MathHelper.atan2(deltaY, horizontal) * 180.0D / Math.PI));
            player.connection.setPlayerLocation(finalX, finalY, finalZ, yaw, pitch);
            return;
        }
    }

    private static void completeDeath(WorldServer world, SupplementalEntities.CommandBlockEntity core,
                                      BowelsInstanceData.Instance instance) {
        if (instance.completed) return;
        core.removeStructureTentacles();
        core.setDead();
        instance.completed = true;
        ChunkLoadingManager.INSTANCE.releaseBowelsInstance(world, instance.stormUuid);
        BowelsInstanceData.get(world).markDirty();
    }

    @Nullable
    private static WitherStormEntity findStorm(WorldServer world, UUID uuid) {
        Entity entity = findEntity(world, uuid);
        return entity instanceof WitherStormEntity ? (WitherStormEntity) entity : null;
    }

    @Nullable
    private static Entity findEntity(WorldServer world, @Nullable UUID uuid) {
        if (uuid == null) return null;
        if (world.getMinecraftServer() == null) return null;
        for (WorldServer level : world.getMinecraftServer().worlds) {
            if (level == null) continue;
            Entity entity = level.getEntityFromUuid(uuid);
            if (entity != null) return entity;
        }
        return null;
    }

    private static List<EntityPlayerMP> playersNear(WorldServer world, SupplementalEntities.CommandBlockEntity core, double radius) {
        List<EntityPlayerMP> result = new ArrayList<EntityPlayerMP>();
        for (EntityPlayerMP player : world.getEntitiesWithinAABB(EntityPlayerMP.class,
                core.getEntityBoundingBox().grow(radius))) result.add(player);
        return result;
    }

    @Nullable
    private static BlockPos randomNearbyPosition(WorldServer world,
                                                 SupplementalEntities.CommandBlockEntity core,
                                                 Entity entity, int diameter, int attempts) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = core.getPosition().getX() + core.getRNG().nextInt(diameter) - diameter / 2;
            int z = core.getPosition().getZ() + core.getRNG().nextInt(diameter) - diameter / 2;
            // Upstream samples the MOTION_BLOCKING_NO_LEAVES heightmap directly;
            // starting at the core Y and walking downward selects a different
            // cave floor in tall arenas.
            BlockPos cursor = new BlockPos(x,
                    WorldUtil.getMotionBlockingHeightIgnoringLeaves(world, x, z), z);
            if (!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(
                    EntityLiving.SpawnPlacementType.ON_GROUND, world, cursor)) continue;
            if (Math.sqrt(cursor.distanceSq(core.getPosition())) <= 6.0D) continue;
            if (!hasEnoughSpace(world, entity, cursor)) continue;
            return cursor;
        }
        return null;
    }

    @Nullable
    private static BlockPos randomPositionAroundPlayer(WorldServer world, EntityPlayerMP player,
                                                       Entity entity, java.util.Random random, int minimumDistance,
                                                       int maximumDistance, int attempts) {
        BlockPos playerPosition = player.getPosition();
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = playerPosition.getX() + random.nextInt(maximumDistance * 2 + 1)
                    - maximumDistance;
            int y = playerPosition.getY() + random.nextInt(maximumDistance);
            int z = playerPosition.getZ() + random.nextInt(maximumDistance * 2 + 1)
                    - maximumDistance;
            BlockPos cursor = new BlockPos(x, y, z);
            double distanceSquared = playerPosition.distanceSq(cursor);
            if (distanceSquared < minimumDistance * minimumDistance
                    || distanceSquared > maximumDistance * maximumDistance) continue;
            for (int down = 0; down < 30 && world.isAirBlock(cursor.down()); down++) {
                cursor = cursor.down();
            }
            if (WorldEntitySpawner.canCreatureTypeSpawnAtLocation(
                    EntityLiving.SpawnPlacementType.ON_GROUND, world, cursor)
                    && Math.sqrt(playerPosition.distanceSq(cursor)) > 6.0D
                    && hasEnoughSpace(world, entity, cursor)) return cursor;
        }
        return null;
    }

    private static boolean hasEnoughSpace(WorldServer world, Entity entity, BlockPos spawnPosition) {
        BlockPos size = new BlockPos(entity.width, entity.height, entity.width);
        for (BlockPos position : BlockPos.getAllInBox(spawnPosition, spawnPosition.add(size))) {
            IBlockState state = world.getBlockState(position);
            if (state.getCollisionBoundingBox(world, position) != Block.NULL_AABB) return false;
        }
        return true;
    }

    private static void applyAttributeModifier(EntityLivingBase entity,
                                               IAttribute attribute,
                                               String name, double amount, int operation) {
        IAttributeInstance instance = entity.getEntityAttribute(attribute);
        if (instance != null) instance.applyModifier(new AttributeModifier(name, amount, operation));
    }

    private static boolean isCommandBlockTool(DamageSource source) {
        if (!(source.getTrueSource() instanceof EntityLivingBase)) return false;
        EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
        return UpstreamItemTags.contains(UpstreamItemTags.COMMAND_BLOCK_TOOLS,
                attacker.getHeldItemMainhand());
    }

    private static boolean isVulnerablePhase(int phase) {
        return phase == 0 || phase == 5 || phase == 11 || phase == 16;
    }

    private static boolean isIdlePhase(int phase) {
        return phase == 0 || phase == 5 || phase == 11 || phase == 16 || phase == 18;
    }

    private static boolean isFixedPhase(int phase) {
        return phase > 0 && phase < FIXED_PHASE_TICKS.length && FIXED_PHASE_TICKS[phase] > 0;
    }

    static double getExpectedCoreY(BowelsInstanceData.Instance instance) {
        return getExpectedCoreY(instance, instance.bossPhase, instance.bossPhaseTicks);
    }

    private static double getExpectedCoreY(BowelsInstanceData.Instance instance, int phase, int ticks) {
        int completedMoves = phase >= 14 ? 3 : phase >= 8 ? 2 : phase >= 3 ? 1 : 0;
        double height = instance.getArenaPosition().getY() + completedMoves * PODIUM_MOVE_HEIGHT;
        if (phase == 2 || phase == 7 || phase == 13) {
            height += MathHelper.clamp(ticks, 0, PODIUM_MOVE_TICKS) * PODIUM_MOVE_PER_TICK;
        }
        return height;
    }

    private static void play(WorldServer world, Entity core, String sound, SoundCategory category, float volume) {
        world.playSound(null, core.getPosition(), ModSounds.get(sound), category, volume, 1.0F);
    }

    private static void play(WorldServer world, SupplementalEntities.CommandBlockEntity core,
                             String sound, SoundCategory category, float volume) {
        play(world, (Entity) core, sound, category, volume);
    }

    private static MobWeight mob(String name, int weight) {
        return new MobWeight(name, weight);
    }

    @Nullable
    private static SickenedMobEntity createMob(WorldServer world, @Nullable MobWeight selected) {
        if (selected == null) return null;
        if ("zombie".equals(selected.name)) return new SickenedEntities.SickenedZombieEntity(world);
        if ("skeleton".equals(selected.name)) return new SickenedEntities.SickenedSkeletonEntity(world);
        if ("spider".equals(selected.name)) return new SickenedEntities.SickenedSpiderEntity(world);
        if ("creeper".equals(selected.name)) return new SickenedEntities.SickenedCreeperEntity(world);
        if ("iron_golem".equals(selected.name)) return new SickenedEntities.SickenedIronGolemEntity(world);
        if ("villager".equals(selected.name)) return new SickenedEntities.SickenedVillagerEntity(world);
        if ("phantom".equals(selected.name)) return new SickenedEntities.SickenedPhantomEntity(world);
        if ("chicken".equals(selected.name)) return new SickenedEntities.SickenedChickenEntity(world);
        if ("cow".equals(selected.name)) return new SickenedEntities.SickenedCowEntity(world);
        if ("mushroom_cow".equals(selected.name)) return new SickenedEntities.SickenedMushroomCowEntity(world);
        if ("pig".equals(selected.name)) return new SickenedEntities.SickenedPigEntity(world);
        if ("bee".equals(selected.name)) return new SickenedEntities.SickenedBeeEntity(world);
        if ("parrot".equals(selected.name)) return new SickenedEntities.SickenedParrotEntity(world);
        if ("wolf".equals(selected.name)) return new SickenedEntities.SickenedWolfEntity(world);
        if ("cat".equals(selected.name)) return new SickenedEntities.SickenedCatEntity(world);
        if ("pillager".equals(selected.name)) return new SickenedEntities.SickenedPillagerEntity(world);
        if ("vindicator".equals(selected.name)) return new SickenedEntities.SickenedVindicatorEntity(world);
        return null;
    }

    @Nullable
    private static MobWeight choose(MobWeight[] values, java.util.Random random) {
        int total = 0;
        for (MobWeight value : values) total += value.weight;
        int selected = random.nextInt(total);
        for (MobWeight value : values) {
            selected -= value.weight;
            if (selected < 0) return value;
        }
        return values[values.length - 1];
    }

    private static final class MobWeight {
        private final String name;
        private final int weight;

        private MobWeight(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }
    }
}
