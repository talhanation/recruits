package com.talhanation.recruits.entities;

import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.util.FormationUtils;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;
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
    public int timerScouting;
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
        nbt.putInt("taskState", this.state.getIndex());
        nbt.putInt("timerScouting", this.timerScouting);
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setTaskState(State.fromIndex(nbt.getInt("taskState")));
        this.timerScouting = nbt.getInt("timerScouting");
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(ForgeMod.SWIM_SPEED.get(), 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(ForgeMod.ENTITY_REACH.get(), 0D)
                .add(Attributes.ATTACK_SPEED);
    }


    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(random, difficultyInstance);

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

        if(--timerScouting <= 0){
            timerScouting = 20*60*1; //1 min
            this.startTask(State.fromIndex(getTaskState()));
        }
    }
    public void startTask(State taskState) {
        setTaskState(taskState);

        switch (taskState) {
            case SEARCHING_STRUCTURE -> {
                //TBD
            }
            case SCOUTING -> {
                this.findNonFriendlyEntities();
                timerScouting = 20*60*1; //1 min
            }
            case SEARCHING_LOST_RECRUITS -> {
                //TBD

            }
            default -> {}
        }
    }

    private void sendMessageToOwner(Component message) {
        if (getOwner() != null) {
            MutableComponent prefix = Component.literal(this.getName().getString() + ": ").withStyle(ChatFormatting.GOLD);
            this.getOwner().sendSystemMessage(prefix.append(message));
        }
    }

    List<ServerPlayer> potentialPlayerTargets;
    List<AbstractRecruitEntity> potentialRecruitTargets;
    public void findNonFriendlyEntities() {
        if (getOwner() == null) return;
        if (this.getCommandSenderWorld().isClientSide()) return;

        potentialPlayerTargets = new ArrayList<>();
        potentialRecruitTargets = new ArrayList<>();

        if (!this.getCommandSenderWorld().isClientSide()) {
            potentialPlayerTargets = this.getCommandSenderWorld().getEntitiesOfClass(
                    ServerPlayer.class,
                    this.getBoundingBox().inflate(SEARCH_RADIUS),
                    (target) -> shouldAttack(target) && this.hasLineOfSight(target)
            );

            potentialRecruitTargets = this.getCommandSenderWorld().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    this.getBoundingBox().inflate(SEARCH_RADIUS),
                    (target) -> shouldAttack(target) && this.hasLineOfSight(target)
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
                Map<String, List<LivingEntity>> teamedRecruitsMap = potentialRecruitTargets.stream()
                        .collect(Collectors.groupingBy(
                                recruit -> recruit.getTeam() != null ? recruit.getTeam().getName() : "No Team"
                        ));

                for (Map.Entry<String, List<LivingEntity>> entry : teamedRecruitsMap.entrySet()) {
                    String teamName = entry.getKey();
                    int recruitCount = entry.getValue().size();
                    Vec3 vec = FormationUtils.getCenterOfPositions(entry.getValue(), (ServerLevel) this.getCommandSenderWorld());
                    int distance = (int) Math.sqrt(this.blockPosition().distSqr(new BlockPos((int) vec.x, (int) vec.y, (int) vec.z)));
                    String direction = getHorizontalDirection(this.blockPosition(), new BlockPos((int) vec.x, (int) vec.y, (int) vec.z));

                    ScoutingResult result = new ScoutingResult(teamName, recruitCount, distance, direction);
                    result.sendInfo(this);
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
        public void sendInfo(ScoutEntity scout) {
            Component recruitsText = Component.literal("" + recruitsCount).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            Component distanceText = Component.literal("" + distance).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            Component directionText = Component.literal("" + direction).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            if (playerInfo != null) {
                Component playerName = Component.literal(playerInfo.getName());

                //Scout: I'm Seeing talhanation (YellowTeam) and 12 units, 125 blocks South from me.
                if (playerInfo.getRecruitsTeam() != null) {
                    playerName = Component.literal(playerInfo.getName()).withStyle(ChatFormatting.getById(playerInfo.getRecruitsTeam().getTeamColor()));
                    Component teamName = Component.literal(playerInfo.getRecruitsTeam().getTeamDisplayName()).withStyle(ChatFormatting.getById(playerInfo.getRecruitsTeam().getTeamColor()));
                    scout.sendMessageToOwner(Component.literal("")
                            .append(playerName).append(" (")
                            .append(teamName).append("), with ")
                            .append(recruitsText).append(" units, ")
                            .append(distanceText).append(" blocks, ")
                            .append(directionText).append(" from me.")
                    );
                } else {//Scout: I'm Seeing talhanation and 12 units, 125 blocks South from me.
                    scout.sendMessageToOwner(Component.literal("")
                            .append(playerName).append(" with ")
                            .append(recruitsText).append(" units, ")
                            .append(distanceText).append(" blocks, ")
                            .append(directionText).append(" from me.")
                    );
                }
            } else {// Scout: I'm Seeing 12 units of YellowTeam, 125 blocks North from me.
                if (team != null) {
                    Component teamText = Component.literal(team);

                    scout.sendMessageToOwner(Component.literal("")
                            .append(recruitsText).append(" units of ")
                            .append(teamText).append(", ")
                            .append(distanceText).append(" blocks, ")
                            .append(directionText).append(" from me.")
                    );
                }
            }
        }
    }

}









