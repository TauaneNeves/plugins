package com.aeriaplugins.listeners;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MedicalListener implements Listener {

    private final AeriaVitals plugin;

    public MedicalListener(AeriaVitals plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        if (!item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) {
            return;
        }

        int cmd = meta.getCustomModelData();
        String itemKey = null;

        org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom-items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (section.getInt(key + ".custom-model-data") == cmd) {
                    itemKey = key;
                    break;
                }
            }
        }

        if (itemKey != null) {
            event.setCancelled(true);
            PlayerData data = plugin.getPlayerData(player);

            double infReduction = plugin.getConfig().getDouble("custom-items." + itemKey + ".infection-reduction", 0.0);
            double immBoost = plugin.getConfig().getDouble("custom-items." + itemKey + ".immunity-boost", 0.0);

            data.setInfection(Math.max(0.0, data.getInfection() - infReduction));
            data.setImmunity(Math.min(100.0, data.getImmunity() + immBoost));

            if (itemKey.equalsIgnoreCase("antibiotic")) {
                data.setHasCold(false);
            }

            String useMessage = plugin.getConfig().getString("custom-items." + itemKey + ".use-message", "");
            if (useMessage != null && !useMessage.isEmpty()) {
                player.sendMessage(useMessage.replace("&", "§"));
            }

            item.setAmount(item.getAmount() - 1);
        }
    }
}