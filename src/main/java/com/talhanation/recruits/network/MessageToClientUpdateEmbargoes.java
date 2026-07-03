package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.Map;
import java.util.UUID;

public class MessageToClientUpdateEmbargoes implements Message<MessageToClientUpdateEmbargoes> {

    public static final CustomPacketPayload.Type<MessageToClientUpdateEmbargoes> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdateembargoes"));
    private CompoundTag embargoNbt;

    public MessageToClientUpdateEmbargoes() {
    }

    public MessageToClientUpdateEmbargoes(Map<UUID, String> embargoMap) {
        this.embargoNbt = RecruitsDiplomacyManager.embargoMapToNbt(embargoMap);
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(IPayloadContext context) {
        ClientManager.embargoMap = RecruitsDiplomacyManager.embargoMapFromNbt(embargoNbt);
    }

    @Override
    public MessageToClientUpdateEmbargoes fromBytes(RegistryFriendlyByteBuf buf) {
        this.embargoNbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(embargoNbt);
    }

    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateEmbargoes> type() {
        return TYPE;
    }
}
