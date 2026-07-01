package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.RecruitHireScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
public class MessageToClientUpdateHireState implements Message<MessageToClientUpdateHireState> {
    public static final CustomPacketPayload.Type<MessageToClientUpdateHireState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdatehirestate"));
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
    public void executeClientSide(IPayloadContext context) {
        ClientManager.canPlayerHire = this.canHire;
    }

    @Override
    public MessageToClientUpdateHireState fromBytes(RegistryFriendlyByteBuf buf) {
        this.canHire = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(canHire);
    }


    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateHireState> type() {
        return TYPE;
    }
}