package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.GameRules;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public class RecruitHurtByTargetGoal extends HurtByTargetGoal {
    private static final TargetingConditions HURT_BY_TARGETING = (new TargetingConditions()).allowUnseeable().ignoreInvisibilityTesting();
    private boolean alertSameType;
    private int timestamp;
    private final Class<?>[] toIgnoreDamage;
    private Class<?>[] toIgnoreAlert;
    private final AbstractRecruitEntity recruit;

    public RecruitHurtByTargetGoal(AbstractRecruitEntity recruit, Class<?>... classes) {
        super(recruit, classes);
        this.recruit = recruit;
        this.toIgnoreDamage = classes;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public boolean canUse() {
        int i = this.recruit.getLastHurtByMobTimestamp();
        LivingEntity livingentity = this.recruit.getLastHurtByMob();

        if (livingentity instanceof AbstractRecruitEntity) {
            AbstractRecruitEntity targetRecruit = (AbstractRecruitEntity) livingentity;
            if(Objects.equals(targetRecruit.getOwnerUUID(), this.recruit.getOwnerUUID()))
            return false;
        }

        if(i != this.timestamp && livingentity != null) {
            if (livingentity.getType() == EntityType.PLAYER && this.recruit.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                return false;
            } else {
                for (Class<?> oclass : this.toIgnoreDamage) {
                    if (oclass.isAssignableFrom(livingentity.getClass())) {
                        return false;
                    }
                }

                return this.canAttack(livingentity, HURT_BY_TARGETING) && (recruit.getState() != 3);
            }
        } else {
            return false;
        }
    }

    public net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal setAlertOthers(Class<?>... p_220794_1_) {
        this.alertSameType = true;
        this.toIgnoreAlert = p_220794_1_;
        return this;
    }

    public void start() {
        this.recruit.setTarget(this.recruit.getLastHurtByMob());
        this.targetMob = this.recruit.getTarget();
        this.timestamp = this.recruit.getLastHurtByMobTimestamp();
        this.unseenMemoryTicks = 300;
        if (this.alertSameType) {
            this.alertOthers();
        }

        super.start();
    }

    protected void alertOthers() {
        double d0 = this.getFollowDistance();
        AABB axisalignedbb = AABB.unitCubeFromLowerCorner(this.recruit.position()).inflate(d0, 10.0D, d0);
        List<Mob> list = this.recruit.level.getLoadedEntitiesOfClass(this.recruit.getClass(), axisalignedbb);
        Iterator iterator = list.iterator();

        while (true) {
            Mob mobentity;
            while (true) {
                if (!iterator.hasNext()) {
                    return;
                }

                mobentity = (Mob) iterator.next();
                if (this.recruit != mobentity && mobentity.getTarget() == null && (this.recruit).getOwner() == ((TamableAnimal) mobentity).getOwner() && !mobentity.isAlliedTo(this.recruit.getLastHurtByMob())) {
                    if (this.toIgnoreAlert == null) {
                        break;
                    }

                    boolean flag = false;

                    for (Class<?> oclass : this.toIgnoreAlert) {
                        if (mobentity.getClass() == oclass) {
                            flag = true;
                            break;
                        }
                    }

                    if (!flag) {
                        break;
                    }
                }
            }

            this.alertOther(mobentity, this.recruit.getLastHurtByMob());
        }
    }

    protected void alertOther(Mob p_220793_1_, LivingEntity p_220793_2_) {
        p_220793_1_.setTarget(p_220793_2_);
    }
}
