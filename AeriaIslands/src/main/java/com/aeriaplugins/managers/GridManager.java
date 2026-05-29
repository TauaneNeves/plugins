package com.aeriaplugins.managers;

import com.aeriaplugins.plugins.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class GridManager {

    // ALTERADO: Agora as ilhas serão sequenciadas dentro do mapa baixado
    private final String NOME_MUNDO_VOID = "skyblockv";
    private final int ESPACAMENTO_ILHAS = 1000; // Distância segura entre as bases no mapa
    private final int ALTURA_FIXA = 100;        // Mantém a colagem estável na camada 100

    public Location calcularProximaCoordenada() {
        World mundo = Bukkit.getWorld(NOME_MUNDO_VOID);
        
        // Se o Multiverse descarregar o mapa, força o carregamento automático
        if (mundo == null) {
            mundo = Bukkit.createWorld(new org.bukkit.WorldCreator(NOME_MUNDO_VOID));
        }
        
        if (mundo == null && !Bukkit.getWorlds().isEmpty()) {
            mundo = Bukkit.getWorlds().get(0);
        }

        // Obtém a quantidade de ilhas para não sobrepor uma base na outra
        int totalIlhasAtivas = Main.getInstance().getIslandStorage().getNomesIlhas(UUID.randomUUID()).size();
        if (totalIlhasAtivas < 0) totalIlhasAtivas = 0;
        
        // Alinhamento linear no eixo Z para distribuir os jogadores pelo mapa
        int proximoZ = totalIlhasAtivas * ESPACAMENTO_ILHAS;
        
        return new Location(mundo, 0, ALTURA_FIXA, proximoZ);
    }
    
    public UUID getDonoDaIlhaNaLocalizacao(Location loc) {
        return null;
    }

    public String getNomeDaIlhaNaLocalizacao(Location loc) {
        return "";
    }
}