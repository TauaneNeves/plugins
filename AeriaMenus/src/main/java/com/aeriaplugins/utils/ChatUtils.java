package com.aeriaplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtils {

    private static boolean supportsHex;

    static {
        // Detecta em tempo de execução se o método de cores hexadecimais .of() existe na API
        try {
            net.md_5.bungee.api.ChatColor.class.getMethod("of", java.awt.Color.class);
            supportsHex = true;
        } catch (Throwable e) {
            supportsHex = false;
        }
    }

    public static String color(Player p, String message, boolean usePapi) {
        if (message == null) return "";
        
        if (p != null && usePapi && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, message);
        }

        // Processamento Seguro de Gradientes
        Pattern gradientPattern = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>");
        Matcher gradientMatcher = gradientPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (gradientMatcher.find()) {
            String hexStart = gradientMatcher.group(1);
            String hexEnd = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);
            
            if (supportsHex) {
                try {
                    gradientMatcher.appendReplacement(buffer, applyGradient(text, hexStart, hexEnd));
                } catch (Throwable t) {
                    gradientMatcher.appendReplacement(buffer, text);
                }
            } else {
                // Fallback definitivo para o chat da 1.8.8
                gradientMatcher.appendReplacement(buffer, text);
            }
        }
        gradientMatcher.appendTail(buffer);
        message = buffer.toString();

        // Processamento Seguro de Cores Hexadecimais Individuais
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher hexMatcher = hexPattern.matcher(message);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            if (supportsHex) {
                try {
                    hexMatcher.appendReplacement(hexBuffer, net.md_5.bungee.api.ChatColor.of("#" + hexMatcher.group(1)).toString());
                } catch (Throwable t) {
                    hexMatcher.appendReplacement(hexBuffer, "§f");
                }
            } else {
                // Fallback para a 1.8.8: Converte para cor clássica branca padrão
                hexMatcher.appendReplacement(hexBuffer, "§f");
            }
        }
        hexMatcher.appendTail(hexBuffer);

        return ChatColor.translateAlternateColorCodes('&', hexBuffer.toString());
    }

    private static String applyGradient(String text, String hexStart, String hexEnd) {
        Matcher formatMatcher = Pattern.compile("&[l-o|r|n|m|k|L-O|R|N|M|K]").matcher(text);
        StringBuilder formats = new StringBuilder();
        while (formatMatcher.find()) {
            formats.append(ChatColor.translateAlternateColorCodes('&', formatMatcher.group()));
        }
        
        String cleanText = text.replaceAll("&[l-o|r|n|m|k|L-O|R|N|M|K]", "");

        java.awt.Color start = java.awt.Color.decode("#" + hexStart);
        java.awt.Color end = java.awt.Color.decode("#" + hexEnd);
        StringBuilder sb = new StringBuilder();
        int length = cleanText.length();
        
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1 == 0 ? 1 : length - 1);
            int red = (int) (start.getRed() * (1 - ratio) + end.getRed() * ratio);
            int green = (int) (start.getGreen() * (1 - ratio) + end.getGreen() * ratio);
            int blue = (int) (start.getBlue() * (1 - ratio) + end.getBlue() * ratio);
            
            sb.append(net.md_5.bungee.api.ChatColor.of(new java.awt.Color(red, green, blue)));
            sb.append(formats.toString());
            sb.append(cleanText.charAt(i));
        }
        return sb.toString();
    }
}