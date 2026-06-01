package com.aeriaplugins.raids;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class RaidSession {
    public Location loc;
    public BlockFace face;
    public int[] digitos = new int[]{0, 0, 0};
    public SessionType type;
    public boolean bloqueadoAnimacao = false;

    public RaidSession(Location loc, BlockFace face, SessionType type) {
        this.loc = loc;
        this.face = face;
        this.type = type;
    }
}