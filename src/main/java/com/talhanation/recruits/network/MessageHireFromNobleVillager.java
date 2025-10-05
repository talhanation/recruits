package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.VillagerEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.world.RecruitsHireTrade;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MessageHireFromNobleVillager implements Message<MessageHireFromNobleVillager> {
    private UUID noble_uuid;
    private UUID villager_uuid;
    private int cost;
    private ResourceLocation resource;
    public MessageHireFromNobleVillager() {
    }

    public MessageHireFromNobleVillager(UUID noble_uuid, UUID villager_uuid, RecruitsHireTrade trade) {
        this.noble_uuid = noble_uuid;
        this.villager_uuid = villager_uuid;
        this.cost = trade.cost;
        this.resource = trade.recruitType;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());

        player.getCommandSenderWorld().getEntitiesOfClass(
                Villager.class,
                player.getBoundingBox().inflate(32.0D),
                villager -> villager.getUUID().equals(this.villager_uuid) && villager.isAlive()
        ).forEach(villager -> this.createRecruit(villager, player));
    }
    public void createRecruit(Villager villager, Player player){
        String string = resource.toString();
        Optional<EntityType<?>> optionalType = EntityType.byString(string);

        optionalType.ifPresent(type -> VillagerEvents.createHiredRecruit(villager, (EntityType<? extends AbstractRecruitEntity>) type, player));

        player.getCommandSenderWorld().getEntitiesOfClass(
                VillagerNobleEntity.class,
                player.getBoundingBox().inflate(32.0D),
                noble -> noble.getUUID().equals(this.noble_uuid) && noble.isAlive()
        ).forEach(noble -> noble.doTrade(resource));
    }
    public MessageHireFromNobleVillager fromBytes(FriendlyByteBuf buf) {
        this.noble_uuid = buf.readUUID();
        this.villager_uuid = buf.readUUID();
        this.cost = buf.readInt();
        this.resource = buf.readResourceLocation();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.noble_uuid);
        buf.writeUUID(this.villager_uuid);
        buf.writeInt(this.cost);
        buf.writeResourceLocation(resource);
    }
}