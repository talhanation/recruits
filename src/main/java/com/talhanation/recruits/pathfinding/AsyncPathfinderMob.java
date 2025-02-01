package com.talhanation.recruits.pathfinding;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public abstract class AsyncPathfinderMob extends PathfinderMob {
    protected AsyncPathfinderMob(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_) {
        super(p_21683_, p_21684_);
        this.navigation = this.createNavigation(p_21684_);
    }

    @Override
    public @NotNull PathNavigation getNavigation() {
        if (this.isPassenger() && this.getVehicle() instanceof Mob mob) {
            return mob.getNavigation();
        } else {
            return this.navigation;
        }
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level p_21480_) {
        return new AsyncGroundPathNavigation(this, p_21480_);
    }
}
