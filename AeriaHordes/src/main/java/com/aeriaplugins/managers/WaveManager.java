package com.aeriaplugins.hordes.managers;

import com.aeriaplugins.hordes.AeriaHordes;
import com.aeriaplugins.hordes.data.HordeEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WaveManager {

    private final AeriaHordes plugin;
    private HordeEvent currentEvent;

    public WaveManager(AeriaHordes plugin) {
        this.plugin = plugin;
        setupTimeChecker();
    }

private void setupTimeChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    if (!plugin.getConfig().getStringList("allowed-worlds").contains(world.getName())) {
                        continue;
                    }
                    long time = world.getTime();
                    long days = world.getFullTime() / 24000;
                    long interval = plugin.getConfig().getLong("horde-night.days-interval", 3);

                    if (days % interval == 0) {
                        if (time >= 13000 && time < 13100 && (currentEvent == null || !currentEvent.isActive())) {
                            startHordeNight();
                        }
                    }

                    if (time >= 23000 && time < 23100 && currentEvent != null && currentEvent.isActive()) {
                        endHordeNight();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    public void startHordeNight() {
        String text = plugin.getConfig().getString("horde-night.bossbar.text", "&cA Horda Está Ativa - Sobreviva!");
        String color = plugin.getConfig().getString("horde-night.bossbar.color", "RED");
        String style = plugin.getConfig().getString("horde-night.bossbar.style", "SOLID");

        currentEvent = new HordeEvent(text, color, style);
        currentEvent.start();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§c[AeriaHordes] §lO cair da noite trouxe a escuridão absoluta. As hordas despertaram!");
        }
    }

    public void endHordeNight() {
        if (currentEvent != null) {
            currentEvent.stop();
        }
        plugin.getHordeManager().clearAllZombies();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§a[AeriaHordes] §lO amanhecer chegou. Os sobreviventes resistiram à horda!");
        }
    }

    public boolean isHordeNightActive() {
        return currentEvent != null && currentEvent.isActive();
    }
}