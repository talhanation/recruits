package com.talhanation.recruits.entities;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.inventory.AssassinLeaderContainer;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.MessageAssassinGui;
import com.talhanation.recruits.network.MessageRecruitGui;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class AssassinLeaderEntity extends AbstractOrderAbleEntity {
    private static final DataParameter<Integer> COUNT = EntityDataManager.defineId(AbstractOrderAbleEntity.class, DataSerializers.INT);

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public AssassinLeaderEntity(EntityType<? extends AbstractOrderAbleEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
       super.registerGoals();
        //this.goalSelector.addGoal(2, new RecruitUseShield(this));
    }

    //ATTRIBUTES
    public static AttributeModifierMap.MutableAttribute setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance difficultyInstance, SpawnReason reason, @Nullable ILivingEntityData data, @Nullable CompoundNBT nbt) {
        ILivingEntityData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigator)this.getNavigation()).setCanOpenDoors(true);
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
        this.setItemSlot(EquipmentSlotType.OFFHAND, new ItemStack(Items.EMERALD));
        this.setItemSlot(EquipmentSlotType.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        this.setItemSlot(EquipmentSlotType.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        this.setItemSlot(EquipmentSlotType.FEET, new ItemStack(Items.IRON_BOOTS));

        inventory.setItem(10, new ItemStack(Items.EMERALD));
        inventory.setItem(12, new ItemStack(Items.IRON_CHESTPLATE));
        inventory.setItem(13, new ItemStack(Items.IRON_LEGGINGS));
        inventory.setItem(14, new ItemStack(Items.IRON_BOOTS));
        int i = this.random.nextInt(8);
        if (i == 0) {
            inventory.setItem(9, new ItemStack(Items.IRON_AXE));
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.IRON_AXE));
        } else  if (i == 1){
            inventory.setItem(9, new ItemStack(Items.IRON_AXE));
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.IRON_AXE));
        } else  if (i == 2){
            inventory.setItem(9, new ItemStack(Items.IRON_AXE));
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.IRON_AXE));
        }else{
            inventory.setItem(9, new ItemStack(Items.DIAMOND_SWORD));
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    @Override
    public void openGUI(PlayerEntity player) {
        this.navigation.stop();

        if (player instanceof ServerPlayerEntity) {
            NetworkHooks.openGui((ServerPlayerEntity) player, new INamedContainerProvider() {
                @Override
                public ITextComponent getDisplayName() {
                    return getName();
                }

                @Nullable
                @Override
                public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
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
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Count", this.getCount());
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
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










