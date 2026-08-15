package com.wdcftgg.witherstormmod.common.config;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.util.ItemPreservationCondition;
import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID, name = Tags.MOD_ID + "/server")
public final class WitherStormConfig {
    public enum UltimateTargetingType { NEAREST, FARTHEST, GROUP, NONE, RANDOM_STROLL, RANDOM_PLAYER, RANDOMIZED, RANDOM_STROLL_NEAR_PLAYER }
    public enum ListMode { BLACKLIST, WHITELIST }

    @Config.Name("autoSpawnWitherStorm")
    @Config.Comment("Whether a Wither Storm automatically spawns from the origin platform after world creation.")
    public static boolean autoSpawnWitherStorm = false;

    @Config.Name("autoSpawnTime")
    @Config.Comment("Minutes before the automatically generated Wither Storm spawns.")
    @Config.RangeInt(min = 0, max = 120)
    public static int autoSpawnTime = 1;

    @Config.Name("summoningDimensionListMode")
    @Config.Comment("Whether summoningDimensions is a blacklist or whitelist. An empty blacklist allows all dimensions; an empty whitelist allows none.")
    public static ListMode summoningDimensionListMode = ListMode.BLACKLIST;

    @Config.Name("summoningDimensions")
    @Config.Comment("Numeric dimension IDs in which Wither Storm summoning is allowed or denied. Supports * and namespace:* entries.")
    public static String[] summoningDimensions = {};

    @Config.Name("ultimateTargetingType")
    @Config.Comment("Strategy used to choose the Wither Storm ultimate player target.")
    public static UltimateTargetingType ultimateTargetingType = UltimateTargetingType.NEAREST;

    @Config.Name("farthestTargetingTime")
    @Config.Comment("Minutes to keep the selected farthest player before switching.")
    @Config.RangeInt(min = 1, max = 60)
    public static int farthestTargetingTime = 15;

    @Config.Name("randomlySpeedUpWithTargetChange")
    @Config.Comment("Whether RANDOMIZED targeting has a chance to accelerate the storm when its strategy changes.")
    public static boolean randomlySpeedUpWithTargetChange = true;

    @Config.Name("randomizedTargetingTime")
    @Config.Comment("Minutes between randomized targeting strategy changes.")
    @Config.RangeInt(min = 1, max = 60)
    public static int randomizedTargetingTime = 15;

    @Config.Name("shouldChaseWhenTargetStopped")
    @Config.Comment("Whether a stationary ultimate target makes phases 4-7 use the faster chase speed.")
    public static boolean shouldChaseWhenTargetStopped = true;

    @Config.Name("targetStationaryChunkRadius")
    @Config.Comment("Chunk radius in which the ultimate target must remain before stationary logic advances.")
    @Config.RangeInt(min = 0, max = 16)
    public static int targetStationaryChunkRadius = 8;

    @Config.Name("targetStationaryMinutes")
    @Config.Comment("Minutes the ultimate target must remain stationary before the storm accelerates.")
    @Config.RangeInt(min = 1, max = 120)
    public static int targetStationaryMinutes = 30;

    @Config.Name("usePhaseAsDistanceMultiplier")
    @Config.Comment("Whether the storm phase increases the stationary-distance timer.")
    public static boolean usePhaseAsDistanceMultiplier = true;

    @Config.Name("distanceMultiplier")
    @Config.Comment("Multiplier applied to the distance used by stationary-target timing.")
    @Config.RangeDouble(min = 0.1D, max = 24.0D)
    public static double distanceMultiplier = 1.0D;

    @Config.Name("targetRunawayMinutes")
    @Config.Comment("Minutes of movement outside the stationary area before the storm slows down.")
    @Config.RangeInt(min = 1, max = 90)
    public static int targetRunawayMinutes = 10;

    @Config.Name("targetRunawayAttempts")
    @Config.Comment("Whether leaving the stationary area counts runaway attempts.")
    public static boolean targetRunawayAttempts = true;

    @Config.Name("targetRunawayAttemptMinutes")
    @Config.Comment("Minutes a target must remain stationary before a runaway attempt can be counted.")
    @Config.RangeInt(min = 1, max = 20)
    public static int targetRunawayAttemptMinutes = 2;

