package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.compat.CrossbowWeapon;
import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import com.talhanation.recruits.util.AttackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class RecruitRangedCrossbowAttackGoal extends Goal {
    private final CrossBowmanEntity crossBowman;
    private final double speedModifier;
    private int seeTime;
    private State state;
    private IWeapon weapon;
    private LivingEntity target;
    private final double stopRange;
    private boolean consumeArrows;

    public RecruitRangedCrossbowAttackGoal(CrossBowmanEntity crossBowman, double stopRange) {
        this.weapon = new CrossbowWeapon();
        this.crossBowman = crossBowman;
        this.speedModifier = this.weapon.getMoveSpeedAmp();
        this.stopRange = stopRange;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        this.consumeArrows = RecruitsServerConfig.RangedRecruitsNeedArrowsToShoot.get();
    }

    public boolean canUse() {
        this.target = this.crossBowman.getTarget();
        boolean shouldRanged = crossBowman.getShouldRanged();
        if (target != null && target.isAlive() && this.isWeaponInMainHand() && shouldRanged) {
            // if (mob.getOwner() != null && mob.getShouldFollow() && mob.getOwner().distanceTo(this.mob) <= 25.00D && !(target.distanceTo(this.mob) <= 7.00D)) return false;
            return target.distanceTo(this.crossBowman) >= stopRange && this.canAttackMovePos() && !this.crossBowman.needsToGetFood() && !this.crossBowman.getShouldMount();
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
        this.state = State.IDLE;
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
        if(target != null && target.isAlive()) {
            double distance = target.distanceToSqr(this.crossBowman);
            boolean isClose = distance <= 150;
            boolean isFar = distance >= 3500;
            boolean isTooFar = distance >= 4500;
            boolean inRange =  !isFar;
            boolean canSee = this.crossBowman.getSensing().hasLineOfSight(target);

            if (canSee) {
                ++this.seeTime;
            } else {
                this.seeTime = 0;
            }

            if(isTooFar){
                this.crossBowman.setTarget(null);
                this.stop();
                return;
            }

            // movement
            if (crossBowman.getShouldFollow() && crossBowman.getOwner() != null) {
                handleFollow(this.crossBowman.getOwner(), inRange, isFar);
            }
            else if(crossBowman.getShouldHoldPos() && crossBowman.getHoldPos() != null){
                handleHoldPos(crossBowman.getHoldPos(), inRange);
            }
            else {
                handleWander(inRange, isFar, isClose);
            }
        }

        //WEAPON HANDLING
        if(isWeaponInMainHand()) {
            if(crossBowman.getShouldStrategicFire() && target == null) {
                BlockPos pos = crossBowman.getStrategicFirePos();
                    switch (state) {
                        case IDLE -> {
                            if (weapon.isLoaded(crossBowman.getMainHandItem())) {
                                if (pos != null) {
                                    this.state = State.AIMING;
                                }
                            } else if(hasArrows()) this.state = State.RELOAD;
                        }

                        case RELOAD -> {
                            this.crossBowman.startUsingItem(InteractionHand.MAIN_HAND);

                            int i = this.crossBowman.getTicksUsingItem();
                            if (i >= weapon.getWeaponLoadTime()) {
                                this.crossBowman.releaseUsingItem();
                                this.crossBowman.playSound(this.weapon.getLoadSound(), 1.0F, 1.0F / (crossBowman.getRandom().nextFloat() * 0.4F + 0.8F));
                                CrossbowItem.setCharged(this.crossBowman.getMainHandItem(), true);

                                this.state = State.AIMING;
                            }
                        }

                        case AIMING -> {
                            if (pos != null) {
                                this.crossBowman.setAggressive(true);
                                this.crossBowman.getLookControl().setLookAt(pos.getX(), pos.getY(), pos.getZ());

                                if (++this.seeTime > weapon.getWeaponLoadTime()) {
                                    this.state = State.SHOOT;
                                    this.seeTime = 0;
                                }
                            }
                        }

                        case SHOOT -> {
                            if (pos != null) {
                                this.weapon.performRangedAttackIWeapon(this.crossBowman, pos.getX(), pos.getY(), pos.getZ(), weapon.getProjectileSpeed());
                                CrossbowItem.setCharged(this.crossBowman.getMainHandItem(), false);
                            }
                            this.state = State.IDLE; //RESUPPLY
                        }
                    }
                }
                else {
                    switch (state) {
                        case IDLE -> {
                            if (weapon.isLoaded(crossBowman.getMainHandItem())) {
                                if (target != null && target.isAlive()) {
                                    this.state = State.AIMING;
                                }
                            } else if(hasArrows()) this.state = State.RELOAD;
                        }
                        case RELOAD -> {
                            this.crossBowman.startUsingItem(InteractionHand.MAIN_HAND);

                            int i = this.crossBowman.getTicksUsingItem();
                            if (i >= weapon.getWeaponLoadTime()) {
                                this.crossBowman.releaseUsingItem();
                                this.crossBowman.playSound(this.weapon.getLoadSound(), 1.0F, 1.0F / (crossBowman.getRandom().nextFloat() * 0.4F + 0.8F));
                                CrossbowItem.setCharged(this.crossBowman.getMainHandItem(), true);

                                this.state = State.AIMING;
                            }
                        }

                        case AIMING -> {
                            if (target != null && target.isAlive()) {
                                boolean canSee = this.crossBowman.getSensing().hasLineOfSight(target);
                                this.crossBowman.setAggressive(true);
                                this.crossBowman.getLookControl().setLookAt(target);

                                if (canSee) {
                                    if (++this.seeTime >= weapon.getWeaponLoadTime()) {
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
                            LivingEntity savedTarget = null;
                            if (target != null && target.isAlive() && this.crossBowman.canAttack(target) && this.crossBowman.getState() != 3) {
                                this.crossBowman.getLookControl().setLookAt(target);

                                if(AttackUtil.canPerformHorseAttack(this.crossBowman, target)){
                                    if(target.getVehicle() instanceof LivingEntity) {
                                        savedTarget = target;
                                        target = (LivingEntity) target.getVehicle();
                                    }
                                }

                                this.weapon.performRangedAttackIWeapon(this.crossBowman, target.getX(), target.getY() + target.getEyeHeight(), target.getZ(), weapon.getProjectileSpeed());

                                if(savedTarget != null){
                                    target = savedTarget;
                                }

                                CrossbowItem.setCharged(this.crossBowman.getMainHandItem(), false);
                            }
                            this.state = State.IDLE;
                        }
                    }
                }
        }
    }


    enum State{
        IDLE,
        RELOAD,
        AIMING,
        SHOOT,
    }

    private boolean hasArrows(){
        return !consumeArrows || this.crossBowman.getInventory().hasAnyMatching(item -> item.is(ItemTags.ARROWS));
    }

    private boolean canAttackMovePos() {
        LivingEntity target = this.crossBowman.getTarget();
        BlockPos pos = crossBowman.getMovePos();

        if (target != null && pos != null && crossBowman.getShouldMovePos()) {
            boolean targetIsFar = target.distanceTo(this.crossBowman) >= 32D;
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


    private void handleFollow(@NotNull LivingEntity owner, boolean inRange, boolean isFar){
        boolean ownerClose = owner.distanceToSqr(this.crossBowman) <= 100;

        if (ownerClose) {
            if (inRange) this.crossBowman.getNavigation().stop();
            if (isFar) this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
        }
        //if (!ownerClose) {
        //    this.mob.getNavigation().moveTo(owner, this.speedModifier);
        //}
    }

    private void handleHoldPos(@NotNull Vec3 pos, boolean inRange){
        boolean posClose = pos.distanceToSqr(this.crossBowman.position()) <= 50;

        if (posClose) {
            if (inRange) this.crossBowman.getNavigation().stop();
        }
        else {
            //this.mob.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1);
        }
        //if (!ownerClose) {
        //    this.mob.getNavigation().moveTo(owner, this.speedModifier);
        //}
    }

    private void handleWander(boolean inRange, boolean isFar, boolean isClose){
        if (inRange) this.crossBowman.getNavigation().stop();
        if (isFar) this.crossBowman.getNavigation().moveTo(crossBowman, this.speedModifier);
    }
}
