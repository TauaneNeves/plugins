package com.aeriaplugins.commands;

import com.aeriaplugins.AeriaMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MapCommands implements CommandExecutor {
    private final AeriaMap plugin;

    public MapCommands(AeriaMap plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            sender.sendMessage("Debug AeriaMap ativado.");
            return true;
        }
        return false;
    }
}