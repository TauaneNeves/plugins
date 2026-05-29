package com.aeriaplugins.plugins;

import com.aeriaplugins.commands.AeriaCommand;
import com.aeriaplugins.listeners.MenuEvents;
import com.aeriaplugins.listeners.PlayerEvents;
import com.aeriaplugins.managers.FileManager;
import com.aeriaplugins.utils.AeriaBoard;
import com.aeriaplugins.utils.ChatUtils;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.*;

public class Main extends JavaPlugin {

    // Jogadores ocultos
    private final Set<UUID> playersHidden = new HashSet<>();

    // Scoreboards
    private final Map<UUID, AeriaBoard> boards = new HashMap<>();

    // BossBars
    private final Map<UUID, Object> bossBars = new HashMap<>();

    private int actionBarIndex = 0;

    private FileManager fileManager;

    // Vault Economy
    private Economy econ = null;

    // Vault Permissions
    private Permission perms = null;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        fileManager = new FileManager(this);
        fileManager.loadAll();

        // Vault
        setupEconomy();
        setupPermissions();

        // Eventos
        getServer().getPluginManager().registerEvents(new PlayerEvents(this), this);
        getServer().getPluginManager().registerEvents(new MenuEvents(this), this);

        // Comandos
        getCommand("aeriamenus").setExecutor(new AeriaCommand(this));

        // BungeeCord
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Tasks
        startTasks();

