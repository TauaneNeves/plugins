package com.aeriaplugins.hordes.listeners;

import com.aeriaplugins.hordes.AeriaHordes;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityTargetListener implements Listener {

    private final AeriaHordes plugin;

    public EntityTargetListener(AeriaHordes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onZombieSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getEntity();

            if (plugin.getHordeManager().isHordeZombie(zombie)) {
                AttributeInstance followRange = zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
                if (followRange != null) {
                    followRange.setBaseValue(100.0);
                }
            }
        }
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie) {
            plugin.getHordeManager().getHordeZombies().remove(event.getEntity().getUniqueId());
        }
    }
}