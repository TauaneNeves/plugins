package com.aeriaplugins.commands;

import com.aeriaplugins.plugins.Main;
import com.aeriaplugins.utils.ChatUtils;
import com.aeriaplugins.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class AeriaCommand implements CommandExecutor {

    private final Main plugin;

    public AeriaCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length > 0) {
            
            // --- COMANDO: /am reload ---
            if (a[0].equalsIgnoreCase("reload")) {
                if (s.hasPermission("aeriamenus.admin") || s.isOp()) {
                    plugin.getFileManager().loadAll();
                    plugin.setupBossBar();
                    s.sendMessage(ChatColor.GREEN + "[AeriaMenus] Configuração, mensagens e menus recarregados com sucesso!");
                } else {
                    s.sendMessage(ChatColor.RED + "Você não tem permissão para usar isso.");
                }
                return true;
            }
            
            // --- COMANDO: /am abrir <menu> [jogador] ---
            if (a[0].equalsIgnoreCase("abrir")) {
                if (s.hasPermission("aeriamenus.admin") || s.isOp()) {
                    if (a.length < 2) {
                        s.sendMessage(ChatColor.RED + "Uso correto: /am abrir <menu> [jogador]");
                        return true;
                    }
                    
                    String menuName = a[1];
                    Player target = null;

                    if (a.length >= 3) {
                        target = Bukkit.getPlayer(a[2]);
                        if (target == null) {
                            s.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline.");
                            return true;
                        }
                    } else {
                        if (s instanceof Player) {
                            target = (Player) s;
                        } else {
                            s.sendMessage(ChatColor.RED + "A consola precisa de especificar um jogador. Uso: /am abrir <menu> <jogador>");
                            return true;
                        }
                    }

                    openMenu(target, menuName, s);
                    return true;
                } else {
                    s.sendMessage(ChatColor.RED + "Você não tem permissão para usar isso.");
                    return true;
                }
            }
        }
        
        // --- MENU DE AJUDA DINÂMICO ---
        sendHelp(s);
        return true;
    }
    
    private void sendHelp(CommandSender s) {
        s.sendMessage("");
        s.sendMessage("§8§m                                                    ");
        s.sendMessage("                §x§0§0§d§4§f§f§lAERIA MENUS");
        s.sendMessage("          §7Central de Ajuda e Comandos");
        s.sendMessage("§8§m                                                    ");
        s.sendMessage("");
        s.sendMessage("  §8• §e/am help §8- §7Mostra esta mensagem.");
        
        // Mostra os comandos administrativos apenas para quem for OP / Admin
        if (s.hasPermission("aeriamenus.admin") || s.isOp()) {
            s.sendMessage("  §8• §e/am abrir <menu> [jogador] §8- §7Abre um menu específico.");
            s.sendMessage("  §8• §e/am reload §8- §7Recarrega todos os arquivos.");
        }
        
        s.sendMessage("");
        s.sendMessage("  §7Atalhos Rápidos:");
        s.sendMessage("  §8• §7Pode usar §e/<nome_do_menu> §7para os abrir rapidamente.");
        s.sendMessage("");
        s.sendMessage("  §7Feito com §c♥ §7para a sua rede!");
        s.sendMessage("§8§m                                                    ");
        s.sendMessage("");
    }

    // Método para processar a abertura do menu via comando
    private void openMenu(Player p, String k, CommandSender sender) {
        FileConfiguration menuConfig = plugin.getFileManager().getMenu(k);
        if (menuConfig == null) {
            sender.sendMessage(ChatColor.RED + "Erro: O menu '" + k + "' não existe na pasta menus!");
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
        
        if (sender != p) {
            sender.sendMessage(ChatColor.GREEN + "Menu '" + k + "' aberto para o jogador " + p.getName() + ".");
        }
    }
}