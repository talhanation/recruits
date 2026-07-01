package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsTreatyManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.Map;

public class MessageToClientUpdateTreaties implements Message<MessageToClientUpdateTreaties> {

    public static final CustomPacketPayload.Type<MessageToClientUpdateTreaties> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdatetreaties"));
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
    public void executeClientSide(IPayloadContext context) {
        ClientManager.treaties = RecruitsTreatyManager.mapFromNbt(nbt);
    }

    @Override
    public MessageToClientUpdateTreaties fromBytes(RegistryFriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(nbt);
    }

    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateTreaties> type() {
        return TYPE;
    }
}
