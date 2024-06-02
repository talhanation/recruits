package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.MessengerAnswerScreen;
import com.talhanation.recruits.client.gui.MessengerScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateMessengerAnswerScreen implements Message<MessageToClientUpdateMessengerAnswerScreen> {
    public String message;

    public MessageToClientUpdateMessengerAnswerScreen() {
    }

    public MessageToClientUpdateMessengerAnswerScreen(String message) {
        this.message = message;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        MessengerAnswerScreen.message = this.message;
    }

    @Override
    public MessageToClientUpdateMessengerAnswerScreen fromBytes(FriendlyByteBuf buf) {
        this.message = buf.readUtf();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(message);
    }

}