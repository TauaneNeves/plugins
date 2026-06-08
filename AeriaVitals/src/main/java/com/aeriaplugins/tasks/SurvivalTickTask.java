package com.aeriaplugins.tasks;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
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

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerData(player);
            if (data == null) continue;

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
                    player.sendMessage("§cVocê está morrendo de desidratação! Encontre água potável.");
                }
            }

            if (data.isBleeding()) {
                player.getWorld().spawnParticle(
                    Particle.DUST, 
                    player.getLocation().add(0, 0.8, 0), 
                    6, 0.2, 0.3, 0.2, 
                    new Particle.DustOptions(org.bukkit.Color.RED, 1.0f)
                );
                
                if (isFiveSeconds) {
                    player.damage(1.0);
                    player.sendMessage("§cVocê está sangrando! Use uma bandagem para estancar o ferimento.");
                }
            }

            if (data.getMorphineTicks() > 0) {
                data.setMorphineTicks(data.getMorphineTicks() - 20);
                if (data.getMorphineTicks() <= 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 0));
                    player.sendMessage("§cO efeito da morfina acabou. Seu corpo está sofrendo crises de abstinência e náuseas.");
                }
            } else {
                if (player.getHealth() <= 6.0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, true, false));
                }
            }

            String bleedingIcon = data.isBleeding() ? " §4[SANGRAMENTO]" : "";
            String actionMessage = String.format(
                "§7Peso: §e%.1f/%.1f kg §8| §7Infecção: §c%.1f%% §8| §7Sede: §b%.1f%% %s",
                data.getCurrentWeight(), data.getMaxWeight(), data.getInfection(), data.getThirst(), bleedingIcon
            );
            
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(actionMessage));
        }
    }
}