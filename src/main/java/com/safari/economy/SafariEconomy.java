package com.safari.economy;

import com.safari.SafariMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class SafariEconomy {

    public static boolean hasEnough(ServerPlayerEntity player, int amount) {
        try {
            Object manager = getManager();
            if (manager == null) {
                System.err.println("SafariEconomy: EconomyManager is null");
                return false;
            }

            // public BigDecimal getBalance(UUID uuid)
            Method getBal = manager.getClass().getMethod("getBalance", java.util.UUID.class);
            BigDecimal current = (BigDecimal) getBal.invoke(manager, player.getUuid());
            
            if (current == null) return false;
            
            return current.compareTo(BigDecimal.valueOf(amount)) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deduct(ServerPlayerEntity player, int amount) {
        try {
            Object manager = getManager();
            if (manager == null) return false;

            // public boolean subtractBalance(UUID uuid, BigDecimal amount)
            Method subBal = manager.getClass().getMethod("subtractBalance", java.util.UUID.class, BigDecimal.class);
            int before = getBalance(player);
            boolean success = (boolean) subBal.invoke(manager, player.getUuid(), BigDecimal.valueOf(amount));
            int after = getBalance(player);
            SafariMod.LOGGER.info(
                    "Safari economy deduct: player={}, amount={}, success={}, balanceBefore={}, balanceAfter={}",
                    player.getName().getString(),
                    amount,
                    success,
                    before,
                    after
            );
            appendTransactionLog(player, "deduct", amount, success, before, after);
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void appendTransactionLog(ServerPlayerEntity player, String type, int amount, boolean success, int before, int after) {
        try {
            var server = player.getServer();
            if (server == null) return;
            Path logFile = server.getSavePath(WorldSavePath.ROOT).resolve("safari-transactions.log");
            String line = String.format(
                    "%s\t%s\t%s\t%s\tamount=%d\tsuccess=%s\tbalanceBefore=%d\tbalanceAfter=%d%n",
                    Instant.now().toString(),
                    player.getName().getString(),
                    player.getUuid(),
                    type,
                    amount,
                    success,
                    before,
                    after
            );
            Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            SafariMod.LOGGER.warn("Safari economy log write failed", e);
        }
    }

    public static int getBalance(ServerPlayerEntity player) {
        try {
            Object manager = getManager();
            if (manager == null) return 0;

            Method getBal = manager.getClass().getMethod("getBalance", java.util.UUID.class);
            BigDecimal current = (BigDecimal) getBal.invoke(manager, player.getUuid());
            if (current == null) return 0;
            return current.intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    private static Object getManager() {
        try {
            // public static EconomyManager getEconomyManager()
            Class<?> mainClass = Class.forName("com.cobblemon.economy.fabric.CobblemonEconomy");
            Method getMgr = mainClass.getMethod("getEconomyManager");
            return getMgr.invoke(null);
        } catch (Exception e) {
            System.err.println("SafariEconomy: Failed to get EconomyManager via Reflection");
            e.printStackTrace();
            return null;
        }
    }
}
