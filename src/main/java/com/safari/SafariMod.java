package com.safari;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafariMod implements ModInitializer {
    public static final String MOD_ID = "safari";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Safari Dimension Mod...");
        
        // 1. Register Config Loader (Load on Server Start)
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            com.safari.config.SafariConfig.load(server.getSavePath(WorldSavePath.ROOT).toFile());
        });
        
        // 2. Init Session Manager
        com.safari.session.SafariSessionManager.init();
        
        // 3. Register Commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(com.safari.command.SafariCommand::register);
        
        // 4. Register Events & Dimension
        com.safari.world.SafariDimension.init();
        com.safari.events.SafariEvents.init();

        // 5. Register Blocks & Items
        com.safari.block.SafariBlocks.registerModBlocks();
        com.safari.item.ModItems.registerModItems();
    }
}
