package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.List;


public class MessageToClientUpdateOnlinePlayers implements RecruitsMessage<MessageToClientUpdateOnlinePlayers> {
    private CompoundTag nbt;

    public MessageToClientUpdateOnlinePlayers() {
    }

    public MessageToClientUpdateOnlinePlayers(List<RecruitsPlayerInfo> playerInfoList) {
        this.nbt = RecruitsPlayerInfo.toNBT(playerInfoList);
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(RecruitsNetworkContext context) {
        ClientManager.onlinePlayers = RecruitsPlayerInfo.getListFromNBT(nbt);
    }

    @Override
    public MessageToClientUpdateOnlinePlayers fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
    }

}