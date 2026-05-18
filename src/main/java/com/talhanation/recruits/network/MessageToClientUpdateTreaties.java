package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsTreatyManager;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Map;

public class MessageToClientUpdateTreaties implements RecruitsMessage<MessageToClientUpdateTreaties> {

    private CompoundTag nbt;

    public MessageToClientUpdateTreaties() {
    }

    public MessageToClientUpdateTreaties(Map<String, Long> treaties) {
        this.nbt = RecruitsTreatyManager.mapToNbt(treaties);
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(RecruitsNetworkContext context) {
        ClientManager.treaties = RecruitsTreatyManager.mapFromNbt(nbt);
    }

    @Override
    public MessageToClientUpdateTreaties fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);
    }
}
