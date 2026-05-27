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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuEvents implements Listener {

    private final Main plugin;

    public MenuEvents(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        String fullCommand = e.getMessage().substring(1).toLowerCase();
        String baseCommand = fullCommand.split(" ")[0]; 

        for (Map.Entry<String, FileConfiguration> entry : plugin.getFileManager().getMenus().entrySet()) {
            FileConfiguration menuConfig = entry.getValue();
            
            if (menuConfig.contains("comando")) {
                List<String> aliases = new ArrayList<>();
                if (menuConfig.isString("comando")) {
                    aliases.add(menuConfig.getString("comando").toLowerCase());
                } else if (menuConfig.isList("comando")) {
                    for (String c : menuConfig.getStringList("comando")) {
                        aliases.add(c.toLowerCase());
                    }
                }

                if (aliases.contains(baseCommand)) {
                    e.setCancelled(true);
                    open(e.getPlayer(), entry.getKey());
                    return;
                }
            }
        }
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
                    String baseItemPath = "itens." + itemKey;
                    
                    if (menuConfig.contains(baseItemPath + ".permissao")) {
                        String permRequirida = menuConfig.getString(baseItemPath + ".permissao");
                        if (!player.hasPermission(permRequirida)) {
                            String msgErro = menuConfig.getString(baseItemPath + ".mensagem-erro", "&cVocê não tem permissão para acessar isto!");
                            player.sendMessage(ChatUtils.color(player, msgErro, plugin.getConfig().getBoolean("modulos.usar-placeholderapi")));
                            
                            String somErro = menuConfig.getString(baseItemPath + ".som-erro");
                            if (somErro != null) {
                                try { player.playSound(player.getLocation(), Sound.valueOf(somErro.toUpperCase()), 1f, 1f); } catch (Exception ignored) {}
                            }
                            return; 
                        }
                    }

                    // Verificação Inteligente de Dinheiro respeitando a Configuração Base
                    if (menuConfig.contains(baseItemPath + ".custo")) {
                        if (plugin.getConfig().getBoolean("modulos.usar-vault") && plugin.getEconomy() != null) {
                            double custo = menuConfig.getDouble(baseItemPath + ".custo");
                            if (!plugin.getEconomy().has(player, custo)) {
                                String msgErro = menuConfig.getString(baseItemPath + ".mensagem-erro-dinheiro", "&cVocê não tem saldo suficiente. Custa: $" + custo);
                                player.sendMessage(ChatUtils.color(player, msgErro, plugin.getConfig().getBoolean("modulos.usar-placeholderapi")));
                                
                                String somErro = menuConfig.getString(baseItemPath + ".som-erro");
                                if (somErro != null) {
                                    try { player.playSound(player.getLocation(), Sound.valueOf(somErro.toUpperCase()), 1f, 1f); } catch (Exception ignored) {}
                                }
                                return; 
                            } else {
                                plugin.getEconomy().withdrawPlayer(player, custo);
                            }
                        } else {
                            // Se o menu exigir custo, mas o administrador desativou o Vault na config, bloqueia e avisa
                            player.sendMessage(ChatColor.RED + "O sistema de economia (Vault) está desativado nas configurações deste servidor!");
                            return;
                        }
                    }

                    String acoesPath = baseItemPath + ".acoes";
                    if (menuConfig.contains(acoesPath)) {
                        execute(player, menuConfig.getStringList(acoesPath));
                    }
                }
            }
            return;
        }

        boolean isCustomMenu = false;
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        
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
            if (s.startsWith("comando: ")) {
                String cmd = s.substring(9).replace("%player%", p.getName());
                p.performCommand(cmd);
            }
            else if (s.startsWith("consola: ")) {
                String cmd = s.substring(9).replace("%player%", p.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            else if (s.startsWith("dinheiro: ")) {
                // Só executa se o Vault estiver ativo na config e carregado
                if (plugin.getConfig().getBoolean("modulos.usar-vault") && plugin.getEconomy() != null) {
                    try {
                        String[] parts = s.substring(10).split(" ");
                        double amount = Double.parseDouble(parts[1]);
                        if (parts[0].equalsIgnoreCase("dar")) {
                            plugin.getEconomy().depositPlayer(p, amount);
                        } else if (parts[0].equalsIgnoreCase("retirar")) {
                            plugin.getEconomy().withdrawPlayer(p, amount);
                        }
                    } catch (Exception ignored) {}
                }
            }
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