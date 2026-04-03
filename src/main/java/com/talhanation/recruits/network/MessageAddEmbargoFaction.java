package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;

public class MessageAddEmbargoFaction implements Message<MessageAddEmbargoFaction> {

    private String faction;

    public MessageAddEmbargoFaction() {
    }

    public MessageAddEmbargoFaction(String faction) {
        this.faction = faction;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
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
