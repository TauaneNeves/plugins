package com.aeriaplugins.raids.listeners;

import com.aeriaplugins.raids.AeriaRaids;
import com.aeriaplugins.raids.api.AeriaClanAPI;
import com.aeriaplugins.raids.commands.RaidCommand;
import com.aeriaplugins.raids.utils.LockUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MenuListener implements Listener {

    public enum SessionType { SETTING_NEW_LOCK, HACKING, CHANGING_PASSWORD, MANAGING }

    public static class RaidSession {
        public Location loc; public BlockFace face; public int[] digitos = new int[]{0, 0, 0}; public SessionType type; public boolean bloqueadoAnimacao;
        public RaidSession(Location loc, BlockFace face, SessionType type) { this.loc = loc; this.face = face; this.type = type; }
    }

    private final Map<UUID, RaidSession> sessoesAtivas = new HashMap<>();

    @EventHandler
    public void aoInteragirComBau(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block bloco = event.getClickedBlock();
        if (!LockUtils.isBlocoTrancavel(bloco)) return;

        if (AeriaRaids.getInstance().getRaidManager().isEmRaid()) {
            event.getPlayer().sendMessage(ChatColor.RED + "❌ A base está sob efeito de Raid. Acesso restrito.");
            event.setCancelled(true);
            return;
        }

        Player jogador = event.getPlayer();
        Location loc = bloco.getLocation();
        boolean trancado = AeriaRaids.getInstance().getRaidStorage().isTrancado(loc);
        UUID dono = AeriaRaids.getInstance().getRaidStorage().getDono(loc);

        if (trancado) {
            boolean temCadeadoVisual = loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5).stream()
                    .anyMatch(e -> e instanceof ItemFrame && LockUtils.isCadeado(((ItemFrame) e).getItem()));
            if (!temCadeadoVisual) { AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(loc); trancado = false; }
        }

        if (!trancado && LockUtils.isCadeado(event.getItem())) {
            BlockFace face = event.getBlockFace();
            BlockFace frente = (bloco.getBlockData() instanceof Directional d) ? d.getFacing() : null;
            
            if (frente != null && face != frente && !(bloco.getBlockData() instanceof org.bukkit.block.data.type.Door)) {
                event.setCancelled(true);
                jogador.sendMessage(ChatColor.RED + "❌ O cadeado só pode ser instalado na parte frontal.");
                return;
            }
            event.setCancelled(true);
            sessoesAtivas.put(jogador.getUniqueId(), new RaidSession(loc, face, SessionType.SETTING_NEW_LOCK));
            abrirMenuDigitos(jogador, "Definir Senha do Cadeado");

        } else if (trancado) {
            boolean isDono = dono != null && dono.equals(jogador.getUniqueId());
            boolean clanPermitido = dono != null && AeriaClanAPI.hasPermission(dono, jogador.getUniqueId(), AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(loc));
            boolean adminBypass = jogador.hasPermission("aeriaraids.admin") && AeriaRaids.getInstance().getConfig().getBoolean("admin-bypass", false);

            if (!isDono && !clanPermitido && !adminBypass) {
                event.setCancelled(true);
                jogador.sendMessage(ChatColor.RED + "🔒 Este compartimento possui uma trava mecânica.\n" + ChatColor.GRAY + "Clique no cadeado fixado no baú para interagir ou hackear.");
            } else if (adminBypass && !isDono && !clanPermitido) {
                jogador.sendMessage(ChatColor.YELLOW + "⚠️ Acesso garantido via Admin Bypass.");
            }
        }
    }

    @EventHandler
    public void aoInteragirComEntidade(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame frame && LockUtils.isCadeado(frame.getItem())) {
            event.setCancelled(true); 
            Player jogador = event.getPlayer();
            Location loc = frame.getLocation().getBlock().getRelative(frame.getFacing().getOppositeFace()).getLocation();

            if (AeriaRaids.getInstance().getRaidStorage().isTrancado(loc)) {
                UUID dono = AeriaRaids.getInstance().getRaidStorage().getDono(loc);
                boolean isDono = dono != null && dono.equals(jogador.getUniqueId());
                boolean clanPerm = dono != null && AeriaClanAPI.hasPermission(dono, jogador.getUniqueId(), AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(loc));
                boolean admin = jogador.hasPermission("aeriaraids.admin") && AeriaRaids.getInstance().getConfig().getBoolean("admin-bypass", false);

                if (isDono || admin) {
                    sessoesAtivas.put(jogador.getUniqueId(), new RaidSession(loc, frame.getFacing(), SessionType.MANAGING));
                    abrirMenuGerenciamento(jogador, loc);
                } else if (clanPerm) {
                    jogador.sendMessage(ChatColor.RED + "❌ Você pode apenas abrir o baú.");
                } else {
                    sessoesAtivas.put(jogador.getUniqueId(), new RaidSession(loc, frame.getFacing(), SessionType.HACKING));
                    abrirMenuDigitos(jogador, "Hackear Cadeado Inimigo");
                }
            }
        }
    }

    private void abrirMenuGerenciamento(Player jogador, Location loc) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "Gerenciar Cadeado");
        inv.setItem(11, LockUtils.criarItem(Material.PAPER, ChatColor.YELLOW + "🔑 Alterar Senha", null));
        inv.setItem(13, LockUtils.criarItem(Material.BARRIER, ChatColor.RED + "🔓 Remover Cadeado", null));
        
        String rank = AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(loc);
        inv.setItem(15, LockUtils.criarItem(Material.SHIELD, ChatColor.AQUA + "🛡️ Permissão do Clan", List.of(ChatColor.GRAY + "Acesso mínimo:", ChatColor.GREEN + AeriaClanAPI.ClanRole.valueOf(rank).getDisplay(), "", ChatColor.YELLOW + "Clique para alterar")));
        jogador.openInventory(inv);
    }

    private void abrirMenuDigitos(Player jogador, String tituloSemCor) {
        String titulo = switch(tituloSemCor) {
            case "Definir Senha do Cadeado" -> ChatColor.DARK_GREEN + tituloSemCor;
            case "Alterar Senha do Cadeado" -> ChatColor.GOLD + tituloSemCor;
            default -> ChatColor.DARK_RED + tituloSemCor;
        };

        Inventory inv = Bukkit.createInventory(null, 36, titulo);
        RaidSession sessao = sessoesAtivas.get(jogador.getUniqueId());

        ItemStack fundo = LockUtils.criarItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 36; i++) inv.setItem(i, fundo);

        for (int i = 0; i < 3; i++) {
            inv.setItem(i + 3, LockUtils.criarItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.YELLOW + "Aumentar Dígito " + (i+1), null));
            inv.setItem(i + 12, LockUtils.criarItem(Material.PAPER, ChatColor.AQUA + "Dígito: " + ChatColor.WHITE + sessao.digitos[i], null));
            inv.setItem(i + 21, LockUtils.criarItem(Material.RED_STAINED_GLASS_PANE, ChatColor.YELLOW + "Diminuir Dígito " + (i+1), null));
        }
        inv.setItem(31, LockUtils.criarItem(Material.GREEN_DYE, ChatColor.GREEN + "Confirmar Código", null));
        jogador.openInventory(inv);
    }

    @EventHandler
    public void aoClicarNoInventario(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();
        if (!titulo.contains("Definir Senha") && !titulo.contains("Hackear") && !titulo.contains("Alterar Senha") && !titulo.equals(ChatColor.DARK_BLUE + "Gerenciar Cadeado")) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player jogador) || !sessoesAtivas.containsKey(jogador.getUniqueId())) return;
        RaidSession sessao = sessoesAtivas.get(jogador.getUniqueId());
        int slot = event.getRawSlot();

        if (titulo.equals(ChatColor.DARK_BLUE + "Gerenciar Cadeado")) {
            switch (slot) {
                case 11 -> { sessao.type = SessionType.CHANGING_PASSWORD; abrirMenuDigitos(jogador, "Alterar Senha do Cadeado"); }
                case 13 -> {
                    LockUtils.sincronizarBaulDuplo(sessao.loc, "QUEBRAR", null, null, null);
                    LockUtils.removerItemFrameVisual(sessao.loc);
                    jogador.getInventory().addItem(RaidCommand.getCadeadoItem());
                    jogador.closeInventory();
                    jogador.sendMessage(ChatColor.GREEN + "🔓 Cadeado removido e devolvido.");
                    sessoesAtivas.remove(jogador.getUniqueId());
                }
                case 15 -> {
                    String novoRank = AeriaClanAPI.getProximoRank(AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(sessao.loc));
                    LockUtils.sincronizarBaulDuplo(sessao.loc, "CLAN", null, null, novoRank);
                    abrirMenuGerenciamento(jogador, sessao.loc);
                }
            }
            return;
        }

        if (sessao.bloqueadoAnimacao) return;

        if (slot >= 3 && slot <= 5) {
            int idx = slot - 3;
            sessao.digitos[idx] = (sessao.digitos[idx] + 1) % 10;
            event.getInventory().setItem(slot + 9, LockUtils.criarItem(Material.PAPER, ChatColor.AQUA + "Dígito: " + ChatColor.WHITE + sessao.digitos[idx], null));
        } else if (slot >= 21 && slot <= 23) {
            int idx = slot - 21;
            sessao.digitos[idx] = (sessao.digitos[idx] - 1 + 10) % 10;
            event.getInventory().setItem(slot - 9, LockUtils.criarItem(Material.PAPER, ChatColor.AQUA + "Dígito: " + ChatColor.WHITE + sessao.digitos[idx], null));
        } else if (slot == 31) {
            String cod = "" + sessao.digitos[0] + sessao.digitos[1] + sessao.digitos[2];

            switch (sessao.type) {
                case SETTING_NEW_LOCK -> {
                    if (LockUtils.removerCadeadoDoInventario(jogador)) {
                        LockUtils.sincronizarBaulDuplo(sessao.loc, "SETAR", cod, jogador.getUniqueId(), null);
                        LockUtils.gerarItemFrameVisual(sessao.loc, sessao.face);
                        jogador.sendMessage(ChatColor.GREEN + "✔️ Cadeado instalado! Senha: " + cod);
                    } else jogador.sendMessage(ChatColor.RED + "❌ Você não possui o Cadeado.");
                    jogador.closeInventory(); sessoesAtivas.remove(jogador.getUniqueId());
                }
                case CHANGING_PASSWORD -> {
                    LockUtils.sincronizarBaulDuplo(sessao.loc, "ATUALIZAR_SENHA", cod, null, null);
                    jogador.sendMessage(ChatColor.GREEN + "✔️ Senha alterada para: " + cod);
                    jogador.closeInventory(); sessoesAtivas.remove(jogador.getUniqueId());
                }
                case HACKING -> {
                    if (cod.equals(AeriaRaids.getInstance().getRaidStorage().getSenha(sessao.loc))) {
                        LockUtils.sincronizarBaulDuplo(sessao.loc, "QUEBRAR", null, null, null);
                        LockUtils.removerItemFrameVisual(sessao.loc);
                        jogador.closeInventory();
                        jogador.sendMessage(ChatColor.GOLD + "🔓 Senha correta! Cadeado rompido.");
                        sessoesAtivas.remove(jogador.getUniqueId());
                    } else executarFeedbackErro(jogador, event.getInventory(), sessao);
                }
            }
        }
    }

    private void executarFeedbackErro(Player jogador, Inventory inv, RaidSession sessao) {
        sessao.bloqueadoAnimacao = true;
        jogador.playSound(jogador.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        ItemStack vidroErro = LockUtils.criarItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "❌ ACESSO NEGADO", null);
        
        List<Integer> bg = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) != null && inv.getItem(i).getType() == Material.GRAY_STAINED_GLASS_PANE) { bg.add(i); inv.setItem(i, vidroErro); }

        Bukkit.getScheduler().runTaskLater(AeriaRaids.getInstance(), () -> {
            if (jogador.getOpenInventory().getTopInventory().equals(inv)) {
                ItemStack padrao = LockUtils.criarItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
                for (int slot : bg) inv.setItem(slot, padrao);
            }
            sessao.bloqueadoAnimacao = false;
        }, 20L);
    }
}