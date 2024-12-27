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

public class MessageToServerRequestUpdateTeamEdit implements Message<MessageToServerRequestUpdateTeamEdit> {


    public MessageToServerRequestUpdateTeamEdit() {
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player != null && player.getTeam() != null){
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> (ServerPlayer) player),
                    new MessageToClientUpdateTeamCreationScreen(TeamEvents.getCurrency(), RecruitsServerConfig.TeamCreationCost.get(), RecruitsServerConfig.MaxRecruitsForPlayer.get(),
                            TeamEvents.recruitsTeamManager.getTeamByName(player.getTeam().getName()).maxNPCsPerPlayer));
        }
    }

    @Override
    public MessageToServerRequestUpdateTeamEdit fromBytes(FriendlyByteBuf buf) {
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {

    }

}