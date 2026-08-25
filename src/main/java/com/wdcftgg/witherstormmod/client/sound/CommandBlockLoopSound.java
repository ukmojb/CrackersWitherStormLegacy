package com.wdcftgg.witherstormmod.client.sound;

import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;

/** 命令方块核心脉冲循环，音量和音高直接跟随同步状态。 */
public final class CommandBlockLoopSound extends MovingSound {
    private final SupplementalEntities.CommandBlockEntity commandBlock;

    public CommandBlockLoopSound(SupplementalEntities.CommandBlockEntity commandBlock, SoundEvent sound) {
        super(sound, SoundCategory.AMBIENT);
        this.commandBlock = commandBlock;
        repeat = true;
        repeatDelay = 0;
        volume = 0.0F;
        pitch = 1.0F;
        updatePosition();
    }

    @Override
    public void update() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (commandBlock.isDead || commandBlock.world != minecraft.world) {
            donePlaying = true;
            return;
        }

        boolean playingDead = commandBlock.getCoreState()
                == SupplementalEntities.CommandBlockEntity.CoreState.PLAYING_DEAD;
        volume = playingDead ? Math.max(0.0F, volume - 0.1F) : Math.min(0.5F, volume + 0.1F);
        float maximumHealth = Math.max(1.0F, commandBlock.getMaxHealth());
        pitch = 1.0F + MathHelper.clamp(
                (maximumHealth - commandBlock.getHealth()) / maximumHealth, 0.0F, 1.0F) * 0.4F;
        updatePosition();
    }

    public void stop() {
        donePlaying = true;
    }

    /** 保证声音首次注册时就位于命令方块，而不是默认的世界原点。 */
    private void updatePosition() {
        xPosF = (float) commandBlock.posX;
        yPosF = (float) commandBlock.posY;
        zPosF = (float) commandBlock.posZ;
    }
}
