package io.github.iruurpg.treetool;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** {@code /treetool}: the tool, and the settings by command (for exact values and for saving under a name). */
final class ProceduralCommand implements TabExecutor {

    private static final String PREFIX = ProceduralModule.PREFIX;
    private static final List<String> SUBS = List.of("give", "presets", "scatter", "off", "menu", "set", "info", "save", "load", "list", "delete", "undo", "redo", "preview", "generate", "block",
            "build");

    private final ProceduralModule tool;

    ProceduralCommand(ProceduralModule tool) {
        this.tool = tool;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission(ProceduralModule.PERMISSION)) {
            player.sendMessage(PREFIX + "このコマンドを使う権限がありません (" + ProceduralModule.PERMISSION + ")。");
            return true;
        }
        String sub = args.length == 0 ? "give" : args[0].toLowerCase(Locale.ROOT);
        TreeSession session = tool.session(player);
        switch (sub) {
            case "give", "on" -> tool.give(player);
            // the presets; the placing tool (and the choosing of its presets)
            case "presets" -> tool.presets().open(player, PresetMenu.Mode.LOAD);
            case "scatter" -> tool.presets().open(player, PresetMenu.Mode.SCATTER);
            case "off", "remove" -> tool.take(player);
            case "menu" -> {
                if (session.base == null) player.sendMessage(PREFIX + "木を立てたい場所のブロックを、ツールでクリックしてください。");
                else tool.menu().open(player);
            }
            case "generate" -> tool.generate(player);
            case "undo" -> tool.undo(player);
            case "redo" -> tool.redo(player);
            case "preview" -> tool.preview().toggle(player);
            case "info" -> info(player, session.settings);
            case "set" -> set(player, session.settings, args);
            case "save" -> save(player, session.settings, args);
            case "load" -> load(player, session, args);
            case "block" -> block(player, session.settings, args);
            // a preset put at a place by command, the same with the same seed
            case "build" -> build(player, session, args);
            case "list" -> {
                List<String> names = tool.store().list();
                player.sendMessage(PREFIX + "保存した設定 (" + names.size() + "): " + (names.isEmpty() ? "なし" : String.join(", ", names)));
            }
            case "delete" -> {
                String name = args.length > 1 ? TreeSettings.safeName(args[1]) : null;
                if (name == null) player.sendMessage(PREFIX + "/treetool delete <名前>");
                else player.sendMessage(PREFIX + (tool.store().delete(name) ? "消しました: " + name : "そんな名前はありません: " + name));
            }
            default -> player.sendMessage(PREFIX + "/treetool [give|presets|scatter|off|menu|set <項目> <値>|info|save <名前>|load <名前>|list|delete <名前>|undo|redo|preview|generate|block <部位> <ブロック>|build <設定名> [x y z] [seed]]");
        }
        return true;
    }

    /**
     * {@code /treetool build <preset> [x y z] [seed]}: puts the saved preset at {@code x y z} (the block it stands in, {@code ~} for the player's own), else on
     * the block in sight, else where the player stands; with the seed the same one again (else a new one).
     */
    private void build(Player player, TreeSession session, String[] args) {
        String name = args.length > 1 ? TreeSettings.safeName(args[1]) : null;
        TreeSettings settings = name == null ? null : tool.store().load(name);
        if (settings == null) {
            player.sendMessage(PREFIX + "/treetool build <設定名> [x y z] [seed]  (設定名は /treetool list)" + (name == null ? "" : "  見つかりません: " + name));
            return;
        }
        org.bukkit.Location base = null;
        int next = 2;
        if (args.length >= 5) {
            try {
                org.bukkit.Location at = player.getLocation();
                base = new org.bukkit.Location(player.getWorld(), coordinate(args[2], at.getBlockX()), coordinate(args[3], at.getBlockY()), coordinate(args[4], at.getBlockZ()));
                next = 5;
            } catch (NumberFormatException exception) {
                player.sendMessage(PREFIX + "座標は整数 (~ で自分の位置、~3 で自分から 3)");
                return;
            }
        }
        if (base == null) {
            Sight.Hit hit = tool.sight(player);
            base = hit == null ? player.getLocation().getBlock().getLocation() : new org.bukkit.Location(player.getWorld(), hit.x(), hit.y() + 1, hit.z());
        }
        long seed;
        try {
            seed = args.length > next ? Long.parseLong(args[next]) : settings.seed() != 0L ? settings.seed() : new java.util.Random().nextLong() & Long.MAX_VALUE;
        } catch (NumberFormatException exception) {
            player.sendMessage(PREFIX + "seed は整数");
            return;
        }
        if (session.busy) {
            player.sendMessage(PREFIX + "前の木をまだ作っています。終わるまで待ってください。");
            return;
        }
        player.sendMessage(PREFIX + "build: " + name + " at " + base.getBlockX() + ", " + base.getBlockY() + ", " + base.getBlockZ() + " seed " + seed);
        tool.build(player, session, settings, base, seed);
    }

    /** A coordinate of the command: a number, or {@code ~} / {@code ~n} from the player's own. */
    static int coordinate(String text, int own) {
        if (text.startsWith("~")) return own + (text.length() == 1 ? 0 : Integer.parseInt(text.substring(1)));
        return Integer.parseInt(text);
    }

    /**
     * {@code /treetool block <part> <block|hand|reset>}: the block of a part; {@code block leaf <block,block,...>} the leaf gradient (bottom to top, one block is
     * one leaf block). Names a hand cannot hold (a potted fern) can be given this way.
     */
    private void block(Player player, TreeSettings settings, String[] args) {
        if (args.length < 3) {
            StringBuilder text = new StringBuilder(PREFIX + "/treetool block <部位> <ブロック名|hand|reset>  (leaf はカンマ区切りで、葉のグラデーション: 下,…,上。hang は装飾の木の垂れる房: カンマ区切りで混ぜる)  部位: ");
            for (TreePart part : TreePart.CHOICES) {
                text.append(part.key()).append('=').append(tool.material(part, settings).getKey().getKey()).append(' ');
            }
            player.sendMessage(text.toString().trim());
            return;
        }
        if (args[1].equalsIgnoreCase("hang")) {
            // (the blocks of an ornate tree's hanging strands, any blocks, mixed)
            List<String> names = new ArrayList<>();
            if (!args[2].equalsIgnoreCase("reset")) {
                for (String name : args[2].split(",")) {
                    String block = name.equalsIgnoreCase("hand") ? player.getInventory().getItemInMainHand().getType().name() : name;
                    if (TreeSettings.blockName(block) == null) {
                        player.sendMessage(PREFIX + "置けるブロックではありません: " + name);
                        return;
                    }
                    names.add(block);
                }
            }
            settings.setHangBlocks(names);
            tool.changed(player);
            player.sendMessage(PREFIX + "垂れる房: " + (settings.hangBlocks().isEmpty() ? "葉のいちばん下の段" : String.join(", ", settings.hangBlocks()).toLowerCase(Locale.ROOT)));
            return;
        }
        TreePart part = null;
        for (TreePart candidate : TreePart.values()) if (candidate.shade() <= 0 && candidate.key().equalsIgnoreCase(args[1])) part = candidate;
        if (part == null) {
            player.sendMessage(PREFIX + "そんな部位はありません: " + args[1]);
            return;
        }
        String value = args[2];
        if (value.equalsIgnoreCase("hand")) value = player.getInventory().getItemInMainHand().getType().name();
        if (value.equalsIgnoreCase("reset")) {
            settings.setBlock(part, null);
            if (part == TreePart.LEAF) settings.setLeafGradient(List.of());
        } else if (part == TreePart.LEAF && value.contains(",")) {
            List<String> names = new ArrayList<>();
            for (String name : value.split(",")) {
                if (TreeSettings.blockName(name) == null) {
                    player.sendMessage(PREFIX + "置けるブロックではありません: " + name);
                    return;
                }
                names.add(name);
            }
            settings.setLeafGradient(names);
        } else {
            if (TreeSettings.blockName(value) == null) {
                player.sendMessage(PREFIX + "置けるブロックではありません: " + value);
                return;
            }
            settings.setBlock(part, value);
            if (part == TreePart.LEAF) settings.setLeafGradient(List.of());
        }
        tool.changed(player);
        player.sendMessage(PREFIX + part.label() + ": " + (part == TreePart.LEAF && !settings.leafGradient().isEmpty()
                ? String.join(" → ", settings.leafGradient()).toLowerCase(Locale.ROOT) + " (下 → 上)"
                : tool.material(part, settings).getKey().getKey()));
    }

    private void info(Player player, TreeSettings settings) {
        player.sendMessage(PREFIX + "設定 (" + settings.name() + "): アルゴリズム " + settings.algorithm() + " / シード " + (settings.seed() == 0L ? "ランダム" : settings.seed()));
        // (only the numbers of the algorithm: those on the pages of the menu)
        java.util.Set<TreeSettings.Key> shown = new java.util.LinkedHashSet<>();
        for (ProceduralMenu.Page page : ProceduralMenu.Page.values()) {
            if (page.shownFor(settings)) shown.addAll(List.of(page.keysFor(settings)));
        }
        for (TreeSettings.Key key : shown) {
            player.sendMessage("  " + key.id() + " = " + (key.toggle() ? (settings.on(key) ? "1 (オン)" : "0 (オフ)") : key.whole() ? Integer.toString(settings.getInt(key)) : Double.toString(settings.get(key)))
                    + (key.toggle() ? "" : "  (" + key.min() + " 〜 " + key.max() + ")") + " " + key.label());
        }
    }

    private void set(Player player, TreeSettings settings, String[] args) {
        if (args.length < 3) {
            player.sendMessage(PREFIX + "/treetool set <項目> <値>  項目: algorithm, seed, " + keyIds());
            return;
        }
        String item = args[1].toLowerCase(Locale.ROOT);
        String value = args[2].toLowerCase(Locale.ROOT);
        switch (item) {
            case "algorithm" -> {
                if (!TreeSettings.ALGORITHMS.contains(value)) {
                    player.sendMessage(PREFIX + "algorithm は " + String.join(" / ", TreeSettings.ALGORITHMS));
                    return;
                }
                settings.setAlgorithm(value);
            }
            case "seed" -> {
                try {
                    settings.setSeed(Long.parseLong(value));
                } catch (NumberFormatException exception) {
                    player.sendMessage(PREFIX + "seed は整数 (0 = 毎回ランダム)");
                    return;
                }
            }
            default -> {
                TreeSettings.Key key = TreeSettings.Key.byId(item);
                if (key == null) {
                    player.sendMessage(PREFIX + "そんな項目はありません。項目: algorithm, seed, " + keyIds());
                    return;
                }
                try {
                    settings.set(key, Double.parseDouble(value));
                } catch (NumberFormatException exception) {
                    player.sendMessage(PREFIX + key.id() + " は数字で指定してください (" + key.min() + " 〜 " + key.max() + ")");
                    return;
                }
            }
        }
        tool.changed(player);
        player.sendMessage(PREFIX + "設定しました: " + item + " = " + value);
    }

    private static String keyIds() {
        List<String> ids = new ArrayList<>();
        for (TreeSettings.Key key : TreeSettings.Key.values()) ids.add(key.id());
        return String.join(", ", ids);
    }

    private void save(Player player, TreeSettings settings, String[] args) {
        String name = TreeSettings.safeName(args.length > 1 ? args[1] : settings.name());
        if (name == null) {
            player.sendMessage(PREFIX + "名前は英小文字・数字・- _ の32文字まで: /treetool save <名前>");
            return;
        }
        settings.setName(name);
        player.sendMessage(PREFIX + (tool.store().save(name, settings) ? "保存しました: " + name : "保存できませんでした (ログを見てください)。"));
    }

    private void load(Player player, TreeSession session, String[] args) {
        String name = args.length > 1 ? TreeSettings.safeName(args[1]) : null;
        if (name == null) {
            player.sendMessage(PREFIX + "/treetool load <名前>  (/treetool list で一覧)");
            return;
        }
        TreeSettings loaded = tool.store().load(name);
        if (loaded == null) {
            player.sendMessage(PREFIX + "そんな名前はありません: " + name);
            return;
        }
        session.settings = loaded;
        session.autoSeed = false;
        tool.changed(player);
        player.sendMessage(PREFIX + "読み込みました: " + name + " (/treetool info で中身)");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> options = new ArrayList<>();
        if (!sender.hasPermission(ProceduralModule.PERMISSION)) return options;
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            options.addAll(SUBS);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            options.addAll(List.of("algorithm", "seed"));
            for (TreeSettings.Key key : TreeSettings.Key.values()) options.add(key.id());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("block")) {
            for (TreePart part : TreePart.values()) if (part.shade() <= 0) options.add(part.key());
            options.add("hang");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("block")) {
            options.addAll(List.of("hand", "reset"));
            String typed = args[2].toUpperCase(Locale.ROOT);
            String head = typed.contains(",") ? typed.substring(0, typed.lastIndexOf(',') + 1) : "";
            String tail = typed.substring(head.length());
            int found = 0;
            for (org.bukkit.Material material : org.bukkit.Material.values()) {
                if (found >= 40 || material.isLegacy() || !material.isBlock() || material.isAir() || !material.name().startsWith(tail)) continue;
                options.add((head + material.name()).toLowerCase(Locale.ROOT));
                found++;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            if (args[1].equalsIgnoreCase("algorithm")) options.addAll(TreeSettings.ALGORITHMS);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("load") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("build"))) {
            options.addAll(tool.store().list());
        }
        List<String> result = new ArrayList<>();
        for (String option : options) if (option.toLowerCase(Locale.ROOT).startsWith(last)) result.add(option);
        return result;
    }
}
