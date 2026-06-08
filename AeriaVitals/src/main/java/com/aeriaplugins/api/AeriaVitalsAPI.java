package com.aeriaplugins.vitals.api;

import com.aeriaplugins.vitals.AeriaVitals;
import org.bukkit.entity.Player;

public class AeriaVitalsAPI {

    public static double getInfection(Player player) {
        return AeriaVitals.getInstance().getPlayerData(player).getInfection();
    }

    public static void setInfection(Player player, double value) {
        AeriaVitals.getInstance().getPlayerData(player).setInfection(value);
    }

    public static void addInfection(Player player, double value) {
        AeriaVitals.getInstance().getPlayerData(player).addInfection(value);
    }

    public static double getImmunity(Player player) {
        return AeriaVitals.getInstance().getPlayerData(player).getImmunity();
    }

    public static void setImmunity(Player player, double value) {
        AeriaVitals.getInstance().getPlayerData(player).setImmunity(value);
    }

    public static double getTemperature(Player player) {
        return AeriaVitals.getInstance().getPlayerData(player).getTemperature();
    }

    public static void setTemperature(Player player, double value) {
        AeriaVitals.getInstance().getPlayerData(player).setTemperature(value);
    }

    public static double getCurrentWeight(Player player) {
        return AeriaVitals.getInstance().getPlayerData(player).getCurrentWeight();
    }

    public static double getMaxWeight(Player player) {
        return AeriaVitals.getInstance().getPlayerData(player).getMaxWeight();
    }
}