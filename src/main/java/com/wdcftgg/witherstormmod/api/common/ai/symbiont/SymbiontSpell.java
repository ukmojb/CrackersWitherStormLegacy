package com.wdcftgg.witherstormmod.api.common.ai.symbiont;

import com.wdcftgg.witherstormmod.common.entity.SickenedEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;




public abstract class SymbiontSpell {

    protected final SickenedEntities.WitheredSymbiontEntity entity;
    protected final SpellType type;
    protected final List<Entity> projectiles = new ArrayList<Entity>();

    public SymbiontSpell(SickenedEntities.WitheredSymbiontEntity entity, SpellType type) {
        this.entity = entity;
        this.type = type;
    }

    public void start(EntityLivingBase target) {
    }

    public abstract void cast(EntityLivingBase target);

    public void finish() {
        for (Entity projectile : projectiles) {
            projectile.setNoGravity(false);
        }
        projectiles.clear();
    }

    public void doCasting(EntityLivingBase target) {
    }

    public abstract int getDelay(Random random, float modifier);
}
