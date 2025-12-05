package com.talhanation.recruits.items;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.scores.PlayerTeam;
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

                fillRecruit(recruit, entityTag, pos);

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

    public static void fillRecruit(AbstractRecruitEntity recruit, CompoundTag entityTag, BlockPos pos){
        if(recruit.getCommandSenderWorld().isClientSide()) return;

        CompoundTag nbt = entityTag.getCompound("EntityTag");

        if (nbt.contains("Team")) {
            String s = nbt.getString("Team");

            PlayerTeam playerteam = recruit.getCommandSenderWorld().getScoreboard().getPlayerTeam(s);
            boolean flag = playerteam != null && recruit.getCommandSenderWorld().getScoreboard().addPlayerToTeam(recruit.getStringUUID(), playerteam);

            if (!flag) {
                Main.LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", (Object)s);
            }
        }
        String name = nbt.getString("Name");
        recruit.setCustomName(Component.literal(name));

        recruit.setXpLevel(nbt.getInt("Level"));
        recruit.setAggroState(nbt.getInt("AggroState"));
        recruit.setFollowState(nbt.getInt("FollowState"));
        recruit.setShouldFollow(nbt.getBoolean("ShouldFollow"));
        recruit.setShouldMount(nbt.getBoolean("ShouldMount"));
        recruit.setShouldBlock(nbt.getBoolean("ShouldBlock"));
        recruit.setShouldProtect(nbt.getBoolean("ShouldProtect"));
        recruit.setFleeing(nbt.getBoolean("Fleeing"));
        if(nbt.contains("Group")) recruit.setGroupUUID(nbt.getUUID("Group"));
        recruit.setListen(nbt.getBoolean("Listen"));
        recruit.setIsFollowing(nbt.getBoolean("isFollowing"));
        recruit.setXp(nbt.getInt("Xp"));
        recruit.setKills(nbt.getInt("Kills"));
        recruit.setVariant(nbt.getInt("Variant"));
        recruit.setHunger(nbt.getFloat("Hunger"));
        recruit.setMoral(nbt.getFloat("Moral"));
        recruit.setIsOwned(nbt.getBoolean("isOwned"));
        recruit.setCost(nbt.getInt("Cost"));
        recruit.setMountTimer(nbt.getInt("mountTimer"));
        recruit.setUpkeepTimer(nbt.getInt("UpkeepTimer"));
        recruit.setColor(nbt.getByte("Color"));
        recruit.setBiome(nbt.getByte("Biome"));

        recruit.setHoldPos(Vec3.atCenterOf(pos));

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

        if(recruit.getGroup() != null){
            RecruitEvents.recruitsGroupsManager.addMember(recruit.getGroup(), recruit.getUUID(), (ServerLevel) recruit.getCommandSenderWorld());
        }
    }
}