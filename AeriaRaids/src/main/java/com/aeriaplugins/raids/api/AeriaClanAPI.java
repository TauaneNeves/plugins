package com.aeriaplugins.raids.api;

import java.util.UUID;

public class AeriaClanAPI {

    public enum ClanRole {
        NENHUMA(0, "Fechado para o Clan"),
        RECRUTA(1, "Recruta"),
        MEMBRO(2, "Membro"),
        OFICIAL(3, "Oficial"),
        VICE_LIDER(4, "Vice-Líder"),
        LIDER(5, "Líder");

        private final int peso;
        private final String display;

        ClanRole(int peso, String display) {
            this.peso = peso;
            this.display = display;
        }

        public int getPeso() { return peso; }
        public String getDisplay() { return display; }
    }

    public static ClanRole getRole(UUID jogador) {
        return ClanRole.MEMBRO;
    }

    public static boolean isMesmoClan(UUID dono, UUID jogador) {
        return false;
    }

    public static boolean hasPermission(UUID dono, UUID jogador, String rankNecessarioStr) {
        if (rankNecessarioStr == null || rankNecessarioStr.equals("NENHUMA")) return false;
        if (!isMesmoClan(dono, jogador)) return false;

        try {
            ClanRole rankNecessario = ClanRole.valueOf(rankNecessarioStr);
            ClanRole rankAtual = getRole(jogador);
            return rankAtual.getPeso() >= rankNecessario.getPeso();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String getProximoRank(String atual) {
        switch (atual) {
            case "NENHUMA": return "RECRUTA";
            case "RECRUTA": return "MEMBRO";
            case "MEMBRO": return "OFICIAL";
            case "OFICIAL": return "VICE_LIDER";
            case "VICE_LIDER": return "LIDER";
            case "LIDER": return "NENHUMA";
            default: return "NENHUMA";
        }
    }
}