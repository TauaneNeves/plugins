package com.aeriaplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
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
        String matStr = config.getString(path + ".material");
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
        m.setDisplayName(ChatUtils.color(p, config.getString(path + ".nome"), usePapi));
        
        if (config.contains(path + ".custom-model-data") && config.getInt(path + ".custom-model-data") > 0) {
            m.setCustomModelData(config.getInt(path + ".custom-model-data"));
        }

        List<String> lore = new ArrayList<>();
        if (config.contains(path + ".lore")) {
            for (String l : config.getStringList(path + ".lore")) {
                lore.add(ChatUtils.color(p, l, usePapi));
            }
        }
        m.setLore(lore);
        item.setItemMeta(m);
        
        return item;
    }
}