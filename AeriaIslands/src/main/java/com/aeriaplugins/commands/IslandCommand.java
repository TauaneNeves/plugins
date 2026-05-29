package com.aeriaplugins.commands;

import com.aeriaplugins.plugins.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IslandCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores em jogo utilizam este comando.");
            return true;
        }

        Player jogador = (Player) sender;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("create")) {
                Location ilhaExistente = Main.getInstance().getIslandStorage().getLocalizacaoIlha(jogador.getUniqueId());
                
                if (ilhaExistente != null) {
                    jogador.sendMessage(ChatColor.RED + "Voce ja possui uma ilha tactica registrada! Use /is go");
                    return true;
                }

                jogador.sendMessage(ChatColor.GREEN + "🔨 Calculando espaco aereo e gerando ilha base...");
                Location localNovo = Main.getInstance().getGridManager().calcularProximaCoordenada();
                
                gerarEstruturaFisica(localNovo);
                Main.getInstance().getIslandStorage().salvarIlha(jogador.getUniqueId(), localNovo);
                
                jogador.teleport(localNovo.add(0.5, 1, 0.5));
                jogador.sendMessage(ChatColor.AQUA + "✨ Ilha estabelecida! Espaco aereo liberado para futuros veiculos.");
                return true;
            }

            if (args[0].equalsIgnoreCase("go")) {
                Location localIlha = Main.getInstance().getIslandStorage().getLocalizacaoIlha(jogador.getUniqueId());
                
                if (localIlha == null) {
                    jogador.sendMessage(ChatColor.RED + "❌ Voce nao possui uma ilha. Digite /is create");
                    return true;
                }

                jogador.teleport(localIlha.add(0.5, 1, 0.5));
                jogador.sendMessage(ChatColor.GREEN + "Teleportado para a sua base operacional.");
                return true;
            }
        }

        jogador.sendMessage(ChatColor.YELLOW + "=== Sistema Operacional Aeria ===");
        jogador.sendMessage(ChatColor.WHITE + " -> /is create : Inicializa sua base no mundo.");
        jogador.sendMessage(ChatColor.WHITE + " -> /is go     : Teleporta ate sua coordenada.");
        return true;
    }

    private void gerarEstruturaFisica(Location centro) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                centro.clone().add(x, 0, z).getBlock().setType(Material.BEDROCK);
            }
        }
    }
}