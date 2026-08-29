package com.wdcftgg.witherstormmod.common.entity;


final class SymbiontHalfHealthGate {

    private SymbiontHalfHealthGate() {
    }

    static float clampDamage(float health, float maxHealth, float amount) {
        return Math.max(0.0F, Math.min(amount, health - maxHealth * 0.5F));
    }

    static boolean reachesThreshold(float health, float maxHealth, float amount) {
        float remaining = health - maxHealth * 0.5F;
        return remaining > 0.0F && amount >= remaining;
    }
}
