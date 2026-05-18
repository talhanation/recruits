package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsRoute;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

/**
 * Server → Client: delivers a transferred route to the receiving player.
 * The client saves it to their local routes directory.
 */
public class MessageToClientReceiveRoute implements RecruitsMessage<MessageToClientReceiveRoute> {

    private CompoundTag routeNBT;

    public MessageToClientReceiveRoute() {}

    public MessageToClientReceiveRoute(CompoundTag routeNBT) {
        this.routeNBT = routeNBT;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(RecruitsNetworkContext context) {
        RecruitsRoute route = RecruitsRoute.fromNBT(routeNBT);
        if (route == null) return;
        ClientManager.saveRoute(route);
    }

    @Override
    public MessageToClientReceiveRoute fromBytes(FriendlyByteBuf buf) {
        this.routeNBT = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.routeNBT);
    }
}
