package com.aeriaplugins.hordes.managers;

import com.aeriaplugins.hordes.AeriaHordes;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class HordeManager {

    private final AeriaHordes plugin;
    private final Set<UUID> hordeZombies = new HashSet<>();
    private final Map<UUID, String> zombieCustomNames = new HashMap<>();
    private final Map<org.bukkit.block.Block, Integer> blockHitsMap = new HashMap<>();
    private final Random random = new Random();

    public HordeManager(AeriaHordes plugin) {
        this.plugin = plugin;
    }

    public boolean isHordeZombie(Zombie zombie) {
        return hordeZombies.contains(zombie.getUniqueId());
    }

    public void registerHordeZombie(Zombie zombie) {
        double defaultHealth = plugin.getConfig().getDouble("horde-night.zombie-health", 80.0);
        String defaultName = plugin.getConfig().getString("horde-night.zombie-name", "&cZumbi da Horda");
        registerHordeZombie(zombie, defaultHealth, defaultName);
    }

    public void registerHordeZombie(Zombie zombie, double customHealth, String customName) {
        hordeZombies.add(zombie.getUniqueId());
        zombieCustomNames.put(zombie.getUniqueId(), customName);
        applyAttributesAndBuffs(zombie, customHealth);
    }

    public void updateZombieName(Zombie zombie, double damageTaken) {
        if (!isHordeZombie(zombie)) return;

        boolean showName = plugin.getConfig().getBoolean("horde-night.show-name", true);
        boolean showHealth = plugin.getConfig().getBoolean("horde-night.show-health", true);

        if (!showName && !showHealth) {
            zombie.setCustomNameVisible(false);
            return;
        }

        AttributeInstance maxHealthAttr = zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            int maxHealth = (int) maxHealthAttr.getValue();
            int currentHealth = (int) Math.max(0, zombie.getHealth() - damageTaken);

            if (currentHealth <= 0) {
                zombie.setCustomNameVisible(false);
                return;
            }

            String baseName = zombieCustomNames.getOrDefault(zombie.getUniqueId(), plugin.getConfig().getString("horde-night.zombie-name", "&cZumbi"));
            
            String finalName;
            if (showName && showHealth) {
                String format = plugin.getConfig().getString("horde-night.name-format", "{name} &7[{current}/{max}❤]");
                finalName = format.replace("{name}", baseName).replace("{current}", String.valueOf(currentHealth)).replace("{max}", String.valueOf(maxHealth));
            } else if (showName) {
                finalName = baseName;
            } else {
                finalName = "§c❤ " + currentHealth + "/" + maxHealth;
            }

            zombie.setCustomName(finalName.replace("&", "§"));
            zombie.setCustomNameVisible(true);
        }
    }

    public Set<UUID> getHordeZombies() {
        return hordeZombies;
    }

    public Map<UUID, String> getZombieCustomNames() {
        return zombieCustomNames;
    }

    public void handleBlockSiege(Zombie zombie, org.bukkit.block.Block targetBlock) {
        String typeName = targetBlock.getType().name();
        java.util.List<String> breakables = plugin.getConfig().getStringList("siege.breakable-blocks");

        if (breakables.contains(typeName)) {
            int currentHits = blockHitsMap.getOrDefault(targetBlock, 0) + 1;
            int requiredHits = plugin.getConfig().getInt("siege.hits-required-to-break", 15);

            if (targetBlock.getType().name().contains("WOOD") || targetBlock.getType().name().contains("DOOR")) {
                targetBlock.getWorld().playSound(targetBlock.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 1.0f);
            } else {
                targetBlock.getWorld().playSound(targetBlock.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_HIT, 0.5f, 0.5f);
            }

            if (currentHits >= requiredHits) {
                blockHitsMap.remove(targetBlock);
                targetBlock.getWorld().playEffect(targetBlock.getLocation(), org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
                targetBlock.setType(org.bukkit.Material.AIR);
                targetBlock.getWorld().playSound(targetBlock.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.0f);
            } else {
                blockHitsMap.put(targetBlock, currentHits);
            }
        }
    }

    public void spawnMiniHorde(Location center) {
        int count = plugin.getConfig().getInt("spawning.group-size", 5);
        double health = plugin.getConfig().getDouble("horde-night.zombie-health", 80.0);
        String name = plugin.getConfig().getString("horde-night.zombie-name", "&cZumbi da Horda");
        spawnMiniHorde(center, health, count, name);
    }

    public void spawnMiniHorde(Location center, double customHealth, int count, String customName) {
        int globalMax = plugin.getConfig().getInt("spawning.global-max-zombies", 40);
        if (hordeZombies.size() >= globalMax) {
            return;
        }

        int minDistance = plugin.getConfig().getInt("spawning.min-distance", 15);
        int maxDistance = plugin.getConfig().getInt("spawning.max-distance", 35);

        for (int i = 0; i < count; i++) {
            if (hordeZombies.size() >= globalMax) {
                break;
            }

            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDistance + random.nextInt(maxDistance - minDistance);
            
            double x = center.getX() + (distance * Math.cos(angle));
            double z = center.getZ() + (distance * Math.sin(angle));
            double y = center.getWorld().getHighestBlockYAt((int) x, (int) z);

            Location spawnLoc = new Location(center.getWorld(), x, y, z);
            Zombie zombie = (Zombie) center.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            registerHordeZombie(zombie, customHealth, customName);
        }
    }

    private void applyAttributesAndBuffs(Zombie zombie, double customHealth) {
        AttributeInstance maxHealthAttr = zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(customHealth);
            zombie.setHealth(customHealth);
        }

        AttributeInstance followRange = zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(100.0);
        }

        for (String buffStr : plugin.getConfig().getStringList("horde-night.zombie-buffs")) {
            String[] split = buffStr.split(":");
            if (split.length >= 2) {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(split[0].toLowerCase());
                PotionEffectType type = org.bukkit.Registry.EFFECT.get(key);
                if (type != null) {
                    try {
                        int amp = Integer.parseInt(split[1]);
                        zombie.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION, amp, false, false));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        updateZombieName(zombie, 0.0);
    }

    public int clearAllZombies() {
        int count = 0;
        for (UUID uuid : new HashSet<>(hordeZombies)) {
            org.bukkit.entity.Entity entity = plugin.getServer().getEntity(uuid);
            if (entity instanceof Zombie) {
                entity.remove();
                count++;
            }
        }
        hordeZombies.clear();
        zombieCustomNames.clear();
        blockHitsMap.clear();
        return count;
    }
}