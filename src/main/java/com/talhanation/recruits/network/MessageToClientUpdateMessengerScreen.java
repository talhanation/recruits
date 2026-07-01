package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.gui.MessengerScreen;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
public class MessageToClientUpdateMessengerScreen implements Message<MessageToClientUpdateMessengerScreen> {
    public static final CustomPacketPayload.Type<MessageToClientUpdateMessengerScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdatemessengerscreen"));
    public String message;
    public CompoundTag nbt;
    public MessageToClientUpdateMessengerScreen() {
    }

    public MessageToClientUpdateMessengerScreen(String message, RecruitsPlayerInfo playerInfo) {
        this.message = message;

        if(playerInfo != null){
            this.nbt = playerInfo.toNBT();
        }
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(IPayloadContext context) {
        //MessengerScreen.message = this.message;

        if(nbt != null){
            MessengerScreen.playerInfo = RecruitsPlayerInfo.getFromNBT(nbt);
        }
    }

    @Override
    public MessageToClientUpdateMessengerScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.message = buf.readUtf();

        if(nbt != null){
            this.nbt = buf.readNbt();
        }

        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(message);

        if(nbt != null){
            buf.writeNbt(nbt);
        }
    }


    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateMessengerScreen> type() {
        return TYPE;
    }
}