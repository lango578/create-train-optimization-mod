package com.createtrain.optimization.hud;

import com.createtrain.optimization.config.ModConfig;
import com.mojang.blaze3d.platform.GlUtil;
import com.sun.management.OperatingSystemMXBean;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PerformanceHudOverlay {

    /** Lazily initialized; null until first HUD render. */
    private static volatile OperatingSystemMXBean osBean = null;
    /** Set to true if com.sun.management is unavailable (e.g. slim JRE), so the
     *  HUD keeps rendering and only the CPU line is reported as unavailable. */
    private static volatile boolean osBeanFailed = false;

    private static double cachedCpuUsage = 0.0;
    private static long lastCpuCheckTime = 0;
    private static String cachedGpuRenderer = null;

    // ============ GPU utilization via Windows PDH (GPU Engine counters) ============
    private interface Pdh extends Library {
        Pdh INSTANCE = Native.load("pdh", Pdh.class);
        int PDH_FMT_DOUBLE = 0x00000200;
        int PDH_CSTATUS_VALID_DATA = 0x00000000;
        int PDH_CSTATUS_NEW_DATA = 0x00000001;

        int PdhOpenQueryW(Pointer szDataSource, long dwUserData, PointerByReference phQuery);
        int PdhAddEnglishCounterW(Pointer hQuery, WString szFullCounterPath, long dwUserData, PointerByReference phCounter);
        int PdhCollectQueryData(Pointer hQuery);
        int PdhGetFormattedCounterValue(Pointer hCounter, int dwFormat, IntByReference lpdwType, PDH_FMT_COUNTERVALUE pValue);
        int PdhCloseQuery(Pointer hQuery);
        int PdhExpandWildCardPathW(Pointer szDataSource, WString szWildCardPath, char[] mszExpandedPathList, IntByReference pcchPathListLength, int dwFlags);
    }

    private static final class PDH_FMT_COUNTERVALUE extends Structure {
        public int CStatus;
        public double doubleValue;
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("CStatus", "doubleValue");
        }
    }

    private static Pointer gpuQuery = null;
    private static final List<Pointer> gpuCounters = new ArrayList<>();
    private static boolean gpuPdhInitialized = false;
    private static boolean gpuPdhFailed = false;
    private static double cachedGpuUtilization = -1.0;

    // ============ GPU VRAM usage (more reliable than utilization on AMD) ============
    private static Pointer vramQuery = null;
    private static Pointer vramCounter = null;
    private static boolean gpuVramInitialized = false;
    private static boolean gpuVramFailed = false;
    private static long totalVramBytes = 0;
    private static long usedVramBytes = 0;

    /** A HUD text line with an optional progress bar (barPercent in 0..100, or -1 = no bar). */
    private static final class HudLine {
        final String text;
        final double barPercent;
        final int barColor;
        HudLine(String text) { this(text, -1, 0); }
        HudLine(String text, double barPercent, int barColor) {
            this.text = text;
            this.barPercent = barPercent;
            this.barColor = barColor;
        }
    }

    /**
     * Returns CPU load in 0.0-1.0 range, or -1.0 when unavailable.
     * Never throws: guards against environments without jdk.management module
     * (ClassCastError/NoClassDefFoundError) so the whole HUD is not lost.
     */
    private static double getCpuLoadSafe() {
        if (osBeanFailed) return -1.0;
        if (osBean == null) {
            try {
                osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            } catch (Throwable t) {
                osBeanFailed = true;
                return -1.0;
            }
        }
        try {
            double load = osBean.getCpuLoad();
            return (load >= 0) ? load : -1.0;
        } catch (Throwable t) {
            osBeanFailed = true;
            return -1.0;
        }
    }

    /** Direct file logging (bypasses log4j, which does not write for some launch paths). */
    private static void logDebug(String msg) {
        try {
            Path p = FMLPaths.GAMEDIR.get().resolve("createtrain_debug.log");
            Files.writeString(p, "[" + java.time.LocalTime.now() + "] " + msg + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    /** Initialize PDH query and add all "engtype_3D" GPU engine counters. */
    private static void initGpuPdh() {
        if (gpuPdhFailed) return;
        try {
            PointerByReference queryRef = new PointerByReference();
            if (Pdh.INSTANCE.PdhOpenQueryW(null, 0, queryRef) != 0) {
                gpuPdhFailed = true;
                logDebug("GPU PDH: PdhOpenQueryW failed");
                return;
            }
            gpuQuery = queryRef.getValue();

            String wild = "\\GPU Engine(*)\\Utilization Percentage";
            IntByReference size = new IntByReference(0);
            Pdh.INSTANCE.PdhExpandWildCardPathW(null, new WString(wild), null, size, 0);
            if (size.getValue() <= 0) {
                gpuPdhFailed = true;
                logDebug("GPU PDH: expand wildcard returned size=" + size.getValue());
                return;
            }
            char[] buffer = new char[size.getValue()];
            if (Pdh.INSTANCE.PdhExpandWildCardPathW(null, new WString(wild), buffer, size, 0) != 0) {
                gpuPdhFailed = true;
                logDebug("GPU PDH: PdhExpandWildCardPathW failed, size=" + size.getValue());
                return;
            }

            String joined = new String(buffer);
            String[] paths = joined.split("\0");
            int total = 0;
            for (String p : paths) {
                if (p.isBlank()) continue;
                total++;
                if (!p.toLowerCase().contains("engtype_3d")) continue;
                PointerByReference counterRef = new PointerByReference();
                if (Pdh.INSTANCE.PdhAddEnglishCounterW(gpuQuery, new WString(p), 0, counterRef) == 0) {
                    gpuCounters.add(counterRef.getValue());
                }
            }
            logDebug("GPU PDH: expanded " + total + " engine paths, matched " + gpuCounters.size() + " engtype_3D");
            if (gpuCounters.isEmpty()) {
                gpuPdhFailed = true;
                logDebug("GPU PDH: no engtype_3D counters found");
                return;
            }
            gpuPdhInitialized = true;
            // Baseline sample: first PdhCollectQueryData returns PDH_INVALID_DATA.
            int baseline = Pdh.INSTANCE.PdhCollectQueryData(gpuQuery);
            logDebug("GPU PDH: initialized OK, baseline collect=" + baseline);
        } catch (Throwable t) {
            gpuPdhFailed = true;
            logDebug("GPU PDH: init threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    /** Refresh GPU 3D utilization (max across all 3D engines), or -1 if unavailable. */
    private static void collectGpuUtilization() {
        if (gpuPdhFailed) {
            cachedGpuUtilization = -1.0;
            return;
        }
        if (!gpuPdhInitialized) {
            initGpuPdh();
            if (gpuPdhFailed) {
                cachedGpuUtilization = -1.0;
                return;
            }
        }
        int ret = Pdh.INSTANCE.PdhCollectQueryData(gpuQuery);
        if (ret != 0) {
            logDebug("GPU PDH: collect ret=" + ret + " (first sample or error)");
            return; // no valid sample yet, keep previous value
        }
        double max = 0.0;
        for (Pointer counter : gpuCounters) {
            try {
                IntByReference type = new IntByReference();
                PDH_FMT_COUNTERVALUE val = new PDH_FMT_COUNTERVALUE();
                int r = Pdh.INSTANCE.PdhGetFormattedCounterValue(counter, Pdh.PDH_FMT_DOUBLE, type, val);
                if (r == 0 && (val.CStatus == Pdh.PDH_CSTATUS_VALID_DATA || val.CStatus == Pdh.PDH_CSTATUS_NEW_DATA)) {
                    if (val.doubleValue > max) max = val.doubleValue;
                }
            } catch (Throwable t) {
                // ignore per-counter errors
            }
        }
        cachedGpuUtilization = max > 0 ? Math.min(100.0, max) : 0.0;
        logDebug("GPU PDH: utilization=" + String.format("%.1f", cachedGpuUtilization));
    }

    /** Read total VRAM from registry (qwMemorySize) and add Dedicated Usage counter. */
    private static void initGpuVram() {
        if (gpuVramFailed) return;
        try {
            Long total = Advapi32Util.registryGetLongValue(
                    WinReg.HKEY_LOCAL_MACHINE,
                    "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}\\0000",
                    "HardwareInformation.qwMemorySize");
            if (total != null && total > 0) {
                totalVramBytes = total;
            }
            PointerByReference queryRef = new PointerByReference();
            if (Pdh.INSTANCE.PdhOpenQueryW(null, 0, queryRef) != 0) {
                gpuVramFailed = true;
                logDebug("GPU VRAM: PdhOpenQueryW failed");
                return;
            }
            vramQuery = queryRef.getValue();
            PointerByReference counterRef = new PointerByReference();
            if (Pdh.INSTANCE.PdhAddEnglishCounterW(vramQuery,
                    new WString("\\GPU Adapter Memory(*)\\Dedicated Usage"), 0, counterRef) == 0) {
                vramCounter = counterRef.getValue();
                gpuVramInitialized = true;
                Pdh.INSTANCE.PdhCollectQueryData(vramQuery); // baseline
                logDebug("GPU VRAM: initialized OK, total=" + totalVramBytes);
            } else {
                gpuVramFailed = true;
                logDebug("GPU VRAM: add Dedicated Usage counter failed");
            }
        } catch (Throwable t) {
            gpuVramFailed = true;
            logDebug("GPU VRAM: init threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static void collectGpuVram() {
        if (gpuVramFailed) return;
        if (!gpuVramInitialized) {
            initGpuVram();
            if (gpuVramFailed) return;
        }
        int ret = Pdh.INSTANCE.PdhCollectQueryData(vramQuery);
        if (ret != 0) return;
        try {
            IntByReference type = new IntByReference();
            PDH_FMT_COUNTERVALUE val = new PDH_FMT_COUNTERVALUE();
            int r = Pdh.INSTANCE.PdhGetFormattedCounterValue(vramCounter, Pdh.PDH_FMT_DOUBLE, type, val);
            if (r == 0 && (val.CStatus == Pdh.PDH_CSTATUS_VALID_DATA || val.CStatus == Pdh.PDH_CSTATUS_NEW_DATA)) {
                if (val.doubleValue > 0) {
                    usedVramBytes = (long) val.doubleValue;
                }
            }
        } catch (Throwable t) {
            gpuVramFailed = true;
        }
    }

    private static long lastRenderLog = 0;

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!ModConfig.enableHud) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        long now = System.currentTimeMillis();
        if (now - lastRenderLog > 10000) {
            lastRenderLog = now;
            logDebug("HUD render OK: screen=" + (mc.screen == null ? "world" : mc.screen.getClass().getSimpleName())
                    + " showGpu=" + ModConfig.showGpu + " gpuUtil=" + cachedGpuUtilization);
        }

        if (now - lastCpuCheckTime > 1000) {
            double load = getCpuLoadSafe();
            cachedCpuUsage = (load >= 0) ? load * 100.0 : 0.0;
            lastCpuCheckTime = now;
            collectGpuUtilization();
            collectGpuVram();
        }

        if (cachedGpuRenderer == null) {
            try {
                String renderer = GlUtil.getRenderer();
                cachedGpuRenderer = renderer == null || renderer.isBlank() ? "未知" : renderer;
            } catch (Throwable t) {
                cachedGpuRenderer = "未知";
            }
        }

        // Memory calculations
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        double ramPercent = (double) usedMemory / maxMemory * 100.0;

        // Collect HUD Info Lines (with optional progress bars)
        List<HudLine> lines = new ArrayList<>();
        lines.add(new HudLine("§6§l[ Create Train 性能引擎 HUD ]"));

        if (ModConfig.showFps) {
            int fps = mc.getFps();
            String fpsColor = fps >= 50 ? "§a" : (fps >= 30 ? "§e" : "§c");
            lines.add(new HudLine(String.format("§7帧率 (FPS): %s%d FPS", fpsColor, fps)));
        }

        if (ModConfig.showCpu) {
            String cpuColor = cachedCpuUsage < 70 ? "§a" : (cachedCpuUsage < 90 ? "§e" : "§c");
            lines.add(new HudLine(
                    String.format("§7CPU 占用: %s%.1f%% §7(%d 核)", cpuColor, cachedCpuUsage, ModConfig.detectedCores),
                    ModConfig.showBars ? cachedCpuUsage : -1.0, barColor(cachedCpuUsage)));
        }

        if (ModConfig.showRam) {
            String ramColor = ramPercent < 75 ? "§a" : (ramPercent < 90 ? "§e" : "§c");
            lines.add(new HudLine(
                    String.format("§7内存占用: %s%.1f%% §7(%dMB / %dMB)",
                            ramColor, ramPercent, usedMemory / 1048576, maxMemory / 1048576),
                    ModConfig.showBars ? ramPercent : -1.0, barColor(ramPercent)));
        }

        if (ModConfig.showGpu) {
            String gpuLine;
            if (totalVramBytes > 0 && usedVramBytes > 0) {
                double vramPct = (double) usedVramBytes / totalVramBytes * 100.0;
                String vramColor = vramPct < 75 ? "§a" : (vramPct < 90 ? "§e" : "§c");
                gpuLine = String.format("§7显卡: §b%s §7显存: %s%.1f / %dMB (%.0f%%)",
                        cachedGpuRenderer, vramColor,
                        usedVramBytes / 1048576.0, totalVramBytes / 1048576, vramPct);
                lines.add(new HudLine(gpuLine,
                        ModConfig.showBars ? vramPct : -1.0, barColor(vramPct)));
            } else if (cachedGpuUtilization >= 0) {
                String gpuColor = cachedGpuUtilization < 70 ? "§a" : (cachedGpuUtilization < 90 ? "§e" : "§c");
                gpuLine = String.format("§7显卡: §b%s §7占用率: %s%.1f%%", cachedGpuRenderer, gpuColor, cachedGpuUtilization);
                lines.add(new HudLine(gpuLine,
                        ModConfig.showBars ? cachedGpuUtilization : -1.0, barColor(cachedGpuUtilization)));
            } else {
                gpuLine = String.format("§7显卡: §b%s", cachedGpuRenderer);
                lines.add(new HudLine(gpuLine));
            }
        }

        if (ModConfig.showOptimizations) {
            lines.add(new HudLine("§e----------------------------------"));
            lines.add(new HudLine("§7运行优化项:"));
            lines.add(new HudLine((ModConfig.enableSableLockFix ? " §a✔" : " §c✘") + " Sable 物理防爆锁 (RwLock)"));
            lines.add(new HudLine((ModConfig.enableAsyncChunkIO ? " §a✔" : " §c✘")
                    + String.format(" 多线程区块存盘 (%d 线程)", ModConfig.ioThreadPoolSize)));
            lines.add(new HudLine((ModConfig.enableConcurrentChunkLoad ? " §a✔" : " §c✘") + " 并发区块加载"));
            lines.add(new HudLine((ModConfig.enableMultithreadEntityTick ? " §a✔" : " §c✘") + " 实体与机械并发 Tick"));
            lines.add(new HudLine(String.format(" §a✔ 硬件档位: [%s]", ModConfig.profileMode.toUpperCase())));
        }

        // Whole-HUD scaling via PoseStack transform
        float scale = Math.max(0.4f, Math.min(2.0f, ModConfig.hudScale));
        int screenWidth = (int) (graphics.guiWidth() / scale);
        int screenHeight = (int) (graphics.guiHeight() / scale);

        int padding = 6;
        int lineHeight = 10;
        int barAreaHeight = 10;
        int barCount = 0;
        int maxLineWidth = 0;
        for (HudLine line : lines) {
            int w = mc.font.width(line.text);
            if (w > maxLineWidth) maxLineWidth = w;
            if (line.barPercent >= 0) barCount++;
        }

        int boxWidth = maxLineWidth + padding * 2;
        int boxHeight = lines.size() * lineHeight + barCount * barAreaHeight + padding * 2;

        int startX = ModConfig.hudXOffset;
        int startY = ModConfig.hudYOffset;
        if ("top_right".equalsIgnoreCase(ModConfig.hudPosition)) {
            startX = screenWidth - boxWidth - ModConfig.hudXOffset;
        } else if ("bottom_left".equalsIgnoreCase(ModConfig.hudPosition)) {
            startY = screenHeight - boxHeight - ModConfig.hudYOffset;
        } else if ("bottom_right".equalsIgnoreCase(ModConfig.hudPosition)) {
            startX = screenWidth - boxWidth - ModConfig.hudXOffset;
            startY = screenHeight - boxHeight - ModConfig.hudYOffset;
        }
        startX = Math.max(0, startX);
        startY = Math.max(0, startY);

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);

        // Background box with configurable transparency (0 = fully transparent)
        int bgColor = ((ModConfig.hudBgAlpha & 0xFF) << 24) | 0x000000;
        graphics.fill(startX, startY, startX + boxWidth, startY + boxHeight, bgColor);
        graphics.fill(startX - 1, startY - 1, startX + boxWidth + 1, startY, 0xFFFFAA00); // Top border accent

        int currentY = startY + padding;
        int barWidth = boxWidth - padding * 2;
        for (HudLine line : lines) {
            graphics.drawString(mc.font, line.text, startX + padding, currentY, 0xFFFFFF, false);
            currentY += lineHeight;
            if (line.barPercent >= 0) {
                drawBar(graphics, startX + padding, currentY, barWidth, line.barPercent, line.barColor);
                currentY += barAreaHeight;
            }
        }
        graphics.pose().popPose();
    }

    private static int barColor(double percent) {
        if (percent < 70) return 0xFF55FF55;
        if (percent < 90) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int maxWidth, double percent, int fillColor) {
        int barHeight = 3;
        graphics.fill(x, y, x + maxWidth, y + barHeight, 0xAA000000);
        int w = (int) (maxWidth * Math.max(0.0, Math.min(1.0, percent / 100.0)));
        if (w > 0) {
            graphics.fill(x, y, x + w, y + barHeight, fillColor);
        }
    }
}
