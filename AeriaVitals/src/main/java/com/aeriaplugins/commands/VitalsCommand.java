package com.aeriaplugins.commands;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import com.aeriaplugins.managers.MedicalManager;
import com.aeriaplugins.managers.WeightManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VitalsCommand implements CommandExecutor, TabCompleter {

        private final AeriaVitals plugin;
        private final MedicalManager medicalManager;
        private final PlayerData dummyData = new PlayerData();

        public VitalsCommand(AeriaVitals plugin) {
                this.plugin = plugin;
                this.medicalManager = new MedicalManager(plugin);
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (args.length == 0) {
                        if (!(sender instanceof Player)) {
                                sender.sendMessage("§cApenas jogadores podem ver seus status vitais.");
                                return true;
                        }
                        Player player = (Player) sender;
                        PlayerData data = plugin.getPlayerData(player);

                        player.sendMessage("§7Peso: §6" + String.format("%.1f", data.getCurrentWeight()) + "§7/§6"
                                        + data.getMaxWeight() + " kg");
                        return true;
                }

                if (args[0].equalsIgnoreCase("help")) {
                        sender.sendMessage("§e========= §6AeriaVitals - Ajuda §e=========");
                        sender.sendMessage("§b/vitals §7- Exibe seus status de infecção, imunidade, temperatura e peso.");
                        sender.sendMessage("§b/vitals help §7- Mostra este menu detalhado de comandos.");
                        if (sender.hasPermission("aeriavitals.admin")) {
                                sender.sendMessage("§e--------------------------------");
                                sender.sendMessage("§c/vitals reload §7- Recarrega o arquivo config.yml instantaneamente.");
                                sender.sendMessage("§c/vitals setinfection <player> <0-100> §7- Ajusta o nível de infecção do jogador.");
                                sender.sendMessage("§c/vitals give <player> <item_id> [quantidade] §7- Envia um item médico da config.");
                                sender.sendMessage("§c/vitals drop <item_id> [quantidade] §7- Dropa o item médico nas suas coordenadas.");
                                sender.sendMessage("§c/radiacao set <raio_interno> <raio_externo> <nome> §7- Configura zona de radiação.");
                        }
                        sender.sendMessage("§e=================================");
                        return true;
                }

                if (!sender.hasPermission("aeriavitals.admin")) {
                        sender.sendMessage("§cVocê não tem permissão para usar os subcomandos administrativos.");
                        return true;
                }

                switch (args[0].toLowerCase()) {
                        case "reload":
                                plugin.reloadConfig();
                                sender.sendMessage("§a[AeriaVitals] Configuração recarregada com sucesso.");
                                break;

                        case "setinfection":
                                if (args.length < 3) {
                                        sender.sendMessage("§cUso: /vitals setinfection <player> <valor>");
                                        return true;
                                }
                                Player targetInf = Bukkit.getPlayer(args[1]);
                                if (targetInf == null) {
                                        sender.sendMessage("§cJogador não encontrado.");
                                        return true;
                                }
                                try {
                                        double val = Double.parseDouble(args[2]);
                                        plugin.getPlayerData(targetInf).setInfection(val);
                                        sender.sendMessage("§aInfecção de " + targetInf.getName() + " definida para "
                                                        + val + "%");
                                } catch (NumberFormatException e) {
                                        sender.sendMessage("§cValor numérico inválido.");
                                }
                                break;

                        case "give":
                                if (args.length < 3) {
                                        sender.sendMessage("§cUso: /vitals give <player> <item_id> [quantidade]");
                                        return true;
                                }
                                Player targetGive = Bukkit.getPlayer(args[1]);
                                if (targetGive == null) {
                                        sender.sendMessage("§cJogador não encontrado.");
                                        return true;
                                }
                                String itemKeyGive = args[2];
                                int amountGive = 1;
                                if (args.length >= 4) {
                                        try {
                                                amountGive = Integer.parseInt(args[3]);
                                        } catch (NumberFormatException e) {
                                                sender.sendMessage("§cQuantidade inválida. Usando 1.");
                                        }
                                }
                                ItemStack itemGive = medicalManager.createMedicalItem(itemKeyGive, amountGive);
                                if (itemGive == null) {
                                        sender.sendMessage("§cItem customizado não encontrado na config.yml.");
                                        return true;
                                }
                                targetGive.getInventory().addItem(itemGive);
                                new WeightManager(plugin).recalculateWeight(targetGive);
                                sender.sendMessage("§a" + amountGive + "x " + itemKeyGive + " entregue para "
                                                + targetGive.getName());
                                break;

                        case "drop":
                                if (!(sender instanceof Player)) {
                                        sender.sendMessage(
                                                        "§cApenas jogadores podem usar o comando de drop posicional.");
                                        return true;
                                }
                                if (args.length < 2) {
                                        sender.sendMessage("§cUso: /vitals drop <item_id> [quantidade]");
                                        return true;
                                }
                                Player playerSender = (Player) sender;
                                String itemKeyDrop = args[1];
                                int amountDrop = 1;
                                if (args.length >= 3) {
                                        try {
                                                amountDrop = Integer.parseInt(args[2]);
                                        } catch (NumberFormatException e) {
                                                sender.sendMessage("§cQuantidade inválida. Usando 1.");
                                        }
                                }
                                ItemStack itemDrop = medicalManager.createMedicalItem(itemKeyDrop, amountDrop);
                                if (itemDrop == null) {
                                        sender.sendMessage("§cItem customizado não encontrado na config.yml.");
                                        return true;
                                }
                                playerSender.getWorld().dropItemNaturally(playerSender.getLocation(), itemDrop);
                                sender.sendMessage("§a" + amountDrop + "x " + itemKeyDrop + " dropado no chão.");
                                break;

                        default:
                                sender.sendMessage("§cSubcomando inválido. Opções: reload, setinfection, give, drop");
                                break;
                }

                return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
                if (!sender.hasPermission("aeriavitals.admin"))
                        return new ArrayList<>();

                if (args.length == 1) {
                        return Arrays.asList("help", "reload", "setinfection", "give", "drop").stream()
                                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                                        .collect(Collectors.toList());
                }

                if (args.length == 2) {
                        if (args[0].equalsIgnoreCase("setinfection") || args[0].equalsIgnoreCase("give")) {
                                return Bukkit.getOnlinePlayers().stream()
                                                .map(Player::getName)
                                                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                                .collect(Collectors.toList());
                        }
                        if (args[0].equalsIgnoreCase("drop")) {
                                if (plugin.getConfig().getConfigurationSection("custom-items") == null)
                                        return new ArrayList<>();
                                return new ArrayList<>(plugin.getConfig().getConfigurationSection("custom-items")
                                                .getKeys(false)).stream()
                                                .filter(key -> key.toLowerCase().startsWith(args[1].toLowerCase()))
                                                .collect(Collectors.toList());
                        }
                }

                if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                        List<String> keys = new ArrayList<>();
                        if (plugin.getConfig().getConfigurationSection("custom-items") != null) {
                                keys.addAll(plugin.getConfig().getConfigurationSection("custom-items").getKeys(false));
                        }
                        if (plugin.getConfig().getConfigurationSection("weight.backpacks") != null) {
                                keys.addAll(plugin.getConfig().getConfigurationSection("weight.backpacks")
                                                .getKeys(false));
                        }
                        return keys.stream()
                                        .filter(key -> key.toLowerCase().startsWith(args[2].toLowerCase()))
                                        .collect(Collectors.toList());
                }

                return new ArrayList<>();
        }
}