package com.aeriaplugins.raids;

import com.aeriaplugins.raids.commands.RaidCommand;
import com.aeriaplugins.raids.data.RaidStorage;
import com.aeriaplugins.raids.listeners.LockListener;
import com.aeriaplugins.raids.listeners.MenuListener;
import com.aeriaplugins.raids.managers.RaidManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AeriaRaids extends JavaPlugin {
    private static AeriaRaids instance;
    private RaidStorage raidStorage;
    private RaidManager raidManager;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        this.raidStorage = new RaidStorage(this);
        this.raidManager = new RaidManager();
        
        getServer().getPluginManager().registerEvents(new LockListener(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        
        getCommand("aeriaraids").setExecutor(new RaidCommand());
        
        getLogger().info("AeriaRaids: Sistema de arrombamento carregado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (raidManager != null) {
            raidManager.finalizarRaid();
        }
        getLogger().info("AeriaRaids: Modulo desativado.");
    }

    public static AeriaRaids getInstance() { return instance; }
    public RaidStorage getRaidStorage() { return raidStorage; }
    public RaidManager getRaidManager() { return raidManager; }
}