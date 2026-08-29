package com.wdcftgg.witherstormmod.common.command;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.api.common.ai.symbiont.SpellType;
import com.wdcftgg.witherstormmod.api.common.registry.WitherStormModRegistries;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessCapability;
import com.wdcftgg.witherstormmod.common.capability.WitherSicknessTracker;
import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.entity.CaveRumbleManager;
import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import com.wdcftgg.witherstormmod.common.entity.SymbiontSpells;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.UltimateTargetManager;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.init.ModDamageSources;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.resource.UpstreamEntityTags;
import com.wdcftgg.witherstormmod.common.taint.TaintingManager;
import com.wdcftgg.witherstormmod.common.util.EvolutionProfiler;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import com.wdcftgg.witherstormmod.common.world.BowelsDimensions;
import com.wdcftgg.witherstormmod.common.world.BowelsInstanceData;
import com.wdcftgg.witherstormmod.common.world.BowelsManager;
import com.wdcftgg.witherstormmod.common.world.ChunkLoadingManager;
import com.wdcftgg.witherstormmod.common.world.StructureTemplates;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.EntityNotFoundException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;





public final class WitherStormAdminCommand extends CommandBase {

    private static final String USAGE = "commands.witherstormmod.usage";

    @Override
    public String getName() {
        return "witherstormmod";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.witherstormmod.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(USAGE);
        }
        String group = args[0];
        if ("phase".equals(group)) {
            executePhase(server, sender, args);
        } else if ("explode".equals(group)) {
            executeExplode(server, sender, args);
        } else if ("kill".equals(group)) {
            executeKill(server, sender, args);
        } else if ("revive".equals(group)) {
            executeRevive(server, sender, args);
        } else if ("evolutionSpeed".equals(group)) {
            executeEvolutionSpeed(server, sender, args);
        } else if ("consumedEntities".equals(group)) {
            executeConsumed(server, sender, args);
        } else if ("ultimateTarget".equals(group)) {
            executeTarget(server, sender, args);
        } else if ("sickness".equals(group)) {
            executeSickness(server, sender, args);
        } else if ("tractorBeam".equals(group)) {
            executeTractorBeam(server, sender, args);
        } else if ("cluster".equals(group)) {
            executeCluster(server, sender, args);
        } else if ("screenShake".equals(group)) {
            executeShake(server, sender, args);
        } else if ("convert".equals(group)) {
            executeConversion(server, sender, args);
        } else if ("bowels".equals(group)) {
            executeBowels(server, sender, args);
        } else if ("chunkLoader".equals(group)) {
            executeChunkLoader(server, sender, args);
        } else if ("config".equals(group)) {
            WitherStormConfigCommandSupport.execute(sender, args);
        } else if ("debug".equals(group)) {
            executeDebug(server, sender, args);
        } else {
            throw new WrongUsageException(USAGE);
        }
    }

    private static void executePhase(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(USAGE);
        }
        if ("set".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[2]);
            double phase = parseCommandDouble(args[3], 0.0D, Double.MAX_VALUE);
            boolean changed;
            if (phase >= 1.1D && phase < 1.2D) {
                changed = storm.setPhase(1, storm.adjustAmountForEvolutionSpeed(150));
            } else if (phase >= 1.2D && phase < 2.0D) {
                changed = storm.setPhase(1, storm.adjustAmountForEvolutionSpeed(250));
            } else if (phase >= 2.1D && phase < 3.0D) {
                changed = storm.setPhase(2, storm.adjustAmountForEvolutionSpeed(800));
            } else if (phase >= 3.1D && phase < 3.2D) {
                changed = storm.setPhase(3, storm.adjustAmountForEvolutionSpeed(2350));
            } else if (phase >= 3.2D && phase < 4.0D) {
                changed = storm.setPhase(3, storm.adjustAmountForEvolutionSpeed(3500));
            } else if (phase >= 4.5D && phase < 5.0D) {
                changed = storm.setPhase(4, storm.getSubPhaseRequirement(4) + 1);
            } else if (phase >= 5.25D && phase < 5.5D) {
                changed = storm.setPhase(5, storm.getSubPhaseRequirement(5) + 1);
            } else if (phase >= 5.5D && phase < 6.0D) {
                changed = storm.setPhase(5, storm.getConsumptionAmountForPhase(5) + 1);
            } else if (phase >= 6.5D && phase < 7.0D) {
                changed = storm.setPhase(6, storm.getSubPhaseRequirement(6) + 1);
            } else if (phase >= 7.5D) {
                changed = storm.setPhase(7, storm.getConsumptionAmountForPhase(7) + 1);
            } else {
                changed = storm.setPhase(MathHelper.floor(phase));
            }
            if (changed) {
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.setphase.success", phase, storm.getDisplayName());
                EvolutionProfiler profiler = storm.getEvolutionProfiler();
                if (profiler.isProfiling()) profiler.begin();
            } else {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.setphase.invalid", phase,
                        storm.getDisplayName()));
            }
            return;
        }
        if ("get".equals(args[1])) {
            if (args.length != 3) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[2]);
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.getphase.result",
                    storm.getDisplayName(), storm.getPhase()));
            return;
        }
        if ("evolve".equals(args[1])) {
            requireArgs(args, 3);
            if (args.length > 4 || args.length == 4
                    && !"force".equals(args[3])) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[2]);
            boolean force = args.length > 3 && "force".equalsIgnoreCase(args[3]);
            boolean evolved = storm.evolve(force);
            if (evolved) {
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.evolve.success",
                        storm.getDisplayName(), storm.getPhase());
            } else {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.evolve.fail"));
            }
            return;
        }
        throw new WrongUsageException(USAGE);
    }

    private static void executeExplode(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length != 2) throw new WrongUsageException(USAGE);
        WitherStormEntity storm = storm(server, sender, args[1]);
        if (!storm.isPlayDeadAiDisabled()) {
            storm.onFormidibombExplosion();
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.explodeStorm.success", storm.getDisplayName());
        } else {
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.explodeStorm.failure", storm.getDisplayName()));
        }
    }

    private static void executeKill(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length != 2) throw new WrongUsageException(USAGE);
        WitherStormEntity storm = storm(server, sender, args[1]);
        EntityPlayerMP player = sender instanceof EntityPlayerMP ? (EntityPlayerMP) sender : null;
        DamageSource source = player == null
                ? DamageSource.OUT_OF_WORLD
                : ModDamageSources.playerAttackWitherStorm(player);
        storm.attackEntityFrom(source, Float.MAX_VALUE);
    }

    private static void executeRevive(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length != 2) throw new WrongUsageException(USAGE);
        WitherStormEntity storm = storm(server, sender, args[1]);
        if (storm.isPlayDeadAiDisabled()) {
            storm.reviveFromPlayingDead();
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.reviveStorm.success", storm.getDisplayName());
        } else {
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.reviveStorm.failure", storm.getDisplayName()));
        }
    }

    private static void executeEvolutionSpeed(MinecraftServer server, ICommandSender sender,
                                              String[] args) throws CommandException {
        if (args.length < 3 || !"set".equals(args[1])) {
            throw new WrongUsageException(USAGE);
        }
        if (args.length != 4) throw new WrongUsageException(USAGE);
        WitherStormEntity storm = storm(server, sender, args[2]);
        double value = parseCommandDouble(args[3], 0.1D, 32.0D);
        storm.setEvolutionSpeedModifier(value);
        storm.setPhase(storm.getPhase());
        notifyCommandListener(sender, new WitherStormAdminCommand(),
                "commands.witherstormmod.setevolution.success", value, storm.getDisplayName());
    }

    private static void executeConsumed(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(USAGE);
        }
        if ("get".equals(args[1])) {
            if (args.length != 3) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[2]);
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.getconsumed.result",
                    storm.getDisplayName(), storm.getConsumedMass(),
                    storm.getPhaseRequirement()));
            return;
        }
        if ("set".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[2]);
            int amount = "blackhole".equals(args[3]) ? 16000 : parseInt(args[3], 0);
            storm.setConsumedMass(amount);
            storm.checkConsumptionAmount();
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.setconsumed.success",
                    amount, storm.getDisplayName());
            return;
        }
        if ("lock".equals(args[1])) {
            if (args.length != 3) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[2]);
            if (storm.isConsumptionLocked()) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.lock.fail"));
                return;
            }
            storm.makeConsumptionLocked(true);
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.lock.success", storm.getDisplayName());
            return;
        }
        if ("unlock".equals(args[1])) {
            if (args.length != 3) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[2]);
            if (!storm.isConsumptionLocked()) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.unlock.fail"));
                return;
            }
            storm.makeConsumptionLocked(false);
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.unlock.success", storm.getDisplayName());
            return;
        }
        throw new WrongUsageException(USAGE);
    }

    private static void executeTarget(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(USAGE);
        }
        if ("set".equals(args[1])) {
            requireArgs(args, 4);
            if (args.length != 4 && args.length != 6) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[2]);
            if (args.length >= 6) {
                BlockPos target = parseBlockPos(sender, args, 3, false);
                BlockPos current = storm.getUltimateTargetManager().getBlockTargetOverride();
                if (target.equals(current)) {
                    sender.sendMessage(new TextComponentTranslation(
                            "commands.witherstormmod.ultimateTarget.set.duplicate"));
                    return;
                }
                storm.getUltimateTargetManager().setBlockTargetOverride(target);
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.ultimateTarget.set.success", target);
                return;
            }
            Entity target = getEntity(server, sender, args[3]);
            if (!(target instanceof EntityLivingBase) || target == storm
                    || target instanceof SupplementalEntities.WitherStormSegmentEntity) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.ultimateTarget.set.entity.invalid"));
                return;
            }
            UUID current = storm.getUltimateTargetManager().getTargetOverride();
            if (target.getUniqueID().equals(current)) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.ultimateTarget.set.duplicate"));
                return;
            }
            storm.getUltimateTargetManager().setTargetOverride(target.getUniqueID());
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.ultimateTarget.set.success",
                    target.getDisplayName());
            return;
        }
        if ("clear".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[2]);
            if ("entity".equals(args[3])) {
                storm.getUltimateTargetManager().setTargetOverride(null);
            } else if ("pos".equals(args[3])) {
                storm.getUltimateTargetManager().setBlockTargetOverride(null);
            } else {
                throw new WrongUsageException(USAGE);
            }
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.ultimateTarget.clear.success");
            return;
        }
        if ("get".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[3]);
            if ("pos".equals(args[2])) {
                Vec3d target = storm.getUltimateTargetPos();
                if (target == null) {
                    sender.sendMessage(new TextComponentTranslation(
                            "commands.witherstormmod.ultimateTarget.get.pos.none",
                            storm.getDisplayName()));
                    return;
                }
                double x = Math.round(target.x * 10.0D) / 10.0D;
                double y = Math.round(target.y * 10.0D) / 10.0D;
                double z = Math.round(target.z * 10.0D) / 10.0D;
                TextComponentTranslation click = new TextComponentTranslation(
                        "commands.witherstormmod.ultimateTarget.get.pos.click");
                click.setStyle(new Style().setColor(TextFormatting.BLUE).setClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tp " + x + " " + y + " " + z)));
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.ultimateTarget.get.pos",
                        storm.getDisplayName(), x, y, z, click));
                return;
            }
            if (!"entity".equals(args[2])) throw new WrongUsageException(USAGE);
            EntityLivingBase target = storm.getUltimateTarget();
            if (target == null) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.ultimateTarget.get.player.none",
                        storm.getDisplayName()));
            } else {
                TextComponentTranslation click = new TextComponentTranslation(
                        "commands.witherstormmod.ultimateTarget.get.player.click");
                click.setStyle(new Style().setColor(TextFormatting.BLUE).setClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tp " + target.getUniqueID())));
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.ultimateTarget.get.player",
                        storm.getDisplayName(), target.getDisplayName(), click));
            }
            return;
        }
        if ("distractions".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[3]);
            UltimateTargetManager manager = storm.getUltimateTargetManager();
            if ("makeDistracted".equals(args[2])) {
                if (manager.isDistracted()) {
                    sender.sendMessage(new TextComponentTranslation(
                            "commands.witherstormmod.distractions.ultimateTargetDistractions.makeDistracted.fail"));
                } else {
                    manager.makeDistracted(UltimateTargetManager.DistractionReason.FORCED);
                    notifyCommandListener(sender, new WitherStormAdminCommand(),
                            "commands.witherstormmod.distractions.ultimateTargetDistractions.makeDistracted.success",
                            storm.getDisplayName());
                }
                return;
            }
            if ("makeFocused".equals(args[2])) {
                if (!manager.isDistracted()) {
                    sender.sendMessage(new TextComponentTranslation(
                            "commands.witherstormmod.distractions.ultimateTargetDistractions.makeFocused.fail"));
                } else {
                    manager.makeFocused();
                    notifyCommandListener(sender, new WitherStormAdminCommand(),
                            "commands.witherstormmod.distractions.ultimateTargetDistractions.makeFocused.success",
                            storm.getDisplayName());
                }
                return;
            }
        }
        if ("chase".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[3]);
            UltimateTargetManager manager = storm.getUltimateTargetManager();
            if ("begin".equals(args[2])) {
                if (manager.isTargetStationary()) {
                    sender.sendMessage(new TextComponentTranslation(
                            "commands.witherstormmod.ultimateTarget.cannotBeginChase"));
                } else {
                    manager.accelerate();
                    notifyCommandListener(sender, new WitherStormAdminCommand(),
                            "commands.witherstormmod.ultimateTarget.beginChase",
                            storm.getDisplayName());
                }
                return;
            }
            if ("stop".equals(args[2])) {
                if (!manager.isTargetStationary()) {
                    sender.sendMessage(new TextComponentTranslation(
                            "commands.witherstormmod.ultimateTarget.cannotStopChase"));
                } else {
                    manager.deaccelerate();
                    notifyCommandListener(sender, new WitherStormAdminCommand(),
                            "commands.witherstormmod.ultimateTarget.stopChase",
                            storm.getDisplayName());
                }
                return;
            }
        }
        throw new WrongUsageException(USAGE);
    }

    private static void executeSickness(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length != 3) {
            throw new WrongUsageException(USAGE);
        }
        Entity entity = getEntity(server, sender, args[2]);
        if (!(entity instanceof EntityLivingBase)) {
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.entity.arg.notLiving"));
            return;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        if (UpstreamEntityTags.contains(UpstreamEntityTags.WITHER_SICKNESS_IMMUNE, living)) {
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.sickness.immune", living.getDisplayName()));
            return;
        }
        WitherSicknessTracker tracker = WitherSicknessCapability.get(living);
        if (tracker == null) return;
        if ("cure".equals(args[1])) {
            if (tracker.isBeingCured()) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.sickness.alreadyBeingCured",
                        living.getDisplayName()));
            } else if (tracker.isInfected()) {
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.sickness.startCure", living.getDisplayName());
                tracker.beginCure();
            } else {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.sickness.cureNotInfected"));
            }
            return;
        }
        if ("infect".equals(args[1])) {
            if (tracker.isInfected()) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.sickness.alreadyInfected",
                        living.getDisplayName()));
            } else {
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.sickness.startInfection",
                        living.getDisplayName());
                tracker.beginInfection();
            }
            return;
        }
        if ("randomizeModifiers".equals(args[1])) {
            tracker.randomizeModifiers();
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.sickness.randomizeModifiers",
                    living.getDisplayName());
            return;
        }
        throw new WrongUsageException(USAGE);
    }

    private static void executeTractorBeam(MinecraftServer server, ICommandSender sender,
                                           String[] args) throws CommandException {
        if (args.length < 2 || !"isInBeam".equals(args[1])) {
            throw new WrongUsageException(USAGE);
        }
        if (args.length != 4) throw new WrongUsageException(USAGE);
        Entity entity = getEntity(server, sender, args[2]);
        WitherStormEntity storm = storm(server, sender, args[3]);
        int head = storm.findContainingTractorBeamHead(entity, 4.0D);
        if (head >= 0) {
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.isInTractorBeam.success",
                    entity.getDisplayName(), head);
        } else {
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.isInTractorBeam.fail",
                    entity.getDisplayName()));
        }
    }

    private static void executeCluster(MinecraftServer server, ICommandSender sender,
                                       String[] args) throws CommandException {
        if (args.length < 2 || !"create".equals(args[1])) {
            throw new WrongUsageException(USAGE);
        }
        requireArgs(args, 7);
        World world = sender.getEntityWorld();
        boolean fill = args.length >= 9 && !args[7].startsWith("{");
        if (fill) {
            requireArgs(args, 9);
            BlockPos first = parseBlockPos(sender, args, 2, false);
            BlockPos second = parseBlockPos(sender, args, 5, false);
            int time = parseInt(args[8], 0);
            ClusterTail tail = parseClusterTail(server, sender, args, 9);
            createFilledCluster(world, first, second, time, tail.data, tail.storm);
        } else {
            BlockPos center = parseBlockPos(sender, args, 2, false);
            int radius = parseInt(args[5], 0);
            int time = parseInt(args[6], 0);
            ClusterTail tail = parseClusterTail(server, sender, args, 7);
            createRadiusCluster(world, center, radius, time, tail.data, tail.storm);
        }
    }

    private static void createRadiusCluster(World world, BlockPos center, int radius,
                                            int time, NBTTagCompound data,
                                            WitherStormEntity storm) {
        SupplementalEntities.BlockClusterEntity cluster =
                new SupplementalEntities.BlockClusterEntity(world);
        if (storm != null) {
            cluster.setNoGravity(true);
            cluster.setPhysics(false);
            storm.trackEntityToConsume(cluster);
        }
        if (data != null) cluster.readFromNBT(data);
        cluster.populateWithRadius(center, radius, (clusterWorld, position, state) -> true);
        cluster.setTime(time);
        world.spawnEntity(cluster);
    }

    private static void createFilledCluster(World world, BlockPos first, BlockPos second,
                                            int time, NBTTagCompound data,
                                            WitherStormEntity storm) {
        SupplementalEntities.BlockClusterEntity cluster =
                new SupplementalEntities.BlockClusterEntity(world);
        if (storm != null) {
            cluster.setNoGravity(true);
            cluster.setPhysics(false);
            storm.trackEntityToConsume(cluster);
        }
        if (data != null) cluster.readFromNBT(data);
        BlockPos minimum = new BlockPos(Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
        BlockPos maximum = new BlockPos(Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
        cluster.populate(minimum, maximum);
        cluster.setTime(time);
        world.spawnEntity(cluster);
    }

    private static WitherStormEntity optionalStorm(MinecraftServer server, ICommandSender sender,
                                                   String input) throws CommandException {
        Entity entity = getEntity(server, sender, input);
        if (!(entity instanceof WitherStormEntity)) {
            throw new EntityNotFoundException(
                    "commands.witherstormmod.entity.arg.invalid", input);
        }
        return (WitherStormEntity) entity;
    }

    private static ClusterTail parseClusterTail(MinecraftServer server, ICommandSender sender,
                                                String[] args, int start)
            throws CommandException {
        if (args.length <= start) return new ClusterTail(null, null);
        String complete = joinArguments(args, start, args.length);
        try {
            return new ClusterTail(JsonToNBT.getTagFromJson(complete), null);
        } catch (NBTException completeException) {
            if (args.length <= start + 1) throw invalidNbt(completeException);
            String withoutStorm = joinArguments(args, start, args.length - 1);
            NBTTagCompound data;
            try {
                data = JsonToNBT.getTagFromJson(withoutStorm);
            } catch (NBTException ignored) {
                throw invalidNbt(completeException);
            }
            return new ClusterTail(data,
                    optionalStorm(server, sender, args[args.length - 1]));
        }
    }

    private static CommandException invalidNbt(NBTException exception) {
        return new CommandException("commands.entitydata.tagError", exception.getMessage());
    }

    private static String joinArguments(String[] args, int start, int end) {
        StringBuilder builder = new StringBuilder();
        for (int index = start; index < end; index++) {
            if (index > start) builder.append(' ');
            builder.append(args[index]);
        }
        return builder.toString();
    }

    private static boolean isValidNbt(String[] args, int start, int end) {
        if (start >= end) return false;
        try {
            JsonToNBT.getTagFromJson(joinArguments(args, start, end));
            return true;
        } catch (NBTException exception) {
            return false;
        }
    }

    private static final class ClusterTail {
        private final NBTTagCompound data;
        private final WitherStormEntity storm;

        private ClusterTail(NBTTagCompound data, WitherStormEntity storm) {
            this.data = data;
            this.storm = storm;
        }
    }

    private static void executeShake(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length != 4) {
            throw new WrongUsageException(USAGE);
        }
        List<EntityPlayerMP> players = getPlayers(server, sender, args[1]);
        int duration = parseTime(args[2]);
        if (duration > 1200) {
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.screenShake.fail"));
            return;
        }
        float power = (float) parseCommandDouble(args[3], 0.0D, Float.MAX_VALUE);
        for (EntityPlayerMP player : players) {
            ModNetwork.shakePlayer(player, duration, power);
        }
        notifyCommandListener(sender, new WitherStormAdminCommand(),
                "commands.witherstormmod.screenShake.success", players.size());
    }

    private static void executeConversion(MinecraftServer server, ICommandSender sender,
                                          String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(USAGE);
        }
        if ("canConvert".equals(args[1])) {
            requireArgs(args, 3);
            if (args.length > 4) throw new WrongUsageException(USAGE);
            Entity entity = getEntity(server, sender, args[2]);
            if (!(entity instanceof EntityLiving)) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.conversion.invalid"));
                return;
            }
            boolean fromSickness = args.length > 3
                    && "fromWitherSickness".equals(args[3]);
            if (args.length > 3 && !fromSickness) throw new WrongUsageException(USAGE);
            boolean possible = TaintingManager.canConvertEntity(
                    (EntityLivingBase) entity, fromSickness);
            sender.sendMessage(new TextComponentTranslation(possible
                            ? "commands.witherstormmod.conversion.convert.possible"
                            : "commands.witherstormmod.conversion.convert.impossible",
                        entity.getDisplayName()));
            return;
        }
        if (args.length == 3 && ("toSickened".equals(args[2])
                || "toCured".equals(args[2]))) {
            Entity entity = getEntity(server, sender, args[1]);
            if ("toSickened".equals(args[2])) {
                if (!(entity instanceof EntityLiving)) {
                    sender.sendMessage(new TextComponentTranslation(
                            "commands.witherstormmod.conversion.invalid"));
                    return;
                }
                boolean converted = TaintingManager.convertEntity(
                        (EntityLivingBase) entity, false);
                if (converted) {
                    notifyCommandListener(sender, new WitherStormAdminCommand(),
                            "commands.witherstormmod.conversion.success",
                            entity.getDisplayName());
                } else {
                    sender.sendMessage(new TextComponentTranslation(
                            "commands.witherstormmod.conversion.fail"));
                }
                return;
            }
            if (!(entity instanceof SickenedMobEntity)) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.conversion.invalid"));
                return;
            }
            boolean cured = TaintingManager.cureEntity((SickenedMobEntity) entity);
            if (cured) {
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.conversion.success",
                        entity.getDisplayName());
            } else {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.conversion.fail"));
            }
            return;
        }
        if (args.length == 4) {
            BlockPos pos = parseBlockPos(sender, args, 1, false);
            WorldServer world = server.getWorld(sender.getEntityWorld().provider.getDimension());
            boolean converted = TaintingManager.taintBlock(world, pos);
            if (converted) {
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.conversion.block.success");
            } else {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.conversion.block.fail"));
            }
            return;
        }
        if (args.length == 7) {
            BlockPos first = parseBlockPos(sender, args, 1, false);
            BlockPos second = parseBlockPos(sender, args, 4, false);
            WorldServer world = server.getWorld(sender.getEntityWorld().provider.getDimension());
            int minX = Math.min(first.getX(), second.getX());
            int minY = Math.min(first.getY(), second.getY());
            int minZ = Math.min(first.getZ(), second.getZ());
            int maxX = Math.max(first.getX(), second.getX());
            int maxY = Math.max(first.getY(), second.getY());
            int maxZ = Math.max(first.getZ(), second.getZ());
            long total = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            int maxAllowed = 32768;
            if (total > maxAllowed) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.conversion.block.area.excessive", maxAllowed));
                return;
            }
            int converted = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (TaintingManager.taintBlock(world, new BlockPos(x, y, z))) {
                            converted++;
                        }
                    }
                }
            }
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.conversion.block.area.success", converted);
            return;
        }
        throw new WrongUsageException(USAGE);
    }

    private static void executeBowels(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(USAGE);
        }
        if ("new".equals(args[1])) {
            if (args.length != 3) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[2]);
            WorldServer bowels = server.getWorld(BowelsDimensions.DIMENSION_ID);
            BowelsInstanceData data = BowelsInstanceData.get(bowels);
            BowelsInstanceData.Instance instance = data.get(storm.getUniqueID());
            if (instance != null && !instance.completed) {
                instance.completed = true;
                data.markDirty();
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.newBowels.success", storm.getDisplayName());
            } else {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.newBowels.failure",
                        storm.getDisplayName()));
            }
            return;
        }
        if (args.length != 3) throw new WrongUsageException(USAGE);
        Entity entering = getEntity(server, sender, args[1]);
        WitherStormEntity storm = storm(server, sender, args[2]);
        if (entering.dimension == BowelsDimensions.DIMENSION_ID) {
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.enterBowels.dim.invalid"));
            return;
        }
        BowelsManager.BowelsEnterStatus status =
                BowelsManager.enterWithStatus(storm, entering);
        if (status == BowelsManager.BowelsEnterStatus.ENTITY_CANNOT_CHANGE) {
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.enterBowels.failure.cannotChangeDim"));
        }
        if (status != BowelsManager.BowelsEnterStatus.SUCCESS) {
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.enterBowels.failure",
                    storm.getDisplayName()));
        }
    }

    private static void executeDebug(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(USAGE);
        }
        if ("logging".equals(args[1])) {
            if (args.length != 3) throw new WrongUsageException(USAGE);
            if ("on".equals(args[2])) {
                StormDiagnosticLogger.setEnabled(true);
                ModNetwork.syncDiagnosticLogging();
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.debug.logging.enabled");
                return;
            }
            if ("off".equals(args[2])) {
                StormDiagnosticLogger.setEnabled(false);
                ModNetwork.syncDiagnosticLogging();
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.debug.logging.disabled");
                return;
            }
            if ("query".equals(args[2])) {
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.debug.logging.state",
                        StormDiagnosticLogger.isEnabled()));
                return;
            }
            throw new WrongUsageException(USAGE);
        }
        if (!debugCommandsAvailable()) throw new WrongUsageException(USAGE);
        if ("podium".equals(args[1])) {
            if (args.length != 7) throw new WrongUsageException(USAGE);
            WorldServer world = parseDimension(server, args[3]);
            BlockPos pos = parseBlockPos(sender, args, 4, false);
            if ("place".equals(args[2])) {
                StructureTemplates.place(world, "command_block_podium",
                        pos, Rotation.NONE, true);
            } else if ("remove".equals(args[2])) {
                StructureTemplates.remove(world, "command_block_podium",
                        pos, Rotation.NONE);
            } else {
                throw new WrongUsageException(USAGE);
            }
            return;
        }
        if ("debris".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            if (!"create".equals(args[2])) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[3]);
            boolean hidden = storm.isDeadOrPlayingDead();
            for (EntityPlayer player : storm.world.playerEntities) {
                if (player instanceof EntityPlayerMP) {
                    ModNetwork.createDebris((EntityPlayerMP) player, storm, hidden);
                }
            }
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.createDebris.success",
                    storm.getDisplayName());
            return;
        }
        if ("deathClusters".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            if (!"drop".equals(args[2])) throw new WrongUsageException(USAGE);
            WitherStormEntity storm = storm(server, sender, args[3]);
            storm.debugDropDeathClusters();
            return;
        }
        if ("beacon".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            if (!"reset".equals(args[2])) throw new WrongUsageException(USAGE);
            EntityPlayerMP player = getPlayer(server, sender, args[3]);
            player.getEntityData().setBoolean("WitherStormActivatedSuperBeacon", false);
            return;
        }
        if ("evolutionProfiler".equals(args[1])) {
            requireArgs(args, 3);
            if ("begin".equals(args[2])) {
                if (args.length != 3) throw new WrongUsageException(USAGE);
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                WitherStormEntity spawned = new WitherStormEntity(player.world);
                spawned.setPosition(player.posX, player.posY + 10.0D, player.posZ);
                spawned.getEvolutionProfiler().begin();
                if (!player.world.spawnEntity(spawned)) {
                    throw new CommandException(
                            "commands.witherstormmod.evolutionProfiler.spawn_failed");
                }
                WitherStormConfig.ultimateTargetingType =
                        WitherStormConfig.UltimateTargetingType.NONE;
                WitherStormConfig.evolutionAttributeModifier = 1.0D;
                return;
            }
            if ("query".equals(args[2])) {
                if (args.length != 4) throw new WrongUsageException(USAGE);
                WitherStormEntity storm = storm(server, sender, args[3]);
                EvolutionProfiler profiler = storm.getEvolutionProfiler();
                if (profiler.isProfiling()) {

                    sender.sendMessage(new TextComponentString(
                            "每秒吞噬实体数：" + (double) Math.round(
                                    profiler.getConsumedEntitiesPerSecond() * 10.0D) / 10.0D));
                }
                return;
            }
            throw new WrongUsageException(USAGE);
        }
        if ("splitCluster".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            Entity entity = getEntity(server, sender, args[2]);
            if (!(entity instanceof SupplementalEntities.BlockClusterEntity)) {
                return;
            }
            EnumFacing.Axis axis;
            try {
                axis = EnumFacing.Axis.valueOf(
                        args[3].toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new WrongUsageException(USAGE);
            }
            SupplementalEntities.BlockClusterEntity cluster =
                    (SupplementalEntities.BlockClusterEntity) entity;
            SupplementalEntities.BlockClusterEntity split = cluster.splitAt(axis);
            if (split != null) cluster.world.spawnEntity(split);
            return;
        }
        if ("symbiont".equals(args[1])) {
            if (args.length != 5) throw new WrongUsageException(USAGE);
            if (!"doSpell".equals(args[2])) throw new WrongUsageException(USAGE);
            Entity entity = getEntity(server, sender, args[3]);
            if (!(entity instanceof SickenedEntities.WitheredSymbiontEntity)) {
                return;
            }
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            ((SickenedEntities.WitheredSymbiontEntity) entity).setAttackTarget(player);
            String rawSpell = args[4].contains(":")
                    ? args[4] : Tags.MOD_ID + ":" + args[4].toLowerCase(java.util.Locale.ROOT);
            SpellType type = WitherStormModRegistries.getSpellType(rawSpell);
            if (type == null) return;
            ((SickenedEntities.WitheredSymbiontEntity) entity).setAndCastSpell(type);
            return;
        }
        if ("potionTest".equals(args[1])) {
            if (args.length != 2) throw new WrongUsageException(USAGE);
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            ItemStack stack = new ItemStack(Items.SPLASH_POTION);
            PotionUtils.addPotionToItemStack(stack, PotionTypes.EMPTY);
            PotionUtils.appendEffects(stack, Collections.singletonList(
                    new PotionEffect(MobEffects.WITHER, 60, 2)));
            EntityPotion potion = new EntityPotion(player.world, player, stack);
            player.world.spawnEntity(potion);
            return;
        }
        if ("caveRumble".equals(args[1])) {
            if (args.length != 4) throw new WrongUsageException(USAGE);
            List<EntityPlayerMP> players = getPlayers(server, sender, args[2]);
            double intensity = parseCommandDouble(args[3], 0.0D, 1.0D);
            WorldServer world = server.getWorld(sender.getEntityWorld().provider.getDimension());
            Random random = new Random();
            for (EntityPlayerMP player : players) {
                CaveRumbleManager.trigger(world, player, intensity, random);
            }
            return;
        }
        throw new WrongUsageException(USAGE);
    }

    private static void executeChunkLoader(MinecraftServer server, ICommandSender sender,
                                           String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(USAGE);
        }
        WorldServer world = sender.getEntityWorld() instanceof WorldServer
                ? (WorldServer) sender.getEntityWorld() : null;
        if (world == null) {
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.chunkloader.none"));
            return;
        }
        if ("get".equals(args[1])) {
            if (args.length > 3) throw new WrongUsageException(USAGE);
            if (args.length == 3) {
                WitherStormEntity storm = storm(server, sender, args[2]);
                ChunkLoadingManager.LoaderDescription description =
                        ChunkLoadingManager.describeStorm(world, storm.getUniqueID());
                if (description == null) {
                    sender.sendMessage(new TextComponentTranslation(
                            "commands.witherstormmod.chunkloader.none"));
                    return;
                }
                sender.sendMessage(new TextComponentTranslation(
                        "commands.witherstormmod.chunkloader.get.specific",
                        storm.getDisplayName(), storm.getPosition().getX(),
                        storm.getPosition().getZ(),
                        description.radius));
                notifyCommandListener(sender, new WitherStormAdminCommand(),
                        "commands.witherstormmod.chunkloader.get.specific.ticket",
                        description.ticketCount, new ChunkPos(storm.getPosition()));
                return;
            }
            List<ChunkLoadingManager.LoaderDescription> descriptions =
                    ChunkLoadingManager.describeStorms(world);
            for (ChunkLoadingManager.LoaderDescription description : descriptions) {
                TextComponentTranslation loader = new TextComponentTranslation(
                        "commands.witherstormmod.chunkloader.get.specific",
                        description.uuid, description.centerX << 4,
                        description.centerZ << 4, description.radius);
                loader.setStyle(new Style().setColor(TextFormatting.DARK_GRAY));
                sender.sendMessage(loader);
            }
            TextComponentTranslation summary = new TextComponentTranslation(
                    "commands.witherstormmod.chunkloader.get", descriptions.size());
            summary.setStyle(new Style().setColor(TextFormatting.YELLOW));
            sender.sendMessage(summary);
            int tickets = 0;
            for (ChunkLoadingManager.LoaderDescription description : descriptions) {
                tickets += description.ticketCount;
            }
            sender.sendMessage(new TextComponentTranslation(
                    "commands.witherstormmod.chunkloader.get.tickets", tickets,
                    dimensionLocation(world)));
            return;
        }
        if ("refresh".equals(args[1])) {
            if (args.length != 2) throw new WrongUsageException(USAGE);
            ChunkLoadingManager.refresh(world);
            notifyCommandListener(sender, new WitherStormAdminCommand(),
                    "commands.witherstormmod.chunkloader.refresh");
            return;
        }
        throw new WrongUsageException(USAGE);
    }

    private static WorldServer parseDimension(MinecraftServer server, String input)
            throws CommandException {
        int id;
        if ("minecraft:overworld".equals(input) || "overworld".equals(input)) {
            id = 0;
        } else if ("minecraft:the_nether".equals(input) || "the_nether".equals(input)
                || "nether".equals(input)) {
            id = -1;
        } else if ("minecraft:the_end".equals(input) || "the_end".equals(input)
                || "end".equals(input)) {
            id = 1;
        } else {
            try {
                id = Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                throw new CommandException("commands.generic.dimension.notFound", input);
            }
        }
        WorldServer world = server.getWorld(id);
        if (world == null || !DimensionManager.isDimensionRegistered(id)) {
            throw new CommandException("commands.generic.dimension.notFound", input);
        }
        return world;
    }

    private static String dimensionLocation(WorldServer world) {
        int dimension = world.provider.getDimension();
        if (dimension == 0) return "minecraft:overworld";
        if (dimension == -1) return "minecraft:the_nether";
        if (dimension == 1) return "minecraft:the_end";
        if (dimension == BowelsDimensions.DIMENSION_ID) return Tags.MOD_ID + ":bowels";
        return world.provider.getDimensionType().getName();
    }

    private static WitherStormEntity storm(MinecraftServer server, ICommandSender sender,
                                           String input) throws CommandException {
        Entity entity = getEntity(server, sender, input);
        if (!(entity instanceof WitherStormEntity)) {
            throw new EntityNotFoundException(
                    "commands.witherstormmod.entity.arg.invalid", input);
        }
        return (WitherStormEntity) entity;
    }

    private static void requireArgs(String[] args, int minimum) throws CommandException {
        if (args.length < minimum) {
            throw new WrongUsageException("commands.witherstormmod.usage");
        }
    }

    public static double parseCommandDouble(String value) throws CommandException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new WrongUsageException("commands.witherstormmod.number.invalid", value);
        }
    }

    private static double parseCommandDouble(String value, double minimum, double maximum)
            throws CommandException {
        double parsed = parseCommandDouble(value);
        if (!Double.isFinite(parsed) || parsed < minimum || parsed > maximum) {
            throw new WrongUsageException("commands.witherstormmod.number.invalid", value);
        }
        return parsed;
    }

    private static int parseTime(String value) throws CommandException {
        String raw = value;
        double multiplier = 1.0D;
        if (value.endsWith("t")) {
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("s")) {
            value = value.substring(0, value.length() - 1);
            multiplier = 20.0D;
        } else if (value.endsWith("d")) {
            value = value.substring(0, value.length() - 1);
            multiplier = 24000.0D;
        }
        double ticks = parseCommandDouble(value, 0.0D, Integer.MAX_VALUE / multiplier)
                * multiplier;
        if (!Double.isFinite(ticks)) {
            throw new WrongUsageException("commands.witherstormmod.number.invalid", raw);
        }
        return MathHelper.floor(ticks);
    }

    private static boolean debugCommandsAvailable() {
        return FMLLaunchHandler.isDeobfuscatedEnvironment();
    }

    private static List<String> entityTargets(MinecraftServer server, String[] args) {
        List<String> targets = new ArrayList<String>();
        Collections.addAll(targets, server.getOnlinePlayerNames());
        targets.add("@w");
        targets.add("@e");
        targets.add("@a");
        targets.add("@p");
        targets.add("@r");
        return getListOfStringsMatchingLastWord(args, targets);
    }

    private static List<String> playerTargets(MinecraftServer server, String[] args) {
        List<String> targets = new ArrayList<String>();
        Collections.addAll(targets, server.getOnlinePlayerNames());
        targets.add("@a");
        targets.add("@p");
        targets.add("@r");
        return getListOfStringsMatchingLastWord(args, targets);
    }

    private static List<String> dimensionTargets(String[] args) {
        List<String> targets = new ArrayList<String>();
        Collections.addAll(targets, "minecraft:overworld", "minecraft:the_nether",
                "minecraft:the_end", "overworld", "nether", "end");
        for (int dimension : DimensionManager.getIDs()) {
            targets.add(Integer.toString(dimension));
        }
        return getListOfStringsMatchingLastWord(args, targets);
    }

    private static List<String> mergeCompletions(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<String>();
        if (first != null) merged.addAll(first);
        if (second != null) {
            for (String value : second) {
                if (!merged.contains(value)) merged.add(value);
            }
        }
        return merged;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            List<String> roots = new ArrayList<String>();
            Collections.addAll(roots, "phase", "explode", "kill", "revive",
                    "evolutionSpeed", "consumedEntities", "ultimateTarget", "sickness",
                    "tractorBeam", "screenShake", "convert", "cluster", "bowels",
                    "chunkLoader", "config");
            roots.add("debug");
            return getListOfStringsMatchingLastWord(args, roots);
        }
        if (args.length == 2) {
            String group = args[0];
            if ("phase".equals(group)) {
                return getListOfStringsMatchingLastWord(args, "set", "get", "evolve");
            }
            if ("consumedEntities".equals(group)) {
                return getListOfStringsMatchingLastWord(args,
                        "get", "set", "lock", "unlock");
            }
            if ("ultimateTarget".equals(group)) {
                return getListOfStringsMatchingLastWord(args,
                        "set", "get", "clear", "distractions", "chase");
            }
            if ("sickness".equals(group)) {
                return getListOfStringsMatchingLastWord(args,
                        "cure", "infect", "randomizeModifiers");
            }
            if ("tractorBeam".equals(group)) {
                return getListOfStringsMatchingLastWord(args, "isInBeam");
            }
            if ("cluster".equals(group)) {
                return getListOfStringsMatchingLastWord(args, "create");
            }
            if ("convert".equals(group)) {
                List<String> values = new ArrayList<String>();
                values.add("canConvert");
                Collections.addAll(values, server.getOnlinePlayerNames());
                values.add("@e");
                return mergeCompletions(getListOfStringsMatchingLastWord(args, values),
                        getTabCompletionCoordinate(args, 1, targetPos));
            }
            if ("bowels".equals(group)) {
                List<String> values = new ArrayList<String>();
                values.add("new");
                Collections.addAll(values, server.getOnlinePlayerNames());
                values.add("@e");
                return getListOfStringsMatchingLastWord(args, values);
            }
            if ("chunkLoader".equals(group)) {
                return getListOfStringsMatchingLastWord(args, "get", "refresh");
            }
            if ("config".equals(group)) {
                return WitherStormConfigCommandSupport.complete(args);
            }
            if ("debug".equals(group)) {
                List<String> values = new ArrayList<String>();
                values.add("logging");
                if (debugCommandsAvailable()) {
                    Collections.addAll(values, "podium", "debris", "deathClusters",
                            "beacon", "evolutionProfiler", "splitCluster", "symbiont",
                            "potionTest", "caveRumble");
                }
                return getListOfStringsMatchingLastWord(args, values);
            }
            if ("evolutionSpeed".equals(group)) {
                return getListOfStringsMatchingLastWord(args, "set");
            }
            if ("screenShake".equals(group)) {
                return playerTargets(server, args);
            }
            if ("explode".equals(group) || "kill".equals(group)
                    || "revive".equals(group)) {
                return entityTargets(server, args);
            }
        }
        if (args.length == 3) {
            String group = args[0];
            if ("config".equals(group)) {
                return WitherStormConfigCommandSupport.complete(args);
            }
            if (("phase".equals(group) || "consumedEntities".equals(group)
                    || "evolutionSpeed".equals(group) || "sickness".equals(group))
                    && !("phase".equals(group) && "evolve".equals(args[1]))) {
                return entityTargets(server, args);
            }
            if ("phase".equals(group) && "evolve".equals(args[1])) {
                return entityTargets(server, args);
            }
            if ("ultimateTarget".equals(group)) {
                if ("get".equals(args[1])) {
                    return getListOfStringsMatchingLastWord(args, "pos", "entity");
                }
                if ("distractions".equals(args[1])) {
                    return getListOfStringsMatchingLastWord(args,
                            "makeDistracted", "makeFocused");
                }
                if ("chase".equals(args[1])) {
                    return getListOfStringsMatchingLastWord(args, "begin", "stop");
                }
                return entityTargets(server, args);
            }
            if ("tractorBeam".equals(group) || "bowels".equals(group)
                    || "chunkLoader".equals(group)) {
                return entityTargets(server, args);
            }
            if ("convert".equals(group)) {
                if ("canConvert".equals(args[1])) return entityTargets(server, args);
                return mergeCompletions(
                        getListOfStringsMatchingLastWord(args, "toSickened", "toCured"),
                        getTabCompletionCoordinate(args, 1, targetPos));
            }
            if ("cluster".equals(group) && "create".equals(args[1])) {
                return getTabCompletionCoordinate(args, 2, targetPos);
            }
        }
        if (args.length == 3 && "debug".equals(args[0]) && debugCommandsAvailable()) {
            if ("evolutionProfiler".equals(args[1])) {
                return getListOfStringsMatchingLastWord(args, "begin", "query");
            }
            if ("deathClusters".equals(args[1])) {
                return getListOfStringsMatchingLastWord(args, "drop");
            }
            if ("beacon".equals(args[1])) {
                return getListOfStringsMatchingLastWord(args, "reset");
            }
            if ("symbiont".equals(args[1])) {
                return getListOfStringsMatchingLastWord(args, "doSpell");
            }
            if ("podium".equals(args[1])) {
                return getListOfStringsMatchingLastWord(args, "place", "remove");
            }
            if ("debris".equals(args[1])) {
                return getListOfStringsMatchingLastWord(args, "create");
            }
            if ("splitCluster".equals(args[1])) {
                return entityTargets(server, args);
            }
            if ("caveRumble".equals(args[1])) {
                return playerTargets(server, args);
            }
        }
        if (args.length == 3 && "debug".equals(args[0])
                && "logging".equals(args[1])) {
            return getListOfStringsMatchingLastWord(args, "on", "off", "query");
        }
        if (args.length == 4
                && "consumedEntities".equals(args[0])
                && "set".equals(args[1])) {
            return getListOfStringsMatchingLastWord(args, "blackhole");
        }
        if (args.length == 4 && "phase".equals(args[0])
                && "evolve".equals(args[1])) {
            return getListOfStringsMatchingLastWord(args, "force");
        }
        if (args.length == 4 && "ultimateTarget".equals(args[0])) {
            if ("get".equals(args[1]) || "distractions".equals(args[1])
                    || "chase".equals(args[1])) return entityTargets(server, args);
            if ("clear".equals(args[1])) {
                return getListOfStringsMatchingLastWord(args, "entity", "pos");
            }
            if ("set".equals(args[1])) {
                return mergeCompletions(entityTargets(server, args),
                        getTabCompletionCoordinate(args, 3, targetPos));
            }
        }
        if (args.length == 4 && "tractorBeam".equals(args[0])
                && "isInBeam".equals(args[1])) {
            return entityTargets(server, args);
        }
        if (args.length == 4 && "debug".equals(args[0]) && debugCommandsAvailable()) {
            if (("debris".equals(args[1]) && "create".equals(args[2]))
                    || ("deathClusters".equals(args[1]) && "drop".equals(args[2]))
                    || ("evolutionProfiler".equals(args[1]) && "query".equals(args[2]))) {
                return entityTargets(server, args);
            }
            if ("beacon".equals(args[1]) && "reset".equals(args[2])) {
                return playerTargets(server, args);
            }
            if ("symbiont".equals(args[1]) && "doSpell".equals(args[2])) {
                return entityTargets(server, args);
            }
            if ("splitCluster".equals(args[1])) {
                return getListOfStringsMatchingLastWord(args, "x", "y", "z");
            }
            if ("podium".equals(args[1])
                    && ("place".equals(args[2]) || "remove".equals(args[2]))) {
                return dimensionTargets(args);
            }
        }
        if (args.length == 4 && "convert".equals(args[0])
                && "canConvert".equals(args[1])) {
            return getListOfStringsMatchingLastWord(args, "fromWitherSickness");
        }
        if (args.length == 4 && "convert".equals(args[0])
                && !"toSickened".equals(args[2]) && !"toCured".equals(args[2])) {
            return getTabCompletionCoordinate(args, 1, targetPos);
        }
        if (args.length >= 5 && args.length <= 7 && "convert".equals(args[0])
                && !"toSickened".equals(args[2]) && !"toCured".equals(args[2])) {
            return getTabCompletionCoordinate(args, 4, targetPos);
        }
        if (args.length >= 5 && args.length <= 6
                && "ultimateTarget".equals(args[0]) && "set".equals(args[1])) {
            return getTabCompletionCoordinate(args, 3, targetPos);
        }
        if (args.length >= 4 && args.length <= 5
                && "cluster".equals(args[0]) && "create".equals(args[1])) {
            return getTabCompletionCoordinate(args, 2, targetPos);
        }
        if (args.length >= 6 && args.length <= 8
                && "cluster".equals(args[0]) && "create".equals(args[1])) {
            return getTabCompletionCoordinate(args, 5, targetPos);
        }
        if (args.length >= 9 && "cluster".equals(args[0])
                && "create".equals(args[1])) {
            int dataStart = !args[7].startsWith("{") ? 9 : 7;
            if (isValidNbt(args, dataStart, args.length - 1)) {
                return entityTargets(server, args);
            }
        }
        if ((args.length == 4 || args.length == 5) && "config".equals(args[0])) {
            return WitherStormConfigCommandSupport.complete(args);
        }
        if (args.length >= 5 && args.length <= 7
                && "debug".equals(args[0]) && debugCommandsAvailable()
                && "podium".equals(args[1])
                && ("place".equals(args[2]) || "remove".equals(args[2]))) {
            return getTabCompletionCoordinate(args, 4, targetPos);
        }
        if (args.length == 5 && "debug".equals(args[0]) && debugCommandsAvailable()
                && "symbiont".equals(args[1]) && "doSpell".equals(args[2])) {
            List<String> spells = new ArrayList<String>();
            for (SpellType spell : WitherStormModRegistries.getSpellTypes()) {
                if (spell.getRegistryName() != null) spells.add(spell.getRegistryName().toString());
            }
            return getListOfStringsMatchingLastWord(args, spells);
        }
        return Collections.<String>emptyList();
    }
}
