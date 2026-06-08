package com.aeriaplugins.listeners;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import com.aeriaplugins.managers.MedicalManager;
import com.aeriaplugins.managers.WeightManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;

public class InventoryListener implements Listener {

    private final AeriaVitals plugin;
    private final WeightManager weightManager;
    private final MedicalManager medicalManager;

    public InventoryListener(AeriaVitals plugin) {
        this.plugin = plugin;
        this.weightManager = new WeightManager(plugin);
        this.medicalManager = new MedicalManager(plugin);
    }

    private ItemStack createVisualPlaceholder(Material material, String slotName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7[Vazio] " + slotName);
            meta.setLore(Collections.singletonList("§8Clique com o equipamento aqui para vestir"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openEquipmentMenu(Player player) {
        Inventory gui = org.bukkit.Bukkit.createInventory(null, 36, "§8Equipamentos e Mochila");
        
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName("§7 ");
            glass.setItemMeta(glassMeta);
        }
        
        for (int i = 0; i < 36; i++) {
            gui.setItem(i, glass);
        }
        
        PlayerData data = plugin.getPlayerData(player);
        
        gui.setItem(2, data.getCustomMascara() != null ? data.getCustomMascara() : createVisualPlaceholder(Material.WHITE_STAINED_GLASS_PANE, "Slot de Máscara"));
        gui.setItem(3, data.getCustomHelmet() != null ? data.getCustomHelmet() : createVisualPlaceholder(Material.CHAINMAIL_HELMET, "Slot de Cabeça / Capacete"));
        gui.setItem(12, data.getCustomChestplate() != null ? data.getCustomChestplate() : createVisualPlaceholder(Material.IRON_CHESTPLATE, "Slot de Peitoral / Colete"));
        gui.setItem(21, data.getCustomLeggings() != null ? data.getCustomLeggings() : createVisualPlaceholder(Material.IRON_LEGGINGS, "Slot de Calça"));
        gui.setItem(30, data.getCustomBoots() != null ? data.getCustomBoots() : createVisualPlaceholder(Material.IRON_BOOTS, "Slot de Bota"));
        
        gui.setItem(14, data.getCustomBackpack() != null ? data.getCustomBackpack() : createVisualPlaceholder(Material.CHEST, "Slot de Mochila"));
        
        player.openInventory(gui);
    }

    public void updateBarriers(Player player) {
        PlayerData data = plugin.getPlayerData(player);
        int tier = data.getBackpackTier();

        ItemStack barrier = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = barrier.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c[Bloqueado - Requer Mochila]");
            barrier.setItemMeta(meta);
        }

        for (int i = 18; i <= 35; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.BLACK_STAINED_GLASS_PANE && item.hasItemMeta()) {
                if (item.getItemMeta().getDisplayName().equals("§c[Bloqueado - Requer Mochila]")) {
                    player.getInventory().setItem(i, null);
                }
            }
        }

        if (tier == 0) {
            for (int i = 18; i <= 35; i++) {
                if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) {
                    player.getInventory().setItem(i, barrier);
                }
            }
        } else if (tier == 1) {
            for (int i = 27; i <= 35; i++) {
                if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) {
                    player.getInventory().setItem(i, barrier);
                }
            }
        }
    }

    private void updateVanillaArmor(Player player, PlayerData data) {
        player.getInventory().setHelmet(data.getCustomHelmet());
        player.getInventory().setChestplate(data.getCustomChestplate());
        player.getInventory().setLeggings(data.getCustomLeggings());
        player.getInventory().setBoots(data.getCustomBoots());
    }

    @EventHandler
    public void onZombieDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager().getType() == EntityType.ZOMBIE) {
            Player player = (Player) event.getEntity();
            PlayerData data = plugin.getPlayerData(player);
            if (Math.random() < 0.35) {
                data.setBleeding(true);
                player.sendMessage("§cO zumbi rasgou sua pele! Você começou a sangrar.");
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();
            if (event.getDamage() > 4.0) {
                PlayerData data = plugin.getPlayerData(player);
                data.setBleeding(true);
                player.sendMessage("§cO impacto da queda fraturou feio e você começou a sangrar!");
            }
        }
    }

    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
        openEquipmentMenu(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            player.sendMessage("§cVocê só pode equipar proteções usando o menu do [F]!");
            return;
        }

        if (event.isShiftClick() && event.getCurrentItem() != null) {
            String name = event.getCurrentItem().getType().name();
            if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS")) {
                event.setCancelled(true);
                player.sendMessage("§cVocê só pode equipar proteções usando o menu do [F]!");
                return;
            }
        }

        if (title.equals("§8Equipamentos e Mochila")) {
            event.setCancelled(true);
            
            Inventory clickedInv = event.getClickedInventory();
            if (clickedInv == null) return;

            PlayerData data = plugin.getPlayerData(player);

            // --- INTERAÇÃO 1: CLIQUE DIRETO NO SEU INVENTÁRIO (EQUIPAR DIRETO) ---
            if (clickedInv == event.getView().getBottomInventory()) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

                String matName = clickedItem.getType().name();
                String leatherName = org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("weight.backpacks.leather_backpack.display-name", "Mochila de Couro"));
                String militaryName = org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("weight.backpacks.military_backpack.display-name", "Mochila Militar"));

                boolean isBackpack = false;
                if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                    String displayName = clickedItem.getItemMeta().getDisplayName();
                    if (displayName.contains(leatherName) || displayName.contains(militaryName)) {
                        isBackpack = true;
                    }
                }

                int targetSlot = -1;
                if (isBackpack) targetSlot = 14;
                else if (matName.contains("HELMET")) targetSlot = 3;
                else if (matName.contains("CHESTPLATE")) targetSlot = 12;
                else if (matName.contains("LEGGINGS")) targetSlot = 21;
                else if (matName.contains("BOOTS")) targetSlot = 30;
                else if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName() && 
                         clickedItem.getItemMeta().getDisplayName().toLowerCase().contains("mascara")) {
                    targetSlot = 2;
                }

                if (targetSlot != -1) {
                    ItemStack storedItem = getSlotData(targetSlot, data);
                    setSlotData(targetSlot, data, clickedItem.clone());
                    event.setCurrentItem(storedItem != null ? storedItem.clone() : null);

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        updateVanillaArmor(player, data);
                        openEquipmentMenu(player);
                        updateBarriers(player);
                        weightManager.recalculateWeight(player);
                    });
                }
                return;
            }

            // --- INTERAÇÃO 2: CLIQUE DENTRO DA GUI DE EQUIPAMENTOS (DESEQUIPAR DIRETO) ---
            int slot = event.getSlot();
            if (slot != 2 && slot != 3 && slot != 12 && slot != 21 && slot != 30 && slot != 14) return;

            ItemStack currentItem = event.getCurrentItem();
            ItemStack cursor = event.getCursor();

            boolean isPlaceholder = currentItem != null && currentItem.hasItemMeta() && 
                                    currentItem.getItemMeta().getDisplayName() != null &&
                                    currentItem.getItemMeta().getDisplayName().startsWith("§7[Vazio]");

            ItemStack storedItem = getSlotData(slot, data);

            if (cursor != null && cursor.getType() != Material.AIR) {
                String matName = cursor.getType().name();
                
                String leatherName = org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("weight.backpacks.leather_backpack.display-name", "Mochila de Couro"));
                String militaryName = org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("weight.backpacks.military_backpack.display-name", "Mochila Militar"));

                boolean isBackpack = false;
                if (cursor.hasItemMeta() && cursor.getItemMeta().hasDisplayName()) {
                    String displayName = cursor.getItemMeta().getDisplayName();
                    if (displayName.contains(leatherName) || displayName.contains(militaryName)) {
                        isBackpack = true;
                    }
                }

                if (isBackpack && slot != 14) {
                    player.sendMessage("§cMochilas só podem ser equipadas no slot de Mochila!");
                    return;
                }

                if (slot == 3 && !matName.contains("HELMET")) {
                    player.sendMessage("§cEste slot aceita apenas Capacetes!");
                    return;
                }
                if (slot == 12 && !matName.contains("CHESTPLATE")) {
                    player.sendMessage("§cEste slot aceita apenas Coletes / Peitorais!");
                    return;
                }
                if (slot == 21 && !matName.contains("LEGGINGS")) {
                    player.sendMessage("§cEste slot aceita apenas Calças!");
                    return;
                }
                if (slot == 30 && !matName.contains("BOOTS")) {
                    player.sendMessage("§cEste slot aceita apenas Botas!");
                    return;
                }
                if (slot == 2 && (matName.contains("HELMET") || matName.contains("CHESTPLATE") || matName.contains("LEGGINGS") || matName.contains("BOOTS"))) {
                    player.sendMessage("§cEste slot aceita apenas Máscaras!");
                    return;
                }

                event.setCursor(isPlaceholder ? null : storedItem.clone());
                setSlotData(slot, data, cursor.clone());
            } else {
                if (isPlaceholder) return;
                
                // Envia o item direto de volta para os quadradinhos do inventário inferior
                HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(storedItem.clone());
                if (!leftOver.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), storedItem);
                }
                setSlotData(slot, data, null);
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                updateVanillaArmor(player, data);
                openEquipmentMenu(player);
                updateBarriers(player);
                weightManager.recalculateWeight(player);
            });
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.getType() == Material.BLACK_STAINED_GLASS_PANE && clicked.hasItemMeta()) {
            if (clicked.getItemMeta().getDisplayName().equals("§c[Bloqueado - Requer Mochila]")) {
                event.setCancelled(true);
                return;
            }
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> weightManager.recalculateWeight(player));
    }

    private ItemStack getSlotData(int slot, PlayerData data) {
        if (slot == 2) return data.getCustomMascara();
        if (slot == 3) return data.getCustomHelmet();
        if (slot == 12) return data.getCustomChestplate();
        if (slot == 21) return data.getCustomLeggings();
        if (slot == 30) return data.getCustomBoots();
        if (slot == 14) return data.getCustomBackpack();
        return null;
    }

    private void setSlotData(int slot, PlayerData data, ItemStack item) {
        if (slot == 2) data.setCustomMascara(item);
        if (slot == 3) data.setCustomHelmet(item);
        if (slot == 12) data.setCustomChestplate(item);
        if (slot == 21) data.setCustomLeggings(item);
        if (slot == 30) data.setCustomBoots(item);
        if (slot == 14) data.setCustomBackpack(item);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals("§8Equipamentos e Mochila")) {
            event.setCancelled(true);
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            ItemStack item = event.getView().getItem(rawSlot);
            if (item != null && item.getType() == Material.BLACK_STAINED_GLASS_PANE && item.hasItemMeta()) {
                if (item.getItemMeta().getDisplayName().equals("§c[Bloqueado - Requer Mochila]")) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            plugin.getServer().getScheduler().runTask(plugin, () -> weightManager.recalculateWeight(player));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateBarriers(player);
            weightManager.recalculateWeight(player);
            updateVanillaArmor(player, plugin.getPlayerData(player));
        }, 2L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateBarriers(player);
            weightManager.recalculateWeight(player);
            updateVanillaArmor(player, plugin.getPlayerData(player));
        }, 2L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData data = plugin.getPlayerData(player);

        event.getDrops().removeIf(item -> item != null && item.getType() == Material.BLACK_STAINED_GLASS_PANE && item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals("§c[Bloqueado - Requer Mochila]"));

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
                event.getDrops().add(item);
            }
        }

        data.setCustomHelmet(null);
        data.setCustomMascara(null);
        data.setCustomChestplate(null);
        data.setCustomLeggings(null);
        data.setCustomBoots(null);
        data.setCustomBackpack(null);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> weightManager.recalculateWeight(player));
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> weightManager.recalculateWeight(player));
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
        
        if (item != null && (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
            String name = item.getType().name();
            if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS")) {
                event.setCancelled(true);
                player.sendMessage("§cVocê só pode equipar proteções usando o menu do [F]!");
                return;
            }
            
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