package com.aeriaplugins.raids.listeners;

import com.aeriaplugins.raids.AeriaRaids;
import com.aeriaplugins.raids.api.AeriaClanAPI;
import com.aeriaplugins.raids.commands.RaidCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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
        boolean bloqueadoAnimacao;

        RaidSession(Location loc, BlockFace face, SessionType type) {
            this.loc = loc;
            this.face = face;
            this.type = type;
            this.bloqueadoAnimacao = false;
        }
    }

    private final Map<UUID, RaidSession> sessoesAtivas = new HashMap<>();

    private boolean isCadeado(ItemStack item) {
        return item != null && item.getType() == Material.ITEM_FRAME &&
               item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Cadeado de Segredo");
    }

    @EventHandler
    public void aoInteragirComBau(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block bloco = event.getClickedBlock();
        if (bloco == null || (bloco.getType() != Material.CHEST && bloco.getType() != Material.TRAPPED_CHEST && bloco.getType() != Material.BARREL)) return;

        Player jogador = event.getPlayer();
        Location loc = bloco.getLocation();
        boolean trancado = AeriaRaids.getInstance().getRaidStorage().isTrancado(loc);
        UUID dono = AeriaRaids.getInstance().getRaidStorage().getDono(loc);

        if (trancado) {
            boolean temCadeadoVisual = false;
            for (Entity entity : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5)) {
                if (entity instanceof ItemFrame) {
                    ItemStack frameItem = ((ItemFrame) entity).getItem();
                    if (isCadeado(frameItem)) {
                        temCadeadoVisual = true;
                        break;
                    }
                }
            }

            if (!temCadeadoVisual) {
                AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(loc);
                trancado = false;
                jogador.sendMessage(ChatColor.YELLOW + "⚠️ [Sistema] Um registo de cadeado fantasma antigo foi limpo.");
            }
        }

        ItemStack itemMao = event.getItem();
        boolean segurandoCadeado = isCadeado(itemMao);

        if (!trancado) {
            if (segurandoCadeado) {
                BlockFace faceClicada = event.getBlockFace();
                BlockFace frenteBau = null;

                if (bloco.getBlockData() instanceof Directional) {
                    frenteBau = ((Directional) bloco.getBlockData()).getFacing();
                }

                if (frenteBau != null && faceClicada != frenteBau) {
                    event.setCancelled(true);
                    jogador.sendMessage(ChatColor.RED + "❌ O cadeado só pode ser instalado na parte frontal do compartimento.");
                    return;
                }

                event.setCancelled(true);
                sessoesAtivas.put(jogador.getUniqueId(), new RaidSession(loc, faceClicada, SessionType.SETTING_NEW_LOCK));
                abrirMenuDigitos(jogador, "Definir Senha do Cadeado");
            }
        } else {
            boolean isDono = dono != null && dono.equals(jogador.getUniqueId());
            boolean clanPermitido = dono != null && AeriaClanAPI.hasPermission(dono, jogador.getUniqueId(), AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(loc));
            boolean adminBypass = jogador.hasPermission("aeriaraids.admin") && AeriaRaids.getInstance().getConfig().getBoolean("admin-bypass", false);

            if (!isDono && !clanPermitido && !adminBypass) {
                event.setCancelled(true);
                jogador.sendMessage(ChatColor.RED + "🔒 Este compartimento possui uma trava mecânica.");
                jogador.sendMessage(ChatColor.GRAY + "Clique no cadeado fixado no baú para interagir ou hackear.");
            } else if (adminBypass && !isDono && !clanPermitido) {
                jogador.sendMessage(ChatColor.YELLOW + "⚠️ Acesso garantido via Admin Bypass.");
            }
        }
    }

    @EventHandler
    public void aoInteragirComEntidade(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame)) return;

        ItemFrame frame = (ItemFrame) event.getRightClicked();
        ItemStack item = frame.getItem();

        if (isCadeado(item)) {
            event.setCancelled(true); 

            Player jogador = event.getPlayer();
            Block bloco = frame.getLocation().getBlock().getRelative(frame.getFacing().getOppositeFace());
            Location loc = bloco.getLocation();

            if (AeriaRaids.getInstance().getRaidStorage().isTrancado(loc)) {
                UUID dono = AeriaRaids.getInstance().getRaidStorage().getDono(loc);
                boolean isDono = dono != null && dono.equals(jogador.getUniqueId());
                boolean clanPermitido = dono != null && AeriaClanAPI.hasPermission(dono, jogador.getUniqueId(), AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(loc));
                boolean adminBypass = jogador.hasPermission("aeriaraids.admin") && AeriaRaids.getInstance().getConfig().getBoolean("admin-bypass", false);

                if (isDono || adminBypass) {
                    sessoesAtivas.put(jogador.getUniqueId(), new RaidSession(loc, frame.getFacing(), SessionType.MANAGING));
                    abrirMenuGerenciamento(jogador, loc);
                    if (adminBypass && !isDono) jogador.sendMessage(ChatColor.YELLOW + "⚠️ Gerenciando cadeado via Admin Bypass.");
                } else if (clanPermitido) {
                    jogador.sendMessage(ChatColor.RED + "❌ Você não tem permissão para gerenciar este cadeado. Você pode apenas abrir o baú.");
                } else {
                    sessoesAtivas.put(jogador.getUniqueId(), new RaidSession(loc, frame.getFacing(), SessionType.HACKING));
                    abrirMenuDigitos(jogador, "Hackear Cadeado Inimigo");
                }
            }
        }
    }

    @EventHandler
    public void aoColocarMoldura(HangingPlaceEvent event) {
        ItemStack item = event.getItemStack();
        if (item == null) {
            item = event.getPlayer().getInventory().getItemInMainHand();
        }
        if (isCadeado(item)) {
            event.setCancelled(true);
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

        Inventory inv = Bukkit.createInventory(null, 36, titulo);
        RaidSession sessao = sessoesAtivas.get(jogador.getUniqueId());

        preencherFundo(inv, Material.GRAY_STAINED_GLASS_PANE, " ");

        inv.setItem(3, getBotaoControle("Aumentar Dígito 1", Material.LIME_STAINED_GLASS_PANE));
        inv.setItem(4, getBotaoControle("Aumentar Dígito 2", Material.LIME_STAINED_GLASS_PANE));
        inv.setItem(5, getBotaoControle("Aumentar Dígito 3", Material.LIME_STAINED_GLASS_PANE));

        inv.setItem(12, criarItemDigito(sessao.digitos[0]));
        inv.setItem(13, criarItemDigito(sessao.digitos[1]));
        inv.setItem(14, criarItemDigito(sessao.digitos[2]));

        inv.setItem(21, getBotaoControle("Diminuir Dígito 1", Material.RED_STAINED_GLASS_PANE));
        inv.setItem(22, getBotaoControle("Diminuir Dígito 2", Material.RED_STAINED_GLASS_PANE));
        inv.setItem(23, getBotaoControle("Diminuir Dígito 3", Material.RED_STAINED_GLASS_PANE));

        ItemStack confirmar = new ItemStack(Material.GREEN_DYE);
        ItemMeta meta = confirmar.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Confirmar Código");
            confirmar.setItemMeta(meta);
        }
        inv.setItem(31, confirmar);

        jogador.openInventory(inv);
    }

    private void preencherFundo(Inventory inv, Material material, String nome) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nome);
            item.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, item);
            }
        }
    }

    private ItemStack getBotaoControle(String nome, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + nome);
            item.setItemMeta(meta);
        }
        return item;
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
                sincronizarBaulDuplo(sessao.loc, "QUEBRAR", null, null, null);

                removerItemFrameVisual(sessao.loc);
                jogador.getInventory().addItem(RaidCommand.getCadeadoItem());
                jogador.closeInventory();
                jogador.sendMessage(ChatColor.GREEN + "🔓 Cadeado removido e devolvido ao seu inventário.");
                sessoesAtivas.remove(jogador.getUniqueId());
            } else if (slot == 15) {
                String rankAtual = AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(sessao.loc);
                String novoRank = AeriaClanAPI.getProximoRank(rankAtual);
                AeriaRaids.getInstance().getRaidStorage().setPermissaoClan(sessao.loc, novoRank);
                sincronizarBaulDuplo(sessao.loc, "CLAN", null, null, novoRank);

                abrirMenuGerenciamento(jogador, sessao.loc);
            }
            return;
        }

        if (isMenuDigitos) {
            if (sessao.bloqueadoAnimacao) return;

            if (slot == 3) {
                sessao.digitos[0] = (sessao.digitos[0] + 1) % 10;
                event.getInventory().setItem(12, criarItemDigito(sessao.digitos[0]));
            } else if (slot == 4) {
                sessao.digitos[1] = (sessao.digitos[1] + 1) % 10;
                event.getInventory().setItem(13, criarItemDigito(sessao.digitos[1]));
            } else if (slot == 5) {
                sessao.digitos[2] = (sessao.digitos[2] + 1) % 10;
                event.getInventory().setItem(14, criarItemDigito(sessao.digitos[2]));
            } else if (slot == 21) {
                sessao.digitos[0] = (sessao.digitos[0] - 1 + 10) % 10;
                event.getInventory().setItem(12, criarItemDigito(sessao.digitos[0]));
            } else if (slot == 22) {
                sessao.digitos[1] = (sessao.digitos[1] - 1 + 10) % 10;
                event.getInventory().setItem(13, criarItemDigito(sessao.digitos[1]));
            } else if (slot == 23) {
                sessao.digitos[2] = (sessao.digitos[2] - 1 + 10) % 10;
                event.getInventory().setItem(14, criarItemDigito(sessao.digitos[2]));
            } else if (slot == 31) {
                String codigoGerado = sessao.digitos[0] + "" + sessao.digitos[1] + "" + sessao.digitos[2];

                if (sessao.type == SessionType.SETTING_NEW_LOCK) {
                    if (removerCadeadoDoInventario(jogador)) {
                        AeriaRaids.getInstance().getRaidStorage().setarSenha(sessao.loc, codigoGerado, jogador.getUniqueId());
                        sincronizarBaulDuplo(sessao.loc, "SETAR", codigoGerado, jogador.getUniqueId(), null);

                        gerarItemFrameVisual(sessao.loc, sessao.face);
                        jogador.sendMessage(ChatColor.GREEN + "✔️ Cadeado instalado! Senha definida: " + codigoGerado);
                    } else {
                        jogador.sendMessage(ChatColor.RED + "❌ Você não possui o Cadeado de Segredo no inventário.");
                    }
                    jogador.closeInventory();
                    sessoesAtivas.remove(jogador.getUniqueId());
                } else if (sessao.type == SessionType.CHANGING_PASSWORD) {
                    AeriaRaids.getInstance().getRaidStorage().atualizarSenha(sessao.loc, codigoGerado);
                    sincronizarBaulDuplo(sessao.loc, "ATUALIZAR_SENHA", codigoGerado, null, null);

                    jogador.sendMessage(ChatColor.GREEN + "✔️ Senha alterada com sucesso para: " + codigoGerado);
                    jogador.closeInventory();
                    sessoesAtivas.remove(jogador.getUniqueId());
                } else if (sessao.type == SessionType.HACKING) {
                    String senhaReal = AeriaRaids.getInstance().getRaidStorage().getSenha(sessao.loc);
                    if (senhaReal != null && senhaReal.equals(codigoGerado)) {
                        AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(sessao.loc);
                        sincronizarBaulDuplo(sessao.loc, "QUEBRAR", null, null, null);

                        removerItemFrameVisual(sessao.loc);
                        jogador.closeInventory();
                        jogador.sendMessage(ChatColor.GOLD + "🔓 Senha correta! O cadeado foi rompido com sucesso.");
                        sessoesAtivas.remove(jogador.getUniqueId());
                    } else {
                        executarFeedbackErro(jogador, event.getInventory(), sessao);
                    }
                }
            }
        }
    }

    private void executarFeedbackErro(Player jogador, Inventory inv, RaidSession sessao) {
        sessao.bloqueadoAnimacao = true;
        jogador.playSound(jogador.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        
        ItemStack vidroErro = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta metaErro = vidroErro.getItemMeta();
        if (metaErro != null) {
            metaErro.setDisplayName(ChatColor.RED + "❌ ACESSO NEGADO");
            vidroErro.setItemMeta(metaErro);
        }

        List<Integer> slotsFundo = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) != null && inv.getItem(i).getType() == Material.GRAY_STAINED_GLASS_PANE) {
                slotsFundo.add(i);
                inv.setItem(i, vidroErro);
            }
        }

        Bukkit.getScheduler().runTaskLater(AeriaRaids.getInstance(), () -> {
            if (jogador.getOpenInventory().getTopInventory().equals(inv)) {
                ItemStack vidroPadrao = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta metaPadrao = vidroPadrao.getItemMeta();
                if (metaPadrao != null) {
                    metaPadrao.setDisplayName(" ");
                    vidroPadrao.setItemMeta(metaPadrao);
                }
                for (int slot : slotsFundo) {
                    inv.setItem(slot, vidroPadrao);
                }
            }
            sessao.bloqueadoAnimacao = false;
        }, 20L);
    }

    @EventHandler
    public void aoColocarBloco(BlockPlaceEvent event) {
        Block bloco = event.getBlockPlaced();
        Location loc = bloco.getLocation();

        if (AeriaRaids.getInstance().getRaidStorage().isTrancado(loc)) {
            AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(loc);
            removerItemFrameVisual(loc);
        }

        if (bloco.getType() == Material.CHEST || bloco.getType() == Material.TRAPPED_CHEST) {
            if (bloco.getBlockData() instanceof org.bukkit.block.data.type.Chest) {
                org.bukkit.block.data.type.Chest chestData = (org.bukkit.block.data.type.Chest) bloco.getBlockData();
                if (chestData.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
                    BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
                    for (BlockFace face : faces) {
                        Block adj = bloco.getRelative(face);
                        if (adj.getType() == bloco.getType() && AeriaRaids.getInstance().getRaidStorage().isTrancado(adj.getLocation())) {
                            UUID dono = AeriaRaids.getInstance().getRaidStorage().getDono(adj.getLocation());
                            String senha = AeriaRaids.getInstance().getRaidStorage().getSenha(adj.getLocation());
                            String clan = AeriaRaids.getInstance().getRaidStorage().getPermissaoClan(adj.getLocation());

                            AeriaRaids.getInstance().getRaidStorage().setarSenha(bloco.getLocation(), senha, dono);
                            AeriaRaids.getInstance().getRaidStorage().setPermissaoClan(bloco.getLocation(), clan);
                            
                            event.getPlayer().sendMessage(ChatColor.GREEN + "✔️ A extensão do baú duplo foi trancada automaticamente junto com a parte original.");
                            break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void aoQuebrarBloco(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (AeriaRaids.getInstance().getRaidStorage().isTrancado(loc)) {
            Block bloco = event.getBlock();
            
            if (bloco.getType() != Material.CHEST && bloco.getType() != Material.TRAPPED_CHEST && bloco.getType() != Material.BARREL) {
                AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(loc);
                removerItemFrameVisual(loc);
                return;
            }

            boolean temCadeadoVisual = false;
            for (Entity entity : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5)) {
                if (entity instanceof ItemFrame) {
                    ItemStack frameItem = ((ItemFrame) entity).getItem();
                    if (isCadeado(frameItem)) {
                        temCadeadoVisual = true;
                        break;
                    }
                }
            }

            if (!temCadeadoVisual) {
                AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(loc);
                event.getPlayer().sendMessage(ChatColor.YELLOW + "⚠️ [Sistema] Um registo de cadeado fantasma foi limpo e o bloco libertado.");
                return;
            }

            boolean adminBypass = event.getPlayer().hasPermission("aeriaraids.admin") && AeriaRaids.getInstance().getConfig().getBoolean("admin-bypass", false);
            if (adminBypass) {
                AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(loc);
                sincronizarBaulDuplo(loc, "QUEBRAR", null, null, null);
                removerItemFrameVisual(loc);
                event.getPlayer().sendMessage(ChatColor.YELLOW + "⚠️ Compartimento trancado destruído via Admin Bypass.");
                return;
            }

            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "❌ Remova o cadeado antes de quebrar este compartimento.");
        }
    }

    @EventHandler
    public void aoQueimarBloco(BlockBurnEvent event) {
        if (AeriaRaids.getInstance().getRaidStorage().isTrancado(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void aoPistaoEstender(BlockPistonExtendEvent event) {
        for (Block bloco : event.getBlocks()) {
            if (AeriaRaids.getInstance().getRaidStorage().isTrancado(bloco.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void aoPistaoRetrair(BlockPistonRetractEvent event) {
        for (Block bloco : event.getBlocks()) {
            if (AeriaRaids.getInstance().getRaidStorage().isTrancado(bloco.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void aoQuebrarMoldura(HangingBreakEvent event) {
        if (event.getEntity() instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame) event.getEntity();
            ItemStack item = frame.getItem();
            if (isCadeado(item)) {
                Location locAtras = frame.getLocation().getBlock().getRelative(frame.getFacing().getOppositeFace()).getLocation();
                Block blocoAtras = locAtras.getBlock();

                if (blocoAtras.getType() != Material.CHEST && blocoAtras.getType() != Material.TRAPPED_CHEST && blocoAtras.getType() != Material.BARREL) {
                    event.setCancelled(true);
                    frame.remove();
                    AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(locAtras);
                    frame.getWorld().dropItemNaturally(frame.getLocation(), RaidCommand.getCadeadoItem());
                    return; 
                }

                if (event instanceof org.bukkit.event.hanging.HangingBreakByEntityEvent) {
                    org.bukkit.event.hanging.HangingBreakByEntityEvent entityEvent = (org.bukkit.event.hanging.HangingBreakByEntityEvent) event;
                    if (entityEvent.getRemover() instanceof Player) {
                        Player jogador = (Player) entityEvent.getRemover();
                        if (jogador.hasPermission("aeriaraids.admin") && AeriaRaids.getInstance().getConfig().getBoolean("admin-bypass", false)) {
                            AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(locAtras);
                            sincronizarBaulDuplo(locAtras, "QUEBRAR", null, null, null);
                            jogador.sendMessage(ChatColor.YELLOW + "⚠️ Cadeado removido via Admin Bypass.");
                            return;
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void aoMoverItemFunil(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) event.getSource().getHolder();
            Location left = ((BlockState) dc.getLeftSide()).getLocation();
            Location right = ((BlockState) dc.getRightSide()).getLocation();
            if (AeriaRaids.getInstance().getRaidStorage().isTrancado(left) || AeriaRaids.getInstance().getRaidStorage().isTrancado(right)) {
                event.setCancelled(true);
                return;
            }
        } else if (event.getSource().getHolder() instanceof BlockState) {
            Location loc = ((BlockState) event.getSource().getHolder()).getLocation();
            if (AeriaRaids.getInstance().getRaidStorage().isTrancado(loc)) {
                event.setCancelled(true);
                return;
            }
        } else if (event.getSource().getLocation() != null) {
            if (AeriaRaids.getInstance().getRaidStorage().isTrancado(event.getSource().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void aoExplodirEntidade(EntityExplodeEvent event) {
        event.blockList().removeIf(bloco -> AeriaRaids.getInstance().getRaidStorage().isTrancado(bloco.getLocation()));
    }

    @EventHandler
    public void aoExplodirBloco(BlockExplodeEvent event) {
        event.blockList().removeIf(bloco -> AeriaRaids.getInstance().getRaidStorage().isTrancado(bloco.getLocation()));
    }

    @EventHandler
    public void aoEndermanPegarBloco(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Enderman) {
            if (AeriaRaids.getInstance().getRaidStorage().isTrancado(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    private void sincronizarBaulDuplo(Location loc, String acao, String senha, UUID dono, String rank) {
        Block bloco = loc.getBlock();
        if (bloco.getType() == Material.CHEST || bloco.getType() == Material.TRAPPED_CHEST) {
            BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            for (BlockFace face : faces) {
                Block adj = bloco.getRelative(face);
                if (adj.getType() == bloco.getType()) {
                    switch (acao) {
                        case "SETAR":
                            AeriaRaids.getInstance().getRaidStorage().setarSenha(adj.getLocation(), senha, dono);
                            break;
                        case "QUEBRAR":
                            AeriaRaids.getInstance().getRaidStorage().quebrarCadeado(adj.getLocation());
                            break;
                        case "ATUALIZAR_SENHA":
                            AeriaRaids.getInstance().getRaidStorage().atualizarSenha(adj.getLocation(), senha);
                            break;
                        case "CLAN":
                            AeriaRaids.getInstance().getRaidStorage().setPermissaoClan(adj.getLocation(), rank);
                            break;
                    }
                }
            }
        }
    }

    private boolean removerCadeadoDoInventario(Player jogador) {
        for (int i = 0; i < jogador.getInventory().getSize(); i++) {
            ItemStack item = jogador.getInventory().getItem(i);
            if (isCadeado(item)) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    private void gerarItemFrameVisual(Location loc, BlockFace face) {
        Location frameLoc = loc.getBlock().getRelative(face).getLocation();
        ItemFrame frame = loc.getWorld().spawn(frameLoc, ItemFrame.class);
        frame.setFacingDirection(face, true);
        frame.setItem(RaidCommand.getCadeadoItem());
        frame.setFixed(true);
        frame.setVisible(false);
    }

    private void removerItemFrameVisual(Location loc) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc.getBlock().getLocation().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5)) {
            if (entity instanceof ItemFrame) {
                ItemFrame frame = (ItemFrame) entity;
                ItemStack item = frame.getItem();
                if          (isCadeado(item)) {
                    frame.remove();
                }
            }
        }
    }
}