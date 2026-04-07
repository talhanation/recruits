package com.talhanation.recruits.entities;

import com.talhanation.recruits.compat.siegeweapons.Ballista;
import com.talhanation.recruits.compat.siegeweapons.Catapult;
import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.compat.smallships.SmallShips;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.entities.ai.controller.siegeengineer.ISiegeController;
import com.talhanation.recruits.entities.ai.controller.siegeengineer.SiegeWeaponBallistaController;
import com.talhanation.recruits.entities.ai.controller.siegeengineer.SiegeWeaponCatapultController;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class SiegeEngineerEntity extends AbstractRecruitEntity implements ICompanion, IStrategicFire, IHasTargetPriority {
    public ISiegeController siegeController;
    private final SiegeWeaponCatapultController catapultController;
    private final SiegeWeaponBallistaController ballistaController;
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(SiegeEngineerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<BlockPos>> STRATEGIC_FIRE_POS = SynchedEntityData.defineId(SiegeEngineerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> SHOULD_STRATEGIC_FIRE = SynchedEntityData.defineId(SiegeEngineerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TARGET_PRIORITY = SynchedEntityData.defineId(SiegeEngineerEntity.class, EntityDataSerializers.INT);

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));
    public SiegeEngineerEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
        this.catapultController = new SiegeWeaponCatapultController(this, world);
        this.ballistaController = new SiegeWeaponBallistaController(this, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(STRATEGIC_FIRE_POS, Optional.empty());
        this.entityData.define(SHOULD_STRATEGIC_FIRE, false);
        this.entityData.define(TARGET_PRIORITY, TargetPriority.CLOSEST.getIndex());
    }
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("OwnerName", this.getOwnerName());
        if(this.getStrategicFirePos() != null){

            nbt.putInt("StrategicFirePosX", this.getStrategicFirePos().getX());
            nbt.putInt("StrategicFirePosY", this.getStrategicFirePos().getY());
            nbt.putInt("StrategicFirePosZ", this.getStrategicFirePos().getZ());
        }
        nbt.putBoolean("ShouldStrategicFire", this.getShouldStrategicFire());
        nbt.putInt("TargetPriority", this.getTargetPriority());
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setOwnerName(nbt.getString("OwnerName"));

        if (nbt.contains("StrategicFirePosX") && nbt.contains("StrategicFirePosY") && nbt.contains("StrategicFirePosZ")) {
            this.setStrategicFirePos(new BlockPos (
                    nbt.getInt("StrategicFirePosX"),
                    nbt.getInt("StrategicFirePosY"),
                    nbt.getInt("StrategicFirePosZ")));

        }
        this.setShouldStrategicFire(nbt.getBoolean("ShouldStrategicFire"));
        if(nbt.contains("TargetPriority")) this.setTargetPriority(TargetPriority.fromIndex(nbt.getInt("TargetPriority")));

    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(ForgeMod.SWIM_SPEED.get(), 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(ForgeMod.ENTITY_REACH.get(), 0D)
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
        this.setPersistenceRequired();
        if(this.getOwner() != null)this.setOwnerName(this.getOwner().getName().getString());
        AbstractRecruitEntity.applySpawnValues(this);
    }

    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack){
        return !(itemStack.getItem() instanceof CrossbowItem || itemStack.getItem() instanceof BowItem);
    }

    @Override
    public AbstractRecruitEntity get() {
        return this;
    }

    @Override
    public void openSpecialGUI(Player player) {

    }
    public String getOwnerName() {
        return entityData.get(OWNER_NAME);
    }

    public void setOwnerName(String name) {
        entityData.set(OWNER_NAME, name);
    }

    public boolean isAtMission() {
        return false;
    }
    public int getTargetPriority() {
        return this.entityData.get(TARGET_PRIORITY);
    }

    public void setTargetPriority(TargetPriority priority) {
        this.entityData.set(TARGET_PRIORITY, priority.getIndex());
    }

    @Override
    public void tick() {
        super.tick();

        if(this.getVehicle() != null & this.siegeController == null){
            this.selectController(this.getVehicle());
        };

        if(siegeController == null) return;

        if(this.getVehicle() != null && siegeController.getSiegeEntity() == null){
            siegeController.tryMount(this.getVehicle());
        }

        siegeController.tick();
    }

    @Override
    public void die(DamageSource dmg) {
        if(this.siegeController != null) siegeController.reset();
        super.die(dmg);
    }

    @Override
    public boolean startRiding(Entity entity) {
        this.selectController(entity);

        if(this.siegeController != null)  siegeController.tryMount(entity);

        return super.startRiding(entity);
    }

    @Override
    public void stopRiding() {
        if(this.siegeController != null) siegeController.tryDismount();

        super.stopRiding();
    }

    private void selectController(Entity vehicle) {
        if(Catapult.isCatapult(vehicle)){
            this.siegeController = catapultController;
            catapultController.tryMount(vehicle);
        }
        else if(Ballista.isBallista(vehicle)){
            this.siegeController = ballistaController;
            ballistaController.tryMount(vehicle);
        }
    }

    @Override
    public void setFollowState(int state) {
        super.setFollowState(state);
        if(this.siegeController != null)  this.siegeController.calculatePath();
    }
    @Override
    public void setAggroState(int state){
        super.setAggroState(state);

        if(this.siegeController != null){
            switch (state) {
                case 0, 3 -> siegeController.setTargetPos(null);
            }
        }
    }

    public void setShouldStrategicFire(boolean bool) {
        if(!bool) if(this.siegeController != null) this.siegeController.setTargetPos(null);
        this.entityData.set(SHOULD_STRATEGIC_FIRE, bool);
    }
    public boolean getShouldStrategicFire(){
        return this.entityData.get(SHOULD_STRATEGIC_FIRE);
    }

    public void setStrategicFirePos(BlockPos pos) {
        this.entityData.set(STRATEGIC_FIRE_POS, Optional.of(pos));
    }
    public BlockPos getStrategicFirePos(){
        return this.entityData.get(STRATEGIC_FIRE_POS).orElse(null);
    }

    // ========================= TARGET FINDING =========================

    public boolean shouldAttackMovingTarget(){
        return this.siegeController instanceof SiegeWeaponBallistaController;
    }

    public void checkForPotentialEnemies() {
        if(level().isClientSide()) return;

        if(shouldAttackMovingTarget()){
            LivingEntity target = this.getTarget();

            if(target != null && target.isAlive() && target.hasLineOfSight(this)){
                siegeController.setTargetPos(target.getEyePosition());
                return;
            }
            else siegeController.setTargetPos(null);
        }

        List<Entity> targets = new ArrayList<>(this.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(200D)).stream()
                .filter((target) -> shouldAttack(target) && this.hasLineOfSight(target) && !target.isUnderWater())
                .toList());

        if(targets.isEmpty()) return;

        TargetPriority targetPriority = TargetPriority.fromIndex(this.getTargetPriority());
        Entity target = null;

        switch (targetPriority) {
            case INFANTRY -> {
                target = findInfantryTarget(new ArrayList<>(targets));
            }
            case CAVALRY -> {
                target = findCavalryTarget(new ArrayList<>(targets));
            }
            case SIEGE_WEAPONS -> {
                target = findMannedSiegeWeaponTarget(new ArrayList<>(targets));
            }
            case SHIPS -> {
                target = findMannedShipTarget(new ArrayList<>(targets));
            }
            default -> {
                // CLOSEST
            }
        }

        if(target == null){
            targets.sort(Comparator.comparing(this::enemyDistanceToThis));
            target = targets.get(0);

            // Still check for manned siege weapons as high-value targets
            for (Entity e : targets){
                if (e.getVehicle() != null && SiegeWeapon.isSiegeWeapon(e.getVehicle())){
                    target = e.getVehicle();
                    break;
                }
            }
        }

        if(this.siegeController != null){
            if(target instanceof LivingEntity living) this.setTarget(living);
            siegeController.setTargetPos(target.getOnPos().above(2).getCenter());
        }
    }

    private Entity findInfantryTarget(List<Entity> targets) {
        List<Entity> infantry = targets.stream()
                .filter(e -> e instanceof LivingEntity living
                        && !(living.getVehicle() instanceof AbstractHorse)
                        && !SiegeWeapon.isSiegeWeapon(living.getVehicle())
                        && !SmallShips.isSmallShip(living.getVehicle()))
                .sorted(Comparator.comparing(this::enemyDistanceToThis))
                .toList();

        return infantry.isEmpty() ? null : infantry.get(0);
    }

    private Entity findCavalryTarget(List<Entity> targets) {
        List<Entity> cavalry = targets.stream()
                .filter(e -> e instanceof LivingEntity living
                        && living.getVehicle() instanceof AbstractHorse)
                .sorted(Comparator.comparing(this::enemyDistanceToThis))
                .toList();

        return cavalry.isEmpty() ? null : cavalry.get(0);
    }

    private Entity findMannedSiegeWeaponTarget(List<Entity> targets) {
        // Find enemies that are riding siege weapons (manned only)
        for(Entity e : targets){
            if(e.getVehicle() != null && SiegeWeapon.isSiegeWeapon(e.getVehicle())){
                return e.getVehicle(); // Target the siege weapon itself
            }
        }

        // Also check nearby siege weapons that have passengers
        List<Entity> allEntities = this.getCommandSenderWorld().getEntitiesOfClass(Entity.class, this.getBoundingBox().inflate(200D)).stream()
                .filter(e -> SiegeWeapon.isSiegeWeapon(e) && !e.getPassengers().isEmpty())
                .sorted(Comparator.comparing(this::enemyDistanceToThis))
                .toList();

        return allEntities.isEmpty() ? null : allEntities.get(0);
    }

    private Entity findMannedShipTarget(List<Entity> targets) {
        // Find enemies on ships (manned only)
        for(Entity e : targets){
            if(e.getVehicle() != null && SmallShips.isSmallShip(e.getVehicle())){
                return e.getVehicle(); // Target the ship itself
            }
        }

        // Also check nearby ships that have passengers
        List<Entity> ships = this.getCommandSenderWorld().getEntitiesOfClass(Entity.class, this.getBoundingBox().inflate(200D)).stream()
                .filter(e -> SmallShips.isSmallShip(e) && !e.getPassengers().isEmpty())
                .sorted(Comparator.comparing(this::enemyDistanceToThis))
                .toList();

        return ships.isEmpty() ? null : ships.get(0);
    }

    public double enemyDistanceToThis(Entity entity){
        return entity.distanceToSqr(this.position());
    }

}
