package io.github.iruurpg.treetool;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Presets that are not trees (a {@code kind:} other than tree, as in the presets folder of another version of the tool) are not read nor listed, and stay in the folder. */
class TreeStoreTest {

    @Test
    void presetsThatAreNotTreesAreLeftAlone(@TempDir Path data) throws Exception {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(data.toFile());
        Path trees = Files.createDirectories(data.resolve("trees"));
        Files.writeString(trees.resolve("rock_boulder.yml"), "settings:\n  kind: rock\n  seed: 3\n");
        Files.writeString(trees.resolve("oak.yml"), "settings:\n  kind: tree\n  seed: 5\n");
        TreeStore store = new TreeStore(plugin);

        assertNull(store.load("rock_boulder"));
        assertNull(store.info("rock_boulder"));
        assertEquals(List.of("oak"), store.list());
        TreeSettings oak = store.load("oak");
        assertNotNull(oak);
        assertEquals(5L, oak.seed());
        assertTrue(new File(trees.toFile(), "rock_boulder.yml").isFile());
        // (it can still be put in the trash by name)
        assertTrue(store.delete("rock_boulder"));
    }
}
