package com.aeriaplugins.listeners;

import com.aeriaplugins.plugins.Main;
import com.aeriaplugins.utils.ChatUtils;
import com.aeriaplugins.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// ========================================================================
// CLASSE: MenuEvents
// OBJETIVO: Gerenciar tudo que envolve os itens clicáveis e menus virtuais.
// ========================================================================
public class MenuEvents implements Listener {
    private final Main plugin;

    public MenuEvents(Main plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------------
    // EVENTO: Ao digitar um comando (ex: /menu)
    // Interceptamos o comando para ver se ele pertence a algum menu criado nas configs.
    // ------------------------------------------------------------------------
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        String fullCommand = e.getMessage().substring(1).toLowerCase();
        String baseCommand = fullCommand.split(" ")[0]; 

        for (Map.Entry<String, FileConfiguration> entry : plugin.getFileManager().getMenus().entrySet()) {
            FileConfiguration menuConfig = entry.getValue();
            
            if (menuConfig.contains("comando")) {
                List<String> aliases = new ArrayList<>();
                if (menuConfig.isString("comando")) {
                    aliases.add(menuConfig.getString("comando").toLowerCase());
                } else if (menuConfig.isList("comando")) {
                    for (String c : menuConfig.getStringList("comando")) {
                        aliases.add(c.toLowerCase());
                    }
                }
                if (aliases.contains(baseCommand)) {
                    e.setCancelled(true); // Cancela o comando original
                    open(e.getPlayer(), entry.getKey()); // Abre o menu correspondente
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // EVENTO: Interação de clique no ar ou em blocos (Usar itens da mão)
    // Aqui é onde o clique no RELÓGIO ou na BÚSSOLA do lobby acontece.
    // ------------------------------------------------------------------------
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack i = e.getItem();
        if (i == null || !i.hasItemMeta()) return;
        Player p = e.getPlayer();
        ItemMeta meta = i.getItemMeta();
        
        String key = null;
        // Verifica se o item possui a marcação "escondida" no final da Lore para identificar a ação
        if (meta.hasLore() && !meta.getLore().isEmpty()) {
            String lastLine = meta.getLore().get(meta.getLore().size() - 1);
            if (lastLine.startsWith("§0id:")) {
                key = lastLine.substring(5);
            }
        }

        if (key != null) {
            e.setCancelled(true); // Impede de colocar o item no chão (se for bloco)
            if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                String path = "itens-entrada." + key + ".acoes";
                if (plugin.getConfig().contains(path)) {
                    // Executa as ações definidas na config.yml (ex: abrir menu, esconder jogadores)
                    execute(p, plugin.getConfig().getStringList(path));
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // EVENTO: Clique dentro de um Inventário (Menus abertos)
    // ------------------------------------------------------------------------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        
        ItemMeta meta = event.getCurrentItem().getItemMeta();
        String data = null;

        // Procura a marcação invisível do menu no item
        if (meta.hasLore() && !meta.getLore().isEmpty()) {
            String lastLine = meta.getLore().get(meta.getLore().size() - 1);
            if (lastLine.startsWith("§0menu:")) {
                data = lastLine.substring(7);
            }
        }

        if (data != null) {
            event.setCancelled(true); // Impede o jogador de roubar o item do menu
            Player player = (Player) event.getWhoClicked();
            String[] parts = data.split(";");
            if (parts.length == 2) {
                String menuKey = parts[0];
                String itemKey = parts[1];
                
                FileConfiguration menuConfig = plugin.getFileManager().getMenu(menuKey);
                if (menuConfig != null) {
                    String baseItemPath = "itens." + itemKey;
                    
                    // Checagem de permissão
                    if (menuConfig.contains(baseItemPath + ".permissao")) {
                        String permRequirida = menuConfig.getString(baseItemPath + ".permissao");
                        if (!player.hasPermission(permRequirida)) {
                            String msgErro = menuConfig.getString(baseItemPath + ".mensagem-erro", "&cVocê não tem permissão!");
                            player.sendMessage(ChatUtils.color(player, msgErro, plugin.getConfig().getBoolean("modulos.usar-placeholderapi")));
                            
                            String somErro = menuConfig.getString(baseItemPath + ".som-erro");
                            if (somErro != null) {
                                try { player.playSound(player.getLocation(), Sound.valueOf(somErro.toUpperCase()), 1f, 1f); } catch (Exception ignored) {}
                            }
                            return; 
                        }
                    }

                    // Checagem de Dinheiro (Vault)
                    if (menuConfig.contains(baseItemPath + ".custo")) {
                        if (plugin.getConfig().getBoolean("modulos.usar-vault") && plugin.getEconomy() != null) {
                            double custo = menuConfig.getDouble(baseItemPath + ".custo");
                            if (!plugin.getEconomy().has(player, custo)) {
                                String msgErro = menuConfig.getString(baseItemPath + ".mensagem-erro-dinheiro", "&cSaldo insuficiente!");
                                player.sendMessage(ChatUtils.color(player, msgErro, plugin.getConfig().getBoolean("modulos.usar-placeholderapi")));
                                return;
                            } else {
                                plugin.getEconomy().withdrawPlayer(player, custo); // Desconta o dinheiro
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "O sistema de economia (Vault) está desativado!");
                            return;
                        }
                    }

                    // Se passou em tudo, executa as ações do botão (ex: ir pro minigame)
                    String acoesPath = baseItemPath + ".acoes";
                    if (menuConfig.contains(acoesPath)) {
                        execute(player, menuConfig.getStringList(acoesPath));
                    }
                }
            }
            return;
        }

        // Impede movimentar itens do próprio inventário enquanto estiver num menu customizado
        boolean isCustomMenu = false;
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        
        for (String menuKey : plugin.getFileManager().getMenus().keySet()) {
            FileConfiguration menuConfig = plugin.getFileManager().getMenu(menuKey);
            String menuTitle = ChatUtils.color((Player) event.getWhoClicked(), menuConfig.getString("titulo", ""), usePapi);
            if (event.getView().getTitle().equals(menuTitle)) {
                isCustomMenu = true;
                break;
            }
        }
        if (isCustomMenu) {
            event.setCancelled(true);
            return;
        }

        // Impede que o jogador mova os itens do Lobby (Bússola/Relógio) caso a config mande
        if (plugin.getConfig().getBoolean("modulos.dar-itens-ao-entrar") && !event.getWhoClicked().isOp()) {
            if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
                event.setCancelled(true);
            }
        }
    }

    // ------------------------------------------------------------------------
    // MÉTODO: execute
    // Este método lê a lista de 'acoes:' no arquivo YML e transforma em ações reais.
    // ------------------------------------------------------------------------
    private void execute(Player p, List<String> a) {
        if (a == null) return;
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        for (String s : a) {
            String linha = s.trim(); // Limpa os espaços invisíveis do YAML
            
            if (linha.startsWith("comando: ")) {
                p.performCommand(linha.substring(9).replace("%player%", p.getName()));
            } else if (linha.startsWith("consola: ")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), linha.substring(9).replace("%player%", p.getName()));
            } else if (linha.startsWith("mensagem: ")) {
                p.sendMessage(ChatUtils.color(p, linha.substring(10), usePapi));
            } else if (linha.startsWith("menu: ")) {
                open(p, linha.substring(6).trim());
            } else if (linha.equalsIgnoreCase("especial: alternar_visibilidade")) {
                toggleVisibility(p); // CHAMA A FUNÇÃO DE ESCONDER/MOSTRAR JOGADORES AQUI!
            } else if (linha.startsWith("servidor: ")) {
                conectarServidor(p, linha.substring(10).trim());
            } else if (linha.startsWith("som: ")) {
                try { p.playSound(p.getLocation(), Sound.valueOf(linha.substring(5).toUpperCase().trim()), 1f, 1f); } catch(Exception ignored) {}
            } else if (linha.equalsIgnoreCase("fechar")) {
                p.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------------------
    // MÉTODO: conectarServidor
    // Envia o jogador para outro servidor da sua rede via BungeeCord.
    // ------------------------------------------------------------------------
    private void conectarServidor(Player p, String serverName) {
        int portaAlvo = 25566; 
        if (serverName.equalsIgnoreCase("lobby")) {
            portaAlvo = 25566;
        } else if (serverName.equalsIgnoreCase("minigames")) {
            portaAlvo = 25567;
        } else if (serverName.equalsIgnoreCase("skyblock")) {
            portaAlvo = 25568;
        }

        final int porta = portaAlvo;
        
        // Faz a checagem em segundo plano para não "congelar" o servidor inteiro
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isServerOnline("localhost", porta)) {
                // Servidor está online, então envia o jogador
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        ByteArrayOutputStream b = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(b);
                        out.writeUTF("Connect");
                        out.writeUTF(serverName);
                        p.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
                    } catch (Exception ignored) {}
                });
            } else {
                // Servidor está Offline! Exibe mensagem bonita de falha.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
                    String formatName = serverName.substring(0, 1).toUpperCase() + serverName.substring(1).toLowerCase();
                    
                    p.sendMessage("");
                    p.sendMessage(ChatUtils.color(p, " &c&m━&4&m━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━", usePapi));
                    p.sendMessage(ChatUtils.color(p, "  <gradient:#ff3333:#ffaa00>&lCONEXÃO FALHOU — REDE</gradient>", usePapi));
                    p.sendMessage("");
                    p.sendMessage(ChatUtils.color(p, "  &8• &fServidor: &e&l" + formatName, usePapi));
                    p.sendMessage(ChatUtils.color(p, "  &8• &fEstado: &c&nManutenção / Offline", usePapi));
                    p.sendMessage("");
                    p.sendMessage(ChatUtils.color(p, "  &7Estamos a preparar novidades. Por favor, aguarde", usePapi));
                    p.sendMessage(ChatUtils.color(p, "  &7alguns minutos e tente conectar novamente!", usePapi));
                    p.sendMessage(ChatUtils.color(p, " &c&m━&4&m━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━━&c&m━━&4&m━", usePapi));
                    p.sendMessage("");
                    
                    try {
                        p.playSound(p.getLocation(), Sound.valueOf("ENTITY_ITEM_BREAK"), 1f, 1f);
                    } catch (Throwable t) {
                        try { p.playSound(p.getLocation(), Sound.valueOf("ITEM_BREAK"), 1f, 1f); } catch (Throwable ignored) {}
                    }
                });
            }
        });
    }

    // Checa se o IP e a Porta estão abertos
    private boolean isServerOnline(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 600); 
            return true;
        } catch (IOException e) {
            return false; 
        }
    }

    // ------------------------------------------------------------------------
    // MÉTODO: toggleVisibility (Relógio Mágico)
    // Se o jogador estiver na lista oculta, mostra todos de novo.
    // Se não estiver, adiciona à lista e esconde todo mundo da tela dele.
    // ------------------------------------------------------------------------
    private void toggleVisibility(Player p) {
        if (plugin.getPlayersHidden().contains(p.getUniqueId())) {
            // Remove da lista e torna todos visíveis
            plugin.getPlayersHidden().remove(p.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) p.showPlayer(target);
            p.sendMessage(ChatColor.GREEN + "Jogadores visíveis!");
        } else {
            // Adiciona na lista e esconde todo mundo
            plugin.getPlayersHidden().add(p.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) p.hidePlayer(target);
            p.sendMessage(ChatColor.RED + "Jogadores ocultos!");
        }
    }

    // ------------------------------------------------------------------------
    // MÉTODO: open (Abrir um Menu Customizado)
    // Lê as propriedades no YML e "pinta" o inventário na tela do jogador.
    // ------------------------------------------------------------------------
    private void open(Player p, String k) {
        FileConfiguration menuConfig = plugin.getFileManager().getMenu(k);
        if (menuConfig == null) return;
        
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        Inventory inv = Bukkit.createInventory(null, menuConfig.getInt("linhas") * 9, ChatUtils.color(p, menuConfig.getString("titulo"), usePapi));
        
        if (menuConfig.contains("itens")) {
            for (String iK : menuConfig.getConfigurationSection("itens").getKeys(false)) {
                String iP = "itens." + iK;
                ItemStack i = ItemUtils.parseItem(menuConfig, iP, p, usePapi);
                
                if (i != null) {
                    ItemMeta m = i.getItemMeta();
                    boolean temAcao = menuConfig.contains(iP + ".acoes") && !menuConfig.getStringList(iP + ".acoes").isEmpty();
                    if (temAcao || menuConfig.contains(iP + ".custo") || menuConfig.contains(iP + ".permissao")) {
                        List<String> lore = m.hasLore() ? m.getLore() : new ArrayList<>();
                        lore.add("§0menu:" + k + ";" + iK); 
                        m.setLore(lore);
                    }
                    i.setItemMeta(m);
                    inv.setItem(menuConfig.getInt(iP + ".slot"), i);
                }
            }
        }
        p.openInventory(inv);
    }
}