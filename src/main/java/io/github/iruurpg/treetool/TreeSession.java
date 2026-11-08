package io.github.iruurpg.treetool;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/** What one player has in hand: the settings being edited, where the tree would stand, and the state of the preview. */
final class TreeSession {

    public TreeSettings settings = new TreeSettings();
    /** The block the tree stands in: the one above the block that was clicked. */
    public Location base;
    /** The block that was clicked. */
    Location clicked;
    /** The page of the menu that was open last (by name): the menu opens on it next time. */
    String lastPage = "BASIC";
    /** A tree is being put in the world. */
    boolean busy;

    // ---- the presets and the placing tool (see PresetMenu) --------------------------------------------------------------------
    /** The presets chosen for the placing tool, with their weights (in the order chosen). */
    final Map<String, Integer> scatter = new java.util.LinkedHashMap<>();

    // ---- the preview (see TreePreview) ----------------------------------------------------------------------------------
    /** The preview is on. */
    boolean preview;
    /** The seed in the settings was chosen by the preview (the settings had none): it goes back to "random" when the preview goes off. */
    boolean autoSeed;
    /** The wait before a changed setting is previewed; running while a change is fresh. */
    BukkitTask timer;
    /** The task that sends the blocks of the preview, a few thousand a tick. */
    BukkitTask show;
    /** A preview is being worked out off the main thread: another one is not started meanwhile (and none is queued). */
    volatile boolean computing;
    /** Number of the last preview started; a result of an older one (the preview was turned off meanwhile) is dropped. */
    int job;
    /** The blocks the player is shown now, by {@link TreePreview#key}, and the world they are in. */
    final Map<Long, org.bukkit.block.data.BlockData> shown = new HashMap<>();
    World shownWorld;
}
