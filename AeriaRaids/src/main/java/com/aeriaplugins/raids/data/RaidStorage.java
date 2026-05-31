package com.aeriaplugins.raids.data;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

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

    public void setarSenha(Location loc, String senha, UUID dono) {
        String path = "cadeados." + formatarLocal(loc);
        config.set(path + ".senha", senha);
        config.set(path + ".dono", dono.toString());
        if (!config.contains(path + ".permissao_clan")) {
            config.set(path + ".permissao_clan", "NENHUMA");
        }
        salvar();
    }

    public void atualizarSenha(Location loc, String senha) {
        config.set("cadeados." + formatarLocal(loc) + ".senha", senha);
        salvar();
    }

    public void setPermissaoClan(Location loc, String rank) {
        config.set("cadeados." + formatarLocal(loc) + ".permissao_clan", rank);
        salvar();
    }

    public String getPermissaoClan(Location loc) {
        return config.getString("cadeados." + formatarLocal(loc) + ".permissao_clan", "NENHUMA");
    }

    public String getSenha(Location loc) {
        return config.getString("cadeados." + formatarLocal(loc) + ".senha", null);
    }

    public UUID getDono(Location loc) {
        String uuidStr = config.getString("cadeados." + formatarLocal(loc) + ".dono", null);
        return uuidStr != null ? UUID.fromString(uuidStr) : null;
    }

    public boolean isTrancado(Location loc) {
        return config.contains("cadeados." + formatarLocal(loc));
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