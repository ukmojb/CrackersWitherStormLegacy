package com.wdcftgg.witherstormmod.common.command;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 1.12 implementation of Cracker's Lib's server ConfigCommandBuilder tree. */
final class WitherStormConfigCommandSupport {
    private static final String USAGE = "commands.witherstormmod.usage";
    private static final Map<String, Field> SERVER_VALUES = new LinkedHashMap<String, Field>();

    static {
        register("server.misc", "shouldChunkLoadWhenNoPlayers", "invulnerabilityTime",
                "flyingHeight", "dynamicFlyingHeight", "dynamicFlyingHeightTime",
                "tillShouldShowHole", "shouldShowHole", "rotationSpeed",
                "witherStormInvulnerability", "smartBossbar", "randomBowelsEntrance",
                "summoningDimensionListMode",
                "crossbowsSupportEnderPearls", "preventWitherStormCamping",
                "bowelsFallResistance", "resummonedPhase", "canAttackHeads",
                "endOfPhaseFiveBombableExclusively",
                "shouldPlayGlobalSoundsCrossDimensionally", "onlyTryPickingUpTractorTagged",
                "constantBlackhole", "instantChomp", "healFromChomp");
        register("server.ultimate_target_logic", "ultimateTargetingType",
                "farthestTargetingTime", "randomizedTargetingTime",
                "randomlySpeedUpWithTargetChange", "amuletOverride",
                "maxRandomStrollTargetingTypeRadius", "ignoreUltimateTargetIfHidden",
                "witherStormsFollowBiggerStorms");
        register("server.ultimate_target_logic.chases", "shouldChaseWhenTargetStopped",
                "chaseOnPhaseChange");
        register("server.ultimate_target_logic.target_stationary_logic",
                "targetStationaryChunkRadius", "targetStationaryMinutes",
                "usePhaseAsDistanceMultiplier", "distanceMultiplier", "targetRunawayMinutes");
        register("server.ultimate_target_logic.runaway_attempts", "targetRunawayAttempts",
                "targetRunawayAttemptMinutes", "targetRunawayAttemptsRequired",
                "minutesTillRunawayAttemptDiminish");
        register("server.ultimate_target_logic.distractions",
                "targettingDistractionsEnabled", "distractionTimeMinutes",
                "maximumDistractionDistance", "minimumDistractionDistance",
                "randomDistractionChances", "searchableRangeMultiplier",
                "distractionWaitTime", "boatingForTooLongDistractions",
                "boatingForTooLongSeconds");
        register("server.ultimate_target_logic.random_strolling",
                "randomStrollingWhenTargetHidden");
        register("server.ultimate_target_logic.speed", "chasingFlyingSpeed",
                "normalFlyingSpeed");
        register("server.targeting", "headEscapeTime", "tractorPullSpeedModifier",
                "specialTargetingBias", "specialTargetingBiasChance");
        register("server.evolution", "evolutionAttributeModifier", "phase0Requirement",
                "phase1Requirement", "phase2Requirement", "phase3Requirement",
                "phase4Requirement", "phase5Requirement", "phase6Requirement",
                "phase7Requirement");
        register("server.performance", "clustersRemoveItems", "squashHitbox",
                "chunkLoadingRadius", "removeNearbyJunk", "mobsRunIntoPortals");
        register("server.world_consumption", "hunchbackClusterPickupInterval",
                "clusterPickupInterval", "devourerClusterPickupInterval",
                "canPickupMobClusters", "clusterSizeModifier", "tractorBeamClusterPickUp",
                "tractorBeamsRemoveFluids", "blockClusterPullSpeedModifier",
                "tractorBeamClusterSpeedModifier", "tractorBeamBlockSearchRadius",
                "canClustersSpiralCounterClockwise", "convertFallingBlocks",
                "tractorBeamFluidRemovalHeight");
        register("server.caves", "caveRumbles", "occludeSoundsUnderground",
                "caveRumbleIntensity", "chanceForExtendedRumbles",
                "caveRumbleIntervalMin", "caveRumbleIntervalMax",
                "caveRumblesMessWithRedstone");
        register("server.wither_sickness", "witherSicknessEnabled",
                "sickenedMobConversions", "increaseAmplifier", "requiredContacts",
                "requiredProximitySeconds", "applicationDelay", "cureDelay",
                "lowImmuneRequiredProximitySeconds", "lowImmuneApplicationDelay",
                "lowImmuneCureDelay", "proximitySecondsModifierMax",
                "applicationDelayModifierMax", "cureDelayModifierMax",
                "lowImmuneProximityModifierMax", "lowImmuneApplicationModifierMax",
                "lowImmuneCureDelayModifierMax", "keepSicknessAfterRespawn");
        register("server.formidibomb", "craftFuseTicks", "catchFireFuseTicks",
                "shouldDropFromInventory", "dropInterval", "lowerBlockResistance",
                "formidibombFuseEnabled");
        register("server.playing_dead", "revivalTimer", "revivalTimeMinutes",
                "revivalPlayerProtection");
        register("server.withered_symbiont", "canSummonSymbiont",
                "shouldSymbiontAttackMobs", "minimumSpawnCheckInterval",
                "witherStormSummoningDelay", "playerInvulnerableTime",
                "playerSummoningDelay", "playerSummoningDelayOnKill",
                "attackableWhenNotVulnerable", "bookDropsInInventory",
                "healthScalePerPlayer");
        register("server.flaming_skulls", "flamingSkullExplosionSize",
                "flamingSkullSpeedModifier");
        register("server.roaring", "minimumRoarInterval", "maximumRoarInterval");
        register("server.item_preservation", "itemPreservation",
                "preserveDropsForAllMobs");
        register("server", "flyingEnabledWarning");
    }

