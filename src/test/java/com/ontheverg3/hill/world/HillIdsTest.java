package com.ontheverg3.hill.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HillIdsTest {
    @Test
    void saveNameStripsVanillaDimensionSuffixes() {
        assertEquals("world", HillIds.saveName("world"));
        assertEquals("world", HillIds.saveName("world_nether"));
        assertEquals("world", HillIds.saveName("world_the_end"));
        assertEquals("event", HillIds.saveName("event_nether"));
        assertEquals("event", HillIds.saveName("event_the_end"));
        assertEquals("event", HillIds.saveName("event"));
    }

    @Test
    void dimensionTokenMapsVanillaAndCustom() {
        assertEquals("overworld", HillIds.dimensionToken("NORMAL", "minecraft", "overworld"));
        assertEquals("nether", HillIds.dimensionToken("NETHER", "minecraft", "the_nether"));
        assertEquals("the_end", HillIds.dimensionToken("THE_END", "minecraft", "the_end"));
        assertEquals("void", HillIds.dimensionToken("CUSTOM", "minecraft", "void"));
        assertEquals("twilightforest_twilight_forest", HillIds.dimensionToken("CUSTOM", "twilightforest", "twilight_forest"));
    }

    @Test
    void sanitizeAndValidateIds() {
        assertEquals("the_end", HillIds.sanitizeId("The-End"));
        assertTrue(HillIds.isId("overworld"));
        assertTrue(HillIds.isId("the_end"));
        assertFalse(HillIds.isId("hill-overworld"));
        assertFalse(HillIds.isId(""));
    }
}
