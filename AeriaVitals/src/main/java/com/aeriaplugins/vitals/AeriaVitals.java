package com.aeriaplugins.vitals;

import com.aeriaplugins.commands.VitalsCommand;
import com.aeriaplugins.data.PlayerData;
import com.aeriaplugins.data.StorageManager;
import com.aeriaplugins.listeners.InfectionListener;
import com.aeriaplugins.listeners.MedicalListener;
import com.aeriaplugins.listeners.InventoryListener;
import com.aeriaplugins.listeners.PlayerListener;
import com.aeriaplugins.tasks.VitalsTickTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AeriaVitals extends JavaPlugin implements Listener {

    private static AeriaVitals instance;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private StorageManager storageManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.storageManager = new StorageManager(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new MedicalListener(this), this);
        getServer().getPluginManager().registerEvents(new InfectionListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        VitalsCommand vitalsCommand = new VitalsCommand(this);
        getCommand("vitals").setExecutor(vitalsCommand);
        getCommand("vitals").setTabCompleter(vitalsCommand);

        long tickRate = getConfig().getLong("settings.update-tick-rate", 20L);
        new VitalsTickTask(this).runTaskTimer(this, 0L, tickRate);

        getLogger().info("AeriaVitals ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        playerDataMap.clear();
        getLogger().info("AeriaVitals desativado.");
    }

    public static AeriaVitals getInstance() {
        return instance;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public Map<UUID, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData());
    }

    public void removePlayerData(Player player) {
        playerDataMap.remove(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        getPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerDataMap.remove(event.getPlayer().getUniqueId());
    }
}