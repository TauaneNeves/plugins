package com.aeriaplugins.managers;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class TemperatureManager {

    private final AeriaVitals plugin;

    public TemperatureManager(AeriaVitals plugin) {
        this.plugin = plugin;
    }

    public void updateTemperature(Player player) {
        PlayerData data = plugin.getPlayerData(player);
        double targetTemp = plugin.getConfig().getDouble("temperature.base-body-temperature", 36.5);

        String biomeName = player.getLocation().getBlock().getBiome().name();
        double biomeMod = plugin.getConfig().getDouble("temperature.biomes." + biomeName, 0.0);
        targetTemp += biomeMod;

        if (player.getWorld().getTime() >= 13000 && player.getWorld().getTime() <= 23000) {
            targetTemp += plugin.getConfig().getDouble("temperature.modifiers.night-penalty", -1.0);
        }

        if (player.getWorld().hasStorm()) {
            targetTemp += plugin.getConfig().getDouble("temperature.modifiers.rain-penalty", -1.5);
        }

        if (player.getLocation().getBlock().getType() == Material.WATER) {
            targetTemp += plugin.getConfig().getDouble("temperature.modifiers.water-modifier", -2.5);
        }

        boolean nearFire = false;
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = player.getLocation().add(x, y, z).getBlock();
                    if (block.getType() == Material.FIRE || block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_FIRE) {
                        nearFire = true;
                        break;
                    }
                }
            }
        }

        if (nearFire) {
            targetTemp += plugin.getConfig().getDouble("temperature.modifiers.near-fire", 5.0);
        }

        double currentTemp = data.getTemperature();
        if (currentTemp < targetTemp) {
            data.setTemperature(Math.min(targetTemp, currentTemp + 0.2));
        } else if (currentTemp > targetTemp) {
            data.setTemperature(Math.max(targetTemp, currentTemp - 0.2));
        }
    }
}