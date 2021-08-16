package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRecruitEntity extends TameableEntity implements IAngerable {
    private static final DataParameter<Integer> DATA_REMAINING_ANGER_TIME = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> STATE = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> FOLLOW = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.BOOLEAN);
    private static final RangedInteger PERSISTENT_ANGER_TIME = TickRangeConverter.rangeOfSeconds(20, 39);
    private UUID persistentAngerTarget;



    public AbstractRecruitEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.setOwned(false);
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    @Override
    public void aiStep() {
        super.aiStep();
    }

    public void tick() {
        super.tick();
        updateSwingTime();
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance diff, SpawnReason reason, @Nullable ILivingEntityData spawnData, @Nullable CompoundNBT nbt) {
        getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("random bonus on spawn", this.random.nextGaussian() * 0.10D, AttributeModifier.Operation.MULTIPLY_BASE));
        setEquipment();
        setCanPickUpLoot(true);
        return spawnData;
    }


    ////////////////////////////////////REGISTER////////////////////////////////////

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new SitGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(4, new MoveTowardsTargetGoal(this, 1.0D, 32.0F));
        //this.goalSelector.addGoal(5, new RecruitFollowOwnerGoal(this, 1.3D, 7.F, 3.0F));
        this.goalSelector.addGoal(5, new RecruitFollowHero(this, 3.0F));
        this.goalSelector.addGoal(6, new ReturnToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(7, new PatrolVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomWalkingGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(10, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(10, new LookRandomlyGoal(this));

        this.targetSelector.addGoal(1, new DefendVillageGoal(this));
        this.targetSelector.addGoal(2, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(3, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new RecruitRaidNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(4, new RecruitAggresiveNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllagerEntity.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, MonsterEntity.class, false));
        this.targetSelector.addGoal(8, new ResetAngerGoal<>(this, true));
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_REMAINING_ANGER_TIME, 0);
        this.entityData.define(FOLLOW, false);
        this.entityData.define(STATE, 0);
        //STATE
        // 0=NEUTRAL
        // 1=AGGRESSIVE
        // 2= RAID

    }

    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        this.addPersistentAngerSaveData(nbt);
    }

    public void readAdditionalSaveData(CompoundNBT p_70037_1_) {
        super.readAdditionalSaveData(p_70037_1_);
        if(!level.isClientSide) //FORGE: allow this entity to be read from nbt on client. (Fixes MC-189565)
            this.readPersistentAngerSaveData((ServerWorld)this.level, p_70037_1_);
    }

    ////////////////////////////////////GET////////////////////////////////////

    public int getState() {
        return entityData.get(STATE);
    }

    protected SoundEvent getHurtSound(DamageSource dmg) {
        return SoundEvents.VILLAGER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    public boolean getFollow(){
       return entityData.get(FOLLOW);
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    protected float getStandingEyeHeight(Pose pos, EntitySize size) {
        return size.height * 0.9F;
    }

    public int getMaxHeadXRot() {
        return this.isInSittingPose() ? 20 : super.getMaxHeadXRot();
    }

    public int getMaxSpawnClusterSize() {
        return 8;
    }

    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(DATA_REMAINING_ANGER_TIME);
    }

    @Nullable
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    ////////////////////////////////////SET////////////////////////////////////

    public void setState(LivingEntity owner, int state) {

        switch (state){
            case 0:
                owner.sendMessage(new StringTextComponent("I will stay Neutral"), owner.getUUID());
                break;
            case 1:
                owner.sendMessage(new StringTextComponent("I will stay Aggressive"), owner.getUUID());
                break;
            case 2:
                owner.sendMessage(new StringTextComponent("I will Raid everything"), owner.getUUID());
                break;
        }

        entityData.set(STATE, state);
    }

    public void setFollow(boolean bool){
        LivingEntity owner = this.getOwner();
        if (bool){
            owner.sendMessage(new StringTextComponent("I will follow you"), owner.getUUID());
        }else
            owner.sendMessage(new StringTextComponent("Im will stay here around"), owner.getUUID());

        entityData.set(FOLLOW, bool);
    }

    public void setOwned(boolean owned) {
        super.setTame(owned);
    }

    public void setRemainingPersistentAngerTime(int time) {
        this.entityData.set(DATA_REMAINING_ANGER_TIME, time);
    }

    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.randomValue(this.random));
    }


    public void setPersistentAngerTarget(@Nullable UUID target) {
        this.persistentAngerTarget = target;
    }


    public void setEquipment(){}


    ////////////////////////////////////ON FUNCTIONS////////////////////////////////////

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (this.level.isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isTame() || isInSittingPose() || item == Items.BONE && !this.isTame() && !this.isAngry();
            return flag ? ActionResultType.CONSUME : ActionResultType.PASS;
        } else {
            if (this.isTame() && player.getUUID().equals(this.getOwnerUUID())) {

                if (!player.isCrouching()) {
                    int state = this.getState();

                    switch (state) {
                        case 0:
                            setState(player, 1);
                            break;
                        case 1:
                            setState(player, 2);
                            break;
                        case 2:
                            setState(player, 0);
                            break;
                    }
                    return ActionResultType.SUCCESS;

                    /*
                    if (!this.getStopFollow()) {
                        setStopFollow(true);

                    } else if (this.getStopFollow()) {
                        setStopFollow(false);

                    }*/
                }
                if(player.isCrouching()) {
                    /*
                    if (this.isInSittingPose()) {
                        setOrderedToSit(false);
                        player.sendMessage(new StringTextComponent("Im will stop Holding here"), player.getUUID());
                        return ActionResultType.SUCCESS;

                    } if (!this.isInSittingPose()) {
                        setOrderedToSit(true);
                        player.sendMessage(new StringTextComponent("Im will Holding here"), player.getUUID());
                        return ActionResultType.SUCCESS;
                    }
                    */
                    if (this.getFollow()) {
                        setFollow(false);
                        return ActionResultType.SUCCESS;

                    } if (!this.getFollow()) {
                        setFollow(true);
                        return ActionResultType.SUCCESS;
                    }
                }

            } else if (item == Items.EMERALD && !this.isAngry() && !this.isTame()) {
                if (!player.abilities.instabuild) {
                    itemstack.shrink(recruitCosts());
                }

                if (!net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(false);
                    this.setFollow(false);
                    this.setState(player, 0);
                    this.level.broadcastEntityEvent(this, (byte)7);
                    return ActionResultType.SUCCESS;
                } else {
                    this.level.broadcastEntityEvent(this, (byte)6);
                }

                return ActionResultType.SUCCESS;
            }

            return super.mobInteract(player, hand);
        }
    }

    public void onRKeyPressed(UUID player) {
        if (this.isTame() &&  Objects.equals(this.getOwnerUUID(), player)) {
            /*
            switch (state) {
                case 1:
            }*/

            this.setOrderedToSit(true);
            this.setInSittingPose(true);
            this.getOwner().sendMessage(new StringTextComponent("Recruits! Stop Following me!"), this.getOwnerUUID());
        }
    }


    public void onXKeyPressed(LivingEntity owner){
        if (this.isTame() && Objects.equals(this.getOwnerUUID(), owner.getUUID())){
            int state = getState();
            setState(owner, 2);
        }
    }


    ////////////////////////////////////ATTACK FUNCTIONS////////////////////////////////////

    public boolean hurt(DamageSource dmg, float amt) {
        if (this.isInvulnerableTo(dmg)) {
            return false;
        } else {
            Entity entity = dmg.getEntity();
            this.setOrderedToSit(false);
            if (entity != null && !(entity instanceof PlayerEntity) && !(entity instanceof AbstractArrowEntity)) {
                amt = (amt + 1.0F) / 2.0F;
            }

            return super.hurt(dmg, amt);
        }
    }

    public boolean doHurtTarget(Entity entity) {
        boolean flag = entity.hurt(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (flag) {
            this.doEnchantDamageEffects(this, entity);

        }

        return flag;
    }

    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        int state = getState();
        switch (state) {
            case 0:
                if (!(target instanceof CreeperEntity) && !(target instanceof GhastEntity)) {
                    if (target instanceof AbstractRecruitEntity) {
                        AbstractRecruitEntity abstractRecruitEntity = (AbstractRecruitEntity) target;
                        return !abstractRecruitEntity.isTame() || abstractRecruitEntity.getOwner() != owner;
                    } else if (target instanceof PlayerEntity && owner instanceof PlayerEntity && !((PlayerEntity) owner).canHarmPlayer((PlayerEntity) target)) {
                        return false;
                    } else if (target instanceof AbstractHorseEntity && ((AbstractHorseEntity) target).isTamed()) {
                        return false;
                    } else {
                        return !(target instanceof TameableEntity) || !((TameableEntity) target).isTame();
                    }
                }
                break;

            case 1: //AGGRESSIVE
                if (!(target instanceof CreeperEntity) && !(target instanceof GhastEntity)) {
                return false;
                }
                if (target instanceof PlayerEntity) {

                    if (target.getUUID() == this.getOwnerUUID()) {
                        return false;

                    } else if (target.getTeam() == Objects.requireNonNull(this.getOwner()).getTeam()) {
                        return false;
                    }
                    else
                        return true;
                }
                break;
            case 2: // RAID
                if (target instanceof PlayerEntity) {

                    if (target.getUUID() == this.getOwnerUUID()) {
                        return false;
                    }

                    if (target.getTeam() == Objects.requireNonNull(this.getOwner()).getTeam()) {
                        return false;
                    }
                }
                else if (target instanceof AbstractRecruitEntity){
                    if (target.getTeam() == Objects.requireNonNull(this.getOwner()).getTeam()) {
                        return false;
                    } else
                        return true;
                } else
                    return true;

        }
        return false;
    }

    public void die(DamageSource dmg) {
        super.die(dmg);
    }

    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    public boolean isOwnedByThisPlayer(AbstractRecruitEntity recruit, PlayerEntity player){
        return  (recruit.getOwnerUUID() == player.getUUID());
    }

    @Override
    public boolean canBeLeashed(PlayerEntity player) {
        return false;
    }

    public int recruitCosts() {
        return 1;
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    protected void spawnTamingParticles(boolean p_70908_1_) {
        IParticleData iparticledata = ParticleTypes.HAPPY_VILLAGER;
        if (!p_70908_1_) {
            iparticledata = ParticleTypes.SMOKE;
        }

        for(int i = 0; i < 7; ++i) {
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(iparticledata, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
        }

    }

}
