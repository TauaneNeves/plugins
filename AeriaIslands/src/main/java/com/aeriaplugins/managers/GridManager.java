package com.aeriaplugins.managers;

import com.aeriaplugins.plugins.Main;
import org.bukkit.Location;

public class GridManager {

    private final int DISTANCIA_ILHAS = 1500; 
    private final int ALTURA_SPAWN = 72;

    public Location calcularProximaCoordenada() {
        int proximoId = Main.getInstance().getIslandStorage().getProximoId();
        int coordenadaX = proximoId * DISTANCIA_ILHAS;
        int coordenadaZ = 0;

        return new Location(Main.getInstance().getMundoSkyblock(), coordenadaX, ALTURA_SPAWN, coordenadaZ);
    }
}