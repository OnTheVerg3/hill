package com.ontheverg3.hill.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HillHelpTest {
    @Test
    void everyTopicHasUsageAndSummary() {
        assertEquals(18, HillHelp.all().size());
        for (HillHelp.Topic topic : HillHelp.all()) {
            assertTrue(topic.usage().startsWith("/hill"), topic.name());
            assertTrue(!topic.summary().isBlank(), topic.name());
            assertTrue(topic.permission().startsWith("hill."), topic.name());
        }
    }

    @Test
    void looksUpCommandsIgnoringCase() {
        assertTrue(HillHelp.byName("ASSIGN").isPresent());
        assertEquals("assign", HillHelp.byName("Assign").orElseThrow().name());
        assertTrue(HillHelp.byName("nope").isEmpty());
    }
}
