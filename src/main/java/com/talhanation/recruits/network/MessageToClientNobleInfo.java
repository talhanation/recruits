package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.NobleScreen;
import com.talhanation.recruits.world.NobleVillageInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

/**
 * Ruf, Dorf und Aufträge eines Nobles für den Client. Öffnet optional den Hauptscreen.
 */
public class MessageToClientNobleInfo implements Message<MessageToClientNobleInfo> {

    private NobleVillageInfo info;
    private boolean openScreen;

    public MessageToClientNobleInfo() {
    }

    public MessageToClientNobleInfo(NobleVillageInfo info, boolean openScreen) {
        this.info = info;
        this.openScreen = openScreen;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        ClientManager.nobleInfo = this.info;
        if (openScreen) {
            Minecraft.getInstance().setScreen(new NobleScreen());
        }
    }

    @Override
    public MessageToClientNobleInfo fromBytes(FriendlyByteBuf buf) {
        this.openScreen = buf.readBoolean();
        this.info = NobleVillageInfo.fromBytes(buf);
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(openScreen);
        info.toBytes(buf);
    }
}
