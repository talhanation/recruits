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


public class PatrolLeaderAttackAI extends Goal {
    private final AbstractLeaderEntity leader;
    private AttackMode mode;
    private List<LivingEntity> targets;
    public PatrolLeaderAttackAI(AbstractLeaderEntity recruit) {
        this.leader = recruit;
    }

    public boolean canUse() {
        return leader.commandCooldown == 0 && leader.getTarget() != null;
    }

    public void start(){
        this.leader.currentRecruitsInCommand = leader.getRecruitsInCommand();
        this.mode = AttackMode.DEFENSIVE;
        if(this.leader.currentRecruitsInCommand.size() > 0){
            attackCommandsToRecruits(this.leader.getTarget());
        }


    }

    @Override
    public void stop() {
        super.stop();
        this.leader.setRecruitsClearTargets();
        this.leader.setRecruitsToFollow();
    }

    public boolean canContinueToUse() {
        return leader.commandCooldown != 0;
    }

    //TODO:
    private void attackCommandsToRecruits(LivingEntity target) {
        if(!this.leader.getCommandSenderWorld().isClientSide()){
            targets = this.leader.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class, this.leader.getBoundingBox().inflate(70D)).stream()
                    .filter(living -> this.leader.canAttack(living) && living.isAlive())
                    .toList();

            int enemySize = targets.size();
            int partySize = this.getPartySize();
            double x = partySize / enemySize;

            if(x >= 1.4){
                this.mode = AttackMode.CHARGE;
            }
            else if(x >= 1){
                int rand = this.leader.getRandom().nextInt(3);
                this.mode = AttackMode.fromIndex(2 + rand); //DEFAULTS
            }
            else if(x > 0.65){
                this.mode = AttackMode.LIGHT_DEFENSIVE;
            }
            else {
                this.mode = AttackMode.DEFENSIVE;
            }



            Vec3 toTarget = target.position().subtract(this.leader.position());

            this.leader.currentRecruitsInCommand = leader.getRecruitsInCommand();

            for(int i = 0; i < targets.size(); i++){
                if(this.leader.currentRecruitsInCommand.size() > i){
                    AbstractRecruitEntity recruit = this.leader.currentRecruitsInCommand.get(i);
                    recruit.setTarget(targets.get(i));
                }
            }

            leader.commandCooldown = 350;

            switch (mode){
                case DEFENSIVE -> {
                    //RANGED GO BEHIND LEADER AND HOLD POS
                    Vec3 moveVecRanged = toTarget.yRot(180).normalize();
                    Vec3 moveRanged = this.leader.position().add(moveVecRanged.scale(7.5D));
                    BlockPos movePosRanged = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveRanged.x, moveRanged.y, moveRanged.z));

                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.CROSSBOWMAN.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.HORSEMAN.get());

                    // INFANTRY HOLD POS AND SHIELDS UP
                    this.leader.setFollowState(2);//HOLD POS
                    this.leader.setTarget(null);
                    this.leader.setShouldBlock(true);
                    this.leader.setRecruitsShields(true);
                    this.leader.setTypedRecruitsToHoldPos(ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToHoldPos(ModEntityTypes.RECRUIT_SHIELDMAN.get());
                }

