package com.safari.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.safari.world.SafariDimension;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Mixin(PokemonEntity.class)
public abstract class MixinPokemonEntity {

    @Inject(method = "forceBattle", at = @At("HEAD"), cancellable = true, remap = false)
    private void onForceBattle(ServerPlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (player.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
            player.sendMessage(Text.translatable("message.safari.no_battling").formatted(Formatting.RED), true);
            cir.setReturnValue(false);
        }
    }
}
