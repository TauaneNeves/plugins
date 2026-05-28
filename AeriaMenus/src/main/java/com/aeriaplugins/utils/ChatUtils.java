package com.aeriaplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtils {

    private static boolean supportsHex = false;

    static {
        try {
            net.md_5.bungee.api.ChatColor.class.getMethod("of", String.class);
            supportsHex = true;
        } catch (Throwable e) {
            supportsHex = false;
        }
    }

    public static String color(Player p, String message, boolean usePapi) {
        if (message == null || message.isEmpty()) return "";
        
        if (p != null && usePapi && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, message);
        }

        // Traduz gradientes avançados <gradient:#hex:#hex>Texto</gradient>
        Pattern gradientPattern = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>");
        Matcher gradientMatcher = gradientPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (gradientMatcher.find()) {
            String hexStart = gradientMatcher.group(1);
            String hexEnd = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);
            
            if (supportsHex) {
                gradientMatcher.appendReplacement(buffer, applyGradientModern(text, hexStart, hexEnd));
            } else {
                gradientMatcher.appendReplacement(buffer, ChatColor.translateAlternateColorCodes('&', "&f" + text));
            }
        }
        gradientMatcher.appendTail(buffer);
        message = buffer.toString();

        // Traduz cores hexadecimais simples &#hex para a 26.1.2
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher hexMatcher = hexPattern.matcher(message);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            if (supportsHex) {
                try {
                    java.lang.reflect.Method ofMethod = net.md_5.bungee.api.ChatColor.class.getMethod("of", String.class);
                    Object colorObj = ofMethod.invoke(null, "#" + hexMatcher.group(1));
                    hexMatcher.appendReplacement(hexBuffer, colorObj.toString());
                } catch (Throwable t) {
                    hexMatcher.appendReplacement(hexBuffer, "§f");
                }
            } else {
                hexMatcher.appendReplacement(hexBuffer, "§f");
            }
        }
        hexMatcher.appendTail(hexBuffer);
        message = hexBuffer.toString();

        // Traduz códigos clássicos (&a, &b, &l) de forma limpa e nativa
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String applyGradientModern(String text, String hexStart, String hexEnd) {
        try {
            Matcher formatMatcher = Pattern.compile("&[l-o|r|n|m|k|L-O|R|N|M|K]").matcher(text);
            StringBuilder formats = new StringBuilder();
            while (formatMatcher.find()) {
                formats.append(ChatColor.translateAlternateColorCodes('&', formatMatcher.group()));
            }
            
            String cleanText = text.replaceAll("&[l-o|r|n|m|k|L-O|R|N|M|K]", "");
            int length = cleanText.length();
            if (length == 0) return text;

            java.awt.Color start = java.awt.Color.decode("#" + hexStart);
            java.awt.Color end = java.awt.Color.decode("#" + hexEnd);
            StringBuilder sb = new StringBuilder();
            
            java.lang.reflect.Method ofMethod = net.md_5.bungee.api.ChatColor.class.getMethod("of", java.awt.Color.class);

            for (int i = 0; i < length; i++) {
                float ratio = (float) i / (float) (length - 1 == 0 ? 1 : length - 1);
                int red = (int) (start.getRed() * (1 - ratio) + end.getRed() * ratio);
                int green = (int) (start.getGreen() * (1 - ratio) + end.getGreen() * ratio);
                int blue = (int) (start.getBlue() * (1 - ratio) + end.getBlue() * ratio);
                
                Object colorObj = ofMethod.invoke(null, new java.awt.Color(red, green, blue));
                sb.append(colorObj.toString());
                sb.append(formats.toString());
                sb.append(cleanText.charAt(i));
            }
            return sb.toString();
        } catch (Throwable t) {
            return text;
        }
    }
}