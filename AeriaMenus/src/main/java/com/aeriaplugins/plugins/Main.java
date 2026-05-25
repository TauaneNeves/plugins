package com.aeriaplugins.plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<UUID> playersHidden = new HashSet<>();
    private BossBar bossBar;
    
    private NamespacedKey lobbyItemKey;
    private NamespacedKey menuItemKey;
    private int actionBarIndex = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("aeriamenus").setExecutor(this);
        
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        
        lobbyItemKey = new NamespacedKey(this, "lobby_item_id");
        menuItemKey = new NamespacedKey(this, "menu_item_id");
        
        if (getConfig().getBoolean("modulos.usar-placeholderapi") && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI ativado na config, mas nao foi encontrado no servidor!");
        }
        
        startScoreboardTask();
        startTimeTask();
        setupBossBar();
        startActionBarTask();
        startTablistTask();
        
        getLogger().info("AeriaMenus carregado com sucesso! (Modo SuperLobby Premium Ativo)");
    }

    @Override
    public void onDisable() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public String color(Player p, String message) {
        if (message == null) return "";
        
        if (p != null && getConfig().getBoolean("modulos.usar-placeholderapi") && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, message);
        }

        Pattern gradientPattern = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>");
        Matcher gradientMatcher = gradientPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (gradientMatcher.find()) {
            String hexStart = gradientMatcher.group(1);
            String hexEnd = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);
            gradientMatcher.appendReplacement(buffer, applyGradient(text, hexStart, hexEnd));
        }
        gradientMatcher.appendTail(buffer);
        message = buffer.toString();

        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher hexMatcher = hexPattern.matcher(message);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexBuffer, net.md_5.bungee.api.ChatColor.of("#" + hexMatcher.group(1)).toString());
        }
        hexMatcher.appendTail(hexBuffer);

        return ChatColor.translateAlternateColorCodes('&', hexBuffer.toString());
    }

    private String applyGradient(String text, String hexStart, String hexEnd) {
        java.awt.Color start = java.awt.Color.decode("#" + hexStart);
        java.awt.Color end = java.awt.Color.decode("#" + hexEnd);
        StringBuilder sb = new StringBuilder();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1 == 0 ? 1 : length - 1);
            int red = (int) (start.getRed() * (1 - ratio) + end.getRed() * ratio);
            int green = (int) (start.getGreen() * (1 - ratio) + end.getGreen() * ratio);
            int blue = (int) (start.getBlue() * (1 - ratio) + end.getBlue() * ratio);
            sb.append(net.md_5.bungee.api.ChatColor.of(new java.awt.Color(red, green, blue)));
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }

    private void setupBossBar() {
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
                             String title = color(p, getConfig().getString("bossbar.titulo").replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())));
                             bossBar.setTitle(title);
                        }
                    }
                }
            }.runTaskTimer(this, 0L, 40L);
        }
    }

    private void startActionBarTask() {
        if (!getConfig().getBoolean("modulos.ativar-actionbar-rotativa")) return;
        
        long ticks = getConfig().getLong("actionbar.tempo-entre-mensagens") * 20L;
        List<String> mensagens = getConfig().getStringList("actionbar.mensagens");
        
        if (mensagens.isEmpty()) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) return;
                
                String rawMsg = mensagens.get(actionBarIndex % mensagens.size());
                actionBarIndex++;
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String msg = color(p, rawMsg);
                    p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
                }
            }
        }.runTaskTimer(this, 0L, ticks);
    }

    private void startTablistTask() {
        if (!getConfig().getBoolean("modulos.ativar-tablist-animada")) return;
        long ticks = getConfig().getLong("tablist.tempo-atualizacao", 20L);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    StringBuilder headerB = new StringBuilder();
                    for (String h : getConfig().getStringList("tablist.header")) {
                        headerB.append(color(p, h)).append("\n");
                    }
                    
                    StringBuilder footerB = new StringBuilder();
                    for (String f : getConfig().getStringList("tablist.footer")) {
                        footerB.append(color(p, f)).append("\n");
                    }
                    
                    p.setPlayerListHeaderFooter(headerB.toString().trim(), footerB.toString().trim());
                }
            }
        }.runTaskTimer(this, 0L, ticks);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!getConfig().getBoolean("modulos.ativar-chat")) return;
        String formato = color(event.getPlayer(), getConfig().getString("chat.formato")
                .replace("%message%", "%2$s").replace("%player_name%", "%1$s"));
        event.setFormat(formato);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        if (bossBar != null) {
            bossBar.addPlayer(p);
        }

        for (UUID uuid : playersHidden) {
            Player hidden = Bukkit.getPlayer(uuid);
            if (hidden != null) p.hidePlayer(this, hidden);
        }

        if (getConfig().getBoolean("modulos.ativar-mensagens-entrada")) {
            String msg = getConfig().getString("mensagens-entrada.entrou");
            if (msg == null || msg.isEmpty()) event.setJoinMessage(null);
            else event.setJoinMessage(color(p, msg));
        }

        if (getConfig().getBoolean("modulos.ativar-efeitos-entrada")) {
            String title = color(p, getConfig().getString("efeitos-entrada.titulo"));
            String subtitle = color(p, getConfig().getString("efeitos-entrada.subtitulo"));
            p.sendTitle(title, subtitle, 10, 70, 20);
            try { p.playSound(p.getLocation(), Sound.valueOf(getConfig().getString("efeitos-entrada.som")), 1.0f, 1.0f); } catch (Exception ignored) {}
        }

        if (getConfig().getBoolean("spawn.teleportar-ao-entrar")) teleportToSpawn(p);

        if (getConfig().getBoolean("modulos.dar-itens-ao-entrar")) {
            p.getInventory().clear();
            if (getConfig().contains("itens-entrada")) {
                for (String key : getConfig().getConfigurationSection("itens-entrada").getKeys(false)) {
                    String path = "itens-entrada." + key;
                    ItemStack i = parseItem(path, p);
                    if (i != null) {
                        ItemMeta mt = i.getItemMeta();
                        mt.getPersistentDataContainer().set(lobbyItemKey, PersistentDataType.STRING, key);
                        i.setItemMeta(mt);
                        p.getInventory().setItem(getConfig().getInt(path + ".slot"), i);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playersHidden.remove(event.getPlayer().getUniqueId());
        if (bossBar != null) {
            bossBar.removePlayer(event.getPlayer());
        }
        
        if (getConfig().getBoolean("modulos.ativar-mensagens-entrada")) {
            String msg = getConfig().getString("mensagens-entrada.saiu");
            if (msg == null || msg.isEmpty()) event.setQuitMessage(null);
            else event.setQuitMessage(color(event.getPlayer(), msg));
        }
    }

    private void startScoreboardTask() {
        if (!getConfig().getBoolean("modulos.ativar-scoreboard")) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) updateScoreboard(player);
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void startTimeTask() {
        if (!getConfig().getBoolean("modulos.ativar-clima-sempre-dia")) return;
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

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (getConfig().getBoolean("modulos.ativar-clima-sempre-dia") && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    private void updateScoreboard(Player player) {
        ScoreboardManager m = Bukkit.getScoreboardManager();
        if (m == null) return;
        Scoreboard b = m.getNewScoreboard();
        Objective o = b.registerNewObjective("aeria", "dummy", color(player, getConfig().getString("scoreboard.titulo")));
        o.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = getConfig().getStringList("scoreboard.linhas");
        int index = lines.size();
        for (String line : lines) {
            String f = color(player, line.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())));
            org.bukkit.scoreboard.Score score = o.getScore(f.isEmpty() ? ChatColor.values()[index % 15].toString() : f);
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
            } catch (Exception ignored) {
            }
        }
        player.setScoreboard(b);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (p.getLocation().getY() <= getConfig().getDouble("spawn.altura-void")) teleportToSpawn(p);
    }

    private void teleportToSpawn(Player p) {
        if (!getConfig().contains("spawn.local.mundo")) return;
        World w = Bukkit.getWorld(getConfig().getString("spawn.local.mundo"));
        if (w != null) p.teleport(new Location(w, getConfig().getDouble("spawn.local.x"), getConfig().getDouble("spawn.local.y"), getConfig().getDouble("spawn.local.z"), (float) getConfig().getDouble("spawn.local.yaw"), (float) getConfig().getDouble("spawn.local.pitch")));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack i = e.getItem();
        if (i == null || !i.hasItemMeta()) return;
        Player p = e.getPlayer();

        ItemMeta meta = i.getItemMeta();
        if (meta.getPersistentDataContainer().has(lobbyItemKey, PersistentDataType.STRING)) {
            e.setCancelled(true);
            if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                String key = meta.getPersistentDataContainer().get(lobbyItemKey, PersistentDataType.STRING);
                String path = "itens-entrada." + key + ".acoes";
                if (getConfig().contains(path)) {
                    execute(p, getConfig().getStringList(path));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        
        ItemMeta meta = event.getCurrentItem().getItemMeta();
        
        if (meta.getPersistentDataContainer().has(menuItemKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            String data = meta.getPersistentDataContainer().get(menuItemKey, PersistentDataType.STRING);
            String[] parts = data.split(";");
            if (parts.length == 2) {
                String menuKey = parts[0];
                String itemKey = parts[1];
                String path = "menus." + menuKey + ".itens." + itemKey + ".acoes";
                if (getConfig().contains(path)) {
                    execute(player, getConfig().getStringList(path));
                }
            }
            return;
        }

        boolean isCustomMenu = false;
        if (getConfig().contains("menus")) {
            for (String menuKey : getConfig().getConfigurationSection("menus").getKeys(false)) {
                String menuTitle = color((Player) event.getWhoClicked(), getConfig().getString("menus." + menuKey + ".titulo"));
                if (event.getView().getTitle().equals(menuTitle)) {
                    isCustomMenu = true;
                    break;
                }
            }
        }

        if (isCustomMenu) {
            event.setCancelled(true);
            return;
        }

        if (getConfig().getBoolean("modulos.dar-itens-ao-entrar") && !event.getWhoClicked().isOp()) {
            if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
                event.setCancelled(true);
            }
        }
    }

    private void execute(Player p, List<String> a) {
        if (a == null) return;
        for (String s : a) {
            if (s.startsWith("comando: ")) p.performCommand(s.substring(9));
            else if (s.startsWith("mensagem: ")) p.sendMessage(color(p, s.substring(10)));
            else if (s.startsWith("menu: ")) open(p, s.substring(6));
            else if (s.startsWith("especial: alternar_visibilidade")) toggleVisibility(p);
            else if (s.startsWith("servidor: ")) conectarServidor(p, s.substring(10));
            else if (s.startsWith("som: ")) {
                try { p.playSound(p.getLocation(), Sound.valueOf(s.substring(5).toUpperCase()), 1f, 1f); } catch(Exception ignored) {}
            }
            else if (s.equalsIgnoreCase("fechar")) p.closeInventory();
        }
    }

    private void conectarServidor(Player p, String serverName) {
        if (serverName.equalsIgnoreCase("configurar_aqui") || serverName.isEmpty()) {
            p.sendMessage(ChatColor.RED + "Ops! Este servidor ainda não foi configurado!");
            return;
        }
        
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(serverName); 
            p.sendPluginMessage(this, "BungeeCord", b.toByteArray());
        } catch (Exception e) {
            p.sendMessage(ChatColor.RED + "Erro ao tentar conectar ao servidor.");
        }
    }

    private void toggleVisibility(Player p) {
        if (playersHidden.contains(p.getUniqueId())) {
            playersHidden.remove(p.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) p.showPlayer(this, target);
            p.sendMessage(ChatColor.GREEN + "Jogadores visíveis!");
        } else {
            playersHidden.add(p.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) p.hidePlayer(this, target);
            p.sendMessage(ChatColor.RED + "Jogadores ocultos!");
        }
    }

    private ItemStack parseItem(String path, Player p) {
        String matStr = getConfig().getString(path + ".material");
        ItemStack item;
        
        if (matStr.startsWith("base64:")) {
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", matStr.substring(7)));
            meta.setPlayerProfile(profile);
            item.setItemMeta(meta);
        } else {
            try {
                item = new ItemStack(Material.valueOf(matStr.toUpperCase()));
            } catch (Exception e) {
                item = new ItemStack(Material.STONE);
            }
        }
        
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(color(p, getConfig().getString(path + ".nome")));
        
        if (getConfig().contains(path + ".custom-model-data") && getConfig().getInt(path + ".custom-model-data") > 0) {
            m.setCustomModelData(getConfig().getInt(path + ".custom-model-data"));
        }

        List<String> lore = new ArrayList<>();
        if (getConfig().contains(path + ".lore")) {
            for (String l : getConfig().getStringList(path + ".lore")) lore.add(color(p, l));
        }
        m.setLore(lore);
        item.setItemMeta(m);
        
        return item;
    }

    private void open(Player p, String k) {
        if (!getConfig().contains("menus." + k)) return;
        Inventory inv = Bukkit.createInventory(null, getConfig().getInt("menus."+k+".linhas")*9, color(p, getConfig().getString("menus."+k+".titulo")));
        
        if (getConfig().contains("menus." + k + ".itens")) {
            for (String iK : getConfig().getConfigurationSection("menus."+k+".itens").getKeys(false)) {
                String iP = "menus."+k+".itens."+iK;
                ItemStack i = parseItem(iP, p);
                
                if (i != null) {
                    ItemMeta m = i.getItemMeta();
                    m.getPersistentDataContainer().set(menuItemKey, PersistentDataType.STRING, k + ";" + iK);
                    i.setItemMeta(m);
                    inv.setItem(getConfig().getInt(iP+".slot"), i);
                }
            }
        }
        p.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (c.getName().equalsIgnoreCase("aeriamenus")) {
            if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
                if (s.isOp()) {
                    reloadConfig();
                    setupBossBar();
                    s.sendMessage(ChatColor.GREEN + "[AeriaMenus] Configuração recarregada com sucesso!");
                } else {
                    s.sendMessage(ChatColor.RED + "Você não tem permissão para usar isso.");
                }
            } else {
                s.sendMessage("§b§lAERIA MENUS §8- §7Central de Ajuda");
                s.sendMessage("§8--------------------------------------------------");
                s.sendMessage("§e/am reload §8- §7Recarrega a config.yml (Apenas OP)");
                s.sendMessage("§8--------------------------------------------------");
            }
            return true;
        }
        return true;
    }

    @EventHandler 
    public void onBreak(BlockBreakEvent e) { 
        if (getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); 
    }

    @EventHandler 
    public void onPlace(BlockPlaceEvent e) { 
        if (getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); 
    }

    @EventHandler 
    public void onDrop(PlayerDropItemEvent e) { 
        if (getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); 
    }

    @EventHandler 
    public void onFood(FoodLevelChangeEvent e) { 
        if (getConfig().getBoolean("modulos.ativar-protecoes")) e.setCancelled(true); 
    }

    @EventHandler 
    public void onDamage(EntityDamageEvent e) { 
        if (getConfig().getBoolean("modulos.ativar-protecoes") && e.getEntity() instanceof Player) e.setCancelled(true); 
    }
}