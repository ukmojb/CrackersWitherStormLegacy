package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.api.common.event.WitherStormChangePhaseEvent;
import com.wdcftgg.witherstormmod.api.common.event.WitherStormConsumeEvent;
import com.wdcftgg.witherstormmod.api.common.event.WitherStormEvolveEvent;
import com.wdcftgg.witherstormmod.api.common.event.WitherStormModifyEvolutionSpeedEvent;
import com.wdcftgg.witherstormmod.api.common.event.WitherStormModifyFlyingSpeedEvent;
import com.wdcftgg.witherstormmod.api.common.entity.WitherStormBase;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.advancement.ModCriteriaTriggers;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessCapability;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessTracker;
import com.wdcftgg.witherstormmod.common.init.ModDamageSources;
import com.wdcftgg.witherstormmod.common.init.ModAttributes;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityLlamaSpit;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.potion.PotionEffect;
import net.minecraft.init.MobEffects;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.BlockJukebox;
import net.minecraft.util.Rotation;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import com.wdcftgg.witherstormmod.common.world.StructureTemplates;
import com.wdcftgg.witherstormmod.common.world.BowelsBossfightController;
import com.wdcftgg.witherstormmod.common.world.BowelsManager;
import com.wdcftgg.witherstormmod.common.world.BowelsDimensions;
import com.wdcftgg.witherstormmod.common.world.BowelsInstanceData;
import com.wdcftgg.witherstormmod.common.world.ChunkLoadingManager;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.init.ModBlocks;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.resource.UpstreamBlockTags;
import com.wdcftgg.witherstormmod.common.resource.WitherStormBlockRules;
import com.wdcftgg.witherstormmod.common.resource.UpstreamEntityTags;
import com.wdcftgg.witherstormmod.common.resource.UpstreamItemTags;
import com.wdcftgg.witherstormmod.common.taint.TaintingManager;
import com.wdcftgg.witherstormmod.common.util.DebrisCluster;
import com.wdcftgg.witherstormmod.common.util.DebrisRingSettings;
import com.wdcftgg.witherstormmod.common.util.EvolutionProfiler;
import com.wdcftgg.witherstormmod.common.util.WorldUtil;
import com.wdcftgg.witherstormmod.common.util.BossVisibility;
import com.wdcftgg.witherstormmod.common.util.TractorBeamHelper;
import com.wdcftgg.witherstormmod.common.advancement.EntityTrigger;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import thedarkcolour.futuremc.entity.trident.Trident;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Random;
import javax.annotation.Nullable;

