package com.talhanation.recruits.entities;

import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ScoutEntity extends AbstractRecruitEntity implements ICompanion {

    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(ScoutEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> TASK_STATE = SynchedEntityData.defineId(ScoutEntity.class, EntityDataSerializers.BYTE);

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));
    private State state = State.IDLE;
    private final int SEARCH_RADIUS = 200;
    private int timerScouting = 120;
    public ScoutEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(TASK_STATE, (byte) 0);
    }
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(ForgeMod.REACH_DISTANCE.get(), 0D)
                .add(Attributes.ATTACK_SPEED);
    }


    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setDropEquipment();
        this.setPersistenceRequired();
        if(this.getOwner() != null)this.setOwnerName(this.getOwner().getName().getString());
        AbstractRecruitEntity.applySpawnValues(this);
    }

    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack){
        return !(itemStack.getItem() instanceof CrossbowItem || itemStack.getItem() instanceof BowItem); //TODO: add ranged combat
    }

    @Override
    public AbstractRecruitEntity get() {
        return this;
    }

    @Override
    public void openSpecialGUI(Player player) {

    }
    public byte getTaskState() {
        return entityData.get(TASK_STATE);
    }
    public String getOwnerName() {
        return entityData.get(OWNER_NAME);
    }

    public void setOwnerName(String name) {
        entityData.set(OWNER_NAME, name);
    }
    public void setTaskState(State state) {
        this.entityData.set(TASK_STATE, (byte) state.getIndex());
        this.state = state;
    }
    public boolean isAtMission() {
        return this.state != State.IDLE;
    }

    public enum State{
        IDLE(0),
        SCOUTING(1),
        SEARCHING_STRUCTURE(2),
        SEARCHING_LOST_RECRUITS(3);

        private final int index;
        State(int index){
            this.index = index;
        }

        public int getIndex(){
            return this.index;
        }

        public static State fromIndex(int index) {
            for (State state : State.values()) {
                if (state.getIndex() == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }
    }

    @Override
    public void tick() {
        super.tick();
        /*
        if(--timerScouting <= 0){
            this.startTask(State.SCOUTING);
            timerScouting = 20*60*2;
        }

         */
    }
    public void startTask(State taskState) {
        setTaskState(taskState);

        switch (taskState) {
            case SEARCHING_STRUCTURE -> {
                sendMessageToOwner("Scout: Ich suche nach einer Struktur...");
                //findNonFriendlyEntities(this);
                this.state = State.IDLE;
            }
            case SCOUTING -> {
                sendMessageToOwner("Scout: Ich suche nach feindlichen Spielern und Recruits...");
                this.findNonFriendlyEntities();
                this.state = State.IDLE;
            }
            case SEARCHING_LOST_RECRUITS -> {
                sendMessageToOwner("Scout: Ich suche nach verlorenen Recruits...");
                //findNonFriendlyEntities(this);
                this.state = State.IDLE;
            }
            default -> sendMessageToOwner("Unbekannte Mission.");
        }
    }

    private void sendMessageToOwner(String message) {
        if (getOwner() != null) {
            this.getOwner().sendMessage(new TextComponent(this.getName().getString() + ": " +  message).withStyle(ChatFormatting.GOLD), getOwnerUUID());
        }
    }
    List<ServerPlayer> potentialPlayerTargets;
    List<AbstractRecruitEntity> potentialRecruitTargets;
    public void findNonFriendlyEntities() {
        if (getOwner() == null) return;
        potentialPlayerTargets = new ArrayList<>();
        potentialRecruitTargets = new ArrayList<>();

        if (!this.getCommandSenderWorld().isClientSide()) {
            potentialPlayerTargets = this.getCommandSenderWorld().getEntitiesOfClass(
                    ServerPlayer.class,
                    this.getBoundingBox().inflate(SEARCH_RADIUS),
                    (target) -> isNonAlly(target) && this.hasLineOfSight(target)
            );

            potentialRecruitTargets = this.getCommandSenderWorld().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    this.getBoundingBox().inflate(SEARCH_RADIUS),
                    (target) -> isNonAlly(target) && this.hasLineOfSight(target)
            );

            if(!potentialPlayerTargets.isEmpty()){
                for(ServerPlayer player : potentialPlayerTargets){
                    String teamName = player.getTeam() != null ? player.getTeam().getName() : "";
                    RecruitsPlayerInfo playerInfo = new RecruitsPlayerInfo(player.getUUID(), player.getName().getString(), TeamEvents.recruitsTeamManager.getTeamByStringID(teamName));
                    int recruitsCount = countRecruits(player.getUUID());
                    int distance = (int) Math.sqrt(this.blockPosition().distSqr(player.blockPosition()));
                    String direction = getHorizontalDirection(this.blockPosition(), player.blockPosition());

                    ScoutingResult result = new ScoutingResult(playerInfo, recruitsCount, distance, direction);
                    result.sendInfo(this);
                }
            }

            if(!potentialRecruitTargets.isEmpty()){
                Map<String, Integer> teamRecruitCount = potentialRecruitTargets.stream()
                        .collect(Collectors.toMap(
                                recruit -> recruit.getTeam() != null ? recruit.getTeam().getName() : "No Team",
                                recruit -> 1,
                                Integer::sum
                        ));

                for (Map.Entry<String, Integer> entry : teamRecruitCount.entrySet()) {
                    String teamName = entry.getKey();
                    int recruitCount = entry.getValue();
                    int distance = (int) Math.sqrt(this.blockPosition().distSqr(null));
                    String direction = getHorizontalDirection(this.blockPosition(), null);

                    ScoutingResult result = new ScoutingResult(teamName, recruitCount, distance, direction);
                }
            }
        }
    }

    private int countRecruits(UUID uuid) {
        Predicate<AbstractRecruitEntity> predicate = recruit -> recruit.isOwned() && recruit.getOwnerUUID().equals(uuid);
        int amount = (int) potentialRecruitTargets.stream().filter(predicate).count();
        potentialRecruitTargets.removeIf(predicate);
        return amount;
    }

    private boolean isNonAlly(LivingEntity entity) {
        if (entity == this || entity == this.getOwner() || !(entity instanceof Player || entity instanceof AbstractRecruitEntity)) {
            return false;
        }

        Team scoutTeam = this.getTeam();
        Team targetTeam = entity.getTeam();

        return targetTeam != null && scoutTeam != null && TeamEvents.recruitsDiplomacyManager != null &&
                TeamEvents.recruitsDiplomacyManager.getRelation(scoutTeam.getName(), targetTeam.getName())
                        != RecruitsDiplomacyManager.DiplomacyStatus.ALLY;
    }


    private String getHorizontalDirection(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        double angle = Math.toDegrees(Math.atan2(dz, dx));
        angle = (angle + 360) % 360;

        if (angle >= 337.5 || angle < 22.5) {
            return "East";
        } else if (angle >= 22.5 && angle < 67.5) {
            return "South-East";
        } else if (angle >= 67.5 && angle < 112.5) {
            return "South";
        } else if (angle >= 112.5 && angle < 157.5) {
            return "South-West";
        } else if (angle >= 157.5 && angle < 202.5) {
            return "West";
        } else if (angle >= 202.5 && angle < 247.5) {
            return "North-West";
        } else if (angle >= 247.5 && angle < 292.5) {
            return "North";
        } else {
            return "North-East";
        }
    }

    public static Component PLAYER_LOCATED(String player){
        return new TranslatableComponent("gui.recruits.text.scoutLocatedPlayer", player);
    }

    private static class ScoutedUnits{
        String team;
        BlockPos pos;

    }

    private static class ScoutingResult {
        RecruitsPlayerInfo playerInfo;
        int recruitsCount;
        int distance;
        String direction;
        String team;

        ScoutingResult(RecruitsPlayerInfo playerInfo, int recruits, int distance, String direction){
            this.playerInfo = playerInfo;
            this.recruitsCount = recruits;
            this.distance = distance;
            this.direction = direction;
        }

        ScoutingResult(String team, int recruits, int distance, String direction){
            this((RecruitsPlayerInfo) null, recruits, distance, direction);
            this.team = team;
        }

        public void sendInfo(ScoutEntity scout){
            if(playerInfo != null){


                if(playerInfo.getRecruitsTeam() != null){
                    //Scout: I'm Seeing talhanation (YellowTeam) and 12 units, 125 blocks South from me.
                    scout.sendMessageToOwner(String.format("%s (%s), %d units, %d blocks %s", playerInfo.getName(), playerInfo.getRecruitsTeam().getTeamDisplayName(), recruitsCount, distance, direction));
                }
                else{//Scout: I'm Seeing talhanation and 12 units, 125 blocks South from me.
                    scout.sendMessageToOwner(String.format("%s, %d units, %d blocks %s",playerInfo.getName(), recruitsCount, distance, direction));
                }
            }
            else{
                if(team != null){// Scout: I'm Seeing 12 units of YellowTeam, 125 blocks North from me.
                    scout.sendMessageToOwner(String.format("%d units of %s, %d blocks %s", recruitsCount, team, distance, direction));
                }
            }
        }
    }
}









