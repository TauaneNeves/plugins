package com.aeriaplugins.plugins;

import com.aeriaplugins.commands.AeriaCommand;
import com.aeriaplugins.listeners.MenuEvents;
import com.aeriaplugins.listeners.PlayerEvents;
import com.aeriaplugins.managers.FileManager;
import com.aeriaplugins.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin {

    private final Set<UUID> playersHidden = new HashSet<>();
    private BossBar bossBar;
    
    private NamespacedKey lobbyItemKey;
    private NamespacedKey menuItemKey;
    private int actionBarIndex = 0;
    
    private FileManager fileManager;

    @Override
    public void onEnable() {
        // Inicializar Gestor de Ficheiros
        fileManager = new FileManager(this);
        fileManager.loadAll();
        
        lobbyItemKey = new NamespacedKey(this, "lobby_item_id");
        menuItemKey = new NamespacedKey(this, "menu_item_id");
        
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        
        // Registar Eventos
        getServer().getPluginManager().registerEvents(new PlayerEvents(this), this);
        getServer().getPluginManager().registerEvents(new MenuEvents(this), this);
        
        // Registar Comandos
        getCommand("aeriamenus").setExecutor(new AeriaCommand(this));
        
        if (getConfig().getBoolean("modulos.usar-placeholderapi") && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI ativado na config, mas nao foi encontrado no servidor!");
        }
        
        startTasks();
        setupBossBar();
        
        getLogger().info("AeriaMenus carregado com sucesso! (Modo SuperLobby Premium Ativo)");
    }

    @Override
    public void onDisable() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void setupBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        if (getConfig().getBoolean("modulos.ativar-bossbar")) {
            BarColor bc = BarColor.valueOf(getConfig().getString("bossbar.cor", "BLUE"));
            BarStyle bs = BarStyle.valueOf(getConfig().getString("bossbar.estilo", "SOLID"));
            
            bossBar = Bukkit.createBossBar("Carregando...", bc, bs);
            bossBar.setProgress(getConfig().getDouble("bossbar.progresso", 1.0));
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(p);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (bossBar != null) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                             String title = ChatUtils.color(p, getConfig().getString("bossbar.titulo").replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())), getConfig().getBoolean("modulos.usar-placeholderapi"));
                             bossBar.setTitle(title);
                        }
                    }
                }
            }.runTaskTimer(this, 0L, 40L);
        }
    }

    private void startTasks() {
        // Action Bar Task
        if (getConfig().getBoolean("modulos.ativar-actionbar-rotativa")) {
            long ticks = getConfig().getLong("actionbar.tempo-entre-mensagens") * 20L;
            List<String> mensagens = getConfig().getStringList("actionbar.mensagens");
            if (!mensagens.isEmpty()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (Bukkit.getOnlinePlayers().isEmpty()) return;
                        String rawMsg = mensagens.get(actionBarIndex % mensagens.size());
                        actionBarIndex++;
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            String msg = ChatUtils.color(p, rawMsg, getConfig().getBoolean("modulos.usar-placeholderapi"));
                            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
                        }
                    }
                }.runTaskTimer(this, 0L, ticks);
            }
        }

        // Tablist Task
        if (getConfig().getBoolean("modulos.ativar-tablist-animada")) {
            long ticks = getConfig().getLong("tablist.tempo-atualizacao", 20L);
            new BukkitRunnable() {
                @Override
                public void run() {
                    boolean usePapi = getConfig().getBoolean("modulos.usar-placeholderapi");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        StringBuilder headerB = new StringBuilder();
                        for (String h : getConfig().getStringList("tablist.header")) {
                            headerB.append(ChatUtils.color(p, h, usePapi)).append("\n");
                        }
                        StringBuilder footerB = new StringBuilder();
                        for (String f : getConfig().getStringList("tablist.footer")) {
                            footerB.append(ChatUtils.color(p, f, usePapi)).append("\n");
                        }
                        p.setPlayerListHeaderFooter(headerB.toString().trim(), footerB.toString().trim());
                    }
                }
            }.runTaskTimer(this, 0L, ticks);
        }

        // Time Task
        if (getConfig().getBoolean("modulos.ativar-clima-sempre-dia")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (World w : Bukkit.getWorlds()) {
                        if (w.getEnvironment() == World.Environment.NORMAL) {
                            w.setTime(6000L);
                        }
                    }
                }
            }.runTaskTimer(this, 0L, 100L);
        }
        
        // Scoreboard Task
        if (getConfig().getBoolean("modulos.ativar-scoreboard")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        updateScoreboard(player);
                    }
                }
            }.runTaskTimer(this, 0L, 20L);
        }
    }
    
    private void updateScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager m = Bukkit.getScoreboardManager();
        if (m == null) return;
        org.bukkit.scoreboard.Scoreboard b = m.getNewScoreboard();
        boolean usePapi = getConfig().getBoolean("modulos.usar-placeholderapi");
        org.bukkit.scoreboard.Objective o = b.registerNewObjective("aeria", "dummy", ChatUtils.color(player, getConfig().getString("scoreboard.titulo"), usePapi));
        o.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);

        List<String> lines = getConfig().getStringList("scoreboard.linhas");
        int index = lines.size();
        for (String line : lines) {
            String f = ChatUtils.color(player, line.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())), usePapi);
            org.bukkit.scoreboard.Score score = o.getScore(f.isEmpty() ? org.bukkit.ChatColor.values()[index % 15].toString() : f);
            score.setScore(index--);
            try {
                try {
                    Class<?> paperFormatClass = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
                    Object blankFormat = paperFormatClass.getMethod("blank").invoke(null);
                    score.getClass().getMethod("numberFormat", paperFormatClass).invoke(score, blankFormat);
                } catch (Exception e1) {
                    Class<?> spigotFormatClass = Class.forName("org.bukkit.scoreboard.NumberFormat");
                    Object blankFormat = spigotFormatClass.getMethod("blank").invoke(null);
                    score.getClass().getMethod("setNumberFormat", spigotFormatClass).invoke(score, blankFormat);
                }
            } catch (Exception ignored) {}
        }
        player.setScoreboard(b);
    }

    public Set<UUID> getPlayersHidden() { return playersHidden; }
    public BossBar getBossBar() { return bossBar; }
    public NamespacedKey getLobbyItemKey() { return lobbyItemKey; }
    public NamespacedKey getMenuItemKey() { return menuItemKey; }
    public FileManager getFileManager() { return fileManager; }
}