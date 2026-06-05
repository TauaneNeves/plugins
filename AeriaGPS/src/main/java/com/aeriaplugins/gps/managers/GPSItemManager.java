package com.aeriaplugins.gps.managers;

import com.aeriaplugins.gps.AeriaGPS;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GPSItemManager {

    private final AeriaGPS plugin;
    public final NamespacedKey gpsKey;
    public final NamespacedKey batteryKey;
    public final NamespacedKey paperKey;
    public final NamespacedKey unlockedKey;

    public GPSItemManager(AeriaGPS plugin) {
        this.plugin = plugin;
        this.gpsKey = new NamespacedKey(plugin, "is_aeria_gps");
        this.batteryKey = new NamespacedKey(plugin, "gps_battery");
        this.paperKey = new NamespacedKey(plugin, "gps_paper");
        this.unlockedKey = new NamespacedKey(plugin, "unlocked_islands");
        
        if (!plugin.getConfig().getBoolean("configuracoes_gerais.usar_sistema_blueprints")) {
            registerGPSRecipe();
        }
    }

    public ItemStack createGPS() {
        String materialName = plugin.getConfig().getString("item_gps.material", "DARK_OAK_HANGING_SIGN");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.DARK_OAK_HANGING_SIGN;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String nome = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("item_gps.nome", "&6AeriaGPS"));
            meta.setDisplayName(nome);

            List<String> loreConfig = plugin.getConfig().getStringList("item_gps.lore");
            List<String> lore = new ArrayList<>();
            int maxBattery = plugin.getConfig().getInt("item_gps.bateria_maxima", 100);

            for (String line : loreConfig) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line.replace("{bateria}", String.valueOf(maxBattery))));
            }
            meta.setLore(lore);

            int cmd = plugin.getConfig().getInt("item_gps.custom_model_data", 0);
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }

            meta.getPersistentDataContainer().set(gpsKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(batteryKey, PersistentDataType.INTEGER, maxBattery);

            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createBlueprint(String islandName) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Coordenadas: " + islandName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Clique direito para registrar", 
                    ChatColor.GRAY + "este local no seu satélite."
            ));
            meta.getPersistentDataContainer().set(paperKey, PersistentDataType.STRING, islandName);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void registerGPSRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "aeriagps_craft");
        
        Bukkit.removeRecipe(recipeKey);

        ItemStack result = createGPS();
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        String linha1 = plugin.getConfig().getString("receita.linha_1", " I ");
        String linha2 = plugin.getConfig().getString("receita.linha_2", "RSR");
        String linha3 = plugin.getConfig().getString("receita.linha_3", " G ");

        recipe.shape(linha1, linha2, linha3);

        for (String key : plugin.getConfig().getConfigurationSection("receita.ingredientes").getKeys(false)) {
            String matName = plugin.getConfig().getString("receita.ingredientes." + key);
            Material mat = Material.matchMaterial(matName);
            if (mat != null) {
                recipe.setIngredient(key.charAt(0), mat);
            }
        }

        Bukkit.addRecipe(recipe);
    }
}