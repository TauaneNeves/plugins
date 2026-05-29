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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// ========================================================================
// CLASSE PRINCIPAL: Main
// É aqui que o seu plugin "nasce". Tudo começa a partir desta classe.
// ========================================================================
public class Main extends JavaPlugin {

    // Lista de jogadores que estão com a visibilidade oculta (usaram o relógio)
    private final Set<UUID> playersHidden = new HashSet<>();

    // Lista que guarda as Scoreboards de cada jogador
    private final Map<UUID, AeriaBoard> boards = new HashMap<>();
    
    // Lista que guarda as Bossbars de cada jogador via Reflexão (Compatibilidade 1.8)
    private final Map<UUID, Object> bossBars = new HashMap<>();

    private int actionBarIndex = 0;
    private FileManager fileManager;
    private Economy econ = null;

    // ------------------------------------------------------------------------
    // MÉTODO: onEnable (Quando o servidor LIGA)
    // Aqui registramos os eventos, os comandos e iniciamos as configurações.
    // ------------------------------------------------------------------------
    @Override
    public void onEnable() {
        // Salva o config.yml padrão caso não exista
        saveDefaultConfig();

        // Carrega os arquivos de configuração (mensagens, menus, etc)
        fileManager = new FileManager(this);
        fileManager.loadAll();

        // Tenta conectar com o sistema de economia (Vault)
        setupEconomy();

        // Registra o canal de comunicação para enviar jogadores a outros servidores
        // (BungeeCord)
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Registra as classes que vão ficar "ouvindo" os acontecimentos do jogo
        getServer().getPluginManager().registerEvents(new PlayerEvents(this), this);
        getServer().getPluginManager().registerEvents(new MenuEvents(this), this);

        // Registra o comando /aeriamenus (ou /am)
        getCommand("aeriamenus").setExecutor(new AeriaCommand(this));

        // Inicia as tarefas que ficam rodando no fundo (Scoreboard, Actionbar, etc)
        startTasks();

        getLogger().info("AeriaMenus carregado com sucesso! Suporte Hibrido Habilitado.");
    }

    // ------------------------------------------------------------------------
    // MÉTODO: onDisable (Quando o servidor DESLIGA)
    // Aqui limpamos os dados para evitar que o servidor fique sobrecarregado.
    // ------------------------------------------------------------------------
    @Override
    public void onDisable() {
        for (AeriaBoard board : boards.values()) {
            board.delete();
        }
        boards.clear();
        
        // Limpa as Bossbars ativas via reflexão
        try {
            Class<?> bossBarClass = Class.forName("org.bukkit.boss.BossBar");
            Method removeAllMethod = bossBarClass.getMethod("removeAll");
            for (Object bar : bossBars.values()) {
                removeAllMethod.invoke(bar);
            }
        } catch (Throwable ignored) {}
        bossBars.clear();
    }

    // ------------------------------------------------------------------------
    // MÉTODO: reloadPlugin
    // Usado pelo comando /am reload. Ele para tudo, recarrega os arquivos e volta.
    // ------------------------------------------------------------------------
    public void reloadPlugin() {
        Bukkit.getScheduler().cancelTasks(this);
        fileManager.loadAll();
        startTasks();
    }

