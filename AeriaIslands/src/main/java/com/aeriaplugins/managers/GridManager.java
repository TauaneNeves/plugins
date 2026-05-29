package com.aeriaplugins.managers;

import com.aeriaplugins.plugins.Main;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class GridManager {

    // Distância radial limite considerada como área interna/protegida de uma ilha específica
    private final int RAIO_PROTECAO = 250; 

    public Location calcularProximaCoordenada() {
        int proximoId = Main.getInstance().getIslandStorage().getRawConfig().getInt("contador-global-id", 0) + 1;
        // Alinhamento linear na grade tática com distanciamento seguro de 1500 blocos
        double x = proximoId * 1500.0;
        double y = 100.0;
        double z = 0.0;
        return new Location(org.bukkit.Bukkit.getWorld("aeria_skyblock"), x, y, z);
    }

    public UUID getDonoDaIlhaNaLocalizacao(Location loc) {
        FileConfiguration config = Main.getInstance().getIslandStorage().getRawConfig();
        if (!config.contains("jogadores")) return null;

        for (String uuidStr : config.getConfigurationSection("jogadores").getKeys(false)) {
            for (String nomeIlha : config.getConfigurationSection("jogadores." + uuidStr).getKeys(false)) {
                String path = "jogadores." + uuidStr + "." + nomeIlha;
                double x = config.getDouble(path + ".x");
                double z = config.getDouble(path + ".z");

                if (Math.abs(loc.getX() - x) <= RAIO_PROTECAO && Math.abs(loc.getZ() - z) <= RAIO_PROTECAO) {
                    return UUID.fromString(uuidStr);
                }
            }
        }
        return null;
    }

    public String getNomeDaIlhaNaLocalizacao(Location loc) {
        FileConfiguration config = Main.getInstance().getIslandStorage().getRawConfig();
        if (!config.contains("jogadores")) return null;

        for (String uuidStr : config.getConfigurationSection("jogadores").getKeys(false)) {
            for (String nomeIlha : config.getConfigurationSection("jogadores." + uuidStr).getKeys(false)) {
                String path = "jogadores." + uuidStr + "." + nomeIlha;
                double x = config.getDouble(path + ".x");
                double z = config.getDouble(path + ".z");

                if (Math.abs(loc.getX() - x) <= RAIO_PROTECAO && Math.abs(loc.getZ() - z) <= RAIO_PROTECAO) {
                    return nomeIlha;
                }
            }
        }
        return null;
    }
}