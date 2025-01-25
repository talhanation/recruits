package com.talhanation.recruits.entities.ai.async;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.*;

public class EntityCache {
    private static final ConcurrentLinkedQueue<Entity> processQueue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<DimensionType, QuadTreeNode> levelNodes = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @SubscribeEvent
    static void onSpawn(EntityJoinLevelEvent event) {
        processQueue.add(event.getEntity());
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        processQueue.add(event.getEntity());
    }

    @SubscribeEvent
    static void onDespawn(LivingSpawnEvent.AllowDespawn event) {
        processQueue.add(event.getEntity());
        event.setResult(Event.Result.DEFAULT);
    }

    public static void pendEntity(Entity entity) {
        processQueue.add(entity);
    }

    private static void insert(Entity entity) {
        EntityCache.withLevel(entity.getLevel()).insert(entity);
    }

    private static void remove(Entity entity) {
        EntityCache.withLevel(entity.getLevel()).remove(entity);
    }

    public static QuadTreeNode withLevel(Level level) {
        return levelNodes.computeIfAbsent(level.dimensionType(), (DimensionType key) -> new QuadTreeNode(
                new AABB(
                        Double.MIN_VALUE,
                        level.getMinBuildHeight(),
                        Double.MIN_VALUE,
                        Double.MAX_VALUE,
                        level.getMaxBuildHeight(),
                        Double.MAX_VALUE))
        );
    }

    public static void schedulePeriodicTask(Runnable runnable, long periodMs) {
        executor.scheduleAtFixedRate(runnable, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if(event.phase != TickEvent.Phase.END) {
            return;
        }

        event.getServer().execute(() -> {
            Entity entity;
            while((entity = processQueue.poll()) != null) {
                if(!entity.isAlive()) EntityCache.remove(entity);
                EntityCache.insert(entity);
            }
        });
    }
}