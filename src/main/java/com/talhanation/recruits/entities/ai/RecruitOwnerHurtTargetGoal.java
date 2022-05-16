package com.talhanation.recruits.entities.ai;

/*
public class RecruitOwnerHurtTargetGoal extends TargetGoal {
    private final AbstractRecruitEntity recruitEntity;
    private LivingEntity ownerLastHurt;
    private int timestamp;


    public RecruitOwnerHurtTargetGoal(AbstractRecruitEntity recruitEntity) {
    super(recruitEntity, false);
    this.recruitEntity = recruitEntity;
    this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    public boolean canUse() {
        if (this.recruitEntity.isOwned() && !this.recruitEntity.isOrderedToHold()) {
            LivingEntity owner = this.recruitEntity.getOwner();
            if (owner == null) {
                return false;
            } else {
                this.ownerLastHurt = owner.getLastHurtMob();
                int i = owner.getLastHurtMobTimestamp();
                return i != this.timestamp && this.canAttack(this.ownerLastHurt, EntityPredicate.DEFAULT) && this.recruitEntity.wantsToAttack(this.ownerLastHurt, owner);
            }
        } else {
            return false;
        }
    }

    public void start() {
        this.mob.setTarget(this.ownerLastHurt);
        LivingEntity livingentity = this.recruitEntity.getOwner();
        if (livingentity != null) {
            this.timestamp = livingentity.getLastHurtMobTimestamp();
        }

        super.start();
    }
}
*/