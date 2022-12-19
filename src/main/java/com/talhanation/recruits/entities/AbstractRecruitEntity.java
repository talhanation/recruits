package com.talhanation.recruits.entities;
//ezgi&talha kantar

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.ai.*;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.inventory.DebugInvContainer;
import com.talhanation.recruits.inventory.RecruitHireContainer;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.MessageDebugScreen;
import com.talhanation.recruits.network.MessageHireGui;
import com.talhanation.recruits.network.MessageRecruitGui;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractRecruitEntity extends AbstractInventoryEntity{
    private static final EntityDataAccessor<Integer> DATA_REMAINING_ANGER_TIME = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FOLLOW_STATE = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SHOULD_FOLLOW = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_BLOCK = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_MOUNT = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_ESCORT = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_HOLD_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_MOVE_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> HOLD_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> MOVE_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> UPKEEP_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> LISTEN = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FOLLOWING = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> MOUNT_ID = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> ESCORT_ID = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
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
    public int blockCoolDown;
    public int eatCoolDown;


    public AbstractRecruitEntity(EntityType<? extends AbstractInventoryEntity> entityType, Level world) {
        super(entityType, world);
        this.setIsOwned(false);
        this.xpReward = 6;
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public void aiStep(){
        super.aiStep();
        updateMoral();
        updateShield();
    }

    public void tick() {
        super.tick();
        updateSwingTime();
        updateSwimming();
        updateHunger();
        updateTeam();

        //Main.LOGGER.debug("OwnerUUID: " + this.getOwnerUUID());
        //Main.LOGGER.debug("Owner: " + this.getOwner());




        //if (getOwner() != null)
        //this.getOwner().sendMessage(new TextComponent("Last Hurt: " + hurtMarked), getOwner().getUUID());


    }

    public void rideTick() {
        super.rideTick();
        /*
        if (this.getVehicle() instanceof CreatureEntity) {
            CreatureEntity creatureentity = (CreatureEntity)this.getVehicle();
            this.yBodyRot = creatureentity.yBodyRot;
        }
        */

    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag nbt) {
        setRandomSpawnBonus();
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
        this.goalSelector.addGoal(0, new RecruitQuaffGoal(this));
        this.goalSelector.addGoal(0, new FleeTNT(this));
        this.goalSelector.addGoal(0, new FleeFire(this));
        this.goalSelector.addGoal(1, new RecruitEscortEntityGoal(this));
        //this.goalSelector.addGoal(0, new (this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RecruitEatGoal(this));
        this.goalSelector.addGoal(1, new RecruitUpkeepGoal(this));
        this.goalSelector.addGoal(2, new RecruitMountEntity(this));
        //this.goalSelector.addGoal(2, new RecruitMountGoal(this, 1.2D, 32.0F));
        this.goalSelector.addGoal(3, new RecruitMoveToPosGoal(this, 1.2D));
        this.goalSelector.addGoal(4, new RecruitFollowOwnerGoal(this, 1.2D, this.getFollowStartDistance(), 3.0F));
        this.goalSelector.addGoal(5, new RecruitMeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(6, new RecruitHoldPosGoal(this, 1.0D, 32.0F));
        this.goalSelector.addGoal(7, new RecruitMoveTowardsTargetGoal(this, 1.15D, 32.0F));
        this.goalSelector.addGoal(8, new RecruitPickupWantedItemGoal(this));
        this.goalSelector.addGoal(9, new MoveBackToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(10, new GolemRandomStrollInVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target) -> {
            return (this.getState() == 2 && this.canAttack(target));
        }));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (target) -> {
            return (this.getState() == 1 && this.canAttack(target));
        }));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractRecruitEntity.class, 10, true, false, (target) -> {
            return (this.getState() == 1 && this.canAttack(target));
        }));

        this.targetSelector.addGoal(0, new RecruitOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(0, new PatrolLeaderTargetAttackers(this));
        this.targetSelector.addGoal(1, (new RecruitHurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(3, new RecruitOwnerHurtTargetGoal(this));

        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, 10, true, false, (target) -> {
            return (this.getState() != 3);
        }));

        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, (target) -> {
            return this.canAttack(target) && !(target instanceof Creeper) && (this.getState() != 3);
        }));
        this.targetSelector.addGoal(10, new RecruitDefendVillageGoal(this));
    }

    protected double getFollowStartDistance(){
        return RecruitsModConfig.RecruitFollowStartDistance.get();
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_REMAINING_ANGER_TIME, 0);
        this.entityData.define(GROUP, 0);
        this.entityData.define(SHOULD_FOLLOW, false);
        this.entityData.define(SHOULD_BLOCK, false);
        this.entityData.define(SHOULD_MOUNT, false);
        this.entityData.define(SHOULD_ESCORT, false);
        this.entityData.define(SHOULD_HOLD_POS, false);
        this.entityData.define(SHOULD_MOVE_POS, false);
        this.entityData.define(FLEEING, false);
        this.entityData.define(STATE, 0);
        this.entityData.define(XP, 0);
        this.entityData.define(KILLS, 0);
        this.entityData.define(LEVEL, 1);
        this.entityData.define(FOLLOW_STATE, 0);
        this.entityData.define(HOLD_POS, Optional.empty());
        this.entityData.define(UPKEEP_POS, Optional.empty());
        this.entityData.define(MOVE_POS, Optional.empty());
        this.entityData.define(LISTEN, true);
        this.entityData.define(MOUNT_ID, Optional.empty());
        this.entityData.define(ESCORT_ID, Optional.empty());
        this.entityData.define(IS_FOLLOWING, false);
        this.entityData.define(IS_EATING, false);
        this.entityData.define(HUNGER, 50F);
        this.entityData.define(MORAL, 50F);
        this.entityData.define(OWNER_ID, Optional.empty());
        this.entityData.define(OWNED, false);
        this.entityData.define(COST, 1);

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
        //5 = Escort

    }
    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("AggroState", this.getState());
        nbt.putBoolean("ShouldFollow", this.getShouldFollow());
        nbt.putBoolean("ShouldMount", this.getShouldMount());
        nbt.putBoolean("ShouldEscort", this.getShouldEscort());
        nbt.putBoolean("ShouldBlock", this.getShouldBlock());
        nbt.putInt("Group", this.getGroup());
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

        if(this.getEscortUUID() != null){
            nbt.putUUID("EscortUUID", this.getEscortUUID());
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
        this.setShouldFollow(nbt.getBoolean("ShouldFollow"));
        this.setShouldMount(nbt.getBoolean("ShouldMount"));
        this.setShouldBlock(nbt.getBoolean("ShouldBlock"));
        this.setShouldEscort(nbt.getBoolean("ShouldEscort"));
        this.setFleeing(nbt.getBoolean("Fleeing"));
        this.setGroup(nbt.getInt("Group"));
        this.setListen(nbt.getBoolean("Listen"));
        this.setIsFollowing(nbt.getBoolean("isFollowing"));
        this.setIsEating(nbt.getBoolean("isEating"));
        this.setXp(nbt.getInt("Xp"));
        this.setKills(nbt.getInt("Kills"));
        this.setHunger(nbt.getFloat("Hunger"));
        this.setMoral(nbt.getFloat("Moral"));
        this.setIsOwned(nbt.getBoolean("isOwned"));
        this.setCost(nbt.getInt("Cost"));

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

        if (nbt.contains("EscortUUID")){
            Optional<UUID> uuid = Optional.of(nbt.getUUID("EscortUUID"));
            this.setEscortUUID(uuid);
        }

        if (nbt.contains("MountUUID")){
            Optional<UUID> uuid = Optional.of(nbt.getUUID("MountUUID"));
            this.setMountUUID(uuid);
        }

        if (nbt.contains("UpkeepPosX") && nbt.contains("UpkeepPosY") && nbt.contains("UpkeepPosZ")) {
            this.setUpkeepPos(new BlockPos (
                    nbt.getInt("UpkeepPosX"),
                    nbt.getInt("UpkeepPosY"),
                    nbt.getInt("UpkeepPosZ")));
        }
    }

    public void setCost(int cost){
        entityData.set(COST, cost);
    }


    ////////////////////////////////////GET////////////////////////////////////

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

    public UUID getEscortUUID(){
        return  this.entityData.get(ESCORT_ID).orElse(null);
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

    public boolean getShouldEscort() {
        return entityData.get(SHOULD_ESCORT);
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
    //5 = Escort
    public int getFollowState(){
        return entityData.get(FOLLOW_STATE);
    }

    public SoundEvent getHurtSound(@NotNull DamageSource ds) {
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

    /*
    public ItemStack getOffHandItemSave(){
        return  entityData.get(OFFHAND_ITEM_SAVE);
    }
     */

    ////////////////////////////////////SET////////////////////////////////////

    public void setUpkeepPos(BlockPos pos){
        this.entityData.set(UPKEEP_POS, Optional.of(pos));
        Main.LOGGER.debug("setUpkeepPos: " + this.getUpkeepPos());
    }

    public void setIsOwned(boolean bool){
        entityData.set(OWNED, bool);
    }

    public void setOwnerUUID(Optional<UUID> id) {
        this.entityData.set(OWNER_ID,id);
    }

    public void setEscortUUID(Optional<UUID> id) {
        this.entityData.set(ESCORT_ID, id);
    }

    public void setMountUUID(Optional<UUID> id) {
        this.entityData.set(MOUNT_ID, id);
    }

    public void setMoral(float value) {
        this.entityData.set(MORAL, value);
    }

    public void setHunger(float value) {
        this.entityData.set(HUNGER, value);
    }

    public void setFleeing(boolean bool){
        entityData.set(FLEEING, bool);
    }

    public void disband(Player player){
        String name = this.getName().getString() + ": ";
        String disband = TEXT_DISBAND.getString();
        player.sendMessage(new TextComponent(name + disband), player.getUUID());
        this.setTarget(null);
        this.setIsOwned(false);
        this.setOwnerUUID(Optional.empty());
        CommandEvents.saveRecruitCount(player, CommandEvents.getSavedRecruitCount(player) - 1);
        this.recalculateCost();
    }

    public void addXpLevel(int level){
        int currentLevel = this.getXpLevel();
        int newLevel = currentLevel + level;
        makeLevelUpSound();
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
    public void setShouldEscort(boolean bool){
        entityData.set(SHOULD_ESCORT, bool);
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
                setTarget(null);//wird nur 1x aufgerufen
                break;
            case 1:
                break;
            case 2:
                setFollowState(0);
                break;
            case 3:
                setTarget(null);//wird nur 1x aufgerufen
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
    //5 = Escort

    public void setFollowState(int state){
        switch (state) {
            case 0 -> {
                setShouldFollow(false);
                setShouldHoldPos(false);
                setShouldEscort(false);
            }
            case 1 -> {
                setShouldFollow(true);
                setShouldHoldPos(false);
                setShouldEscort(false);
            }
            case 2 -> {
                setShouldFollow(false);
                setShouldHoldPos(true);
                clearHoldPos();
                setHoldPos(getOnPos());
                setShouldEscort(false);
            }
            case 3 -> {
                setShouldFollow(false);
                setShouldHoldPos(true);
                setShouldEscort(false);
            }
            case 4 -> {
                setShouldFollow(false);
                setShouldHoldPos(true);
                clearHoldPos();
                setHoldPos(this.getOwner().blockPosition());
                setShouldEscort(false);
                state = 3;
            }
            case 5 -> {
                setShouldFollow(false);
                setShouldHoldPos(false);
                setShouldEscort(true);
            }
        }
        entityData.set(FOLLOW_STATE, state);
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

    public void setEquipment(){}

    public void initSpawn(){
        this.setCanPickUpLoot(true);
        this.recalculateCost();
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
        String name = this.getName().getString() + ": ";
        boolean isPlayerTarget = this.getTarget() != null && getTarget().equals(player);

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
                    checkItemsInInv();
                    openGUI(player);
                    this.navigation.stop();
                    return InteractionResult.SUCCESS;
                }
                if(!player.isCrouching()) {
                       int state = this.getFollowState();
                    switch (state) {
                        default:
                        case 0:
                            setFollowState(1);
                            String follow = TEXT_FOLLOW.getString();
                            player.sendMessage(new TextComponent(name + follow), player.getUUID());
                            break;
                        case 1:
                            setFollowState(2);
                            String holdyourpos = TEXT_HOLD_YOUR_POS.getString();
                            player.sendMessage(new TextComponent(name + holdyourpos), player.getUUID());
                            break;
                        case 2:
                            setFollowState(0);
                            String wander = TEXT_WANDER.getString();
                            player.sendMessage(new TextComponent(name + wander), player.getUUID());
                            break;
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

            String info_max = INFO_RECRUITING_MAX.getString();
            player.sendMessage(new TextComponent(name + info_max), player.getUUID());
            return false;
        }
        else
            this.makeHireSound();

            this.setOwnerUUID(Optional.of(player.getUUID()));
            this.setIsOwned(true);
            this.navigation.stop();
            this.setTarget(null);
            this.setFollowState(2);
            this.setState(0);

            int i = this.random.nextInt(4);
        switch (i) {
            case 1 -> {
                String recruited1 = TEXT_RECRUITED1.getString();
                player.sendMessage(new TextComponent(name + recruited1), player.getUUID());
            }
            case 2 -> {
                String recruited2 = TEXT_RECRUITED2.getString();
                player.sendMessage(new TextComponent(name + recruited2), player.getUUID());
            }
            case 3 -> {
                String recruited3 = TEXT_RECRUITED3.getString();
                player.sendMessage(new TextComponent(name + recruited3), player.getUUID());
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
                String hello1 = TEXT_HELLO_1.getString();
                player.sendMessage(new TextComponent(name + hello1), player.getUUID());
            }
            case 2 -> {
                String hello2 = TEXT_HELLO_2.getString();
                player.sendMessage(new TextComponent(name + hello2), player.getUUID());
            }
            case 3 -> {
                String hello3 = TEXT_HELLO_3.getString();
                player.sendMessage(new TextComponent(name + hello3), player.getUUID());
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
            return super.hurt(dmg, amt);
        }
    }

    public boolean doHurtTarget(Entity entity) {
        boolean flag = entity.hurt(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (flag) {
            this.doEnchantDamageEffects(this, entity);
        }

        this.addXp(2);
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

        if(this.getTeam() != null) {
            Team team = this.getTeam();
            String teamName = team.getName();
            PlayerTeam playerteam = this.level.getScoreboard().getPlayerTeam(teamName);
            this.level.getScoreboard().removePlayerFromTeam(this.getStringUUID(), playerteam);
        }

        if (this.dead) {
            if (!this.level.isClientSide && this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && this.getOwner() instanceof ServerPlayer) {
                this.getOwner().sendMessage(deathMessage, Util.NIL_UUID);
            }
        }
    }

    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    public void updateMoral(){
        boolean confused =  10 <= getMoral() && getMoral() < 20;
        boolean lowMoral =  20 <= getMoral() && getMoral() < 40;
        boolean highMoral =  80 <= getMoral() && getMoral() < 95;
        boolean strong =  95 <= getMoral();

        if(this.getIsEating()){
            if(getMoral() < 100) setMoral((getMoral() + 0.001F));
        }

        if (isStarving() && this.isOwned()){
            if(getMoral() > 0) setMoral((getMoral() - 0.01F));
        }

        if (this.isOwned() && !isSaturated()){
            if(getMoral() > 35) setMoral((getMoral() - 0.0001F));
        }

        if(this.isSaturated() || getHealth() >= getMaxHealth()*0.85){
            if(getMoral() < 65) setMoral((getMoral() + 0.0002F));
        }

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

        if (strong) {
            if (!this.hasEffect(MobEffects.DAMAGE_BOOST))
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, false, true));
            if (!this.hasEffect(MobEffects.DAMAGE_RESISTANCE))
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, false, true));
        }
    }

    public void updateHunger(){
        if(getHunger() > 0) {
            setHunger((getHunger() - 0.0001F));
        }
        if(eatCoolDown > 0){
            eatCoolDown--;
        }
        if(getHunger() >=  75F && getHealth() < getMaxHealth()){
            this.heal(1.0F/40F);// 1 hp in 2s
        }
    }

    public boolean needsToGetFood(){
        return this.needsToEat() && this.getUpkeepPos() != null;
    }
    public boolean needsToEat(){
        /*
        else if(getHealth() <= (getMaxHealth() * 0.20) && eatCoolDown == 0) {
            return true;
        }
        else if(getHealth() <= (getMaxHealth() * 0.90) && this.getTarget() == null){
            return true;
        }
         */
        //Main.LOGGER.debug(getHunger());
        if (getHunger() <= 50F){
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

    @Override
    public void checkItemsInInv(){
    }

    public void checkLevel(){
        int currentXp = this.getXp();
        if (currentXp >= RecruitsModConfig.RecruitsMaxXpForLevelUp.get()){
            this.addXpLevel(1);
            this.setXp(0);
            this.addLevelBuffs();
            this.heal(10F);
            this.recalculateCost();

            if(this.getMoral() < 100)
                this.setMoral(getMoral() + 5F);
        }
    }

    private void recalculateCost() {
        int currCost = getCost();
        int armorBonus = this.getArmorValue() * 2;
        int weaponBonus = this.getMainHandItem().getMaxDamage() * 2;
        int speedBonus = (int) (this.getSpeed() * 2);
        int shieldBonus = this.getOffhandItem().getItem() instanceof ShieldItem ? 10 : 0;
        int newCost = Math.abs((shieldBonus + speedBonus + weaponBonus + armorBonus + currCost + getXpLevel() * 2));

        this.setCost(newCost);
    }

    public void makeLevelUpSound() {
        this.playSound(SoundEvents.VILLAGER_YES, 15.0F, 0.8F + 0.4F * this.random.nextFloat());
        this.playSound(SoundEvents.PLAYER_LEVELUP, 15.0F, 0.8F + 0.4F * this.random.nextFloat());
    }

    public void makeHireSound() {
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
        if (damage >= 0.0F) {
            damage = damage / 4.0F;
            if (damage < 1.0F) {
                damage = 1.0F;
            }
            for (int i = 0; i < 4; ++i) {//0,1,2,3 armor
                ItemStack itemstack = this.inventory.getItem(i);
                if ((!damageSource.isFire() || !itemstack.getItem().isFireResistant()) && itemstack.getItem() instanceof ArmorItem) {
                    itemstack.setDamageValue((int) damage);
                }
            }
        }
    }

    protected void damageMainHandItem() {
        ItemStack itemstack = this.getMainHandItem();
        if (itemstack.getItem().isDamageable(itemstack)) {
            itemstack.setDamageValue(1);
        }
    }

    @Override
    public void killed(@NotNull ServerLevel level, @NotNull LivingEntity living) {
        super.killed(level, living);
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
    }

    @Override
    protected void blockUsingShield(@NotNull LivingEntity living) {
        super.blockUsingShield(living);
        if (living.getMainHandItem().canDisableShield(this.useItem, this, living))
            this.disableShield();
    }

    public void disableShield() {
            this.blockCoolDown = 100;
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

    @Override
    protected void hurtCurrentlyUsedShield(float damage) {
        if (this.useItem.getItem() instanceof ShieldItem) {
            int i = 1 + Mth.floor(damage);
            InteractionHand hand = this.getUsedItemHand();
            this.useItem.hurtAndBreak(i, this, (entity) -> entity.broadcastBreakEvent(hand));
            if (this.useItem.isEmpty()) {
                if (hand == InteractionHand.MAIN_HAND) {
                    this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                } else {
                    this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
                this.useItem = ItemStack.EMPTY;
                this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            }

            ItemStack itemstack = this.getOffhandItem();
            if (itemstack.getItem() instanceof ShieldItem) {
                itemstack.setDamageValue((int) damage);
            }
        }
    }

    @Override
    public void openGUI(Player player) {

        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return getName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new RecruitInventoryContainer(i, AbstractRecruitEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageRecruitGui(player, this.getUUID()));
        }
    }

    public void openDebugScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return getName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new DebugInvContainer(i, AbstractRecruitEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugScreen(player, this.getUUID()));
        }
    }

    public boolean isValidTarget(LivingEntity living){
        boolean notAllowed = living instanceof AbstractFish || living instanceof AbstractHorse || living instanceof Creeper || living instanceof RecruitHorseEntity || living instanceof Ghast;


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
        if (target.canBeSeenAsEnemy()){
            if (target instanceof Player player){
                return this.isValidTargetPlayer(player);
            }
            else
                return isValidTarget(target);
        }
        return false;
    }

    public void updateTeam(){
        if(this.isOwned() && getOwner() != null){
            Team team = this.getTeam();
            Team ownerTeam = this.getOwner().getTeam();
            if (team == ownerTeam) {
                return;
            }
            else if(ownerTeam == null){
                String teamName = team.getName();
                PlayerTeam recruitTeam = this.level.getScoreboard().getPlayerTeam(teamName);
                this.level.getScoreboard().removePlayerFromTeam(this.getStringUUID(), recruitTeam);
            }
            else{
                String ownerTeamName = ownerTeam.getName();
                PlayerTeam playerteam = this.level.getScoreboard().getPlayerTeam(ownerTeamName);

                boolean flag = playerteam != null && this.level.getScoreboard().addPlayerToTeam(this.getStringUUID(), playerteam);
                if (!flag) {
                    Main.LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", ownerTeamName);
                }else
                    this.setTarget(null);// fix "if owner was other team and now same team und was target"
            }
        }
    }

    public void openHireGUI(Player player) {
        this.navigation.stop();

        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return getName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new RecruitHireContainer(i, playerInventory.player, AbstractRecruitEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageHireGui(player, this.getUUID()));
        }
    }

    private static final TranslatableComponent TEXT_DISBAND = new TranslatableComponent("chat.recruits.text.disband");
    private static final TranslatableComponent TEXT_HELLO_1 = new TranslatableComponent("chat.recruits.text.hello_1");
    private static final TranslatableComponent TEXT_HELLO_2 = new TranslatableComponent("chat.recruits.text.hello_2");
    private static final TranslatableComponent TEXT_HELLO_3 = new TranslatableComponent("chat.recruits.text.hello_3");
    public static final TranslatableComponent TEXT_RECRUITED1 = new TranslatableComponent("chat.recruits.text.recruited1");
    public static final TranslatableComponent TEXT_RECRUITED2 = new TranslatableComponent("chat.recruits.text.recruited2");
    public static final TranslatableComponent TEXT_RECRUITED3 = new TranslatableComponent("chat.recruits.text.recruited3");
    private static final TranslatableComponent INFO_RECRUITING_MAX = new TranslatableComponent("chat.recruits.info.reached_max");
    private static final TranslatableComponent TEXT_FOLLOW = new TranslatableComponent("chat.recruits.text.follow");
    private static final TranslatableComponent TEXT_HOLD_YOUR_POS = new TranslatableComponent("chat.recruits.text.hold_your_pos");
    private static final TranslatableComponent TEXT_WANDER = new TranslatableComponent("chat.recruits.text.wander");

    public void shouldMount(boolean should, UUID mount_uuid) {
        if (!this.isPassenger()){
            this.setShouldMount(should);
            if(mount_uuid != null) {
                this.setMountUUID(Optional.of(mount_uuid));
            }
            else this.setMountUUID(Optional.empty());
        }
    }

    public void shouldEscort(boolean should, UUID escort_uuid) {
        this.setShouldEscort(should);
        if(escort_uuid != null) this.setEscortUUID(Optional.of(escort_uuid));
        else this.setEscortUUID(Optional.empty());
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

}
