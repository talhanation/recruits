package com.talhanation.recruits.entities;

import com.talhanation.recruits.entities.ai.*;
import jdk.nashorn.internal.ir.Block;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractRecruitEntity extends TameableEntity implements IAngerable {
    private static final DataParameter<Integer> DATA_REMAINING_ANGER_TIME = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> STATE = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> FOLLOW = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.INT);
    private static final DataParameter<Optional<BlockPos>> HOLD_POS = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Optional<BlockPos>> MOVE_POS = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Boolean> MOVE = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> LISTEN = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Optional<UUID>> MOUNT = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.OPTIONAL_UUID);
    private static final RangedInteger PERSISTENT_ANGER_TIME = TickRangeConverter.rangeOfSeconds(20, 39);
    private static final DataParameter<Integer> GROUP = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.INT);
    private UUID persistentAngerTarget;

    public AbstractRecruitEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.setOwned(false);
        this.xpReward = 6;
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public void aiStep() {
        super.aiStep();
    }

    public void tick() {
        super.tick();
        updateSwingTime();
        updateSwimming();
    }

    public void rideTick() {
        super.rideTick();
        if (this.getVehicle() instanceof CreatureEntity) {
            CreatureEntity creatureentity = (CreatureEntity)this.getVehicle();
            this.yBodyRot = creatureentity.yBodyRot;
        }

    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance diff, SpawnReason reason, @Nullable ILivingEntityData spawnData, @Nullable CompoundNBT nbt) {
        setRandomSpawnBonus();
        return spawnData;
    }
    public void setRandomSpawnBonus(){
        getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus", this.random.nextGaussian() * 0.10D, AttributeModifier.Operation.MULTIPLY_BASE));
        getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("attack_bonus", this.random.nextGaussian() * 0.10D, AttributeModifier.Operation.MULTIPLY_BASE));
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("knockback_bonus", this.random.nextGaussian() * 0.10D, AttributeModifier.Operation.MULTIPLY_BASE));
        getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier("speed_bonus", this.random.nextGaussian() * 0.10D, AttributeModifier.Operation.MULTIPLY_BASE));

    }

    public void setDropEquipment(){
        this.dropEquipment();
    }

    ////////////////////////////////////REGISTER////////////////////////////////////

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new RecruitUseShield(this));
        //this.goalSelector.addGoal(2, new RecruitMountGoal(this, 1.2D, 32.0F));
        this.goalSelector.addGoal(3, new RecruitMoveToPosGoal(this, 1.2D, 32.0F));
        this.goalSelector.addGoal(4, new RecruitFollowOwnerGoal(this, 1.2D, 9.0F, 3.0F));
        this.goalSelector.addGoal(5, new RecruitMeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(6, new RecruitHoldPosGoal(this, 1.0D, 32.0F));
        this.goalSelector.addGoal(7, new RecruitMoveTowardsTargetGoal(this, 1.15D, 24.0F));
        this.goalSelector.addGoal(8, new ReturnToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(9, new PatrolVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomWalkingGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(11, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(12, new LookRandomlyGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, (new RecruitHurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new RecruitRaidNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(4, new RecruitAggresiveNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllagerEntity.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, MonsterEntity.class, false));
        this.targetSelector.addGoal(8, new ResetAngerGoal<>(this, true));
        this.targetSelector.addGoal(10, new RecruitDefendVillageGoal(this));
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_REMAINING_ANGER_TIME, 0);
        this.entityData.define(GROUP, 0);
        this.entityData.define(FOLLOW, 0);
        this.entityData.define(STATE, 0);
        this.entityData.define(HOLD_POS, Optional.empty());
        this.entityData.define(MOVE_POS, Optional.empty());
        this.entityData.define(MOVE, true);
        this.entityData.define(LISTEN, true);
        this.entityData.define(MOUNT, Optional.empty());
        //STATE
        // 0=NEUTRAL
        // 1=AGGRESSIVE
        // 2= RAID

        //FOLLOW
        //0 = false
        //1 = true
        //2 = false, hold position

    }
    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);

        nbt.putInt("FollowState", this.getFollow());
        nbt.putInt("AggroState", this.getState());
        //nbt.putBoolean("Listen", this.getListen());

        this.getHoldPos().ifPresent((pos) -> {
            nbt.putInt("HoldPosX", pos.getX());
            nbt.putInt("HoldPosY", pos.getY());
            nbt.putInt("HoldPosZ", pos.getZ());
        });

        this.addPersistentAngerSaveData(nbt);
    }
    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("FollowState",1)) this.setFollow(nbt.getInt("FollowState"));
        if (nbt.contains("AggroState",1)) this.setState(nbt.getInt("AggroState"));
        //this.setListen(nbt.getBoolean("Listen"));

        if (nbt.contains("HoldPosX", 99) &&
            nbt.contains("HoldPosY", 99) &&
            nbt.contains("HoldPosZ", 99)) {
            BlockPos blockpos = new BlockPos(
                    nbt.getInt("HoldPosX"),
                    nbt.getInt("HoldPosY"),
                    nbt.getInt("HoldPosZ"));
            this.setHoldPos(blockpos);
        }

        if(!level.isClientSide)
            this.readPersistentAngerSaveData((ServerWorld)this.level, nbt);
    }


    ////////////////////////////////////GET////////////////////////////////////

    public int getState() {
        return entityData.get(STATE);
    }

    public int getGroup() {
        return entityData.get(GROUP);
    }

    public int getFollow(){
        return entityData.get(FOLLOW);
    }


    public SoundEvent getHurtSound(DamageSource ds) {
        if (this.isBlocking())
            return SoundEvents.SHIELD_BLOCK;
        return SoundEvents.VILLAGER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
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

    public Optional<BlockPos> getHoldPos(){
        return entityData.get(HOLD_POS);
    }

    @Nullable
    public BlockPos getMovePos(){
        return entityData.get(MOVE_POS).orElse(null);
    }

    public boolean getMove() {
        return entityData.get(MOVE);
    }

    public boolean getListen() {
        return entityData.get(LISTEN);
    }

    @Nullable
    public UUID getMount() {
        return entityData.get(MOUNT).orElse(null);
    }

    ////////////////////////////////////SET////////////////////////////////////

    public void setGroup(int group){
        entityData.set(GROUP, group);
    }

    public void setState(int state) {
        switch (state){
            case 0:
                setTarget(null);//wird nur 1x aufgerufen
                break;
            case 1:
                break;
            case 2:
                break;
        }
        entityData.set(STATE, state);
    }

    public void setFollow(int state){
        switch (state){
            case 0:
                break;
            case 1:
                break;
            case 2:
                setHoldPos(this.getOnPos());
                break;
        }
        entityData.set(FOLLOW, state);
    }

    public void setHoldPos(BlockPos holdPos){
        this.entityData.set(HOLD_POS, Optional.of(holdPos));
    }

    public void clearHoldPos(){
        this.entityData.set(HOLD_POS, Optional.empty());
    }

    public void setMovePos(BlockPos holdPos){
        this.entityData.set(MOVE_POS, Optional.of(holdPos));
    }

    public void clearMovePos(){
        this.entityData.set(MOVE_POS, Optional.empty());
    }

    public void setMove(boolean bool) {
        entityData.set(MOVE, bool);
    }

    public void setListen(boolean bool) {
        entityData.set(LISTEN, bool);
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

    public void setMount(UUID uuid){
        entityData.set(MOUNT, Optional.of(uuid));
    }

    ////////////////////////////////////ON FUNCTIONS////////////////////////////////////

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (this.level.isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isTame() || isInSittingPose() || item == Items.BONE && !this.isTame() && !this.isAngry();
            return flag ? ActionResultType.CONSUME : ActionResultType.PASS;
        } else {
            if (this.isTame() && player.getUUID().equals(this.getOwnerUUID())) {

                if (player.isCrouching()) {
                    int state = this.getState();

                    switch (state) {
                        case 0:
                            setState(1);
                            player.sendMessage(new StringTextComponent("I will stay Aggressive"), player.getUUID());
                            break;
                        case 1:
                            setState(2);
                            player.sendMessage(new StringTextComponent("I will Raid everything"), player.getUUID());
                             break;
                        case 2:
                            setState(0);
                            player.sendMessage(new StringTextComponent("I will be Neutral"), player.getUUID());
                            break;
                    }
                    return ActionResultType.SUCCESS;
                }
                if(!player.isCrouching()) {
                    int state = this.getFollow();
                    switch (state) {
                        case 0:
                            setFollow(1);
                            player.sendMessage(new StringTextComponent("I will follow you"), player.getUUID());
                            break;
                        case 1:
                            setFollow(2);
                            player.sendMessage(new StringTextComponent("Im will hold this Position"), player.getUUID());
                            break;
                        case 2:
                            setFollow(0);
                            player.sendMessage(new StringTextComponent("Im will stay here around"), player.getUUID());
                            break;
                    }
                    return ActionResultType.SUCCESS;
                }

            } else if (item == Items.EMERALD && !this.isAngry() && !this.isTame() && playerHasEnoughEmeralds(player)) {
                if (!player.abilities.instabuild) {
                    if (!player.isCreative()) {
                        itemstack.shrink(recruitCosts());
                    }
                }

                if (!net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(false);
                    this.setFollow(0);
                    this.setState(0);
                    this.level.broadcastEntityEvent(this, (byte)7);
                    return ActionResultType.SUCCESS;
                } else {
                    this.level.broadcastEntityEvent(this, (byte)6);
                }

                return ActionResultType.SUCCESS;
            }
            else if (item == Items.EMERALD && !this.isAngry() && !this.isTame() && !playerHasEnoughEmeralds(player)) {
                    player.sendMessage(new StringTextComponent("You need " + recruitCosts() + " Emeralds to recruit me!"), player.getUUID());
            }
            else if (!this.isTame() && item != Items.EMERALD ) {
                int i = this.random.nextInt(5);
                switch (i) {
                    case 0:
                        player.sendMessage(new StringTextComponent("I am a " + getRecruitName() + ". I'm here to keep things safe in these areas."), player.getUUID());
                        break;
                    case 1:
                        player.sendMessage(new StringTextComponent("Stay Safe, I'm here to protect you."), player.getUUID());
                        break;
                    case 2:
                        player.sendMessage(new StringTextComponent("Everyone needs a " + getRecruitName() + " like me, to have peace in these Lands."), player.getUUID());
                        break;
                        default:
                        player.sendMessage(new StringTextComponent("I am a " + getRecruitName() + " defending these areas from Monsters!"), player.getUUID());
                        break;
                }
            }
            return super.mobInteract(player, hand);
        }
    }

    private boolean playerHasEnoughEmeralds(PlayerEntity player) {
        int recruitCosts = this.recruitCosts();
        int emeraldCount = player.getItemInHand(Hand.MAIN_HAND).getCount();
        if (emeraldCount >= recruitCosts){
            return true;
        }
        if (player.isCreative()){
            return true;
        }
        else return false;
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
        int state = this.getState();
        switch (state) {
            case 0://NEUTRAL
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
                    if (target instanceof PlayerEntity) {
                        if (target.getUUID() == this.getOwnerUUID()){
                            return false;
                        }
                        else if (target.getTeam() == Objects.requireNonNull(this.getOwner()).getTeam()) {
                            return false;
                        }
                        else return true;
                    }
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
                } else if (target instanceof AbstractRecruitEntity) {
                    if (((AbstractRecruitEntity) target).getOwnerUUID() == this.getOwnerUUID()) {
                        return false;
                    }

                    if (target.getTeam() == Objects.requireNonNull(this.getOwner()).getTeam()) {
                        return false;
                    }

                } else if (target instanceof AbstractHorseEntity) {
                    if (((AbstractHorseEntity) target).isTamed()) {
                        return false;
                    }
                } else
                    return true;
                break;
            //case 3:
        }
    return false;
    }

    public void die(DamageSource dmg) {
        super.die(dmg);
    }

    public void clearTarget(){
        this.setTarget(null);
    }

    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    public boolean isOwnedByThisPlayer(AbstractRecruitEntity recruit, PlayerEntity player){
        return  (recruit.getOwnerUUID() == player.getUUID());
    }

    @Override
    public boolean canBeLeashed(PlayerEntity player) {
        return false;
    }
    public abstract int recruitCosts() ;

    public abstract String getRecruitName();

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
    /*
    @Override
    protected void hurtArmor(DamageSource p_230294_1_, float p_230294_2_) {
        super.hurtArmor(p_230294_1_, p_230294_2_);
    }
    */

    @Override
    protected void hurtCurrentlyUsedShield(float damage) {
        if (this.useItem.isShield(this)) {
            if (damage >= 3.0F) {
                int i = 1 + MathHelper.floor(damage);
                Hand hand = this.getUsedItemHand();
                this.useItem.hurtAndBreak(i, this, (entity) -> entity.broadcastBreakEvent(hand));
                if (this.useItem.isEmpty()) {
                    if (hand == Hand.MAIN_HAND) {
                        this.setItemSlot(EquipmentSlotType.MAINHAND, ItemStack.EMPTY);
                    } else {
                        this.setItemSlot(EquipmentSlotType.OFFHAND, ItemStack.EMPTY);
                    }
                    this.useItem = ItemStack.EMPTY;
                    this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
                }
            }
        }
    }
}
