package com.talhanation.recruits.network;

import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.List;


public class MessageToClientUpdateLeaderScreen implements RecruitsMessage<MessageToClientUpdateLeaderScreen> {
    public List<BlockPos> waypoints;
    public List<ItemStack> waypointItems;
    public int size;

    public MessageToClientUpdateLeaderScreen() {
    }

    public MessageToClientUpdateLeaderScreen(List<BlockPos> waypoints, List<ItemStack> waypointItems, int size) {
        this.waypoints = waypoints;
        this.waypointItems = waypointItems;
        this.size = size;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(RecruitsNetworkContext context) {

    }

    @Override
    public MessageToClientUpdateLeaderScreen fromBytes(FriendlyByteBuf buf) {
        this.waypoints = buf.readList(byteBuf -> byteBuf.readBlockPos());
        this.waypointItems = buf.readList(byteBuf -> ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) byteBuf));
        this.size = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(waypoints, (byteBuf, pos) -> byteBuf.writeBlockPos(pos));
        buf.writeCollection(waypointItems, (byteBuf, stack) -> ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) byteBuf, stack == null ? ItemStack.EMPTY : stack));
        buf.writeInt(this.size);
    }
}
