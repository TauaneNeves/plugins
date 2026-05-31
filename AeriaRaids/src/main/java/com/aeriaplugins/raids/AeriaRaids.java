package com.aeriaplugins.raids;

import com.aeriaplugins.raids.data.RaidStorage;
import com.aeriaplugins.raids.listeners.LockListener;
import org.bukkit.plugin.java.JavaPlugin;

public class AeriaRaids extends JavaPlugin {

    private static AeriaRaids instance;
    private RaidStorage raidStorage;

    @Override
    public void onEnable() {
        instance = this;
        this.raidStorage = new RaidStorage(this);
        
        getServer().getPluginManager().registerEvents(new LockListener(), this);
        getLogger().info("AeriaRaids: Sistema de arrombamento carregado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AeriaRaids: Modulo desativado.");
    }

    public static AeriaRaids getInstance() { return instance; }
    public RaidStorage getRaidStorage() { return raidStorage; }
}