    private WitherStormConfigCommandSupport() {
    }

    static void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 4 || !"server".equals(args[1])) {
            throw new WrongUsageException(USAGE);
        }
        Field field = SERVER_VALUES.get(args[3]);
        if (field == null) throw new WrongUsageException(USAGE);
        if ("get".equals(args[2])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.config.get", args[3], get(field)));
            return;
        }
        if (!"set".equals(args[2]) || args.length != 5) {
            throw new WrongUsageException(USAGE);
        }
        if ("default".equals(args[4])) {
            reset(field);
        } else {
            set(field, args[4]);
        }
        sender.sendMessage(new TextComponentTranslation(
                "commands.witherstormmod.config.set", args[3], get(field)));
    }

    static List<String> complete(String[] args) {
        if (args.length == 2) return match(args, Collections.singletonList("server"));
        if (args.length == 3 && "server".equals(args[1])) {
            List<String> operations = new ArrayList<String>();
            operations.add("get");
            operations.add("set");
            return match(args, operations);
        }
        if (args.length == 4 && "server".equals(args[1])
                && ("get".equals(args[2]) || "set".equals(args[2]))) {
            return match(args, new ArrayList<String>(SERVER_VALUES.keySet()));
        }
        if (args.length == 5 && "server".equals(args[1]) && "set".equals(args[2])) {
            Field field = SERVER_VALUES.get(args[3]);
            List<String> values = new ArrayList<String>();
            values.add("default");
            if (field != null && field.getType() == boolean.class) {
                values.add("true");
                values.add("false");
            } else if (field != null && field.getType().isEnum()) {
                for (Object constant : field.getType().getEnumConstants()) {
                    values.add(((Enum<?>) constant).name().toLowerCase(Locale.ROOT));
                }
            }
            return match(args, values);
        }
        return Collections.emptyList();
    }

    private static void register(String category, String... names) {
        for (String name : names) {
            try {
                Field field = WitherStormConfig.class.getField(name);
                if (!Modifier.isStatic(field.getModifiers())) {
                    throw new IllegalStateException(name + " is not static");
                }
                SERVER_VALUES.put(category + "." + name, field);
            } catch (NoSuchFieldException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }

    private static Object get(Field field) throws CommandException {
        try {
            return field.get(null);
        } catch (IllegalAccessException exception) {
            throw configError(exception);
        }
    }

    private static void set(Field field, String raw) throws CommandException {
        Object value = parse(field, raw);
        validateRange(field, value);
        Property property = property(field);
        if (value instanceof Boolean) property.set((Boolean) value);
        else if (value instanceof Integer) property.set((Integer) value);
        else if (value instanceof Double) property.set((Double) value);
        else property.set(value instanceof Enum ? ((Enum<?>) value).name() : String.valueOf(value));
        saveAndSync(property);
    }

    private static void reset(Field field) throws CommandException {
        Property property = property(field);
        property.setToDefault();
        saveAndSync(property);
    }

    private static Object parse(Field field, String raw) throws CommandException {
        Class<?> type = field.getType();
        try {
            if (type == boolean.class) {
                if (!"true".equals(raw) && !"false".equals(raw)) {
                    throw new IllegalArgumentException(raw);
                }
                return Boolean.valueOf(raw);
            }
            if (type == int.class) return Integer.valueOf(raw);
            if (type == double.class) return Double.valueOf(raw);
            if (type == float.class) return Float.valueOf(raw);
            if (type.isEnum()) {
                for (Object constant : type.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(raw)) return constant;
                }
                throw new IllegalArgumentException(raw);
            }
            return raw;
        } catch (IllegalArgumentException exception) {
            throw new WrongUsageException("commands.witherstormmod.number.invalid", raw);
        }
    }

    private static void validateRange(Field field, Object value) throws CommandException {
        Config.RangeInt integerRange = field.getAnnotation(Config.RangeInt.class);
        if (integerRange != null && value instanceof Number) {
            long number = ((Number) value).longValue();
            if (number < integerRange.min() || number > integerRange.max()) {
                throw new WrongUsageException("commands.witherstormmod.number.invalid", value);
            }
        }
        Config.RangeDouble doubleRange = field.getAnnotation(Config.RangeDouble.class);
        if (doubleRange != null && value instanceof Number) {
            double number = ((Number) value).doubleValue();
            if (!Double.isFinite(number) || number < doubleRange.min()
                    || number > doubleRange.max()) {
                throw new WrongUsageException("commands.witherstormmod.number.invalid", value);
            }
        }
    }

    private static Property property(Field field) throws CommandException {
        Configuration configuration = configuration();
        for (String category : configuration.getCategoryNames()) {
            if (configuration.hasKey(category, field.getName())) {
                return configuration.getCategory(category).get(field.getName());
            }
        }
        throw new CommandException("commands.witherstormmod.config.missing", field.getName());
    }

    private static Configuration configuration() throws CommandException {
        try {
            Method method = ConfigManager.class.getDeclaredMethod(
                    "getConfiguration", String.class, String.class);
            method.setAccessible(true);
            Configuration configuration = (Configuration) method.invoke(
                    null, Tags.MOD_ID, Tags.MOD_ID + "/server");
            if (configuration == null) {
                throw new IllegalStateException("Forge configuration has not been loaded");
            }
            return configuration;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw configError(exception);
        }
    }

    private static void saveAndSync(Property property) throws CommandException {
        Configuration configuration = configuration();
        configuration.save();
        ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
    }

    private static CommandException configError(Exception exception) {
        return new CommandException("commands.witherstormmod.config.error",
                exception.getClass().getSimpleName());
    }

    private static List<String> match(String[] args, List<String> values) {
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<String>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) matches.add(value);
        }
        return matches;
    }
}
