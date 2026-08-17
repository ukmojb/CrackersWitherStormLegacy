package com.wdcftgg.witherstormmod.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbiontHalfHealthGateTest {

    @Test
    void capsCrossingDamageAtExactlyHalfHealth() {
        assertEquals(1.0F, SymbiontHalfHealthGate.clampDamage(31.0F, 60.0F, 12.0F));
        assertTrue(SymbiontHalfHealthGate.reachesThreshold(31.0F, 60.0F, 12.0F));
    }

    @Test
    void preservesDamageThatDoesNotReachTheThreshold() {
        assertEquals(4.0F, SymbiontHalfHealthGate.clampDamage(50.0F, 60.0F, 4.0F));
        assertFalse(SymbiontHalfHealthGate.reachesThreshold(50.0F, 60.0F, 4.0F));
    }

    @Test
    void neverProducesNegativeDamageAtOrBelowHalfHealth() {
        assertEquals(0.0F, SymbiontHalfHealthGate.clampDamage(30.0F, 60.0F, 8.0F));
        assertEquals(0.0F, SymbiontHalfHealthGate.clampDamage(29.0F, 60.0F, 8.0F));
    }
}
