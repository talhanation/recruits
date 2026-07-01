package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
public class MessageToClientUpdateUnitInfo implements Message<MessageToClientUpdateUnitInfo> {
    public static final CustomPacketPayload.Type<MessageToClientUpdateUnitInfo> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdateunitinfo"));
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
    public void executeClientSide(IPayloadContext context) {
        ClientManager.configValueNobleNeedsVillagers = configValueNobleNeedsVillagers;
        ClientManager.availableRecruitsToHire = availableRecruitsToHire;
    }

    @Override
    public MessageToClientUpdateUnitInfo fromBytes(RegistryFriendlyByteBuf buf) {
        this.configValueNobleNeedsVillagers = buf.readBoolean();
        this.availableRecruitsToHire = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.configValueNobleNeedsVillagers);
        buf.writeInt(this.availableRecruitsToHire);
    }


    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateUnitInfo> type() {
        return TYPE;
    }
}