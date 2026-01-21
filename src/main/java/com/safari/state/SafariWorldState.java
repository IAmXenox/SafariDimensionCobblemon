package com.safari.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Random;

public class SafariWorldState {
    private static SafariWorldState INSTANCE;
    private static final File STATE_FILE = FabricLoader.getInstance().getConfigDir().resolve("safari-state.json").toFile();

    public long currentDailySeed;
    public String lastResetDate; // YYYY-MM-DD
    public int centerX = 0;
    public int centerZ = 0;

    public static SafariWorldState get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (!STATE_FILE.exists()) {
            INSTANCE = new SafariWorldState();
            INSTANCE.resetDailySeed();
            save();
            return;
        }
        try (FileReader reader = new FileReader(STATE_FILE)) {
            INSTANCE = new Gson().fromJson(reader, SafariWorldState.class);
        } catch (IOException e) {
            e.printStackTrace();
            INSTANCE = new SafariWorldState();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(STATE_FILE)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetDailySeed() {
        this.currentDailySeed = new Random().nextLong();
        this.lastResetDate = LocalDate.now(ZoneId.of(com.safari.config.SafariConfig.get().resetTimezone)).toString();
        this.centerX = 0;
        this.centerZ = 0;
        save();
    }
    
    public boolean needsReset() {
        String today = LocalDate.now(ZoneId.of(com.safari.config.SafariConfig.get().resetTimezone)).toString();
        return !today.equals(lastResetDate);
    }
}
