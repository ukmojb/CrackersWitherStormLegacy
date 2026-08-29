package com.wdcftgg.witherstormmod.client.sound;

import com.wdcftgg.witherstormmod.common.item.FormidiBladeItem;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;


public class FormidiBladeChargeSound extends MovingSound {
    private static final float FADE_TICKS = 20.0F;
    private final EntityPlayer player;
    private float fade;
    private boolean stopping;

    public FormidiBladeChargeSound(EntityPlayer player, SoundEvent sound) {
        super(sound, SoundCategory.AMBIENT);
        this.player = player;
        repeat = true;
        repeatDelay = 0;
        volume = 0.0F;
        pitch = 0.5F;
    }

    public void stop() {
        if (stopping) return;
        stopping = true;
        fade = Math.max(fade, FADE_TICKS);
    }

    public void stopImmediately() {
        donePlaying = true;
    }

    @Override
    public void update() {
        if (player.isDead || !player.isEntityAlive()) {
            donePlaying = true;
            return;
        }
        float power = getPower(player);
        if (power <= 0.05F) stopping = true;
        if (stopping) {
            fade -= 1.0F;
            if (fade <= 0.0F) {
                volume = 0.0F;
                donePlaying = true;
                return;
            }
        } else if (fade < FADE_TICKS) {
            fade += 1.0F;
        }
        xPosF = (float) player.posX;
        yPosF = (float) (player.posY + player.getEyeHeight());
        zPosF = (float) player.posZ;
        volume = Math.max(0.0F, Math.min(fade / FADE_TICKS, 1.0F) - (1.0F - power));
        pitch = 0.5F + power * 0.5F;
    }

    public static float getPower(EntityPlayer player) {
        for (EnumHand hand : EnumHand.values()) {
            ItemStack stack = player.getHeldItem(hand);
            if (stack.getItem() instanceof FormidiBladeItem) {
                return FormidiBladeItem.getPower(player, stack, false);
            }
        }
        return 0.0F;
    }
}
