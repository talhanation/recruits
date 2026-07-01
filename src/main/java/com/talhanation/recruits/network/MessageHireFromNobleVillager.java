package com.talhanation.recruits.network;
import de.maxhenkel.corelib.net.NetUtils;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.VillagerEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsHireTrade;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MessageHireFromNobleVillager implements Message<MessageHireFromNobleVillager> {
    public static final CustomPacketPayload.Type<MessageHireFromNobleVillager> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagehirefromnoblevillager"));
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
            this.resource = ResourceLocation.fromNamespaceAndPath("","");
        }

        this.needsVillager = needsVillager;
        this.closing = closing;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        ServerLevel serverLevel = player.serverLevel();
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
            ).forEach(villager -> this.createRecruit(serverLevel, villager, villagerNoble, player, group));
        }
        else{
            String string = resource.toString();
            Optional<EntityType<?>> optionalType = EntityType.byString(string);
            optionalType.ifPresent(type -> VillagerEvents.spawnHiredRecruit(serverLevel, (EntityType<? extends AbstractRecruitEntity>) type, player, group));

            villagerNoble.doTrade(resource);
        }

        String stringID = player.getTeam() != null ? player.getTeam().getName() : "";
        boolean canHire = RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(stringID, player.getUUID());
        NetUtils.sendTo(player, new MessageToClientUpdateHireState(canHire));
    }
    public void createRecruit(ServerLevel serverLevel, Villager villager, VillagerNobleEntity villagerNoble, Player player, RecruitsGroup group){
        String string = resource.toString();
        Optional<EntityType<?>> optionalType = EntityType.byString(string);

        optionalType.ifPresent(type -> {
            VillagerEvents.createHiredRecruitFromVillager(serverLevel, villager, (EntityType<? extends AbstractRecruitEntity>) type, player, group);
        });

        villagerNoble.doTrade(resource);
    }

    public MessageHireFromNobleVillager fromBytes(RegistryFriendlyByteBuf buf) {
        this.nobleUUID = buf.readUUID();
        this.villagerUUID = buf.readUUID();
        this.cost = buf.readInt();
        this.resource = buf.readResourceLocation();
        this.needsVillager = buf.readBoolean();
        this.closing = buf.readBoolean();
        this.groupUUID = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.nobleUUID);
        buf.writeUUID(this.villagerUUID);
        buf.writeInt(this.cost);
        buf.writeResourceLocation(resource);
        buf.writeBoolean(needsVillager);
        buf.writeBoolean(closing);
        buf.writeUUID(this.groupUUID);
    }

    @Override
    public CustomPacketPayload.Type<MessageHireFromNobleVillager> type() {
        return TYPE;
    }
}