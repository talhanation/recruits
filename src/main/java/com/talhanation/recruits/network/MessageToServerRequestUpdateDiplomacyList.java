package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;

public class MessageToServerRequestUpdateDiplomacyList implements Message<MessageToServerRequestUpdateDiplomacyList> {


    public MessageToServerRequestUpdateDiplomacyList() {
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player.getTeam() != null){
            List<RecruitsTeam> teamList = TeamEvents.recruitsTeamManager.getTeams().stream().toList();
            Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> map = TeamEvents.recruitsDiplomacyManager.diplomacyMap;

            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateDiplomacyList(teamList, map));
        }


    }

    @Override
    public MessageToServerRequestUpdateDiplomacyList fromBytes(FriendlyByteBuf buf) {
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {

    }

}