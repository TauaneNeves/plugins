package com.aeriaplugins.hordes.data;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class HordeEvent {

    private final BossBar bossBar;
    private boolean active;

    public HordeEvent(String text, String colorStr, String styleStr) {
        BarColor color = BarColor.RED;
        try { color = BarColor.valueOf(colorStr); } catch (Exception ignored) {}
        
        BarStyle style = BarStyle.SOLID;
        try { style = BarStyle.valueOf(styleStr); } catch (Exception ignored) {}

        this.bossBar = Bukkit.createBossBar(text.replace("&", "§"), color, style);
        this.active = false;
    }

    public void start() {
        this.active = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
    }

    public void stop() {
        this.active = false;
        bossBar.removeAll();
    }

    public boolean isActive() {
        return active;
    }

    public BossBar getBossBar() {
        return bossBar;
    }
}