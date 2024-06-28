package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;


public class PatrolLeaderAttackAI extends Goal {
    public final AbstractLeaderEntity leader;
    public List<LivingEntity> targets;
    public List<AbstractRecruitEntity> infantry;
    public List<AbstractRecruitEntity> ranged;

    public PatrolLeaderAttackAI(AbstractLeaderEntity recruit) {
        this.leader = recruit;
    }

    public boolean canUse() {
        if(!leader.getShouldFollow() && !leader.getShouldMovePos() && leader.commandCooldown == 0 && !leader.retreating && (leader.getTarget() != null || (int) leader.getPatrollingState() == AbstractLeaderEntity.State.ATTACKING.getIndex())){
            this.leader.currentRecruitsInCommand = leader.getRecruitsInCommand();
            return this.leader.currentRecruitsInCommand.size() > 0;
        }
        return false;
    }

    public void start(){
        this.infantry = this.getOwnInfantry();
        this.ranged = this.getOwnRanged();
        attackCommandsToRecruits(this.leader.getTarget());
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void stop() {
        super.stop();
        this.leader.setRecruitsToFollow();
        if(this.leader.getFollowState() != 1) this.leader.setFollowState(0);
        this.leader.setTarget(null);
        this.leader.setShouldBlock(false);

        for(int i = 0; i < this.leader.currentRecruitsInCommand.size(); i++) {
            AbstractRecruitEntity recruit = this.leader.currentRecruitsInCommand.get(i);
            if(!(recruit.getTarget() != null && recruit.getTarget().isAlive())){
                recruit.setShouldBlock(false);
            }
        }

        this.leader.setPatrolState(leader.prevState);
    }

    public boolean canContinueToUse() {
        return leader.commandCooldown != 0 && !leader.retreating;
    }

    //TODO:
    public void attackCommandsToRecruits(LivingEntity target) {
        if(!this.leader.getCommandSenderWorld().isClientSide() && target != null){
            double distanceToTarget = this.leader.distanceToSqr(target);
            if(distanceToTarget < 5000){
                targets = this.leader.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(70D)).stream()
                        .filter(living -> this.canAttack(living) && living.isAlive() && this.leader.getSensing().hasLineOfSight(living))
                        .toList();

                this.leader.currentRecruitsInCommand = leader.getRecruitsInCommand();
                for(AbstractRecruitEntity recruit : this.leader.currentRecruitsInCommand) recruit.setState(leader.getState());

                double enemyArmor = calculateEnemyArmor(targets);
                double armor = calculateArmor();
                int enemySize = targets.size();

                int partySize = this.getPartySize();
                double sizeFactor = Math.abs((partySize + 1) / (enemySize + 1) );
                double armorFactor = Math.abs((armor + 1) / (enemyArmor + 1));
                double factor = (sizeFactor + armorFactor)/2;
                /*
                int enemyRanged = ;
                int enemyCav = ;
                int enemyInfantry = ;
                int ownRanged = ;
                int ownCav = ;
                int ownInfantry = ;
                Main.LOGGER.info("--------------------------");
                Main.LOGGER.info("PartySize: " + partySize);
                Main.LOGGER.info("PartySize: " + partySize);
                Main.LOGGER.info("EnemySize: " + enemySize);
                Main.LOGGER.info("--------------------------");
                Main.LOGGER.info("armorFactor: " + armorFactor);
                Main.LOGGER.info("SizeFactor : " + sizeFactor);
                Main.LOGGER.info("gen. factor: " + factor);
                Main.LOGGER.info("--------------------------");

                 */

                this.setRecruitsTargets();

                if(distanceToTarget < 3000) {
                    if(factor > 1.5){
                        charge(target);
                        if(leader.getOwner() != null && leader.getInfoMode() != 1) this.leader.getOwner().sendSystemMessage(Component.literal(leader.getName().getString() + ": Im charging the enemy, their size is " + enemySize));
                    }
                    else if(factor > 0.6){
                        defaultAttack(target);
                        if(leader.getOwner() != null && leader.getInfoMode() != 1) this.leader.getOwner().sendSystemMessage(Component.literal(leader.getName().getString() + ": Im engaging the enemy, their size is " + enemySize));
                    }
                    else {
                        back(target);
                        if(leader.getOwner() != null && leader.getInfoMode() != 1) this.leader.getOwner().sendSystemMessage(Component.literal(leader.getName().getString() + ": Im moving backwards, i could need assistance!. Their size is " + enemySize));
                    }
                    leader.commandCooldown = 400;
                }
                else{
                    if(leader.getOwner() != null) this.leader.getOwner().sendSystemMessage(Component.literal(leader.getName().getString() + ": Enemy contact! Im advancing, their size is " + enemySize));
                    advance(target);
                    leader.commandCooldown = 150;
                }
            }
        }
    }


