package me.yourname.customchaos;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class CustomChaosClient implements ClientModInitializer {
    private static final KeyMapping OPEN_MENU = KeyBindingHelper.registerKeyBinding(
        new KeyMapping(
            "key.customchaos.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            KeyMapping.Category.register(CustomChaos.id("controls"))
        )
    );

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU.consumeClick()) {
                if (client.player != null) {
                    Minecraft.getInstance().gui.setScreen(
                        new ChaosScreen(Minecraft.getInstance().gui.screen())
                    );
                }
            }
        });
    }
}
