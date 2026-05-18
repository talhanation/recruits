package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

public class MessageToClientUpdateOwnFaction implements RecruitsMessage<MessageToClientUpdateOwnFaction> {
    private CompoundTag nbt;
    public MessageToClientUpdateOwnFaction() {

    }

    public MessageToClientUpdateOwnFaction(RecruitsFaction ownFaction) {
        if(ownFaction == null) this.nbt = new CompoundTag();
        else this.nbt = ownFaction.toNBT();
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(RecruitsNetworkContext context) {
        if(nbt.isEmpty()){
            ClientManager.ownFaction = null;
            return;
        }
        ClientManager.ownFaction = RecruitsFaction.fromNBT(nbt);
    }

    @Override
    public MessageToClientUpdateOwnFaction fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
    }

}