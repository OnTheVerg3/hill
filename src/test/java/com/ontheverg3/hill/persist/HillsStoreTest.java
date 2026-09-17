package com.ontheverg3.hill.persist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ontheverg3.hill.game.HillRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HillsStoreTest {
    private static final String MID =
            """
            {
              "mode": "koth",
              "hills": [
                {
                  "id": "mid",
                  "display": "Mid",
                  "world": "world",
                  "save": "world",
                  "dimension": "overworld",
                  "shape": "circle",
                  "x": 0.0,
                  "y": 64.0,
                  "z": 0.0,
                  "rx": 8.0,
                  "ry": 16.0,
                  "rz": 8.0
                }
              ],
              "pads": []
            }
            """;
    private static final String OTHER =
            """
            {
              "mode": "ctf",
              "hills": [
                {
                  "id": "other",
                  "display": "Other",
                  "world": "world",
                  "save": "world",
                  "dimension": "overworld",
                  "shape": "square",
                  "x": 32.0,
                  "y": 64.0,
                  "z": 0.0,
                  "rx": 4.0,
                  "ry": 8.0,
                  "rz": 4.0
                }
              ],
              "pads": []
            }
            """;

    @TempDir
    Path temp;

    @Test
    void parseTreatsNullListsAsEmpty() {
        HillsStore.ParsedFile parsed = HillsStore.parse("{\"mode\":\"koth\",\"hills\":null,\"pads\":null}");
        assertTrue(parsed.ok);
        assertNotNull(parsed.model.hills);
        assertNotNull(parsed.model.pads);
        assertTrue(parsed.model.hills.isEmpty());
        assertTrue(parsed.model.pads.isEmpty());
    }

    @Test
    void parseAcceptsMissingLists() {
        HillsStore.ParsedFile parsed = HillsStore.parse("{}");
        assertTrue(parsed.ok);
        assertNotNull(parsed.model.hills);
        assertNotNull(parsed.model.pads);
        assertTrue(parsed.model.hills.isEmpty());
    }

    @Test
    void parseRejectsMalformedJson() {
        HillsStore.ParsedFile parsed = HillsStore.parse("{not json");
        assertFalse(parsed.ok);
        assertNull(parsed.model);
    }

    @Test
    void parseRejectsBlank() {
        HillsStore.ParsedFile parsed = HillsStore.parse("   \n");
        assertFalse(parsed.ok);
        assertNull(parsed.model);
    }

    @Test
    void parseRejectsJsonNull() {
        HillsStore.ParsedFile parsed = HillsStore.parse("null");
        assertFalse(parsed.ok);
    }

    @Test
    void missingFileLoadsEmptyAndStaysPersistable() {
        HillsStore store = store();
        HillRegistry registry = new HillRegistry();
        assertTrue(store.load(registry));
        assertTrue(store.persistable());
        assertTrue(registry.all().isEmpty());
        assertFalse(Files.exists(file()));
    }

    @Test
    void blankExistingFileFailsAndIsLeftUnchanged() throws Exception {
        Files.writeString(file(), "   \n", StandardCharsets.UTF_8);
        HillsStore store = store();
        HillRegistry registry = new HillRegistry();
        assertFalse(store.load(registry));
        assertFalse(store.persistable());
        assertEquals("   \n", Files.readString(file(), StandardCharsets.UTF_8));
        store.save(registry);
        assertEquals("   \n", Files.readString(file(), StandardCharsets.UTF_8));
    }

    @Test
    void brokenJsonDoesNotClearRegistryOrOverwriteTheFile() throws Exception {
        Files.writeString(file(), MID, StandardCharsets.UTF_8);
        HillsStore store = store();
        HillRegistry registry = new HillRegistry();
        assertTrue(store.load(registry));
        assertNotNull(registry.byId("mid"));

        String broken = "{not json";
        Files.writeString(file(), broken, StandardCharsets.UTF_8);
        assertFalse(store.reload(registry));
        assertFalse(store.persistable());
        assertNotNull(registry.byId("mid"));
        assertEquals(broken, Files.readString(file(), StandardCharsets.UTF_8));

        store.save(registry);
        assertEquals(broken, Files.readString(file(), StandardCharsets.UTF_8));
    }

    @Test
    void reloadReadsDiskThenWritesNormalizedJson() throws Exception {
        Files.writeString(file(), MID, StandardCharsets.UTF_8);
        HillsStore store = store();
        HillRegistry registry = new HillRegistry();
        assertTrue(store.load(registry));
        assertNotNull(registry.byId("mid"));

        Files.writeString(file(), OTHER, StandardCharsets.UTF_8);
        assertTrue(store.reload(registry));
        assertTrue(store.persistable());
        assertNull(registry.byId("mid"));
        assertNotNull(registry.byId("other"));
        String written = Files.readString(file(), StandardCharsets.UTF_8);
        assertTrue(written.contains("\"other\""));
        assertFalse(written.contains("\"mid\""));
        assertTrue(written.contains("\"ctf\""));
    }

    @Test
    void nullHillsArrayDoesNotThrowAndDoesNotWipeALaterSaveGuard() throws Exception {
        Files.writeString(file(), "{\"mode\":\"koth\",\"hills\":null}", StandardCharsets.UTF_8);
        HillsStore store = store();
        HillRegistry registry = new HillRegistry();
        assertTrue(store.load(registry));
        assertTrue(store.persistable());
        assertTrue(registry.all().isEmpty());
    }

    private HillsStore store() {
        return new HillsStore(Logger.getLogger("HillsStoreTest"), file().toFile());
    }

    private Path file() {
        return temp.resolve("hills.json");
    }
}
