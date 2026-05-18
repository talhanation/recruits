package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

public class MessageLeaveTeam implements RecruitsMessage<MessageLeaveTeam> {

    public MessageLeaveTeam(){
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = context.getSender();
        ServerLevel level = player.serverLevel();
        FactionEvents.leaveTeam(false, player, null, level, false);
    }

    public MessageLeaveTeam fromBytes(FriendlyByteBuf buf) {
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {

    }
}
