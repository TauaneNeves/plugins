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

    private Location esperaLocation;
    private Location saidaLocation;
    private Location startLocation;
    private double finishZ;
    private boolean isFinishGreater;
    
    private int maxPlayers;
    private int initialPlayerCount;

    private BukkitRunnable gameTask;

    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        setupDefaultConfig();
        loadConfigSettings();
        setupDataFile();
        
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("dontmove").setExecutor(this);
        getLogger().info("Plugin DontMove habilitado com Sucesso! Identidade visual atualizada.");
    }

    @Override
    public void onDisable() {
        stopGame();
        getLogger().info("Plugin DontMove desabilitado.");
    }

    private void checkAndSet(String path, Object value) {
        if (!getConfig().contains(path)) {
            getConfig().set(path, value);
        }
    }

    private void setupDefaultConfig() {
        checkAndSet("settings.max_players", 20);
        checkAndSet("settings.min_players_for_points", 4);
        checkAndSet("settings.enable_reward", true);
        checkAndSet("settings.reward_command", "eco give %player% 500");
        checkAndSet("settings.enable_consolation", true);
        checkAndSet("settings.consolation_command", "eco give %player% 50");

        checkAndSet("messages.prefix", "&8[&bDontMove&8] ");
        checkAndSet("messages.no_permission", "&cVoce nao tem permissao.");
        checkAndSet("messages.joined", "&b%player% entrou no jogo! (%current%/%max%)");
        checkAndSet("messages.already_in_game", "&cVoce ja esta no jogo.");
        checkAndSet("messages.left_game", "&eVoce saiu do minigame.");
        checkAndSet("messages.not_in_game", "&cVoce nao esta na partida.");
        checkAndSet("messages.arena_full", "&cA arena esta lotada! (%max% jogadores)");
        checkAndSet("messages.eliminated", "&c%player% se moveu e foi eliminado!");
        checkAndSet("messages.winner", "&a%player% cruzou a linha de chegada e sobreviveu!");
        checkAndSet("messages.game_over", "&6O jogo acabou! Vencedores: %winners%");
        checkAndSet("messages.nobody_survived", "&cNinguem sobreviveu ao jogo!");
        checkAndSet("messages.cancelled", "&cO jogo foi cancelado por um administrador.");
        checkAndSet("messages.need_setup", "&cDefina a Espera, Saida, Start e Finish antes de jogar.");
        checkAndSet("messages.not_enough_players", "&cNao ha jogadores suficientes.");
        checkAndSet("messages.top_1_join", "&6&lO Campeao Atual &e%player% &6&lentrou na arena!");
        checkAndSet("messages.earned_points", "&aVoce ganhou &e%points% pontos &apara o ranking da temporada!");
        checkAndSet("messages.no_points_farm", "&cPartida sem jogadores suficientes para pontuar no ranking. (Minimo: %min%)");
        checkAndSet("messages.game_started", "&aA partida comecou! Siga as instrucoes na tela!");

        checkAndSet("titles.prepare_title", "&cDon't Move!");
        checkAndSet("titles.prepare_subtitle", "&ePrepare-se para correr...");
        checkAndSet("titles.green_title", "&aCORRA!");
        checkAndSet("titles.green_subtitle", "");
        checkAndSet("titles.yellow_title", "&eATENCAO...");
        checkAndSet("titles.yellow_subtitle", "");
        checkAndSet("titles.red_title", "&cNAO SE MOVA!");
        checkAndSet("titles.red_subtitle", "");

        saveConfig();
    }

    private void loadConfigSettings() {
        if (getConfig().contains("locations.espera")) {
            esperaLocation = (Location) getConfig().get("locations.espera");
        }
        if (getConfig().contains("locations.saida")) {
            saidaLocation = (Location) getConfig().get("locations.saida");
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
        sender.sendMessage(ChatColor.GOLD + "--- TOP JOGADORES (DON'T MOVE) ---");
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

    private String getMsg(String path) {
        String prefix = getConfig().getString("messages.prefix", "&8[&bDontMove&8] ");
        String msg = getConfig().getString(path, "&c[Erro: Mensagem nao configurada no config.yml]");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    private String getRawMsg(String path) {
        String msg = getConfig().getString(path, "");
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private void sendHelpMenu(CommandSender sender) {
        String linha = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "---------------------------------------------";
        sender.sendMessage(linha);
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "                 DON'T MOVE MINIGAME");
        sender.sendMessage(linha);
        sender.sendMessage(ChatColor.YELLOW + "/dontmove entrar " + ChatColor.GRAY + "- Entra na sala de espera.");
        sender.sendMessage(ChatColor.YELLOW + "/dontmove sair " + ChatColor.GRAY + "- Sai do minigame.");
        sender.sendMessage(ChatColor.YELLOW + "/dontmove top " + ChatColor.GRAY + "- Mostra o ranking de jogadores.");

        if (sender.hasPermission("dontmove.admin")) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "   [COMANDOS ADMINISTRATIVOS]");
            sender.sendMessage(ChatColor.GOLD + "/dontmove start " + ChatColor.GRAY + "- Inicia a partida.");
            sender.sendMessage(ChatColor.GOLD + "/dontmove stop " + ChatColor.GRAY + "- Cancela a partida.");
            sender.sendMessage(ChatColor.GOLD + "/dontmove resetar " + ChatColor.GRAY + "- Zera o ranking.");
            sender.sendMessage(ChatColor.GOLD + "/dontmove setlimit <num> " + ChatColor.GRAY + "- Limite de jogadores.");
            sender.sendMessage(ChatColor.GOLD + "/dontmove setespera " + ChatColor.GRAY + "- Marca a Sala de Espera.");
            sender.sendMessage(ChatColor.GOLD + "/dontmove setsaida " + ChatColor.GRAY + "- Marca o Lobby Final.");
            sender.sendMessage(ChatColor.GOLD + "/dontmove setstart " + ChatColor.GRAY + "- Marca a Linha de Partida.");
            sender.sendMessage(ChatColor.GOLD + "/dontmove setfinish " + ChatColor.GRAY + "- Marca a Linha de Chegada.");
        }
        sender.sendMessage(linha);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMenu(sender);
            return true;
        }

        String action = args[0].toLowerCase();

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
            sender.sendMessage(ChatColor.GREEN + "Ranking resetado com sucesso!");
            Bukkit.broadcastMessage(getMsg("messages.prefix") + ChatColor.AQUA + "A Temporada do Don't Move foi resetada! Pontos zerados.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando especifico.");
            return true;
        }

        Player player = (Player) sender;

        switch (action) {
            case "setespera":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("messages.no_permission"));
                    return true;
                }
                esperaLocation = player.getLocation();
                getConfig().set("locations.espera", esperaLocation);
                saveConfig();
                player.sendMessage(getMsg("messages.prefix") + ChatColor.GREEN + "Sala de Espera definida com sucesso!");
                break;
                
            case "setsaida":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("messages.no_permission"));
                    return true;
                }
                saidaLocation = player.getLocation();
                getConfig().set("locations.saida", saidaLocation);
                saveConfig();
                player.sendMessage(getMsg("messages.prefix") + ChatColor.GREEN + "Lobby de Saida definido com sucesso!");
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
                player.sendMessage(getMsg("messages.prefix") + ChatColor.GREEN + "Linha de Partida definida!");
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
                player.sendMessage(getMsg("messages.prefix") + ChatColor.GREEN + "Linha de Chegada definida!");
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
                if (esperaLocation == null || saidaLocation == null) {
                    player.sendMessage(getMsg("messages.need_setup"));
                    return true;
                }
                if (activePlayers.size() >= maxPlayers && !activePlayers.contains(player.getUniqueId())) {
                    player.sendMessage(getMsg("messages.arena_full").replace("%max%", String.valueOf(maxPlayers)));
                    return true;
                }
                if (!activePlayers.contains(player.getUniqueId())) {
                    activePlayers.add(player.getUniqueId());
                    player.teleport(esperaLocation);
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    
                    String joinMsg = getMsg("messages.joined")
                            .replace("%player%", player.getName())
                            .replace("%current%", String.valueOf(activePlayers.size()))
                            .replace("%max%", String.valueOf(maxPlayers));
                    Bukkit.broadcastMessage(joinMsg);

                    String top1 = getTop1Name();
                    if (top1 != null && top1.equals(player.getName())) {
                        Bukkit.broadcastMessage(getMsg("messages.top_1_join").replace("%player%", player.getName()));
                    }

                } else {
                    player.sendMessage(getMsg("messages.already_in_game"));
                }
                break;
                
            case "sair":
                if (activePlayers.contains(player.getUniqueId())) {
                    if (gameState == GameState.PLAYING) {
                        eliminatePlayer(player);
                    } else {
                        activePlayers.remove(player.getUniqueId());
                        if (saidaLocation != null) player.teleport(saidaLocation);
                        player.sendMessage(getMsg("messages.left_game"));
                        Bukkit.broadcastMessage(getMsg("messages.prefix") + ChatColor.YELLOW + player.getName() + " saiu da sala.");
                    }
                } else {
                    player.sendMessage(getMsg("messages.not_in_game"));
                }
                break;

            case "start":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("messages.no_permission"));
                    return true;
                }
                if (startLocation == null || esperaLocation == null || saidaLocation == null) {
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
                sendHelpMenu(sender);
                break;
        }

        return true;
    }

    private void startGame() {
        gameState = GameState.PLAYING;
        lightState = LightState.RUN;
        winners.clear();
        initialPlayerCount = activePlayers.size();
        
        Bukkit.broadcastMessage(getMsg("messages.game_started"));

        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(startLocation);
                broadcastPhaseToPlayer(p, getRawMsg("titles.prepare_title"), getRawMsg("titles.prepare_subtitle"), ChatColor.RED + "Prepare-se...");
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
                    broadcastGlobalPhase(getRawMsg("titles.green_title"), getRawMsg("titles.green_subtitle"), ChatColor.GREEN + "=== CORRA! ===");
                } else if (ticks == 60) {
                    broadcastGlobalPhase(getRawMsg("titles.yellow_title"), getRawMsg("titles.yellow_subtitle"), ChatColor.YELLOW + "=== ATENCAO... ===");
                } else if (ticks == 90) {
                    lightState = LightState.STOP;
                    broadcastGlobalPhase(getRawMsg("titles.red_title"), getRawMsg("titles.red_subtitle"), ChatColor.RED + "=== PAROU! NAO SE MOVA! ===");
                } else if (ticks >= 150) {
                    ticks = -1; 
                }

                ticks++;
            }
        };
        gameTask.runTaskTimer(this, 30L, 1L); 
    }

    private void stopGame() {
        if (gameState == GameState.PLAYING || gameState == GameState.LOBBY) {
            for (UUID uuid : activePlayers) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && saidaLocation != null) {
                    p.teleport(saidaLocation);
                }
            }
        }
        
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
        
        if (saidaLocation != null) {
            player.teleport(saidaLocation);
        }
        
        Bukkit.broadcastMessage(getMsg("messages.eliminated").replace("%player%", player.getName()));

        if (getConfig().getBoolean("settings.enable_consolation", false)) {
            String cmd = getConfig().getString("settings.consolation_command").replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            player.sendMessage(getMsg("messages.prefix") + ChatColor.YELLOW + "Você recebeu um premio!");
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
        
        if (saidaLocation != null) {
            player.teleport(saidaLocation);
        }
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        if (getConfig().getBoolean("settings.enable_reward", false)) {
            String cmd = getConfig().getString("settings.reward_command").replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        int minPlayers = getConfig().getInt("settings.min_players_for_points", 4);
        if (initialPlayerCount >= minPlayers) {
            int earnedPoints = initialPlayerCount;
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

    private void broadcastGlobalPhase(String title, String subtitle, String fallbackText) {
        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                broadcastPhaseToPlayer(p, title, subtitle, fallbackText);
            }
        }
    }

    private void broadcastPhaseToPlayer(Player player, String title, String subtitle, String fallbackText) {
        sendTitle1_8(player, title, subtitle);
        sendActionBar(player, fallbackText);
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
            if (title != null && !title.isEmpty()) player.sendMessage(title);
            if (subtitle != null && !subtitle.isEmpty()) player.sendMessage(subtitle);
        }
    }

    private void sendActionBar(Player player, String message) {
        try {
            Object chatMsg = getNMSClass("IChatBaseComponent$ChatSerializer").getMethod("a", String.class).invoke(null, "{\"text\":\"" + message + "\"}");
            Constructor<?> constructor = getNMSClass("PacketPlayOutChat").getConstructor(getNMSClass("IChatBaseComponent"), byte.class);
            Object packet = constructor.newInstance(chatMsg, (byte) 2);
            sendPacket(player, packet);
        } catch (Exception e) {
            player.sendMessage(message);
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
        } catch (Exception e) {
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
        Player player = event.getPlayer();
        if (activePlayers.contains(player.getUniqueId())) {
            if (gameState == GameState.PLAYING) {
                eliminatePlayer(player);
            } else {
                activePlayers.remove(player.getUniqueId());
            }
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