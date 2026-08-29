package com.createtrain.optimization.client;

import com.createtrain.optimization.gui.PerformanceConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class KeyInputHandler {

    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.createtrain_optimization.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.createtrain_optimization"
    );

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_CONFIG_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new PerformanceConfigScreen(null));
            }
        }
    }
}
