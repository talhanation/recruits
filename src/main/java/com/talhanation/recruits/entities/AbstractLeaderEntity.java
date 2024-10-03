package com.talhanation.recruits.entities;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.PatrolLeaderContainer;
import com.talhanation.recruits.network.MessageOpenSpecialScreen;
import com.talhanation.recruits.network.MessageToClientUpdateLeaderScreen;
import com.talhanation.recruits.util.FormationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class AbstractLeaderEntity extends AbstractChunkLoaderEntity implements ICompanion {
    private static final EntityDataAccessor<Integer> WAYPOINT_INDEX = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAIT_TIME_IN_MIN = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CYCLE = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FAST_PATROLLING = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> PATROLLING_STATE = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> INFO_MODE = SynchedEntityData.defineId(AbstractLeaderEntity.class, EntityDataSerializers.BYTE);
    public boolean returning;
    public boolean retreating;
    public int commandCooldown = 0;
    public BlockPos currentWaypoint;
    protected int waitingTime = 0;
    protected int waitForRecruitsUpkeepTime = 0;
    public int infoCooldown = 0;
    protected State state = State.IDLE;
    public State prevState = State.IDLE;
    protected String ownerName = "";
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
        this.entityData.define(FAST_PATROLLING, false);
        this.entityData.define(PATROLLING_STATE, (byte) 3);
        this.entityData.define(INFO_MODE, (byte) 0);
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("waypoint_index", this.getWaypointIndex());
        nbt.putInt("wait_time_in_min", this.getWaitTimeInMin());
        nbt.putInt("waiting_time", this.waitingTime);
        nbt.putBoolean("cycle", this.getCycle());
        nbt.putByte("patrolState", (byte) this.state.getIndex());
        nbt.putByte("prevPatrolState", (byte) this.prevState.getIndex());
        nbt.putBoolean("returning", this.returning);
        nbt.putBoolean("retreating", this.retreating);
        nbt.putByte("infoMode", this.getInfoMode());
        nbt.putString("OwnerName", this.ownerName);
        nbt.putBoolean("fastPatrolling", this.getFastPatrolling());
        nbt.putInt("waitForRecruitsUpkeepTime", this.waitForRecruitsUpkeepTime);

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
        this.setPatrolState(State.fromIndex(nbt.getByte("patrolState")));
        this.prevState = State.fromIndex(nbt.getByte("prevPatrolState"));
        this.returning = nbt.getBoolean("returning");
        this.retreating = nbt.getBoolean("retreating");
        this.waitingTime = nbt.getInt("waiting_time");
        this.waitForRecruitsUpkeepTime = nbt.getInt("waitForRecruitsUpkeepTime");
        this.setInfoMode(nbt.getByte("infoMode"));
        this.ownerName = nbt.getString("ownerName");
        this.setFastPatrolling(nbt.getBoolean("fastPatrolling"));

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
    private boolean retreatingMessage = false;
    public void tick(){
        super.tick();

        if(infoCooldown > 0) infoCooldown--;
        if(commandCooldown > 0) commandCooldown--;
        if(waitForRecruitsUpkeepTime > 0) waitForRecruitsUpkeepTime--;

        double distance = 0D;
        if(currentWaypoint != null) distance = this.distanceToSqr(currentWaypoint.getX(), currentWaypoint.getY(), currentWaypoint.getZ());

        switch (state){
            case IDLE, PAUSED, STOPPED -> {

            }

            case PATROLLING -> {

                if(currentWaypoint != null){

                    if(distance <= this.getDistanceToReachWaypoint()){

                        //re-supply at first waypoint
                        boolean isFirstWaypoint = getWaypointIndex() == 0;
                        BlockPos pos = this.getUpkeepPos();
                        if(pos != null && pos.distSqr(this.getOnPos()) < 5000 && isFirstWaypoint && waitForRecruitsUpkeepTime == 0){

                            this.handleResupply();

                            this.waitForRecruitsUpkeepTime = this.getResupplyTime(); // resupplying time
                            this.setPatrolState(State.UPKEEP);
                            this.retreating = false;
                            this.retreatingMessage = false;
                        }
                        else
                        {
                            this.updateWaypointIndex();

                            this.waitingTime = 120;

                            this.setPatrolState(State.WAITING);
                        }
                    }
                    else{
                        moveToCurrentWaypoint();
                    }
                }
                else if(!WAYPOINTS.isEmpty() && hasIndex()) {
                    this.currentWaypoint = WAYPOINTS.get(getWaypointIndex());
                }
                else
                    this.setPatrolState(State.IDLE);

                if(this.getTarget() != null && !retreating){
                    this.sendInfoAboutTarget(this.getTarget());

                    if(canAttackWhilePatrolling(this.getTarget())){

                        this.setRecruitsToFollow();
                        this.setPatrolState(State.ATTACKING);
                    }
                    else{
                        this.setTarget(null);
                    }
                }
            }

            case WAITING -> {
                if(timerElapsed() && hasIndex()){
                    this.currentWaypoint = WAYPOINTS.get(getWaypointIndex());
                    this.currentRecruitsInCommand = getRecruitsInCommand();
                    this.setPatrolState(State.PATROLLING);
                }

                if(distance > 25D && this.getTarget() == null){
                    moveToCurrentWaypoint();
                }

                if(this.getTarget() != null && this.getTarget().isAlive()){
                    this.sendInfoAboutTarget(this.getTarget());

                    if(canAttackWhilePatrolling(this.getTarget())){
                        this.setRecruitsToFollow();
                        this.setPatrolState(State.ATTACKING);

                    }
                    else {
                        this.setTarget(null);
                    }
                }
            }

            case ATTACKING -> {
                if(this.retreating && WAYPOINTS != null && WAYPOINTS.size() > 0){
                    this.setPatrolState(State.RETREATING);
                }
                if(this.getTarget() != null && !this.getTarget().isAlive()){

                    this.setPatrolState(prevState);
                }
                //AI-Task Taking care of this state
            }

            case RETREATING -> {
                if(this.getOwner() != null && !retreatingMessage) {
                    this.getOwner().sendSystemMessage(RETREATING());
                    retreatingMessage = true;
                }
                this.retreating = true;
                this.setRecruitsClearTargets();
                this.setRecruitsToFollow();
                this.setRecruitsShields(false);
                this.setPatrolState(State.PATROLLING);
            }

            case UPKEEP -> {
                this.handleUpkeepState();
            }
        }
    }

    @Override
    public void setState(int state) {
        super.setState(state);
        this.setRecruitsAggroState(state);
    }

    protected void handleUpkeepState() {
        if(waitForRecruitsUpkeepTime == 0){
            waitForRecruitsUpkeepTime = this.getAgainResupplyTime(); // time to resupply again
            this.setPatrolState(State.PATROLLING);
        }
    }

    public boolean canAttackWhilePatrolling(LivingEntity target) {
        return target != null && target.isAlive();
    }

    public void handleResupply() {
        setRecruitsWanderFreely();
        setRecruitsUpkeep();
        this.forcedUpkeep = true;
    }

    public int getResupplyTime() {
        return 1000;
    }

    public int getAgainResupplyTime() {
        return 60*20*10;
    }

    public void resetPatrolling(){
        waitForRecruitsUpkeepTime = 0;
        this.retreating = false;
        this.waitingTime = 0;
        this.setRecruitsClearTargets();
        this.setRecruitsToFollow();
        this.setRecruitsShields(false);
    }
    public double getDistanceToReachWaypoint() {
        return 15D;
    }

    protected void moveToCurrentWaypoint() {
        if(this.tickCount % 20 == 0){
            this.getNavigation().moveTo(currentWaypoint.getX(), currentWaypoint.getY(), currentWaypoint.getZ(), this.getFastPatrolling() ? 1F : 0.65F);
            this.setRecruitsToMove(this.currentWaypoint);
        }

        if (horizontalCollision || minorHorizontalCollision) {
            this.getJumpControl().jump();
        }
    }

    public void setPatrolState(State state){
        this.entityData.set(PATROLLING_STATE, (byte)  state.getIndex());
        if(this.state != this.prevState) this.prevState = this.state;
        this.state = state;
    }

    private void sendInfoAboutTarget(LivingEntity target) {
        InfoMode infoMode = InfoMode.fromIndex(getInfoMode());
        if(this.getOwner() != null && infoCooldown == 0 && infoMode != InfoMode.NONE){
            if((infoMode == InfoMode.ALL || infoMode == InfoMode.HOSTILE) && (target.getType().getCategory() == MobCategory.MONSTER || target instanceof Pillager)){
                this.getOwner().sendSystemMessage(HOSTILE_CONTACT(this.getOnPos()));
            }
            else if((infoMode == InfoMode.ALL || infoMode == InfoMode.ENEMY) && (target instanceof Player || target instanceof AbstractRecruitEntity recruitTarget && (recruitTarget.isOwned() || recruitTarget.getTeam() != null))){
                this.getOwner().sendSystemMessage(ENEMY_CONTACT(target.getType().toString(), this.getOnPos()));
            }

            infoCooldown = 20 * 60;
        }
    }

    private boolean hasIndex(){
        return !WAYPOINTS.isEmpty() && WAYPOINTS.size() > getWaypointIndex();
    }

    private boolean timerElapsed() {
        return ++waitingTime > getWaitTimeInMin() * 60 * 20;
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

    public void setFastPatrolling(boolean fastPatrolling) {
        this.entityData.set(FAST_PATROLLING, fastPatrolling);
    }

    public boolean getFastPatrolling() {
        return this.entityData.get(FAST_PATROLLING);
    }

    public MutableComponent ENEMY_CONTACT(String name, BlockPos pos){
        return Component.translatable("chat.recruits.text.patrol_leader_enemy_contact", this.getName().getString(), name, pos.getX(), pos.getY(), pos.getZ());
    }

    public MutableComponent HOSTILE_CONTACT(BlockPos pos){
        return Component.translatable("chat.recruits.text.patrol_leader_hostile_contact", this.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
    }

    public MutableComponent RETREATING(){
        return Component.translatable("chat.recruits.text.patrol_leader_retreating", this.getName().getString());
    }

    public ItemStack getItemStackToRender(BlockPos pos){
        BlockState state = this.level.getBlockState(pos);
        ItemStack itemStack;
        if (state.is(Blocks.WATER) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)){
            if(this instanceof CaptainEntity){
                itemStack = IBoatController.getSmallShipsItem();
                if(itemStack == null) {
                    itemStack = Items.OAK_BOAT.getDefaultInstance();
                }
            }
            else
                itemStack = new BlockItem(Blocks.WATER, new Item.Properties()).getDefaultInstance();

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
        PATROLLING((byte) 1), //traveling from first to last or cycle
        PAUSED((byte) 2),
        STOPPED((byte) 3),
        WAITING((byte) 4),
        ATTACKING((byte) 5), //traveling is paused attacking enemies
        RETREATING((byte) 6), //traveling back to first waypoint from current one
        UPKEEP((byte) 7);
        private final byte index;
        State(byte index){
            this.index = index;
        }

        public byte getIndex(){
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


    //FOLLOW
    //0 = wander
    //1 = follow
    //2 = hold position
    //3 = back to position
    //4 = hold my position
    //5 = Protect


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
            if(recruit.isAlive() && !recruit.getUUID().equals(this.getUUID()) && RECRUITS_IN_COMMAND.contains(recruit.getUUID()) && recruit.getProtectUUID() != null && recruit.getProtectUUID().equals(this.getUUID())){
                recruits.add(recruit);
            }
        }

        return recruits;
    }

    public void setRecruitsToListen(){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.setListen(true);
        }
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

    public void setTypedRecruitsSetAndHoldPos(Vec3 target, Vec3 linePos, EntityType<?> type){
        List<AbstractRecruitEntity> typedList = currentRecruitsInCommand.stream().filter(recruit ->  recruit.getType().equals(type)).toList();


        for (AbstractRecruitEntity recruit : typedList){
            Vec3 pos = FormationUtils.calculateLineBlockPosition(target, linePos, typedList.size(), typedList.indexOf(recruit), this.getCommandSenderWorld());

            recruit.setHoldPos(pos);//set pos
            recruit.setFollowState(3);//back to pos
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

    public void setRecruitsToMoveAndHold(Vec3 target, Vec3 linePos){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){

            recruit.reachedMovePos = false;
            Vec3 pos = FormationUtils.calculateLineBlockPosition(target, linePos, this.currentRecruitsInCommand.size(), currentRecruitsInCommand.indexOf(recruit), this.getCommandSenderWorld());
            //recruit.setMovePos();
            recruit.setFollowState(0);// needs to be above setShouldMovePos
            recruit.setShouldMovePos(true);
        }
    }

    public void setTypedRecruitsToMoveAndHold(Vec3 target, Vec3 linePos, EntityType<?> type){
        List<AbstractRecruitEntity> typedList = currentRecruitsInCommand.stream().filter(recruit ->  recruit.getType().equals(type)).toList();


        for (AbstractRecruitEntity recruit : typedList){

            recruit.reachedMovePos = false;

            Vec3 pos = FormationUtils.calculateLineBlockPosition(target, linePos, typedList.size(), typedList.indexOf(recruit), this.getCommandSenderWorld());
            //recruit.setMovePos(pos);
            recruit.setFollowState(0);// needs to be above setShouldMovePos
            recruit.setShouldMovePos(true);
        }
    }

    public void setRecruitStrategicFirePos(boolean should, BlockPos pos) {
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            if(recruit instanceof IStrategicFire strategicFire){
                strategicFire.setShouldStrategicFire(should);
                strategicFire.setStrategicFirePos(pos);
            }
        }
    }

    public void setRecruitsUpkeep(){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.clearUpkeepEntity();
            recruit.clearUpkeepPos();

            if(this.getUpkeepPos() != null)
                recruit.setUpkeepPos(this.getUpkeepPos());
            recruit.setUpkeepUUID(Optional.ofNullable(this.getUpkeepUUID()));

            recruit.setUpkeepTimer(0);
            recruit.setTarget(null);
            recruit.forcedUpkeep = true;
        }
    }

    public void clearRecruitsUpkeep(){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.clearUpkeepEntity();
            recruit.clearUpkeepPos();
            recruit.setUpkeepTimer(0);
        }
    }

    public void setRecruitsMount(){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            if(this.getMountUUID() != null) recruit.shouldMount(true, this.getMountUUID());
            recruit.dismount = 0;
        }
    }

    public void setRecruitsDismount(){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.shouldMount(false, null);
            if(recruit.isPassenger()){
                recruit.stopRiding();
                recruit.dismount = 180;
            }
        }
    }

    public void setRecruitsAggroState(int x){
        for (AbstractRecruitEntity recruit : currentRecruitsInCommand){
            recruit.setState(x);
        }
    }

    @Override
    public void die(DamageSource dmg) {
        super.die(dmg);
        if(!currentRecruitsInCommand.isEmpty()){
            setRecruitsWanderFreely();
            setRecruitsToListen();
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

    public void openSpecialGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return AbstractLeaderEntity.this.getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new PatrolLeaderContainer(i, playerEntity,  AbstractLeaderEntity.this);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(this.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenSpecialScreen(player, this.getUUID()));
        }

        if (player instanceof ServerPlayer) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateLeaderScreen(this.WAYPOINTS, this.WAYPOINT_ITEMS, this.getRecruitsInCommand().size()));
        }
    }


    public enum CombatTactics{
        AUTO((byte) 0),
        FOLLOW((byte) 1),
        HOLD((byte) 2),
        CHARGE((byte) 3);
        private final byte index;
        CombatTactics(byte index){
            this.index = index;
        }

        public byte getIndex(){
            return this.index;
        }
        public static CombatTactics fromIndex(byte index) {
            for (CombatTactics state : CombatTactics.values()) {
                if (state.getIndex() == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid InfoMode index: " + index);
        }
    }
}










