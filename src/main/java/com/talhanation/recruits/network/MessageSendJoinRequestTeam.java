package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageSendJoinRequestTeam implements Message<MessageSendJoinRequestTeam> {
    private UUID player_uuid;
    private String teamName;

    public MessageSendJoinRequestTeam(){
    }

    public MessageSendJoinRequestTeam(UUID player_uuid, String teamName){
        this.player_uuid = player_uuid;
        this.teamName = teamName;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player_uuid != player.getUUID()) return;

        ServerLevel level = player.getLevel();
        TeamEvents.sendJoinRequest(level, player, teamName);
    }

    public MessageSendJoinRequestTeam fromBytes(FriendlyByteBuf buf) {
        player_uuid = buf.readUUID();
        teamName = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {

    }
}
