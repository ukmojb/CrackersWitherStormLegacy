package com.wdcftgg.witherstormmod.common.capability;

import com.wdcftgg.witherstormmod.common.config.WitherStormConfig;
import com.wdcftgg.witherstormmod.common.init.ModEffects;
import com.wdcftgg.witherstormmod.common.network.ModNetwork;
import com.wdcftgg.witherstormmod.common.potion.WitherSicknessEffect;
import com.wdcftgg.witherstormmod.common.resource.UpstreamEntityTags;
import com.wdcftgg.witherstormmod.common.taint.TaintingManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nullable;
import java.util.Collections;


public final class WitherSicknessTracker {
    private static final int MINIMUM_STAGE_TICKS = 1200;
    private static final int EFFECT_DURATION = 12000;
    private static final int EFFECT_RENEWAL_THRESHOLD = 7200;
    private static final int AMPLIFIER_DECREASE_TIME = 7200;
    private static final int CONTACT_DECREASE_TIME = 9600;

    private EntityLivingBase entity;
    private int requiredProximityTicks;
    private int applicationDelay;
    private int cureDelay;
    private int requiredContacts;
    private int proximityTicksModifier;
    private int applicationDelayModifier;
    private int cureDelayModifier;
    private int proximityTicks;
    private int delayTicks;
    private int contacts;
    private int totalInfections;
    private int totalCures;
    private int amplifierDecreaseTicks;
    private int amplifier;
    private int contactsDecreaseTicks;
    private int cureDelayTicks;
    private boolean infected;
    private boolean beingCured;
    private boolean nearStorm;
    private boolean actuallyImmune = true;
    private boolean shouldSynchronize;

    public WitherSicknessTracker() {
        loadConfiguredDurations(false);
    }

    public WitherSicknessTracker(EntityLivingBase entity) {
        this.entity = entity;
        loadConfiguredDurations(true);
    }

    private void loadConfiguredDurations(boolean randomize) {
        boolean lowImmunity = entity != null && isLowImmunity();
        requiredProximityTicks = Math.max(240, (lowImmunity
                ? WitherStormConfig.lowImmuneRequiredProximitySeconds
                : WitherStormConfig.requiredProximitySeconds) * 20);
        applicationDelay = Math.max(240, (lowImmunity
                ? WitherStormConfig.lowImmuneApplicationDelay
                : WitherStormConfig.applicationDelay) * 20);
        cureDelay = Math.max(240, (lowImmunity
                ? WitherStormConfig.lowImmuneCureDelay
                : WitherStormConfig.cureDelay) * 20);
        requiredContacts = Math.max(1, WitherStormConfig.requiredContacts);
        if (randomize && entity != null && !entity.world.isRemote) randomizeModifiers(lowImmunity);
    }

    private void randomizeModifiers(boolean lowImmunity) {
        if (lowImmunity) {
            proximityTicksModifier = -entity.getRNG().nextInt(Math.max(240,
                    WitherStormConfig.lowImmuneProximityModifierMax * 20));
            applicationDelayModifier = -entity.getRNG().nextInt(Math.max(240,
                    WitherStormConfig.lowImmuneApplicationModifierMax * 20));
            cureDelayModifier = -entity.getRNG().nextInt(Math.max(240,
                    WitherStormConfig.lowImmuneCureDelayModifierMax * 20));
        } else {
            proximityTicksModifier = entity.getRNG().nextInt(Math.max(240,
                    WitherStormConfig.proximitySecondsModifierMax * 20));
            applicationDelayModifier = entity.getRNG().nextInt(Math.max(240,
                    WitherStormConfig.applicationDelayModifierMax * 20));
            cureDelayModifier = entity.getRNG().nextInt(Math.max(240,
                    WitherStormConfig.cureDelayModifierMax * 20));
        }
    }


    public void randomizeModifiers() {
        randomizeModifiers(entity != null && isLowImmunity());
    }

