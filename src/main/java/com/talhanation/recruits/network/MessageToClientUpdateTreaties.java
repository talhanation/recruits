package com.talhanation.recruits.network;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsTreatyManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;

public class MessageToClientUpdateTreaties implements Message<MessageToClientUpdateTreaties> {

    private CompoundTag nbt;

    public MessageToClientUpdateTreaties() {
    }

    public MessageToClientUpdateTreaties(Map<String, Long> treaties) {
        this.nbt = RecruitsTreatyManager.mapToNbt(treaties);
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        ClientManager.treaties = RecruitsTreatyManager.mapFromNbt(nbt);
    }

    @Override
    public MessageToClientUpdateTreaties fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);
    }
}
