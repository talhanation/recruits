package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.HorsemanAttackAI;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

public class HorsemanEntity extends RecruitShieldmanEntity {

    private static final EntityDataAccessor<Boolean> HAD_HORSE = SynchedEntityData.defineId(HorsemanEntity.class, EntityDataSerializers.BOOLEAN);

    public HorsemanEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HAD_HORSE, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("hadHorse", this.getHadHorse());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setHadHorse(nbt.getBoolean("hadHorse"));
        //this.reassessWeaponGoal();
    }

    private void setHadHorse(boolean hadHorse) {
        entityData.set(HAD_HORSE, hadHorse);
    }

    private boolean getHadHorse() {
        return entityData.get(HAD_HORSE);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new HorsemanAttackAI(this));
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);

    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        RandomSource randomsource = world.getRandom();
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(randomsource, difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
        this.setGroup(3);
    }

    @Override
    public void tick() {
        super.tick();

        if (!getHadHorse()){
            boolean hasHorse = this.getVehicle() != null && this.getVehicle() instanceof RecruitHorseEntity;
            if (!hasHorse){
                RecruitHorseEntity horse = new RecruitHorseEntity(ModEntityTypes.RECRUIT_HORSE.get(), this.level);
                horse.setPos(this.getX(), this.getY(), this.getZ());
                horse.setRandomVariant();
                horse.setRandomSpawnBonus();
                //if (this.getOwner() != null) horse.setOwnerUUID(this.getOwnerUUID());

                this.startRiding(horse);
                this.level.addFreshEntity(horse);
                this.setHadHorse(true);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.level.getProfiler().push("looting");
        if (!this.level.isClientSide && this.canPickUpLoot() && this.isAlive() && !this.dead && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this)) {
            for(ItemEntity itementity : this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(2.5D, 2.5D, 2.5D))) {
                if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && this.wantsToPickUp(itementity.getItem())) {
                    this.pickUpItem(itementity);
                }
            }
        }
    }

    public enum State {
        //IDLE,
        SELECT_TARGET,
        CHARGE_TARGET,
    }
}
