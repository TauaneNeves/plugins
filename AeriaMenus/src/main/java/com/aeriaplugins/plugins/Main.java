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
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin {

    private final Set<UUID> playersHidden = new HashSet<>();
    private final Map<UUID, AeriaBoard> boards = new HashMap<>(); 
    
    private int actionBarIndex = 0;
    private FileManager fileManager;
    private Economy econ = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
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
        
        getLogger().info("AeriaMenus carregado com sucesso! Suporte Hibrido Habilitado.");
    }

    @Override
    public void onDisable() {
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
                                Object textComp = Class.forName("net.md_5.bungee.api.chat.TextComponent")
                                        .getMethod("fromLegacyText", String.class).invoke(null, msg);
                                
                                try {
                                    Class<?> chatMsgTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");
                                    Object actionBarEnum = chatMsgTypeClass.getField("ACTION_BAR").get(null);
                                    Method sendMsgMethod = p.spigot().getClass().getMethod("sendMessage", chatMsgTypeClass, Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;"));
                                    sendMsgMethod.invoke(p.spigot(), actionBarEnum, textComp);
                                } catch (Throwable t) {
                                    Method sendMsgLegacy = p.spigot().getClass().getMethod("sendMessage", Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;"));
                                    sendMsgLegacy.invoke(p.spigot(), textComp);
                                }
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
                        
                        String hStr = headerB.toString().trim();
                        String fStr = footerB.toString().trim();
                        
                        try {
                            Method modernMethod = p.getClass().getMethod("setPlayerListHeaderFooter", String.class, String.class);
                            modernMethod.invoke(p, hStr, fStr);
                        } catch (Throwable t1) {
                            try {
                                Class<?> componentClass = Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;");
                                Object headerComp = Class.forName("net.md_5.bungee.api.chat.TextComponent").getMethod("fromLegacyText", String.class).invoke(null, hStr);
                                Object footerComp = Class.forName("net.md_5.bungee.api.chat.TextComponent").getMethod("fromLegacyText", String.class).invoke(null, fStr);
                                
                                Method legacyMethod = p.getClass().getMethod("setPlayerListHeaderFooter", componentClass, componentClass);
                                legacyMethod.invoke(p, headerComp, footerComp);
                            } catch (Throwable t2) {
                                try {
                                    Object craftPlayer = p.getClass().getMethod("getHandle").invoke(p);
                                    Object playerConnection = craftPlayer.getClass().getField("playerConnection").get(craftPlayer);
                                    Class<?> ichat = Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".IChatBaseComponent");
                                    Class<?> chatSerializer = Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".IChatBaseComponent$ChatSerializer");
                                    
                                    Object tabHeader = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + hStr + "\"}");
                                    Object tabFooter = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + fStr + "\"}");
                                    
                                    Class<?> packetClass = Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".PacketPlayOutPlayerListHeaderFooter");
                                    Constructor<?> packetConstructor = packetClass.getConstructor(ichat);
                                    Object packet = packetConstructor.newInstance(tabHeader);
                                    packet.getClass().getField("b").set(packet, tabFooter);
                                    
                                    playerConnection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".Packet")).invoke(playerConnection, packet);
                                } catch (Throwable ignored) {}
                            }
                        }
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
                    int onlineCount = Bukkit.getOnlinePlayers().size();
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        AeriaBoard board = boards.get(player.getUniqueId());
                        if (board != null) {
                            String titulo = ChatUtils.color(player, getConfig().getString("scoreboard.titulo"), usePapi);
                            board.updateTitle(titulo);

                            List<String> linhasRaw = getConfig().getStringList("scoreboard.linhas");
                            List<String> linhasFormatadas = new java.util.ArrayList<>();
                            for (String linha : linhasRaw) {
                                // Substituição direta e segura antes do parse de cores para evitar o bug do f1
                                String processada = linha.replace("%online%", String.valueOf(onlineCount));
                                linhasFormatadas.add(ChatUtils.color(player, processada, usePapi));
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
}