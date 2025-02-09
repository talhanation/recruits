package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.RecruitHireScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateHireScreen implements Message<MessageToClientUpdateHireScreen> {
    public ItemStack currency;
    public boolean canHire;
    public MessageToClientUpdateHireScreen() {
    }

    public MessageToClientUpdateHireScreen(ItemStack currency, boolean canHire) {
        this.currency = currency;
        this.canHire = canHire;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        RecruitHireScreen.currency = this.currency;
        RecruitHireScreen.canHire = this.canHire;
    }

    @Override
    public MessageToClientUpdateHireScreen fromBytes(FriendlyByteBuf buf) {
        this.currency = buf.readItem();
        this.canHire = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItemStack(currency, false);
        buf.writeBoolean(canHire);
    }

}