package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.UUID;

public class MessageSendJoinRequestTeam implements Message<MessageSendJoinRequestTeam> {
    public static final CustomPacketPayload.Type<MessageSendJoinRequestTeam> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagesendjoinrequestteam"));
    private UUID player_uuid;
    private String stringID;

    public MessageSendJoinRequestTeam(){
    }

    public MessageSendJoinRequestTeam(UUID player_uuid, String stringID){
        this.player_uuid = player_uuid;
        this.stringID = stringID;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        ServerLevel level = (ServerLevel) player.getCommandSenderWorld();
        if(player.getTeam() == null) FactionEvents.sendJoinRequest(level, player, stringID);
        player.sendSystemMessage(JOIN_REQUEST(stringID));
    }

    public MessageSendJoinRequestTeam fromBytes(RegistryFriendlyByteBuf buf) {
        this.player_uuid = buf.readUUID();
        this.stringID = buf.readUtf();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(player_uuid);
        buf.writeUtf(stringID);
    }

    private MutableComponent JOIN_REQUEST(String teamName){
      return Component.translatable("gui.recruits.team_creation.sendJoinRequest", teamName);
    }

    @Override
    public CustomPacketPayload.Type<MessageSendJoinRequestTeam> type() {
        return TYPE;
    }
}