    public void copyFrom(WitherSicknessTracker original) {
        requiredProximityTicks = original.requiredProximityTicks;
        applicationDelay = original.applicationDelay;
        cureDelay = original.cureDelay;
        requiredContacts = original.requiredContacts;
        proximityTicksModifier = original.proximityTicksModifier;
        applicationDelayModifier = original.applicationDelayModifier;
        cureDelayModifier = original.cureDelayModifier;
        proximityTicks = original.proximityTicks;
        delayTicks = original.delayTicks;
        contacts = original.contacts;
        totalInfections = original.totalInfections;
        totalCures = original.totalCures;
        amplifierDecreaseTicks = original.amplifierDecreaseTicks;
        amplifier = original.amplifier;
        contactsDecreaseTicks = original.contactsDecreaseTicks;
        cureDelayTicks = original.cureDelayTicks;
        infected = original.infected;
        beingCured = original.beingCured;
        nearStorm = original.nearStorm;
        actuallyImmune = original.actuallyImmune;
        shouldSynchronize = true;
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("ProximityTicks", proximityTicks);
        tag.setInteger("DelayTicks", delayTicks);
        tag.setInteger("Contacts", contacts);
        tag.setInteger("AmplifierDecreaseTicks", amplifierDecreaseTicks);
        tag.setInteger("Amplifier", amplifier);
        tag.setInteger("TotalInfections", totalInfections);
        tag.setBoolean("IsInfected", infected);
        tag.setInteger("ProximityTicksModifier", proximityTicksModifier);
        tag.setInteger("ApplicationDelayModifier", applicationDelayModifier);
        tag.setInteger("CureDelayModifier", cureDelayModifier);
        tag.setInteger("ContactsDecreaseTicks", contactsDecreaseTicks);
        tag.setInteger("CureDelayTicks", cureDelayTicks);
        tag.setBoolean("IsBeingCured", beingCured);
        tag.setInteger("TotalCures", totalCures);
        tag.setBoolean("IsActuallyImmune", actuallyImmune);
        return tag;
    }

    public void read(NBTTagCompound tag) {
        proximityTicks = tag.getInteger("ProximityTicks");
        delayTicks = tag.getInteger("DelayTicks");
        contacts = tag.getInteger("Contacts");
        amplifierDecreaseTicks = tag.getInteger("AmplifierDecreaseTicks");
        amplifier = tag.getInteger("Amplifier");
        totalInfections = tag.getInteger("TotalInfections");
        infected = tag.getBoolean("IsInfected");
        proximityTicksModifier = tag.getInteger("ProximityTicksModifier");
        applicationDelayModifier = tag.getInteger("ApplicationDelayModifier");
        cureDelayModifier = tag.getInteger("CureDelayModifier");
        contactsDecreaseTicks = tag.getInteger("ContactsDecreaseTicks");
        cureDelayTicks = tag.getInteger("CureDelayTicks");
        beingCured = tag.getBoolean("IsBeingCured");
        totalCures = tag.getInteger("TotalCures");
        if (tag.hasKey("IsActuallyImmune")) actuallyImmune = tag.getBoolean("IsActuallyImmune");
    }

    public void tick() {
        if (entity == null || entity.world.isRemote) return;
        boolean immune = !WitherStormConfig.witherSicknessEnabled
                || UpstreamEntityTags.contains(UpstreamEntityTags.WITHER_SICKNESS_IMMUNE, entity);
        if (actuallyImmune != immune) {
            actuallyImmune = immune;
            shouldSynchronize = true;
        }
        if (actuallyImmune) {
            synchronizeIfNeeded();
            return;
        }

        int previousProximityTicks = proximityTicks;
        int previousDelayTicks = delayTicks;
        int previousCureDelayTicks = cureDelayTicks;
        if (nearStorm) {
            if (proximityTicks < getRequiredProximityTicks()) proximityTicks++;
        } else {
            if (proximityTicks > 0) proximityTicks--;
            if (!infected && amplifierDecreaseTicks < AMPLIFIER_DECREASE_TIME) {
                amplifierDecreaseTicks++;
            } else if (!infected) {
                amplifierDecreaseTicks = 0;
                if (amplifier > 0) amplifier--;
            }
            if (contactsDecreaseTicks < CONTACT_DECREASE_TIME) {
                contactsDecreaseTicks++;
            } else {
                contactsDecreaseTicks = 0;
                if (contacts > 0) contacts--;
            }
        }

        if (previousProximityTicks != proximityTicks
                && proximityTicks >= getRequiredProximityTicks()) beginInfection();
        if (infected) {
            if (delayTicks < getApplicationDelay() && !beingCured) delayTicks++;
            if (delayTicks >= getApplicationDelay()) {
                if (delayTicks != previousDelayTicks) infect();
                renewEffect();
            }
            if (!entity.isPotionActive(ModEffects.WITHER_SICKNESS)
                    && delayTicks >= getApplicationDelay()) {
                setInfected(false);
                setProximityTicks(0);
            }
            if (beingCured) {
                if (cureDelayTicks < getCureDelay()) cureDelayTicks++;
                if (cureDelayTicks >= getCureDelay()
                        && cureDelayTicks != previousCureDelayTicks) cure();
            } else if (cureDelayTicks >= getCureDelay()) {
                cureDelayTicks = 0;
            }
        } else {
            delayTicks = 0;
        }

        if (entity.ticksExisted % 120 == 0) shouldSynchronize = true;
        synchronizeIfNeeded();
    }


