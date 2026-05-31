package com.aeriaplugins.raids.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RaidCommand implements CommandExecutor {

    public static ItemStack getCadeadoItem() {
        ItemStack item = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Cadeado de Segredo");
            meta.setCustomModelData(1);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player jogador = (Player) sender;

        if (!jogador.isOp()) {
            jogador.sendMessage(ChatColor.RED + "Sem permissão.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            jogador.sendMessage(ChatColor.DARK_GRAY + "=== " + ChatColor.GOLD + "AeriaRaids - Comandos" + ChatColor.DARK_GRAY + " ===");
            jogador.sendMessage(ChatColor.YELLOW + "/aeriaraids give " + ChatColor.GRAY + "- Gera um Cadeado de Segredo no inventário.");
            jogador.sendMessage(ChatColor.YELLOW + "/aeriaraids help " + ChatColor.GRAY + "- Exibe esta lista de comandos.");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            jogador.getInventory().addItem(getCadeadoItem());
            jogador.sendMessage(ChatColor.GREEN + "Cadeado de Segredo recebido.");
            return true;
        }

        jogador.sendMessage(ChatColor.RED + "Comando não encontrado. Use /aeriaraids help.");
        return true;
    }
}