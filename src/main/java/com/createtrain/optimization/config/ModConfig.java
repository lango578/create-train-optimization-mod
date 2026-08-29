package com.createtrain.optimization.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateTrainOptimization-Config");
    private static Path configPath;
    public static String profileMode = "auto";
    public static boolean enableSableLockFix = true;
    public static boolean enableAsyncChunkIO = true;
    public static boolean enableConcurrentChunkLoad = true;
    public static boolean enableMultithreadEntityTick = true;
    public static int ioThreadPoolSize = 2;
    public static int chunkLoadThreadPoolSize = 2;
    public static int entityThreadPoolSize = 2;
    public static int detectedCores = 4;
    public static boolean enableHud = true;
    public static String hudPosition = "top_left";
    public static int hudXOffset = 10;
    public static int hudYOffset = 10;
    public static boolean showFps = true;
    public static boolean showCpu = true;
    public static boolean showRam = true;
    public static boolean showGpu = false; // disabled by default (GPU data unreliable on some drivers)
    public static boolean showOptimizations = true;
    public static float hudScale = 1.0f;
    public static int hudBgAlpha = 170;
    public static boolean showBars = true;

    private ModConfig() {}

    public static void loadConfig(Path path) {
        configPath = path;
        detectedCores = Runtime.getRuntime().availableProcessors();
        readConfig();
        autoTuneHardware();
        saveConfig();
    }

    public static void reloadConfig() {
        if (configPath != null) {
            readConfig();
            autoTuneHardware();
            saveConfig();
        }
    }

    public static void autoTuneHardware() {
        detectedCores = Runtime.getRuntime().availableProcessors();
        int workers = "low".equalsIgnoreCase(profileMode) ? 2 : Math.max(2, detectedCores - 2);
        if ("high".equalsIgnoreCase(profileMode)) workers = Math.max(4, detectedCores - 1);
        ioThreadPoolSize = workers;
        chunkLoadThreadPoolSize = workers;
        entityThreadPoolSize = workers;
    }

    private static void readConfig() {
        if (configPath == null || !Files.exists(configPath)) return;
        try {
            for (String line : Files.readAllLines(configPath)) {
                String[] pair = line.trim().split("=", 2);
                if (pair.length != 2) continue;
                String key = pair[0].trim();
                String value = pair[1].trim().replace("\"", "");
                if (key.equals("profile_mode")) profileMode = value;
                else if (key.equals("enable_sable_lock_fix")) enableSableLockFix = Boolean.parseBoolean(value);
                else if (key.equals("enable_async_chunk_io")) enableAsyncChunkIO = Boolean.parseBoolean(value);
                else if (key.equals("enable_concurrent_chunk_load")) enableConcurrentChunkLoad = Boolean.parseBoolean(value);
                else if (key.equals("enable_multithread_entity_tick")) enableMultithreadEntityTick = Boolean.parseBoolean(value);
                else if (key.equals("enable_hud")) enableHud = Boolean.parseBoolean(value);
                else if (key.equals("hud_position")) hudPosition = value;
                else if (key.equals("hud_x_offset")) hudXOffset = Integer.parseInt(value);
                else if (key.equals("hud_y_offset")) hudYOffset = Integer.parseInt(value);
                else if (key.equals("show_fps")) showFps = Boolean.parseBoolean(value);
                else if (key.equals("show_cpu")) showCpu = Boolean.parseBoolean(value);
                else if (key.equals("show_ram")) showRam = Boolean.parseBoolean(value);
                else if (key.equals("show_gpu")) showGpu = Boolean.parseBoolean(value);
                else if (key.equals("show_optimizations")) showOptimizations = Boolean.parseBoolean(value);
                else if (key.equals("hud_scale")) hudScale = Float.parseFloat(value);
                else if (key.equals("hud_bg_alpha")) hudBgAlpha = Integer.parseInt(value);
                else if (key.equals("show_bars")) showBars = Boolean.parseBoolean(value);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to read optimization config", e);
        }
    }

    public static void saveConfig() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath,
                "profile_mode = \"" + profileMode + "\"\n" +
                "enable_sable_lock_fix = " + enableSableLockFix + "\n" +
                "enable_async_chunk_io = " + enableAsyncChunkIO + "\n" +
                "enable_concurrent_chunk_load = " + enableConcurrentChunkLoad + "\n" +
                "enable_multithread_entity_tick = " + enableMultithreadEntityTick + "\n" +
                "enable_hud = " + enableHud + "\n" +
                "hud_position = \"" + hudPosition + "\"\n" +
                "hud_x_offset = " + hudXOffset + "\n" +
                "hud_y_offset = " + hudYOffset + "\n" +
                "show_fps = " + showFps + "\n" +
                "show_cpu = " + showCpu + "\n" +
                "show_ram = " + showRam + "\n" +
                "show_gpu = " + showGpu + "\n" +
                "show_optimizations = " + showOptimizations + "\n" +
                "hud_scale = " + hudScale + "\n" +
                "hud_bg_alpha = " + hudBgAlpha + "\n" +
                "show_bars = " + showBars + "\n");
        } catch (IOException e) {
            LOGGER.error("Unable to save optimization config", e);
        }
    }
}