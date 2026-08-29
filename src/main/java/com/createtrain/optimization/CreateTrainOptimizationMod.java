package com.createtrain.optimization;

import com.createtrain.optimization.command.OptCommands;
import com.createtrain.optimization.client.KeyInputHandler;
import com.createtrain.optimization.config.ModConfig;
import com.createtrain.optimization.hud.PerformanceHudOverlay;
import com.createtrain.optimization.threadpool.SafeThreadPoolManager;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("createtrain_optimization")
public class CreateTrainOptimizationMod {
    public static final String MOD_ID = "createtrain_optimization";
    public static final Logger LOGGER = LoggerFactory.getLogger("CreateTrainOptimization");

    public CreateTrainOptimizationMod(IEventBus modEventBus) {
        LOGGER.info("[CreateTrainOptimization] === Phase 1: Executing Hardware Adaptive Core Prerequisite Phase ===");
        
        ModConfig.loadConfig(FMLPaths.CONFIGDIR.get().resolve("createtrain_optimization.toml"));
        LOGGER.info("[CreateTrainOptimization] Prerequisite Adaptive State Ready. Detected Logical Cores: {}, Profile Mode: [{}], Allocated Worker Threads: {}", 
                ModConfig.detectedCores, ModConfig.profileMode.toUpperCase(), ModConfig.ioThreadPoolSize);
        
        LOGGER.info("[CreateTrainOptimization] === Phase 2: Initializing Downstream Optimization Modules ===");
        SafeThreadPoolManager.initThreadPools();
        
        modEventBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(PerformanceHudOverlay::onRenderGuiPost);
            NeoForge.EVENT_BUS.addListener(KeyInputHandler::onClientTick);
            modEventBus.addListener(KeyInputHandler::onRegisterKeyMappings);
            LOGGER.info("[CreateTrainOptimization] Registered Performance HUD Overlay for Client Dist.");
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[CreateTrainOptimization] Common setup complete. All Adaptive Optimization Patches ACTIVE.");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        OptCommands.register(event.getDispatcher());
        LOGGER.info("[CreateTrainOptimization] Registered in-game optimization management commands.");
    }
}



