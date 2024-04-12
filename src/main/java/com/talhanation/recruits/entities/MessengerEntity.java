package com.talhanation.recruits.entities;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.inventory.MessengerContainer;
import com.talhanation.recruits.network.MessageOpenSpecialScreen;
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
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class MessengerEntity extends AbstractRecruitEntity implements ICompanion {

    private static final EntityDataAccessor<String> TARGET_PLAYER_NAME = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> MESSAGE = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> TASK_STATE = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.BYTE);
    private final SimpleContainer deliverSlot = new SimpleContainer(1);

    private String ownerName = "";
    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public MessengerEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MESSAGE, "");
        this.entityData.define(TARGET_PLAYER_NAME, "");
        this.entityData.define(TASK_STATE, (byte) 0);
    }

    @Override
    protected void registerGoals() {
       super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        CompoundTag itemTag = new CompoundTag();
        this.deliverSlot.getItem(0).save(itemTag);
        nbt.put("DeliverItem", itemTag);
        nbt.putString("Message", this.getMessage());
        nbt.putString("TargetPlayerName", this.getTargetPlayerName());
        nbt.putString("OwnerName", this.getOwnerName());
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.deliverSlot.setItem(0, ItemStack.of(nbt.getCompound("DeliverItem")));
        this.setTargetPlayerName(nbt.getString("TargetPlayerName"));
        this.setMessage(nbt.getString("Message"));
        this.setOwnerName(nbt.getString("OwnerName"));
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

    public void openSpecialGUI(Player player) {
        if (player instanceof ServerPlayer) {
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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String name) {
        ownerName = name;
    }

    public Container getDeliverSlot() {
        return this.deliverSlot;
    }

    public String getMessage() {
        return this.entityData.get(MESSAGE);
    }

    public void setMessage(String message) {
        this.entityData.set(MESSAGE, message);
    }

    public String getTargetPlayerName() {
        return this.entityData.get(TARGET_PLAYER_NAME);
    }

    public void setTargetPlayerName(String name) {
        this.entityData.set(TARGET_PLAYER_NAME, name);
    }

    public void start(){
        if(!this.getCommandSenderWorld().isClientSide()){
            ServerLevel serverLevel = (ServerLevel) getCommandSenderWorld();

            MinecraftServer server = serverLevel.getServer();
            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(this.getTargetPlayerName());
            if(targetPlayer == null && this.getOwner() != null){
                this.getOwner().sendSystemMessage(PLAYER_NOT_FOUND());
                return;
            }

            Vec3 targetPlayerPos = targetPlayer.position();

            BlockPos validPos = null;

            this.moveTo(targetPlayerPos);
            this.arriveAtTargetPlayer(targetPlayer);
            //this.setTaskState((byte)1);
        }
    }

    public void arriveAtTargetPlayer(ServerPlayer target){
        tellTargetPlayerArrived(target);
    }

    public void tellTargetPlayerArrived(ServerPlayer target){
        Team ownerTeam = this.getTeam();
        if(ownerTeam != null )target.sendSystemMessage(MESSENGER_ARRIVED_TEAM(this.getOwnerName(), ownerTeam.getName()));
        else target.sendSystemMessage(MESSENGER_ARRIVED(this.getOwnerName()));
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


    public enum State{
        IDLE(0),
        MOVING_TO_DIRECTION(1),
        TELEPORT(2),
        MOVING_TO_TARGET_PLAYER(3),
        ARRIVED(4),
        MOVING_BACK(5),
        TELEPORT_BACK(6),
        MOVING_TO_PLAYER(7);


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

}










