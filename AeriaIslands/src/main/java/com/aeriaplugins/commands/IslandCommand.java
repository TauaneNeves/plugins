package com.aeriaplugins.commands;

import com.aeriaplugins.plugins.Main;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IslandCommand implements CommandExecutor, Listener {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final String TITULO_MENU;

    public IslandCommand() {
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        String titulo = Main.getInstance().getConfig().getString("nome-do-menu", "&8Painel de Controle: Navegação");
        this.TITULO_MENU = ChatColor.translateAlternateColorCodes('&', titulo);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores em jogo utilizam este comando.");
            return true;
        }

        Player jogador = (Player) sender;

        if (args.length == 0) {
            abrirMenuPainelis(jogador);
            return true;
        }

        String subComando = args[0].toLowerCase();

        switch (subComando) {
            case "go":
                Location localIlha = Main.getInstance().getIslandStorage().getPrimeiraIlhaLocation(jogador.getUniqueId());
                if (localIlha == null) {
                    jogador.sendMessage(ChatColor.RED + "❌ Você não possui uma ilha. Use o menu para criar uma.");
                    return true;
                }
                if (localIlha.getWorld() == null) {
                    jogador.sendMessage(ChatColor.RED + "❌ O mundo da sua ilha está descarregado. Use /mv load skyblock_void");
                    return true;
                }
                jogador.teleport(localIlha);
                jogador.sendMessage(ChatColor.GREEN + "🚀 Teleportado para o spawn oficial da sua base.");
                return true;

            case "setspawn":
                configurarSpawnCustomizado(jogador);
                return true;

            case "info":
                exibirInfoIlha(jogador);
                return true;

            case "delete":
                if (args.length < 2) {
                    jogador.sendMessage(ChatColor.RED + "❌ Informe o ID da ilha. Ex: /is delete 1");
                    return true;
                }

                String idAlvo = args[1];
                String nomeAlvoDelecao = "Ilha #" + idAlvo;

                Location localDeletar = Main.getInstance().getIslandStorage().getLocalizacaoIlhaPorNome(jogador.getUniqueId(), nomeAlvoDelecao);
                
                if (localDeletar == null) {
                    jogador.sendMessage(ChatColor.RED + "❌ Registro de ID " + idAlvo + " não localizado.");
                    return true;
                }

                jogador.sendMessage(ChatColor.RED + "⚠️ Removendo estruturas da " + nomeAlvoDelecao + "...");
                removerEstruturaFisica(localDeletar);
                Main.getInstance().getIslandStorage().removerIlhaPorNome(jogador.getUniqueId(), nomeAlvoDelecao);
                
                jogador.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                jogador.sendMessage(ChatColor.GOLD + "💥 Ilha deletada com sucesso.");
                return true;
        }
        return true;
    }

    private void abrirMenuPainelis(Player jogador) {
        FileConfiguration config = Main.getInstance().getConfig();
        Inventory inv = Bukkit.createInventory(null, 27, TITULO_MENU);

        Material matFundo = Material.valueOf(config.getString("itens.fundo.material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack itemFundo = criarItemMenu(matFundo, config.getString("itens.fundo.nome"), null);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, itemFundo);
        }

        inv.setItem(config.getInt("itens.criar-ilha.slot"), criarItemMenu(
                Material.valueOf(config.getString("itens.criar-ilha.material")),
                config.getString("itens.criar-ilha.nome"),
                config.getStringList("itens.criar-ilha.lore")
        ));

        inv.setItem(config.getInt("itens.setar-spawn.slot"), criarItemMenu(
                Material.valueOf(config.getString("itens.setar-spawn.material")),
                config.getString("itens.setar-spawn.nome"),
                config.getStringList("itens.setar-spawn.lore")
        ));

        List<String> loreConfigInfo = config.getStringList("itens.informacoes.lore");
        List<String> loreFinalInfo = new ArrayList<>();
        List<String> minhasIlhas = Main.getInstance().getIslandStorage().getNomesIlhas(jogador.getUniqueId());

        for (String ServerLinha : loreConfigInfo) {
            if (ServerLinha.contains("{lista_ilhas}")) {
                if (minhasIlhas.isEmpty()) {
                    loreFinalInfo.add(ChatColor.RED + "   Nenhuma base operacional encontrada.");
                } else {
                    for (String nomeIlha : minhasIlhas) {
                        loreFinalInfo.add(ChatColor.GRAY + "   ▪ " + ChatColor.YELLOW + nomeIlha);
                    }
                }
            } else if (ServerLinha.contains("{nivel_ilha}")) {
                loreFinalInfo.add(ServerLinha.replace("{nivel_ilha}", String.valueOf(Main.getInstance().getIslandStorage().getNivelIlha(jogador.getUniqueId(), minhasIlhas.isEmpty() ? "" : minhasIlhas.get(0)))));
            } else {
                loreFinalInfo.add(ServerLinha);
            }
        }

        inv.setItem(config.getInt("itens.informacoes.slot"), criarItemMenu(
                Material.valueOf(config.getString("itens.informacoes.material")),
                config.getString("itens.informacoes.nome"),
                loreFinalInfo
        ));

        inv.setItem(config.getInt("itens.teleportar.slot"), criarItemMenu(
                Material.valueOf(config.getString("itens.teleportar.material")),
                config.getString("itens.teleportar.nome"),
                config.getStringList("itens.teleportar.lore")
        ));

        jogador.openInventory(inv);
    }

    @EventHandler
    public void aoClicarNoMenu(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player jogador = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        FileConfiguration config = Main.getInstance().getConfig();

        if (slot == config.getInt("itens.criar-ilha.slot")) {
            jogador.closeInventory();
            processarCriacaoIlha(jogador, config.getString("itens.criar-ilha.schematic"));
        } else if (slot == config.getInt("itens.setar-spawn.slot")) {
            jogador.closeInventory();
            jogador.performCommand("is setspawn");
        } else if (slot == config.getInt("itens.informacoes.slot")) {
            jogador.performCommand("is info");
        } else if (slot == config.getInt("itens.teleportar.slot")) {
            jogador.closeInventory();
            jogador.performCommand("is go");
        }
    }

    @EventHandler
    public void aoCairNoVoid(PlayerMoveEvent event) {
        Player jogador = event.getPlayer();
        if (jogador.getLocation().getY() < 0) {
            if (jogador.isOp() && jogador.getGameMode().toString().equals("CREATIVE")) {
                return;
            }
            jogador.setHealth(0.0);
        }
    }

    private void configurarSpawnCustomizado(Player jogador) {
        List<String> ilhas = Main.getInstance().getIslandStorage().getNomesIlhas(jogador.getUniqueId());
        if (ilhas.isEmpty()) {
            jogador.sendMessage(ChatColor.RED + "❌ Você não possui nenhuma ilha para fixar o ponto de spawn!");
            return;
        }
        
        String nomeIlha = ilhas.get(0);
        Main.getInstance().getIslandStorage().atualizarSpawnIlha(jogador.getUniqueId(), nomeIlha, jogador.getLocation());
        jogador.sendMessage(ChatColor.GREEN + "🎯 Ponto de spawn atualizado com sucesso!");
    }

    private void processarCriacaoIlha(Player jogador, String nomeSchematic) {
        List<String> ilhasDoJogador = Main.getInstance().getIslandStorage().getNomesIlhas(jogador.getUniqueId());
        if (!ilhasDoJogador.isEmpty() && !jogador.isOp()) {
            jogador.sendMessage(ChatColor.RED + "Você já possui uma ilha tática registrada! Use /is go");
            return;
        }

        jogador.sendMessage(ChatColor.GREEN + "🔨 Mobilizando engenharia aérea para projetar sua ilha...");

        Location localNovo = Main.getInstance().getGridManager().calcularProximaCoordenada();
        File arquivoSchem = new File(Main.getInstance().getDataFolder() + "/schematics", nomeSchematic);
        
        if (!arquivoSchem.exists()) {
            jogador.sendMessage(ChatColor.YELLOW + "[AVISO] Arquivo '" + nomeSchematic + "' não encontrado em /schematics. Gerando bloco emergencial.");
            localNovo.getBlock().setType(Material.BEDROCK);
        } else {
            colarSchematicFAWE(arquivoSchem, localNovo);
        }

        int proximoIdGlobal = Main.getInstance().getIslandStorage().getProximoId();
        String nomeFinalIlha = "Ilha #" + proximoIdGlobal;
        String dataCriacao = dateFormat.format(new Date());

        // Força o spawn inicial com deslocamento preciso na camada 103 (3 blocos acima do centro Y:100 da colagem)
        Location locSpawnInicial = new Location(localNovo.getWorld(), localNovo.getX() + 0.5, 103.5, localNovo.getZ() + 0.5);
        Main.getInstance().getIslandStorage().criarNovaIlha(jogador.getUniqueId(), nomeFinalIlha, locSpawnInicial, dataCriacao);
        
        jogador.teleport(locSpawnInicial);
        jogador.sendMessage(ChatColor.AQUA + "✨ " + nomeFinalIlha + " estabelecida com sucesso!");
    }

    private void colarSchematicFAWE(File arquivo, Location loc) {
        try {
            com.sk89q.worldedit.world.World worldEditWorld = BukkitAdapter.adapt(loc.getWorld());
            ClipboardFormat format = ClipboardFormats.findByFile(arquivo);
            
            if (format == null) {
                format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
            }
            
            try (ClipboardReader reader = format.getReader(new FileInputStream(arquivo))) {
                Clipboard clipboard = reader.read();
                try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(worldEditWorld).build()) {
                    ClipboardHolder holder = new ClipboardHolder(clipboard);
                    Operation operation = holder.createPaste(editSession)
                            .to(BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
                            .ignoreAirBlocks(false)
                            .build();
                    Operations.complete(operation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exibirInfoIlha(Player jogador) {
        List<String> minhasIlhas = Main.getInstance().getIslandStorage().getNomesIlhas(jogador.getUniqueId());
        if (minhasIlhas.isEmpty()) {
            jogador.sendMessage(ChatColor.RED + "❌ Você não possui ilhas registradas.");
        } else {
            String nome = minhasIlhas.get(0);
            String data = Main.getInstance().getIslandStorage().getDataCriacao(jogador.getUniqueId(), nome);
            int nivel = Main.getInstance().getIslandStorage().getNivelIlha(jogador.getUniqueId(), nome);
            
            jogador.sendMessage(ChatColor.AQUA + "=== Detalhes da Base Operacional ===");
            jogador.sendMessage(ChatColor.DARK_GRAY + " ▪ Nome de Identificação: " + ChatColor.YELLOW + nome);
            jogador.sendMessage(ChatColor.DARK_GRAY + " ▪ Proprietário Core: " + ChatColor.GREEN + jogador.getName());
            jogador.sendMessage(ChatColor.DARK_GRAY + " ▪ Data de Ativação: " + ChatColor.WHITE + data);
            jogador.sendMessage(ChatColor.DARK_GRAY + " ▪ Nível Territorial: " + ChatColor.AQUA + nivel + " XP");
        }
    }

    private ItemStack criarItemMenu(Material material, String nome, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', nome));
            if (lore != null) {
                List<String> loreColorida = new ArrayList<>();
                for (String linea : lore) {
                    loreColorida.add(ChatColor.translateAlternateColorCodes('&', linea));
                }
                meta.setLore(loreColorida);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void removerEstruturaFisica(Location centro) {
        World mundo = centro.getWorld();
        if (mundo == null) return;

        int cX = centro.getBlockX();
        int cY = centro.getBlockY();
        int cZ = centro.getBlockZ();

        for (int x = -30; x <= 30; x++) {
            for (int y = -20; y <= 30; y++) {
                for (int z = -30; z <= 30; z++) {
                    mundo.getBlockAt(cX + x, cY + y, cZ + z).setType(Material.AIR, false);
                }
            }
        }
    }
}