package me.yourname.customchaos;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomChaos implements ModInitializer {
    public static final String MOD_ID = "customchaos";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ChaosCommands.register();
        LOGGER.info("CustomChaos 1.0.0 initialized for Minecraft 26.2.");
    }
}
