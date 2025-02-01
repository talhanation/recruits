package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.MessengerAnswerScreen;
import com.talhanation.recruits.client.gui.MessengerScreen;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateMessengerAnswerScreen implements Message<MessageToClientUpdateMessengerAnswerScreen> {
    public String message;
    public CompoundTag nbt;

    public MessageToClientUpdateMessengerAnswerScreen() {
    }

    public MessageToClientUpdateMessengerAnswerScreen(String message, RecruitsPlayerInfo playerInfo) {
        this.message = message;
        this.nbt = playerInfo.toNBT();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        MessengerAnswerScreen.message = this.message;
        MessengerAnswerScreen.playerInfo = RecruitsPlayerInfo.getFromNBT(this.nbt);
    }

    @Override
    public MessageToClientUpdateMessengerAnswerScreen fromBytes(FriendlyByteBuf buf) {
        this.message = buf.readUtf();
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(message);

    }

}