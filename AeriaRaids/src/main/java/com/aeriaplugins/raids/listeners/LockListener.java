package com.aeriaplugins.raids.listeners;

import com.aeriaplugins.raids.AeriaRaids;
import com.aeriaplugins.raids.api.AeriaClanAPI;
import com.aeriaplugins.raids.commands.RaidCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LockListener implements Listener {

    private enum SessionType {
        SETTING_NEW_LOCK,
        HACKING,
        CHANGING_PASSWORD,
        MANAGING
    }

    private static class RaidSession {
        Location loc;
        BlockFace face;
        int[] digitos = new int[]{0, 0, 0};
        SessionType type;

        RaidSession(Location loc, BlockFace face, SessionType type) {
            this.loc = loc;
            this.face = face;
            this.type = type;
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
        UUID dono = AeriaRaids.getInstance().getRaidStorage().getDono(loc);

        ItemStack itemMao = event.getItem();
        boolean segurandoCadeado = itemMao != null && itemMao.getType() == Material.ITEM_FRAME && itemMao.hasItemMeta() && itemMao.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Cadeado de Segredo");

        if (!trancado) {
            if (segurandoCadeado) {
                event.setCancelled(true);
                sessoesAtivas.put(jogador.getUniqueId(), new RaidSession(loc, event.getBlockFace(), SessionType.SETTING_NEW_LOCK));
                abrirMenuDigitos(jogador, "Definir Senha do Cadeado");
            }
        } else {
            boolean isDono = dono != null && dono.equals(jogador.getUniqueId());
            boolean clanPermitido = dono != null && AeriaClanAPI.hasPermission(dono, jogador.getUniqueId(), AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(loc));

            if (isDono) {
                if (jogador.isSneaking()) {
                    event.setCancelled(true);
                    sessoesAtivas.put(jogador.getUniqueId(), new RaidSession(loc, event.getBlockFace(), SessionType.MANAGING));
                    abrirMenuGerenciamento(jogador, loc);
                }
            } else if (clanPermitido) {
                if (jogador.isSneaking()) {
                    event.setCancelled(true);
                    jogador.sendMessage(ChatColor.RED + "❌ Você não tem permissão para gerenciar este cadeado, apenas para abri-lo.");
                }
            } else {
                event.setCancelled(true);
                if (jogador.isSneaking()) {
                    sessoesAtivas.put(jogador.getUniqueId(), new RaidSession(loc, event.getBlockFace(), SessionType.HACKING));
                    abrirMenuDigitos(jogador, "Hackear Cadeado Inimigo");
                } else {
                    jogador.sendMessage(ChatColor.RED + "🔒 Este compartimento possui uma trava mecânica.");
                    jogador.sendMessage(ChatColor.GRAY + "Agache e clique com o botão direito para tentar hackear o código.");
                }
            }
        }
    }

    private void abrirMenuGerenciamento(Player jogador, Location loc) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "Gerenciar Cadeado");

        ItemStack alterarSenha = new ItemStack(Material.PAPER);
        ItemMeta metaSenha = alterarSenha.getItemMeta();
        if (metaSenha != null) {
            metaSenha.setDisplayName(ChatColor.YELLOW + "🔑 Alterar Senha");
            alterarSenha.setItemMeta(metaSenha);
        }
        inv.setItem(11, alterarSenha);

        ItemStack removerCadeado = new ItemStack(Material.BARRIER);
        ItemMeta metaRemover = removerCadeado.getItemMeta();
        if (metaRemover != null) {
            metaRemover.setDisplayName(ChatColor.RED + "🔓 Remover Cadeado");
            removerCadeado.setItemMeta(metaRemover);
        }
        inv.setItem(13, removerCadeado);

        String rankAtual = AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(loc);
        ItemStack permissaoClan = new ItemStack(Material.SHIELD);
        ItemMeta metaClan = permissaoClan.getItemMeta();
        if (metaClan != null) {
            metaClan.setDisplayName(ChatColor.AQUA + "🛡️ Permissão do Clan");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Cargo mínimo para acessar:");
            lore.add(ChatColor.GREEN + AeriaClanAPI.ClanRole.valueOf(rankAtual).getDisplay());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Clique para alterar");
            metaClan.setLore(lore);
            permissaoClan.setItemMeta(metaClan);
        }
        inv.setItem(15, permissaoClan);

        jogador.openInventory(inv);
    }

    private void abrirMenuDigitos(Player jogador, String tituloSemCor) {
        String titulo;
        if (tituloSemCor.equals("Definir Senha do Cadeado")) titulo = ChatColor.DARK_GREEN + tituloSemCor;
        else if (tituloSemCor.equals("Alterar Senha do Cadeado")) titulo = ChatColor.GOLD + tituloSemCor;
        else titulo = ChatColor.DARK_RED + tituloSemCor;

        Inventory inv = Bukkit.createInventory(null, 27, titulo);
        RaidSession sessao = sessoesAtivas.get(jogador.getUniqueId());

        inv.setItem(11, criarItemDigito(sessao.digitos[0]));
        inv.setItem(12, criarItemDigito(sessao.digitos[1]));
        inv.setItem(13, criarItemDigito(sessao.digitos[2]));

        ItemStack confirmar = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = confirmar.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Confirmar Código");
            confirmar.setItemMeta(meta);
        }
        inv.setItem(22, confirmar);

        jogador.openInventory(inv);
    }

    private ItemStack criarItemDigito(int numero) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Dígito: " + ChatColor.WHITE + numero);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void aoClicarNoInventario(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();
        boolean isMenuDigitos = titulo.contains("Definir Senha") || titulo.contains("Hackear Cadeado") || titulo.contains("Alterar Senha");
        boolean isMenuGerenciamento = titulo.equals(ChatColor.DARK_BLUE + "Gerenciar Cadeado");

        if (!isMenuDigitos && !isMenuGerenciamento) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player jogador = (Player) event.getWhoClicked();
        RaidSession sessao = sessoesAtivas.get(jogador.getUniqueId());
        if (sessao == null) return;

        int slot = event.getRawSlot();

        if (isMenuGerenciamento) {
            if (slot == 11) {
                sessao.type = SessionType.CHANGING_PASSWORD;
                abrirMenuDigitos(jogador, "Alterar Senha do Cadeado");
            } else if (slot == 13) {
                AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(sessao.loc);
                removerItemFrameVisual(sessao.loc);
                jogador.getInventory().addItem(RaidCommand.getCadeadoItem());
                jogador.closeInventory();
                jogador.sendMessage(ChatColor.GREEN + "🔓 Cadeado removido e devolvido ao seu inventário.");
                sessoesAtivas.remove(jogador.getUniqueId());
            } else if (slot == 15) {
                String rankAtual = AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(sessao.loc);
                String novoRank = AeriaClanAPI.getProximoRank(rankAtual);
                AeriaRaids.getInstance().getRaidStorage().setPermissaoClan(sessao.loc, novoRank);
                abrirMenuGerenciamento(jogador, sessao.loc);
            }
            return;
        }

        if (isMenuDigitos) {
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

                if (sessao.type == SessionType.SETTING_NEW_LOCK) {
                    if (removerCadeadoDoInventario(jogador)) {
                        AeriaRaids.getInstance().getRaidStorage().setarSenha(sessao.loc, codigoGerado, jogador.getUniqueId());
                        gerarItemFrameVisual(sessao.loc, sessao.face);
                        jogador.sendMessage(ChatColor.GREEN + "✔️ Cadeado instalado! Senha definida: " + codigoGerado);
                    } else {
                        jogador.sendMessage(ChatColor.RED + "❌ Você não possui o Cadeado de Segredo no inventário.");
                    }
                    jogador.closeInventory();
                    sessoesAtivas.remove(jogador.getUniqueId());
                } else if (sessao.type == SessionType.CHANGING_PASSWORD) {
                    AeriaRaids.getInstance().getRaidStorage().atualizarSenha(sessao.loc, codigoGerado);
                    jogador.sendMessage(ChatColor.GREEN + "✔️ Senha alterada com sucesso para: " + codigoGerado);
                    jogador.closeInventory();
                    sessoesAtivas.remove(jogador.getUniqueId());
                } else if (sessao.type == SessionType.HACKING) {
                    String senhaReal = AeriaRaids.getInstance().getRaidStorage().getSenha(sessao.loc);
                    if (senhaReal != null && senhaReal.equals(codigoGerado)) {
                        AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(sessao.loc);
                        removerItemFrameVisual(sessao.loc);
                        jogador.closeInventory();
                        jogador.sendMessage(ChatColor.GOLD + "🔓 Senha correta! O cadeado foi rompido com sucesso.");
                    } else {
                        jogador.closeInventory();
                        jogador.sendMessage(ChatColor.RED + "❌ Senha incorreta! O mecanismo travou.");
                    }
                    sessoesAtivas.remove(jogador.getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void aoQuebrarBloco(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (AeriaRaids.getInstance().getRaidStorage().isTrancado(loc)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "❌ Remova o cadeado antes de quebrar este compartimento.");
        }
    }

    @EventHandler
    public void aoQuebrarMoldura(HangingBreakEvent event) {
        if (event.getEntity() instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame) event.getEntity();
            ItemStack item = frame.getItem();
            if (item != null && item.getType() == Material.ITEM_FRAME && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Cadeado de Segredo")) {
                event.setCancelled(true);
            }
        }
    }

    private boolean removerCadeadoDoInventario(Player jogador) {
        for (int i = 0; i < jogador.getInventory().getSize(); i++) {
            ItemStack item = jogador.getInventory().getItem(i);
            if (item != null && item.getType() == Material.ITEM_FRAME && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Cadeado de Segredo")) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    private void gerarItemFrameVisual(Location loc, BlockFace face) {
        Location frameLoc = loc.getBlock().getRelative(face).getLocation();
        ItemFrame frame = loc.getWorld().spawn(frameLoc, ItemFrame.class);
        frame.setFacingDirection(face);
        frame.setItem(RaidCommand.getCadeadoItem());
        frame.setFixed(true);
        frame.setVisible(false);
    }

    private void removerItemFrameVisual(Location loc) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc.getBlock().getLocation().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5)) {
            if (entity instanceof ItemFrame) {
                ItemFrame frame = (ItemFrame) entity;
                ItemStack item = frame.getItem();
                if (item != null && item.getType() == Material.ITEM_FRAME && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Cadeado de Segredo")) {
                    frame.remove();
                }
            }
        }
    }
}