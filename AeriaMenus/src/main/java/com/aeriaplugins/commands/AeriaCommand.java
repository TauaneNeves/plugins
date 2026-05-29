
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

// ========================================================================
// CLASSE: AeriaCommand
// OBJETIVO: Gerir o comando principal do plugin (/am)
// ========================================================================
public class AeriaCommand implements CommandExecutor {

    private final Main plugin;

    public AeriaCommand(Main plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------------
    // MÉTODO: onCommand
    // ------------------------------------------------------------------------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");

        Player player = (sender instanceof Player)
                ? (Player) sender
                : null;

        // --------------------------------------------------------------------
        // /am
        // --------------------------------------------------------------------
        if (args.length == 0
                || args[0].equalsIgnoreCase("help")
                || args[0].equalsIgnoreCase("ajuda")) {

            enviarAjudaDetalhada(sender, player, usePapi);
            return true;
        }

        // --------------------------------------------------------------------
        // /am reload
        // --------------------------------------------------------------------
        if (args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("aeriamenus.admin")) {

                sender.sendMessage(ChatUtils.color(
                        player,
                        "&cVocê não possui permissão!",
                        usePapi
                ));

                return true;
            }

            plugin.reloadPlugin();

            sender.sendMessage(ChatUtils.color(
                    player,
                    "&aConfigurações recarregadas com sucesso!",
                    usePapi
            ));

            return true;
        }

        // --------------------------------------------------------------------
        // /am abrir <menu> [jogador]
        // --------------------------------------------------------------------
        if (args[0].equalsIgnoreCase("abrir")
                || args[0].equalsIgnoreCase("open")) {

            if (!sender.hasPermission("aeriamenus.admin")) {

                sender.sendMessage(ChatUtils.color(
                        player,
                        "&cVocê não possui permissão!",
                        usePapi
                ));

                return true;
            }

            if (args.length < 2) {

                sender.sendMessage(ChatUtils.color(
                        player,
                        "&c&lERRO! &7Uso: &e/am abrir <menu> [jogador]",
                        usePapi
                ));

                return true;
            }

            String menuKey = args[1];

            FileConfiguration menuConfig =
                    plugin.getFileManager().getMenu(menuKey);

            if (menuConfig == null) {

                sender.sendMessage(ChatUtils.color(
                        player,
                        "&c&lERRO! &7Menu não encontrado.",
                        usePapi
                ));

                return true;
            }

            Player alvo = player;

            if (args.length >= 3) {

                alvo = Bukkit.getPlayer(args[2]);

                if (alvo == null) {

                    sender.sendMessage(ChatUtils.color(
                            player,
                            "&c&lERRO! &7Jogador offline.",
                            usePapi
                    ));

                    return true;
                }
            }

            if (alvo == null) {

                sender.sendMessage(ChatColor.RED +
                        "Especifique um jogador alvo via Console!");

                return true;
            }

            int linhas = menuConfig.getInt("linhas", 3);

            String titulo = ChatUtils.color(
                    alvo,
                    menuConfig.getString("titulo", "Menu"),
                    usePapi
            );

            Inventory inv = Bukkit.createInventory(
                    null,
                    linhas * 9,
                    titulo
            );

            // ----------------------------------------------------------------
            // ITENS DO MENU
            // ----------------------------------------------------------------
            if (menuConfig.contains("itens")) {

                for (String iK : menuConfig
                        .getConfigurationSection("itens")
                        .getKeys(false)) {

                    String iP = "itens." + iK;

                    org.bukkit.inventory.ItemStack item =
                            com.aeriaplugins.utils.ItemUtils.parseItem(
                                    menuConfig,
                                    iP,
                                    alvo,
                                    usePapi
                            );

                    if (item != null) {

                        org.bukkit.inventory.meta.ItemMeta meta =
                                item.getItemMeta();

                        boolean temAcao =
                                menuConfig.contains(iP + ".acoes")
                                        && !menuConfig
                                        .getStringList(iP + ".acoes")
                                        .isEmpty();

                        if (temAcao
                                || menuConfig.contains(iP + ".custo")
                                || menuConfig.contains(iP + ".permissao")) {

                            List<String> lore =
                                    meta.hasLore()
                                            ? meta.getLore()
                                            : new ArrayList<>();

                            lore.add("§0menu:" + menuKey + ";" + iK);

                            meta.setLore(lore);
                        }

                        item.setItemMeta(meta);

                        inv.setItem(
                                menuConfig.getInt(iP + ".slot"),
                                item
                        );
                    }
                }
            }

            alvo.openInventory(inv);

            sender.sendMessage(ChatUtils.color(
                    player,
                    "&aMenu aberto com sucesso!",
                    usePapi
            ));

            return true;
        }

