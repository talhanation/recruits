package com.talhanation.recruits.network;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/**
 * Client → Server: the player is done writing orders at his Messenger. Either the Messenger sets off with them
 * to the companion, or they are thrown away ({@link CompanionOrders#endDraft}).
 */
public class MessageSendRemoteOrders implements Message<MessageSendRemoteOrders> {
    private UUID messenger;
    private boolean send;

    public MessageSendRemoteOrders() {
    }

    public MessageSendRemoteOrders(UUID messenger, boolean send) {
        this.messenger = messenger;
        this.send = send;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        CompanionOrders.endDraft(player, this.messenger, this.send);
    }

    public MessageSendRemoteOrders fromBytes(FriendlyByteBuf buf) {
        this.messenger = buf.readUUID();
        this.send = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.messenger);
        buf.writeBoolean(this.send);
    }
}
