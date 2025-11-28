package com.talhanation.recruits.network;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.VillagerEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsHireTrade;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MessageHireFromNobleVillager implements Message<MessageHireFromNobleVillager> {
    private UUID nobleUUID;
    private UUID villagerUUID;
    private int cost;
    private boolean needsVillager;
    private boolean closing;
    private ResourceLocation resource;
    private UUID groupUUID;
    public MessageHireFromNobleVillager() {
    }

    public MessageHireFromNobleVillager(UUID nobleUUID, UUID villagerUUID, RecruitsHireTrade trade, RecruitsGroup group, boolean needsVillager, boolean closing) {
        this.nobleUUID = nobleUUID;
        this.villagerUUID = villagerUUID;
        this.groupUUID = group.getUUID();
        if(trade != null){
            this.cost = trade.cost;
            this.resource = trade.resourceLocation;
        }
        else{
            this.cost = 0;
            this.resource = new ResourceLocation("","");
        }

        this.needsVillager = needsVillager;
        this.closing = closing;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        VillagerNobleEntity villagerNoble = player.getCommandSenderWorld().getEntitiesOfClass(
                VillagerNobleEntity.class,
                player.getBoundingBox().inflate(32.0D),
                noble -> noble.getUUID().equals(this.nobleUUID) && noble.isAlive()
        ).stream().findAny().get();

        if(closing){
            villagerNoble.isTrading(false);
            return;
        }

        RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);

        if(this.needsVillager){
            player.getCommandSenderWorld().getEntitiesOfClass(
                    Villager.class,
                    player.getBoundingBox().inflate(32.0D),
                    villager -> villager.getUUID().equals(this.villagerUUID) && villager.isAlive()
            ).forEach(villager -> this.createRecruit(villager, villagerNoble, player, group));
        }
        else{
            String string = resource.toString();
            Optional<EntityType<?>> optionalType = EntityType.byString(string);
            optionalType.ifPresent(type -> VillagerEvents.spawnHiredRecruit((EntityType<? extends AbstractRecruitEntity>) type, player, group));

            villagerNoble.doTrade(resource);
        }
    }
    public void createRecruit(Villager villager, VillagerNobleEntity villagerNoble, Player player, RecruitsGroup group){
        String string = resource.toString();
        Optional<EntityType<?>> optionalType = EntityType.byString(string);

        optionalType.ifPresent(type -> {
            VillagerEvents.createHiredRecruitFromVillager(villager, (EntityType<? extends AbstractRecruitEntity>) type, player, group);
        });

        villagerNoble.doTrade(resource);
    }

    public MessageHireFromNobleVillager fromBytes(FriendlyByteBuf buf) {
        this.nobleUUID = buf.readUUID();
        this.villagerUUID = buf.readUUID();
        this.cost = buf.readInt();
        this.resource = buf.readResourceLocation();
        this.needsVillager = buf.readBoolean();
        this.closing = buf.readBoolean();
        this.groupUUID = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.nobleUUID);
        buf.writeUUID(this.villagerUUID);
        buf.writeInt(this.cost);
        buf.writeResourceLocation(resource);
        buf.writeBoolean(needsVillager);
        buf.writeBoolean(closing);
        buf.writeUUID(this.groupUUID);
    }
}