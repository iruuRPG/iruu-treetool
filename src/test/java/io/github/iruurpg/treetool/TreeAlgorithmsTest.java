package io.github.iruurpg.treetool;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeAlgorithmsTest {

    private static TreeSettings tree(String algorithm) {
        TreeSettings settings = new TreeSettings();
        settings.setAlgorithm(algorithm);
        assertEquals(algorithm, settings.algorithm());
        return settings;
    }

    private static Map<TreePart, Integer> count(Map<Long, TreePart> blocks) {
        Map<TreePart, Integer> counts = new EnumMap<>(TreePart.class);
        for (TreePart part : blocks.values()) counts.merge(part, 1, Integer::sum);
        return counts;
    }

    @Test
    void everyAlgorithmGrowsTheSameTreeFromTheSameSeed() {
        for (String algorithm : TreeSettings.ALGORITHMS) {
            TreeSettings settings = tree(algorithm);
            Map<Long, TreePart> a = TreeGenerator.build(settings, 9L, null);
            assertEquals(a, TreeGenerator.build(settings, 9L, null), algorithm);
            Map<TreePart, Integer> parts = count(a);
            // (a thin tree's trunk is one block thick; and short, it soon splits)
            // (the silhouette trees: a small round tree of 11 by default, its logs partly lying)
            assertTrue(parts.getOrDefault(TreePart.TRUNK, 0) > (algorithm.equals("thin") ? 3 : algorithm.equals("silhouette") ? 8 : 20), algorithm + " " + parts);
            assertTrue(parts.getOrDefault(TreePart.LEAF, 0) > 50, algorithm + " " + parts);
        }
    }

    @Test
    void theLeafGradientRunsUpEachClump() {
        TreeSettings settings = tree("thin");
        settings.set(TreeSettings.Key.THIN_LEAF_SIZE, 3.0);
        settings.setLeafGradient(List.of("azalea_leaves", "oak_leaves", "flowering_azalea_leaves"));
        Map<TreePart, Integer> parts = count(TreeGenerator.build(settings, 2L, null));
        assertTrue(parts.containsKey(TreePart.LEAF) && parts.containsKey(TreePart.LEAF_2) && parts.containsKey(TreePart.LEAF_3), parts.toString());
        assertFalse(parts.containsKey(TreePart.LEAF_4));
    }

    @Test
    void everyPageOfEveryAlgorithmFitsTheMenu() {
        for (String algorithm : TreeSettings.ALGORITHMS) {
            TreeSettings settings = tree(algorithm);
            for (ProceduralMenu.Page page : ProceduralMenu.Page.values()) {
                if (!page.shownFor(settings)) continue;
                int count = page.keysFor(settings).length;
                assertTrue(count <= ProceduralMenu.room(page), algorithm + " " + page);
                // no two numbers in one slot, and none on a slot of the leaf gradient (28-34) or of the hanging strands (37-43) of the leaf page
                java.util.Set<Integer> slots = new java.util.HashSet<>();
                for (int index = 0; index < count; index++) {
                    int slot = ProceduralMenu.slot(page, index);
                    assertTrue(slots.add(slot), algorithm + " " + page + " slot " + slot);
                    if (page == ProceduralMenu.Page.LEAF) assertTrue(slot < 28 || slot > 43, algorithm + " " + page + " slot " + slot);
                }
            }
            // the parts shown on the basic page fit its slots, and each part has a block
            assertTrue(ProceduralMenu.legendParts(settings).size() <= 14, algorithm);
            for (TreePart part : ProceduralMenu.legendParts(settings)) assertNotNull(part.defaultMaterial());
        }
    }

    @Test
    void blocksCanCarryStates() {
        assertEquals("minecraft:oak_log[axis=x]", TreeSettings.blockName("oak_log[axis=x]"));
        assertEquals("SPRUCE_LEAVES", TreeSettings.blockName("minecraft:spruce_leaves"));
        assertNull(TreeSettings.blockName("oak_log[axis=x"));
        assertNull(TreeSettings.blockName("air"));
        assertEquals(Material.OAK_LOG, TreeSettings.materialOf("minecraft:oak_log[axis=x]"));
        TreeSettings settings = tree("thin");
        settings.setBlock(TreePart.BRANCH, "birch_log[axis=z]");
        assertEquals("minecraft:birch_log[axis=z]", TreeSettings.fromMap(settings.toMap(), "x").block(TreePart.BRANCH));
        assertTrue(TreeSettings.Key.THIN_VINES.toggle());
        assertFalse(TreeSettings.Key.THIN_DEPTH.toggle());
    }

    @Test
    void theSampleTreesLoadAndFitThePlacingLimits() throws IOException {
        File folder = new File("src/main/resources/trees");
        List<String> names = Files.readAllLines(new File(folder, "samples.txt").toPath()).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        assertTrue(names.size() >= 9, names.toString());
        for (String name : names) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(folder, name + ".yml"));
            assertNotNull(yaml.getConfigurationSection("settings"), name);
            TreeSettings settings = TreeSettings.fromMap(new LinkedHashMap<>(yaml.getConfigurationSection("settings").getValues(false)), name);
            assertEquals(yaml.getString("settings.algorithm"), settings.algorithm(), name);
            for (TreePart part : TreePart.values()) {
                String block = settings.block(part);
                if (block != null) assertNotNull(TreeSettings.materialOf(block), name + " " + block);
            }
            assertEquals(yaml.getStringList("settings.leaf-gradient").size(), settings.leafGradient().size(), name);
            for (long seed = 1; seed <= 3; seed++) {
                int size = TreeGenerator.build(settings, seed, null).size();
                // (the placing limit of the server is 60000 blocks, the preview's 40000)
                assertTrue(size > 150 && size < 40000, name + " seed " + seed + ": " + size);
            }
        }
    }

    @Test
    void aThinTreeHasAThinTrunkAndBranchesOfWallsFencesAndSlabs() {
        // one block thick, its branches in posts and halves, leaves over the tips
        for (long seed = 1; seed <= 5; seed++) {
            Map<Long, TreePart> blocks = TreeGenerator.build(tree("thin"), seed, null);
            Map<TreePart, Integer> counts = count(blocks);
            String what = "seed " + seed + " " + counts;
            for (TreePart part : new TreePart[]{TreePart.TRUNK, TreePart.BRANCH, TreePart.BRANCH_WALL, TreePart.BRANCH_FENCE, TreePart.BRANCH_SLAB, TreePart.BRANCH_SLAB_TOP}) {
                assertTrue(counts.getOrDefault(part, 0) > 0, part + " " + what);
            }
            assertTrue(blocks.values().stream().filter(part -> part.shade() >= 0).count() > 100, what);
            // the trunk low down: one block a layer (two where it steps over)
            for (int y = 0; y <= 3; y++) {
                int layer = y;
                long trunk = blocks.entrySet().stream().filter(entry -> entry.getValue() == TreePart.TRUNK && TreeVoxelizer.unpackY(entry.getKey()) == layer).count();
                assertTrue(trunk >= 1 && trunk <= 2, "y " + y + " " + trunk + " " + what);
            }
        }
    }

    @Test
    void theLeavesOfAThinTreeKeepARoomRoundTheWood() {
        // the more room asked for, the fewer leaves touch the branches (those on the tips stay, the clumps hang on them)
        long[] touching = new long[2];
        for (int run = 0; run < 2; run++) {
            TreeSettings settings = tree("thin");
            settings.set(TreeSettings.Key.THIN_CLEARANCE, run == 0 ? 0.0 : 2.0);
            Map<Long, TreePart> blocks = TreeGenerator.build(settings, 3L, null);
            for (Map.Entry<Long, TreePart> entry : blocks.entrySet()) {
                if (entry.getValue().shade() < 0) continue;
                int x = TreeVoxelizer.unpackX(entry.getKey());
                int y = TreeVoxelizer.unpackY(entry.getKey());
                int z = TreeVoxelizer.unpackZ(entry.getKey());
                for (int[] step : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                    TreePart next = blocks.get(TreeVoxelizer.pack(x + step[0], y + step[1], z + step[2]));
                    if (next != null && next.shade() < 0) {
                        touching[run]++;
                        break;
                    }
                }
            }
        }
        assertTrue(touching[1] * 5 < touching[0] * 4, touching[0] + " / " + touching[1]);
    }

    @Test
    void fencesAndWallsOfAThinTreeJoinTheWoodNextToThem() {
        // a post between two logs east and west, a fence to the north, a leaf to the south
        Map<Long, TreePart> blocks = new java.util.HashMap<>();
        blocks.put(TreeVoxelizer.pack(1, 0, 0), TreePart.TRUNK);
        blocks.put(TreeVoxelizer.pack(-1, 0, 0), TreePart.BRANCH);
        blocks.put(TreeVoxelizer.pack(0, 0, 1), TreePart.LEAF);
        boolean[] joined = ThinTree.joined(blocks, 0, 0, 0);
        assertArrayEquals(new boolean[]{false, true, false, true}, joined);
        // straight through with nothing over it: no post; with wood over it: a post
        assertFalse(ThinTree.post(blocks, joined, 0, 0, 0));
        blocks.put(TreeVoxelizer.pack(0, 1, 0), TreePart.BRANCH_FENCE);
        assertTrue(ThinTree.post(blocks, joined, 0, 0, 0));
        blocks.put(TreeVoxelizer.pack(0, 0, -1), TreePart.BRANCH_FENCE);
        assertTrue(ThinTree.joined(blocks, 0, 0, 0)[0]);
    }

    @Test
    void aThinTreeSplitsSoonThinsOutAndIsDressedWithStairs() {
        // over the height the side branches start, more than one block of wood a layer (the side branches); stairs at the foot (the foot is heavy)
        TreeSettings settings = tree("thin");
        int split = settings.getInt(TreeSettings.Key.THIN_HEIGHT);
        int forked = 0;
        int footed = 0;
        for (long seed = 1; seed <= 8; seed++) {
            Map<Long, TreePart> blocks = TreeGenerator.build(settings, seed, null);
            long over = blocks.entrySet().stream().filter(entry -> entry.getValue().shade() < 0 && TreeVoxelizer.unpackY(entry.getKey()) == split + 2).count();
            if (over >= 2) forked++;
            if (blocks.entrySet().stream().anyMatch(entry -> entry.getValue().blockOf() == TreePart.BRANCH_STAIR && TreeVoxelizer.unpackY(entry.getKey()) == 0)) footed++;
            // the further out, the thinner: no log far up the crown, fences only beyond the walls
            double logs = blocks.entrySet().stream().filter(entry -> entry.getValue() == TreePart.TRUNK || entry.getValue() == TreePart.BRANCH)
                    .mapToInt(entry -> TreeVoxelizer.unpackY(entry.getKey())).average().orElse(0.0);
            double fences = blocks.entrySet().stream().filter(entry -> entry.getValue() == TreePart.BRANCH_FENCE)
                    .mapToInt(entry -> TreeVoxelizer.unpackY(entry.getKey())).average().orElse(0.0);
            assertTrue(logs < fences, "seed " + seed + " logs " + logs + " fences " + fences);
        }
        assertTrue(forked >= 6, Integer.toString(forked));
        assertTrue(footed >= 6, Integer.toString(footed));
        // no foot asked for: no stair at the foot
        settings.set(TreeSettings.Key.THIN_FOOT, 0);
        settings.set(TreeSettings.Key.THIN_STAIRS, 0);
        assertTrue(TreeGenerator.build(settings, 1L, null).values().stream().noneMatch(part -> part.blockOf() == TreePart.BRANCH_STAIR));
    }

    @Test
    void aThinTreeRunsItsTrunkUpAndTurnsItsStemsBackUp() {
        // one stem: wood up near the top of the crown, nothing but the trunk and its stairs under the height the side branches start
        TreeSettings settings = tree("thin");
        int clear = settings.getInt(TreeSettings.Key.THIN_HEIGHT);
        double crown = settings.get(TreeSettings.Key.THIN_LENGTH);
        for (long seed = 1; seed <= 5; seed++) {
            Map<Long, TreePart> blocks = TreeGenerator.build(settings, seed, null);
            int top = blocks.entrySet().stream().filter(entry -> entry.getValue().shade() < 0).mapToInt(entry -> TreeVoxelizer.unpackY(entry.getKey())).max().orElse(0);
            assertTrue(top >= clear + crown * 0.8, "seed " + seed + " top " + top);
            for (Map.Entry<Long, TreePart> entry : blocks.entrySet()) {
                if (entry.getValue().shade() >= 0 || TreeVoxelizer.unpackY(entry.getKey()) >= clear - 1) continue;
                assertTrue(entry.getValue() == TreePart.TRUNK || entry.getValue().blockOf() == TreePart.BRANCH_STAIR, "seed " + seed + " " + entry.getValue());
            }
        }
        // stems that turn back up stand closer together than stems that keep opening
        double[] spread = new double[2];
        for (int run = 0; run < 2; run++) {
            TreeSettings stems = tree("thin");
            stems.set(TreeSettings.Key.THIN_STEMS, 3);
            stems.set(TreeSettings.Key.THIN_LEAN, 0.8);
            stems.set(TreeSettings.Key.THIN_SIDE, 0);
            stems.set(TreeSettings.Key.THIN_UPTURN, run == 0 ? 0.0 : 1.0);
            for (long seed = 1; seed <= 4; seed++) {
                spread[run] += TreeGenerator.build(stems, seed, null).entrySet().stream().filter(entry -> entry.getValue().shade() < 0)
                        .mapToDouble(entry -> Math.hypot(TreeVoxelizer.unpackX(entry.getKey()), TreeVoxelizer.unpackZ(entry.getKey()))).max().orElse(0.0);
            }
        }
        assertTrue(spread[1] < spread[0] * 0.6, java.util.Arrays.toString(spread));
    }

    @Test
    void theLeavesOfAThinTreeAreLaidDarkToBrightAndNoneAlone() {
        // three shades in three passes, the brighter higher; every leaf touches a leaf or the wood
        TreeSettings settings = tree("thin");
        settings.setLeafGradient(java.util.List.of("DARK_OAK_LEAVES", "OAK_LEAVES", "AZALEA_LEAVES"));
        Map<Long, TreePart> blocks = TreeGenerator.build(settings, 4L, null);
        double[] height = new double[3];
        int[] count = new int[3];
        for (Map.Entry<Long, TreePart> entry : blocks.entrySet()) {
            int shade = entry.getValue().shade();
            if (shade < 0) continue;
            height[shade] += TreeVoxelizer.unpackY(entry.getKey());
            count[shade]++;
            int x = TreeVoxelizer.unpackX(entry.getKey());
            int y = TreeVoxelizer.unpackY(entry.getKey());
            int z = TreeVoxelizer.unpackZ(entry.getKey());
            boolean touches = false;
            for (int[] step : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                touches |= blocks.containsKey(TreeVoxelizer.pack(x + step[0], y + step[1], z + step[2]));
            }
            assertTrue(touches, "a leaf alone at " + x + "," + y + "," + z);
        }
        for (int shade = 0; shade < 3; shade++) assertTrue(count[shade] > 20, java.util.Arrays.toString(count));
        assertTrue(height[0] / count[0] < height[2] / count[2], java.util.Arrays.toString(height) + " " + java.util.Arrays.toString(count));
    }
}
