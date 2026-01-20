package com.safari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SafariConfig {
    private static SafariConfig INSTANCE;
    private static File currentConfigFile;

    // Session
    public int sessionTimeMinutes = 2; // Reduced for testing
    public int initialSafariBalls = 25;
    public String safariBallItem = "safari:safari_ball";
    public boolean carryOverSafariBalls = false;
    public boolean logoutClearInventory = true;
    public boolean allowMultiplayerSessions = true;

    // Economy
    public int entrancePrice = 500;
    public int pack5BallsPrice = 150;
    public int pack10BallsPrice = 250;
    public int maxBallsPurchasable = 20;
    
    // Capture Rates
    public double commonCatchRate = 0.22;
    public double uncommonCatchRate = 0.15;
    public double rareCatchRate = 0.08;
    
    // Timezone
    public String resetTimezone = "Europe/Paris";

    // Dimension
    public int dimensionSize = 2000;
    public int coreRadius = 350; 
    public List<String> allowedBiomes = Arrays.asList(
            "minecraft:plains", "minecraft:savanna", "minecraft:jungle", 
            "minecraft:swamp", "minecraft:forest", "minecraft:badlands"
    );
    public double spawnRateMultiplier = 1.5;

    public static SafariConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new SafariConfig(); // Default fallback
        }
        return INSTANCE;
    }

    public static void load() {
        if (currentConfigFile != null) load(currentConfigFile);
    }

    public static void load(File worldDir) {
        File configFile = new File(worldDir, "safari-config.json");
        currentConfigFile = configFile;
        
        if (!configFile.exists()) {
            INSTANCE = new SafariConfig();
            save();
            return;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            INSTANCE = gson.fromJson(reader, SafariConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            INSTANCE = new SafariConfig();
        }
    }

    public static void save() {
        if (currentConfigFile == null) return;
        try (FileWriter writer = new FileWriter(currentConfigFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
