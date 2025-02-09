package com.talhanation.recruits.entities;
//ezgi&talha kantar

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.ai.*;
import com.talhanation.recruits.entities.ai.compat.BlockWithWeapon;
import com.talhanation.recruits.entities.ai.navigation.RecruitPathNavigation;
import com.talhanation.recruits.init.ModItems;
import com.talhanation.recruits.inventory.DebugInvMenu;
import com.talhanation.recruits.inventory.RecruitHireMenu;
import com.talhanation.recruits.inventory.RecruitInventoryMenu;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.RecruitsTeam;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
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
    private static final EntityDataAccessor<Boolean> FLEEING = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> HUNGER = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MORAL = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> OWNED = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> COST = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> UPKEEP_ID = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> COLOR = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> BIOME = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> SHOULD_REST = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_RANGED = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    public int blockCoolDown;
    public boolean needsTeamUpdate = true;
    public boolean forcedUpkeep;
    public int dismount = 0;
    public int upkeepTimer = 0;
    public int mountTimer = 0;
    public int despawnTimer = -1;
    public boolean reachedMovePos;
    public int attackCooldown = 0;
    public boolean rotate;
    public float ownerRot;
    public int formationPos = -1;
    private int maxFallDistance;
    public Vec3 holdPosVec;
    public boolean isInFormation;
    public boolean needsColorUpdate = true;
    public AbstractRecruitEntity(EntityType<? extends AbstractInventoryEntity> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 6;
        this.navigation = this.createNavigation(world);
        this.maxUpStep = 1F;
        this.setMaxFallDistance(1);
    }

    ///////////////////////////////////NAVIGATION/////////////////////////////////////////
    @NotNull
    protected PathNavigation createNavigation(@NotNull Level level) {
        return new RecruitPathNavigation(this, level);
    }

    public @NotNull PathNavigation getNavigation() {
        return super.getNavigation();
    }

    public void rideTick() {
        super.rideTick();
    }

    public double getMyRidingOffset() {
        return -0.35D;
    }

    public int getMaxFallDistance() {
        return maxFallDistance;
    }

    public void setMaxFallDistance(int x){
        this.maxFallDistance = x;
    }

    ///////////////////////////////////TICK/////////////////////////////////////////
    // @Override
    public void aiStep(){
        super.aiStep();
        updateSwingTime();
        updateShield();
        if(this instanceof IRangedRecruit  && this.tickCount % 20 == 0) pickUpArrows();
        if(needsTeamUpdate) updateTeam();
        if(needsColorUpdate && this.getTeam() != null) updateColor(this.getTeam().getName());
    }
    public void tick() {
        super.tick();
        if(despawnTimer > 0) despawnTimer--;
        if(despawnTimer == 0) recruitCheckDespawn();
        if(getMountTimer() > 0) setMountTimer(getMountTimer() - 1);
        if(getUpkeepTimer() > 0) setUpkeepTimer(getUpkeepTimer() - 1);
        if(getHunger() >=  70F && getHealth() < getMaxHealth()){
            this.heal(1.0F/50F);// 1 hp in 2.5s
        }

        if(this.reachedMovePos){
            this.setFollowState(2);
            this.reachedMovePos = false;
        }

        if(this.attackCooldown > 0) this.attackCooldown--;

    }

    private void recruitCheckDespawn() {
        Entity entity = this.level.getNearestPlayer(this, -1.0D);

        if (entity != null) {
            double d0 = entity.distanceToSqr(this);
            int k = this.getType().getCategory().getNoDespawnDistance();
            int l = k * k;

            if (this.random.nextInt(800) == 0 && d0 > (double) l) {
                if(this.getVehicle() instanceof LivingEntity livingMount) livingMount.discard();
                this.discard();
            }
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
        this.goalSelector.addGoal(4, new BlockWithWeapon(this));
        this.goalSelector.addGoal(0, new RecruitFloatGoal(this));
        this.goalSelector.addGoal(1, new RecruitQuaffGoal(this));
        this.goalSelector.addGoal(1, new FleeTNT(this));
        this.goalSelector.addGoal(1, new FleeFire(this));
        this.goalSelector.addGoal(6, new OpenDoorGoal(this, true) {});
        this.goalSelector.addGoal(1, new RecruitProtectEntityGoal(this));
        this.goalSelector.addGoal(0, new RecruitEatGoal(this));
        this.goalSelector.addGoal(5, new RecruitUpkeepPosGoal(this));
        this.goalSelector.addGoal(6, new RecruitUpkeepEntityGoal(this));
        this.goalSelector.addGoal(3, new RecruitMountEntity(this));
        this.goalSelector.addGoal(3, new RecruitDismountEntity(this));
        this.goalSelector.addGoal(3, new RecruitMoveToPosGoal(this, 1.05D));
        this.goalSelector.addGoal(2, new RecruitFollowOwnerGoal(this, 1.05D, 300, 100));
        this.goalSelector.addGoal(2, new RecruitMeleeAttackGoal(this, 1.05D, this.getMeleeStartRange()));
        this.goalSelector.addGoal(3, new RecruitHoldPosGoal(this, 1.0D, 32.0F));
        //this.goalSelector.addGoal(7, new RecruitDodgeGoal(this));
        this.goalSelector.addGoal(4, new RestGoal(this));
        this.goalSelector.addGoal(10, new RecruitWanderGoal(this));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        //this.goalSelector.addGoal(13, new RecruitPickupWantedItemGoal(this));
        this.targetSelector.addGoal(0, new RecruitProtectHurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new RecruitOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new RecruitNearestAttackableTargetGoal<>(this, LivingEntity.class, true, false, this::shouldAttack));
        this.targetSelector.addGoal(3, (new RecruitHurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(4, new RecruitOwnerHurtTargetGoal(this));

        this.targetSelector.addGoal(7, new RecruitDefendVillageFromPlayerGoal(this));
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
        this.entityData.define(HUNGER, 50F);
        this.entityData.define(MORAL, 50F);
        this.entityData.define(OWNER_ID, Optional.empty());
        this.entityData.define(UPKEEP_ID, Optional.empty());
        this.entityData.define(OWNED, false);
        this.entityData.define(COST, 1);
        this.entityData.define(COLOR, (byte) 0);
        this.entityData.define(BIOME, (byte) 0);
        this.entityData.define(SHOULD_REST, false);
        this.entityData.define(SHOULD_RANGED, true);
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
        nbt.putInt("despawnTimer", this.despawnTimer);
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
        nbt.putInt("Xp", this.getXp());
        nbt.putInt("Level", this.getXpLevel());
        nbt.putInt("Kills", this.getKills());
        nbt.putFloat("Hunger", this.getHunger());
        nbt.putFloat("Moral", this.getMorale());
        nbt.putBoolean("isOwned", this.getIsOwned());
        nbt.putInt("Cost", this.getCost());
        nbt.putInt("mountTimer", this.getMountTimer());
        nbt.putInt("upkeepTimer", this.getUpkeepTimer());
        nbt.putInt("Color", this.getColor());
        nbt.putInt("Biome", this.getBiome());
        nbt.putInt("MaxFallDistance", this.getMaxFallDistance());
        nbt.putInt("formationPos", formationPos);
        nbt.putBoolean("ShouldRest", this.getShouldRest());
        nbt.putBoolean("ShouldRanged", this.getShouldRanged());
        nbt.putBoolean("isInFormation", this.isInFormation);

        if(this.getHoldPos() != null){
            nbt.putDouble("HoldPosX", this.getHoldPos().x());
            nbt.putDouble("HoldPosY", this.getHoldPos().y());
            nbt.putDouble("HoldPosZ", this.getHoldPos().z());
            nbt.putBoolean("ShouldHoldPos", this.getShouldHoldPos());
        }

        if(this.getMovePos() != null){
            nbt.putDouble("MovePosX", this.getMovePos().getX());
            nbt.putDouble("MovePosY", this.getMovePos().getY());
            nbt.putDouble("MovePosZ", this.getMovePos().getZ());
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

        if(nbt.contains("despawnTimer")) this.despawnTimer = nbt.getInt("despawnTimer");
        else this.despawnTimer = -1;//fixes random recruits disappearing

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
        this.setXp(nbt.getInt("Xp"));
        this.setKills(nbt.getInt("Kills"));
        this.setVariant(nbt.getInt("Variant"));
        this.setHunger(nbt.getFloat("Hunger"));
        this.setMoral(nbt.getFloat("Moral"));
        this.setIsOwned(nbt.getBoolean("isOwned"));
        this.setCost(nbt.getInt("Cost"));
        this.setMountTimer(nbt.getInt("mountTimer"));
        this.setUpkeepTimer(nbt.getInt("UpkeepTimer"));
        this.setColor(nbt.getByte("Color"));

        this.setMaxFallDistance(nbt.getInt("MaxFallDistance"));
        this.formationPos = (nbt.getInt("formationPos"));
        this.setShouldRest(nbt.getBoolean("ShouldRest"));
        this.isInFormation = nbt.getBoolean("isInFormation");

        if (nbt.contains("HoldPosX") && nbt.contains("HoldPosY") && nbt.contains("HoldPosZ")) {
            this.setShouldHoldPos(nbt.getBoolean("ShouldHoldPos"));
            this.setHoldPos(new Vec3 (
                    nbt.getDouble("HoldPosX"),
                    nbt.getDouble("HoldPosY"),
                    nbt.getDouble("HoldPosZ")));
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

        if(nbt.contains("Biome"))this.setBiome(nbt.getByte("Biome"));
        else applyBiomeAndVariant(this);;
    }

    ////////////////////////////////////GET////////////////////////////////////

    public int getUpkeepTimer(){
        return this.upkeepTimer;
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

    public float getMorale() {
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
    public boolean getShouldRest() {
        return entityData.get(SHOULD_REST);
    }

    public boolean getShouldRanged() {
        return entityData.get(SHOULD_RANGED);
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
    //2 = hold your position
    //3 = back to position
    //4 = hold my position
    //5 = Protect
    public int getFollowState(){
        return entityData.get(FOLLOW_STATE);
    }

    public SoundEvent getHurtSound(@NotNull DamageSource ds) {
        if (this.isBlocking())
            return SoundEvents.SHIELD_BLOCK;
        return RecruitsClientConfig.RecruitsLookLikeVillagers.get() ? SoundEvents.VILLAGER_HURT : SoundEvents.GENERIC_HURT;
    }

    protected SoundEvent getDeathSound() {
        return RecruitsClientConfig.RecruitsLookLikeVillagers.get() ? SoundEvents.VILLAGER_DEATH : SoundEvents.GENERIC_DEATH;
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    protected float getStandingEyeHeight(@NotNull Pose pos, EntityDimensions size) {
        return size.height * 0.98F;
    }

    public int getMaxHeadXRot() {
        return super.getMaxHeadXRot();
    }

    public int getMaxSpawnClusterSize() {
        return 8;
    }

    public Vec3 getHoldPos(){
        return this.holdPosVec;
        //return entityData.get(HOLD_POS).orElse(null);
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
        List<LivingEntity> list = this.getLevel().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(32D),
                (living) -> this.getProtectUUID() != null && living.getUUID().equals(this.getProtectUUID()) && living.isAlive()
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public int getColor() {
        return entityData.get(COLOR);
    }

    public int getBiome() {
        return entityData.get(BIOME);
    }
    public DyeColor getDyeColor() {
        return DyeColor.byId(getColor());
    }

    ////////////////////////////////////SET////////////////////////////////////

    public void setUpkeepTimer(int x){
        this.upkeepTimer =  x;
    }
    public void setVariant(int variant){
        entityData.set(VARIANT, variant);
    }
    public void setColor(byte color){
        entityData.set(COLOR, color);
    }
    public void setBiome(byte biome){
        entityData.set(BIOME, biome);
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
        float currentHunger = getHunger();
        if(value < 0 && currentHunger - value <= 0)
            this.entityData.set(HUNGER, 0F);
        else
            this.entityData.set(HUNGER, value);
    }

    public void setFleeing(boolean bool){
        entityData.set(FLEEING, bool);
    }
    public void setMountTimer(int x){
        this.mountTimer = x;
    }

    public void disband(@Nullable Player player, boolean keepTeam, boolean increaseCost){
        String name = this.getName().getString();
        RecruitEvents.recruitsPlayerUnitManager.removeRecruits(this.getOwnerUUID(), 1);
        if(player != null){
            player.sendSystemMessage(TEXT_DISBAND(name));
        }
        this.setTarget(null);
        this.setIsOwned(false);
        this.setOwnerUUID(Optional.empty());

        if(increaseCost) this.recalculateCost();
        if (this.getTeam() != null){
            if(!this.level.isClientSide() && !keepTeam)
                TeamEvents.removeRecruitFromTeam(this, this.getTeam(), (ServerLevel) this.getCommandSenderWorld());
        }
    }

    public void addXpLevel(int level){
        int currentLevel = this.getXpLevel();
        int newLevel = currentLevel + level;

        if(newLevel > RecruitsServerConfig.RecruitsMaxXpLevel.get()){
            newLevel = RecruitsServerConfig.RecruitsMaxXpLevel.get();
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
        this.entityData.set(XP, xp);
    }

    public void addXp(int xp){
        int currentXp = this.getXp();
        int newXp = currentXp + xp;

        this.entityData.set(XP, newXp);
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
    public void setShouldRest(boolean bool){
        if(bool) setFollowState(0);
        entityData.set(SHOULD_REST, bool);
    }

    public void setShouldRanged(boolean should) {
        entityData.set(SHOULD_RANGED, should);
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
                setHoldPos(position());
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
                setHoldPos(this.getOwner().position());
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

    public void setHoldPos(Vec3 holdPos){
        //this.entityData.set(HOLD_POS, Optional.of(holdPos));
        this.holdPosVec = holdPos;
    }
    public void setMovePos(BlockPos holdPos){
        this.entityData.set(MOVE_POS, Optional.of(holdPos));
        reachedMovePos = false;
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
        //Equipment
        List<List<String>> equipmentSets = getEquipment();
        if(!equipmentSets.isEmpty()){

            int size = equipmentSets.size();
            int i = this.random.nextInt(0, size);

            if(i >= 0){
                List<String> equipmentSet = equipmentSets.get(i);
                while(equipmentSet.size() < 6) equipmentSet.add("");

                String mainHandStr = equipmentSet.get(0);
                String offHandStr = equipmentSet.get(1);
                String feetStr = equipmentSet.get(2);
                String legsStr = equipmentSet.get(3);
                String chestStr = equipmentSet.get(4);
                String headStr = equipmentSet.get(5);

                Optional<Holder<Item>> holderHead = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(headStr));
                holderHead.ifPresent(itemHolder -> this.setItemSlot(EquipmentSlot.HEAD, itemHolder.value().getDefaultInstance()));

                Optional<Holder<Item>> holderChest = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(chestStr));
                holderChest.ifPresent(itemHolder -> this.setItemSlot(EquipmentSlot.CHEST, itemHolder.value().getDefaultInstance()));

                Optional<Holder<Item>> holderLegs = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(legsStr));
                holderLegs.ifPresent(itemHolder -> this.setItemSlot(EquipmentSlot.LEGS, itemHolder.value().getDefaultInstance()));

                Optional<Holder<Item>> holderFeet = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(feetStr));
                holderFeet.ifPresent(itemHolder -> this.setItemSlot(EquipmentSlot.FEET, itemHolder.value().getDefaultInstance()));

                Optional<Holder<Item>> holderMainHand = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(mainHandStr));
                holderMainHand.ifPresent(itemHolder -> this.setItemSlot(EquipmentSlot.MAINHAND, itemHolder.value().getDefaultInstance()));

                Optional<Holder<Item>> holderOffHand = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(offHandStr));
                holderOffHand.ifPresent(itemHolder -> this.setItemSlot(EquipmentSlot.OFFHAND, itemHolder.value().getDefaultInstance()));
            }
        }
    }

    public List<List<String>> getEquipment() {
        return null;
    }

    public double getMeleeStartRange() {
        return 32D;
    }

    public abstract void initSpawn();

    public static void applySpawnValues(AbstractRecruitEntity recruit){
        recruit.setHunger(50);
        recruit.setMoral(50);
        recruit.setListen(true);
        recruit.setXpLevel(1);

        applyBiomeAndVariant(recruit);
    }

    public static void applyBiomeAndVariant(AbstractRecruitEntity recruit){
        //ForgeBiomeTagsProvider
        Holder<Biome> biome = recruit.getCommandSenderWorld().getBiome(recruit.getOnPos());
        byte biomeByte = 2; //PLAINS
        int variant = recruit.random.nextInt(0, 14);
        //DESERT
        if(biome.is(Biomes.ERODED_BADLANDS) || biome.containsTag(Tags.Biomes.IS_DESERT) || biome.containsTag(Tags.Biomes.IS_SANDY) && !biome.containsTag(Tags.Biomes.IS_WET_OVERWORLD)){
            biomeByte = 0;
            variant = recruit.random.nextInt(15, 19);
        }
        //TAIGA
        else if(biome.is(Tags.Biomes.IS_CONIFEROUS) && biome.is(Tags.Biomes.IS_COLD_OVERWORLD) && !(biome.is(Tags.Biomes.IS_SNOWY))){
            biomeByte = 6;
            variant = recruit.random.nextInt(5, 14);
        }
        //JUNGLE
        else if(biome.is(Tags.Biomes.IS_WET_OVERWORLD) && !biome.is(Tags.Biomes.IS_SANDY) && !biome.is(Tags.Biomes.IS_SWAMP)){
            biomeByte = 1;
            variant = recruit.random.nextInt(15, 19);
        }
        //SVANNA
        else if(biome.is(Tags.Biomes.IS_HOT_OVERWORLD) && biome.is(Tags.Biomes.IS_SPARSE_OVERWORLD)){
            biomeByte = 3;
            variant = recruit.random.nextInt(15, 19);
        }
        //SNOWY
        else if(biome.is(Tags.Biomes.IS_SNOWY)){
            biomeByte = 4;
            variant = recruit.random.nextInt(5, 10);
        }
        //SWAMP
        else if(biome.is(Tags.Biomes.IS_SWAMP)){
            biomeByte = 5;
            variant = recruit.random.nextInt(5, 14);
        }

        recruit.setBiome(biomeByte);
        recruit.setVariant(variant);
    }

    ////////////////////////////////////is FUNCTIONS////////////////////////////////////

    public boolean isEffectedByCommand(UUID player_uuid, int group){
        return (this.isOwned() && this.isAlive() && (this.getListen()) && Objects.equals(this.getOwnerUUID(), player_uuid) && (this.getGroup() == group || group == 0));
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
        Team ownerTeam = this.getTeam();
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
                    if(this instanceof AbstractLeaderEntity) CommandEvents.checkPatrolLeaderState(this);
                    return InteractionResult.SUCCESS;
                }
            }
            else if(this.isOwned() && this.getTeam() != null && !player.getUUID().equals(this.getOwnerUUID()) &&
                    TeamEvents.recruitsTeamManager.getTeamByStringID(this.getTeam().getName()).getTeamLeaderUUID().equals(player.getUUID())){
                    //this will not work:
                    Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientOpenTakeOverScreen(this.getUUID()));
            }
            else if (!this.isOwned() && !isPlayerTarget) {
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
        Team ownerTeam = player.getTeam();// player is the new owner
        String stringId = ownerTeam != null ? ownerTeam.getName() : "";
        if (!RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(stringId, player.getUUID())) {

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
            this.despawnTimer = -1;

            if(!this.level.isClientSide() && ownerTeam != null) TeamEvents.addRecruitToTeam(this, ownerTeam, (ServerLevel) this.getCommandSenderWorld());

            int i = this.random.nextInt(4);
            switch (i) {
                default -> {
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

        RecruitEvents.recruitsPlayerUnitManager.addRecruits(player.getUUID(), 1);
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
            if(this.getMorale() > 0) this.setMoral(this.getMorale() - 0.25F);
            if(isBlocking()) hurtCurrentlyUsedShield(amt);

            if(entity instanceof LivingEntity living && RecruitEvents.canAttack(this, living)){
                if(this.getFollowState() == 5){//Protecting
                    List<AbstractRecruitEntity> list = this.getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, this.getBoundingBox().inflate(32D));
                    for(AbstractRecruitEntity recruit : list){
                        if (recruit.getUUID().equals(recruit.getProtectUUID()) && recruit.isAlive() && !recruit.equals(living)){
                            //Patrolleader
                            recruit.setTarget(living);
                        }
                    }
                }


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

        boolean flag = entity.hurt(DamageSource.mobAttack(this), f);
        if (flag) {

            this.doEnchantDamageEffects(this, entity);
            this.setLastHurtMob(entity);
        }

        this.addXp(1);
        if(this.getHunger() > 0) this.setHunger(this.getHunger() - 0.1F);
        this.checkLevel();
        if(this.getMorale() < 100) this.setMoral(this.getMorale() + 0.25F);
        this.damageMainHandItem();
        return true;
    }

    public void addLevelBuffs(){
        int level = getXpLevel();
        if(level <= 10){
            getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 2D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("attack_bonus_level", 0.03D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("knockback_bonus_level", 0.0012D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier("speed_bonus_level", 0.0025D, AttributeModifier.Operation.ADDITION));
        }
        if(level > 10){
            getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 2D, AttributeModifier.Operation.ADDITION));
        }
    }

    public void addLevelBuffsForLevel(int level){

        for(int i = 0; i < level; i++) {
            if (level <= 10) {
                getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 2D, AttributeModifier.Operation.ADDITION));
                getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("attack_bonus_level", 0.03D, AttributeModifier.Operation.ADDITION));
                getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("knockback_bonus_level", 0.0012D, AttributeModifier.Operation.ADDITION));
                getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier("speed_bonus_level", 0.0025D, AttributeModifier.Operation.ADDITION));}
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

    public boolean isAlliedTo(Team p_20032_) {
        return this.getTeam() != null && this.getTeam().isAlliedTo(p_20032_);
    }

    public void die(DamageSource dmg) {
        net.minecraft.network.chat.Component deathMessage = this.getCombatTracker().getDeathMessage();
        super.die(dmg);
        if (this.dead) {
            if (!this.level.isClientSide && this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && this.getOwner() instanceof ServerPlayer) {
                this.getOwner().sendSystemMessage(deathMessage);

                if(this.isOwned()){
                    RecruitEvents.recruitsPlayerUnitManager.removeRecruits(this.getOwnerUUID(), 1);
                    TeamEvents.removeRecruitFromTeam(this, this.getTeam(), (ServerLevel) this.getLevel());
                }
                if(this.getTeam() != null){
                    TeamEvents.recruitsTeamManager.getTeamByStringID(this.getTeam().getName()).addNPCs(-1);
                }
            }
        }
    }

    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    public void updateMorale(){
        //fast recovery
        float currentMorale = getMorale();
        float newMorale = currentMorale;

        if (isStarving() && this.isOwned()){
            if(currentMorale > 0) newMorale -= 2F;
        }

        if (this.isOwned() && !isSaturated()){
            if(currentMorale > 35) newMorale -= 1F;
        }

        if(this.isSaturated() || getHealth() >= getMaxHealth() * 0.85){
            if(currentMorale < 65) newMorale += 2F;
        }

        if(newMorale < 0) newMorale = 0;

        this.setMoral(newMorale);
    }

    public void applyMoralEffects(){
        boolean confused =  0 <= getMorale() && getMorale() < 20;
        boolean lowMoral =  20 <= getMorale() && getMorale() < 40;
        boolean highMoral =  90 <= getMorale() && getMorale() <= 100;

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
        float hunger = getHunger();

        if (this.getFollowState() == 2) {
            hunger -= 2/60F;
        }
        else{
            hunger -= 3/60F;
        }

        if (hunger < 0) hunger = 0;

        this.setHunger(hunger);
    }

    public boolean needsToGetFood(){
        int timer = this.getUpkeepTimer();
        boolean needsToEat = this.needsToEat();
        boolean hasFood = this.hasFoodInInv();
        boolean isChest = this.getUpkeepPos() != null;
        boolean isEntity = this.getUpkeepUUID() != null;

        return (forcedUpkeep || (!hasFood && timer == 0 && needsToEat) && (isChest || isEntity)) && !getShouldProtect();
    }

    public boolean hasFoodInInv(){
        return this.getInventory().items
                .stream()
                .anyMatch(ItemStack::isEdible);
    }

    public boolean needsToEat(){
        if (getHunger() <= 50F){
            return true;
        }
        if (getHunger() <= 70F && getHealth() != getMaxHealth() && this.getTarget() == null && this.getIsOwned()){
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
        if (currentXp >= RecruitsServerConfig.RecruitsMaxXpForLevelUp.get()){
            this.addXpLevel(1);
            this.setXp(0);
            this.heal(10F);
            this.recalculateCost();

            if(this.getMorale() < 100)
                this.setMoral(getMorale() + 5F);
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
        this.level.playSound(null, this.getX(), this.getY() + 1 , this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat());

        if(RecruitsClientConfig.RecruitsLookLikeVillagers.get())
            this.level.playSound(null, this.getX(), this.getY() + 1 , this.getZ(), SoundEvents.VILLAGER_CELEBRATE, this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat());
    }

    public void makeHireSound() {
        if(RecruitsClientConfig.RecruitsLookLikeVillagers.get())
            this.playSound(SoundEvents.VILLAGER_AMBIENT, 1.0F, 0.8F + 0.4F * this.random.nextFloat());
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
            if ((!damageSource.isFire() || !headArmor.getItem().isFireResistant()) && headArmor.getItem() instanceof ArmorItem) {
                //damage
                headArmor.hurtAndBreak(1, this, (p_43296_) -> {

                });
            }

        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && hasHeadArmor) {
            this.inventory.setItem(0, ItemStack.EMPTY);
            this.getInventory().setChanged();
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequip(EquipmentSlot.HEAD);
        }

        ItemStack chestArmor = this.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasChestArmor = !chestArmor.isEmpty();
        if ((!damageSource.isFire() || !chestArmor.getItem().isFireResistant()) && chestArmor.getItem() instanceof ArmorItem) {
            //damage
            chestArmor.hurtAndBreak(1, this, (p_43296_) -> {

            });
        }
        if (this.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && hasChestArmor) {
            this.inventory.setItem(1, ItemStack.EMPTY);
            this.getInventory().setChanged();
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequip(EquipmentSlot.CHEST);
        }

        ItemStack legsArmor = this.getItemBySlot(EquipmentSlot.LEGS);
        boolean hasLegsArmor = !legsArmor.isEmpty();

        if ((!damageSource.isFire() || !legsArmor.getItem().isFireResistant()) && legsArmor.getItem() instanceof ArmorItem) {
            //damage
            legsArmor.hurtAndBreak(1, this, (p_43296_) -> {

            });
        }
        if (this.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && hasLegsArmor) {
            this.inventory.setItem(2, ItemStack.EMPTY);
            this.getInventory().setChanged();
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequip(EquipmentSlot.LEGS);
        }


        ItemStack feetArmor = this.getItemBySlot(EquipmentSlot.FEET);
        boolean hasFeetArmor = !feetArmor.isEmpty();

        if ((!damageSource.isFire() || !feetArmor.getItem().isFireResistant()) && feetArmor.getItem() instanceof ArmorItem) {
            //damage
            feetArmor.hurtAndBreak(1, this, (p_43296_) -> {

            });

        }
        if (this.getItemBySlot(EquipmentSlot.FEET).isEmpty() && hasFeetArmor) {
            this.inventory.setItem(3, ItemStack.EMPTY);
            this.getInventory().setChanged();
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
            this.getInventory().setChanged();
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequip(EquipmentSlot.MAINHAND);
        }
    }

    public void tryToReequip(EquipmentSlot equipmentSlot){
        for(int i = 6; i < 15; i++){
            ItemStack itemStack = this.getInventory().getItem(i);
            if(canEquipItemToSlot(itemStack, equipmentSlot)) {
                this.playEquipSound(itemStack);
                this.setItemSlot(equipmentSlot, itemStack);
                this.inventory.setItem(getInventorySlotIndex(equipmentSlot), itemStack);
                this.inventory.removeItemNoUpdate(i);
            }
        }
    }

    public void tryToReequipShield(){
        for(ItemStack itemStack : this.getInventory().items){
            if(itemStack.getItem() instanceof ShieldItem){
                this.setItemSlot(EquipmentSlot.OFFHAND, itemStack);
                this.inventory.setItem(getInventorySlotIndex(EquipmentSlot.OFFHAND), itemStack);
                this.playEquipSound(itemStack);
                itemStack.shrink(1);
            }
        }
    }

    @Override
    public boolean wasKilled(@NotNull ServerLevel level, @NotNull LivingEntity living) {
        super.wasKilled(level, living);

        this.addXp(5);
        this.setKills(this.getKills() + 1);
        if(this.getMorale() < 100) this.setMoral(this.getMorale() + 1);

        if(living instanceof Player){
            this.addXp(45);
            if(this.getMorale() < 100) this.setMoral(this.getMorale() + 9);
        }

        if(living instanceof Raider){
            this.addXp(5);
            if(this.getMorale() < 100) this.setMoral(this.getMorale() + 2);
        }

        if(living instanceof Villager villager){
            if (villager.isBaby()) if(this.getMorale() > 0) this.setMoral(this.getMorale() - 10);
            else {
                if (this.getMorale() > 0) this.setMoral(this.getMorale() - 2);
            }
        }

        if(living instanceof WitherBoss){
            this.addXp(99);
            if(this.getMorale() < 100) this.setMoral(this.getMorale() + 9);
        }

        if(living instanceof IronGolem){
            this.addXp(49);
            if(this.getMorale() > 0) this.setMoral(this.getMorale() - 1);
        }

        if(living instanceof EnderDragon){
            this.addXp(999);
            if(this.getMorale() < 100) this.setMoral(this.getMorale() + 49);
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
        return this.mountTimer;
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
            this.getInventory().setChanged();
            this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            this.tryToReequipShield();
        }
    }

    @Override
    public void openGUI(Player player) {
        if (player instanceof ServerPlayer) {
            CommandEvents.updateRecruitInventoryScreen((ServerPlayer) player);
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

    public static void openTakeOverGUI(Player player) {

    }

    @Override
    public boolean canAttack(@Nonnull LivingEntity target) {
        return RecruitEvents.canAttack(this, target);
    }
    // 0 = NEUTRAL
    // 1 = AGGRESSIVE
    // 2 = RAID
    // 3 = PASSIVE
    public boolean shouldAttack(LivingEntity target) {
        if(RecruitsServerConfig.TargetBlackList.get().contains(target.getEncodeId())) return false;
        return switch (this.getState()) {
            case 3 -> false; // Passive mode: never attack
            case 0 -> shouldAttackOnNeutral(target) && canAttack(target);
            case 1 -> (shouldAttackOnNeutral(target) || shouldAttackOnAggressive(target)) && canAttack(target);
            case 2 -> !RecruitEvents.isAlly(this.getTeam(), target.getTeam()) && canAttack(target);
            default -> canAttack(target);
        };
    }

    private boolean shouldAttackOnNeutral(LivingEntity target){
        return isMonster(target) || isAttackingOwnerOrSelf(this, target) || RecruitEvents.isEnemy(this.getTeam(), target.getTeam());
    }

    private boolean shouldAttackOnAggressive(LivingEntity target){
        return (target instanceof AbstractRecruitEntity || target instanceof Player) && (RecruitEvents.isNeutral(this.getTeam(), target.getTeam()) || RecruitEvents.isEnemy(this.getTeam(), target.getTeam()));
    }

    private boolean isMonster(LivingEntity target) {
        return target instanceof Enemy;
    }

    private boolean isAttackingOwnerOrSelf(AbstractRecruitEntity recruit, LivingEntity target) {
        return target.getLastHurtByMob() != null &&
                (target.getLastHurtByMob().equals(recruit) || target.getLastHurtByMob().equals(recruit.getOwner()));
    }

    public boolean isAlliedTo(Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            return !RecruitEvents.canHarmTeam(this, livingTarget);
        } else {
            return super.isAlliedTo(target);
        }
    }

    //
    /*********************************************************
     * Update the current team of the recruit in following conditions:
     * - If recruit team is not the same team as the owner
     * - If recruit team is null but owner team != null
     * - If recruit team is != null but owner team is null
     *********************************************************/
    public void updateTeam(){
        if(this.isOwned() && !this.getCommandSenderWorld().isClientSide()){
            Player owner = getOwner();
            if(owner != null) {
                Team recruitTeam = this.getTeam();
                Team ownerTeam = owner.getTeam();

                if (ownerTeam == null) {
                    if(recruitTeam != null){
                        //Remove from current team because ownerTeam is null
                        TeamEvents.removeRecruitFromTeam(this, recruitTeam, (ServerLevel) this.getCommandSenderWorld());
                        TeamEvents.addNPCToData((ServerLevel) this.getCommandSenderWorld(), recruitTeam.getName(), -1 );
                    }
                    //recruit team is also null, so no do nothing
                    needsTeamUpdate = false;
                }
                else if(recruitTeam == null){
                    TeamEvents.addRecruitToTeam(this, ownerTeam, (ServerLevel) this.getCommandSenderWorld());
                    TeamEvents.addNPCToData((ServerLevel) this.getCommandSenderWorld(), ownerTeam.getName(), +1 );
                    needsTeamUpdate = false;
                }
                else if(recruitTeam == ownerTeam){
                    updateColor(ownerTeam.getName());
                    needsTeamUpdate = false;
                }
                else{
                    TeamEvents.removeRecruitFromTeam(this, recruitTeam, (ServerLevel) this.getCommandSenderWorld());
                    TeamEvents.addNPCToData((ServerLevel) this.getCommandSenderWorld(), recruitTeam.getName(), -1 );

                    TeamEvents.addRecruitToTeam(this, ownerTeam, (ServerLevel) this.getCommandSenderWorld());
                    TeamEvents.addNPCToData((ServerLevel) this.getCommandSenderWorld(), ownerTeam.getName(), +1 );
                    needsTeamUpdate = false;
                }
            }
        }
    }

    private void updateColor(String name) {
        if(!this.getCommandSenderWorld().isClientSide()){
            RecruitsTeam recruitsTeam = TeamEvents.recruitsTeamManager.getTeamByStringID(name);
            if(recruitsTeam != null && recruitsTeam.getUnitColor() != this.getColor()){
                this.setColor(recruitsTeam.getUnitColor());
                this.needsColorUpdate = false;
            }
        }
    }

    public void openHireGUI(Player player) {
        if (player instanceof ServerPlayer) {
            this.navigation.stop();
            Team ownerTeam = player.getTeam();
            String stringId = ownerTeam != null ? ownerTeam.getName() : "";
            boolean canHire = RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(stringId, player.getUUID());
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> (ServerPlayer) player), new MessageToClientUpdateHireScreen(TeamEvents.getCurrency(), canHire));
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
        if(should) this.dismount = 0;
    }

    public void shouldProtect(boolean should, UUID protect_uuid) {
        this.setShouldProtect(should);
        if(protect_uuid != null) this.setProtectUUID(Optional.of(protect_uuid));
        else this.setProtectUUID(Optional.empty());
    }

    public void clearUpkeepPos() {
        this.entityData.set(UPKEEP_POS, Optional.empty());
    }

    public void clearUpkeepEntity() {
        this.entityData.set(UPKEEP_ID, Optional.empty());
    }

    public boolean hasUpkeep(){
        return this.getUpkeepPos() != null || this.getUpkeepUUID() != null;
    }

    public void upkeepReequip(@NotNull Container container) {
        //Try to reequip
        for(int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemstack = container.getItem(i);
            ItemStack equipment;
            if(!this.canEatItemStack(itemstack) && this.wantsToPickUp(itemstack)){
                if (this.canEquipItem(itemstack)) {
                    equipment = itemstack.copy();
                    equipment.setCount(1);
                    this.equipItem(equipment);
                    itemstack.shrink(1);
                }
                if(this instanceof CrossBowmanEntity crossBowmanEntity && Main.isMusketModLoaded && IWeapon.isMusketModWeapon(crossBowmanEntity.getMainHandItem()) && itemstack.getDescriptionId().contains("cartridge")){
                    if(this.canTakeCartridge()){
                        equipment = itemstack.copy();
                        this.inventory.addItem(equipment);
                        itemstack.shrink(equipment.getCount());
                    }
                }
                else if (this instanceof IRangedRecruit && itemstack.is(ItemTags.ARROWS)){ //all that are ranged
                    if(this.canTakeArrows()){
                        equipment = itemstack.copy();
                        this.inventory.addItem(equipment);
                        itemstack.shrink(equipment.getCount());
                    }
                }
            }

            if (this instanceof CaptainEntity && Main.isSmallShipsLoaded){
                if(itemstack.getDescriptionId().contains("cannon_ball")){
                    if(this.canTakeCannonBalls()){
                        equipment = itemstack.copy();
                        this.inventory.addItem(equipment);
                        itemstack.shrink(equipment.getCount());
                    }
                }
                else if (itemstack.is(ItemTags.PLANKS)){
                    if(this.canTakePlanks()){
                        equipment = itemstack.copy();
                        this.inventory.addItem(equipment);
                        itemstack.shrink(equipment.getCount());
                    }
                }
                else if (itemstack.is(Items.IRON_NUGGET)){
                    if(this.canTakeIronNuggets()){
                        equipment = itemstack.copy();
                        this.inventory.addItem(equipment);
                        itemstack.shrink(equipment.getCount());
                    }
                }
            }
        }
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

    public int getUpkeepCooldown() {
        return 3000;
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

    private void pickUpArrows() {
        this.getLevel().getEntitiesOfClass(
                AbstractArrow.class,
                this.getBoundingBox().inflate(4D),
                (arrow) -> arrow.inGround &&
                        arrow.pickup == AbstractArrow.Pickup.ALLOWED &&
                        this.getInventory().canAddItem(Items.ARROW.getDefaultInstance())
        ).forEach((arrow) -> {
            this.getInventory().addItem(Items.ARROW.getDefaultInstance());
            arrow.moveTo(this.position());
            arrow.discard();
        });
    }

    @Override
    public boolean startRiding(Entity entity) {
        this.setMountUUID(Optional.of(entity.getUUID()));
        return super.startRiding(entity);
    }

    @Override
    public boolean removeWhenFarAway(double p_21542_) {
        return false;
    }

    public boolean canEatItemStack(ItemStack stack){
        ResourceLocation location = ForgeRegistries.ITEMS.getKey(stack.getItem());

        if(RecruitsServerConfig.FoodBlackList.get().contains(location.toString())){
            return false;
        }
        return stack.isEdible();
    }
}
