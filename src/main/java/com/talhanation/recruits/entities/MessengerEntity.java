package com.talhanation.recruits.entities;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.inventory.MessengerAnswerContainer;
import com.talhanation.recruits.inventory.MessengerContainer;
import com.talhanation.recruits.network.MessageOpenMessengerAnswerScreen;
import com.talhanation.recruits.network.MessageOpenSpecialScreen;
import com.talhanation.recruits.network.MessageToClientUpdateMessengerAnswerScreen;
import com.talhanation.recruits.network.MessageToClientUpdateMessengerScreen;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Predicate;

public class MessengerEntity extends AbstractChunkLoaderEntity implements ICompanion {

    private static final EntityDataAccessor<String> TARGET_PLAYER_NAME = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Byte> TASK_STATE = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> WAITING_TIME = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.INT);
    private String ownerName = "";
    private String message = "";
    public int teleportWaitTimer;
    private int arrivedWaitTimer;
    public State state;
    public boolean targetPlayerOpened;
    public BlockPos initialPos;

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public MessengerEntity(EntityType<? extends AbstractChunkLoaderEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_PLAYER_NAME, "");
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(TASK_STATE, (byte) 0);
        this.entityData.define(WAITING_TIME, 0);
    }

    @Override
    protected void registerGoals() {
       super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("Message", this.getMessage());
        nbt.putString("TargetPlayerName", this.getTargetPlayerName());
        nbt.putString("OwnerName", this.getOwnerName());
        nbt.putInt("waitTimer", teleportWaitTimer);
        nbt.putInt("arrivedWaitTimer", arrivedWaitTimer);
        nbt.putInt("waitingTime", this.getWaitingTime());
        if(state != null) nbt.putInt("state", state.getIndex());

        if(this.initialPos != null){
            nbt.putInt("initialPosX", this.initialPos.getX());
            nbt.putInt("initialPosY", this.initialPos.getY());
            nbt.putInt("initialPosZ", this.initialPos.getZ());
        }
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setTargetPlayerName(nbt.getString("TargetPlayerName"));
        this.setMessage(nbt.getString("Message"));
        this.setOwnerName(nbt.getString("OwnerName"));
        this.setWaitingTime(nbt.getInt("waitingTime"));
        this.teleportWaitTimer = nbt.getInt("waitTimer");
        this.arrivedWaitTimer = nbt.getInt("arrivedWaitTimer");
        if(nbt.contains("state")){
            this.state = State.fromIndex(nbt.getInt("state"));
        }
        if(state == null) state = State.IDLE;

        if (nbt.contains("initialPosX")) {
            this.initialPos = new BlockPos(
                    nbt.getInt("initialPosX"),
                    nbt.getInt("initialPosY"),
                    nbt.getInt("initialPosZ"));
        }
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(ForgeMod.ATTACK_RANGE.get(), 0D)
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

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {//TODO: add ranged combat
        if((itemStack.getItem() instanceof SwordItem && this.getMainHandItem().isEmpty()) ||
          (itemStack.getItem() instanceof ShieldItem) && this.getOffhandItem().isEmpty())
            return !hasSameTypeOfItem(itemStack);

        else return super.wantsToPickUp(itemStack);
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
    @Nullable
    public ServerPlayer getTargetPlayer(){
        if(this.getTargetPlayerName() != null && !this.getCommandSenderWorld().isClientSide()){
            ServerLevel serverLevel = (ServerLevel) this.getCommandSenderWorld();
            return serverLevel.getServer().getPlayerList().getPlayerByName(this.getTargetPlayerName());
        }
        return null;
    }

    public void openSpecialGUI(Player player) {
        if (player instanceof ServerPlayer) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateMessengerScreen(this.message));
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return MessengerEntity.this.getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new MessengerContainer(i, playerEntity,  MessengerEntity.this);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(this.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenSpecialScreen(player, this.getUUID()));
        }
    }

    public void openAnswerGUI(Player player) {
        if (player instanceof ServerPlayer) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateMessengerAnswerScreen(this.message));
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return MessengerEntity.this.getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new MessengerAnswerContainer(i, playerEntity,  MessengerEntity.this);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(this.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenMessengerAnswerScreen(player, this.getUUID()));
        }
    }

    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        //if(this.state != State.IDLE && !player.isCrouching()){ //For debug
        if(this.getTargetPlayer() != null && this.getTargetPlayer().getUUID().equals(player.getUUID()) && !this.getTargetPlayer().getUUID().equals(getOwnerUUID())){
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

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetPlayerName() {
        return this.entityData.get(TARGET_PLAYER_NAME);
    }

    public void setTargetPlayerName(String name) {
        this.entityData.set(TARGET_PLAYER_NAME, name);
    }

    public void setWaitingTime(int x){
        this.entityData.set(WAITING_TIME, x);
    }

    public int getWaitingTime(){
        return entityData.get(WAITING_TIME);
    }

    public void start(){
        if(!this.getCommandSenderWorld().isClientSide()){
            this.initialPos = this.getOnPos();
            ServerLevel serverLevel = (ServerLevel) getCommandSenderWorld();

            MinecraftServer server = serverLevel.getServer();
            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(this.getTargetPlayerName());
            if(this.getOwner() != null){
                if(targetPlayer == null || targetPlayer.equals(this.getOwner())){
                    this.getOwner().sendSystemMessage(PLAYER_NOT_FOUND());
                    return;
                }
                else
                    this.getOwner().sendSystemMessage(MESSENGER_INFO_ON_MY_WAY());
            }

            this.setListen(false);
            this.setState(3);//PASSIVE
            this.teleportWaitTimer = 200;
            this.state = State.TELEPORT;
        }
    }
    @Override
    public void tick() {
        super.tick();

        if(state != null){
            switch (state){
                case IDLE -> {

                }

                case TELEPORT -> {
                    //TELEPORT WITH HORSE
                    if(--teleportWaitTimer <= 0){
                        this.teleportNearTargetPlayer(getTargetPlayer());
                        this.arriveAtTargetPlayer(getTargetPlayer());
                        this.playHornSound();
                        this.setFollowState(0);
                        this.state = State.MOVING_TO_TARGET_PLAYER;
                    }
                }

                case MOVING_TO_TARGET_PLAYER -> {
                    Player targetPlayer = getTargetPlayer();
                    if(targetPlayer != null){
                        if(this.tickCount % 20 == 0) {
                            this.getNavigation().moveTo(targetPlayer, 1);
                        }

                        double distance = this.distanceToSqr(targetPlayer);
                        if(distance <= 100){
                            if(this.getOwner() != null) this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_AT_TARGET_OWNER());
                            if(!this.getMainHandItem().isEmpty()) targetPlayer.sendSystemMessage(MESSENGER_INFO_AT_TARGET_WITH_ITEM());
                            else targetPlayer.sendSystemMessage(MESSENGER_INFO_AT_TARGET());

                            this.setFollowState(2);
                            this.arrivedWaitTimer = 1500;
                            this.targetPlayerOpened = false;
                            this.state = State.ARRIVED;
                        }
                    }
                    else {
                        if(this.getOwner() != null) this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_NO_TARGET_PLAYER());
                        teleportWaitTimer = 100;
                        this.state = State.TELEPORT_BACK;
                    }
                }

                case ARRIVED -> {
                    if(--arrivedWaitTimer < 0){
                        if(this.getOwner() != null) this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_NO_TARGET_PLAYER());
                        teleportWaitTimer = 0;
                        state = State.TELEPORT_BACK;
                    }
                    if(targetPlayerOpened){
                        state = State.WAITING;
                        setWaitingTime(5 * 60 * 20);
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
                        state = State.TELEPORT_BACK;
                    }

                }

                case TELEPORT_BACK -> {
                    if(--teleportWaitTimer <= 0){
                        this.teleportNearOwner();
                        this.state = State.MOVING_TO_OWNER;
                    }
                }

                case MOVING_TO_OWNER -> {
                    this.setListen(true);
                    this.state = State.IDLE;
                }
            }
        }
    }

    public void dropDeliverItem(){
        ItemStack deliverItem = this.getMainHandItem();
        if(!deliverItem.isEmpty()){
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.getInventory().setChanged();

            ItemEntity itementity = new ItemEntity(this.getCommandSenderWorld(), this.getX() + this.getLookAngle().x, this.getY() + 2.0D, this.getZ() + this.getLookAngle().z, deliverItem);
            this.getInventory().setChanged();
            this.getCommandSenderWorld().addFreshEntity(itementity);
        }
    }
    private void teleportNearOwner() {
        if(getOwner() != null && !this.getCommandSenderWorld().isClientSide()){
            BlockPos targetPos = getOwner().getOnPos();
            BlockPos tpPos = RecruitsPatrolSpawn.func_221244_a(targetPos, 20, new Random(), (ServerLevel) this.getCommandSenderWorld());
            if(tpPos == null) tpPos = targetPos;

            if(this.getVehicle() instanceof AbstractHorse horse) horse.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
            else this.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());

            this.setFollowState(1);
        }
        else {
            BlockPos tpPos = RecruitsPatrolSpawn.func_221244_a(initialPos, 20, new Random(), (ServerLevel) this.getCommandSenderWorld());
            if(tpPos == null) tpPos = initialPos;

            if(this.getVehicle() instanceof AbstractHorse horse) horse.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
            else this.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());

            this.setHoldPos(Vec3.atCenterOf(initialPos));
            this.setFollowState(3);
        }
    }
    private void teleportNearTargetPlayer(Player player) {
        if(player != null && !this.getCommandSenderWorld().isClientSide()){
            BlockPos targetPos = player.getOnPos();
            BlockPos tpPos = RecruitsPatrolSpawn.func_221244_a(targetPos, 60, new Random(), (ServerLevel) this.getCommandSenderWorld());
            if(tpPos == null) tpPos = targetPos;

            if(this.getVehicle() instanceof AbstractHorse horse) horse.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
            else this.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
        }
    }

    private void playHornSound() {
        this.getCommandSenderWorld().playSound(null, this, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(1), SoundSource.NEUTRAL, 128F, 1.0F);
        this.getCommandSenderWorld().gameEvent(GameEvent.INSTRUMENT_PLAY, this.position(), GameEvent.Context.of(this));
    }

    public void arriveAtTargetPlayer(ServerPlayer target){
        tellTargetPlayerArrived(target);
    }

    public void tellTargetPlayerArrived(ServerPlayer target){
        if(target != null){
            Team ownerTeam = this.getTeam();
            if(ownerTeam != null )target.sendSystemMessage(MESSENGER_ARRIVED_TEAM(this.getOwnerName(), ownerTeam.getName()));
            else target.sendSystemMessage(MESSENGER_ARRIVED(this.getOwnerName()));
        }
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
        return Component.translatable("chat.recruits.text.messenger_arrived_at_target_owner", this.getName().getString(), this.getTargetPlayerName());
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
        return Component.translatable("chat.recruits.text.messenger_arrived_no_player", this.getName().getString(), this.getTargetPlayerName());
    }

    private MutableComponent MESSENGER_ARRIVED_TARGET_PLAYER_NOT_ANSWERED(){
        return Component.translatable("chat.recruits.text.messenger_target_player_not_answered", this.getName().getString());
    }

    public enum State{
        IDLE(0),
        TELEPORT(1),
        MOVING_TO_TARGET_PLAYER(2),
        ARRIVED(3),
        WAITING(4),
        TELEPORT_BACK(5),
        MOVING_TO_OWNER(6);


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
    public boolean hurt(@NotNull DamageSource dmg, float amt) {
        if(this.state == null || this.state == State.IDLE){
            return super.hurt(dmg, amt);
        }
        else return false;
    }
}










