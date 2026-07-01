package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.events.RecruitsToastManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import static com.talhanation.recruits.client.events.RecruitsToastManager.*;


public class MessageToClientSetToast implements Message<MessageToClientSetToast> {

    public static final CustomPacketPayload.Type<MessageToClientSetToast> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientsettoast"));
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
    public void executeClientSide(IPayloadContext context) {
        switch (x){
            case 0 -> RecruitsToastManager.setToastForPlayer(Images.LETTER, TOAST_RECRUIT_ASSIGNED_TITLE, TOAST_RECRUIT_ASSIGNED_INFO(s));
            case 1 -> RecruitsToastManager.setToastForPlayer(Images.LETTER, TOAST_MESSENGER_ARRIVED_TITLE, TOAST_MESSENGER_ARRIVED_INFO(s));
            case 2 -> RecruitsToastManager.setToastForPlayer(Images.LETTER, TOAST_GROUP_ASSIGNED_TITLE, TOAST_GROUP_ASSIGNED_INFO(s));
        }
    }

    @Override
    public MessageToClientSetToast fromBytes(RegistryFriendlyByteBuf buf) {
       this.x = buf.readInt();
       this.s = buf.readUtf();
       return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(x);
        buf.writeUtf(s);
    }

    @Override
    public CustomPacketPayload.Type<MessageToClientSetToast> type() {
        return TYPE;
    }
}