package com.safari.mixin;

import com.safari.world.SafariDimension;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cobblemon.mod.common.net.messages.server.SendOutPokemonPacket;

@Mixin(targets = "com.cobblemon.mod.common.net.serverhandling.storage.SendOutPokemonHandler", remap = false)
public abstract class MixinSendOutPokemonHandler {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void onHandle(SendOutPokemonPacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
        if (player.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
            player.sendMessage(net.minecraft.text.Text.translatable("message.safari.no_send_out"), true);
            ci.cancel();
        }
    }
}
