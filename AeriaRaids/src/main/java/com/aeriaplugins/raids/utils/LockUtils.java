package com.aeriaplugins.raids.utils;

import com.aeriaplugins.raids.AeriaRaids;
import com.aeriaplugins.raids.commands.RaidCommand;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.data.type.Door;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LockUtils {

    public static boolean isCadeado(ItemStack item) {
        return item != null && item.getType() == Material.ITEM_FRAME && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Cadeado de Segredo");
    }

    public static boolean isBlocoTrancavel(Block bloco) {
        if (bloco == null) return false;
        Material type = bloco.getType();
        return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL || bloco.getBlockData() instanceof Door;
    }

    public static void sincronizarBaulDuplo(Location loc, String acao, String senha, UUID dono, String rank) {
        Block bloco = loc.getBlock();
        List<Block> adjs = new ArrayList<>();
        adjs.add(bloco);

        if (bloco.getType() == Material.CHEST || bloco.getType() == Material.TRAPPED_CHEST) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block adj = bloco.getRelative(face);
                if (adj.getType() == bloco.getType()) adjs.add(adj);
            }
        } else if (bloco.getBlockData() instanceof Door door) {
            adjs.add(door.getHalf() == Door.Half.TOP ? bloco.getRelative(BlockFace.DOWN) : bloco.getRelative(BlockFace.UP));
        }

        for (Block adj : adjs) {
            switch (acao) {
                case "SETAR" -> AeriaRaids.getInstance().getRaidStorage().setarSenha(adj.getLocation(), senha, dono);
                case "QUEBRAR" -> AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(adj.getLocation());
                case "ATUALIZAR_SENHA" -> AeriaRaids.getInstance().getRaidStorage().atualizarSenha(adj.getLocation(), senha);
                case "CLAN" -> AeriaRaids.getInstance().getRaidStorage().setPermissaoClan(adj.getLocation(), rank);
            }
        }
    }

    public static void gerarItemFrameVisual(Location loc, BlockFace face) {
        ItemFrame frame = loc.getWorld().spawn(loc.getBlock().getRelative(face).getLocation(), ItemFrame.class);
        frame.setFacingDirection(face, true);
        frame.setItem(RaidCommand.getCadeadoItem());
        frame.setFixed(true);
        frame.setVisible(false);
    }

    public static void removerItemFrameVisual(Location loc) {
        loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5).forEach(e -> {
            if (e instanceof ItemFrame frame && isCadeado(frame.getItem())) frame.remove();
        });
    }

    public static ItemStack criarItem(Material mat, String nome, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nome);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean removerCadeadoDoInventario(Player jogador) {
        for (ItemStack item : jogador.getInventory().getContents()) {
            if (isCadeado(item)) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }
}