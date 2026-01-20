package com.safari.world;

import com.safari.config.SafariConfig;
import com.safari.state.SafariWorldState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.block.Blocks;

import java.util.Random;

public class SafariWorldManager {

    public static void performDailyReset(ServerWorld safariWorld) {
        // 1. Update Date
        SafariWorldState.get().resetDailySeed(); // This updates date and raw coordinates
        
        // 2. Find a better spot based on Biomes
        findAndSetSafeSpot(safariWorld);

        // 3. Place a small spawn structure
        placeSpawnStructure(safariWorld);
    }

    public static void findAndSetSafeSpot(ServerWorld world) {
        Random rand = new Random();
        int originX = SafariWorldState.get().centerX;
        int originZ = SafariWorldState.get().centerZ;
        
        int attempt = 0;
        int maxAttempts = 50;
        
        while (attempt < maxAttempts) {
            // Check current spot
            BlockPos pos = new BlockPos(originX, 64, originZ);
            if (isValidBiome(world, pos)) {
                // Found it!
                SafariWorldState.get().centerX = originX;
                SafariWorldState.get().centerZ = originZ;
                SafariWorldState.get().save();
                
                updateBorder(world, originX, originZ);
                placeSpawnStructure(world);
                return;
            }
            
            // Move 500 blocks and try again
            originX += (rand.nextBoolean() ? 500 : -500);
            originZ += (rand.nextBoolean() ? 500 : -500);
            attempt++;
        }
        
        // If we fail, just accept the last one, but at least we tried.
        SafariWorldState.get().centerX = originX;
        SafariWorldState.get().centerZ = originZ;
        SafariWorldState.get().save();
        updateBorder(world, originX, originZ);
        placeSpawnStructure(world);
    }

    public static void placeSpawnStructure(ServerWorld world) {
        int x = SafariWorldState.get().centerX;
        int z = SafariWorldState.get().centerZ;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);

        // 11x11 grass patch with a small oak "marker"
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                BlockPos base = new BlockPos(x + dx, y - 1, z + dz);
                world.setBlockState(base, Blocks.GRASS_BLOCK.getDefaultState());
                BlockPos above = base.up();
                if (world.getBlockState(above).isAir() && world.random.nextFloat() < 0.2f) {
                    world.setBlockState(above, Blocks.GRASS.getDefaultState());
                }
            }
        }

        // Simple oak marker (log + leaves)
        BlockPos trunk = new BlockPos(x, y, z);
        world.setBlockState(trunk, Blocks.OAK_LOG.getDefaultState());
        world.setBlockState(trunk.up(), Blocks.OAK_LOG.getDefaultState());
        world.setBlockState(trunk.up(2), Blocks.OAK_LOG.getDefaultState());

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3) {
                    world.setBlockState(trunk.up(3).add(dx, 0, dz), Blocks.OAK_LEAVES.getDefaultState());
                }
            }
        }
    }
    
    private static void updateBorder(ServerWorld world, int x, int z) {
        world.getWorldBorder().setCenter(x, z);
        world.getWorldBorder().setSize(2000);
    }
    
    public static boolean isSafeSpot(ServerWorld world, BlockPos pos) {
        return isValidBiome(world, pos);
    }

    private static boolean isValidBiome(ServerWorld world, BlockPos pos) {
        // Get Biome
        var biomeEntry = world.getBiome(pos);
        Identifier biomeId = world.getRegistryManager().get(RegistryKeys.BIOME).getId(biomeEntry.value());
        
        if (biomeId == null) return false;
        String idStr = biomeId.toString();
        
        // Check Config Whitelist
        for (String allowed : SafariConfig.get().allowedBiomes) {
            if (idStr.equals(allowed)) return true;
        }
        
        // Also allow if it's "similar" (e.g. if config says "jungle" and we find "sparse_jungle")
        // But for now strict check is safer to guarantee visuals.
        
        return false;
    }
}
