package com.ontheverg3.hill.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuotedTest {
    @Test
    void splitsQuotedDisplayNames() {
        List<String> tokens = Quoted.split("new ~ ~ ~ 10 circle mid \"Castle Hill\"");
        assertEquals(List.of("new", "~", "~", "~", "10", "circle", "mid", "Castle Hill"), tokens);
    }

    @Test
    void keepsUnquotedTokens() {
        List<String> tokens = Quoted.split("assign @a[gamemode=survival] blue");
        assertEquals(List.of("assign", "@a[gamemode=survival]", "blue"), tokens);
    }
}
