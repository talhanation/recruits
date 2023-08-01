package com.talhanation.recruits.entities;
//ezgi&talha kantar

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.ai.*;
import com.talhanation.recruits.entities.ai.navigation.RecruitPathNavigation;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.inventory.DebugInvMenu;
import com.talhanation.recruits.inventory.RecruitHireMenu;
import com.talhanation.recruits.inventory.RecruitInventoryMenu;
import com.talhanation.recruits.network.MessageAddRecruitToTeam;
import com.talhanation.recruits.network.MessageDebugScreen;
import com.talhanation.recruits.network.MessageHireGui;
import com.talhanation.recruits.network.MessageRecruitGui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractRecruitEntity extends AbstractInventoryEntity{
    private static final EntityDataAccessor<Integer> DATA_REMAINING_ANGER_TIME = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FOLLOW_STATE = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SHOULD_FOLLOW = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_BLOCK = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_MOUNT = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_PROTECT = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_HOLD_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_MOVE_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> HOLD_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> MOVE_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> UPKEEP_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> LISTEN = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FOLLOWING = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> MOUNT_ID = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> PROTECT_ID = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> GROUP = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> XP = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KILLS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_EATING = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLEEING = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> HUNGER = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MORAL = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> OWNED = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> COST = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> UPKEEP_ID = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> mountTimer = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> UpkeepTimer = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);

    public int blockCoolDown;
    private boolean needsTeamUpdate = true;


    public AbstractRecruitEntity(EntityType<? extends AbstractInventoryEntity> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 6;
        this.navigation = this.createNavigation(world);
    }

    ///////////////////////////////////NAVIGATION/////////////////////////////////////////
    @NotNull
    protected PathNavigation createNavigation(@NotNull Level level) {
        return new RecruitPathNavigation(this, level);
    }

    public void rideTick() {
        super.rideTick();
    }

    public double getMyRidingOffset() {
        return -0.35D;
    }

    ///////////////////////////////////TICK/////////////////////////////////////////
    // @Override
    public void aiStep(){
        super.aiStep();
        updateSwingTime();
        updateShield();
        if(needsTeamUpdate) updateTeam();
    }

    public void tick() {
        super.tick();
        if(getMountTimer() > 0) setMountTimer(getMountTimer() - 1);
        if(getUpkeepTimer() > 0) setUpkeepTimer(getUpkeepTimer() - 1);
        if(getHunger() >=  70F && getHealth() < getMaxHealth()){
            this.heal(1.0F/50F);// 1 hp in 2.5s
        }
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag nbt) {
        this.setRandomSpawnBonus();
        this.createNavigation(world.getLevel());
        return spawnData;
    }
    public void setRandomSpawnBonus(){
        getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus", this.random.nextDouble() * 0.5D, AttributeModifier.Operation.MULTIPLY_BASE));
        getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("attack_bonus", this.random.nextDouble() * 0.5D, AttributeModifier.Operation.MULTIPLY_BASE));
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("knockback_bonus", this.random.nextDouble() * 0.1D, AttributeModifier.Operation.MULTIPLY_BASE));
        getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier("speed_bonus", this.random.nextDouble() * 0.1D, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    public void setDropEquipment(){
        this.dropEquipment();
    }

    ////////////////////////////////////REGISTER////////////////////////////////////

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new RecruitFloatGoal(this));
        this.goalSelector.addGoal(1, new RecruitQuaffGoal(this));
        this.goalSelector.addGoal(1, new FleeTNT(this));
        this.goalSelector.addGoal(1, new FleeFire(this));
        this.goalSelector.addGoal(6, new OpenDoorGoal(this, true) {
        });
        this.goalSelector.addGoal(1, new RecruitProtectEntityGoal(this));

        //this.goalSelector.addGoal(0, new (this));


        this.goalSelector.addGoal(1, new RecruitEatGoal(this));
        this.goalSelector.addGoal(5, new RecruitUpkeepPosGoal(this));
        this.goalSelector.addGoal(6, new RecruitUpkeepEntityGoal(this));
        this.goalSelector.addGoal(3, new RecruitMountEntity(this));
        this.goalSelector.addGoal(4, new RecruitMoveToPosGoal(this, 1.05D));
        this.goalSelector.addGoal(2, new RecruitFollowOwnerGoal(this, 1.05D, RecruitsModConfig.RecruitFollowStartDistance.get()));
        this.goalSelector.addGoal(2, new RecruitMeleeAttackGoal(this, 1.05D, false, this.getMeleeStartRange()));
        this.goalSelector.addGoal(7, new RecruitHoldPosGoal(this, 1.0D, 32.0F));
        this.goalSelector.addGoal(8, new RecruitMoveTowardsTargetGoal(this, 1.15D, (float) this.getMeleeStartRange()));
        //this.goalSelector.addGoal(7, new RecruitDodgeGoal(this));

        this.goalSelector.addGoal(9, new MoveBackToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(10, new GolemRandomStrollInVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        //this.goalSelector.addGoal(13, new RecruitPickupWantedItemGoal(this));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target) -> {
            return (this.getState() == 2 && this.canAttack(target));
        }));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (target) -> {
            return (this.getState() == 1 && this.canAttack(target));
        }));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractRecruitEntity.class, 10, true, false, (target) -> {
            return (this.getState() == 1 && this.canAttack(target));
        }));

        this.targetSelector.addGoal(0, new RecruitProtectHurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new RecruitOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new PatrolLeaderTargetAttackers(this));
        this.targetSelector.addGoal(3, (new RecruitHurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(4, new RecruitOwnerHurtTargetGoal(this));

        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, 10, true, false, (target) -> {
            return (this.getState() != 3);
        }));

        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, (target) -> {
            return this.canAttack(target) && !(target instanceof Creeper) && (this.getState() != 3);
        }));
        this.targetSelector.addGoal(7, new RecruitDefendVillageGoal(this));

        this.updateTeam();
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_REMAINING_ANGER_TIME, 0);
        this.entityData.define(GROUP, 0);
        this.entityData.define(SHOULD_FOLLOW, false);
        this.entityData.define(SHOULD_BLOCK, false);
        this.entityData.define(SHOULD_MOUNT, false);
        this.entityData.define(SHOULD_PROTECT, false);
        this.entityData.define(SHOULD_HOLD_POS, false);
        this.entityData.define(SHOULD_MOVE_POS, false);
        this.entityData.define(FLEEING, false);
        this.entityData.define(STATE, 0);
        this.entityData.define(VARIANT, 0);
        this.entityData.define(XP, 0);
        this.entityData.define(KILLS, 0);
        this.entityData.define(LEVEL, 1);
        this.entityData.define(FOLLOW_STATE, 0);
        this.entityData.define(HOLD_POS, Optional.empty());
        this.entityData.define(UPKEEP_POS, Optional.empty());
        this.entityData.define(MOVE_POS, Optional.empty());
        this.entityData.define(LISTEN, true);
        this.entityData.define(MOUNT_ID, Optional.empty());
        this.entityData.define(PROTECT_ID, Optional.empty());
        this.entityData.define(IS_FOLLOWING, false);
        this.entityData.define(IS_EATING, false);
        this.entityData.define(HUNGER, 50F);
        this.entityData.define(MORAL, 50F);
        this.entityData.define(OWNER_ID, Optional.empty());
        this.entityData.define(UPKEEP_ID, Optional.empty());
        this.entityData.define(OWNED, false);
        this.entityData.define(COST, 1);
        this.entityData.define(mountTimer, 0);
        this.entityData.define(UpkeepTimer, 0);

        //STATE
        // 0 = NEUTRAL
        // 1 = AGGRESSIVE
        // 2 = RAID
        // 3 = PASSIVE

        //FOLLOW
        //0 = wander
        //1 = follow
        //2 = hold position
        //3 = back to position
        //4 = hold my position
        //5 = Protect

    }
    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("AggroState", this.getState());
        nbt.putInt("FollowState", this.getFollowState());
        nbt.putBoolean("ShouldFollow", this.getShouldFollow());
        nbt.putBoolean("ShouldMount", this.getShouldMount());
        nbt.putBoolean("ShouldProtect", this.getShouldProtect());
        nbt.putBoolean("ShouldBlock", this.getShouldBlock());
        nbt.putInt("Group", this.getGroup());
        nbt.putInt("Variant", this.getVariant());
        nbt.putBoolean("Listen", this.getListen());
        nbt.putBoolean("Fleeing", this.getFleeing());
        nbt.putBoolean("isFollowing", this.isFollowing());
        nbt.putBoolean("isEating", this.getIsEating());
        nbt.putInt("Xp", this.getXp());
        nbt.putInt("Level", this.getXpLevel());
        nbt.putInt("Kills", this.getKills());
        nbt.putFloat("Hunger", this.getHunger());
        nbt.putFloat("Moral", this.getMoral());
        nbt.putBoolean("isOwned", this.getIsOwned());
        nbt.putInt("Cost", this.getCost());
        nbt.putInt("mountTimer", this.getMountTimer());
        nbt.putInt("upkeepTimer", this.getUpkeepTimer());

        if(this.getHoldPos() != null){
            nbt.putInt("HoldPosX", this.getHoldPos().getX());
            nbt.putInt("HoldPosY", this.getHoldPos().getY());
            nbt.putInt("HoldPosZ", this.getHoldPos().getZ());
            nbt.putBoolean("ShouldHoldPos", this.getShouldHoldPos());
        }

        if(this.getMovePos() != null){
            nbt.putInt("MovePosX", this.getMovePos().getX());
            nbt.putInt("MovePosY", this.getMovePos().getY());
            nbt.putInt("MovePosZ", this.getMovePos().getZ());
            nbt.putBoolean("ShouldMovePos", this.getShouldMovePos());
        }

        if(this.getOwnerUUID() != null){
            nbt.putUUID("OwnerUUID", this.getOwnerUUID());
        }

        if(this.getMountUUID() != null){
            nbt.putUUID("MountUUID", this.getMountUUID());
        }

        if(this.getProtectUUID() != null){
            nbt.putUUID("ProtectUUID", this.getProtectUUID());
        }

        if(this.getUpkeepUUID() != null){
            nbt.putUUID("UpkeepUUID", this.getUpkeepUUID());
        }

        if(this.getUpkeepPos() != null){
            nbt.putInt("UpkeepPosX", this.getUpkeepPos().getX());
            nbt.putInt("UpkeepPosY", this.getUpkeepPos().getY());
            nbt.putInt("UpkeepPosZ", this.getUpkeepPos().getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setXpLevel(nbt.getInt("Level"));
        this.setState(nbt.getInt("AggroState"));
        this.setFollowState(nbt.getInt("FollowState"));
        this.setShouldFollow(nbt.getBoolean("ShouldFollow"));
        this.setShouldMount(nbt.getBoolean("ShouldMount"));
        this.setShouldBlock(nbt.getBoolean("ShouldBlock"));
        this.setShouldProtect(nbt.getBoolean("ShouldProtect"));
        this.setFleeing(nbt.getBoolean("Fleeing"));
        this.setGroup(nbt.getInt("Group"));
        this.setListen(nbt.getBoolean("Listen"));
        this.setIsFollowing(nbt.getBoolean("isFollowing"));
        this.setIsEating(nbt.getBoolean("isEating"));
        this.setXp(nbt.getInt("Xp"));
        this.setKills(nbt.getInt("Kills"));
        this.setVariant(nbt.getInt("Variant"));
        this.setHunger(nbt.getFloat("Hunger"));
        this.setMoral(nbt.getFloat("Moral"));
        this.setIsOwned(nbt.getBoolean("isOwned"));
        this.setCost(nbt.getInt("Cost"));
        this.setMountTimer(nbt.getInt("mountTimer"));
        this.setUpkeepTimer(nbt.getInt("UpkeepTimer"));


        if (nbt.contains("HoldPosX") && nbt.contains("HoldPosY") && nbt.contains("HoldPosZ")) {
            this.setShouldHoldPos(nbt.getBoolean("ShouldHoldPos"));
            this.setHoldPos(new BlockPos (
                    nbt.getInt("HoldPosX"),
                    nbt.getInt("HoldPosY"),
                    nbt.getInt("HoldPosZ")));
        }

        if (nbt.contains("MovePosX") && nbt.contains("MovePosY") && nbt.contains("MovePosZ")) {
            this.setShouldMovePos(nbt.getBoolean("ShouldMovePos"));
            this.setMovePos(new BlockPos (
                    nbt.getInt("MovePosX"),
                    nbt.getInt("MovePosY"),
                    nbt.getInt("MovePosZ")));
        }

        if (nbt.contains("OwnerUUID")){
            Optional<UUID> uuid = Optional.of(nbt.getUUID("OwnerUUID"));
            this.setOwnerUUID(uuid);
        }

        if (nbt.contains("ProtectUUID")){
            Optional<UUID> uuid = Optional.of(nbt.getUUID("ProtectUUID"));
            this.setProtectUUID(uuid);
        }

        if (nbt.contains("MountUUID")){
            Optional<UUID> uuid = Optional.of(nbt.getUUID("MountUUID"));
            this.setMountUUID(uuid);
        }

        if (nbt.contains("UpkeepUUID")){
            Optional<UUID> uuid = Optional.of(nbt.getUUID("UpkeepUUID"));
            this.setUpkeepUUID(uuid);
        }

        if (nbt.contains("UpkeepPosX") && nbt.contains("UpkeepPosY") && nbt.contains("UpkeepPosZ")) {
            this.setUpkeepPos(new BlockPos (
                    nbt.getInt("UpkeepPosX"),
                    nbt.getInt("UpkeepPosY"),
                    nbt.getInt("UpkeepPosZ")));
        }
    }

    ////////////////////////////////////GET////////////////////////////////////

    public int getUpkeepTimer(){
        return this.entityData.get(UpkeepTimer);
    }

    public int getVariant() {
        return entityData.get(VARIANT);
    }
    public int getBlockCoolDown(){
        return 200;
    }
    public UUID getUpkeepUUID(){
        return  this.entityData.get(UPKEEP_ID).orElse(null);
    }
    public BlockPos getUpkeepPos(){
        return entityData.get(UPKEEP_POS).orElse(null);
    }

    @Nullable
    public Player getOwner(){
        if (this.isOwned() && this.getOwnerUUID() != null){
            UUID ownerID = this.getOwnerUUID();
            return level.getPlayerByUUID(ownerID);
        }
        else
            return null;
    }

    public UUID getOwnerUUID(){
        return  this.entityData.get(OWNER_ID).orElse(null);
    }

    public UUID getProtectUUID(){
        return  this.entityData.get(PROTECT_ID).orElse(null);
    }

    public UUID getMountUUID(){
        return  this.entityData.get(MOUNT_ID).orElse(null);
    }

    public boolean getIsOwned() {
        return entityData.get(OWNED);
    }

    public float getMoral() {
        return this.entityData.get(MORAL);
    }

    public float getHunger() {
        return this.entityData.get(HUNGER);
    }
    public float getAttackDamage(){
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public float getMovementSpeed(){
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    public boolean getFleeing() {
        return entityData.get(FLEEING);
    }

    public int getKills() {
        return entityData.get(KILLS);
    }

    public int getXpLevel() {
        return entityData.get(LEVEL);
    }

    public int getXp() {
        return entityData.get(XP);
    }

    public boolean getIsEating() {
        return entityData.get(IS_EATING);
    }

    public boolean getShouldMovePos() {
        return entityData.get(SHOULD_MOVE_POS);
    }
    public boolean getShouldHoldPos() {
        return entityData.get(SHOULD_HOLD_POS);
    }

    public boolean getShouldMount() {
        return entityData.get(SHOULD_MOUNT);
    }

    public boolean getShouldProtect() {
        return entityData.get(SHOULD_PROTECT);
    }

    public boolean getShouldFollow() {
        return entityData.get(SHOULD_FOLLOW);
    }

    public boolean getShouldBlock() {
        return entityData.get(SHOULD_BLOCK);
    }

    public boolean isFollowing(){
        return entityData.get(IS_FOLLOWING);
    }

    public int getState() {
        return entityData.get(STATE);
    }
    //STATE
    // 0 = NEUTRAL
    // 1 = AGGRESSIVE
    // 2 = RAID
    // 3 = PASSIVE

    public int getGroup() {
        return entityData.get(GROUP);
    }


    //FOLLOW
    //0 = wander
    //1 = follow
    //2 = hold position
    //3 = back to position
    //4 = hold my position
    //5 = Protect
    public int getFollowState(){
        return entityData.get(FOLLOW_STATE);
    }

    public SoundEvent getHurtSound(@NotNull DamageSource ds) {
        if (this.isBlocking())
            return SoundEvents.SHIELD_BLOCK;
        return RecruitsModConfig.RecruitsLookLikeVillagers.get() ? SoundEvents.VILLAGER_HURT : SoundEvents.GENERIC_HURT;
    }

    protected SoundEvent getDeathSound() {
        return RecruitsModConfig.RecruitsLookLikeVillagers.get() ? SoundEvents.VILLAGER_DEATH : SoundEvents.GENERIC_DEATH;
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    protected float getStandingEyeHeight(@NotNull Pose pos, EntityDimensions size) {
        return size.height * 0.9F;
    }

    public int getMaxHeadXRot() {
        return super.getMaxHeadXRot();
    }

    public int getMaxSpawnClusterSize() {
        return 8;
    }

    public BlockPos getHoldPos(){
        return entityData.get(HOLD_POS).orElse(null);
    }

    @Nullable
    public BlockPos getMovePos(){
        return entityData.get(MOVE_POS).orElse(null);
    }

    public boolean getListen() {
        return entityData.get(LISTEN);
    }

    @Nullable
    public LivingEntity getProtectingMob(){
        List<LivingEntity> list = this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(32D));
        for(LivingEntity living : list){
            if (this.getProtectUUID() != null && living.getUUID().equals(this.getProtectUUID()) && living.isAlive()){
                return living;
            }
        }
        return null;
    }

    ////////////////////////////////////SET////////////////////////////////////

    public void setUpkeepTimer(int x){
        this.entityData.set(UpkeepTimer, x);

    }
    public void setVariant(int variant){
        entityData.set(VARIANT, variant);
    }
    public void setUpkeepUUID(Optional<UUID> id) {
        this.entityData.set(UPKEEP_ID, id);
    }
    public void setCost(int cost){
        entityData.set(COST, cost);
    }
    public void setUpkeepPos(BlockPos pos){
        this.entityData.set(UPKEEP_POS, Optional.of(pos));
    }

    public void setIsOwned(boolean bool){
        entityData.set(OWNED, bool);
    }

    public void setOwnerUUID(Optional<UUID> id) {
        this.entityData.set(OWNER_ID,id);
    }

    public void setProtectUUID(Optional<UUID> id) {
        this.entityData.set(PROTECT_ID, id);
    }

    public void setMountUUID(Optional<UUID> id) {
        this.entityData.set(MOUNT_ID, id);
    }

    public void setMoral(float value) {
        this.entityData.set(MORAL, value);
        this.applyMoralEffects();
    }

    public void setHunger(float value) {
        this.entityData.set(HUNGER, value);
    }

    public void setFleeing(boolean bool){
        entityData.set(FLEEING, bool);
    }
    public void setMountTimer(int x){
        entityData.set(mountTimer, x);
    }

    public void disband(Player player){
        String name = this.getName().getString();
        player.sendSystemMessage(TEXT_DISBAND(name));
        this.setTarget(null);
        this.setIsOwned(false);
        this.setUpkeepPos(BlockPos.ZERO);
        this.setUpkeepUUID(Optional.empty());
        this.setOwnerUUID(Optional.empty());
        CommandEvents.saveRecruitCount(player, CommandEvents.getSavedRecruitCount(player) - 1);

        this.recalculateCost();
        if (this.getTeam() != null) Main.SIMPLE_CHANNEL.sendToServer(new MessageAddRecruitToTeam(this.getTeam().getName(), -1));

        this.updateTeam();
    }


    public void addXpLevel(int level){
        int currentLevel = this.getXpLevel();
        int newLevel = currentLevel + level;

        if(newLevel > RecruitsModConfig.RecruitsMaxXpLevel.get()){
            newLevel = RecruitsModConfig.RecruitsMaxXpLevel.get();
        }
        else{
            this.makeLevelUpSound();
            this.addLevelBuffs();
        }

        this.entityData.set(LEVEL, newLevel);
    }

    public void setKills(int kills){
        this.entityData.set(KILLS, kills);
    }

    public void setXpLevel(int XpLevel){
        this.entityData.set(LEVEL, XpLevel);
    }

    public void setXp(int xp){
        this. entityData.set(XP, xp);
    }

    public void addXp(int xp){
        int currentXp = this.getXp();
        int newXp = currentXp + xp;

        this. entityData.set(XP, newXp);
    }

    public void setIsEating(boolean bool){
        entityData.set(IS_EATING, bool);
    }

    public void setShouldHoldPos(boolean bool){
        entityData.set(SHOULD_HOLD_POS, bool);
    }

    public void setShouldMovePos(boolean bool){
        entityData.set(SHOULD_MOVE_POS, bool);
    }
    public void setShouldProtect(boolean bool){
        entityData.set(SHOULD_PROTECT, bool);
    }

    public void setShouldMount(boolean bool){
        entityData.set(SHOULD_MOUNT, bool);
    }

    public void setShouldFollow(boolean bool){
            entityData.set(SHOULD_FOLLOW, bool);
    }

    public void setShouldBlock(boolean bool){
        entityData.set(SHOULD_BLOCK, bool);
    }

    public void setIsFollowing(boolean bool){
        entityData.set(IS_FOLLOWING, bool);
    }

    public void setGroup(int group){
        entityData.set(GROUP, group);
    }

    public void setState(int state) {
        switch (state){
            case 0:
            case 3:
                setTarget(null);//wird nur 1x aufgerufen
                break;
            case 1:
                break;
            case 2:
                setFollowState(0);
                break;
        }
        entityData.set(STATE, state);
    }

    //STATE
    // 0 = NEUTRAL
    // 1 = AGGRESSIVE
    // 2 = RAID
    // 3 = PASSIVE

    //FOLLOW
    //0 = wander
    //1 = follow
    //2 = hold position
    //3 = back to position
    //4 = hold my position
    //5 = Protect

    public void setFollowState(int state){
        switch (state) {
            case 0 -> {
                setShouldFollow(false);
                setShouldHoldPos(false);
                setShouldProtect(false);
                setShouldMovePos(false);
            }
            case 1 -> {
                setShouldFollow(true);
                setShouldHoldPos(false);
                setShouldProtect(false);
                setShouldMovePos(false);
            }
            case 2 -> {
                setShouldFollow(false);
                setShouldHoldPos(true);
                clearHoldPos();
                setHoldPos(getOnPos());
                setShouldProtect(false);
                setShouldMovePos(false);
            }
            case 3 -> {
                setShouldFollow(false);
                setShouldHoldPos(true);
                setShouldProtect(false);
                setShouldMovePos(false);
            }
            case 4 -> {
                setShouldFollow(false);
                setShouldHoldPos(true);
                clearHoldPos();
                setHoldPos(this.getOwner().blockPosition());
                setShouldProtect(false);
                setShouldMovePos(false);
                state = 3;
            }
            case 5 -> {
                setShouldFollow(false);
                setShouldHoldPos(false);
                setShouldProtect(true);
                setShouldMovePos(false);
            }
        }

        this.entityData.set(FOLLOW_STATE, state);
    }

    public void setHoldPos(BlockPos holdPos){
        this.entityData.set(HOLD_POS, Optional.of(holdPos));
    }
    public void setMovePos(BlockPos holdPos){
        this.entityData.set(MOVE_POS, Optional.of(holdPos));
    }

    public void clearHoldPos(){
        this.entityData.set(HOLD_POS, Optional.empty());
    }

    public void clearMovePos(){
        this.entityData.set(MOVE_POS, Optional.empty());
    }

    public void setListen(boolean bool) {
        entityData.set(LISTEN, bool);
    }

    public void setEquipment(){
        this.setHandEquipment(getHandEquipment());
        //Armor
        List<String> armor = RecruitsModConfig.StartArmorList.get();
        List<ItemStack> itemStackArmor = new ArrayList<>(Arrays.asList(new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.LEATHER_CHESTPLATE), new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.LEATHER_BOOTS)));
        List<EquipmentSlot> equipmentslot = new ArrayList<>(Arrays.asList(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET));

        for(int i = 0; i < armor.size(); i++){
            String str = armor.get(i);
            Optional<Holder<Item>> holder = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(str));
            if (holder.isPresent()){
                this.setItemSlot(equipmentslot.get(i), holder.get().value().getDefaultInstance());
            }
        }
    }

    public double getMeleeStartRange() {
        return 32D;
    }

    public abstract List<String> getHandEquipment();

    public void setHandEquipment(List<String> hand) {
        if(!hand.get(0).isEmpty()){
            String str = hand.get(0);
            Optional<Holder<Item>> holder = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(str));
            holder.ifPresent(itemHolder -> this.setItemSlot(EquipmentSlot.MAINHAND, itemHolder.value().getDefaultInstance()));
        }
        else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        }

        if(!hand.get(1).isEmpty()){
            String str = hand.get(1);
            Optional<Holder<Item>> holder = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(str));
            holder.ifPresent(itemHolder -> this.setItemSlot(EquipmentSlot.OFFHAND, itemHolder.value().getDefaultInstance()));
        }
        else {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    public void initSpawn(){
        this.setVariant(random.nextInt(3));
    }

    ////////////////////////////////////is FUNCTIONS////////////////////////////////////

    public boolean isEffectedByCommand(UUID player_uuid, int group){
        return (this.isOwned() && (this.getListen()) && Objects.equals(this.getOwnerUUID(), player_uuid) && (this.getGroup() == group || group == 0));
    }
    public boolean isOwned(){
        return getIsOwned();
    }

    public boolean isOwnedBy(Player player){
       return player.getUUID() == this.getOwnerUUID() || player == this.getOwner();
    }

    ////////////////////////////////////ON FUNCTIONS////////////////////////////////////

    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        String name = this.getName().getString();
        boolean isPlayerTarget = this.getTarget() != null && getTarget().equals(player);

        if(isPlayerTarget) return InteractionResult.PASS;

        if (this.level.isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isOwned() || !this.isOwned();
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else {
            if (player.isCreative() && player.getItemInHand(hand).getItem().equals(ModItems.RECRUIT_SPAWN_EGG.get())){
                openDebugScreen(player);
                return InteractionResult.SUCCESS;
            }
            if ((this.isOwned() && player.getUUID().equals(this.getOwnerUUID()))) {
                if (player.isCrouching()) {
                    this.openGUI(player);
                    this.navigation.stop();
                    return InteractionResult.SUCCESS;
                }
                if(!player.isCrouching()) {

                    this.setUpkeepTimer(this.getUpkeepCooldown());
                    if(this.getShouldMount()) this.setShouldMount(false);

                    int state = this.getFollowState();
                    switch (state) {
                        default -> {
                            setFollowState(1);
                            player.sendSystemMessage(TEXT_FOLLOW(name));
                        }
                        case 1 -> {
                            setFollowState(4);
                            player.sendSystemMessage(TEXT_HOLD_YOUR_POS(name));
                        }
                        case 3 -> {
                            setFollowState(0);
                            player.sendSystemMessage(TEXT_WANDER(name));
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
            }

            else if (!this.isOwned() && CommandEvents.playerCanRecruit(player) && !isPlayerTarget) {

                this.openHireGUI(player);
                this.dialogue(name, player);
                this.navigation.stop();
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }
    }

    public boolean hire(Player player) {
        String name = this.getName().getString() + ": ";
        if (!CommandEvents.playerCanRecruit(player)) {

            player.sendSystemMessage(INFO_RECRUITING_MAX(name));
            return false;
        }
        else {
            this.makeHireSound();

            this.setOwnerUUID(Optional.of(player.getUUID()));
            this.setIsOwned(true);
            this.navigation.stop();
            this.setTarget(null);
            this.setFollowState(2);
            this.setState(0);
            this.updateTeam();

            int i = this.random.nextInt(4);
            switch (i) {
                case 1 -> {

                    player.sendSystemMessage(TEXT_RECRUITED1(name));
                }
                case 2 -> {
                    player.sendSystemMessage(TEXT_RECRUITED2(name));
                }
                case 3 -> {
                    player.sendSystemMessage(TEXT_RECRUITED3(name));
                }
            }
        }
        int currentRecruits = CommandEvents.getSavedRecruitCount(player);
        CommandEvents.saveRecruitCount(player,  currentRecruits + 1);
        return true;
    }

    public void dialogue(String name, Player player) {
        int i = this.random.nextInt(4);
        switch (i) {
            case 1 -> {
                player.sendSystemMessage(TEXT_HELLO_1(name));
            }
            case 2 -> {
                player.sendSystemMessage(TEXT_HELLO_2(name));
            }
            case 3 -> {
                player.sendSystemMessage(TEXT_HELLO_3(name));
            }
        }
    }

    ////////////////////////////////////ATTACK FUNCTIONS////////////////////////////////////

    public boolean hurt(@NotNull DamageSource dmg, float amt) {
        if (this.isInvulnerableTo(dmg)) {
            return false;
        } else {
            Entity entity = dmg.getEntity();
            if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractArrow)) {
                amt = (amt + 1.0F) / 2.0F;
            }
            if(this.getMoral() > 0) this.setMoral(this.getMoral() - 0.25F);
            if(isBlocking()) hurtCurrentlyUsedShield(amt);

            if(entity instanceof LivingEntity living && RecruitEvents.canDamageTarget(this, living)){
                if(this.getTarget() != null){
                    double d1 = this.distanceToSqr(this.getTarget());
                    double d2 = this.distanceToSqr(living);

                    if(d2 < d1) this.setTarget(living);
                }
                else
                    this.setTarget(living);

                if(this.getShouldProtect() && this.getProtectingMob() instanceof AbstractRecruitEntity patrolLeader){
                    patrolLeader.setTarget(living);
                }
            }
            return super.hurt(dmg, amt);
        }
    }

    public boolean doHurtTarget(@NotNull Entity entity) {
        float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (entity instanceof LivingEntity) {
            f += EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity)entity).getMobType());
        }

        int i = EnchantmentHelper.getFireAspect(this);
        if (i > 0) {
            entity.setSecondsOnFire(i * 4);
        }

        boolean flag = entity.hurt(this.damageSources().mobAttack(this), f);
        if (flag) {

            this.doEnchantDamageEffects(this, entity);
            this.setLastHurtMob(entity);
        }

        this.addXp(1);
        if(this.getHunger() > 0) this.setHunger(this.getHunger() - 0.1F);
        this.checkLevel();
        if(this.getMoral() < 100) this.setMoral(this.getMoral() + 0.25F);
        this.damageMainHandItem();
        return true;
    }

    public void addLevelBuffs(){
        int level = getXpLevel();
        if(level <= 10){
            getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 2D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("attack_bonus_level", 0.05D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("knockback_bonus_level", 0.0025D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier("speed_bonus_level", 0.005D, AttributeModifier.Operation.ADDITION));
        }
        if(level > 10){
            getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 2D, AttributeModifier.Operation.ADDITION));
        }
    }

    public void addLevelBuffsForLevel(int level){

        for(int i = 0; i < level; i++) {
            if (level <= 10) {
                getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 2D, AttributeModifier.Operation.ADDITION));
                getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("attack_bonus_level", 0.05D, AttributeModifier.Operation.ADDITION));
                getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("knockback_bonus_level", 0.0025D, AttributeModifier.Operation.ADDITION));
                getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier("speed_bonus_level", 0.005D, AttributeModifier.Operation.ADDITION));}
            if (level > 10) {
                getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 2D, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    /*
           .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    */

    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Player player && owner instanceof Player && !((Player)owner).canHarmPlayer((Player)target)) {
            return isValidTargetPlayer(player);

        } else if (target instanceof AbstractHorse && ((AbstractHorse)target).isTamed()) {
            return false;
        //} else if (target instanceof AbstractOrderAbleEntity && ((AbstractOrderAbleEntity)target).getIsInOrder() && ((AbstractOrderAbleEntity)target).getOwner() != owner) {
        //    return true;
        } else {
            //return !(target instanceof TamableAnimal) || !((TamableAnimal)target).isTame();
            return isValidTarget(target);
        }
    }

    public void die(DamageSource dmg) {
        net.minecraft.network.chat.Component deathMessage = this.getCombatTracker().getDeathMessage();
        super.die(dmg);
        LivingEntity owner = this.getOwner();
        if (owner instanceof Player player){
            CommandEvents.saveRecruitCount(player, CommandEvents.getSavedRecruitCount(player) - 1);
        }

        if (this.dead) {
            if (!this.level.isClientSide && this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && this.getOwner() instanceof ServerPlayer) {
                this.getOwner().sendSystemMessage(deathMessage);
            }
        }
    }

    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    public void updateMoral(){
        //fast recovery
        if (isStarving() && this.isOwned()){
            if(getMoral() > 0) setMoral((getMoral() - 2F));
        }

        if (this.isOwned() && !isSaturated()){
            if(getMoral() > 35) setMoral((getMoral() - 1F));
        }

        if(this.isSaturated() || getHealth() >= getMaxHealth() * 0.85){
            if(getMoral() < 65) setMoral((getMoral() + 2F));
        }
    }

    public void applyMoralEffects(){
        boolean confused =  0 <= getMoral() && getMoral() < 20;
        boolean lowMoral =  20 <= getMoral() && getMoral() < 40;
        boolean highMoral =  90 <= getMoral() && getMoral() <= 100;

        if (confused) {
            if (!this.hasEffect(MobEffects.WEAKNESS))
                this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 3, false, false, true));
            if (!this.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, false, true));
            if (!this.hasEffect(MobEffects.CONFUSION))
                this.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1, false, false, true));
        }

        if (lowMoral) {
            if (!this.hasEffect(MobEffects.WEAKNESS))
                this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, false, true));
            if (!this.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, false, true));
        }

        if (highMoral) {
            if (!this.hasEffect(MobEffects.DAMAGE_BOOST))
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, false, true));
            if (!this.hasEffect(MobEffects.DAMAGE_RESISTANCE))
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, false, false, true));
        }
    }

    public void updateHunger(){
        if(getHunger() > 0) {
            setHunger((getHunger() - 10F));
        }
        this.updateMoral();
    }

    public boolean needsToGetFood(){
        boolean isChest = this.getUpkeepPos() != null;
        boolean isEntity = this.getUpkeepUUID() != null;
        return (this.getUpkeepTimer() == 0 && this.needsToEat() && (isChest || isEntity));
    }

    public boolean needsToEat(){
        //Main.LOGGER.debug(getHunger());
        if (getHunger() <= 50F){
            return true;
        }
        else if (getHunger() <= 70F && getHealth() != getMaxHealth() && this.getTarget() == null && this.getIsOwned()){
            return true;
        }
        else return getHealth() <= (getMaxHealth() * 0.30) && this.getTarget() == null;
    }

    public boolean needsToPotion(){
        LivingEntity target = this.getTarget();
        if(target != null){
            return getHealth() <= (getMaxHealth() * 0.60) || target.getHealth() > this.getHealth();
        }
        return false;
    }

    public boolean isStarving(){
        return (getHunger() <= 1F );
    }

    public boolean isSaturated(){
        return (getHunger() >= 90F);
    }

    public void checkLevel(){
        int currentXp = this.getXp();
        if (currentXp >= RecruitsModConfig.RecruitsMaxXpForLevelUp.get()){
            this.addXpLevel(1);
            this.setXp(0);
            this.heal(10F);
            this.recalculateCost();

            if(this.getMoral() < 100)
                this.setMoral(getMoral() + 5F);
        }
    }

    private void recalculateCost() {
        int currCost = getCost();
        int armorBonus = this.getArmorValue() * 2;
        //Main.LOGGER.debug("armorBonus: " + armorBonus);

        int weaponBonus = 4;
        //Main.LOGGER.debug("weaponBonus: " + weaponBonus);

        int speedBonus = (int) (this.getSpeed() * 2);
        //Main.LOGGER.debug("speedBonus: " + speedBonus);

        int shieldBonus = this.getOffhandItem().getItem() instanceof ShieldItem ? 10 : 0;
        //Main.LOGGER.debug("shieldBonus: " + shieldBonus);

        int newCost = Math.abs((shieldBonus + speedBonus + weaponBonus + armorBonus + currCost + getXpLevel() * 2));
        this.setCost(newCost);
    }

    public void makeLevelUpSound() {
        if(RecruitsModConfig.RecruitsLookLikeVillagers.get())
            this.playSound(SoundEvents.VILLAGER_YES, 15.0F, 0.8F + 0.4F * this.random.nextFloat());

        this.playSound(SoundEvents.PLAYER_LEVELUP, 15.0F, 0.8F + 0.4F * this.random.nextFloat());
    }

    public void makeHireSound() {
        if(RecruitsModConfig.RecruitsLookLikeVillagers.get())
            this.playSound(SoundEvents.VILLAGER_AMBIENT, 15.0F, 0.8F + 0.4F * this.random.nextFloat());
    }

    @Override
    public boolean canBeLeashed(@NotNull Player player) {
        return false;
    }

    public int getCost(){
        return entityData.get(COST);
    }

    protected void hurtArmor(@NotNull DamageSource damageSource, float damage) {
        ItemStack headArmor = this.getItemBySlot(EquipmentSlot.HEAD);
        boolean hasHeadArmor = !headArmor.isEmpty();
        //Main.LOGGER.debug("headArmor :" + headArmor);
        //Main.LOGGER.debug("hasHeadArmor: " + hasHeadArmor);
        if (((!(damageSource.is(DamageTypes.IN_FIRE) && (damageSource.is(DamageTypes.ON_FIRE))) || !headArmor.getItem().isFireResistant()) && headArmor.getItem() instanceof ArmorItem)){
        //damage
            headArmor.hurtAndBreak(1, this, (p_43296_) -> {
                p_43296_.broadcastBreakEvent(EquipmentSlot.HEAD);
            });
        }

        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && hasHeadArmor) {
            this.inventory.setItem(0, ItemStack.EMPTY);
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequip(EquipmentSlot.HEAD);
        }

        ItemStack chestArmor = this.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasChestArmor = !chestArmor.isEmpty();
        if (((!(damageSource.is(DamageTypes.IN_FIRE) && (damageSource.is(DamageTypes.ON_FIRE))) || !chestArmor.getItem().isFireResistant()) && chestArmor.getItem() instanceof ArmorItem)){
            //damage
            chestArmor.hurtAndBreak(1, this, (p_43296_) -> {
                p_43296_.broadcastBreakEvent(EquipmentSlot.CHEST);
            });
        }
        if (this.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && hasChestArmor) {
            this.inventory.setItem(1, ItemStack.EMPTY);
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequip(EquipmentSlot.CHEST);
        }

        ItemStack legsArmor = this.getItemBySlot(EquipmentSlot.LEGS);
        boolean hasLegsArmor = !legsArmor.isEmpty();

        if (((!(damageSource.is(DamageTypes.IN_FIRE) && (damageSource.is(DamageTypes.ON_FIRE))) || !legsArmor.getItem().isFireResistant()) && legsArmor.getItem() instanceof ArmorItem)){
            //damage
            legsArmor.hurtAndBreak(1, this, (p_43296_) -> {
                p_43296_.broadcastBreakEvent(EquipmentSlot.LEGS);
            });
        }
        if (this.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && hasLegsArmor) {
            this.inventory.setItem(2, ItemStack.EMPTY);
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequip(EquipmentSlot.LEGS);
        }


        ItemStack feetArmor = this.getItemBySlot(EquipmentSlot.FEET);
        boolean hasFeetArmor = !feetArmor.isEmpty();

        if (((!(damageSource.is(DamageTypes.IN_FIRE) && (damageSource.is(DamageTypes.ON_FIRE))) || !feetArmor.getItem().isFireResistant()) && feetArmor.getItem() instanceof ArmorItem)){
            //damage
            feetArmor.hurtAndBreak(1, this, (p_43296_) -> {
                p_43296_.broadcastBreakEvent(EquipmentSlot.FEET);
            });

        }
        if (this.getItemBySlot(EquipmentSlot.FEET).isEmpty() && hasFeetArmor) {
            this.inventory.setItem(3, ItemStack.EMPTY);
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequip(EquipmentSlot.FEET);
        }

    }

    public void damageMainHandItem() {
        //dont know why the fuck i cant assign this mainhand slot to inventory slot 4
        //therefor i need to make this twice
        ItemStack handItem = this.getItemBySlot(EquipmentSlot.MAINHAND);
        boolean hasHandItem = !handItem.isEmpty();
        /*//Fixes damage duplication
        this.getMainHandItem().hurtAndBreak(1, this, (p_43296_) -> {
            p_43296_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
        });
         */
        this.inventory.getItem(5).hurtAndBreak(1, this, (p_43296_) -> {
            p_43296_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
        });

        if (this.getMainHandItem().isEmpty() && hasHandItem) {
            this.inventory.setItem(5, ItemStack.EMPTY);
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequip(EquipmentSlot.MAINHAND);
        }
    }

    public void tryToReequip(EquipmentSlot equipmentSlot){
        for(int i = 6; i < 15; i++){
            ItemStack itemStack = this.getInventory().getItem(i);
            if(canEquipItemToSlot(itemStack, equipmentSlot)) {
                this.setItemSlot(equipmentSlot, itemStack);
                this.inventory.setItem(getInventorySlotIndex(equipmentSlot), itemStack);
                this.inventory.removeItemNoUpdate(i);
                Equipable equipable = Equipable.get(itemStack);
                if(equipable != null)
                    this.level.playSound(null, this.getX(), this.getY(), this.getZ(), equipable.getEquipSound(), this.getSoundSource(), 1.0F, 1.0F);
            }
        }
    }

    public void tryToReequipShield(){
        for(ItemStack itemStack : this.getInventory().items){
            if(itemStack.getItem() instanceof ShieldItem){
                this.setItemSlot(EquipmentSlot.OFFHAND, itemStack);
                this.inventory.setItem(getInventorySlotIndex(EquipmentSlot.OFFHAND), itemStack);
                Equipable equipable = Equipable.get(itemStack);
                if(equipable != null)
                    this.level.playSound(null, this.getX(), this.getY(), this.getZ(), equipable.getEquipSound(), this.getSoundSource(), 1.0F, 1.0F);

                itemStack.shrink(1);
            }
        }
    }

    @Override
    public boolean wasKilled(@NotNull ServerLevel level, @NotNull LivingEntity living) {
        super.wasKilled(level, living);

        this.addXp(5);
        this.setKills(this.getKills() + 1);
        if(this.getMoral() < 100) this.setMoral(this.getMoral() + 1);

        if(living instanceof Player){
            this.addXp(45);
            if(this.getMoral() < 100) this.setMoral(this.getMoral() + 9);
        }

        if(living instanceof Raider){
            this.addXp(5);
            if(this.getMoral() < 100) this.setMoral(this.getMoral() + 2);
        }

        if(living instanceof Villager villager){
            if (villager.isBaby()) if(this.getMoral() > 0) this.setMoral(this.getMoral() - 10);
            else {
                if (this.getMoral() > 0) this.setMoral(this.getMoral() - 2);
            }
        }

        if(living instanceof WitherBoss){
            this.addXp(99);
            if(this.getMoral() < 100) this.setMoral(this.getMoral() + 9);
        }

        if(living instanceof IronGolem){
            this.addXp(49);
            if(this.getMoral() > 0) this.setMoral(this.getMoral() - 1);
        }

        if(living instanceof EnderDragon){
            this.addXp(999);
            if(this.getMoral() < 100) this.setMoral(this.getMoral() + 49);
        }

        this.checkLevel();
        return true;
    }

    @Override
    protected void blockUsingShield(@NotNull LivingEntity living) {
        super.blockUsingShield(living);
        if (living.getMainHandItem().canDisableShield(this.useItem, this, living))
            this.disableShield();
    }

    public void disableShield() {
            this.blockCoolDown = this.getBlockCoolDown();
            this.stopUsingItem();
            this.level.broadcastEntityEvent(this, (byte) 30);
    }

    public boolean canBlock(){
        return this.blockCoolDown == 0;
    }

    public void updateShield(){
        if(this.blockCoolDown > 0){
            this.blockCoolDown--;
        }
    }


    public int getMountTimer() {
        return entityData.get(mountTimer);
    }

    @Override
    protected void hurtCurrentlyUsedShield(float damage) {
        //dont know why the fuck i cant assign this offhand slot to inventory slot 4
        //therefor i need to make this twice
        this.getOffhandItem().hurtAndBreak(1, this, (p_43296_) -> {
            p_43296_.broadcastBreakEvent(EquipmentSlot.OFFHAND);
        });
        this.inventory.getItem(4).hurtAndBreak(1, this, (p_43296_) -> {
            p_43296_.broadcastBreakEvent(EquipmentSlot.OFFHAND);
        });
        if (this.getOffhandItem().isEmpty()) {
            this.inventory.setItem(4, ItemStack.EMPTY);
            this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequipShield();
        }
    }

    @Override
    public void openGUI(Player player) {

        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return getName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new RecruitInventoryMenu(i, AbstractRecruitEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageRecruitGui(player, this.getUUID()));
        }
    }

    public void openDebugScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new DebugInvMenu(i, AbstractRecruitEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugScreen(player, this.getUUID()));
        }
    }

    public boolean isValidTarget(LivingEntity living){
        boolean notAllowed = living instanceof AbstractFish || living instanceof Squid || living instanceof AbstractHorse;

        if (living instanceof AbstractRecruitEntity otherRecruit) {
            if (otherRecruit.isOwned() && this.isOwned()){
                UUID recruitOwnerUuid = this.getOwnerUUID();
                UUID otherRecruitOwnerUuid = otherRecruit.getOwnerUUID();

                if(otherRecruit.getTeam() != null && this.getTeam() != null){
                    return !otherRecruit.getTeam().isAlliedTo(this.getTeam());
                }
                else if(recruitOwnerUuid != null && otherRecruitOwnerUuid != null){
                    return !recruitOwnerUuid.equals(otherRecruitOwnerUuid);
                }
                else if(otherRecruit.getProtectUUID() != null && this.getProtectUUID() != null){
                    return !otherRecruit.getProtectUUID().equals(this.getProtectUUID());
                }
            }
            else if(otherRecruit.getProtectUUID() != null && this.getProtectUUID() != null){
                return !otherRecruit.getProtectUUID().equals(this.getProtectUUID());
            }
            else
                return RecruitEvents.canHarmTeam(this, living);
            return false;
        }
        return !notAllowed && !RecruitsModConfig.TargetBlackList.get().contains(living.getEncodeId());
    }

    public boolean isValidTargetPlayer(Player player){
        if(player.getUUID().equals(this.getOwnerUUID())) {
            return false;
        }
        else
            return RecruitEvents.canHarmTeam(this, player);
    }

    @Override
    public boolean canAttack(@Nonnull LivingEntity target) {
        if (target.canBeSeenAsEnemy() && target.isAlive()){
            if (target instanceof Player player){
                return this.isValidTargetPlayer(player);
            }
            else
                return isValidTarget(target);
        }
        return false;
    }

    public void updateTeam(){
        if(this.isOwned()){
            if(getOwner() != null) {
                Team team = this.getTeam();
                Team ownerTeam = this.getOwner().getTeam();
                if (team == ownerTeam) {
                    needsTeamUpdate = false;
                    return;
                }
                else if (ownerTeam == null) {
                    String teamName = team.getName();
                    PlayerTeam recruitTeam = this.level.getScoreboard().getPlayerTeam(teamName);
                    this.level.getScoreboard().removePlayerFromTeam(this.getStringUUID(), recruitTeam);
                    needsTeamUpdate = false;
                }
                else {
                    String ownerTeamName = ownerTeam.getName();
                    PlayerTeam playerteam = this.level.getScoreboard().getPlayerTeam(ownerTeamName);


                    boolean flag = playerteam != null && this.level.getScoreboard().addPlayerToTeam(this.getStringUUID(), playerteam);
                    if (!flag) {
                        Main.LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", ownerTeamName);
                    } else
                        this.setTarget(null);// fix "if owner was other team and now same team und was target"

                    needsTeamUpdate = false;
                }

            }
        }
        else{
            Team team = this.getTeam();
            if(team != null){
                PlayerTeam recruitTeam = this.level.getScoreboard().getPlayerTeam(team.getName());
                this.level.getScoreboard().removePlayerFromTeam(this.getStringUUID(), recruitTeam);
                needsTeamUpdate = false;
            }
        }
    }

    public void openHireGUI(Player player) {
        this.navigation.stop();

        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new RecruitHireMenu(i, playerInventory.player, AbstractRecruitEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageHireGui(player, this.getUUID()));
        }
    }

    public void shouldMount(boolean should, UUID mount_uuid) {
        if (!this.isPassenger()){
            this.setShouldMount(should);
            if(mount_uuid != null) {
                this.setMountUUID(Optional.of(mount_uuid));
            }
            else
                this.setMountUUID(Optional.empty());
        }
    }

    public void shouldProtect(boolean should, UUID protect_uuid) {
        this.setShouldProtect(should);
        if(protect_uuid != null) this.setProtectUUID(Optional.of(protect_uuid));
        else this.setProtectUUID(Optional.empty());
    }

    public void clearMount() {
        this.setShouldMount(false);
        this.setMountUUID(Optional.empty());
    }

    public int getUpkeepCooldown() {
        return 3000;
    }

    public static enum ArmPose {
        ATTACKING,
        BLOCKING,
        BOW_AND_ARROW,
        CROSSBOW_HOLD,
        CROSSBOW_CHARGE,
        CELEBRATING,
        NEUTRAL;
    }

    public AbstractRecruitEntity.ArmPose getArmPose() {
        return AbstractRecruitEntity.ArmPose.NEUTRAL;
    }

    private MutableComponent TEXT_RECRUITED1(String name) {
        return Component.translatable("chat.recruits.text.recruited1", name);
    }

    private MutableComponent TEXT_RECRUITED2(String name) {
        return Component.translatable("chat.recruits.text.recruited2", name);
    }

    private MutableComponent TEXT_RECRUITED3(String name) {
        return Component.translatable("chat.recruits.text.recruited3", name);
    }

    private Component INFO_RECRUITING_MAX(String name) {
        return Component.translatable("chat.recruits.info.reached_max", name);
    }

    private MutableComponent TEXT_DISBAND(String name) {
        return Component.translatable("chat.recruits.text.disband", name);
    }

    private MutableComponent TEXT_WANDER(String name) {
        return Component.translatable("chat.recruits.text.wander", name);
    }

    private MutableComponent TEXT_HOLD_YOUR_POS(String name) {
        return Component.translatable("chat.recruits.text.holdPos", name);
    }

    private MutableComponent TEXT_FOLLOW(String name) {
        return Component.translatable("chat.recruits.text.follow", name);
    }

    private MutableComponent TEXT_HELLO_1(String name) {
        return Component.translatable("chat.recruits.text.hello_1", name);
    }

    private MutableComponent TEXT_HELLO_2(String name) {
        return Component.translatable("chat.recruits.text.hello_2", name);
    }

    private MutableComponent TEXT_HELLO_3(String name) {
        return Component.translatable("chat.recruits.text.hello_3", name);
    }

}
