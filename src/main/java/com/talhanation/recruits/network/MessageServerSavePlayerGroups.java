package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;

public class MessageServerSavePlayerGroups implements Message<MessageServerSavePlayerGroups> {
    private CompoundTag nbt;
    private boolean update;

    public MessageServerSavePlayerGroups() {

    }
    public MessageServerSavePlayerGroups(List<RecruitsGroup> groups, boolean update) {
        this.nbt = CommandEvents.getCompoundTagFromRecruitsGroupList(groups);
        this.update = update;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        CommandEvents.savePlayersGroupsToNBT(context.getSender(), CommandEvents.getRecruitsGroupListFormNBT(nbt), update);
    }

    @Override
    public MessageServerSavePlayerGroups fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        this.update = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);
        buf.writeBoolean(update);
    }
}
