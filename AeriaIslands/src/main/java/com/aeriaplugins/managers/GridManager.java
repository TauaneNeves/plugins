package com.aeriaplugins.managers;

import com.aeriaplugins.plugins.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class GridManager {

    private final String NOME_MUNDO_VOID = "skyblockv";
    private final int ESPACAMENTO_ILHAS = 1000; 
    private final int ALTURA_FIXA = 100;        

    public Location calcularProximaCoordenada() {
        World mundo = Bukkit.getWorld(NOME_MUNDO_VOID);
        
        if (mundo == null) {
            mundo = Bukkit.createWorld(new org.bukkit.WorldCreator(NOME_MUNDO_VOID));
        }
        
        if (mundo == null && !Bukkit.getWorlds().isEmpty()) {
            mundo = Bukkit.getWorlds().get(0);
        }

        int totalIlhasAtivas = Main.getInstance().getIslandStorage().getRawConfig().getInt("contador-global-id", 0);
        
        int proximoZ = totalIlhasAtivas * ESPACAMENTO_ILHAS;
        
        return new Location(mundo, 0, ALTURA_FIXA, proximoZ);
    }
    
    public UUID getDonoDaIlhaNaLocalizacao(Location loc) {
        FileConfiguration config = Main.getInstance().getIslandStorage().getRawConfig();
        if (!config.contains("jogadores")) return null;

        for (String uuidStr : config.getConfigurationSection("jogadores").getKeys(false)) {
            for (String ilhaNome : config.getConfigurationSection("jogadores." + uuidStr).getKeys(false)) {
                String path = "jogadores." + uuidStr + "." + ilhaNome;
                if (!config.getString(path + ".world", "").equals(loc.getWorld().getName())) continue;

                double x = config.getDouble(path + ".x");
                double z = config.getDouble(path + ".z");
                
                if (Math.abs(loc.getX() - x) <= (ESPACAMENTO_ILHAS / 2.0) &&
                    Math.abs(loc.getZ() - z) <= (ESPACAMENTO_ILHAS / 2.0)) {
                    return UUID.fromString(uuidStr);
                }
            }
        }
        return null;
    }

    public String getNomeDaIlhaNaLocalizacao(Location loc) {
        FileConfiguration config = Main.getInstance().getIslandStorage().getRawConfig();
        if (!config.contains("jogadores")) return "";

        for (String uuidStr : config.getConfigurationSection("jogadores").getKeys(false)) {
            for (String ilhaNome : config.getConfigurationSection("jogadores." + uuidStr).getKeys(false)) {
                String path = "jogadores." + uuidStr + "." + ilhaNome;
                if (!config.getString(path + ".world", "").equals(loc.getWorld().getName())) continue;

                double x = config.getDouble(path + ".x");
                double z = config.getDouble(path + ".z");
                
                if (Math.abs(loc.getX() - x) <= (ESPACAMENTO_ILHAS / 2.0) &&
                    Math.abs(loc.getZ() - z) <= (ESPACAMENTO_ILHAS / 2.0)) {
                    return ilhaNome;
                }
            }
        }
        return "";
    }
}