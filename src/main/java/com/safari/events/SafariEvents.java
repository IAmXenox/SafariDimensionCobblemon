package com.safari.events;

import com.safari.block.SafariBlocks;
import com.safari.config.SafariConfig;
import com.safari.session.SafariSessionManager;
import com.safari.world.SafariDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.Items;

public class SafariEvents {

    public static void init() {
        // 1. Block Vanilla Spawns (Only Cobblemon allowed in Safari Dimension)
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (world.getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
                if (entity.isPlayer()) return;
                
                Identifier id = Registries.ENTITY_TYPE.getId(entity.getType());
                if (id.getNamespace().equals("cobblemon")) return;
                
                // Block other living entities
                if (entity instanceof net.minecraft.entity.LivingEntity) {
                    entity.discard();
                }
            }
        });

        // 2. Block Breaking
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (isInSafari(player)) {
                if (!player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.of("§cYou cannot break blocks in the Safari!"), true);
                    return false;
                }
            }
            return true;
        });

        // 3. Block Placing + Portal Lighting
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            if (world.isClient) return ActionResult.PASS;

            // Portal lighting: Flint and Steel on frame
            if (!world.getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)
                    && player.getStackInHand(hand).isOf(Items.FLINT_AND_STEEL)
                    && world.getBlockState(pos).isOf(SafariBlocks.SAFARI_PORTAL_FRAME)) {

                BlockPos target = pos.offset(hitResult.getSide());
                if (world.getBlockState(target).isAir()) {
                    world.setBlockState(target, SafariBlocks.SAFARI_PORTAL.getDefaultState());
                    player.getStackInHand(hand).damage(1, player, p -> p.sendToolBreakStatus(hand));
                    return ActionResult.SUCCESS;
                }
            }

            // Block placing in safari
            if (isInSafari(player)) {
                if (!player.hasPermissionLevel(2)) {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        // 4. Logout Handling
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (SafariSessionManager.isInSession(handler.getPlayer())) {
                 if (SafariConfig.get().logoutClearInventory) {
                     SafariSessionManager.endSession(handler.getPlayer());
                 }
            }
        });

        // 5. Prevent attacking entities
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (isInSafari(player)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    private static boolean isInSafari(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return serverPlayer.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY);
        }
        return false;
    }
}