    @Config.Name("targetRunawayAttemptsRequired")
    @Config.Comment("Runaway attempts required to force the faster chase speed.")
    @Config.RangeInt(min = 1, max = 32)
    public static int targetRunawayAttemptsRequired = 5;

    @Config.Name("minutesTillRunawayAttemptDiminish")
    @Config.Comment("Minutes inside the stationary area before one runaway attempt is forgotten.")
    @Config.RangeInt(min = 1, max = 48)
    public static int minutesTillRunawayAttemptDiminish = 16;

    @Config.Name("targettingDistractionsEnabled")
    @Config.Comment("Whether the storm can leave a prolonged chase for a distant distraction.")
    public static boolean targettingDistractionsEnabled = true;

    @Config.Name("distractionTimeMinutes")
    @Config.Comment("Base duration in minutes for an ultimate-target distraction.")
    @Config.RangeInt(min = 1, max = 25)
    public static int distractionTimeMinutes = 25;

    @Config.Name("maximumDistractionDistance")
    @Config.Comment("Additional target range within which a distraction may begin.")
    @Config.RangeInt(min = 100, max = 3000)
    public static int maximumDistractionDistance = 1000;

    @Config.Name("minimumDistractionDistance")
    @Config.Comment("Minimum distance beyond the follow range before immediate distraction.")
    @Config.RangeInt(min = 10, max = 500)
    public static int minimumDistractionDistance = 50;

    @Config.Name("randomDistractionChances")
    @Config.Comment("Whether the upstream random distraction chances are applied.")
    public static boolean randomDistractionChances = true;

    @Config.Name("searchableRangeMultiplier")
    @Config.Comment("Multiplier used while searching for a distractable structure or location.")
    @Config.RangeInt(min = 1, max = 8)
    public static int searchableRangeMultiplier = 1;

    @Config.Name("distractionWaitTime")
    @Config.Comment("Base wait in minutes before a delayed distraction may start.")
    @Config.RangeInt(min = 1, max = 20)
    public static int distractionWaitTime = 2;

    @Config.Name("boatingForTooLongDistractions")
    @Config.Comment("Whether sustained high-speed target movement can trigger a distraction.")
    public static boolean boatingForTooLongDistractions = true;

    @Config.Name("boatingForTooLongSeconds")
    @Config.Comment("Seconds of sustained high-speed target movement before a distraction.")
    @Config.RangeInt(min = 30, max = 300)
    public static int boatingForTooLongSeconds = 60;

    @Config.Name("maxRandomStrollTargetingTypeRadius")
    @Config.Comment("Maximum radius of a random stroll target.")
    @Config.RangeInt(min = 200, max = 5000)
    public static int maxRandomStrollTargetingTypeRadius = 500;

    @Config.Name("tillShouldShowHole")
    @Config.Comment("Base minutes before a command-block tool causes the phase 7 hole to appear.")
    @Config.RangeInt(min = 1, max = 30)
    public static int tillShouldShowHole = 6;

    @Config.Name("tractorBeamBlockSearchRadius")
    @Config.Comment("Radius searched around a tractor-beam hit for tagged distraction blocks.")
    @Config.RangeInt(min = 4, max = 256)
    public static int tractorBeamBlockSearchRadius = 10;

    @Config.Name("hunchbackClusterPickupInterval")
    @Config.Comment("Alters the interval (in ticks) of picking up block clusters for the hunchback phases (phase 0 - 3).")
    @Config.RangeInt(min = 10, max = 80)
    public static int hunchbackClusterPickupInterval = 20;

    @Config.Name("witherStormsFollowBiggerStorms")
    @Config.Comment("Whether smaller storms follow a nearby storm with greater consumed mass.")
    public static boolean witherStormsFollowBiggerStorms = true;

    @Config.Name("phantomsOrbitWitherStorm")
    @Config.Comment("Whether sickened phantoms circle above any nearby Wither Storms instead of wandering.")
    public static boolean phantomsOrbitWitherStorm = true;

    @Config.Name("bookDropsInInventory")
    @Config.Comment("Whether the command block book drops directly into the inventory of nearby contributors.")
    public static boolean bookDropsInInventory = true;

