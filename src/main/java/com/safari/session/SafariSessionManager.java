package com.safari.session;

import com.safari.config.SafariConfig;
import com.safari.state.SafariWorldState;
import com.safari.world.SafariDimension;
import com.safari.world.SafariWorldManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SafariSessionManager {
    private static final Map<UUID, SafariSession> activeSessions = new ConcurrentHashMap<>();
    private static long resetCheckTicks = 0;
    private static net.minecraft.server.MinecraftServer server;

    public static void setServer(net.minecraft.server.MinecraftServer srv) {
        server = srv;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    public static void startSession(ServerPlayerEntity player) {
        // 1. Validate Dimension
        ServerWorld safariWorld = player.getServer().getWorld(SafariDimension.SAFARI_DIM_KEY);
        if (safariWorld == null) {
            player.sendMessage(Text.of("§cError: Safari Dimension not loaded! Contact Admin."), false);
            return;
        }

        // 2. Ensure Safe Spawn (No Ocean)
        if (!SafariWorldManager.isSafeSpot(safariWorld, new BlockPos(SafariWorldState.get().centerX, 64, SafariWorldState.get().centerZ))) {
             player.sendMessage(Text.of("§eFinding a safe landing spot... please wait."), true);
             SafariWorldManager.findAndSetSafeSpot(safariWorld);
        }

        // Ensure spawn structure exists
        SafariWorldManager.placeSpawnStructure(safariWorld);

        // 3. Require at least one empty slot for safari balls
        if (player.getInventory().getEmptySlot() == -1) {
            player.sendMessage(Text.of("§cYou need at least one empty slot for Safari Balls."), false);
            return;
        }

        // 4. Create Session Object with safe return position
        long duration = SafariConfig.get().sessionTimeMinutes * 60L * 20L;
        BlockPos safeReturnPos = getSafeReturnPos(player);
        SafariSession session = new SafariSession(
                player,
                duration,
                player.getWorld().getRegistryKey(),
                safeReturnPos,
                player.getYaw(),
                player.getPitch()
        );
        activeSessions.put(player.getUuid(), session);

        // 5. Give Safari Kit (do not clear inventory)
        SafariInventoryHandler.giveSafariKit(player, SafariConfig.get().initialSafariBalls);

        // 6. Teleport to Safari
        int x = SafariWorldState.get().centerX;
        int z = SafariWorldState.get().centerZ;
        int y = SafariConfig.get().safariSpawnY + SafariConfig.get().safariSpawnOffsetY;
        player.teleport(safariWorld, x + 0.5, y, z + 0.5, 0, 0);
        
        // 7. Apply Resistance/Safety for 10 seconds (loading protection)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 255, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 200, 1, false, false));

        player.sendMessage(Text.of("§2Welcome to the Safari Zone! You have " + SafariConfig.get().sessionTimeMinutes + " minutes."), false);
    }

    public static void endSession(ServerPlayerEntity player) {
        SafariSession session = activeSessions.remove(player.getUuid());
        if (session != null) {
            // 1. Teleport Back (inventory untouched)
            ServerWorld returnWorld = player.getServer().getWorld(session.getReturnDimension());
            if (returnWorld != null) {
                player.teleport(returnWorld, 
                    session.getReturnPos().getX(), 
                    session.getReturnPos().getY(), 
                    session.getReturnPos().getZ(), 
                    session.getReturnYaw(), 
                    session.getReturnPitch()
                );
            } else {
                // Fallback to Overworld Spawn if original world is invalid
                ServerWorld overworld = player.getServer().getWorld(World.OVERWORLD);
                player.teleport(overworld, overworld.getSpawnPos().getX(), overworld.getSpawnPos().getY(), overworld.getSpawnPos().getZ(), 0, 0);
            }

            player.sendMessage(Text.of("§cYour Safari session has ended."), false);
        }
    }

    private static void tick() {
        resetCheckTicks++;
        if (resetCheckTicks % (20 * 60) == 0 && server != null) { // check every minute
            var safariWorld = server.getWorld(com.safari.world.SafariDimension.SAFARI_DIM_KEY);
            if (safariWorld != null && com.safari.state.SafariWorldState.get().needsReset()) {
                com.safari.world.SafariWorldManager.performDailyResetWithEvacuation(safariWorld);
            }
        }
        activeSessions.values().forEach(session -> {
            session.tick();
            
            // Action Bar Timer (Every second)
            if (session.getTicksRemaining() % 20 == 0) {
                long seconds = session.getTicksRemaining() / 20;
                long mins = seconds / 60;
                long secs = seconds % 60;
                session.getPlayer().sendMessage(Text.of("§eTime Remaining: " + String.format("%02d:%02d", mins, secs)), true);
            }

            if (session.getPlayer().isDisconnected()) {
                 // Logic handled in SafariEvents usually, but good to clean up if missed
            }

            if (session.isExpired()) {
                endSession(session.getPlayer());
            }
        });
    }

    public static boolean isInSession(ServerPlayerEntity player) {
        return activeSessions.containsKey(player.getUuid());
    }
    
    public static SafariSession getSession(ServerPlayerEntity player) {
        return activeSessions.get(player.getUuid());
    }

    private static BlockPos getSafeReturnPos(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        // If standing in portal block, move one block away from portal
        if (player.getWorld().getBlockState(pos).isOf(com.safari.block.SafariBlocks.SAFARI_PORTAL)) {
            var dir = player.getHorizontalFacing().getOpposite();
            BlockPos candidate = pos.offset(dir);
            if (player.getWorld().getBlockState(candidate).isAir()) {
                return candidate;
            }
        }
        return pos;
    }
}
