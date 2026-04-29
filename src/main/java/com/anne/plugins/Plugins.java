package com.anne.plugins;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.bukkit.FluidCollisionMode;

import java.util.*;

public class Plugins extends JavaPlugin implements Listener, CommandExecutor {

    private final String PREFIX = "§8[§6§lQuarentine§8] §f";
    
    private final int C_FUNDACAO = 20;
    private final int C_PILAR = 5;
    private final int C_PAREDE = 10;
    private final int C_PORTAL = 15;
    private final int C_PORTA = 10;
    private final int C_TETO = 20;
    private final int CUSTO_UPGRADE = 20;
    
    // Grelha 6x6 matematicamente perfeita. Blocos 0 a 5. Parede preenche do 1 ao 4.
    private final int GRID_SIZE = 6; 
    private final int HEIGHT = 4;    
    
    private class PreviewData {
        List<BlockDisplay> displays = new ArrayList<>();
        Location anchor; 
        String tipo;     
        String posicao;     
        boolean valido;
        String msgErro;
    }
    
    private final Map<UUID, PreviewData> previews = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("kit").setExecutor(this);
        getCommand("quarentine").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        iniciarMotor();
        getLogger().info("Quarentine v7.0 - Grelha Perfeita, Sem Clipping e Portas 100% Funcionais!");
    }

    @Override
    public void onDisable() {
        previews.values().forEach(pd -> pd.displays.forEach(Entity::remove));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (label.equalsIgnoreCase("kit")) {
            String[] tipos = {"Fundação", "Pilar", "Parede", "Portal", "Porta", "Teto"};
            for(String t : tipos) p.getInventory().addItem(gerarMartelo(t));
            p.getInventory().addItem(gerarMarteloManutencao());
            p.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, 64));
            p.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
            p.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 64));
            p.getInventory().addItem(new ItemStack(Material.BRICK, 64));
            p.getInventory().addItem(new ItemStack(Material.GRAY_CONCRETE, 64));
            p.sendMessage(PREFIX + "§aKit Completo v7.0 entregue!");
            return true;
        }

        if (label.equalsIgnoreCase("quarentine") && args.length > 0 && args[0].equalsIgnoreCase("purgar")) {
            int bL = 0, eL = 0;
            Location loc = p.getLocation();
            for (int x = -30; x <= 30; x++) {
                for (int y = -15; y <= 30; y++) {
                    for (int z = -30; z <= 30; z++) {
                        Block b = loc.clone().add(x, y, z).getBlock();
                        if (b.getType() == Material.BARRIER) { b.setType(Material.AIR); bL++; }
                    }
                }
            }
            for (Entity en : p.getNearbyEntities(35, 35, 35)) {
                if (en instanceof BlockDisplay && en.getScoreboardTags().contains("q_built")) { en.remove(); eL++; }
            }
            p.sendMessage(PREFIX + "§6Limpeza Completa: " + bL + " barreiras e " + eL + " entidades.");
            return true;
        }
        return true;
    }

    private ItemStack gerarMartelo(String tipo) {
        ItemStack item = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Martelo Quarentine: " + tipo);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack gerarMarteloManutencao() {
        ItemStack item = new ItemStack(Material.IRON_HOE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bMartelo de Manutenção");
        meta.setLore(Arrays.asList("§7Botão Direito: Upar Estrutura", "§cBotão Esquerdo: Destruir Estrutura"));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.BARRIER) e.setCancelled(true);
    }

    private void iniciarMotor() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    ItemStack mao = p.getInventory().getItemInMainHand();
                    if (mao.getType() != Material.GOLDEN_HOE || !mao.hasItemMeta() || !mao.getItemMeta().getDisplayName().contains("Martelo Quarentine:")) {
                        removerPreview(p); continue;
                    }
                    atualizarPreview(p, mao.getItemMeta().getDisplayName());
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void removerPreview(Player p) {
        PreviewData pd = previews.remove(p.getUniqueId());
        if (pd != null) pd.displays.forEach(Entity::remove);
    }

    private void atualizarPreview(Player p, String nome) {
        RayTraceResult rB = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 12, FluidCollisionMode.NEVER);
        RayTraceResult rE = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), 12, 0.5, e -> e.getScoreboardTags().contains("q_built"));

        String tipo = nome.contains("Fundação") ? "FUNDACAO" : (nome.contains("Pilar") ? "PILAR" : (nome.contains("Parede") ? "PAREDE" : (nome.contains("Portal") ? "PORTAL" : (nome.contains("Porta") ? "PORTA" : "TETO"))));

        Vector hitPos = (rE != null && rE.getHitPosition() != null) ? rE.getHitPosition() : (rB != null ? rB.getHitPosition() : null);
        if (hitPos == null) { removerPreview(p); return; }

        Entity baseRef = null;
        for (Entity e : p.getWorld().getNearbyEntities(hitPos.toLocation(p.getWorld()), 15, 15, 15)) {
            if (e.getScoreboardTags().contains("tag_fund") || e.getScoreboardTags().contains("tag_teto")) {
                if (baseRef == null || e.getLocation().distanceSquared(hitPos.toLocation(p.getWorld())) < baseRef.getLocation().distanceSquared(hitPos.toLocation(p.getWorld()))) {
                    baseRef = e;
                }
            }
        }

        double alignY = Math.round(hitPos.getY());
        if (baseRef != null) {
            if (tipo.equals("TETO")) alignY = baseRef.getLocation().getY() + HEIGHT; 
            else alignY = baseRef.getLocation().getY();
        } else if (rB != null && rB.getHitBlockFace() == BlockFace.UP) {
            alignY = Math.round(rB.getHitBlock().getY() + 1);
        }

        double gridX = Math.round(hitPos.getX() / (double)GRID_SIZE) * GRID_SIZE;
        double gridZ = Math.round(hitPos.getZ() / (double)GRID_SIZE) * GRID_SIZE;
        double cellX = Math.floor(hitPos.getX() / (double)GRID_SIZE) * GRID_SIZE;
        double cellZ = Math.floor(hitPos.getZ() / (double)GRID_SIZE) * GRID_SIZE;

        Location exactAnchor = null;
        String dir = "NENHUM";

        if (tipo.equals("FUNDACAO") || tipo.equals("TETO")) {
            exactAnchor = new Location(p.getWorld(), cellX, alignY, cellZ);
        } else if (tipo.equals("PILAR")) {
            exactAnchor = new Location(p.getWorld(), gridX, alignY, gridZ);
        } else if (tipo.equals("PAREDE") || tipo.equals("PORTAL") || tipo.equals("PORTA")) {
            double distToZLine = Math.abs(hitPos.getZ() - gridZ);
            double distToXLine = Math.abs(hitPos.getX() - gridX);
            if (distToZLine < distToXLine) {
                exactAnchor = new Location(p.getWorld(), cellX, alignY, gridZ);
                dir = "X";
            } else {
                exactAnchor = new Location(p.getWorld(), gridX, alignY, cellZ);
                dir = "Z";
            }
        }

        if (exactAnchor == null) { removerPreview(p); return; }

        String[] val = validar(exactAnchor, tipo, dir);
        PreviewData pd = previews.computeIfAbsent(p.getUniqueId(), k -> new PreviewData());
        pd.anchor = exactAnchor; pd.tipo = tipo; pd.posicao = dir; pd.valido = val[0].equals("OK"); pd.msgErro = val[1];

        int numPartes = tipo.equals("PORTAL") ? 3 : 1;
        while(pd.displays.size() < numPartes) pd.displays.add((BlockDisplay) p.getWorld().spawnEntity(exactAnchor, EntityType.BLOCK_DISPLAY));
        while(pd.displays.size() > numPartes) pd.displays.remove(0).remove();

        for(BlockDisplay bd : pd.displays) {
            bd.setBlock(Bukkit.createBlockData(pd.valido ? Material.CYAN_STAINED_GLASS : Material.RED_STAINED_GLASS));
            bd.teleport(exactAnchor);
        }
        setVisual(pd.displays, tipo, dir, false);
    }

    private String[] validar(Location loc, String tipo, String dir) {
        boolean p1 = false, p2 = false;
        boolean portalLocal = false, hasFoundation = false;

        for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(GRID_SIZE/2.0, HEIGHT/2.0, GRID_SIZE/2.0), 12, 12, 12)) {
            if (!e.getScoreboardTags().contains("q_built")) continue;
            Location eL = e.getLocation();

            if (tipo.equals("FUNDACAO") && e.getScoreboardTags().contains("tag_fund") && eL.equals(loc)) return new String[]{"ERR", "Fundação ocupada!"};
            if (tipo.equals("TETO") && e.getScoreboardTags().contains("tag_teto") && eL.equals(loc)) return new String[]{"ERR", "Teto ocupado!"};
            if (tipo.equals("PILAR") && e.getScoreboardTags().contains("tag_pilar") && eL.equals(loc)) return new String[]{"ERR", "Pilar já existe!"};
            if ((tipo.equals("PAREDE") || tipo.equals("PORTAL")) && (e.getScoreboardTags().contains("tag_parede_" + dir) || e.getScoreboardTags().contains("tag_portal_" + dir)) && eL.equals(loc)) return new String[]{"ERR", "Lado ocupado!"};
            if (tipo.equals("PORTA") && e.getScoreboardTags().contains("tag_porta_" + dir) && eL.equals(loc)) return new String[]{"ERR", "Porta já existe!"};

            if ((tipo.equals("PAREDE") || tipo.equals("PORTAL")) && e.getScoreboardTags().contains("tag_pilar")) {
                if (eL.equals(loc)) p1 = true;
                if (dir.equals("X") && eL.getBlockX() == loc.getBlockX() + GRID_SIZE && eL.getBlockY() == loc.getBlockY() && eL.getBlockZ() == loc.getBlockZ()) p2 = true;
                if (dir.equals("Z") && eL.getBlockX() == loc.getBlockX() && eL.getBlockY() == loc.getBlockY() && eL.getBlockZ() == loc.getBlockZ() + GRID_SIZE) p2 = true;
            }
            if (tipo.equals("PORTA") && e.getScoreboardTags().contains("tag_portal_" + dir) && eL.equals(loc)) portalLocal = true;
            
            if (tipo.equals("PILAR") && (e.getScoreboardTags().contains("tag_fund") || e.getScoreboardTags().contains("tag_teto"))) {
                if (Math.abs(eL.getBlockY() - loc.getBlockY()) <= HEIGHT + 1) hasFoundation = true;
            }
        }

        if (tipo.equals("PAREDE") || tipo.equals("PORTAL")) {
            if (!p1 && !p2) return new String[]{"ERR", "Requer pelo menos 1 pilar base!"};
        }
        if (tipo.equals("PORTA") && !portalLocal) return new String[]{"ERR", "Exige Portal!"};
        if (tipo.equals("PILAR") && !hasFoundation) return new String[]{"ERR", "Exige aproximação a uma Base!"};

        return new String[]{"OK", ""};
    }

    private void setVisual(List<BlockDisplay> displays, String tipo, String dir, boolean portaAberta) {
        if (tipo.equals("PORTAL")) {
            if (dir.equals("X")) {
                displays.get(0).setTransformation(new Transformation(new Vector3f(1, 1, 0.25f), new org.joml.Quaternionf(), new Vector3f(2, 3, 0.5f), new org.joml.Quaternionf())); // Lado Esquerdo
                displays.get(1).setTransformation(new Transformation(new Vector3f(4, 1, 0.25f), new org.joml.Quaternionf(), new Vector3f(2, 3, 0.5f), new org.joml.Quaternionf())); // Lado Direito
                displays.get(2).setTransformation(new Transformation(new Vector3f(3, 3, 0.25f), new org.joml.Quaternionf(), new Vector3f(1, 1, 0.5f), new org.joml.Quaternionf())); // Padieira no meio
            } else {
                displays.get(0).setTransformation(new Transformation(new Vector3f(0.25f, 1, 1), new org.joml.Quaternionf(), new Vector3f(0.5f, 3, 2), new org.joml.Quaternionf()));
                displays.get(1).setTransformation(new Transformation(new Vector3f(0.25f, 1, 4), new org.joml.Quaternionf(), new Vector3f(0.5f, 3, 2), new org.joml.Quaternionf()));
                displays.get(2).setTransformation(new Transformation(new Vector3f(0.25f, 3, 3), new org.joml.Quaternionf(), new Vector3f(0.5f, 1, 1), new org.joml.Quaternionf()));
            }
        } else {
            Vector3f tr = new Vector3f(0, 0, 0), sc = new Vector3f(1, 1, 1);
            
            // GRID_SIZE exato elimina o Z-Fighting e o Clipping entre blocos adjacentes.
            if (tipo.equals("FUNDACAO") || tipo.equals("TETO")) { tr.set(0, 0, 0); sc.set((float)GRID_SIZE, 1f, (float)GRID_SIZE); }
            else if (tipo.equals("PILAR")) { tr.set(0, 1, 0); sc.set(1, HEIGHT - 1, 1); }
            else if (tipo.equals("PAREDE")) {
                if (dir.equals("X")) { tr.set(1, 1, 0.25f); sc.set(GRID_SIZE - 1, HEIGHT - 1, 0.5f); } // Encaixe sem buracos
                else { tr.set(0.25f, 1, 1); sc.set(0.5f, HEIGHT - 1, GRID_SIZE - 1); }
            } else if (tipo.equals("PORTA")) {
                if (!portaAberta) {
                    if (dir.equals("X")) { tr.set(3f, 1, 0.4f); sc.set(1f, 2f, 0.2f); }
                    else { tr.set(0.4f, 1, 3f); sc.set(0.2f, 2f, 1f); }
                } else {
                    if (dir.equals("X")) { tr.set(3f, 1, 1f); sc.set(0.2f, 2f, 1f); } 
                    else { tr.set(1f, 1, 3f); sc.set(1f, 2f, 0.2f); }
                }
            }
            displays.get(0).setTransformation(new Transformation(tr, new org.joml.Quaternionf(), sc, new org.joml.Quaternionf()));
        }
    }

    private boolean pertenceAFisica(Block b, Entity e) {
        Location a = e.getLocation(); String t = extrairTipo(e); String dir = extrairPosicao(e);
        int bx = b.getX() - a.getBlockX(); int by = b.getY() - a.getBlockY(); int bz = b.getZ() - a.getBlockZ();

        if (t.equals("FUNDACAO") || t.equals("TETO")) return bx >= 0 && bx < GRID_SIZE && bz >= 0 && bz < GRID_SIZE && by == 0;
        if (t.equals("PILAR")) return bx == 0 && bz == 0 && by >= 1 && by < HEIGHT;
        if (t.equals("PORTA")) {
            if (e.getScoreboardTags().contains("porta_aberta")) {
                if (dir.equals("X")) return bx == 3 && bz == 1 && by >= 1 && by <= 2;
                if (dir.equals("Z")) return bz == 3 && bx == 1 && by >= 1 && by <= 2;
            } else {
                if (dir.equals("X")) return bx == 3 && bz == 0 && by >= 1 && by <= 2;
                if (dir.equals("Z")) return bz == 3 && bx == 0 && by >= 1 && by <= 2;
            }
        }
        if (t.equals("PAREDE")) {
            if (dir.equals("X")) return bx >= 1 && bx < GRID_SIZE && bz == 0 && by >= 1 && by < HEIGHT;
            if (dir.equals("Z")) return bz >= 1 && bz < GRID_SIZE && bx == 0 && by >= 1 && by < HEIGHT;
        }
        if (t.equals("PORTAL")) {
            if (dir.equals("X")) return bx >= 1 && bx < GRID_SIZE && bz == 0 && by >= 1 && by < HEIGHT && !(bx == 3 && by <= 2);
            if (dir.equals("Z")) return bz >= 1 && bz < GRID_SIZE && bx == 0 && by >= 1 && by < HEIGHT && !(bz == 3 && by <= 2);
        }
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer(); ItemStack m = e.getItem();
        boolean isHammer = m != null && m.hasItemMeta() && m.getItemMeta().getDisplayName().contains("Manutenção");
        
        // 1. SISTEMA CLICK & GO (PORTAS - Baseado em Bloco para nunca falhar)
        if (!isHammer && e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.BARRIER) {
            Block b = e.getClickedBlock();
            Entity portaEncontrada = null;
            
            for (Entity en : b.getWorld().getNearbyEntities(b.getLocation().add(0.5, 0.5, 0.5), 3, 3, 3)) {
                if (en instanceof BlockDisplay && en.getScoreboardTags().contains("tag_porta")) {
                    if (pertenceAFisica(b, en)) { portaEncontrada = en; break; }
                }
            }
            
            if (portaEncontrada != null) {
                e.setCancelled(true); // Cancela para não colocar blocos nem executar outras ações
                String dir = extrairPosicao(portaEncontrada); 
                boolean aberta = portaEncontrada.getScoreboardTags().contains("porta_aberta");
                
                // Limpa a física anterior
                operarFisica(portaEncontrada.getLocation(), "PORTA", dir, aberta, Material.AIR); 
                
                if (aberta) { 
                    portaEncontrada.removeScoreboardTag("porta_aberta"); 
                    setVisual(Collections.singletonList((BlockDisplay)portaEncontrada), "PORTA", dir, false); 
                    operarFisica(portaEncontrada.getLocation(), "PORTA", dir, false, Material.BARRIER); 
                    p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f); 
                } else { 
                    portaEncontrada.addScoreboardTag("porta_aberta"); 
                    setVisual(Collections.singletonList((BlockDisplay)portaEncontrada), "PORTA", dir, true); 
                    operarFisica(portaEncontrada.getLocation(), "PORTA", dir, true, Material.BARRIER); 
                    p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1f, 1f); 
                }
                return;
            }
        }

        if (m == null || !m.hasItemMeta()) return;
        String nItem = m.getItemMeta().getDisplayName();

        // 2. MARTELO DE MANUTENÇÃO (UPGRADE/DESTRUIR)
        if (isHammer && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.BARRIER) {
            e.setCancelled(true);
            Entity alvo = null;
            
            for (Entity en : e.getClickedBlock().getWorld().getNearbyEntities(e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5), 8, 8, 8)) {
                if (en instanceof BlockDisplay && en.getScoreboardTags().contains("q_built") && pertenceAFisica(e.getClickedBlock(), en)) {
                    alvo = en; break;
                }
            }

            if (alvo != null) {
                Location a = alvo.getLocation(); String t = extrairTipo(alvo);
                
                if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                    destruirEntidade((BlockDisplay)alvo);
                    if (t.equals("FUNDACAO")) p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1f);
                    else p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1f);
                    aplicarGravidade(a); 
                } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Material mat = ((BlockDisplay)alvo).getBlock().getMaterial();
                    Material pM = Material.STONE; Material r1 = Material.COBBLESTONE;
                    if (mat == Material.STONE) { pM = Material.IRON_BLOCK; r1 = Material.IRON_INGOT; }
                    else if (mat == Material.IRON_BLOCK) { pM = Material.BRICKS; r1 = Material.BRICK; }
                    else if (mat == Material.BRICKS) { pM = Material.GRAY_CONCRETE; r1 = Material.GRAY_CONCRETE; }
                    if (mat == Material.GRAY_CONCRETE) return;
                    
                    if (consumirInteligente(p, r1, null, CUSTO_UPGRADE)) { 
                        String bId = null; for(String tg : alvo.getScoreboardTags()) if(tg.startsWith("build_")) { bId = tg; break; }
                        if (bId != null) {
                            for(Entity pA : a.getWorld().getNearbyEntities(a, 10, 10, 10)) {
                                if (pA instanceof BlockDisplay && pA.getScoreboardTags().contains(bId)) {
                                    ((BlockDisplay)pA).setBlock(Bukkit.createBlockData(pM));
                                }
                            }
                        }
                        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f); 
                    } else { p.sendMessage(PREFIX + "§cFaltam recursos! Requer 20x " + r1.name()); }
                }
            }
            return;
        }

        // 3. MARTELOS DE CONSTRUÇÃO
        if (!nItem.contains("Martelo Quarentine:") || e.getAction() == Action.LEFT_CLICK_AIR) return;
        PreviewData pd = previews.get(p.getUniqueId()); if (pd == null) return;
        e.setCancelled(true);
        if (!pd.valido) { p.sendMessage(PREFIX + "§c" + pd.msgErro); return; }
        
        int c = pd.tipo.equals("PORTA") ? C_PORTA : (pd.tipo.equals("FUNDACAO") ? C_FUNDACAO : (pd.tipo.equals("PILAR") ? C_PILAR : (pd.tipo.equals("PORTAL") ? C_PORTAL : C_TETO)));
        if (consumirInteligente(p, Material.OAK_PLANKS, Material.SPRUCE_PLANKS, c)) { 
            String buildId = "build_" + UUID.randomUUID().toString();
            for(BlockDisplay prev : pd.displays) {
                BlockDisplay b = (BlockDisplay) p.getWorld().spawnEntity(pd.anchor, EntityType.BLOCK_DISPLAY);
                b.setBlock(Bukkit.createBlockData(pd.tipo.equals("PILAR") ? Material.STRIPPED_OAK_LOG : (pd.tipo.equals("PORTA") ? Material.SPRUCE_PLANKS : Material.OAK_PLANKS)));
                b.setTransformation(prev.getTransformation()); b.addScoreboardTag("q_built");
                b.addScoreboardTag(buildId); // Tag de Agrupamento
                if (pd.tipo.equals("FUNDACAO")) b.addScoreboardTag("tag_fund");
                else if (pd.tipo.equals("TETO")) b.addScoreboardTag("tag_teto");
                else if (pd.tipo.equals("PILAR")) b.addScoreboardTag("tag_pilar");
                else { 
                    b.addScoreboardTag("tag_parede_" + pd.posicao); 
                    if (pd.tipo.equals("PORTAL")) b.addScoreboardTag("tag_portal_" + pd.posicao); 
                    if (pd.tipo.equals("PORTA")) { b.addScoreboardTag("tag_porta_" + pd.posicao); b.addScoreboardTag("tag_porta"); }
                }
            }
            operarFisica(pd.anchor, pd.tipo, pd.posicao, false, Material.BARRIER);
            p.playSound(p.getLocation(), Sound.BLOCK_WOOD_PLACE, 1f, 1f);
        } else { p.sendMessage(PREFIX + "§cMadeiras insuficientes (" + c + ")."); }
    }

    private void aplicarGravidade(Location epi) {
        boolean caiu = true;
        while (caiu) {
            caiu = false;
            for (Entity e : epi.getWorld().getNearbyEntities(epi, 35, 35, 35)) {
                if (!(e instanceof BlockDisplay) || !e.getScoreboardTags().contains("q_built")) continue;
                if (!isEstavel(e)) {
                    destruirEntidade((BlockDisplay) e);
                    caiu = true;
                    epi.getWorld().playSound(e.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1f);
                }
            }
        }
    }

    private boolean isEstavel(Entity e) {
        String tipo = extrairTipo(e);
        Location l = e.getLocation();
        
        if (tipo.equals("FUNDACAO")) return true;
        
        if (tipo.equals("PILAR")) {
            for (Entity f : e.getNearbyEntities(1, HEIGHT + 1, 1)) {
                if (f.getScoreboardTags().contains("tag_fund") || f.getScoreboardTags().contains("tag_teto")) {
                    if (l.getBlockY() >= f.getLocation().getBlockY()) return true;
                }
            }
            return false;
        }
        if (tipo.equals("PAREDE") || tipo.equals("PORTAL")) {
            for (Entity p : e.getNearbyEntities(GRID_SIZE, 2, GRID_SIZE)) {
                if (p.getScoreboardTags().contains("tag_pilar")) return true;
            }
            return false;
        }
        if (tipo.equals("PORTA")) return true;
        if (tipo.equals("TETO")) {
            for (Entity p : e.getNearbyEntities(GRID_SIZE, HEIGHT + 1, GRID_SIZE)) {
                if (p.getScoreboardTags().contains("tag_pilar") || p.getScoreboardTags().contains("tag_parede_")) return true;
            }
            return false;
        }
        return true;
    }

    private void destruirEntidade(BlockDisplay alvo) {
        String bId = null;
        for(String t : alvo.getScoreboardTags()) if(t.startsWith("build_")) { bId = t; break; }
        
        if (bId != null) {
            for(Entity e : alvo.getWorld().getNearbyEntities(alvo.getLocation(), 10, 10, 10)) {
                if (e.getScoreboardTags().contains(bId)) {
                    operarFisica(e.getLocation(), extrairTipo(e), extrairPosicao(e), e.getScoreboardTags().contains("porta_aberta"), Material.AIR);
                    e.remove();
                }
            }
            restaurarFisicaVizinhos(alvo.getLocation());
        }
    }

    // Impede a criação de "buracos invisiveis" em bases adjacentes caso uma parede/fundação ligada seja destruida
    private void restaurarFisicaVizinhos(Location loc) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 10, 10, 10)) {
            if (e instanceof BlockDisplay && e.getScoreboardTags().contains("q_built")) {
                operarFisica(e.getLocation(), extrairTipo(e), extrairPosicao(e), e.getScoreboardTags().contains("porta_aberta"), Material.BARRIER);
            }
        }
    }

    private void operarFisica(Location a, String t, String dir, boolean aberta, Material mat) {
        if (t.equals("FUNDACAO") || t.equals("TETO")) { 
            for(int x=0;x<GRID_SIZE;x++) for(int z=0;z<GRID_SIZE;z++) a.clone().add(x,0,z).getBlock().setType(mat); 
        }
        else if (t.equals("PILAR")) { 
            for(int y=1;y<HEIGHT;y++) a.clone().add(0,y,0).getBlock().setType(mat); 
        }
        else if (t.equals("PORTA")) {
            for (int y=1; y<HEIGHT-1; y++) {
                Location bl = a.clone();
                if (!aberta) {
                    if (dir.equals("X")) bl.add(3, y, 0); else bl.add(0, y, 3);
                } else {
                    if (dir.equals("X")) bl.add(3, y, 1); else bl.add(1, y, 3);
                }
                bl.getBlock().setType(mat);
            }
        }
        else {
            for (int i=1; i<GRID_SIZE; i++) {
                for (int y=1; y<HEIGHT; y++) {
                    if (t.equals("PORTAL") && i == 3 && y <= 2) continue; // Abre o buraco da porta
                    Location bl = a.clone(); 
                    if(dir.equals("X")) bl.add(i, y, 0); else bl.add(0, y, i);
                    bl.getBlock().setType(mat);
                }
            }
        }
    }

    private String extrairTipo(Entity e) {
        if (e.getScoreboardTags().contains("tag_fund")) return "FUNDACAO";
        if (e.getScoreboardTags().contains("tag_teto")) return "TETO";
        if (e.getScoreboardTags().contains("tag_porta")) return "PORTA";
        if (e.getScoreboardTags().contains("tag_pilar")) return "PILAR";
        return "PAREDE";
    }

    private String extrairPosicao(Entity e) {
        for (String tag : e.getScoreboardTags()) {
            if (tag.startsWith("tag_parede_")) return tag.replace("tag_parede_", "");
            if (tag.startsWith("tag_portal_")) return tag.replace("tag_portal_", "");
            if (tag.startsWith("tag_porta_")) return tag.replace("tag_porta_", "");
        }
        return "NENHUM";
    }

    private boolean consumirInteligente(Player p, Material m1, Material m2, int custo) {
        int total = 0;
        for (ItemStack is : p.getInventory().getContents()) {
            if (is != null && (is.getType() == m1 || (m2 != null && is.getType() == m2))) total += is.getAmount();
        }
        if (total >= custo) {
            int restante = custo;
            for (ItemStack is : p.getInventory().getContents()) {
                if (is != null && (is.getType() == m1 || (m2 != null && is.getType() == m2))) {
                    if (is.getAmount() <= restante) {
                        restante -= is.getAmount(); is.setAmount(0);
                    } else {
                        is.setAmount(is.getAmount() - restante); restante = 0;
                    }
                    if (restante == 0) break;
                }
            }
            return true;
        }
        return false;
    }
}