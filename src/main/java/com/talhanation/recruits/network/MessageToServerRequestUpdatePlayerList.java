package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
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

public class MessageToServerRequestUpdatePlayerList implements Message<MessageToServerRequestUpdatePlayerList> {


    public MessageToServerRequestUpdatePlayerList() {
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

        for(ServerPlayer serverPlayer : playerList){
            if(serverPlayer.getTeam() != null){
                RecruitsTeam team = TeamEvents.recruitsTeamManager.getTeamByStringID(serverPlayer.getTeam().getName());
                playerInfoList.add(new RecruitsPlayerInfo(serverPlayer.getUUID(), serverPlayer.getScoreboardName(), team));
            }
            else
                playerInfoList.add(new RecruitsPlayerInfo(serverPlayer.getUUID(), serverPlayer.getScoreboardName()));
        }

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdatePlayerList(playerInfoList));
    }

    @Override
    public MessageToServerRequestUpdatePlayerList fromBytes(FriendlyByteBuf buf) {
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {

    }

}