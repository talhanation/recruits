package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;

public class MessageAddEmbargoFaction implements RecruitsMessage<MessageAddEmbargoFaction> {

    private String faction;

    public MessageAddEmbargoFaction() {
    }

    public MessageAddEmbargoFaction(String faction) {
        this.faction = faction;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        ServerLevel level = (ServerLevel) player.level();

        // Guard: only leaders may add embargoes
        RecruitsFaction ownFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(
                player.getTeam() != null ? player.getTeam().getName() : ""
        );

        RecruitsFaction embargoFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(faction);

        if (ownFaction == null || embargoFaction == null || !ownFaction.getTeamLeaderUUID().equals(player.getUUID())) return;

        for(RecruitsPlayerInfo info : embargoFaction.getMembers()){
            FactionEvents.recruitsDiplomacyManager.addEmbargo(info.getUUID(), ownFaction.getStringID(), level);
        }


    }

    @Override
    public MessageAddEmbargoFaction fromBytes(FriendlyByteBuf buf) {
        this.faction = buf.readUtf();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(faction);
    }
}
