package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.List;


public class MessageToClientUpdateOnlinePlayers implements Message<MessageToClientUpdateOnlinePlayers> {
    public static final CustomPacketPayload.Type<MessageToClientUpdateOnlinePlayers> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdateonlineplayers"));
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
    public void executeClientSide(IPayloadContext context) {
        ClientManager.onlinePlayers = RecruitsPlayerInfo.getListFromNBT(nbt);
    }

    @Override
    public MessageToClientUpdateOnlinePlayers fromBytes(RegistryFriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
    }


    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateOnlinePlayers> type() {
        return TYPE;
    }
}