    @Config.Name("flyingEnabledWarning")
    @Config.Comment("Whether a warning is printed to server operators when flying is disabled.")
    public static boolean flyingEnabledWarning = true;

    @Config.Name("ignoreUltimateTargetIfHidden")
    @Config.Comment("Temporarily ignore a target that remains hidden from the storm in multiplayer.")
    public static boolean ignoreUltimateTargetIfHidden = true;

    @Config.Name("randomStrollingWhenTargetHidden")
    @Config.Comment("Use a temporary random stroll when the target cannot be seen.")
    public static boolean randomStrollingWhenTargetHidden = true;
    @Config.Name("itemPreservation")
    @Config.Comment("Condition under which player drops are preserved in a withered phlegm cluster.")
    public static ItemPreservationCondition itemPreservation =
            ItemPreservationCondition.CHOMPED_OR_KILLED_NEAR_HEAD;

    @Config.Name("preserveDropsForAllMobs")
    @Config.Comment("Preserve drops in phlegm clusters for all living mobs instead of players only.")
    public static boolean preserveDropsForAllMobs = false;

    @Config.Name("resummonedPhase")
    @Config.Comment("Phase assigned to a Wither Storm resummoned by a super beacon.")
    @Config.RangeInt(min = 0, max = 7)
    public static int resummonedPhase = 4;

    @Config.Name("shouldShowHole")
    @Config.Comment("Whether the bowels entrance hole is available at the end of phase 7.")
    public static boolean shouldShowHole = true;

    @Config.Name("randomBowelsEntrance")
    @Config.Comment("进入肠道维度时是否在结构外围寻找随机安全入口；关闭后使用固定结构入口。")
    public static boolean randomBowelsEntrance = true;

    @Config.Name("crossbowsSupportEnderPearls")
    @Config.Comment("Crossbow 弩是否可以从主手或副手装填并发射末影珍珠。")
    public static boolean crossbowsSupportEnderPearls = true;

    @Config.Name("bowelsFallResistance")
    @Config.Comment("离开肠道并从高处落下时是否短暂获得最高等级抗性。")
    public static boolean bowelsFallResistance = true;

    @Config.Name("amuletOverride")
    @Config.Comment("Whether carrying an amulet forces the Wither Storm to prioritize that player.")
    public static boolean amuletOverride = true;

    @Config.Name("witherStormInvulnerability")
    @Config.Comment("Whether the Wither Storm regenerates and ignores ordinary damage after phase 3.")
    public static boolean witherStormInvulnerability = true;

    @Config.Name("smartBossbar")
    @Config.Comment("玩家位于地下狭小空间且看不到凋零风暴时隐藏 Boss 条和 Boss 主题。")
    public static boolean smartBossbar = true;

    @Config.Name("preventWitherStormCamping")
    @Config.Comment("玩家被大型凋零风暴杀死且重生点仍在其附近时，临时将玩家送到风暴范围外。")
    public static boolean preventWitherStormCamping = true;

    @Config.Name("shouldPlayGlobalSoundsCrossDimensionally")
    @Config.Comment("凋零风暴的进化、分裂和复活等全局音效是否跨维度播放。")
    public static boolean shouldPlayGlobalSoundsCrossDimensionally = false;

    @Config.Name("occludeSoundsUnderground")
    @Config.Comment("玩家位于地下狭小空间时，是否按深度进一步削弱凋零风暴音效。")
    public static boolean occludeSoundsUnderground = true;

    @Config.Name("invulnerabilityTime")
    @Config.Comment("Initial Wither Storm invulnerability duration in seconds.")
    @Config.RangeInt(min = 1, max = 320)
    public static int invulnerabilityTime = 50;

    @Config.Name("flyingHeight")
    @Config.Comment("Target height above the highest nearby terrain during destroyer phases.")
    @Config.RangeInt(min = 10, max = 150)
    public static int flyingHeight = 75;

    @Config.Name("dynamicFlyingHeight")
    @Config.Comment("Whether destroyer phases periodically choose a new flying height.")
    public static boolean dynamicFlyingHeight = false;

    @Config.Name("dynamicFlyingHeightTime")
    @Config.Comment("Seconds between dynamic flying-height changes.")
    @Config.RangeInt(min = 15, max = 1200)
    public static int dynamicFlyingHeightTime = 60;

