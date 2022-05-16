package com.talhanation.recruits.entities;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.inventory.AssassinLeaderContainer;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.MessageAssassinGui;
import com.talhanation.recruits.network.MessageRecruitGui;
import net.minecraft.world.entity.AgableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class AssassinLeaderEntity extends AbstractOrderAbleEntity {
    private static final EntityDataAccessor<Integer> COUNT = SynchedEntityData.defineId(AbstractOrderAbleEntity.class, EntityDataSerializers.INT);

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public AssassinLeaderEntity(EntityType<? extends AbstractOrderAbleEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
       super.registerGoals();
        //this.goalSelector.addGoal(2, new RecruitUseShield(this));
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);
        this.setEquipment();
        //this.setDropEquipment();
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
        return ilivingentitydata;
    }
    @Override
    public void setEquipment() {// doppelt weil bug
        // wenn nur setItemSlot = dann geht beim gui opening weg
        // wenn nur setItem = dann geht beim gui opening rein
        // wtf
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.EMERALD));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));

        inventory.setItem(10, new ItemStack(Items.EMERALD));
        inventory.setItem(12, new ItemStack(Items.IRON_CHESTPLATE));
        inventory.setItem(13, new ItemStack(Items.IRON_LEGGINGS));
        inventory.setItem(14, new ItemStack(Items.IRON_BOOTS));
        int i = this.random.nextInt(8);
        if (i == 0) {
            inventory.setItem(9, new ItemStack(Items.IRON_AXE));
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        } else  if (i == 1){
            inventory.setItem(9, new ItemStack(Items.IRON_AXE));
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        } else  if (i == 2){
            inventory.setItem(9, new ItemStack(Items.IRON_AXE));
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }else{
            inventory.setItem(9, new ItemStack(Items.DIAMOND_SWORD));
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    @Nullable
    @Override
    public AgableMob getBreedOffspring(ServerLevel p_241840_1_, AgableMob p_241840_2_) {
        return null;
    }

    @Override
    public void openGUI(Player player) {
        this.navigation.stop();

        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return getName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new AssassinLeaderContainer(i, AssassinLeaderEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAssassinGui(player, this.getUUID()));
        }
    }
    @Override
    public boolean shouldShowName(){
        return true;
    }

    @Override
    public void checkItemsInInv() {

    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        return false;
    }

    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COUNT, 1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Count", this.getCount());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setCount(nbt.getInt("Count"));
    }

    public void setCount(int x){
        entityData.set(COUNT, x);
    }

    public int getCount(){
        return entityData.get(COUNT);
    }

    public int getAssassinCosts(){
        return 12;
    }

    public int calculateAssassinateCosts(int count, int price){
        return count * price;
    }

    public int getMaxAssassinCount(){
        return RecruitsModConfig.MaxAssassinCount.get();
    }
}










