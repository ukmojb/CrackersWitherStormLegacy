package com.wdcftgg.witherstormmod.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.UUID;


final class CommandBlockTentacleManager {
    private static final int TENTACLE_COUNT = 6;
    private static final int SAVED_ENTITY_RESOLVE_TIMEOUT = 200;
    private static final TentacleOffset[] OFFSETS = {
            new TentacleOffset(2.0D, -2.0D, 3.0D, 1.45F, 1.0F, 40.0F, -70.0F),
            new TentacleOffset(0.0D, -2.0D, 4.0D, 1.4F, 1.0F, 35.0F, -90.0F),
            new TentacleOffset(-2.0D, -2.0D, 3.0D, 1.45F, 1.0F, 40.0F, -110.0F),
            new TentacleOffset(2.0D, -2.0D, -3.0D, 1.45F, 1.0F, 40.0F, 70.0F),
            new TentacleOffset(0.0D, -2.0D, -4.0D, 1.4F, 1.0F, 35.0F, 90.0F),
            new TentacleOffset(-2.0D, -2.0D, -3.0D, 1.45F, 1.0F, 40.0F, 110.0F)
    };

    private final SupplementalEntities.CommandBlockEntity commandBlock;
    private final SickenedEntities.TentacleEntity[] tentacles =
            new SickenedEntities.TentacleEntity[TENTACLE_COUNT];
    private final UUID[] savedTentacleUuids = new UUID[TENTACLE_COUNT];
    private final int[] unresolvedTicks = new int[TENTACLE_COUNT];
    private boolean protectCommandBlock;

    CommandBlockTentacleManager(SupplementalEntities.CommandBlockEntity commandBlock) {
        this.commandBlock = commandBlock;
    }

    void tick(SupplementalEntities.CommandBlockEntity.CoreMode mode) {
        if (commandBlock.world.isRemote || !(commandBlock.world instanceof WorldServer)) return;
        resolveSavedTentacles();
        recoverLoadedTentacles();
        if (mode != SupplementalEntities.CommandBlockEntity.CoreMode.TENTACLES) return;
        for (int index = 0; index < tentacles.length; index++) {
            SickenedEntities.TentacleEntity tentacle = tentacles[index];
            if ((tentacle == null || tentacle.isDead) && savedTentacleUuids[index] == null) {
                tentacle = createTentacle(index);
            }
            if (tentacle != null && !tentacle.isDead) {
                OFFSETS[index].apply(commandBlock, tentacle);
                if (protectCommandBlock && !tentacle.isCurling()) {
                    tentacle.curlAround(commandBlock.getPositionVector());
                }
            }
        }
    }

    void removeTentacles() {
        if (commandBlock.world.isRemote) return;
        resolveSavedTentacles();
        recoverLoadedTentacles();
        for (int index = 0; index < tentacles.length; index++) {
            SickenedEntities.TentacleEntity tentacle = tentacles[index];
            if (tentacle != null && !tentacle.isDead) tentacle.setDead();
            tentacles[index] = null;
            savedTentacleUuids[index] = null;
            unresolvedTicks[index] = 0;
        }
    }

    void discardOwnedTentaclesWithoutResolvingSavedReferences() {
        if (commandBlock.world.isRemote) return;
        for (SickenedEntities.TentacleEntity tentacle : commandBlock.world.getEntities(
                SickenedEntities.TentacleEntity.class,
                entity -> entity.isCommandBlockStructureOf(commandBlock))) {
            if (!tentacle.isDead) tentacle.setDead();
        }
        for (int index = 0; index < tentacles.length; index++) {
            tentacles[index] = null;
            savedTentacleUuids[index] = null;
            unresolvedTicks[index] = 0;
        }
    }

    void awakenTentacles(boolean indefinite) {
        if (commandBlock.world.isRemote || !(commandBlock.world instanceof WorldServer)) return;
        resolveSavedTentacles();
        recoverLoadedTentacles();
        for (SickenedEntities.TentacleEntity tentacle : tentacles) {
            if (tentacle == null || tentacle.isDead) continue;
            tentacle.setDormant(false);
            if (indefinite) tentacle.doIndefiniteAwakeAnimation();
            else tentacle.doAwakeAnimation();
        }
    }

    void curlTentacles(boolean skipSwinging) {
        if (commandBlock.world.isRemote || !(commandBlock.world instanceof WorldServer)) return;
        protectCommandBlock = true;
        resolveSavedTentacles();
        recoverLoadedTentacles();
        for (SickenedEntities.TentacleEntity tentacle : tentacles) {
            if (tentacle == null || tentacle.isDead
                    || skipSwinging && tentacle.isDoingSwingAttack()) continue;
            tentacle.curlAround(commandBlock.getPositionVector());
        }
    }

    void stopCurlingTentacles() {
        if (commandBlock.world.isRemote || !(commandBlock.world instanceof WorldServer)) return;
        protectCommandBlock = false;
        resolveSavedTentacles();
        recoverLoadedTentacles();
        for (SickenedEntities.TentacleEntity tentacle : tentacles) {
            if (tentacle != null && !tentacle.isDead) tentacle.stopCurlingAround();
        }
    }

