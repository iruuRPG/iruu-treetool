package io.github.iruurpg.treetool;

import org.bukkit.plugin.java.JavaPlugin;

/** IruuTreeTool: a tool for building trees block by block (a Paper plugin). All of it lives in {@link ProceduralModule}; this class only starts and stops it. */
public final class IruuTreeTool extends JavaPlugin {

    private ProceduralModule tool;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        tool = ProceduralModule.start(this);
    }

    @Override
    public void onDisable() {
        if (tool != null) tool.stop();
        tool = null;
    }
}
