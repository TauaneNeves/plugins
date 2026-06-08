package com.aeriaplugins.vitals.data;

public class PlayerData {

    private double infection;
    private double immunity;
    private double temperature;
    private double currentWeight;
    private double maxWeight;

    public PlayerData() {
        this.infection = 0.0;
        this.immunity = 100.0;
        this.temperature = 0.0;
        this.currentWeight = 0.0;
        this.maxWeight = 50.0;
    }

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
        this.maxWeight = Math.max(1.0, maxWeight);
    }
}