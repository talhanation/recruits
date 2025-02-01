package com.talhanation.recruits.pathfinding;

import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NodeEvaluatorCache {
    private static final Map<NodeEvaluatorGenerator, ConcurrentLinkedQueue<NodeEvaluator>> threadLocalNodeEvaluators = new ConcurrentHashMap<>();
    private static final Map<NodeEvaluator, NodeEvaluatorGenerator> nodeEvaluatorToGenerator = new ConcurrentHashMap<>();

    private static @NotNull Queue<NodeEvaluator> getDequeForGenerator(@NotNull NodeEvaluatorGenerator generator) {
        return threadLocalNodeEvaluators.computeIfAbsent(generator, (key) -> new ConcurrentLinkedQueue<>());
    }

    public static @NotNull NodeEvaluator takeNodeEvaluator(@NotNull NodeEvaluatorGenerator generator) {
        var nodeEvaluator = getDequeForGenerator(generator).poll();

        if (nodeEvaluator == null) {
            nodeEvaluator = generator.generate();
        }

        nodeEvaluatorToGenerator.put(nodeEvaluator, generator);

        return nodeEvaluator;
    }

    public static void returnNodeEvaluator(@NotNull NodeEvaluator nodeEvaluator) {
        final var generator = nodeEvaluatorToGenerator.remove(nodeEvaluator);
        Validate.notNull(generator, "NodeEvaluator already returned");

        getDequeForGenerator(generator).offer(nodeEvaluator);
    }
}