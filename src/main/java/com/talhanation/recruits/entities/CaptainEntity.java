package com.talhanation.recruits.entities;

import com.talhanation.recruits.compat.smallships.SmallShipsBridge;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.entities.ai.controller.PatrolLeaderAttackController;
import com.talhanation.recruits.entities.ai.controller.ship.CaptainShipController;
import com.talhanation.recruits.entities.ai.controller.ship.ShipProfile;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

public class CaptainEntity extends AbstractLeaderEntity implements IStrategicFire, IHasTargetPriority {
    /** everything he decides and does with a ship: helm, fight, retreat, harbour, the trail home - see CaptainShipController */
    public final CaptainShipController shipController;
    private static final EntityDataAccessor<Optional<BlockPos>> SAIL_POS = SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> STRATEGIC_FIRE_POS = SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> SHOULD_STRATEGIC_FIRE = SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> HARBOUR_POS = SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> TARGET_PRIORITY = SynchedEntityData.defineId(CaptainEntity.class, EntityDataSerializers.INT);

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));
    public CaptainEntity(EntityType<? extends AbstractLeaderEntity> entityType, Level world) {
        super(entityType, world);
        // at the helm the fight belongs to the ship controller alone - ashore he leads his troops like any patrol leader
        this.attackController = new PatrolLeaderAttackController(this) {
            @Override
            public void tick() {
                if (!CaptainEntity.this.isSteeringShip()) super.tick();
            }

            @Override
            public boolean isTargetInRange() {
                return false;
            }
        };
        this.shipController = new CaptainShipController(this);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SAIL_POS, Optional.empty());
        this.entityData.define(STRATEGIC_FIRE_POS, Optional.empty());
        this.entityData.define(SHOULD_STRATEGIC_FIRE, false);
        this.entityData.define(HARBOUR_POS, Optional.empty());
        this.entityData.define(TARGET_PRIORITY, TargetPriority.AUTO.getIndex());
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
    public void tick() {
        super.tick();
        if(this.level().isClientSide()) return;

        this.shipController.tick();
    }

    /** @return true while this captain stands at the helm of a ship he can sail */
    public boolean isSteeringShip() {
        return this.shipController.isActive();
    }

    @Override
    protected boolean leadsOwnFight() {
        return this.isSteeringShip() || this.shipController.isHarbourRunning() || this.shipController.isRetreating();
    }

    @Override
    protected void checkForPotentialEnemies() {
        // at the helm the ship controller searches for contacts itself
        if (this.isSteeringShip()) return;
        super.checkForPotentialEnemies();
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

        BlockPos harbour = this.getHarbourPos();
        if(harbour != null){
            nbt.putInt("HarbourPosX", harbour.getX());
            nbt.putInt("HarbourPosY", harbour.getY());
            nbt.putInt("HarbourPosZ", harbour.getZ());
            this.shipController.save(nbt);
        }
        nbt.putInt("TargetPriority", this.getTargetPriority());
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

        if (nbt.contains("HarbourPosX") && nbt.contains("HarbourPosY") && nbt.contains("HarbourPosZ")) {
            BlockPos harbour = new BlockPos(
                    nbt.getInt("HarbourPosX"),
                    nbt.getInt("HarbourPosY"),
                    nbt.getInt("HarbourPosZ"));
            this.entityData.set(HARBOUR_POS, Optional.of(harbour));
            // after super: loading the follow state would have cancelled a way home again
            this.shipController.load(nbt, harbour);
        }
        if (nbt.contains("TargetPriority")) this.setTargetPriority(TargetPriority.fromIndex(nbt.getInt("TargetPriority")));
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

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setPersistenceRequired();

        if(this.getCustomName() == null || this.getCustomName().getString().isEmpty()) this.setCustomName(Component.literal("Captain"));
    }

    @Override
    public void stopRiding() {
        // keys released while he still holds the helm
        shipController.release();
        super.stopRiding();
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {//TODO: add ranged combat
        if(itemStack.getDescriptionId().contains("smallships")) return true;

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
        return SmallShipsBridge.isShip(this.getVehicle()) ? 100 : 50;
    }

    public float getPrecisionMax(){
        return SmallShipsBridge.isShip(this.getVehicle()) ? 200 : 150;
    }

    public void setSailPos(BlockPos pos) {
        if(pos == null) this.setSailPos(Optional.empty());
        else this.setSailPos(Optional.of(pos));
    }

    public void setSailPos(Optional<BlockPos>  pos) {
        this.entityData.set(SAIL_POS, pos);
    }

    @Nullable
    public BlockPos getHarbourPos() {
        return this.entityData.get(HARBOUR_POS).orElse(null);
    }

    /** Sets the home harbour, or none - the way home is recorded anew from there. */
    public void setHarbourPos(@Nullable BlockPos pos) {
        this.entityData.set(HARBOUR_POS, Optional.ofNullable(pos));
        this.shipController.resetTrail(pos);
    }

    @Override
    public int getTargetPriority() {
        return this.entityData.get(TARGET_PRIORITY);
    }

    @Override
    public void setTargetPriority(TargetPriority priority) {
        this.entityData.set(TARGET_PRIORITY, priority.getIndex());
    }

    @Override
    public void setFollowState(int state){
        // any movement order ends a return to the harbour - null while the Mob constructor runs
        if (this.shipController != null) this.shipController.cancelHomeward();
        super.setFollowState(state);

        this.calculateSailPos(state);
    }
    @Override
    protected void moveToCurrentWaypoint() {
        if(this.getVehicle() != null && this.getVehicle() instanceof Boat){
            // the water surface at the waypoint - the ship controller plans the way there
            int surfaceY = getCommandSenderWorld().getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                    this.currentWaypoint.getX(), this.currentWaypoint.getZ()) - 1;
            int y = Math.max(surfaceY, getCommandSenderWorld().getMinBuildHeight());
            this.setSailPos(new BlockPos(this.currentWaypoint.getX(), y, this.currentWaypoint.getZ()));
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
    }
    @Override
    public boolean canAttackWhilePatrolling() {
        if (this.isSteeringShip()) {
            return this.shipController.canFight() || (this.army != null && !this.army.getRanged().isEmpty());
        }
        return enemyArmySpotted() || this.getTarget() != null && this.getTarget().isAlive();
    }

    /**
     * On a ship the harbour routine does the upkeep: to the harbour, ashore to
     * the upkeep chest, repair, cannon balls into the hold, aboard again.
     * Ashore without a ship he is a patrol leader like any other.
     */
    @Override
    public void handleUpkeepState() {
        if (this.shipController.takeHarbourDone()) {
            this.waitForRecruitsUpkeepTime = this.getAgainResupplyTime();
            this.setPatrolState(State.PATROLLING);
            return;
        }
        if (this.shipController.isHarbourRunning()) return;
        if (SmallShipsBridge.isShip(this.getVehicle())) {
            this.shipController.startHarbour();
            return;
        }
        super.handleUpkeepState();
    }

    @Override
    public void handleResupply() {
        // on a ship the harbour routine sends everybody ashore once she lies still
        if (SmallShipsBridge.isShip(this.getVehicle())) return;
        super.handleResupply();
    }

    @Override
    public boolean getOtherUpkeepInterruption(){
        Entity ship = this.getVehicle();
        if (SmallShipsBridge.isShip(ship)) {
            CompoundTag condition = SmallShipsBridge.getCondition(ship);
            // below what hand repair can mend, torn canvas, or few balls left
            if (condition.getFloat("Damage") > condition.getFloat("HandRepairFloor") + 0.5F) return true;
            if (condition.getInt("SailCondition") > 0) return true;
            ShipProfile profile = this.shipController.getProfile();
            if (profile != null && profile.guns() > 0 && condition.getInt("CannonBalls") < 32) return true;
        }
        return super.getOtherUpkeepInterruption();
    }

    public int getResupplyTime()  {
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

    /** Puts the cannon balls he carries into the hold of his ship. */
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
