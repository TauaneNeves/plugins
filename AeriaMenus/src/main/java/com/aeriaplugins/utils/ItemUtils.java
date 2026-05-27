package com.aeriaplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemUtils {

    public static ItemStack parseItem(FileConfiguration config, String path, Player p, boolean usePapi) {
        // Define as chaves de caminhos padrão
        String matPath = path + ".material";
        String nomePath = path + ".nome";
        String lorePath = path + ".lore";
        String cmdPath = path + ".custom-model-data";
        String encPath = path + ".encantamentos";
        String flagsPath = path + ".flags";

        // --- Sistema de Aparência Alternativa por Permissão ---
        if (config.contains(path + ".se-nao-tiver-permissao") && p != null) {
            String permissaoAparencia = config.getString(path + ".se-nao-tiver-permissao");
            if (!p.hasPermission(permissaoAparencia)) {
                // Se o jogador NÃO tiver a permissão requisitada, redireciona para as configurações alternativas
                if (config.contains(path + ".material-alternativo")) matPath = path + ".material-alternativo";
                if (config.contains(path + ".nome-alternativo")) nomePath = path + ".nome-alternativo";
                if (config.contains(path + ".lore-alternativa")) lorePath = path + ".lore-alternativa";
                if (config.contains(path + ".custom-model-data-alternativo")) cmdPath = path + ".custom-model-data-alternativo";
                if (config.contains(path + ".encantamentos-alternativos")) encPath = path + ".encantamentos-alternativos";
                if (config.contains(path + ".flags-alternativas")) flagsPath = path + ".flags-alternativas";
            }
        }

        String matStr = config.getString(matPath);
        if (matStr == null) return null;
        
        ItemStack item;
        
        if (matStr.startsWith("base64:")) {
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", matStr.substring(7)));
            meta.setPlayerProfile(profile);
            item.setItemMeta(meta);
        } else {
            try {
                item = new ItemStack(Material.valueOf(matStr.toUpperCase()));
            } catch (Exception e) {
                item = new ItemStack(Material.STONE);
            }
        }
        
        ItemMeta m = item.getItemMeta();
        if (m == null) return item;

        // --- PLACEHOLDERS DINÂMICOS NO NOME ---
        if (config.contains(nomePath)) {
            m.setDisplayName(ChatUtils.color(p, config.getString(nomePath), usePapi));
        }
        
        if (config.contains(cmdPath) && config.getInt(cmdPath) > 0) {
            m.setCustomModelData(config.getInt(cmdPath));
        }

        // --- PLACEHOLDERS DINÂMICOS NA LORE ---
        List<String> lore = new ArrayList<>();
        if (config.contains(lorePath)) {
            for (String l : config.getStringList(lorePath)) {
                lore.add(ChatUtils.color(p, l, usePapi));
            }
        }
        m.setLore(lore);

        // Suporte a Encantamentos
        if (config.contains(encPath)) {
            for (String enc : config.getStringList(encPath)) {
                String[] split = enc.split(":");
                if (split.length == 2) {
                    try {
                        String enchantName = split[0].toLowerCase();
                        int level = Integer.parseInt(split[1]);
                        
                        Enchantment enchantment = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantName));
                        if (enchantment != null) {
                            m.addEnchant(enchantment, level, true);
                        } else {
                            Enchantment legacy = Enchantment.getByName(enchantName.toUpperCase());
                            if (legacy != null) {
                                m.addEnchant(legacy, level, true);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // Suporte a ItemFlags (Bandeiras)
        if (config.contains(flagsPath)) {
            for (String flagStr : config.getStringList(flagsPath)) {
                try {
                    m.addItemFlags(ItemFlag.valueOf(flagStr.toUpperCase()));
                } catch (Exception ignored) {}
            }
        }

        item.setItemMeta(m);
        return item;
    }
}