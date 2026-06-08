package com.aeriaplugins.vitals.data;

import com.aeriaplugins.vitals.AeriaVitals;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class StorageManager {

    private final AeriaVitals plugin;
    private final File dataFolder;

    public StorageManager(AeriaVitals plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(dataFolder, uuid + ".yml");
        PlayerData data = plugin.getPlayerData(player);

        if (!file.exists()) {
            data.setInfection(0.0);
            data.setImmunity(100.0);
            data.setTemperature(0.0);
            data.setCurrentWeight(0.0);
            data.setMaxWeight(50.0);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        data.setInfection(config.getDouble("infection", 0.0));
        data.setImmunity(config.getDouble("immunity", 100.0));
        data.setTemperature(config.getDouble("temperature", 0.0));
        data.setCurrentWeight(config.getDouble("currentWeight", 0.0));
        data.setMaxWeight(config.getDouble("maxWeight", 50.0));
    }

    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(dataFolder, uuid + ".yml");
        PlayerData data = plugin.getPlayerDataMap().get(uuid);

        if (data == null) return;

        FileConfiguration config = new YamlConfiguration();
        config.set("infection", data.getInfection());
        config.set("immunity", data.getImmunity());
        config.set("temperature", data.getTemperature());
        config.set("currentWeight", data.getCurrentWeight());
        config.set("maxWeight", data.getMaxWeight());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Não foi possível salvar os dados do jogador: " + player.getName());
            e.printStackTrace();
        }
    }
}