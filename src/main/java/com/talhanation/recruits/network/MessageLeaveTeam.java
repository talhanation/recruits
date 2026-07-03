package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
public class MessageLeaveTeam implements Message<MessageLeaveTeam> {

    public static final CustomPacketPayload.Type<MessageLeaveTeam> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageleaveteam"));
    public MessageLeaveTeam(){
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        ServerLevel level = player.serverLevel();
        FactionEvents.leaveTeam(false, player, null, level, false);
    }

    public MessageLeaveTeam fromBytes(RegistryFriendlyByteBuf buf) {
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {

    }

    @Override
    public CustomPacketPayload.Type<MessageLeaveTeam> type() {
        return TYPE;
    }
}
