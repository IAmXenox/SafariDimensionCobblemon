package com.safari.logic;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SafariSpawnRarity {
    private static final String[] POOL_FILES = {
            "common.json",
            "ucommon.json",
            "rare.json",
            "ultra_rare.json",
            "cafe.json",
            "enhanced_celestials.json"
    };
    private static final Map<String, String> BUCKET_BY_POKEMON = new HashMap<>();
    private static final Map<String, Integer> BUCKET_RANK = Map.of(
            "common", 0,
            "uncommon", 1,
            "rare", 2,
            "ultra-rare", 3
    );

    static {
        loadPools();
    }

    private SafariSpawnRarity() {
    }

    public static String getBucket(PokemonEntity entity) {
        String name = entity.getPokemon().getSpecies().getName().toString().toLowerCase(Locale.ROOT);
        int colon = name.indexOf(':');
        if (colon >= 0) {
            name = name.substring(colon + 1);
        }

        String bucket = BUCKET_BY_POKEMON.get(name);
        if (bucket != null) {
            return bucket;
        }

        int space = name.indexOf(' ');
        if (space > 0) {
            bucket = BUCKET_BY_POKEMON.get(name.substring(0, space));
        }
        return bucket;
    }

    private static void loadPools() {
        ClassLoader loader = SafariSpawnRarity.class.getClassLoader();
        Gson gson = new Gson();
        for (String file : POOL_FILES) {
            String path = "data/cobblemon/spawn_pool_world/safari/" + file;
            try (InputStreamReader reader = new InputStreamReader(loader.getResourceAsStream(path))) {
                if (reader == null) {
                    continue;
                }

                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray spawns = root.getAsJsonArray("spawns");
                if (spawns == null) {
                    continue;
                }

                for (int i = 0; i < spawns.size(); i++) {
                    JsonObject spawn = spawns.get(i).getAsJsonObject();
                    if (!spawn.has("pokemon") || !spawn.has("bucket")) {
                        continue;
                    }
                    String pokemon = spawn.get("pokemon").getAsString().toLowerCase(Locale.ROOT);
                    String bucket = spawn.get("bucket").getAsString().toLowerCase(Locale.ROOT);
                    register(pokemon, bucket);

                    int space = pokemon.indexOf(' ');
                    if (space > 0) {
                        register(pokemon.substring(0, space), bucket);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void register(String pokemon, String bucket) {
        if (pokemon.isEmpty()) {
            return;
        }
        String existing = BUCKET_BY_POKEMON.get(pokemon);
        if (existing == null) {
            BUCKET_BY_POKEMON.put(pokemon, bucket);
            return;
        }

        int existingRank = BUCKET_RANK.getOrDefault(existing, 0);
        int nextRank = BUCKET_RANK.getOrDefault(bucket, 0);
        if (nextRank > existingRank) {
            BUCKET_BY_POKEMON.put(pokemon, bucket);
        }
    }
}
