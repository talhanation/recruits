package com.talhanation.recruits.entities.ai.async;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class QuadTreeNode {
    private static final int MAX_ENTITIES_PER_NODE = 100;
    private final AABB bounds;
    private final Map<Entity, Boolean> entities;
    private final QuadTreeNode[] children;

    QuadTreeNode(AABB bounds) {
        this.bounds = bounds;
        this.entities = new ConcurrentHashMap<>(100, 0.75F, 2);
        this.children = new QuadTreeNode[4];
        EntityCache.schedulePeriodicTask(() -> {
            if(this.entities.isEmpty()) return;

            this.entities.keySet().parallelStream().
                    filter((entity) -> !this.bounds.intersects(entity.getBoundingBox())).
                    forEach((entity) -> {
                        this.entities.remove(entity);

                        EntityCache.pendEntity(entity);
                    });
        }, 650 + new Random().nextInt(200));
    }

    private int getChildIndex(AABB entityBounds) {
        int index = 0;
        Vec3 center = entityBounds.getCenter();
        if (entityBounds.maxX >= center.x()) index |= 1;
        if (entityBounds.maxZ >= center.z()) index |= 2;
        return index;
    }

    private void splitNode() {
        Vec3 center = bounds.getCenter();

        for (int i = 0; i < 4; i++) {
            double minX = (i & 1) > 0 ? center.x() : bounds.minX;
            double minZ = (i & 2) > 0 ? center.z() : bounds.minZ;
            double maxX = (i & 1) > 0 ? bounds.maxX : center.x();
            double maxZ = (i & 2) > 0 ? bounds.maxZ : center.z();

            children[i] = new QuadTreeNode(new AABB(minX, bounds.minY, minZ, maxX, bounds.maxY, maxZ));
        }

        for (Entity entity : entities.keySet()) {
            int index = getChildIndex(entity.getBoundingBox());
            children[index].insert(entity);
        }
        entities.clear();
    }

    void insert(Entity entity) {
        if (!bounds.intersects(entity.getBoundingBox())) return;

        if (children[0] == null) {
            if (entities.size() > MAX_ENTITIES_PER_NODE) {
                splitNode();
                int index = getChildIndex(entity.getBoundingBox());
                children[index].insert(entity);
            } else {
                entities.put(entity, true);
            }
        } else {
            int index = getChildIndex(entity.getBoundingBox());
            children[index].insert(entity);
        }
    }

    void remove(Entity entity) {
        if (!bounds.intersects(entity.getBoundingBox())) return;

        if (children[0] == null) {
            entities.remove(entity);
        } else {
            int index = getChildIndex(entity.getBoundingBox());
            children[index].remove(entity);
        }
    }

    <T extends Entity> ConcurrentHashMap<T, Boolean> getEntitiesInAABB(AABB queryAABB, EntityTypeTest<Entity, T> type, Predicate<? super T> predicate) {
        ConcurrentHashMap<T, Boolean> result = new ConcurrentHashMap<>();

        if (!bounds.intersects(queryAABB)) return result;

        entities.keySet().forEach((entity) -> {
            T castedEntity = type.tryCast(entity);
            if (castedEntity != null && queryAABB.intersects(entity.getBoundingBox()) && predicate.test(castedEntity)) {
                result.put(castedEntity, true);
            }
        });

        if(children[0] != null){
            for (QuadTreeNode child : children) {
                result.putAll(child.getEntitiesInAABB(queryAABB, type, predicate));
            }
        }

        return result;
    }

    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> type, AABB queryAABB){
        return this.getEntitiesOfClass(type, queryAABB, EntitySelector.NO_SPECTATORS);
    }

    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> type, AABB queryAABB, Predicate<? super T> predicate) {
        return this.getEntitiesInAABB(queryAABB, EntityTypeTest.forClass(type), predicate).keySet().parallelStream().collect(Collectors.toList());
    }
}

