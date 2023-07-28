package com.talhanation.recruits.items;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;


public class RecruitsSpawnEgg extends ForgeSpawnEggItem {
    private final Supplier<? extends EntityType<? extends AbstractRecruitEntity>> entityType;

    public RecruitsSpawnEgg(Supplier<? extends EntityType<? extends AbstractRecruitEntity>> entityType, int primaryColor, int secondaryColor, Properties properties) {
        super(entityType, primaryColor, secondaryColor, properties);
        this.entityType = entityType;
    }
    @Override
    public @NotNull EntityType<?> getType(CompoundTag compound){
        if(compound != null && compound.contains("EntityTag", 10)) {
            CompoundTag entityTag = compound.getCompound("EntityTag");

            if(entityTag.contains("id", 8)) {
                return EntityType.byString(entityTag.getString("id")).orElse(this.entityType.get());
            }


        }
        return this.entityType.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        else{
            ItemStack stack = context.getItemInHand();
            BlockPos pos = context.getClickedPos();
            EntityType<?> entitytype = this.getType(stack.getTag());
            Entity entity = entitytype.create(world);


            CompoundTag entityTag = stack.getTag();
            if(entity instanceof AbstractRecruitEntity recruit && entityTag != null) {
                CompoundTag nbt = entityTag.getCompound("EntityTag");
                String name = nbt.getString("Name");
                recruit.setCustomName(Component.literal(name));

                recruit.setXpLevel(nbt.getInt("Level"));
                recruit.setState(nbt.getInt("AggroState"));
                recruit.setFollowState(nbt.getInt("FollowState"));
                recruit.setShouldFollow(nbt.getBoolean("ShouldFollow"));
                recruit.setShouldMount(nbt.getBoolean("ShouldMount"));
                recruit.setShouldBlock(nbt.getBoolean("ShouldBlock"));
                recruit.setShouldProtect(nbt.getBoolean("ShouldProtect"));
                recruit.setFleeing(nbt.getBoolean("Fleeing"));
                recruit.setGroup(nbt.getInt("Group"));
                recruit.setListen(nbt.getBoolean("Listen"));
                recruit.setIsFollowing(nbt.getBoolean("isFollowing"));
                recruit.setIsEating(nbt.getBoolean("isEating"));
                recruit.setXp(nbt.getInt("Xp"));
                recruit.setKills(nbt.getInt("Kills"));
                recruit.setVariant(nbt.getInt("Variant"));
                recruit.setHunger(nbt.getFloat("Hunger"));
                recruit.setMoral(nbt.getFloat("Moral"));
                recruit.setIsOwned(nbt.getBoolean("isOwned"));
                recruit.setCost(nbt.getInt("Cost"));
                recruit.setMountTimer(nbt.getInt("mountTimer"));
                recruit.setUpkeepTimer(nbt.getInt("UpkeepTimer"));

                recruit.setHoldPos(context.getClickedPos());

                /*
                if (nbt.contains("HoldPosX") && nbt.contains("HoldPosY") && nbt.contains("HoldPosZ")) {
                    recruit.setShouldHoldPos(nbt.getBoolean("ShouldHoldPos"));
                    recruit.setHoldPos(new BlockPos (
                            nbt.getInt("HoldPosX"),
                            nbt.getInt("HoldPosY"),
                            nbt.getInt("HoldPosZ")));
                }
                */

                if (nbt.contains("MovePosX") && nbt.contains("MovePosY") && nbt.contains("MovePosZ")) {
                    recruit.setShouldMovePos(nbt.getBoolean("ShouldMovePos"));
                    recruit.setMovePos(new BlockPos (
                            nbt.getInt("MovePosX"),
                            nbt.getInt("MovePosY"),
                            nbt.getInt("MovePosZ")));
                }

                if (nbt.contains("OwnerUUID")){
                    Optional<UUID> uuid = Optional.of(nbt.getUUID("OwnerUUID"));
                    recruit.setOwnerUUID(uuid);
                }

                if (nbt.contains("ProtectUUID")){
                    Optional<UUID> uuid = Optional.of(nbt.getUUID("ProtectUUID"));
                    recruit.setProtectUUID(uuid);
                }

                if (nbt.contains("MountUUID")){
                    Optional<UUID> uuid = Optional.of(nbt.getUUID("MountUUID"));
                    recruit.setMountUUID(uuid);
                }

                if (nbt.contains("UpkeepUUID")){
                    Optional<UUID> uuid = Optional.of(nbt.getUUID("UpkeepUUID"));
                    recruit.setUpkeepUUID(uuid);
                }

                if (nbt.contains("UpkeepPosX") && nbt.contains("UpkeepPosY") && nbt.contains("UpkeepPosZ")) {
                    recruit.setUpkeepPos(new BlockPos (
                            nbt.getInt("UpkeepPosX"),
                            nbt.getInt("UpkeepPosY"),
                            nbt.getInt("UpkeepPosZ")));
                }

                ListTag listnbt = nbt.getList("Items", 10);//muss 10 sein amk sonst nix save
                recruit.createInventory();
                recruit.setPersistenceRequired();

                for (int i = 0; i < listnbt.size(); ++i) {
                    CompoundTag compoundnbt = listnbt.getCompound(i);
                    int j = compoundnbt.getByte("Slot") & 255;
                    if (j < recruit.inventory.getContainerSize()) {
                        recruit.inventory.setItem(j, ItemStack.of(compoundnbt));
                    }
                }

                ListTag armorItems = nbt.getList("ArmorItems", 10);
                for (int i = 0; i < recruit.armorItems.size(); ++i) {
                    int index = recruit.getInventorySlotIndex(Mob.getEquipmentSlotForItem(ItemStack.of(armorItems.getCompound(i))));
                    recruit.setItemSlot(recruit.getEquipmentSlotIndex(index), ItemStack.of(armorItems.getCompound(i)));
                }

                ListTag handItems = nbt.getList("HandItems", 10);
                for (int i = 0; i < recruit.handItems.size(); ++i) {
                    int index = i == 0 ? 5 : 4; //5 = mainhand 4 = offhand
                    recruit.setItemSlot(recruit.getEquipmentSlotIndex(index), ItemStack.of(handItems.getCompound(i)));
                }

                recruit.setPos(pos.getX() + 0.5, pos.getY() + 1 , pos.getZ() + 0.5);
                //if(recruit instanceof BowmanEntity bowman) bowman.reassessWeaponGoal();

                world.addFreshEntity(recruit);

                if (!context.getPlayer().isCreative()) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            else{
                return super.useOn(context);
            }
        }
    }
}