package com.aeriaplugins.vitals.tasks;

import com.aeriaplugins.vitals.AeriaVitals;
import com.aeriaplugins.vitals.data.PlayerData;
import com.aeriaplugins.vitals.managers.TemperatureManager;
import org.bukkit.Bukkit;
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

            String actionBarMessage = String.format(
                "§c☣ %d%% §8| §a❤ %d%% §8| §b❄ %.1f°C §8| §6⚖ %.1f/%.1f kg",
                (int) data.getInfection(),
                (int) data.getImmunity(),
                data.getTemperature(),
                data.getCurrentWeight(),
                data.getMaxWeight()
            );
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBarMessage));
        }
    }
}