package com.aeriaplugins.vitals.managers;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.vitals.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
            String typeName = item.getType().name();
            double itemWeight = plugin.getConfig().getDouble("weight.items." + typeName, 0.05);
            totalWeight += itemWeight * item.getAmount();
        }

        double maxWeight = plugin.getConfig().getDouble("weight.default-max-weight", 50.0);
        ItemStack chest = player.getInventory().getChestplate();
        if (chest != null && chest.hasItemMeta()) {
            ItemMeta meta = chest.getItemMeta();
            if (meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();
                if (displayName.contains(plugin.getConfig().getString("weight.backpacks.leather_backpack.display-name", "Mochila de Couro"))) {
                    maxWeight += plugin.getConfig().getDouble("weight.backpacks.leather_backpack.bonus-weight", 25.0);
                } else if (displayName.contains(plugin.getConfig().getString("weight.backpacks.military_backpack.display-name", "Mochila Militar"))) {
                    maxWeight += plugin.getConfig().getDouble("weight.backpacks.military_backpack.bonus-weight", 60.0);
                }
            }
        }

        data.setCurrentWeight(totalWeight);
        data.setMaxWeight(maxWeight);
    }
}