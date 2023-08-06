package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.compat.CrossbowWeapon;
import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.core.BlockPos;
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
    private final double stopRange;

    public RecruitRangedCrossbowAttackGoal(CrossBowmanEntity crossBowman, double stopRange) {
        this.weapon = new CrossbowWeapon();
        this.crossBowman = crossBowman;
        this.speedModifier = this.weapon.getMoveSpeedAmp();
        this.stopRange = stopRange;
    }

    public boolean canUse() {
        LivingEntity livingentity = this.crossBowman.getTarget();
        if (livingentity != null && livingentity.isAlive() && this.isWeaponInMainHand()) {
            // if (mob.getOwner() != null && mob.getShouldFollow() && mob.getOwner().distanceTo(this.mob) <= 25.00D && !(target.distanceTo(this.mob) <= 7.00D)) return false;
            return livingentity.distanceTo(this.crossBowman) >= stopRange && this.canAttackHoldPos() && this.canAttackMovePos() && !this.crossBowman.needsToGetFood() && !this.crossBowman.getShouldMount();
        } else {
            return crossBowman.getShouldStrategicFire();
        }
    }

    public boolean canContinueToUse() {
        return this.canUse() && this.isWeaponInMainHand();
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

        if (target != null && target.isAlive()) {
            double distanceToTarget = target.distanceTo(this.crossBowman);
            boolean isFar = distanceToTarget >= 26D;
            boolean inRange = !isFar && distanceToTarget <= 15.0D;

            if (!crossBowman.isFollowing()){
                if (inRange){
                    this.crossBowman.setAggressive(true);
                    this.crossBowman.getNavigation().stop();
                }
                else {
                    this.crossBowman.setAggressive(true);
                    this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
                }
            }

            if (crossBowman.getShouldHoldPos() && crossBowman.getHoldPos() != null) {
                if ((!crossBowman.getHoldPos().closerThan(crossBowman.getOnPos(), 5D))){
                    this.crossBowman.setAggressive(true);
                    this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
                }
            }

        }

        //WEAPON HANDLING
        if(isWeaponInMainHand()) {
            if(crossBowman.getShouldStrategicFire() && target == null) {
                BlockPos pos = crossBowman.getStrategicFirePos();
                    switch (state) {
                        case RELOAD -> {
                            if (weapon.isLoaded(crossBowman.getMainHandItem())) {
                                if (pos != null) {
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
                            if (pos != null) {
                                this.crossBowman.setAggressive(true);
                                this.crossBowman.getLookControl().setLookAt(pos.getX(), pos.getY(), pos.getZ());

                                this.seeTime++;

                                if (this.seeTime >= 15 + crossBowman.getRandom().nextInt(8)) {
                                    this.state = State.SHOOT;
                                    this.seeTime = 0;
                                }
                            }
                        }


                        case SHOOT -> {
                            if (pos != null) {
                                double distance = this.crossBowman.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                                this.weapon.performRangedAttackIWeapon(this.crossBowman, pos.getX(), pos.getY() + distance * 0.0025, pos.getZ(), weapon.getProjectileSpeed());
                                CrossbowItem.setCharged(this.crossBowman.getMainHandItem(), false);
                            }
                            this.state = State.RELOAD;
                        }
                    }
                }
                else {
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
                            if (target != null && target.isAlive() && this.crossBowman.canAttack(target) && this.crossBowman.getState() != 3) {
                                this.crossBowman.getLookControl().setLookAt(target);

                                this.weapon.performRangedAttackIWeapon(this.crossBowman, target.getX(), target.getY(), target.getZ(), weapon.getProjectileSpeed());
                                CrossbowItem.setCharged(this.crossBowman.getMainHandItem(), false);
                            }
                            this.state = State.RELOAD;
                        }
                    }
                }
        }
    }


    enum State{
        RELOAD,
        AIMING,
        SHOOT,
    }

    private boolean canAttackHoldPos() {
        LivingEntity target = this.crossBowman.getTarget();
        BlockPos pos = crossBowman.getHoldPos();

        if (target != null && pos != null && crossBowman.getShouldHoldPos()) {
            boolean targetIsFar = target.distanceTo(this.crossBowman) >= 21.5D;
            boolean posIsClose = pos.distSqr(this.crossBowman.getOnPos()) <= 15.0D;
            boolean posIsFar = pos.distSqr(this.crossBowman.getOnPos()) > 15.0D;

            if (posIsFar) {
                return false;
            }

            else if (posIsClose && targetIsFar){
                return false;
            }
        }
        return true;
    }

    private boolean canAttackMovePos() {
        LivingEntity target = this.crossBowman.getTarget();
        BlockPos pos = crossBowman.getMovePos();

        if (target != null && pos != null && crossBowman.getShouldMovePos()) {
            boolean targetIsFar = target.distanceTo(this.crossBowman) >= 21.5D;
            boolean posIsClose = pos.distSqr(this.crossBowman.getOnPos()) <= 15.0D;
            boolean posIsFar = pos.distSqr(this.crossBowman.getOnPos()) > 15.0D;

            if (posIsFar) {
                return false;
            }

            else if (posIsClose && targetIsFar){
                return false;
            }
        }
        return true;
    }
}
