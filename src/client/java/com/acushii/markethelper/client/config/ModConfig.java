package com.acushii.markethelper.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("wynncraft-market-helper.json");

    public enum DisplayMode {
        INTEGER("Integer"),
        DECIMAL("Decimal");

        private final String name;

        DisplayMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public DisplayMode displayMode = DisplayMode.INTEGER;
    public ChatFormatting primaryColor = ChatFormatting.WHITE;
    public ChatFormatting secondaryColor = ChatFormatting.GRAY;
    public boolean globalToggle = true;

    private static ModConfig INSTANCE;

    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }

        return INSTANCE;
    }

    public static ModConfig load() {
        ModConfig config = null;

        if (Files.exists(CONFIG_PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (Exception e) {
                System.err.println("[Wynncraft Market Price Helper] Failed to load config, resetting to default: " + e.getMessage());
            }
        }

        if (config == null) {
            config = new ModConfig();
        } else {
            if (config.displayMode == null) config.displayMode = DisplayMode.INTEGER;
            if (config.primaryColor == null) config.primaryColor = ChatFormatting.WHITE;
            if (config.secondaryColor == null) config.secondaryColor = ChatFormatting.GRAY;
        }

        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("[Wynncraft Market Price Helper] Failed to write config: " + e.getMessage());
        }
    }
}