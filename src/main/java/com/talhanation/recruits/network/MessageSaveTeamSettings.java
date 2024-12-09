package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.world.RecruitsTeam;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageSaveTeamSettings implements Message<MessageSaveTeamSettings> {
    private CompoundTag nbt;
    private String name;

    public MessageSaveTeamSettings() {
    }

    public MessageSaveTeamSettings(RecruitsTeam team, String name) {
        this.nbt = team.toNBT();
        this.name = name;
    }

    @Override
    public Dist getExecutingSide()  {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        RecruitsTeam editedTeam = RecruitsTeam.fromNBT(nbt);
        TeamEvents.modifyTeam(context.getSender().server.overworld(), name, editedTeam);
    }

    @Override
    public MessageSaveTeamSettings fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        this.name = buf.readUtf();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
        buf.writeUtf(this.name);
    }
}
