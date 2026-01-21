package com.safari.world;

import com.safari.config.SafariConfig;
import com.safari.state.SafariWorldState;
import com.safari.session.SafariSessionManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import com.safari.SafariMod;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import java.util.Random;

public class SafariWorldManager {

    public static void performDailyReset(ServerWorld safariWorld) {
        // 1. Delete dimension folder to force regeneration
        deleteDimensionFolder(safariWorld);

        // 2. Update Date + Seed
        SafariWorldState.get().resetDailySeed();

        // 3. Find a better spot based on Biomes
        findAndSetSafeSpot(safariWorld);
    }

    public static void performDailyResetWithEvacuation(ServerWorld safariWorld) {
        for (var player : new java.util.ArrayList<>(safariWorld.getPlayers())) {
            SafariSessionManager.endSession(player);
            player.sendMessage(net.minecraft.text.Text.of("§cSafari reset: you have been evacuated."), false);
        }
        performDailyReset(safariWorld);
    }

    public static void findAndSetSafeSpot(ServerWorld world) {
        SafariWorldState.get().centerX = 0;
        SafariWorldState.get().centerZ = 0;
        SafariWorldState.get().save();
        updateBorder(world, 0, 0);
        placeSpawnStructure(world);
    }

    public static void placeSpawnStructure(ServerWorld world) {
        int centerX = SafariWorldState.get().centerX;
        int centerZ = SafariWorldState.get().centerZ;
        int y = SafariConfig.get().safariSpawnY;

        Identifier structureId = Identifier.of(SafariConfig.get().spawnStructureId);
        StructureTemplate template = world.getStructureTemplateManager().getTemplateOrBlank(structureId);
        Vec3i size = template.getSize();

        if (size.getX() == 0 || size.getY() == 0 || size.getZ() == 0) {
            SafariMod.LOGGER.warn("Safari spawn structure '{}' not found or empty. Using fallback grass patch.", structureId);
            placeFallbackGrass(world, centerX, y, centerZ);
            return;
        }

        // Center the structure at (centerX, centerZ)
        int originX = centerX - (size.getX() / 2);
        int originZ = centerZ - (size.getZ() / 2);
        BlockPos origin = new BlockPos(originX, y, originZ);

        StructurePlacementData placement = new StructurePlacementData();
        placement.setIgnoreEntities(true);

        template.place(world, origin, origin, placement, world.getRandom(), 2);
    }

    private static void placeFallbackGrass(ServerWorld world, int centerX, int y, int centerZ) {
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                BlockPos base = new BlockPos(centerX + dx, y - 1, centerZ + dz);
                world.setBlockState(base, Blocks.GRASS_BLOCK.getDefaultState());
                BlockPos above = base.up();
                if (!world.getBlockState(above).isAir()) continue;

                float roll = world.random.nextFloat();
                if (roll < 0.15f) {
                    // Tall grass (two blocks)
                    BlockPos upper = above.up();
                    if (world.getBlockState(upper).isAir()) {
                        world.setBlockState(above, Blocks.TALL_GRASS.getDefaultState().with(net.minecraft.block.TallPlantBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.LOWER));
                        world.setBlockState(upper, Blocks.TALL_GRASS.getDefaultState().with(net.minecraft.block.TallPlantBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));
                    }
                } else if (roll < 0.45f) {
                    world.setBlockState(above, Blocks.SHORT_GRASS.getDefaultState());
                }
            }
        }
    }

    public static void deleteDimensionFolder(ServerWorld world) {
        try {
            Path root = world.getServer().getSavePath(WorldSavePath.ROOT).resolve("dimensions");
            Identifier id = world.getRegistryKey().getValue();
            Path dimPath = root.resolve(id.getNamespace()).resolve(id.getPath());

            // Save world chunks before deletion
            world.getChunkManager().save(true);

            if (Files.exists(dimPath)) {
                Files.walk(dimPath)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
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
