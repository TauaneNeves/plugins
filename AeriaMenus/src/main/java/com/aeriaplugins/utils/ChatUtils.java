package com.aeriaplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtils {

    public static String color(Player p, String message, boolean usePapi) {
        if (message == null) return "";
        
        if (p != null && usePapi && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, message);
        }

        Pattern gradientPattern = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>");
        Matcher gradientMatcher = gradientPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (gradientMatcher.find()) {
            String hexStart = gradientMatcher.group(1);
            String hexEnd = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);
            gradientMatcher.appendReplacement(buffer, applyGradient(text, hexStart, hexEnd));
        }
        gradientMatcher.appendTail(buffer);
        message = buffer.toString();

        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher hexMatcher = hexPattern.matcher(message);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexBuffer, net.md_5.bungee.api.ChatColor.of("#" + hexMatcher.group(1)).toString());
        }
        hexMatcher.appendTail(hexBuffer);

        return ChatColor.translateAlternateColorCodes('&', hexBuffer.toString());
    }

    private static String applyGradient(String text, String hexStart, String hexEnd) {
        java.awt.Color start = java.awt.Color.decode("#" + hexStart);
        java.awt.Color end = java.awt.Color.decode("#" + hexEnd);
        StringBuilder sb = new StringBuilder();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1 == 0 ? 1 : length - 1);
            int red = (int) (start.getRed() * (1 - ratio) + end.getRed() * ratio);
            int green = (int) (start.getGreen() * (1 - ratio) + end.getGreen() * ratio);
            int blue = (int) (start.getBlue() * (1 - ratio) + end.getBlue() * ratio);
            sb.append(net.md_5.bungee.api.ChatColor.of(new java.awt.Color(red, green, blue)));
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }
}