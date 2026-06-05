package com.aeriaplugins.gps.listeners;

import com.aeriaplugins.gps.AeriaGPS;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import com.aeriaplugins.gps.map.GPSMapRenderer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Arrays;

public class GPSListener implements Listener {
    private final AeriaGPS plugin;

    public GPSListener(AeriaGPS plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getItemMeta().getPersistentDataContainer().has(plugin.getItemManager().gpsKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Você não pode instalar este equipamento no chão.");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        // Consumo de Papel de Coordenada
        if (item.getItemMeta().getPersistentDataContainer().has(plugin.getItemManager().paperKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
            String island = item.getItemMeta().getPersistentDataContainer().get(plugin.getItemManager().paperKey, PersistentDataType.STRING);
            
            PersistentDataContainer pdc = event.getPlayer().getPersistentDataContainer();
            String unlocked = pdc.getOrDefault(plugin.getItemManager().unlockedKey, PersistentDataType.STRING, "");
            
            if (Arrays.asList(unlocked.toLowerCase().split(";")).contains(island.toLowerCase())) {
                event.getPlayer().sendMessage(ChatColor.RED + "Você já possui os dados de '" + island + "' no seu GPS.");
                return;
            }
            
            unlocked += (unlocked.isEmpty() ? "" : ";") + island;
            pdc.set(plugin.getItemManager().unlockedKey, PersistentDataType.STRING, unlocked);
            
            item.setAmount(item.getAmount() - 1);
            event.getPlayer().sendMessage(ChatColor.GREEN + "Coordenadas de '" + island + "' transferidas para o seu GPS com sucesso!");
            return;
        }

        // Ativar GPS
        if (item.getItemMeta().getPersistentDataContainer().has(plugin.getItemManager().gpsKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            ativarRadar(event.getPlayer());
        }
    }

    private void ativarRadar(Player player) {
        ItemStack maoSecundaria = player.getInventory().getItemInOffHand();
        
        if (maoSecundaria != null && maoSecundaria.getType() == org.bukkit.Material.FILLED_MAP) {
            if (maoSecundaria.hasItemMeta() && maoSecundaria.getItemMeta().hasDisplayName() && maoSecundaria.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Tela LCD: AeriaGPS")) {
                player.getInventory().setItemInOffHand(new ItemStack(org.bukkit.Material.AIR));
                player.sendMessage(ChatColor.YELLOW + "Painel de navegação desligado.");
                return;
            }
        }

        if (maoSecundaria.getType() != org.bukkit.Material.AIR && maoSecundaria.getType() != org.bukkit.Material.FILLED_MAP) {
            player.sendMessage(ChatColor.RED + "Esvazie sua mão esquerda (Off-hand) para ligar o painel do GPS.");
            return;
        }

        ItemStack mapaRadar = new ItemStack(org.bukkit.Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapaRadar.getItemMeta();
        
        MapView view = Bukkit.createMap(player.getWorld());
        view.getRenderers().clear();
        view.addRenderer(new GPSMapRenderer());
        
        if (meta != null) {
            meta.setMapView(view);
            meta.setDisplayName(ChatColor.GREEN + "Tela LCD: AeriaGPS");
            mapaRadar.setItemMeta(meta);
        }

        player.getInventory().setItemInOffHand(mapaRadar);
        player.sendMessage(ChatColor.GREEN + "Painel de navegação ativado com sucesso.");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item != null && item.getType() == org.bukkit.Material.FILLED_MAP) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Tela LCD: AeriaGPS")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == org.bukkit.Material.FILLED_MAP) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Tela LCD: AeriaGPS")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        ItemStack itemParaMaoPrincipal = event.getMainHandItem();
        if (itemParaMaoPrincipal != null && itemParaMaoPrincipal.getType() == org.bukkit.Material.FILLED_MAP) {
            if (itemParaMaoPrincipal.hasItemMeta() && itemParaMaoPrincipal.getItemMeta().hasDisplayName() && itemParaMaoPrincipal.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Tela LCD: AeriaGPS")) {
                event.setCancelled(true);
            }
        }
    }
}