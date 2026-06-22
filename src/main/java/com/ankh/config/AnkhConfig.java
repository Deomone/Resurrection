package com.ankh.config;

import com.ankh.AnkhResurrection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AnkhConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "ankh-resurrection.json";

    public float suspiciousSandChance = 0.01f;

    public float pyramidChestChance = 0.05f;

    public int sessionMinutes = 20;

    public double guardianSoftLeashRadius = 10.0;

    public double guardianHardLeashRadius = 30.0;

    public double observerRadius = 10.0;

    public int resurrectionAbsorptionSeconds = 60;

    public int sessionTicks() { return Math.max(1, sessionMinutes) * 60 * 20; }
    public int resurrectionAbsorptionTicks() { return Math.max(0, resurrectionAbsorptionSeconds) * 20; }

    private static AnkhConfig instance;

    public static AnkhConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static AnkhConfig reload() {
        instance = load();
        return instance;
    }

    private static AnkhConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        AnkhConfig config = new AnkhConfig();
        try {
            if (Files.exists(path)) {
                String json = Files.readString(path);
                AnkhConfig loaded = GSON.fromJson(json, AnkhConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            }
        } catch (IOException | RuntimeException e) {
            AnkhResurrection.LOGGER.warn("[Ankh] Failed to load config, using defaults: {}", e.toString());
        }

        config.validate();

        save(config, path);
        return config;
    }

    private void validate() {
        suspiciousSandChance = clamp01(suspiciousSandChance);
        pyramidChestChance = clamp01(pyramidChestChance);
        sessionMinutes = clampInt(sessionMinutes, 1, 24 * 60);
        guardianSoftLeashRadius = clampDouble(guardianSoftLeashRadius, 1.0, 256.0);
        guardianHardLeashRadius = clampDouble(guardianHardLeashRadius, guardianSoftLeashRadius, 512.0);
        observerRadius = clampDouble(observerRadius, 1.0, 256.0);
        resurrectionAbsorptionSeconds = clampInt(resurrectionAbsorptionSeconds, 0, 86400);
    }

    private static void save(AnkhConfig config, Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config));
        } catch (IOException e) {
            AnkhResurrection.LOGGER.warn("[Ankh] Failed to write config: {}", e.toString());
        }
    }

    private static float clamp01(float v) {
        if (Float.isNaN(v) || v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static int clampInt(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static double clampDouble(double v, double min, double max) {
        if (Double.isNaN(v) || v < min) return min;
        if (v > max) return max;
        return v;
    }
}
