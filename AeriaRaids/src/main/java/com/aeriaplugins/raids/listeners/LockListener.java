package com.aeriaplugins.raids.listeners;

import com.aeriaplugins.raids.AeriaRaids;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LockListener implements Listener {

    private static class RaidSession {
        Location loc;
        int[] digitos = new int[]{0, 0, 0};
        boolean isSetting;
        
        RaidSession(Location loc, boolean isSetting) {
            this.loc = loc;
            this.isSetting = isSetting;
        }
    }

    private final Map<UUID, RaidSession> sessoesAtivas = new HashMap<>();

    @EventHandler
    public void aoInteragirComBau(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block bloco = event.getClickedBlock();
        if (bloco == null || (bloco.getType() != Material.CHEST && bloco.getType() != Material.TRAPPED_CHEST && bloco.getType() != Material.BARREL)) return;

        Player jogador = event.getPlayer();
        Location loc = bloco.getLocation();
        boolean trancado = AeriaRaids.getInstance().getRaidStorage().isTrancado(loc);

        if (jogador.isSneaking()) {
            event.setCancelled(true);
            sessoesAtivas.put(jogador.getUniqueId(), new RaidSession(loc, !trancado));
            abrirMenuCadeado(jogador, !trancado);
        } else {
            if (trancado) {
                event.setCancelled(true);
                jogador.sendMessage(ChatColor.RED + "🔒 Este compartimento possui uma trava mecânica.");
                jogador.sendMessage(ChatColor.GRAY + "Agache e clique com o botão direito para tentar hackear o código.");
            }
        }
    }

    private void abrirMenuCadeado(Player jogador, boolean isSetting) {
        String titulo = isSetting ? ChatColor.DARK_GREEN + "Definir Senha do Cadeado" : ChatColor.DARK_RED + "Hackear Cadeado Inimigo";
        Inventory inv = Bukkit.createInventory(null, 27, titulo);

        RaidSession sessao = sessoesAtivas.get(jogador.getUniqueId());

        inv.setItem(11, criarItemDigito(sessao.digitos[0]));
        inv.setItem(12, criarItemDigito(sessao.digitos[1]));
        inv.setItem(13, criarItemDigito(sessao.digitos[2]));

        ItemStack confirmar = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = confirmar.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Confirmar Código");
        confirmar.setItemMeta(meta);
        inv.setItem(22, confirmar);

        jogador.openInventory(inv);
    }

    private ItemStack criarItemDigito(int numero) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Dígito: " + ChatColor.WHITE + numero);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void aoClicarNoCadeado(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();
        if (!titulo.equals(ChatColor.DARK_GREEN + "Definir Senha do Cadeado") && !titulo.equals(ChatColor.DARK_RED + "Hackear Cadeado Inimigo")) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player jogador = (Player) event.getWhoClicked();
        RaidSession sessao = sessoesAtivas.get(jogador.getUniqueId());
        if (sessao == null) return;

        int slot = event.getRawSlot();

        if (slot == 11) {
            sessao.digitos[0] = (sessao.digitos[0] + 1) % 10;
            event.getInventory().setItem(11, criarItemDigito(sessao.digitos[0]));
        } else if (slot == 12) {
            sessao.digitos[1] = (sessao.digitos[1] + 1) % 10;
            event.getInventory().setItem(12, criarItemDigito(sessao.digitos[1]));
        } else if (slot == 13) {
            sessao.digitos[2] = (sessao.digitos[2] + 1) % 10;
            event.getInventory().setItem(13, criarItemDigito(sessao.digitos[2]));
        } else if (slot == 22) {
            String codigoGerado = sessao.digitos[0] + "" + sessao.digitos[1] + "" + sessao.digitos[2];

            if (sessao.isSetting) {
                AeriaRaids.getInstance().getRaidStorage().setarSenha(sessao.loc, codigoGerado);
                jogador.closeInventory();
                jogador.sendMessage(ChatColor.GREEN + "✔️ Cadeado instalado! Senha definida: " + codigoGerado);
            } else {
                String senhaReal = AeriaRaids.getInstance().getRaidStorage().getSenha(sessao.loc);
                if (senhaReal != null && senhaReal.equals(codigoGerado)) {
                    AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(sessao.loc);
                    jogador.closeInventory();
                    jogador.sendMessage(ChatColor.GOLD + "🔓 Senha correta! O cadeado foi rompido com sucesso.");
                } else {
                    jogador.closeInventory();
                    jogador.sendMessage(ChatColor.RED + "❌ Senha incorreta! O mecanismo travou.");
                }
            }
            sessoesAtivas.remove(jogador.getUniqueId());
        }
    }
}