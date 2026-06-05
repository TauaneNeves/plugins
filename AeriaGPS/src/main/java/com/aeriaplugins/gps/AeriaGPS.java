package com.aeriaplugins.gps;

import com.aeriaplugins.gps.commands.GPSCommand;
import com.aeriaplugins.gps.managers.GPSItemManager;
import com.aeriaplugins.gps.managers.IslandManager;
import com.aeriaplugins.gps.listeners.GPSListener;
import org.bukkit.plugin.java.JavaPlugin;

public class AeriaGPS extends JavaPlugin {

    private static AeriaGPS instance;
    private GPSItemManager itemManager;
    private IslandManager islandManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.itemManager = new GPSItemManager(this);
        this.islandManager = new IslandManager(this);

        getCommand("aeriagps").setExecutor(new GPSCommand(this));
        getServer().getPluginManager().registerEvents(new GPSListener(this), this);

        getLogger().info("AeriaGPS ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AeriaGPS desativado.");
    }

    public static AeriaGPS getInstance() {
        return instance;
    }

    public GPSItemManager getItemManager() {
        return itemManager;
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }
}