package com.aeriaplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import java.util.List;

public class AeriaBoard {
    private final Scoreboard scoreboard;
    private Objective objective;
    private final Player player;

    public AeriaBoard(Player player) {
        this.player = player;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.scoreboard = manager != null ? manager.getNewScoreboard() : null;
        
        if (this.scoreboard != null) {
            // Tenta registrar usando o método moderno de 3 parâmetros, caso falhe (1.8.8), usa o clássico de 2 parâmetros
            try {
                this.objective = (Objective) Scoreboard.class.getMethod("registerNewObjective", String.class, String.class, String.class)
                        .invoke(this.scoreboard, "aeria", "dummy", "aeria");
            } catch (Throwable t) {
                this.objective = this.scoreboard.registerNewObjective("aeria", "dummy");
            }
            
            if (this.objective != null) {
                this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
            player.setScoreboard(this.scoreboard);
        } else {
            this.objective = null;
        }
    }

    public void updateTitle(String title) {
        if (objective != null) {
            objective.setDisplayName(title);
        }
    }

    public void updateLines(List<String> lines) {
        if (scoreboard == null || objective == null) return;
        int size = Math.min(lines.size(), 15);
        
        for (int i = 0; i < 15; i++) {
            Team team = scoreboard.getTeam("line_" + i);
            if (team == null) {
                team = scoreboard.registerNewTeam("line_" + i);
            }
            
            String entry = ChatColor.values()[i].toString() + ChatColor.RESET;
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }
            
            if (i < size) {
                String line = lines.get(size - 1 - i);
                
                // Divisor de caracteres seguro para evitar desconexão/kick na 1.8.8
                if (line.length() > 16) {
                    team.setPrefix(line.substring(0, 16));
                    String suffix = line.substring(16);
                    team.setSuffix(suffix.length() > 16 ? suffix.substring(0, 16) : suffix);
                } else {
                    team.setPrefix(line);
                    team.setSuffix("");
                }
                
                objective.getScore(entry).setScore(i + 1);
                
                // Ocultação nativa dos números vermelhos (Disponível a partir do Paper 1.20.6+)
                try {
                    Class<?> paperFormatClass = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
                    Object blankFormat = paperFormatClass.getMethod("blank").invoke(null);
                    Object scoreObj = objective.getScore(entry);
                    scoreObj.getClass().getMethod("numberFormat", paperFormatClass).invoke(scoreObj, blankFormat);
                } catch (Exception ignored) {}
            } else {
                scoreboard.resetScores(entry);
            }
        }
    }

    public void delete() {
        if (player != null && player.isOnline()) {
            ScoreboardManager m = Bukkit.getScoreboardManager();
            if (m != null) player.setScoreboard(m.getMainScoreboard());
        }
    }
}