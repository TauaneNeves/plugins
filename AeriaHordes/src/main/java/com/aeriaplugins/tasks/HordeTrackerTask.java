package com.aeriaplugins.hordes.tasks;

import com.aeriaplugins.hordes.AeriaHordes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class HordeTrackerTask extends BukkitRunnable {

    private final AeriaHordes plugin;

    public HordeTrackerTask(AeriaHordes plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getWaveManager().isHordeNightActive()) {
            int globalMax = plugin.getConfig().getInt("spawning.global-max-zombies", 40);
            int totalActiveHorde = plugin.getHordeManager().getHordeZombies().size();

            if (totalActiveHorde < globalMax) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!plugin.getConfig().getStringList("allowed-worlds").contains(player.getWorld().getName())) {
                        continue;
                    }

                    int activeNear = 0;
                    for (UUID uuid : plugin.getHordeManager().getHordeZombies()) {
                        org.bukkit.entity.Entity e = Bukkit.getEntity(uuid);
                        if (e != null && e.getWorld().equals(player.getWorld()) && e.getLocation().distance(player.getLocation()) <= 50) {
                            activeNear++;
                        }
                    }

                    int maxPerPlayer = plugin.getConfig().getInt("spawning.max-zombies-per-player", 10);
                    if (activeNear < maxPerPlayer && plugin.getHordeManager().getHordeZombies().size() < globalMax) {
                        plugin.getHordeManager().spawnMiniHorde(player.getLocation());
                    }
                }
            }
        }

        org.bukkit.configuration.ConfigurationSection regionsSection = plugin.getConfig().getConfigurationSection("infested-regions");
        if (regionsSection != null) {
            int globalMax = plugin.getConfig().getInt("spawning.global-max-zombies", 40);
            for (String key : regionsSection.getKeys(false)) {
                String worldName = regionsSection.getString(key + ".world");
                double x = regionsSection.getDouble(key + ".x");
                double y = regionsSection.getDouble(key + ".y");
                double z = regionsSection.getDouble(key + ".z");
                double radius = regionsSection.getDouble(key + ".radius");
                int maxZombies = regionsSection.getInt(key + ".max-zombies");
                int groupSize = regionsSection.getInt(key + ".group-size");
                double health = regionsSection.getDouble(key + ".zombie-health");
                String name = regionsSection.getString(key + ".zombie-name", "&cZumbi");

                org.bukkit.World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                org.bukkit.Location regionCenter = new org.bukkit.Location(world, x, y, z);
                
                int currentInRegion = 0;
                for (UUID uuid : plugin.getHordeManager().getHordeZombies()) {
                    org.bukkit.entity.Entity e = Bukkit.getEntity(uuid);
                    if (e != null && e.getWorld().equals(world) && e.getLocation().distance(regionCenter) <= radius) {
                        currentInRegion++;
                    }
                }

                int remaining = maxZombies - currentInRegion;
                if (remaining > 0 && plugin.getHordeManager().getHordeZombies().size() < globalMax) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(world) && player.getLocation().distance(regionCenter) <= radius) {
                            int spawnCount = Math.min(groupSize, remaining);
                            plugin.getHordeManager().spawnMiniHorde(player.getLocation(), health, spawnCount, name);
                            break;
                        }
                    }
                }
            }
        }

        for (UUID uuid : plugin.getHordeManager().getHordeZombies()) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof Zombie && !entity.isDead()) {
                Zombie zombie = (Zombie) entity;

                if (zombie.getTarget() != null) {
                    org.bukkit.block.Block targetBlock = zombie.getTargetBlockExact(2);
                    if (targetBlock != null && targetBlock.getType() != org.bukkit.Material.AIR) {
                        plugin.getHordeManager().handleBlockSiege(zombie, targetBlock);
                    }
                }

                if (zombie.getTarget() == null) {
                    Player closest = null;
                    double closestDist = Double.MAX_VALUE;

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(zombie.getWorld())) {
                            double dist = player.getLocation().distance(zombie.getLocation());
                            if (dist < closestDist && dist <= 100.0) {
                                closestDist = dist;
                                closest = player;
                            }
                        }
                    }

                    if (closest != null) {
                        zombie.setTarget(closest);
                    }
                }
            }
        }
    }
}