    @Config.Name("normalFlyingSpeed")
    @Config.Comment("Horizontal speed while chasing a moving target.")
    @Config.RangeDouble(min = 0.01D, max = 1.0D)
    public static double normalFlyingSpeed = 0.02D;

    @Config.Name("chasingFlyingSpeed")
    @Config.Comment("Horizontal speed while chasing a stationary target.")
    @Config.RangeDouble(min = 0.01D, max = 1.0D)
    public static double chasingFlyingSpeed = 0.4D;

    @Config.Name("chaseOnPhaseChange")
    @Config.Comment("Whether evolving beyond the small phases immediately starts a target chase.")
    public static boolean chaseOnPhaseChange = true;

    @Config.Name("rotationSpeed")
    @Config.Comment("大型凋零风暴朝目标转动身体时每刻允许转过的最大角度。")
    @Config.RangeDouble(min = 0.1D, max = 1.0D)
    public static double rotationSpeed = 0.1D;

    @Config.Name("tractorPullSpeedModifier")
    @Config.Comment("Base speed of entities pulled by tractor beams.")
    @Config.RangeDouble(min = 0.1D, max = 1.0D)
    public static double tractorPullSpeedModifier = 0.2D;

    @Config.Name("blockClusterPullSpeedModifier")
    @Config.Comment("Speed multiplier for ordinary block clusters, items, and slimes consumed by the storm.")
    @Config.RangeDouble(min = 0.1D, max = 10.0D)
    public static double blockClusterPullSpeedModifier = 1.0D;

    @Config.Name("tractorBeamClusterSpeedModifier")
    @Config.Comment("Speed multiplier for block clusters created by tractor beams.")
    @Config.RangeDouble(min = 0.1D, max = 10.0D)
    public static double tractorBeamClusterSpeedModifier = 1.0D;

    @Config.Name("flamingSkullSpeedModifier")
    @Config.Comment("Speed multiplier for flaming Wither Storm skulls.")
    @Config.RangeDouble(min = 0.5D, max = 8.0D)
    public static double flamingSkullSpeedModifier = 1.0D;

    @Config.Name("flamingSkullExplosionSize")
    @Config.Comment("火焰凋零头颅撞击方块时的基础爆炸半径；蓝色头颅会额外增加 4。")
    @Config.RangeDouble(min = 1.0D, max = 16.0D)
    public static double flamingSkullExplosionSize = 5.0D;

    @Config.Name("canClustersSpiralCounterClockwise")
    @Config.Comment("Whether marked block clusters may spiral counterclockwise while being consumed.")
    public static boolean canClustersSpiralCounterClockwise = false;

    @Config.Name("tractorBeamClusterPickUp")
    @Config.Comment("Whether tractor beams create and consume block clusters.")
    public static boolean tractorBeamClusterPickUp = true;

    @Config.Name("onlyTryPickingUpTractorTagged")
    @Config.Comment("Only create tractor-beam clusters from externally tagged distraction blocks.")
    public static boolean onlyTryPickingUpTractorTagged = false;

    @Config.Name("tractorBeamsRemoveFluids")
    @Config.Comment("Whether destroyer tractor beams remove fluids along their ray.")
    public static boolean tractorBeamsRemoveFluids = true;

    @Config.Name("tractorBeamFluidRemovalHeight")
    @Config.Comment("Minimum Y level at which tractor beams remove fluids.")
    @Config.RangeInt(min = -64, max = 320)
    public static int tractorBeamFluidRemovalHeight = 63;

    @Config.Name("canPickupMobClusters")
    @Config.Comment("Whether tractor beams can pull living entities into the storm.")
    public static boolean canPickupMobClusters = true;

    @Config.Name("instantChomp")
    @Config.Comment("Whether a Wither Storm head immediately kills a player it bites outside the bowels.")
    public static boolean instantChomp = false;

    @Config.Name("healFromChomp")
    @Config.Comment("Whether a Wither Storm heals for half of a non-player mob's maximum health when it bites it.")
    public static boolean healFromChomp = false;

    @Config.Name("shouldPickUpVehicles")
    @Config.Comment("Whether pulling a mounted entity also pulls its vehicle.")
    public static boolean shouldPickUpVehicles = true;

