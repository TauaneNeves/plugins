package com.aeriaplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemUtils {

    public static ItemStack parseItem(FileConfiguration config, String path, Player p, boolean usePapi) {
        String matPath = path + ".material";
        String nomePath = path + ".nome";
        String lorePath = path + ".lore";
        String cmdPath = path + ".custom-model-data";
        
        String matStr = config.getString(matPath);
        if (matStr == null) return null;
        
        ItemStack item;
        
        if (matStr.startsWith("base64:")) {
            // Suporte universal a cabecas customizadas texturizadas
            ItemStack base64Skull;
            try {
                base64Skull = new ItemStack(Material.valueOf("PLAYER_HEAD"));
            } catch (IllegalArgumentException e) {
                // Fallback caso o material PLAYER_HEAD nao exista (Minecraft 1.8.8)
                try {
                    base64Skull = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
                } catch (Exception ex) {
                    base64Skull = new ItemStack(Material.STONE);
                }
            }
            
            item = base64Skull;
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            
            if (meta != null) {
                try {
                    Class<?> playerProfileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
                    Object profile = Bukkit.class.getMethod("createProfile", UUID.class).invoke(null, UUID.randomUUID());
                    Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
                    Class<?> profilePropertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
                    Object property = profilePropertyClass.getConstructor(String.class, String.class).newInstance("textures", matStr.substring(7));
                    properties.getClass().getMethod("add", Object.class).invoke(properties, property);
                    meta.getClass().getMethod("setPlayerProfile", playerProfileClass).invoke(meta, profile);
                } catch (Exception t) {
                    // Fallback estavel de assinatura para cabecas no Spigot antigo 1.8.8
                    meta.setOwner(p != null ? p.getName() : "Steve");
                }
                item.setItemMeta(meta);
            }
        } else {
            // Conversao de materiais e nomes antigos da 1.8.8 ate as versoes mais novas 26.1.2
            Material materialEscolhido = null;
            try {
                materialEscolhido = Material.valueOf(matStr.toUpperCase());
            } catch (Exception e) {
                // Dicionario inteligente de Fallbacks de materiais da 26.1.2 para a 1.8.8
                if (matStr.equalsIgnoreCase("PLAYER_HEAD") || matStr.equalsIgnoreCase("PLAYER_WALL_HEAD")) {
                    try { materialEscolhido = Material.valueOf("SKULL_ITEM"); } catch(Exception ex) { materialEscolhido = Material.STONE; }
                } else if (matStr.equalsIgnoreCase("GRASS_BLOCK")) {
                    try { materialEscolhido = Material.valueOf("GRASS"); } catch(Exception ex) { materialEscolhido = Material.STONE; }
                } else if (matStr.equalsIgnoreCase("CLOCK") || matStr.equalsIgnoreCase("WATCH")) {
                    try { materialEscolhido = Material.valueOf("WATCH"); } catch(Exception ex) { materialEscolhido = Material.CLOCK; }
                } else if (matStr.equalsIgnoreCase("REDSTONE_TORCH") || matStr.equalsIgnoreCase("REDSTONE_TORCH_OFF")) {
                    try { materialEscolhido = Material.valueOf("REDSTONE_TORCH_ON"); } catch(Exception ex) { materialEscolhido = Material.STONE; }
                } else {
                    materialEscolhido = Material.STONE;
                }
            }
            
            // Tratamento especifico de Data/Durabilidade para cabecas na versao antiga 1.8.8
            if (materialEscolhido != null && materialEscolhido.name().equals("SKULL_ITEM")) {
                item = new ItemStack(materialEscolhido, 1, (short) 3);
            } else if (materialEscolhido != null) {
                item = new ItemStack(materialEscolhido);
            } else {
                item = new ItemStack(Material.STONE);
            }
        }
        
        ItemMeta m = item.getItemMeta();
        if (m == null) return item;
        
        if (config.contains(nomePath)) {
            m.setDisplayName(ChatUtils.color(p, config.getString(nomePath), usePapi));
        }
        
        // CustomModelData (Ignorado silenciosamente na 1.8.8 e lido nativamente na 26.1.2)
        if (config.contains(cmdPath) && config.getInt(cmdPath) > 0) {
            try {
                m.setCustomModelData(config.getInt(cmdPath));
            } catch (Throwable ignored) {}
        }
        
        List<String> lore = new ArrayList<>();
        if (config.contains(lorePath)) {
            for (String l : config.getStringList(lorePath)) {
                lore.add(ChatUtils.color(p, l, usePapi));
            }
        }
        m.setLore(lore);
        
        // Remove visibilidade de encantamentos de forma segura nas flags
        try {
            m.addItemFlags(ItemFlag.valueOf("HIDE_ENCHANTS"));
        } catch (Throwable ignored) {}
        
        item.setItemMeta(m);
        return item;
    }
}