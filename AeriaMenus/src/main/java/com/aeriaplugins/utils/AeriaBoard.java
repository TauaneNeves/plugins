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

// ========================================================================
// CLASSE: AeriaBoard
// OBJETIVO: Criar e atualizar o painel lateral do jogador de forma suave
// (sem "piscar").
// ========================================================================
public class AeriaBoard {

    private final Scoreboard scoreboard;
    private Objective objective;
    private final Player player;

    // Variável que ajuda a saber se estamos num servidor antigo (como o 1.8)
    // ou moderno
    private static boolean isLegacyServer = true;

    static {
        try {
            Team.class.getMethod("setColor", ChatColor.class);
            isLegacyServer = false;
        } catch (Throwable e) {
            isLegacyServer = true;
        }
    }

    // ------------------------------------------------------------------------
    // MÉTODO: Construtor (AeriaBoard)
    // Chamado na primeira vez que o jogador entra no servidor.
    // ------------------------------------------------------------------------
    public AeriaBoard(Player player) {

        this.player = player;

        ScoreboardManager manager = Bukkit.getScoreboardManager();

        this.scoreboard = manager != null
                ? manager.getNewScoreboard()
                : null;

        if (this.scoreboard != null) {

            Objective old = this.scoreboard.getObjective("aeria");

            if (old != null) {
                old.unregister();
            }

            // Regista o título superior do quadro (Sidebar)
            try {

                this.objective = (Objective) Scoreboard.class
                        .getMethod(
                                "registerNewObjective",
                                String.class,
                                String.class,
                                String.class
                        )
                        .invoke(
                                this.scoreboard,
                                "aeria",
                                "dummy",
                                "aeria"
                        );

            } catch (Throwable t) {

                this.objective = this.scoreboard.registerNewObjective(
                        "aeria",
                        "dummy"
                );
            }

            if (this.objective != null) {
                this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }

            // Envia o quadro vazio para o jogador
            player.setScoreboard(this.scoreboard);

        } else {

            this.objective = null;
        }
    }

    // ------------------------------------------------------------------------
    // Atualiza apenas o título
    // ------------------------------------------------------------------------
    public void updateTitle(String title) {

        if (objective != null) {
            objective.setDisplayName(title);
        }
    }

    // ------------------------------------------------------------------------
    // Atualiza as linhas
    // ------------------------------------------------------------------------
    public void updateLines(List<String> lines) {

        if (scoreboard == null || objective == null) {
            return;
        }

        int size = Math.min(lines.size(), 15);

        for (int i = 0; i < 15; i++) {

            // Entrada única da linha
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

                // =========================================================
                // SERVIDORES LEGACY (1.8)
                // =========================================================
                if (isLegacyServer) {

                    if (line.length() > 16) {

                        String prefix = getSafeSubstring(line, 0, 16);

                        String lastColors = ChatColor.getLastColors(prefix);

                        String suffix = lastColors + getSafeSubstring(
                                line,
                                prefix.length(),
                                prefix.length() + 16 - lastColors.length()
                        );

                        team.setPrefix(prefix);
                        team.setSuffix(suffix);

                    } else {

                        team.setPrefix(line);
                        team.setSuffix("");
                    }

                } else {

                    // =========================================================
                    // SERVIDORES MODERNOS
                    // =========================================================
                    try {

                        team.setPrefix(line);
                        team.setSuffix("");

                    } catch (Throwable ignored) {
                    }
                }

                objective.getScore(entry).setScore(i + 1);

                // =========================================================
                // Remove números vermelhos (Paper moderno)
                // =========================================================
                try {

                    java.lang.reflect.Method method = objective
                            .getScore(entry)
                            .getClass()
                            .getMethod(
                                    "numberFormat",
                                    Class.forName(
                                            "io.papermc.paper.scoreboard.numbers.NumberFormat"
                                    )
                            );

                    Object blankFormat = Class
                            .forName(
                                    "io.papermc.paper.scoreboard.numbers.NumberFormat"
                            )
                            .getMethod("blank")
                            .invoke(null);

                    method.invoke(
                            objective.getScore(entry),
                            blankFormat
                    );

                } catch (Exception ignored) {
                }

            } else {

                // Limpa linhas antigas
                if (team != null) {

                    try {
                        team.unregister();
                    } catch (Throwable ignored) {
                    }
                }

                scoreboard.resetScores(entry);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Evita cortar código de cor no meio
    // ------------------------------------------------------------------------
    private String getSafeSubstring(String text, int start, int end) {

        if (start >= text.length()) {
            return "";
        }

        if (end > text.length()) {
            end = text.length();
        }

        String result = text.substring(start, end);

        // Evita cortar "§"
        if (result.endsWith("§")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    // ------------------------------------------------------------------------
    // Remove o scoreboard
    // ------------------------------------------------------------------------
    public void delete() {

        if (player != null && player.isOnline()) {

            ScoreboardManager manager = Bukkit.getScoreboardManager();

            if (manager != null) {
                player.setScoreboard(manager.getMainScoreboard());
            }
        }

        if (scoreboard != null) {

            for (Team team : scoreboard.getTeams()) {

                try {
                    team.unregister();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}