        // --------------------------------------------------------------------
        // /am vault ajuda
        // --------------------------------------------------------------------
        if (args[0].equalsIgnoreCase("vault")) {

            if (!sender.hasPermission("aeriamenus.admin")) {

                sender.sendMessage(ChatColor.RED +
                        "Você não possui permissão.");

                return true;
            }

            if (args.length < 2) {

                sender.sendMessage(ChatColor.RED +
                        "Use: /am vault ajuda");

                return true;
            }

            if (args[1].equalsIgnoreCase("ajuda")) {

                sendPermissionHelp(sender);

                return true;
            }
        }

        return true;
    }

    // ------------------------------------------------------------------------
    // AJUDA DE PERMISSÕES
    // ------------------------------------------------------------------------
    private void sendPermissionHelp(CommandSender sender) {

        sender.sendMessage(ChatColor.YELLOW +
                "=========== Vault Help ===========");

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {

            sender.sendMessage(ChatColor.GREEN +
                    "LuckPerms detectado.");

            sender.sendMessage(ChatColor.GRAY +
                    "Criar grupo:");

            sender.sendMessage(ChatColor.AQUA +
                    "/lp creategroup dono");

            sender.sendMessage(ChatColor.GRAY +
                    "Adicionar prefixo:");

            sender.sendMessage(ChatColor.AQUA +
                    "/lp group dono meta setprefix 100 \"&4[DONO] \"");

            sender.sendMessage(ChatColor.GRAY +
                    "Adicionar jogador ao grupo:");

            sender.sendMessage(ChatColor.AQUA +
                    "/lp user <jogador> parent add dono");

        } else {

            sender.sendMessage(ChatColor.RED +
                    "Nenhum plugin de permissões compatível detectado.");

            sender.sendMessage(ChatColor.GRAY +
                    "Instale LuckPerms + Vault.");
        }

        sender.sendMessage(ChatColor.YELLOW +
                "==================================");
    }

    // ------------------------------------------------------------------------
    // MENU DE AJUDA
    // ------------------------------------------------------------------------
    private void enviarAjudaDetalhada(
            CommandSender sender,
            Player player,
            boolean usePapi
    ) {

        sender.sendMessage(ChatUtils.color(
                player,
                "&8&m                                                                ",
                usePapi
        ));

        sender.sendMessage(ChatUtils.color(
                player,
                "<gradient:#00d4ff:#00ff55>&lCENTRAL DE AJUDA — AERIAMENUS v1.0.0</gradient>",
                usePapi
        ));

        sender.sendMessage(ChatUtils.color(
                player,
                " &7Gerencie seus Menus, Lobby e Sistemas.",
                usePapi
        ));

        sender.sendMessage(ChatUtils.color(
                player,
                "&8&m                                                                ",
                usePapi
        ));

        sender.sendMessage(ChatUtils.color(
                player,
                "  &f/am ajuda &8- &7Exibe os comandos disponíveis.",
                usePapi
        ));

        sender.sendMessage(ChatUtils.color(
                player,
                "  &f/am reload &8- &7Recarrega o plugin.",
                usePapi
        ));

        sender.sendMessage(ChatUtils.color(
                player,
                "  &f/am abrir <menu> [jogador] &8- &7Abre um menu.",
                usePapi
        ));

        sender.sendMessage(ChatUtils.color(
                player,
                "  &f/am vault ajuda &8- &7Ajuda com grupos/permissões.",
                usePapi
        ));

        sender.sendMessage(ChatUtils.color(
                player,
                "&8&m                                                                ",
                usePapi
        ));
    }
}
