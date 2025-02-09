package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsTeam;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class MessageToServerRequestUpdateTeamInspaction implements Message<MessageToServerRequestUpdateTeamInspaction> {


    public MessageToServerRequestUpdateTeamInspaction() {
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        List<ServerPlayer> playerList = (List<ServerPlayer>) player.getCommandSenderWorld().players();
        List<RecruitsPlayerInfo> playerInfoList = new ArrayList<>();
        RecruitsTeam recruitsTeam = TeamEvents.recruitsTeamManager.getTeamByStringID(player.getTeam().getName());

        if(recruitsTeam != null){
            for(ServerPlayer serverPlayer : playerList){
                if(serverPlayer.getTeam() != null && serverPlayer.getTeam().equals(player.getTeam())){
                    playerInfoList.add(new RecruitsPlayerInfo(serverPlayer.getUUID(), serverPlayer.getScoreboardName(), recruitsTeam));
                }
            }
        }


        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateTeamInspection(playerInfoList, recruitsTeam, RecruitsServerConfig.ShouldFactionEditingBeAllowed.get(), RecruitsServerConfig.ShouldFactionManagingBeAllowed.get()));
    }

    @Override
    public MessageToServerRequestUpdateTeamInspaction fromBytes(FriendlyByteBuf buf) {
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {

    }

}