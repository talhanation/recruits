package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.CaptainAttackAI;
import com.talhanation.recruits.entities.ai.CaptainControlBoatAI;
import com.talhanation.recruits.entities.ai.PatrolLeaderAttackAI;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.entities.ai.navigation.SailorPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CaptainEntity extends AbstractLeaderEntity implements IBoatController, IStrategicFire {

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
    public double getDistanceToReachWaypoint() {
        return 150D;
    }

    @Override
    @NotNull
    public PathNavigation getNavigation() {
        if (this.getVehicle() instanceof Boat) {
            return new SailorPathNavigation(this, this.getCommandSenderWorld());
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
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(ForgeMod.ATTACK_RANGE.get(), 0D)
                .add(Attributes.ATTACK_SPEED);
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
        if(itemStack.getDescriptionId().contains("smallships")) return true;

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
    @Override
    protected void moveToCurrentWaypoint() {
        if(this.getVehicle() != null && this.getVehicle() instanceof Boat){
            this.setSailPos(this.currentWaypoint);
        }
        else super.moveToCurrentWaypoint();
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
                LivingEntity protect = this.getProtectingMob();
                if(protect != null){
                    BlockPos pos = protect.getOnPos();
                    setSailPos(pos);
                }
            }
        }
    }

    public boolean canAttackWhilePatrolling(LivingEntity target) {
        if(target != null && target.isAlive() && this.getSensing().hasLineOfSight(target) && this.getCommandSenderWorld().canSeeSky(target.blockPosition().above())) {
            if(this.getRecruitsInCommand().stream().anyMatch(PatrolLeaderAttackAI::isRanged)){
                return true;
            }
            else if(this.getVehicle() != null){
                return IBoatController.hasCannons(this.getVehicle()) && IBoatController.canShootCannons(this.getVehicle());
            }
            return true;
        }
        else
            return false;
    }

    protected void handleUpkeepState() {
        if(this.getVehicle() != null && waitForRecruitsUpkeepTime != 0){
            double speed = IBoatController.getSmallshipSpeed(this.getVehicle());
            if(speed < 0.01){
                this.stopRiding();
                this.setRecruitsDismount();

                //Repair ship
                //restock cannonballs
                //
            }
        }

        if(waitForRecruitsUpkeepTime == 0){
            this.shouldMount(true, this.getMountUUID());
            this.setRecruitsToFollow();
            if(this.getVehicle() != null && this.getVehicle().getUUID().equals(this.getMountUUID())){
                if(isRecruitsInCommandOnBoard()){
                    waitForRecruitsUpkeepTime = this.getAgainResupplyTime(); // time to resupply again
                    this.setPatrolState(State.PATROLLING);
                }
            }
        }
    }

    private boolean isRecruitsInCommandOnBoard() {
        return getRecruitsInCommand().stream().allMatch(recruit -> recruit.getVehicle() != null && recruit.getVehicle().equals(this.getVehicle()));
    }

    public int getResupplyTime() {
        return 1000;
    }

    @Override
    public void setShouldStrategicFire(boolean should) {

    }

    @Override
    public void setStrategicFirePos(BlockPos blockpos) {

    }
}










