package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.entities.IHasTargetPriority;
import com.talhanation.recruits.events.RecruitsOnWriteSpawnEggEvent;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.init.ModItems;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.UUID;

public class MessageWriteSpawnEgg implements Message<MessageWriteSpawnEgg> {

    public UUID recruit;

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
        if (!player.isCreative()) return;

        Entity entity = player.serverLevel().getEntity(this.recruit);
        if (!(entity instanceof AbstractRecruitEntity recruitEntity)
                || !recruitEntity.isAlive()
                || player.distanceToSqr(recruitEntity) > 64.0D * 64.0D) return;

        ItemStack itemStack = this.getItemStack(recruitEntity.getType());
        CompoundTag entityTag = this.fillRecruitsInfo(new CompoundTag(), recruitEntity);
        CompoundTag itemTag = new CompoundTag();
        itemTag.put("EntityTag", entityTag);
        itemStack.setTag(itemTag);
        player.getInventory().setPickedItem(itemStack);
        player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
        player.inventoryMenu.broadcastChanges();
    }

    public CompoundTag fillRecruitsInfo(CompoundTag entityTag, AbstractRecruitEntity recruitEntity) {
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(recruitEntity.getType());
        if (typeId != null) entityTag.putString("id", typeId.toString());

        Component customName = recruitEntity.getCustomName();
        if (customName != null) {
            entityTag.putString("CustomName", Component.Serializer.toJson(customName));
            entityTag.putBoolean("CustomNameVisible", recruitEntity.isCustomNameVisible());
            entityTag.putString("Name", customName.getString());
        }

        Team team = recruitEntity.getTeam();
        if (team != null) {
            entityTag.putString("Team", team.getName());
        }

        entityTag.putInt("AggroState", recruitEntity.getState());
        entityTag.putInt("FollowState", recruitEntity.getFollowState());
        entityTag.putBoolean("ShouldBlock", recruitEntity.getShouldBlock());
        entityTag.putBoolean("ShouldRest", recruitEntity.getShouldRest());
        entityTag.putBoolean("ShouldRanged", recruitEntity.getShouldRanged());
        if(recruitEntity.getGroup() != null) entityTag.putUUID("Group", recruitEntity.getGroup());
        entityTag.putInt("Variant", recruitEntity.getVariant());
        entityTag.putBoolean("Listen", recruitEntity.getListen());
        entityTag.putInt("Xp", recruitEntity.getXp());
        entityTag.putInt("Level", recruitEntity.getXpLevel());
        entityTag.putInt("Kills", recruitEntity.getKills());
        entityTag.putFloat("Hunger", recruitEntity.getHunger());
        entityTag.putFloat("Moral", recruitEntity.getMorale());
        entityTag.putBoolean("isOwned", recruitEntity.getIsOwned());
        entityTag.putInt("Cost", recruitEntity.getCost());
        entityTag.putByte("Color", (byte) recruitEntity.getColor());
        entityTag.putByte("Biome", (byte) recruitEntity.getBiome());
        entityTag.put("Attributes", recruitEntity.getAttributes().save());

        if (recruitEntity.getOwnerUUID() != null) {
            entityTag.putUUID("OwnerUUID", recruitEntity.getOwnerUUID());
        }

        if (recruitEntity.getUpkeepUUID() != null) {
            entityTag.putUUID("UpkeepUUID", recruitEntity.getUpkeepUUID());
        }

        if (recruitEntity.getUpkeepPos() != null) {
            entityTag.putInt("UpkeepPosX", recruitEntity.getUpkeepPos().getX());
            entityTag.putInt("UpkeepPosY", recruitEntity.getUpkeepPos().getY());
            entityTag.putInt("UpkeepPosZ", recruitEntity.getUpkeepPos().getZ());
        }

        if (recruitEntity instanceof ICompanion companion) {
            entityTag.putString("CompanionOwnerName", companion.getOwnerName());
        }
        if (recruitEntity instanceof IHasTargetPriority priorityRecruit) {
            entityTag.putInt("TargetPriority", priorityRecruit.getTargetPriority());
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

        MinecraftForge.EVENT_BUS.post(new RecruitsOnWriteSpawnEggEvent(recruitEntity, entityTag));

        return entityTag;
    }

    public ItemStack getItemStack(EntityType<?> type){
        if (type == ModEntityTypes.RECRUIT_SHIELDMAN.get()) return new ItemStack(ModItems.RECRUIT_SHIELD_SPAWN_EGG.get());
        if (type == ModEntityTypes.BOWMAN.get()) return new ItemStack(ModItems.BOWMAN_SPAWN_EGG.get());
        if (type == ModEntityTypes.CROSSBOWMAN.get()) return new ItemStack(ModItems.CROSSBOWMAN_SPAWN_EGG.get());
        if (type == ModEntityTypes.HORSEMAN.get()) return new ItemStack(ModItems.HORSEMAN_SPAWN_EGG.get());
        if (type == ModEntityTypes.NOMAD.get()) return new ItemStack(ModItems.NOMAD_SPAWN_EGG.get());
        if (type == ModEntityTypes.VILLAGER_NOBLE.get()) return new ItemStack(ModItems.VILLAGER_NOBLE_SPAWN_EGG.get());
        return new ItemStack(ModItems.RECRUIT_SPAWN_EGG.get());
    }

    public MessageWriteSpawnEgg fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
    }
}