package io.github.iruurpg.treetool;

import java.util.Map;
import java.util.Random;

/** One tree from its settings, a seed and the terrain, by the algorithm of the settings. Arithmetic only, so it runs off the main thread. */
final class TreeGenerator {

    private TreeGenerator() {
    }

    static Map<Long, TreePart> build(TreeSettings settings, long seed, TreeGround ground) {
        Random random = new Random(seed);
        return switch (settings.algorithm()) {
            case "tiered" -> TieredTree.build(settings, random);
            case "palm" -> PalmTree.build(settings, random);
            case "thin" -> ThinTree.build(settings, random);
            case "silhouette" -> SilhouetteTree.build(settings, random);
            case "ornate" -> OrnateTree.build(settings, random, ground);
            default -> CanopyTree.build(settings, random);
        };
    }

    /** How far from the base the terrain is read (only an ornate tree reads it: it lays its roots on the ground and hangs its strands down to it). */
    static int groundRadius(TreeSettings settings) {
        return OrnateTree.groundRadius(settings);
    }
}
