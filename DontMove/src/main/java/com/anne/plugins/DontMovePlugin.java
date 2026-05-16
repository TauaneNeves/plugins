package com.anne.plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DontMovePlugin extends JavaPlugin implements Listener {

    private enum GameState { LOBBY, PLAYING }
    private enum LightState { RUN, STOP }

    private GameState gameState = GameState.LOBBY;
    private LightState lightState = LightState.RUN;

    private List<UUID> activePlayers = new ArrayList<>();
    private List<UUID> winners = new ArrayList<>();

    private Location lobbyLocation;
    private Location startLocation;
    private double finishZ;
    private boolean isFinishGreater;
    
    private int maxPlayers;
    private int initialPlayerCount;

    private BukkitRunnable gameTask;

    // Ficheiro para guardar os pontos do Ranking
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        setupDefaultConfig();
        loadConfigSettings();
        setupDataFile();
        
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("dontmove").setExecutor(this);
        getLogger().info("Plugin DontMove habilitado com Sucesso! Sistema de Ranking ativo.");
    }

    @Override
    public void onDisable() {
        stopGame();
        getLogger().info("Plugin DontMove desabilitado.");
    }

    private void setupDefaultConfig() {
        getConfig().addDefault("settings.max_players", 20);
        
        // Configuracoes de Premios e Anti-Farm
        getConfig().addDefault("settings.min_players_for_points", 4);
        
        getConfig().addDefault("settings.enable_reward", true);
        getConfig().addDefault("settings.reward_command", "eco give %player% 500");
        
        getConfig().addDefault("settings.enable_consolation", true);
        getConfig().addDefault("settings.consolation_command", "eco give %player% 50");

        // Mensagens
        getConfig().addDefault("messages.prefix", "&8[&cRound6&8] ");
        getConfig().addDefault("messages.no_permission", "&cVoce nao tem permissao.");
        getConfig().addDefault("messages.joined", "&b%player% entrou no jogo! (%current%/%max%)");
        getConfig().addDefault("messages.already_in_game", "&cVoce ja esta no jogo.");
        getConfig().addDefault("messages.arena_full", "&cA arena esta lotada! (%max% jogadores)");
        getConfig().addDefault("messages.eliminated", "&c%player% se moveu e foi eliminado!");
        getConfig().addDefault("messages.winner", "&a%player% cruzou a linha de chegada e sobreviveu!");
        getConfig().addDefault("messages.game_over", "&6O jogo acabou! Vencedores: %winners%");
        getConfig().addDefault("messages.nobody_survived", "&cNinguem sobreviveu ao jogo!");
        getConfig().addDefault("messages.cancelled", "&cO jogo foi cancelado por um administrador.");
        getConfig().addDefault("messages.need_setup", "&cDefina o lobby, start e finish antes de iniciar.");
        getConfig().addDefault("messages.not_enough_players", "&cNao ha jogadores suficientes.");
        getConfig().addDefault("messages.top_1_join", "&6&lO Campeao Atual &e%player% &6&lentrou na arena!");
        getConfig().addDefault("messages.earned_points", "&aVoce ganhou &e%points% pontos &apara o ranking da temporada!");
        getConfig().addDefault("messages.no_points_farm", "&cPartida sem jogadores suficientes para pontuar no ranking. (Minimo: %min%)");

        // Titulos no ecra
        getConfig().addDefault("titles.prepare_title", "&cDon't Move!");
        getConfig().addDefault("titles.prepare_subtitle", "&ePrepare-se para correr...");
        getConfig().addDefault("titles.green_title", "&aCORRA!");
        getConfig().addDefault("titles.green_subtitle", "");
        getConfig().addDefault("titles.yellow_title", "&eATENCAO...");
        getConfig().addDefault("titles.yellow_subtitle", "");
        getConfig().addDefault("titles.red_title", "&cNAO SE MOVA!");
        getConfig().addDefault("titles.red_subtitle", "");

        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    private void loadConfigSettings() {
        if (getConfig().contains("locations.lobby")) {
            lobbyLocation = (Location) getConfig().get("locations.lobby");
        }
        if (getConfig().contains("locations.start")) {
            startLocation = (Location) getConfig().get("locations.start");
        }
        finishZ = getConfig().getDouble("locations.finishZ", 0.0);
        
        if (startLocation != null) {
            isFinishGreater = finishZ > startLocation.getZ();
        }
        
        maxPlayers = getConfig().getInt("settings.max_players", 20);
    }

    // ==========================================
    // SISTEMA DE DADOS (JOGADORES E RANKING)
    // ==========================================
    private void setupDataFile() {
        dataFile = new File(getDataFolder(), "jogadores.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void addPoints(UUID uuid, String name, int amount) {
        String path = "jogadores." + uuid.toString();
        int current = dataConfig.getInt(path + ".pontos", 0);
        dataConfig.set(path + ".nome", name);
        dataConfig.set(path + ".pontos", current + amount);
        saveData();
    }

    private String getTop1Name() {
        if (dataConfig.getConfigurationSection("jogadores") == null) return null;
        String topName = null;
        int maxPts = 0;
        for (String uuidStr : dataConfig.getConfigurationSection("jogadores").getKeys(false)) {
            int pts = dataConfig.getInt("jogadores." + uuidStr + ".pontos", 0);
            if (pts > maxPts) {
                maxPts = pts;
                topName = dataConfig.getString("jogadores." + uuidStr + ".nome");
            }
        }
        return topName;
    }

    private void showTop(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- TOP Jogadores (Round 6) ---");
        if (dataConfig.getConfigurationSection("jogadores") == null) {
            sender.sendMessage(ChatColor.RED + "Nenhum jogador pontuou ainda.");
            return;
        }
        
        Map<String, Integer> scores = new HashMap<>();
        for (String uuidStr : dataConfig.getConfigurationSection("jogadores").getKeys(false)) {
            String name = dataConfig.getString("jogadores." + uuidStr + ".nome", "Desconhecido");
            int pts = dataConfig.getInt("jogadores." + uuidStr + ".pontos", 0);
            scores.put(name, pts);
        }
        
        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int rank = 1;
        for (Map.Entry<String, Integer> entry : list) {
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(rank) + "o " + entry.getKey() + " - " + entry.getValue() + " pontos");
            rank++;
            if (rank > 10) break;
        }
    }
    // ==========================================

    private String getMsg(String path) {
        String prefix = getConfig().getString("messages.prefix", "");
        String msg = getConfig().getString(path, "");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    private String getRawMsg(String path) {
        String msg = getConfig().getString(path, "");
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso: /dontmove <setlobby|setstart|setfinish|setlimit|entrar|start|stop|top|resetar>");
            return true;
        }

        String action = args[0].toLowerCase();

        // Comandos que a consola tambem pode usar
        if (action.equals("top")) {
            showTop(sender);
            return true;
        }
        
        if (action.equals("resetar")) {
            if (!sender.hasPermission("dontmove.admin")) {
                sender.sendMessage(getMsg("messages.no_permission"));
                return true;
            }
            dataConfig.set("jogadores", null);
            saveData();
            sender.sendMessage(ChatColor.GREEN + "Ranking resetado com sucesso! Nova Temporada iniciada.");
            Bukkit.broadcastMessage(getMsg("messages.prefix") + ChatColor.AQUA + "A Temporada do Round 6 foi resetada! Os pontos foram zerados.");
            return true;
        }

        // Comandos apenas para jogadores
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando especifico.");
            return true;
        }

        Player player = (Player) sender;

        switch (action) {
            case "setlobby":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("messages.no_permission"));
                    return true;
                }
                lobbyLocation = player.getLocation();
                getConfig().set("locations.lobby", lobbyLocation);
                saveConfig();
                player.sendMessage(getMsg("messages.prefix") + ChatColor.GREEN + "Lobby definido com sucesso!");
                break;

            case "setstart":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("messages.no_permission"));
                    return true;
                }
                startLocation = player.getLocation();
                getConfig().set("locations.start", startLocation);
                isFinishGreater = finishZ > startLocation.getZ();
                saveConfig();
                player.sendMessage(getMsg("messages.prefix") + ChatColor.GREEN + "Local de inicio definido com sucesso!");
                break;

            case "setfinish":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("messages.no_permission"));
                    return true;
                }
                finishZ = player.getLocation().getZ();
                getConfig().set("locations.finishZ", finishZ);
                if (startLocation != null) {
                    isFinishGreater = finishZ > startLocation.getZ();
                }
                saveConfig();
                player.sendMessage(getMsg("messages.prefix") + ChatColor.GREEN + "Linha de chegada definida com sucesso!");
                break;

            case "setlimit":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("messages.no_permission"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /dontmove setlimit <numero>");
                    return true;
                }
                try {
                    maxPlayers = Integer.parseInt(args[1]);
                    getConfig().set("settings.max_players", maxPlayers);
                    saveConfig();
                    player.sendMessage(getMsg("messages.prefix") + ChatColor.GREEN + "Limite definido para: " + maxPlayers);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Digite um numero valido.");
                }
                break;

            case "entrar":
                if (gameState != GameState.LOBBY) {
                    player.sendMessage(getMsg("messages.prefix") + ChatColor.RED + "O jogo ja esta em andamento!");
                    return true;
                }
                if (lobbyLocation == null) {
                    player.sendMessage(getMsg("messages.need_setup"));
                    return true;
                }
                if (activePlayers.size() >= maxPlayers && !activePlayers.contains(player.getUniqueId())) {
                    player.sendMessage(getMsg("messages.arena_full").replace("%max%", String.valueOf(maxPlayers)));
                    return true;
                }
                if (!activePlayers.contains(player.getUniqueId())) {
                    activePlayers.add(player.getUniqueId());
                    player.teleport(lobbyLocation);
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    
                    String joinMsg = getMsg("messages.joined")
                            .replace("%player%", player.getName())
                            .replace("%current%", String.valueOf(activePlayers.size()))
                            .replace("%max%", String.valueOf(maxPlayers));
                    Bukkit.broadcastMessage(joinMsg);

                    // Anuncia o TOP 1 se ele entrar
                    String top1 = getTop1Name();
                    if (top1 != null && top1.equals(player.getName())) {
                        Bukkit.broadcastMessage(getMsg("messages.top_1_join").replace("%player%", player.getName()));
                    }

                } else {
                    player.sendMessage(getMsg("messages.already_in_game"));
                }
                break;

            case "start":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("messages.no_permission"));
                    return true;
                }
                if (startLocation == null || lobbyLocation == null) {
                    player.sendMessage(getMsg("messages.need_setup"));
                    return true;
                }
                if (activePlayers.isEmpty()) {
                    player.sendMessage(getMsg("messages.not_enough_players"));
                    return true;
                }
                startGame();
                break;

            case "stop":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("messages.no_permission"));
                    return true;
                }
                stopGame();
                Bukkit.broadcastMessage(getMsg("messages.cancelled"));
                break;

            default:
                player.sendMessage(ChatColor.RED + "Comando desconhecido.");
                break;
        }

        return true;
    }

    private void startGame() {
        gameState = GameState.PLAYING;
        lightState = LightState.RUN;
        winners.clear();
        initialPlayerCount = activePlayers.size(); // Guarda quantos comecaram para o calculo de pontos

        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(startLocation);
                sendTitle1_8(p, getRawMsg("titles.prepare_title"), getRawMsg("titles.prepare_subtitle"));
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
            }
        }

        gameTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (activePlayers.isEmpty()) {
                    Bukkit.broadcastMessage(getMsg("messages.nobody_survived"));
                    stopGame();
                    return;
                }

                if (ticks == 0) {
                    lightState = LightState.RUN;
                    broadcastTitle(getRawMsg("titles.green_title"), getRawMsg("titles.green_subtitle"));
                } else if (ticks == 60) {
                    broadcastTitle(getRawMsg("titles.yellow_title"), getRawMsg("titles.yellow_subtitle"));
                } else if (ticks == 90) {
                    lightState = LightState.STOP;
                    broadcastTitle(getRawMsg("titles.red_title"), getRawMsg("titles.red_subtitle"));
                } else if (ticks >= 150) {
                    ticks = -1; 
                }

                ticks++;
            }
        };
        gameTask.runTaskTimer(this, 30L, 1L); 
    }

    private void stopGame() {
        gameState = GameState.LOBBY;
        lightState = LightState.RUN;
        activePlayers.clear();
        winners.clear();
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
    }

    private void eliminatePlayer(Player player) {
        activePlayers.remove(player.getUniqueId());
        player.getWorld().strikeLightningEffect(player.getLocation());
        player.setHealth(0.0);
        
        Bukkit.broadcastMessage(getMsg("messages.eliminated").replace("%player%", player.getName()));

        // Premio de Consolacao
        if (getConfig().getBoolean("settings.enable_consolation", false)) {
            String cmd = getConfig().getString("settings.consolation_command").replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            player.sendMessage(getMsg("messages.prefix") + ChatColor.YELLOW + "Recebeste um premio de consolacao por teres participado!");
        }

        if (activePlayers.isEmpty()) {
            Bukkit.broadcastMessage(getMsg("messages.game_over").replace("%winners%", String.valueOf(winners.size())));
            stopGame();
        }
    }

    private void winGame(Player player) {
        activePlayers.remove(player.getUniqueId());
        winners.add(player.getUniqueId());
        
        Bukkit.broadcastMessage(getMsg("messages.winner").replace("%player%", player.getName()));
        player.teleport(lobbyLocation);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        // Sistema de Premio Financeiro / Item
        if (getConfig().getBoolean("settings.enable_reward", false)) {
            String cmd = getConfig().getString("settings.reward_command").replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        // Sistema de Pontos Anti-Farm
        int minPlayers = getConfig().getInt("settings.min_players_for_points", 4);
        if (initialPlayerCount >= minPlayers) {
            int earnedPoints = initialPlayerCount; // Ganha 1 ponto por cada pessoa que iniciou a partida
            addPoints(player.getUniqueId(), player.getName(), earnedPoints);
            player.sendMessage(getMsg("messages.earned_points").replace("%points%", String.valueOf(earnedPoints)));
        } else {
            player.sendMessage(getMsg("messages.no_points_farm").replace("%min%", String.valueOf(minPlayers)));
        }

        if (activePlayers.isEmpty()) {
            Bukkit.broadcastMessage(getMsg("messages.game_over").replace("%winners%", String.valueOf(winners.size())));
            stopGame();
        }
    }

    private void broadcastTitle(String title, String subtitle) {
        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                sendTitle1_8(p, title, subtitle);
            }
        }
    }

    private void sendTitle1_8(Player player, String title, String subtitle) {
        try {
            Object enumTimes = getNMSClass("PacketPlayOutTitle$EnumTitleAction").getField("TIMES").get(null);
            Constructor<?> timeConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle$EnumTitleAction"), getNMSClass("IChatBaseComponent"), int.class, int.class, int.class);
            Object packetTimes = timeConstructor.newInstance(enumTimes, null, 10, 40, 10);
            sendPacket(player, packetTimes);

            if (title != null && !title.isEmpty()) {
                Object enumTitle = getNMSClass("PacketPlayOutTitle$EnumTitleAction").getField("TITLE").get(null);
                Object chatTitle = getNMSClass("IChatBaseComponent$ChatSerializer").getMethod("a", String.class).invoke(null, "{\"text\":\"" + title + "\"}");
                Constructor<?> titleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle$EnumTitleAction"), getNMSClass("IChatBaseComponent"));
                Object packetTitle = titleConstructor.newInstance(enumTitle, chatTitle);
                sendPacket(player, packetTitle);
            }

            if (subtitle != null && !subtitle.isEmpty()) {
                Object enumSubtitle = getNMSClass("PacketPlayOutTitle$EnumTitleAction").getField("SUBTITLE").get(null);
                Object chatSubtitle = getNMSClass("IChatBaseComponent$ChatSerializer").getMethod("a", String.class).invoke(null, "{\"text\":\"" + subtitle + "\"}");
                Constructor<?> subtitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle$EnumTitleAction"), getNMSClass("IChatBaseComponent"));
                Object packetSubtitle = subtitleConstructor.newInstance(enumSubtitle, chatSubtitle);
                sendPacket(player, packetSubtitle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Class<?> getNMSClass(String name) {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            return Class.forName("net.minecraft.server." + version + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (gameState != GameState.PLAYING) return;

        Player player = event.getPlayer();
        if (!activePlayers.contains(player.getUniqueId())) return;

        double currentZ = player.getLocation().getZ();

        if (isFinishGreater) {
            if (currentZ >= finishZ) {
                winGame(player);
                return;
            }
        } else {
            if (currentZ <= finishZ) {
                winGame(player);
                return;
            }
        }

        if (lightState == LightState.STOP) {
            double diffX = Math.abs(event.getFrom().getX() - event.getTo().getX());
            double diffZ = Math.abs(event.getFrom().getZ() - event.getTo().getZ());
            
            if (diffX > 0.02 || diffZ > 0.02) {
                eliminatePlayer(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (gameState == GameState.PLAYING && activePlayers.contains(event.getPlayer().getUniqueId())) {
            eliminatePlayer(event.getPlayer());
        } else {
            activePlayers.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (gameState == GameState.PLAYING && activePlayers.contains(player.getUniqueId())) {
                if (event.getCause() != EntityDamageEvent.DamageCause.CUSTOM && event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) {
                    event.setCancelled(true);
                }
            }
        }
    }
}