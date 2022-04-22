package com.talhanation.recruits.entities;


import com.talhanation.recruits.entities.ai.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;


public class BowmanEntity extends RecruitEntity implements IRangedAttackMob {

    public BowmanEntity(EntityType<? extends AbstractRecruitEntity> entityType, World world) {
        super(entityType, world);
        this.reassessWeaponGoal();
    }
    @Override
    public void readAdditionalSaveData(CompoundNBT p_70037_1_) {
        super.readAdditionalSaveData(p_70037_1_);
        this.reassessWeaponGoal();
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

    @Override
    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance difficultyInstance, SpawnReason reason, @Nullable ILivingEntityData data, @Nullable CompoundNBT nbt) {
        ILivingEntityData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigator)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);
        this.initSpawn();
        return ilivingentitydata;
    }

    @Override
    public void setEquipment() {
        this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.BOW));
        this.setItemSlot(EquipmentSlotType.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        this.setItemSlot(EquipmentSlotType.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        this.setItemSlot(EquipmentSlotType.FEET, new ItemStack(Items.LEATHER_BOOTS));

        inventory.setItem(12, new ItemStack(Items.LEATHER_CHESTPLATE));
        inventory.setItem(13, new ItemStack(Items.LEATHER_LEGGINGS));
        inventory.setItem(14, new ItemStack(Items.LEATHER_BOOTS));
        inventory.setItem(9, new ItemStack(Items.BOW));
    }

    @Override
    public void initSpawn() {
        this.setCustomName(new StringTextComponent("Bowman"));
        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
        this.reassessWeaponGoal();
        this.setGroup(2);
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack) {
        return !(itemStack.getItem() instanceof SwordItem ||  itemStack.getItem() instanceof ShieldItem);
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    @Override
    public void performRangedAttack(LivingEntity entity, float f) {
        ItemStack itemstack = this.getProjectile(this.getItemInHand(ProjectileHelper.getWeaponHoldingHand(this, Items.BOW)));
        AbstractArrowEntity abstractarrowentity = this.getArrow(itemstack, f);
        if (this.getMainHandItem().getItem() instanceof BowItem)
            abstractarrowentity = ((BowItem)this.getMainHandItem().getItem()).customArrow(abstractarrowentity);
        double d0 = entity.getX() - this.getX();
        double d1 = entity.getY(0.25D) - abstractarrowentity.getY();
        double d2 = entity.getZ() - this.getZ();
        double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
                                                        //angle                 //force     //accuracy 0 = 100%
        abstractarrowentity.shoot(d0, d1 + d3 * (double)0.196F, d2, 1.75F, (float)(0));
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.4F));
        this.level.addFreshEntity(abstractarrowentity);
    }

    protected AbstractArrowEntity getArrow(ItemStack stack, float f) {
        return ProjectileHelper.getMobArrow(this, stack, f);
    }

    public void reassessWeaponGoal() {
        if (this.level != null && !this.level.isClientSide) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);
            ItemStack itemstack = this.getItemInHand(ProjectileHelper.getWeaponHoldingHand(this, Items.BOW));
            if (itemstack.getItem() == Items.BOW) {
                int i = 20;
                if (this.level.getDifficulty() != Difficulty.HARD) {
                    i = 40;
                }

                this.bowGoal.setMinAttackInterval(i - 10);
                this.goalSelector.addGoal(3, this.bowGoal);
            } else {
                this.goalSelector.addGoal(4, this.meleeGoal);
            }
        }
    }

    private final RecruitRangedBowAttackGoal<BowmanEntity> bowGoal = new RecruitRangedBowAttackGoal<>(this, 1.2D, 10, 20, 32.0F);
    private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.2D, false) {
        public void stop() {
            super.stop();
            BowmanEntity.this.setAggressive(false);
        }

        public void start() {
            super.start();
            BowmanEntity.this.setAggressive(true);
        }
    };

    @Override
    public int recruitCosts() {
        return 4;
    }

    public void fleeEntity(LivingEntity target) {
        if (target != null) {
            double fleeDistance = 10.0D;
            Vector3d vecTarget = new Vector3d(target.getX(), target.getY(), target.getZ());
            Vector3d vecBowman = new Vector3d(this.getX(), this.getY(), this.getZ());
            Vector3d fleeDir = vecBowman.subtract(vecTarget);
            fleeDir = fleeDir.normalize();
            double rnd = this.random.nextGaussian() * 1.2;
            Vector3d fleePos = new Vector3d(vecBowman.x + rnd + fleeDir.x * fleeDistance, vecBowman.y + fleeDir.y * fleeDistance, vecBowman.z + rnd + fleeDir.z * fleeDistance);
            this.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.2D);
        }
    }
}
