package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;

public class RecruitHailOfArrows extends Goal {

    private BlockPos pos;
    private BowmanEntity bowman;

    private int attackIntervalMin;
    private int attackTime = -1;
    private final int attackIntervalMax;
    public RecruitHailOfArrows(BowmanEntity bowman, int attackIntervalMin, int attackIntervalMax) {
        this.bowman = bowman;
        this.attackIntervalMin = attackIntervalMin;
        this.attackIntervalMax = attackIntervalMax;
    }

    @Override
    public boolean canUse() {
        if(bowman.getTarget() == null && bowman.getShouldArrow() && this.isHoldingBow() && bowman.getFollowState() != 1 && bowman.getFollowState() != 5 && !bowman.getShouldUpkeep() && !bowman.getShouldMount()){
            return true;
        }
        else{
            this.pos = null;
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }


    public void stop() {
        super.stop();
        this.attackTime = -1;
        this.bowman.stopUsingItem();
        this.bowman.clearArrowsPos();
    }

    @Override
    public void tick() {
        this.pos = this.bowman.getArrowPos();
        if (pos != null) {
            double d0 = this.bowman.distanceToSqr(pos.getX(), bowman.getY(), pos.getZ());

            this.bowman.getLookControl().setLookAt(pos.getX(), pos.getY() * 16, pos.getZ(), 30.0F, 30.0F);
            //Main.LOGGER.debug("d0: " + d0);
            if (this.bowman.isUsingItem()) {

                int i = this.bowman.getTicksUsingItem();
                if (i >= 20) {


                    float angle = (float) (0.9F + (1/d0) * 1.25);// increase = ++ / decrease = --
                    if (angle < 1000){
                        angle = (float) (0.9F + (1/d0) * 10 );
                    }

                    float force = (float) (d0 * 0.00008F);
                    this.bowman.performRangedAttackXYZ(pos.getX(), pos.getY(), pos.getZ(), BowItem.getPowerForTime(i), angle, force);
                    float f = Mth.sqrt((float) d0) / 44F;
                    this.attackTime = Mth.floor(f * (float) (this.attackIntervalMax - this.attackIntervalMin) + (float) this.attackIntervalMin);
                    this.bowman.stopUsingItem();
                }
            } else if (--this.attackTime <= 0) {
                this.bowman.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.bowman, Items.BOW));
            }
        }
    }

    protected boolean isHoldingBow() {
        return this.bowman.isHolding(is -> is.getItem() instanceof BowItem);
    }
}
