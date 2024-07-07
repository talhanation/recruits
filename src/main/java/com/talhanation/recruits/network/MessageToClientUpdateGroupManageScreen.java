package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.GroupManageScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateGroupManageScreen implements Message<MessageToClientUpdateGroupManageScreen> {
    private CompoundTag nbt;
    public MessageToClientUpdateGroupManageScreen() {

    }

    public MessageToClientUpdateGroupManageScreen(CompoundTag nbt) {
        this.nbt = nbt;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {

        GroupManageScreen.groups = CommandEvents.getRecruitsGroupListFormNBT(this.nbt);
    }

    @Override
    public MessageToClientUpdateGroupManageScreen fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);

    }

}