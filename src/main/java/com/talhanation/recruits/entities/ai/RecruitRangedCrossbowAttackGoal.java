package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.compat.CrossbowWeapon;
import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.compat.MusketWeapon;
import com.talhanation.recruits.compat.PistolWeapon;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import com.talhanation.recruits.entities.ai.compat.RecruitRangedMusketAttackGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import java.util.EnumSet;

public class RecruitRangedCrossbowAttackGoal extends Goal {
    private final CrossBowmanEntity crossBowman;
    private final double speedModifier;
    private int seeTime;
    private State state;
    private IWeapon weapon;

    public RecruitRangedCrossbowAttackGoal(CrossBowmanEntity crossBowman) {
        this.weapon = new MusketWeapon();
        this.crossBowman = crossBowman;
        this.speedModifier = this.weapon.getMoveSpeedAmp();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.crossBowman.getTarget() != null && this.isWeaponInMainHand();
    }

    @Override
    public boolean canContinueToUse() {
        return (this.canUse() || this.crossBowman.getNavigation().getPath() != null) && this.isWeaponInMainHand();
    }

    @Override
    public void start() {
        super.start();
        this.crossBowman.setAggressive(true);
        this.state = State.RELOAD;
    }

    @Override
    public void stop() {
        super.stop();
        this.seeTime = 0;
        this.crossBowman.stopUsingItem();
        this.crossBowman.setAggressive(false);
    }

    protected boolean isWeaponInMainHand() {
        ItemStack itemStack = crossBowman.getItemBySlot(crossBowman.getEquipmentSlotIndex(5));
        if(itemStack.getItem() instanceof CrossbowItem) {
            this.weapon = new CrossbowWeapon();
            return true;
        }
        else
            return false;
    }

    public void tick() {
        LivingEntity target = this.crossBowman.getTarget();

        if (target != null) {
            double distanceToTarget = target.distanceTo(this.crossBowman);
            boolean isClose = distanceToTarget <= 7.00D;
            boolean isFar = distanceToTarget >= 21.5D;
            boolean inRange =  !isFar && distanceToTarget <= 15.0D;

            // movement
            if (crossBowman.getShouldHoldPos() && crossBowman.getHoldPos() != null) {
                if ((!crossBowman.getHoldPos().closerThan(crossBowman.getOnPos(), 5D))){
                    if (inRange) this.crossBowman.getNavigation().stop();
                    if (isFar) this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
                    if (isClose) this.crossBowman.fleeEntity(target);
                }
            }

            else if (crossBowman.getShouldFollow() && crossBowman.getOwner() != null){
                boolean playerClose = crossBowman.getOwner().distanceTo(this.crossBowman) <= 15.00D;

                if (playerClose){
                    if (inRange) this.crossBowman.getNavigation().stop();
                    if (isFar) this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
                    if (isClose) this.crossBowman.fleeEntity(target);
                }
                if (!playerClose) {
                    this.crossBowman.getNavigation().moveTo(crossBowman.getOwner(), this.speedModifier);
                }
            }

            else {
                if (inRange) this.crossBowman.getNavigation().stop();
                if (isFar) this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
                if (isClose) this.crossBowman.fleeEntity(target);
            }
        }

        //WEAPON HANDLING
        if(isWeaponInMainHand()) {
            switch (state) {
                case RELOAD -> {
                    if (weapon.isLoaded(crossBowman.getMainHandItem())) {
                        if (target != null && target.isAlive()) {
                            this.state = State.AIMING;
                        }
                    } else {
                        this.crossBowman.startUsingItem(InteractionHand.MAIN_HAND);

                        int i = this.crossBowman.getTicksUsingItem();
                        if (i >= weapon.getWeaponLoadTime()) {
                            this.crossBowman.releaseUsingItem();
                            this.crossBowman.playSound(this.weapon.getLoadSound(), 1.0F, 1.0F / (crossBowman.getRandom().nextFloat() * 0.4F + 0.8F));
                            CrossbowItem.setCharged(this.crossBowman.getMainHandItem(), true);

                            this.state = State.AIMING;
                        }
                    }
                }

                case AIMING -> {
                    if (target != null && target.isAlive()) {
                        boolean canSee = this.crossBowman.getSensing().hasLineOfSight(target);
                        this.crossBowman.setAggressive(true);
                        this.crossBowman.getLookControl().setLookAt(target);

                        if (canSee) {
                            this.seeTime++;

                            if (this.seeTime >= 15 + crossBowman.getRandom().nextInt(8)) {
                                this.state = State.SHOOT;
                                this.seeTime = 0;
                            }
                        } else if (crossBowman.getShouldHoldPos()) {
                            this.crossBowman.setTarget(null);
                        }
                    } else {
                        this.crossBowman.setAggressive(false);
                        seeTime = 0;
                    }
                }

                case SHOOT -> {
                    if (target != null && target.isAlive()) {
                        this.crossBowman.getLookControl().setLookAt(target);
                        this.weapon.performRangedAttackIWeapon(this.crossBowman, target, weapon.getProjectileSpeed());
                        CrossbowItem.setCharged(this.crossBowman.getMainHandItem(), false);
                    }
                    this.state = State.RELOAD;
                }
            }
        }
    }


    enum State{
        RELOAD,
        AIMING,
        SHOOT,
    }


    /*
    if (this.crossBowman.isUsingItem()) {
                if (!canSee && this.seeTime < -60) {
                    this.crossBowman.releaseUsingItem();
                    this.crossBowman.setAggressive(false);
                } else if (canSee) {
                    this.crossBowman.setAggressive(true);
                    int i = this.crossBowman.getTicksUsingItem();
                    if (i >= weapon.getWeaponLoadTime()) {
                        this.crossBowman.releaseUsingItem();
                        this.weapon.performRangedAttackIWeapon(this.crossBowman, target, weapon.getProjectileSpeed(i));

                        this.attackTime = this.attackCooldown;
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
                this.crossBowman.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.crossBowman, weapon.getWeapon()));
            }
     */
}