    @Config.Name("canAttackHeads")
    @Config.Comment("Whether projectiles and attacks can injure individual storm heads.")
    public static boolean canAttackHeads = true;

    @Config.Name("endOfPhaseFiveBombableExclusively")
    @Config.Comment("Require full phase progress before a Formidibomb can knock down the Wither Storm.")
    public static boolean endOfPhaseFiveBombableExclusively = false;

    @Config.Name("revivalTimer")
    @Config.Comment("Whether a fallen Wither Storm automatically reactivates after the configured delay.")
    public static boolean revivalTimer = true;

    @Config.Name("revivalTimeMinutes")
    @Config.Comment("Minutes before an unattended command block automatically reactivates its fallen Wither Storm; zero disables the timer.")
    @Config.RangeInt(min = 1, max = 120)
    public static int revivalTimeMinutes = 60;

    @Config.Name("revivalPlayerProtection")
    @Config.Comment("Minutes after revival during which the Wither Storm will not target players.")
    @Config.RangeInt(min = 1, max = 40)
    public static int revivalPlayerProtection = 3;

    @Config.Name("canSummonSymbiont")
    @Config.Comment("凋零风暴是否能够召唤凋零共生体。")
    public static boolean canSummonSymbiont = true;

    @Config.Name("minimumSpawnCheckInterval")
    @Config.Comment("凋零风暴检查共生体召唤条件的最小间隔，单位为秒。")
    @Config.RangeInt(min = 1, max = 240)
    public static int minimumSpawnCheckInterval = 60;

    @Config.Name("witherStormSummoningDelay")
    @Config.Comment("凋零风暴成功召唤共生体后的基础冷却，单位为分钟。")
    @Config.RangeInt(min = 1, max = 20)
    public static int witherStormSummoningDelay = 10;

    @Config.Name("playerInvulnerableTime")
    @Config.Comment("共生体死亡后附近玩家暂时不被凋零风暴选为目标的基础时长，单位为分钟。")
    @Config.RangeInt(min = 1, max = 10)
    public static int playerInvulnerableTime = 5;

    @Config.Name("playerSummoningDelay")
    @Config.Comment("同一凋零风暴再次为同一玩家召唤共生体的基础冷却，单位为分钟。")
    @Config.RangeInt(min = 1, max = 60)
    public static int playerSummoningDelay = 10;

    @Config.Name("playerSummoningDelayOnKill")
    @Config.Comment("玩家击杀共生体后，该凋零风暴再次为其召唤共生体的基础冷却，单位为分钟。")
    @Config.RangeInt(min = 1, max = 60)
    public static int playerSummoningDelayOnKill = 40;

    @Config.Name("shouldSymbiontAttackMobs")
    @Config.Comment("新生成的凋零共生体是否会主动攻击非凋零阵营生物。")
    public static boolean shouldSymbiontAttackMobs = false;

    @Config.Name("attackableWhenNotVulnerable")
    @Config.Comment("凋零共生体未进入虚弱状态时是否仍能从正面受伤。")
    public static boolean attackableWhenNotVulnerable = false;

    @Config.Name("healthScalePerPlayer")
    @Config.Comment("附近玩家超过一人时，每名玩家为新生成共生体增加的最大生命值。")
    @Config.RangeDouble(min = 0.0D, max = 100.0D)
    public static double healthScalePerPlayer = 20.0D;

    @Config.Name("craftFuseTicks")
    @Config.Comment("Fuse duration assigned to a newly crafted Formidibomb.")
    @Config.RangeInt(min = 1, max = 12000)
    public static int craftFuseTicks = 12000;

    @Config.Name("catchFireFuseTicks")
    @Config.Comment("Maximum fuse assigned when a placed Formidibomb is ignited.")
    @Config.RangeInt(min = 1, max = 12000)
    public static int catchFireFuseTicks = 1200;

    @Config.Name("dropInterval")
    @Config.Comment("Divisor used to decide when a ticking Formidibomb leaves its holder as a primed entity.")
    @Config.RangeInt(min = 1, max = 8)
    public static int dropInterval = 4;

    @Config.Name("shouldDropFromInventory")
    @Config.Comment("Whether a ticking Formidibomb becomes a primed entity before its fuse expires in an inventory.")
    public static boolean shouldDropFromInventory = true;

