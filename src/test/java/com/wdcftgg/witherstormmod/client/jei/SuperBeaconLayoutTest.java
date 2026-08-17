package com.wdcftgg.witherstormmod.client.jei;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperBeaconLayoutTest {

    @Test
    void laysOutEveryIngredientAroundTheFullRing() {
        for (int ingredientCount : new int[] {3, 10, 16}) {
            int centerY = SuperBeaconLayout.centerY("none");
            Set<String> positions = new HashSet<String>();
            for (int index = 0; index < ingredientCount; index++) {
                positions.add(SuperBeaconLayout.inputX(index, ingredientCount) + ":"
                        + SuperBeaconLayout.inputY(index, ingredientCount, centerY));
            }
            assertEquals(ingredientCount, positions.size());
        }
    }

    @Test
    void conditionMakesRoomForDescriptionWithoutChangingCanvasSize() {
        assertEquals(60, SuperBeaconLayout.centerY("none"));
        assertEquals(55, SuperBeaconLayout.centerY("all_supports"));
        assertEquals(180, SuperBeaconLayout.WIDTH);
        assertEquals(120, SuperBeaconLayout.HEIGHT);
    }

    @Test
    void identifiesTheOnlyUpstreamHiddenSummoningRecipe() {
        assertFalse(SuperBeaconLayout.shouldShowSummoningEntity("minecraft:pig"));
        assertTrue(SuperBeaconLayout.shouldShowSummoningEntity("witherstormmod:wither_storm"));
        assertTrue(SuperBeaconLayout.shouldShowSummoningEntity("witherstormmod:withered_symbiont"));
    }
}
