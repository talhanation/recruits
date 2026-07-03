package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsRoute;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
/**
 * Server → Client: delivers a transferred route to the receiving player.
 * The client saves it to their local routes directory.
 */
public class MessageToClientReceiveRoute implements Message<MessageToClientReceiveRoute> {

    public static final CustomPacketPayload.Type<MessageToClientReceiveRoute> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientreceiveroute"));
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
    public void executeClientSide(IPayloadContext context) {
        RecruitsRoute route = RecruitsRoute.fromNBT(routeNBT);
        if (route == null) return;
        ClientManager.saveRoute(route);
    }

    @Override
    public MessageToClientReceiveRoute fromBytes(RegistryFriendlyByteBuf buf) {
        this.routeNBT = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(this.routeNBT);
    }

    @Override
    public CustomPacketPayload.Type<MessageToClientReceiveRoute> type() {
        return TYPE;
    }
}
