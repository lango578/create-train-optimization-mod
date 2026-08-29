package com.createtrain.optimization.command;

import com.createtrain.optimization.config.ModConfig;
import com.createtrain.optimization.threadpool.SafeThreadPoolManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class OptCommands {
    private OptCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("createtrain_opt")
            .then(Commands.literal("status").executes(c -> status(c.getSource())))
            .then(Commands.literal("reload").requires(s -> s.hasPermission(2)).executes(c -> reload(c.getSource())))
            .then(Commands.literal("profile").requires(s -> s.hasPermission(2))
                .then(Commands.argument("mode", StringArgumentType.word())
                    .executes(c -> profile(c.getSource(), StringArgumentType.getString(c, "mode")))))
            .then(Commands.literal("hud").requires(s -> s.hasPermission(2))
                .then(Commands.literal("toggle").then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(c -> hudToggle(c.getSource(), BoolArgumentType.getBool(c, "enabled")))))
                .then(Commands.literal("position").then(Commands.argument("position", StringArgumentType.word())
                    .executes(c -> hudPosition(c.getSource(), StringArgumentType.getString(c, "position")))))
                .then(Commands.literal("offset").then(Commands.argument("x", IntegerArgumentType.integer(0, 1000))
                    .then(Commands.argument("y", IntegerArgumentType.integer(0, 1000))
                        .executes(c -> hudOffset(c.getSource(), IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "y")))))))
            .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                .then(Commands.literal("sableLock").then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(c -> setSable(c.getSource(), BoolArgumentType.getBool(c, "enabled")))))
                .then(Commands.literal("asyncChunkIO").then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(c -> setAsync(c.getSource(), BoolArgumentType.getBool(c, "enabled")))))
                .then(Commands.literal("multithreadedEntities").then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(c -> setEntities(c.getSource(), BoolArgumentType.getBool(c, "enabled")))))
                .then(Commands.literal("threads").then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                    .executes(c -> setThreads(c.getSource(), IntegerArgumentType.getInteger(c, "count"))))));
        dispatcher.register(root);
        dispatcher.register(Commands.literal("ctopt").redirect(root.build()));
    }

    private static void send(CommandSourceStack s, String text) { s.sendSuccess(() -> Component.literal(text), false); }
    private static int status(CommandSourceStack s) { send(s, "[CreateTrain] cores=" + ModConfig.detectedCores + ", profile=" + ModConfig.profileMode + ", workers=" + ModConfig.ioThreadPoolSize); send(s, "[CreateTrain] HUD=" + ModConfig.enableHud + ", position=" + ModConfig.hudPosition); return 1; }
    private static int reload(CommandSourceStack s) { ModConfig.reloadConfig(); SafeThreadPoolManager.resizeThreadPool(ModConfig.ioThreadPoolSize); send(s, "[CreateTrain] 配置已重新加载。"); return 1; }
    private static int profile(CommandSourceStack s, String mode) { if (!(mode.equalsIgnoreCase("auto") || mode.equalsIgnoreCase("low") || mode.equalsIgnoreCase("medium") || mode.equalsIgnoreCase("high"))) { send(s, "[CreateTrain] 档位只能是 auto、low、medium 或 high。"); return 0; } ModConfig.profileMode = mode.toLowerCase(); ModConfig.autoTuneHardware(); ModConfig.saveConfig(); SafeThreadPoolManager.resizeThreadPool(ModConfig.ioThreadPoolSize); send(s, "[CreateTrain] 性能档位已修改。"); return 1; }
    private static int hudToggle(CommandSourceStack s, boolean value) { ModConfig.enableHud = value; ModConfig.saveConfig(); send(s, "[CreateTrain] HUD 已" + (value ? "开启" : "关闭") + "。"); return 1; }
    private static int hudPosition(CommandSourceStack s, String value) { if (!(value.equalsIgnoreCase("top_left") || value.equalsIgnoreCase("top_right") || value.equalsIgnoreCase("bottom_left") || value.equalsIgnoreCase("bottom_right"))) { send(s, "[CreateTrain] 位置只能是 top_left、top_right、bottom_left 或 bottom_right。"); return 0; } ModConfig.hudPosition = value.toLowerCase(); ModConfig.saveConfig(); return 1; }
    private static int hudOffset(CommandSourceStack s, int x, int y) { ModConfig.hudXOffset = x; ModConfig.hudYOffset = y; ModConfig.saveConfig(); return 1; }
    private static int setSable(CommandSourceStack s, boolean value) { ModConfig.enableSableLockFix = value; ModConfig.saveConfig(); return 1; }
    private static int setAsync(CommandSourceStack s, boolean value) { ModConfig.enableAsyncChunkIO = value; ModConfig.saveConfig(); return 1; }
    private static int setEntities(CommandSourceStack s, boolean value) { ModConfig.enableMultithreadEntityTick = value; ModConfig.saveConfig(); return 1; }
    private static int setThreads(CommandSourceStack s, int value) { SafeThreadPoolManager.resizeThreadPool(value); ModConfig.saveConfig(); return 1; }
}