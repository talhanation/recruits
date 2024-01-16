package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.CaptainAttackAI;
import com.talhanation.recruits.entities.ai.CaptainControlBoatAI;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.entities.ai.navigation.SailorPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class CaptainEntity extends AbstractLeaderEntity implements IBoatController {

    public boolean shipAttacking = false;
    private static final EntityDataAccessor<Optional<BlockPos>> SAIL_POS = SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));
    public CaptainEntity(EntityType<? extends AbstractLeaderEntity> entityType, Level world) {
        super(entityType, world);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SAIL_POS, Optional.empty());
    }
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new CaptainAttackAI(this));
        this.goalSelector.addGoal(0, new CaptainControlBoatAI(this));
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    @Override
    @NotNull
    public PathNavigation getNavigation() {
        if (this.getVehicle() instanceof Boat) {
            return new SailorPathNavigation(this, level);
        }
        else
            return super.getNavigation();
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if(this.getSailPos() != null){
            nbt.putInt("SailPosX", this.getSailPos().getX());
            nbt.putInt("SailPosY", this.getSailPos().getY());
            nbt.putInt("SailPosZ", this.getSailPos().getZ());
        }
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("SailPosX") && nbt.contains("SailPosY") && nbt.contains("SailPosZ")) {
            this.setSailPos(new BlockPos (
                    nbt.getInt("SailPosX"),
                    nbt.getInt("SailPosY"),
                    nbt.getInt("SailPosZ")));
        }
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }


    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(random, difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setDropEquipment();
        this.setPersistenceRequired();
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

    public BlockPos getCurrentWaypoint(){
        return this.currentWaypoint;
    }

    @Override
    public BlockPos getSailPos() {
        return this.entityData.get(SAIL_POS).orElse(null);
    }

    public float getPrecisionMin(){
        int base = 50;
        if(this.getVehicle() != null && this.getVehicle().getEncodeId().contains("smallships")){
            base = 100;
        }
        return base;
    }

    public float getPrecisionMax(){
        int base = 150;
        if(this.getVehicle() != null && this.getVehicle().getEncodeId().contains("smallships")){
            base = 200;
        }

        return base;
    }

    @Override
    public void setSailPos(BlockPos pos) {
        if(pos == null) this.setSailPos(Optional.empty());
        else this.setSailPos(Optional.of(pos));
    }

    public void setSailPos(Optional<BlockPos>  pos) {
        this.entityData.set(SAIL_POS, pos);
    }

    @Override
    public void setFollowState(int state){
        super.setFollowState(state);

        this.calculateSailPos(state);
    }

    //0 = wander
    //1 = follow
    //2 = hold position
    //3 = back to position
    //4 = hold my position
    //5 = Protect
    public void calculateSailPos(int state) {
        switch (state){
            case 0 -> {// WANDER
                if(this.getMovePos() != null){
                    BlockPos pos = this.getMovePos();
                    setSailPos(pos);

                }
            }

            case 2,3,4 -> {
                if(this.getHoldPos() != null){
                    BlockPos pos = this.getHoldPos();
                    setSailPos(pos);
                }
            }

            case 5 -> {// PROTECT

            }
        }
        this.shipAttacking = false;
    }
}










