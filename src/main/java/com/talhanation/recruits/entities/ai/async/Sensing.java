package com.talhanation.recruits.entities.ai.async;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class Sensing extends net.minecraft.world.entity.ai.sensing.Sensing {
    private final Mob mob;
    private final IntSet seen = new IntOpenHashSet();
    private final IntSet unseen = new IntOpenHashSet();

    public Sensing(Mob p_26788_) {
        super(p_26788_);
        this.mob = p_26788_;
    }

    @Override
    public void tick() {
        this.seen.clear();
        this.unseen.clear();
    }

    @Override
    public boolean hasLineOfSight(Entity p_148307_) {
        int i = p_148307_.getId();
        if (this.seen.contains(i)) {
            return true;
        } else if (this.unseen.contains(i)) {
            return false;
        } else {
            this.mob.level.getProfiler().push("hasLineOfSight");
            boolean flag = VisibilityGraphCache.canSee(this.mob, p_148307_);
            this.mob.level.getProfiler().pop();
            if (flag) {
                this.seen.add(i);
            } else {
                this.unseen.add(i);
            }

            return flag;
        }
    }
}