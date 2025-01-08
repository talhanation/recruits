package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.team.TeamEditScreen;
import com.talhanation.recruits.world.RecruitsTeam;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateTeamEditScreen implements Message<MessageToClientUpdateTeamEditScreen> {
    public ItemStack currency;
    public int price;
    public int maxRecruitsPerPlayerConfigSetting;
    private CompoundTag recruitsTeam;
    public MessageToClientUpdateTeamEditScreen() {
    }

    public MessageToClientUpdateTeamEditScreen(ItemStack currency, int price, int maxRecruitsPerPlayerConfigSetting, RecruitsTeam recruitsTeam) {
        this.currency = currency;
        this.price = price;
        this.maxRecruitsPerPlayerConfigSetting = maxRecruitsPerPlayerConfigSetting;

        if(recruitsTeam != null){
            this.recruitsTeam = recruitsTeam.toNBT();
        }
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

        if(this.recruitsTeam != null){
            TeamEditScreen.recruitsTeam = RecruitsTeam.fromNBT(this.recruitsTeam);
        }

        TeamEditScreen.postInit = true;
    }

    @Override
    public MessageToClientUpdateTeamEditScreen fromBytes(FriendlyByteBuf buf) {
        this.currency = buf.readItem();
        this.price = buf.readInt();
        this.maxRecruitsPerPlayerConfigSetting = buf.readInt();
        this.recruitsTeam = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItemStack(currency, false);
        buf.writeInt(this.price);
        buf.writeInt(this.maxRecruitsPerPlayerConfigSetting);
        buf.writeNbt(this.recruitsTeam);
    }

}