                case LIGHT_DEFENSIVE -> {
                    //RANGED GO BEHIND LEADER AND HOLD POS
                    Vec3 moveVecRanged = toTarget.yRot(180).normalize();
                    Vec3 moveRanged = this.leader.position().add(moveVecRanged.scale(7.5D));
                    BlockPos movePosRanged = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveRanged.x, moveRanged.y, moveRanged.z));

                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.CROSSBOWMAN.get());

                    //CAVALRY CHARGE
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());

                    // INFANTRY HOLD POS AND SHIELDS UP
                    this.leader.setFollowState(2);//HOLD POS
                    this.leader.setTarget(null);
                    this.leader.setShouldBlock(true);
                    this.leader.setRecruitsShields(true);
                    Vec3 moveVecInf = toTarget.normalize();
                    Vec3 moveInf = this.leader.position().add(moveVecInf.scale(-7.5D));
                    BlockPos movePosInf = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveInf.x, moveInf.y, moveInf.z));

                    this.leader.setTypedRecruitsSetAndHoldPos(movePosInf, ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosInf, ModEntityTypes.RECRUIT_SHIELDMAN.get());
                }

                case DEFAULT_1 -> {
                    //RANGED GO BEHIND LEADER AND HOLD POS
                    int rnd = this.leader.getRandom().nextInt(180);
                    Vec3 moveVecRanged = toTarget.yRot(90 - rnd).normalize();
                    Vec3 moveRanged = this.leader.position().add(moveVecRanged.scale(15D));
                    BlockPos movePosRanged = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveRanged.x, moveRanged.y, moveRanged.z));

                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.CROSSBOWMAN.get());

                    // LEADER HOLD POS
                    this.leader.setFollowState(2);//HOLD POS
                    this.leader.setTarget(null);
                    this.leader.setShouldBlock(false);

                    //Cavalry move freely
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());
                }
                case DEFAULT_2 -> {
                    //RANGED GO BEHIND LEADER AND HOLD POS
                    int rnd = this.leader.getRandom().nextInt(180);
                    Vec3 moveVecRanged = toTarget.yRot(90 - rnd).normalize();
                    Vec3 moveRanged = this.leader.position().add(moveVecRanged.scale(15D));
                    BlockPos movePosRanged = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveRanged.x, moveRanged.y, moveRanged.z));
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRanged, ModEntityTypes.CROSSBOWMAN.get());

                    //Cavalry move freely
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.NOMAD.get());
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.HORSEMAN.get());


                    this.leader.setTypedRecruitsToFollow(ModEntityTypes.RECRUIT.get());
                    this.leader.setTypedRecruitsToFollow(ModEntityTypes.RECRUIT_SHIELDMAN.get());

                    this.leader.setRecruitsShields(false);
                }

                case DEFAULT_3 -> {
                    //RANGED GO BEHIND LEADER AND HOLD POS
                    Vec3 moveVecLeft = toTarget.yRot(-90).normalize();
                    Vec3 moveLeft = this.leader.position().add(moveVecLeft.scale(20D));
                    BlockPos movePosLeft = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveLeft.x, moveLeft.y, moveLeft.z));
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosLeft, ModEntityTypes.BOWMAN.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosLeft, ModEntityTypes.RECRUIT.get());
                    this.leader.setHoldPos(movePosLeft);
                    this.leader.setFollowState(3);//BACK TO POS
                    this.leader.setTarget(null);

                    Vec3 moveVecRight = toTarget.yRot(-90).normalize();
                    Vec3 moveRight = this.leader.position().add(moveVecRight.scale(20D));
                    BlockPos movePosRight = this.leader.getCommandSenderWorld().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(moveRight.x, moveRight.y, moveRight.z));
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRight, ModEntityTypes.CROSSBOWMAN.get());
                    this.leader.setTypedRecruitsSetAndHoldPos(movePosRight, ModEntityTypes.RECRUIT_SHIELDMAN.get());

                    this.leader.setTypedRecruitsToFollow(ModEntityTypes.HORSEMAN.get());

                    //Nomads move freely
                    this.leader.setTypedRecruitsToWanderFreely(ModEntityTypes.NOMAD.get());

                    this.leader.setRecruitsShields(false);
                }

                case CHARGE -> {
                    //All freely
                    this.leader.setFollowState(2);//HOLD POS
                    this.leader.setRecruitsShields(false);
                    this.leader.setRecruitsWanderFreely();
                }

            }
        }
    }

    public int getPartySize(){
        return this.leader.currentRecruitsInCommand.size();
    }

    public int getRangedSize(){
        int bowman = Collections.frequency(this.leader.currentRecruitsInCommand, BowmanEntity.class);
        int crossbow = Collections.frequency(this.leader.currentRecruitsInCommand, CrossBowmanEntity.class);
        return bowman + crossbow;
    }

    public int getInfantrySize(){
        int rec = Collections.frequency(this.leader.currentRecruitsInCommand, RecruitEntity.class);
        int shield = Collections.frequency(this.leader.currentRecruitsInCommand, RecruitShieldmanEntity.class);
        return rec + shield;
    }

    public int getCavalrySize(){
        int horseman = Collections.frequency(this.leader.currentRecruitsInCommand, HorsemanEntity.class);
        int nomad = Collections.frequency(this.leader.currentRecruitsInCommand, NomadEntity.class);
        return horseman + nomad;
    }


    private enum AttackMode{
        DEFENSIVE(0),
        LIGHT_DEFENSIVE(1),
        DEFAULT_1(2),
        DEFAULT_2(3),
        DEFAULT_3(4),
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


