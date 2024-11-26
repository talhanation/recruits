package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.client.gui.group.RecruitsGroupList;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateGroupList implements Message<MessageToClientUpdateGroupList> {
    private CompoundTag nbt;
    public MessageToClientUpdateGroupList() {

    }

    public MessageToClientUpdateGroupList(CompoundTag nbt) {
        this.nbt = nbt;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        RecruitsGroupList.groups = CommandEvents.getRecruitsGroupListFormNBT(this.nbt);
    }

    @Override
    public MessageToClientUpdateGroupList fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);

    }

}