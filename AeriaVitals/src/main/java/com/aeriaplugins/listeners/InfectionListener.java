package com.aeriaplugins.listeners;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class InfectionListener implements Listener {

    private final AeriaVitals plugin;

    public InfectionListener(AeriaVitals plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Zombie) {
            Player player = (Player) event.getEntity();
            PlayerData data = plugin.getPlayerData(player);

            double baseChance = plugin.getConfig().getDouble("infection.zombie-bite-chance", 35.0);
            double protection = data.getImmunity() / 2.0; 
            double finalChance = Math.max(5.0, baseChance - protection);

            if (Math.random() * 100 < finalChance) {
                data.setInfection(Math.min(100.0, data.getInfection() + 15.0));
                player.sendMessage("§cVocê foi mordido por um zumbi e a infecção se espalhou!");
            }
        }
    }
}