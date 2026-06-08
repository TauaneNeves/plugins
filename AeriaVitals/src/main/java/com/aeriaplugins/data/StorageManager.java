package com.aeriaplugins.data;

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
            data.setTemperature(36.5);
            data.setCurrentWeight(0.0);
            data.setMaxWeight(50.0);
            data.setBleeding(false);
            data.setThirst(100.0);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        data.setInfection(config.getDouble("infection", 0.0));
        data.setImmunity(config.getDouble("immunity", 100.0));
        data.setTemperature(config.getDouble("temperature", 36.5));
        data.setCurrentWeight(config.getDouble("currentWeight", 0.0));
        data.setMaxWeight(config.getDouble("maxWeight", 50.0));
        data.setBleeding(config.getBoolean("bleeding", false));
        data.setThirst(config.getDouble("thirst", 100.0));
        
        data.setCustomHelmet(config.getItemStack("equipment.helmet"));
        data.setCustomMascara(config.getItemStack("equipment.mascara"));
        data.setCustomChestplate(config.getItemStack("equipment.chestplate"));
        data.setCustomLeggings(config.getItemStack("equipment.leggings"));
        data.setCustomBoots(config.getItemStack("equipment.boots"));
        data.setCustomBackpack(config.getItemStack("equipment.backpack"));
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
        config.set("bleeding", data.isBleeding());
        config.set("thirst", data.getThirst());
        
        config.set("equipment.helmet", data.getCustomHelmet());
        config.set("equipment.mascara", data.getCustomMascara());
        config.set("equipment.chestplate", data.getCustomChestplate());
        config.set("equipment.leggings", data.getCustomLeggings());
        config.set("equipment.boots", data.getCustomBoots());
        config.set("equipment.backpack", data.getCustomBackpack());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Não foi possível salvar os dados do jogador: " + player.getName());
            e.printStackTrace();
        }
    }
}