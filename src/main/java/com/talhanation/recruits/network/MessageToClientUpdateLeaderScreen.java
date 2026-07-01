package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.gui.PatrolLeaderScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import java.util.List;


public class MessageToClientUpdateLeaderScreen implements Message<MessageToClientUpdateLeaderScreen> {
    public static final CustomPacketPayload.Type<MessageToClientUpdateLeaderScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdateleaderscreen"));
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
    public void executeClientSide(IPayloadContext context) {

    }

    @Override
    public MessageToClientUpdateLeaderScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.waypoints = buf.readList(RegistryFriendlyByteBuf::readBlockPos);
        this.waypointItems = buf.readList(b -> net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        this.size = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeCollection(waypoints, RegistryFriendlyByteBuf::writeBlockPos);
        buf.writeCollection(waypointItems, (b, stack) -> net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack));
        buf.writeInt(this.size);
    }

    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateLeaderScreen> type() {
        return TYPE;
    }
}