    // ------------------------------------------------------------------------
    // MÉTODO: setupEconomy
    // Prepara a conexão com o plugin Vault para usar dinheiro nos menus.
    // ------------------------------------------------------------------------
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    // ------------------------------------------------------------------------
    // MÉTODO: startTasks
    // Liga os relógios (Timers) que executam ações de tempo em tempo.
    // ------------------------------------------------------------------------
    private void startTasks() {

        // 1. TAREFA: Action Bar Rotativa (Mensagens em cima da barra de itens)
        if (getConfig().getBoolean("modulos.ativar-actionbar-rotativa")) {
            long ticks = getConfig().getLong("actionbar.tempo-entre-mensagens") * 20L;
            new BukkitRunnable() {
                @Override
                public void run() {
                    List<String> mensagens = getConfig().getStringList("actionbar.mensagens");
                    if (mensagens.isEmpty() || Bukkit.getOnlinePlayers().isEmpty())
                        return;
                    String rawMsg = mensagens.get(actionBarIndex++ % mensagens.size());
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        String msg = ChatUtils.color(p, rawMsg, getConfig().getBoolean("modulos.usar-placeholderapi"));
                        try {
                            Object textComp = Class.forName("net.md_5.bungee.api.chat.TextComponent")
                                    .getMethod("fromLegacyText", String.class).invoke(null, msg);
                            try {
                                Class<?> chatMsgTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");
                                Object actionBarEnum = chatMsgTypeClass.getField("ACTION_BAR").get(null);
                                Method sendMsgMethod = p.spigot().getClass().getMethod("sendMessage", chatMsgTypeClass,
                                        Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;"));
                                sendMsgMethod.invoke(p.spigot(), actionBarEnum, textComp);
                            } catch (Throwable t) {
                                Method sendMsgLegacy = p.spigot().getClass().getMethod("sendMessage",
                                        Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;"));
                                sendMsgLegacy.invoke(p.spigot(), textComp);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }.runTaskTimer(this, 0L, ticks);
        }

        // 2. TAREFA: Tablist Animada (A lista de jogadores apertando TAB)
        if (getConfig().getBoolean("modulos.ativar-tablist-animada")) {
            long ticks = getConfig().getLong("tablist.tempo-atualizacao", 20L);
            new BukkitRunnable() {
                @Override
                public void run() {
                    boolean usePapi = getConfig().getBoolean("modulos.usar-placeholderapi");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        StringBuilder headerB = new StringBuilder();
                        for (String h : getConfig().getStringList("tablist.header"))
                            headerB.append(ChatUtils.color(p, h, usePapi)).append("\n");
                        StringBuilder footerB = new StringBuilder();
                        for (String f : getConfig().getStringList("tablist.footer"))
                            footerB.append(ChatUtils.color(p, f, usePapi)).append("\n");

                        String hStr = headerB.toString().isEmpty() ? "" : headerB.toString().substring(0, headerB.length() - 1);
                        String fStr = footerB.toString().isEmpty() ? "" : footerB.toString().substring(0, footerB.length() - 1);

                        try {
                            // 1. Tenta a API moderna (1.13 a 1.21+) de forma segura
                            org.bukkit.entity.Player.class.getMethod("setPlayerListHeader", String.class).invoke(p, hStr);
                            org.bukkit.entity.Player.class.getMethod("setPlayerListFooter", String.class).invoke(p, fStr);
                        } catch (Throwable t1) {
                            try {
                                // 2. Tenta a API combinada caso exista
                                org.bukkit.entity.Player.class.getMethod("setPlayerListHeaderFooter", String.class, String.class).invoke(p, hStr, fStr);
                            } catch (Throwable t2) {
                                try {
                                    // 3. Fallback NMS (1.8 a 1.12)
                                    Object craftPlayer = p.getClass().getMethod("getHandle").invoke(p);
                                    Object connection = craftPlayer.getClass().getField("playerConnection").get(craftPlayer);
                                    String nmsVersion = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                                    Class<?> chatSerializer = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent$ChatSerializer");
                                    Class<?> packetClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutPlayerListHeaderFooter");
                                    
                                    // Escapar quebras de linha (\n) e aspas evita a quebra fatal do JSON
                                    String safeHeader = hStr.replace("\"", "\\\"").replace("\n", "\\n");
                                    String safeFooter = fStr.replace("\"", "\\\"").replace("\n", "\\n");
                                    
                                    Object headerComp = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + safeHeader + "\"}");
                                    Object footerComp = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + safeFooter + "\"}");
                                    
                                    Object packet = packetClass.getConstructor(Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent")).newInstance(headerComp);
                                    
                                    java.lang.reflect.Field bField = packet.getClass().getDeclaredField("b");
                                    bField.setAccessible(true);
                                    bField.set(packet, footerComp);
                                    
                                    connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + nmsVersion + ".Packet")).invoke(connection, packet);
                                } catch (Throwable t3) {
                                    // Falha silenciosa final
                                }
                            }
                        }
                    }
                }
            }.runTaskTimer(this, 0L, ticks);
        }
        
        // 4. TAREFA: Atualizar a Scoreboard (Quadro lateral)
        if (getConfig().getBoolean("modulos.ativar-scoreboard")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    boolean usePapi = getConfig().getBoolean("modulos.usar-placeholderapi");
                    int onlineCount = Bukkit.getOnlinePlayers().size();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        AeriaBoard board = boards.get(player.getUniqueId());
                        if (board != null) {
                            board.updateTitle(
                                    ChatUtils.color(player, getConfig().getString("scoreboard.titulo"), usePapi));
                            List<String> linhasRaw = getConfig().getStringList("scoreboard.linhas");
                            List<String> linhasFormatadas = new ArrayList<>();
                            for (String linha : linhasRaw) {
                                linhasFormatadas.add(ChatUtils.color(player,
                                        linha.replace("%online%", String.valueOf(onlineCount)), usePapi));
                            }
                            board.updateLines(linhasFormatadas);
                        }
                    }
                }
            }.runTaskTimer(this, 0L, 20L);
        }

        // 5. TAREFA: Bossbar (Atualização via Reflexão Híbrida)
        try {
            Class<?> bossBarClass = Class.forName("org.bukkit.boss.BossBar");
            Method removeAllMethod = bossBarClass.getMethod("removeAll");
            for (Object b : bossBars.values()) {
                removeAllMethod.invoke(b);
            }
        } catch (Throwable ignored) {}
        bossBars.clear();

        if (getConfig().contains("bossbar")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
                        Class<?> bossBarClass = Class.forName("org.bukkit.boss.BossBar");
                        Class<?> barColorClass = Class.forName("org.bukkit.boss.BarColor");
                        Class<?> barStyleClass = Class.forName("org.bukkit.boss.BarStyle");
                        Class<?> barFlagClass = Class.forName("org.bukkit.boss.BarFlag");
                        Class<?> barFlagArrayClass = java.lang.reflect.Array.newInstance(barFlagClass, 0).getClass();
                        
                        Method createBossBarMethod = bukkitClass.getMethod("createBossBar", String.class, barColorClass, barStyleClass, barFlagArrayClass);
                        Method setTitleMethod = bossBarClass.getMethod("setTitle", String.class);
                        Method setColorMethod = bossBarClass.getMethod("setColor", barColorClass);
                        Method setStyleMethod = bossBarClass.getMethod("setStyle", barStyleClass);
                        Method setProgressMethod = bossBarClass.getMethod("setProgress", double.class);
                        Method addPlayerMethod = bossBarClass.getMethod("addPlayer", Player.class);

                        boolean usePapi = getConfig().getBoolean("modulos.usar-placeholderapi");
                        String rawTitle = getConfig().getString("bossbar.titulo");
                        
                        Object color = barColorClass.getMethod("valueOf", String.class).invoke(null, getConfig().getString("bossbar.cor", "BLUE").toUpperCase());
                        Object style = barStyleClass.getMethod("valueOf", String.class).invoke(null, getConfig().getString("bossbar.estilo", "SOLID").toUpperCase());
                        double progress = getConfig().getDouble("bossbar.progresso", 1.0);

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            Object bar = bossBars.get(p.getUniqueId());
                            if (bar == null) {
                                Object flagsArray = java.lang.reflect.Array.newInstance(barFlagClass, 0);
                                bar = createBossBarMethod.invoke(null, "", color, style, flagsArray);
                                addPlayerMethod.invoke(bar, p);
                                bossBars.put(p.getUniqueId(), bar);
                            }
                            setTitleMethod.invoke(bar, ChatUtils.color(p, rawTitle, usePapi));
                            setColorMethod.invoke(bar, color);
                            setStyleMethod.invoke(bar, style);
                            setProgressMethod.invoke(bar, progress);
                        }
                    } catch (Throwable ignored) {}
                }
            }.runTaskTimer(Main.this, 0L, 20L);
        }
    }

    // Métodos para outras classes conseguirem acessar as listas de jogadores
    // ocultos, scoreboards, etc.
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
}