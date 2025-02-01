package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageSendJoinRequestTeam implements Message<MessageSendJoinRequestTeam> {
    private UUID player_uuid;
    private String stringID;

    public MessageSendJoinRequestTeam(){
    }

    public MessageSendJoinRequestTeam(UUID player_uuid, String stringID){
        this.player_uuid = player_uuid;
        this.stringID = stringID;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        ServerLevel level = (ServerLevel) player.getCommandSenderWorld();
        if(player.getTeam() == null) TeamEvents.sendJoinRequest(level, player, stringID);
        player.sendSystemMessage(JOIN_REQUEST(stringID));
    }

    public MessageSendJoinRequestTeam fromBytes(FriendlyByteBuf buf) {
        this.player_uuid = buf.readUUID();
        this.stringID = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(player_uuid);
        buf.writeUtf(stringID);
    }

    private MutableComponent JOIN_REQUEST(String teamName){
      return Component.translatable("gui.recruits.team_creation.sendJoinRequest", teamName);
    }
}
