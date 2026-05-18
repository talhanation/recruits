package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;


public class MessageToClientUpdateUnitInfo implements RecruitsMessage<MessageToClientUpdateUnitInfo> {
    private boolean configValueNobleNeedsVillagers;
    private int availableRecruitsToHire;
    public MessageToClientUpdateUnitInfo() {

    }

    public MessageToClientUpdateUnitInfo(boolean configValueNobleNeedsVillagers, int availableRecruitsToHire) {
        this.configValueNobleNeedsVillagers = configValueNobleNeedsVillagers;
        this.availableRecruitsToHire = availableRecruitsToHire;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(RecruitsNetworkContext context) {
        ClientManager.configValueNobleNeedsVillagers = configValueNobleNeedsVillagers;
        ClientManager.availableRecruitsToHire = availableRecruitsToHire;
    }

    @Override
    public MessageToClientUpdateUnitInfo fromBytes(FriendlyByteBuf buf) {
        this.configValueNobleNeedsVillagers = buf.readBoolean();
        this.availableRecruitsToHire = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.configValueNobleNeedsVillagers);
        buf.writeInt(this.availableRecruitsToHire);
    }

}