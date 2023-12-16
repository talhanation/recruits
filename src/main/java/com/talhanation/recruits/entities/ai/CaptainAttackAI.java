package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.*;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;


public class CaptainAttackAI extends Goal {
    private final AbstractLeaderEntity captain;
    private AttackMode mode;
    private List<LivingEntity> targets;
    public CaptainAttackAI(AbstractLeaderEntity recruit) {
        this.captain = recruit;
    }

    public boolean canUse() {
        return captain.getTarget() != null;
    }

    public void start(){
        this.captain.currentRecruitsInCommand = captain.getRecruitsInCommand();
        this.mode = AttackMode.DEFENSIVE;
        if(this.captain.currentRecruitsInCommand.size() > 0){
            attackCommandsToRecruits(this.captain.getTarget());
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.captain.setRecruitsClearTargets();
        this.captain.setRecruitsToFollow();
        this.captain.setFollowState(0);
        this.captain.setTarget(null);
        this.captain.setShouldBlock(false);
        this.captain.setRecruitsShields(false);
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    //TODO:
    private void attackCommandsToRecruits(LivingEntity target) {
        if(!this.captain.getCommandSenderWorld().isClientSide()){
            targets = this.captain.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class, this.captain.getBoundingBox().inflate(70D)).stream()
                    .filter(living -> this.captain.canAttack(living) && living.isAlive())
                    .toList();

            int enemySize = targets.size();
            int partySize = this.getPartySize();
            double x = partySize / enemySize;

            if(x >= 1.3){
                this.mode = AttackMode.CHARGE;
            }
            else if(x >= 1){
                int rand = this.captain.getRandom().nextInt(3);
                this.mode = AttackMode.fromIndex(2 + rand); //DEFAULTS
            }
            else {
                this.mode = AttackMode.DEFENSIVE;
            }

            Vec3 toTarget = target.position().subtract(this.captain.position());

            this.captain.currentRecruitsInCommand = captain.getRecruitsInCommand();

            for(int i = 0; i < targets.size(); i++){
                if(this.captain.currentRecruitsInCommand.size() > i){
                    AbstractRecruitEntity recruit = this.captain.currentRecruitsInCommand.get(i);
                    recruit.setTarget(targets.get(i));
                }
            }

            captain.commandCooldown = 350;

            //LAND
            if(!this.captain.isPassenger()) {

                switch (mode) {
                    case DEFENSIVE -> {
                        //RANGED GO BEHIND LEADER AND HOLD POS
                        Vec3 moveVecRanged = toTarget.yRot(180).normalize();
                        Vec3 moveRanged = this.captain.position().add(moveVecRanged.scale(7.5D));
                        BlockPos movePosRanged = this.captain.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveRanged.x, moveRanged.y, moveRanged.z));

                        this.captain.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.BOWMAN.get());
                        this.captain.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.CROSSBOWMAN.get());
                        this.captain.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.NOMAD.get());
                        this.captain.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.HORSEMAN.get());

                        // INFANTRY HOLD POS AND SHIELDS UP
                        this.captain.setFollowState(2);//HOLD POS
                        this.captain.setTarget(null);
                        this.captain.setShouldBlock(true);
                        this.captain.setRecruitsShields(true);
                        this.captain.setTypedRecruitsToHoldPos(ModEntityTypes.RECRUIT.get());
                        this.captain.setTypedRecruitsToHoldPos(ModEntityTypes.RECRUIT_SHIELDMAN.get());
                    }

                    case CHARGE -> {
                        //All freely
                        this.captain.setFollowState(2);//HOLD POS
                        this.captain.setRecruitsShields(false);
                        this.captain.setRecruitsWanderFreely();
                    }
                }

            }
            //WATER
            else if(this.captain.getVehicle() != null && this.captain.getVehicle().getEncodeId().contains("smallships")){

            }
        }
    }

    public int getPartySize(){
        return this.captain.currentRecruitsInCommand.size();
    }

    public int getRangedSize(){
        int bowman = Collections.frequency(this.captain.currentRecruitsInCommand, BowmanEntity.class);
        int crossbow = Collections.frequency(this.captain.currentRecruitsInCommand, CrossBowmanEntity.class);
        return bowman + crossbow;
    }

    public int getInfantrySize(){
        int rec = Collections.frequency(this.captain.currentRecruitsInCommand, RecruitEntity.class);
        int shield = Collections.frequency(this.captain.currentRecruitsInCommand, RecruitShieldmanEntity.class);
        return rec + shield;
    }

    public int getCavalrySize(){
        int horseman = Collections.frequency(this.captain.currentRecruitsInCommand, HorsemanEntity.class);
        int nomad = Collections.frequency(this.captain.currentRecruitsInCommand, NomadEntity.class);
        return horseman + nomad;
    }


    private enum AttackMode{
        DEFENSIVE(0),
        DEFAULT(1),
        CHARGE(5);

        private final int index;
        AttackMode(int index){
            this.index = index;
        }

        public static AttackMode fromIndex(int index) {
            for (AttackMode state : AttackMode.values()) {
                if (state.index == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid AttackMode index: " + index);
        }
    }

    //Defenssive
    //  ->  inf hold pos
    //  ->  ranged hold pos, behind inf
    //  ->
}


