package com.aeriaplugins.vitals;

import com.aeriaplugins.vitals.commands.VitalsCommand;
import com.aeriaplugins.vitals.data.PlayerData;
import com.aeriaplugins.vitals.data.StorageManager;
import com.aeriaplugins.vitals.listeners.EnvironmentListener;
import com.aeriaplugins.vitals.listeners.InventoryListener;
import com.aeriaplugins.vitals.listeners.PlayerListener;
import com.aeriaplugins.vitals.tasks.VitalsTickTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AeriaVitals extends JavaPlugin {

    private static AeriaVitals instance;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private StorageManager storageManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.storageManager = new StorageManager(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new EnvironmentListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        
        getCommand("aeriavitals").setExecutor(new VitalsCommand(this));

        new VitalsTickTask(this).runTaskTimer(this, 0L, getConfig().getLong("settings.update-tick-rate", 20L));

        for (Player player : Bukkit.getOnlinePlayers()) {
            storageManager.loadPlayerData(player);
        }

        getLogger().info("AeriaVitals ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            storageManager.savePlayerData(player);
        }
        playerDataMap.clear();
        getLogger().info("AeriaVitals desativado.");
    }

    public static AeriaVitals getInstance() {
        return instance;
    }

    public Map<UUID, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData());
    }

    public void removePlayerData(Player player) {
        playerDataMap.remove(player.getUniqueId());
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }
}