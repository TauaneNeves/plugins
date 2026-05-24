package com.aeriaplugins.plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<UUID> playersHidden = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("aeriamenus").setExecutor(this);
        getCommand("setspawn").setExecutor(this);
        getCommand("spawn").setExecutor(this);
        
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        
        startScoreboardTask();
        startTimeTask();
        
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
            getLogger().info("AeriaMenus carregado (Suporte ao WorldEdit ATIVO)!");
        } else {
            getLogger().info("AeriaMenus carregado (WorldEdit nao encontrado).");
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!getConfig().getBoolean("modulos.ativar-chat")) return;
        String formato = ChatColor.translateAlternateColorCodes('&', getConfig().getString("chat.formato")
                .replace("%player%", "%1$s").replace("%message%", "%2$s"));
        event.setFormat(formato);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playersHidden.remove(event.getPlayer().getUniqueId());
        if (getConfig().getBoolean("modulos.ativar-mensagens-entrada")) {
            String msg = getConfig().getString("mensagens-entrada.saiu");
            if (msg == null || msg.isEmpty()) event.setQuitMessage(null);
            else event.setQuitMessage(ChatColor.translateAlternateColorCodes('&', msg.replace("%player%", event.getPlayer().getName())));
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
                    // Impede o erro verificando se o mundo é o ambiente normal (Não é o Nether ou o The End)
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
        Objective o = b.registerNewObjective("aeria", "dummy", ChatColor.translateAlternateColorCodes('&', getConfig().getString("scoreboard.titulo")));
        o.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = getConfig().getStringList("scoreboard.linhas");
        int index = lines.size();
        for (String line : lines) {
            String f = ChatColor.translateAlternateColorCodes('&', line.replace("%player%", player.getName()).replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())));
            o.getScore(f.isEmpty() ? ChatColor.values()[index % 15].toString() : f).setScore(index--);
        }
        player.setScoreboard(b);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        for (UUID uuid : playersHidden) {
            Player hidden = Bukkit.getPlayer(uuid);
            if (hidden != null) p.hidePlayer(this, hidden);
        }

        if (getConfig().getBoolean("modulos.ativar-mensagens-entrada")) {
            String msg = getConfig().getString("mensagens-entrada.entrou");
            if (msg == null || msg.isEmpty()) event.setJoinMessage(null);
            else event.setJoinMessage(ChatColor.translateAlternateColorCodes('&', msg.replace("%player%", p.getName())));
        }

        if (getConfig().getBoolean("modulos.ativar-efeitos-entrada")) {
            String title = ChatColor.translateAlternateColorCodes('&', getConfig().getString("efeitos-entrada.titulo").replace("%player%", p.getName()));
            String subtitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("efeitos-entrada.subtitulo").replace("%player%", p.getName()));
            p.sendTitle(title, subtitle, 10, 70, 20);
            try { p.playSound(p.getLocation(), Sound.valueOf(getConfig().getString("efeitos-entrada.som")), 1.0f, 1.0f); } catch (Exception ignored) {}
        }

        if (getConfig().getBoolean("spawn.teleportar-ao-entrar")) teleportToSpawn(p);

        if (getConfig().getBoolean("modulos.dar-itens-ao-entrar")) {
            p.getInventory().clear();
            for (String key : getConfig().getConfigurationSection("itens-entrada").getKeys(false)) {
                String path = "itens-entrada." + key;
                ItemStack i = new ItemStack(Material.valueOf(getConfig().getString(path + ".material")));
                ItemMeta mt = i.getItemMeta();
                mt.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString(path + ".nome")));
                
                List<String> lore = new ArrayList<>();
                for (String l : getConfig().getStringList(path + ".lore")) lore.add(ChatColor.translateAlternateColorCodes('&', l));
                mt.setLore(lore);
                i.setItemMeta(mt);
                p.getInventory().setItem(getConfig().getInt(path + ".slot"), i);
            }
        }
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
        if (i == null || !i.hasItemMeta() || !i.getItemMeta().hasDisplayName()) return;
        Player p = e.getPlayer();

        if (!getConfig().contains("itens-entrada")) return;
        for (String key : getConfig().getConfigurationSection("itens-entrada").getKeys(false)) {
            if (i.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', getConfig().getString("itens-entrada."+key+".nome")))) {
                e.setCancelled(true); 
                execute(p, getConfig().getStringList("itens-entrada."+key+".acoes"));
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        boolean isCustomMenu = false;
        String clickedMenuKey = null;

        if (getConfig().contains("menus")) {
            for (String menuKey : getConfig().getConfigurationSection("menus").getKeys(false)) {
                String menuTitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("menus." + menuKey + ".titulo"));
                if (event.getView().getTitle().equals(menuTitle)) {
                    isCustomMenu = true;
                    clickedMenuKey = menuKey;
                    break;
                }
            }
        }

        if (isCustomMenu) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

            String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
            String menuPath = "menus." + clickedMenuKey + ".itens";

            if (getConfig().contains(menuPath)) {
                for (String itemKey : getConfig().getConfigurationSection(menuPath).getKeys(false)) {
                    String itemPath = menuPath + "." + itemKey;
                    String configName = ChatColor.translateAlternateColorCodes('&', getConfig().getString(itemPath + ".nome"));

                    if (displayName.equals(configName)) {
                        execute(player, getConfig().getStringList(itemPath + ".acoes"));
                        player.closeInventory();
                        return;
                    }
                }
            }
            return;
        }

        if (getConfig().getBoolean("modulos.dar-itens-ao-entrar") && !player.isOp()) {
            event.setCancelled(true);
        }
    }

    private void execute(Player p, List<String> a) {
        if (a == null) return;
        for (String s : a) {
            if (s.startsWith("comando: ")) p.performCommand(s.substring(9));
            else if (s.startsWith("mensagem: ")) p.sendMessage(ChatColor.translateAlternateColorCodes('&', s.substring(10)));
            else if (s.startsWith("menu: ")) open(p, s.substring(6));
            else if (s.startsWith("especial: alternar_visibilidade")) toggleVisibility(p);
            else if (s.startsWith("servidor: ")) conectarServidor(p, s.substring(10));
        }
    }

    private void conectarServidor(Player p, String serverName) {
        if (serverName.equalsIgnoreCase("configurar_aqui") || serverName.isEmpty()) {
            p.sendMessage(ChatColor.RED + "Ops! Este servidor ainda nao foi configurado pela administracao!");
            p.sendMessage(ChatColor.GRAY + "Acesse o config.yml e mude o nome do servidor em 'servidor: configurar_aqui'.");
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

    private void open(Player p, String k) {
        if (!getConfig().contains("menus." + k)) return;
        Inventory inv = Bukkit.createInventory(null, getConfig().getInt("menus."+k+".linhas")*9, ChatColor.translateAlternateColorCodes('&', getConfig().getString("menus."+k+".titulo")));
        for (String iK : getConfig().getConfigurationSection("menus."+k+".itens").getKeys(false)) {
            String iP = "menus."+k+".itens."+iK;
            ItemStack i = new ItemStack(Material.valueOf(getConfig().getString(iP+".material")));
            ItemMeta m = i.getItemMeta();
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString(iP+".nome")));
            List<String> lore = new ArrayList<>();
            for (String l : getConfig().getStringList(iP + ".lore")) lore.add(ChatColor.translateAlternateColorCodes('&', l));
            m.setLore(lore);
            i.setItemMeta(m);
            inv.setItem(getConfig().getInt(iP+".slot"), i);
        }
        p.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (c.getName().equalsIgnoreCase("aeriamenus")) {
            if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
                if (s.hasPermission("aeriaplugins.admin")) {
                    reloadConfig();
                    s.sendMessage(ChatColor.GREEN + "[AeriaMenus] Configuração recarregada com sucesso!");
                } else {
                    s.sendMessage(ChatColor.RED + "Você não tem permissão para usar isso.");
                }
            } else if (a.length >= 4 && a[0].equalsIgnoreCase("region") && a[1].equalsIgnoreCase("create")) {
                if (!(s instanceof Player)) return true;
                Player p = (Player) s;
                if (!p.hasPermission("aeriaplugins.admin")) {
                    p.sendMessage(ChatColor.RED + "Sem permissão.");
                    return true;
                }
                
                if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
                    p.sendMessage(ChatColor.RED + "Erro: O plugin WorldEdit não está instalado no servidor!");
                    p.sendMessage(ChatColor.GRAY + "Instale o WorldEdit para poder criar regiões protegidas.");
                    return true;
                }
                
                criarRegiaoWorldEdit(p, a[2], a[3]);
                
            } else {
                s.sendMessage("§b§lAERIA MENUS §8- §7Central de Ajuda");
                s.sendMessage("§8--------------------------------------------------");
                s.sendMessage("§e/am reload §8- §7Recarrega o arquivo config.yml");
                s.sendMessage("§e/am region create <nome> <perm> §8- §7Protege área (Requer WorldEdit)");
                s.sendMessage("§e/setspawn §8- §7Define o local onde os jogadores nascem");
                s.sendMessage("§e/spawn §8- §7Teleporta você para o spawn do lobby");
                s.sendMessage("§8--------------------------------------------------");
            }
            return true;
        }

        if (!(s instanceof Player)) return true;
        Player p = (Player) s;

        if (c.getName().equalsIgnoreCase("setspawn") && p.hasPermission("aeriaplugins.admin")) {
            Location loc = p.getLocation();
            getConfig().set("spawn.local.mundo", loc.getWorld().getName());
            getConfig().set("spawn.local.x", loc.getX());
            getConfig().set("spawn.local.y", loc.getY());
            getConfig().set("spawn.local.z", loc.getZ());
            getConfig().set("spawn.local.yaw", loc.getYaw());
            getConfig().set("spawn.local.pitch", loc.getPitch());
            saveConfig();
            p.sendMessage(ChatColor.GREEN + "Ponto de spawn definido com sucesso no local onde você está!");
        } else if (c.getName().equalsIgnoreCase("spawn")) {
            teleportToSpawn(p);
            p.sendMessage(ChatColor.GREEN + "Teleportado para o spawn do lobby!");
        }
        return true;
    }

    private void criarRegiaoWorldEdit(Player p, String nome, String perm) {
        try {
            com.sk89q.worldedit.LocalSession session = com.sk89q.worldedit.WorldEdit.getInstance().getSessionManager().get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(p));
            com.sk89q.worldedit.regions.Region sel = session.getSelection(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(p.getWorld()));
            
            getConfig().set("regioes." + nome + ".mundo", p.getWorld().getName());
            getConfig().set("regioes." + nome + ".minX", sel.getMinimumPoint().getBlockX());
            getConfig().set("regioes." + nome + ".minY", sel.getMinimumPoint().getBlockY());
            getConfig().set("regioes." + nome + ".minZ", sel.getMinimumPoint().getBlockZ());
            getConfig().set("regioes." + nome + ".maxX", sel.getMaximumPoint().getBlockX());
            getConfig().set("regioes." + nome + ".maxY", sel.getMaximumPoint().getBlockY());
            getConfig().set("regioes." + nome + ".maxZ", sel.getMaximumPoint().getBlockZ());
            getConfig().set("regioes." + nome + ".permissao", perm);
            saveConfig();
            
            p.sendMessage(ChatColor.GREEN + "Região '" + nome + "' protegida com sucesso!");
            p.sendMessage(ChatColor.GRAY + "Apenas jogadores com a permissão: " + ChatColor.YELLOW + perm + ChatColor.GRAY + " podem construir aqui.");
        } catch (Exception ex) {
            p.sendMessage(ChatColor.RED + "Erro: Selecione uma área completa com o machado do WorldEdit primeiro!");
        }
    }

    private boolean checkRegionPermission(Player p, Location loc) {
        if (!getConfig().contains("regioes")) return true;
        
        for (String reg : getConfig().getConfigurationSection("regioes").getKeys(false)) {
            String path = "regioes." + reg + ".";
            if (loc.getWorld().getName().equals(getConfig().getString(path + "mundo"))) {
                int minX = getConfig().getInt(path + "minX");
                int minY = getConfig().getInt(path + "minY");
                int minZ = getConfig().getInt(path + "minZ");
                int maxX = getConfig().getInt(path + "maxX");
                int maxY = getConfig().getInt(path + "maxY");
                int maxZ = getConfig().getInt(path + "maxZ");
                
                if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                    loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                    loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                    
                    String permReq = getConfig().getString(path + "permissao");
                    if (!p.hasPermission(permReq) && !p.isOp()) {
                        p.sendMessage(ChatColor.RED + "Você não tem permissão (" + permReq + ") para alterar blocos nesta região.");
                        return false; 
                    }
                }
            }
        }
        return true; 
    }

    @EventHandler 
    public void onBreak(BlockBreakEvent e) { 
        if (!checkRegionPermission(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }
        if(getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); 
    }

    @EventHandler 
    public void onPlace(BlockPlaceEvent e) { 
        if (!checkRegionPermission(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }
        if(getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); 
    }

    @EventHandler public void onDrop(PlayerDropItemEvent e) { if(getConfig().getBoolean("modulos.ativar-protecoes") && !e.getPlayer().isOp()) e.setCancelled(true); }
    @EventHandler public void onFood(FoodLevelChangeEvent e) { if(getConfig().getBoolean("modulos.ativar-protecoes")) e.setCancelled(true); }
    @EventHandler public void onDamage(EntityDamageEvent e) { if(getConfig().getBoolean("modulos.ativar-protecoes") && e.getEntity() instanceof Player) e.setCancelled(true); }
}