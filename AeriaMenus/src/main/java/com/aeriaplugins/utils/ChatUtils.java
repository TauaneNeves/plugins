package com.aeriaplugins.utils;

import com.viaversion.viaversion.api.Via;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ========================================================================
// CLASSE: ChatUtils
// OBJETIVO: Pintar textos e interpretar Placeholders (variáveis como %player%).
// ========================================================================
public class ChatUtils {

    // Deteta se o servidor é novo o suficiente para suportar cores RGB/Hexadecimais
    private static boolean supportsHex = false;

    static {
        try {
            net.md_5.bungee.api.ChatColor.class.getMethod("of", String.class);
            supportsHex = true;
        } catch (Throwable e) {
            supportsHex = false;
        }
    }

    // ------------------------------------------------------------------------
    // DETETA SE O PLAYER SUPORTA RGB (1.16+)
    // ------------------------------------------------------------------------
    private static boolean playerSupportsHex(Player player) {
        try {
            int protocol = Via.getAPI().getPlayerVersion(player.getUniqueId());

            // 1.16+
            return protocol >= 735;

        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // MÉTODO: color
    // A função principal que pega num texto cru e devolve-o colorido.
    // ------------------------------------------------------------------------
    public static String color(Player p, String message, boolean usePapi) {

        if (message == null || message.isEmpty())
            return "";

        // Aplica o PlaceholderAPI se ativado (substitui %server_online%, %player_name%,
        // etc)
        if (p != null && usePapi && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, message);
        }
        // Corrige placeholders quebrados
        message = fixBrokenPlaceholders(message);

        // 1. Traduz gradientes modernos no formato
        // <gradient:#hex1:#hex2>Texto</gradient>
        Pattern gradientPattern = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>");
        Matcher gradientMatcher = gradientPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (gradientMatcher.find()) {

            String hexStart = gradientMatcher.group(1);
            String hexEnd = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);

            if (supportsHex && p != null && playerSupportsHex(p)) {

                // Mistura as cores se o player suportar RGB
                gradientMatcher.appendReplacement(
                        buffer,
                        Matcher.quoteReplacement(
                                applyGradientModern(text, hexStart, hexEnd)));

            } else {

                // Fallback bonito para versões antigas
                gradientMatcher.appendReplacement(
                        buffer,
                        Matcher.quoteReplacement(
                                ChatColor.translateAlternateColorCodes('&', "&d" + text)));
            }
        }

        gradientMatcher.appendTail(buffer);
        message = buffer.toString();

        // 2. Traduz cores hexadecimais simples, formato &#hex
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher hexMatcher = hexPattern.matcher(message);
        StringBuffer hexBuffer = new StringBuffer();

        while (hexMatcher.find()) {

            if (supportsHex && p != null && playerSupportsHex(p)) {

                // GERAÇÃO DIRETA DO HEX NATIVO
                String hex = hexMatcher.group(1);

                StringBuilder colorBuilder = new StringBuilder("§x");

                for (char c : hex.toCharArray()) {
                    colorBuilder.append('§').append(c);
                }

                hexMatcher.appendReplacement(
                        hexBuffer,
                        Matcher.quoteReplacement(colorBuilder.toString()));

            } else {

                // Conversão RGB -> Legacy bonita
                String hex = hexMatcher.group(1).toUpperCase();

                switch (hex) {

                    case "F4F7FB":
                        hexMatcher.appendReplacement(hexBuffer, "§f");
                        break;

                    case "8EC5FC":
                        hexMatcher.appendReplacement(hexBuffer, "§b");
                        break;

                    case "C8B6FF":
                        hexMatcher.appendReplacement(hexBuffer, "§d");
                        break;

                    case "98F5E1":
                        hexMatcher.appendReplacement(hexBuffer, "§a");
                        break;

                    case "AAB7C4":
                        hexMatcher.appendReplacement(hexBuffer, "§7");
                        break;

                    case "E8B4BC":
                        hexMatcher.appendReplacement(hexBuffer, "§d");
                        break;

                    default:
                        hexMatcher.appendReplacement(hexBuffer, "§f");
                        break;
                }
            }
        }

        hexMatcher.appendTail(hexBuffer);
        message = hexBuffer.toString();

        // 3. Por fim, traduz os códigos clássicos do Minecraft (&a, &b, &l, &n...)
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // ------------------------------------------------------------------------
    // Remove placeholders inválidos conhecidos
    // ------------------------------------------------------------------------
    private static String fixBrokenPlaceholders(String message) {

        // Vault / LuckPerms
        if (message.contains("%vault_rank%")) {
            message = message.replace("%vault_rank%", "Carregando...");
        }

        // Economia
        if (message.contains("%vault_eco_balance_formatted%")) {
            message = message.replace("%vault_eco_balance_formatted%", "0");
        }

        // Ping
        if (message.contains("%player_ping%")) {
            message = message.replace("%player_ping%", "--");
        }

        // Online
        if (message.contains("%server_online%")) {
            message = message.replace("%server_online%", "0");
        }

        // Máximo de players
        if (message.contains("%server_max_players%")) {
            message = message.replace("%server_max_players%", "0");
        }

        return message;
    }

    // ------------------------------------------------------------------------
    // MÉTODO: applyGradientModern
    // Uma fórmula matemática que calcula a transição suave de uma cor para a outra
    // letra a letra.
    // ------------------------------------------------------------------------
    private static String applyGradientModern(String text, String hexStart, String hexEnd) {

        try {

            // Guarda códigos de formatação como Negrito (&l) ou Sublinhado (&n)
            Matcher formatMatcher = Pattern.compile("&[l-o|r|n|m|k|L-O|R|N|M|K]").matcher(text);

            StringBuilder formats = new StringBuilder();

            while (formatMatcher.find()) {
                formats.append(ChatColor.translateAlternateColorCodes('&', formatMatcher.group()));
            }

            // Limpa o texto original para não pintar a formatação por engano
            String cleanText = text.replaceAll("&[l-o|r|n|m|k|L-O|R|N|M|K]", "");

            int length = cleanText.length();

            if (length == 0)
                return text;

            java.awt.Color start = java.awt.Color.decode("#" + hexStart);
            java.awt.Color end = java.awt.Color.decode("#" + hexEnd);

            StringBuilder sb = new StringBuilder();

            // Um ciclo que passa por cada letra e mistura a percentagem da Cor A com a Cor
            // B
            for (int i = 0; i < length; i++) {

                float ratio = (float) i / (float) (length - 1 == 0 ? 1 : length - 1);

                int red = (int) (start.getRed() * (1 - ratio) + end.getRed() * ratio);
                int green = (int) (start.getGreen() * (1 - ratio) + end.getGreen() * ratio);
                int blue = (int) (start.getBlue() * (1 - ratio) + end.getBlue() * ratio);

                // GERAÇÃO DIRETA DO HEX NATIVO PARA O GRADIENTE
                String hex = String.format("%02x%02x%02x", red, green, blue);

                StringBuilder colorBuilder = new StringBuilder("§x");

                for (char c : hex.toCharArray()) {
                    colorBuilder.append('§').append(c);
                }

                sb.append(colorBuilder.toString()); // Aplica a cor misturada
                sb.append(formats.toString()); // Aplica formatação (ex: Negrito)
                sb.append(cleanText.charAt(i)); // Cola a letra atual
            }

            return sb.toString();

        } catch (Throwable t) {
            return text; // Em caso de erro extremo, devolve o texto sem estragar nada
        }
    }
}