    public void charge(LivingEntity target){
        BlockPos movePosLeader = getBlockPosTowardsTarget(target, 0.2);
        this.leader.setHoldPos(Vec3.atCenterOf(movePosLeader));
        this.leader.setFollowState(3);//LEADER BACK TO POS

        this.leader.setRecruitsWanderFreely();
        this.setRecruitsTargets();
    }
    public void defaultAttack(LivingEntity target){

        Vec3 moveVecRanged = getPosTowardsTarget(target, 0.4);
        BlockPos movePosLeader = getBlockPosTowardsTarget(target, 0.2);
        BlockPos moveBlockPosInfantry = getBlockPosTowardsTarget(target, 0.6);

        this.leader.setTypedRecruitsToMove(moveBlockPosInfantry, ModEntityTypes.RECRUIT.get());
        this.leader.setTypedRecruitsToMove(moveBlockPosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());

        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecRanged, ModEntityTypes.BOWMAN.get());
        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecRanged, ModEntityTypes.CROSSBOWMAN.get());

        this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.NOMAD.get());
        this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());

        this.leader.setHoldPos(Vec3.atCenterOf(movePosLeader));
        this.leader.setFollowState(3);//LEADER BACK TO POS
    }
    public void advance(LivingEntity target){
        Vec3 moveVecInfantry = getPosTowardsTarget(target, 0.6);
        Vec3 moveVecRanged = getPosTowardsTarget(target, 0.4);
        BlockPos movePosLeader = getBlockPosTowardsTarget(target, 0.2);

        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecInfantry, ModEntityTypes.RECRUIT.get());
        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());

        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecRanged, ModEntityTypes.BOWMAN.get());
        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecRanged, ModEntityTypes.CROSSBOWMAN.get());

        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecRanged, ModEntityTypes.NOMAD.get());
        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecRanged, ModEntityTypes.HORSEMAN.get());

        this.leader.setHoldPos(Vec3.atCenterOf(movePosLeader));
        this.leader.setFollowState(3);//LEADER BACK TO POS
    }

    public void back(LivingEntity target){
        Vec3 moveVecInfantry = getPosTowardsTarget(target, -0.4);
        Vec3 moveVecRanged = getPosTowardsTarget(target, -0.6);
        BlockPos movePosLeader = getBlockPosTowardsTarget(target, -0.7);

        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecInfantry, ModEntityTypes.RECRUIT.get());
        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());

        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecRanged, ModEntityTypes.BOWMAN.get());
        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecRanged, ModEntityTypes.CROSSBOWMAN.get());

        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecRanged, ModEntityTypes.NOMAD.get());
        this.leader.setTypedRecruitsSetAndHoldPos(target.position(), moveVecRanged, ModEntityTypes.HORSEMAN.get());

        this.leader.setHoldPos(Vec3.atCenterOf(movePosLeader));
        this.leader.setFollowState(3);//LEADER BACK TO POS
    }

    public BlockPos getBlockPosTowardsTarget(LivingEntity target, double x){
        Vec3 pos = leader.position().lerp(target.position(), x);
        return this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(pos.x, pos.y, pos.z));
    }
    public Vec3 getPosTowardsTarget(LivingEntity target, double x){
        return leader.position().lerp(target.position(), x);
    }
    public Comparison getInfantryComparison() {
        int enemyInfantry = (int) targets.stream().filter(this::isInfantry).count();
        double infantryFactor = Math.abs((infantry.size() + 1) / (enemyInfantry + 1));

        if(infantryFactor >= 1.15)
            return Comparison.BIGGER;
        else if (infantryFactor >= 0.75)
            return Comparison.SAME;
        else //if (infantryFactor >= 0.50)
            return Comparison.SMALLER;
    }

    public Comparison getRangedComparison() {
        int enemyRanged = (int) targets.stream().filter(PatrolLeaderAttackAI::isRanged).count();
        double rangedFactor = Math.abs((ranged.size()  + 1) / (enemyRanged + 1));

        if(rangedFactor >= 1.15)
            return Comparison.BIGGER;
        else if (rangedFactor >= 0.75)
            return Comparison.SAME;
        else //if (rangedFactor >= 0.50)
            return Comparison.SMALLER;
    }

    public void setRecruitsTargets() {
        for(int i = 0; i < this.leader.currentRecruitsInCommand.size(); i++){
            AbstractRecruitEntity recruit = this.leader.currentRecruitsInCommand.get(i);
            if(targets.size() > i) recruit.setTarget(targets.get(i));
        }
    }

    public boolean isInfantry(LivingEntity living){
        if(living.getVehicle() instanceof AbstractHorse)
            return false;
        else if(living.getMainHandItem().getItem() instanceof BowItem
                || living.getMainHandItem().getItem() instanceof CrossbowItem){
            return false;
        }
        else
            return true;
    }

    public static boolean isRanged(LivingEntity living){
        return living.getMainHandItem().getItem() instanceof BowItem || living.getMainHandItem().getItem() instanceof CrossbowItem;
    }

    public boolean isCavalry(LivingEntity living){
        if(living.getMainHandItem().getItem() instanceof BowItem || living.getMainHandItem().getItem() instanceof CrossbowItem)
            return false;
        else
            return living.getVehicle() instanceof AbstractHorse;
    }

    public double calculateEnemyArmor(List<LivingEntity> targets) {
        double enemyArmor = 0;
        for(LivingEntity living : targets){
            enemyArmor += living.getArmorValue();
        }
        return enemyArmor;
    }

    public double calculateArmor() {
        double armor = 0;
        for(LivingEntity living : this.leader.currentRecruitsInCommand){
            armor += living.getArmorValue();
        }
        return armor;
    }

    public boolean canAttack(LivingEntity living) {
        int aggroState = this.leader.getState();
        switch(aggroState){
            case 0 -> { //Neutral
                if(living instanceof Monster){
                    return this.leader.canAttack(living);
                }
            }
            case 1 -> { //AGGRO
                if(living instanceof Player || living instanceof AbstractRecruitEntity || living instanceof Monster){
                    return this.leader.canAttack(living);
                }
            }
            case 2 -> { //RAID
                return this.leader.canAttack(living);
            }

            default -> {
                return false;
            }
        }
        return false;
    }

    public int getPartySize(){
        return this.leader.currentRecruitsInCommand.size();
    }

    public List<AbstractRecruitEntity> getOwnRanged(){
        List<AbstractRecruitEntity> ranged = new ArrayList<>();
        for(AbstractRecruitEntity recruit : this.leader.currentRecruitsInCommand){
            if(recruit instanceof BowmanEntity || recruit instanceof CrossBowmanEntity) ranged.add(recruit);
        }
        return ranged;
    }

    public List<AbstractRecruitEntity> getOwnInfantry(){
        List<AbstractRecruitEntity> infantry = new ArrayList<>();
        for(AbstractRecruitEntity recruit : this.leader.currentRecruitsInCommand){
            if(recruit instanceof RecruitEntity || recruit instanceof RecruitShieldmanEntity) infantry.add(recruit);
        }
        return infantry;
    }

    public List<AbstractRecruitEntity> getOwnCavalry(){
        List<AbstractRecruitEntity> cav = new ArrayList<>();
        for(AbstractRecruitEntity recruit : this.leader.currentRecruitsInCommand){
            if(recruit instanceof HorsemanEntity || recruit instanceof NomadEntity) cav.add(recruit);
        }
        return cav;
    }


    public enum Comparison{
        BIGGER(0),
        SAME(1),
        SMALLER(3);

        private final int index;
        Comparison(int index){
            this.index = index;
        }

        public static Comparison fromIndex(int index) {
            for (Comparison state : Comparison.values()) {
                if (state.index == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid AttackMode index: " + index);
        }
    }

    /*
    //CHARGE INFANTRY
                if(comparisonOwnRanged == Comparison.BIGGER && comparisonOwnInfantry == Comparison.BIGGER){
                    if(distanceToTarget > 1500){
                        BlockPos moveBlockPosInfantry = getBlockPosTowardsTarget(target, 0.9);
                        this.leader.setTypedRecruitsToMove(moveBlockPosInfantry, ModEntityTypes.RECRUIT.get());
                        this.leader.setTypedRecruitsToMove(moveBlockPosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());
                    }
                    else{
                        this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.RECRUIT.get());
                        this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.RECRUIT_SHIELDMAN.get());
                    }

                    movePosRanged = getPosTowardsTarget(target, 0.4);
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(),   movePosRanged, ModEntityTypes.CROSSBOWMAN.get());

                }
                else if(comparisonOwnRanged == Comparison.SAME && comparisonOwnInfantry == Comparison.SAME){
                    movePosInfantry = getPosTowardsTarget(target, 0.75);
                    movePosRanged = getPosTowardsTarget(target, 0.5);

                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());

                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.CROSSBOWMAN.get());
                }

                else if(comparisonOwnRanged == Comparison.SMALLER && comparisonOwnInfantry == Comparison.BIGGER){
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());
                    this.leader.setRecruitsShields(true);

                }
                else if(comparisonOwnRanged == Comparison.SMALLER && comparisonOwnInfantry == Comparison.SMALLER){
                    movePosRanged = getPosTowardsTarget(target, -0.2);
                    movePosLeader = getBlockPosTowardsTarget(target, -0.3);
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.CROSSBOWMAN.get());

                    movePosInfantry = getPosTowardsTarget(target, 0.3);
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());
                }
                else {
                    movePosInfantry = getPosTowardsTarget(target, 0.6);
                    movePosRanged = getPosTowardsTarget(target, 0.4);

                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());

                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsToMoveAndHold(target.position(), movePosRanged, ModEntityTypes.CROSSBOWMAN.get());
                }
     */
}