public class WitherStormEntity extends EntityMob
        implements BossThemeProvider, TractorBeamProvider, WitherStormBase, DistantStormPart {

    public enum PlayDeadState { NORMAL_BEHAVIOR, FALLING, PLAYING_DEAD, REVIVING }

    private static final DataParameter<Integer> PHASE = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> CONSUMED_MASS = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> PLAY_DEAD_STATE = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> PLAY_DEAD_STATE_TICKS = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> INVULNERABLE_TICKS = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> STARTING_INVULNERABLE_TICKS = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> SHOULD_SHOW_HOLE = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> OTHER_HEADS_DISABLED = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> HEAD_ANIMATION_FLAGS = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Float> BODY_X_ROTATION = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> BODY_Y_ROTATION = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> HOLE_ENABLED = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> FIRST_HEAD_TARGET = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> SECOND_HEAD_TARGET = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> THIRD_HEAD_TARGET = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> HEAD_INJURY_FLAGS = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Float> FIRST_HEAD_YAW = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SECOND_HEAD_YAW = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> THIRD_HEAD_YAW = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FIRST_HEAD_PITCH = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SECOND_HEAD_PITCH = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> THIRD_HEAD_PITCH = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> HEAD_DISTRACTION_FLAGS = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> MIRRORED = EntityDataManager.createKey(WitherStormEntity.class, DataSerializers.BOOLEAN);
    private static final String EVOLUTION_SPEED_NBT_KEY = "WitherStormEvolutionSpeedModifier";
    private static final double DEFAULT_EVOLUTION_SPEED_MODIFIER = 1.0D;
    private static final double RESUMMONED_EVOLUTION_SPEED_MODIFIER = 0.5D;
    private static final UUID RESUMMONED_EVOLUTION_MODIFIER_ID = UUID.fromString(
            "238f7f93-cad8-42ea-a503-7f2e18a9eb2d");
    private static final double BASE_MAX_HEALTH = 400.0D;
    private static final double PHASE_MAX_HEALTH_BONUS = 624.0D;
    private static final double BASE_ARMOR = 8.0D;
    private static final UUID PHASE_HEALTH_MODIFIER_UUID = UUID.fromString("9B8DA22B-138B-4B68-879D-3FD329FAF903");
    private static final UUID PHASE_ARMOR_MODIFIER_UUID = UUID.fromString("C806DBFA-2B10-4BEA-B16C-C3233707399C");
    private static final float[] PHASE_WIDTH = {0.9F, 0.9F, 0.9F, 0.9F, 10.0F, 10.0F, 15.0F, 15.0F};
    private static final float[] PHASE_HEIGHT = {3.5F, 3.5F, 3.5F, 3.5F, 30.0F, 60.0F, 90.0F, 120.0F};
    private static final int SEGMENT_RESTORE_GRACE_TICKS = 200;
    private final UUID[] segmentUuids = new UUID[2];
    private final int[] missingSegmentTicks = new int[2];
    private final WitherStormHeadManager headManager = new WitherStormHeadManager(this);
    private final UltimateTargetManager targetManager = new UltimateTargetManager(this);
    private final SymbiontSummoningManager summoningManager = new SymbiontSummoningManager(this);
    private final WitherStormClusterManager clusterManager = new WitherStormClusterManager(this);
    private final WitherStormSectionManager sectionManager = new WitherStormSectionManager(this);
    private final IgnoredTargetsManager ignoredTargetsManager = new IgnoredTargetsManager(this);
    private final EvolutionProfiler evolutionProfiler = new EvolutionProfiler();
    private final WitherStormPulling.Source trackedEntityPullSource = new WitherStormPulling.Source() {
        @Override public WitherStormEntity getStorm() { return WitherStormEntity.this; }
        @Override public int getPhase() { return WitherStormEntity.this.getPhase(); }
        @Override public float getWidth() { return WitherStormEntity.this.width; }
        @Override public Vec3d getEyePosition() { return WitherStormEntity.this.getPositionEyes(1.0F); }
        @Override public BlockPos getBlockPosition() { return WitherStormEntity.this.getPosition(); }
        @Override public boolean isTractorBeamActive(int head) {
            return WitherStormEntity.this.tractorBeamActive(head);
        }
        @Override public Vec3d getHeadPosition(int head) {
            return WitherStormEntity.this.getHeadPosition(head, 1.0F);
        }
        @Override public Vec3d getHeadDirection(int head) {
            return WitherStormEntity.this.headManager.getLookVector(head);
        }
        @Override public double getTractorBeamCutoff(int head) {
            return WitherStormEntity.this.headManager.getTractorBeamCutoff(head);
        }
    };
    private final BossInfoServer legacyBossInfo = (BossInfoServer) new BossInfoServer(
            getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS).setDarkenSky(true);
    private UUID commandBlockUuid;
    private SupplementalEntities.CommandBlockEntity playingDeadCommandBlock;
    private UUID formidibombUuid;
    private PowerfulExplosiveEntity.FormidibombEntity formidibomb;
    private BlockPos podiumPosition;
    private boolean podiumPlaced;
    private boolean podiumOffsetCorrected = true;
    private final Map<UUID, NBTTagCompound> consumedPets = new LinkedHashMap<UUID, NBTTagCompound>();
    private final Map<UUID, Entity> trackedEntities = new LinkedHashMap<UUID, Entity>();
    private final List<UUID> savedTrackedEntities = new ArrayList<UUID>();
    private final Set<UUID> pendingBowelsTransfers = new HashSet<UUID>();
    private final Set<EntityPlayerMP> trackingPlayers = new HashSet<EntityPlayerMP>();
    private final Map<UUID, Boolean> bossThemeAccess = new LinkedHashMap<UUID, Boolean>();
    private final List<BlockPos> playingJukeboxes = new ArrayList<BlockPos>();
    private List<DebrisCluster> debrisClusters = Collections.emptyList();
    private List<DebrisCluster> hunchbackDebrisClusters = Collections.emptyList();
    private List<DebrisRingSettings> debrisRings = Collections.emptyList();
    private int stateTicks;
    private int missingCommandBlockTicks;
    private int recentlyRevivedTicks;
    private int trackedEntityTicks;
    private int destroyBlocksTick;
    private int terrainDestructionCooldown;
    /** Prevents the landing impact from being emitted more than once per fall. */
    private boolean fallingImpactHandled;
    private int witherStormDeathTime;
    private int lastConsumedMass;
    private int entityConsumptionRadius = 16;
    private int lastFlyingHeightChange;
    private int nextUndergroundRumble = 1200 + rand.nextInt(1200);
    private int clientVisualStateTicks;
    private int flickerTime;
    private int nextFlicker = 40;
    private int tentacleTickCount;
    /** 性能优化：牵引光束候选实体每 5 tick 全量扫描一次，tick 间仅对缓存候选施力。 */
    private final List<Entity> tractorBeamCandidates = new ArrayList<Entity>();
    /** 性能优化：质量吸收候选与附近玩家，与牵引候选共用每 tick 单次实体遍历。 */
    private final List<Entity> absorbCandidates = new ArrayList<Entity>();
    private final List<EntityPlayerMP> nearbyPlayers = new ArrayList<EntityPlayerMP>();
    /** 性能优化：同 tick 内缓存头部视线射线结果（检测机制不变）。 */
    private long pullSightCacheCycle = Long.MIN_VALUE;
    private final java.util.Map<String, Boolean> pullSightCache =
            new java.util.HashMap<String, Boolean>();
    /** 诊断：风暴服务端 tick 分段耗时统计（每 200 tick 输出一次平均耗时）。 */
    private final java.util.LinkedHashMap<String, Long> profileNanos =
            new java.util.LinkedHashMap<String, Long>();
    private final java.util.LinkedHashMap<String, Integer> profileCounts =
            new java.util.LinkedHashMap<String, Integer>();
    private String profileSection;
    private long profileSectionStart;
    private int previousTentacleTickCount;
    private double currentFlyingHeight = 10.0D;
    private boolean deathRewardsReleased;
    private boolean deathLootReleased;
    private boolean resummoned;
    private boolean restoredFromPersistentData;
    private boolean consumptionLocked;
    private boolean attractingFormidibomb;
    private float bodyXRotation;
    private float previousBodyXRotation;
    private float clientBodyXRotationTarget;
    private float clientBodyYRotationTarget;
    private int clientBodyXRotationSteps;
    private float onGroundAnimation;
    private float previousOnGroundAnimation;
    private PlayDeadState clientVisualState;
    private float shineAlpha;
    private float previousShineAlpha;
    private float shineScale;
    private boolean shouldFlicker;
    private boolean soundLoopActive;
    private boolean completedBowelsDeathChecked;
    public boolean shouldPlaySoundLoop = true;
    public boolean shouldPlayGlobalSounds = true;

    public WitherStormEntity(World worldIn) {
        super(worldIn);
        forceSpawn = true;
        experienceValue = 10000;
        isImmuneToFire = true;
        stepHeight = 0.0F;
        setNoGravity(true);
        enablePersistence();
    }

    /** The 1.20 entity type uses the hostile sound source for entity-owned sounds. */
    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    public void ignite() {
        makeInvulnerable();
    }

    public void makeInvulnerable() {
        int duration = Math.max(1, WitherStormConfig.invulnerabilityTime * 20);
        dataManager.set(STARTING_INVULNERABLE_TICKS, duration);
        dataManager.set(INVULNERABLE_TICKS, duration);
        setHealth(1.0F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(PHASE, 0);
        dataManager.register(CONSUMED_MASS, 0);
        dataManager.register(PLAY_DEAD_STATE, PlayDeadState.NORMAL_BEHAVIOR.ordinal());
        dataManager.register(PLAY_DEAD_STATE_TICKS, 0);
        dataManager.register(INVULNERABLE_TICKS, 0);
        dataManager.register(STARTING_INVULNERABLE_TICKS, Math.max(1, WitherStormConfig.invulnerabilityTime * 20));
        dataManager.register(SHOULD_SHOW_HOLE, false);
        dataManager.register(OTHER_HEADS_DISABLED, false);
        dataManager.register(HEAD_ANIMATION_FLAGS, 0);
        dataManager.register(BODY_X_ROTATION, 0.0F);
        dataManager.register(BODY_Y_ROTATION, 0.0F);
        dataManager.register(HOLE_ENABLED, WitherStormConfig.shouldShowHole);
        dataManager.register(FIRST_HEAD_TARGET, 0);
        dataManager.register(SECOND_HEAD_TARGET, 0);
        dataManager.register(THIRD_HEAD_TARGET, 0);
        dataManager.register(HEAD_INJURY_FLAGS, 0);
        dataManager.register(FIRST_HEAD_YAW, 0.0F);
        dataManager.register(SECOND_HEAD_YAW, 0.0F);
        dataManager.register(THIRD_HEAD_YAW, 0.0F);
        dataManager.register(FIRST_HEAD_PITCH, 0.0F);
        dataManager.register(SECOND_HEAD_PITCH, 0.0F);
        dataManager.register(THIRD_HEAD_PITCH, 0.0F);
        dataManager.register(HEAD_DISTRACTION_FLAGS, 0);
        dataManager.register(MIRRORED, false);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote) {
            if (evolutionProfiler.isProfiling()) evolutionProfiler.tick(this);
            synchronizeSoundLoop();
        }
        previousTentacleTickCount = tentacleTickCount;
        if (!isDeadOrPlayingDead()) ++tentacleTickCount;
        previousOnGroundAnimation = onGroundAnimation;
        onGroundAnimation = WitherStormPartLogic.advanceFade(onGroundAnimation,
                onGround && isDeadOrPlayingDead(), rand);
    }

    /** 上游主实体不使用原版凋灵 AI，所有目标和攻击都由移植状态机管理。 */
    @Override
    protected void initEntityAI() {
    }

    @Override
    protected void updateAITasks() {
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(ModAttributes.TARGET_STATIONARY_FLYING_SPEED);
        getAttributeMap().registerAttribute(ModAttributes.SLOW_FLYING_SPEED);
        getAttributeMap().registerAttribute(ModAttributes.EVOLUTION_SPEED)
                .setBaseValue(DEFAULT_EVOLUTION_SPEED_MODIFIER);
        getAttributeMap().registerAttribute(ModAttributes.HUNCHBACK_FOLLOW_RANGE)
                .setBaseValue(40.0D);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(BASE_MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(BASE_ARMOR);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(120.0D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.5D);
    }

    @Override
    public void onLivingUpdate() {
        if (!world.isRemote && !completedBowelsDeathChecked && getPhase() >= 7) {
            completedBowelsDeathChecked = true;
            if (BowelsBossfightController.reconcileCompletedDeath(this)) return;
        }
        tickFlicker();
        if (!world.isRemote) {
            resolveSavedTrackedEntities();
            ignoredTargetsManager.tick();
        }
        if (world.isRemote) {
            updateClientBodyRotation();
            updateClientDebris();
        }
        if (!world.isRemote && dataManager.get(HOLE_ENABLED) != WitherStormConfig.shouldShowHole) {
            dataManager.set(HOLE_ENABLED, WitherStormConfig.shouldShowHole);
        }
        if (!world.isRemote && getInvulnerableTicks() > 0) {
            int remaining = getInvulnerableTicks() - 1;
            dataManager.set(INVULNERABLE_TICKS, remaining);
            int startingTicks = Math.max(1, getStartingInvulnerableTicks());
            if (ticksExisted % 10 == 0) {
                heal((getMaxHealth() - 1.0F) / startingTicks * 10.0F);
            }
            headManager.tickWithoutLookAi();
            updateBodyRotation();
            sectionManager.tick();
            updateAttachedEntities();
            updateLegacyBossInfo();
            spawnStormParticles();
            if (remaining <= 0) {
                world.newExplosion(this, posX, posY, posZ, 7.0F, false,
                        ForgeEventFactory.getMobGriefingEvent(world, this));
                world.playEvent(1023, getPosition(), 0);
            }
            return;
        }
        if (!world.isRemote && isPlayDeadAiDisabled()) {
            clearTrackedEntities(false);
            headManager.tick();
            updatePlayDeadState();
            updateBodyRotation();
            sectionManager.tick();
            updateAttachedEntities();
            updateLegacyBossInfo();
            return;
        }
        if (!world.isRemote && isEntityAlive()) {
            profileStart("formidibomb");
            updateFormidibombTarget();
            profileEnd();
            profileStart("target");
            targetManager.tick();
            profileEnd();
            profileStart("summon");
            summoningManager.tick();
            profileEnd();
            profileStart("movement");
            updateCustomMovement();
            profileEnd();
            searchForPlayingJukeboxes();
            prunePlayingJukeboxes();
            updateFlickerTrigger();
        }
        profileStart("vanillaTick");
        super.onLivingUpdate();
        profileEnd();
        spawnStormParticles();
        if (!world.isRemote && isEntityAlive()) tickTerrainDestruction();
        profileStart("heads");
        headManager.tick();
        profileEnd();
        profileStart("sections");
        sectionManager.tick();
        profileEnd();
        if (world.isRemote || !isEntityAlive()) return;
        profileStart("formidibombPull");
        pullFormidibombTowardMainHead();
        profileEnd();
        if (getPlayDeadState() == PlayDeadState.REVIVING) updatePlayDeadState();
        updateBodyRotation();
        if (recentlyRevivedTicks > 0
                && ++recentlyRevivedTicks > Math.max(0, WitherStormConfig.revivalPlayerProtection) * 1200) {
            recentlyRevivedTicks = 0;
        }
        updateEvolution();
        updateAttachedEntities();
        tickDelayedBlockDestruction();
        profileStart("fallingBlocks");
        convertFallingBlocks();
        profileEnd();
        profileStart("massAbsorption");
        refreshEntityCandidates();
        applyMassAbsorption();
        profileEnd();
        profileStart("tractorBeam");
        applyTractorBeam();
        profileEnd();
        tickProjectilesHittingHeads();
        profileStart("tracked");
        tickTrackedEntities();
        profileEnd();
        profileStart("clusters");
        clusterManager.tick();
        profileEnd();
        clusterManager.createCollisionClusters();
        healWhileCompletelyInvulnerable();
        updateCaveRumbles();
        profileStart("bossInfo");
        updateLegacyBossInfo();
        profileEnd();
        dumpProfileIfDue();
    }

    private void profileStart(String name) {
        profileSection = name;
        profileSectionStart = System.nanoTime();
    }

    private void profileEnd() {
        if (profileSection == null) return;
        long elapsed = System.nanoTime() - profileSectionStart;
        Long total = profileNanos.get(profileSection);
        profileNanos.put(profileSection, total == null ? elapsed : total + elapsed);
        Integer count = profileCounts.get(profileSection);
        profileCounts.put(profileSection, count == null ? 1 : count + 1);
        profileSection = null;
    }

    private void dumpProfileIfDue() {
        if (ticksExisted % 200 != 0 || profileNanos.isEmpty()) return;
        StringBuilder builder = new StringBuilder("WitherStorm tick profile (tick=")
                .append(ticksExisted).append("):");
        for (java.util.Map.Entry<String, Long> entry : profileNanos.entrySet()) {
            int count = profileCounts.containsKey(entry.getKey())
                    ? profileCounts.get(entry.getKey()) : 1;
            builder.append(' ').append(entry.getKey()).append('=')
                    .append(String.format(java.util.Locale.ROOT, "%.3f",
                            entry.getValue() / 1000000.0D / count)).append("ms");
        }
        WitherStormMod.LOGGER.info(builder.toString());
        profileNanos.clear();
        profileCounts.clear();
    }

    private void healWhileCompletelyInvulnerable() {
        if (WitherStormConfig.witherStormInvulnerability && ticksExisted % 20 == 0) {
            heal(10.0F);
        }
    }

    private void updateCaveRumbles() {
        if (!WitherStormConfig.caveRumbles || nextUndergroundRumble <= 0) return;
        --nextUndergroundRumble;
        if (nextUndergroundRumble > 0) return;

        if (getPhase() > 3 && world instanceof WorldServer) {
            for (EntityPlayerMP player : world.getEntitiesWithinAABB(EntityPlayerMP.class,
                    getSearchBox().grow(50.0D))) {
                if (!canSeeOrIsInOpenArea(player)) {
                    CaveRumbleManager.trigger((WorldServer) world, player,
                            WitherStormConfig.caveRumbleIntensity, rand);
                }
            }
        }

        if (WitherStormConfig.chanceForExtendedRumbles && rand.nextInt(3) != 0) {
            nextUndergroundRumble = 100 + rand.nextInt(60);
        } else {
            int minimum = Math.max(1, WitherStormConfig.caveRumbleIntervalMin * 20);
            int maximum = Math.max(minimum, WitherStormConfig.caveRumbleIntervalMax * 20);
            nextUndergroundRumble = minimum + (maximum > minimum ? rand.nextInt(maximum - minimum) : 0);
        }
    }

    private void updateFlickerTrigger() {
        SupplementalEntities.CommandBlockEntity commandBlock = getBowelsCommandBlock();
        if (commandBlock == null || commandBlock.getHealth() >= commandBlock.getMaxHealth()
                || nextFlicker <= 0) return;
        --nextFlicker;
        if (nextFlicker > 0) return;
        doFlicker();
        float healthRatio = commandBlock.getHealth() / Math.max(1.0F, commandBlock.getMaxHealth());
        nextFlicker = (int) (rand.nextInt(40) + 60.0F * Math.max(0.2F, healthRatio));
    }

    private void tickFlicker() {
        if (flickerTime <= 0) return;
        --flickerTime;
        shouldFlicker = MathHelper.cos(flickerTime + rand.nextInt(20))
                * MathHelper.sin(flickerTime + 30 + rand.nextInt(20)) < -0.5F;
        if (flickerTime == 0) shouldFlicker = false;
    }

    public void doFlicker() {
        flickerTime = 60;
        if (!world.isRemote) world.setEntityState(this, (byte) 11);
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 11) {
            flickerTime = 60;
        } else {
            super.handleStatusUpdate(id);
        }
    }

    public boolean shouldFlicker() {
        return shouldFlicker;
    }

    private void updateClientDebris() {
        previousShineAlpha = shineAlpha;
        ensureDebrisInitialized(isDeadOrPlayingDead());

        PlayDeadState state = getPlayDeadState();
        if (clientVisualState != state) {
            clientVisualState = state;
            clientVisualStateTicks = Math.max(0, dataManager.get(PLAY_DEAD_STATE_TICKS));
        }
        ++clientVisualStateTicks;

        for (DebrisCluster cluster : getDebrisClusters()) {
            if (!cluster.isDisabled()) cluster.tick();
        }

        if (state == PlayDeadState.NORMAL_BEHAVIOR) {
            float alpha = Math.min(clientVisualStateTicks, 80) / 80.0F;
            setDebrisAlpha(alpha);
            if (clientVisualStateTicks % 10 == 0) restoreRandomDebris();
        } else if (state == PlayDeadState.FALLING) {
            float alpha = (120.0F - Math.min(clientVisualStateTicks, 120)) / 120.0F;
            setDebrisAlpha(alpha);
            if (clientVisualStateTicks % 5 == 0) disableRandomDebris();
        } else if (state == PlayDeadState.PLAYING_DEAD) {
            setDebrisAlpha(0.0F);
            if (clientVisualStateTicks % 5 == 0) disableRandomDebris();
        } else {
            setDebrisAlpha(0.0F);
            if (clientVisualStateTicks % 10 == 0) restoreRandomDebris();
        }

        if (shouldShine()) {
            float factor = Math.max(0.1F, getPhase() - getPhaseProgress());
            shineScale = getUnmodifiedHeight() * (10.0F / factor);
        } else {
            shineScale = getUnmodifiedHeight();
        }
    }

    private void setDebrisAlpha(float alpha) {
        for (DebrisRingSettings settings : debrisRings) settings.setAlpha(alpha);
        shineAlpha = MathHelper.clamp(alpha, 0.0F, 1.0F);
    }

    private void disableRandomDebris() {
        List<DebrisCluster> clusters = getDebrisClusters();
        if (clusters.isEmpty()) return;
        int attempts = Math.max(10, rand.nextInt(15));
        for (int attempt = 0; attempt < attempts; attempt++) {
            for (DebrisCluster cluster : clusters) {
                if (!cluster.isDisabled() && rand.nextInt(clusters.size()) == 0) {
                    cluster.setDisabled(true);
                }
            }
        }
    }

    private void restoreRandomDebris() {
        List<DebrisCluster> clusters = getDebrisClusters();
        if (clusters.isEmpty()) return;
        int attempts = Math.max(10, rand.nextInt(15));
        for (int attempt = 0; attempt < attempts; attempt++) {
            for (DebrisCluster cluster : clusters) {
                if (cluster.isDisabled() && rand.nextInt(clusters.size()) == 0) {
                    cluster.setDisabled(false);
                }
            }
        }
    }

    public void createDebrisClusters(boolean hidden) {
        List<DebrisCluster> largeClusters = new ArrayList<DebrisCluster>();
        for (int index = 0; index < 100; index++) {
            DebrisCluster cluster = new DebrisCluster(rand.nextFloat() * 360.0F,
                    rand.nextFloat() * 100.0F, 40.0F + rand.nextFloat() * 160.0F,
                    rand.nextFloat() * 2.0F - 1.0F, 1.0F);
            cluster.randomize(rand, 15, 18.0F);
            cluster.setDisabled(hidden);
            cluster.determineRenderPhase();
            largeClusters.add(cluster);
        }

        List<DebrisCluster> smallClusters = new ArrayList<DebrisCluster>();
        addDebrisClusters(smallClusters, 30, hidden, 3.0F, 1.25F, 0.75F,
                8.0F, 0.125F, 1, 0.1F, 1, false);
        addDebrisClusters(smallClusters, 20, hidden, 4.0F, 2.0F, 5.0F,
                12.0F, 0.25F, 1, 0.5F, 2, false);
        addDebrisClusters(smallClusters, 5, hidden, 4.0F, 3.75F, 1.25F,
                6.0F, 0.125F, 3, 0.5F, 2, true);

        for (int index = 0; index < 25; index++) {
            DebrisCluster cluster = new DebrisCluster(rand.nextFloat() * 360.0F,
                    rand.nextFloat() * 20.0F - 10.0F, rand.nextFloat() * 20.0F + 5.0F,
                    rand.nextFloat() * 6.0F - 3.0F, 0.25F);
            cluster.randomize(rand, 4, 2.5F);
            cluster.setGlowing(false);
            cluster.setDisabled(hidden);
            cluster.setRenderPhase(3);
            smallClusters.add(cluster);
        }

        for (int index = 0; index < 5; index++) {
            DebrisCluster cluster = new DebrisCluster(rand.nextFloat() * 360.0F,
                    rand.nextFloat() * 15.0F - 5.0F, rand.nextFloat() * 7.5F + 20.0F,
                    rand.nextFloat() * 2.0F - 1.0F, 0.125F);
            cluster.randomize(rand, 8, 1.0F);
            cluster.setGlowing(true);
            cluster.setDisabled(hidden);
            cluster.setRenderPhase(3);
            smallClusters.add(cluster);
        }

        for (int index = 0; index < 15; index++) {
            DebrisCluster cluster = new DebrisCluster(rand.nextFloat() * 360.0F,
                    rand.nextFloat() * 10.0F - 5.0F, rand.nextFloat() * 24.0F + 6.0F,
                    rand.nextFloat() * 5.0F - 2.5F, 1.0F);
            cluster.randomize(rand, 1, 0.5F);
            cluster.setGlowing(false);
            cluster.setDisabled(hidden);
            cluster.setRenderPhase(3);
            smallClusters.add(cluster);
        }

        debrisClusters = Collections.unmodifiableList(largeClusters);
        hunchbackDebrisClusters = Collections.unmodifiableList(smallClusters);
    }

    private void addDebrisClusters(List<DebrisCluster> target, int count, boolean hidden,
                                   float verticalRange, float radiusBase, float radiusRange,
                                   float speedRange, float sizeModifier, int pieceCount,
                                   float spread, int renderPhase, boolean glowing) {
        for (int index = 0; index < count; index++) {
            DebrisCluster cluster = new DebrisCluster(rand.nextFloat() * 360.0F,
                    rand.nextFloat() * verticalRange, rand.nextFloat() * radiusRange + radiusBase,
                    rand.nextFloat() * speedRange - speedRange * 0.5F, sizeModifier);
            cluster.randomize(rand, pieceCount, spread);
            cluster.setDisabled(hidden);
            cluster.setGlowing(glowing);
            cluster.setRenderPhase(renderPhase);
            target.add(cluster);
        }
    }

    public void createDebrisRings(boolean hidden) {
        List<DebrisRingSettings> rings = new ArrayList<DebrisRingSettings>();
        rings.add(new DebrisRingSettings(16, 100.0F, 60.0F, 30.0F, 25.0F, 0.02F, true, 4, hidden));
        rings.add(new DebrisRingSettings(24, 160.0F, 120.0F, 10.0F, 50.0F, 0.005F, false, 4, hidden));
        rings.add(new DebrisRingSettings(24, 180.0F, 100.0F, 30.0F, 60.0F, 0.001F, true, 4, hidden));
        rings.add(new DebrisRingSettings(24, 130.0F, 50.0F, 80.0F, 10.0F, 0.008F, false, 4, hidden));
        rings.add(new DebrisRingSettings(36, 240.0F, 200.0F, 0.0F, 40.0F, 0.002F, true, 6, hidden));
        rings.add(new DebrisRingSettings(36, 250.0F, 210.0F, -30.0F, 10.0F, 0.001F, true, 6, hidden));
        debrisRings = Collections.unmodifiableList(rings);
    }

    public void ensureDebrisInitialized(boolean hidden) {
        if (!debrisClusters.isEmpty() && !hunchbackDebrisClusters.isEmpty() && !debrisRings.isEmpty()) return;
        createDebrisClusters(hidden);
        createDebrisRings(hidden);
    }

    public List<DebrisCluster> getDebrisClusters() {
        return getPhase() > 3 ? debrisClusters : hunchbackDebrisClusters;
    }

    public List<DebrisRingSettings> getDebrisRings() {
        return debrisRings;
    }

    public boolean shouldShine() {
        return getPhase() > 3;
    }

    public float getShineAlpha(float partialTicks) {
        return previousShineAlpha + (shineAlpha - previousShineAlpha) * partialTicks;
    }

    public float getShineScale() {
        return shineScale;
    }

    /** 维护风暴搜索范围内正在播放唱片的唱片机位置，供声音和分裂体表现使用。 */
    private void searchForPlayingJukeboxes() {
        AxisAlignedBB search = getSearchBox();
        for (TileEntity tile : world.loadedTileEntityList) {
            if (!(tile instanceof BlockJukebox.TileEntityJukebox)
                    || !search.contains(new Vec3d(tile.getPos()).add(0.5D, 0.5D, 0.5D))) continue;
            BlockJukebox.TileEntityJukebox jukebox = (BlockJukebox.TileEntityJukebox) tile;
            if (!jukebox.getRecord().isEmpty() && !playingJukeboxes.contains(tile.getPos())) {
                playingJukeboxes.add(tile.getPos());
            }
        }
    }

    private void prunePlayingJukeboxes() {
        Iterator<BlockPos> iterator = playingJukeboxes.iterator();
        AxisAlignedBB search = getSearchBox();
        while (iterator.hasNext()) {
            BlockPos position = iterator.next();
            if (!search.contains(new Vec3d(position).add(0.5D, 0.5D, 0.5D))) {
                iterator.remove();
                continue;
            }
            TileEntity tile = world.getTileEntity(position);
            if (!(tile instanceof BlockJukebox.TileEntityJukebox)
                    || ((BlockJukebox.TileEntityJukebox) tile).getRecord().isEmpty()) iterator.remove();
        }
    }

    public List<BlockPos> getPlayingJukeboxes() { return playingJukeboxes; }

    private void spawnStormParticles() {
        if (getPlayDeadState() == PlayDeadState.FALLING && !onGround) {
            double spread = getEntityBoundingBox().maxY - getEntityBoundingBox().minY;
            double offsetX = (rand.nextFloat() - 0.5F) * spread;
            double offsetY = (rand.nextFloat() - 0.5F) * spread;
            double offsetZ = (rand.nextFloat() - 0.5F) * spread;
            world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    posX + offsetX, posY + offsetY, posZ + offsetZ,
                    -5.0D, 0.0D, 0.0D);
        }
        if (getPhase() < 4) {
            for (int head = 0; head < getTotalHeads(); head++) {
                Vec3d position = getHeadPosition(head, 1.0F);
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                        position.x + rand.nextGaussian() * 0.3D,
                        position.y + rand.nextGaussian() * 0.3D,
                        position.z + rand.nextGaussian() * 0.3D, 0.0D, 0.0D, 0.0D);
                if (isArmored() && rand.nextInt(4) == 0) {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB,
                            position.x + rand.nextGaussian() * 0.3D,
                            position.y + rand.nextGaussian() * 0.3D,
                            position.z + rand.nextGaussian() * 0.3D, 0.7D, 0.7D, 0.5D);
                }
            }
        }
        if (getInvulnerableTicks() > 0) {
            for (int index = 0; index < 3; index++) {
                world.spawnParticle(EnumParticleTypes.SPELL_MOB,
                        posX + rand.nextGaussian(), posY + rand.nextFloat() * 3.3F,
                        posZ + rand.nextGaussian(), 0.7D, 0.7D, 0.9D);
            }
        }
    }

    /** 1.12 没有 Monster 飞行基类；直接移动可保留上游无重力速度模型。 */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        // 死亡移动由 onDeathUpdate 按上游的纵向阻尼与重力顺序执行，避免同一 tick 重复位移。
        if (getHealth() <= 0.0F) return;
        moveRelative(strafe, vertical, forward, 0.02F);
        move(MoverType.SELF, motionX, motionY, motionZ);
        prevLimbSwingAmount = limbSwingAmount;
        double movedX = posX - prevPosX;
        double movedZ = posZ - prevPosZ;
        float movement = Math.min(1.0F, MathHelper.sqrt(movedX * movedX + movedZ * movedZ) * 4.0F);
        limbSwingAmount += (movement - limbSwingAmount) * 0.4F;
        limbSwing += limbSwingAmount;
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        int damage = MathHelper.ceil((distance - 3.0F) * damageMultiplier);
        if (damage > 15) onBigFall();
    }

    @Override
    public void setInWeb() {
    }

    @Override
    public boolean isOnLadder() {
        return false;
    }

    @Override
    public boolean isPotionApplicable(PotionEffect effect) {
        return false;
    }

    @Override
    public boolean isImmuneToExplosions() {
        return true;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        // The upstream entity remains pickable. In 1.12.2 this method is also
        // used by the client entity ray trace; returning false prevents the
        // player attack packet from ever reaching attackEntityFrom(). Keep
        // canBePushed() false separately so the storm is still non-colliding
        // for movement physics.
        return true;
    }

    /**
     * 修复渲染剔除：RenderManager 用该包围盒做视锥判断，默认碰撞箱仅 15x120x15，
     * 玩家仰视/远距离时风暴模型（含高耸身体、碎片环）会整体被剔除。
     * 扩大范围以覆盖完整模型与碎片环（与 WitherStormRenderer.shouldRender 的 grow 一致）。
     */
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(260.0D, 220.0D, 260.0D);
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void doBlockCollisions() {
    }

    @Override
    public void applyEntityCollision(Entity entityIn) {
    }

    @Override
    public void addVelocity(double x, double y, double z) {
    }

    @Override
    public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) {
    }

    @Override
    protected boolean canBeRidden(Entity entityIn) {
        return false;
    }

    @Override
    public Entity changeDimension(int dimensionIn) {
        return null;
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return false;
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    public boolean isNonBoss() {
        return false;
    }

    /** 上游主体和分裂体使用 248 倍尺寸渲染距离，避免远距离部分突然消失。 */
    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return isInWitherStormRenderRange(this, distance);
    }

    static boolean isInWitherStormRenderRange(Entity entity, double distance) {
        double edgeLength = entity.getEntityBoundingBox().getAverageEdgeLength();
        if (Double.isNaN(edgeLength)) edgeLength = 1.0D;
        double renderDistance = edgeLength * 248.0D * Entity.getRenderDistanceWeight();
        return distance < renderDistance * renderDistance;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (isDeadOrPlayingDead()) return null;
        return getPhase() < 4 ? SoundEvents.ENTITY_WITHER_AMBIENT : ModSounds.get("wither_storm_ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return getPhase() < 4 ? SoundEvents.ENTITY_WITHER_HURT : ModSounds.get("wither_storm_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return getPhase() < 4 ? SoundEvents.ENTITY_WITHER_DEATH : null;
    }

    @Override
    public int getTalkInterval() {
        return getPhase() > 3 ? Math.max(80, rand.nextInt(120)) : super.getTalkInterval();
    }

    @Override
    protected float getSoundVolume() {
        return getPhase() > 3 ? 25.0F : super.getSoundVolume();
    }

    /** 大型阶段允许头部追踪更大的垂直角度，贴合上游的头部控制语义。 */
    @Override
    public int getVerticalFaceSpeed() {
        return getPhase() > 3 ? 180 : super.getVerticalFaceSpeed();
    }

    /** 牵引光束或受伤状态下减慢水平转头，避免目标瞬间跳转。 */
    @Override
    public int getHorizontalFaceSpeed() {
        if (getPhase() > 3 && !isHeadInjured(0) && !isHeadDistracted(0)) return 1;
        return isHeadDistracted(0) ? 2 : super.getHorizontalFaceSpeed();
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return 0;
    }

    private void updateEvolution() {
        int consumedMass = getConsumedMass();
        if (consumedMass == lastConsumedMass) return;
        lastConsumedMass = consumedMass;
        if (isDeadOrPlayingDead() || consumedMass <= getConsumptionAmountForPhase(getPhase())) return;
        // 上游每次只推进一个阶段，并把质量重置到新阶段的起始阈值。
        if (evolve(false)) lastConsumedMass = getConsumedMass();
    }

    /** Mirrors the upstream command hook after directly changing consumed entities. */
    public void checkConsumptionAmount() {
        updateEvolution();
    }

    private void accelerateAfterEvolution() {
        if (WitherStormConfig.chaseOnPhaseChange && getPhase() > 3) targetManager.accelerate();
    }

    /** 返回当前阶段是否允许进入下一阶段；阶段 5 只能通过 Formidibomb 倒地流程离开。 */
    public boolean canEvolve(boolean force) {
        int phase = getPhase();
        return phase < 7 && (force || evolutionProfiler.isProfiling() || phase != 5);
    }

    /** 尝试推进一个阶段，并执行上游进化后的追逐与全局音效。 */
    public boolean evolve(boolean force) {
        int nextPhase = getPhase() + 1;
        if (!canEvolve(force)
                || MinecraftForge.EVENT_BUS.post(new WitherStormEvolveEvent(this, nextPhase))
                || !setPhase(nextPhase)) return false;
        if (evolutionProfiler.isProfiling()) evolutionProfiler.onEvolve(this);
        accelerateAfterEvolution();
        if (shouldPlayGlobalSounds && nextPhase == 4) {
            playSoundToEveryone(ModSounds.get("wither_storm_evolves"), 1.0F, 1.0F);
        }
        return true;
    }

    /** 直接切换到指定阶段，使用该阶段前一阈值作为质量起点。 */
    public void evolveToPhase(int phase) {
        if (setPhase(phase)) {
            if (evolutionProfiler.isProfiling()) evolutionProfiler.onEvolve(this);
            accelerateAfterEvolution();
            if (shouldPlayGlobalSounds && phase == 4) {
                playSoundToEveryone(ModSounds.get("wither_storm_evolves"), 1.0F, 1.0F);
            }
        }
    }

    public boolean setPhase(int newPhase) {
        return setPhase(newPhase, newPhase == 0 ? 0 : getConsumptionAmountForPhase(newPhase - 1));
    }

    public boolean setPhase(int newPhase, int consumedMass) {
        if (newPhase < 0 || newPhase > 7) return false;
        MinecraftForge.EVENT_BUS.post(new WitherStormChangePhaseEvent(this, newPhase));
        boolean wasOtherHeadsDisabled = areOtherHeadsDisabled();
        dataManager.set(PHASE, newPhase);
        entityConsumptionRadius = newPhase > 3
                ? 80 : 12 + Math.round(getConsumedMass() * 0.00445F);
        dataManager.set(CONSUMED_MASS, consumedMass);
        boolean otherHeadsDisabled = newPhase == 6 && consumedMass < getSubPhaseRequirement(6);
        dataManager.set(OTHER_HEADS_DISABLED, otherHeadsDisabled);
        currentFlyingHeight = newPhase < 4 ? 10.0D : WitherStormConfig.flyingHeight;
        updatePhaseAttributes(newPhase);
        headManager.onPhaseChanged(newPhase);
        if (wasOtherHeadsDisabled && !otherHeadsDisabled) headManager.onOtherHeadsEnabled();
        updateSizeForPlayDeadState();
        if (newPhase < 6) removeSegments();
        synchronizeLoadedSegments();
        return true;
    }

    private void updatePhaseAttributes(int phase) {
        IAttributeInstance maxHealth = getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        IAttributeInstance armor = getEntityAttribute(SharedMonsterAttributes.ARMOR);
        maxHealth.setBaseValue(BASE_MAX_HEALTH);
        armor.setBaseValue(BASE_ARMOR);
        if (phase < 4) {
            maxHealth.removeModifier(PHASE_HEALTH_MODIFIER_UUID);
            armor.removeModifier(PHASE_ARMOR_MODIFIER_UUID);
            return;
        }
        if (maxHealth.getModifier(PHASE_HEALTH_MODIFIER_UUID) == null) {
            maxHealth.applyModifier(new AttributeModifier(PHASE_HEALTH_MODIFIER_UUID,
                    "Phase health modifier", PHASE_MAX_HEALTH_BONUS, 0));
        }
        if (armor.getModifier(PHASE_ARMOR_MODIFIER_UUID) == null) {
            armor.applyModifier(new AttributeModifier(PHASE_ARMOR_MODIFIER_UUID,
                    "Phase armor modifier", getInitialPhaseArmorBonus(phase), 0));
        }
    }

    static double getInitialPhaseArmorBonus(int phase) {
        return (phase + 1) * 2.0D;
    }

    /** Applies the distinct state used by the upstream super-beacon resurrection path. */
    public void initializeFromSuperBeacon(int phase) {
        applyResummonedEvolutionModifier();
        setPhase(MathHelper.clamp(phase, 0, 7));
        dataManager.set(INVULNERABLE_TICKS, 0);
        dataManager.set(STARTING_INVULNERABLE_TICKS, Math.max(1, WitherStormConfig.invulnerabilityTime * 20));
        recentlyRevivedTicks = 1;
        resummoned = true;
        dataManager.set(PLAY_DEAD_STATE, PlayDeadState.NORMAL_BEHAVIOR.ordinal());
        stateTicks = 0;
        dataManager.set(PLAY_DEAD_STATE_TICKS, 0);
        missingCommandBlockTicks = 0;
    }

    public int getSubPhaseRequirement(int phase) {
        int previous = phase == 0 ? 0 : getConsumptionAmountForPhase(phase - 1);
        return previous + (getConsumptionAmountForPhase(phase) - previous) / 2;
    }

    public int getPhaseRequirement() {
        return getConsumptionAmountForPhase(getPhase());
    }

    public int getConsumptionAmountForPhase(int phase) {
        if (phase < 0 || phase > 7) return 0;
        return adjustAmountForEvolutionSpeed(
                WitherStormConfig.getConfiguredPhaseRequirement(phase));
    }

    public int adjustAmountForEvolutionSpeed(int rawRequirement) {
        return scaleConsumptionRequirement(rawRequirement, getEvolutionSpeedModifier());
    }

    public double getEvolutionSpeedModifier() {
        double modifier = attributeOrConfigValue(ModAttributes.EVOLUTION_SPEED,
                WitherStormConfig.evolutionAttributeModifier);
        WitherStormModifyEvolutionSpeedEvent event =
                new WitherStormModifyEvolutionSpeedEvent(this, modifier);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getOriginalEvolutionSpeedModifier();
    }

    public int getPreviousPhaseRequirement() {
        return getPhase() == 0 ? 0 : getConsumptionAmountForPhase(getPhase() - 1);
    }

    static int scaleConsumptionRequirement(int rawRequirement, double modifier) {
        return (int) (rawRequirement * modifier);
    }

    static double readEvolutionSpeedModifier(NBTTagCompound compound, boolean isResummoned) {
        if (compound.hasKey(EVOLUTION_SPEED_NBT_KEY, 99)) {
            double modifier = compound.getDouble(EVOLUTION_SPEED_NBT_KEY);
            if (Double.isFinite(modifier) && modifier >= 0.0D) return modifier;
        }
        return isResummoned ? RESUMMONED_EVOLUTION_SPEED_MODIFIER : DEFAULT_EVOLUTION_SPEED_MODIFIER;
    }

    public float getPhaseProgress() {
        int previous = getPreviousPhaseRequirement();
        int required = getPhaseRequirement();
        return MathHelper.clamp((getConsumedMass() - previous) / (float) Math.max(1, required - previous), 0.0F, 1.0F);
    }

    public void onFormidibombExplosion() {
        if (!canBeFormidibombed(true)
                || isDeadOrPlayingDead()
                || getPlayDeadState() != PlayDeadState.NORMAL_BEHAVIOR) return;
        setPlayDeadState(PlayDeadState.FALLING);
        setAttackTarget(null);
        navigator.clearPath();
    }

    static boolean canStartFormidibombFall(int phase, boolean beingTornApart,
                                           PlayDeadState playDeadState) {
        return phase >= 5 && !(phase > 6 && beingTornApart)
                && playDeadState == PlayDeadState.NORMAL_BEHAVIOR;
    }

    public void reviveFromPlayingDead() {
        if (getPlayDeadState() == PlayDeadState.PLAYING_DEAD) setPlayDeadState(PlayDeadState.REVIVING);
    }

    private void setPlayDeadState(PlayDeadState state) {
        PlayDeadState previous = getPlayDeadState();
        if (previous == state) return;
        if (!world.isRemote && previous == PlayDeadState.NORMAL_BEHAVIOR) {
            clearMobTargets();
        }
        dataManager.set(PLAY_DEAD_STATE, state.ordinal());
        stateTicks = 0;
        dataManager.set(PLAY_DEAD_STATE_TICKS, 0);
        missingCommandBlockTicks = 0;
        dismountRidingEntity();
        updateSizeForPlayDeadState();
        legacyBossInfo.setVisible(!disablesAi(state));
        setNoGravity(!disablesAi(state));
        if (!world.isRemote && disablesAi(state)) stopAttractingFormidibomb();
        if (disablesAi(state) && !disablesAi(previous)) clearTrackedEntities(false);
        if (disablesAi(state)) recentlyRevivedTicks = 0;
        if (state == PlayDeadState.PLAYING_DEAD && getPhase() == 5) {
            setPhase(6);
            dataManager.set(OTHER_HEADS_DISABLED, true);
            ensureSegments();
        }
        if (state == PlayDeadState.REVIVING) {
            recentlyRevivedTicks = 1;
            playSoundToEveryone(ModSounds.get("wither_storm_reactivates"), 10.0F, 1.0F);
        }
        if (!world.isRemote && state == PlayDeadState.FALLING) {
            fallingImpactHandled = false;
            headManager.onStartFalling();
            triggerNearby(ModCriteriaTriggers.PLAY_DEAD, 100.0D);
        } else if (!world.isRemote && state == PlayDeadState.PLAYING_DEAD) {
            headManager.onStartPlayingDead();
        } else if (!world.isRemote && state == PlayDeadState.REVIVING
                && previous == PlayDeadState.PLAYING_DEAD) {
            headManager.onAiRestored();
            triggerNearby(ModCriteriaTriggers.REVIVAL, 100.0D);
        } else if (!world.isRemote && state == PlayDeadState.NORMAL_BEHAVIOR) {
            headManager.onAiRestored();
        }
        if (state == PlayDeadState.NORMAL_BEHAVIOR
                || previous == PlayDeadState.PLAYING_DEAD && state != PlayDeadState.REVIVING) {
            removeCommandBlockCore();
        }
        if (!world.isRemote) synchronizeLoadedSegments();
    }

    private void clearMobTargets() {
        for (EntityLiving mob : world.getEntitiesWithinAABB(EntityLiving.class, getSearchBox())) {
            if (mob.getAttackTarget() == this) mob.setAttackTarget(null);
        }
    }

    private void updatePlayDeadState() {
        ++stateTicks;
        if (stateTicks % 120 == 0) dataManager.set(PLAY_DEAD_STATE_TICKS, stateTicks);
        prevPosX = posX; prevPosY = posY; prevPosZ = posZ;
        if (getPlayDeadState() == PlayDeadState.FALLING) {
            tickFallingMovement(stateTicks <= 301);
            if (stateTicks % 8 == 0) spawnFallingDebris();
            if (getPhase() == 5 && stateTicks == 201) {
                setPhase(6);
                dataManager.set(OTHER_HEADS_DISABLED, true);
                ensureSegments();
                playSoundToEveryone(ModSounds.get("wither_storm_splits"), 1.0F, 1.0F);
            }
            if (onGround) {
                if (!fallingImpactHandled && getPhase() >= 5) {
                    fallingImpactHandled = true;
                    createFallingImpactCrater();
                }
                setPlayDeadState(PlayDeadState.PLAYING_DEAD);
            }
        } else if (getPlayDeadState() == PlayDeadState.PLAYING_DEAD) {
            motionX = motionY = motionZ = 0.0D;
            if (isOnBack()) {
                ensurePlayingDeadPodium();
                if (podiumPosition != null && !consumedPets.isEmpty()) {
                    spawnConsumedPets(new Vec3d(podiumPosition.getX() + 0.5D,
                            podiumPosition.getY() + 12.0D, podiumPosition.getZ() + 0.5D));
                }
            }
            SupplementalEntities.CommandBlockEntity core = getPlayingDeadCommandBlockReference();
            if (core != null && (core.isDead || getDistance(core) > 64.0F)) {
                ++missingCommandBlockTicks;
            }
            if (missingCommandBlockTicks > 200) setPlayDeadState(PlayDeadState.REVIVING);
        } else if (getPlayDeadState() == PlayDeadState.REVIVING && stateTicks > 20) {
            ModNetwork.shakeTracking(this, 60.0F, 4.0F);
            if (podiumPosition != null) {
                world.newExplosion(this, podiumPosition.getX(), podiumPosition.getY(),
                        podiumPosition.getZ(), 16.0F, false, false);
            }
            world.playSound(null, getPosition(), ModSounds.get("tremble"), SoundCategory.AMBIENT, 10.0F, 1.0F);
            setPlayDeadState(PlayDeadState.NORMAL_BEHAVIOR);
        }
    }

    /**
     * 上游会在坠落前 301 tick 将上一刻的纵向速度乘以 0.6，之后才保留普通重力累积。
     * 1.12 的飞行实现不会替本实体施加重力，因此在这里按原版空气移动顺序显式还原。
     */
    private void tickFallingMovement(boolean dampVerticalSpeed) {
        if (onGround) {
            motionX = motionY = motionZ = 0.0D;
            return;
        }
        if (dampVerticalSpeed) motionY *= 0.6D;
        move(MoverType.SELF, motionX, motionY, motionZ);
        motionX *= 0.91D;
        motionY = (motionY - 0.08D) * 0.98D;
        motionZ *= 0.91D;
    }

    /** The phase-five landing is a terrain event in the upstream fight. */
    private void createFallingImpactCrater() {
        if (world.isRemote || !ForgeEventFactory.getMobGriefingEvent(world, this)) return;
        // 坑洞以风暴宽度为基准并锚定地表，保证在巨大的主体下仍清晰可见。
        int radius = Math.max(8, MathHelper.ceil(width));
        int centerX = MathHelper.floor(posX);
        int centerZ = MathHelper.floor(posZ);
        int groundTop = Math.min(world.getActualHeight() - 1,
                world.getHeight(new BlockPos(centerX, 0, centerZ)).getY() - 1);
        int centerY = Math.min(groundTop, MathHelper.floor(getEntityBoundingBox().minY));
        boolean destroyed = false;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                double distance = Math.sqrt((x - posX) * (x - posX) + (z - posZ) * (z - posZ));
                if (distance > radius) continue;
                int depth = Math.max(1, (int) Math.ceil((radius - distance) * 0.45D));
                for (int y = centerY - depth; y <= centerY + 1; y++) {
                    BlockPos position = new BlockPos(x, y, z);
                    if (!world.isBlockLoaded(position)) continue;
                    IBlockState state = world.getBlockState(position);
                    Block block = state.getBlock();
                    if (block == Blocks.AIR || block == Blocks.BEDROCK || block == Blocks.BARRIER
                            || block == Blocks.COMMAND_BLOCK || block == Blocks.CHAIN_COMMAND_BLOCK
                            || block == Blocks.REPEATING_COMMAND_BLOCK
                            || UpstreamBlockTags.contains(UpstreamBlockTags.WITHER_STORM_BLOCK_BLACKLIST, state)
                            || !block.canEntityDestroy(state, world, position, this)
                            || !ForgeEventFactory.onEntityDestroyBlock(this, position, state)) continue;
                    destroyed = world.destroyBlock(position, true) || destroyed;
                }
            }
        }
        if (destroyed) world.playEvent(1022, getPosition(), 0);
    }

    private void updateBodyRotation() {
        previousBodyXRotation = bodyXRotation;
        PlayDeadState state = getPlayDeadState();
        float updated = !canFallOnBack() && state == PlayDeadState.PLAYING_DEAD
                ? bodyXRotation : getNextBodyXRotation(bodyXRotation, state);
        boolean landedOnBack = getPlayDeadState() == PlayDeadState.PLAYING_DEAD
                && bodyXRotation < 90.0F && updated >= 90.0F;
        bodyXRotation = updated;
        dataManager.set(BODY_X_ROTATION, bodyXRotation);
        if (landedOnBack) onFallOnBack();
        ModNetwork.syncWitherStormRotation(this);
    }

    /**
     * EntityLivingBase calls this after onLivingUpdate and normally derives
     * renderYawOffset from movement.  The storm has its own target-driven body
     * rotation, so allowing the vanilla pass to run would overwrite the value
     * that was just interpolated (most visibly in phase 4).
     */
    @Override
    protected float updateDistance(float yaw, float offset) {
        return offset;
    }

    private void updateClientBodyRotation() {
        previousBodyXRotation = bodyXRotation;
        prevRenderYawOffset = renderYawOffset;
        if (clientBodyXRotationSteps > 0) {
            bodyXRotation += (clientBodyXRotationTarget - bodyXRotation) / clientBodyXRotationSteps;
            renderYawOffset += MathHelper.wrapDegrees(clientBodyYRotationTarget - renderYawOffset)
                    / clientBodyXRotationSteps;
            rotationYaw = renderYawOffset;
            --clientBodyXRotationSteps;
        }
    }

    public void lerpBodyRotationTo(float xBodyRotation, float yBodyRotation, int steps) {
        clientBodyXRotationTarget = xBodyRotation;
        clientBodyYRotationTarget = yBodyRotation;
        clientBodyXRotationSteps = Math.max(0, steps);
    }

    static float getNextBodyXRotation(float current, PlayDeadState state) {
        if (state == PlayDeadState.PLAYING_DEAD) {
            if (current < 90.0F) current += current * 0.04F + 0.05F;
            return Math.min(90.0F, current);
        }
        if (!disablesAi(state)) {
            current += -current * 0.015F - 0.02F;
            return Math.max(0.0F, current);
        }
        return current;
    }

    public boolean canFallOnBack() {
        return true;
    }

    public void onFallOnBack() {
        playLandingImpact();
    }

    public void onBigFall() {
        if (getPhase() > 3) playLandingImpact();
    }

    private void playLandingImpact() {
        world.playSound(null, getPosition(), ModSounds.get("wither_storm_thump"), SoundCategory.HOSTILE,
                getSoundVolume() + 3.0F, 1.0F);
        shake(30.0F, 12.0F);
    }

    public void shake(float duration, float power) {
        if (!world.isRemote) ModNetwork.shakeTracking(this, duration, power);
    }

    private void updateSizeForPlayDeadState() {
        int phase = getPhase();
        float height = PHASE_HEIGHT[phase];
        if (WitherStormConfig.squashHitbox && phase > 3) height = 1.0F;
        if (getPlayDeadState() == PlayDeadState.PLAYING_DEAD) height = 0.1F;
        setSize(PHASE_WIDTH[phase], height);
    }

    private void updateLegacyBossInfo() {
        legacyBossInfo.setPercent(MathHelper.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        SupplementalEntities.CommandBlockEntity commandBlock = getBowelsCommandBlock();
        boolean commandBlockOverride = commandBlock != null && commandBlock.shouldShowOwnerBossBar();
        boolean shouldShowBossBar = !isPlayDeadAiDisabled() || commandBlockOverride;
        for (EntityPlayerMP player : trackingPlayers) {
            Boolean cachedAccess = bossThemeAccess.get(player.getUniqueID());
            boolean refreshAccess = cachedAccess == null
                    || Math.floorMod(ticksExisted + player.getEntityId(), 20) == 0;
            boolean hasAccess = !WitherStormConfig.smartBossbar
                    || (!refreshAccess ? cachedAccess.booleanValue()
                    : BossVisibility.canSeeOrIsNotInSmallArea(this, player));
            Boolean previousAccess = bossThemeAccess.put(player.getUniqueID(), hasAccess);
            if (previousAccess == null || previousAccess.booleanValue() != hasAccess) {
                ModNetwork.sendBossThemeAccess(player, this, hasAccess);
            }
            if (shouldShowBossBar && hasAccess) legacyBossInfo.addPlayer(player);
            else legacyBossInfo.removePlayer(player);
        }
        if (commandBlock != null) commandBlock.synchronizeOutsideBossBarViewers(trackingPlayers);
    }

    private void spawnFallingDebris() {
        if (!world.isRemote && getPhase() >= 5) {
            for (int index = 0; index < 3; index++) dropSmallMassCluster(1);
        }
    }

    private void updateAttachedEntities() {
        if (getHealth() <= 0.0F || isDead) return;
        if (getPhase() >= 6) {
            ensureSegments();
            synchronizeLoadedSegments();
        } else {
            removeSegments();
        }
    }

    private void ensureSegments() {
        for (int i = 0; i < segmentUuids.length; i++) {
            SupplementalEntities.WitherStormSegmentEntity segment = findSegment(i);
            if (segment != null) {
                segmentUuids[i] = segment.getUniqueID();
                missingSegmentTicks[i] = 0;
                continue;
            }
            if (segmentUuids[i] != null && ++missingSegmentTicks[i] <= SEGMENT_RESTORE_GRACE_TICKS) {
                continue;
            }
            segmentUuids[i] = null;
            missingSegmentTicks[i] = 0;
            int segmentIndex = i == 1 ? 1 : 2;
            double desiredX = getDesiredSegmentX(segmentIndex);
            double desiredY = getDesiredSegmentY(segmentIndex);
            double desiredZ = getDesiredSegmentZ(segmentIndex);
            if (!isEntityTicking(new BlockPos(desiredX, desiredY, desiredZ))) continue;
            segment = new SupplementalEntities.WitherStormSegmentEntity(world);
            segment.bindToWithoutStateTransition(this, i);
            segment.setSilent(isSilent());
            segment.setEntityInvulnerable(getIsInvulnerable());
            segment.setNoGravity(hasNoGravity());
            segment.setPosition(desiredX, desiredY, desiredZ);
            if (world.spawnEntity(segment)) {
                segmentUuids[i] = segment.getUniqueID();
                segment.getSegmentManager().onAddedToOwner(this);
            }
        }
    }

    /** 在当前世界所有已加载实体中按父 UUID 和镜像索引查找分裂体，并清理重复实例。 */
    @Nullable
    private SupplementalEntities.WitherStormSegmentEntity findSegment(int index) {
        int partIndex = MathHelper.clamp(index, 0, segmentUuids.length - 1);
        UUID preferredUuid = segmentUuids[partIndex];
        // 性能优化：UUID 有效时直接用 O(1) 查询，避免 phase 6/7 每 tick 全量遍历实体
        if (preferredUuid != null) {
            Entity resolved = resolveAny(preferredUuid);
            if (resolved instanceof SupplementalEntities.WitherStormSegmentEntity) {
                SupplementalEntities.WitherStormSegmentEntity segment =
                        (SupplementalEntities.WitherStormSegmentEntity) resolved;
                if (!segment.isDead && !segment.isIndependentBowelsPart()
                        && getUniqueID().equals(segment.getOwnerUuid())
                        && segment.getPartIndex() == partIndex) {
                    return segment;
                }
            }
        }
        SupplementalEntities.WitherStormSegmentEntity selected = null;
        List<SupplementalEntities.WitherStormSegmentEntity> candidates =
                world.getEntities(SupplementalEntities.WitherStormSegmentEntity.class,
                        segment -> !segment.isIndependentBowelsPart()
                                && getUniqueID().equals(segment.getOwnerUuid())
                                && segment.getPartIndex() == partIndex && !segment.isDead);
        for (SupplementalEntities.WitherStormSegmentEntity candidate : candidates) {
            if (selected == null || shouldPreferSegment(candidate, selected, preferredUuid)) {
                selected = candidate;
            }
        }
        for (SupplementalEntities.WitherStormSegmentEntity candidate : candidates) {
            if (candidate != selected) candidate.setDead();
        }
        return selected;
    }

    private static boolean shouldPreferSegment(
            SupplementalEntities.WitherStormSegmentEntity candidate,
            SupplementalEntities.WitherStormSegmentEntity selected,
            @Nullable UUID preferredUuid) {
        boolean candidatePreferred = preferredUuid != null && preferredUuid.equals(candidate.getUniqueID());
        boolean selectedPreferred = preferredUuid != null && preferredUuid.equals(selected.getUniqueID());
        if (candidatePreferred != selectedPreferred) return candidatePreferred;
        int candidateTime = candidate.getTimeWithParent();
        int selectedTime = selected.getTimeWithParent();
        if (candidateTime != selectedTime) return candidateTime > selectedTime;
        return candidate.getUniqueID().compareTo(selected.getUniqueID()) < 0;
    }

    @Nullable
    private SupplementalEntities.WitherStormSegmentEntity getLoadedSegment(int index) {
        int partIndex = MathHelper.clamp(index, 0, segmentUuids.length - 1);
        Entity resolved = resolveAny(segmentUuids[partIndex]);
        if (resolved instanceof SupplementalEntities.WitherStormSegmentEntity) {
            SupplementalEntities.WitherStormSegmentEntity segment =
                    (SupplementalEntities.WitherStormSegmentEntity) resolved;
            if (!segment.isDead && !segment.isIndependentBowelsPart()
                    && getUniqueID().equals(segment.getOwnerUuid())
                    && segment.getPartIndex() == partIndex) {
                return segment;
            }
        }
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof SupplementalEntities.WitherStormSegmentEntity)) continue;
            SupplementalEntities.WitherStormSegmentEntity segment =
                    (SupplementalEntities.WitherStormSegmentEntity) entity;
            if (!segment.isDead && !segment.isIndependentBowelsPart()
                    && getUniqueID().equals(segment.getOwnerUuid())
                    && segment.getPartIndex() == partIndex) {
                segmentUuids[partIndex] = segment.getUniqueID();
                return segment;
            }
        }
        return null;
    }

    private boolean isEntityTicking(BlockPos position) {
        if (!(world instanceof WorldServer)) return world.isBlockLoaded(position);
        WorldServer serverWorld = (WorldServer) world;
        Chunk chunk = serverWorld.getChunkProvider().getLoadedChunk(
                position.getX() >> 4, position.getZ() >> 4);
        return chunk != null && chunk.isLoaded() && !chunk.unloadQueued;
    }

    private void synchronizeLoadedSegments() {
        if (world.isRemote) return;
        for (int index = 0; index < segmentUuids.length; index++) {
            SupplementalEntities.WitherStormSegmentEntity segment = findSegment(index);
            if (segment != null) {
                segment.synchronizeStateFromOwner();
                segmentUuids[index] = segment.getUniqueID();
            }
        }
    }

    public double getDesiredSegmentX(int segment) {
        if (segment <= 0) return posX;
        double staticX = isPlayDeadAiDisabled() ? 45.0D : 75.0D;
        double staticZ = isPlayDeadAiDisabled() ? 0.0D : (segment == 1 ? 50.0D : -50.0D);
        float angle = (renderYawOffset + 180.0F * (segment - 1)) * 0.017453292F;
        float offset = (float) MathHelper.atan2(staticZ, staticX);
        return posX + MathHelper.sin(angle + offset) * Math.sqrt(staticX * staticX + staticZ * staticZ);
    }

    public double getDesiredSegmentY(int segment) {
        return isPlayDeadAiDisabled()
                ? WorldUtil.centerYOf(getEntityBoundingBox()) + 10.0D : posY + getEyeHeight();
    }

    public double getDesiredSegmentZ(int segment) {
        if (segment <= 0) return posZ;
        double staticX = isPlayDeadAiDisabled() ? 45.0D : 75.0D;
        double staticZ = isPlayDeadAiDisabled() ? 0.0D : (segment == 1 ? 50.0D : -50.0D);
        float angle = (renderYawOffset + 180.0F * (segment - 1)) * 0.017453292F;
        float offset = (float) MathHelper.atan2(staticZ, staticX);
        return posZ + MathHelper.cos(angle + offset) * Math.sqrt(staticX * staticX + staticZ * staticZ);
    }

    private void ensurePlayingDeadPodium() {
        if (!podiumPlaced && onGround) placePlayingDeadPodium();
        if (podiumPlaced && !podiumOffsetCorrected) migratePlayingDeadPodium();
        SupplementalEntities.CommandBlockEntity existing = getPlayingDeadCommandBlockReference();
        if (existing != null && !existing.isDead) return;
        // UUID 已存在时等待原实体重新加载；此处重建会在区块加载延迟时复制核心。
        if (commandBlockUuid != null) return;
        if (!podiumPlaced) return;
        SupplementalEntities.CommandBlockEntity core = new SupplementalEntities.CommandBlockEntity(world);
        core.bindTo(this, 0);
        core.setCoreMode(SupplementalEntities.CommandBlockEntity.CoreMode.RIBS);
        core.setCoreState(SupplementalEntities.CommandBlockEntity.CoreState.PLAYING_DEAD);
        core.setPlayingDeadPodiumAnchor(podiumPosition);
        core.setPosition(podiumPosition.getX() + 0.5D, podiumPosition.getY() + 11.0D, podiumPosition.getZ() + 0.5D);
        if (world.spawnEntity(core)) registerPlayingDeadCommandBlock(core);
    }

    void registerPlayingDeadCommandBlock(SupplementalEntities.CommandBlockEntity commandBlock) {
        if (commandBlock == null || commandBlock.isDead || commandBlock.world != world) return;
        playingDeadCommandBlock = commandBlock;
        commandBlockUuid = commandBlock.getUniqueID();
    }

    @Nullable
    private SupplementalEntities.CommandBlockEntity getPlayingDeadCommandBlockReference() {
        if (playingDeadCommandBlock != null) {
            if (commandBlockUuid == null
                    || commandBlockUuid.equals(playingDeadCommandBlock.getUniqueID())) {
                return playingDeadCommandBlock;
            }
            playingDeadCommandBlock = null;
        }
        Entity entity = resolve(commandBlockUuid);
        if (entity instanceof SupplementalEntities.CommandBlockEntity) {
            playingDeadCommandBlock = (SupplementalEntities.CommandBlockEntity) entity;
        }
        return playingDeadCommandBlock;
    }

    private void placePlayingDeadPodium() {
        if (world.isRemote || podiumPlaced) return;
        BlockPos anchor = getPlayingDeadPodiumAnchor();
        if (!isPodiumAreaLoaded(anchor)) return;
        Template template = StructureTemplates.get("command_block_podium");
        if (template == null) return;
        Rotation rotation = StructureTemplates.getFeatureRotation(anchor);
        BlockPos origin = StructureTemplates.getFeatureOrigin(anchor, template, rotation);
        if (!StructureTemplates.place(world, "command_block_podium", origin, rotation, false)) return;
        podiumPosition = anchor;
        podiumPlaced = true;
        podiumOffsetCorrected = true;
    }

    /** Upstream uses cos for X and sin for Z after subtracting 90 degrees. */
    private BlockPos getPlayingDeadPodiumAnchor() {
        float angle = (renderYawOffset - 90.0F) * 0.017453292F;
        int offsetX = (int) (MathHelper.cos(angle) * 5.0F);
        int offsetZ = (int) (MathHelper.sin(angle) * 5.0F);
        return getPosition().add(offsetX, -4, offsetZ);
    }

    /** Moves podiums written by the earlier legacy build with swapped sin/cos. */
    private void migratePlayingDeadPodium() {
        if (world.isRemote || podiumPosition == null) return;
        BlockPos corrected = getPlayingDeadPodiumAnchor();
        if (corrected.equals(podiumPosition)) {
            podiumOffsetCorrected = true;
            return;
        }
        if (!isPodiumAreaLoaded(podiumPosition) || !isPodiumAreaLoaded(corrected)) return;
        Template template = StructureTemplates.get("command_block_podium");
        if (template == null) return;

        Rotation oldRotation = StructureTemplates.getFeatureRotation(podiumPosition);
        BlockPos oldOrigin = StructureTemplates.getFeatureOrigin(podiumPosition, template, oldRotation);
        if (!StructureTemplates.remove(world, "command_block_podium", oldOrigin, oldRotation)) return;

        Rotation correctedRotation = StructureTemplates.getFeatureRotation(corrected);
        BlockPos correctedOrigin = StructureTemplates.getFeatureOrigin(corrected, template, correctedRotation);
        if (!StructureTemplates.place(world, "command_block_podium", correctedOrigin,
                correctedRotation, false)) return;
        podiumPosition = corrected;
        podiumOffsetCorrected = true;
        SupplementalEntities.CommandBlockEntity core = getPlayingDeadCommandBlockReference();
        if (core != null && !core.isDead) {
            core.setPlayingDeadPodiumAnchor(corrected);
            core.setPosition(corrected.getX() + 0.5D, corrected.getY() + 11.0D,
                    corrected.getZ() + 0.5D);
        }
    }

    private void removePlayingDeadPodium() {
        if (world.isRemote || podiumPosition == null || !podiumPlaced || !isPodiumAreaLoaded(podiumPosition)) return;
        Template template = StructureTemplates.get("command_block_podium");
        if (template == null) return;
        Rotation rotation = StructureTemplates.getFeatureRotation(podiumPosition);
        BlockPos origin = StructureTemplates.getCenteredFeatureOrigin(template, podiumPosition, rotation);
        if (StructureTemplates.remove(world, "command_block_podium", origin, rotation)) {
            podiumPosition = null;
            podiumPlaced = false;
        }
    }

    private boolean isPodiumAreaLoaded(BlockPos anchor) {
        if (!(world instanceof WorldServer)) return false;
        int centerChunkX = anchor.getX() >> 4;
        int centerChunkZ = anchor.getZ() >> 4;
        WorldServer serverWorld = (WorldServer) world;
        for (int offsetX = -3; offsetX <= 3; offsetX++) {
            for (int offsetZ = -3; offsetZ <= 3; offsetZ++) {
                if (serverWorld.getChunkProvider().getLoadedChunk(centerChunkX + offsetX, centerChunkZ + offsetZ) == null) return false;
            }
        }
        return true;
    }

    private Entity resolve(UUID uuid) {
        if (uuid == null) return null;
        if (world instanceof WorldServer) {
            Entity resolved = ((WorldServer) world).getEntityFromUuid(uuid);
            if (resolved != null) return resolved;
        }
        List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, getEntityBoundingBox().grow(256.0D),
                entity -> uuid.equals(entity.getUniqueID()));
        return entities.isEmpty() ? null : entities.get(0);
    }

    private void removeSegments() {
        Set<UUID> removedSegments = new HashSet<UUID>();
        for (int index = 0; index < segmentUuids.length; index++) {
            UUID uuid = segmentUuids[index];
            Entity entity = resolveAny(uuid);
            if (entity instanceof SupplementalEntities.WitherStormSegmentEntity) {
                SupplementalEntities.WitherStormSegmentEntity segment =
                        (SupplementalEntities.WitherStormSegmentEntity) entity;
                segment.releaseTrackedEntities();
                removedSegments.add(segment.getUniqueID());
                segment.setDead();
            }
            segmentUuids[index] = null;
            missingSegmentTicks[index] = 0;
        }
        for (SupplementalEntities.WitherStormSegmentEntity segment :
                world.getEntities(SupplementalEntities.WitherStormSegmentEntity.class,
                        candidate -> !candidate.isDead && !candidate.isIndependentBowelsPart()
                                && getUniqueID().equals(candidate.getOwnerUuid()))) {
            if (removedSegments.add(segment.getUniqueID())) {
                segment.releaseTrackedEntities();
                segment.setDead();
            }
        }
    }

    private void removeAttached(UUID[] uuids, boolean deathSequence) {
        for (int i = 0; i < uuids.length; i++) {
            Entity entity = resolve(uuids[i]);
            if (entity instanceof SupplementalEntities.WitherStormSegmentEntity && deathSequence) {
                ((SupplementalEntities.WitherStormSegmentEntity) entity).beginDeathSequence();
            } else if (entity != null) {
                entity.setDead();
            }
            uuids[i] = null;
            if (uuids == segmentUuids && i < missingSegmentTicks.length) missingSegmentTicks[i] = 0;
        }
    }

    private void beginSegmentDeathSequences() {
        for (SupplementalEntities.WitherStormSegmentEntity segment :
                world.getEntities(SupplementalEntities.WitherStormSegmentEntity.class,
                        candidate -> !candidate.isDead && !candidate.isIndependentBowelsPart()
                                && getUniqueID().equals(candidate.getOwnerUuid()))) {
            segmentUuids[MathHelper.clamp(segment.getPartIndex(), 0, segmentUuids.length - 1)] =
                    segment.getUniqueID();
            segment.beginDeathSequenceFromParent();
        }
    }

    private void removeCommandBlockCore() {
        Entity entity = getPlayingDeadCommandBlockReference();
        if (entity == null) entity = resolve(commandBlockUuid);
        if (entity != null) entity.setDead();
        playingDeadCommandBlock = null;
        commandBlockUuid = null;
        removePlayingDeadPodium();
    }

    private void tickDelayedBlockDestruction() {
        if (destroyBlocksTick <= 0) return;
        if (--destroyBlocksTick > 0 || !ForgeEventFactory.getMobGriefingEvent(world, this)) return;
        boolean destroyed = false;
        int minX = MathHelper.floor(posX);
        int minY = MathHelper.floor(posY);
        int minZ = MathHelper.floor(posZ);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                for (int offsetY = 0; offsetY <= 3; offsetY++) {
                    BlockPos position = new BlockPos(minX + offsetX, minY + offsetY, minZ + offsetZ);
                    if (!world.isBlockLoaded(position)) continue;
                    IBlockState state = world.getBlockState(position);
                    Block block = state.getBlock();
                    if (block == Blocks.AIR || block == Blocks.BEDROCK || block == Blocks.BARRIER
                            || block == Blocks.COMMAND_BLOCK || block == Blocks.CHAIN_COMMAND_BLOCK
                            || block == Blocks.REPEATING_COMMAND_BLOCK
                            || UpstreamBlockTags.contains(UpstreamBlockTags.WITHER_STORM_BLOCK_BLACKLIST, state)) {
                        continue;
                    }
                    if (!block.canEntityDestroy(state, world, position, this)
                            || !ForgeEventFactory.onEntityDestroyBlock(this, position, state)) continue;
                    destroyed = world.destroyBlock(position, true) || destroyed;
                }
            }
        }
        if (destroyed) world.playEvent(1022, getPosition(), 0);
    }

    /** Continuous terrain damage used by the moving storm, separate from the
     * delayed hit reaction above.  The vanilla mob-griefing and Forge destroy
     * hooks remain authoritative for every block.  The damage column is anchored
     * to the ground surface below the storm instead of its flying height, so a
     * hovering storm actually carves the terrain beneath it. */
    private void tickTerrainDestruction() {
        if (terrainDestructionCooldown > 0) {
            --terrainDestructionCooldown;
            return;
        }
        if (getInvulnerableTicks() > 0
                || isPlayDeadAiDisabled()
                || !ForgeEventFactory.getMobGriefingEvent(world, this)) return;

        terrainDestructionCooldown = getPhase() > 3 ? 3 : 2;
        int radius = getPhase() > 3 ? Math.min(4, 1 + getPhase() / 2) : 2;
        int minX = MathHelper.floor(posX) - radius;
        int maxX = MathHelper.floor(posX) + radius;
        int minZ = MathHelper.floor(posZ) - radius;
        int maxZ = MathHelper.floor(posZ) + radius;
        // 以风暴正下方地表为中心，向下挖 1~2 层、向上覆盖地表植被层。
        int groundTop = Math.min(world.getActualHeight() - 1,
                world.getHeight(new BlockPos(MathHelper.floor(posX), 0, MathHelper.floor(posZ))).getY() - 1);
        int minY = groundTop - (getPhase() > 3 ? 2 : 1);
        int maxY = Math.min(world.getActualHeight() - 1, groundTop + 1);
        int limit = getPhase() > 3 ? 32 : 12;
        boolean destroyed = false;
        for (int x = minX; x <= maxX && limit > 0; x++) {
            for (int z = minZ; z <= maxZ && limit > 0; z++) {
                for (int y = minY; y <= maxY && limit > 0; y++) {
                    BlockPos position = new BlockPos(x, y, z);
                    if (!world.isBlockLoaded(position)) continue;
                    IBlockState state = world.getBlockState(position);
                    Block block = state.getBlock();
                    if (block == Blocks.AIR || block == Blocks.BEDROCK || block == Blocks.BARRIER
                            || block == Blocks.COMMAND_BLOCK || block == Blocks.CHAIN_COMMAND_BLOCK
                            || block == Blocks.REPEATING_COMMAND_BLOCK
                            || UpstreamBlockTags.contains(UpstreamBlockTags.WITHER_STORM_BLOCK_BLACKLIST, state)
                            || !block.canEntityDestroy(state, world, position, this)
                            || !ForgeEventFactory.onEntityDestroyBlock(this, position, state)) continue;
                    destroyed = world.destroyBlock(position, true) || destroyed;
                    --limit;
                }
            }
        }
        if (destroyed) world.playEvent(1022, getPosition(), 0);
    }

    public void trackEntityToConsume(Entity entity) {
        if (entity == null || entity == this || entity.isDead || entity.dimension != dimension
                || entity instanceof EntityPlayer || entity instanceof WitherStormEntity
                || entity instanceof SupplementalEntities.StormPartBase
                || entity instanceof PowerfulExplosiveEntity.FormidibombEntity) return;
        UUID uuid = entity.getUniqueID();
        if (trackedEntities.containsKey(uuid)) return;
        trackedEntities.put(uuid, entity);
        savedTrackedEntities.remove(uuid);
        if (entity instanceof SupplementalEntities.BlockClusterEntity) {
            entity.setNoGravity(true);
            ((SupplementalEntities.BlockClusterEntity) entity).setPhysics(false);
        }
    }

    private void tickTrackedEntities() {
        if (trackedEntities.isEmpty() || isDeadOrPlayingDead()) return;
        Vec3d absorptionPoint = WorldUtil.centerOf(getEntityBoundingBox());
        AxisAlignedBB absorptionBox = getEntityBoundingBox();
        if (getPhase() > 3) {
            absorptionBox = getEntityBoundingBox().grow(Math.max(1.0D, width / 1.5D));
        }
        List<Entity> splitClusters = new ArrayList<Entity>();
        Iterator<Map.Entry<UUID, Entity>> tracked = trackedEntities.entrySet().iterator();
        while (tracked.hasNext()) {
            Map.Entry<UUID, Entity> entry = tracked.next();
            Entity entity = entry.getValue();
            if (entity == null || entity.isDead || entity.world != world) {
                tracked.remove();
                continue;
            }
            if (WitherStormPulling.canPullIn(entity, trackedEntityPullSource)) {
                Vec3d delta = absorptionPoint.subtract(entity.getPositionVector());
                double distance = delta.length();
                if (distance >= 320.0D || !world.isBlockLoaded(entity.getPosition())) {
                    entity.setPosition(absorptionPoint.x, absorptionPoint.y, absorptionPoint.z);
                }
                Vec3d pullVelocity = WitherStormPulling.getPullVelocity(
                        entity, trackedEntityPullSource, absorptionPoint);
                WitherStormPulling.applyVelocity(entity, pullVelocity, trackedEntityPullSource);
                if (WitherStormPulling.reachesAbsorptionBox(entity, absorptionBox, pullVelocity)) {
                    consumeTrackedEntity(entity);
                    tracked.remove();
                }
            }
            if (entity instanceof SupplementalEntities.BlockClusterEntity && !entity.isDead) {
                SupplementalEntities.BlockClusterEntity cluster = (SupplementalEntities.BlockClusterEntity) entity;
                if (cluster.shouldCrumble() && cluster.getShakeTime() <= 0 && ticksExisted % 20 == 0
                        && rand.nextInt(3) == 0) {
                    SupplementalEntities.BlockClusterEntity split = cluster.splitAt(EnumFacing.Axis.values()[rand.nextInt(3)]);
                    if (split != null && world.spawnEntity(split)) splitClusters.add(split);
                }
            }
        }
        for (Entity split : splitClusters) assignSplitCluster(split);
    }

    private void resolveSavedTrackedEntities() {
        ++trackedEntityTicks;
        if (savedTrackedEntities.isEmpty()) return;
        Iterator<UUID> saved = savedTrackedEntities.iterator();
        while (saved.hasNext()) {
            UUID uuid = saved.next();
            Entity entity = resolveAny(uuid);
            if (entity != null && !entity.isDead) {
                trackedEntities.put(uuid, entity);
                saved.remove();
            } else if (trackedEntityTicks > 80) {
                saved.remove();
            }
        }
    }

    private void assignSplitCluster(Entity split) {
        if (rand.nextBoolean() || segmentUuids.length == 0) {
            trackEntityToConsume(split);
            return;
        }
        SupplementalEntities.WitherStormSegmentEntity segment =
                getLoadedSegment(rand.nextInt(segmentUuids.length));
        if (segment == null || segment.isDead) {
            trackEntityToConsume(split);
        } else {
            segment.getSegmentManager().trackEntityToConsume(split);
        }
    }

    private void convertFallingBlocks() {
        if (!WitherStormConfig.convertFallingBlocks) return;
        List<EntityFallingBlock> fallingBlocks = world.getEntitiesWithinAABB(EntityFallingBlock.class,
                getSearchBox(), falling -> !falling.isDead && world.isBlockLoaded(falling.getPosition())
                        && (isInOpenArea(falling) || canSeeWithCache(0, falling)));
        for (EntityFallingBlock falling : fallingBlocks) {
            SupplementalEntities.BlockClusterEntity cluster = new SupplementalEntities.BlockClusterEntity(world,
                    falling.posX, falling.posY, falling.posZ, falling.getBlock());
            cluster.setRotationDelta(rand.nextInt(20) * 0.05F, rand.nextInt(20) * 0.05F);
            cluster.setNoGravity(true);
            cluster.setPhysics(false);
            cluster.setCreatedFromFallingBlock(true);
            falling.setDead();
            if (world.spawnEntity(cluster)) trackEntityToConsume(cluster);
        }
    }

    boolean isInOpenArea(Entity entity) {
        return WorldUtil.isInAnOpenArea(entity);
    }

    boolean canSeeOrIsInOpenArea(Entity entity) {
        return isInOpenArea(entity) || WorldUtil.hasLineOfSight(this, entity);
    }

    private void consumeTrackedEntity(Entity entity) {
        if (entity instanceof SupplementalEntities.BlockClusterEntity) {
            SupplementalEntities.BlockClusterEntity cluster = (SupplementalEntities.BlockClusterEntity) entity;
            if (!cluster.shouldNotCountToConsumedMass()) {
                consumeEntity(entity, cluster.getBlocks().size());
            }
        } else if (entity instanceof EntityItem) {
            consumeEntity(entity, ((EntityItem) entity).getItem().getCount());
        } else {
            consumeEntity(entity, 1);
        }
        if (entity instanceof EntityLivingBase) {
            captureConsumedPet((EntityLivingBase) entity);
            entity.attackEntityFrom(ModDamageSources.witherStormAttackMob(this), Float.MAX_VALUE);
        } else {
            entity.setDead();
        }
    }

    private Entity resolveAny(UUID uuid) {
        if (uuid == null) return null;
        for (Entity entity : world.loadedEntityList) {
            if (uuid.equals(entity.getUniqueID())) return entity;
        }
        return null;
    }

    void handleBowelsEntranceCollision(Entity entity) {
        if (world.isRemote || dimension == BowelsDimensions.DIMENSION_ID
                || entity == null || entity == this || entity.isDead
                || isTargetedByMainHeadFamily(entity)) return;
        if (entity instanceof EntityEnderPearl) {
            handleBowelsEnderPearl((EntityEnderPearl) entity);
        } else if (canTravelToBowels(entity)) {
            sendEntityToBowels(entity);
        }
    }

    private boolean canTravelToBowels(Entity entity) {
        return !(entity instanceof SupplementalEntities.BlockClusterEntity)
                && !(entity instanceof EntityItem)
                && !(entity instanceof SickenedEntities.SickenedPhantomEntity)
                && !(entity instanceof EntityArrow)
                && !(entity instanceof EntityThrowable)
                && !(entity instanceof EntityFireball)
                && !(entity instanceof EntityFishHook)
                && !(entity instanceof EntityShulkerBullet)
                && !(entity instanceof EntityLlamaSpit)
                && !(entity instanceof EntityFireworkRocket);
    }

    private void handleBowelsEnderPearl(EntityEnderPearl pearl) {
        EntityLivingBase thrower = pearl.getThrower();
        if (thrower != null && !thrower.isDead && thrower.world == world) {
            if (thrower instanceof EntityPlayerMP && ((EntityPlayerMP) thrower).isPlayerSleeping()) {
                pearl.setDead();
                return;
            }
            if (thrower.isRiding()) thrower.dismountRidingEntity();
            thrower.setPositionAndUpdate(pearl.posX, pearl.posY, pearl.posZ);
            thrower.fallDistance = 0.0F;
            if (thrower instanceof EntityPlayerMP) {
                thrower.attackEntityFrom(DamageSource.FALL, 5.0F);
            }
            sendEntityToBowels(thrower);
        }
        pearl.setDead();
    }

    private void sendEntityToBowels(Entity entity) {
        if (entity == null || world.getMinecraftServer() == null
                || !pendingBowelsTransfers.add(entity.getUniqueID())) return;
        UUID entityUuid = entity.getUniqueID();
        world.getMinecraftServer().addScheduledTask(() -> {
            try {
                if (!entity.isDead && entity.world == world && entity.dimension == dimension) {
                    if (entity instanceof EntityPlayerMP) {
                        BowelsManager.enter(this, (EntityPlayerMP) entity);
                    } else {
                        BowelsManager.enter(this, entity);
                    }
                }
            } finally {
                pendingBowelsTransfers.remove(entityUuid);
            }
        });
    }

    private void applyTractorBeam() {
        if (isDeadOrPlayingDead()) return;
        double defaultSpeed = WitherStormConfig.tractorPullSpeedModifier;
        if (getPhase() < 4) {
            for (int head = 0; head < getTotalHeads(); head++) {
                pullInTarget(getTarget(head), defaultSpeed, head);
            }
        }

        // 候选列表由 refreshEntityCandidates 每 tick 单次遍历填充，检测机制与上游每 tick 一致
        for (int head = 0; head < 3; head++) {
            if (!tractorBeamActive(head)) continue;
            final int headIndex = head;
            Vec3d headPosition = getHeadPosition(head, 1.0F);
            Vec3d direction = headManager.getLookVector(head);
            for (Entity entity : tractorBeamCandidates) {
                if (entity.isDead || !isInsideBeam(entity, headPosition, direction, headIndex)) continue;
                boolean selectedTarget = getTarget(head) == entity;
                if (getPhase() > 3 && selectedTarget) {
                    pullInTarget(entity, defaultSpeed, head);
                    continue;
                }
                if (!canPullUntargeted(entity, head)) continue;
                pullInTarget(entity, getTractorPullSpeed(entity), head);
                if (!(entity instanceof EntityPlayer) && entity.getDistanceSq(headPosition.x, headPosition.y,
                        headPosition.z) < 400.0D) {
                    trackEntityToConsume(entity);
                }
            }
        }
    }

    /** 每 tick 单次遍历世界实体，同时填充牵引候选、吸收候选与附近玩家（AABB 相交语义与 getEntitiesWithinAABB 一致）。 */
    private void refreshEntityCandidates() {
        tractorBeamCandidates.clear();
        absorbCandidates.clear();
        nearbyPlayers.clear();
        if (isDeadOrPlayingDead()) return;
        AxisAlignedBB pullBox = getSearchBox();
        AxisAlignedBB absorbBox = null;
        if (ForgeEventFactory.getMobGriefingEvent(world, this)) {
            double consumptionRadius = getPhase() > 3 ? 80.0D
                    : Math.min(48.0D, 12.0D + Math.round(getConsumedMass() * 0.00445D));
            double searchRadius = consumptionRadius + 50.0D + (getPhase() >= 6 ? 100.0D : 0.0D);
            absorbBox = getEntityBoundingBox().grow(searchRadius);
        }
        for (Entity entity : world.loadedEntityList) {
            if (entity.isDead || entity.dimension != dimension) continue;
            if (pullBox.intersects(entity.getEntityBoundingBox())
                    && isTractorBeamCandidate(entity)) {
                tractorBeamCandidates.add(entity);
            }
            if (absorbBox != null && absorbBox.intersects(entity.getEntityBoundingBox())) {
                absorbCandidates.add(entity);
                if (entity instanceof EntityPlayerMP) nearbyPlayers.add((EntityPlayerMP) entity);
            }
        }
    }

    private boolean isTractorBeamCandidate(Entity entity) {
        return isBasicTractorBeamCandidate(entity) && entity != this && !entity.isDead
                && entity.dimension == dimension
                && !ignoredTargetsManager.shouldIgnoreTarget(entity)
                && !(entity instanceof SupplementalEntities.StormPartBase)
                && !(entity instanceof WitherStormEntity)
                && !(entity instanceof PowerfulExplosiveEntity.FormidibombEntity)
                && !isTrackedForConsumption(entity)
                && (!(entity instanceof EntityPlayer)
                || !((EntityPlayer) entity).capabilities.disableDamage
                && !((EntityPlayer) entity).isSpectator());
    }

    private static boolean isBasicTractorBeamCandidate(Entity entity) {
        return entity instanceof EntityLivingBase || isTractorBeamPullableObject(entity);
    }

    private static boolean isTractorBeamPullableObject(Entity entity) {
        return entity instanceof EntityItem
                || entity instanceof EntityBoat || entity instanceof EntityMinecart;
    }

    private boolean canPullUntargeted(Entity entity, int head) {
        if (!WitherStormConfig.canPickupMobClusters
                || isHeadDistracted(head)
                || ignoredTargetsManager.shouldIgnoreTarget(entity)
                || isTargetedByMainHeadFamily(entity)
                || isTargetInUseBySegment(entity)
                || (!(entity instanceof EntityPlayer && getPhase() >= 4)
                && !canSeeWithCache(head, entity))) {
            return false;
        }
        if (!(entity instanceof EntityLivingBase)) return isTractorBeamPullableObject(entity);
        EntityLivingBase living = (EntityLivingBase) entity;
        return isValidStormTarget(living) && !isBlockingWithShield(living);
    }

    private double getTractorPullSpeed(Entity entity) {
        if (isTractorBeamPullableObject(entity)) return 0.4D;
        if (entity instanceof EntityPlayer) return WitherStormConfig.tractorPullSpeedModifier;
        return WitherStormConfig.tractorPullSpeedModifier - 0.05D
                + new java.util.Random(entity.getEntityId()).nextDouble() * 0.1D;
    }

    private void tickProjectilesHittingHeads() {
        if (getPhase() <= 3) return;
        for (int head = 0; head < getTotalHeads(); head++) {
            if (!tractorBeamActive(head)) continue;
            List<Entity> projectiles = world.getEntitiesWithinAABB(Entity.class, getHeadBounds(head),
                    entity -> !entity.isDead && isHeadHittingProjectile(entity));
            for (Entity projectile : projectiles) {
                Entity owner = getProjectileOwner(projectile);
                if (owner == this) continue;
                boolean wasInjured = isHeadInjured(head);
                boolean accepted = headManager.attemptAttack(head, owner, 40);
                if (accepted && !wasInjured && isHeadInjured(head) && owner instanceof EntityPlayer) {
                    world.playSound(null, owner.posX, owner.posY, owner.posZ,
                            SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
                if (!(projectile instanceof Trident)) projectile.setDead();
            }
        }
    }

    static boolean isHeadHittingProjectile(Entity entity) {
        return entity instanceof IProjectile
                || entity instanceof EntityFishHook
                || entity instanceof EntityFireworkRocket;
    }

    @Nullable
    static Entity getProjectileOwner(Entity projectile) {
        if (projectile instanceof EntityArrow) return ((EntityArrow) projectile).shootingEntity;
        if (projectile instanceof EntityThrowable) return ((EntityThrowable) projectile).getThrower();
        if (projectile instanceof EntityFireball) return ((EntityFireball) projectile).shootingEntity;
        if (projectile instanceof EntityFishHook) return ((EntityFishHook) projectile).getAngler();
        if (projectile instanceof EntityLlamaSpit) return ((EntityLlamaSpit) projectile).owner;
        return null;
    }

    /** 让风暴在 1.12 的飞行实体实现上保留上游的追逐和悬浮行为。 */
    private void updateCustomMovement() {
        if (isDeadOrPlayingDead() || getInvulnerableTicks() > 0) return;
        PowerfulExplosiveEntity.FormidibombEntity nearbyFormidibomb = getNearbyTickingFormidibomb();
        Vec3d target = nearbyFormidibomb == null && shouldTrackUltimateTarget()
                ? getUltimateTargetPos() : null;
        Vec3d velocity = new Vec3d(motionX, motionY * 0.6D, motionZ);
        double ascendSpeed = getPhase() > 3 && !WitherStormConfig.dynamicFlyingHeight
                ? 0.005D : 0.02D;
        double desiredHeight = getDesiredFlyingHeight();
        if (posY < desiredHeight || !isArmored() && posY < desiredHeight + 5.0D) {
            velocity = new Vec3d(velocity.x, (desiredHeight - posY)
                    * ascendSpeed, velocity.z);
        }
        EntityLivingBase attackTarget = getAttackTarget();
        boolean targetInMainBeam = getPhase() < 4 && attackTarget != null
                && tractorBeamActive(0)
                && isInsideBeam(attackTarget, getHeadPosition(0, 1.0F),
                headManager.getLookVector(0), 0);
        if (targetInMainBeam) {
            // Do not chase a target already caught by the main beam.
            velocity = new Vec3d(velocity.x * 0.35D, velocity.y, velocity.z * 0.35D);
        } else if (getPhase() < 4 && attackTarget != null) {
            double horizontalX = attackTarget.posX - posX;
            double horizontalZ = attackTarget.posZ - posZ;
            double horizontalDistance = Math.sqrt(horizontalX * horizontalX + horizontalZ * horizontalZ);
            if (horizontalDistance > 20.0D) {
                velocity = velocity.add(horizontalX / horizontalDistance * 0.3D - velocity.x * 0.6D,
                        0.0D, horizontalZ / horizontalDistance * 0.3D - velocity.z * 0.6D);
            }
        } else if (target != null) {
            double horizontalX = target.x - posX;
            double horizontalZ = target.z - posZ;
            double horizontalDistance = Math.sqrt(horizontalX * horizontalX + horizontalZ * horizontalZ);
            double minimumDistance = getPhase() > 3 ? 6000.0D : 12000.0D;
            double movementSpeed = getEntityAttribute(
                    SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
            double speed = getPhase() > 3
                    ? movementSpeed + getDefaultNormalSpeed()
                    : getEntityAttribute(ModAttributes.SLOW_FLYING_SPEED).getAttributeValue();
            if (getPhase() > 3 && shouldSpeedUp()) {
                speed = movementSpeed + Math.min(getDefaultChasingSpeed(),
                        getPositionVector().distanceTo(target) * 0.001D);
            } else if (getPhase() > 3 && horizontalDistance > 205.0D) {
                speed += 0.03D;
            }
            WitherStormModifyFlyingSpeedEvent event =
                    new WitherStormModifyFlyingSpeedEvent(this, speed);
            MinecraftForge.EVENT_BUS.post(event);
            speed = event.getOriginalSpeed();
            if (horizontalDistance * horizontalDistance > minimumDistance) {
                velocity = velocity.add(horizontalX / horizontalDistance * speed - velocity.x * 0.6D,
                        0.0D, horizontalZ / horizontalDistance * speed - velocity.z * 0.6D);
            }
        }
        // 前期主光束正在牵引目标时保持悬停：硬归零水平速度，而不是逐帧衰减，
        // 否则上一 tick 的追逐动量仍会让身体在吸附过程中持续前移。
        if (getPhase() < 4 && tractorBeamActive(0)
                && (getTarget(0) != null || getAttackTarget() != null)) {
            velocity = new Vec3d(0.0D, velocity.y, 0.0D);
        }
        motionX = velocity.x;
        motionY = velocity.y;
        motionZ = velocity.z;
        updateHorizontalBodyRotation(nearbyFormidibomb);
    }

    private void updateHorizontalBodyRotation(
            @Nullable PowerfulExplosiveEntity.FormidibombEntity nearbyFormidibomb) {
        Vec3d targetPosition = getUltimateTargetPos();
        EntityLivingBase ultimateTarget = getUltimateTarget();
        boolean targetClaimed = ultimateTarget != null
                && (isTargetInUseBySegment(ultimateTarget)
                || isTargetedByAdditionalHead(ultimateTarget));
        if (getPhase() > 3) {
            if (nearbyFormidibomb != null) targetPosition = nearbyFormidibomb.getPositionVector();
            if (targetPosition != null && (nearbyFormidibomb != null
                    || !targetClaimed && shouldRotateTowardsUltimateTarget())) {
                double deltaX = targetPosition.x - posX;
                double deltaZ = targetPosition.z - posZ;
                if (deltaX * deltaX + deltaZ * deltaZ > 0.0001D) {
                    float wantedYaw = (float) (MathHelper.atan2(deltaZ, deltaX)
                            * 180.0D / Math.PI) - 90.0F;
                    float rotationSpeed = nearbyFormidibomb == null
                            ? (float) WitherStormConfig.rotationSpeed : 0.1F;
                    renderYawOffset = rotateTowards(renderYawOffset, wantedYaw, rotationSpeed);
                }
            }
            rotationYaw = renderYawOffset;
            dataManager.set(BODY_Y_ROTATION, renderYawOffset);
            return;
        }
        double movedX = posX - prevPosX;
        double movedZ = posZ - prevPosZ;
        if (getAttackTarget() == null && getTarget(1) == null && getTarget(2) == null
                && targetPosition != null && movedX * movedX + movedZ * movedZ > 2.5000003E-7D) {
            double deltaX = targetPosition.x - posX;
            double deltaZ = targetPosition.z - posZ;
            if (deltaX * deltaX + deltaZ * deltaZ > 0.0001D) {
                float wantedYaw = (float) (MathHelper.atan2(deltaZ, deltaX)
                        * 180.0D / Math.PI) - 90.0F;
                renderYawOffset = rotateTowards(renderYawOffset, wantedYaw, 5.0F);
                rotationYaw = renderYawOffset;
            }
        } else if (motionX * motionX + motionZ * motionZ > 0.0025D) {
            rotationYaw = (float) (MathHelper.atan2(motionZ, motionX) * 180.0D / Math.PI) - 90.0F;
            renderYawOffset = rotationYaw;
        }
        dataManager.set(BODY_Y_ROTATION, renderYawOffset);
    }

    private boolean isTargetedByAdditionalHead(Entity entity) {
        if (entity == null) return false;
        for (int head = 1; head < getTotalHeads(); head++) {
            if (getTarget(head) == entity) return true;
        }
        return false;
    }

    private static float rotateTowards(float current, float wanted, float maximumChange) {
        float difference = MathHelper.wrapDegrees(wanted - current);
        return current + MathHelper.clamp(difference, -maximumChange, maximumChange);
    }

    private double getDesiredFlyingHeight() {
        if (getPhase() > 3) {
            if (!WitherStormConfig.dynamicFlyingHeight) {
                currentFlyingHeight = WitherStormConfig.flyingHeight;
            } else if (ticksExisted - lastFlyingHeightChange
                    >= WitherStormConfig.dynamicFlyingHeightTime * 20) {
                lastFlyingHeightChange = ticksExisted;
                currentFlyingHeight = 40.0D + rand.nextInt(41);
            }
        } else {
            currentFlyingHeight = 10.0D;
        }

        int centerX = MathHelper.floor(posX);
        int centerZ = MathHelper.floor(posZ);
        int startingHeight = MathHelper.floor(Math.min(world.getActualHeight() - 1,
                WorldUtil.centerYOf(getEntityBoundingBox())));
        int radius = Math.max(1, MathHelper.floor(PHASE_WIDTH[getPhase()] * 1.5F));
        int highest = -1;
        for (int offsetX = -radius; offsetX < radius; offsetX++) {
            for (int offsetZ = -radius; offsetZ < radius; offsetZ++) {
                int terrainHeight = getPhase() > 3
                        ? WorldUtil.getMotionBlockingHeightIgnoringLeaves(world,
                        centerX + offsetX, centerZ + offsetZ)
                        : WorldUtil.getHeightStartingAt(world, startingHeight,
                        centerX + offsetX, centerZ + offsetZ);
                highest = Math.max(highest, Math.min(startingHeight, terrainHeight));
            }
        }
        return highest + currentFlyingHeight;
    }

    private void applyMassAbsorption() {
        if (isDeadOrPlayingDead() || !ForgeEventFactory.getMobGriefingEvent(world, this)) return;
        double consumptionRadius = getPhase() > 3 ? 80.0D
                : Math.min(48.0D, 12.0D + Math.round(getConsumedMass() * 0.00445D));
        List<AxisAlignedBB> protectedDropAreas = new ArrayList<AxisAlignedBB>();
        for (EntityPlayerMP player : nearbyPlayers) {
            protectedDropAreas.add(player.getEntityBoundingBox().grow(8.0D));
        }
        for (Entity entity : absorbCandidates) {
            if (entity.isDead || entity.dimension != dimension
                    || !(entity instanceof EntityItem)
                    && !(entity instanceof EntitySlime
                    && ((EntitySlime) entity).isSmallSlime())
                    || trackedEntities.containsKey(entity.getUniqueID())
                    || rand.nextFloat() < 0.9F) continue;
            double distance = entity.getDistance(this);
            if (entity instanceof EntityItem) {
                EntityItem itemEntity = (EntityItem) entity;
                ItemStack stack = itemEntity.getItem();
                if (getPhase() > 3 && WitherStormConfig.removeNearbyJunk
                        && UpstreamItemTags.contains(UpstreamItemTags.JUNK, stack)) {
                    if (!isInsideAny(entity.getPositionVector(), protectedDropAreas)) entity.setDead();
                    continue;
                }
                boolean unappetizing = UpstreamItemTags.contains(UpstreamItemTags.UNAPPETIZING, stack);
                if (distance > consumptionRadius
                        || unappetizing && distance >= (getPhase() > 3 ? 35.0D : 2.0D)
                        || isProtectedFromConsumption(stack)
                        || !canTrackMassEntity(entity)) continue;
            } else if (distance > consumptionRadius || !canTrackMassEntity(entity)) {
                continue;
            }
            trackEntityToConsume(entity);
            entity.setNoGravity(true);
        }
    }

    private boolean canTrackMassEntity(Entity entity) {
        if (isTrackedByAnySegment(entity)) return false;
        return isInOpenArea(entity) || WorldUtil.hasLineOfSight(this, entity);
    }

    private boolean isTrackedByAnySegment(Entity entity) {
        if (entity == null) return false;
        for (int index = 0; index < segmentUuids.length; index++) {
            SupplementalEntities.WitherStormSegmentEntity segment = getLoadedSegment(index);
            if (segment != null && segment.isTrackingForConsumption(entity)) return true;
        }
        return false;
    }

    static boolean isInsideAny(Vec3d position, List<AxisAlignedBB> areas) {
        for (AxisAlignedBB area : areas) {
            if (area.contains(position)) return true;
        }
        return false;
    }

    static boolean isProtectedFromConsumption(ItemStack stack) {
        return stack.getItem() == ModItems.get("command_block_book")
                || stack.getItem() == ModItems.get("withered_nether_star")
                || UpstreamItemTags.contains(UpstreamItemTags.COMMAND_BLOCK_TOOLS, stack);
    }

    void captureConsumedPet(EntityLivingBase living) {
        ConsumedPetStorage.capture(consumedPets, living);
    }

    public void spawnConsumedPets(Vec3d position) {
        ConsumedPetStorage.release(world, consumedPets, position);
    }

    private boolean isInsideBeam(Entity entity, Vec3d origin, Vec3d direction, int head) {
        double cutoff = headManager.getTractorBeamCutoff(head);
        return TractorBeamHelper.isInsideTractorBeam(
                entity.getPositionVector(), origin, direction, cutoff,
                entity instanceof EntityPlayer && getPhase() >= 4 ? 8.0D : 4.0D);
    }

    public boolean tractorBeamActive(int head) {
        if (isDeadOrPlayingDead() || head < 0 || head > 2) return false;
        if (getPhase() < 2) return false;
        if (isHeadInjured(head)) return false;
        if (head > 0 && areOtherHeadsDisabled()) return false;
        return getPhase() >= 4 || head == 0;
    }

    public void pullInTarget(Entity target, double speed, int head) {
        if (target == null || target == this || target.isDead || !tractorBeamActive(head)) return;
        Vec3d headPosition = getHeadPosition(head, 1.0F);
        Vec3d pullPosition = headPosition;
        if (!(target instanceof EntityPlayer) && target.getPositionVector().distanceTo(headPosition) >= 25.0D) {
            Vec3d look = headManager.getLookVector(head);
            double cutoff = headManager.getTractorBeamCutoff(head);
            pullPosition = TractorBeamHelper.calculateClosestPoint(
                    target.getPositionVector(), headPosition, look, cutoff, -5.0D);
        }
        Vec3d velocity = TractorBeamHelper.calculatePullVelocity(
                target.getPositionVector(), pullPosition, speed);
        if (velocity.lengthSquared() > 0.0D) {
            Entity pulled = target;
            Entity vehicle = target.getRidingEntity();
            if (vehicle != null && WitherStormConfig.shouldPickUpVehicles
                    && canPullVehicle(vehicle)) {
                pulled = vehicle;
            }
            pulled.motionX = velocity.x;
            pulled.motionY = velocity.y;
            pulled.motionZ = velocity.z;
            pulled.velocityChanged = true;
            if (target instanceof EntityPlayerMP) {
                ModNetwork.setPlayerMotion((EntityPlayerMP) target, pulled, velocity);
            }
        }
        target.velocityChanged = true;
        AxisAlignedBB headBox = new AxisAlignedBB(headPosition.x - 2.0D, headPosition.y - 4.0D, headPosition.z - 2.0D,
                headPosition.x + 2.0D, headPosition.y + 2.0D, headPosition.z + 2.0D);
        if (!headBox.intersects(target.getEntityBoundingBox())) return;
        if (target instanceof EntityLivingBase && !((EntityLivingBase) target).isEntityAlive()) return;
        if (target instanceof EntityPlayer) {
            EntityLivingBase living = (EntityLivingBase) target;
            float damage = WitherStormConfig.instantChomp ? Float.MAX_VALUE
                    : (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
            living.attackEntityFrom(ModDamageSources.witherStormAttack(this), damage);
            if (living.isDead || !living.isEntityAlive()) consumeEntity(living, 1);
            headManager.startBiting(head);
        } else if (target instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) target;
            consumeEntity(living, 1);
            if (WitherStormConfig.healFromChomp) heal(living.getMaxHealth() * 0.5F);
            captureConsumedPet(living);
            living.attackEntityFrom(ModDamageSources.witherStormAttackMob(this), Float.MAX_VALUE);
            headManager.startBiting(head);
            if (head > 0) headManager.delayAfterChomp(head);
        }
    }

    public int getClusterRadius() {
        return (int) Math.max(1.0F, getPhase() * 0.75F);
    }

    public void removeFluidFromLook(float pitch, float yaw, int head) {
        Vec3d start = getHeadPosition(head, 1.0F);
        Vec3d direction = headManager.getLookVector(head);
        removeFluidFromRay(start, direction);
    }

    void removeFluidFromRay(Vec3d start, Vec3d direction) {
        removeFluidFromRay(start, direction, this);
    }

    void removeFluidFromRay(Vec3d start, Vec3d direction, Entity griefingEntity) {
        if (world.isRemote || getPhase() <= 3 || !WitherStormConfig.tractorBeamsRemoveFluids
                || !ForgeEventFactory.getMobGriefingEvent(world, griefingEntity)) return;
        Vec3d end = start.add(direction.scale(200.0D));
        RayTraceResult result = world.rayTraceBlocks(start, end, true, true, false);
        BlockPos hit = result == null ? new BlockPos(end) : result.getBlockPos();
        if (!world.isBlockLoaded(hit)
                || hit.getY() <= WitherStormConfig.tractorBeamFluidRemovalHeight) return;
        for (int offsetX = -6; offsetX <= 6; offsetX++) {
            for (int offsetY = -6; offsetY <= 6; offsetY++) {
                for (int offsetZ = -6; offsetZ <= 6; offsetZ++) {
                    BlockPos position = hit.add(offsetX, offsetY, offsetZ);
                    IBlockState state = world.getBlockState(position);
                    if (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER) {
                        world.setBlockState(position, Blocks.FLOWING_WATER.getDefaultState(), 3);
                    } else if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FLOWING_LAVA) {
                        world.setBlockState(position, Blocks.FLOWING_LAVA.getDefaultState(), 3);
                    }
                }
            }
        }
    }

    public void createClusterFromLook(float pitch, float yaw, int time, int head) {
        SupplementalEntities.BlockClusterEntity cluster = createTractorBeamCluster(
                getHeadPosition(head, 1.0F), headManager.getLookVector(head), time, head);
        if (cluster != null) trackEntityToConsume(cluster);
    }

    @Nullable
    SupplementalEntities.BlockClusterEntity createTractorBeamCluster(Vec3d start, Vec3d direction,
                                                                int time, int head) {
        return createTractorBeamCluster(start, direction, time, head, rand, this);
    }

    @Nullable
    SupplementalEntities.BlockClusterEntity createTractorBeamCluster(Vec3d start, Vec3d direction,
                                                                int time, int head, java.util.Random random) {
        return createTractorBeamCluster(start, direction, time, head, random, this);
    }

    @Nullable
    SupplementalEntities.BlockClusterEntity createTractorBeamCluster(Vec3d start, Vec3d direction,
                                                                int time, int head, java.util.Random random,
                                                                Entity griefingEntity) {
        if (world.isRemote || !WitherStormConfig.tractorBeamClusterPickUp
                || !ForgeEventFactory.getMobGriefingEvent(world, griefingEntity)) return null;
        Vec3d end = start.add(direction.scale(200.0D));
        RayTraceResult result = world.rayTraceBlocks(start, end, false, true, false);
        BlockPos hit = result == null || result.typeOfHit == RayTraceResult.Type.MISS
                ? new BlockPos(end) : result.getBlockPos();
        if (!world.isBlockLoaded(hit)) return null;
        for (int attempt = 0; attempt < 512; attempt++) {
            double offsetScale = getPhase() <= 3 ? 1.0D : 2.25D;
            BlockPos candidate = hit.add((int) Math.round(random.nextGaussian() * offsetScale),
                    (int) Math.round(random.nextGaussian() * offsetScale),
                    (int) Math.round(random.nextGaussian() * offsetScale));
            IBlockState candidateState = world.getBlockState(candidate);
            if (candidateState.getBlock() == Blocks.AIR
                    || !WorldUtil.isBlockExposed(world, candidate)
                    || !WitherStormBlockRules.canConsume(candidateState)) continue;
            if (WitherStormConfig.onlyTryPickingUpTractorTagged
                    && !UpstreamBlockTags.contains(
                    UpstreamBlockTags.TRACTOR_BEAM_DISTRACTION_BLOCKS, candidateState)) continue;
            if (random.nextDouble() > 0.999D && (UpstreamBlockTags.contains("forge:stone", candidateState)
                    || UpstreamBlockTags.contains("minecraft:dirt", candidateState)
                    || UpstreamBlockTags.contains("minecraft:sand", candidateState))) continue;
            float clusterRadius = getTractorBeamClusterRadius(random);
            SupplementalEntities.BlockClusterEntity cluster = new SupplementalEntities.BlockClusterEntity(world);
            cluster.populateWithRadius(candidate, clusterRadius,
                    (level, position, state) -> WitherStormBlockRules.canConsume(state));
            if (cluster.getBlocks().isEmpty()) continue;
            cluster.setTime(time);
            if (random.nextInt(3) == 0) {
                cluster.playSound(ModSounds.get("block_cluster_shake"), 2.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
            }
            cluster.setCreatedFromTractorBeam(true);
            cluster.setHeadCreatedFrom(head);
            cluster.setTractorBeamDistanceThreshold(random.nextDouble() * 5.0D);
            cluster.setRotationDelta((random.nextInt(120) - 60) * 0.05F / (clusterRadius * 3.0F),
                    (random.nextInt(120) - 60) * 0.05F / (clusterRadius * 3.0F));
            cluster.setNoGravity(true);
            cluster.setPhysics(false);
            if (world.spawnEntity(cluster)) return cluster;
            return null;
        }
        return null;
    }

    private float getTractorBeamClusterRadius(java.util.Random random) {
        switch (getPhase()) {
            case 4:
                return MathHelper.clamp((float) (1.0D + 0.125D * random.nextGaussian()), 1.0F, 1.5F);
            case 5:
                return MathHelper.clamp((float) (1.0D + 0.5D * random.nextGaussian()), 1.0F, 3.0F);
            case 6:
                return MathHelper.clamp((float) (1.0D + 0.75D * random.nextGaussian()), 1.0F, 4.5F);
            case 7:
                return MathHelper.clamp((float) (1.5D + 1.25D * random.nextGaussian()), 1.0F, 8.0F);
            default:
                return 1.0F;
        }
    }

    public int getPhase() {
        return MathHelper.clamp(dataManager.get(PHASE), 0, 7);
    }

    public int getConsumedMass() {
        return dataManager.get(CONSUMED_MASS);
    }

    public int getEntityConsumptionRadius() {
        return entityConsumptionRadius;
    }

    public int getHunchbackConsumptionRadius() {
        return Math.min(48, 12 + (int) Math.round(getConsumedMass() * 0.00445D));
    }

    public void consumeEntity(@Nullable Entity entity, int amount) {
        WitherStormConsumeEvent event = new WitherStormConsumeEvent(this, entity, amount);
        if (!MinecraftForge.EVENT_BUS.post(event)) addConsumedMass(event.getConsumedAmount());
    }

    public void addConsumedMass(int amount) {
        if (!isConsumptionLocked()) {
            dataManager.set(CONSUMED_MASS, getConsumedMass() + amount);
            if (getPhase() == 6 && getConsumedMass() > getSubPhaseRequirement(6)
                    && areOtherHeadsDisabled()) {
                dataManager.set(OTHER_HEADS_DISABLED, false);
                headManager.onOtherHeadsEnabled();
                for (UUID segmentUuid : segmentUuids) {
                    Entity entity = resolveAny(segmentUuid);
                    if (entity instanceof SupplementalEntities.WitherStormSegmentEntity) {
                        ((SupplementalEntities.WitherStormSegmentEntity) entity)
                                .getSegmentManager().onOtherHeadsEnabled();
                    }
                }
            }
        }
    }

    /** 供管理命令直接设置已吞噬质量。 */
    public void setConsumedMass(int amount) {
        dataManager.set(CONSUMED_MASS, Math.max(0, amount));
    }

    /** 供管理命令设置进化速度倍率，范围与上游 evolutionSpeed 命令一致。 */
    public void setEvolutionSpeedModifier(double value) {
        getEntityAttribute(ModAttributes.EVOLUTION_SPEED).setBaseValue(
                MathHelper.clamp(value, 0.1D, 32.0D));
    }

    public void makeConsumptionLocked(boolean locked) {
        consumptionLocked = locked;
    }

    public boolean isConsumptionLocked() {
        return consumptionLocked;
    }

    public PlayDeadState getPlayDeadState() {
        return PlayDeadState.values()[MathHelper.clamp(dataManager.get(PLAY_DEAD_STATE), 0, PlayDeadState.values().length - 1)];
    }

    int getPlayDeadStateTicks() {
        return world.isRemote ? Math.max(clientVisualStateTicks,
                Math.max(0, dataManager.get(PLAY_DEAD_STATE_TICKS)))
                : Math.max(0, stateTicks);
    }

    private static boolean disablesAi(PlayDeadState state) {
        return state == PlayDeadState.FALLING || state == PlayDeadState.PLAYING_DEAD;
    }

    public boolean isPlayDeadAiDisabled() { return disablesAi(getPlayDeadState()); }
    public boolean shouldDoNothing() { return getInvulnerableTicks() > 0 || isPlayDeadAiDisabled(); }
    public boolean isPlayingDead() { return getPlayDeadState() == PlayDeadState.PLAYING_DEAD; }
    public boolean isReviving() { return getPlayDeadState() == PlayDeadState.REVIVING; }
    public int getInvulnerableTicks() { return dataManager.get(INVULNERABLE_TICKS); }
    public int getStartingInvulnerableTicks() { return dataManager.get(STARTING_INVULNERABLE_TICKS); }
    public boolean shouldShowHole() { return dataManager.get(SHOULD_SHOW_HOLE); }
    public BlockPos getPlayingDeadPodiumPosition() { return podiumPosition; }
    public boolean isOnBack() { return bodyXRotation >= 90.0F; }
    public float getXBodyRot() { return bodyXRotation; }
    public float getXBodyRotO() { return previousBodyXRotation; }
    public float getBodyXRotation(float partialTicks) {
        return previousBodyXRotation + (bodyXRotation - previousBodyXRotation) * partialTicks;
    }
    public float getBodyYRotation(float partialTicks) {
        return prevRenderYawOffset + MathHelper.wrapDegrees(renderYawOffset - prevRenderYawOffset) * partialTicks;
    }
    public float getTentacleAnimation(float partialTicks) {
        return previousTentacleTickCount + (tentacleTickCount - previousTentacleTickCount) * partialTicks;
    }
    public float getFadeAnimation(float partialTicks) {
        return previousOnGroundAnimation + (onGroundAnimation - previousOnGroundAnimation) * partialTicks;
    }
    public float getFadeAnimation() { return onGroundAnimation; }
    public boolean areOtherHeadsDisabled() { return dataManager.get(OTHER_HEADS_DISABLED); }
    public void setMirrored(boolean mirrored) { dataManager.set(MIRRORED, mirrored); }
    public boolean isMirrored() { return dataManager.get(MIRRORED); }
    int getHeadAnimationFlags() { return dataManager.get(HEAD_ANIMATION_FLAGS); }
    boolean isHeadFlagSet(int bit) { return (getHeadAnimationFlags() & bit) != 0; }
    void setHeadFlag(int bit, boolean value) {
        int flags = getHeadAnimationFlags();
        dataManager.set(HEAD_ANIMATION_FLAGS, value ? flags | bit : flags & ~bit);
    }
    public boolean isDeadOrPlayingDead() { return isDead || getHealth() <= 0.0F || isPlayDeadAiDisabled(); }
    public boolean hasRecentlyBeenRevived() { return recentlyRevivedTicks > 0; }
    public boolean isResummoned() { return resummoned; }

    @Override
    public SoundEvent getBossTheme() {
        if (hasRecentlyBeenRevived()) return ModSounds.get("wither_storm_reviving_theme");
        if (getPhase() == 5 && getConsumedMass() > getConsumptionAmountForPhase(5)) {
            return ModSounds.get("wither_storm_formidibomb_theme");
        }
        return ModSounds.get(isBeingTornApart()
                ? "wither_storm_bowels_exposed_theme" : "wither_storm_boss_theme");
    }

    @Override
    public boolean shouldPlayBossTheme() {
        SupplementalEntities.CommandBlockEntity commandBlock = getBowelsCommandBlock();
        boolean commandBlockOverride = commandBlock != null && commandBlock.shouldShowOwnerBossBar();
        return isEntityAlive() && shouldPlaySoundLoop && !isAIDisabled() && !isSilent()
                && (!isDeadOrPlayingDead() || commandBlockOverride);
    }

    @Override
    public int getBossThemePriority() {
        return 1;
    }

    @Override
    public Vec3d getBossThemePosition() {
        return getPositionVector();
    }

    @Override
    public int getBrightnessForRender() {
        return WitherStormPartLogic.applyFadeLight(super.getBrightnessForRender(), getFadeAnimation());
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if ((BODY_X_ROTATION.equals(key) || BODY_Y_ROTATION.equals(key)) && world.isRemote) {
            lerpBodyRotationTo(dataManager.get(BODY_X_ROTATION), dataManager.get(BODY_Y_ROTATION), 3);
        }
        if (PLAY_DEAD_STATE_TICKS.equals(key) && world.isRemote) {
            clientVisualStateTicks = Math.max(0, dataManager.get(PLAY_DEAD_STATE_TICKS));
        }
        if (PHASE.equals(key) || PLAY_DEAD_STATE.equals(key)) updateSizeForPlayDeadState();
    }

    /** 返回与上游搜索范围对应的 1.12 轴对齐搜索盒。 */
    public AxisAlignedBB getSearchBox() {
        double range = getPhase() > 3
                ? getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue()
                : getEntityAttribute(ModAttributes.HUNCHBACK_FOLLOW_RANGE).getAttributeValue();
        return getEntityBoundingBox().grow(range, getPhase() > 3 ? range + 255.0D : range * 2.0D, range);
    }

    public boolean isEntityNearby(Entity entity) {
        return entity != null && getSearchBox().contains(entity.getPositionVector());
    }

    public boolean isAttractingFormidibomb() {
        return attractingFormidibomb;
    }

    public boolean isNearbyTickingFormidibomb() {
        return getNearbyTickingFormidibomb() != null;
    }

    @Nullable
    private PowerfulExplosiveEntity.FormidibombEntity getNearbyTickingFormidibomb() {
        PowerfulExplosiveEntity.FormidibombEntity bomb = getFormidibomb();
        return bomb != null && !bomb.isDead && bomb.getStartFuse() > 0
                && bomb.getFuse() <= 800 ? bomb : null;
    }

    @Nullable
    public PowerfulExplosiveEntity.FormidibombEntity getFormidibomb() {
        if (formidibomb == null && formidibombUuid != null) {
            Entity entity = resolveAny(formidibombUuid);
            if (entity instanceof PowerfulExplosiveEntity.FormidibombEntity) {
                formidibomb = (PowerfulExplosiveEntity.FormidibombEntity) entity;
            }
        }
        return formidibomb;
    }

    public boolean canBeFormidibombed(boolean allowRemovedBomb) {
        if (getPhase() < 5 || getPhase() > 6 && isBeingTornApart()) return false;
        PowerfulExplosiveEntity.FormidibombEntity bomb = getFormidibomb();
        if (bomb == null) return false;
        if (WitherStormConfig.endOfPhaseFiveBombableExclusively && getPhaseProgress() < 1.0F) return false;
        if (!allowRemovedBomb && bomb.isDead) return false;
        return bomb.getFuse() <= 600.0F + getDistance(bomb);
    }

    private void updateFormidibombTarget() {
        PowerfulExplosiveEntity.FormidibombEntity nearest = findNearestVisibleFormidibomb();
        PowerfulExplosiveEntity.FormidibombEntity previous = getFormidibomb();
        if (nearest != null && nearest != previous) {
            if (attractingFormidibomb && previous != null && !previous.isDead) {
                previous.setNoGravity(false);
            }
            formidibomb = nearest;
            formidibombUuid = nearest.getUniqueID();
        }

        boolean shouldAttract = canBeFormidibombed(false);
        if (attractingFormidibomb && !shouldAttract) {
            PowerfulExplosiveEntity.FormidibombEntity bomb = getFormidibomb();
            if (bomb != null && !bomb.isDead) bomb.setNoGravity(false);
        } else if (!attractingFormidibomb && shouldAttract) {
            headManager.startRoar(0);
        }
        attractingFormidibomb = shouldAttract;
    }

    @Nullable
    private PowerfulExplosiveEntity.FormidibombEntity findNearestVisibleFormidibomb() {
        double followDistance = getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue() + 50.0D;
        double horizontalRange = getPhase() > 3 ? followDistance : 40.0D;
        double verticalRange = getPhase() > 3 ? horizontalRange + 255.0D : 20.0D;
        AxisAlignedBB searchArea = getEntityBoundingBox().grow(
                horizontalRange, verticalRange, horizontalRange);
        PowerfulExplosiveEntity.FormidibombEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (PowerfulExplosiveEntity.FormidibombEntity bomb : world.getEntitiesWithinAABB(
                PowerfulExplosiveEntity.FormidibombEntity.class, searchArea)) {
            if (bomb.isDead || !canSeeWithCache(0, bomb)) continue;
            double distance = getDistanceSq(bomb);
            if (distance > followDistance * followDistance) continue;
            if (distance < nearestDistance) {
                nearest = bomb;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void pullFormidibombTowardMainHead() {
        if (!attractingFormidibomb) return;
        PowerfulExplosiveEntity.FormidibombEntity bomb = getFormidibomb();
        if (bomb == null || bomb.isDead) {
            stopAttractingFormidibomb();
            return;
        }
        Vec3d direction = getHeadPosition(0, 1.0F).subtract(bomb.getPositionVector());
        double length = direction.length();
        bomb.setNoGravity(true);
        if (length > 1.0E-6D) {
            bomb.motionX = direction.x / length * 0.1D;
            bomb.motionY = direction.y / length * 0.1D;
            bomb.motionZ = direction.z / length * 0.1D;
        } else {
            bomb.motionX = bomb.motionY = bomb.motionZ = 0.0D;
        }
        bomb.velocityChanged = true;
    }

    private void stopAttractingFormidibomb() {
        PowerfulExplosiveEntity.FormidibombEntity bomb = getFormidibomb();
        if (attractingFormidibomb && bomb != null && !bomb.isDead) bomb.setNoGravity(false);
        attractingFormidibomb = false;
    }

    @Nullable
    public SupplementalEntities.CommandBlockEntity getBowelsCommandBlock() {
        SupplementalEntities.CommandBlockEntity local = getPlayingDeadCommandBlockReference();
        if (local != null && !local.isDead) {
            return local;
        }
        WorldServer bowels = DimensionManager.getWorld(BowelsDimensions.DIMENSION_ID);
        if (bowels == null) return null;
        BowelsInstanceData.Instance instance = BowelsInstanceData.get(bowels).get(getUniqueID());
        if (instance == null || instance.commandBlockUuid == null) return null;
        Entity entity = bowels.getEntityFromUuid(instance.commandBlockUuid);
        return entity instanceof SupplementalEntities.CommandBlockEntity
                ? (SupplementalEntities.CommandBlockEntity) entity : null;
    }
    void playHeadRoarSound(int head) {
        if (!shouldPlayHeadVoice(head)) return;
        Vec3d p = getHeadPosition(head, 1.0F);
        world.playSound(null, p.x, p.y, p.z, ModSounds.get("wither_storm_roar"), SoundCategory.HOSTILE,
                Math.max(6.0F, getSoundVolume() + 2.5F), 1.0F);
    }
    void playHeadBiteSound(int head) {
        Vec3d p = getHeadPosition(head, 1.0F);
        world.playSound(null, p.x, p.y, p.z, ModSounds.get("wither_storm_bite"), SoundCategory.HOSTILE,
                Math.max(2.0F, getSoundVolume()), 1.0F);
    }
    void playHeadHurtSound(int head) {
        if (!shouldPlayHeadVoice(head)) return;
        Vec3d p = getHeadPosition(head, 1.0F);
        world.playSound(null, p.x, p.y, p.z, ModSounds.get("wither_storm_hurt"), SoundCategory.HOSTILE,
                Math.max(6.0F, getSoundVolume() + 2.5F), 1.0F);
    }
    void playHeadTractorBeamActivationSound(int head) {
        Vec3d p = getHeadPosition(head, 1.0F);
        world.playSound(null, p.x, p.y, p.z, ModSounds.get("wither_storm_tractor_beam_activate"),
                SoundCategory.HOSTILE, getSoundVolume() + 2.5F, 1.0F);
    }
    private boolean shouldPlayHeadVoice(int head) {
        if (head < 0 || head >= getTotalHeads()) return false;
        if (areOtherHeadsDisabled() || getPhase() > 1 && getPhase() < 4) return head == 0;
        return getPhase() > 3;
    }

    public void performRangedAttack(int head, EntityLivingBase target) {
        if (target == null) return;
        performRangedAttack(head, target.posX, target.posY + target.getEyeHeight() * 0.5D, target.posZ,
                head == 0 && rand.nextFloat() < 0.001F);
    }

    public void performRangedAttack(int head, double x, double y, double z, boolean dangerous) {
        Vec3d origin = getHeadPosition(head, 1.0F);
        world.playEvent(null, 1024, new BlockPos(posX, posY, posZ), 0);
        EntityWitherSkull skull = new EntityWitherSkull(world, this, x - origin.x, y - origin.y, z - origin.z);
        skull.setInvulnerable(dangerous);
        skull.setPosition(origin.x, origin.y, origin.z);
        world.spawnEntity(skull);
    }

    public void spawnFlamingWitherSkull(int head, double x, double y, double z) {
        Vec3d origin = getHeadPosition(head, 1.0F);
        world.playSound(null, origin.x, origin.y, origin.z, ModSounds.get("wither_storm_shoot"), SoundCategory.HOSTILE,
                Math.max(5.0F, getSoundVolume() - 5.0F), 1.0F);
        double speed = WitherStormConfig.flamingSkullSpeedModifier;
        SupplementalEntities.FlamingWitherSkullEntity skull = new SupplementalEntities.FlamingWitherSkullEntity(world, this,
                (x - origin.x) * speed, (y - origin.y) * speed, (z - origin.z) * speed);
        skull.setPosition(origin.x, origin.y, origin.z);
        world.spawnEntity(skull);
    }

    public void spawnBlueFlamingWitherSkull(int head, double x, double y, double z) {
        Vec3d origin = getHeadPosition(head, 1.0F);
        world.playSound(null, origin.x, origin.y, origin.z, ModSounds.get("wither_storm_shoot"), SoundCategory.HOSTILE,
                Math.max(5.0F, getSoundVolume() - 5.0F), 1.0F);
        double speed = WitherStormConfig.flamingSkullSpeedModifier;
        SupplementalEntities.BlueFlamingWitherSkullEntity skull = new SupplementalEntities.BlueFlamingWitherSkullEntity(world, this,
                (x - origin.x) * speed, (y - origin.y) * speed, (z - origin.z) * speed);
        skull.setPosition(origin.x, origin.y, origin.z);
        world.spawnEntity(skull);
    }
    public int getWatchedTargetId(int head) {
        return dataManager.get(getHeadTargetParameter(head));
    }
    public void updateWatchedTargetId(int head, int targetId) {
        dataManager.set(getHeadTargetParameter(head), Math.max(0, targetId));
    }
    private static DataParameter<Integer> getHeadTargetParameter(int head) {
        switch (MathHelper.clamp(head, 0, 2)) {
            case 1: return SECOND_HEAD_TARGET;
            case 2: return THIRD_HEAD_TARGET;
            default: return FIRST_HEAD_TARGET;
        }
    }
    void updateHeadRotation(int head, float yaw, float pitch) {
        dataManager.set(getHeadYawParameter(head), yaw);
        dataManager.set(getHeadPitchParameter(head), pitch);
    }
    float getSyncedHeadYaw(int head) { return dataManager.get(getHeadYawParameter(head)); }
    float getSyncedHeadPitch(int head) { return dataManager.get(getHeadPitchParameter(head)); }
    private static DataParameter<Float> getHeadYawParameter(int head) {
        switch (MathHelper.clamp(head, 0, 2)) {
            case 1: return SECOND_HEAD_YAW;
            case 2: return THIRD_HEAD_YAW;
            default: return FIRST_HEAD_YAW;
        }
    }
    private static DataParameter<Float> getHeadPitchParameter(int head) {
        switch (MathHelper.clamp(head, 0, 2)) {
            case 1: return SECOND_HEAD_PITCH;
            case 2: return THIRD_HEAD_PITCH;
            default: return FIRST_HEAD_PITCH;
        }
    }
    void setHeadDistractionFlag(int head, boolean distracted) {
        int bit = 1 << MathHelper.clamp(head, 0, 2);
        int flags = dataManager.get(HEAD_DISTRACTION_FLAGS);
        int updated = distracted ? flags | bit : flags & ~bit;
        if (updated != flags) dataManager.set(HEAD_DISTRACTION_FLAGS, updated);
    }
    public boolean isHeadDistracted(int head) {
        return (dataManager.get(HEAD_DISTRACTION_FLAGS) & 1 << MathHelper.clamp(head, 0, 2)) != 0;
    }
    boolean isHeadInjuryFlagSet(int head) {
        return (dataManager.get(HEAD_INJURY_FLAGS) & 1 << MathHelper.clamp(head, 0, 2)) != 0;
    }
    void setHeadInjuryFlag(int head, boolean injured) {
        int bit = 1 << MathHelper.clamp(head, 0, 2);
        int flags = dataManager.get(HEAD_INJURY_FLAGS);
        dataManager.set(HEAD_INJURY_FLAGS, injured ? flags | bit : flags & ~bit);
    }
    public Vec3d getHeadPosition(int head, float partialTicks) { return headManager.getPosition(head, partialTicks); }
    public int getTotalHeads() { return 3; }
    public AxisAlignedBB getHeadBounds(int head) { return headManager.getBounds(head); }
    public AxisAlignedBB getHeadBounds(int head, float partialTicks) {
        return headManager.getBounds(head, partialTicks);
    }
    public AxisAlignedBB[] getBodySectionBounds() { return sectionManager.getBodySectionBounds(); }
    @Nullable public AxisAlignedBB getBowelsEntranceBounds() {
        return sectionManager.getBowelsEntranceBounds();
    }
    public WitherStormHeadManager getHeadManager() { return headManager; }
    @Nullable public Vec3d getDistractedPos(int head) { return headManager.getDistractedPos(head); }
    public void setDistractedPos(int head, @Nullable Vec3d position) {
        headManager.setDistractedPos(head, position);
    }
    public void setLookAt(int head, @Nullable Vec3d position, int steps) {
        headManager.setLookAt(head, position, steps);
    }
    public void setLookAt(int head, @Nullable Vec3d position) {
        setLookAt(head, position, 3);
    }
    public EntityLivingBase getTarget(int head) { return headManager.getTarget(head); }
    public void setTarget(int head, EntityLivingBase target) { headManager.setTarget(head, target); }
    public boolean isHeadInjured(int head) { return headManager.isHeadInjured(head); }
    public boolean canBeDistracted(int head) { return getPhase() > 3 && tractorBeamActive(head); }
    public void makeDistracted(Vec3d position, int ticks, int head) {
        headManager.makeDistracted(head, position, ticks);
    }
    public int getHeadInjuryTicks(int head) { return headManager.getHeadInjuryTicks(head); }
    public boolean attackHead(int head, Entity attacker) { return headManager.attemptAttack(head, attacker, 20); }
    public boolean attackHeadFromExplosion(int head, Entity attacker) {
        return headManager.attackFromExplosion(head, attacker);
    }
    public boolean canPlayerReachHead(EntityPlayer player, int head, double reach) {
        if (player == null || player.world != world || player.isDead || player.isSpectator()
                || head < 0 || head >= getTotalHeads() || reach <= 0.0D) return false;
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d end = eye.add(player.getLook(1.0F).scale(reach));
        AxisAlignedBB bounds = getHeadBounds(head);
        return bounds.contains(eye) || bounds.calculateIntercept(eye, end) != null;
    }
    boolean isTrackedForConsumption(Entity entity) {
        return entity != null && trackedEntities.containsKey(entity.getUniqueID());
    }
    void trackEntityFromSegment(Entity entity) { trackEntityToConsume(entity); }
    @Nullable
    SupplementalEntities.BlockClusterEntity createDefaultClusterForSegment(
            SupplementalEntities.WitherStormSegmentEntity segment) {
        return clusterManager.createDefaultClusterForSegment(segment);
    }
    void notifySegmentConsumption(Entity segment, Entity entity, int amount) {
        WitherStormConsumeEvent event = new WitherStormConsumeEvent(this, segment, entity, amount);
        if (!MinecraftForge.EVENT_BUS.post(event)) addConsumedMass(event.getConsumedAmount());
    }
    void consumeEntityFromSegment(Entity entity, EntityLivingBase segment) {
        if (entity instanceof EntityLivingBase) {
            entity.attackEntityFrom(ModDamageSources.witherStormAttackMob(segment), Float.MAX_VALUE);
        } else if (entity != null) {
            entity.setDead();
        }
    }
    boolean isTargetedByMainHeadFamily(Entity entity) {
        if (entity == null) return false;
        for (int head = 0; head < getTotalHeads(); head++) {
            if (getTarget(head) == entity) return true;
        }
        return false;
    }
    boolean isTargetInUseBySegment(Entity entity) {
        if (entity == null) return false;
        for (int index = 0; index < segmentUuids.length; index++) {
            SupplementalEntities.WitherStormSegmentEntity segment = getLoadedSegment(index);
            if (segment != null && segment.isTargeting(entity)) return true;
        }
        return false;
    }
    boolean isValidStormTarget(@Nullable EntityLivingBase entity) {
        if (!isStormTargetType(entity)
                || entity == null || entity == this || !entity.isEntityAlive()
                || entity.world != world || entity.dimension != dimension
                || getPhase() <= 3 && entity instanceof EntitySquid) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            return !player.capabilities.disableDamage && !player.isSpectator()
                    && !hasRecentlyBeenRevived()
                    && !SymbiontSummoningManager.shouldIgnorePlayer(player);
        }
        return true;
    }
    static boolean isStormTargetType(@Nullable EntityLivingBase entity) {
        return entity != null
                && !(entity instanceof WitherStormEntity)
                && !(entity instanceof SupplementalEntities.StormPartBase)
                && !(entity instanceof SickenedMobEntity)
                && !(entity instanceof EntityWither)
                && !(entity instanceof EntityFlying)
                && !(entity instanceof EntityDragon)
                && !(entity instanceof EntityAmbientCreature)
                && !(entity instanceof EntityArmorStand)
                && !UpstreamEntityTags.contains(
                        UpstreamEntityTags.WITHER_STORM_TARGETING_BLACKLIST, entity);
    }
    boolean canPullVehicle(Entity vehicle) {
        return !(vehicle instanceof EntityLivingBase)
                || isStormTargetType((EntityLivingBase) vehicle);
    }
    boolean isBlockingWithShield(EntityLivingBase entity) {
        return entity instanceof EntityPlayer
                && ((EntityPlayer) entity).isHandActive()
                && ((EntityPlayer) entity).getActiveItemStack().getItem() == Items.SHIELD;
    }
    boolean isPositionBehindBack(Vec3d position) {
        if (position == null) return false;
        float angle = (float) (MathHelper.atan2(position.x - posX, position.z - posZ)
                * 180.0D / Math.PI);
        float difference = MathHelper.wrapDegrees(-renderYawOffset - angle + 180.0F);
        return difference > 80.0F || difference < -80.0F;
    }
    public boolean isPosBehindBack(Vec3d position) {
        return isPositionBehindBack(position);
    }
        public boolean isEntityBehindBack(Entity entity) {
        return entity != null && isPositionBehindBack(entity.getPositionVector());
    }
    boolean isInsideOtherTractorBeam(Entity entity, int excludedHead) {
        if (entity == null) return false;
        int containingHead = findContainingTractorBeamHead(entity, 5.0D);
        if (containingHead >= 0 && containingHead != excludedHead) return true;
        for (int index = 0; index < segmentUuids.length; index++) {
            SupplementalEntities.WitherStormSegmentEntity segment = getLoadedSegment(index);
            if (segment == null) continue;
            containingHead = segment.getSegmentManager().findContainingTractorBeamHead(entity, 5.0D);
            if (containingHead >= 0 && containingHead != excludedHead) return true;
        }
        return false;
    }

    public int findContainingTractorBeamHead(Entity entity, double radius) {
        if (entity == null) return -1;
        for (int head = 0; head < getTotalHeads(); head++) {
            if (!tractorBeamActive(head)) continue;
            Vec3d origin = getHeadPosition(head, 1.0F);
            if (TractorBeamHelper.isInsideTractorBeam(entity.getPositionVector(), origin,
                    headManager.getLookVector(head), headManager.getTractorBeamCutoff(head), radius)) {
                return head;
            }
        }
        return -1;
    }

    public boolean isInsideTractorBeam(Entity entity, double radius) {
        return findContainingTractorBeamHead(entity, radius) >= 0;
    }

    /** 上游不让已经骑乘在风暴家族附近实体上的乘客重复成为目标。 */
    boolean isPassengerTarget(Entity entity) {
        if (entity == null) return false;
        AxisAlignedBB nearby = getEntityBoundingBox().grow(10.0D, 255.0D, 10.0D);
        for (EntityLivingBase other : world.getEntitiesWithinAABB(EntityLivingBase.class, nearby)) {
            if (other != entity && other.isRidingSameEntity(entity)) return true;
        }
        return false;
    }
    public UltimateTargetManager getUltimateTargetManager() { return targetManager; }
    public IgnoredTargetsManager getIgnoredTargetsManager() { return ignoredTargetsManager; }
    public IgnoredTargetsManager getIgnoredTargets() { return ignoredTargetsManager; }
    public EvolutionProfiler getEvolutionProfiler() { return evolutionProfiler; }
    public EntityLivingBase getUltimateTarget() { return targetManager.getUltimateTarget(); }
    public Vec3d getUltimateTargetPos() { return targetManager.getMovementTargetPos(); }
    public boolean isUltimateTargetStationary() { return targetManager.isTargetStationary(); }
    public boolean isDistracted() { return targetManager.isDistracted(); }
    public boolean shouldSpeedUp() {
        Vec3d target = targetManager.getUltimateTargetPos();
        return getPhase() >= 4 && WitherStormConfig.shouldChaseWhenTargetStopped
                && target != null && targetManager.isTargetStationary()
                && getPositionVector().distanceTo(target) > 122.0D
                && !targetManager.isDistracted();
    }

    double getDefaultChasingSpeed() {
        return attributeOrConfigValue(ModAttributes.TARGET_STATIONARY_FLYING_SPEED,
                WitherStormConfig.chasingFlyingSpeed);
    }

    double getDefaultNormalSpeed() {
        return attributeOrConfigValue(ModAttributes.SLOW_FLYING_SPEED,
                WitherStormConfig.normalFlyingSpeed);
    }

    private double attributeOrConfigValue(IAttribute attribute, double configValue) {
        IAttributeInstance instance = getEntityAttribute(attribute);
        double value = instance.getAttributeValue();
        return Double.compare(value, attribute.getDefaultValue()) == 0 ? configValue : value;
    }

    private void applyResummonedEvolutionModifier() {
        IAttributeInstance evolution = getEntityAttribute(ModAttributes.EVOLUTION_SPEED);
        AttributeModifier existing = evolution.getModifier(RESUMMONED_EVOLUTION_MODIFIER_ID);
        if (existing != null) evolution.removeModifier(existing);
        evolution.applyModifier(new AttributeModifier(RESUMMONED_EVOLUTION_MODIFIER_ID,
                "resummonedModifier", -0.5D, 0));
    }

    private void migrateLegacyEvolutionAttribute(NBTTagCompound compound, boolean wasResummoned) {
        if (hasSerializedAttribute(compound, ModAttributes.EVOLUTION_SPEED)) return;
        IAttributeInstance evolution = getEntityAttribute(ModAttributes.EVOLUTION_SPEED);
        double legacyValue = readEvolutionSpeedModifier(compound, wasResummoned);
        if (wasResummoned) {
            evolution.setBaseValue(legacyValue + 0.5D);
            applyResummonedEvolutionModifier();
        } else {
            evolution.setBaseValue(legacyValue);
        }
    }

    private static boolean hasSerializedAttribute(NBTTagCompound compound, IAttribute attribute) {
        NBTTagList attributes = compound.getTagList("Attributes", 10);
        for (int index = 0; index < attributes.tagCount(); index++) {
            if (attribute.getName().equals(attributes.getCompoundTagAt(index).getString("Name"))) {
                return true;
            }
        }
        return false;
    }
    public boolean shouldTrackUltimateTarget() {
        if (isDeadOrPlayingDead()) return false;
        SupplementalEntities.CommandBlockEntity commandBlock = getBowelsCommandBlock();
        return commandBlock == null || commandBlock.getHealth() >= commandBlock.getMaxHealth();
    }
    public boolean shouldRotateTowardsUltimateTarget() {
        if (isDeadOrPlayingDead()) return false;
        SupplementalEntities.CommandBlockEntity commandBlock = getBowelsCommandBlock();
        return commandBlock == null || commandBlock.getHealth()
                / Math.max(1.0F, commandBlock.getMaxHealth()) > 0.25F;
    }
    public boolean canSee(int head, Entity entity) {
        if (entity == null || entity.world != world) return false;
        Vec3d start = getHeadPosition(head, 1.0F);
        Vec3d end = entity.getPositionEyes(1.0F);
        RayTraceResult hit = world.rayTraceBlocks(start, end, false, true, false);
        return hit == null || hit.typeOfHit == RayTraceResult.Type.MISS;
    }

    /** 性能优化：同 tick 内缓存头部视线结果，同一 tick 内同一头对同一实体只做一次射线（检测机制不变）。 */
    boolean canSeeWithCache(int head, Entity entity) {
        if (entity == null || entity.world != world) return false;
        long cycle = world.getTotalWorldTime();
        if (cycle != pullSightCacheCycle) {
            pullSightCacheCycle = cycle;
            pullSightCache.clear();
        }
        String key = head + ":" + entity.getUniqueID();
        Boolean cached = pullSightCache.get(key);
        if (cached != null) return cached;
        boolean result = canSee(head, entity);
        pullSightCache.put(key, result);
        return result;
    }

    public float getHeadYRotation(int head) {
        return getHeadYRotation(head, 1.0F);
    }

    public float getHeadYRotation(int head, float partialTicks) {
        return headManager.getYaw(head, partialTicks);
    }

    /** Applies the complete upstream structure-summon rotation before the entity enters the world. */
    public void initializeStructureSummonYaw(float yaw) {
        rotationYaw = prevRotationYaw = yaw;
        renderYawOffset = prevRenderYawOffset = yaw;
        rotationYawHead = prevRotationYawHead = yaw;
        clientBodyYRotationTarget = yaw;
        clientBodyXRotationSteps = 0;
        dataManager.set(BODY_Y_ROTATION, yaw);
        headManager.initializeAdditionalHeadYaw(yaw);
    }

    public float getHeadXRotation(int head) {
        return getHeadXRotation(head, 1.0F);
    }

    public float getHeadXRotation(int head, float partialTicks) {
        return headManager.getPitch(head, partialTicks);
    }

    @Override
    public Vec3d getHeadPos(int head) {
        return getHeadPosition(head, 1.0F);
    }

    @Override
    public float getHeadYRot(int head) {
        return getHeadYRotation(head);
    }

    @Override
    public float getHeadYRotO(int head) {
        return getHeadYRotation(head, 0.0F);
    }

    @Override
    public float getHeadXRot(int head) {
        return getHeadXRotation(head);
    }

    @Override
    public float getHeadXRotO(int head) {
        return getHeadXRotation(head, 0.0F);
    }

    @Override
    public float getHeadShakeAnim(int head, float partialTicks) {
        return getHeadShakeAnimation(head, partialTicks);
    }

    @Override
    public boolean isDistracted(int head) {
        return headManager.isDistracted(head);
    }
    @Override
    public Vec3d getHeadPositionForBeam(int head) { return getHeadPosition(head, 1.0F); }
    @Override
    public Vec3d getHeadPositionForBeam(int head, float partialTicks) {
        return getHeadPosition(head, partialTicks);
    }
    @Override
    public Vec3d getHeadDirectionForBeam(int head) { return headManager.getLookVector(head); }
    @Override
    public Vec3d getHeadDirectionForBeam(int head, float partialTicks) {
        return headManager.getLookVector(head, partialTicks);
    }
    @Override
    public double getTractorBeamCutoffDistance(int head) {
        return headManager.getTractorBeamCutoff(head);
    }
    @Override
    public double getTractorBeamCutoffDistance(int head, float partialTicks) {
        if (!world.isRemote) return getTractorBeamCutoffDistance(head);
        return TractorBeamHelper.findCutoffDistance(world,
                getHeadPositionForBeam(head, partialTicks),
                getHeadDirectionForBeam(head, partialTicks), 250.0D);
    }
    public boolean isArmored() { return getInvulnerableTicks() > 900 && getPhase() < 4; }
    public boolean isCompletelyInvulnerable() { return WitherStormConfig.witherStormInvulnerability; }

    public boolean shouldPlaySoundLoops() {
        return shouldPlaySoundLoop && !isSilent() && !isDeadOrPlayingDead();
    }

    public static SoundEvent getSoundForLoop(int phase, float distanceFade) {
        if (phase < 3) return ModSounds.get("command_block_pulse_loop");
        if (phase == 3) return ModSounds.get("wither_storm_loop");
        if (distanceFade > 3.0F && distanceFade < 6.0F) {
            return ModSounds.get("wither_storm_distant_loop");
        }
        if (distanceFade > 6.0F) return ModSounds.get("wither_storm_far_loop");
        return ModSounds.get("wither_storm_close_loop");
    }

    public static float getSoundLoopAttenuationDistance(int phase) {
        if (phase < 3) return 16.0F;
        if (phase == 3) return 128.0F;
        return 1024.0F;
    }

    public static boolean isOccludedSound(SoundEvent sound) {
        return sound != null && (sound == ModSounds.get("wither_storm_ambient")
                || sound == ModSounds.get("wither_storm_hurt")
                || sound == ModSounds.get("wither_storm_shoot")
                || sound == ModSounds.get("wither_storm_bite")
                || sound == ModSounds.get("wither_storm_roar")
                || sound == ModSounds.get("wither_storm_tractor_beam_activate"));
    }

    private void synchronizeSoundLoop() {
        boolean active = shouldPlaySoundLoops();
        if (active) {
            ModNetwork.updateWitherStormLoop(this);
        } else if (soundLoopActive) {
            ModNetwork.removeWitherStormLoop(this);
        }
        soundLoopActive = active;
    }

    public void playSoundToEveryone(SoundEvent sound, float volume, float pitch) {
        if (WitherStormConfig.shouldPlayGlobalSoundsCrossDimensionally) {
            ModNetwork.playGlobalSoundAll(world, sound, volume, pitch);
        } else {
            ModNetwork.playGlobalSound(world, sound, volume, pitch);
        }
    }
    public float getMouthAnimation(int head, float partialTicks) {
        return headManager.getMouth(head, partialTicks);
    }
    public float getBrokenJawAnimation(int head, float partialTicks) {
        return headManager.getBrokenAnimation(head, partialTicks);
    }
    public float getHeadShakeAnimation(int head, float partialTicks) {
        return headManager.getShakeRoll(head, partialTicks);
    }
    public int getHeadHurtDuration(int head) { return headManager.getHeadHurtDuration(head); }
    public void handleHeadAttackedOnClient(int head) { headManager.handleHeadAttackedOnClient(head); }
    public boolean isBeingTornApart() {
        return isBeingTornApart(getPhase(), getConsumedMass(), getConsumptionAmountForPhase(7),
                shouldShowHole(), dataManager.get(HOLE_ENABLED));
    }
    static boolean isBeingTornApart(int phase, int consumedMass, int phaseRequirement,
                                    boolean explicitlyShown, boolean holeEnabled) {
        return holeEnabled && phase >= 7 && (consumedMass >= phaseRequirement || explicitlyShown);
    }
    public void setShouldShowHole(boolean value) { dataManager.set(SHOULD_SHOW_HOLE, value); }

    @Override
    public boolean canBeAttackedWithItem() {
        // Let the attack packet reach attackEntityFrom; the server-side damage
        // path owns the summoning invulnerability check.  Gating this on the
        // client-side timer can discard legitimate phase 0-3 attacks when the
        // synced timer is one tick behind.
        return getPhase() < 4;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // 1.12.2 没有伤害类型标签，允许伤害创造模式是绕过实体无敌的对应语义。
        if (source.canHarmInCreative()) return super.attackEntityFrom(source, amount);
        Entity attacker = source.getTrueSource();
        if (isEntityInvulnerable(source)
                || source == DamageSource.DROWN
                || attacker instanceof WitherStormEntity
                || attacker instanceof SickenedMobEntity) {
            return false;
        }
        if (isPlayDeadAiDisabled() || getInvulnerableTicks() > 0
                || isCompletelyInvulnerable() && getPhase() > 3) {
            return false;
        }
        if (isArmored() && source.getImmediateSource() instanceof EntityArrow) return false;
        if (!world.isRemote) {
            if (destroyBlocksTick <= 0) destroyBlocksTick = 20;
            headManager.onHurt();
        }
        return super.attackEntityFrom(source, amount);
    }

    /** 在原版图腾死亡保护检查尾部执行上游的低阶段濒死进化。 */
    public boolean tryEvolveFromDeathProtection(DamageSource source) {
        if (!isCompletelyInvulnerable() || getPhase() >= 4
                || getHealth() / getMaxHealth() > 0.1F) return false;
        evolveToPhase(4);
        setHealth(getMaxHealth());
        if (source.getTrueSource() instanceof EntityPlayerMP) {
            ModCriteriaTriggers.NEARLY_KILL_WITHER_STORM.trigger(
                    (EntityPlayerMP) source.getTrueSource(), this);
        }
        return true;
    }

    private void triggerNearby(EntityTrigger trigger,
                               double range) {
        for (EntityPlayerMP player : world.getEntitiesWithinAABB(EntityPlayerMP.class,
                getEntityBoundingBox().grow(range))) {
            trigger.trigger(player, this);
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        setNoGravity(false);
        noClip = false;
        if (!world.isRemote) {
            ModNetwork.removeWitherStormLoop(this);
            soundLoopActive = false;
            stopAttractingFormidibomb();
            releaseConsumedPetsAndCureSickened();
            if (getPhase() > 3) {
                world.playSound(null, getPosition(), ModSounds.get("wither_storm_death"),
                        SoundCategory.HOSTILE, 20.0F, 1.0F);
                headManager.onDeath();
                clearTrackedEntities(false);
            }
            beginSegmentDeathSequences();
        }
    }

    @Override
    protected void onDeathUpdate() {
        boolean usesExtendedDeath = getPhase() > 3;
        if (usesExtendedDeath) ++witherStormDeathTime;
        else ++deathTime;

        if (world.isRemote && usesExtendedDeath) updateDeathDebris();
        if (!world.isRemote) {
            if (getPhase() > 5 && witherStormDeathTime < 240
                    && ForgeEventFactory.getMobGriefingEvent(world, this)) {
                dropDeathClusters();
            }
            if (usesExtendedDeath && witherStormDeathTime == 360) {
                releaseFinalDeathLoot();
                releaseConsumedPetsAndCureSickened();
                setDead();
            } else if (!usesExtendedDeath && deathTime == 20) {
                setDead();
            }
            legacyBossInfo.setPercent(usesExtendedDeath
                    ? MathHelper.clamp(1.0F - witherStormDeathTime / 360.0F, 0.0F, 1.0F)
                    : 1.0F);
        }
        if (!isDead) tickFallingMovement(true);
    }

    private void updateDeathDebris() {
        previousShineAlpha = shineAlpha;
        if (ticksExisted % 20 == 0) disableRandomDebris();
        setDebrisAlpha(Math.max(0.0F, (360.0F - witherStormDeathTime) / 360.0F));
    }

    private void dropDeathClusters() {
        int interval = Math.max(1, 240 / Math.max(1, getPhase()));
        if (witherStormDeathTime % interval == 0) dropMassCluster(Math.max(1, getPhase() - 2));
        if (witherStormDeathTime % 5 == 0) dropMassCluster(2);
        if (witherStormDeathTime > 5) {
            for (int index = 0; index < 3; index++) dropSmallMassCluster(1);
        }
    }

    private void dropMassCluster(int radius) {
        SupplementalEntities.BlockClusterEntity cluster =
                MassClusterBuilder.buildLargeDeathCluster(world, rand, radius);
        if (cluster.getBlocks().isEmpty()) return;
        cluster.setPosition(posX, posY + getUnmodifiedHeight() * 0.5D, posZ);
        cluster.setSink(radius / 2 + 1);
        cluster.motionX = rand.nextGaussian() * 0.3D;
        cluster.motionY = 0.0D;
        cluster.motionZ = rand.nextGaussian() * 0.3D;
        cluster.setRotationDelta(rand.nextInt(20) * 0.15F, rand.nextInt(20) * 0.15F);
        cluster.setAntiStacking(true);
        world.spawnEntity(cluster);
    }

    /** 对应上游 debug deathClusters drop。 */
    public void debugDropDeathClusters() {
        dropMassCluster(Math.max(1, getPhase()));
    }

    private void dropSmallMassCluster(int radius) {
        SupplementalEntities.BlockClusterEntity cluster =
                MassClusterBuilder.buildSmallDeathCluster(world, rand, radius);
        if (cluster.getBlocks().isEmpty()) return;
        cluster.setPosition(posX + rand.nextGaussian() * 20.0D,
                posY + getUnmodifiedHeight() * 0.5D + rand.nextGaussian() * 40.0D,
                posZ + rand.nextGaussian() * 20.0D);
        cluster.setSink(-1);
        cluster.motionX = rand.nextGaussian() * 0.6D;
        cluster.motionY = rand.nextGaussian() * 0.3D;
        cluster.motionZ = rand.nextGaussian() * 0.6D;
        cluster.setRotationDelta(rand.nextInt(90) * 0.15F, rand.nextInt(90) * 0.15F);
        world.spawnEntity(cluster);
    }

    public float getUnmodifiedHeight() {
        return PHASE_HEIGHT[MathHelper.clamp(getPhase(), 0, PHASE_HEIGHT.length - 1)];
    }

    /** 上游 getDeathTime：扩展死亡使用 360 帧的撕裂计时，普通死亡沿用原版计时。 */
    public int getDeathTime() {
        return getPhase() > 3 ? witherStormDeathTime : deathTime;
    }

    @Override
    public float getEyeHeight() {
        return getPhase() > 3 ? 17.5F : super.getEyeHeight();
    }

    private void clearTrackedEntities(boolean destroyClusters) {
        if (destroyClusters) {
            Iterator<Map.Entry<UUID, Entity>> iterator = trackedEntities.entrySet().iterator();
            while (iterator.hasNext()) {
                Entity entity = iterator.next().getValue();
                if (entity instanceof SupplementalEntities.BlockClusterEntity) {
                    iterator.remove();
                    if (!entity.isDead) entity.setDead();
                }
            }
        }
        for (Entity entity : trackedEntities.values()) {
            if (entity == null || entity.isDead) continue;
            entity.setNoGravity(false);
            if (entity instanceof SupplementalEntities.BlockClusterEntity) {
                ((SupplementalEntities.BlockClusterEntity) entity).setPhysics(true);
            }
        }
        trackedEntities.clear();
    }

    /** 还原肠道命令方块每次非致死受击对主体与全部分裂体的联动。 */
    public void reactToCommandBlockDamage() {
        reactToCommandBlockDamage(rand);
    }

    /** 使用命令方块实体的随机源，保持主体与分裂体头部受伤抽样顺序和上游一致。 */
    public void reactToCommandBlockDamage(Random random) {
        if (world.isRemote || isDead) return;
        Random damageRandom = random == null ? rand : random;
        for (int index = 0; index < segmentUuids.length; index++) {
            SupplementalEntities.WitherStormSegmentEntity segment = getLoadedSegment(index);
            if (segment == null || segment.isDead) continue;
            segment.releaseTrackedEntities();
            for (int head = 0; head < segment.getTotalHeads(); head++) {
                if (damageRandom.nextFloat() > 0.6F) segment.hurtHeadDirectly(head, null);
            }
        }
        clearTrackedEntities(false);
        for (int head = 0; head < getTotalHeads(); head++) {
            if (damageRandom.nextFloat() > 0.6F) headManager.hurtDirectly(head, null);
        }
    }

    public void finishBowelsDeath(@Nullable Entity killer) {
        if (world.isRemote || isDead || getHealth() <= 0.0F) return;
        DamageSource source = killer instanceof EntityPlayer
                ? ModDamageSources.playerAttackWitherStorm((EntityPlayer) killer)
                : killer instanceof EntityLivingBase
                ? ModDamageSources.mobAttackWitherStorm((EntityLivingBase) killer)
                : DamageSource.OUT_OF_WORLD;
        // The upstream final phase deals Float.MAX_VALUE through its dedicated
        // damage type. Clear 1.12's generic hurt window before doing the same so
        // an earlier maximum-damage attempt cannot reject the terminal hit.
        hurtResistantTime = 0;
        attackEntityFrom(source, Float.MAX_VALUE);
        if (getHealth() <= 0.0F || isDead) return;

        // A completed bowels fight is authoritative. Some 1.12 compatibility
        // handlers can consume the attack before EntityLivingBase reaches
        // damageEntity; start the normal death sequence with the same source.
        WitherStormMod.LOGGER.warn("Bowels fight completed for storm {} but the terminal damage was rejected; "
                + "starting the death sequence directly", getUniqueID());
        if (killer instanceof EntityPlayer) {
            attackingPlayer = (EntityPlayer) killer;
            recentlyHit = 100;
        }
        setHealth(0.0F);
        onDeath(source);
    }

    private void releaseFinalDeathLoot() {
        if (deathLootReleased) return;
        deathLootReleased = true;
        double range = getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue() + 50.0D;
        EntityPlayer nearest = world.getClosestPlayer(posX, posY, posZ, range, false);
        if (nearest == null) return;

        EntityItem star = new EntityItem(world, nearest.posX,
                nearest.posY + 2.0D, nearest.posZ,
                new ItemStack(ModItems.get("withered_nether_star")));
        star.motionX = 0.0D;
        star.motionY = -0.08D;
        star.motionZ = 0.0D;
        star.setNoGravity(true);
        world.spawnEntity(star);

        int remainingExperience = ForgeEventFactory.getExperienceDrop(this, attackingPlayer, experienceValue);
        while (remainingExperience > 0) {
            int splitExperience = EntityXPOrb.getXPSplit(remainingExperience);
            remainingExperience -= splitExperience;
            world.spawnEntity(new EntityXPOrb(world, nearest.posX, nearest.posY + 10.0D,
                    nearest.posZ, splitExperience));
        }
    }

    private void releaseConsumedPetsAndCureSickened() {
        if (deathRewardsReleased) return;
        deathRewardsReleased = true;
        BlockPos currentPosition = getPosition();
        BlockPos surface = new BlockPos(currentPosition.getX(),
                WorldUtil.getMotionBlockingHeightIgnoringLeaves(world,
                        currentPosition.getX(), currentPosition.getZ()),
                currentPosition.getZ());
        spawnConsumedPets(new Vec3d(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D));
        cureSickenedInArea(world, getSearchBox());
    }

    static void cureSickenedInArea(World world, AxisAlignedBB area) {
        for (EntityLivingBase living : world.getEntitiesWithinAABB(EntityLivingBase.class, area)) {
            WitherSicknessTracker tracker = WitherSicknessCapability.get(living);
            if (tracker != null) tracker.cure();
            if (living instanceof SickenedMobEntity && !living.isDead) {
                TaintingManager.cureEntity((SickenedMobEntity) living);
            }
        }
    }

    @Override
    public void setDead() {
        if (!world.isRemote && getHealth() <= 0.0F && !deathRewardsReleased) {
            releaseConsumedPetsAndCureSickened();
        }
        if (!world.isRemote) {
            ModNetwork.removeWitherStormLoop(this);
            soundLoopActive = false;
            ChunkLoadingManager.INSTANCE.releaseEntity(world, "storm", getUniqueID());
        }
        pendingBowelsTransfers.clear();
        removeAttached(segmentUuids, true);
        clearTrackedEntities(getHealth() <= 0.0F);
        removeCommandBlockCore();
        super.setDead();
    }

    /** 父类私有 Boss 条无法在倒地时隐藏，因此由移植实体独占玩家跟踪。 */
    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        if (player == null) return;
        trackingPlayers.add(player);
        boolean hasAccess = !WitherStormConfig.smartBossbar
                || BossVisibility.canSeeOrIsNotInSmallArea(this, player);
        bossThemeAccess.put(player.getUniqueID(), hasAccess);
        ModNetwork.sendBossThemeAccess(player, this, hasAccess);
        ModNetwork.createDebris(player, this, isDeadOrPlayingDead());
        SupplementalEntities.CommandBlockEntity commandBlock = getBowelsCommandBlock();
        if (commandBlock != null) commandBlock.addOutsideBossBarViewer(player);
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        trackingPlayers.remove(player);
        if (player != null) bossThemeAccess.remove(player.getUniqueID());
        legacyBossInfo.removePlayer(player);
        SupplementalEntities.CommandBlockEntity commandBlock = getBowelsCommandBlock();
        if (commandBlock != null) commandBlock.removeOutsideBossBarViewer(player);
    }

    @Override
    public void setCustomNameTag(String name) {
        super.setCustomNameTag(name);
        legacyBossInfo.setName(getDisplayName());
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        headManager.writeToNBT(compound);

        NBTTagCompound playDeadManager = new NBTTagCompound();
        playDeadManager.setBoolean("PodiumPlaced", podiumPlaced);
        playDeadManager.setBoolean("PodiumOffsetCorrected", podiumOffsetCorrected);
        if (podiumPosition != null) {
            playDeadManager.setTag("PodiumPos", NBTUtil.createPosTag(podiumPosition));
        }
        playDeadManager.setInteger("StateTicks", stateTicks);
        playDeadManager.setInteger("State", getPlayDeadState().ordinal());
        playDeadManager.setInteger("RevivalTime", recentlyRevivedTicks);
        playDeadManager.setBoolean("RecentlyRevived", recentlyRevivedTicks > 0);
        playDeadManager.setInteger("CommandBlockMissingTicks", missingCommandBlockTicks);
        compound.setTag("PlayDeadManager", playDeadManager);

        compound.setInteger("Phase", getPhase());
        compound.setInteger("Invul", getInvulnerableTicks());
        compound.setInteger("StartingInvul", getStartingInvulnerableTicks());
        compound.setInteger("ConsumedEntities", getConsumedMass());
        compound.setBoolean("OtherHeadsDisabled", areOtherHeadsDisabled());
        targetManager.save(compound);
        compound.setFloat("YBodyRot", renderYawOffset);
        compound.setFloat("XBodyRot", bodyXRotation);
        compound.setBoolean("Mirrored", isMirrored());
        compound.setInteger("SymbiontSummoningCooldown", summoningManager.getSummoningDelay());
        compound.setBoolean("ShouldShowHole", shouldShowHole());
        compound.setBoolean("Resummoned", resummoned);
        NBTTagCompound profilerTag = new NBTTagCompound();
        evolutionProfiler.writeToNBT(profilerTag);
        compound.setTag("EvolutionProfiler", profilerTag);
        ConsumedPetStorage.write(compound, "ConsumedPets", consumedPets);

        NBTTagCompound trackedEntitiesTag = new NBTTagCompound();
        NBTTagList trackedEntityList = new NBTTagList();
        Set<UUID> trackedIds = new LinkedHashSet<UUID>(trackedEntities.keySet());
        trackedIds.addAll(savedTrackedEntities);
        for (UUID uuid : trackedIds) trackedEntityList.appendTag(NBTUtil.createUUIDTag(uuid));
        trackedEntitiesTag.setTag("Entities", trackedEntityList);
        compound.setTag("TrackedEntities", trackedEntitiesTag);
        compound.setTag("IgnoredTargets", ignoredTargetsManager.writeToNBT());

        NBTTagList jukeboxes = new NBTTagList();
        for (BlockPos position : playingJukeboxes) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("Pos", NBTUtil.createPosTag(position));
            jukeboxes.appendTag(entry);
        }
        compound.setTag("PlayingJukeboxes", jukeboxes);
        compound.setBoolean("IsConsumptionLocked", consumptionLocked);

        // 1.12 端额外需要这些实体引用与终局幂等状态；它们不替代上游键。
        compound.setDouble(EVOLUTION_SPEED_NBT_KEY, getEvolutionSpeedModifier());
        writeUuid(compound, "WitherStormFormidibomb", formidibombUuid);
        writeUuid(compound, "WitherStormCommandBlock", commandBlockUuid);
        for (int i = 0; i < segmentUuids.length; i++) writeUuid(compound, "WitherStormSegment" + i, segmentUuids[i]);
        compound.setInteger("WitherStormDeathTime", witherStormDeathTime);
        compound.setBoolean("WitherStormDeathRewardsReleased", deathRewardsReleased);
        compound.setBoolean("WitherStormDeathLootReleased", deathLootReleased);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        restoredFromPersistentData = compound.hasKey("UUIDMost", 99)
                && compound.hasKey("UUIDLeast", 99);
        super.readEntityFromNBT(compound);
        resummoned = compound.hasKey("Resummoned", 1)
                ? compound.getBoolean("Resummoned") : compound.getBoolean("WitherStormResummoned");
        evolutionProfiler.readFromNBT(compound.hasKey("EvolutionProfiler", 10)
                ? compound.getCompoundTag("EvolutionProfiler") : new NBTTagCompound());
        migrateLegacyEvolutionAttribute(compound, resummoned);
        int phase = compound.hasKey("Phase", 3)
                ? compound.getInteger("Phase") : compound.getInteger("WitherStormPhase");
        int consumedMass = compound.hasKey("ConsumedEntities", 3)
                ? compound.getInteger("ConsumedEntities") : compound.getInteger("WitherStormConsumedMass");
        setPhase(MathHelper.clamp(phase, 0, 7), consumedMass);
        lastConsumedMass = getConsumedMass();

        NBTTagCompound playDeadManager = compound.hasKey("PlayDeadManager", 10)
                ? compound.getCompoundTag("PlayDeadManager") : null;
        int playDeadState = playDeadManager == null
                ? compound.getInteger("WitherStormPlayDeadState") : playDeadManager.getInteger("State");
        dataManager.set(PLAY_DEAD_STATE, MathHelper.clamp(playDeadState,
                0, PlayDeadState.values().length - 1));
        stateTicks = Math.max(0, playDeadManager == null
                ? compound.getInteger("WitherStormStateTicks") : playDeadManager.getInteger("StateTicks"));
        dataManager.set(PLAY_DEAD_STATE_TICKS, stateTicks);
        missingCommandBlockTicks = Math.max(0, playDeadManager == null
                ? compound.getInteger("WitherStormMissingCoreTicks")
                : playDeadManager.getInteger("CommandBlockMissingTicks"));
        if (playDeadManager == null) {
            recentlyRevivedTicks = Math.max(0, compound.getInteger("WitherStormRecentlyRevivedTicks"));
            podiumPosition = compound.hasKey("WitherStormPodiumPosition", 4)
                    ? BlockPos.fromLong(compound.getLong("WitherStormPodiumPosition")) : null;
            podiumPlaced = compound.getBoolean("WitherStormPodiumPlaced") && podiumPosition != null;
            podiumOffsetCorrected = !podiumPlaced
                    || compound.getBoolean("WitherStormPodiumOffsetCorrected");
        } else {
            recentlyRevivedTicks = playDeadManager.getBoolean("RecentlyRevived")
                    ? Math.max(1, playDeadManager.getInteger("RevivalTime")) : 0;
            podiumPosition = playDeadManager.hasKey("PodiumPos", 10)
                    ? NBTUtil.getPosFromTag(playDeadManager.getCompoundTag("PodiumPos")) : null;
            podiumPlaced = playDeadManager.getBoolean("PodiumPlaced") && podiumPosition != null;
            podiumOffsetCorrected = !podiumPlaced
                    || playDeadManager.getBoolean("PodiumOffsetCorrected");
        }

        int invulnerableTicks = compound.hasKey("Invul", 3)
                ? compound.getInteger("Invul") : compound.getInteger("WitherStormInvulnerableTicks");
        dataManager.set(INVULNERABLE_TICKS, Math.max(0, invulnerableTicks));
        int startingInvulnerableTicks = compound.hasKey("StartingInvul", 3)
                ? compound.getInteger("StartingInvul")
                : compound.hasKey("WitherStormStartingInvulnerableTicks", 3)
                ? compound.getInteger("WitherStormStartingInvulnerableTicks")
                : WitherStormConfig.invulnerabilityTime * 20;
        dataManager.set(STARTING_INVULNERABLE_TICKS,
                Math.max(0, startingInvulnerableTicks));
        dataManager.set(SHOULD_SHOW_HOLE, compound.hasKey("ShouldShowHole", 1)
                ? compound.getBoolean("ShouldShowHole") : compound.getBoolean("WitherStormShouldShowHole"));
        dataManager.set(OTHER_HEADS_DISABLED, compound.hasKey("OtherHeadsDisabled", 1)
                ? compound.getBoolean("OtherHeadsDisabled")
                : compound.getBoolean("WitherStormOtherHeadsDisabled"));
        setMirrored(compound.getBoolean("Mirrored"));
        dataManager.set(HEAD_ANIMATION_FLAGS, compound.getInteger("WitherStormHeadAnimationFlags"));
        bodyXRotation = MathHelper.clamp(compound.hasKey("XBodyRot", 5)
                ? compound.getFloat("XBodyRot") : compound.getFloat("WitherStormBodyXRotation"),
                0.0F, 90.0F);
        previousBodyXRotation = bodyXRotation;
        clientBodyXRotationTarget = bodyXRotation;
        renderYawOffset = compound.hasKey("YBodyRot", 5) ? compound.getFloat("YBodyRot")
                : compound.hasKey("WitherStormBodyYRotation", 5)
                ? compound.getFloat("WitherStormBodyYRotation") : rotationYaw;
        prevRenderYawOffset = renderYawOffset;
        clientBodyYRotationTarget = renderYawOffset;
        clientBodyXRotationSteps = 0;
        dataManager.set(BODY_X_ROTATION, bodyXRotation);
        dataManager.set(BODY_Y_ROTATION, renderYawOffset);
        consumptionLocked = compound.getBoolean("IsConsumptionLocked");
        formidibombUuid = readUuid(compound, "WitherStormFormidibomb");
        formidibomb = null;
        attractingFormidibomb = false;
        ConsumedPetStorage.read(compound, compound.hasKey("ConsumedPets", 9)
                ? "ConsumedPets" : "WitherStormConsumedPets", consumedPets);
        commandBlockUuid = readUuid(compound, "WitherStormCommandBlock");
        playingDeadCommandBlock = null;
        for (int i = 0; i < segmentUuids.length; i++) segmentUuids[i] = readUuid(compound, "WitherStormSegment" + i);
        trackedEntities.clear();
        savedTrackedEntities.clear();
        trackedEntityTicks = 0;
        NBTTagList tracked = compound.hasKey("TrackedEntities", 10)
                ? compound.getCompoundTag("TrackedEntities").getTagList("Entities", 10)
                : compound.getTagList("WitherStormTrackedEntities", 10);
        for (int index = 0; index < tracked.tagCount(); index++) {
            NBTTagCompound entry = tracked.getCompoundTagAt(index);
            UUID uuid = entry.hasUniqueId("UUID") ? entry.getUniqueId("UUID")
                    : entry.hasKey("M", 4) && entry.hasKey("L", 4) ? NBTUtil.getUUIDFromTag(entry) : null;
            if (uuid != null && !savedTrackedEntities.contains(uuid)) savedTrackedEntities.add(uuid);
        }
        witherStormDeathTime = Math.max(0, compound.getInteger("WitherStormDeathTime"));
        deathRewardsReleased = compound.getBoolean("WitherStormDeathRewardsReleased");
        deathLootReleased = compound.getBoolean("WitherStormDeathLootReleased");
        playingJukeboxes.clear();
        NBTTagList jukeboxes = compound.getTagList("PlayingJukeboxes", 10);
        for (int index = 0; index < jukeboxes.tagCount(); index++) {
            NBTTagCompound entry = jukeboxes.getCompoundTagAt(index);
            if (entry.hasKey("Pos", 10)) {
                playingJukeboxes.add(NBTUtil.getPosFromTag(entry.getCompoundTag("Pos")));
            } else if (entry.hasKey("Pos", 4)) {
                playingJukeboxes.add(BlockPos.fromLong(entry.getLong("Pos")));
            }
        }
        headManager.readFromNBT(compound);
        headManager.restorePlayDeadPose(getPlayDeadState());
        targetManager.read(compound);
        if (compound.hasKey("IgnoredTargets", 10)) {
            ignoredTargetsManager.readFromNBT(compound.getCompoundTag("IgnoredTargets"));
        } else if (compound.hasKey("IgnoredEntities", 10)) {
            ignoredTargetsManager.readFromNBT(compound.getCompoundTag("IgnoredEntities"));
        } else if (compound.hasKey("WitherStormIgnoredTargets", 10)) {
            ignoredTargetsManager.readFromNBT(compound.getCompoundTag("WitherStormIgnoredTargets"));
        }
        if (compound.hasKey("SymbiontSummoningCooldown", 3)) {
            summoningManager.setSummoningDelay(compound.getInteger("SymbiontSummoningCooldown"));
        } else if (compound.hasKey("WitherStormSymbiontSummoning", 10)) {
            summoningManager.readFromNBT(compound.getCompoundTag("WitherStormSymbiontSummoning"));
        }
        updateSizeForPlayDeadState();
        setNoGravity(!isPlayDeadAiDisabled());
        legacyBossInfo.setName(getDisplayName());
        legacyBossInfo.setVisible(!isPlayDeadAiDisabled());
        setHealth(Math.min(getHealth(), getMaxHealth()));
    }

    /** True only when this entity was reconstructed from a saved entity NBT record. */
    public boolean wasRestoredFromPersistentData() {
        return restoredFromPersistentData;
    }

    private static void writeUuid(NBTTagCompound compound, String key, UUID uuid) {
        if (uuid != null) compound.setUniqueId(key, uuid);
    }

    private static UUID readUuid(NBTTagCompound compound, String key) {
        return compound.hasUniqueId(key) ? compound.getUniqueId(key) : null;
    }

    @Override
    protected void despawnEntity() {
    }
}
