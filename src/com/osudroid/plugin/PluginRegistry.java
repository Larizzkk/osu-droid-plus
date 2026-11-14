package com.osudroid.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry for all loaded plugins.
 * Currently simple — just stores the list. Can be extended later
 * for mod registration, mover registration, etc.
 */
public class PluginRegistry {

    private static PluginRegistry instance;
    private final List<LuaPlugin> plugins = new ArrayList<>();

    public static PluginRegistry getInstance() {
        if (instance == null) instance = new PluginRegistry();
        return instance;
    }

    public void register(LuaPlugin plugin) {
        plugins.add(plugin);
    }

    public List<LuaPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public void clear() {
        plugins.clear();
    }
}
