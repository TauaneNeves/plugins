package com.aeriaplugins.commands;

import com.aeriaplugins.plugins.Main;
import com.aeriaplugins.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.ArrayList;
import java.util.List;

public class AeriaCommand implements CommandExecutor {
    private final Main plugin;

    public AeriaCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        Player player = (sender instanceof Player) ? (Player) sender : null;

        if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("ajuda")) {
            enviarAjudaDetalhada(sender, player, usePapi);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("aeriamenus.admin")) {
                sender.sendMessage(ChatUtils.color(player, "&cVocê não possui permissão!", usePapi));
                return true;
            }
            plugin.getFileManager().loadAll();
            sender.sendMessage(ChatUtils.color(player, "<gradient:#00d4ff:#00ff55>&l[AeriaMenus]&r &aConfigurações recarregadas com sucesso!", usePapi));
            return true;
        }

        if (args[0].equalsIgnoreCase("abrir") || args[0].equalsIgnoreCase("open")) {
            if (!sender.hasPermission("aeriamenus.admin")) {
                sender.sendMessage(ChatUtils.color(player, "&cVocê não possui permissão!", usePapi));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatUtils.color(player, "&c&lERRO! &7Uso: &e/am abrir <nome_do_menu> [jogador]", usePapi));
                return true;
            }
            String menuKey = args[1];
            FileConfiguration menuConfig = plugin.getFileManager().getMenu(menuKey);
            if (menuConfig == null) {
                sender.sendMessage(ChatUtils.color(player, "&c&lERRO! &7Menu não encontrado.", usePapi));
                return true;
            }
            Player alvo = player;
            if (args.length >= 3) {
                alvo = Bukkit.getPlayer(args[2]);
                if (alvo == null) {
                    sender.sendMessage(ChatUtils.color(player, "&c&lERRO! &7Jogador offline.", usePapi));
                    return true;
                }
            }
            if (alvo == null) {
                sender.sendMessage(ChatColor.RED + "Especifique um jogador alvo via Console!");
                return true;
            }

            int linhas = menuConfig.getInt("linhas", 3);
            String titulo = ChatUtils.color(alvo, menuConfig.getString("titulo", "Menu"), usePapi);
            Inventory inv = Bukkit.createInventory(null, linhas * 9, titulo);
            
            if (menuConfig.contains("itens")) {
                for (String iK : menuConfig.getConfigurationSection("itens").getKeys(false)) {
                    String iP = "itens." + iK;
                    org.bukkit.inventory.ItemStack i = com.aeriaplugins.utils.ItemUtils.parseItem(menuConfig, iP, alvo, usePapi);
                    if (i != null) {
                        org.bukkit.inventory.meta.ItemMeta m = i.getItemMeta();
                        List<String> lore = m.hasLore() ? m.getLore() : new ArrayList<>();
                        lore.add("§0menu:" + menuKey + ";" + iK);
                        m.setLore(lore);
                        i.setItemMeta(m);
                        inv.setItem(menuConfig.getInt(iP + ".slot"), i);
                    }
                }
            }
            alvo.openInventory(inv);
            sender.sendMessage(ChatUtils.color(player, "<gradient:#00d4ff:#00ff55>&l[AeriaMenus]&r &7Menu aberto para &f" + alvo.getName(), usePapi));
            return true;
        }
        return true;
    }

    private void enviarAjudaDetalhada(CommandSender sender, Player player, boolean usePapi) {
        sender.sendMessage(ChatUtils.color(player, "&8&m                                                                     ", usePapi));
        sender.sendMessage(ChatUtils.color(player, "       <gradient:#00d4ff:#00ff55>&lCENTRAL DE AJUDA — AERIAMENUS v1.0.0</gradient>", usePapi));
        sender.sendMessage(ChatUtils.color(player, " &7Gerencie seus Lobbies, Scoreboards e Menus de forma hibrida.", usePapi));
        sender.sendMessage(ChatUtils.color(player, "&8&m                                                                     ", usePapi));
        sender.sendMessage(ChatUtils.color(player, "  &f/am ajuda &8- &7Exibe os comandos disponíveis.", usePapi));
        sender.sendMessage(ChatUtils.color(player, "  &f/am reload &8- &7Recarrega todas as configurações.", usePapi));
        sender.sendMessage(ChatUtils.color(player, "  &f/am abrir <menu> [jogador] &8- &7Abre um menu remoto.", usePapi));
        sender.sendMessage(ChatUtils.color(player, "&8&m                                                                     ", usePapi));
    }
}