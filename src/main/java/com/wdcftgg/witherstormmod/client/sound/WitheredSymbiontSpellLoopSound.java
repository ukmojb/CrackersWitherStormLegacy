package com.wdcftgg.witherstormmod.client.sound;

import com.wdcftgg.witherstormmod.api.common.ai.symbiont.SpellType;
import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;


public class WitheredSymbiontSpellLoopSound extends MovingSound {
    private static final float FADE_TICKS = 20.0F;
    private final SickenedEntities.WitheredSymbiontEntity symbiont;
    private final SpellType spell;
    private float fade;
    private boolean stopping;

    public WitheredSymbiontSpellLoopSound(
            SickenedEntities.WitheredSymbiontEntity symbiont, SoundEvent sound) {
        super(sound, SoundCategory.AMBIENT);
        this.symbiont = symbiont;
        this.spell = symbiont.getSpell();
        repeat = true;
        repeatDelay = 0;
        volume = 0.0F;
        pitch = 1.0F;
    }

    public void stop() {
        if (stopping) return;
        stopping = true;
        fade = Math.max(fade, FADE_TICKS);
    }

    public void stopImmediately() {
        donePlaying = true;
    }

    public boolean matches(SpellType spell) {
        return this.spell == spell;
    }

    @Override
    public void update() {
        if (symbiont.isDead || !symbiont.isEntityAlive()) {
            donePlaying = true;
            return;
        }
        if (!symbiont.isCastingSpell() || symbiont.getSpell() != spell) stopping = true;
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
        volume = Math.max(0.0F, Math.min(fade / FADE_TICKS, 1.0F));
        xPosF = (float) symbiont.posX;
        yPosF = (float) symbiont.posY;
        zPosF = (float) symbiont.posZ;
    }
}
