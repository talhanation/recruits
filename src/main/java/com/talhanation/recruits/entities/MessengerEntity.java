package com.talhanation.recruits.entities;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

public class MessengerEntity extends AbstractChunkLoaderEntity implements ICompanion {
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> MESSENGER_STATE = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAITING_TIME = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> TARGETPLAYER = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<String> MESSAGE = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.STRING);

    private String ownerName = "";
    public int teleportWaitTimer;
    private int arrivedWaitTimer;
    public boolean targetPlayerOpened;
    public BlockPos initialPos;

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
    }

    @Override
    protected void registerGoals() {
       super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("Message", this.getMessage());


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
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if(nbt.contains("TargetPlayerInfo")){
            this.setTargetPlayerInfo(RecruitsPlayerInfo.getFromNBT(nbt.getCompound("TargetPlayerInfo")));
        }

        this.setMessage(nbt.getString("Message"));
        this.setOwnerName(nbt.getString("OwnerName"));
        this.setWaitingTime(nbt.getInt("waitingTime"));
        this.teleportWaitTimer = nbt.getInt("waitTimer");
        this.arrivedWaitTimer = nbt.getInt("arrivedWaitTimer");
        if(nbt.contains("state")){
            this.setMessengerState(MessengerState.fromIndex(nbt.getInt("state")));
        }

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
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new MessageToClientOpenMessengerAnswerScreen(MessengerEntity.this, this.getMessage(), this.getTargetPlayerInfo()));
        }

    }

    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        //if(!player.isCrouching()){ //For debug
        MessengerState state = getMessengerState();
        if(this.getTargetPlayer() != null
                && this.getTargetPlayer().getUUID().equals(player.getUUID())
                && !this.getTargetPlayer().getUUID().equals(getOwnerUUID())
                && state == MessengerState.ARRIVED || state == MessengerState.WAITING || state == MessengerState.MOVING_TO_TARGET_PLAYER)
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
            this.setMessengerState(MessengerState.TELEPORT);
        }
    }
    @Override
    public void tick() {
        super.tick();
        MessengerState state = getMessengerState();
        if(state != null && !this.getCommandSenderWorld().isClientSide()){
            switch (state){
                case IDLE -> {

                }

                case TELEPORT -> {
                    if(--teleportWaitTimer <= 0){
                        ServerPlayer targetPlayer = getTargetPlayer();
                        this.teleportNearTargetPlayer(targetPlayer);
                        this.arriveAtTargetPlayer(targetPlayer);
                        this.setFollowState(0);

                        this.setMessengerState(MessengerState.MOVING_TO_TARGET_PLAYER);
                    }
                }

                case MOVING_TO_TARGET_PLAYER -> {
                    Player targetPlayer = getTargetPlayer();
                    if(targetPlayer != null){
                        if(this.tickCount % 20 == 0) {
                            this.getNavigation().moveTo(targetPlayer, 1);
                        }

                        double distance = this.distanceToSqr(targetPlayer);
                        if(distance <= 60){
                            if(this.getOwner() != null) this.getOwner().sendSystemMessage(MESSENGER_ARRIVED_AT_TARGET_OWNER());
                            if(!this.getMainHandItem().isEmpty()) targetPlayer.sendSystemMessage(MESSENGER_INFO_AT_TARGET_WITH_ITEM());
                            else targetPlayer.sendSystemMessage(MESSENGER_INFO_AT_TARGET());


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
                        this.setMessengerState(MessengerState.WAITING);
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
                        this.setMessengerState(MessengerState.TELEPORT_BACK);
                    }

                }

                case TELEPORT_BACK -> {
                    if(--teleportWaitTimer <= 0){
                        this.teleportNearOwner();
                        this.setMessengerState(MessengerState.MOVING_TO_OWNER);
                    }
                }

                case MOVING_TO_OWNER -> {
                    if(this.getOwner() != null){
                        if(this.distanceToSqr(this.getOwner()) < 50F) {
                            this.setListen(true);
                            this.setMessengerState(MessengerState.IDLE);
                        }
                    }
                    else{
                        teleportNearOwner();
                        this.setMessengerState(MessengerState.IDLE);
                    }
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
        if(!this.getCommandSenderWorld().isClientSide()){
            if(getOwner() != null ){
                BlockPos targetPos = getOwner().getOnPos();
                BlockPos tpPos = RecruitsPatrolSpawn.func_221244_a(targetPos, 10, new Random(), (ServerLevel) this.getCommandSenderWorld());
                if(tpPos == null) tpPos = targetPos;

                if(this.getVehicle() instanceof AbstractHorse horse) horse.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
                else this.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());

                this.setFollowState(1);
            }
            else {
                BlockPos tpPos = RecruitsPatrolSpawn.func_221244_a(initialPos, 10, new Random(), (ServerLevel) this.getCommandSenderWorld());

                if(tpPos == null) tpPos = initialPos;

                if(this.getVehicle() instanceof AbstractHorse horse) horse.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
                else this.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());

                this.setHoldPos(Vec3.atCenterOf(initialPos));
                this.setFollowState(3);
            }
        }
    }
    private void teleportNearTargetPlayer(Player player) {
        if (!this.getCommandSenderWorld().isClientSide()){
            if (player != null) {
                BlockPos targetPos = player.getOnPos();
                BlockPos tpPos = RecruitsPatrolSpawn.func_221244_a(targetPos, 20, new Random(), (ServerLevel) this.getCommandSenderWorld());

                if (tpPos == null) tpPos = targetPos;

                if (this.getVehicle() instanceof AbstractHorse horse)
                    horse.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
                else this.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
            }
        }
    }

    public void playHornSound() {
        this.playSound(SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(1).get(), 20F, 0.8F + 0.4F * this.random.nextFloat());
    }

    public void arriveAtTargetPlayer(ServerPlayer target){
        this.tellTargetPlayerArrived(target);
        this.playHornSound();
    }

    public void tellTargetPlayerArrived(ServerPlayer target){
        if(target == null) return;
        Team ownerTeam = this.getTeam();
        if(ownerTeam != null )target.sendSystemMessage(MESSENGER_ARRIVED_TEAM(this.getOwnerName(), ownerTeam.getName()));
        else target.sendSystemMessage(MESSENGER_ARRIVED(this.getOwnerName()));
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> target), new MessageToClientSetToast(1, this.getOwnerName()));
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

    private MutableComponent MESSENGER_ARRIVED_TARGET_PLAYER_NOT_ANSWERED(){
        return Component.translatable("chat.recruits.text.messenger_target_player_not_answered", this.getName().getString());
    }
    @OnlyIn(Dist.CLIENT)
    public boolean shouldGlow() {
        LocalPlayer clientPlayer = Minecraft.getInstance().player;
        MessengerState messengerState = this.getMessengerState();

        if(getTargetPlayerInfo() != null && clientPlayer != null){
            UUID uuid = getTargetPlayerInfo().getUUID();
            return clientPlayer.getUUID().equals(uuid) && messengerState == MessengerState.ARRIVED || messengerState == MessengerState.MOVING_TO_TARGET_PLAYER;
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
        if(state == null || state == MessengerState.IDLE){
            return super.hurt(dmg, amt);
        }
        else return false;
    }
}










