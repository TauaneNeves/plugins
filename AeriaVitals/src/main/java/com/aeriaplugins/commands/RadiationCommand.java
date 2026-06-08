package com.aeriaplugins.commands;

import com.aeriaplugins.vitals.AeriaVitals;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RadiationCommand implements CommandExecutor {

    private final AeriaVitals plugin;

    public RadiationCommand(AeriaVitals plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser executado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("aeriavitals.admin")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando.");
            return true;
        }

        if (args.length >= 4 && args[0].equalsIgnoreCase("set")) {
            try {
                double innerRadius = Double.parseDouble(args[1]);
                double outerRadius = Double.parseDouble(args[2]);
                String zoneName = args[3];

                Location loc = player.getLocation();
                String path = "radiation-zones." + zoneName;

                plugin.getConfig().set(path + ".world", loc.getWorld().getName());
                plugin.getConfig().set(path + ".x", loc.getX());
                plugin.getConfig().set(path + ".y", loc.getY());
                plugin.getConfig().set(path + ".z", loc.getZ());
                plugin.getConfig().set(path + ".inner-radius", innerRadius);
                plugin.getConfig().set(path + ".outer-radius", outerRadius);
                plugin.getConfig().set(path + ".outer-damage", 0.5);
                plugin.getConfig().set(path + ".outer-infection", 0.2);
                plugin.getConfig().set(path + ".inner-damage", 2.0);
                plugin.getConfig().set(path + ".inner-infection", 1.5);
                plugin.getConfig().set(path + ".mask-protection-factor", 1.0);

                plugin.saveConfig();
                player.sendMessage("§a[+] Zona de radiação '" + zoneName + "' configurada com sucesso nesta posição!");
                player.sendMessage("§7Raio Interno: " + innerRadius + " | Raio Externo: " + outerRadius);
                return true;

            } catch (NumberFormatException e) {
                player.sendMessage("§cErro: Os raios interno e externo precisam ser números válidos.");
                return true;
            }
        }

        player.sendMessage("§cFormato correto: /radiacao set <raio_interno> <raio_externo> <nome_da_zona>");
        return true;
    }
}