    @Config.Name("lowerBlockResistance")
    @Config.Comment("Whether Formidibomb explosions use the upstream reduced block-resistance multiplier.")
    public static boolean lowerBlockResistance = true;

    @Config.Name("formidibombFuseEnabled")
    @Config.Comment("Whether Formidibomb item and block fuses count down automatically.")
    public static boolean formidibombFuseEnabled = true;

    @Config.Name("headEscapeTime")
    @Config.Comment("Reserved head-escape protection duration in seconds.")
    @Config.RangeInt(min = 0, max = 60)
    public static int headEscapeTime = 40;

    @Config.Name("specialTargetingBias")
    @Config.Comment("头部选择目标时是否优先考虑外部 favourable_mobs 标签中的实体。")
    public static boolean specialTargetingBias = true;

    @Config.Name("specialTargetingBiasChance")
    @Config.Comment("头部每次选取目标时启用特殊目标偏置的百分比概率。")
    @Config.RangeInt(min = 0, max = 100)
    public static int specialTargetingBiasChance = 75;

    @Config.Name("evolutionAttributeModifier")
    @Config.Comment("凋零风暴各阶段消耗量阈值的倍率；数值越高，完整进化越慢。")
    @Config.RangeDouble(min = 0.01D, max = 32.0D)
    public static double evolutionAttributeModifier = 1.0D;

    @Config.Name("phase0Requirement")
    @Config.Comment("Consumed mass required for phase 0.")
    @Config.RangeInt(min = 1)
    public static int phase0Requirement = 100;

    @Config.Name("phase1Requirement")
    @Config.Comment("Consumed mass required for phase 1.")
    @Config.RangeInt(min = 1)
    public static int phase1Requirement = 400;

    @Config.Name("phase2Requirement")
    @Config.Comment("Consumed mass required for phase 2.")
    @Config.RangeInt(min = 1)
    public static int phase2Requirement = 1200;

    @Config.Name("phase3Requirement")
    @Config.Comment("Consumed mass required for phase 3.")
    @Config.RangeInt(min = 1)
    public static int phase3Requirement = 18800;

    @Config.Name("phase4Requirement")
    @Config.Comment("Consumed mass required for phase 4.")
    @Config.RangeInt(min = 1)
    public static int phase4Requirement = 195000;

    @Config.Name("phase5Requirement")
    @Config.Comment("Consumed mass required for phase 5.")
    @Config.RangeInt(min = 1)
    public static int phase5Requirement = 351400;

    @Config.Name("phase6Requirement")
    @Config.Comment("Consumed mass required for phase 6.")
    @Config.RangeInt(min = 1)
    public static int phase6Requirement = 580800;

    @Config.Name("phase7Requirement")
    @Config.Comment("Consumed mass required for phase 7.")
    @Config.RangeInt(min = 1)
    public static int phase7Requirement = 2125000;

    @Config.Name("consumableBlockWhitelist")
    @Config.Comment("Additional block registry names the Wither Storm may consume, overriding the upstream protection list.")
    public static String[] consumableBlockWhitelist = {};

    @Config.Name("consumableBlockBlacklist")
    @Config.Comment("Block registry names the Wither Storm must never consume. This overrides the whitelist and upstream rules.")
    public static String[] consumableBlockBlacklist = {};

    @Config.Name("witherSicknessEnabled")
    @Config.Comment("是否启用凋零病感染状态机。")
    @Config.RequiresMcRestart
    public static boolean witherSicknessEnabled = true;

    @Config.Name("sickenedMobConversions")
    @Config.Comment("可转化生物因凋零病死亡时是否变为对应的病化生物。")
    public static boolean sickenedMobConversions = true;

    @Config.Name("increaseAmplifier")
    @Config.Comment("实体在较短时间内反复感染时是否提高凋零病等级。")
    public static boolean increaseAmplifier = true;

    @Config.Name("requiredContacts")
    @Config.Comment("通过接触开始感染所需的凋零风暴锁定次数。")
    @Config.RangeInt(min = 1, max = 40)
    public static int requiredContacts = 6;

