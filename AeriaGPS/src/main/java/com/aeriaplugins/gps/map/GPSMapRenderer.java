package com.aeriaplugins.gps.map;

import com.aeriaplugins.gps.AeriaGPS;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.*;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GPSMapRenderer extends MapRenderer {
    
    private final Random random = new Random();

    @SuppressWarnings("deprecation")
    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (random.nextInt(100) < 5) {
            for (int x = 0; x < 128; x++) {
                for (int z = 0; z < 128; z++) {
                    byte cor = random.nextBoolean() ? MapPalette.GRAY_1 : (random.nextBoolean() ? MapPalette.GRAY_2 : MapPalette.WHITE);
                    canvas.setPixel(x, z, cor);
                }
            }
            return;
        }

        byte corFundo = MapPalette.matchColor(235, 235, 240);
        byte corGrade = MapPalette.matchColor(200, 200, 210);
        
        for (int x = 0; x < 128; x++) {
            for (int z = 0; z < 128; z++) {
                if (x % 32 == 0 || z % 32 == 0) canvas.setPixel(x, z, corGrade);
                else canvas.setPixel(x, z, corFundo);
            }
        }

        double yaw = Math.toRadians(player.getLocation().getYaw());
        desenharBussola(canvas, yaw);

        Map<String, Location> ilhas = AeriaGPS.getInstance().getIslandManager().getIslands();
        double escala = 5.0; 
        int raioMaximo = 54;

        // Bloqueio rigoroso: Lendo as ilhas desbloqueadas no PersistentDataContainer do jogador
        // Lendo as ilhas que este jogador específico desbloqueou
        String unlockedStr = player.getPersistentDataContainer().getOrDefault(AeriaGPS.getInstance().getItemManager().unlockedKey, PersistentDataType.STRING, "");
        List<String> unlockedList = Arrays.asList(unlockedStr.toLowerCase().split(";"));

        for (Map.Entry<String, Location> entry : ilhas.entrySet()) {
            Location loc = entry.getValue();
            if (loc.getWorld() == null || !loc.getWorld().equals(player.getWorld())) continue;

            // Filtro: Se o jogador não tem o papel dessa ilha, ela não aparece no radar.
            if (!unlockedList.contains(entry.getKey().toLowerCase())) continue;

            double dx = loc.getX() - player.getLocation().getX();
            double dz = loc.getZ() - player.getLocation().getZ();
            int distR = (int) Math.sqrt(dx * dx + dz * dz);

            double rotX = -dx * Math.cos(yaw) - dz * Math.sin(yaw);
            double rotZ = dx * Math.sin(yaw) - dz * Math.cos(yaw);

            int px = 64 + (int) (rotX / escala);
            int pz = 64 + (int) (rotZ / escala);

            if (Math.sqrt(Math.pow(px - 64, 2) + Math.pow(pz - 64, 2)) > raioMaximo) {
                double angulo = Math.atan2(pz - 64, px - 64);
                px = 64 + (int) (raioMaximo * Math.cos(angulo));
                pz = 64 + (int) (raioMaximo * Math.sin(angulo));
            }

            desenharIconeIlha(canvas, px, pz);
            canvas.drawText(px + 4, pz - 4, MinecraftFont.Font, entry.getKey());
            canvas.drawText(px + 4, pz + 4, MinecraftFont.Font, distR + "m");
        }

        desenharTrianguloJogador(canvas);
    }

    private void desenharIconeIlha(MapCanvas canvas, int px, int pz) {
        byte borda = MapPalette.matchColor(20, 100, 200);
        byte preenchimento = MapPalette.matchColor(120, 180, 255);
        byte vazado = MapPalette.matchColor(235, 235, 240); // Fundo do mapa para o buraco central
        
        String[] pixelArt = {
            "  BBB  ",
            " BPPPB ",
            "BPBBBPB",
            "BPBVBPB",
            "BPBBBPB",
            " BPPPB ",
            "  BPB  ",
            "   B   "
        };

        int startX = px - 3;
        int startZ = pz - 7; // Ajustado para a ponta do pino (bottom) ficar na coordenada exata (px, pz)

        for (int z = 0; z < pixelArt.length; z++) {
            for (int x = 0; x < pixelArt[z].length(); x++) {
                char c = pixelArt[z].charAt(x);
                if (c == ' ') continue;
                
                int drawX = startX + x;
                int drawZ = startZ + z;
                
                if (drawX >= 0 && drawX < 128 && drawZ >= 0 && drawZ < 128) {
                    if (c == 'B') canvas.setPixel(drawX, drawZ, borda);
                    else if (c == 'P') canvas.setPixel(drawX, drawZ, preenchimento);
                    else if (c == 'V') canvas.setPixel(drawX, drawZ, vazado);
                }
            }
        }
    }

    private void desenharTrianguloJogador(MapCanvas canvas) {
        byte borda = MapPalette.matchColor(20, 20, 20);
        byte preenchimento = MapPalette.matchColor(60, 60, 60);
        int cx = 64, cz = 64;

        String[] pixelArt = {
            "   B   ",
            "  BPB  ",
            "  BPB  ",
            " BPPPB ",
            " BPPPB ",
            "BPPPPPB",
            "BB P BB",
            "B  B  B"
        };

        int startX = cx - 3;
        int startZ = cz - 4; // Centraliza a seta de navegação na tela

        for (int z = 0; z < pixelArt.length; z++) {
            for (int x = 0; x < pixelArt[z].length(); x++) {
                char c = pixelArt[z].charAt(x);
                if (c == ' ') continue;
                
                int drawX = startX + x;
                int drawZ = startZ + z;
                
                if (drawX >= 0 && drawX < 128 && drawZ >= 0 && drawZ < 128) {
                    if (c == 'B') canvas.setPixel(drawX, drawZ, borda);
                    else if (c == 'P') canvas.setPixel(drawX, drawZ, preenchimento);
                }
            }
        }
    }
    private void desenharBussola(MapCanvas canvas, double yaw) {
        int cx = 16, cz = 16;
        double len = 6.0;
        double nRotX = len * Math.sin(yaw);
        double nRotZ = len * Math.cos(yaw);
        
        desenharLinha(canvas, cx, cz, cx + (int)nRotX, cz + (int)nRotZ, MapPalette.RED);
        canvas.drawText(cx + (int)(nRotX * 1.2) - 2, cz + (int)(nRotZ * 1.2) - 3, MinecraftFont.Font, "N");
    }

    private void desenharLinha(MapCanvas canvas, int x1, int z1, int x2, int z2, byte cor) {
        int dX = Math.abs(x2 - x1), dZ = Math.abs(z2 - z1);
        int sX = x1 < x2 ? 1 : -1, sZ = z1 < z2 ? 1 : -1;
        int err = dX - dZ;
        while (true) {
            if (x1 >= 0 && x1 < 128 && z1 >= 0 && z1 < 128) canvas.setPixel(x1, z1, cor);
            if (x1 == x2 && z1 == z2) break;
            int e2 = 2 * err;
            if (e2 > -dZ) { err -= dZ; x1 += sX; }
            if (e2 < dX) { err += dX; z1 += sZ; }
        }
    }
}