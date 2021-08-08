package com.talhanation.recruits.entities;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.management.PreYggdrasilConverter;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractHoldingEntity extends CreatureEntity {
    public static  final DataParameter<Optional<UUID>> OWNER_UUID = EntityDataManager.defineId(AbstractHoldingEntity.class, DataSerializers.OPTIONAL_UUID);
    protected static final DataParameter<Integer> STATE = EntityDataManager.defineId(AbstractHoldingEntity.class, DataSerializers.INT);
    private boolean orderedToHold;


    public AbstractHoldingEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(STATE, 0);
    }
    // STATE:
    // 0 == normal
    // 1 == owned
    // 2 == holding


    ////////////////////////////////////REGISTER////////////////////////////////////

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.getOwnerUUID() != null) {
            nbt.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        UUID uuid;
        if (nbt.hasUUID("Owner")) {
            uuid = nbt.getUUID("Owner");
        } else {
            String s = nbt.getString("Owner");
            uuid = PreYggdrasilConverter.convertMobOwnerIfNecessary(Objects.requireNonNull(this.getServer()), s);
        }

        if (uuid != null) {
            try {
                this.setOwnerUUID(uuid);
                this.setOwned(true);
            } catch (Throwable throwable) {
                this.setOwned(false);
            }
        }

        this.orderedToHold = nbt.getBoolean("Holding");
        this.setHoldPose(this.orderedToHold);
    }

    ////////////////////////////////////GET////////////////////////////////////

    public UUID getOwnerUUID(){
        return entityData.get(OWNER_UUID).orElse(null);
    }

    public LivingEntity getOwner(){
        UUID uuid = this.getOwnerUUID();
        return uuid == null ? null : this.level.getPlayerByUUID(uuid);
    }

    public Team getTeam() {
        if (this.isOwned()) {
            LivingEntity livingentity = this.getOwner();
            if (livingentity != null) {
                return livingentity.getTeam();
            }
        }

        return super.getTeam();
    }

    ////////////////////////////////////SET////////////////////////////////////

    public void setOwnerUUID(@Nullable UUID owner) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(owner));
    }

    public void setOwner(PlayerEntity player){
        this.isOwned();
        this.setOwnerUUID(player.getUUID());
    }

    public void setOwned(boolean owned){
        if (owned) {
            this.entityData.set(STATE, 1);
        } else {
            this.entityData.set(STATE, 0);
        }

        //this.reassessTameGoals();
    }

    public void setHoldPose(boolean holding) {
        if (holding) {
            this.entityData.set(STATE, 2);
        } else {
            this.entityData.set(STATE, 1);
        }

    }

    public void setOrderedToHold(boolean hold) {
        this.orderedToHold = hold;
    }
    ////////////////////////////////////BOOL FUNCTIONS////////////////////////////////////

    public boolean isOrderedToHold() {
        return this.orderedToHold;
    }


    public boolean isOwned(){
        return (this.entityData.get(STATE)) != 0;
    }

    public boolean isHoldingPose() {
        return (this.entityData.get(STATE)) == 2;
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity == this.getOwner();
    }

    ////////////////////////////////////ON FUNCTIONS////////////////////////////////////

    public void recruit(PlayerEntity player) {
        this.setOwned(true);
        this.setOwnerUUID(player.getUUID());
        if (player instanceof ServerPlayerEntity) {
            //CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayerEntity)player, this);
        }

    }

    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    protected void spawnTamingParticles(boolean bool) {
        IParticleData iparticledata = ParticleTypes.HAPPY_VILLAGER;
        if (!bool) {
            iparticledata = ParticleTypes.SMOKE;
        }

        for(int i = 0; i < 7; ++i) {
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(iparticledata, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
        }

    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte x) {
        if (x == 7) {
            this.spawnTamingParticles(true);
        } else if (x == 6) {
            this.spawnTamingParticles(false);
        } else {
            super.handleEntityEvent(x);
        }

    }

    public boolean isAlliedTo(Entity entity) {
        if (this.isOwned()) {
            LivingEntity livingentity = this.getOwner();
            if (entity == livingentity) {
                return true;
            }

            if (livingentity != null) {
                return livingentity.isAlliedTo(entity);
            }
        }

        return super.isAlliedTo(entity);
    }

    protected void reassessTameGoals() {
    }

    public boolean wantsToAttack(LivingEntity p_142018_1_, LivingEntity p_142018_2_) {
        return true;
    }
}
