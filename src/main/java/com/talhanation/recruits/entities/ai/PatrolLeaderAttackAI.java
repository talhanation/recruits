package com.talhanation.recruits.entities.ai;

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
    private final AbstractLeaderEntity leader;
    private List<LivingEntity> targets;
    private List<AbstractRecruitEntity> infantry;
    private List<AbstractRecruitEntity> ranged;

    public PatrolLeaderAttackAI(AbstractLeaderEntity recruit) {
        this.leader = recruit;
    }

    public boolean canUse() {
        return leader.commandCooldown == 0 && leader.getTarget() != null && !leader.retreating;
    }

    public void start(){
        this.leader.currentRecruitsInCommand = leader.getRecruitsInCommand();
        if(this.leader.currentRecruitsInCommand.size() > 0){
            this.infantry = this.getOwnInfantry();
            this.ranged = this.getOwnRanged();
            attackCommandsToRecruits(this.leader.getTarget());
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void stop() {
        super.stop();
        this.leader.setRecruitsToFollow();
        this.leader.setFollowState(0);
        this.leader.setTarget(null);
        this.leader.setShouldBlock(false);

        for(int i = 0; i < this.leader.currentRecruitsInCommand.size(); i++) {
            AbstractRecruitEntity recruit = this.leader.currentRecruitsInCommand.get(i);
            if(!(recruit.getTarget() != null && recruit.getTarget().isAlive())){
                recruit.setShouldBlock(false);
            }
        }
    }

    public boolean canContinueToUse() {
        return leader.commandCooldown != 0 && !leader.retreating;
    }

    //TODO:
    private void attackCommandsToRecruits(LivingEntity target) {
        if(!this.leader.getCommandSenderWorld().isClientSide()){
            double distanceToTarget = this.leader.distanceToSqr(target);

            if(distanceToTarget < 4000){
                targets = this.leader.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(70D)).stream()
                        .filter(living -> this.canAttack(living) && living.isAlive())
                        .toList();

                this.leader.currentRecruitsInCommand = leader.getRecruitsInCommand();

                double enemyArmor = calculateEnemyArmor(targets);
                double armor = calculateArmor();
                int enemySize = targets.size();

                int partySize = this.getPartySize();
                double sizeFactor = Math.abs((partySize + 1) / (enemySize + 1) );
                double armorFactor = Math.abs((armor + 1) / (enemyArmor + 1));


                if((sizeFactor + armorFactor)/2 <= 0.3){
                    if(this.leader.getOwner() != null) this.leader.getOwner().sendSystemMessage(Component.literal("Retreat!"));
                    //this.leader.retreating = true;
                }
                //int enemyCavalry = (int) targets.stream().filter(this::isCavalry).count();
                //double cavalryFactor = Math.abs((cav.size() + 1) / (enemyCavalry+ 1));

                Comparison comparisonOwnInfantry = getInfantryComparison();
                Comparison comparisonOwnRanged = getRangedComparison();

                //RANGED DARF NICHT VOR WENN GEGNER INFANTRY GRÖßER IST -> HOLD POS
                //RANGED DARF NICHT VOR WENN GEGNER RANGED GRÖßER ist -> HOLD POS

                //INFANTRY DARF NICHT VOR WENN GEGNER INFANTRY GRÖßER ist -> HOLD POS
                //INFANTRY SHIELDS UP WENN GEGNER RANGED GRÖßER ist -> CHARGE

                //CAV IST IN JEDEM FALL CHARGE



                Vec3 movePosInfantry = getPosTowardsTarget(target, 0.6);
                Vec3 movePosRanged = getPosTowardsTarget(target, 0.4);
                BlockPos movePosLeader = getBlockPosTowardsTarget(target, 0.2);


                //CHARGE INFANTRY
                if(comparisonOwnRanged == Comparison.BIGGER && comparisonOwnInfantry == Comparison.BIGGER){
                    BlockPos moveBlockPosInfantry = getBlockPosTowardsTarget(target, 0.9);
                    this.leader.setTypedRecruitsToMove(moveBlockPosInfantry, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToMove(moveBlockPosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());


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
                this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());
                this.leader.setHoldPos(movePosLeader);
                this.leader.setFollowState(3);//LEADER HOLD POS

                for(AbstractRecruitEntity recruit : this.leader.currentRecruitsInCommand) recruit.setState(leader.getState());

                this.setRecruitsTargets();
                leader.commandCooldown = 350;
            }

        }
    }

    public BlockPos getBlockPosTowardsTarget(LivingEntity target, double x){
        Vec3 pos = leader.position().lerp(target.position(), x);
        return this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(pos.x, pos.y, pos.z));
    }
    public Vec3 getPosTowardsTarget(LivingEntity target, double x){
        return leader.position().lerp(target.position(), x);
    }
    private Comparison getInfantryComparison() {
        int enemyInfantry = (int) targets.stream().filter(this::isInfantry).count();
        double infantryFactor = Math.abs((infantry.size() + 1) / (enemyInfantry + 1));

        if(infantryFactor >= 1.15)
            return Comparison.BIGGER;
        else if (infantryFactor >= 0.75)
            return Comparison.SAME;
        else //if (infantryFactor >= 0.50)
            return Comparison.SMALLER;
    }

    private Comparison getRangedComparison() {
        int enemyRanged = (int) targets.stream().filter(this::isRanged).count();
        double rangedFactor = Math.abs((ranged.size()  + 1) / (enemyRanged + 1));

        if(rangedFactor >= 1.15)
            return Comparison.BIGGER;
        else if (rangedFactor >= 0.75)
            return Comparison.SAME;
        else //if (rangedFactor >= 0.50)
            return Comparison.SMALLER;
    }

    private void setRecruitsTargets() {
        for(int i = 0; i < this.leader.currentRecruitsInCommand.size(); i++){
            AbstractRecruitEntity recruit = this.leader.currentRecruitsInCommand.get(i);
            if(targets.size() > i) recruit.setTarget(targets.get(i));
        }
    }

    private boolean isInfantry(LivingEntity living){
        if(living.getVehicle() instanceof AbstractHorse)
            return false;
        else if(living.getMainHandItem().getItem() instanceof BowItem
                || living.getMainHandItem().getItem() instanceof CrossbowItem){
            return false;
        }
        else
            return true;
    }

    private boolean isRanged(LivingEntity living){
        return living.getMainHandItem().getItem() instanceof BowItem || living.getMainHandItem().getItem() instanceof CrossbowItem;
    }

    private boolean isCavalry(LivingEntity living){
        if(living.getMainHandItem().getItem() instanceof BowItem || living.getMainHandItem().getItem() instanceof CrossbowItem)
            return false;
        else
            return living.getVehicle() instanceof AbstractHorse;
    }

    private double calculateEnemyArmor(List<LivingEntity> targets) {
        double enemyArmor = 0;
        for(LivingEntity living : targets){
            enemyArmor += living.getArmorValue();
        }
        return enemyArmor;
    }

    private double calculateArmor() {
        double armor = 0;
        for(LivingEntity living : this.leader.currentRecruitsInCommand){
            armor += living.getArmorValue();
        }
        return armor;
    }

    private boolean canAttack(LivingEntity living) {
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


    private enum Comparison{
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
    switch (mode){
                    case DEFENSIVE -> {
                        if(this.leader.getOwner() != null) this.leader.getOwner().sendSystemMessage(Component.literal("Defensive tactics"));

                        double dist1 = distanceToTarget < 500 ? -0.5 : 0.2;
                        Vec3 moveRanged = this.leader.position().add(toTarget.scale(dist1));
                        BlockPos movePosRanged = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveRanged.x, moveRanged.y, moveRanged.z));

                        double dist2 = distanceToTarget < 500 ? -0.4 : 0.4;
                        Vec3 moveInfantry = this.leader.position().add(toTarget.scale(dist2));
                        BlockPos movePosInfantry = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveInfantry.x, moveInfantry.y, moveInfantry.z));

                        if(this.leader.getRangedTactics() == AbstractLeaderEntity.CombatTactics.AUTO.getIndex()){
                            //RANGED AND LEADER GO BEHIND AND HOLD POS
                            this.leader.setTypedRecruitsToMove(movePosRanged, ModEntityTypes.BOWMAN.get());
                            this.leader.setTypedRecruitsToMove(movePosRanged, ModEntityTypes.CROSSBOWMAN.get());
                        }

                        if(this.leader.getCavalryTactics() == AbstractLeaderEntity.CombatTactics.AUTO.getIndex()){
                            //CAVALRY CHARGE
                            this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.NOMAD.get());
                            this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());
                        }

                        if(this.leader.getInfantryTactics() == AbstractLeaderEntity.CombatTactics.AUTO.getIndex()){
                            this.leader.setTypedRecruitsToMove(movePosInfantry, ModEntityTypes.RECRUIT.get());
                            this.leader.setTypedRecruitsToMove(movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());
                        }

                        this.leader.setFollowState(2);//HOLD wPOS
                        this.leader.setShouldBlock(true);

                        this.leader.setRecruitsShields(true);

                    }
                    case DEFAULT -> {
                        if(this.leader.getOwner() != null)this.leader.getOwner().sendSystemMessage(Component.literal("Default!"));

                        Vec3 moveRanged = this.leader.position().add(toTarget.scale(0.4));
                        BlockPos movePosRanged = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveRanged.x, moveRanged.y, moveRanged.z));

                        Vec3 moveInfantry = this.leader.position().add(toTarget.scale(0.2));
                        BlockPos movePosInfantry = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveInfantry.x, moveInfantry.y, moveInfantry.z));

                        this.leader.setHoldPos(movePosRanged);//HOLD POS
                        this.leader.setFollowState(3);
                        this.leader.setShouldBlock(true);

                        if(this.leader.getRangedTactics() == AbstractLeaderEntity.CombatTactics.AUTO.getIndex()){
                            //RANGED AND LEADER GO BEHIND AND HOLD POS
                            this.leader.setTypedRecruitsToMove(movePosRanged, ModEntityTypes.BOWMAN.get());
                            this.leader.setTypedRecruitsToMove(movePosRanged, ModEntityTypes.CROSSBOWMAN.get());
                        }

                        if(this.leader.getInfantryTactics() == AbstractLeaderEntity.CombatTactics.AUTO.getIndex()){
                            this.leader.setTypedRecruitsToMove(movePosInfantry, ModEntityTypes.RECRUIT.get());
                            this.leader.setTypedRecruitsToMove(movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());
                        }

                        if(this.leader.getCavalryTactics() == AbstractLeaderEntity.CombatTactics.AUTO.getIndex()){
                            this.leader.setTypedRecruitsToMove(movePosInfantry, ModEntityTypes.NOMAD.get());
                            this.leader.setTypedRecruitsToMove(movePosInfantry, ModEntityTypes.HORSEMAN.get());
                        }
                    }

                    case CHARGE -> {
                        //All freely
                        if(this.leader.getOwner() != null)this.leader.getOwner().sendSystemMessage(Component.literal("Charge!"));

                        Vec3 moveRanged = this.leader.position().add(toTarget.scale(0.5));
                        BlockPos movePosRanged = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveRanged.x, moveRanged.y, moveRanged.z));

                        Vec3 moveInfantry = this.leader.position().add(toTarget.scale(0.75));
                        BlockPos movePosInfantry = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveInfantry.x, moveInfantry.y, moveInfantry.z));

                        this.leader.setHoldPos(movePosInfantry);//HOLD POS
                        this.leader.setFollowState(3);

                        this.leader.setRecruitsShields(false);

                        if(this.leader.getRangedTactics() == AbstractLeaderEntity.CombatTactics.AUTO.getIndex()){
                            //RANGED AND LEADER GO BEHIND AND HOLD POS
                            this.leader.setTypedRecruitsToMove(movePosRanged, ModEntityTypes.BOWMAN.get());
                            this.leader.setTypedRecruitsToMove(movePosRanged, ModEntityTypes.CROSSBOWMAN.get());
                        }

                        if(this.leader.getInfantryTactics() == AbstractLeaderEntity.CombatTactics.AUTO.getIndex()){
                            this.leader.setTypedRecruitsToMove(movePosInfantry, ModEntityTypes.RECRUIT.get());
                            this.leader.setTypedRecruitsToMove(movePosInfantry, ModEntityTypes.RECRUIT_SHIELDMAN.get());
                        }

                        if(this.leader.getCavalryTactics() == AbstractLeaderEntity.CombatTactics.AUTO.getIndex()){
                            this.leader.setTypedRecruitsToMove(movePosInfantry, ModEntityTypes.NOMAD.get());
                            this.leader.setTypedRecruitsToMove(movePosInfantry, ModEntityTypes.HORSEMAN.get());
                        }
                    }
                }
     */
}


