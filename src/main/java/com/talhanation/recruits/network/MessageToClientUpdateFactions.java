package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.List;


public class MessageToClientUpdateFactions implements RecruitsMessage<MessageToClientUpdateFactions> {
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
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    public void executeClientSide(RecruitsNetworkContext context) {
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
        this.currency = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        this.factionCreationPrice = buf.readInt();
        this.factionMaxRecruitsPerPlayerConfigSetting = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
        buf.writeBoolean(this.editing);
        buf.writeBoolean(this.managing);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, this.currency == null ? ItemStack.EMPTY : this.currency);
        buf.writeInt(this.factionCreationPrice);
        buf.writeInt(this.factionMaxRecruitsPerPlayerConfigSetting);
    }

}
