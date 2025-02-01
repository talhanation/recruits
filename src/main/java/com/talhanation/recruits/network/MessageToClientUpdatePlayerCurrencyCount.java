package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.team.TeamEditScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdatePlayerCurrencyCount implements Message<MessageToClientUpdatePlayerCurrencyCount> {

    private int x;

    public MessageToClientUpdatePlayerCurrencyCount() {
    }

    public MessageToClientUpdatePlayerCurrencyCount(int x) {
        this.x = x;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        TeamEditScreen.playerCurrencyCount = x;
    }

    @Override
    public MessageToClientUpdatePlayerCurrencyCount fromBytes(FriendlyByteBuf buf) {
       this.x = buf.readInt();
       return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(x);
    }
}