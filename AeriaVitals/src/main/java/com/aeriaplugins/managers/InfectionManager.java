package com.aeriaplugins.managers;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import org.bukkit.entity.Player;

public class InfectionManager {

    private final AeriaVitals plugin;

    public InfectionManager(AeriaVitals plugin) {
        this.plugin = plugin;
    }

    public void handleZombieBite(Player player) {
        PlayerData data = plugin.getPlayerData(player);
        double baseChance = plugin.getConfig().getDouble("infection.zombie-bite-chance", 35.0);
        double finalChance = baseChance * (1.0 - (data.getImmunity() / 100.0));

        if (Math.random() * 100 < finalChance) {
            data.addInfection(15.0);
            player.sendMessage("§cVocê foi mordido! A infecção começou a se espalhar.");
        }
    }
}