package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.HorsemanEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

import static com.talhanation.recruits.entities.HorsemanEntity.State.*;

public class HorsemanAttackAI extends Goal {
    private final HorsemanEntity horseman;
    private LivingEntity target;
    private HorsemanEntity.State state;
    private Vec3 movePos;
    private int ticksUntilNextAttack;

    public HorsemanAttackAI(HorsemanEntity recruit) {
        this.horseman = recruit;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public boolean canUse() {
        return horseman.getVehicle() instanceof AbstractHorse && horseman.getFollowState() == 0 && !horseman.needsToGetFood() && !horseman.getShouldMount();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void start() {
        super.start();
        this.target = horseman.getTarget();
        this.state = SELECT_TARGET;
        this.ticksUntilNextAttack = 10 + getCooldownModifier();
    }

    public void tick() {
        if(ticksUntilNextAttack > 0) ticksUntilNextAttack--;
        switch (state) {
            case SELECT_TARGET -> {
                this.target = horseman.getTarget();
                if (target != null) {
                    Vec3 moveVec = target.position().subtract(horseman.position()).normalize();

                    this.movePos = target.position().add(moveVec.scale(10D));
                    this.state = CHARGE_TARGET;
                }
            }

            case CHARGE_TARGET -> {

                if (target == null || !target.isAlive()) {
                    state = SELECT_TARGET;
                    return;
                }

                if (horseman.distanceToSqr(target) > 5F) {
                    horseman.getNavigation().moveTo(target.position().x, target.position().y, target.position().z, 1.15F);
                }
                else
                    state = MOVE_TO_POS;

                horseman.getLookControl().setLookAt(target, 30.0F, 30.0F);

                //Perform Attack
                if (horseman.distanceToSqr(target) < 5F) {
                    if(this.ticksUntilNextAttack <= 0) {
                        this.checkAndPerformAttack(target);
                    }
                }

                this.knockback();
            }

            case MOVE_TO_POS -> {
                if (target == null || !target.isAlive()) {
                    state = SELECT_TARGET;
                    return;
                }

                Vec3 movePos2 = new Vec3(movePos.x, horseman.position().y, movePos.z);
                horseman.getLookControl().setLookAt(movePos2);
                horseman.getNavigation().moveTo(movePos2.x, movePos2.y, movePos2.z, 1.15F);

                if (horseman.distanceToSqr(movePos2) < 6F) {
                    this.state = SELECT_TARGET;
                }

                this.knockback();
            }
        }
    }

    private void knockback() {
        List<LivingEntity> list = horseman.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class, horseman.getBoundingBox().inflate(8D));
        for(LivingEntity entity : list){

            if (horseman.distanceToSqr(entity) < 3F) {
                if(horseman.canAttack(entity) && !entity.equals(horseman) && entity.getVehicle() == null){
                   entity.knockback(0.85, (double) Mth.sin(this.horseman.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.horseman.getYRot() * ((float)Math.PI / 180F))));
                   entity.hurt(this.horseman.damageSources().mobAttack(horseman), 1F);;
                }
            }
        }
    }

    protected void checkAndPerformAttack(LivingEntity target) {
        if(!horseman.swinging) {
            this.horseman.swing(InteractionHand.MAIN_HAND);
            this.horseman.doHurtTarget(target);
            this.resetAttackCooldown();
        }
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = 15 + getCooldownModifier();
    }

    private int getCooldownModifier(){
        int modifier = 0;
        Item item = horseman.getMainHandItem().getItem();

        if(item instanceof TieredItem tieredItem){
            modifier = 5 - (int) tieredItem.getTier().getSpeed();
        }

        if (item instanceof AxeItem){
            modifier += 3;
        }

        return modifier;
    }
}
