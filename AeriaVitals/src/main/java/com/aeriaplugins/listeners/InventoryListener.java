package com.aeriaplugins.vitals.listeners;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.vitals.managers.MedicalManager;
import com.aeriaplugins.vitals.managers.WeightManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private final AeriaVitals plugin;
    private final WeightManager weightManager;
    private final MedicalManager medicalManager;

    public InventoryListener(AeriaVitals plugin) {
        this.plugin = plugin;
        this.weightManager = new WeightManager(plugin);
        this.medicalManager = new MedicalManager(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            plugin.getServer().getScheduler().runTask(plugin, () -> weightManager.recalculateWeight(player));
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            plugin.getServer().getScheduler().runTask(plugin, () -> weightManager.recalculateWeight(player));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> weightManager.recalculateWeight(player));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null) {
            if (medicalManager.useMedicalItem(player, item)) {
                event.setCancelled(true);
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                weightManager.recalculateWeight(player);
            }
        }
    }
}