package com.aeriaplugins.data;

import com.aeriaplugins.plugins.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class IslandStorage {

    private final File arquivo;
    private final FileConfiguration config;

    public IslandStorage() {
        this.arquivo = new File(Main.getInstance().getDataFolder(), "islands.yml");
        if (!arquivo.exists()) {
            try {
                Main.getInstance().getDataFolder().mkdirs();
                arquivo.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(arquivo);
    }

    public int getProximoId() {
        return config.getInt("contador-global-id", 0) + 1;
    }

    public void salvarIlha(UUID uuid, Location loc) {
        int novoId = getProximoId();
        config.set("contador-global-id", novoId);
        
        String path = "ilhas." + uuid.toString();
        config.set(path + ".id", novoId);
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        
        salvar();
    }

    public Location getLocalizacaoIlha(UUID uuid) {
        String path = "ilhas." + uuid.toString();
        if (!config.contains(path)) return null;

        String nomeMundo = config.getString(path + ".world");
        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");

        return new Location(Bukkit.getWorld(nomeMundo), x, y, z);
    }

    private void salvar() {
        try {
            config.save(arquivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}