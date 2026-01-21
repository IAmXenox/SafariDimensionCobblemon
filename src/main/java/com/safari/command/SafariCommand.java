package com.safari.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.safari.config.SafariConfig;
import com.safari.session.SafariSession;
import com.safari.session.SafariSessionManager;
import com.safari.economy.SafariEconomy;
import com.safari.world.SafariWorldManager;
import com.safari.world.SafariDimension;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class SafariCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("safari")
            .executes(SafariCommand::info)
            .then(CommandManager.literal("enter").executes(SafariCommand::enter))
            .then(CommandManager.literal("leave").executes(SafariCommand::leave))
            .then(CommandManager.literal("info").executes(SafariCommand::info))
            .then(CommandManager.literal("buy")
                .then(CommandManager.literal("balls")
                    .then(CommandManager.literal("5").executes(ctx -> buyBalls(ctx, 5)))
                    .then(CommandManager.literal("10").executes(ctx -> buyBalls(ctx, 10)))
                )
            )
            // Admin commands
            .then(CommandManager.literal("reload").requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> {
                    SafariConfig.load();
                    ctx.getSource().sendMessage(Text.of("§aConfig reloaded!"));
                    return 1;
                })
            )
            .then(CommandManager.literal("reset").requires(source -> source.hasPermissionLevel(2))
                .executes(SafariCommand::reset)
            )
        );
    }

    private static int reset(CommandContext<ServerCommandSource> ctx) {
        ServerWorld world = ctx.getSource().getServer().getWorld(SafariDimension.SAFARI_DIM_KEY);
        if (world == null) {
            ctx.getSource().sendMessage(Text.of("§cError: Safari Dimension not loaded."));
            return 0;
        }
        
        // Kick all players in dimension
        for (ServerPlayerEntity player : world.getPlayers()) {
            SafariSessionManager.endSession(player);
            player.sendMessage(Text.of("§cThe Safari Zone is resetting! You have been evacuated."), false);
        }
        
        SafariWorldManager.performDailyResetWithEvacuation(world);
        ctx.getSource().sendMessage(Text.of("§6Safari Dimension reset! Players evacuated and world regenerated."));
        return 1;
    }

    private static int enter(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.of("§cYou are already in a Safari session!"));
                return 0;
            }
            
            int price = SafariConfig.get().entrancePrice;
            if (!SafariEconomy.deduct(player, price)) {
                ctx.getSource().sendMessage(Text.of("§cYou need " + price + " Pokédollars to enter!"));
                return 0;
            }
            
            ctx.getSource().sendMessage(Text.of("§aPaid " + price + " Pokédollars. Entering Safari..."));
            SafariSessionManager.startSession(player);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int leave(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (!SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.of("§cYou are not in a Safari session!"));
                return 0;
            }
            SafariSessionManager.endSession(player);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int buyBalls(CommandContext<ServerCommandSource> ctx, int amount) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (!SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.of("§cYou must be in the Safari to buy supplies!"));
                return 0;
            }
            
            SafariSession session = SafariSessionManager.getSession(player);
            if (session.getPurchasedBalls() + amount > SafariConfig.get().maxBallsPurchasable) {
                ctx.getSource().sendMessage(Text.of("§cYou have reached the purchase limit for this session!"));
                return 0;
            }

            int price = (amount == 5) ? SafariConfig.get().pack5BallsPrice : SafariConfig.get().pack10BallsPrice;
            
            if (SafariEconomy.deduct(player, price)) {
                com.safari.session.SafariInventoryHandler.giveSafariKit(player, amount);
                session.incrementPurchasedBalls(amount);
                ctx.getSource().sendMessage(Text.of("§aPurchased " + amount + " Safari Balls!"));
                return 1;
            } else {
                ctx.getSource().sendMessage(Text.of("§cNot enough money!"));
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (!SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.of("§7You are not currently in a Safari session."));
                return 0;
            }
            SafariSession session = SafariSessionManager.getSession(player);
            long minutes = (session.getTicksRemaining() / 20) / 60;
            ctx.getSource().sendMessage(Text.of("§2Safari Session Info:\n§7Time Left: " + minutes + "m"));
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
