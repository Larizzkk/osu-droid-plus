package com.osudroid.plugin;

import android.util.Log;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inter-plugin event bus. Plugins can emit named events and subscribe to them.
 * Thread-safe: emit() may be called from any thread, listeners fire on the caller's thread.
 */
public class PluginEventBus {

    private static final String TAG = "PluginEventBus";
    private static final Map<String, List<Listener>> listeners = new HashMap<>();

    private static class Listener {
        final LuaPlugin plugin;
        final Globals sandbox;
        final LuaValue callback;

        Listener(LuaPlugin plugin, Globals sandbox, LuaValue callback) {
            this.plugin = plugin;
            this.sandbox = sandbox;
            this.callback = callback;
        }
    }

    public static void on(LuaPlugin plugin, Globals sandbox, String eventName, LuaValue callback) {
        if (eventName == null || eventName.isEmpty() || callback == null || !callback.isfunction()) return;
        List<Listener> list = listeners.computeIfAbsent(eventName, k -> new ArrayList<>());
        list.add(new Listener(plugin, sandbox, callback));
    }

    public static void emit(String eventName, LuaValue... args) {
        if (eventName == null || eventName.isEmpty()) return;
        List<Listener> list = listeners.get(eventName);
        if (list == null) return;

        for (int i = list.size() - 1; i >= 0; i--) {
            Listener l = list.get(i);
            try {
                l.callback.invoke(args);
            } catch (Exception e) {
                Log.w(TAG, "Event listener error on '" + eventName + "': " + e.getMessage());
            }
        }
    }

    public static void off(LuaPlugin plugin, String eventName) {
        List<Listener> list = listeners.get(eventName);
        if (list == null) return;
        list.removeIf(l -> l.plugin == plugin);
        if (list.isEmpty()) listeners.remove(eventName);
    }

    public static void offAll(LuaPlugin plugin) {
        listeners.values().removeIf(list -> {
            list.removeIf(l -> l.plugin == plugin);
            return list.isEmpty();
        });
    }

    public static void clear() {
        listeners.clear();
    }
}
