package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.VillagerEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.RecruitsHireTrade;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

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
        ServerLevel serverLevel = player.serverLevel();
        VillagerNobleEntity villagerNoble = player.getCommandSenderWorld().getEntitiesOfClass(
                VillagerNobleEntity.class,
                player.getBoundingBox().inflate(32.0D),
                noble -> noble.getUUID().equals(this.nobleUUID) && noble.isAlive()
        ).stream().findAny().orElse(null);
        if(villagerNoble == null) return; // was .get() -> exception for unknown UUIDs

        if(closing){
            villagerNoble.isTrading(false);
            return;
        }

        // Only what THIS noble really offers, while uses are left, at the noble's price.
        // Before: the client chose any entity type, uses could go negative and the trade price was ignored.
        RecruitsHireTrade trade = findTrade(villagerNoble, this.resource);
        if(trade == null || trade.uses <= 0) return;

        Optional<EntityType<?>> optionalType = EntityType.byString(this.resource.toString());
        if(optionalType.isEmpty()) return;

        RecruitsGroup group = RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);
        if(group == null || !NetworkSecurity.canManageGroup(player, group)) return;

        boolean hired = false;
        if(this.needsVillager){
            Villager villager = player.getCommandSenderWorld().getEntitiesOfClass(
                    Villager.class,
                    player.getBoundingBox().inflate(32.0D),
                    v -> v.getUUID().equals(this.villagerUUID) && v.isAlive()
            ).stream().findAny().orElse(null);
            if(villager != null) {
                hired = VillagerEvents.createHiredRecruitFromVillager(serverLevel, villager, optionalType.get(), player, group, trade.cost);
            }
        }
        else{
            hired = VillagerEvents.spawnHiredRecruit(serverLevel, optionalType.get(), player, group, trade.cost);
        }

        if(hired) villagerNoble.doTrade(this.resource);

        String stringID = player.getTeam() != null ? player.getTeam().getName() : "";
        boolean canHire = RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(stringID, player.getUUID());
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateHireState(canHire));
    }
    private static RecruitsHireTrade findTrade(VillagerNobleEntity noble, ResourceLocation resource) {
        if(resource == null || noble.getTrades() == null) return null;
        for(RecruitsHireTrade trade : noble.getTrades()){
            if(trade != null && resource.equals(trade.resourceLocation)) return trade;
        }
        return null;
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