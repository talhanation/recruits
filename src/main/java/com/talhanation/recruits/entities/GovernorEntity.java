package com.talhanation.recruits.entities;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorController;
import com.talhanation.recruits.governor.GovernorGarrison;
import com.talhanation.recruits.governor.GovernorManager;
import com.talhanation.recruits.network.MessageOpenSpecialScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The Governor: a companion that manages one claim (garrison, posts, treasury, recruiting, taxes).
 * Everything he manages is stored per claim in {@link ClaimGovernorData}, not on this entity.
 * He belongs to one player, cannot defect, and only fights in self-defence.
 */
public class GovernorEntity extends AbstractRecruitEntity implements ICompanion {

    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(GovernorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> GOVERNED_CLAIM = SynchedEntityData.defineId(GovernorEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> RULING = SynchedEntityData.defineId(GovernorEntity.class, EntityDataSerializers.BOOLEAN);

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public final GovernorController controller;
    /** Currency the Governor carries between treasury and noble / village bell. */
    private int purse;
    /** Items the Governor carries from the treasury to a noble (delivery quests). */
    private final List<ItemStack> carried = new ArrayList<>();
    /** Day on which the Governor last got his daily experience for governing. */
    private long lastXpDay = -1;
    @Nullable
    private BlockPos homePos;

    public GovernorEntity(EntityType<? extends AbstractRecruitEntity> entityType, Level world) {
        super(entityType, world);
        this.controller = new GovernorController(this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(GOVERNED_CLAIM, Optional.empty());
        this.entityData.define(RULING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new GovernorTaskGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getCommandSenderWorld().isClientSide() && this.isAlive()) {
            controller.tick();
        }
    }

    /** Self-defence only: no active target search. Retaliation comes from the hurt-by-target goals. */
    @Override
    public void searchForTargets() {
    }

    /**
     * While he governs, the Governor only fights whoever attacked him. Calls for help of the owner or the
     * team (alertTeam, owner-hurt goals) are ignored, so he never leaves his post to join a fight.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && this.isRuling() && target != this.getLastHurtByMob()) return;
        super.setTarget(target);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!this.getCommandSenderWorld().isClientSide() && this.isOwned()
                && player.getUUID().equals(this.getOwnerUUID()) && !player.isCrouching()) {
            this.openSpecialGUI(player);
            this.getNavigation().stop();
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void openSpecialGUI(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.getUUID().equals(this.getOwnerUUID())) return;
            GovernorManager.sendGovernorSnapshot(serverPlayer, this, true);
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenSpecialScreen(player, this.getUUID()));
        }
    }

    @Override
    public void die(@NotNull DamageSource dmg) {
        if (!this.getCommandSenderWorld().isClientSide() && this.getCommandSenderWorld() instanceof ServerLevel serverLevel) {
            GovernorManager manager = GovernorManager.get();
            UUID claimId = this.getGovernedClaim();
            if (manager != null && claimId != null) {
                ClaimGovernorData data = manager.getData(claimId);
                if (data != null && this.getUUID().equals(data.getGovernorId())) {
                    data.clearGovernor();
                }
            }
            BlockPos pos = this.blockPosition();
            GovernorManager.notifyPlayer(serverLevel, this.getOwnerUUID(), Component.translatable("chat.recruits.governor.died",
                    this.getName().getString(), pos.getX(), pos.getY(), pos.getZ()));

            // the money he carried between treasury and noble/bell is dropped
            int left = this.purse;
            Item currency = GovernorGarrison.getCurrency();
            while (left > 0) {
                int size = Math.min(left, currency.getMaxStackSize());
                this.spawnAtLocation(new ItemStack(currency, size));
                left -= size;
            }
            this.purse = 0;
            for (ItemStack stack : this.carried) this.spawnAtLocation(stack);
            this.carried.clear();
        }
        super.die(dmg);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("OwnerName", this.getOwnerName());
        if (this.getGovernedClaim() != null) nbt.putUUID("GovernedClaim", this.getGovernedClaim());
        nbt.putInt("Purse", this.purse);
        nbt.putLong("LastXpDay", this.lastXpDay);
        ListTag carriedTag = new ListTag();
        for (ItemStack stack : this.carried) {
            if (!stack.isEmpty()) carriedTag.add(stack.save(new CompoundTag()));
        }
        nbt.put("Carried", carriedTag);
        if (this.homePos != null) {
            nbt.putInt("HomeX", homePos.getX());
            nbt.putInt("HomeY", homePos.getY());
            nbt.putInt("HomeZ", homePos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setOwnerName(nbt.getString("OwnerName"));
        this.setGovernedClaim(nbt.hasUUID("GovernedClaim") ? nbt.getUUID("GovernedClaim") : null);
        this.purse = nbt.getInt("Purse");
        this.lastXpDay = nbt.contains("LastXpDay") ? nbt.getLong("LastXpDay") : -1;
        this.carried.clear();
        ListTag carriedTag = nbt.getList("Carried", Tag.TAG_COMPOUND);
        for (int i = 0; i < carriedTag.size(); i++) {
            ItemStack stack = ItemStack.of(carriedTag.getCompound(i));
            if (!stack.isEmpty()) this.carried.add(stack);
        }
        if (nbt.contains("HomeX")) {
            this.homePos = new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ"));
        }
    }

    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(ForgeMod.SWIM_SPEED.get(), 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(ForgeMod.ENTITY_REACH.get(), 0D)
                .add(Attributes.ATTACK_SPEED);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor world, @NotNull DifficultyInstance difficultyInstance, @NotNull MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData spawnData = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        this.initSpawn();
        return spawnData;
    }

    @Override
    public void initSpawn() {
        if (this.getCustomName() == null || this.getCustomName().getString().isEmpty()) this.setCustomName(Component.literal("Governor"));
        this.setPersistenceRequired();
        if (this.getOwner() != null) this.setOwnerName(this.getOwner().getName().getString());
        AbstractRecruitEntity.applySpawnValues(this);
    }

    public Predicate<ItemEntity> getAllowedItems() {
        return ALLOWED_ITEMS;
    }

    @Override
    public AbstractRecruitEntity get() {
        return this;
    }

    @Override
    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    @Override
    public void setOwnerName(String name) {
        this.entityData.set(OWNER_NAME, name == null ? "" : name);
    }

    @Override
    public boolean isAtMission() {
        return false;
    }

    // ------------------------------------------------------------------ governor state

    @Nullable
    public UUID getGovernedClaim() {
        return this.entityData.get(GOVERNED_CLAIM).orElse(null);
    }

    public void setGovernedClaim(@Nullable UUID claimId) {
        this.entityData.set(GOVERNED_CLAIM, Optional.ofNullable(claimId));
    }

    public boolean isRuling() {
        return this.entityData.get(RULING);
    }

    public void setRuling(boolean ruling) {
        this.entityData.set(RULING, ruling);
    }

    public int getPurse() {
        return purse;
    }

    public void setPurse(int purse) {
        this.purse = Math.max(0, purse);
    }

    /** Items carried from the treasury (delivery quests); live list. */
    public List<ItemStack> getCarried() {
        return carried;
    }

    public boolean hasCarried() {
        for (ItemStack stack : carried) if (!stack.isEmpty()) return true;
        return false;
    }

    public long getLastXpDay() {
        return lastXpDay;
    }

    public void setLastXpDay(long day) {
        this.lastXpDay = day;
    }

    /** Experience for governing (taxes, hiring, quests, every day in office). */
    public void addGovernorXp(int xp) {
        if (xp <= 0) return;
        this.addXp(xp);
        this.checkLevel();
    }

    @Nullable
    public BlockPos getHomePos() {
        return homePos;
    }

    public void setHomePos(@Nullable BlockPos homePos) {
        this.homePos = homePos == null ? null : homePos.immutable();
    }

    /**
     * Walks the Governor to the target of his current task (treasury, noble, recruit, village bell).
     * Higher priority than holding his position, so he leaves his spot only while working.
     */
    public static class GovernorTaskGoal extends Goal {
        private final GovernorEntity governor;
        private int recalcPath;

        public GovernorTaskGoal(GovernorEntity governor) {
            this.governor = governor;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return governor.controller.getTaskTarget() != null && governor.getTarget() == null && !governor.getFleeing();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            recalcPath = 0;
        }

        @Override
        public void stop() {
            governor.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 target = governor.controller.getTaskTarget();
            if (target == null) return;
            governor.getLookControl().setLookAt(target.x, target.y + 1, target.z, 10.0F, (float) governor.getMaxHeadXRot());
            if (governor.distanceToSqr(target) <= 9D) {
                governor.getNavigation().stop();
                governor.controller.onTaskTargetReached();
                return;
            }
            if (--recalcPath <= 0) {
                recalcPath = this.adjustedTickDelay(10);
                governor.getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
            }
            if (governor.horizontalCollision || governor.minorHorizontalCollision) {
                governor.getJumpControl().jump();
            }
        }
    }
}
