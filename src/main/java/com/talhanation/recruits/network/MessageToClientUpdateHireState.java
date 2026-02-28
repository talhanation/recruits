package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.RecruitHireScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateHireState implements Message<MessageToClientUpdateHireState> {
    public ItemStack currency;
    public boolean canHire;
    public MessageToClientUpdateHireState() {
    }

    public MessageToClientUpdateHireState(boolean canHire) {
        this.canHire = canHire;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        ClientManager.canPlayerHire = this.canHire;
    }

    @Override
    public MessageToClientUpdateHireState fromBytes(FriendlyByteBuf buf) {
        this.canHire = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(canHire);
    }

}