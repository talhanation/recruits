package com.talhanation.recruits.entities.ai.compat;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.compat.MusketWeapon;
import com.talhanation.recruits.compat.PistolWeapon;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import java.util.EnumSet;
import java.util.List;

public class RecruitRangedMusketAttackGoal extends Goal {
    private final CrossBowmanEntity crossBowman;
    private final double speedModifier;
    private final int attackCooldown;
    private int attackTime = -1;
    private int seeTime;
    private State state;
    private boolean isInMainHand;
    private boolean isInOffHand;

    private IWeapon weapon;

    public RecruitRangedMusketAttackGoal(CrossBowmanEntity crossBowman) {
        this.weapon = new MusketWeapon();
        this.crossBowman = crossBowman;
        this.speedModifier = this.weapon.getMoveSpeedAmp();
        this.attackCooldown = this.weapon.getAttackCooldown();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if(this.crossBowman.getTarget() != null && (this.isWeaponInHand())){
            return true;
        }
        else
            return (this.isWeaponInHand() && weapon != null && !weapon.isLoaded(crossBowman.getMainHandItem()));
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
            boolean isClose = distanceToTarget <= 7.00D;
            boolean isFar = distanceToTarget >= 21.5D;
            boolean inRange = !isFar && distanceToTarget <= 15.0D;
            //if (bowman.getHoldPos() != null)Objects.requireNonNull(this.bowman.getOwner()).sendMessage(new StringTextComponent("Pos vorhanden"), bowman.getOwner().getUUID());


            // movement
            if (crossBowman.getShouldHoldPos() && crossBowman.getHoldPos() != null) {
                if ((!crossBowman.getHoldPos().closerThan(crossBowman.getOnPos(), 5D))) {
                    if (inRange) this.crossBowman.getNavigation().stop();
                    if (isFar) this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
                    if (isClose) this.crossBowman.fleeEntity(target);
                }
            } else if (crossBowman.getShouldFollow() && crossBowman.getOwner() != null) {
                boolean playerClose = crossBowman.getOwner().distanceTo(this.crossBowman) <= 15.00D;

                if (playerClose) {
                    if (inRange) this.crossBowman.getNavigation().stop();
                    if (isFar) this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
                    if (isClose) this.crossBowman.fleeEntity(target);
                }
                if (!playerClose) {
                    this.crossBowman.getNavigation().moveTo(crossBowman.getOwner(), this.speedModifier);
                }
            } else {
                if (inRange) this.crossBowman.getNavigation().stop();
                if (isFar) this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
                if (isClose) this.crossBowman.fleeEntity(target);
            }
        }

        //WEAPON HANDLING
        if (isWeaponInHand()) {
            switch (state) {
                case IDLE -> {

                    //if(!isInOffHand)
                    //    switchWeaponToOffHandFromMainHand();
                    this.crossBowman.setAggressive(false);
                    State newState;
                    if (isInMainHand && !weapon.isLoaded(crossBowman.getMainHandItem())) {
                        if (canLoad()) newState = State.RELOAD;
                        else newState = State.IDLE;
                    } else if (isInOffHand && !weapon.isLoaded(crossBowman.getOffhandItem())) {
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
                    //if(!isInMainHand)
                    //    switchWeaponToMainHandFromOffHand();
                    this.crossBowman.startUsingItem(InteractionHand.MAIN_HAND);
                    int i = this.crossBowman.getTicksUsingItem();
                    if (i >= weapon.getWeaponLoadTime()) {
                        this.crossBowman.releaseUsingItem();
                        this.crossBowman.playSound(this.weapon.getLoadSound(), 1.0F, 1.0F / (crossBowman.getRandom().nextFloat() * 0.4F + 0.8F));
                        this.weapon.setLoaded(crossBowman.getMainHandItem(), true);
                        this.consumeAmmo();

                        if (target != null && target.isAlive()) state = State.AIMING;
                        else state = State.IDLE;
                    }
                }

                case AIMING -> {
                    //if(!isInMainHand)
                    //    switchWeaponToMainHandFromOffHand();
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
                    //if(!isInMainHand)
                    //    switchWeaponToMainHandFromOffHand();
                    if (target != null && target.isAlive()) {
                        this.crossBowman.getLookControl().setLookAt(target);
                        this.weapon.performRangedAttackIWeapon(this.crossBowman, target, weapon.getProjectileSpeed());
                        this.weapon.setLoaded(crossBowman.getMainHandItem(), false);
                    }
                    if (canLoad()) this.state = State.RELOAD;
                    else this.state = State.IDLE; //RESUPPLY
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
}
