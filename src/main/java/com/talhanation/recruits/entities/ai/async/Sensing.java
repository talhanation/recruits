package com.talhanation.recruits.entities.ai.async;

import com.talhanation.recruits.config.RecruitsServerConfig;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class Sensing extends net.minecraft.world.entity.ai.sensing.Sensing {
    private final Function<Entity, Boolean> lineOfSightSupplier;
    private final Mob mob;

    public Sensing(Mob p_26788_) {
        super(p_26788_);
        this.mob = p_26788_;
        if (RecruitsServerConfig.UseVisibilityCache.get()) {
            this.lineOfSightSupplier = (entity) -> VisibilityGraphCache.canSee(this.mob, (Entity) entity);
        } else {
            this.lineOfSightSupplier = (entity) -> this.mob.hasLineOfSight((Entity) entity);
        }
    }

    @Override
    public boolean hasLineOfSight(Entity p_148307_) {
        this.mob.level.getProfiler().push("hasLineOfSight");
        boolean flag = this.lineOfSightSupplier.apply(p_148307_);
        this.mob.level.getProfiler().pop();

        return flag;
    }
}