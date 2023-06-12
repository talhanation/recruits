package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.CommandScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;


public class MessageToClientUpdateCommandScreen implements Message<MessageToClientUpdateCommandScreen> {
    public int recruitsInCommand;

    public MessageToClientUpdateCommandScreen() {
    }

    public MessageToClientUpdateCommandScreen(int recruitsInCommand) {
        this.recruitsInCommand = recruitsInCommand;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        CommandScreen.recruitsInCommand = this.recruitsInCommand;
    }

    @Override
    public MessageToClientUpdateCommandScreen fromBytes(FriendlyByteBuf buf) {
        this.recruitsInCommand = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(recruitsInCommand);
    }

}