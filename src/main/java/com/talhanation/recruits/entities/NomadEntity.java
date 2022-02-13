package com.talhanation.recruits.entities;

import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.PillagerEntity;
import net.minecraft.entity.monster.RavagerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;

public class NomadEntity extends BowmanEntity{

    private static final DataParameter<Boolean> HAD_HORSE = EntityDataManager.defineId(NomadEntity.class, DataSerializers.BOOLEAN);

    public NomadEntity(EntityType<? extends AbstractRecruitEntity> entityType, World world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HAD_HORSE, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("hadHorse", this.getHadHorse());
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        this.setHadHorse(nbt.getBoolean("hadHorse"));
        this.reassessWeaponGoal();
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
    }

    //ATTRIBUTES
    public static AttributeModifierMap.MutableAttribute setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);

    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance difficultyInstance, SpawnReason reason, @Nullable ILivingEntityData data, @Nullable CompoundNBT nbt) {
        ILivingEntityData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigator)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);

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
        this.reassessWeaponGoal();
        this.setGroup(2);
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
                if (this.getOwner() != null) horse.setOwnerUUID(this.getOwnerUUID());

                this.startRiding(horse);
                this.level.addFreshEntity(horse);
                this.setHadHorse(true);
            }
        }
    }



    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.level.getProfiler().push("looting");
        if (!this.level.isClientSide && this.canPickUpLoot() && this.isAlive() && !this.dead && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this)) {
            for(ItemEntity itementity : this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(2.5D, 2.5D, 2.5D))) {
                if (!itementity.removed && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && this.wantsToPickUp(itementity.getItem())) {
                    this.pickUpItem(itementity);
                }
            }
        }
    }
}
