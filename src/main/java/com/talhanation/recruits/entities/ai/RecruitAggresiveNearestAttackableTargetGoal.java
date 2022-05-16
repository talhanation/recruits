package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Predicate;

import net.minecraft.world.entity.ai.goal.Goal.Flag;

public class RecruitAggresiveNearestAttackableTargetGoal<T extends LivingEntity> extends TargetGoal {
    protected final Class<T> targetType;
    public Player target;
    public AbstractRecruitEntity recruit;
    public TargetingConditions targetConditions;

    public RecruitAggresiveNearestAttackableTargetGoal(AbstractRecruitEntity recruit, Class<T> target, boolean p_i50313_3_) {
        this(recruit, target, p_i50313_3_, false);
        this.recruit = recruit;
    }

    public RecruitAggresiveNearestAttackableTargetGoal(AbstractRecruitEntity recruit, Class<T> target, boolean p_i50314_3_, boolean p_i50314_4_) {
        this(recruit, target, p_i50314_3_, p_i50314_4_, null);
    }

    public RecruitAggresiveNearestAttackableTargetGoal(AbstractRecruitEntity recruit, Class<T> target, boolean p_i50315_4_, boolean p_i50315_5_, @Nullable Predicate<LivingEntity> p_i50315_6_) {
        super(recruit, p_i50315_4_, p_i50315_5_);
        this.targetType = target;
        this.setFlags(EnumSet.of(Flag.TARGET));
        this.targetConditions = (new TargetingConditions()).range(this.getFollowDistance()).selector(p_i50315_6_);
    }

    public boolean canUse() {
        int state = recruit.getState();
        if (state == 1) {
            this.recruit.setTarget(null);
            this.findTarget();
            return isValidTargetPlayer(target);
        }
        return false;
    }

    protected void findTarget() {
        this.target = this.mob.level.getNearestPlayer(this.targetConditions, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
    }

    public void start() {
        this.mob.setTarget(this.target);
        super.start();
    }

    private boolean isValidTargetPlayer(Player player){
        //ATTACK PLAYERS
        if (player != null && player.getUUID() == recruit.getOwnerUUID()) {
            return false;
        }
        else
            return true;
    }

}