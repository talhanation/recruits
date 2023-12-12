package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.PatrolLeaderAttackAI;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;


import java.util.*;

public abstract class AbstractLeaderEntity extends AbstractChunkLoaderEntity implements ICompanion {
    private static final EntityDataAccessor<Integer> WAYPOINT_INDEX = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAIT_TIME_IN_MIN = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CYCLE = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> PATROLLING_STATE = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> INFO_MODE = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.BYTE);
    private boolean returning;
    private boolean retreating;
    public int commandCooldown = 0;
    private BlockPos currentWaypoint;
    private int waitingTime = 0;
    private int waitingForEnemiesTime = 0;

    private int waitForRecruitsTime = 0;
    public int infoCooldown = 0;
    private State state = State.IDLE;
    private String ownerName = "";
    public AbstractLeaderEntity(EntityType<? extends AbstractLeaderEntity> entityType, Level world) {
        super(entityType, world);
    }

    public Stack<BlockPos> WAYPOINTS = new Stack<>();
    public Stack<ItemStack> WAYPOINT_ITEMS = new Stack<>();
    public Stack<UUID> RECRUITS_IN_COMMAND = new Stack<>();

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WAYPOINT_INDEX, 0);
        this.entityData.define(WAIT_TIME_IN_MIN, 0);
        this.entityData.define(CYCLE, false);
        this.entityData.define(PATROLLING_STATE, (byte) 3);
        this.entityData.define(INFO_MODE, (byte) 0);
    }

    @Override
    protected void registerGoals() {
       super.registerGoals();
        this.goalSelector.addGoal(0, new PatrolLeaderAttackAI(this));
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("waypoint_index", this.getWaypointIndex());
        nbt.putInt("wait_time_in_min", this.getWaitTimeInMin());
        nbt.putInt("waiting_time", this.waitingTime);
        nbt.putBoolean("cycle", this.getCycle());
        nbt.putByte("patrollingState", this.getPatrollingState());
        nbt.putBoolean("returning", this.returning);
        nbt.putBoolean("retreating", this.retreating);
        nbt.putByte("infoMode", this.getInfoMode());
        nbt.putString("OwnerName", this.ownerName);

        ListTag waypointItems = new ListTag();
        for (int i = 0; i < WAYPOINT_ITEMS.size(); ++i) {
            ItemStack itemstack = WAYPOINT_ITEMS.get(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundnbt = new CompoundTag();
                compoundnbt.putByte("WaypointItem", (byte) i);
                itemstack.save(compoundnbt);
                waypointItems.add(compoundnbt);
            }
        }
        nbt.put("WaypointItems", waypointItems);

        ListTag waypoints = new ListTag();
        for(int i = 0; i < WAYPOINTS.size(); i++){
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putByte("Waypoint", (byte) i);
            BlockPos pos = WAYPOINTS.get(i);
            compoundnbt.putDouble("PosX", pos.getX());
            compoundnbt.putDouble("PosY", pos.getY());
            compoundnbt.putDouble("PosZ", pos.getZ());

            waypoints.add(compoundnbt);
        }
        nbt.put("Waypoints", waypoints);

        ListTag recruitsInCommand = new ListTag();
        for (int i = 0; i < RECRUITS_IN_COMMAND.size(); ++i) {
            UUID recruit = RECRUITS_IN_COMMAND.get(i);

            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putByte("Recruit", (byte) i);
            compoundnbt.putUUID("UUID", recruit);
            recruitsInCommand.add(compoundnbt);
        }
        nbt.put("RecruitsInCommand", recruitsInCommand);
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        this.setWaypointIndex(nbt.getInt("waypoint_index"));
        this.setWaitTimeInMin(nbt.getInt("wait_time_in_min"));
        this.setCycle(nbt.getBoolean("cycle"));
        this.setPatrollingState(nbt.getByte("patrollingState"), false);
        this.returning = nbt.getBoolean("returning");
        this.retreating = nbt.getBoolean("retreating");
        this.waitingTime = nbt.getInt("waiting_time");
        this.setInfoMode(nbt.getByte("infoMode"));
        this.ownerName = nbt.getString("ownerName");

        ListTag waypointItems = nbt.getList("WaypointItems", 10);
        for (int i = 0; i < waypointItems.size(); ++i) {
            CompoundTag compoundnbt = waypointItems.getCompound(i);

            ItemStack itemStack = ItemStack.of(compoundnbt);
            this.WAYPOINT_ITEMS.push(itemStack);
        }

        ListTag waypoints = nbt.getList("Waypoints", 10);
        for (int i = 0; i < waypoints.size(); ++i) {
            CompoundTag compoundnbt = waypoints.getCompound(i);
            BlockPos pos = new BlockPos(
                    compoundnbt.getDouble("PosX"),
                    compoundnbt.getDouble("PosY"),
                    compoundnbt.getDouble("PosZ"));
            this.WAYPOINTS.push(pos);
        }

        ListTag recruitsInCommand = nbt.getList("RecruitsInCommand", 10);
        for (int i = 0; i < recruitsInCommand.size(); ++i) {
            CompoundTag compoundnbt = recruitsInCommand.getCompound(i);

            UUID recruit = compoundnbt.getUUID("UUID");
            this.RECRUITS_IN_COMMAND.push(recruit);
        }

    }


    public void tick(){
        super.tick();

        if(infoCooldown > 0) infoCooldown--;
        if(commandCooldown > 0) commandCooldown--;

        if(this.tickCount % 10 == 0){
            double distance = 0D;
            if(currentWaypoint != null) distance = this.distanceToSqr(currentWaypoint.getX(), currentWaypoint.getY(), currentWaypoint.getZ());

            switch (state){
                case IDLE -> {

                }

                case STARTED -> {

                    if(currentWaypoint != null){

                        if(distance <= 10D){
                            updateWaypointIndex();
                            this.setRecruitsToFollow();
                            this.waitingTime = 0;

                            this.state = State.WAITING;
                        }
                        else{
                            this.getNavigation().moveTo(currentWaypoint.getX(), currentWaypoint.getY(), currentWaypoint.getZ(), 1F);
                            if (horizontalCollision || minorHorizontalCollision) {
                                this.getJumpControl().jump();
                            }
                        }


                    }
                    else if(!WAYPOINTS.isEmpty() && hasIndex())
                        this.currentWaypoint = WAYPOINTS.get(getWaypointIndex());

                    if(this.getTarget() != null && !retreating){
                        this.sendInfoAboutTarget(this.getTarget());
                        this.state = State.ATTACKING;
                    }

                    boolean isFirstWaypoint = getWaypointIndex() == 0;
                    if(isFirstWaypoint && waitForRecruitsTime == 0){
                        setRecruitsUpkeep();
                        setRecruitsWanderFreely();

                        this.waitForRecruitsTime = 400;
                        this.state = State.WAITING_RECRUITS;
                    }

                }

                case WAITING_RECRUITS -> {
                    if(--waitForRecruitsTime == 0){
                        state = State.STARTED;
                    }
                }

                case PAUSED -> {

                }

                case STOPPED -> {

                }

                case WAITING -> {
                    if(timerElapsed() && hasIndex()){
                        this.currentWaypoint = WAYPOINTS.get(getWaypointIndex());
                        this.state = State.STARTED;
                    }

                    if(distance > 25D && this.getTarget() == null){
                        this.getNavigation().moveTo(currentWaypoint.getX(), currentWaypoint.getY(), currentWaypoint.getZ(), 1F);
                        if (this.horizontalCollision || minorHorizontalCollision) {
                            this.getJumpControl().jump();
                        }
                    }

                    if(this.getTarget() != null){
                        this.sendInfoAboutTarget(this.getTarget());
                        this.state = State.ATTACKING;
                    }
                }

                case ATTACKING -> {
                    if(distance > 500D || (this.getTarget() != null && !this.getTarget().isAlive()) || this.getTarget() == null){
                        this.setTarget(null);
                        this.setRecruitsClearTargets();
                        this.setRecruitsToFollow();
                        this.state = State.WAITING_ENEMIES;
                    }
                }

                case RETREATING -> {
                    if(this.getOwner() != null) {
                        this.getOwner().sendMessage(RETREATING(), this.getOwnerUUID());
                    }
                    this.retreating = true;
                    this.setRecruitsClearTargets();
                    this.setRecruitsToFollow();
                    this.state = State.STARTED;
                }

                case WAITING_ENEMIES -> {
                    if(this.getTarget() != null){
                        this.state = State.ATTACKING;
                    }
                    else if(++waitingForEnemiesTime > 100){
                        this.waitingForEnemiesTime = 0;
                        this.state = State.STARTED;
                    }
                }
            }
        }

    }

    private void sendInfoAboutTarget(LivingEntity target) {
        InfoMode infoMode = InfoMode.fromIndex(getInfoMode());
        if(this.getOwner() != null && infoCooldown == 0 && infoMode != InfoMode.NONE){
            if((infoMode == InfoMode.ALL || infoMode == InfoMode.HOSTILE) && (target.getType().getCategory() == MobCategory.MONSTER || target instanceof Pillager)){
                this.getOwner().sendMessage(HOSTILE_CONTACT(this.getOnPos()), this.getOwnerUUID());
            }
            else if((infoMode == InfoMode.ALL || infoMode == InfoMode.ENEMY) && (target instanceof Player || target instanceof AbstractRecruitEntity recruitTarget && (recruitTarget.isOwned() || recruitTarget.getTeam() != null))){
                this.getOwner().sendMessage(ENEMY_CONTACT(target.getType().toString(), this.getOnPos()), this.getOwnerUUID());
            }

            infoCooldown = 20 * 60;
        }
    }

    private boolean hasIndex(){
        return !WAYPOINTS.isEmpty() && WAYPOINTS.size() > getWaypointIndex();
    }

    private boolean timerElapsed() {
        return ++waitingTime >= this.getWaitTimeInMin() * 60 * 20;
    }

    public void decreaseIndex() {
        int currentIndex = this.getWaypointIndex();
        int nextIndex = currentIndex - 1;
        if (nextIndex >= 0) this.setWaypointIndex(nextIndex);
    }

    public void increaseIndex(){
        int currentIndex = this.getWaypointIndex();
        int nextIndex = currentIndex + 1;

        if(nextIndex < WAYPOINTS.size())
            this.setWaypointIndex(nextIndex);
    }

    public void updateWaypointIndex(){
        int currentIndex = this.getWaypointIndex();
        boolean isCycling = this.getCycle();
        boolean isLastWaypoint = currentIndex == WAYPOINTS.size() - 1;
        boolean isFirstWaypoint = currentIndex == 0;
        if(isCycling && !retreating){
            if(isLastWaypoint){
                this.setWaypointIndex(0);
            }
            else {
                increaseIndex();
            }
        }
        else{
            if(returning || retreating){
                if(isFirstWaypoint){ //is current last waypoint
                    this.returning = false;
                    this.retreating = false;
                }
                else {
                    decreaseIndex();
                }
            }
            else{
                if(isLastWaypoint){
                    this.returning = true;
                }
                else{
                    increaseIndex();
                }
            }
        }
    }
    public void setPatrollingState(byte state, boolean setFollow) {
        this.entityData.set(PATROLLING_STATE, state);
        this.state = State.fromIndex(state);

        if(this.state == State.STARTED){
            this.setRecruitsToFollow();
            if(setFollow) this.setFollowState(0);//wander freely
        }
        else if (this.state == State.PAUSED){
            if(setFollow) this.setFollowState(1);//follow
            this.retreating = false;
        }
        else if (this.state == State.STOPPED){
            this.retreating = false;
            this.returning = false;
            this.setWaypointIndex(0);
            if(WAYPOINTS != null && WAYPOINTS.size() > getWaypointIndex()) this.currentWaypoint = WAYPOINTS.get(getWaypointIndex());
            if(setFollow) this.setFollowState(1);//follow
        }
    }

    @Override
    public void setFollowState(int state) {
        super.setFollowState(state);

        //Pause the patrolling when command is not wander freely
        if(state != 0  && WAYPOINTS != null  && WAYPOINTS.size() > 0){
            if(getTarget() != null) this.setPatrollingState((byte) 5, false);//ATTACKING
            else this.setPatrollingState((byte) 2, false);//PAUSED
        }
    }
    public String getOwnerName() {
        return ownerName;
    }
    public void setOwnerName(String name) {
        ownerName = name;
    }
    public byte getInfoMode() {
        return this.entityData.get(INFO_MODE);
    }
    public void setInfoMode(byte x) {
        this.entityData.set(INFO_MODE, x);
    }

    public void setCycle(boolean cycle) {
        this.entityData.set(CYCLE, cycle);
    }

    public void setWaitTimeInMin(int wait_time_in_min) {
        this.entityData.set(WAIT_TIME_IN_MIN, wait_time_in_min);
    }

    public void setWaypointIndex(int current_waypoint_index) {
        this.entityData.set(WAYPOINT_INDEX, current_waypoint_index);
    }

    public byte getPatrollingState() {
        return this.entityData.get(PATROLLING_STATE);
    }

    public boolean getCycle() {
        return this.entityData.get(CYCLE);
    }

    public int getWaitTimeInMin() {
        return this.entityData.get(WAIT_TIME_IN_MIN);
    }

    public int getWaypointIndex() {
        return this.entityData.get(WAYPOINT_INDEX);
    }

    public MutableComponent ENEMY_CONTACT(String name, BlockPos pos){
        return new TranslatableComponent("chat.recruits.text.patrol_leader_enemy_contact", this.getName().getString(), name, pos.getX(), pos.getY(), pos.getZ());
    }

    public MutableComponent HOSTILE_CONTACT(BlockPos pos){
        return new TranslatableComponent("chat.recruits.text.patrol_leader_hostile_contact", this.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
    }

    public MutableComponent RETREATING(){
        return new TranslatableComponent("chat.recruits.text.patrol_leader_retreating", this.getName().getString());
    }

    public ItemStack getItemStackToRender(BlockPos pos){
        BlockState state = this.level.getBlockState(pos);
        ItemStack itemStack;
        if (state.is(Blocks.WATER) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)){

            itemStack = new ItemStack(Blocks.WATER);
        }
        else if (state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR)) itemStack = new ItemStack(Items.GRASS_BLOCK);
        else itemStack = new ItemStack(state.getBlock().asItem());

        return itemStack;
    }

    public void addWaypoint(BlockPos pos) {
        ItemStack itemStack = this.getItemStackToRender(pos);

        WAYPOINT_ITEMS.push(itemStack);
        WAYPOINTS.push(pos);
    }

    public enum State{
        IDLE((byte) 0), //follow, hold pos, protect, wander freely
        STARTED((byte) 1), //traveling from first to last or cycle
        PAUSED((byte) 2),
        STOPPED((byte) 3),
        WAITING((byte) 4),
        ATTACKING((byte) 5), //traveling is paused attacking enemies
        RETREATING((byte) 6), //traveling back to first waypoint from current one
        WAITING_ENEMIES((byte) 7), //waiting for more enemies
        WAITING_RECRUITS((byte) 8);
        private final byte index;
        State(byte index){
            this.index = index;
        }

        public int getIndex(){
            return this.index;
        }

        public static State fromIndex(byte index) {
            for (State state : State.values()) {
                if (state.getIndex() == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }
    }

    public enum InfoMode{
        ALL((byte) 0),
        NONE((byte) 1),
        ENEMY((byte) 2),
        HOSTILE((byte) 3);

        private final byte index;
        InfoMode(byte index){
            this.index = index;
        }

        public byte getIndex(){
            return this.index;
        }

        public InfoMode getNext(){
            int length = values().length;
            byte newIndex = (byte) (this.index + 1);
            if(newIndex >= length){
                return ALL;
            }
            else
                return fromIndex(newIndex);
        }

        public static InfoMode fromIndex(byte index) {
            for (InfoMode state : InfoMode.values()) {
                if (state.getIndex() == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid InfoMode index: " + index);
        }
    }

    public List<AbstractRecruitEntity> currentRecruitsInCommand = new ArrayList<>();
    public List<AbstractRecruitEntity> getRecruitsInCommand(){
        List<AbstractRecruitEntity> list = this.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, getBoundingBox().inflate(100D));
        List<AbstractRecruitEntity> recruits = new ArrayList<>();

        for (AbstractRecruitEntity recruit : list){
            if(!recruit.getUUID().equals(this.getUUID()) && RECRUITS_IN_COMMAND.contains(recruit.getUUID()) && recruit.getProtectUUID() != null && recruit.getProtectUUID().equals(this.getUUID())){
                recruits.add(recruit);
            }
        }

        return recruits;
    }

    public void setRecruitsToFollow(){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.setProtectUUID(Optional.of(this.getUUID()));
            recruit.setFollowState(5);//Protect/Follow
        }
    }

    public void setRecruitsToHoldPos(){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.setFollowState(2);//HOLD POS
        }
    }

    public void setRecruitsToMove(BlockPos pos){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.setMovePos(pos);
            recruit.setFollowState(0);// needs to be above setShouldMovePos
            recruit.setShouldMovePos(true);
        }
    }

    public void setRecruitsClearTargets(){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.setTarget(null);
        }
    }

    public void setTypedRecruitsTarget(EntityType<?> type, LivingEntity target){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            if(recruit.getType().equals(type)){
                recruit.setTarget(target);
            }
        }
    }
    public void setRecruitsWanderFreely(){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.clearHoldPos();
            recruit.setFollowState(0);
        }
    }

    public void setRecruitsShields(boolean shields){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.clearHoldPos();
            recruit.setShouldBlock(shields);
        }
    }
    public void setTypedRecruitsToMove(BlockPos pos, EntityType<?> type){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            if(recruit.getType().equals(type)){
                recruit.setMovePos(pos);
                recruit.setFollowState(0);// needs to be above setShouldMovePos
                recruit.setShouldMovePos(true);
            }
        }
    }

    public void setTypedRecruitsToHoldPos(EntityType<?> type){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            if(recruit.getType().equals(type))
                recruit.setFollowState(2);//HOLD POS
        }
    }

    public void setTypedRecruitsSetAndHoldPos(BlockPos pos, EntityType<?> type){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            if(recruit.getType().equals(type)){
                recruit.setHoldPos(pos);//set pos
                recruit.setFollowState(3);//back to pos
            }
        }
    }

    public void setTypedRecruitsToFollow(EntityType<?> type){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            if(recruit.getType().equals(type))
                recruit.setFollowState(5);//FOLLOW/PROTECT
        }
    }

    public void setTypedRecruitsToWanderFreely(EntityType<?> type){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            if(recruit.getType().equals(type)){
                recruit.clearHoldPos();
                recruit.setFollowState(0);//Freely
            }

        }
    }

    public void setRecruitsUpkeep(){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.clearUpkeepEntity();
            recruit.clearUpkeepPos();

            if(this.getUpkeepPos() != null) recruit.setUpkeepPos(this.getUpkeepPos());
            recruit.setUpkeepUUID(Optional.ofNullable(this.getUpkeepUUID()));

            recruit.setUpkeepTimer(0);
            recruit.setTarget(null);
        }
    }

    @Override
    public void die(DamageSource dmg) {
        super.die(dmg);
        if(!currentRecruitsInCommand.isEmpty()){
            setRecruitsWanderFreely();
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource dmg, float amt) {
        if (this.getMaxHealth() * 0.25 > this.getHealth() && state != State.RETREATING) {
            this.setRecruitsClearTargets();
            this.setRecruitsToFollow();
            this.setRecruitsShields(false);
            this.state = State.RETREATING;
        }
        return super.hurt(dmg, amt);
    }
}