    public void tickClient() {
        if (entity == null || !entity.world.isRemote) return;
        if (isInfected() && delayTicks < getApplicationDelay()) delayTicks++;
        if (isBeingCured() && cureDelayTicks < getCureDelay()) cureDelayTicks++;
    }

    private void renewEffect() {
        PotionEffect current = entity.getActivePotionEffect(ModEffects.WITHER_SICKNESS);
        if (current == null || current.getDuration() >= EFFECT_RENEWAL_THRESHOLD) return;
        entity.addPotionEffect(createEffect(current.getAmplifier()));
    }

    private PotionEffect createEffect(int effectAmplifier) {
        PotionEffect effect = new PotionEffect(ModEffects.WITHER_SICKNESS,
                EFFECT_DURATION, effectAmplifier, false, false);
        effect.setCurativeItems(Collections.emptyList());
        return effect;
    }

    private void synchronizeIfNeeded() {
        if (!shouldSynchronize) return;
        ModNetwork.syncWitherSickness(entity, write());
        shouldSynchronize = false;
    }

    public void beginInfection() {
        if (actuallyImmune) return;
        infected = true;
        beingCured = false;
        shouldSynchronize = true;
    }

    public void infect() {
        if (beingCured || actuallyImmune
                || entity.isEntityInvulnerable(WitherSicknessEffect.DAMAGE_SOURCE)
                || entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isCreativeMode) return;
        totalInfections++;
        entity.addPotionEffect(createEffect(getAmplifier()));
        if (WitherStormConfig.increaseAmplifier) amplifier++;
        shouldSynchronize = true;
    }

    public void beginCure() {
        if (!infected || beingCured || actuallyImmune) return;
        beingCured = true;
        shouldSynchronize = true;
    }

    public void cure() {
        if (!infected || actuallyImmune) return;
        entity.removePotionEffect(ModEffects.WITHER_SICKNESS);
        infected = false;
        proximityTicks = 0;
        cureDelayTicks = 0;
        beingCured = false;
        totalCures++;
        shouldSynchronize = true;
    }

    public void resetInfection() {
        infected = false;
        beingCured = false;
        nearStorm = false;
        proximityTicks = 0;
        delayTicks = 0;
        contacts = 0;
        contactsDecreaseTicks = 0;
        cureDelayTicks = 0;
        shouldSynchronize = true;
    }

    public void countContact() {
        if (actuallyImmune) return;
        contacts++;
        shouldSynchronize = true;
        if (contacts > requiredContacts) beginInfection();
    }

    public int getRequiredProximityTicks() {
        return Math.max(MINIMUM_STAGE_TICKS, requiredProximityTicks + proximityTicksModifier);
    }

    public int getApplicationDelay() {
        return Math.max(MINIMUM_STAGE_TICKS, applicationDelay + applicationDelayModifier);
    }

    public int getCureDelay() {
        return Math.max(MINIMUM_STAGE_TICKS, cureDelay + cureDelayModifier);
    }

    public int getAmplifier() {
        return TaintingManager.canConvertEntity(entity) ? amplifier + 5 : amplifier;
    }

    public boolean isLowImmunity() {
        return entity != null && !UpstreamEntityTags.contains(UpstreamEntityTags.HIGH_IMMUNITY, entity);
    }

    public boolean isInfected() {
        return infected;
    }

    public boolean isBeingCured() {
        return beingCured;
    }

    public boolean isActuallyImmune() {
        return actuallyImmune;
    }

    public boolean isNearStorm() {
        return nearStorm;
    }

    public void setNearStorm(boolean value) {
        if (actuallyImmune || nearStorm == value) return;
        nearStorm = value;
        shouldSynchronize = true;
    }

    public void setInfected(boolean value) {
        if (actuallyImmune || infected == value) return;
        infected = value;
        if (value) beingCured = false;
        shouldSynchronize = true;
    }

    public void setProximityTicks(int value) {
        if (actuallyImmune || proximityTicks == value) return;
        proximityTicks = value;
        shouldSynchronize = true;
    }

    public int getProximityTicks() {
        return proximityTicks;
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public int getContacts() {
        return contacts;
    }

    public int getTotalInfections() {
        return totalInfections;
    }

    public int getTotalCures() {
        return totalCures;
    }

    public int getCureDelayTicks() {
        return cureDelayTicks;
    }

    public int getBaseAmplifier() {
        return amplifier;
    }

    @Nullable
    public EntityLivingBase getEntity() {
        return entity;
    }
}
