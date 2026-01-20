package com.safari.session;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SafariSession {
    private final ServerPlayerEntity player;
    private long ticksRemaining;
    private final RegistryKey<World> returnDimension;
    private final BlockPos returnPos;
    private final float returnYaw;
    private final float returnPitch;
    private int purchasedBalls = 0;

    public SafariSession(ServerPlayerEntity player, long durationTicks) {
        this.player = player;
        this.ticksRemaining = durationTicks;
        this.returnDimension = player.getWorld().getRegistryKey();
        this.returnPos = player.getBlockPos();
        this.returnYaw = player.getYaw();
        this.returnPitch = player.getPitch();
    }

    public void tick() {
        this.ticksRemaining--;
    }

    public boolean isExpired() {
        return ticksRemaining <= 0;
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public long getTicksRemaining() {
        return ticksRemaining;
    }
    
    public void addTime(long ticks) {
        this.ticksRemaining += ticks;
    }

    public RegistryKey<World> getReturnDimension() {
        return returnDimension;
    }

    public BlockPos getReturnPos() {
        return returnPos;
    }

    public float getReturnYaw() {
        return returnYaw;
    }

    public float getReturnPitch() {
        return returnPitch;
    }
    
    public int getPurchasedBalls() {
        return purchasedBalls;
    }
    
    public void incrementPurchasedBalls(int amount) {
        this.purchasedBalls += amount;
    }
}
