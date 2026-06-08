package com.aeriaplugins.data;

import org.bukkit.inventory.ItemStack;

public class PlayerData {

    private double infection;
    private double immunity;
    private double temperature = 36.5;
    private double currentWeight = 0.0;
    private double maxWeight = 50.0;
    private boolean hasCold = false;

    private boolean bleeding = false;
    private double thirst = 100.0;
    private int morphineTicks = 0;

    // Slots simplificados: Apenas Armadura nativa, Máscara e Mochila
    private ItemStack customHelmet;
    private ItemStack customMascara;
    private ItemStack customChestplate;
    private ItemStack customLeggings;
    private ItemStack customBoots;
    private ItemStack customBackpack;

    public double getInfection() {
        return infection;
    }

    public void setInfection(double infection) {
        this.infection = Math.max(0.0, Math.min(100.0, infection));
    }

    public void addInfection(double amount) {
        setInfection(this.infection + amount);
    }

    public double getImmunity() {
        return immunity;
    }

    public void setImmunity(double immunity) {
        this.immunity = Math.max(0.0, Math.min(100.0, immunity));
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = Math.max(-50.0, Math.min(50.0, temperature));
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(double currentWeight) {
        this.currentWeight = Math.max(0.0, currentWeight);
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(double maxWeight) {
        this.maxWeight = maxWeight;
    }
    
    public boolean hasCold() {
        return hasCold;
    }

    public void setHasCold(boolean hasCold) {
        this.hasCold = hasCold;
    }

    public boolean isBleeding() {
        return bleeding;
    }

    public void setBleeding(boolean bleeding) {
        this.bleeding = bleeding;
    }

    public double getThirst() {
        return thirst;
    }

    public void setThirst(double thirst) {
        this.thirst = Math.max(0.0, Math.min(100.0, thirst));
    }

    public int getMorphineTicks() {
        return morphineTicks;
    }

    public void setMorphineTicks(int morphineTicks) {
        this.morphineTicks = Math.max(0, morphineTicks);
    }

    public int getBackpackTier() {
        if (customBackpack == null || !customBackpack.hasItemMeta() || !customBackpack.getItemMeta().hasDisplayName()) {
            return 0;
        }
        String name = customBackpack.getItemMeta().getDisplayName();
        if (name.contains("Mochila de Couro")) return 1;
        if (name.contains("Mochila Militar")) return 2;
        return 0;
    }

    public ItemStack getCustomHelmet() { return customHelmet; }
    public void setCustomHelmet(ItemStack customHelmet) { this.customHelmet = customHelmet; }

    public ItemStack getCustomMascara() { return customMascara; }
    public void setCustomMascara(ItemStack customMascara) { this.customMascara = customMascara; }

    public ItemStack getCustomChestplate() { return customChestplate; }
    public void setCustomChestplate(ItemStack customChestplate) { this.customChestplate = customChestplate; }

    public ItemStack getCustomLeggings() { return customLeggings; }
    public void setCustomLeggings(ItemStack customLeggings) { this.customLeggings = customLeggings; }

    public ItemStack getCustomBoots() { return customBoots; }
    public void setCustomBoots(ItemStack customBoots) { this.customBoots = customBoots; }

    public ItemStack getCustomBackpack() { return customBackpack; }
    public void setCustomBackpack(ItemStack customBackpack) { this.customBackpack = customBackpack; }
}