    @Config.Name("requiredProximitySeconds")
    @Config.Comment("高免疫实体在凋零风暴附近开始感染所需的秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int requiredProximitySeconds = 600;

    @Config.Name("applicationDelay")
    @Config.Comment("高免疫实体开始感染后获得凋零病效果的延迟秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int applicationDelay = 720;

    @Config.Name("cureDelay")
    @Config.Comment("高免疫实体完成凋零病治疗所需的秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int cureDelay = 480;

    @Config.Name("lowImmuneRequiredProximitySeconds")
    @Config.Comment("低免疫实体在凋零风暴附近开始感染所需的秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int lowImmuneRequiredProximitySeconds = 360;

    @Config.Name("lowImmuneApplicationDelay")
    @Config.Comment("低免疫实体开始感染后获得凋零病效果的延迟秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int lowImmuneApplicationDelay = 410;

    @Config.Name("lowImmuneCureDelay")
    @Config.Comment("低免疫实体完成凋零病治疗所需的秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int lowImmuneCureDelay = 480;

    @Config.Name("proximitySecondsModifierMax")
    @Config.Comment("高免疫实体接近感染时间的随机正向修正上限秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int proximitySecondsModifierMax = 180;

    @Config.Name("applicationDelayModifierMax")
    @Config.Comment("高免疫实体效果施加延迟的随机正向修正上限秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int applicationDelayModifierMax = 300;

    @Config.Name("cureDelayModifierMax")
    @Config.Comment("高免疫实体治疗时间的随机正向修正上限秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int cureDelayModifierMax = 180;

    @Config.Name("lowImmuneProximityModifierMax")
    @Config.Comment("低免疫实体接近感染时间的随机负向修正上限秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int lowImmuneProximityModifierMax = 180;

    @Config.Name("lowImmuneApplicationModifierMax")
    @Config.Comment("低免疫实体效果施加延迟的随机负向修正上限秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int lowImmuneApplicationModifierMax = 140;

    @Config.Name("lowImmuneCureDelayModifierMax")
    @Config.Comment("低免疫实体治疗时间的随机负向修正上限秒数。")
    @Config.RangeInt(min = 12, max = 1200)
    public static int lowImmuneCureDelayModifierMax = 180;

    @Config.Name("keepSicknessAfterRespawn")
    @Config.Comment("玩家死亡重生后是否保留凋零病状态和效果。")
    public static boolean keepSicknessAfterRespawn = true;

    @Config.Name("minimumRoarInterval")
    @Config.Comment("Minimum interval between storm head roars in seconds.")
    @Config.RangeInt(min = 1, max = 100)
    public static int minimumRoarInterval = 20;

    @Config.Name("maximumRoarInterval")
    @Config.Comment("Maximum interval between storm head roars in seconds.")
    @Config.RangeInt(min = 1, max = 100)
    public static int maximumRoarInterval = 50;

    @Config.Name("convertFallingBlocks")
    @Config.Comment("Whether nearby falling blocks become persistent storm clusters.")
    public static boolean convertFallingBlocks = false;

    @Config.Name("caveRumbles")
    @Config.Comment("Whether underground players near a large Wither Storm experience cave rumbles.")
    public static boolean caveRumbles = true;

    @Config.Name("caveRumbleIntensity")
    @Config.Comment("Strength of cave-rumble shaking, cave-ins, falling blocks, and environmental disturbances.")
    @Config.RangeDouble(min = 0.0D, max = 1.0D)
    public static double caveRumbleIntensity = 0.25D;

    @Config.Name("chanceForExtendedRumbles")
    @Config.Comment("Whether cave rumbles can continue with several short follow-up rumbles.")
    public static boolean chanceForExtendedRumbles = true;

    @Config.Name("caveRumbleIntervalMin")
    @Config.Comment("Minimum normal interval between cave rumbles, in seconds.")
    @Config.RangeInt(min = 5, max = 1800)
    public static int caveRumbleIntervalMin = 60;

    @Config.Name("caveRumbleIntervalMax")
    @Config.Comment("Maximum normal interval between cave rumbles, in seconds.")
    @Config.RangeInt(min = 5, max = 1800)
    public static int caveRumbleIntervalMax = 180;

    @Config.Name("caveRumblesMessWithRedstone")
    @Config.Comment("Whether cave rumbles briefly activate nearby redstone controls and light sources.")
    public static boolean caveRumblesMessWithRedstone = true;

    @Config.Name("removeNearbyJunk")
    @Config.Comment("Immediately remove externally tagged junk near large Wither Storm phases when no player is nearby.")
    public static boolean removeNearbyJunk = true;

    @Config.Name("squashHitbox")
    @Config.Comment("Shrink large Wither Storm and segment hitboxes vertically to one block.")
    @Config.RequiresMcRestart
    public static boolean squashHitbox = false;

    @Config.Name("chunkLoadingRadius")
    @Config.Comment("Chunk-loading radius used by the main Wither Storm.")
    @Config.RangeInt(min = 6, max = 32)
    @Config.RequiresMcRestart
    public static int chunkLoadingRadius = 12;

    @Config.Name("shouldChunkLoadWhenNoPlayers")
    @Config.Comment("服务器无人在线时是否继续强加载凋零风暴、分裂体和肠道区域。")
    public static boolean shouldChunkLoadWhenNoPlayers = false;

    @Config.Name("playerCannotDismountTentacles")
    @Config.Comment("玩家被触手抓住时是否禁止主动下马。")
    public static boolean playerCannotDismountTentacles = true;

    @Config.Name("injectCustomAiBehavior")
    @Config.Comment("是否为原版生物注入逃离凋零风暴、在牵引中反击以及攻击病化生物的 AI。")
    public static boolean injectCustomAiBehavior = true;

    @Config.Name("injectAiMobBlacklist")
    @Config.Comment("不注入凋零风暴自定义 AI 的生物注册名列表。")
    public static String[] injectAiMobBlacklist = {"witherstormmod:example"};

    @Config.Name("mobsRunIntoPortals")
    @Config.Comment("生物逃离凋零风暴时是否优先进入 16 格内的下界传送门。")
    public static boolean mobsRunIntoPortals = true;

    @Config.Name("clusterPickupInterval")
    @Config.Comment("Block-cluster pickup interval for destroyer phases 4 and 5.")
    @Config.RangeInt(min = 10, max = 80)
    public static int clusterPickupInterval = 40;

    @Config.Name("devourerClusterPickupInterval")
    @Config.Comment("Block-cluster pickup interval for devourer phases 6 and 7.")
    @Config.RangeInt(min = 10, max = 80)
    public static int devourerClusterPickupInterval = 40;

    @Config.Name("clusterSizeModifier")
    @Config.Comment("Amount added to the radius of default block clusters.")
    @Config.RangeInt(min = 0, max = 16)
    public static int clusterSizeModifier = 0;

    @Config.Name("blockClustersDropItems")
    @Config.Comment("方块质量簇无法重新放置方块时是否生成对应掉落物；开启后可能增加实体负载。")
    public static boolean blockClustersDropItems = false;

    @Config.Name("clustersRemoveItems")
    @Config.Comment("非牵引光束生成的大型方块质量簇是否清除碰撞箱内没有所有者的掉落物。")
    public static boolean clustersRemoveItems = true;

    @Config.Name("constantBlackhole")
    @Config.Comment("Remove small-cluster cooldowns. This can be extremely expensive.")
    public static boolean constantBlackhole = false;

    public static boolean isSummoningDimensionAllowed(int dimensionId) {
        return com.wdcftgg.witherstormmod.common.config.ConfiguredListMatcher.allows(
                String.valueOf(dimensionId), summoningDimensions,
                summoningDimensionListMode == ListMode.WHITELIST);
    }

    public static int getConfiguredPhaseRequirement(int phase) {
        int configured;
        switch (phase) {
            case 0: configured = phase0Requirement; break;
            case 1: configured = phase1Requirement; break;
            case 2: configured = phase2Requirement; break;
            case 3: configured = phase3Requirement; break;
            case 4: configured = phase4Requirement; break;
            case 5: configured = phase5Requirement; break;
            case 6: configured = phase6Requirement; break;
            case 7: configured = phase7Requirement; break;
            default: return 0;
        }
        int previous = phase == 0 ? 0 : getConfiguredPhaseRequirement(phase - 1);
        return Math.max(previous, Math.max(1, configured));
    }

    private WitherStormConfig() {
    }
}
