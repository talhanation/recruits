package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.RecruitHorseEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Predicate;

public class RecruitRaidNearestAttackableTargetGoal<T extends LivingEntity> extends TargetGoal {
    protected final Class<T> targetType;
    public LivingEntity target;
    public AbstractRecruitEntity recruit;
    public EntityPredicate targetConditions;

    public RecruitRaidNearestAttackableTargetGoal(AbstractRecruitEntity recruit, Class<T> target, boolean p_i50313_3_) {
        this(recruit, target, p_i50313_3_, false);
        this.recruit = recruit;
    }

    public RecruitRaidNearestAttackableTargetGoal(AbstractRecruitEntity recruit, Class<T> target, boolean p_i50314_3_, boolean p_i50314_4_) {
        this(recruit, target, p_i50314_3_, p_i50314_4_, (Predicate<LivingEntity>)null);
    }

    public RecruitRaidNearestAttackableTargetGoal(AbstractRecruitEntity recruit, Class<T> target, boolean p_i50315_4_, boolean p_i50315_5_, @Nullable Predicate<LivingEntity> p_i50315_6_) {
        super(recruit, p_i50315_4_, p_i50315_5_);
        this.targetType = target;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.targetConditions = (new EntityPredicate()).range(this.getFollowDistance()).selector(p_i50315_6_);
    }

    public boolean canUse() {
        int state = recruit.getState();
        if (state == 2) {
            this.recruit.setTarget(null);
            this.findTarget();

            if (target instanceof PlayerEntity)
                return isValidTargetPlayer((PlayerEntity)target);

            else
                return isValidTarget(target);
        }
        return false;
    }

    protected AxisAlignedBB getTargetSearchArea(double area) {
        return this.mob.getBoundingBox().inflate(area, 8.0D, area);
    }

    protected void findTarget() {
        if(this.targetType != PlayerEntity.class && this.targetType != ServerPlayerEntity.class) {
            this.target = this.mob.level.getNearestLoadedEntity(this.targetType, this.targetConditions, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ(), this.getTargetSearchArea(this.getFollowDistance()));
        } else {
            this.target = this.mob.level.getNearestPlayer(this.targetConditions, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
        }
    }

    public void start() {
        this.mob.setTarget(this.target);
        super.start();
    }

    private boolean isValidTarget(LivingEntity living){
        //OTHER RECRUITS RAID
        if (living instanceof AbstractRecruitEntity) {
            AbstractRecruitEntity otherRecruit = (AbstractRecruitEntity) living;
            if (otherRecruit.isTame())
                return false;
        }
        if (living instanceof RecruitHorseEntity){
                return false;
        }
        return true;
    }

    private boolean isValidTargetPlayer(PlayerEntity player){
        //RAID PLAYERS
        if (player.getUUID() == recruit.getOwnerUUID()) {
            return false;
        }
        else
            return true;
    }
}