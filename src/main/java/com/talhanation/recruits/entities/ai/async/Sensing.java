package com.talhanation.recruits.entities.ai.async;

import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public class Sensing extends net.minecraft.world.entity.ai.sensing.Sensing {
    private final boolean useCache;
    private final Mob mob;

    public Sensing(Mob p_26788_) {
        super(p_26788_);
        this.mob = p_26788_;
        useCache = RecruitsServerConfig.UseVisibilityCache.get() || RecruitsServerConfig.UseAsyncTargetFinding.get();
    }

    @Override
    public synchronized boolean hasLineOfSight(Entity p_148307_) {
        return this.hasLineOfSight(p_148307_, false);
    }

    public synchronized boolean hasLineOfSight(Entity target, boolean concurrent) {
        this.mob.level.getProfiler().push("hasLineOfSight");

        CacheEntry entry = null;
        if (useCache && (entry = VisibilityGraphCache.canSee(this.mob, target)) != null && entry.getCanSee()) return true;

        if (!concurrent && this.mob.hasLineOfSight(target)) return true;

        this.mob.level.getProfiler().pop();
        return false;
    }
}