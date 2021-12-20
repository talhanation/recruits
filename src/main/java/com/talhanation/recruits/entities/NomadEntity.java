package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.AbstractIllagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;

public class NomadEntity extends BowmanEntity{

    public NomadEntity(EntityType<? extends AbstractRecruitEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT p_70037_1_) {
        super.readAdditionalSaveData(p_70037_1_);
        this.reassessWeaponGoal();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new RecruitFollowOwnerGoal(this, 1.2D, 7.F, 4.0F));
        this.goalSelector.addGoal(3, new RecruitMoveToPosGoal(this, 1.2D, 32.0F));
        this.goalSelector.addGoal(4, new RecruitHoldPosGoal(this, 1.0D, 32.0F));
        this.goalSelector.addGoal(5, new ReturnToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(6, new PatrolVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomWalkingGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(8, new ReturnToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(9, new PatrolVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(11, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(12, new LookRandomlyGoal(this));

        this.targetSelector.addGoal(1, new RecruitDefendVillageGoal(this));
        this.targetSelector.addGoal(2, (new RecruitHurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(3, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new RecruitRaidNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(4, new RecruitAggresiveNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllagerEntity.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, MonsterEntity.class, false));
        //this.targetSelector.addGoal(8, new ResetAngerGoal<>(this, true));
        this.targetSelector.addGoal(10, new RecruitDefendVillageGoal(this));
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
        if (nbt == null) nbt = new CompoundNBT();
        nbt.putString("id", "minecraft:horse");

        CompoundNBT passenger = new CompoundNBT();
        passenger.putString("id", "recruits:nomad");

        ListNBT passengers = new ListNBT();
        passengers.add(passenger);

        nbt.put("Passengers", passengers);

        ServerWorld serverworld = world.getLevel();
        HorseEntity horseentity = (HorseEntity) EntityType.loadEntityRecursive(nbt, serverworld, (entity) -> {
            entity.moveTo(this.getX(), this.getY(), this.getZ(), this.yRot, this.xRot);
            return entity;
        });

        this.remove();

        assert horseentity != null;
        horseentity.setTamed(true);
        serverworld.addFreshEntityWithPassengers(horseentity);

        NomadEntity that = (NomadEntity) horseentity.getPassengers().get(0);
        (that.getNavigation()).moveTo(this.getNavigation().getPath(), 1D);
        that.setEquipment();
        that.populateDefaultEquipmentEnchantments(difficultyInstance);
        that.setCanPickUpLoot(true);
        that.dropEquipment();
        that.reassessWeaponGoal();
        that.setGroup(2);

        return data;
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    public void setEquipment() {
        this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.BOW));
        this.setItemSlot(EquipmentSlotType.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        this.setItemSlot(EquipmentSlotType.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        this.setItemSlot(EquipmentSlotType.FEET, new ItemStack(Items.LEATHER_BOOTS));
    }
}
