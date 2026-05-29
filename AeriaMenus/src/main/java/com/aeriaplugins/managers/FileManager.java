package com.aeriaplugins.managers;

import com.aeriaplugins.plugins.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

// ========================================================================
// CLASSE: FileManager
// OBJETIVO: Gerir todos os ficheiros .yml (ler, criar e detetar erros).
// ========================================================================
public class FileManager {

    private final Main plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    
    // Um "dicionário" que guarda o nome do menu e o seu respetivo ficheiro de configuração
    private final Map<String, FileConfiguration> menus;

    public FileManager(Main plugin) {
        this.plugin = plugin;
        this.menus = new HashMap<>();
    }

    // ------------------------------------------------------------------------
    // MÉTODO: loadAll
    // Chama as funções para carregar tudo de uma vez. Usado quando o servidor liga.
    // ------------------------------------------------------------------------
    public void loadAll() {
        plugin.saveDefaultConfig(); // Salva o config.yml principal
        plugin.reloadConfig();
        
        loadMessages();
        loadMenus();
    }

    // ------------------------------------------------------------------------
    // MÉTODO: loadMessages
    // Cria ou lê o ficheiro messages.yml (onde ficam as mensagens de chat).
    // ------------------------------------------------------------------------
    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            // Se o ficheiro não existir na pasta, copia o padrão que está dentro do plugin
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = new YamlConfiguration();
        try {
            messagesConfig.load(messagesFile);
        } catch (Exception e) {
            // Se houver algum erro (como aspas esquecidas), avisa na consola em vermelho
            plugin.getLogger().severe("==================================================");
            plugin.getLogger().severe("ERRO DE SINTAXE ENCONTRADO NO: messages.yml");
            plugin.getLogger().severe("Detalhes do Erro: " + e.getMessage());
            plugin.getLogger().severe("Corrija as aspas ou espaços e digite /am reload");
            plugin.getLogger().severe("==================================================");
        }
    }

    // ------------------------------------------------------------------------
    // MÉTODO: loadMenus
    // Vasculha a pasta "menus" e carrega todos os ficheiros .yml que lá estiverem.
    // ------------------------------------------------------------------------
    private void loadMenus() {
        menus.clear(); // Limpa a memória antiga antes de recarregar
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs(); // Cria a pasta se não existir
            plugin.saveResource("menus/principal.yml", false); // Cria um menu de exemplo
        }

        // Procura todos os ficheiros que terminam em .yml dentro da pasta "menus"
        File[] files = menusFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String menuName = file.getName().replace(".yml", ""); // Remove o .yml do nome
                YamlConfiguration menuConfig = new YamlConfiguration();
                try {
                    menuConfig.load(file);
                    menus.put(menuName, menuConfig); // Guarda na memória
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

    // Métodos para outros locais do código poderem aceder aos ficheiros lidos
    public FileConfiguration getMessages() { return messagesConfig; }
    public Map<String, FileConfiguration> getMenus() { return menus; }
    public FileConfiguration getMenu(String name) { return menus.get(name); }
}