package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsFaction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import java.util.List;


public class MessageToClientUpdateOwnFaction implements Message<MessageToClientUpdateOwnFaction> {
    public static final CustomPacketPayload.Type<MessageToClientUpdateOwnFaction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdateownfaction"));
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
    public void executeClientSide(IPayloadContext context) {
        if(nbt.isEmpty()){
            ClientManager.ownFaction = null;
            return;
        }
        ClientManager.ownFaction = RecruitsFaction.fromNBT(nbt);
    }

    @Override
    public MessageToClientUpdateOwnFaction fromBytes(RegistryFriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
    }


    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateOwnFaction> type() {
        return TYPE;
    }
}