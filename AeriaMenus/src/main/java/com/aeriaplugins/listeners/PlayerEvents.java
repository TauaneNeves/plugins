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
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class PlayerEvents implements Listener {

    private final Main plugin;

    public PlayerEvents(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("modulos.ativar-chat")) return;
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");
        String formato = ChatUtils.color(event.getPlayer(), plugin.getFileManager().getMessages().getString("chat.formato")
                .replace("%message%", "%2$s").replace("%player_name%", "%1$s"), usePapi);
        event.setFormat(formato);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        boolean usePapi = plugin.getConfig().getBoolean("modulos.usar-placeholderapi");

        // --- NOVO: Cria a Scoreboard Anti-Flicker para este jogador ---
        if (plugin.getConfig().getBoolean("modulos.ativar-scoreboard")) {
            plugin.getBoards().put(p.getUniqueId(), new AeriaBoard(p));
        }

        if (plugin.getBossBar() != null) {
            plugin.getBossBar().addPlayer(p);
        }

        for (UUID uuid : plugin.getPlayersHidden()) {
            Player hidden = Bukkit.getPlayer(uuid);
            if (hidden != null) p.hidePlayer(plugin, hidden);
        }

        if (plugin.getConfig().getBoolean("modulos.ativar-mensagens-entrada")) {
            String msg = plugin.getFileManager().getMessages().getString("mensagens-entrada.entrou");
            if (msg == null || msg.isEmpty()) event.setJoinMessage(null);
            else event.setJoinMessage(ChatUtils.color(p, msg, usePapi));
        }

        if (plugin.getConfig().getBoolean("modulos.ativar-efeitos-entrada")) {
            String title = ChatUtils.color(p, plugin.getFileManager().getMessages().getString("efeitos-entrada.titulo"), usePapi);
            String subtitle = ChatUtils.color(p, plugin.getFileManager().getMessages().getString("efeitos-entrada.subtitulo"), usePapi);
            p.sendTitle(title, subtitle, 10, 70, 20);
            try { p.playSound(p.getLocation(), Sound.valueOf(plugin.getFileManager().getMessages().getString("efeitos-entrada.som")), 1.0f, 1.0f); } catch (Exception ignored) {}
        }

        if (plugin.getConfig().getBoolean("spawn.teleportar-ao-entrar")) teleportToSpawn(p);

        if (plugin.getConfig().getBoolean("modulos.dar-itens-ao-entrar")) {
            p.getInventory().clear();
            if (plugin.getConfig().contains("itens-entrada")) {
                for (String key : plugin.getConfig().getConfigurationSection("itens-entrada").getKeys(false)) {
                    String path = "itens-entrada." + key;
                    ItemStack i = ItemUtils.parseItem(plugin.getConfig(), path, p, usePapi);
                    if (i != null) {
                        ItemMeta mt = i.getItemMeta();
                        mt.getPersistentDataContainer().set(plugin.getLobbyItemKey(), PersistentDataType.STRING, key);
                        i.setItemMeta(mt);
                        p.getInventory().setItem(plugin.getConfig().getInt(path + ".slot"), i);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        // --- NOVO: Remove a Scoreboard da Memória ---
        AeriaBoard board = plugin.getBoards().remove(uuid);
        if (board != null) board.delete();
        
        plugin.getPlayersHidden().remove(uuid);
        
        if (plugin.getBossBar() != null) {
            plugin.getBossBar().removePlayer(event.getPlayer());
        }
        
        if (plugin.getConfig().getBoolean("modulos.ativar-mensagens-entrada")) {
            String msg = plugin.getFileManager().getMessages().getString("mensagens-entrada.saiu");
            if (msg == null || msg.isEmpty()) event.setQuitMessage(null);
            else event.setQuitMessage(ChatUtils.color(event.getPlayer(), msg, plugin.getConfig().getBoolean("modulos.usar-placeholderapi")));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (p.getLocation().getY() <= plugin.getConfig().getDouble("spawn.altura-void")) teleportToSpawn(p);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (plugin.getConfig().getBoolean("modulos.ativar-clima-sempre-dia") && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler 
    public void onBreak(BlockBreakEvent e) { 
        if (plugin.getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); 
    }

    @EventHandler 
    public void onPlace(BlockPlaceEvent e) { 
        if (plugin.getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); 
    }

    @EventHandler 
    public void onDrop(PlayerDropItemEvent e) { 
        if (plugin.getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); 
    }

    @EventHandler 
    public void onFood(FoodLevelChangeEvent e) { 
        if (plugin.getConfig().getBoolean("modulos.ativar-protecoes")) e.setCancelled(true); 
    }

    @EventHandler 
    public void onDamage(EntityDamageEvent e) { 
        if (plugin.getConfig().getBoolean("modulos.ativar-protecoes") && e.getEntity() instanceof Player) e.setCancelled(true); 
    }

    private void teleportToSpawn(Player p) {
        if (!plugin.getConfig().contains("spawn.local.mundo")) return;
        World w = Bukkit.getWorld(plugin.getConfig().getString("spawn.local.mundo"));
        if (w != null) p.teleport(new Location(w, plugin.getConfig().getDouble("spawn.local.x"), plugin.getConfig().getDouble("spawn.local.y"), plugin.getConfig().getDouble("spawn.local.z"), (float) plugin.getConfig().getDouble("spawn.local.yaw"), (float) plugin.getConfig().getDouble("spawn.local.pitch")));
    }
}