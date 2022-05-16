package com.talhanation.recruits.entities;
//ezgi&talha kantar

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.ai.*;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.MessageRecruitGui;
import net.minecraft.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.*;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;

public abstract class AbstractRecruitEntity extends AbstractInventoryEntity{
    private static final EntityDataAccessor<Integer> DATA_REMAINING_ANGER_TIME = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FOLLOW_STATE = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SHOULD_FOLLOW = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOULD_HOLD_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> HOLD_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> MOVE_POS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> MOVE = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LISTEN = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> isFollowing = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> MOUNT = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> GROUP = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> XP = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KILLS = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> isEating = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLEEING = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(AbstractRecruitEntity.class, EntityDataSerializers.STRING);

    //private static final DataParameter<ItemStack> OFFHAND_ITEM_SAVE = EntityDataManager.defineId(AbstractRecruitEntity.class, DataSerializers.ITEM_STACK);

    public ItemStack beforeFoodItem;

    public AbstractRecruitEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
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
    private void resetItemInHand() {
        this.setItemInHand(InteractionHand.OFF_HAND, this.beforeFoodItem);
        this.setSlot(10, this.beforeFoodItem);
        this.beforeFoodItem = null;
    }

    public void tick() {
        super.tick();
        updateSwingTime();
        updateSwimming();

        /*
        if (getOwner() != null) {
            this.getOwner().sendMessage(new StringTextComponent("Health: " + getAttribute(Attributes.ATTACK_DAMAGE).getValue()), getOwner().getUUID());
            this.getOwner().sendMessage(new StringTextComponent("Attack: " + getAttribute(Attributes.ATTACK_DAMAGE).getValue()), getOwner().getUUID());
            this.getOwner().sendMessage(new StringTextComponent("Speed: " + getAttribute(Attributes.ATTACK_DAMAGE).getValue()), getOwner().getUUID());
        }
        */
        if (this.getIsEating() && !this.isUsingItem()) {
            if (beforeFoodItem != null) resetItemInHand();
            setIsEating(false);
        }
        /*
        if (this.isBlocking()) {
            this.blockCooldown++;
        }

        if (blockCooldown >= 100){
            blockCooldown = 0;
        }

        if (blockCooldown < 50){
            canBlock = true;
        }else
            canBlock = false;


        if (getOwner() != null)
        this.getOwner().sendMessage(new StringTextComponent("Block Timer: " + blockCooldown), getOwner().getUUID());
        */

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
        getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus", this.random.nextDouble() * 1.5, AttributeModifier.Operation.MULTIPLY_BASE));
        getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("attack_bonus", this.random.nextDouble(), AttributeModifier.Operation.MULTIPLY_BASE));
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
        //this.goalSelector.addGoal(0, new (this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RecruitEatGoal(this));
        //this.goalSelector.addGoal(2, new RecruitMountGoal(this, 1.2D, 32.0F));
        this.goalSelector.addGoal(3, new RecruitMoveToPosGoal(this, 1.2D, 32.0F));
        this.goalSelector.addGoal(4, new RecruitFollowOwnerGoal(this, 1.2D, this.getFollowStartDistance(), 3.0F));
        this.goalSelector.addGoal(5, new RecruitMeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(6, new RecruitHoldPosGoal(this, 1.0D, 32.0F));
        this.goalSelector.addGoal(7, new RecruitMoveTowardsTargetGoal(this, 1.15D, 24.0F));
        this.goalSelector.addGoal(8, new RecruitPickupWantedItemGoal(this));
        this.goalSelector.addGoal(9, new MoveBackToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(10, new GolemRandomStrollInVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(2, new RecruitRaidNearestAttackableTargetGoal<>(this, LivingEntity.class, false));
        this.targetSelector.addGoal(2, new RecruitAggresiveNearestAttackableTargetGoal<>(this, Player.class, false));

        this.targetSelector.addGoal(0, new RecruitOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(1, (new RecruitHurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this));

        //this.targetSelector.addGoal(4, new RecruitNearestAttackableTargetGoal<>(this, AbstractOrderAbleEntity.class, false));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, 10, true, false, (target) -> {
            return (this.getState() != 3);
        }));

        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, (target) -> {
            return !(target instanceof Creeper) && (this.getState() != 3);
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
        this.entityData.define(SHOULD_HOLD_POS, false);
        this.entityData.define(FLEEING, false);
        this.entityData.define(STATE, 0);
        this.entityData.define(XP, 0);
        this.entityData.define(KILLS, 0);
        this.entityData.define(LEVEL, 1);
        this.entityData.define(FOLLOW_STATE, 0);
        this.entityData.define(HOLD_POS, Optional.empty());
        this.entityData.define(MOVE_POS, Optional.empty());
        this.entityData.define(MOVE, true);
        this.entityData.define(LISTEN, true);
        this.entityData.define(MOUNT, Optional.empty());
        this.entityData.define(isFollowing, false);
        this.entityData.define(isEating, true);
        //STATE
        // 0 = NEUTRAL
        // 1 = AGGRESSIVE
        // 2 = RAID

        //FOLLOW
        //0 = wander
        //1 = follow
        //2 = hold position
        //3 = back to position
        //4 = hold my position

    }
    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("AggroState", this.getState());
        nbt.putBoolean("ShouldFollow", this.getShouldFollow());
        nbt.putInt("Group", this.getGroup());
        nbt.putBoolean("Listen", this.getListen());
        nbt.putBoolean("Fleeing", this.getFleeing());
        nbt.putBoolean("isFollowing", this.isFollowing());
        nbt.putBoolean("isEating", this.getIsEating());
        nbt.putInt("Xp", this.getXp());
        nbt.putInt("Level", this.getXpLevel());
        nbt.putInt("Kills", this.getKills());

        if(this.getHoldPos() != null){
            nbt.putInt("HoldPosX", this.getHoldPos().getX());
            nbt.putInt("HoldPosY", this.getHoldPos().getY());
            nbt.putInt("HoldPosZ", this.getHoldPos().getZ());
            nbt.putBoolean("ShouldHoldPos", this.getShouldHoldPos());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setXpLevel(nbt.getInt("Level"));
        this.setState(nbt.getInt("AggroState"));
        this.setShouldFollow(nbt.getBoolean("ShouldFollow"));
        this.setFleeing(nbt.getBoolean("Fleeing"));
        this.setGroup(nbt.getInt("Group"));
        this.setListen(nbt.getBoolean("Listen"));
        this.setIsFollowing(nbt.getBoolean("isFollowing"));
        this.setIsEating(nbt.getBoolean("isEating"));
        this.setXp(nbt.getInt("Xp"));
        this.setKills(nbt.getInt("Kills"));

        if (nbt.contains("HoldPosX") && nbt.contains("HoldPosY") && nbt.contains("HoldPosZ")) {
            this.setShouldHoldPos(nbt.getBoolean("ShouldHoldPos"));
            this.setHoldPos(new BlockPos (
                    nbt.getInt("HoldPosX"),
                    nbt.getInt("HoldPosY"),
                    nbt.getInt("HoldPosZ")));
        }
    }


    ////////////////////////////////////GET////////////////////////////////////

    public float getAttackDamage(){
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public BlockPos getRecruitOnPos(){
        return getOnPos();
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
        return entityData.get(isEating);
    }

    public boolean getShouldHoldPos() {
        return entityData.get(SHOULD_HOLD_POS);
    }

    public boolean getShouldFollow() {
        return entityData.get(SHOULD_FOLLOW);
    }

    public boolean isFollowing(){
        return entityData.get(isFollowing);
    }

    public int getState() {
        return entityData.get(STATE);
    }

    public int getGroup() {
        return entityData.get(GROUP);
    }

    public int getFollowState(){
        return entityData.get(FOLLOW_STATE);
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

    protected float getStandingEyeHeight(Pose pos, EntityDimensions size) {
        return size.height * 0.9F;
    }

    public int getMaxHeadXRot() {
        return this.isInSittingPose() ? 20 : super.getMaxHeadXRot();
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

    /*
    public ItemStack getOffHandItemSave(){
        return  entityData.get(OFFHAND_ITEM_SAVE);
    }

     */

    ////////////////////////////////////SET////////////////////////////////////

    public void setFleeing(boolean bool){
        entityData.set(FLEEING, bool);
    }

    public void disband(Player player){
        player.sendMessage(new TextComponent(this.getName().getString() + ": " +"Then this is where we part ways."), player.getUUID());
        this.setTame(false);
        this.setTarget(null);
        this.setOwned(false);
        this.setOwnerUUID(null);
        CommandEvents.saveRecruitCount(player, CommandEvents.getSavedRecruitCount(player) - 1);
    }

    public void addXpLevel(int level){
        int currentLevel = this.getXpLevel();
        int newLevel = currentLevel + level;
        makelevelUpSound();
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
        entityData.set(isEating, bool);
    }

    public void setShouldHoldPos(boolean bool){
        entityData.set(SHOULD_HOLD_POS, bool);
    }

    public void setShouldFollow(boolean bool){
            entityData.set(SHOULD_FOLLOW, bool);
    }

    public void setIsFollowing(boolean bool){
        entityData.set(isFollowing, bool);
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
                break;
            case 3:
                setTarget(null);//wird nur 1x aufgerufen
                break;
        }
        entityData.set(STATE, state);
    }

    public void setFollowState(int state){
        switch (state){
            case 0:
                setShouldFollow(false);
                setShouldHoldPos(false);
                break;

            case 1:
                setShouldFollow(true);
                setShouldHoldPos(false);
                break;

            case 2:
                setShouldFollow(false);
                setShouldHoldPos(true);
                clearHoldPos();
                setHoldPos(getOnPos());
                break;

            case 3:
                setShouldFollow(false);
                setShouldHoldPos(true);
                break;

            case 4:
                setShouldFollow(false);
                setShouldHoldPos(true);
                clearHoldPos();
                setHoldPos(this.getOwner().blockPosition());
                state = 3;
                break;
        }
        entityData.set(FOLLOW_STATE, state);
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

    public void setEquipment(){}

    public void setMount(UUID uuid){
        entityData.set(MOUNT, Optional.of(uuid));
    }

    public abstract void initSpawn();

    ////////////////////////////////////ON FUNCTIONS////////////////////////////////////

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (this.level.isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isTame() || isInSittingPose() || item == Items.BONE && !this.isTame();
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else {
            if ((this.isTame() && player.getUUID().equals(this.getOwnerUUID()))) {
                if (player.isCrouching()) {
                    checkItemsInInv();
                    openGUI(player);
                    return InteractionResult.SUCCESS;
                }
                if(!player.isCrouching()) {
                    int state = this.getFollowState();
                    switch (state) {
                        default:
                        case 0:
                            setFollowState(1);
                            player.sendMessage(new TextComponent(this.getName().getString() + ": " +"I will follow you"), player.getUUID());
                            return InteractionResult.SUCCESS;
                        case 1:
                            setFollowState(4);
                            player.sendMessage(new TextComponent(this.getName().getString() + ": " +"I will hold your Position"), player.getUUID());
                            return InteractionResult.SUCCESS;
                        case 4:
                            setFollowState(0);
                            player.sendMessage(new TextComponent(this.getName().getString() + ": " +"I will stay here around"), player.getUUID());
                            return InteractionResult.SUCCESS;
                    }
                }

            }

            else if (item == Items.EMERALD && !this.isTame() && playerHasEnoughEmeralds(player) && playerCanRecruit(player)) {
                if (!player.abilities.instabuild) {
                    if (!player.isCreative()) {
                        itemstack.shrink(recruitCosts());
                    }
                }
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setOrderedToSit(false);
                this.setFollowState(1);
                this.setState(0);
                int currentRecruits = CommandEvents.getSavedRecruitCount(player);
                CommandEvents.saveRecruitCount(player,  currentRecruits + 1);

                return InteractionResult.SUCCESS;
            }

            else if (item == Items.EMERALD && !this.isTame() && !playerHasEnoughEmeralds(player) && playerCanRecruit(player)) {
                player.sendMessage(new TextComponent(this.getName().getString() + ": " + "You need " + recruitCosts() + " Emeralds to recruit me!"), player.getUUID());
                return InteractionResult.SUCCESS;
            }

            else if (!this.isTame() && item != Items.EMERALD && playerCanRecruit(player)) {
                int i = this.random.nextInt(5);
                switch (i) {
                    case 0:
                        player.sendMessage(new TextComponent(this.getName().getString() + ": " +" Hello my Friend."), player.getUUID());
                        return InteractionResult.SUCCESS;

                    case 1:
                        player.sendMessage(new TextComponent(this.getName().getString() + ": " +"It's a honor for me to protect you."), player.getUUID());
                        return InteractionResult.SUCCESS;

                        default:
                        player.sendMessage(new TextComponent(this.getName().getString() + ": " +"I will defend you from Monsters!"), player.getUUID());
                        return InteractionResult.SUCCESS;
                }
            }
            else if (!playerCanRecruit(player)) {
                player.sendMessage(new TextComponent(this.getName().getString() + ": " +" You reached the maximum limit you can recruit."), player.getUUID());
                return InteractionResult.SUCCESS;
            }

            return super.mobInteract(player, hand);
        }
    }

    private boolean playerHasEnoughEmeralds(Player player) {
        int recruitCosts = this.recruitCosts();
        int emeraldCount = player.getItemInHand(InteractionHand.MAIN_HAND).getCount();
        if (emeraldCount >= recruitCosts){
            return true;
        }
        if (player.isCreative()){
            return true;
        }
        else return false;
    }

    private boolean playerCanRecruit(Player player) {
        return  (CommandEvents.getSavedRecruitCount(player) < RecruitsModConfig.MaxRecruitsForPlayer.get());
    }

    ////////////////////////////////////ATTACK FUNCTIONS////////////////////////////////////

    public boolean hurt(DamageSource dmg, float amt) {
        if (this.isInvulnerableTo(dmg)) {
            return false;
        } else {
            Entity entity = dmg.getEntity();
            this.setOrderedToSit(false);
            if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractArrow)) {
                amt = (amt + 1.0F) / 2.0F;
            }
            //this.addXp(1);
            //this.checkLevel();

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
        this.damageMainHandItem();
        return flag;
    }

    public void addLevelBuffs(){
        int level = getXpLevel();
        if(level <= 10){
            getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 3D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier("attack_bonus_level", 0.15D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(new AttributeModifier("knockback_bonus_level", 0.01D, AttributeModifier.Operation.ADDITION));
            getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier("speed_bonus_level", 0.01D, AttributeModifier.Operation.ADDITION));
        }
        if(level > 10){
            getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus_level", 2D, AttributeModifier.Operation.ADDITION));
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
        if (!(target instanceof Creeper) && !(target instanceof Ghast)) {
            if (target instanceof AbstractRecruitEntity) {

                AbstractRecruitEntity otherRecruit = (AbstractRecruitEntity)target;
                // || otherRecruit.getOwner().getTeam() != owner.getTeam() fix
                return otherRecruit.getOwner() != owner ;
            } else if (target instanceof Player && owner instanceof Player && !((Player)owner).canHarmPlayer((Player)target)) {
                return false;
            } else if (target instanceof AbstractHorse && ((AbstractHorse)target).isTamed()) {
                return false;
            } else if (target instanceof AbstractOrderAbleEntity && ((AbstractOrderAbleEntity)target).getIsInOrder() && ((AbstractOrderAbleEntity)target).getOwner() != owner) {
                return true;
            } else if (target instanceof RecruitHorseEntity) {
                return false;
            } else {
                return !(target instanceof TamableAnimal) || !((TamableAnimal)target).isTame();
            }
        } else {
            return false;
        }
    }

    public void die(DamageSource dmg) {
        super.die(dmg);
        LivingEntity owner = this.getOwner();
        if (owner instanceof Player){
            Player player = (Player) owner;
            CommandEvents.saveRecruitCount(player, CommandEvents.getSavedRecruitCount(player) - 1);
        }
    }


    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

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
        }
    }

    public void makelevelUpSound() {
        this.level.playSound(null, this.getX(), this.getY() + 4 , this.getZ(), SoundEvents.VILLAGER_YES, this.getSoundSource(), 15.0F, 0.8F + 0.4F * this.random.nextFloat());
        this.level.playSound(null, this.getX(), this.getY() + 4 , this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), 15.0F, 0.8F + 0.4F * this.random.nextFloat());
    }

    public boolean isOwnedByThisPlayer(AbstractRecruitEntity recruit, Player player){
        return  (recruit.getOwnerUUID() == player.getUUID());
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }
    public abstract int recruitCosts();

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void spawnTamingParticles(boolean p_70908_1_) {
        ParticleOptions iparticledata = ParticleTypes.HAPPY_VILLAGER;
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

    protected void hurtArmor(DamageSource damageSource, float damage) {
        if (damage >= 0.0F) {
            damage = damage / 4.0F;
            if (damage < 1.0F) {
                damage = 1.0F;
            }
            for (int i = 11; i < 15; ++i) {//11,12,13,14 = armor
                ItemStack itemstack = this.inventory.getItem(i);
                if ((!damageSource.isFire() || !itemstack.getItem().isFireResistant()) && itemstack.getItem() instanceof ArmorItem) {
                    itemstack.setDamageValue((int) damage);
                }
            }
        }
    }

    protected void damageMainHandItem() {
        ItemStack itemstack = this.inventory.getItem(9);// 10 = hoffhand slot
        if (itemstack.getItem().isDamageable(itemstack)) {
            itemstack.setDamageValue(1);
        }
    }

    @Override
    public void killed(ServerLevel p_241847_1_, LivingEntity p_241847_2_) {
        super.killed(p_241847_1_, p_241847_2_);
        this.addXp(5);
        this.setKills(this.getKills() + 1);
    }

    @Override
    protected void hurtCurrentlyUsedShield(float damage) {
        if (this.useItem.isShield(this)) {
            int i = 1 + Mth.floor(damage);
            InteractionHand hand = this.getUsedItemHand();
            this.useItem.hurtAndBreak(i, this, (entity) -> entity.broadcastBreakEvent(hand));
            if (this.useItem.isEmpty()) {
                if (hand == InteractionHand.MAIN_HAND) {
                    this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    this.setSlot(9, ItemStack.EMPTY);
                } else {
                    this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    this.setSlot(10, ItemStack.EMPTY);
                }
                this.useItem = ItemStack.EMPTY;
                this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                this.setSlot(10, ItemStack.EMPTY);
                this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            }

            ItemStack itemstack = this.inventory.getItem(10);// 10 = hoffhand slot
            if (itemstack.getItem() instanceof ShieldItem) {
                itemstack.setDamageValue((int) damage);
            }
        }
    }

    public static boolean canDamageTarget(AbstractRecruitEntity recruit, LivingEntity target) {
        if (recruit.isTame() && target instanceof AbstractRecruitEntity) {
            return !Objects.equals(recruit.getOwnerUUID(), ((AbstractRecruitEntity) target).getOwnerUUID());
        } else
            return true;
    }

    @Override
    public void openGUI(Player player) {
        this.navigation.stop();

        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return getName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new RecruitInventoryContainer(i, AbstractRecruitEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageRecruitGui(player, this.getUUID()));
        }
    }
}
