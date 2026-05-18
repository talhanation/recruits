package com.talhanation.recruits.network;

import com.talhanation.recruits.client.events.RecruitsToastManager;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import static com.talhanation.recruits.client.events.RecruitsToastManager.*;


public class MessageToClientSetToast implements RecruitsMessage<MessageToClientSetToast> {

    private int x;
    private String s;

    public MessageToClientSetToast() {
    }

    public MessageToClientSetToast(int x, String s) {
        this.x = x;
        this.s = s;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(RecruitsNetworkContext context) {
        switch (x){
            case 0 -> RecruitsToastManager.setToastForPlayer(Images.LETTER, TOAST_RECRUIT_ASSIGNED_TITLE, TOAST_RECRUIT_ASSIGNED_INFO(s));
            case 1 -> RecruitsToastManager.setToastForPlayer(Images.LETTER, TOAST_MESSENGER_ARRIVED_TITLE, TOAST_MESSENGER_ARRIVED_INFO(s));
            case 2 -> RecruitsToastManager.setToastForPlayer(Images.LETTER, TOAST_GROUP_ASSIGNED_TITLE, TOAST_GROUP_ASSIGNED_INFO(s));
        }
    }

    @Override
    public MessageToClientSetToast fromBytes(FriendlyByteBuf buf) {
       this.x = buf.readInt();
       this.s = buf.readUtf();
       return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(x);
        buf.writeUtf(s);
    }
}