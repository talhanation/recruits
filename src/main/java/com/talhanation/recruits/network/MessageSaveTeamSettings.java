package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsFaction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageSaveTeamSettings implements Message<MessageSaveTeamSettings> {
    private CompoundTag nbt;
    private String stringID;
    private int cost;

    public MessageSaveTeamSettings() {
    }

    public MessageSaveTeamSettings(RecruitsFaction team, int cost) {
        this.nbt = team.toNBT();
        this.stringID = team.getStringID();
        this.cost = cost;
    }

    @Override
    public Dist getExecutingSide()  {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        RecruitsFaction editedTeam = RecruitsFaction.fromNBT(nbt);
        FactionEvents.modifyTeam(context.getSender().server.overworld(), stringID, editedTeam, context.getSender(), cost);
    }

    @Override
    public MessageSaveTeamSettings fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        this.stringID = buf.readUtf();
        this.cost = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
        buf.writeUtf(this.stringID);
        buf.writeInt(this.cost);
    }
}
