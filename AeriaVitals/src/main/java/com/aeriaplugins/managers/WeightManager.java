package com.aeriaplugins.managers;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WeightManager {

    private final AeriaVitals plugin;

    public WeightManager(AeriaVitals plugin) {
        this.plugin = plugin;
    }

    public void recalculateWeight(Player player) {
        PlayerData data = plugin.getPlayerData(player);
        double totalWeight = 0.0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;

            if (item.getType() == Material.BLACK_STAINED_GLASS_PANE && item.hasItemMeta()) {
                if (item.getItemMeta().getDisplayName().equals("§c[Bloqueado - Requer Mochila]")) {
                    continue;
                }
            }

            String typeName = item.getType().name();
            double itemWeight = plugin.getConfig().getDouble("weight.items." + typeName, 0.05);
            totalWeight += itemWeight * item.getAmount();
        }

        ItemStack[] customSlots = {
            data.getCustomHelmet(), 
            data.getCustomMascara(),
            data.getCustomChestplate(), 
            data.getCustomLeggings(), 
            data.getCustomBoots(),
            data.getCustomBackpack()
        };
        for (ItemStack item : customSlots) {
            if (item != null && !item.getType().isAir()) {
                String typeName = item.getType().name();
                double itemWeight = plugin.getConfig().getDouble("weight.items." + typeName, 0.05);
                totalWeight += itemWeight * item.getAmount();
            }
        }

        double maxWeight = plugin.getConfig().getDouble("weight.default-max-weight", 50.0);
        int tier = data.getBackpackTier();
        if (tier == 1) {
            maxWeight += plugin.getConfig().getDouble("weight.backpacks.leather_backpack.bonus-weight", 25.0);
        } else if (tier == 2) {
            maxWeight += plugin.getConfig().getDouble("weight.backpacks.military_backpack.bonus-weight", 60.0);
        }

        data.setCurrentWeight(totalWeight);
        data.setMaxWeight(maxWeight);
    }
}