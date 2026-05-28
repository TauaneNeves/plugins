package com.aeriaplugins.plugins;

import com.aeriaplugins.commands.AeriaCommand;
import com.aeriaplugins.listeners.MenuEvents;
import com.aeriaplugins.listeners.PlayerEvents;
import com.aeriaplugins.managers.FileManager;
import com.aeriaplugins.utils.AeriaBoard;
import com.aeriaplugins.utils.ChatUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin {

    private final Set<UUID> playersHidden = new HashSet<>();
    private final Map<UUID, AeriaBoard> boards = new HashMap<>(); 
    private Object bossBarObject; 
    
    private int actionBarIndex = 0;
    private FileManager fileManager;
    private Economy econ = null;
    private boolean isLegacy = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        try {
            Class.forName("org.bukkit.NamespacedKey");
            isLegacy = false;
        } catch (ClassNotFoundException e) {
            isLegacy = true;
        }
        
        fileManager = new FileManager(this);
        fileManager.loadAll();
        
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        
        getServer().getPluginManager().registerEvents(new PlayerEvents(this), this);
        getServer().getPluginManager().registerEvents(new MenuEvents(this), this);
        
        getCommand("aeriamenus").setExecutor(new AeriaCommand(this));
        
        if (getConfig().getBoolean("modulos.usar-placeholderapi") && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI ativado na config, mas nao foi encontrado no servidor!");
        }

        if (getConfig().getBoolean("modulos.usar-vault")) {
            if (!setupEconomy()) {
                getLogger().warning("Módulo Vault ativo na config, mas o plugin Vault ou um plugin de economia compativel nao foi encontrado!");
            } else {
                getLogger().info("Integraçao com o Vault realizada com sucesso!");
            }
        } else {
            getLogger().info("O módulo do Vault foi desativado na config.yml pelo administrador.");
        }
        
        startTasks();
        setupBossBar();
        
        getLogger().info("AeriaMenus carregado com sucesso! Suporte Hibrido (1.8.8 - 26.1.2) Ativo.");
    }

    @Override
    public void onDisable() {
        try {
            if (bossBarObject != null && bossBarObject instanceof BossBar) {
                ((BossBar) bossBarObject).removeAll();
            }
        } catch (Throwable ignored) {}
        
        for (AeriaBoard board : boards.values()) {
            board.delete();
        }
        boards.clear();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public void setupBossBar() {
        if (isLegacy) return;
        
        try {
            if (bossBarObject != null && bossBarObject instanceof BossBar) {
                ((BossBar) bossBarObject).removeAll();
            }
            if (getConfig().getBoolean("modulos.ativar-bossbar")) {
                BarColor bc = BarColor.valueOf(getConfig().getString("bossbar.cor", "BLUE"));
                BarStyle bs = BarStyle.valueOf(getConfig().getString("bossbar.estilo", "SOLID"));
                
                BossBar bar = Bukkit.createBossBar("Carregando...", bc, bs);
                bar.setProgress(getConfig().getDouble("bossbar.progresso", 1.0));
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    bar.addPlayer(p);
                }
                
                bossBarObject = bar;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (bossBarObject != null && bossBarObject instanceof BossBar) {
                            BossBar currentBar = (BossBar) bossBarObject;
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                 String title = ChatUtils.color(p, getConfig().getString("bossbar.titulo").replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())), getConfig().getBoolean("modulos.usar-placeholderapi"));
                                 currentBar.setTitle(title);
                            }
                        }
                    }
                }.runTaskTimer(this, 0L, 40L);
            }
        } catch (Throwable e) {
            bossBarObject = null;
        }
    }

    private void startTasks() {
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
                            try {
                                p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
                            } catch (Throwable ignored) {}
                        }
                    }
                }.runTaskTimer(this, 0L, ticks);
            }
        }

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
                        try {
                            p.setPlayerListHeaderFooter(headerB.toString().trim(), footerB.toString().trim());
                        } catch (Throwable ignored) {}
                    }
                }
            }.runTaskTimer(this, 0L, ticks);
        }

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
        
        if (getConfig().getBoolean("modulos.ativar-scoreboard")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    boolean usePapi = getConfig().getBoolean("modulos.usar-placeholderapi");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        AeriaBoard board = boards.get(player.getUniqueId());
                        if (board != null) {
                            String titulo = ChatUtils.color(player, getConfig().getString("scoreboard.titulo"), usePapi);
                            board.updateTitle(titulo);

                            List<String> linhasRaw = getConfig().getStringList("scoreboard.linhas");
                            List<String> linhasFormatadas = new java.util.ArrayList<>();
                            for (String linha : linhasRaw) {
                                linhasFormatadas.add(ChatUtils.color(player, linha.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())), usePapi));
                            }
                            board.updateLines(linhasFormatadas);
                        }
                    }
                }
            }.runTaskTimer(this, 0L, 20L);
        }
    }
    
    public Set<UUID> getPlayersHidden() { return playersHidden; }
    public Map<UUID, AeriaBoard> getBoards() { return boards; }
    public FileManager getFileManager() { return fileManager; }
    public Economy getEconomy() { return econ; } 
    public boolean isLegacy() { return isLegacy; }
    
    public BossBar getBossBar() { 
        if (bossBarObject instanceof BossBar) {
            return (BossBar) bossBarObject;
        }
        return null;
    } 
}