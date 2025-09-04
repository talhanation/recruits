package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsFaction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;


public class MessageToClientUpdateFactions implements Message<MessageToClientUpdateFactions> {
    private CompoundTag nbt;
    private boolean editing;
    private boolean managing;
    private ItemStack currency;
    private int factionCreationPrice;
    private int factionMaxRecruitsPerPlayerConfigSetting;
    public MessageToClientUpdateFactions() {

    }

    public MessageToClientUpdateFactions(List<RecruitsFaction> teamList, String ownFaction, boolean editing, boolean managing, int factionCreationPrice, int factionMaxRecruitsPerPlayerConfigSetting, ItemStack currency) {
        this.nbt = RecruitsFaction.toNBT(teamList);
        this.editing = editing;
        this.managing = managing;
        this.currency = currency;
        this.factionCreationPrice = factionCreationPrice;
        this.factionMaxRecruitsPerPlayerConfigSetting = factionMaxRecruitsPerPlayerConfigSetting;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        ClientManager.factions = RecruitsFaction.getListFromNBT(nbt);
        ClientManager.isFactionEditingAllowed = editing;
        ClientManager.isFactionManagingAllowed = managing;
        ClientManager.currency = currency;
        ClientManager.factionCreationPrice = factionCreationPrice;
        ClientManager.factionMaxRecruitsPerPlayerConfigSetting = factionMaxRecruitsPerPlayerConfigSetting;
    }

    @Override
    public MessageToClientUpdateFactions fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        this.editing = buf.readBoolean();
        this.managing = buf.readBoolean();
        this.currency = buf.readItem();
        this.factionCreationPrice = buf.readInt();
        this.factionMaxRecruitsPerPlayerConfigSetting = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
        buf.writeBoolean(this.editing);
        buf.writeBoolean(this.managing);
        buf.writeItem(this.currency);
        buf.writeInt(this.factionCreationPrice);
        buf.writeInt(this.factionMaxRecruitsPerPlayerConfigSetting);
    }

}