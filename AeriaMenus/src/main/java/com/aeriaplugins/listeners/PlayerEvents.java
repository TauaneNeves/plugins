package com.aeriaplugins.listeners;

import com.aeriaplugins.plugins.Main;
import com.aeriaplugins.utils.AeriaBoard;
import com.aeriaplugins.utils.ChatUtils;
import com.aeriaplugins.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// ========================================================================
// CLASSE: PlayerEvents
// OBJETIVO: Cuidar da vida do jogador (entrada, saída, danos, proteger mapa)
// ========================================================================
public class PlayerEvents implements Listener {
    private final Main plugin;

    public PlayerEvents(Main plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------------
    // EVENTO: Quando o jogador fala no chat
    // Formata o chat do jogador deixando bonitinho conforme a configuração.
    // ------------------------------------------------------------------------
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("modulos.ativar-chat")) return;
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        String formato = ChatUtils.color(event.getPlayer(), plugin.getFileManager().getMessages().getString("chat.formato")
                .replace("%message%", "%2$s").replace("%player_name%", "%1$s"), usePapi);
        event.setFormat(formato);
    }
    
    // ------------------------------------------------------------------------
    // EVENTO: Quando um jogador ENTRA no servidor (Join)
    // Cria scoreboard, teleporta pro spawn, dá os itens (bússola/relógio).
    // ------------------------------------------------------------------------
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        
        // 1. Cria a Scoreboard
        if (plugin.getConfig().getBoolean("modulos.ativar-scoreboard")) {
            plugin.getBoards().put(p.getUniqueId(), new AeriaBoard(p));
        }
        
        // 2. Se um jogador que já estava no servidor estiver "escondido", oculta ele pro cara novo que entrou
        for (UUID uuid : plugin.getPlayersHidden()) {
            Player hidden = Bukkit.getPlayer(uuid);
            if (hidden != null) p.hidePlayer(hidden);
        }
        
        // 3. Manda mensagem no chat de que entrou
        if (plugin.getConfig().getBoolean("modulos.ativar-mensagens-entrada")) {
            String msg = plugin.getFileManager().getMessages().getString("mensagens-entrada.entrou");
            if (msg == null || msg.isEmpty()) event.setJoinMessage(null);
            else event.setJoinMessage(ChatUtils.color(p, msg, usePapi));
        }
        
        // 4. Manda título gigante na tela e toca som
        if (plugin.getConfig().getBoolean("modulos.ativar-efeicos-entrada")) {
            String title = ChatUtils.color(p, plugin.getFileManager().getMessages().getString("efeitos-entrada.titulo"), usePapi);
            String subtitle = ChatUtils.color(p, plugin.getFileManager().getMessages().getString("efeitos-entrada.subtitulo"), usePapi);
            try {
                p.sendTitle(title, subtitle);
            } catch (Throwable t) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title " + title);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle " + subtitle);
            }
            try { 
                p.playSound(p.getLocation(), Sound.valueOf(plugin.getFileManager().getMessages().getString("efeitos-entrada.som")), 1.0f, 1.0f); 
            } catch (Exception ignored) {}
        }
        
        // 5. Teleporta para o spawn central
        if (plugin.getConfig().getBoolean("spawn.teleportar-ao-entrar")) teleportToSpawn(p);
        
        // 6. Entrega os Itens (Bússola e Relógio) no inventário
        if (plugin.getConfig().getBoolean("modulos.dar-itens-ao-entrar")) {
            p.getInventory().clear();
            if (plugin.getConfig().contains("itens-entrada")) {
                for (String key : plugin.getConfig().getConfigurationSection("itens-entrada").getKeys(false)) {
                    String path = "itens-entrada." + key;
                    ItemStack i = ItemUtils.parseItem(plugin.getConfig(), path, p, usePapi);
                    if (i != null) {
                        ItemMeta mt = i.getItemMeta();
                        List<String> lore = mt.hasLore() ? mt.getLore() : new ArrayList<>();
                        lore.add("§0id:" + key); // Marca o item para depois identificarmos o clique nele
                        mt.setLore(lore);
                        i.setItemMeta(mt);
                        p.getInventory().setItem(plugin.getConfig().getInt(path + ".slot"), i);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // EVENTO: Quando o jogador SAI do servidor (Quit)
    // Limpa ele da memória pra evitar lags (lixo digital)
    // ------------------------------------------------------------------------
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        AeriaBoard board = plugin.getBoards().remove(uuid);
        if (board != null) board.delete();
        
        Object bar = plugin.getBossBars().remove(uuid);
        if (bar != null) {
            try {
                bar.getClass().getMethod("removeAll").invoke(bar);
            } catch (Throwable ignored) {}
        }
        
        plugin.getPlayersHidden().remove(uuid); // Tira da lista de esconder jogadores
        
        if (plugin.getConfig().getBoolean("modulos.ativar-mensagens-entrada")) {
            String msg = plugin.getFileManager().getMessages().getString("mensagens-entrada.saiu");
            if (msg == null || msg.isEmpty()) event.setQuitMessage(null);
            else event.setQuitMessage(ChatUtils.color(event.getPlayer(), msg, plugin.getConfig().getBoolean("modulos.usar-placeholderapi")));
        }
    }

    // ------------------------------------------------------------------------
    // EVENTO: Ao andar
    // Verifica se ele caiu no vazio do mapa e teleporta ele de volta.
    // ------------------------------------------------------------------------
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (p.getLocation().getY() <= plugin.getConfig().getDouble("spawn.altura-void")) teleportToSpawn(p);
    }

    // Bloqueia a chuva se configurado
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (plugin.getConfig().getBoolean("modulos.ativar-clima-sempre-dia") && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    // ========================================================================
    // BLOCO DE PROTEÇÕES DO MAPA
    // Impede jogadores de quebrar blocos, colocar, perder fome e tomar dano.
    // Pessoas com OP (Admin) conseguem burlar isso pra construir o mapa.
    // ========================================================================
    @EventHandler public void onBreak(BlockBreakEvent e) { if (plugin.getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e) { if (plugin.getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { if (plugin.getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); }
    @EventHandler public void onFood(FoodLevelChangeEvent e) { if (plugin.getConfig().getBoolean("modulos.ativar-protecoes")) e.setCancelled(true); }
    @EventHandler public void onDamage(EntityDamageEvent e) { if (plugin.getConfig().getBoolean("modulos.ativar-protecoes") && e.getEntity() instanceof Player) e.setCancelled(true); }

    // Método rápido para mandar alguém pro Spawn baseando-se na config.yml
    private void teleportToSpawn(Player p) {
        if (!plugin.getConfig().contains("spawn.local.mundo")) return;
        World w = Bukkit.getWorld(plugin.getConfig().getString("spawn.local.mundo"));
        if (w != null) p.teleport(new Location(w, plugin.getConfig().getDouble("spawn.local.x"), plugin.getConfig().getDouble("spawn.local.y"), plugin.getConfig().getDouble("spawn.local.z"), (float) plugin.getConfig().getDouble("spawn.local.yaw"), (float) plugin.getConfig().getDouble("spawn.local.pitch")));
    }
}