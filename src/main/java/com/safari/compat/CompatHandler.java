package com.safari.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;

public class CompatHandler {
    private static boolean isYawpLoaded = false;

    public static void init() {
        if (FabricLoader.getInstance().isModLoaded("yawp")) {
            isYawpLoaded = true;
            try {
                com.safari.compat.yawp.YawpIntegration.register();
                com.safari.SafariMod.LOGGER.info("YAWP integration registered.");
            } catch (Exception e) {
                com.safari.SafariMod.LOGGER.error("Failed to register YAWP integration: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if interaction/attack is allowed by YAWP.
     * @return TRUE (Allowed), FALSE (Denied), or NULL (Undefined/No YAWP).
     */
    public static Boolean checkYawp(Entity target, Entity attacker) {
        if (isYawpLoaded) {
            try {
                return com.safari.compat.yawp.YawpIntegration.checkFlag(target, attacker);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
