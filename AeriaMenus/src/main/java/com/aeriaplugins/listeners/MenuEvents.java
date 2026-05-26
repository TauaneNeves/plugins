package com.aeriaplugins.listeners;

import com.aeriaplugins.plugins.Main;
import com.aeriaplugins.utils.ChatUtils;
import com.aeriaplugins.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;

public class MenuEvents implements Listener {

    private final Main plugin;

    public MenuEvents(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack i = e.getItem();
        if (i == null || !i.hasItemMeta()) return;
        Player p = e.getPlayer();

        ItemMeta meta = i.getItemMeta();
        if (meta.getPersistentDataContainer().has(plugin.getLobbyItemKey(), PersistentDataType.STRING)) {
            e.setCancelled(true);
            if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                String key = meta.getPersistentDataContainer().get(plugin.getLobbyItemKey(), PersistentDataType.STRING);
                String path = "itens-entrada." + key + ".acoes";
                if (plugin.getConfig().contains(path)) {
                    execute(p, plugin.getConfig().getStringList(path));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        
        ItemMeta meta = event.getCurrentItem().getItemMeta();
        
        // Verifica se clicou num item que pertence a um menu (criado pelo plugin)
        if (meta.getPersistentDataContainer().has(plugin.getMenuItemKey(), PersistentDataType.STRING)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            String data = meta.getPersistentDataContainer().get(plugin.getMenuItemKey(), PersistentDataType.STRING);
            String[] parts = data.split(";");
            if (parts.length == 2) {
                String menuKey = parts[0];
                String itemKey = parts[1];
                
                FileConfiguration menuConfig = plugin.getFileManager().getMenu(menuKey);
                if (menuConfig != null) {
                    String path = "itens." + itemKey + ".acoes";
                    if (menuConfig.contains(path)) {
                        execute(player, menuConfig.getStringList(path));
                    }
                }
            }
            return;
        }

        boolean isCustomMenu = false;
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        
        // Bloqueia cliques normais nos inventários abertos pelo plugin
        for (String menuKey : plugin.getFileManager().getMenus().keySet()) {
            FileConfiguration menuConfig = plugin.getFileManager().getMenu(menuKey);
            String menuTitle = ChatUtils.color((Player) event.getWhoClicked(), menuConfig.getString("titulo", ""), usePapi);
            if (event.getView().getTitle().equals(menuTitle)) {
                isCustomMenu = true;
                break;
            }
        }

        if (isCustomMenu) {
            event.setCancelled(true);
            return;
        }

        // Bloqueia mexer no inventário do jogador (se dar itens ao entrar estiver ativo)
        if (plugin.getConfig().getBoolean("modulos.dar-itens-ao-entrar") && !event.getWhoClicked().isOp()) {
            if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
                event.setCancelled(true);
            }
        }
    }

    private void execute(Player p, List<String> a) {
        if (a == null) return;
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        for (String s : a) {
            if (s.startsWith("comando: ")) p.performCommand(s.substring(9));
            else if (s.startsWith("mensagem: ")) p.sendMessage(ChatUtils.color(p, s.substring(10), usePapi));
            else if (s.startsWith("menu: ")) open(p, s.substring(6));
            else if (s.startsWith("especial: alternar_visibilidade")) toggleVisibility(p);
            else if (s.startsWith("servidor: ")) conectarServidor(p, s.substring(10));
            else if (s.startsWith("som: ")) {
                try { p.playSound(p.getLocation(), Sound.valueOf(s.substring(5).toUpperCase()), 1f, 1f); } catch(Exception ignored) {}
            }
            else if (s.equalsIgnoreCase("fechar")) p.closeInventory();
        }
    }

    private void conectarServidor(Player p, String serverName) {
        if (serverName.equalsIgnoreCase("configurar_aqui") || serverName.isEmpty()) {
            p.sendMessage(ChatColor.RED + "Ops! Este servidor ainda não foi configurado!");
            return;
        }
        
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(serverName); 
            p.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (Exception e) {
            p.sendMessage(ChatColor.RED + "Erro ao tentar conectar ao servidor.");
        }
    }

    private void toggleVisibility(Player p) {
        if (plugin.getPlayersHidden().contains(p.getUniqueId())) {
            plugin.getPlayersHidden().remove(p.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) p.showPlayer(plugin, target);
            p.sendMessage(ChatColor.GREEN + "Jogadores visíveis!");
        } else {
            plugin.getPlayersHidden().add(p.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) p.hidePlayer(plugin, target);
            p.sendMessage(ChatColor.RED + "Jogadores ocultos!");
        }
    }

    private void open(Player p, String k) {
        FileConfiguration menuConfig = plugin.getFileManager().getMenu(k);
        if (menuConfig == null) {
            p.sendMessage(ChatColor.RED + "Erro: O menu '" + k + "' não existe na pasta menus!");
            return;
        }
        
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        Inventory inv = Bukkit.createInventory(null, menuConfig.getInt("linhas") * 9, ChatUtils.color(p, menuConfig.getString("titulo"), usePapi));
        
        if (menuConfig.contains("itens")) {
            for (String iK : menuConfig.getConfigurationSection("itens").getKeys(false)) {
                String iP = "itens." + iK;
                // ParseItem agora usa o config do próprio menu
                ItemStack i = ItemUtils.parseItem(menuConfig, iP, p, usePapi);
                
                if (i != null) {
                    ItemMeta m = i.getItemMeta();
                    m.getPersistentDataContainer().set(plugin.getMenuItemKey(), PersistentDataType.STRING, k + ";" + iK);
                    i.setItemMeta(m);
                    inv.setItem(menuConfig.getInt(iP + ".slot"), i);
                }
            }
        }
        p.openInventory(inv);
    }
}