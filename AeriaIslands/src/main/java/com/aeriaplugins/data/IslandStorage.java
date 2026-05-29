package com.aeriaplugins.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class IslandStorage {

    private final File arquivo;
    private final FileConfiguration config;

    public IslandStorage(JavaPlugin plugin) {
        this.arquivo = new File(plugin.getDataFolder(), "islands.yml");
        if (!arquivo.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                arquivo.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(arquivo);
    }

    public int getProximoId() {
        int atual = config.getInt("contador-global-id", 0);
        int proximo = atual + 1;
        config.set("contador-global-id", proximo);
        salvar();
        return proximo;
    }

    public void criarNovaIlha(UUID uuid, String nomeIlha, Location loc, String dataCriacao) {
        String path = "jogadores." + uuid.toString() + "." + nomeIlha;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
        config.set(path + ".data", dataCriacao);
        config.set(path + ".nivel", 1);
        config.set(path + ".membros", new ArrayList<String>());
        salvar();
    }

    public void atualizarSpawnIlha(UUID uuid, String nomeIlha, Location loc) {
        String path = "jogadores." + uuid.toString() + "." + nomeIlha;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
        salvar();
    }

    public List<String> getNomesIlhas(UUID uuid) {
        String path = "jogadores." + uuid.toString();
        if (!config.contains(path) || config.getConfigurationSection(path) == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(config.getConfigurationSection(path).getKeys(false));
    }

    public Location getPrimeiraIlhaLocation(UUID uuid) {
        List<String> nomes = getNomesIlhas(uuid);
        if (nomes.isEmpty()) return null;
        return getLocalizacaoIlhaPorNome(uuid, nomes.get(0));
    }

    public Location getLocalizacaoIlhaPorNome(UUID uuid, String nomeIlha) {
        String path = "jogadores." + uuid.toString() + "." + nomeIlha;
        if (!config.contains(path)) return null;
        
        return new Location(
                Bukkit.getWorld(config.getString(path + ".world")),
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw", 0.0),
                (float) config.getDouble(path + ".pitch", 0.0)
        );
    }

    public int getNivelIlha(UUID uuid, String nomeIlha) {
        String path = "jogadores." + uuid.toString() + "." + nomeIlha + ".nivel";
        return config.getInt(path, 1);
    }

    public void setNivelIlha(UUID uuid, String nomeIlha, int nivel) {
        String path = "jogadores." + uuid.toString() + "." + nomeIlha + ".nivel";
        config.set(path, nivel);
        salvar();
    }

    public List<String> getMembrosIlha(UUID uuid, String nomeIlha) {
        String path = "jogadores." + uuid.toString() + "." + nomeIlha + ".membros";
        return config.getStringList(path);
    }

    public void adicionarMembroIlha(UUID uuid, String nomeIlha, UUID membroUuid) {
        String path = "jogadores." + uuid.toString() + "." + nomeIlha + ".membros";
        List<String> membros = config.getStringList(path);
        if (!membros.contains(membroUuid.toString())) {
            membros.add(membroUuid.toString());
            config.set(path, membros);
            salvar();
        }
    }

    public String getDataCriacao(UUID uuid, String nomeIlha) {
        String path = "jogadores." + uuid.toString() + "." + nomeIlha + ".data";
        return config.getString(path, "Desconhecida");
    }

    public void removerIlhaPorNome(UUID uuid, String nomeIlha) {
        String path = "jogadores." + uuid.toString() + "." + nomeIlha;
        config.set(path, null);
        salvar();
    }

    public FileConfiguration getRawConfig() {
        return this.config;
    }

    private void salvar() {
        try {
            config.save(arquivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}