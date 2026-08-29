package com.createtrain.optimization.gui;

import com.createtrain.optimization.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PerformanceConfigScreen extends Screen {
    private final Screen parent;

    public PerformanceConfigScreen(Screen parent) {
        super(Component.literal("性能监控与优化设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y = 36, bw = 180, bh = 20, gap = 22;

        this.addRenderableWidget(Button.builder(
                Component.literal("HUD 监控: " + (ModConfig.enableHud ? "§a[开启]" : "§c[关闭]")),
                b -> {
                    ModConfig.enableHud = !ModConfig.enableHud;
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("HUD 监控: " + (ModConfig.enableHud ? "§a[开启]" : "§c[关闭]")));
                }).pos(cx - bw / 2, y).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("HUD 位置: " + getPosName(ModConfig.hudPosition)),
                b -> {
                    ModConfig.hudPosition = nextPos(ModConfig.hudPosition);
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("HUD 位置: " + getPosName(ModConfig.hudPosition)));
                }).pos(cx - bw / 2, y + gap).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("帧率(FPS): " + (ModConfig.showFps ? "§a✔" : "§c✘")),
                b -> {
                    ModConfig.showFps = !ModConfig.showFps;
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("帧率(FPS): " + (ModConfig.showFps ? "§a✔" : "§c✘")));
                }).pos(cx - bw - 4, y + gap * 2).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("CPU 占用: " + (ModConfig.showCpu ? "§a✔" : "§c✘")),
                b -> {
                    ModConfig.showCpu = !ModConfig.showCpu;
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("CPU 占用: " + (ModConfig.showCpu ? "§a✔" : "§c✘")));
                }).pos(cx + 4, y + gap * 2).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("内存占用: " + (ModConfig.showRam ? "§a✔" : "§c✘")),
                b -> {
                    ModConfig.showRam = !ModConfig.showRam;
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("内存占用: " + (ModConfig.showRam ? "§a✔" : "§c✘")));
                }).pos(cx - bw - 4, y + gap * 3).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("显卡信息: " + (ModConfig.showGpu ? "§a✔" : "§c✘")),
                b -> {
                    ModConfig.showGpu = !ModConfig.showGpu;
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("显卡信息: " + (ModConfig.showGpu ? "§a✔" : "§c✘")));
                }).pos(cx + 4, y + gap * 3).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("优化项详情: " + (ModConfig.showOptimizations ? "§a[开启]" : "§c[关闭]")),
                b -> {
                    ModConfig.showOptimizations = !ModConfig.showOptimizations;
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("优化项详情: " + (ModConfig.showOptimizations ? "§a[开启]" : "§c[关闭]")));
                }).pos(cx - bw / 2, y + gap * 4).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("硬件调度: §e[" + ModConfig.profileMode.toUpperCase() + "]"),
                b -> {
                    if ("auto".equalsIgnoreCase(ModConfig.profileMode)) ModConfig.profileMode = "low";
                    else if ("low".equalsIgnoreCase(ModConfig.profileMode)) ModConfig.profileMode = "medium";
                    else if ("medium".equalsIgnoreCase(ModConfig.profileMode)) ModConfig.profileMode = "high";
                    else ModConfig.profileMode = "auto";
                    ModConfig.autoTuneHardware();
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("硬件调度: §e[" + ModConfig.profileMode.toUpperCase() + "]"));
                }).pos(cx - bw / 2, y + gap * 5).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("HUD 大小: §b[" + String.format("%.2f", ModConfig.hudScale) + "x]"),
                b -> {
                    float next = HUD_SCALES[0];
                    for (float s : HUD_SCALES) {
                        if (s > ModConfig.hudScale + 0.001f) { next = s; break; }
                    }
                    ModConfig.hudScale = next;
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("HUD 大小: §b[" + String.format("%.2f", ModConfig.hudScale) + "x]"));
                }).pos(cx - bw - 4, y + gap * 6).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("背景透明度: §b[" + ModConfig.hudBgAlpha + "]"),
                b -> {
                    int next = BG_ALPHAS[0];
                    for (int a : BG_ALPHAS) {
                        if (a > ModConfig.hudBgAlpha) { next = a; break; }
                    }
                    ModConfig.hudBgAlpha = next;
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("背景透明度: §b[" + ModConfig.hudBgAlpha + "]"));
                }).pos(cx + 4, y + gap * 6).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("HUD 进度条: " + (ModConfig.showBars ? "§a✔" : "§c✘")),
                b -> {
                    ModConfig.showBars = !ModConfig.showBars;
                    ModConfig.saveConfig();
                    b.setMessage(Component.literal("HUD 进度条: " + (ModConfig.showBars ? "§a✔" : "§c✘")));
                }).pos(cx - bw / 2, y + gap * 7).size(bw, bh).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("保存并返回"),
                b -> {
                    ModConfig.saveConfig();
                    if (this.minecraft != null) this.minecraft.setScreen(this.parent);
                }).pos(cx - 90, this.height - 28).size(180, bh).build());
    }

    private static final float[] HUD_SCALES = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f};
    private static final int[] BG_ALPHAS = {0, 60, 120, 170, 220, 255};

    private static String getPosName(String p) {
        if ("top_right".equalsIgnoreCase(p)) return "§b右上角";
        if ("bottom_left".equalsIgnoreCase(p)) return "§b左下角";
        if ("bottom_right".equalsIgnoreCase(p)) return "§b右下角";
        return "§b左上角";
    }

    private static String nextPos(String p) {
        if ("top_left".equalsIgnoreCase(p)) return "top_right";
        if ("top_right".equalsIgnoreCase(p)) return "bottom_right";
        if ("bottom_right".equalsIgnoreCase(p)) return "bottom_left";
        return "top_left";
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFAA00);
        g.drawCenteredString(this.font, "§7点击按钮切换 HUD 内容与屏幕停靠位置", this.width / 2, 23, 0x888888);
    }

    @Override
    public void onClose() {
        ModConfig.saveConfig();
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }
}
