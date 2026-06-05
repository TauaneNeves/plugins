package com.aeriaplugins.gps.commands;

import com.aeriaplugins.gps.AeriaGPS;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class GPSCommand implements CommandExecutor {

    private final AeriaGPS plugin;

    public GPSCommand(AeriaGPS plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            enviarAjuda(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                if (!sender.hasPermission("aeriagps.admin")) return semPermissao(sender);
                plugin.reloadConfig();
                plugin.getIslandManager().carregarIlhas();
                sender.sendMessage(ChatColor.GREEN + "Configuração recarregada.");
                break;

            case "give":
                if (!sender.hasPermission("aeriagps.admin")) return semPermissao(sender);
                if (args.length < 3) return false;
                Player alvo = Bukkit.getPlayer(args[1]);
                if (alvo == null) return false;
                alvo.getInventory().addItem(plugin.getItemManager().createGPS());
                sender.sendMessage(ChatColor.GREEN + "GPS entregue.");
                break;

            case "givebattery":
                if (!sender.hasPermission("aeriagps.admin")) return semPermissao(sender);
                if (args.length < 3) return false;
                Player bAlvo = Bukkit.getPlayer(args[1]);
                int qtd = Integer.parseInt(args[2]);
                bAlvo.getInventory().addItem(criarBateria(qtd));
                break;

            case "givepaper":
                if (!sender.hasPermission("aeriagps.admin")) return semPermissao(sender);
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /aeriagps givepaper <jogador> <nome_da_ilha>");
                    return true;
                }
                Player pAlvo = Bukkit.getPlayer(args[1]);
                if (pAlvo == null) return true;
                
                String nomeIlha = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).replace("\"", "");
                if (!plugin.getIslandManager().getIslands().containsKey(nomeIlha)) {
                    sender.sendMessage(ChatColor.RED + "Erro: Nenhuma ilha chamada '" + nomeIlha + "' foi salva no config.");
                    return true;
                }
                pAlvo.getInventory().addItem(plugin.getItemManager().createBlueprint(nomeIlha));
                sender.sendMessage(ChatColor.GREEN + "Papel de coordenadas entregue para " + pAlvo.getName() + ".");
                break;

            case "setwaypoint":
            case "marcar":
                if (!(sender instanceof Player)) return true;
                if (args.length < 2) return false;
                String nome = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replace("\"", "");
                plugin.getIslandManager().salvarIlha(nome, ((Player) sender).getLocation());
                sender.sendMessage(ChatColor.AQUA + "Waypoint global '" + nome + "' salvo com sucesso no servidor!");
                break;

            case "list":
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                String unlocked = p.getPersistentDataContainer().getOrDefault(plugin.getItemManager().unlockedKey, PersistentDataType.STRING, "");
                if (unlocked.isEmpty()) {
                    p.sendMessage(ChatColor.RED + "Seu banco de dados do GPS está vazio. Encontre papéis de coordenadas.");
                } else {
                    p.sendMessage(ChatColor.GOLD + "--- Locais Registrados no seu GPS ---");
                    for (String s : unlocked.split(";")) {
                        p.sendMessage(ChatColor.YELLOW + "- " + s);
                    }
                }
                break;

            case "clear":
                if (!(sender instanceof Player)) return true;
                Player pClear = (Player) sender;
                pClear.getPersistentDataContainer().remove(plugin.getItemManager().unlockedKey);
                sender.sendMessage(ChatColor.YELLOW + "Memória do GPS formatada. Todos os papéis aprendidos foram apagados.");
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Comando desconhecido.");
        }
        return true;
    }

    private ItemStack criarBateria(int carga) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Bateria de GPS (" + carga + "%)");
        meta.getPersistentDataContainer().set(plugin.getItemManager().batteryKey, PersistentDataType.INTEGER, carga);
        item.setItemMeta(meta);
        return item;
    }

    private void enviarAjuda(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- AeriaGPS Ajuda ---");
        sender.sendMessage(ChatColor.YELLOW + "/gps give <jogador> <tier>");
        sender.sendMessage(ChatColor.YELLOW + "/gps givebattery <jogador> <qtd>");
        sender.sendMessage(ChatColor.YELLOW + "/gps setwaypoint <nome>");
        sender.sendMessage(ChatColor.YELLOW + "/gps givepaper <jogador> <nome>");
        sender.sendMessage(ChatColor.YELLOW + "/gps list");
        sender.sendMessage(ChatColor.YELLOW + "/gps clear");
        sender.sendMessage(ChatColor.YELLOW + "/gps reload");
    }

    private boolean semPermissao(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Sem permissão.");
        return true;
    }
}