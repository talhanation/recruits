package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.world.RecruitsRoute;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;
import java.util.UUID;

import de.maxhenkel.corelib.net.NetUtils;

/**
 * Client → Server: player wants to transfer a route to another online player.
 * Server → forwards as {@link MessageToClientReceiveRoute} to the target player.
 */
public class MessageTransferRoute implements Message<MessageTransferRoute> {

    public static final CustomPacketPayload.Type<MessageTransferRoute> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetransferroute"));
    private UUID targetPlayerUUID;
    private CompoundTag routeNBT;

    public MessageTransferRoute() {}

    public MessageTransferRoute(UUID targetPlayerUUID, RecruitsRoute route) {
        this.targetPlayerUUID = targetPlayerUUID;
        this.routeNBT = route.toNBT();
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer sender = Objects.requireNonNull(((ServerPlayer) context.player()));
        RecruitsRoute route = RecruitsRoute.fromNBT(routeNBT);
        if (route == null) return;

        ServerPlayer target = sender.getServer().getPlayerList().getPlayer(targetPlayerUUID);
        if (target == null) return;
        NetUtils.sendTo(target, new MessageToClientReceiveRoute(route.toNBT()));
    }

    @Override
    public MessageTransferRoute fromBytes(RegistryFriendlyByteBuf buf) {
        this.targetPlayerUUID = buf.readUUID();
        this.routeNBT = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.targetPlayerUUID);
        buf.writeNbt(this.routeNBT);
    }

    @Override
    public CustomPacketPayload.Type<MessageTransferRoute> type() {
        return TYPE;
    }
}
