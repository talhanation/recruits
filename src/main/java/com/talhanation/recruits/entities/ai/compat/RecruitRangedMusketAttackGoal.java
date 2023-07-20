package com.talhanation.recruits.entities.ai.compat;

import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.compat.MusketBayonetWeapon;
import com.talhanation.recruits.compat.MusketWeapon;
import com.talhanation.recruits.compat.PistolWeapon;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RecruitRangedMusketAttackGoal extends Goal {
    private final CrossBowmanEntity crossBowman;
    private final double speedModifier;
    private int attackTime = -1;
    private int seeTime;
    private State state;
    private boolean isInMainHand;
    private boolean isInOffHand;

    private IWeapon weapon;
    private int weaponLoadTime;
    private final double stopRange;
    public RecruitRangedMusketAttackGoal(CrossBowmanEntity crossBowman, double stopRange) {
        this.weapon = new MusketWeapon();
        this.crossBowman = crossBowman;
        this.speedModifier = this.weapon.getMoveSpeedAmp();
        this.stopRange = stopRange;
    }

    public boolean canUse() {
        LivingEntity livingentity = this.crossBowman.getTarget();
        if(livingentity != null && (this.isWeaponInHand())){
            return livingentity.distanceTo(this.crossBowman) >= stopRange;
        }
        else
            return crossBowman.getShouldStrategicFire() || (this.isWeaponInHand() && weapon != null && !weapon.isLoaded(crossBowman.getMainHandItem()));
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.crossBowman.setAggressive(true);
        this.state = State.IDLE;
        this.weaponLoadTime = crossBowman.isPassenger() ? weapon.getWeaponLoadTime() * 2 : weapon.getWeaponLoadTime();
        //this.checkHands();
    }

    @Override
    public void stop() {
        super.stop();
        this.seeTime = 0;
        this.attackTime = -1;
        this.crossBowman.stopUsingItem();
        this.crossBowman.setAggressive(false);
    }

    protected boolean isWeaponInHand() {
        ItemStack itemStack = crossBowman.getItemBySlot(crossBowman.getEquipmentSlotIndex(5));

        if(itemStack.getDescriptionId().equals("item.musketmod.musket")) {
            this.weapon = new MusketWeapon();
            return true;
        }
        else if(itemStack.getDescriptionId().equals("item.musketmod.musket_with_bayonet")){
            this.weapon = new MusketBayonetWeapon();
            return true;
        }
        else if(itemStack.getDescriptionId().equals("item.musketmod.pistol")){
            this.weapon = new PistolWeapon();
            return true;
        }
        else
            return false;
    }

    public void tick() {
        LivingEntity target = this.crossBowman.getTarget();

        if (target != null && target.isAlive()) {
            double distanceToTarget = target.distanceTo(this.crossBowman);
            boolean isFar = distanceToTarget >= 29D;
            boolean inRange = !isFar && distanceToTarget <= 17.0D;

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
        if (isWeaponInHand()) {
            if(crossBowman.getShouldStrategicFire() && target == null){
                BlockPos pos = crossBowman.getStrategicFirePos();
                if(pos != null) {
                    switch (state) {
                        case IDLE -> {
                            this.crossBowman.setAggressive(false);
                            State newState;
                            if (!weapon.isLoaded(crossBowman.getMainHandItem())) {
                                if (canLoad()) newState = State.RELOAD;
                                else newState = State.IDLE;
                            } else {
                                newState = State.AIMING;
                            }

                            this.state = newState;
                        }

                        case RELOAD -> {
                            this.crossBowman.startUsingItem(InteractionHand.MAIN_HAND);
                            int i = this.crossBowman.getTicksUsingItem();
                            if (i >= this.weaponLoadTime) {
                                this.crossBowman.releaseUsingItem();
                                this.crossBowman.playSound(this.weapon.getLoadSound(), 1.0F, 1.0F / (crossBowman.getRandom().nextFloat() * 0.4F + 0.8F));
                                this.weapon.setLoaded(crossBowman.getMainHandItem(), true);
                                this.consumeAmmo();

                                state = State.AIMING;
                            }
                        }

                        case AIMING -> {
                            this.crossBowman.getLookControl().setLookAt(Vec3.atCenterOf(pos));
                            this.crossBowman.setAggressive(true);
                            this.seeTime++;

                            if (this.seeTime >= 15 + crossBowman.getRandom().nextInt(15)) {
                                this.seeTime = 0;
                                this.state = State.SHOOT;
                            }
                        }

                        case SHOOT -> {
                            this.crossBowman.getLookControl().setLookAt(Vec3.atCenterOf(pos));
                            this.weapon.performRangedAttackIWeapon(this.crossBowman, pos.getX(), pos.getY(), pos.getZ(), weapon.getProjectileSpeed());
                            this.weapon.setLoaded(crossBowman.getMainHandItem(), false);

                            if (canLoad()) this.state = State.RELOAD;
                            else this.state = State.IDLE; //RESUPPLY
                        }
                    }
                }
            }
            else{
                switch (state) {
                    case IDLE -> {
                        this.crossBowman.setAggressive(false);
                        State newState;
                        if (!weapon.isLoaded(crossBowman.getMainHandItem())) {
                            if (canLoad()) newState = State.RELOAD;
                            else newState = State.IDLE;
                        } else if (target != null && target.isAlive()) {
                            newState = State.AIMING;
                        } else {
                            newState = State.IDLE;

                        }

                        this.state = newState;
                    }

                    case RELOAD -> {
                        this.crossBowman.startUsingItem(InteractionHand.MAIN_HAND);
                        int i = this.crossBowman.getTicksUsingItem();
                        if (i >= this.weaponLoadTime) {
                            this.crossBowman.releaseUsingItem();
                            this.crossBowman.playSound(this.weapon.getLoadSound(), 1.0F, 1.0F / (crossBowman.getRandom().nextFloat() * 0.4F + 0.8F));
                            this.weapon.setLoaded(crossBowman.getMainHandItem(), true);
                            this.consumeAmmo();
                            if (target != null && target.isAlive()) state = State.AIMING;
                            else state = State.IDLE;
                        }
                    }

                    case AIMING -> {
                        boolean canSee = target != null && this.crossBowman.getSensing().hasLineOfSight(target) && target.isAlive();
                        if (canSee) {
                            this.crossBowman.getLookControl().setLookAt(target);
                            this.crossBowman.setAggressive(true);
                            this.seeTime++;

                            if (this.seeTime >= 10 + crossBowman.getRandom().nextInt(8)) {
                                this.state = State.SHOOT;
                                this.seeTime = 0;
                            }
                        } else {
                            this.crossBowman.setAggressive(false);
                            this.seeTime = 0;
                            state = State.IDLE;
                        }
                    }

                    case SHOOT -> {
                        if (target != null && target.isAlive() && this.crossBowman.canAttack(target) && this.crossBowman.getState() != 3) {
                            this.crossBowman.getLookControl().setLookAt(target);
                            this.weapon.performRangedAttackIWeapon(this.crossBowman, target.getX(), target.getY(), target.getZ(), weapon.getProjectileSpeed());
                            this.weapon.setLoaded(crossBowman.getMainHandItem(), false);
                        }
                        if (canLoad()) this.state = State.RELOAD;
                        else this.state = State.IDLE; //RESUPPLY
                    }
                }
            }

        }
    }
    private void consumeAmmo() {
        List<ItemStack> items = this.crossBowman.getInventory().items;

        for (ItemStack stack : items) {
            if (stack.getDescriptionId().equals("item.musketmod.cartridge")) {
                stack.shrink(1);
                break;
            }
        }
    }

    private boolean canLoad(){
        return this.crossBowman.getInventory().items.stream().anyMatch(itemStack -> itemStack.getDescriptionId().equals("item.musketmod.cartridge"));
    }

    public void checkHands(){
        //this.isInMainHand = isWeaponInHand(InteractionHand.MAIN_HAND);
        //this.isInOffHand = isWeaponInHand(InteractionHand.OFF_HAND);
    }

    private void switchWeaponToMainHandFromOffHand() {
        ItemStack off = this.crossBowman.getOffhandItem().copy().copy() ;

        this.crossBowman.getInventory().setItem(this.crossBowman.getInventorySlotIndex(EquipmentSlot.MAINHAND), off);
        this.crossBowman.setItemSlot(EquipmentSlot.MAINHAND, off);


        this.crossBowman.getInventory().removeItemNoUpdate(this.crossBowman.getInventorySlotIndex(EquipmentSlot.OFFHAND));
        this.crossBowman.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.crossBowman.getInventory().setItem(this.crossBowman.getInventorySlotIndex(EquipmentSlot.OFFHAND), ItemStack.EMPTY);


        checkHands();
    }

    private void switchWeaponToOffHandFromMainHand() {
        ItemStack main = this.crossBowman.getMainHandItem().copy().copy();

        this.crossBowman.getInventory().setItem(this.crossBowman.getInventorySlotIndex(EquipmentSlot.OFFHAND), main);
        this.crossBowman.setItemSlot(EquipmentSlot.OFFHAND, main);

        this.crossBowman.getInventory().removeItemNoUpdate(this.crossBowman.getInventorySlotIndex(EquipmentSlot.MAINHAND));
        this.crossBowman.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.crossBowman.getInventory().setItem(this.crossBowman.getInventorySlotIndex(EquipmentSlot.MAINHAND), ItemStack.EMPTY);


        checkHands();
    }



    enum State{
        IDLE,
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
