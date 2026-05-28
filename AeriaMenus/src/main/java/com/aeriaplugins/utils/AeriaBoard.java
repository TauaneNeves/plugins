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
    private static boolean isLegacyServer = true;

    static {
        try {
            Team.class.getMethod("setColor", ChatColor.class);
            isLegacyServer = false;
        } catch (Throwable e) {
            isLegacyServer = true;
        }
    }

    public AeriaBoard(Player player) {
        this.player = player;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.scoreboard = manager != null ? manager.getNewScoreboard() : null;
        
        if (this.scoreboard != null) {
            Objective old = this.scoreboard.getObjective("aeria");
            if (old != null) old.unregister();

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
            String entry = ChatColor.values()[i].toString() + ChatColor.RESET;
            Team team = scoreboard.getTeam("line_" + i);
            
            if (i < size) {
                if (team == null) {
                    team = scoreboard.registerNewTeam("line_" + i);
                }
                if (!team.hasEntry(entry)) {
                    team.addEntry(entry);
                }

                String line = lines.get(size - 1 - i);
                
                if (isLegacyServer) {
                    if (line.length() > 16) {
                        String prefix = line.substring(0, 16);
                        String lastColors = ChatColor.getLastColors(prefix);
                        String suffix = lastColors + line.substring(16);
                        
                        team.setPrefix(prefix);
                        team.setSuffix(suffix.length() > 16 ? suffix.substring(0, 16) : suffix);
                    } else {
                        team.setPrefix(line);
                        team.setSuffix("");
                    }
                } else {
                    try {
                        team.setPrefix(line);
                        team.setSuffix("");
                    } catch (Throwable ignored) {}
                }
                
                objective.getScore(entry).setScore(i + 1);
                
                try {
                    java.lang.reflect.Method method = objective.getScore(entry).getClass().getMethod("numberFormat", 
                            Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat"));
                    Object blankFormat = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat").getMethod("blank").invoke(null);
                    method.invoke(objective.getScore(entry), blankFormat);
                } catch (Exception ignored) {}
            } else {
                if (team != null) {
                    team.unregister();
                }
                scoreboard.resetScores(entry);
            }
        }
    }

    public void delete() {
        if (player != null && player.isOnline()) {
            ScoreboardManager m = Bukkit.getScoreboardManager();
            if (m != null) player.setScoreboard(m.getMainScoreboard());
        }
        if (scoreboard != null) {
            for (Team team : scoreboard.getTeams()) {
                try { team.unregister(); } catch (Throwable ignored) {}
            }
        }
    }
}