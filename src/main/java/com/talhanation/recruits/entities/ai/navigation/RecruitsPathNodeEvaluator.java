package com.talhanation.recruits.entities.ai.navigation;

import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Recruit ground path node evaluator.
 *
 * In 1.20.1 this was a full copy of vanilla {@link WalkNodeEvaluator} carrying a
 * {@code setTarget} hook whose stored coordinates were never actually read. The 1.21
 * pathfinder rework ({@code PathfindingContext}) makes maintaining a hand-copied
 * evaluator unnecessary, so this now extends vanilla {@link WalkNodeEvaluator}
 * directly and keeps {@code setTarget} for API compatibility with the recruit
 * path navigations.
 */
public class RecruitsPathNodeEvaluator extends WalkNodeEvaluator {

    @SuppressWarnings("unused")
    private int x, y, z;

    public void setTarget(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
