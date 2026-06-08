package com.aeriaplugins.hordes.commands;

import com.aeriaplugins.hordes.AeriaHordes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HordesCommand implements CommandExecutor, TabCompleter {

    private final AeriaHordes plugin;

    public HordesCommand(AeriaHordes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§e========= §6AeriaHordes - Guia de Ajuda §e=========");
            sender.sendMessage("§b/hordes status §7- Verifica o estado atual do sistema de hordas.");
            sender.sendMessage("§b/hordes help §7- Exibe este menu explicativo.");
            
            if (sender.hasPermission("aeriahordes.admin")) {
                sender.sendMessage("§e---------------- Administrador ----------------");
                sender.sendMessage("§c/hordes start §7- Força o início imediato da Noite da Horda.");
                sender.sendMessage("§c/hordes stop §7- Força o encerramento da Noite da Horda.");
                sender.sendMessage("§c/hordes clear §7- Remove instantaneamente todos os zumbis de horda.");
                sender.sendMessage("§c/hordes reload §7- Recarrega as configurações do arquivo config.yml.");
                sender.sendMessage("§e---------------- Funcionamento ----------------");
                sender.sendMessage("§7• §fSistema de Ruído: §7Correr, quebrar blocos ou disparar armas acumula");
                sender.sendMessage("§7  pontos de barulho no chunk. Ao atingir o limite, uma mini-horda surge.");
                sender.sendMessage("§7• §fMecânica de Cerco: §7Zumbis de horda atacam e destroem blocos");
                sender.sendMessage("§7  configurados (barricadas/portas) se o alvo estiver atrás deles.");
                sender.sendMessage("§7• §fRastreamento: §7IA estendida para perseguição de até 100 blocos.");
            }
            sender.sendMessage("§e=========================================");
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            boolean active = plugin.getWaveManager().isHordeNightActive();
            int count = plugin.getHordeManager().getHordeZombies().size();
            sender.sendMessage("§e=== Status do Evento ===");
            sender.sendMessage("§7Noite da Horda Ativa: " + (active ? "§aSim" : "§cNão"));
            sender.sendMessage("§7Zumbis de Horda Vivos: §6" + count);
            return true;
        }

        if (!sender.hasPermission("aeriahordes.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar subcomandos administrativos.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (plugin.getWaveManager().isHordeNightActive()) {
                    sender.sendMessage("§cA Noite da Horda já está em andamento.");
                } else {
                    plugin.getWaveManager().startHordeNight();
                    sender.sendMessage("§a[AeriaHordes] Evento iniciado e BossBar aplicada.");
                }
                break;
            case "stop":
                if (!plugin.getWaveManager().isHordeNightActive()) {
                    sender.sendMessage("§cA Noite da Horda não está ativa no momento.");
                } else {
                    plugin.getWaveManager().endHordeNight();
                    sender.sendMessage("§c[AeriaHordes] Evento finalizado e entidades limpas.");
                }
                break;
            case "clear":
                int removed = plugin.getHordeManager().clearAllZombies();
                sender.sendMessage("§a[AeriaHordes] " + removed + " zumbis de horda foram removidos do servidor.");
                break;
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§a[AeriaHordes] Configurações recarregadas com sucesso.");
                break;
            default:
                sender.sendMessage("§cSubcomando inválido. Utilize /hordes help para ver as opções.");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("help");
            completions.add("status");
            if (sender.hasPermission("aeriahordes.admin")) {
                completions.add("start");
                completions.add("stop");
                completions.add("clear");
                completions.add("reload");
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return completions;
    }
}