        getLogger().info("AeriaMenus carregado com sucesso!");
    }

    @Override
    public void onDisable() {

        for (AeriaBoard board : boards.values()) {
            board.delete();
        }

        boards.clear();

        try {

            Class<?> bossBarClass = Class.forName("org.bukkit.boss.BossBar");
            Method removeAllMethod = bossBarClass.getMethod("removeAll");

            for (Object bar : bossBars.values()) {
                removeAllMethod.invoke(bar);
            }

        } catch (Throwable ignored) {
        }

        bossBars.clear();
    }

    // =========================================================
    // RELOAD
    // =========================================================

    public void reloadPlugin() {

        Bukkit.getScheduler().cancelTasks(this);

        reloadConfig();

        fileManager.loadAll();

        startTasks();
    }

    // =========================================================
    // VAULT ECONOMY
    // =========================================================

    private boolean setupEconomy() {

        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null)
            return false;

        econ = rsp.getProvider();

        return econ != null;
    }

    // =========================================================
    // VAULT PERMISSIONS
    // =========================================================

    private boolean setupPermissions() {

        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);

        if (rsp == null)
            return false;

        perms = rsp.getProvider();

        return perms != null;
    }

    // =========================================================
    // TASKS
    // =========================================================

    private void startTasks() {

        // =====================================================
        // ACTIONBAR
        // =====================================================

        if (getConfig().getBoolean("modulos.ativar-actionbar-rotativa")) {

            long ticks = getConfig().getLong("actionbar.tempo-entre-mensagens") * 20L;

            new BukkitRunnable() {

                @Override
                public void run() {

                    List<String> mensagens = getConfig().getStringList("actionbar.mensagens");

                    if (mensagens.isEmpty())
                        return;

                    String rawMsg = mensagens.get(actionBarIndex++ % mensagens.size());

                    for (Player p : Bukkit.getOnlinePlayers()) {

                        String msg = ChatUtils.color(
                                p,
                                rawMsg,
                                getConfig().getBoolean("modulos.usar-placeholderapi"));

                        try {

                            Object textComp = Class.forName("net.md_5.bungee.api.chat.TextComponent")
                                    .getMethod("fromLegacyText", String.class)
                                    .invoke(null, msg);

                            try {

                                Class<?> chatMsgTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");

                                Object actionBarEnum = chatMsgTypeClass.getField("ACTION_BAR").get(null);

                                Method sendMsgMethod = p.spigot().getClass().getMethod(
                                        "sendMessage",
                                        chatMsgTypeClass,
                                        Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;"));

                                sendMsgMethod.invoke(
                                        p.spigot(),
                                        actionBarEnum,
                                        textComp);

                            } catch (Throwable t) {

                                Method sendMsgLegacy = p.spigot().getClass().getMethod(
                                        "sendMessage",
                                        Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;"));

                                sendMsgLegacy.invoke(
                                        p.spigot(),
                                        textComp);
                            }

                        } catch (Throwable ignored) {
                        }
                    }
                }

            }.runTaskTimer(this, 0L, ticks);
        }
        // =====================================================
        // TABLIST
        // =====================================================

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

                        String hStr = headerB.toString().isEmpty()
                                ? ""
                                : headerB.substring(0, headerB.length() - 1);

                        String fStr = footerB.toString().isEmpty()
                                ? ""
                                : footerB.substring(0, footerB.length() - 1);

                        try {

                            // =====================================================
                            // API MODERNA
                            // =====================================================

                            Player.class
                                    .getMethod("setPlayerListHeader", String.class)
                                    .invoke(p, hStr);

                            Player.class
                                    .getMethod("setPlayerListFooter", String.class)
                                    .invoke(p, fStr);

                        } catch (Throwable t1) {

                            try {

                                // =====================================================
                                // API COMBINADA
                                // =====================================================

                                Player.class
                                        .getMethod(
                                                "setPlayerListHeaderFooter",
                                                String.class,
                                                String.class)
                                        .invoke(p, hStr, fStr);

                            } catch (Throwable t2) {

                                try {

                                    // =====================================================
                                    // FALLBACK NMS (1.8 → 1.12)
                                    // =====================================================

                                    Object craftPlayer = p.getClass().getMethod("getHandle").invoke(p);

                                    Object connection = craftPlayer.getClass()
                                            .getField("playerConnection")
                                            .get(craftPlayer);

                                    String version = Bukkit.getServer()
                                            .getClass()
                                            .getPackage()
                                            .getName()
                                            .split("\\.")[3];

                                    Class<?> serializer = Class.forName(
                                            "net.minecraft.server."
                                                    + version
                                                    + ".IChatBaseComponent$ChatSerializer");

                                    Class<?> packetClass = Class.forName(
                                            "net.minecraft.server."
                                                    + version
                                                    + ".PacketPlayOutPlayerListHeaderFooter");

                                    String safeHeader = hStr.replace("\"", "\\\"")
                                            .replace("\n", "\\n");

                                    String safeFooter = fStr.replace("\"", "\\\"")
                                            .replace("\n", "\\n");

                                    Object header = serializer
                                            .getMethod("a", String.class)
                                            .invoke(
                                                    null,
                                                    "{\"text\":\"" + safeHeader + "\"}");

                                    Object footer = serializer
                                            .getMethod("a", String.class)
                                            .invoke(
                                                    null,
                                                    "{\"text\":\"" + safeFooter + "\"}");

                                    Object packet = packetClass
                                            .getConstructor(
                                                    Class.forName(
                                                            "net.minecraft.server."
                                                                    + version
                                                                    + ".IChatBaseComponent"))
                                            .newInstance(header);

                                    java.lang.reflect.Field footerField = packet.getClass().getDeclaredField("b");

                                    footerField.setAccessible(true);
                                    footerField.set(packet, footer);

                                    connection.getClass()
                                            .getMethod(
                                                    "sendPacket",
                                                    Class.forName(
                                                            "net.minecraft.server."
                                                                    + version
                                                                    + ".Packet"))
                                            .invoke(connection, packet);

                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    }
                }

            }.runTaskTimer(this, 0L, ticks);
        }
    
        // =====================================================
        // SCOREBOARD
        // =====================================================

        if (getConfig().getBoolean("modulos.ativar-scoreboard")) {

            new BukkitRunnable() {

                @Override
                public void run() {

                    boolean usePapi = getConfig().getBoolean("modulos.usar-placeholderapi");

                    int onlineCount = Bukkit.getOnlinePlayers().size();

                    for (Player player : Bukkit.getOnlinePlayers()) {

                        AeriaBoard board = boards.get(player.getUniqueId());

                        if (board == null)
                            continue;

                        // Atualiza título
                        board.updateTitle(
                                ChatUtils.color(
                                        player,
                                        getConfig().getString("scoreboard.titulo"),
                                        usePapi));

                        // Atualiza linhas
                        List<String> linhas = new ArrayList<>();

                        for (String linha : getConfig().getStringList("scoreboard.linhas")) {

                            linha = linha.replace(
                                    "%online%",
                                    String.valueOf(onlineCount));

                            linhas.add(
                                    ChatUtils.color(
                                            player,
                                            linha,
                                            usePapi));
                        }

                        board.updateLines(linhas);
                    }
                }

            }.runTaskTimer(this, 0L, 20L);
        }
    }

    // ------------------------------------------------------------------------
    // GETTERS
    // Permitem que outras classes acessem os sistemas do plugin
    // ------------------------------------------------------------------------

    public Set<UUID> getPlayersHidden() {
        return playersHidden;
    }

    public Map<UUID, AeriaBoard> getBoards() {
        return boards;
    }

    public Map<UUID, Object> getBossBars() {
        return bossBars;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public Economy getEconomy() {
        return econ;
    }

    public Permission getPermissions() {
        return perms;
    }

    public boolean hasVaultPermissions() {
        return perms != null;
    }
}
