package io.github.iruurpg.treetool;

import org.bukkit.Material;

import java.util.List;

/**
 * The parts a generated tree is made of. The block of each is chosen in the menu (or by command) and saved with the settings; what is not chosen comes from the
 * config ({@code treetool.materials}), else from the default here. The lower the {@code priority}, the stronger the part where two meet. The leaves are a
 * gradient of up to {@link #SHADES} blocks from the bottom of a clump to its top: {@link #LEAF} is the lowest shade, {@code LEAF_2}.. the ones above it.
 */
enum TreePart {
    TRUNK("trunk", "幹", 0, Material.OAK_WOOD),
    ROOT("root", "根", 1, Material.OAK_WOOD),
    BRANCH("branch", "枝", 2, Material.OAK_WOOD),
    LEAF("leaf", "葉", 4, Material.OAK_LEAVES),
    /** The snow on the leaves of a tiered tree. */
    SNOW("snow", "雪", 3, Material.SNOW_BLOCK),
    /** The shades of the leaf gradient above the lowest ({@link #LEAF}): their blocks are the entries of the gradient of the settings. */
    LEAF_2("leaf-2", "葉 (2段目)", 4, Material.OAK_LEAVES),
    LEAF_3("leaf-3", "葉 (3段目)", 4, Material.OAK_LEAVES),
    LEAF_4("leaf-4", "葉 (4段目)", 4, Material.OAK_LEAVES),
    LEAF_5("leaf-5", "葉 (5段目)", 4, Material.OAK_LEAVES),
    LEAF_6("leaf-6", "葉 (6段目)", 4, Material.OAK_LEAVES),
    /** the thin branches of a thin tree: a post near the trunk (a wall), a post further out (a fence), a branch along (a slab, its bottom or top half). */
    BRANCH_WALL("branch-wall", "枝 (壁)", 2, Material.MUD_BRICK_WALL),
    BRANCH_FENCE("branch-fence", "細い枝 (フェンス)", 3, Material.OAK_FENCE),
    BRANCH_SLAB("branch-slab", "横の枝 (ハーフ)", 3, Material.OAK_SLAB),
    /** The upper half of a block a slab branch passes: the block of {@link #BRANCH_SLAB}, as its top half. */
    BRANCH_SLAB_TOP("branch-slab-top", "横の枝 (ハーフの上)", 3, Material.OAK_SLAB),
    /** a stair of the thin tree: where a log steps up as it goes over, and round the foot. Its way: the back toward north (this), east, south, west; one block for all. */
    BRANCH_STAIR("branch-stair", "幹・枝の階段", 1, Material.OAK_STAIRS),
    BRANCH_STAIR_EAST("branch-stair-east", "幹・枝の階段 (東)", 1, Material.OAK_STAIRS),
    BRANCH_STAIR_SOUTH("branch-stair-south", "幹・枝の階段 (南)", 1, Material.OAK_STAIRS),
    BRANCH_STAIR_WEST("branch-stair-west", "幹・枝の階段 (西)", 1, Material.OAK_STAIRS),
    /** (the silhouette trees) the stairs upside down (their top half full), the same four ways; and logs lying along x and z. */
    BRANCH_STAIR_TOP("branch-stair-top", "幹・枝の階段 (上付き・北)", 1, Material.OAK_STAIRS),
    BRANCH_STAIR_TOP_EAST("branch-stair-top-east", "幹・枝の階段 (上付き・東)", 1, Material.OAK_STAIRS),
    BRANCH_STAIR_TOP_SOUTH("branch-stair-top-south", "幹・枝の階段 (上付き・南)", 1, Material.OAK_STAIRS),
    BRANCH_STAIR_TOP_WEST("branch-stair-top-west", "幹・枝の階段 (上付き・西)", 1, Material.OAK_STAIRS),
    TRUNK_X("trunk-x", "幹 (東西に寝た丸太)", 0, Material.OAK_WOOD),
    TRUNK_Z("trunk-z", "幹 (南北に寝た丸太)", 0, Material.OAK_WOOD),
    /** the ornate tree: trapdoors (the level tips of thin branches, the streaks of bark) and open fence gates (level tips). */
    BRANCH_TRAPDOOR("branch-trapdoor", "枝先・樹皮 (トラップドア)", 3, Material.OAK_TRAPDOOR),
    BRANCH_GATE("branch-gate", "枝先 (フェンスゲート)", 3, Material.OAK_FENCE_GATE);

    /** The parts whose blocks are chosen in the menu and by command (the others are shades, other ways and other halves of these). */
    static final List<TreePart> CHOICES = List.of(TRUNK, BRANCH, ROOT, LEAF, SNOW, BRANCH_WALL, BRANCH_FENCE, BRANCH_SLAB, BRANCH_STAIR, BRANCH_TRAPDOOR, BRANCH_GATE);

    /** The part whose block this one is put in (a stair's other ways and a slab's top half are their first part's block). */
    TreePart blockOf() {
        return switch (this) {
            case BRANCH_SLAB_TOP -> BRANCH_SLAB;
            case BRANCH_STAIR_EAST, BRANCH_STAIR_SOUTH, BRANCH_STAIR_WEST, BRANCH_STAIR_TOP, BRANCH_STAIR_TOP_EAST, BRANCH_STAIR_TOP_SOUTH, BRANCH_STAIR_TOP_WEST -> BRANCH_STAIR;
            case TRUNK_X, TRUNK_Z -> TRUNK;
            default -> this;
        };
    }

    /** How many blocks the leaf gradient can have. */
    static final int SHADES = 6;
    private static final TreePart[] LEAF_SHADES = {LEAF, LEAF_2, LEAF_3, LEAF_4, LEAF_5, LEAF_6};

    private final String key;
    private final String label;
    private final int priority;
    private final Material defaultMaterial;

    TreePart(String key, String label, int priority, Material defaultMaterial) {
        this.key = key;
        this.label = label;
        this.priority = priority;
        this.defaultMaterial = defaultMaterial;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public int priority() {
        return priority;
    }

    public Material defaultMaterial() {
        return defaultMaterial;
    }

    /** The place of the part in the leaf gradient (0 = the lowest, {@link #LEAF}); -1 for a part that is not a leaf. */
    int shade() {
        for (int index = 0; index < LEAF_SHADES.length; index++) if (LEAF_SHADES[index] == this) return index;
        return -1;
    }

    /** The leaf of shade {@code index} (0 = the lowest), kept inside the gradient. */
    static TreePart leaf(int index) {
        return LEAF_SHADES[Math.max(0, Math.min(SHADES - 1, index))];
    }

    /**
     * The shade of a leaf block at {@code height} of its clump (0 = the bottom, 1 = the top) in a gradient of {@code shades} blocks: the bottom of a clump takes the
     * first block of the gradient, the top the last. {@code jitter} (about -1..1, from a noise) moves the borders a little, so the bands are not flat stripes.
     */
    static TreePart leafAt(double height, int shades, double jitter) {
        if (shades <= 1) return LEAF;
        double place = (height + 0.12 * jitter) * shades;
        return leaf((int) Math.floor(Math.max(0.0, Math.min(shades - 1.0e-9, place))));
    }
}
