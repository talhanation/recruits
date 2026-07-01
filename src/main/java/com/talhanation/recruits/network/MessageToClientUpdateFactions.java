package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsFaction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import java.util.List;


public class MessageToClientUpdateFactions implements Message<MessageToClientUpdateFactions> {
    public static final CustomPacketPayload.Type<MessageToClientUpdateFactions> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientupdatefactions"));
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
    public void executeClientSide(IPayloadContext context) {
        ClientManager.factions = RecruitsFaction.getListFromNBT(nbt);
        ClientManager.isFactionEditingAllowed = editing;
        ClientManager.isFactionManagingAllowed = managing;
        ClientManager.currency = currency;
        ClientManager.factionCreationPrice = factionCreationPrice;
        ClientManager.factionMaxRecruitsPerPlayerConfigSetting = factionMaxRecruitsPerPlayerConfigSetting;
    }

    @Override
    public MessageToClientUpdateFactions fromBytes(RegistryFriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        this.editing = buf.readBoolean();
        this.managing = buf.readBoolean();
        this.currency = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        this.factionCreationPrice = buf.readInt();
        this.factionMaxRecruitsPerPlayerConfigSetting = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
        buf.writeBoolean(this.editing);
        buf.writeBoolean(this.managing);
        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.currency);
        buf.writeInt(this.factionCreationPrice);
        buf.writeInt(this.factionMaxRecruitsPerPlayerConfigSetting);
    }


    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateFactions> type() {
        return TYPE;
    }
}