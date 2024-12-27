package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.team.TeamEditScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateTeamCreationScreen implements Message<MessageToClientUpdateTeamCreationScreen> {
    public ItemStack currency;
    public int price;
    public int maxRecruitsPerPlayerConfigSetting;
    public int maxRecruitsPerPlayer;
    public MessageToClientUpdateTeamCreationScreen() {
    }

    public MessageToClientUpdateTeamCreationScreen(ItemStack currency, int price, int maxRecruitsPerPlayerConfigSetting, int maxRecruitsPerPlayer) {
        this.currency = currency;
        this.price = price;
        this.maxRecruitsPerPlayerConfigSetting = maxRecruitsPerPlayerConfigSetting;
        this.maxRecruitsPerPlayer = maxRecruitsPerPlayer;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        TeamEditScreen.currency = this.currency;
        TeamEditScreen.creationPrice = this.price;
        TeamEditScreen.maxRecruitsPerPlayerConfigSetting = this.maxRecruitsPerPlayerConfigSetting;
        TeamEditScreen.maxRecruitsPerPlayer = this.maxRecruitsPerPlayer;
    }

    @Override
    public MessageToClientUpdateTeamCreationScreen fromBytes(FriendlyByteBuf buf) {
        this.currency = buf.readItem();
        this.price = buf.readInt();
        this.maxRecruitsPerPlayerConfigSetting = buf.readInt();
        this.maxRecruitsPerPlayer = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItemStack(currency, false);
        buf.writeInt(this.price);
        buf.writeInt(this.maxRecruitsPerPlayerConfigSetting);
        buf.writeInt(this.maxRecruitsPerPlayer);
    }

}