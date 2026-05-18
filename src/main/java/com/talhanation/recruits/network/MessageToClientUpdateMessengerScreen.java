package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.MessengerScreen;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;


public class MessageToClientUpdateMessengerScreen implements RecruitsMessage<MessageToClientUpdateMessengerScreen> {
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
    public void executeClientSide(RecruitsNetworkContext context) {
        //MessengerScreen.message = this.message;

        if(nbt != null){
            MessengerScreen.playerInfo = RecruitsPlayerInfo.getFromNBT(nbt);
        }
    }

    @Override
    public MessageToClientUpdateMessengerScreen fromBytes(FriendlyByteBuf buf) {
        this.message = buf.readUtf();

        if(nbt != null){
            this.nbt = buf.readNbt();
        }

        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(message);

        if(nbt != null){
            buf.writeNbt(nbt);
        }
    }

}