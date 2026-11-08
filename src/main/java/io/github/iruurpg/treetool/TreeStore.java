package io.github.iruurpg.treetool;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The settings of trees saved by name, one yaml file each in {@code plugins/IruuTreeTool/trees/}. */
final class TreeStore {

    private final JavaPlugin plugin;

    public TreeStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private File folder() {
        File folder = new File(plugin.getDataFolder(), "trees");
        if (!folder.exists() && !folder.mkdirs()) plugin.getLogger().warning("Could not create " + folder);
        return folder;
    }

    private File file(String name) {
        return new File(folder(), name + ".yml");
    }

    /** Saves under {@code name} (already made safe by {@link TreeSettings#safeName}); false when the file cannot be written. */
    public boolean save(String name, TreeSettings settings) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of("木の設定 (IruuTreeTool の /treetool で読み込める)。数字は範囲の中に収められる。"));
        yaml.set("settings", settings.toMap());
        try {
            yaml.save(file(name));
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save tree " + name + ": " + exception);
            return false;
        }
    }

    /**
     * The settings saved as {@code name}, or null when there are none (or the file is broken, or it is not a tree: a file with another {@code kind:} than
     * {@code tree}, such as a rock of a presets folder made by another version of the tool, is left in the folder as it is).
     */
    public TreeSettings load(String name) {
        File file = file(name);
        if (!file.isFile()) return null;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("settings");
        if (section == null) return null;
        Map<String, Object> map = new LinkedHashMap<>(section.getValues(false));
        if (map.get("kind") instanceof String kind && !kind.equalsIgnoreCase("tree")) return null;
        return TreeSettings.fromMap(map, name);
    }

    /**
     * Deletes the preset {@code name}: its file is moved into {@code trees/trash/} with the time in its name (put it back in {@code trees/} by hand to
     * restore it). False when there is none or it cannot be moved.
     */
    public boolean delete(String name) {
        File file = file(name);
        if (!file.isFile()) return false;
        File trash = new File(folder(), "trash");
        if (!trash.isDirectory() && !trash.mkdirs()) return false;
        String stamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        boolean moved = file.renameTo(new File(trash, name + "-" + stamp + ".yml"));
        if (moved) {
            infos.remove(name);
            infoTimes.remove(name);
        }
        return moved;
    }

    /**
     * Puts the sample trees of the jar ({@code trees/samples.txt} lists them) in the folder, each once: a sample is written when it was never written before
     * (the names written are kept in {@code trees/.samples}), so a sample deleted or changed by hand is not brought back. Never throws.
     */
    public void installSamples() {
        try {
            File marker = new File(folder(), ".samples");
            java.util.Set<String> done = new java.util.HashSet<>();
            if (marker.isFile()) done.addAll(java.nio.file.Files.readAllLines(marker.toPath()));
            java.io.InputStream list = plugin.getResource("trees/samples.txt");
            if (list == null) return;
            List<String> names;
            try (list) {
                names = new String(list.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
            }
            int added = 0;
            for (String name : names) {
                if (done.contains(name)) continue;
                if (!file(name).exists()) {
                    plugin.saveResource("trees/" + name + ".yml", false);
                    added++;
                }
                done.add(name);
            }
            java.nio.file.Files.write(marker.toPath(), new ArrayList<>(done));
            if (added > 0) plugin.getLogger().info("Tree tool: " + added + " sample trees added to " + folder().getName() + "/");
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().warning("Tree tool: sample trees not added: " + exception);
        }
    }

    /** What a saved preset is, for the list of presets: its algorithm, its style (an ornate tree) and the block of its trunk, if any. */
    record Info(String algorithm, int style, String trunk) {
    }

    private final Map<String, Info> infos = new java.util.HashMap<>();
    private final Map<String, Long> infoTimes = new java.util.HashMap<>();

    /** The info of preset {@code name} (read again only when its file changed); null when it cannot be read. */
    public Info info(String name) {
        File file = file(name);
        if (!file.isFile()) return null;
        Long time = infoTimes.get(name);
        if (time != null && time == file.lastModified() && infos.containsKey(name)) return infos.get(name);
        TreeSettings settings = load(name);
        // (one that cannot be read is kept as null too, not read again every time the presets are listed)
        Info info = settings == null ? null : new Info(settings.algorithm(), settings.getInt(TreeSettings.Key.ORN_STYLE), settings.block(TreePart.TRUNK));
        infos.put(name, info);
        infoTimes.put(name, file.lastModified());
        return info;
    }

    /** The names saved, in alphabetical order (not those that cannot be read: not a tree, or broken). */
    public List<String> list() {
        File[] files = folder().listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        List<String> names = new ArrayList<>();
        if (files == null) return names;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4);
            if (info(name) != null) names.add(name);
        }
        return names;
    }
}
