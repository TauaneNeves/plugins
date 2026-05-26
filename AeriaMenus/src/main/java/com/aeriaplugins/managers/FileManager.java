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
        // Carrega ou cria a config.yml padrão
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
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void loadMenus() {
        menus.clear();
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
            // Salva um menu de exemplo padrão na primeira vez
            plugin.saveResource("menus/principal.yml", false);
        }

        File[] files = menusFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String menuName = file.getName().replace(".yml", "");
                menus.put(menuName, YamlConfiguration.loadConfiguration(file));
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