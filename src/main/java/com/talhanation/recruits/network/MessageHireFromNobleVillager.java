package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.VillagerEvents;
import com.talhanation.recruits.command.RecruitCommandAuthority;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsHireTrade;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;
import com.talhanation.recruits.network.compat.RecruitsPacketDistributor;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MessageHireFromNobleVillager implements RecruitsMessage<MessageHireFromNobleVillager> {
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
            this.resource = ResourceLocation.withDefaultNamespace("empty");
        }

        this.needsVillager = needsVillager;
        this.closing = closing;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        ServerLevel serverLevel = player.serverLevel();
        VillagerNobleEntity villagerNoble = player.getCommandSenderWorld().getEntitiesOfClass(
                VillagerNobleEntity.class,
                player.getBoundingBox().inflate(32.0D),
                noble -> noble.getUUID().equals(this.nobleUUID) && noble.isAlive()
        ).stream().findFirst().orElse(null);

        if (villagerNoble == null) {
            return;
        }

        if(closing){
            if (villagerNoble.isTradingWith(player)) {
                villagerNoble.isTrading(false);
            }
            return;
        }

        if (!villagerNoble.isTradingWith(player)) {
            return;
        }

        RecruitsGroup group = RecruitCommandAuthority.ownedGroup(player, groupUUID);
        if (group == null) {
            return;
        }

        RecruitsHireTrade trade = getAvailableTrade(villagerNoble);
        if (trade == null) {
            return;
        }

        boolean requiresVillager = RecruitsServerConfig.NobleVillagerNeedsVillagers.get();
        boolean hired;
        if (requiresVillager) {
            var eligibleVillagers = player.getCommandSenderWorld().getEntitiesOfClass(
                    Villager.class,
                    player.getBoundingBox().inflate(32.0D),
                    MessageHireFromNobleVillager::isEligibleHireVillager
            );
            if (eligibleVillagers.size() <= 2) {
                return;
            }
            hired = eligibleVillagers.stream()
                    .filter(villager -> villager.getUUID().equals(this.villagerUUID))
                    .findFirst()
                    .map(villager -> this.createRecruit(serverLevel, villager, player, group, trade.cost))
                    .orElse(false);
        }
        else {
            hired = getRecruitType()
                    .map(type -> VillagerEvents.spawnHiredRecruit(serverLevel, type, player, group, trade.cost))
                    .orElse(false);
        }

        if (hired) {
            villagerNoble.doTrade(resource);
        }

        String stringID = player.getTeam() != null ? player.getTeam().getName() : "";
        boolean canHire = RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(stringID, player.getUUID());
        Main.SIMPLE_CHANNEL.send(RecruitsPacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateHireState(canHire));
    }

    private RecruitsHireTrade getAvailableTrade(VillagerNobleEntity villagerNoble) {
        for (RecruitsHireTrade trade : villagerNoble.getTrades()) {
            if (trade != null && trade.uses > 0 && trade.resourceLocation.equals(this.resource)) {
                return trade;
            }
        }
        return null;
    }

    private static boolean isEligibleHireVillager(Villager villager) {
        return villager.isAlive()
                && !villager.isBaby()
                && villager.getVillagerData().getProfession().equals(VillagerProfession.NONE);
    }

    private Optional<EntityType<? extends AbstractRecruitEntity>> getRecruitType() {
        return EntityType.byString(resource.toString())
                .map(type -> (EntityType<? extends AbstractRecruitEntity>) type);
    }

    private boolean createRecruit(ServerLevel serverLevel, Villager villager, Player player, RecruitsGroup group, int price) {
        return getRecruitType()
                .map(type -> VillagerEvents.createHiredRecruitFromVillager(serverLevel, villager, type, player, group, price))
                .orElse(false);
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
