package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Map;


public class MessageToClientUpdateDiplomacyList implements RecruitsMessage<MessageToClientUpdateDiplomacyList> {
    private CompoundTag diplomacyNbt;
    public MessageToClientUpdateDiplomacyList() {
    }

    public MessageToClientUpdateDiplomacyList(Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> diplomacyStatusMap) {
        this.diplomacyNbt = RecruitsDiplomacyManager.mapToNbt(diplomacyStatusMap);
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(RecruitsNetworkContext context) {
        ClientManager.diplomacyMap = RecruitsDiplomacyManager.mapFromNbt(diplomacyNbt);
    }

    @Override
    public MessageToClientUpdateDiplomacyList fromBytes(FriendlyByteBuf buf) {
        this.diplomacyNbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.diplomacyNbt);
    }

}