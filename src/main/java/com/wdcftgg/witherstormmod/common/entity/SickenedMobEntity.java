package com.wdcftgg.witherstormmod.common.entity;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.common.init.ModItems;
import com.wdcftgg.witherstormmod.common.init.ModEffects;
import com.wdcftgg.witherstormmod.common.init.ModCreatureAttributes;
import com.wdcftgg.witherstormmod.common.init.ModSounds;
import com.wdcftgg.witherstormmod.common.taint.TaintingManager;
import net.minecraft.block.BlockBed;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.init.SoundEvents;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class SickenedMobEntity extends EntityMob {

    private static final DataParameter<Boolean> CONVERTING = EntityDataManager.createKey(SickenedMobEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> CHILD = EntityDataManager.createKey(SickenedMobEntity.class, DataSerializers.BOOLEAN);

    private static final java.util.Set<String> UPSTREAM_LOOT_TABLES = new java.util.HashSet<String>(java.util.Arrays.asList(
            "sickened_chicken", "sickened_cow", "sickened_creeper", "sickened_iron_golem",
            "sickened_mushroom_cow", "sickened_phantom", "sickened_pig", "sickened_skeleton",
            "sickened_snow_golem", "sickened_spider", "sickened_villager", "sickened_zombie",
            "withered_symbiont"));

    private int conversionTime = -1;
    private UUID conversionStarter;
    private ResourceLocation originalType;
    private NBTTagCompound originalData;
    private float adultWidth = 0.6F;
    private float adultHeight = 1.8F;
    private boolean childStateReady;
    private int sickenedAge;

    protected SickenedMobEntity(World worldIn) {
        super(worldIn);
        experienceValue = getSickenedExperience();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(CONVERTING, false);
        dataManager.register(CHILD, false);
        childStateReady = true;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackMelee(this, 1.15D, true));
        tasks.addTask(6, new EntityAIWanderAvoidWater(this, 0.9D));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 12.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
        addSickenedMobTargetGoal(3);
    }

    protected final void addSickenedMobTargetGoal(int priority) {
        targetTasks.addTask(priority, new EntityAINearestAttackableTarget<EntityLiving>(this, EntityLiving.class, 10, true, false,
                target -> target != null
                        && !(target instanceof SickenedMobEntity)
                        && !(target instanceof WitherStormEntity)
                        && !(target instanceof SupplementalEntities.StormPartBase)
                        && !(target instanceof EntityWither)
                        && !(target instanceof EntityWitherSkeleton)
                        && !(target instanceof EntityCreeper)
                        && !(target instanceof EntityEnderman)
                        && (target instanceof EntityVillager
                        || target instanceof EntityGolem
                        || target instanceof EntityMob
                        || target instanceof EntitySlime
                        || target instanceof EntityBat
                        || target instanceof EntityAnimal)));
    }

    protected final void initStandardAnimalAI(double attackSpeed) {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIAttackMelee(this, attackSpeed, false));
        tasks.addTask(2, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(3, new EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(4, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
        addSickenedMobTargetGoal(2);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(getSickenedHealth());
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(getSickenedSpeed());
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(getSickenedDamage());
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(getSickenedFollowRange());
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(getSickenedArmor());
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(getSickenedKnockbackResistance());
    }

    protected double getSickenedHealth() {
        return 24.0D;
    }

    protected double getSickenedSpeed() {
        return 0.28D;
    }

    protected double getSickenedDamage() {
        return 2.0D;
    }

    protected double getSickenedFollowRange() {
        return 35.0D;
    }

    protected double getSickenedArmor() {
        return 0.0D;
    }

    protected double getSickenedKnockbackResistance() {
        return 0.0D;
    }

    protected int getSickenedExperience() {
        return 5;
    }

    protected boolean growsFromChild() {
        return false;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!world.isRemote && growsFromChild() && sickenedAge < 0) {
            ++sickenedAge;
            if (sickenedAge == 0) setSickenedChild(false);
        }
        if (!world.isRemote && isEntityAlive() && isConverting()) {
            conversionTime -= getConversionProgress();
            if (conversionTime <= 0) {
                TaintingManager.cureEntity(this);
            }
        }
    }

    private int getConversionProgress() {
        int progress = 1;
        if (rand.nextFloat() >= 0.01F) return progress;
        int found = 0;
        BlockPos origin = new BlockPos((int) posX, (int) posY, (int) posZ);
        for (int x = -4; x < 4 && found < 14; x++) {
            for (int y = -4; y < 4 && found < 14; y++) {
                for (int z = -4; z < 4 && found < 14; z++) {
                    Block block = world.getBlockState(origin.add(x, y, z)).getBlock();
                    if (block != Blocks.IRON_BARS && !(block instanceof BlockBed)) continue;
                    if (rand.nextFloat() < 0.3F) progress++;
                    found++;
                }
            }
        }
        return progress;
    }

    public void startConverting(@Nullable UUID player, int duration) {
        if (getOriginalType() == null || isConverting()) return;
        conversionStarter = player;
        conversionTime = duration;
        dataManager.set(CONVERTING, true);
        int amplifier = Math.min(world.getDifficulty().getId() - 1, 0);
        addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, duration, amplifier));
        playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.3F);
    }

    public void setConversionTime(int duration) {
        conversionTime = Math.max(0, duration);
    }

    public void setSickenedChild(boolean child) {
        dataManager.set(CHILD, child);
        if (child && growsFromChild() && sickenedAge >= 0) sickenedAge = -24000;
        if (!child && sickenedAge < 0) sickenedAge = 0;
        applyChildSize();
    }

    @Override
    public boolean isChild() {
        return childStateReady && dataManager.get(CHILD);
    }

    @Override
    protected void setSize(float width, float height) {
        adultWidth = width;
        adultHeight = height;
        if (childStateReady && isChild()) {
            super.setSize(width * 0.5F, height * 0.5F);
        } else {
            super.setSize(width, height);
        }
    }

    private void applyChildSize() {
        if (!childStateReady) return;
        float scale = isChild() ? 0.5F : 1.0F;
        super.setSize(adultWidth * scale, adultHeight * scale);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (CHILD.equals(key)) applyChildSize();
    }

    public boolean isConverting() {
        return dataManager.get(CONVERTING);
    }

    public int getConversionTime() {
        return conversionTime;
    }

    @Nullable
    public UUID getConversionStarter() {
        return conversionStarter;
    }

    public void rememberOriginal(EntityLivingBase original) {
        originalType = EntityList.getKey(original);
        NBTTagCompound saved = new NBTTagCompound();
        original.writeToNBT(saved);
        saved.removeTag("UUIDMost");
        saved.removeTag("UUIDLeast");
        saved.removeTag("Pos");
        saved.removeTag("Motion");
        saved.removeTag("Rotation");
        saved.removeTag("Dimension");
        saved.removeTag("id");
        originalData = saved;
    }

    public void copySpeciesDataFrom(EntityLivingBase original) {
        if (growsFromChild() && original instanceof EntityAgeable) {
            sickenedAge = Math.min(0, ((EntityAgeable) original).getGrowingAge());
            setSickenedChild(sickenedAge < 0);
        }
    }

    public void copySpeciesDataTo(EntityLivingBase cured) {
        if (growsFromChild() && cured instanceof EntityAgeable) {
            ((EntityAgeable) cured).setGrowingAge(sickenedAge);
        }
    }

    @Nullable
    public ResourceLocation getOriginalType() {
        return originalType != null ? originalType : TaintingManager.getOriginalType(getSickenedType());
    }

    @Nullable
    public NBTTagCompound getOriginalData() {
        return originalData == null ? null : originalData.copy();
    }

    public abstract String getSickenedType();

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        String type = getSickenedType();
        return UPSTREAM_LOOT_TABLES.contains(type)
                ? new ResourceLocation(Tags.MOD_ID, "entities/" + type)
                : null;
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return ModCreatureAttributes.SICKENED;
    }

    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        if (!infectTarget(entityIn)) return false;
        boolean attacked = super.attackEntityAsMob(entityIn);
        if (attacked && getHeldItemMainhand().isEmpty() && entityIn instanceof EntityLivingBase) {
            int difficulty = (int) world.getDifficultyForLocation(new BlockPos(this)).getAdditionalDifficulty();
            ((EntityLivingBase) entityIn).addPotionEffect(new PotionEffect(MobEffects.WITHER, 120 * difficulty));
        }
        return attacked;
    }





    protected boolean infectTarget(Entity entityIn) {
        if (!(entityIn instanceof EntityLiving)) return true;
        if (!TaintingManager.convertEntity((EntityLiving) entityIn, false)) return true;
        int healAmount = getInfectedHealAmount();
        if (healAmount > 0) heal(healAmount);
        return false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.FALL && isSickenedFallImmune()) return false;
        Entity projectile = source.getImmediateSource();
        Entity shooter = null;
        if (projectile instanceof EntityArrow) {
            shooter = ((EntityArrow) projectile).shootingEntity;
        } else if (projectile instanceof EntityFireball) {
            shooter = ((EntityFireball) projectile).shootingEntity;
        }
        if (shooter instanceof SickenedMobEntity) {
            return false;
        }
        return super.attackEntityFrom(source, amount);
    }

    private boolean isSickenedFallImmune() {
        String type = getSickenedType();
        return "sickened_cat".equals(type)
                || "sickened_chicken".equals(type)
                || "sickened_iron_golem".equals(type)
                || "sickened_snow_golem".equals(type);
    }

    @Override
    public void onKillEntity(EntityLivingBase victim) {
        super.onKillEntity(victim);
    }

    protected int getInfectedHealAmount() {
        return 8;
    }

    @Override
    public boolean isPotionApplicable(PotionEffect effect) {
        return effect.getPotion() != ModEffects.WITHER_SICKNESS
                && effect.getPotion() != MobEffects.WITHER
                && super.isPotionApplicable(effect);
    }

    @Override
    public boolean canAttackClass(Class<? extends EntityLivingBase> cls) {
        return !SickenedMobEntity.class.isAssignableFrom(cls)
                && cls != WitherStormEntity.class
                && !SupplementalEntities.StormPartBase.class.isAssignableFrom(cls)
                && super.canAttackClass(cls);
    }

    @Override
    protected boolean canDespawn() {
        return !isConverting();
    }

    @Override
    protected float getSoundPitch() {
        float basePitch;
        if (usesAlternateVoicePitch()) {
            basePitch = isChild() ? 1.15F : 0.65F;
        } else {
            basePitch = isChild() ? 1.35F : 0.85F;
        }
        return (rand.nextFloat() - rand.nextFloat()) * 0.2F + basePitch;
    }

    protected boolean usesAlternateVoicePitch() {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        String type = getSickenedType();
        if ("sickened_bee".equals(type)) return registeredSound(
                "futuremc:bee_passive", SoundEvents.ENTITY_BAT_AMBIENT);
        if ("sickened_cat".equals(type)) return SoundEvents.ENTITY_CAT_AMBIENT;
        if ("sickened_chicken".equals(type)) return SoundEvents.ENTITY_CHICKEN_AMBIENT;
        if ("sickened_cow".equals(type) || "sickened_mushroom_cow".equals(type)) {
            return SoundEvents.ENTITY_COW_AMBIENT;
        }
        if ("sickened_parrot".equals(type)) return SoundEvents.ENTITY_PARROT_AMBIENT;
        if ("sickened_phantom".equals(type)) return SoundEvents.ENTITY_BAT_AMBIENT;
        if ("sickened_pig".equals(type)) return SoundEvents.ENTITY_PIG_AMBIENT;
        if ("sickened_pillager".equals(type) || "sickened_vindicator".equals(type)) {
            return SoundEvents.VINDICATION_ILLAGER_AMBIENT;
        }
        if ("sickened_skeleton".equals(type)) return SoundEvents.ENTITY_SKELETON_AMBIENT;
        if ("sickened_snow_golem".equals(type)) return SoundEvents.ENTITY_SNOWMAN_AMBIENT;
        if ("sickened_spider".equals(type)) return SoundEvents.ENTITY_SPIDER_AMBIENT;
        if ("sickened_villager".equals(type)) return SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT;
        if ("sickened_wolf".equals(type)) {
            SickenedEntities.SickenedWolfEntity wolf =
                    (SickenedEntities.SickenedWolfEntity) this;
            if (wolf.isSickenedAngry()) return SoundEvents.ENTITY_WOLF_GROWL;
            if (rand.nextInt(3) == 0) {
                return wolf.isSickenedTamed() && getHealth() < 10.0F
                        ? SoundEvents.ENTITY_WOLF_WHINE
                        : SoundEvents.ENTITY_WOLF_PANT;
            }
            return SoundEvents.ENTITY_WOLF_AMBIENT;
        }
        if ("sickened_zombie".equals(type)) return SoundEvents.ENTITY_ZOMBIE_AMBIENT;
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        String type = getSickenedType();
        if ("sickened_bee".equals(type)) return registeredSound(
                "futuremc:bee_hurt", SoundEvents.ENTITY_BAT_HURT);
        if ("sickened_cat".equals(type)) return SoundEvents.ENTITY_CAT_HURT;
        if ("sickened_chicken".equals(type)) return SoundEvents.ENTITY_CHICKEN_HURT;
        if ("sickened_cow".equals(type) || "sickened_mushroom_cow".equals(type)) {
            return SoundEvents.ENTITY_COW_HURT;
        }
        if ("sickened_creeper".equals(type)) return SoundEvents.ENTITY_CREEPER_HURT;
        if ("sickened_iron_golem".equals(type)) return SoundEvents.ENTITY_IRONGOLEM_HURT;
        if ("sickened_parrot".equals(type)) return SoundEvents.ENTITY_PARROT_HURT;
        if ("sickened_phantom".equals(type)) return SoundEvents.ENTITY_BAT_HURT;
        if ("sickened_pig".equals(type)) return SoundEvents.ENTITY_PIG_HURT;
        if ("sickened_pillager".equals(type) || "sickened_vindicator".equals(type)) {
            return SoundEvents.ENTITY_VINDICATION_ILLAGER_HURT;
        }
        if ("sickened_skeleton".equals(type)) return SoundEvents.ENTITY_SKELETON_HURT;
        if ("sickened_snow_golem".equals(type)) return SoundEvents.ENTITY_SNOWMAN_HURT;
        if ("sickened_spider".equals(type)) return SoundEvents.ENTITY_SPIDER_HURT;
        if ("sickened_villager".equals(type)) return SoundEvents.ENTITY_ZOMBIE_VILLAGER_HURT;
        if ("sickened_wolf".equals(type)) return SoundEvents.ENTITY_WOLF_HURT;
        if ("sickened_zombie".equals(type)) return SoundEvents.ENTITY_ZOMBIE_HURT;
        return super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        String type = getSickenedType();
        if ("sickened_bee".equals(type)) return registeredSound(
                "futuremc:bee_death", SoundEvents.ENTITY_BAT_DEATH);
        if ("sickened_cat".equals(type)) return SoundEvents.ENTITY_CAT_DEATH;
        if ("sickened_chicken".equals(type)) return SoundEvents.ENTITY_CHICKEN_DEATH;
        if ("sickened_cow".equals(type) || "sickened_mushroom_cow".equals(type)) {
            return SoundEvents.ENTITY_COW_DEATH;
        }
        if ("sickened_creeper".equals(type)) return SoundEvents.ENTITY_CREEPER_DEATH;
        if ("sickened_iron_golem".equals(type)) return SoundEvents.ENTITY_IRONGOLEM_DEATH;
        if ("sickened_parrot".equals(type)) return SoundEvents.ENTITY_PARROT_DEATH;
        if ("sickened_phantom".equals(type)) return SoundEvents.ENTITY_BAT_DEATH;
        if ("sickened_pig".equals(type)) return SoundEvents.ENTITY_PIG_DEATH;
        if ("sickened_pillager".equals(type) || "sickened_vindicator".equals(type)) {
            return SoundEvents.VINDICATION_ILLAGER_DEATH;
        }
        if ("sickened_skeleton".equals(type)) return SoundEvents.ENTITY_SKELETON_DEATH;
        if ("sickened_snow_golem".equals(type)) return SoundEvents.ENTITY_SNOWMAN_DEATH;
        if ("sickened_spider".equals(type)) return SoundEvents.ENTITY_SPIDER_DEATH;
        if ("sickened_villager".equals(type)) return SoundEvents.ENTITY_ZOMBIE_VILLAGER_DEATH;
        if ("sickened_wolf".equals(type)) return SoundEvents.ENTITY_WOLF_DEATH;
        if ("sickened_zombie".equals(type)) return SoundEvents.ENTITY_ZOMBIE_DEATH;
        return super.getDeathSound();
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        SoundEvent step = getSpeciesStepSound();
        if (step != null) {
            playSound(step, 0.15F, 1.0F);
        } else if (!suppressesDefaultStepSound()) {
            super.playStepSound(pos, block);
        }
    }

    @Nullable
    private SoundEvent getSpeciesStepSound() {
        String type = getSickenedType();
        if ("sickened_chicken".equals(type)) return SoundEvents.ENTITY_CHICKEN_STEP;
        if ("sickened_cow".equals(type) || "sickened_mushroom_cow".equals(type)) {
            return SoundEvents.ENTITY_COW_STEP;
        }
        if ("sickened_iron_golem".equals(type)) return SoundEvents.ENTITY_IRONGOLEM_STEP;
        if ("sickened_parrot".equals(type)) return SoundEvents.ENTITY_PARROT_STEP;
        if ("sickened_pig".equals(type)) return SoundEvents.ENTITY_PIG_STEP;
        if ("sickened_skeleton".equals(type)) return SoundEvents.ENTITY_SKELETON_STEP;
        if ("sickened_spider".equals(type)) return SoundEvents.ENTITY_SPIDER_STEP;
        if ("sickened_villager".equals(type)) return SoundEvents.ENTITY_ZOMBIE_VILLAGER_STEP;
        if ("sickened_wolf".equals(type)) return SoundEvents.ENTITY_WOLF_STEP;
        if ("sickened_zombie".equals(type)) return SoundEvents.ENTITY_ZOMBIE_STEP;
        return null;
    }

    private boolean suppressesDefaultStepSound() {
        String type = getSickenedType();
        return "sickened_bee".equals(type) || "sickened_phantom".equals(type);
    }

    private static SoundEvent registeredSound(String id, SoundEvent fallback) {
        SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation(id));
        return sound == null ? fallback : sound;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (originalType != null) compound.setString("OriginalType", originalType.toString());
        if (originalData != null) compound.setTag("OriginalData", originalData);
        compound.setBoolean("SickenedChild", isChild());
        if (growsFromChild()) compound.setInteger("SickenedAge", sickenedAge);
        compound.setInteger("ConversionTime", isConverting() ? conversionTime : -1);
        if (conversionStarter != null) compound.setUniqueId("ConversionPlayer", conversionStarter);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("OriginalType", 8)) originalType = new ResourceLocation(compound.getString("OriginalType"));
        if (compound.hasKey("OriginalData", 10)) originalData = compound.getCompoundTag("OriginalData");
        boolean child = compound.getBoolean("SickenedChild");
        setSickenedChild(child);
        if (growsFromChild()) {
            sickenedAge = compound.hasKey("SickenedAge", 99)
                    ? Math.min(0, compound.getInteger("SickenedAge"))
                    : child ? -24000 : 0;
            setSickenedChild(sickenedAge < 0);
        }
        int savedTime = compound.getInteger("ConversionTime");
        if (savedTime > -1) {
            conversionStarter = compound.hasUniqueId("ConversionPlayer") ? compound.getUniqueId("ConversionPlayer") : null;
            conversionTime = savedTime;
            dataManager.set(CONVERTING, true);
        }
    }

    @Nullable
    @Override
    protected Item getDropItem() {
        return rand.nextBoolean() ? ModItems.get("withered_flesh") : ModItems.get("withered_bone");
    }
}
