package com.safari.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.safari.state.SafariWorldState;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SafariChunkGenerator extends ChunkGenerator {
    public static final MapCodec<SafariChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.getBiomeSource()),
                    ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(gen -> gen.settings)
            ).apply(instance, SafariChunkGenerator::new)
    );

    private static RegistryEntryLookup<net.minecraft.util.math.noise.DoublePerlinNoiseSampler.NoiseParameters> NOISE_PARAMS_LOOKUP;

    private final NoiseChunkGenerator delegate;
    private final RegistryEntry<ChunkGeneratorSettings> settings;

    public SafariChunkGenerator(BiomeSource biomeSource,
                                RegistryEntry<ChunkGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;
        this.delegate = new NoiseChunkGenerator(biomeSource, settings);
    }

    public static void setNoiseParamsLookup(RegistryEntryLookup<net.minecraft.util.math.noise.DoublePerlinNoiseSampler.NoiseParameters> lookup) {
        NOISE_PARAMS_LOOKUP = lookup;
    }

    private NoiseConfig createNoiseConfig() {
        long seed = SafariWorldState.get().currentDailySeed;
        return NoiseConfig.create(settings.value(), NOISE_PARAMS_LOOKUP, seed);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateBiomes(NoiseConfig noiseConfig, Blender blender, StructureAccessor structureAccessor, Chunk chunk) {
        return delegate.populateBiomes(createNoiseConfig(), blender, structureAccessor, chunk);
    }

    @Override
    public void carve(ChunkRegion region, long seed, NoiseConfig noiseConfig, net.minecraft.world.biome.source.BiomeAccess biomeAccess,
                      StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver step) {
        delegate.carve(region, seed, createNoiseConfig(), biomeAccess, structureAccessor, chunk, step);
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        return delegate.populateNoise(blender, createNoiseConfig(), structureAccessor, chunk);
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structureAccessor, NoiseConfig noiseConfig, Chunk chunk) {
        delegate.buildSurface(region, structureAccessor, createNoiseConfig(), chunk);
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        delegate.populateEntities(region);
    }

    @Override
    public int getWorldHeight() {
        return delegate.getWorldHeight();
    }

    @Override
    public int getSeaLevel() {
        return delegate.getSeaLevel();
    }

    @Override
    public int getMinimumY() {
        return delegate.getMinimumY();
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return delegate.getHeight(x, z, heightmap, world, createNoiseConfig());
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        return delegate.getColumnSample(x, z, world, createNoiseConfig());
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        delegate.getDebugHudText(text, createNoiseConfig(), pos);
    }

    @Override
    public Pool<net.minecraft.world.biome.SpawnSettings.SpawnEntry> getEntitySpawnList(net.minecraft.registry.entry.RegistryEntry<Biome> biome, StructureAccessor structureAccessor, SpawnGroup group, BlockPos pos) {
        return delegate.getEntitySpawnList(biome, structureAccessor, group, pos);
    }
}
