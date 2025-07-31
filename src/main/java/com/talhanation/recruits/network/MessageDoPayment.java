package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;


public class MessageDoPayment implements Message<MessageDoPayment> {

    private int amount;
    private UUID uuid;
    public MessageDoPayment(){

    }

    public MessageDoPayment(UUID uuid, int amount) {
        this.amount = amount;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer serverPlayer = context.getSender();
        if(serverPlayer == null) return;

        if(!serverPlayer.getUUID().equals(uuid)) return;

        TeamEvents.doPayment(serverPlayer, this.amount);
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
