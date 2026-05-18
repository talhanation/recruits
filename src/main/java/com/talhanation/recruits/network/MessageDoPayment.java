package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.UUID;


public class MessageDoPayment implements RecruitsMessage<MessageDoPayment> {

    private int amount;
    private UUID uuid;
    public MessageDoPayment(){

    }

    public MessageDoPayment(UUID uuid, int amount) {
        this.amount = amount;
        this.uuid = uuid;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(RecruitsNetworkContext context){
        ServerPlayer serverPlayer = context.getSender();
        if(serverPlayer == null) return;

        if(!serverPlayer.getUUID().equals(uuid)) return;

        if (this.amount <= 0) return;

        if(serverPlayer.isCreative() && serverPlayer.hasPermissions(2)){
            return;
        }

        if (!FactionEvents.playerHasEnoughEmeralds(serverPlayer, this.amount)) return;

        FactionEvents.doPayment(serverPlayer, this.amount);
    }
    public MessageDoPayment fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.amount = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeInt(amount);
    }
}
