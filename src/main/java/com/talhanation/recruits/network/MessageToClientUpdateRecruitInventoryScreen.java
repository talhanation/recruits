package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.client.gui.RecruitInventoryScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateRecruitInventoryScreen implements Message<MessageToClientUpdateRecruitInventoryScreen> {
    private CompoundTag nbt;
    public MessageToClientUpdateRecruitInventoryScreen() {

    }

    public MessageToClientUpdateRecruitInventoryScreen(CompoundTag nbt) {
        this.nbt = nbt;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        RecruitInventoryScreen.groups = CommandEvents.getRecruitsGroupListFormNBT(this.nbt);
    }

    @Override
    public MessageToClientUpdateRecruitInventoryScreen fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);

    }

}