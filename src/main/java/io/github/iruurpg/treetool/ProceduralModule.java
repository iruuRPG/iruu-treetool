package io.github.iruurpg.treetool;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The tree tool (for players with the permission {@value #PERMISSION}). {@code /treetool} gives a placement tool (a sapling); clicking a block with it opens the settings menu
 * ({@link ProceduralMenu}) for a tree standing on that block. The menu can turn the preview on ({@link TreePreview}: the tree in its blocks, to that player alone, following
 * the settings), and starts the generation: the skeleton of the tree is grown and turned into blocks off the main thread ({@link TreeGenerator}), and the blocks are put in
 * the world through FastAsyncWorldEdit or WorldEdit in the player's own edit session ({@link TreeWorldEdit}), so {@code //undo} and {@code //redo} take the whole tree back and put it
 * again. Every part of the tree is the block chosen for it ({@link TreePart}: in the menu or by command, else the config's). Only air and plants are replaced (not terrain),
 * unless the config says otherwise. It has its own command, item and listeners.
 */
public final class ProceduralModule implements Listener {

    static final String PREFIX = "[木] ";
    /** The permission to use the tool, the placing tool and the command. */
    static final String PERMISSION = "iruutreetool.use";

    private final JavaPlugin plugin;
    private final NamespacedKey toolKey;
    private final TreeStore store;
    private final ProceduralMenu menu;
    /** The list of presets (loading, and choosing the placing tool's). */
    private final PresetMenu presets;
    /** The mark of the placing tool, and the presets it puts with their weights ("name=weight;..."). */
    private final NamespacedKey scatterKey;
    private final NamespacedKey scatterListKey;
    private final TreePreview preview;
    private final boolean worldEdit;
    private final Map<UUID, TreeSession> sessions = new HashMap<>();

    /** Starts the tree tool: registers its listeners and its command. Never throws: a failure is logged and the tool is simply not there. */
    public static ProceduralModule start(JavaPlugin plugin) {
        try {
            ProceduralModule module = new ProceduralModule(plugin);
            plugin.getServer().getPluginManager().registerEvents(module, plugin);
            plugin.getServer().getPluginManager().registerEvents(module.menu, plugin);
            plugin.getServer().getPluginManager().registerEvents(module.presets, plugin);
            org.bukkit.command.PluginCommand command = plugin.getCommand("treetool");
            if (command != null) {
                ProceduralCommand handler = new ProceduralCommand(module);
                command.setExecutor(handler);
                command.setTabCompleter(handler);
            }
            return module;
        } catch (RuntimeException | LinkageError exception) {
            plugin.getLogger().warning("Tree tool not started: " + exception);
            return null;
        }
    }

    private ProceduralModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "tree_tool");
        this.store = new TreeStore(plugin);
        store.installSamples();
        this.menu = new ProceduralMenu(this);
        this.presets = new PresetMenu(this);
        this.scatterKey = new NamespacedKey(plugin, "scatter_tool");
        this.scatterListKey = new NamespacedKey(plugin, "scatter_list");
        this.preview = new TreePreview(this);
        this.worldEdit = plugin.getServer().getPluginManager().isPluginEnabled("FastAsyncWorldEdit")
                || plugin.getServer().getPluginManager().isPluginEnabled("WorldEdit");
        if (!worldEdit) plugin.getLogger().warning("Tree tool: WorldEdit / FastAsyncWorldEdit is not there, trees cannot be undone.");
    }

    /** The plugin stops: every preview is taken back and no task is left. */
    public void stop() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (sessions.containsKey(player.getUniqueId())) preview.drop(player);
        }
    }

    JavaPlugin plugin() {
        return plugin;
    }

    TreeStore store() {
        return store;
    }

    ProceduralMenu menu() {
        return menu;
    }

    TreePreview preview() {
        return preview;
    }

    PresetMenu presets() {
        return presets;
    }

    TreeSession session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), ignored -> new TreeSession());
    }

    /** A setting or the place changed: the preview, when it is on, follows after its wait. */
    void changed(Player player) {
        syncBase(session(player));
        preview.touch(player);
    }

    /** The place of the tree: above the block clicked. */
    private static void syncBase(TreeSession session) {
        if (session.clicked == null) return;
        session.base = session.clicked.clone().add(BlockFace.UP.getModX(), BlockFace.UP.getModY(), BlockFace.UP.getModZ());
    }

    // ---- config ---------------------------------------------------------------------------------------------------------

    /** The block of a part when nothing is chosen in the settings: {@code treetool.materials.<part>} of the config, else the part's default. */
    Material material(TreePart part) {
        TreePart base = part.shade() > 0 ? TreePart.LEAF : part.blockOf();
        String name = plugin.getConfig().getString("treetool.materials." + base.key(), base.defaultMaterial().name());
        Material material = name == null ? null : Material.matchMaterial(name);
        return material != null && material.isBlock() && !material.isAir() ? material : base.defaultMaterial();
    }

    /**
     * The block of a part for these settings: a shade of the leaf gradient when the leaves have one, else the block chosen for the part (menu or command), else
     * the config's. A name with states ({@code minecraft:oak_log[axis=x]}) gives its material here; the states are in {@link #blockData}.
     */
    Material material(TreePart part, TreeSettings settings) {
        String chosen = chosenName(part, settings);
        Material material = TreeSettings.materialOf(chosen);
        return material != null && material.isBlock() && !material.isAir() ? material : material(part);
    }

    /** The name chosen for the part in the settings (a shade of the gradient for a leaf), or null. */
    private static String chosenName(TreePart part, TreeSettings settings) {
        // (the top half of a slab branch, the other ways of a stair, are the block of their first part)
        part = part.blockOf();
        int shade = part.shade();
        List<String> gradient = settings.leafGradient();
        if (shade >= 0 && !gradient.isEmpty()) return gradient.get(Math.min(shade, gradient.size() - 1));
        return settings.block(shade > 0 ? TreePart.LEAF : part);
    }

    /**
     * The block a part is put in the world (and shown in the preview) as: with the states chosen ({@code [axis=x]} and so on; states the block does not have
     * leave it plain); leaves are persistent unless the states say otherwise, so a tree far from its logs does not decay.
     */
    BlockData blockData(TreePart part, TreeSettings settings) {
        String chosen = chosenName(part, settings);
        Material material = material(part, settings);
        BlockData data = null;
        if (chosen != null && chosen.indexOf('[') > 0 && material == TreeSettings.materialOf(chosen)) {
            try {
                data = Bukkit.createBlockData(chosen);
            } catch (IllegalArgumentException exception) {
                data = null;
            }
        }
        if (data == null) {
            data = material.createBlockData();
            if (data instanceof org.bukkit.block.data.type.Leaves leaves) leaves.setPersistent(true);
        }
        return data;
    }

    // ---- the tool item ----------------------------------------------------------------------------------------------------

    ItemStack toolItem() {
        ItemStack item = new ItemStack(Material.OAK_SAPLING);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("木のツール", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                text("ブロックを右クリック: そこに立てる木の設定画面を開く", NamedTextColor.GRAY),
                text("遠くのブロックも、見てクリックすれば同じ (届くのは treetool.reach ブロック、初期値 96)", NamedTextColor.GRAY),
                text("左クリック: 置く場所はそのままで設定画面を開く (まだ場所がなければ、右クリックと同じ)", NamedTextColor.GRAY),
                text("Shift+空中を右クリック (見える所にブロックがないときは、ただの右クリック): 前回の場所の設定画面を開く", NamedTextColor.GRAY),
                text("設定画面の「生成」で、部位ごとに選んだブロックで作る", NamedTextColor.GRAY),
                text("設定画面の「プレビュー」で、あなたにだけ見える試し置きを確かめられる", NamedTextColor.GRAY),
                text("作った木は //undo で戻せる (WorldEdit / FastAsyncWorldEdit があるとき)", NamedTextColor.GRAY),
                text("/treetool off でこのツールを消す", NamedTextColor.DARK_GRAY)));
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private static Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    boolean isTool(ItemStack item) {
        if (item == null || item.getType() != Material.OAK_SAPLING || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(toolKey, PersistentDataType.BYTE);
    }

    void give(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTool(item)) {
                player.sendMessage(PREFIX + "もう持っています。ブロックを右クリックすると、そこに立てる設定画面が開きます (左クリックは、場所はそのままで設定画面)。");
                return;
            }
        }
        player.getInventory().addItem(toolItem()).values().forEach(left -> player.getWorld().dropItem(player.getLocation(), left));
        player.sendMessage(PREFIX + "木のツールを渡しました。ブロックを右クリックすると、そこに立てる木の設定画面が開きます。左クリックは、置く場所を変えずに設定画面を開きます。");
    }

    void take(Player player) {
        preview.off(player, false);
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isTool(contents[slot])) {
                player.getInventory().setItem(slot, null);
                removed++;
            }
        }
        player.sendMessage(PREFIX + (removed > 0 ? "ツールを片づけました。" : "ツールは持っていません。"));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (isScatter(event.getItem())) {
            event.setCancelled(true);
            if (!event.getPlayer().hasPermission(PERMISSION)) {
                event.getPlayer().sendMessage(PREFIX + "この配置ツールを使う権限がありません (" + PERMISSION + ")。");
                return;
            }
            scatter(event.getPlayer(), event.getItem(), event.getAction(), event.getClickedBlock());
            return;
        }
        if (!isTool(event.getItem())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(PREFIX + "このツールを使う権限がありません (" + PERMISSION + ")。");
            return;
        }
        TreeSession session = session(player);
        Action action = event.getAction();
        boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if (menuOnly(left, session.base != null && player.getWorld().equals(session.base.getWorld()))) {
            // (a left click opens the menu of the place there is and leaves the place as it was; the right click chooses the place)
            menu.open(player);
            return;
        }
        Block target = event.getClickedBlock();
        boolean air = action == Action.RIGHT_CLICK_AIR || action == Action.LEFT_CLICK_AIR;
        if (air && !(action == Action.RIGHT_CLICK_AIR && player.isSneaking())) {
            // (a click in the air is a click on the block in sight, however far within the reach)
            Sight.Hit hit = sight(player);
            if (hit != null) {
                target = player.getWorld().getBlockAt(hit.x(), hit.y(), hit.z());
                air = false;
            }
        }
        if (!air) {
            if (target == null) return;
            session.clicked = target.getLocation();
            syncBase(session);
            changed(player);
            menu.open(player);
        } else if (action == Action.RIGHT_CLICK_AIR) {
            if (session.base == null) {
                player.sendMessage(PREFIX + "木を立てたい場所のブロックをクリックしてください (遠くても見えていれば届きます)。");
                return;
            }
            menu.open(player);
        }
    }

    /**
     * Whether a click with the tool only opens the menu, the place staying as it was: a left click once there is a place in this world. A left click before
     * there is a place chooses it, as a right click does. Pure, for the tests.
     */
    static boolean menuOnly(boolean left, boolean placed) {
        return left && placed;
    }

    // ---- the placing tool ------------------------------------------------------------------------------------------------------

    ItemStack scatterItem(Map<String, Integer> chosen) {
        ItemStack item = new ItemStack(Material.BIRCH_SAPLING);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("配置ツール", NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(scatterKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(scatterListKey, PersistentDataType.STRING, encode(chosen));
        item.setItemMeta(meta);
        describeScatter(item, chosen);
        return item;
    }

    private void describeScatter(ItemStack item, Map<String, Integer> chosen) {
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(text("右クリック: 下のプリセットのどれかを、重みの割合で、クリックしたブロックの上にランダムなシードで置く (//undo で戻せる)", NamedTextColor.GRAY));
        lore.add(text("遠くのブロックも、見てクリックすれば同じ", NamedTextColor.GRAY));
        lore.add(text("左クリック: プリセットと重みを選ぶ画面", NamedTextColor.GRAY));
        int total = 0;
        for (int weight : chosen.values()) total += weight;
        for (Map.Entry<String, Integer> entry : chosen.entrySet()) {
            lore.add(text("・" + entry.getKey() + " × " + entry.getValue() + String.format(java.util.Locale.ROOT, " (%.0f%%)", 100.0 * entry.getValue() / Math.max(1, total)), NamedTextColor.WHITE));
        }
        if (chosen.isEmpty()) lore.add(text("(まだプリセットを選んでいない)", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    boolean isScatter(ItemStack item) {
        if (item == null || item.getType() != Material.BIRCH_SAPLING || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(scatterKey, PersistentDataType.BYTE);
    }

    /** The presets a placing tool puts, with their weights. */
    Map<String, Integer> scatterList(ItemStack item) {
        String text = item.getItemMeta().getPersistentDataContainer().get(scatterListKey, PersistentDataType.STRING);
        return decode(text);
    }

    static String encode(Map<String, Integer> chosen) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Integer> entry : chosen.entrySet()) {
            if (!out.isEmpty()) out.append(';');
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    static Map<String, Integer> decode(String text) {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        if (text == null || text.isEmpty()) return out;
        for (String pair : text.split(";")) {
            int equals = pair.lastIndexOf('=');
            if (equals <= 0) continue;
            try {
                int weight = Integer.parseInt(pair.substring(equals + 1));
                if (weight > 0) out.put(pair.substring(0, equals), weight);
            } catch (NumberFormatException ignored) {
                // (a broken entry is left out)
            }
        }
        return out;
    }

    /** Gives a placing tool of these presets, or writes them into the one in hand. */
    void giveScatter(Player player, Map<String, Integer> chosen) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (isScatter(hand)) {
            ItemMeta meta = hand.getItemMeta();
            meta.getPersistentDataContainer().set(scatterListKey, PersistentDataType.STRING, encode(chosen));
            hand.setItemMeta(meta);
            describeScatter(hand, chosen);
            player.sendMessage(PREFIX + "手に持っている配置ツールの、プリセットを変えました (" + chosen.size() + " 種類)。");
            return;
        }
        player.getInventory().addItem(scatterItem(chosen)).values().forEach(left -> player.getWorld().dropItem(player.getLocation(), left));
        player.sendMessage(PREFIX + "配置ツールを渡しました (" + chosen.size() + " 種類)。右クリックで置く / 左クリックでプリセットを選ぶ。");
    }

    /** One of a placing tool's presets, by weight (null when there is none). Pure, for the tests. */
    static String pick(Map<String, Integer> chosen, Random random) {
        int total = 0;
        for (int weight : chosen.values()) total += Math.max(0, weight);
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (Map.Entry<String, Integer> entry : chosen.entrySet()) {
            roll -= Math.max(0, entry.getValue());
            if (roll < 0) return entry.getKey();
        }
        return null;
    }

    /** A click with the placing tool: the left one opens the choosing of its presets, the right one puts one of them where it is clicked (or the block in sight). */
    private void scatter(Player player, ItemStack item, Action action, Block clicked) {
        TreeSession session = session(player);
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            session.scatter.clear();
            session.scatter.putAll(scatterList(item));
            presets.open(player, PresetMenu.Mode.SCATTER);
            return;
        }
        Block target = clicked;
        if (target == null) {
            Sight.Hit hit = sight(player);
            if (hit == null) {
                player.sendMessage(PREFIX + "置く場所のブロックを見て、右クリックしてください。");
                return;
            }
            target = player.getWorld().getBlockAt(hit.x(), hit.y(), hit.z());
        }
        if (session.busy) {
            player.sendMessage(PREFIX + "前の木をまだ作っています。終わるまで待ってください。");
            return;
        }
        Random random = new Random();
        String name = pick(scatterList(item), random);
        if (name == null) {
            player.sendMessage(PREFIX + "配置ツールにプリセットがありません。左クリックで選んでください。");
            return;
        }
        TreeSettings settings = store.load(name);
        if (settings == null) {
            player.sendMessage(PREFIX + "プリセットが読めません (消された?): " + name);
            return;
        }
        Location base = target.getLocation().add(0, 1, 0);
        long seed = random.nextLong() & Long.MAX_VALUE;
        player.sendMessage(PREFIX + "配置: " + name + " (" + algorithmLabel(settings.algorithm()) + ")");
        build(player, session, settings, base, seed);
    }

    // ---- the block in sight ---------------------------------------------------------------------------------------------------

    /** How far the tool reaches: the block in sight up to this many blocks ({@code treetool.reach}). */
    double reach() {
        return Math.max(8.0, Math.min(256.0, plugin.getConfig().getDouble("treetool.reach", 96.0)));
    }

    /** The block the player looks at within the reach, or null. */
    Sight.Hit sight(Player player) {
        Location eye = player.getEyeLocation();
        World world = player.getWorld();
        org.bukkit.util.Vector direction = eye.getDirection();
        return Sight.ray(eye.getX(), eye.getY(), eye.getZ(), direction.getX(), direction.getY(), direction.getZ(), reach(), null,
                (x, y, z) -> y >= world.getMinHeight() && y < world.getMaxHeight() && world.isChunkLoaded(x >> 4, z >> 4) && solid(world.getBlockAt(x, y, z)));
    }

    private static boolean solid(Block block) {
        return block.getType().isSolid() || block.isLiquid();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        preview.drop(event.getPlayer());
        sessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        TreeSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null && session.preview) preview.off(event.getPlayer(), true);
    }

    // ---- generating ---------------------------------------------------------------------------------------------------------

    /** Grows the tree of the player's settings on their base block and puts it in the world. */
    void generate(Player player) {
        TreeSession session = session(player);
        if (session.base == null || session.base.getWorld() == null) {
            player.sendMessage(PREFIX + "木を立てたい場所のブロックをクリックしてください。");
            return;
        }
        if (session.busy) {
            player.sendMessage(PREFIX + "前の木をまだ作っています。終わるまで待ってください。");
            return;
        }
        TreeSettings settings = session.settings.copy();
        // (with the preview on the seed is fixed: the tree put in the world is the one that was shown)
        long seed = settings.seed() != 0L ? settings.seed() : new Random().nextLong() & Long.MAX_VALUE;
        Location base = session.base.clone();
        boolean wasPreview = session.preview;
        if (wasPreview) preview.off(player, false);
        player.sendMessage(PREFIX + "作っています… (" + algorithmLabel(settings.algorithm()) + ")");
        build(player, session, settings, base, seed);
    }

    /**
     * Grows {@code settings} at {@code base} off the main thread (the terrain read first, where it is needed) and puts it in the world as one edit; the player's session is
     * busy until it is in. The settings' own button, the command and the placing tool all come here.
     */
    void build(Player player, TreeSession session, TreeSettings settings, Location base, long seed) {
        session.busy = true;
        TreeGround ground;
        try {
            ground = OrnateTree.readsGround(settings) ? TreeGround.sample(base, TreeGenerator.groundRadius(settings)) : null;
        } catch (RuntimeException exception) {
            ground = null;
        }
        TreeGround terrain = ground;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Long, TreePart> blocks;
            try {
                blocks = TreeGenerator.build(settings, seed, terrain);
            } catch (RuntimeException exception) {
                boolean tooBig = exception instanceof TreeVoxelizer.TooBig;
                if (!tooBig) plugin.getLogger().warning("tree generation failed: " + exception);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    session.busy = false;
                    player.sendMessage(PREFIX + (tooBig ? "大きすぎて作れません (" + TreeVoxelizer.LIMIT + " ブロックを超える)。設定を小さくしてください。" : "生成に失敗しました (ログを見てください)。"));
                });
                return;
            }
            Map<Long, TreePart> result = blocks;
            Bukkit.getScheduler().runTask(plugin, () -> place(player, session, settings, base, result, seed));
        });
    }

    /**
     * Puts the blocks in the world. The blocks that can be replaced are found a few thousand a tick (reading the world is the part that must stay on the main thread), then
     * all of them go through one WorldEdit edit session of the player, so that one //undo takes the tree back. Without WorldEdit they are set directly, as they are found.
     */
    private void place(Player player, TreeSession session, TreeSettings settings, Location base, Map<Long, TreePart> blocks, long seed) {
        int cap = plugin.getConfig().getInt("treetool.max-blocks", 60000);
        if (blocks.size() > cap) {
            session.busy = false;
            player.sendMessage(PREFIX + "ブロックが多すぎます (" + blocks.size() + " > " + cap + ")。設定 (高さ・枝の数・葉の大きさなど) を小さくしてください。");
            return;
        }
        World world = base.getWorld();
        List<Long> order = new ArrayList<>(blocks.keySet());
        order.sort((a, b) -> {
            int y = Integer.compare(TreeVoxelizer.unpackY(a), TreeVoxelizer.unpackY(b));
            if (y != 0) return y;
            int x = Integer.compare(TreeVoxelizer.unpackX(a), TreeVoxelizer.unpackX(b));
            return x != 0 ? x : Integer.compare(TreeVoxelizer.unpackZ(a), TreeVoxelizer.unpackZ(b));
        });
        Map<TreePart, BlockData> materials = new EnumMap<>(TreePart.class);
        for (TreePart part : TreePart.values()) materials.put(part, blockData(part, settings));
        if (blocks instanceof OrnateTree.Blocks ornate) ornate.useParts(materials);
        boolean replaceSolid = plugin.getConfig().getBoolean("treetool.replace-solid", false);
        int perTick = Math.max(200, plugin.getConfig().getInt("treetool.blocks-per-tick", 4000));
        List<TreeWorldEdit.Change> changes = new ArrayList<>();
        Map<TreePart, Integer> counts = new EnumMap<>(TreePart.class);

        new BukkitRunnable() {
            private int next;
            private int direct;

            @Override
            public void run() {
                try {
                    int end = Math.min(order.size(), next + perTick);
                    for (; next < end; next++) {
                        long key = order.get(next);
                        int x = base.getBlockX() + TreeVoxelizer.unpackX(key);
                        int y = base.getBlockY() + TreeVoxelizer.unpackY(key);
                        int z = base.getBlockZ() + TreeVoxelizer.unpackZ(key);
                        if (y < world.getMinHeight() || y >= world.getMaxHeight()) continue;
                        if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                        Block block = world.getBlockAt(x, y, z);
                        if (!replaceable(block, replaceSolid)) continue;
                        TreePart part = blocks.get(key);
                        counts.merge(part.shade() > 0 ? TreePart.LEAF : part.blockOf(), 1, Integer::sum);
                        // (fences and walls joined to the wood next to them, top slabs)
                        BlockData data = ThinTree.shape(materials.get(part), part, blocks, TreeVoxelizer.unpackX(key), TreeVoxelizer.unpackY(key), TreeVoxelizer.unpackZ(key));
                        if (worldEdit) {
                            changes.add(new TreeWorldEdit.Change(x, y, z, data));
                        } else {
                            block.setBlockData(data, false);
                            direct++;
                        }
                    }
                    if (next >= order.size()) {
                        cancel();
                        finish();
                    }
                } catch (RuntimeException exception) {
                    cancel();
                    session.busy = false;
                    plugin.getLogger().warning("tree placing stopped: " + exception);
                    player.sendMessage(PREFIX + "置いている途中で失敗しました (ログを見てください)。");
                }
            }

            private void finish() {
                int count = direct;
                boolean undoable = worldEdit;
                if (worldEdit) {
                    try {
                        count = TreeWorldEdit.apply(player, world, changes);
                    } catch (RuntimeException | LinkageError exception) {
                        plugin.getLogger().warning("Tree: WorldEdit failed, putting the tree without undo: " + exception);
                        for (TreeWorldEdit.Change change : changes) world.getBlockAt(change.x(), change.y(), change.z()).setBlockData(change.data(), false);
                        count = changes.size();
                        undoable = false;
                    }
                }
                session.busy = false;
                StringBuilder text = new StringBuilder(PREFIX + "できました (" + count + "ブロック, seed " + seed + "): ");
                for (TreePart part : TreePart.CHOICES) {
                    text.append(part.label()).append(' ').append(counts.getOrDefault(part, 0)).append("  ");
                }
                player.sendMessage(text.toString().trim());
                player.sendMessage(PREFIX + (undoable ? "気に入らなければ //undo (やり直しは //redo)。" : "WorldEdit がないので戻せません。") + "seed を固定すると同じ木になります。");
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    static boolean replaceable(Block block, boolean replaceSolid) {
        Material type = block.getType();
        if (type == Material.BEDROCK || type == Material.BARRIER) return false;
        if (replaceSolid) return true;
        if (block.isLiquid()) return false;
        return type.isAir() || block.isReplaceable();
    }

    /** Takes the last edit of the player back: the same as //undo (a tree, or any other WorldEdit edit). */
    void undo(Player player) {
        if (!worldEdit) {
            player.sendMessage(PREFIX + "WorldEdit がないので戻せません。");
            return;
        }
        player.performCommand("/undo");
    }

    void redo(Player player) {
        if (!worldEdit) {
            player.sendMessage(PREFIX + "WorldEdit がないので使えません。");
            return;
        }
        player.performCommand("/redo");
    }

    /** The word the algorithm is called by in messages, for the menu and the command. */
    static String algorithmLabel(String algorithm) {
        return switch (algorithm) {
            case "tiered" -> "段の枝 (針葉樹・雪)";
            case "palm" -> "ヤシ";
            case "thin" -> "細い木 (壁・フェンス・ハーフの枝)";
            case "silhouette" -> "樹形 (丸・しだれ・針葉樹・流れ・傘)";
            case "ornate" -> "装飾の木 (オーク・盆栽・トウヒ・白樺・柳・桜・アカシア)";
            default -> "樹冠 (フラクタル)";
        };
    }
}
