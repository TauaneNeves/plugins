package com.aeriaplugins.hordes;

import com.aeriaplugins.hordes.commands.HordesCommand;
import com.aeriaplugins.hordes.listeners.EntityTargetListener;
import com.aeriaplugins.hordes.listeners.NoiseListener;
import com.aeriaplugins.hordes.listeners.SiegeListener;
import com.aeriaplugins.hordes.managers.HordeManager;
import com.aeriaplugins.hordes.managers.WaveManager;
import com.aeriaplugins.hordes.tasks.HordeTrackerTask;
import org.bukkit.plugin.java.JavaPlugin;

public class AeriaHordes extends JavaPlugin {

    private static AeriaHordes instance;
    private HordeManager hordeManager;
    private WaveManager waveManager;
    private NoiseListener noiseListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.hordeManager = new HordeManager(this);
        this.waveManager = new WaveManager(this);
        this.noiseListener = new NoiseListener(this);

        getServer().getPluginManager().registerEvents(new EntityTargetListener(this), this);
        getServer().getPluginManager().registerEvents(noiseListener, this);
        getServer().getPluginManager().registerEvents(new SiegeListener(this), this);

        HordesCommand hordesCommand = new HordesCommand(this);
        getCommand("aeriahordes").setExecutor(hordesCommand);
        getCommand("aeriahordes").setTabCompleter(hordesCommand);

        long interval = getConfig().getLong("settings.check-interval-ticks", 40L);
        new HordeTrackerTask(this).runTaskTimer(this, 0L, interval);

        getLogger().info("AeriaHordes ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        if (waveManager != null) {
            waveManager.endHordeNight();
        }
        if (hordeManager != null) {
            hordeManager.clearAllZombies();
        }
        if (noiseListener != null) {
            noiseListener.clearNoiseData();
        }
        getLogger().info("AeriaHordes desativado.");
    }

    public static AeriaHordes getInstance() {
        return instance;
    }

    public HordeManager getHordeManager() {
        return hordeManager;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }
}