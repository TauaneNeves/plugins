package com.aeriaplugins.raids.listeners;

import com.aeriaplugins.raids.AeriaRaids;
import com.aeriaplugins.raids.commands.RaidCommand;
import com.aeriaplugins.raids.utils.LockUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

import java.util.List;

public class LockListener implements Listener {

    @EventHandler
    public void aoColocarBloco(BlockPlaceEvent event) {
        if (AeriaRaids.getInstance().getRaidManager().isEmRaid()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "❌ Construção bloqueada durante o Raid!");
            return;
        }
        
        Block bloco = event.getBlockPlaced();
        Location loc = bloco.getLocation();

        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Block alvo = bloco.getRelative(x, y, z);
                    if (LockUtils.isBlocoTrancavel(alvo) && AeriaRaids.getInstance().getRaidStorage().isTrancado(alvo.getLocation())) {
                        if (!(Math.abs(x) + Math.abs(y) + Math.abs(z) == 1 && bloco.getType() == alvo.getType())) {
                            event.setCancelled(true);
                            event.getPlayer().sendMessage(ChatColor.RED + "❌ Mantenha 4 blocos de espaço.");
                            return;
                        }
                    }
                }
            }
        }

        if (AeriaRaids.getInstance().getRaidStorage().isTrancado(loc)) {
            AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(loc);
            LockUtils.removerItemFrameVisual(loc);
        }

        if ((bloco.getType() == Material.CHEST || bloco.getType() == Material.TRAPPED_CHEST) && bloco.getBlockData() instanceof org.bukkit.block.data.type.Chest chestData && chestData.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block adj = bloco.getRelative(face);
                if (adj.getType() == bloco.getType() && AeriaRaids.getInstance().getRaidStorage().isTrancado(adj.getLocation())) {
                    String senha = AeriaRaids.getInstance().getRaidStorage().getSenha(adj.getLocation());
                    AeriaRaids.getInstance().getRaidStorage().setarSenha(loc, senha, AeriaRaids.getInstance().getRaidStorage().getDono(adj.getLocation()));
                    AeriaRaids.getInstance().getRaidStorage().setPermissaoClan(loc, AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(adj.getLocation()));
                    event.getPlayer().sendMessage(ChatColor.GREEN + "✔️ Extensão trancada automaticamente.");
                    break;
                }
            }
        }
    }

    @EventHandler
    public void aoExplodirEntidade(EntityExplodeEvent event) {
        event.blockList().clear(); // Cancela a quebra nativa do Minecraft
        processarExplosaoForcada(event.getLocation());
    }

    @EventHandler
    public void aoExplodirBloco(BlockExplodeEvent event) {
        event.blockList().clear(); // Cancela a quebra nativa do Minecraft
        processarExplosaoForcada(event.getBlock().getLocation());
    }

    private void processarExplosaoForcada(Location centro) {
        Block alvo = null;
        double minDist = Double.MAX_VALUE;

        // Escaneia um raio de 2 blocos para encontrar o bloco sólido mais próximo do impacto real
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Block b = centro.clone().add(x, y, z).getBlock();
                    if (b.getType().isSolid() && b.getType() != Material.BEDROCK && b.getType() != Material.BARRIER) {
                        double dist = b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(centro);
                        if (dist < minDist) {
                            minDist = dist;
                            alvo = b;
                        }
                    }
                }
            }
        }

        if (alvo == null) return;

        if (!AeriaRaids.getInstance().getRaidManager().isEmRaid()) {
            AeriaRaids.getInstance().getRaidManager().iniciarRaid();
        }

        // Calcula a orientação da parede para não quebrar no vazio
        BlockFace lateral = BlockFace.EAST;
        if (alvo.getRelative(BlockFace.NORTH).getType().isSolid() || alvo.getRelative(BlockFace.SOUTH).getType().isSolid()) {
            lateral = BlockFace.NORTH;
        }

        BlockFace vertical = BlockFace.UP;
        if (!alvo.getRelative(BlockFace.UP).getType().isSolid() && alvo.getRelative(BlockFace.DOWN).getType().isSolid()) {
            vertical = BlockFace.DOWN;
        }

        // Define o molde rígido 2x2 com base no bloco atingido
        List<Block> buraco2x2 = List.of(
            alvo,
            alvo.getRelative(lateral),
            alvo.getRelative(vertical),
            alvo.getRelative(lateral).getRelative(vertical)
        );

        // Destrói os 4 blocos instantaneamente
        for (Block b : buraco2x2) {
            if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK && b.getType() != Material.BARRIER) {
                if (!AeriaRaids.getInstance().getRaidStorage().isTrancado(b.getLocation())) {
                    b.setType(Material.AIR, false);
                }
            }
        }
    }

    @EventHandler
    public void aoQuebrarBloco(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (AeriaRaids.getInstance().getRaidStorage().isTrancado(loc)) {
            if (!LockUtils.isBlocoTrancavel(event.getBlock())) {
                AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(loc); LockUtils.removerItemFrameVisual(loc); return;
            }
            boolean temCadeado = loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5).stream()
                .anyMatch(e -> e instanceof ItemFrame && LockUtils.isCadeado(((ItemFrame) e).getItem()));

            if (!temCadeado || (event.getPlayer().hasPermission("aeriaraids.admin") && AeriaRaids.getInstance().getConfig().getBoolean("admin-bypass", false))) {
                LockUtils.sincronizarBaulDuplo(loc, "QUEBRAR", null, null, null); LockUtils.removerItemFrameVisual(loc);
                if (temCadeado) event.getPlayer().sendMessage(ChatColor.YELLOW + "⚠️ Destruído via Admin Bypass.");
                return;
            }
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "❌ Remova o cadeado antes de quebrar.");
        }
    }

    @EventHandler
    public void aoColocarMoldura(HangingPlaceEvent event) {
        if (LockUtils.isCadeado(event.getItemStack() != null ? event.getItemStack() : event.getPlayer().getInventory().getItemInMainHand())) event.setCancelled(true);
    }

    @EventHandler
    public void aoQuebrarMoldura(HangingBreakEvent event) {
        if (event.getEntity() instanceof ItemFrame frame && LockUtils.isCadeado(frame.getItem())) {
            Location locAtras = frame.getLocation().getBlock().getRelative(frame.getFacing().getOppositeFace()).getLocation();
            if (!LockUtils.isBlocoTrancavel(locAtras.getBlock()) || (event instanceof org.bukkit.event.hanging.HangingBreakByEntityEvent e && e.getRemover() instanceof Player p && p.hasPermission("aeriaraids.admin") && AeriaRaids.getInstance().getConfig().getBoolean("admin-bypass", false))) {
                frame.remove();
                LockUtils.sincronizarBaulDuplo(locAtras, "QUEBRAR", null, null, null);
                if (!LockUtils.isBlocoTrancavel(locAtras.getBlock())) frame.getWorld().dropItemNaturally(frame.getLocation(), RaidCommand.getCadeadoItem());
            } else { event.setCancelled(true); }
        }
    }

    @EventHandler
    public void aoQueimarBloco(BlockBurnEvent event) { if (AeriaRaids.getInstance().getRaidStorage().isTrancado(event.getBlock().getLocation())) event.setCancelled(true); }

    @EventHandler
    public void aoPistaoEstender(BlockPistonExtendEvent event) { event.getBlocks().forEach(b -> { if (AeriaRaids.getInstance().getRaidStorage().isTrancado(b.getLocation())) event.setCancelled(true); }); }

    @EventHandler
    public void aoPistaoRetrair(BlockPistonRetractEvent event) { event.getBlocks().forEach(b -> { if (AeriaRaids.getInstance().getRaidStorage().isTrancado(b.getLocation())) event.setCancelled(true); }); }

    @EventHandler
    public void aoMoverItemFunil(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof DoubleChest dc && (AeriaRaids.getInstance().getRaidStorage().isTrancado(((BlockState)dc.getLeftSide()).getLocation()) || AeriaRaids.getInstance().getRaidStorage().isTrancado(((BlockState)dc.getRightSide()).getLocation()))) event.setCancelled(true);
        else if (event.getSource().getHolder() instanceof BlockState bs && AeriaRaids.getInstance().getRaidStorage().isTrancado(bs.getLocation())) event.setCancelled(true);
        else if (event.getSource().getLocation() != null && AeriaRaids.getInstance().getRaidStorage().isTrancado(event.getSource().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void aoEndermanPegarBloco(EntityChangeBlockEvent event) { if (event.getEntity() instanceof Enderman && AeriaRaids.getInstance().getRaidStorage().isTrancado(event.getBlock().getLocation())) event.setCancelled(true); }
}