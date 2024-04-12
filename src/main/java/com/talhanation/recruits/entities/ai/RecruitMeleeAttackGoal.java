package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;

import com.talhanation.recruits.util.AttackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.common.ForgeMod;

import java.text.AttributedString;
import java.util.EnumSet;



public class RecruitMeleeAttackGoal extends Goal {
    protected final AbstractRecruitEntity recruit;
    private final double speedModifier;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private long lastCanUseCheck;
    private int failedPathFindingPenalty = 0;
    private final double range;
    private final boolean canPenalize = true;

    public RecruitMeleeAttackGoal(AbstractRecruitEntity recruit, double speedModifier, double range) {
        this.recruit = recruit;
        this.speedModifier = speedModifier;

        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        //check if last use awas 20 tick before
        long i = this.recruit.level.getGameTime();
        if (i - this.lastCanUseCheck >= 20L) {
            this.lastCanUseCheck = i;

            LivingEntity target = this.recruit.getTarget();
            if (target != null && target.isAlive()) {
                boolean isClose = target.distanceTo(this.recruit) <= range;
                boolean canSee = this.recruit.getSensing().hasLineOfSight(target);
                if (isClose && canSee) {
                    if (canPenalize) {
                        if (--this.ticksUntilNextPathRecalculation <= 0) {
                            this.path = this.recruit.getNavigation().createPath(target, 0);
                            this.ticksUntilNextPathRecalculation = 4 + this.recruit.getRandom().nextInt(7);
                            return this.path != null;
                        } else {
                            return true;
                        }
                    }
                    this.path = this.recruit.getNavigation().createPath(target, 0);
                    if (this.path != null) {
                        return true;
                    } else {
                        double distance = this.recruit.distanceToSqr(target.getX(), target.getY(), target.getZ());
                        double reach = AttackUtil.getAttackReachSqr(recruit);
                        return (reach >=  distance) && canAttackHoldPos() && recruit.getState() != 3 && !recruit.needsToGetFood() && !recruit.getShouldMount() && !recruit.getShouldMovePos();
                    }
                }
            }
        }
        return false;
    }

    public boolean canContinueToUse() {
        LivingEntity target = this.recruit.getTarget();

        if (target == null) {
            return false;
        }
        else if (!target.isAlive() && !this.recruit.getSensing().hasLineOfSight(target)) {
            return false;
        }
        else if (!this.recruit.isWithinRestriction(target.blockPosition())) {
            return false;
        }
        else {
            return (!(target instanceof Player) || !target.isSpectator() && !((Player)target).isCreative()) && canAttackHoldPos() && recruit.getState() != 3 && !recruit.needsToGetFood() && !recruit.getShouldMount() && !recruit.getShouldMovePos();
        }
    }

    public void start() {
        LivingEntity target = this.recruit.getTarget();
        if ((!recruit.getShouldHoldPos()) || target.position().closerThan(recruit.position(), 12D)) {
            this.recruit.getNavigation().moveTo(this.path, this.speedModifier);
            this.recruit.setAggressive(true);
            this.ticksUntilNextPathRecalculation = 0;
        }
    }

    public void stop() {
        LivingEntity target = this.recruit.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.recruit.setTarget(null);
        }

        this.recruit.setAggressive(false);
        this.recruit.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity target = this.recruit.getTarget();
        if(target != null && target.isAlive()){
            this.recruit.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distanceToTarget = this.recruit.distanceToSqr(target.getX(), target.getY(), target.getZ());
            Main.LOGGER.info("distance"  + distanceToTarget);
            double reach = AttackUtil.getAttackReachSqr(recruit);
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if(distanceToTarget <= reach){
                this.recruit.getNavigation().stop();
                AttackUtil.performAttack(this.recruit, target);
            }
            else if ((this.recruit.getSensing().hasLineOfSight(target))
                    && this.ticksUntilNextPathRecalculation <= 0
                    && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D || this.recruit.getRandom().nextFloat() < 0.05F)) {

                this.pathedTargetX = target.getX();
                this.pathedTargetY = target.getY();
                this.pathedTargetZ = target.getZ();
                this.ticksUntilNextPathRecalculation = 4 + this.recruit.getRandom().nextInt(7);
                if (this.canPenalize) {
                    this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;
                    if (this.recruit.getNavigation().getPath() != null) {
                        Node finalPathPoint = this.recruit.getNavigation().getPath().getEndNode();
                        if (finalPathPoint != null && target.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1)
                            failedPathFindingPenalty = 0;
                        else
                            failedPathFindingPenalty += 10;
                    } else {
                        failedPathFindingPenalty += 10;
                    }
                }
                if (distanceToTarget > 1024.0D) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (distanceToTarget > 256.0D) {
                    this.ticksUntilNextPathRecalculation += 5;
                }

                if (!this.recruit.getNavigation().moveTo(target, this.speedModifier)) {
                    this.ticksUntilNextPathRecalculation += 15;
                }
            }
        }
    }

    /*
    Cooldown Infos MC-Wiki
    Swords: 0.6s
    Stone and Wood axe: 1.25s
    Gold/Dia/Neatherite: 1s
    Iron: 1.1s

    1s = 20ticks

    cooldown should be + 5 ticks for gameplay
     */

    private boolean canAttackHoldPos() {
        LivingEntity target = this.recruit.getTarget();
        BlockPos pos = recruit.getHoldPos();

        if (target != null && pos != null && recruit.getShouldHoldPos()) {
            double distanceToPos = target.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());

            return distanceToPos < 750;
        }
        return true;
    }
}
