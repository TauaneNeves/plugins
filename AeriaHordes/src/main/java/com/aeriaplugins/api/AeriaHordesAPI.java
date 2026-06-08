package com.aeriaplugins.hordes.api;

import com.aeriaplugins.hordes.AeriaHordes;

public class AeriaHordesAPI {

    public static boolean isHordeNightActive() {
        return AeriaHordes.getInstance().getWaveManager().isHordeNightActive();
    }

    public static int getActiveHordeZombieCount() {
        return AeriaHordes.getInstance().getHordeManager().getHordeZombies().size();
    }
}