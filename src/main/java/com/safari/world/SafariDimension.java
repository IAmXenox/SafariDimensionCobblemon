package com.safari.world;

import com.safari.SafariMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class SafariDimension {
    public static final RegistryKey<DimensionType> SAFARI_DIM_TYPE_KEY = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, Identifier.of(SafariMod.MOD_ID, "safari"));
    public static final RegistryKey<World> SAFARI_DIM_KEY = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(SafariMod.MOD_ID, "safari"));

    public static void init() {
        // Dimensions are loaded from Data Packs (JSON), so we just need the Keys here.
    }
}
