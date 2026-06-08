package com.aeriaplugins.vitals.managers;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.vitals.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TemperatureManager {

    private final AeriaVitals plugin;

    public TemperatureManager(AeriaVitals plugin) {
        this.plugin = plugin;
    }

    public void updateTemperature(Player player) {
        PlayerData data = plugin.getPlayerData(player);
        Location loc = player.getLocation();
        double tempTarget = 0.0;

        String biomeStr = loc.getBlock().getBiome().name();
        double biomeMod = plugin.getConfig().getDouble("temperature.biomes." + biomeStr, 0.0);
        tempTarget += biomeMod;

        if (loc.getWorld().getTime() >= 13000 && loc.getWorld().getTime() <= 23000) {
            tempTarget += plugin.getConfig().getDouble("temperature.modifiers.night-penalty", -0.5);
        }

        if (loc.getWorld().hasStorm() && loc.getBlockY() >= loc.getWorld().getHighestBlockYAt(loc)) {
            tempTarget += plugin.getConfig().getDouble("temperature.modifiers.rain-penalty", -0.8);
        }

        boolean nearFire = false;
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    if (b.getType() == Material.FIRE || b.getType() == Material.CAMPFIRE || b.getType() == Material.LAVA) {
                        nearFire = true;
                        break;
                    }
                }
            }
        }

        if (nearFire) {
            tempTarget += plugin.getConfig().getDouble("temperature.modifiers.near-fire", 2.5);
        }

        ItemStack chest = player.getInventory().getChestplate();
        if (chest != null && chest.getType() == Material.LEATHER_CHESTPLATE) {
            if (tempTarget < 0) {
                tempTarget += 1.5;
            }
        }

        double current = data.getTemperature();
        double rate = plugin.getConfig().getDouble("temperature.base-change-rate", 0.2);
        if (current < tempTarget) {
            data.setTemperature(current + rate);
        } else if (current > tempTarget) {
            data.setTemperature(current - rate);
        }
    }
}