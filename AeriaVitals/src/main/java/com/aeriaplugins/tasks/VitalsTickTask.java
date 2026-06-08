package com.aeriaplugins.tasks;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.data.PlayerData;
import com.aeriaplugins.managers.TemperatureManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class VitalsTickTask extends BukkitRunnable {

    private final AeriaVitals plugin;
    private final TemperatureManager temperatureManager;

    public VitalsTickTask(AeriaVitals plugin) {
        this.plugin = plugin;
        this.temperatureManager = new TemperatureManager(plugin);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerData(player);

            temperatureManager.updateTemperature(player);

            // --- INÍCIO DA MODIFICAÇÃO ---
            double heatThreshold = plugin.getConfig().getDouble("temperature.heat-damage-threshold", 39.5);
            double coldThreshold = plugin.getConfig().getDouble("temperature.cold-damage-threshold", 35.0);

            if (data.getTemperature() >= heatThreshold) {
                player.damage(1.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
            } else if (data.getTemperature() <= coldThreshold) {
                player.damage(1.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
            }

            if (data.getTemperature() < 36.0 && (player.getWorld().hasStorm() || player.getLocation().getBlock().getType() == Material.WATER)) {
                if (!data.hasCold() && Math.random() * 100 < plugin.getConfig().getDouble("temperature.cold-chance-percentage", 5.0)) {
                    data.setHasCold(true);
                    player.sendMessage("§cVocê contraiu um resfriado devido à exposição ao frio e umidade!");
                }
            }

            if (data.hasCold()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, false, false));
                if (Math.random() < 0.05) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, false));
                }
            }
            // --- FIM DA MODIFICAÇÃO ---

            if (player.getFoodLevel() <= 6) {
                data.setImmunity(data.getImmunity() - 0.2);
            } else if (player.getFoodLevel() >= 18) {
                data.setImmunity(data.getImmunity() + 0.1);
            }

            double decay = plugin.getConfig().getDouble("infection.decay-per-second", 0.05);
            if (data.getImmunity() >= 80.0 && data.getInfection() > 0) {
                data.setInfection(data.getInfection() - decay);
            }

            if (data.getInfection() >= 50.0 && data.getInfection() < 80.0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, false, false));
            } else if (data.getInfection() >= 80.0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
            }

            if (data.getTemperature() <= -30.0) {
                player.damage(1.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
            } else if (data.getTemperature() >= 40.0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 0, false, false));
            }

            if (data.getCurrentWeight() > data.getMaxWeight() * 1.3) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false));
            } else if (data.getCurrentWeight() > data.getMaxWeight()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
            }

            // --- INÍCIO DA MODIFICAÇÃO ---
            String coldStatus = data.hasCold() ? " §7[§bResfriado§7]" : "";
            String actionBarMessage = String.format(
                "§c☣ %d%% §8| §a❤ %d%% §8| §b❄ %.1f°C%s §8| §6⚖ %.1f/%.1f kg",
                (int) data.getInfection(),
                (int) data.getImmunity(),
                data.getTemperature(),
                coldStatus,
                data.getCurrentWeight(),
                data.getMaxWeight()
            );
            // --- FIM DA MODIFICAÇÃO ---
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBarMessage));
        
        }
    }
}