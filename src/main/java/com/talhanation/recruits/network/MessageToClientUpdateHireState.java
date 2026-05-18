package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;


public class MessageToClientUpdateHireState implements RecruitsMessage<MessageToClientUpdateHireState> {
    public ItemStack currency;
    public boolean canHire;
    public MessageToClientUpdateHireState() {
    }

    public MessageToClientUpdateHireState(boolean canHire) {
        this.canHire = canHire;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(RecruitsNetworkContext context) {
        ClientManager.canPlayerHire = this.canHire;
    }

    @Override
    public MessageToClientUpdateHireState fromBytes(FriendlyByteBuf buf) {
        this.canHire = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(canHire);
    }

}