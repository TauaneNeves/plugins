package com.aeriaplugins.hordes.listeners;

import com.aeriaplugins.hordes.AeriaHordes;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;

public class NoiseListener implements Listener {

    private final AeriaHordes plugin;
    private final Map<Chunk, Integer> chunkNoiseMap = new HashMap<>();

    public NoiseListener(AeriaHordes plugin) {
        this.plugin = plugin;
    }

    // --- INÍCIO DA ADIÇÃO ---
    public void clearNoiseData() {
        chunkNoiseMap.clear();
    }
    // --- FIM DA ADIÇÃO ---

    private void addNoise(Location loc, int amount) {
        if (!plugin.getConfig().getBoolean("noise-system.enabled", true)) return;

        Chunk chunk = loc.getChunk();
        int currentNoise = chunkNoiseMap.getOrDefault(chunk, 0) + amount;
        int threshold = plugin.getConfig().getInt("noise-system.chunk-threshold", 100);

        if (currentNoise >= threshold) {
            chunkNoiseMap.put(chunk, 0);
            plugin.getHordeManager().spawnMiniHorde(loc);
        } else {
            chunkNoiseMap.put(chunk, currentNoise);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isSprinting() && (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ())) {
            int runningPoints = plugin.getConfig().getInt("noise-system.points.running", 5);
            addNoise(player.getLocation(), runningPoints);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        int breakPoints = plugin.getConfig().getInt("noise-system.points.block-break", 15);
        addNoise(event.getBlock().getLocation(), breakPoints);
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            int firePoints = plugin.getConfig().getInt("noise-system.points.weapon-fire", 50);
            addNoise(event.getEntity().getLocation(), firePoints);
        }
    }
}