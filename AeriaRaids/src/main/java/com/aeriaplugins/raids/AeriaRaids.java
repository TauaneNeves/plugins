// src/main/java/com/aeriaplugins/raids/AeriaRaids.java
package com.aeriaplugins.raids;

import com.aeriaplugins.raids.commands.RaidCommand;
import com.aeriaplugins.raids.data.RaidStorage;
import com.aeriaplugins.raids.listeners.LockListener;
import org.bukkit.plugin.java.JavaPlugin;

public class AeriaRaids extends JavaPlugin {
    private static AeriaRaids instance;
    private RaidStorage raidStorage;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        this.raidStorage = new RaidStorage(this);
        
        getServer().getPluginManager().registerEvents(new LockListener(), this);
        getCommand("aeriaraids").setExecutor(new RaidCommand());
        
        getLogger().info("AeriaRaids: Sistema de arrombamento carregado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AeriaRaids: Modulo desativado.");
    }

    public static AeriaRaids getInstance() { return instance; }
    public RaidStorage getRaidStorage() { return raidStorage; }
}