    private SickenedEntities.TentacleEntity createTentacle(int index) {
        SickenedEntities.TentacleEntity tentacle = new SickenedEntities.TentacleEntity(commandBlock.world);
        float bodyYaw = commandBlock.renderYawOffset;
        tentacle.bindToCommandBlock(commandBlock, index);
        tentacle.setDormant(true);
        tentacle.setCanStrangle(false);
        tentacle.setNoGravity(true);
        tentacle.setEntityInvulnerable(true);
        tentacle.configureStructurePose(OFFSETS[index].baseXRotation,
                OFFSETS[index].baseYRotation - bodyYaw - 90.0F,
                OFFSETS[index].xCurl, OFFSETS[index].yCurl,
                commandBlock.getRNG().nextInt(35) * 10000);
        tentacle.lerpCurlTo(0.0F, 0.0F, 1);
        OFFSETS[index].apply(commandBlock, tentacle);
        if (!commandBlock.world.spawnEntity(tentacle)) return null;
        tentacles[index] = tentacle;
        return tentacle;
    }

    private void resolveSavedTentacles() {
        WorldServer world = (WorldServer) commandBlock.world;
        for (int index = 0; index < savedTentacleUuids.length; index++) {
            UUID savedUuid = savedTentacleUuids[index];
            if (savedUuid == null) continue;
            SickenedEntities.TentacleEntity current = tentacles[index];
            if (current != null && !current.isDead && savedUuid.equals(current.getUniqueID())) {
                savedTentacleUuids[index] = null;
                unresolvedTicks[index] = 0;
                continue;
            }
            Entity resolved = world.getEntityFromUuid(savedUuid);
            if (resolved instanceof SickenedEntities.TentacleEntity && !resolved.isDead) {
                if (current != null && !current.isDead && current != resolved) current.setDead();
                SickenedEntities.TentacleEntity tentacle = (SickenedEntities.TentacleEntity) resolved;
                tentacle.bindToCommandBlock(commandBlock, index);
                tentacles[index] = tentacle;
                savedTentacleUuids[index] = null;
                unresolvedTicks[index] = 0;
            } else if (++unresolvedTicks[index] > SAVED_ENTITY_RESOLVE_TIMEOUT) {
                savedTentacleUuids[index] = null;
                unresolvedTicks[index] = 0;
            }
        }
    }

    private void recoverLoadedTentacles() {
        List<SickenedEntities.TentacleEntity> loaded = commandBlock.world.getEntities(
                SickenedEntities.TentacleEntity.class,
                tentacle -> tentacle.isCommandBlockStructureOf(commandBlock));
        for (SickenedEntities.TentacleEntity tentacle : loaded) {
            int index = tentacle.getCommandBlockStructureIndex();
            if (index < 0 || index >= tentacles.length) {
                tentacle.setDead();
                continue;
            }
            SickenedEntities.TentacleEntity current = tentacles[index];
            if (current == null || current.isDead) {
                tentacles[index] = tentacle;
            } else if (current != tentacle) {
                tentacle.setDead();
            }
        }
    }

    void writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (int index = 0; index < tentacles.length; index++) {
            NBTTagCompound entry = new NBTTagCompound();
            SickenedEntities.TentacleEntity tentacle = tentacles[index];
            UUID uuid = tentacle != null && !tentacle.isDead
                    ? tentacle.getUniqueID() : savedTentacleUuids[index];
            if (uuid != null) entry.setUniqueId("UUID", uuid);
            list.appendTag(entry);
        }
        compound.setTag("Tentacles", list);
    }

    void readFromNBT(NBTTagCompound compound) {
        for (int index = 0; index < tentacles.length; index++) {
            tentacles[index] = null;
            savedTentacleUuids[index] = null;
            unresolvedTicks[index] = 0;
        }
        NBTTagList list = compound.getTagList("Tentacles", 10);
        for (int index = 0; index < tentacles.length && index < list.tagCount(); index++) {
            NBTTagCompound entry = list.getCompoundTagAt(index);
            if (entry.hasUniqueId("UUID")) savedTentacleUuids[index] = entry.getUniqueId("UUID");
        }
    }

    private static final class TentacleOffset {
        private final double x;
        private final double y;
        private final double z;
        private final float xCurl;
        private final float yCurl;
        private final float baseXRotation;
        private final float baseYRotation;

        private TentacleOffset(double x, double y, double z, float xCurl, float yCurl,
                               float baseXRotation, float baseYRotation) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.xCurl = xCurl;
            this.yCurl = yCurl;
            this.baseXRotation = baseXRotation;
            this.baseYRotation = baseYRotation;
        }

        private void apply(SupplementalEntities.CommandBlockEntity core,
                           SickenedEntities.TentacleEntity tentacle) {
            float bodyYaw = core.renderYawOffset;
            float offsetAngle = (float) MathHelper.atan2(x, z);
            double distance = Math.sqrt(x * x + z * z);
            float bodyAngle = -bodyYaw * 0.017453292F;
            double offsetX = MathHelper.cos(bodyAngle + offsetAngle) * distance;
            double offsetZ = MathHelper.sin(bodyAngle + offsetAngle) * distance;
            tentacle.setPosition(core.posX + offsetX, core.posY + y, core.posZ + offsetZ);
            tentacle.configureStructurePose(baseXRotation,
                    baseYRotation - bodyYaw - 90.0F, xCurl, yCurl,
                    tentacle.getStructureAnimationOffset());
        }
    }
}
