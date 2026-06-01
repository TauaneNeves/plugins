package com.aeriaplugins.raids.managers;

import com.aeriaplugins.raids.AeriaRaids;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RaidManager {

    private BossBar raidBossBar;
    private int tempoRestante = 0;

    public boolean isEmRaid() {
        return raidBossBar != null;
    }

    public void iniciarRaid() {
        if (isEmRaid()) return;
        
        tempoRestante = AeriaRaids.getInstance().getConfig().getInt("raid-duracao", 7200);
        raidBossBar = Bukkit.createBossBar("Invasão em Andamento", BarColor.RED, BarStyle.SOLID);
        for (Player p : Bukkit.getOnlinePlayers()) raidBossBar.addPlayer(p);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tempoRestante <= 0) {
                    finalizarRaid();
                    this.cancel();
                    return;
                }
                tempoRestante--;
                double progresso = (double) tempoRestante / AeriaRaids.getInstance().getConfig().getInt("raid-duracao", 7200);
                raidBossBar.setProgress(Math.max(0.0, Math.min(1.0, progresso)));
            }
        }.runTaskTimer(AeriaRaids.getInstance(), 0L, 20L);
    }

    public void finalizarRaid() {
        if (raidBossBar != null) {
            raidBossBar.removeAll();
            raidBossBar = null;
        }
    }
}