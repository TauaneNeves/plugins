package com.aeriaplugins.tasks;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SurvivalTickTask extends BukkitRunnable {

    private final AeriaVitals plugin;
    private int secondsCounter = 0;

    public SurvivalTickTask(AeriaVitals plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        secondsCounter++;
        boolean isFiveSeconds = (secondsCounter % 5 == 0);

        boolean thirstEnabled = plugin.getConfig().getBoolean("modules.thirst", true);
        boolean bleedingEnabled = plugin.getConfig().getBoolean("modules.bleeding", true);
        boolean radiationEnabled = plugin.getConfig().getBoolean("modules.radiation", true);

        String msgDehydration = plugin.getConfig().getString("messages.dehydration-warning", "").replace("&", "§");
        String msgBleeding = plugin.getConfig().getString("messages.bleeding-warning", "").replace("&", "§");
        String msgMorphineEnd = plugin.getConfig().getString("messages.morphine-end", "").replace("&", "§");
        String msgRadOuter = plugin.getConfig().getString("messages.radiation-warning-outer", "").replace("&", "§");
        String msgRadInner = plugin.getConfig().getString("messages.radiation-warning-inner", "").replace("&", "§");

        int maskCmd = plugin.getConfig().getInt("custom-items.mask_item.custom-model-data", 1005);

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerData(player);
            if (data == null) continue;

            // 1. GESTÃO DA SEDE
            if (thirstEnabled) {
                double baseThirstDrain = 0.04;
                if (data.getCurrentWeight() > 30.0) {
                    baseThirstDrain += (data.getCurrentWeight() - 30.0) * 0.002;
                }
                Biome biome = player.getLocation().getBlock().getBiome();
                if (biome == Biome.DESERT || biome == Biome.BADLANDS || biome == Biome.SAVANNA || data.getTemperature() > 38.5) {
                    baseThirstDrain *= 2.0;
                }
                data.setThirst(data.getThirst() - baseThirstDrain);

                if (data.getThirst() <= 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 1, true, false));
                    player.setSprinting(false);
                    if (isFiveSeconds) {
                        player.damage(1.0);
                        if (!msgDehydration.isEmpty()) player.sendMessage(msgDehydration);
                    }
                }
            }

            // 2. GESTÃO DO SANGRAMENTO
            if (bleedingEnabled && data.isBleeding()) {
                player.getWorld().spawnParticle(
                    Particle.DUST, 
                    player.getLocation().add(0, 0.8, 0), 
                    6, 0.2, 0.3, 0.2, 
                    new Particle.DustOptions(org.bukkit.Color.RED, 1.0f)
                );
                if (isFiveSeconds) {
                    player.damage(1.0);
                    if (!msgBleeding.isEmpty()) player.sendMessage(msgBleeding);
                }
            }

            // 3. MORFINA E SISTEMA DE DOR
            if (data.getMorphineTicks() > 0) {
                data.setMorphineTicks(data.getMorphineTicks() - 20);
                if (data.getMorphineTicks() <= 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 0));
                    if (!msgMorphineEnd.isEmpty()) player.sendMessage(msgMorphineEnd);
                }
            } else {
                if (player.getHealth() <= 6.0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, true, false));
                }
            }

            // 4. SISTEMA PROGRESSIVO DE RADIAÇÃO (DISTÂNCIA CILÍNDRICA 2D)
            boolean insideAnyZone = false;

            if (radiationEnabled) {
                ConfigurationSection zones = plugin.getConfig().getConfigurationSection("radiation-zones");
                if (zones != null) {
                    Location pLoc = player.getLocation();
                    for (String key : zones.getKeys(false)) {
                        String worldName = zones.getString(key + ".world");
                        World world = Bukkit.getWorld(worldName);
                        if (world == null || !pLoc.getWorld().equals(world)) continue;

                        double x = zones.getDouble(key + ".x");
                        double z = zones.getDouble(key + ".z");

                        double distance = Math.sqrt(Math.pow(pLoc.getX() - x, 2) + Math.pow(pLoc.getZ() - z, 2));
                        double outerRadius = zones.getDouble(key + ".outer-radius");
                        double innerRadius = zones.getDouble(key + ".inner-radius");

                        if (distance <= outerRadius) {
                            insideAnyZone = true;
                            
                            ItemStack maskItem = data.getCustomMascara();
                            boolean hasMask = maskItem != null && maskItem.hasItemMeta() && 
                                              maskItem.getItemMeta().hasCustomModelData() && 
                                              maskItem.getItemMeta().getCustomModelData() == maskCmd;

                            double protectionFactor = zones.getDouble(key + ".mask-protection-factor", 1.0);
                            double appliedFactor = hasMask ? (1.0 - protectionFactor) : 1.0;

                            if (distance <= innerRadius) {
                                double innerDamage = zones.getDouble(key + ".inner-damage", 2.0) * appliedFactor;
                                double innerInfection = zones.getDouble(key + ".inner-infection", 1.5) * appliedFactor;

                                data.setRadiation(data.getRadiation() + (3.0 * appliedFactor));
                                if (innerDamage > 0 && isFiveSeconds) player.damage(innerDamage);
                                if (innerInfection > 0) data.addInfection(innerInfection);
                                if (isFiveSeconds && !msgRadInner.isEmpty()) player.sendMessage(msgRadInner);
                            } else {
                                double proximityFactor = 1.0 - ((distance - innerRadius) / (outerRadius - innerRadius));
                                
                                double outerDamage = zones.getDouble(key + ".outer-damage", 0.5) * proximityFactor * appliedFactor;
                                double outerInfection = zones.getDouble(key + ".outer-infection", 0.2) * proximityFactor * appliedFactor;

                                data.setRadiation(data.getRadiation() + (1.2 * proximityFactor * appliedFactor));
                                if (outerDamage > 0 && isFiveSeconds) player.damage(outerDamage);
                                if (outerInfection > 0) data.addInfection(outerInfection);
                                if (isFiveSeconds && !msgRadOuter.isEmpty()) player.sendMessage(msgRadOuter);
                            }
                        }
                    }
                }
            }

            if (!insideAnyZone && data.getRadiation() > 0) {
                data.setRadiation(data.getRadiation() - 0.4);
            }

            // --- INSERÇÃO DOS SINTOMAS VISUAIS NA INTERFACE DE POÇÕES ---
            if (data.isBleeding()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 35, 0, true, false, true));
            } else if (player.hasPotionEffect(PotionEffectType.UNLUCK)) {
                player.removePotionEffect(PotionEffectType.UNLUCK);
            }

            if (data.getRadiation() >= 10.0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BAD_OMEN, 35, 0, true, false, true));
            } else if (player.hasPotionEffect(PotionEffectType.BAD_OMEN)) {
                player.removePotionEffect(PotionEffectType.BAD_OMEN);
            }

            if (data.getRadiation() >= 70.0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 35, 0, true, false, true));
                if (isFiveSeconds) player.damage(1.0);
            } else if (player.hasPotionEffect(PotionEffectType.GLOWING)) {
                player.removePotionEffect(PotionEffectType.GLOWING);
            }

            if (data.getInfection() >= 40.0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 35, 0, true, false, true));
            } else if (player.hasPotionEffect(PotionEffectType.LUCK)) {
                player.removePotionEffect(PotionEffectType.LUCK);
            }

            if (data.getThirst() <= 25.0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 35, 0, true, false, true));
            }

            if (data.getRadiation() >= 90.0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 140, 0, true, false));
            }

            // --- CORREÇÃO DA SEGURANÇA CONTRA NPE NA BUSCA DA VIDA MÁXIMA ---
            double healthPercentage = (player.getHealth() / player.getMaxHealth()) * 100.0;
            String coldStatus = data.hasCold() ? " [Resfriado]" : "";
            String bleedingIcon = data.isBleeding() ? " §4[SANGRAMENTO]" : "";
            
            String actionMessage = String.format(
                "§c☣ %.0f%% §8| §5☢ %.0f%% §8| §b💧 %.0f%% §8| §a❤ %.0f%% §8| §b❄ %.1f°C%s §8| §6⚖ %.1f/%.1f kg%s",
                data.getInfection(),
                data.getRadiation(),
                data.getThirst(),
                healthPercentage,
                data.getTemperature(),
                coldStatus,
                data.getCurrentWeight(),
                data.getMaxWeight(),
                bleedingIcon
            );
            
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(actionMessage));
        }
    }
}