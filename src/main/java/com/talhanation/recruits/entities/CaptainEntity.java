package com.talhanation.recruits.entities;

import com.talhanation.recruits.compat.SmallShips;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.entities.ai.controller.SmallShipsController;
import com.talhanation.recruits.entities.ai.controller.CaptainPrepareShipAttackController;
import com.talhanation.recruits.entities.ai.navigation.SailorPathNavigation;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.util.RecruitCommanderUtil;
import com.talhanation.recruits.util.WaterObstacleScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class CaptainEntity extends AbstractLeaderEntity implements IStrategicFire {
    public final SmallShipsController smallShipsController;
    private static final EntityDataAccessor<Optional<BlockPos>> SAIL_POS = SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> STRATEGIC_FIRE_POS = SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> SHOULD_STRATEGIC_FIRE = SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.BOOLEAN);

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));
    public CaptainEntity(EntityType<? extends AbstractLeaderEntity> entityType, Level world) {
        super(entityType, world);
        this.attackController = new CaptainPrepareShipAttackController(this);
        this.smallShipsController = new SmallShipsController(this, world);
        this.smallShipsController.tryMountShip(getVehicle());
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SAIL_POS, Optional.empty());
        this.entityData.define(STRATEGIC_FIRE_POS, Optional.empty());
        this.entityData.define(SHOULD_STRATEGIC_FIRE, false);
    }
    @Override
    protected void registerGoals() {
        super.registerGoals();
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

    @Override
    public void tick() {
        super.tick();
        if(this.level().isClientSide()) return;

        if(this.getVehicle() != null && smallShipsController.ship == null){
            smallShipsController.tryMountShip(this.getVehicle());
        }

        smallShipsController.tick();
        attackController.tick();
    }


    public void setHoldPos(Vec3 holdPos){
        if(SmallShips.isSmallShip(this.getVehicle())){
            if(!WaterObstacleScanner.isWaterBlockPos(this.getCommandSenderWorld(), new BlockPos((int) holdPos.x, (int) holdPos.y, (int) holdPos.z))){
                super.setHoldPos(holdPos);
            }
        }
        super.setHoldPos(holdPos);
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if(this.getSailPos() != null){
            nbt.putInt("SailPosX", this.getSailPos().getX());
            nbt.putInt("SailPosY", this.getSailPos().getY());
            nbt.putInt("SailPosZ", this.getSailPos().getZ());
        }

        if(this.StrategicFirePos() != null){

            nbt.putInt("StrategicFirePosX", this.StrategicFirePos().getX());
            nbt.putInt("StrategicFirePosY", this.StrategicFirePos().getY());
            nbt.putInt("StrategicFirePosZ", this.StrategicFirePos().getZ());
            nbt.putBoolean("ShouldStrategicFire", this.getShouldStrategicFire());
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

        if (nbt.contains("StrategicFirePosX") && nbt.contains("StrategicFirePosY") && nbt.contains("StrategicFirePosZ")) {
            this.setStrategicFirePos(new BlockPos (
                    nbt.getInt("StrategicFirePosX"),
                    nbt.getInt("StrategicFirePosY"),
                    nbt.getInt("StrategicFirePosZ")));
            this.setShouldStrategicFire(nbt.getBoolean("ShouldStrategicFire"));
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
                .add(ForgeMod.ENTITY_REACH.get(), 0D)
                .add(Attributes.ATTACK_SPEED);
    }


    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((AsyncGroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
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
    public boolean startRiding(Entity entity) {
        smallShipsController.tryMountShip(entity);
        return super.startRiding(entity);
    }

    @Override
    public void stopRiding() {
        smallShipsController.tryDisMountShip();
        super.stopRiding();
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
                    Vec3 pos = this.getHoldPos();
                    setSailPos(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
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
        this.smallShipsController.calculatePath();
    }
    @Override
    public boolean canAttackWhilePatrolling() {
        if(enemyArmySpotted() || this.getTarget() != null && this.getTarget().isAlive()) {
            if(this.army != null && !this.army.getRanged().isEmpty()){
                return true;
            }
            else if(this.getVehicle() != null && smallShipsController.ship != null){
                return smallShipsController.ship.hasCannons() && smallShipsController.ship.canShootCannons();
            }
            return true;
        }
        else
            return false;
    }
    @Override
    public void handleUpkeepState() {
        if(army == null) return;

        if(waitForRecruitsUpkeepTime != 0){
            if(smallShipsController.ship != null){
                double speed = smallShipsController.ship.getShipSpeed();
                if(speed < 0.01){
                    this.stopRiding();
                    RecruitCommanderUtil.setRecruitsDismount(army.getAllRecruitUnits());
                }
            }
        }
        else {
            this.shouldMount(true, this.getMountUUID());
            RecruitCommanderUtil.setRecruitsFollow(this.army.getAllRecruitUnits(), this.getUUID());
            if(this.getVehicle() != null && this.getVehicle().getUUID().equals(this.getMountUUID())){
                if(canRepair() && smallShipsController.ship.getDamage() > 10){
                    if(this.tickCount % 20 == 0) smallShipsController.ship.repairShip(this);
                }
                else if(this.getCannonBallCount(this.getInventory()) > 0 && smallShipsController.ship instanceof Container container && this.getCannonBallCount(container) < 128){
                    if(this.tickCount % 20 == 0) refillCannonBalls();
                }
                else{
                    if(isRecruitsInCommandOnBoard()) {
                        waitForRecruitsUpkeepTime = this.getAgainResupplyTime(); // time to resupply again
                        this.setPatrolState(State.PATROLLING);
                    }
                    else{
                        this.setFollowState(2);
                        RecruitCommanderUtil.setRecruitsMount(this.army.getAllRecruitUnits(), this.getVehicle().getUUID());
                    }
                }
            }
        }
    }
    @Override
    public boolean getOtherUpkeepInterruption(){
        if(smallShipsController.ship != null && smallShipsController.ship.getDamage() > 10){
            return true;
        }

        if(smallShipsController.ship != null && smallShipsController.ship.hasCannons() && smallShipsController.ship.getBoat() instanceof Container container && this.getCannonBallCount(container) < 32){
            return true;
        }
        return super.getOtherUpkeepInterruption();
    }

    private boolean isRecruitsInCommandOnBoard() {
        return this.army.getAllRecruitUnits().stream().allMatch(recruit -> recruit.getVehicle() != null && recruit.getVehicle().equals(this.getVehicle()));
    }

    public int getResupplyTime() {
        return 1000;
    }

    public void setStrategicFirePos(BlockPos pos) {
        if(pos != null) this.entityData.set(STRATEGIC_FIRE_POS, Optional.of(pos));
        else this.entityData.set(STRATEGIC_FIRE_POS, Optional.empty());
    }
    public BlockPos StrategicFirePos(){
        return this.entityData.get(STRATEGIC_FIRE_POS).orElse(null);
    }

    public void setShouldStrategicFire(boolean bool) {
        this.entityData.set(SHOULD_STRATEGIC_FIRE, bool);
    }
    public boolean getShouldStrategicFire(){
        return this.entityData.get(SHOULD_STRATEGIC_FIRE);
    }

    public boolean canRepair() {
        return this.getInventory().hasAnyMatching(itemStack -> itemStack.is(Items.IRON_NUGGET)) && this.getInventory().hasAnyMatching(itemStack -> itemStack.is(ItemTags.PLANKS));
    }

    public void refillCannonBalls() {
        if(this.getInventory().hasAnyMatching(itemStack -> itemStack.getDescriptionId().contains("cannon_ball"))){
            if(this.getVehicle() instanceof Container container){

                for(int i = 0; i < this.getInventory().getContainerSize(); i++){
                    ItemStack stack = this.getInventory().getItem(i);

                    if(stack.getDescriptionId().contains("cannon_ball")){
                        ItemStack cannonball = stack.copy();
                        for(int k = 0; k < container.getContainerSize(); k++){
                            if(container.getItem(k).isEmpty()) {
                                container.setItem(k, cannonball);
                                stack.shrink(cannonball.getCount());

                                this.getInventory().setChanged();
                                container.setChanged();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public int getCannonBallCount(Container container) {
        int count = 0;
        for(int i = 0; i < container.getContainerSize(); i++){
            ItemStack stack = container.getItem(i);

            if(stack.getDescriptionId().contains("cannon_ball")){
                    count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public boolean isAtMission() {
        return this.state != State.IDLE && this.state != State.PAUSED && this.state != State.STOPPED;
    }

    @Override
    public boolean hurt(@NotNull DamageSource dmg, float amt) {
        this.commandCooldown = 0;
        return super.hurt(dmg, amt);
    }
}











