package com.aeriaplugins.gps.managers;

import com.aeriaplugins.gps.AeriaGPS;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class IslandManager {
    private final AeriaGPS plugin;
    private final Map<String, Location> islands = new HashMap<>();

    public IslandManager(AeriaGPS plugin) {
        this.plugin = plugin;
        carregarIlhas();
    }

    public void carregarIlhas() {
        islands.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("ilhas");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                double x = sec.getDouble(key + ".x");
                double z = sec.getDouble(key + ".z");
                String world = sec.getString(key + ".mundo", "world");
                islands.put(key, new Location(org.bukkit.Bukkit.getWorld(world), x, 0, z));
            }
        }
    }

    public void salvarIlha(String nome, Location loc) {
        islands.put(nome, loc);
        plugin.getConfig().set("ilhas." + nome + ".x", loc.getX());
        plugin.getConfig().set("ilhas." + nome + ".z", loc.getZ());
        plugin.getConfig().set("ilhas." + nome + ".mundo", loc.getWorld().getName());
        plugin.saveConfig();
    }

    public Map<String, Location> getIslands() {
        return islands;
    }
}