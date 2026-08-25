package com.wdcftgg.witherstormmod.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WitherStormHeadYawConstraintTest {

    @Test
    void keepsAllCardinalFrontDirectionsInsideForwardArc() {
        assertFalse(WitherStormHeadYawConstraint.isOutsideForwardArc(0.0D, 10.0D, 0.0F));
        assertFalse(WitherStormHeadYawConstraint.isOutsideForwardArc(10.0D, 0.0D, -90.0F));
        assertFalse(WitherStormHeadYawConstraint.isOutsideForwardArc(-10.0D, 0.0D, 90.0F));
        assertFalse(WitherStormHeadYawConstraint.isOutsideForwardArc(0.0D, -10.0D, 180.0F));
    }

    @Test
    void rejectsAllCardinalBackDirections() {
        assertTrue(WitherStormHeadYawConstraint.isOutsideForwardArc(0.0D, -10.0D, 0.0F));
        assertTrue(WitherStormHeadYawConstraint.isOutsideForwardArc(-10.0D, 0.0D, -90.0F));
        assertTrue(WitherStormHeadYawConstraint.isOutsideForwardArc(10.0D, 0.0D, 90.0F));
        assertTrue(WitherStormHeadYawConstraint.isOutsideForwardArc(0.0D, 10.0D, 180.0F));
    }

    @Test
    void preservesTheEightyDegreeBoundary() {
        double inside = Math.toRadians(80.0D);
        double outside = Math.toRadians(81.0D);

        assertFalse(WitherStormHeadYawConstraint.isOutsideForwardArc(
                Math.sin(inside), Math.cos(inside), 0.0F));
        assertTrue(WitherStormHeadYawConstraint.isOutsideForwardArc(
                Math.sin(outside), Math.cos(outside), 0.0F));
    }
}
