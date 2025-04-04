package com.talhanation.recruits.util;

import net.minecraft.world.level.pathfinder.Node;

import java.util.List;

public class PathAnalyzer {

    public static double pathStraightness(List<Node> path) {
        if (path == null || path.size() < 3) {
            return 100;
        }

        double totalAngleChange = 0;
        int segmentCount = 0;

        for (int i = 1; i < path.size() - 1; i++) {
            Node prev = path.get(i - 1);
            Node current = path.get(i);
            Node next = path.get(i + 1);

            double angle = calculateAngle(prev, current, next);
            totalAngleChange += Math.abs(angle);
            segmentCount++;
        }

        if (segmentCount == 0) return 100;

        return totalAngleChange / segmentCount;
    }

    private static double calculateAngle(Node a, Node b, Node c) {
        double dx1 = b.x - a.x;
        double dz1 = b.z - a.z;
        double dx2 = c.x - b.x;
        double dz2 = c.z - b.z;

        double dotProduct = dx1 * dx2 + dz1 * dz2;
        double mag1 = Math.sqrt(dx1 * dx1 + dz1 * dz1);
        double mag2 = Math.sqrt(dx2 * dx2 + dz2 * dz2);

        if (mag1 == 0 || mag2 == 0) return 0;

        double cosTheta = dotProduct / (mag1 * mag2);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosTheta)))); // Begrenzung für numerische Stabilität
    }
}

