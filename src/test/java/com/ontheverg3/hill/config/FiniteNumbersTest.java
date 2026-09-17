package com.ontheverg3.hill.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class FiniteNumbersTest {
    @Test
    void parseRejectsNanAndInfinity() {
        assertEquals(6.5, FiniteNumbers.parse("6.5"), 1e-9);
        assertThrows(NumberFormatException.class, () -> FiniteNumbers.parse("NaN"));
        assertThrows(NumberFormatException.class, () -> FiniteNumbers.parse("Infinity"));
        assertThrows(NumberFormatException.class, () -> FiniteNumbers.parse("-Infinity"));
        assertFalse(FiniteNumbers.looksFinite("NaN"));
        assertFalse(FiniteNumbers.looksFinite("Infinity"));
        assertTrue(FiniteNumbers.looksFinite("3.25"));
    }

    @Test
    void yamlNaNIsRejected() throws Exception {
        assertThrows(ConfigException.class, () -> FiniteNumbers.fromObject(Double.NaN, "scoring.points"));
        assertThrows(
                ConfigException.class, () -> FiniteNumbers.fromObject(Double.POSITIVE_INFINITY, "scoring.points"));
        assertEquals(5.0, FiniteNumbers.fromObject(5, "scoring.interval-seconds"), 1e-9);
    }

    @Test
    void yamlSectionIgnoresNonFiniteInts() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("scores.blue", Double.POSITIVE_INFINITY);
        yaml.set("scores.yellow", Double.NaN);
        assertEquals(0, FiniteNumbers.finiteInt(yaml, "scores.blue", 0));
        assertEquals(0, FiniteNumbers.finiteInt(yaml, "scores.yellow", 0));
        yaml.set("scores.blue", 18);
        assertEquals(18, FiniteNumbers.finiteInt(yaml, "scores.blue", 0));
    }

    @Test
    void gsonLenientJsonCanDecodeNan() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        Holder holder = gson.fromJson("{\"rx\":NaN,\"ry\":Infinity}", Holder.class);
        assertFalse(FiniteNumbers.isFinite(holder.rx));
        assertFalse(FiniteNumbers.isFinite(holder.ry));
    }

    private static final class Holder {
        double rx;
        double ry;
    }
}
