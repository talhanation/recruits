package com.talhanation.recruits.entities;


import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.network.MessageToClientOpenTreatyAnswerScreen;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.TravelPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Carries a message, an item or a treaty to another player and comes back.
 * <p>
 * Both ways are real journeys ({@link AbstractTravelEntity}): he walks or rides off, travels as a simulation
 * while no player can see him and walks in from afar at the other end. The time depends on the distance.
 * <ul>
 *     <li>{@code TELEPORT}: on the way to the target player (the name is kept for old saves).</li>
 *     <li>{@code MOVING_TO_TARGET_PLAYER}, {@code ARRIVED}, {@code WAITING}: at the target player.</li>
 *     <li>{@code TELEPORT_BACK}: on the way back to the owner, or to where he was sent from if the owner is gone.</li>
 *     <li>{@code MOVING_TO_OWNER}: the last steps to the owner.</li>
 * </ul>
 */
public class MessengerEntity extends AbstractChunkLoaderEntity implements ICompanion {
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> MESSENGER_STATE = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAITING_TIME = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> TARGETPLAYER = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<String> MESSAGE = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> TREATY_DURATION_HOURS = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_TREATY_MESSENGER = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.BOOLEAN);

    private String ownerName = "";
    /** Ticks until he sets off (to the target in TELEPORT, back home in TELEPORT_BACK). */
    public int teleportWaitTimer;
    private int arrivedWaitTimer;
    public boolean targetPlayerOpened;
    public BlockPos initialPos;
    public boolean treatyNotAccepted;
    /** Further away than this (64 blocks) the last steps to a player become a journey again. */
    private static final double LOST_SIGHT_SQR = 64.0D * 64.0D;
    /** Carrying orders to this companion of the owner instead of a message to a player, see {@link #startOrders}. */
    @Nullable
    private UUID orderCompanion;
    private String orderCompanionName = "";
    private ListTag orders = new ListTag();

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public MessengerEntity(EntityType<? extends AbstractChunkLoaderEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(MESSENGER_STATE, 0);
        this.entityData.define(WAITING_TIME, 0);
        this.entityData.define(TARGETPLAYER, new CompoundTag());
        this.entityData.define(MESSAGE, "");
        this.entityData.define(TREATY_DURATION_HOURS, 0);
        this.entityData.define(IS_TREATY_MESSENGER, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("Message", this.getMessage());
        nbt.putInt("TreatyDurationHours", this.getTreatyDurationHours());
        nbt.putBoolean("IsTreatyMessenger", this.isTreatyMessenger());
        nbt.putBoolean("targetPlayerOpened", targetPlayerOpened);
        nbt.putBoolean("treatyNotAccepted", treatyNotAccepted);

        if(this.getTargetPlayerInfo() != null) nbt.put("TargetPlayerInfo", this.getTargetPlayerInfo().toNBT());


        nbt.putString("OwnerName", this.getOwnerName());
        nbt.putInt("waitTimer", teleportWaitTimer);
        nbt.putInt("arrivedWaitTimer", arrivedWaitTimer);
        nbt.putInt("waitingTime", this.getWaitingTime());
        if(getMessengerState() != null) nbt.putInt("state", getMessengerState().getIndex());

        if(this.initialPos != null){
            nbt.putInt("initialPosX", this.initialPos.getX());
            nbt.putInt("initialPosY", this.initialPos.getY());
            nbt.putInt("initialPosZ", this.initialPos.getZ());
        }

        if(this.orderCompanion != null){
            nbt.putUUID("OrderCompanion", this.orderCompanion);
            nbt.putString("OrderCompanionName", this.orderCompanionName);
            nbt.put("Orders", this.orders);
        }
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if(nbt.contains("TargetPlayerInfo")){
            this.setTargetPlayerInfo(RecruitsPlayerInfo.getFromNBT(nbt.getCompound("TargetPlayerInfo")));
        }

        this.setMessage(nbt.getString("Message"));
        this.setTreatyDurationHours(nbt.getInt("TreatyDurationHours"));
        this.setIsTreatyMessenger(nbt.getBoolean("IsTreatyMessenger"));
        this.setOwnerName(nbt.getString("OwnerName"));
        this.setWaitingTime(nbt.getInt("waitingTime"));
        this.teleportWaitTimer = nbt.getInt("waitTimer");
        this.arrivedWaitTimer = nbt.getInt("arrivedWaitTimer");
        this.treatyNotAccepted = nbt.getBoolean("treatyNotAccepted");
        this.targetPlayerOpened = nbt.getBoolean("targetPlayerOpened");
        if(nbt.contains("state")){
            this.setMessengerState(MessengerState.fromIndex(nbt.getInt("state")));
        }

        if (nbt.contains("initialPosX")) {
            this.initialPos = new BlockPos(
                    nbt.getInt("initialPosX"),
                    nbt.getInt("initialPosY"),
                    nbt.getInt("initialPosZ"));
        }

        if (nbt.hasUUID("OrderCompanion")) {
            this.orderCompanion = nbt.getUUID("OrderCompanion");
            this.orderCompanionName = nbt.getString("OrderCompanionName");
            this.orders = nbt.getList("Orders", 10);
        }
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(ForgeMod.SWIM_SPEED.get(), 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(ForgeMod.ENTITY_REACH.get(), 0D)
                .add(Attributes.ATTACK_SPEED);
    }


    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((AsyncGroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        if(this.getCustomName() == null || this.getCustomName().getString().isEmpty())  this.setCustomName(Component.literal("Messenger"));
        this.setPersistenceRequired();
        if(this.getOwner() != null)this.setOwnerName(this.getOwner().getName().getString());
        AbstractRecruitEntity.applySpawnValues(this);


    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        if((itemStack.getItem() instanceof SwordItem && this.getMatchingItem(item -> item.getItem() instanceof SwordItem) == null) ||
                (itemStack.getItem() instanceof BowItem && this.getMatchingItem(item -> item.getItem() instanceof BowItem) == null) ||
                (itemStack.getItem() instanceof ShieldItem) && this.getMatchingItem(item -> item.getItem() instanceof ShieldItem) == null)
            return true;

        else return super.wantsToPickUp(itemStack);
    }

    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack){
        return !(itemStack.getItem() instanceof CrossbowItem);
    }

    @Override
    public AbstractRecruitEntity get() {
        return this;
    }
    @Nullable
    public ServerPlayer getTargetPlayer(){
        if(this.getTargetPlayerInfo() != null && !this.getCommandSenderWorld().isClientSide()){
            ServerLevel serverLevel = (ServerLevel) this.getCommandSenderWorld();
            return serverLevel.getServer().getPlayerList().getPlayer(this.getTargetPlayerInfo().getUUID());
        }
        return null;
    }

    public void openSpecialGUI(Player player) {
        //NOT USED NO CONTAINER NEEDED
    }

    public void openAnswerGUI(Player player) {
        if(this.level().isClientSide())return;
        if(player instanceof ServerPlayer serverPlayer && getTargetPlayerInfo() != null){
            if (this.isTreatyMessenger()) {
                String team = this.getTeam() != null ? this.getTeam().getName() : "";
                RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(team);
                RecruitsPlayerInfo ownerPlayerInfo = new RecruitsPlayerInfo(this.getOwnerUUID(), this.getOwnerName(), faction);

                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new MessageToClientOpenTreatyAnswerScreen(MessengerEntity.this, this.getTreatyDurationHours(), ownerPlayerInfo));
            } else {
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new MessageToClientOpenMessengerAnswerScreen(MessengerEntity.this, this.getMessage(), this.getTargetPlayerInfo()));
            }
            this.targetPlayerOpened = true;
        }
    }

    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        //if(!player.isCrouching()){ //For debug
        MessengerState state = getMessengerState();
        if(this.getTargetPlayer() != null
                && this.getTargetPlayer().getUUID().equals(player.getUUID())
                && !this.getTargetPlayer().getUUID().equals(getOwnerUUID())
                && (state == MessengerState.ARRIVED || state == MessengerState.WAITING || state == MessengerState.MOVING_TO_TARGET_PLAYER))
        {
            openAnswerGUI(player);
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    public String getOwnerName() {
        return entityData.get(OWNER_NAME);
    }

    public void setOwnerName(String name) {
        entityData.set(OWNER_NAME, name);
    }

    @Override
    public boolean isAtMission() {
        return this.getMessengerState() != MessengerState.IDLE;
    }

    public String getMessage() {
        return this.entityData.get(MESSAGE);
    }

    public void setMessage(String message) {
        this.entityData.set(MESSAGE, message);
    }

    public int getTreatyDurationHours() {
        return this.entityData.get(TREATY_DURATION_HOURS);
    }

    public void setTreatyDurationHours(int hours) {
        this.entityData.set(TREATY_DURATION_HOURS, hours);
    }

    public boolean isTreatyMessenger() {
        return this.entityData.get(IS_TREATY_MESSENGER);
    }

    public void setIsTreatyMessenger(boolean isTreaty) {
        this.entityData.set(IS_TREATY_MESSENGER, isTreaty);
    }

    public RecruitsPlayerInfo getTargetPlayerInfo() {
        return RecruitsPlayerInfo.getFromNBT(this.entityData.get(TARGETPLAYER));
    }

    public void setTargetPlayerInfo(RecruitsPlayerInfo info) {
        this.entityData.set(TARGETPLAYER, info.toNBT());
    }

    public void setWaitingTime(int x){
        this.entityData.set(WAITING_TIME, x);
    }

    public int getWaitingTime(){
        return entityData.get(WAITING_TIME);
    }

    public void setMessengerState(MessengerState state){
        this.entityData.set(MESSENGER_STATE, state.getIndex());
    }

    public MessengerState getMessengerState(){
        return MessengerState.fromIndex(entityData.get(MESSENGER_STATE));
    }

    public void start(){
        if(!this.getCommandSenderWorld().isClientSide()){
            this.initialPos = this.getOnPos();
            ServerLevel serverLevel = (ServerLevel) getCommandSenderWorld();

            MinecraftServer server = serverLevel.getServer();
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(this.getTargetPlayerInfo().getUUID());
            // a journey stays in its dimension
            boolean reachable = targetPlayer != null && targetPlayer.level() == this.level() && !targetPlayer.equals(this.getOwner());
            if(this.getOwner() != null){
                if(!reachable){
                    this.getOwner().sendSystemMessage(PLAYER_NOT_FOUND());
                    return;
                }
                else
                    this.getOwner().sendSystemMessage(MESSENGER_INFO_ON_MY_WAY());
            }
            else if(!reachable) return;

            this.setListen(false);
            this.setAggroState(3);//PASSIVE
            this.teleportWaitTimer = 40;
            this.setMessengerState(MessengerState.TELEPORT);
        }
    }

    /**
     * Sets off to one of the owner's companions with the orders the owner wrote down for him
     * ({@link CompanionOrders}). He travels to wherever the companion is, hands the orders over and comes back.
     * Unlike with a message he can be hurt and killed on this errand - then the orders are lost.
     */
    public void startOrders(UUID companion, String companionName, ListTag orders){
        if(this.getCommandSenderWorld().isClientSide()) return;

        this.initialPos = this.getOnPos();
        this.orderCompanion = companion;
        this.orderCompanionName = companionName;
        this.orders = orders;
        if(this.getOwner() != null) this.getOwner().sendSystemMessage(MESSENGER_ORDERS_ON_MY_WAY());

        this.setListen(false);
        this.setAggroState(3);//PASSIVE
        this.teleportWaitTimer = 40;
        this.setMessengerState(MessengerState.TELEPORT);
    }

    /** On the way to a companion with orders (and not back yet from handing them over). */
    public boolean carriesOrders(){
        return this.orderCompanion != null;
    }

    /** At the companion: hands the orders over and tells the owner. */
    private void deliverOrders(){
        boolean delivered = this.orderCompanion != null && this.getOwnerUUID() != null
                && this.level() instanceof ServerLevel serverLevel
                && CompanionOrders.deliver(serverLevel, this.getOwnerUUID(), this.orderCompanion, this.orders);

        if(this.getOwner() != null) this.getOwner().sendSystemMessage(delivered ? MESSENGER_ORDERS_DELIVERED() : MESSENGER_ORDERS_NOT_FOUND());
        if(delivered && this.isAddedToWorld()) this.playHornSound();
        this.clearOrders();
    }

    private void clearOrders(){
        this.orderCompanion = null;
        this.orderCompanionName = "";
        this.orders = new ListTag();
    }

    @Override
    public void tick() {
        super.tick();
        // removed: he was just taken out of the world for the simulated part of his journey
        if(this.getCommandSenderWorld().isClientSide() || this.isRemoved()) return;

        MessengerState state = getMessengerState();
        if(state != null){
            switch (state){
                case IDLE -> {

                }

                case TELEPORT -> {
                    // on the way to the target player; the arrival is onTravelArrived()
                    if(teleportWaitTimer > 0) teleportWaitTimer--;
                    else if(!this.isTravelling()){
                        if(this.orderCompanion != null){
                            // with orders to a companion; should he be gone, the plan leads back to where he was sent from
                            this.startTravel(TravelPlan.toCompanion(this.orderCompanion, this.getHomePos()));
                        }
                        else {
                            ServerPlayer targetPlayer = getTargetPlayer();
                            if(targetPlayer == null || targetPlayer.level() != this.level()) this.returnWithoutDelivery();
                            else this.startTravel(TravelPlan.toPlayer(targetPlayer.getUUID(), this.getHomePos()));
                        }
                    }
                }

                case MOVING_TO_TARGET_PLAYER -> {
                    Player targetPlayer = getTargetPlayer();
                    if(targetPlayer != null && targetPlayer.level() == this.level()){
                        if(this.tickCount % 20 == 0) {
                            this.getNavigation().moveTo(targetPlayer, 1);
                        }

                        double distance = this.distanceToSqr(targetPlayer);
                        if(distance > LOST_SIGHT_SQR){
                            // he rode or flew off: back on the road after him
                            this.teleportWaitTimer = 0;
                            this.setMessengerState(MessengerState.TELEPORT);
                        }
                        else if(distance <= 60){
                            if(this.getOwner() != null) this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_AT_TARGET_OWNER());

                            if(this.isTreatyMessenger()){
                                targetPlayer.sendSystemMessage(MESSENGER_INFO_AT_TARGET_TREATY());
                            }
                            else{
                                if(!this.getMainHandItem().isEmpty()) targetPlayer.sendSystemMessage(MESSENGER_INFO_AT_TARGET_WITH_ITEM());
                                else targetPlayer.sendSystemMessage(MESSENGER_INFO_AT_TARGET());
                            }

                            this.setFollowState(2);
                            this.arrivedWaitTimer = 1500;
                            this.targetPlayerOpened = false;
                            this.setMessengerState(MessengerState.ARRIVED);
                        }
                    }
                    else {
                        if(this.getOwner() != null) this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_NO_TARGET_PLAYER());
                        teleportWaitTimer = 100;
                        this.setMessengerState(MessengerState.TELEPORT_BACK);
                    }
                }

                case ARRIVED -> {
                    if(--arrivedWaitTimer < 0){
                        if(this.getOwner() != null) this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_NO_TARGET_PLAYER());
                        teleportWaitTimer = 0;
                        this.setMessengerState(MessengerState.TELEPORT_BACK);
                    }
                    if(targetPlayerOpened){
                        setWaitingTime(5 * 60 * 20);
                        this.setMessengerState(MessengerState.WAITING);
                    }
                }

                case WAITING ->{

                    if(this.tickCount % 20 == 0) {
                        this.getNavigation().stop();
                        if(getTargetPlayer() != null) this.getLookControl().setLookAt(getTargetPlayer());
                    }

                    int time = getWaitingTime();
                    if(time > 0){
                        time--;
                        setWaitingTime(time);
                    }
                    else{
                        if(this.getOwner() != null) {
                            if(this.targetPlayerOpened) this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_TARGET_PLAYER_NOT_ANSWERED());
                            else this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_NO_TARGET_PLAYER());
                        }
                        teleportWaitTimer = 100;
                        this.setMessengerState(MessengerState.TELEPORT_BACK);
                    }

                }

                case TELEPORT_BACK -> {
                    // on the way back: to the owner, or to where he was sent from if the owner is gone
                    if(teleportWaitTimer > 0) teleportWaitTimer--;
                    else if(!this.isTravelling()) this.startJourneyHome();
                }

                case MOVING_TO_OWNER -> {
                    Player owner = this.getOwner();
                    if(owner == null || this.distanceToSqr(owner) > LOST_SIGHT_SQR){
                        // the owner left again before the messenger reached him
                        this.teleportWaitTimer = 0;
                        this.setMessengerState(MessengerState.TELEPORT_BACK);
                    }
                    else if(this.distanceToSqr(owner) < 50F) {
                        this.setListen(true);
                        this.setMessengerState(MessengerState.IDLE);

                        if(isTreatyMessenger() && treatyNotAccepted){
                            treatyNotAccepted = false;
                            owner.sendSystemMessage(MESSENGER_ARRIVED_TARGET_PLAYER_NOT_ACCEPTED());
                        }
                    }
                }
            }
        }
    }

    public void giveDeliverItem(ServerPlayer player){
        ItemStack deliverItem = this.getMainHandItem();
        if(!deliverItem.isEmpty()){

            if(player.getInventory().hasAnyMatching(ItemStack::isEmpty)){
                player.getInventory().add(deliverItem.copy());
            }
            else{
                ItemEntity itementity = new ItemEntity(this.getCommandSenderWorld(), this.getX() + this.getLookAngle().x, this.getY() + 2.0D, this.getZ() + this.getLookAngle().z, deliverItem);
                this.getInventory().setChanged();
                this.getCommandSenderWorld().addFreshEntity(itementity);
            }

            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.getInventory().setChanged();
        }
    }
    /** Where he was sent from; he waits there when the owner is gone. */
    private BlockPos getHomePos() {
        return this.initialPos != null ? this.initialPos : this.getOnPos();
    }

    /** The target player is not there (any more): tell the owner and turn back. */
    private void returnWithoutDelivery() {
        if(this.getOwner() != null) this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_NO_TARGET_PLAYER());
        this.stopTravel();
        this.teleportWaitTimer = 0;
        this.setMessengerState(MessengerState.TELEPORT_BACK);
    }

    /** Sets off for home right now: to the owner, or to where he was sent from if the owner is gone. */
    private void startJourneyHome() {
        this.teleportWaitTimer = 0;
        this.setMessengerState(MessengerState.TELEPORT_BACK);
        if(this.getOwnerUUID() != null) this.startTravel(TravelPlan.toPlayer(this.getOwnerUUID(), this.getHomePos()));
        else this.startTravel(TravelPlan.toPoint(this.getHomePos()));
    }

    /** Sent out and not back yet. */
    @Override
    public boolean isOnMission() {
        return this.getMessengerState() != MessengerState.IDLE;
    }

    /**
     * His chunk is about to be unloaded while he is not on the road: the player he stood with logged out or went far
     * away. Instead of being frozen in the chunk he sets off - on to the target if he had not left yet, otherwise home.
     */
    @Override
    public void onTravelStranded() {
        MessengerState state = this.getMessengerState();
        switch (state) {
            case TELEPORT -> {
                ServerPlayer targetPlayer = this.getTargetPlayer();
                if(this.orderCompanion != null){
                    this.teleportWaitTimer = 0;
                    this.startTravel(TravelPlan.toCompanion(this.orderCompanion, this.getHomePos()));
                }
                else if(targetPlayer != null && targetPlayer.level() == this.level()){
                    this.teleportWaitTimer = 0;
                    this.startTravel(TravelPlan.toPlayer(targetPlayer.getUUID(), this.getHomePos()));
                }
                else{
                    this.returnWithoutDelivery();
                    this.startJourneyHome();
                }
            }
            case MOVING_TO_TARGET_PLAYER, ARRIVED, WAITING -> {
                if(this.getOwner() != null){
                    if(this.targetPlayerOpened) this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_TARGET_PLAYER_NOT_ANSWERED());
                    else this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_NO_TARGET_PLAYER());
                }
                this.startJourneyHome();
            }
            case TELEPORT_BACK, MOVING_TO_OWNER -> this.startJourneyHome();
            default -> {
            }
        }
    }

    @Override
    protected void onTravelArrived(TravelPlan plan) {
        MessengerState state = this.getMessengerState();
        if(state == MessengerState.TELEPORT && this.orderCompanion != null){
            if(plan.targetLost){
                // the companion is gone; the plan led him back to where he was sent from
                if(this.getOwner() != null) this.getOwner().sendSystemMessage(MESSENGER_ORDERS_NOT_FOUND());
                this.clearOrders();
                this.setMessengerState(MessengerState.TELEPORT_BACK);
                this.arriveHome();
            }
            else{
                this.deliverOrders();
                this.teleportWaitTimer = 60;
                this.setMessengerState(MessengerState.TELEPORT_BACK);
            }
        }
        else if(state == MessengerState.TELEPORT){
            ServerPlayer targetPlayer = this.getTargetPlayer();
            if(plan.targetLost || targetPlayer == null || targetPlayer.level() != this.level()){
                this.returnWithoutDelivery();
                return;
            }
            // near the target player: announce himself and walk up to him
            this.arriveAtTargetPlayer(targetPlayer);
            this.setFollowState(0);
            this.setMessengerState(MessengerState.MOVING_TO_TARGET_PLAYER);
        }
        else if(state == MessengerState.TELEPORT_BACK){
            this.arriveHome();
        }
    }

    /** Back from his errand: up to the owner if he is here, otherwise he waits where he was sent from. */
    private void arriveHome() {
        Player owner = this.getOwner();
        if(owner != null && this.distanceToSqr(owner) <= LOST_SIGHT_SQR){
            this.setFollowState(1);
            this.setMessengerState(MessengerState.MOVING_TO_OWNER);
        }
        else{
            // the owner is not there: wait where he was sent from
            this.setHoldPos(Vec3.atCenterOf(this.getHomePos()));
            this.setFollowState(3);
            this.setListen(true);
            this.setMessengerState(MessengerState.IDLE);
        }
    }

    /**
     * He reached the companion while nobody is near: the orders are handed over out of the world and he turns back.
     * This entity is a copy in no world here, see {@link AbstractTravelEntity#onTravelArrivedOutOfWorld}.
     */
    @Override
    @Nullable
    public TravelPlan onTravelArrivedOutOfWorld(TravelPlan plan) {
        if(this.getMessengerState() != MessengerState.TELEPORT || this.orderCompanion == null) return null;

        this.deliverOrders();
        this.teleportWaitTimer = 0;
        this.setMessengerState(MessengerState.TELEPORT_BACK);
        if(this.getOwnerUUID() != null) return TravelPlan.toPlayer(this.getOwnerUUID(), this.getHomePos());
        return TravelPlan.toPoint(this.getHomePos());
    }

    /** A messenger travels, he keeps no chunks loaded. */
    @Override
    public boolean shouldLoadChunk() {
        return false;
    }

    public void playHornSound() {
        this.playSound(SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(1).get(), 20F, 0.8F + 0.4F * this.getCommandSenderWorld().random.nextFloat());
    }

    public void arriveAtTargetPlayer(ServerPlayer target){
        this.tellTargetPlayerArrived(target);
        this.playHornSound();
    }

    public void tellTargetPlayerArrived(ServerPlayer target){
        if(target == null) return;
        if(this.getTeam() == null){
            target.sendSystemMessage(MESSENGER_ARRIVED(this.getOwnerName()));
        }
        else{
            PlayerTeam ownerTeam = this.level().getScoreboard().getPlayerTeam(this.getTeam().getName());
            if(ownerTeam != null) target.sendSystemMessage(MESSENGER_ARRIVED_TEAM(this.getOwnerName(), ownerTeam.getDisplayName().getString()));
        }

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> target), new MessageToClientSetToast(1, this.getOwnerName()));
    }

    private MutableComponent MESSENGER_ORDERS_ON_MY_WAY(){
        return Component.translatable("chat.recruits.text.messenger_orders_on_my_way", this.getName().getString(), this.orderCompanionName);
    }

    private MutableComponent MESSENGER_ORDERS_DELIVERED(){
        return Component.translatable("chat.recruits.text.messenger_orders_delivered", this.getName().getString(), this.orderCompanionName);
    }

    private MutableComponent MESSENGER_ORDERS_NOT_FOUND(){
        return Component.translatable("chat.recruits.text.messenger_orders_not_found", this.getName().getString(), this.orderCompanionName);
    }

    private MutableComponent MESSENGER_ORDERS_LOST(){
        return Component.translatable("chat.recruits.text.messenger_orders_lost", this.getName().getString(), this.orderCompanionName);
    }

    private MutableComponent PLAYER_NOT_FOUND(){
        return Component.translatable("chat.recruits.text.messenger_player_not_found", this.getName().getString());
    }

    private MutableComponent MESSENGER_ARRIVED(String ownerName){
        return Component.translatable("chat.recruits.text.messenger_arrived_at_target", this.getName().getString(), ownerName);
    }

    private MutableComponent MESSENGER_ARRIVED_TEAM(String ownerName, String teamName){
        return Component.translatable("chat.recruits.text.messenger_arrived_at_target_team", this.getName().getString(), ownerName, teamName);
    }

    private MutableComponent MESSENGER_ARRIVED_AT_TARGET_OWNER(){
        return Component.translatable("chat.recruits.text.messenger_arrived_at_target_owner", this.getName().getString(), this.getTargetPlayerInfo().getName());
    }

    private MutableComponent MESSENGER_INFO_AT_TARGET_TREATY(){
        return Component.translatable("chat.recruits.text.messenger_info_to_target_treaty", this.getName().getString(), this.getOwnerName());
    }
    private MutableComponent MESSENGER_INFO_AT_TARGET(){
        return Component.translatable("chat.recruits.text.messenger_info_to_target", this.getName().getString(), this.getOwnerName());
    }
    private MutableComponent MESSENGER_INFO_AT_TARGET_WITH_ITEM(){
        return Component.translatable("chat.recruits.text.messenger_info_to_target_with_item", this.getName().getString(), this.getOwnerName());
    }

    public MutableComponent MESSENGER_INFO_ON_MY_WAY(){
        return Component.translatable("chat.recruits.text.messenger_info_on_my_way", this.getName().getString());
    }

    private MutableComponent MESSENGER_ARRIVED_NO_TARGET_PLAYER(){
        return Component.translatable("chat.recruits.text.messenger_arrived_no_player", this.getName().getString(), this.getTargetPlayerInfo().getName());
    }

    private MutableComponent MESSENGER_ARRIVED_TARGET_PLAYER_NOT_ACCEPTED(){
        return Component.translatable("chat.recruits.text.messenger_target_player_not_accepted", this.getName().getString(), this.getTargetPlayerInfo().getName());
    }

    private MutableComponent MESSENGER_ARRIVED_TARGET_PLAYER_NOT_ANSWERED(){
        return Component.translatable("chat.recruits.text.messenger_target_player_not_answered", this.getName().getString());
    }
    @OnlyIn(Dist.CLIENT)
    public boolean shouldGlow() {
        LocalPlayer clientPlayer = Minecraft.getInstance().player;
        MessengerState messengerState = this.getMessengerState();

        if(getTargetPlayerInfo() != null && clientPlayer != null){
            UUID uuid = getTargetPlayerInfo().getUUID();
            return clientPlayer.getUUID().equals(uuid) && (messengerState == MessengerState.ARRIVED || messengerState == MessengerState.MOVING_TO_TARGET_PLAYER);
        }
        return false;
    }

    public enum MessengerState {
        IDLE(0),
        TELEPORT(1),
        MOVING_TO_TARGET_PLAYER(2),
        ARRIVED(3),
        WAITING(4),
        TELEPORT_BACK(5),
        MOVING_TO_OWNER(6);


        private final int index;
        MessengerState(int index){
            this.index = index;
        }

        public int getIndex(){
            return this.index;
        }

        public static MessengerState fromIndex(int index) {
            for (MessengerState messengerState : MessengerState.values()) {
                if (messengerState.getIndex() == index) {
                    return messengerState;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource dmg, float amt) {
        MessengerState state = getMessengerState();
        // with orders for a companion he is not protected: sending him is a risk
        if(state == null || state == MessengerState.IDLE || this.orderCompanion != null){
            return super.hurt(dmg, amt);
        }
        else return false;
    }

    @Override
    public void die(@NotNull DamageSource dmg) {
        if(!this.level().isClientSide() && this.orderCompanion != null && this.getOwner() != null){
            this.getOwner().sendSystemMessage(MESSENGER_ORDERS_LOST());
        }
        super.die(dmg);
    }
}