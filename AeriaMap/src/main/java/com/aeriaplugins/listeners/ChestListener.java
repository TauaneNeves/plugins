package com.aeriaplugins.listeners;

import com.aeriaplugins.AeriaMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataType;

public class ChestListener implements Listener {
    private final AeriaMap plugin;
    private final NamespacedKey chestKey;

    public ChestListener(AeriaMap plugin) {
        this.plugin = plugin;
        this.chestKey = new NamespacedKey(plugin, "apoc_chest");
    }

    @EventHandler
    public void onChestPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        
        plugin.getLogger().info("Event fired: " + block.getType());

        if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
            // Acessa o estado do bloco (TileState) para manipular o PDC
            if (block.getState() instanceof TileState) {
                TileState state = (TileState) block.getState();
                
                // Grava a marcação no PDC
                state.getPersistentDataContainer().set(chestKey, PersistentDataType.BYTE, (byte) 1);
                state.update(); // Salva as alterações no bloco
                
                plugin.getLogger().info("Baú marcado com sucesso em: " + block.getLocation().toVector());
            }
        }
    }
}