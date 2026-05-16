package com.anne.plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
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

    private BukkitRunnable gameTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLocations();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("dontmove").setExecutor(this);
        getLogger().info("Plugin DontMove habilitado com sucesso!");
    }

    @Override
    public void onDisable() {
        stopGame();
        getLogger().info("Plugin DontMove desabilitado.");
    }

    private void loadLocations() {
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
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Uso: /dontmove <setlobby|setstart|setfinish|entrar|start|stop>");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "setlobby":
                if (!player.hasPermission("dontmove.admin")) return true;
                lobbyLocation = player.getLocation();
                getConfig().set("locations.lobby", lobbyLocation);
                saveConfig();
                player.sendMessage(ChatColor.GREEN + "Lobby definido com sucesso!");
                break;

            case "setstart":
                if (!player.hasPermission("dontmove.admin")) return true;
                startLocation = player.getLocation();
                getConfig().set("locations.start", startLocation);
                isFinishGreater = finishZ > startLocation.getZ();
                saveConfig();
                player.sendMessage(ChatColor.GREEN + "Local de inicio definido com sucesso!");
                break;

            case "setfinish":
                if (!player.hasPermission("dontmove.admin")) return true;
                finishZ = player.getLocation().getZ();
                getConfig().set("locations.finishZ", finishZ);
                if (startLocation != null) {
                    isFinishGreater = finishZ > startLocation.getZ();
                }
                saveConfig();
                player.sendMessage(ChatColor.GREEN + "Linha de chegada (Eixo Z: " + finishZ + ") definida com sucesso!");
                break;

            case "entrar":
                if (gameState != GameState.LOBBY) {
                    player.sendMessage(ChatColor.RED + "O jogo ja esta em andamento!");
                    return true;
                }
                if (lobbyLocation == null) {
                    player.sendMessage(ChatColor.RED + "O lobby ainda nao foi definido.");
                    return true;
                }
                if (!activePlayers.contains(player.getUniqueId())) {
                    activePlayers.add(player.getUniqueId());
                    player.teleport(lobbyLocation);
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    Bukkit.broadcastMessage(ChatColor.AQUA + player.getName() + " entrou no jogo! (" + activePlayers.size() + " jogadores)");
                } else {
                    player.sendMessage(ChatColor.RED + "Voce ja esta no jogo.");
                }
                break;

            case "start":
                if (!player.hasPermission("dontmove.admin")) return true;
                if (startLocation == null || lobbyLocation == null) {
                    player.sendMessage(ChatColor.RED + "Defina o lobby, start e finish antes de iniciar.");
                    return true;
                }
                if (activePlayers.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Nao ha jogadores suficientes.");
                    return true;
                }
                startGame();
                break;

            case "stop":
                if (!player.hasPermission("dontmove.admin")) return true;
                stopGame();
                Bukkit.broadcastMessage(ChatColor.RED + "O jogo foi cancelado por um administrador.");
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

        for (UUID uuid : activePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(startLocation);
                sendTitle1_8(p, ChatColor.RED + "Don't Move!", ChatColor.YELLOW + "Prepare-se para correr...");
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
            }
        }

        gameTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (activePlayers.isEmpty()) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Ninguem sobreviveu ao jogo!");
                    stopGame();
                    return;
                }

                if (ticks == 0) {
                    lightState = LightState.RUN;
                    broadcastTitle(ChatColor.GREEN + "CORRA!", "");
                } else if (ticks == 60) {
                    broadcastTitle(ChatColor.YELLOW + "ATENCAO...", "");
                } else if (ticks == 90) {
                    lightState = LightState.STOP;
                    broadcastTitle(ChatColor.RED + "NAO SE MOVA!", "");
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
        Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " se moveu e foi eliminado!");

        if (activePlayers.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.RED + "Todos os jogadores foram eliminados. Fim de jogo!");
            stopGame();
        }
    }

    private void winGame(Player player) {
        activePlayers.remove(player.getUniqueId());
        winners.add(player.getUniqueId());
        Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " cruzou a linha de chegada e sobreviveu!");
        player.teleport(lobbyLocation);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        if (activePlayers.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "O jogo acabou! Vencedores: " + winners.size());
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