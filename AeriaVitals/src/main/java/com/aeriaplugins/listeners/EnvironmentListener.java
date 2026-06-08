package com.aeriaplugins.listeners;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.managers.InfectionManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EnvironmentListener implements Listener {

    private final InfectionManager infectionManager;

    public EnvironmentListener(AeriaVitals plugin) {
        this.infectionManager = new InfectionManager(plugin);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Zombie) {
            Player player = (Player) event.getEntity();
            infectionManager.handleZombieBite(player);
        }
    }
}