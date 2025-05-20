package com.talhanation.recruits.entities;

import com.talhanation.recruits.compat.siegeweapons.SiegeWeapon;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.entities.ai.controller.siegeengineer.ISiegeController;
import com.talhanation.recruits.entities.ai.controller.siegeengineer.SiegeWeaponCatapultController;
import com.talhanation.recruits.util.NPCArmy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SiegeEngineerEntity extends AbstractRecruitEntity implements ICompanion, IStrategicFire {
    public final ISiegeController siegeController;
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(SiegeEngineerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<BlockPos>> STRATEGIC_FIRE_POS = SynchedEntityData.defineId(SiegeEngineerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> SHOULD_STRATEGIC_FIRE = SynchedEntityData.defineId(SiegeEngineerEntity.class, EntityDataSerializers.BOOLEAN);
    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));
    public SiegeEngineerEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
        this.siegeController = new SiegeWeaponCatapultController(this, world);
        this.siegeController.tryMount(getVehicle());
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(STRATEGIC_FIRE_POS, Optional.empty());
        this.entityData.define(SHOULD_STRATEGIC_FIRE, false);
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
        this.setDropEquipment();
        this.setPersistenceRequired();
        if(this.getOwner() != null)this.setOwnerName(this.getOwner().getName().getString());
        AbstractRecruitEntity.applySpawnValues(this);
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

    @Override
    public void tick() {
        super.tick();
        if(this.getVehicle() != null && siegeController.getSiegeEntity() == null){
            siegeController.tryMount(this.getVehicle());
        }

        siegeController.tick();
    }

    @Override
    public void die(DamageSource dmg) {
        siegeController.reset();
        super.die(dmg);
    }

    @Override
    public boolean startRiding(Entity entity) {
        siegeController.tryMount(entity);
        return super.startRiding(entity);
    }

    @Override
    public void stopRiding() {
        siegeController.tryDismount();
        super.stopRiding();
    }

    @Override
    public void setFollowState(int state) {
        super.setFollowState(state);
        this.siegeController.calculatePath();
    }

    public void setShouldStrategicFire(boolean bool) {
        if(!bool) this.siegeController.setTargetPos(null);
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

    public void checkForPotentialEnemies() {
        if(!level().isClientSide()){
            List<Entity> targets = new ArrayList<>(this.getCommandSenderWorld().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(200D)).stream()
                    .filter((target) -> shouldAttack(target) && this.hasLineOfSight(target) && !target.isUnderWater())
                    .toList());

            if(targets.isEmpty()) return;


            //TODO: prio setting for targets e.g.: siege weapons first, infantry first, ships first, strategic first

            targets.sort(Comparator.comparing(this::enemyDistanceToThis));
            Entity target = targets.get(0);

            for (Entity e : targets){
                if (e.getVehicle() != null && SiegeWeapon.isSiegeWeapon(e.getVehicle())){
                    target = e.getVehicle();
                    break;
                }
            }

            siegeController.setTargetPos(target.position());
        }
    }

    public double enemyDistanceToThis(Entity entity){
        return entity.distanceToSqr(this.position());
    }
}









