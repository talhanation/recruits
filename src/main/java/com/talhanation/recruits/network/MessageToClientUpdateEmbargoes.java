package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Map;
import java.util.UUID;

public class MessageToClientUpdateEmbargoes implements RecruitsMessage<MessageToClientUpdateEmbargoes> {

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
    public void executeClientSide(RecruitsNetworkContext context) {
        ClientManager.embargoMap = RecruitsDiplomacyManager.embargoMapFromNbt(embargoNbt);
    }

    @Override
    public MessageToClientUpdateEmbargoes fromBytes(FriendlyByteBuf buf) {
        this.embargoNbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(embargoNbt);
    }
}
