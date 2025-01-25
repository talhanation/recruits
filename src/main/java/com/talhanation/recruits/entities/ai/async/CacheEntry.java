package com.talhanation.recruits.entities.ai.async;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.time.Instant;
import java.util.Random;

public class CacheEntry {
    private final double timeToLiveMs = 1400 + new Random().nextInt(200);
    LivingEntity first;
    Entity second;
    boolean canSee;
    long calculatedAt;

    public CacheEntry(LivingEntity first, Entity second, boolean canSee, long calculatedAt) {
        this.first = first;
        this.second = second;
        this.canSee = canSee;
        this.calculatedAt = calculatedAt;
    }

    public boolean getCanSee() {
        return this.canSee;
    }

    public boolean isAlive() {
        return (Instant.now().toEpochMilli() - this.calculatedAt) >= timeToLiveMs;
    }
}
