package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.NomadEntity;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import static com.talhanation.recruits.entities.NomadEntity.State.*;

public class NomadAttackAI extends Goal {
    private final NomadEntity nomad;
    private LivingEntity target;
    private NomadEntity.State state;
    private int seeTime;
    private int attackTime = -1;
    private boolean consumeArrows;

    public NomadAttackAI(NomadEntity recruit) {
        this.nomad = recruit;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.consumeArrows = RecruitsServerConfig.RangedRecruitsNeedArrowsToShoot.get();
    }
    public boolean canUse() {
        return nomad.getVehicle() instanceof AbstractHorse && nomad.getTarget() != null && !nomad.needsToGetFood() && !nomad.getShouldMount() && isHoldingBow() && nomad.getShouldRanged();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void start(){
        super.start();
        this.state = SELECT_TARGET;
        this.nomad.setAggressive(true);
    }
    public void stop() {
        super.stop();
        this.nomad.setAggressive(false);
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
        this.nomad.stopUsingItem();
    }
    protected boolean isHoldingBow() {
        String name = nomad.getMainHandItem().getDescriptionId();
        if(this.nomad.isHolding(bow -> bow.is(Items.BOW))){
            return true;
        }
        else if (this.nomad.isHolding(bow -> bow.getItem() instanceof BowItem))
            return true;

        else if (this.nomad.isHolding(bow -> bow.getItem() instanceof ProjectileWeaponItem))
            return true;

        else
            return name.contains("bow");
    }

    private boolean hasArrows(){
        return !consumeArrows || this.nomad.getInventory().hasAnyMatching(item -> item.is(ItemTags.ARROWS));
    }
    public void tick() {
        if(nomad.getFollowState() == 0) {
            switch (state) {
                case SELECT_TARGET -> {
                    this.target = nomad.getTarget();
                    if (target != null && target.isAlive() && hasArrows()) {

                        this.state = CIRCLE_TARGET;
                    }
                }

                case CIRCLE_TARGET -> {
                    if (target != null && target.isAlive()) {
                        Vec3 toTarget = target.position().subtract(nomad.position()).normalize();
                        Vec3 moveVec = toTarget.yRot(-90).normalize();
                        Vec3 movePos = target.position().add(moveVec.scale(20D));

                        if (nomad.distanceToSqr(movePos) > 3F)
                            nomad.getNavigation().moveTo(movePos.x, movePos.y, movePos.z, 1);

                        nomad.getLookControl().setLookAt(target, 30.0F, 30.0F);
                    } else {
                        state = SELECT_TARGET;
                    }
                }
            }
        }
        else {
            this.target = nomad.getTarget();
        }

        if (target != null && target.isAlive() && this.nomad.canAttack(target) && this.nomad.getState() != 3) {
            boolean canSee = this.nomad.getSensing().hasLineOfSight(target);
            if (canSee) {
                ++this.seeTime;
            } else {
                this.seeTime = 0;
            }

            double d0 = this.nomad.distanceToSqr(target.getX(), target.getY(), target.getZ());
            if (this.nomad.isUsingItem()) {
                if (!canSee && this.seeTime < -60) {
                    this.nomad.stopUsingItem();
                } else if (canSee) {
                    int i = this.nomad.getTicksUsingItem();
                    if (i >= 20) {
                        this.nomad.stopUsingItem();
                        this.nomad.performRangedAttack(target, BowItem.getPowerForTime(i));
                        float f = Mth.sqrt((float) d0) / 32;
                        int attackIntervalMax = 20;
                        int attackIntervalMin = 10;
                        this.attackTime = Mth.floor(f * (float) (attackIntervalMax - attackIntervalMin) + (float) attackIntervalMin);
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60 && hasArrows()) {
                this.nomad.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }
    }
}

