package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class RecruitHurtByTargetGoal extends HurtByTargetGoal {
    private static final TargetingConditions HURT_BY_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();;
    private boolean alertSameType;
    private int timestamp;
    private Class<?>[] toIgnoreAlert;
    private final AbstractRecruitEntity recruit;

    public RecruitHurtByTargetGoal(AbstractRecruitEntity recruit) {
        super(recruit);
        this.recruit = recruit;
    }

    public boolean canUse() {
        int i = this.recruit.getLastHurtByMobTimestamp();
        LivingEntity livingentity = this.recruit.getLastHurtByMob();

        if(i != this.timestamp && livingentity != null) {
                return this.canAttack(livingentity, HURT_BY_TARGETING) && (recruit.getState() != 3);
            }
        return false;
    }

    public @NotNull HurtByTargetGoal setAlertOthers(Class<?> @NotNull ... p_220794_1_) {
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
        AABB axisalignedbb = AABB.unitCubeFromLowerCorner(this.recruit.position()).inflate(d0, 16.0D, d0);
        List<? extends AbstractRecruitEntity> list = this.recruit.getCommandSenderWorld().getEntitiesOfClass(this.recruit.getClass(), axisalignedbb);
        Iterator iterator = list.iterator();

        while (true) {
            AbstractRecruitEntity recruitToAlert;
            while (true) {
                if (!iterator.hasNext()) {
                    return;
                }

                recruitToAlert = (AbstractRecruitEntity) iterator.next();
                if (this.recruit != recruitToAlert &&
                        recruitToAlert.getTarget() == null &&
                        this.recruit.getLastHurtByMob() != null &&
                        recruitToAlert.getOwnerUUID() != null &&
                        this.recruit.getOwnerUUID() != null &&
                        this.recruit.getOwnerUUID().equals((recruitToAlert).getOwnerUUID()) &&
                        !recruitToAlert.isAlliedTo(this.recruit.getLastHurtByMob())) {
                    if (this.toIgnoreAlert == null) {
                        break;
                    }

                    boolean flag = false;

                    for (Class<?> oclass : this.toIgnoreAlert) {
                        if (recruitToAlert.getClass() == oclass) {
                            flag = true;
                            break;
                        }
                    }

                    if (!flag) {
                        break;
                    }
                }
            }

            this.alertOther(recruitToAlert, this.recruit.getLastHurtByMob());
        }
    }

    protected void alertOther(Mob recruit, LivingEntity target) {
        recruit.setTarget(target);
    }
}
