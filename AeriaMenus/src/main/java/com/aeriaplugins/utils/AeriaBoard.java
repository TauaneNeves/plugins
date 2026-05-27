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
    private final Objective objective;
    private final Player player;

    public AeriaBoard(Player player) {
        this.player = player;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.scoreboard = manager != null ? manager.getNewScoreboard() : null;
        
        if (this.scoreboard != null) {
            this.objective = scoreboard.registerNewObjective("aeria", "dummy", "Carregando...");
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
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
        
        // O limite nativo do Minecraft para scoreboards laterais é de 15 linhas
        int size = Math.min(lines.size(), 15); 
        
        for (int i = 0; i < 15; i++) {
            Team team = scoreboard.getTeam("line_" + i);
            if (team == null) {
                team = scoreboard.registerNewTeam("line_" + i);
            }
            
            // Cria identificadores únicos invisíveis para cada linha
            String entry = ChatColor.values()[i].toString() + ChatColor.RESET;
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }

            if (i < size) {
                // A lista vem de cima para baixo, mas a scoreboard lê de baixo para cima
                String line = lines.get(size - 1 - i);
                team.setPrefix(line);
                
                org.bukkit.scoreboard.Score score = objective.getScore(entry);
                score.setScore(i + 1);
                
                // Oculta os números vermelhos laterais (Exclusivo Paper 1.20.6+)
                try {
                    Class<?> paperFormatClass = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
                    Object blankFormat = paperFormatClass.getMethod("blank").invoke(null);
                    score.getClass().getMethod("numberFormat", paperFormatClass).invoke(score, blankFormat);
                } catch (Exception ignored) {}
                
            } else {
                scoreboard.resetScores(entry); // Apaga as linhas que sobrarem
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