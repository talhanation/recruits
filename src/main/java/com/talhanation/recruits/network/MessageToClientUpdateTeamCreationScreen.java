package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.RecruitHireScreen;
import com.talhanation.recruits.client.gui.team.TeamCreationScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateTeamCreationScreen implements Message<MessageToClientUpdateTeamCreationScreen> {
    public ItemStack currency;
    public int price;
    public MessageToClientUpdateTeamCreationScreen() {
    }

    public MessageToClientUpdateTeamCreationScreen(ItemStack currency, int price) {
        this.currency = currency;
        this.price = price;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        TeamCreationScreen.currency = this.currency;
        TeamCreationScreen.price = this.price;
    }

    @Override
    public MessageToClientUpdateTeamCreationScreen fromBytes(FriendlyByteBuf buf) {
        this.currency = buf.readItem();
        this.price = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItemStack(currency, false);
        buf.writeInt(this.price);
    }

}