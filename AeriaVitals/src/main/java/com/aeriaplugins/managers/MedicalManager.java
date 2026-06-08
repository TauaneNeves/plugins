package com.aeriaplugins.managers;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class MedicalManager {

    private final AeriaVitals plugin;

        public MedicalManager(AeriaVitals plugin) {
        this.plugin = plugin;
    }

    public boolean useMedicalItem(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return false;

        int cmd = item.getItemMeta().getCustomModelData();
        PlayerData data = plugin.getPlayerData(player);

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom-items");
        if (section == null) return false;

        for (String key : section.getKeys(false)) {
            int configCmd = section.getInt(key + ".custom-model-data");
            if (cmd == configCmd) {
                
                if (key.equals("dirty_bandage")) {
                    if (!data.isBleeding()) {
                        player.sendMessage("§cVocê não está sangrando para usar isto.");
                        return false;
                    }
                    data.setBleeding(false);
                    player.sendMessage("§6Você aplicou a bandagem suja. O sangramento parou, mas o risco de infecção foi alto.");
                    if (Math.random() < 0.50) {
                        data.addInfection(15.0);
                        player.sendMessage("§cSeus ferimentos infeccionaram devido ao estado da bandagem!");
                    }
                } 
                else if (key.equals("sterile_bandage")) {
                    if (!data.isBleeding()) {
                        player.sendMessage("§cVocê não está sangrando para usar isto.");
                        return false;
                    }
                    data.setBleeding(false);
                    player.sendMessage("§aVocê aplicou uma bandagem estéril. O sangramento parou de forma segura.");
                } 
                else if (key.equals("morphine")) {
                    data.setMorphineTicks(900);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 900, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 900, 0));
                    player.sendMessage("§dVocê injetou morfina. A dor sumiu temporariamente, mas o colateral virá.");
                } 
                else if (key.equals("antibiotics")) {
                    data.setInfection(data.getInfection() - 20.0);
                    player.sendMessage("§aVocê tomou antibióticos. A infecção biológica foi contida.");
                } 
                else {
                    double reduction = section.getDouble(key + ".infection-reduction", 0.0);
                    double immunityBoost = section.getDouble(key + ".immunity-boost", 0.0);
                    
                    if (reduction > 0) data.setInfection(data.getInfection() - reduction);
                    if (immunityBoost > 0) data.setImmunity(data.getImmunity() + immunityBoost);
                    
                    String message = section.getString(key + ".use-message", "§aVocê utilizou um item médico.");
                    player.sendMessage(message.replace("&", "§"));
                }
                
                return true;
            }
        }
        return false;
    }

    public ItemStack createMedicalItem(String key, int amount) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom-items." + key);
        if (section == null) {
            ConfigurationSection bpSection = plugin.getConfig().getConfigurationSection("weight.backpacks." + key);
            if (bpSection == null) return null;
            
            ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE, amount);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String name = bpSection.getString("display-name", "&6Mochila");
                meta.setDisplayName(name.replace("&", "§"));
                item.setItemMeta(meta);
            }
            return item;
        }

        String matStr = section.getString("material", "SUGAR");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.SUGAR;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("display-name", "&cItem");
            meta.setDisplayName(name.replace("&", "§"));
            meta.setCustomModelData(section.getInt("custom-model-data", 1000));
            
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(line.replace("&", "§"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}