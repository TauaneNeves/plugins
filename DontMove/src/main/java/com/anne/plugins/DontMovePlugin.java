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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
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
    double finishZ;
    boolean isFinishGreater;
    
    private int maxPlayers;
    private int initialPlayerCount;

    private BukkitRunnable gameTask;

    private File dataFile;
    private FileConfiguration dataConfig;

    private boolean licensed = false;
    private boolean isLegacy = true;
    private String lang = "en";

    @Override
    public void onEnable() {
        setupDefaultConfig();
        loadConfigSettings();
        setupDataFile();

        try {
            Player.class.getMethod("sendTitle", String.class, String.class);
            isLegacy = false; 
        } catch (Exception e) {
            isLegacy = true;  
        }

        String key = getConfig().getString("settings.license_key", "YOUR_KEY_HERE");

        // Link oficial do seu Pastebin Oculto integrado diretamente no codigo
        String pastebinURL = "https://pastebin.com/raw/L8V86x7D";

        getLogger().info("[AuraPlugins] Verificando autenticacao remota da licenca via nuvem...");
        if (checkPastebinLicense(key, pastebinURL)) {
            this.licensed = true;
            getLogger().info("[AuraPlugins] Licenca AUTENTICADA com sucesso! Obrigado por comprar na AuraPlugins.");
        } else {
            this.licensed = false;
            getLogger().severe("=========================================================");
            getLogger().severe("[AuraPlugins] LICENCA INVALIDA, EXPIRADA OU PIRATEADA!");
            getLogger().severe("[AuraPlugins] Insira sua chave valida no config.yml.");
            getLogger().severe("[AuraPlugins] O plugin DontMove foi DESATIVADO.");
            getLogger().severe("[AuraPlugins] Suporte: suporte.auraplugins@gmail.com");
            getLogger().severe("=========================================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("dontmove").setExecutor(this);
    }

    @Override
    public void onDisable() {
        stopGame();
        getLogger().info("[AuraPlugins] Plugin DontMove desabilitado.");
    }

    private boolean checkPastebinLicense(String key, String urlString) {
        if (key.equals("YOUR_KEY_HERE") || key.isEmpty()) {
            return false;
        }
        
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.trim().equals(key.trim())) {
                        in.close();
                        return true; 
                    }
                }
                in.close();
            }
        } catch (Exception e) {
            getLogger().severe("[AuraPlugins] Falha ao conectar ao servidor de autenticacao central.");
            return false; 
        }
        return false;
    }

    private void checkAndSet(String path, Object value) {
        if (!getConfig().contains(path)) {
            getConfig().set(path, value);
        }
    }

    private void setupDefaultConfig() {
        checkAndSet("settings.license_key", "YOUR_KEY_HERE");
        checkAndSet("settings.language", "en"); 

        checkAndSet("settings.max_players", 20);
        checkAndSet("settings.min_players_for_points", 4);
        checkAndSet("settings.enable_reward", true);
        checkAndSet("settings.reward_command", "eco give %player% 500");
        checkAndSet("settings.enable_consolation", true);
        checkAndSet("settings.consolation_command", "eco give %player% 50");

        checkAndSet("messages.prefix", "&8[&bDontMove&8] ");
        
        // Mensagens em Ingles (en)
        checkAndSet("messages.en.no_permission", "&cYou do not have permission.");
        checkAndSet("messages.en.joined", "&b%player% joined the game! (%current%/%max%)");
        checkAndSet("messages.en.already_in_game", "&cYou are already in the game.");
        checkAndSet("messages.en.left_game", "&eYou left the minigame.");
        checkAndSet("messages.en.not_in_game", "&cYou are not in the game.");
        checkAndSet("messages.en.arena_full", "&cThe arena is full! (%max% players)");
        checkAndSet("messages.en.eliminated", "&c%player% moved and was eliminated!");
        checkAndSet("messages.en.winner", "&a%player% crossed the finish line and survived!");
        checkAndSet("messages.en.game_over", "&6Game over! Winners: %winners%");
        checkAndSet("messages.en.nobody_survived", "&cNobody survived the game!");
        checkAndSet("messages.en.cancelled", "&cThe game was cancelled by an administrator.");
        checkAndSet("messages.en.need_setup", "&cPlease set Espera, Saida, Start and Finish locations first.");
        checkAndSet("messages.en.not_enough_players", "&cThere are not enough players.");
        checkAndSet("messages.en.top_1_join", "&6&lThe Current Champion &e%player% &6&lentered the arena!");
        checkAndSet("messages.en.earned_points", "&aYou earned &e%points% points &afor the season ranking!");
        checkAndSet("messages.en.no_points_farm", "&cNot enough players to earn ranking points. (Minimum: %min%)");
        checkAndSet("messages.en.game_started", "&aThe game has started! Follow the instructions on your screen!");
        checkAndSet("messages.en.admin.espera_set", "&aWaiting Room set successfully!");
        checkAndSet("messages.en.admin.saida_set", "&aExit Lobby set successfully!");
        checkAndSet("messages.en.admin.start_set", "&aStarting Line set successfully!");
        checkAndSet("messages.en.admin.finish_set", "&aFinish Line set successfully!");
        checkAndSet("messages.en.admin.limit_set", "&aPlayer limit set to: %limit%");
        checkAndSet("messages.en.admin.ranking_reset", "&aRanking reset successfully!");
        checkAndSet("messages.en.admin.ranking_reset_global", "&eThe Season has been reset! All points were wiped.");
        checkAndSet("messages.en.admin.reload", "&aConfiguration files reloaded successfully!");
        checkAndSet("titles.en.prepare_title", "&cDon't Move!");
        checkAndSet("titles.en.prepare_subtitle", "&ePrepare to run...");
        checkAndSet("titles.en.green_title", "&aRUN!");
        checkAndSet("titles.en.green_subtitle", "");
        checkAndSet("titles.en.yellow_title", "&eATTENTION...");
        checkAndSet("titles.en.yellow_subtitle", "");
        checkAndSet("titles.en.red_title", "&cDON'T MOVE!");
        checkAndSet("titles.en.red_subtitle", "");

        // Mensagens em Portugues (pt)
        checkAndSet("messages.pt.no_permission", "&cVoce nao tem permissao.");
        checkAndSet("messages.pt.joined", "&b%player% entrou no jogo! (%current%/%max%)");
        checkAndSet("messages.pt.already_in_game", "&cVoce ja esta no jogo.");
        checkAndSet("messages.pt.left_game", "&eVoce saiu do minigame.");
        checkAndSet("messages.pt.not_in_game", "&cVoce nao esta na partida.");
        checkAndSet("messages.pt.arena_full", "&cA arena esta lotada! (%max% jogadores)");
        checkAndSet("messages.pt.eliminated", "&c%player% se moveu e foi eliminado!");
        checkAndSet("messages.pt.winner", "&a%player% cruzou a linha de chegada e sobreviveu!");
        checkAndSet("messages.pt.game_over", "&6O jogo acabou! Vencedores: %winners%");
        checkAndSet("messages.pt.nobody_survived", "&cNinguem sobreviveu ao jogo!");
        checkAndSet("messages.pt.cancelled", "&cO jogo foi cancelado por um administrador.");
        checkAndSet("messages.pt.need_setup", "&cDefina a Espera, Saida, Start e Finish antes de jogar.");
        checkAndSet("messages.pt.not_enough_players", "&cNao ha jogadores suficientes.");
        checkAndSet("messages.pt.top_1_join", "&6&lO Campeao Atual &e%player% &6&lentrou na arena!");
        checkAndSet("messages.pt.earned_points", "&aVoce ganhou &e%points% pontos &apara o ranking da temporada!");
        checkAndSet("messages.pt.no_points_farm", "&cPartida sem jogadores suficientes para pontuar no ranking. (Minimo: %min%)");
        checkAndSet("messages.pt.game_started", "&aA partida comecou! Siga as instrucoes na tela!");
        checkAndSet("messages.pt.admin.espera_set", "&aSala de Espera definida com sucesso!");
        checkAndSet("messages.pt.admin.saida_set", "&aLobby de Saida definido com sucesso!");
        checkAndSet("messages.pt.admin.start_set", "&aLinha de Partida definida!");
        checkAndSet("messages.pt.admin.finish_set", "&aLinha de Chegada definida!");
        checkAndSet("messages.pt.admin.limit_set", "&aLimite de jogadores definido para: %limit%");
        checkAndSet("messages.pt.admin.ranking_reset", "&aRanking resetado com sucesso!");
        checkAndSet("messages.pt.admin.ranking_reset_global", "&eA Temporada foi resetada! Os pontos foram zerados.");
        checkAndSet("messages.pt.admin.reload", "&aArquivos de configuracao recarregados com sucesso!");
        checkAndSet("titles.pt.prepare_title", "&cDon't Move!");
        checkAndSet("titles.pt.prepare_subtitle", "&ePrepare-se para correr...");
        checkAndSet("titles.pt.green_title", "&aCORRA!");
        checkAndSet("titles.pt.green_subtitle", "");
        checkAndSet("titles.pt.yellow_title", "&eATENCAO...");
        checkAndSet("titles.pt.yellow_subtitle", "");
        checkAndSet("titles.pt.red_title", "&cNAO SE MOVA!");
        checkAndSet("titles.pt.red_subtitle", "");

        saveConfig();
    }

    private void loadConfigSettings() {
        lang = getConfig().getString("settings.language", "en").toLowerCase();
        if (!lang.equals("en") && !lang.equals("pt")) {
            lang = "en"; 
        }

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
        if (lang.equals("pt")) {
            sender.sendMessage(ChatColor.GOLD + "--- TOP JOGADORES (DON'T MOVE) ---");
        } else {
            sender.sendMessage(ChatColor.GOLD + "--- TOP PLAYERS (DON'T MOVE) ---");
        }
        
        if (dataConfig.getConfigurationSection("jogadores") == null) {
            if (lang.equals("pt")) {
                sender.sendMessage(ChatColor.RED + "Nenhum jogador pontuou ainda.");
            } else {
                sender.sendMessage(ChatColor.RED + "No players have scored points yet.");
            }
            return;
        }
        
        Map<String, Integer> scores = new HashMap<>();
        for (String uuidStr : dataConfig.getConfigurationSection("jogadores").getKeys(false)) {
            String name = dataConfig.getString("jogadores." + uuidStr + ".nome", "Unknown");
            int pts = dataConfig.getInt("jogadores." + uuidStr + ".pontos", 0);
            scores.put(name, pts);
        }
        
        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int rank = 1;
        for (Map.Entry<String, Integer> entry : list) {
            String sufixo = lang.equals("pt") ? "o " : "# ";
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(rank) + sufixo + entry.getKey() + " - " + entry.getValue() + " " + (lang.equals("pt") ? "pontos" : "points"));
            rank++;
            if (rank > 10) break;
        }
    }

    private String getMsg(String path) {
        String prefix = getConfig().getString("messages.prefix", "&8[&bDontMove&8] ");
        String fullPath = "messages." + lang + "." + path;
        String msg = getConfig().getString(fullPath, getConfig().getString("messages.en." + path, "&c[Error]"));
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    private String getRawMsg(String path) {
        String fullPath = "titles." + lang + "." + path;
        String msg = getConfig().getString(fullPath, getConfig().getString("titles.en." + path, ""));
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private void sendHelpMenu(CommandSender sender, String label) {
        String ServerLine = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "---------------------------------------------";
        sender.sendMessage(ServerLine);
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "                 DON'T MOVE MINIGAME");
        sender.sendMessage(ServerLine);
        
        if (lang.equals("pt")) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " entrar " + ChatColor.GRAY + "- Entra na sala de espera.");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " sair " + ChatColor.GRAY + "- Sai do minigame.");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " top " + ChatColor.GRAY + "- Mostra o ranking de jogadores.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " entrar " + ChatColor.GRAY + "- Join the waiting lobby.");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " sair " + ChatColor.GRAY + "- Leave the minigame safely.");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " top " + ChatColor.GRAY + "- View the leaderboards.");
        }

        if (sender.hasPermission("dontmove.admin")) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + (lang.equals("pt") ? "   [COMANDOS ADMINISTRATIVOS]" : "   [ADMINISTRATIVE COMMANDS]"));
            
            if (lang.equals("pt")) {
                sender.sendMessage(ChatColor.GOLD + "/" + label + " start " + ChatColor.GRAY + "- Inicia a partida.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " stop " + ChatColor.GRAY + "- Cancela a partida.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " resetar " + ChatColor.GRAY + "- Zera o ranking.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " reload " + ChatColor.GRAY + "- Recarrega a config.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " setlimit <num> " + ChatColor.GRAY + "- Limite de jogadores.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " setespera " + ChatColor.GRAY + "- Marca a Sala de Espera.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " setsaida " + ChatColor.GRAY + "- Marca o Lobby Final.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " setstart " + ChatColor.GRAY + "- Marca a Linha de Partida.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " setfinish " + ChatColor.GRAY + "- Marca a Linha de Chegada.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "/" + label + " start " + ChatColor.GRAY + "- Starts the match.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " stop " + ChatColor.GRAY + "- Cancels the current match.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " resetar " + ChatColor.GRAY + "- Wipes the season ranking.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " reload " + ChatColor.GRAY + "- Reloads the configuration.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " setlimit <num> " + ChatColor.GRAY + "- Sets max players limit.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " setespera " + ChatColor.GRAY + "- Marks Waiting Room location.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " setsaida " + ChatColor.GRAY + "- Marks main server Lobby location.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " setstart " + ChatColor.GRAY + "- Marks the starting line.");
                sender.sendMessage(ChatColor.GOLD + "/" + label + " setfinish " + ChatColor.GRAY + "- Marks the finish line.");
            }
        }
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Desenvolvido por AuraPlugins");
        sender.sendMessage(ChatColor.DARK_AQUA + "Suporte: " + ChatColor.AQUA + "suporte.auraplugins@gmail.com");
        sender.sendMessage(ServerLine);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!licensed) {
            sender.sendMessage(ChatColor.RED + "Este plugin nao esta devidamente licenciado.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMenu(sender, label);
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("top")) {
            showTop(sender);
            return true;
        }
        
        if (action.equals("resetar")) {
            if (!sender.hasPermission("dontmove.admin")) {
                sender.sendMessage(getMsg("no_permission"));
                return true;
            }
            dataConfig.set("jogadores", null);
            saveData();
            sender.sendMessage(getMsg("admin.ranking_reset"));
            Bukkit.broadcastMessage(getMsg("admin.ranking_reset_global"));
            return true;
        }

        if (action.equals("reload")) {
            if (!sender.hasPermission("dontmove.admin")) {
                sender.sendMessage(getMsg("no_permission"));
                return true;
            }
            reloadConfig();
            loadConfigSettings();
            sender.sendMessage(getMsg("admin.reload"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;

        switch (action) {
            case "setespera":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("no_permission"));
                    return true;
                }
                esperaLocation = player.getLocation();
                getConfig().set("locations.espera", esperaLocation);
                saveConfig();
                player.sendMessage(getMsg("admin.espera_set"));
                break;
                
            case "setsaida":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("no_permission"));
                    return true;
                }
                saidaLocation = player.getLocation();
                getConfig().set("locations.saida", saidaLocation);
                saveConfig();
                player.sendMessage(getMsg("admin.saida_set"));
                break;

            case "setstart":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("no_permission"));
                    return true;
                }
                startLocation = player.getLocation();
                getConfig().set("locations.start", startLocation);
                isFinishGreater = finishZ > startLocation.getZ();
                saveConfig();
                player.sendMessage(getMsg("admin.start_set"));
                break;

            case "setfinish":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("no_permission"));
                    return true;
                }
                finishZ = player.getLocation().getZ();
                getConfig().set("locations.finishZ", finishZ);
                if (startLocation != null) {
                    isFinishGreater = finishZ > startLocation.getZ();
                }
                saveConfig();
                player.sendMessage(getMsg("admin.finish_set"));
                break;

            case "setlimit":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("no_permission"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + (lang.equals("pt") ? "Uso: /dontmove setlimit <numero>" : "Usage: /dontmove setlimit <number>"));
                    return true;
                }
                try {
                    maxPlayers = Integer.parseInt(args[1]);
                    getConfig().set("settings.max_players", maxPlayers);
                    saveConfig();
                    player.sendMessage(getMsg("admin.limit_set").replace("%limit%", String.valueOf(maxPlayers)));
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + (lang.equals("pt") ? "Digite um numero valido." : "Please enter a valid number."));
                }
                break;

            case "entrar":
                if (gameState != GameState.LOBBY) {
                    player.sendMessage(getMsg("prefix") + ChatColor.RED + (lang.equals("pt") ? "O jogo ja esta em andamento!" : "The game is already in progress!"));
                    return true;
                }
                if (esperaLocation == null || saidaLocation == null) {
                    player.sendMessage(getMsg("need_setup"));
                    return true;
                }
                if (activePlayers.size() >= maxPlayers && !activePlayers.contains(player.getUniqueId())) {
                    player.sendMessage(getMsg("arena_full").replace("%max%", String.valueOf(maxPlayers)));
                    return true;
                }
                if (!activePlayers.contains(player.getUniqueId())) {
                    activePlayers.add(player.getUniqueId());
                    player.teleport(esperaLocation);
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    
                    String joinMsg = getMsg("joined")
                            .replace("%player%", player.getName())
                            .replace("%current%", String.valueOf(activePlayers.size()))
                            .replace("%max%", String.valueOf(maxPlayers));
                    Bukkit.broadcastMessage(joinMsg);

                    String top1 = getTop1Name();
                    if (top1 != null && top1.equals(player.getName())) {
                        Bukkit.broadcastMessage(getMsg("top_1_join").replace("%player%", player.getName()));
                    }

                } else {
                    player.sendMessage(getMsg("already_in_game"));
                }
                break;
                
            case "sair":
                if (activePlayers.contains(player.getUniqueId())) {
                    if (gameState == GameState.PLAYING) {
                        eliminatePlayer(player);
                    } else {
                        activePlayers.remove(player.getUniqueId());
                        if (saidaLocation != null) player.teleport(saidaLocation);
                        player.sendMessage(getMsg("left_game"));
                        Bukkit.broadcastMessage(getMsg("prefix") + ChatColor.YELLOW + player.getName() + (lang.equals("pt") ? " saiu da sala." : " left the lobby."));
                    }
                } else {
                    player.sendMessage(getMsg("not_in_game"));
                }
                break;

            case "start":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("no_permission"));
                    return true;
                }
                if (startLocation == null || esperaLocation == null || saidaLocation == null) {
                    player.sendMessage(getMsg("need_setup"));
                    return true;
                }
                if (activePlayers.isEmpty()) {
                    player.sendMessage(getMsg("not_enough_players"));
                    return true;
                }
                startGame();
                break;

            case "stop":
                if (!player.hasPermission("dontmove.admin")) {
                    player.sendMessage(getMsg("no_permission"));
                    return true;
                }
                stopGame();
                Bukkit.broadcastMessage(getMsg("cancelled"));
                break;

            default:
                sendHelpMenu(sender, label);
                break;
        }

        return true;
    }

    private void startGame() {
        gameState = GameState.PLAYING;
        lightState = LightState.RUN;
        winners.clear();
        initialPlayerCount = activePlayers.size();
        
        Bukkit.broadcastMessage(getMsg("game_started"));

        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(startLocation);
                try {
                    p.playSound(p.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"), 1.0f, 1.0f);
                } catch (Exception e) {
                    p.playSound(p.getLocation(), Sound.valueOf("NOTE_PLING"), 1.0f, 1.0f);
                }
                broadcastPhaseToPlayer(p, getRawMsg("prepare_title"), getRawMsg("prepare_subtitle"), ChatColor.RED + (lang.equals("pt") ? "Prepare-se..." : "Get ready..."));
            }
        }

        gameTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (activePlayers.isEmpty()) {
                    Bukkit.broadcastMessage(getMsg("nobody_survived"));
                    stopGame();
                    return;
                }

                if (ticks == 0) {
                    lightState = LightState.RUN;
                    broadcastGlobalPhase(getRawMsg("green_title"), getRawMsg("green_subtitle"), ChatColor.GREEN + (lang.equals("pt") ? "=== CORRA! ===" : "=== RUN! ==="));
                } else if (ticks == 60) {
                    broadcastGlobalPhase(getRawMsg("yellow_title"), getRawMsg("yellow_subtitle"), ChatColor.YELLOW + (lang.equals("pt") ? "=== ATENCAO... ===" : "=== ATTENTION... ==="));
                } else if (ticks == 90) {
                    lightState = LightState.STOP;
                    broadcastGlobalPhase(getRawMsg("red_title"), getRawMsg("red_subtitle"), ChatColor.RED + (lang.equals("pt") ? "=== PAROU! NAO SE MOVA! ===" : "=== STOP! DON'T MOVE! ==="));
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
        
        Bukkit.broadcastMessage(getMsg("eliminated").replace("%player%", player.getName()));

        if (getConfig().getBoolean("settings.enable_consolation", false)) {
            String cmd = getConfig().getString("settings.consolation_command").replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            player.sendMessage(getMsg("prefix") + ChatColor.YELLOW + (lang.equals("pt") ? "Recebeste um premio de consolacao!" : "You received a consolation prize!"));
        }

        if (activePlayers.isEmpty()) {
            Bukkit.broadcastMessage(getMsg("game_over").replace("%winners%", String.valueOf(winners.size())));
            stopGame();
        }
    }

    private void winGame(Player player) {
        activePlayers.remove(player.getUniqueId());
        winners.add(player.getUniqueId());
        
        Bukkit.broadcastMessage(getMsg("winner").replace("%player%", player.getName()));
        
        if (saidaLocation != null) {
            player.teleport(saidaLocation);
        }
        
        try {
            player.playSound(player.getLocation(), Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 1.0f, 1.0f);
        } catch (Exception e) {
            player.playSound(player.getLocation(), Sound.valueOf("LEVEL_UP"), 1.0f, 1.0f);
        }

        if (getConfig().getBoolean("settings.enable_reward", false)) {
            String cmd = getConfig().getString("settings.reward_command").replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        int minPlayers = getConfig().getInt("settings.min_players_for_points", 4);
        if (initialPlayerCount >= minPlayers) {
            int earnedPoints = initialPlayerCount;
            addPoints(player.getUniqueId(), player.getName(), earnedPoints);
            player.sendMessage(getMsg("earned_points").replace("%points%", String.valueOf(earnedPoints)));
        } else {
            player.sendMessage(getMsg("no_points_farm").replace("%min%", String.valueOf(minPlayers)));
        }

        if (activePlayers.isEmpty()) {
            Bukkit.broadcastMessage(getMsg("game_over").replace("%winners%", String.valueOf(winners.size())));
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
        if (isLegacy) {
            sendTitle1_8(player, title, subtitle);
            sendActionBar1_8(player, fallbackText);
        } else {
            try {
                Method sendTitleMethod = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
                sendTitleMethod.invoke(player, title, subtitle, 10, 40, 10);
            } catch (Exception e) {
                player.sendMessage(title + " - " + subtitle);
            }
            player.sendMessage(fallbackText); 
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
            if (title != null && !title.isEmpty()) player.sendMessage(title);
            if (subtitle != null && !subtitle.isEmpty()) player.sendMessage(subtitle);
        }
    }

    private void sendActionBar1_8(Player player, String message) {
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
        if (!licensed) return;
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

    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String current = args[0].toLowerCase();
            if ("entrar".startsWith(current)) completions.add("entrar");
            if ("sair".startsWith(current)) completions.add("sair");
            if ("top".startsWith(current)) completions.add("top");
            if (sender.hasPermission("dontmove.admin")) {
                if ("start".startsWith(current)) completions.add("start");
                if ("stop".startsWith(current)) completions.add("stop");
                if ("resetar".startsWith(current)) completions.add("resetar");
                if ("reload".startsWith(current)) completions.add("reload");
                if ("setlimit".startsWith(current)) completions.add("setlimit");
                if ("setespera".startsWith(current)) completions.add("setespera");
                if ("setsaida".startsWith(current)) completions.add("setsaida");
                if ("setstart".startsWith(current)) completions.add("setstart");
                if ("setfinish".startsWith(current)) completions.add("setfinish");
            }
        }
        return completions;
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