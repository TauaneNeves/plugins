package com.aeriaplugins.hordes.listeners;

import com.aeriaplugins.hordes.AeriaHordes;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiegeListener implements Listener {

    private final AeriaHordes plugin;
    private final Map<Block, Integer> blockHitsMap = new HashMap<>();

    public SiegeListener(AeriaHordes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onZombieStrikeBlock(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("siege.allow-block-destruction", true)) return;

        if (event.getDamager() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getDamager();
            
            if (!plugin.getHordeManager().isHordeZombie(zombie)) return;

            Block targetBlock = zombie.getTargetBlockExact(2);
            if (targetBlock == null || targetBlock.getType() == Material.AIR) return;

            String typeName = targetBlock.getType().name();
            List<String> breakables = plugin.getConfig().getStringList("siege.breakable-blocks");

            if (breakables.contains(typeName)) {
                int currentHits = blockHitsMap.getOrDefault(targetBlock, 0) + 1;
                int requiredHits = plugin.getConfig().getInt("siege.hits-required-to-break", 10);

                if (targetBlock.getType().name().contains("WOOD") || targetBlock.getType().name().contains("DOOR")) {
                    targetBlock.getWorld().playSound(targetBlock.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 1.0f);
                } else {
                    targetBlock.getWorld().playSound(targetBlock.getLocation(), Sound.BLOCK_ANVIL_HIT, 0.5f, 0.5f);
                }

                if (currentHits >= requiredHits) {
                    blockHitsMap.remove(targetBlock);
                    targetBlock.getWorld().playEffect(targetBlock.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                    targetBlock.setType(Material.AIR);
                    targetBlock.getWorld().playSound(targetBlock.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.0f);
                } else {
                    blockHitsMap.put(targetBlock, currentHits);
                }
            }
        }
    }
}