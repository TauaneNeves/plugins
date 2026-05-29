package com.aeriaplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// ========================================================================
// CLASSE: ItemUtils
// OBJETIVO: Criar e formatar Itens do jogo com base no texto das configurações.
// ========================================================================
public class ItemUtils {

    // ------------------------------------------------------------------------
    // MÉTODO: parseItem
    // Lê o material, o nome, a descrição (lore) e devolve um ItemStack pronto a usar.
    // ------------------------------------------------------------------------
    @SuppressWarnings("deprecation")
    public static ItemStack parseItem(FileConfiguration config, String path, Player p, boolean usePapi) {
        String matPath = path + ".material";
        String nomePath = path + ".nome";
        String lorePath = path + ".lore";
        
        String matStr = config.getString(matPath);
        if (matStr == null) return null; // Se não houver material configurado, cancela
        
        ItemStack item;
        
        // Verificação especial: Cabeças customizadas por Base64 (Texturas da internet)
        if (matStr.startsWith("base64:")) {
            ItemStack base64Skull;
            try {
                base64Skull = new ItemStack(Material.valueOf("PLAYER_HEAD"));
            } catch (IllegalArgumentException e) {
                try {
                    base64Skull = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
                } catch (Exception ex) {
                    base64Skull = new ItemStack(Material.STONE);
                }
            }
            
            item = base64Skull;
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            
            if (meta != null) {
                // Tenta aplicar a textura da cabeça usando métodos avançados de "Reflexão"
                try {
                    Class<?> playerProfileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
                    Object profile = Bukkit.class.getMethod("createProfile", UUID.class).invoke(null, UUID.randomUUID());
                    Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
                    Class<?> profilePropertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
                    Object property = profilePropertyClass.getConstructor(String.class, String.class).newInstance("textures", matStr.substring(7));
                    properties.getClass().getMethod("add", Object.class).invoke(properties, property);
                    meta.getClass().getMethod("setPlayerProfile", playerProfileClass).invoke(meta, profile);
                } catch (Exception t) {
                    // Fallback: Se falhar, coloca a cabeça do jogador atual ou do Steve
                    meta.setOwner(p != null ? p.getName() : "Steve");
                }
                item.setItemMeta(meta);
            }
        } else {
            // Conversor de itens clássicos e modernos (Híbrido 1.8 a 1.21)
            Material materialEscolhido = null;
            String upperMat = matStr.toUpperCase().trim();
            short durabilidade = 0;

            // Tradutor inteligente: Painéis de Vidro Preto (Evita que se transformem em Pedra na 1.8)
            if (upperMat.equals("BLACK_STAINED_GLASS_PANE") || upperMat.equals("STAINED_GLASS_PANE")) {
                try {
                    materialEscolhido = Material.valueOf("BLACK_STAINED_GLASS_PANE");
                } catch (IllegalArgumentException e) {
                    try {
                        materialEscolhido = Material.valueOf("STAINED_GLASS_PANE");
                        durabilidade = 15; // ID do vidro preto na versão 1.8.8
                    } catch (IllegalArgumentException ex) {
                        materialEscolhido = Material.AIR;
                    }
                }
            }
            // Tradutor inteligente: Relógio
            else if (upperMat.equals("CLOCK") || upperMat.equals("WATCH")) {
                try {
                    materialEscolhido = Material.valueOf("CLOCK");
                } catch (IllegalArgumentException e) {
                    try {
                        materialEscolhido = Material.valueOf("WATCH");
                    } catch (IllegalArgumentException ex) {
                        materialEscolhido = Material.STONE;
                    }
                }
            }
            // Tradutor inteligente: Espadas Modernas (Netherite converte-se em Diamante na 1.8)
            else if (upperMat.equals("NETHERITE_SWORD")) {
                try {
                    materialEscolhido = Material.valueOf("NETHERITE_SWORD");
                } catch (IllegalArgumentException e) {
                    materialEscolhido = Material.DIAMOND_SWORD;
                }
            }
            else {
                // Tenta encontrar o material normal
                try {
                    materialEscolhido = Material.valueOf(upperMat);
                } catch (Exception e) {
                    // Correções de emergência para blocos comuns
                    if (upperMat.equals("PLAYER_HEAD") || upperMat.equals("PLAYER_WALL_HEAD")) {
                        try { materialEscolhido = Material.valueOf("SKULL_ITEM"); } catch(Exception ex) { materialEscolhido = Material.STONE; }
                    } else if (upperMat.equals("GRASS_BLOCK")) {
                        try { materialEscolhido = Material.valueOf("GRASS"); } catch(Exception ex) { materialEscolhido = Material.STONE; }
                    } else if (upperMat.equals("REDSTONE_TORCH") || upperMat.equals("REDSTONE_TORCH_OFF")) {
                        try { materialEscolhido = Material.valueOf("REDSTONE_TORCH_ON"); } catch(Exception ex) { materialEscolhido = Material.STONE; }
                    } else {
                        materialEscolhido = Material.STONE; // Se não encontrar, vira pedra para evitar que o servidor crash
                    }
                }
            }
            
            // Cria o item finalmente com o material encontrado
            if (materialEscolhido != null && materialEscolhido.name().equals("SKULL_ITEM")) {
                item = new ItemStack(materialEscolhido, 1, (short) 3);
            } else if (materialEscolhido != null) {
                item = new ItemStack(materialEscolhido, 1, durabilidade);
            } else {
                item = new ItemStack(Material.STONE);
            }
        }
        
        // Formata o Nome e a Descrição (Lore) do item
        ItemMeta m = item.getItemMeta();
        if (m == null) return item;
        
        if (config.contains(nomePath)) {
            m.setDisplayName(ChatUtils.color(p, config.getString(nomePath), usePapi));
        }
        
        List<String> lore = new ArrayList<>();
        if (config.contains(lorePath)) {
            for (String l : config.getStringList(lorePath)) {
                lore.add(ChatUtils.color(p, l, usePapi));
            }
        }
        m.setLore(lore);
        
        // Esconde os encantamentos do item (caso tenha)
        try {
            m.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf("HIDE_ENCHANTS"));
        } catch (Throwable ignored) {}
        
        item.setItemMeta(m);
        return item;
    }
}