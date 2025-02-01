package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.init.ModItems;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageWriteSpawnEgg implements Message<MessageWriteSpawnEgg> {

    private UUID recruit;

    public MessageWriteSpawnEgg() {
    }

    public MessageWriteSpawnEgg(UUID recruit) {
        this.recruit = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                context.getSender().getBoundingBox().inflate(64.0D),
                (recruit) -> recruit.getUUID().equals(this.recruit)
        ).forEach((recruitEntity) -> {
            EntityType<?> type = recruitEntity.getType();
            ItemStack itemStack = null;

            if (type.getDescriptionId().equals("entity.recruits.recruit")) {
                itemStack = new ItemStack(ModItems.RECRUIT_SPAWN_EGG.get());
            } else if (type.getDescriptionId().equals("entity.recruits.recruit_shieldman")) {
                itemStack = new ItemStack(ModItems.RECRUIT_SHIELD_SPAWN_EGG.get());
            } else if (type.getDescriptionId().equals("entity.recruits.bowman")) {
                itemStack = new ItemStack(ModItems.BOWMAN_SPAWN_EGG.get());
            } else if (type.getDescriptionId().equals("entity.recruits.crossbowman")) {
                itemStack = new ItemStack(ModItems.CROSSBOWMAN_SPAWN_EGG.get());
            } else if (type.getDescriptionId().equals("entity.recruits.horseman")) {
                itemStack = new ItemStack(ModItems.HORSEMAN_SPAWN_EGG.get());
            } else if (type.getDescriptionId().equals("entity.recruits.nomad")) {
                itemStack = new ItemStack(ModItems.NOMAD_SPAWN_EGG.get());
            }

            CompoundTag entityTag = new CompoundTag();
            String name = recruitEntity.getName().getString();
            Team team = recruitEntity.getTeam();
            if (team != null) {
                entityTag.putString("Team", team.getName());
            }
            entityTag.putString("Name", name);
            entityTag.putInt("AggroState", recruitEntity.getState());
            entityTag.putInt("FollowState", recruitEntity.getFollowState());
            entityTag.putBoolean("ShouldFollow", recruitEntity.getShouldFollow());
            entityTag.putBoolean("ShouldMount", recruitEntity.getShouldMount());
            entityTag.putBoolean("ShouldProtect", recruitEntity.getShouldProtect());
            entityTag.putBoolean("ShouldBlock", recruitEntity.getShouldBlock());
            entityTag.putInt("Group", recruitEntity.getGroup());
            entityTag.putInt("Variant", recruitEntity.getVariant());
            entityTag.putBoolean("Listen", recruitEntity.getListen());
            entityTag.putBoolean("Fleeing", recruitEntity.getFleeing());
            entityTag.putBoolean("isFollowing", recruitEntity.isFollowing());
            entityTag.putInt("Xp", recruitEntity.getXp());
            entityTag.putInt("Level", recruitEntity.getXpLevel());
            entityTag.putInt("Kills", recruitEntity.getKills());
            entityTag.putFloat("Hunger", recruitEntity.getHunger());
            entityTag.putFloat("Moral", recruitEntity.getMorale());
            entityTag.putBoolean("isOwned", recruitEntity.getIsOwned());
            entityTag.putInt("Cost", recruitEntity.getCost());
            entityTag.putInt("mountTimer", recruitEntity.getMountTimer());
            entityTag.putInt("upkeepTimer", recruitEntity.getUpkeepTimer());
            entityTag.putInt("Color", recruitEntity.getColor());
            entityTag.putInt("Biome", recruitEntity.getBiome());

            if (recruitEntity.getHoldPos() != null) {
                entityTag.putDouble("HoldPosX", recruitEntity.getHoldPos().x());
                entityTag.putDouble("HoldPosY", recruitEntity.getHoldPos().y());
                entityTag.putDouble("HoldPosZ", recruitEntity.getHoldPos().z());
                entityTag.putBoolean("ShouldHoldPos", recruitEntity.getShouldHoldPos());
            }

            if (recruitEntity.getMovePos() != null) {
                entityTag.putInt("MovePosX", recruitEntity.getMovePos().getX());
                entityTag.putInt("MovePosY", recruitEntity.getMovePos().getY());
                entityTag.putInt("MovePosZ", recruitEntity.getMovePos().getZ());
                entityTag.putBoolean("ShouldMovePos", recruitEntity.getShouldMovePos());
            }

            if (recruitEntity.getOwnerUUID() != null) {
                entityTag.putUUID("OwnerUUID", recruitEntity.getOwnerUUID());
            }

            if (recruitEntity.getMountUUID() != null) {
                entityTag.putUUID("MountUUID", recruitEntity.getMountUUID());
            }

            if (recruitEntity.getProtectUUID() != null) {
                entityTag.putUUID("ProtectUUID", recruitEntity.getProtectUUID());
            }

            if (recruitEntity.getUpkeepUUID() != null) {
                entityTag.putUUID("UpkeepUUID", recruitEntity.getUpkeepUUID());
            }

            if (recruitEntity.getUpkeepPos() != null) {
                entityTag.putInt("UpkeepPosX", recruitEntity.getUpkeepPos().getX());
                entityTag.putInt("UpkeepPosY", recruitEntity.getUpkeepPos().getY());
                entityTag.putInt("UpkeepPosZ", recruitEntity.getUpkeepPos().getZ());
            }

            ListTag listnbt = new ListTag();
            for (int i = 0; i < recruitEntity.inventory.getContainerSize(); ++i) {
                ItemStack itemstack = recruitEntity.inventory.getItem(i);
                if (!itemstack.isEmpty()) {
                    CompoundTag compoundnbt = new CompoundTag();
                    compoundnbt.putByte("Slot", (byte) i);
                    itemstack.save(compoundnbt);
                    listnbt.add(compoundnbt);
                }
            }
            entityTag.put("Items", listnbt);

            ListTag listtag = new ListTag();
            for (ItemStack itemstack : recruitEntity.armorItems) {
                CompoundTag compoundtag = new CompoundTag();
                if (!itemstack.isEmpty()) {
                    itemstack.save(compoundtag);
                }

                listtag.add(compoundtag);
            }

            entityTag.put("ArmorItems", listtag);
            ListTag listtag1 = new ListTag();

            for (ItemStack itemstack1 : recruitEntity.handItems) {
                CompoundTag compoundtag1 = new CompoundTag();
                if (!itemstack1.isEmpty()) {
                    itemstack1.save(compoundtag1);
                }

                listtag1.add(compoundtag1);
            }

            entityTag.put("HandItems", listtag1);


            CompoundTag itemTag = new CompoundTag();
            itemTag.put("EntityTag", entityTag);

            if (itemStack != null && player.getMainHandItem().isEmpty()) {
                itemStack.setTag(itemTag);
                player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
            }
        });
    }

    public MessageWriteSpawnEgg fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
    }
}