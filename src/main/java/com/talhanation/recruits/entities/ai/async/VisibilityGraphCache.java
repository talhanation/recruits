package com.talhanation.recruits.entities.ai.async;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VisibilityGraphCache {
    private static final ConcurrentLinkedQueue<BiDirectionalPair<LivingEntity, Entity>> processQueue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<BiDirectionalPair<LivingEntity, Entity>, CacheEntry> visibilityCache = new ConcurrentHashMap<>();

    public static @Nullable CacheEntry canSee(LivingEntity first, Entity second) {
        CacheEntry entry = visibilityCache.get(new BiDirectionalPair<>(first, second));
        if(entry != null && entry.isAlive()){
            return entry;
        }

        if(first == null || second == null ||
                first.distanceToSqr(second.getPosition(0)) >= 62500.0D) {
            return null;
        }

        processQueue.add(new BiDirectionalPair<>(first, second));
        return null;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        event.getServer().execute(() -> {
            BiDirectionalPair<LivingEntity, Entity> pair;
            while((pair = processQueue.poll()) != null) {
                visibilityCache.put(
                        pair,
                        new CacheEntry(
                                pair.left(),
                                pair.right(),
                                pair.left().hasLineOfSight(pair.right()),
                                Instant.now().toEpochMilli()
                        )
                );
            }
        });
    }
}
