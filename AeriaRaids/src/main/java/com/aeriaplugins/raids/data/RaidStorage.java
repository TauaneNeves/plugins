package com.aeriaplugins.raids.data;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class RaidStorage {

    private final File arquivo;
    private final FileConfiguration config;

    public RaidStorage(JavaPlugin plugin) {
        this.arquivo = new File(plugin.getDataFolder(), "locks.yml");
        if (!arquivo.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                arquivo.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(arquivo);
    }

    private String formatarLocal(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    public void setarSenha(Location loc, String senha) {
        config.set("cadeados." + formatarLocal(loc), senha);
        salvar();
    }

    public String getSenha(Location loc) {
        return config.getString("cadeados." + formatarLocal(loc), null);
    }

    public boolean isTrancado(Location loc) {
        return getSenha(loc) != null;
    }

    public void quebrarCadeado(Location loc) {
        config.set("cadeados." + formatarLocal(loc), null);
        salvar();
    }

    private void salvar() {
        try {
            config.save(arquivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}