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

public class ItemUtils {

    public static ItemStack parseItem(FileConfiguration config, String path, Player p, boolean usePapi) {
        String matPath = path + ".material";
        String nomePath = path + ".nome";
        String lorePath = path + ".lore";
        
        String matStr = config.getString(matPath);
        if (matStr == null) return null;
        
        ItemStack item;
        
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
                try {
                    Class<?> playerProfileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
                    Object profile = Bukkit.class.getMethod("createProfile", UUID.class).invoke(null, UUID.randomUUID());
                    Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
                    Class<?> profilePropertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
                    Object property = profilePropertyClass.getConstructor(String.class, String.class).newInstance("textures", matStr.substring(7));
                    properties.getClass().getMethod("add", Object.class).invoke(properties, property);
                    meta.getClass().getMethod("setPlayerProfile", playerProfileClass).invoke(meta, profile);
                } catch (Exception t) {
                    meta.setOwner(p != null ? p.getName() : "Steve");
                }
                item.setItemMeta(meta);
            }
        } else {
            Material materialEscolhido = null;
            try {
                materialEscolhido = Material.valueOf(matStr.toUpperCase());
            } catch (Exception e) {
                if (matStr.equalsIgnoreCase("PLAYER_HEAD") || matStr.equalsIgnoreCase("PLAYER_WALL_HEAD")) {
                    try { materialEscolhido = Material.valueOf("SKULL_ITEM"); } catch(Exception ex) { materialEscolhido = Material.STONE; }
                } else if (matStr.equalsIgnoreCase("GRASS_BLOCK")) {
                    try { materialEscolhido = Material.valueOf("GRASS"); } catch(Exception ex) { materialEscolhido = Material.STONE; }
                } else if (matStr.equalsIgnoreCase("CLOCK") || matStr.equalsIgnoreCase("WATCH")) {
                    try { materialEscolhido = Material.valueOf("WATCH"); } catch(Exception ex) { try { materialEscolhido = Material.valueOf("CLOCK"); } catch(Exception exc) { materialEscolhido = Material.STONE; } }
                } else if (matStr.equalsIgnoreCase("REDSTONE_TORCH") || matStr.equalsIgnoreCase("REDSTONE_TORCH_OFF")) {
                    try { materialEscolhido = Material.valueOf("REDSTONE_TORCH_ON"); } catch(Exception ex) { materialEscolhido = Material.STONE; }
                } else {
                    materialEscolhido = Material.STONE;
                }
            }
            
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
        
        List<String> lore = new ArrayList<>();
        if (config.contains(lorePath)) {
            for (String l : config.getStringList(lorePath)) {
                lore.add(ChatUtils.color(p, l, usePapi));
            }
        }
        m.setLore(lore);
        
        try {
            m.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf("HIDE_ENCHANTS"));
        } catch (Throwable ignored) {}
        
        item.setItemMeta(m);
        return item;
    }
}