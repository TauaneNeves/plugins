package com.aeriaplugins;

import com.aeriaplugins.commands.MapCommands;
import com.aeriaplugins.listeners.ChestListener;
import org.bukkit.plugin.java.JavaPlugin;

public class AeriaMap extends JavaPlugin {
    @Override
    public void onEnable() {
        // Registro de comandos
        getCommand("apoc").setExecutor(new MapCommands(this));
        // Registro de eventos
        getServer().getPluginManager().registerEvents(new ChestListener(this), this);

        getLogger().info("AeriaMap ativado.");
    }
}