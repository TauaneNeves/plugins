package com.aeriaplugins.vitals;

import com.aeriaplugins.commands.RadiationCommand;
import com.aeriaplugins.data.PlayerData;
import com.aeriaplugins.data.StorageManager;
import com.aeriaplugins.listeners.InventoryListener;
import com.aeriaplugins.tasks.SurvivalTickTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AeriaVitals extends JavaPlugin {

    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private StorageManager storageManager;
    private static AeriaVitals instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        this.storageManager = new StorageManager(this);

        // Contexto de inserção: Localize o bloco de registro de comandos dentro do onEnable()
        if (this.getCommand("radiacao") != null) {
            this.getCommand("radiacao").setExecutor(new RadiationCommand(this));
        }

        if (this.getCommand("vitals") != null) {
            this.getCommand("vitals").setExecutor(new com.aeriaplugins.commands.VitalsCommand(this));
        }

        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        new SurvivalTickTask(this).runTaskTimer(this, 20L, 20L);

        for (Player player : getServer().getOnlinePlayers()) {
            PlayerData data = new PlayerData();
            playerDataMap.put(player.getUniqueId(), data);
            storageManager.loadPlayerData(player, data);
        }
        
        getLogger().info("AeriaVitals iniciado com sucesso e sistema de sobrevivencia ativo!");
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (playerDataMap.containsKey(player.getUniqueId())) {
                storageManager.savePlayerData(player);
            }
        }
        playerDataMap.clear();
        getLogger().info("AeriaVitals desligado com sucesso!");
    }

    public PlayerData getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerDataMap.containsKey(uuid)) {
            PlayerData data = new PlayerData();
            playerDataMap.put(uuid, data);
            if (storageManager != null) {
                storageManager.loadPlayerData(player, data);
            }
        }
        return playerDataMap.get(uuid);
    }

    public Map<UUID, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }

    public static AeriaVitals getInstance() {
        return instance;
    }

    public void removePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerDataMap.containsKey(uuid)) {
            storageManager.savePlayerData(player);
            playerDataMap.remove(uuid);
        }
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }
}