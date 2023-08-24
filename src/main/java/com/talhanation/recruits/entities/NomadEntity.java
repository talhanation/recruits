package com.talhanation.recruits.entities;

import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.ai.NomadAttackAI;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class NomadEntity extends BowmanEntity {

    private static final EntityDataAccessor<Boolean> HAD_HORSE = SynchedEntityData.defineId(NomadEntity.class, EntityDataSerializers.BOOLEAN);

    public NomadEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
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
        this.goalSelector.addGoal(2, new NomadAttackAI(this));
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
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
        this.setCustomName(Component.literal("Nomad"));
        this.setCost(RecruitsModConfig.NomadCost.get());
        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();

        this.setGroup(2);
    }
    @Override
    public double arrowDamageModifier() {
        return 1D;
    }


    public List<String> getHandEquipment(){
        return RecruitsModConfig.NomadHandEquipment.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!getHadHorse()){
            boolean hasHorse = this.getVehicle() != null && this.getVehicle() instanceof AbstractHorse;
            if (!hasHorse){
                Horse horse = new Horse(EntityType.HORSE, this.getCommandSenderWorld());
                horse.setPos(this.getX(), this.getY(), this.getZ());
                horse.setTamed(true);
                horse.equipSaddle(null);

                Variant variant = Util.getRandom(Variant.values(), this.random);
                Markings markings = Util.getRandom(Markings.values(), this.random);
                horse.setVariantAndMarkings(variant, markings);

                this.startRiding(horse);
                this.getCommandSenderWorld().addFreshEntity(horse);
                this.setHadHorse(true);
                this.setMountUUID(Optional.of(horse.getUUID()));
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.getCommandSenderWorld().getProfiler().push("looting");
        if (!this.getCommandSenderWorld().isClientSide && this.canPickUpLoot() && this.isAlive() && !this.dead && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.getCommandSenderWorld(), this)) {
            for (ItemEntity itementity : this.getCommandSenderWorld().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(2.5D, 2.5D, 2.5D))) {
                if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && this.wantsToPickUp(itementity.getItem())) {
                    this.pickUpItem(itementity);
                }
            }
        }
        if (this.getVehicle() instanceof AbstractHorse abstractHorse) {
            abstractHorse.setDeltaMovement(abstractHorse.getDeltaMovement().add(this.getDeltaMovement().scale(2)));

        }
    }

    public enum State {
        //IDLE,
        SELECT_TARGET, // Der Bogenschütze richtet seine Waffe auf das ausgewählte Ziel aus
        CIRCLE_TARGET, // Der Bogenschütze bewegt sich um das Ziel herum, um es aus verschiedenen Winkeln anzugreifen
    }
}
