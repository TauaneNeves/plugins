package com.aeriaplugins.managers;

import com.aeriaplugins.plugins.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileManager {

    private final Main plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private final Map<String, FileConfiguration> menus;

    public FileManager(Main plugin) {
        this.plugin = plugin;
        this.menus = new HashMap<>();
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        
        loadMessages();
        loadMenus();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = new YamlConfiguration();
        try {
            messagesConfig.load(messagesFile);
        } catch (Exception e) {
            plugin.getLogger().severe("==================================================");
            plugin.getLogger().severe("ERRO DE SINTAXE ENCONTRADO NO: messages.yml");
            plugin.getLogger().severe("Detalhes do Erro: " + e.getMessage());
            plugin.getLogger().severe("Corrija as aspas ou espaços e digite /am reload");
            plugin.getLogger().severe("==================================================");
        }
    }

    private void loadMenus() {
        menus.clear();
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
            plugin.saveResource("menus/principal.yml", false);
        }

        File[] files = menusFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String menuName = file.getName().replace(".yml", "");
                YamlConfiguration menuConfig = new YamlConfiguration();
                try {
                    menuConfig.load(file);
                    menus.put(menuName, menuConfig);
                } catch (Exception e) {
                    plugin.getLogger().severe("==================================================");
                    plugin.getLogger().severe("ERRO DE SINTAXE ENCONTRADO NO MENU: " + file.getName());
                    plugin.getLogger().severe("Detalhes do Erro: " + e.getMessage());
                    plugin.getLogger().severe("Corrija as aspas ou espaços e digite /am reload");
                    plugin.getLogger().severe("==================================================");
                }
            }
        }
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    public Map<String, FileConfiguration> getMenus() {
        return menus;
    }

    public FileConfiguration getMenu(String name) {
        return menus.get(name);
    }
}