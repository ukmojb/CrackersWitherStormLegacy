package com.wdcftgg.witherstormmod.common.entity;

/** 处理 1.12 护甲结算发生在半血伤害裁剪之后的差异。 */
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
