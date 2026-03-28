package com.talhanation.recruits.network;

import com.talhanation.recruits.world.RecruitsRoute;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Objects;
import java.util.UUID;

import static com.talhanation.recruits.Main.SIMPLE_CHANNEL;

/**
 * Client → Server: player wants to transfer a route to another online player.
 * Server → forwards as {@link MessageToClientReceiveRoute} to the target player.
 */
public class MessageTransferRoute implements Message<MessageTransferRoute> {

    private UUID targetPlayerUUID;
    private CompoundTag routeNBT;

    public MessageTransferRoute() {}

    public MessageTransferRoute(UUID targetPlayerUUID, RecruitsRoute route) {
        this.targetPlayerUUID = targetPlayerUUID;
        this.routeNBT = route.toNBT();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer sender = Objects.requireNonNull(context.getSender());
        ServerPlayer target = sender.getServer().getPlayerList().getPlayer(targetPlayerUUID);
        if (target == null) return;
        SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> target),
                new MessageToClientReceiveRoute(routeNBT));
    }

    @Override
    public MessageTransferRoute fromBytes(FriendlyByteBuf buf) {
        this.targetPlayerUUID = buf.readUUID();
        this.routeNBT = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.targetPlayerUUID);
        buf.writeNbt(this.routeNBT);
    }
}
