package com.safari.mixin;

import com.cobblemon.mod.common.api.pokeball.catching.CaptureContext;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.safari.config.SafariConfig;
import com.safari.logic.SafariSpawnRarity;
import com.safari.world.SafariDimension;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Random;

@Mixin(targets = "com.cobblemon.mod.common.api.events.pokeball.PokeBallCaptureCalculatedEvent", remap = false)
public abstract class MixinPokeBallCaptureCalculatedEvent {
    private static final Random RANDOM = new Random();

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void onInit(
        LivingEntity thrower,
        PokemonEntity pokemonEntity,
        EmptyPokeBallEntity pokeBallEntity,
        CaptureContext captureResult,
        CallbackInfo ci
    ) {
        if (thrower.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
            double rate = getCatchRate(pokemonEntity);

            boolean success = RANDOM.nextDouble() < rate;
            
            try {
                CaptureContext newResult = new CaptureContext(success ? 3 : 1, success, false);
                Method setter = this.getClass().getMethod("setCaptureResult", CaptureContext.class);
                setter.invoke(this, newResult);
            } catch (Exception ignored) {
            }
        }
    }

    private double getCatchRate(PokemonEntity pokemonEntity) {
        String bucket = SafariSpawnRarity.getBucket(pokemonEntity);
        if (bucket == null) {
            return SafariConfig.get().commonCatchRate;
        }
        return switch (bucket) {
            case "uncommon" -> SafariConfig.get().uncommonCatchRate;
            case "rare" -> SafariConfig.get().rareCatchRate;
            case "ultra-rare" -> SafariConfig.get().ultraRareCatchRate;
            default -> SafariConfig.get().commonCatchRate;
        };
    }
}
