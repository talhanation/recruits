package com.talhanation.recruits.entities;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.inventory.PatrolLeaderContainer;
import com.talhanation.recruits.network.MessageOpenSpecialScreen;
import com.talhanation.recruits.network.MessageToClientUpdateLeaderScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class PatrolLeaderEntity extends AbstractLeaderEntity {
    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public PatrolLeaderEntity(EntityType<? extends AbstractLeaderEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    protected void registerGoals() {
       super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }


    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setDropEquipment();
        this.setPersistenceRequired();
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {//TODO: add ranged combat
        if((itemStack.getItem() instanceof SwordItem && this.getMainHandItem().isEmpty()) ||
          (itemStack.getItem() instanceof ShieldItem) && this.getOffhandItem().isEmpty())
            return !hasSameTypeOfItem(itemStack);

        else return super.wantsToPickUp(itemStack);
    }

    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack){
        return !(itemStack.getItem() instanceof CrossbowItem || itemStack.getItem() instanceof BowItem); //TODO: add ranged combat
    }

    @Override
    public AbstractRecruitEntity get() {
        return this;
    }

    public void openSpecialGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return PatrolLeaderEntity.this.getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new PatrolLeaderContainer(i, playerEntity,  PatrolLeaderEntity.this);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(this.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenSpecialScreen(player, this.getUUID()));
        }

        if (player instanceof ServerPlayer) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateLeaderScreen(this.WAYPOINTS, this.WAYPOINT_ITEMS));
        }
    }
}










