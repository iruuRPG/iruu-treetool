package io.github.iruurpg.treetool;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The config.yml and the sample trees that come in the jar, and the placing tool's list of presets. */
class PresetsAndConfigTest {

    @Test
    void everyPartTheMenuChoosesHasABlockInTheConfigAndEveryBlockOfTheConfigIsOne() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        ConfigurationSection materials = config.getConfigurationSection("treetool.materials");
        assertNotNull(materials);
        for (TreePart part : TreePart.CHOICES) assertTrue(materials.contains(part.key()), "no block in the config for " + part.key());
        for (String key : materials.getKeys(false)) {
            Material material = Material.matchMaterial(materials.getString(key, ""));
            assertTrue(material != null && material.isBlock(), key + ": " + materials.getString(key));
        }
        // the numbers of the config are inside what the code accepts
        assertTrue(config.getInt("treetool.max-blocks") > 0 && config.getInt("treetool.blocks-per-tick") >= 200);
        assertTrue(config.getDouble("treetool.reach") >= 8 && config.getDouble("treetool.reach") <= 256);
    }

    @Test
    void everySampleFileIsListedAndEveryListedSampleIsThere() throws IOException {
        File folder = new File("src/main/resources/trees");
        Set<String> listed = new TreeSet<>(Files.readAllLines(new File(folder, "samples.txt").toPath()).stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        Set<String> files = new TreeSet<>();
        for (File file : folder.listFiles((dir, name) -> name.endsWith(".yml"))) files.add(file.getName().replace(".yml", ""));
        assertEquals(files, listed);
        for (String name : listed) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(folder, name + ".yml"));
            ConfigurationSection section = yaml.getConfigurationSection("settings");
            assertNotNull(section, name);
            TreeSettings settings = TreeSettings.fromMap(new LinkedHashMap<>(section.getValues(false)), name);
            assertTrue(TreeSettings.ALGORITHMS.contains(settings.algorithm()), name);
            assertEquals(section.getString("algorithm"), settings.algorithm(), name);
            assertEquals(name, TreeSettings.safeName(name));
        }
    }

    @Test
    void aSavedTreeComesBackAsItWas() {
        TreeSettings settings = new TreeSettings();
        settings.setAlgorithm("thin");
        settings.set(TreeSettings.Key.THIN_SIDE, 9);
        settings.setSeed(77L);
        settings.setBlock(TreePart.TRUNK, "birch_log");
        settings.setLeafGradient(List.of("azalea_leaves", "oak_leaves"));
        settings.setHangBlocks(List.of("vine"));
        TreeSettings back = TreeSettings.fromMap(settings.toMap(), "x");
        assertEquals("thin", back.algorithm());
        assertEquals(9, back.getInt(TreeSettings.Key.THIN_SIDE));
        assertEquals(77L, back.seed());
        assertEquals("BIRCH_LOG", back.block(TreePart.TRUNK));
        assertEquals(List.of("AZALEA_LEAVES", "OAK_LEAVES"), back.leafGradient());
        assertEquals(List.of("VINE"), back.hangBlocks());
        // a name that is not an algorithm leaves the default one
        TreeSettings odd = TreeSettings.fromMap(Map.of("algorithm", "sequoia"), "y");
        assertEquals(TreeSettings.DEFAULT_ALGORITHM, odd.algorithm());
    }

    @Test
    void thePlacingToolsListIsWrittenReadAndPickedByWeight() {
        Map<String, Integer> chosen = new LinkedHashMap<>();
        chosen.put("thin_birch", 3);
        chosen.put("ornate_fantasy", 1);
        assertEquals(chosen, ProceduralModule.decode(ProceduralModule.encode(chosen)));
        // a broken entry is left out
        assertEquals(Map.of("a", 2), ProceduralModule.decode("a=2;b=x;c;=4;d=0"));
        assertNull(ProceduralModule.pick(Map.of(), new Random(1)));
        int thin = 0;
        Random random = new Random(5);
        for (int index = 0; index < 4000; index++) if ("thin_birch".equals(ProceduralModule.pick(chosen, random))) thin++;
        assertTrue(thin > 2800 && thin < 3200, "thin_birch " + thin + " of 4000");
    }
}
