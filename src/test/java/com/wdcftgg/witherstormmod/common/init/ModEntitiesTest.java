package com.wdcftgg.witherstormmod.common.init;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModEntitiesTest {

    @Test
    void witheredSymbiontUsesUpstreamDefaultUpdateInterval() {
        assertEquals(3, ModEntities.WITHERED_SYMBIONT_UPDATE_FREQUENCY);
    }
}
