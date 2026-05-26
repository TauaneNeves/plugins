package com.aeriaplugins.commands;

import com.aeriaplugins.plugins.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AeriaCommand implements CommandExecutor {

    private final Main plugin;

    public AeriaCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
            if (s.isOp()) {
                // Agora o reload carrega tudo (config, messages e menus)
                plugin.getFileManager().loadAll();
                plugin.setupBossBar();
                s.sendMessage(ChatColor.GREEN + "[AeriaMenus] Configuração, mensagens e menus recarregados com sucesso!");
            } else {
                s.sendMessage(ChatColor.RED + "Você não tem permissão para usar isso.");
            }
        } else {
            s.sendMessage("§b§lAERIA MENUS §8- §7Central de Ajuda");
            s.sendMessage("§8--------------------------------------------------");
            s.sendMessage("§e/am reload §8- §7Recarrega todos os arquivos (Apenas OP)");
            s.sendMessage("§8--------------------------------------------------");
        }
        return true;
    }
}