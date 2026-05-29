package com.aeriaplugins.plugins;

import com.aeriaplugins.commands.IslandCommand;
import com.aeriaplugins.data.IslandStorage;
import com.aeriaplugins.managers.GridManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private World mundoSkyblock;
    private GridManager gridManager;
    private IslandStorage islandStorage;

    @Override
    public void onEnable() {
        instance = this;

        this.islandStorage = new IslandStorage();
        this.gridManager = new GridManager();

        WorldCreator creator = new WorldCreator("aeria_skyblock");
        creator.type(WorldType.FLAT);
        creator.generatorSettings("{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biome\":\"minecraft:plains\"}");
        creator.generateStructures(false);
        this.mundoSkyblock = Bukkit.createWorld(creator);

        this.getCommand("is").setExecutor(new IslandCommand());

        getLogger().info("AeriaIslands: Sistema em mundo unico carregado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AeriaIslands: Modulo desativado.");
    }

    public static Main getInstance() { return instance; }
    public World getMundoSkyblock() { return mundoSkyblock; }
    public GridManager getGridManager() { return gridManager; }
    public IslandStorage getIslandStorage() { return islandStorage; }
}