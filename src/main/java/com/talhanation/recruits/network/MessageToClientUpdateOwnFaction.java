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


public class MessageToClientUpdateOwnFaction implements Message<MessageToClientUpdateOwnFaction> {
    private CompoundTag nbt;
    public MessageToClientUpdateOwnFaction() {

    }

    public MessageToClientUpdateOwnFaction(RecruitsFaction ownFaction) {
        if(ownFaction == null) this.nbt = new CompoundTag();
        else this.nbt = ownFaction.toNBT();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        if(nbt.isEmpty()){
            ClientManager.ownFaction = null;
            return;
        }
        ClientManager.ownFaction = RecruitsFaction.fromNBT(nbt);
    }

    @Override
    public MessageToClientUpdateOwnFaction fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
    }

}